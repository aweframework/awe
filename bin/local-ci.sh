#!/bin/bash
set -uo pipefail

# ============================================================
# local-ci.sh
#
# Runs on this machine the jobs that the GitLab merge-request pipeline would run for the current branch, so a branch that
# passes here also passes there and no pipeline minutes are spent on a failure a laptop could have caught.
#
# The jobs are selected as GitLab selects them: the rules:changes globs of each job are read from .gitlab-ci.yml (no copy)
# and matched against the paths changed since the merge base with the base branch (bin/local-ci-tool.mjs). On top of the CI
# jobs it checks what SonarCloud would fail: the coverage of the new lines (quality gate: 80%) and a few Sonar rules.
#
# Usage:
#   bin/local-ci.sh [base] [options]          base defaults to origin/develop
#
# Options:
#   --title "<MR title>"      lint the merge request title (MR title lint); without it that job is skipped
#   --description-file <f>    the merge request description, for the docs warning (CI_MERGE_REQUEST_DESCRIPTION)
#   --list                    show which jobs the change set triggers and why, and run nothing
#   --only <ids>              run only these jobs (comma separated), whether the change set triggers them or not
#   --skip <ids>              do not run these jobs (comma separated)
#   --no-browser              skip the Playwright browser suites
#   --no-db-containers        skip the MySQL, PostgreSQL, SQL Server and Oracle jobs (Testcontainers, need Docker)
#   --browsers <list>         browsers of the Playwright suites: chromium (default: what a merge request runs), firefox
#                             (CI runs it on develop and master only) or chromium,firefox
#   --suites <words>          only the Playwright suites whose TEST_NAME contains one of these (comma separated)
#   --fast                    opt-in scope narrower than the merge request pipeline (also LOCAL_CI_SCOPE=affected): the heavy
#                             jobs run only when the change affects them (see "Fast scope" below). A pass no longer predicts a
#                             pass of the jobs it dropped.
#   --threshold <n>           new-code coverage threshold in percent (default 80, the SonarCloud quality gate)
#   --timeout-factor <n>      multiply every step timeout by n (default 1)
#   -h, --help                this help
#
# Job ids: run "bin/local-ci.sh --list". Logs: target/local-ci/<timestamp>/<step>.log, and a summary table at the end.
# Exit code: 0 when no blocking job failed, 1 when one did, 2 on a usage error, 130 when interrupted.
#
# Fast scope (--fast, LOCAL_CI_SCOPE=affected): by default the jobs are exactly those the merge request pipeline creates. The fast
# scope drops the heavy jobs that the change does not affect: a change of .gitlab-ci.yml alone (in every rules:changes list, so
# the pipeline runs everything) does not trigger them; the database jobs run only for a change in the framework modules or
# awe-boot; the AngularJS browser suites are left out for a change only in the React client, and the React suites for a change
# only in the AngularJS client or awe-boot. The pipeline would run what the fast scope drops, so use it while iterating and run
# without it before pushing. The database jobs always run only the awe-boot module, as in the pipeline (#863).
#
# Environment rules (fixed): every JVM is headless and never takes the focus on macOS (JAVA_TOOL_OPTIONS, which also sets the
# locale: -DargLine would stop the JaCoCo agent from attaching), Maven runs on the JDK of the build (java.version) with -s .m2/settings.xml,
# each browser suite picks free ports (never 8080) for its application, the JMX port of spring-boot:start and JaCoCo, and every
# process this script starts is stopped at the end, also on Ctrl-C. The awe-boot Spring test context listens on a random port.
#   LOCAL_CI_JDK<n>   home of the JDK <n> (the java.version of the build, e.g. LOCAL_CI_JDK21) instead of one found
#   LOCAL_CI_DB_PARALLEL       how many of All UT, the embedded DB jobs and the database jobs run at once (default 3; 1 = one
#                              after another, in this checkout). Each runs in a clone of the checkout (target/local-ci/<run>/work)
#   LOCAL_CI_BROWSER_PARALLEL  how many browser applications at a time on the machine, across runs (default 2: each is about 2 GB)
#   LOCAL_CI_SCOPE             "affected" is --fast
#
# Platforms: macOS, Linux and Windows through WSL2 (Ubuntu). A native Windows shell (Git Bash, MSYS2, Cygwin) is refused.
# ============================================================

# The repository is the one of the current directory (so one copy of the script can check another worktree), else the
# one of the script
BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
TOOL="$BIN_DIR/local-ci-tool.mjs"
# shellcheck source=bin/local-ci-platform.sh
. "$BIN_DIR/local-ci-platform.sh"
lci_platform_check "$(pwd)" || exit 2
lci_clean_env
lci_check_commands git node npm || exit 2
ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || ROOT=$(cd "$BIN_DIR/.." && pwd)
cd "$ROOT" || exit 2
CI_FILE="$ROOT/.gitlab-ci.yml"

BASE=origin/develop
TITLE=
DESCRIPTION=
LIST=0
ONLY=
SKIP=
NO_BROWSER=0
NO_DB=0
FAST=0
[ "${LOCAL_CI_SCOPE:-}" = affected ] && FAST=1
BROWSERS=chromium
SUITES=
THRESHOLD=80
TIMEOUT_FACTOR=1
# Ports of the browser suite that runs (lci_pick_ports): free ones per suite, never 8080
APP_PORT=
JMX_PORT=
JACOCO_PORT=
# Jobs of the unit-testing phase (All UT, the embedded DB jobs, the database jobs) that run at once
DB_PARALLEL=${LOCAL_CI_DB_PARALLEL:-3}
case "$DB_PARALLEL" in ''|*[!0-9]*|0) DB_PARALLEL=3 ;; esac
DB_PARALLEL=$(lci_parallel_width "$DB_PARALLEL")   # without rsync (clones) the jobs run one at a time, with a warning

usage() {
  sed -n '/^# Usage:/,/^# Environment rules/p' "$0" | sed -e 's/^# \{0,1\}//' -e '$d'
}

# An option that takes a value must have one: "$1" option, "$2" number of arguments left (the option included)
need_value() {
  if [ "$2" -lt 2 ]; then
    echo "local-ci: $1 needs a value." >&2
    usage >&2
    exit 2
  fi
}

while [ $# -gt 0 ]; do
  case "$1" in
    --title|--description-file|--only|--skip|--browsers|--suites|--threshold|--timeout-factor) need_value "$1" $# ;;
  esac
  case "$1" in
    --title) TITLE=$2; shift 2 ;;
    --description-file) DESCRIPTION=$(cat "$2") || exit 2; shift 2 ;;
    --list) LIST=1; shift ;;
    --only) ONLY=$2; shift 2 ;;
    --skip) SKIP=$2; shift 2 ;;
    --no-browser) NO_BROWSER=1; shift ;;
    --no-db-containers) NO_DB=1; shift ;;
    --fast) FAST=1; shift ;;
    --browsers) BROWSERS=$2; shift 2 ;;
    --suites) SUITES=$2; shift 2 ;;
    --threshold) THRESHOLD=$2; shift 2 ;;
    --timeout-factor) TIMEOUT_FACTOR=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    -*) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    *) BASE=$1; shift ;;
  esac
done

if ! MERGE_BASE=$(git merge-base "$BASE" HEAD 2>/dev/null); then
  echo "local-ci: no merge base between $BASE and HEAD (fetch it first: git fetch origin)." >&2
  exit 2
fi

# One run per checkout (--list changes nothing and needs no lock): two runs would share target/, the ports and the website
# swap. The lock lives in the git dir of this worktree, is taken before anything is written (a refused run touches nothing of
# the live one) and is released on exit.
LOCK_DIR=${LOCAL_CI_LOCK:-$(lci_lock_path)}
if [ "$LIST" != 1 ]; then
  lci_lock_acquire "$LOCK_DIR" || exit 2
  trap 'lci_lock_release "$LOCK_DIR"' EXIT
fi

STAMP=$(date +%Y%m%d-%H%M%S)
LOG_DIR="$ROOT/target/local-ci/$STAMP"
mkdir -p "$LOG_DIR"
lci_lock_write_owner "$LOG_DIR"   # this run's pid and start time: later runs prune the clones of dead runs only
RESULTS="$LOG_DIR/results.tsv"
LCI_ORACLE_GIVEN_UP_FILE="$LOG_DIR/oracle-given-up"   # shared by the pool workers: one Oracle lock timeout fails the other Oracle rows
: > "$RESULTS"
# Reports older than this marker come from an earlier run and never count (coverage)
STARTED_MARKER="$LOG_DIR/started"
touch "$STARTED_MARKER"

# ------------------------------------------------------------
# Change set: what the merge request changes (merge base to HEAD), plus the uncommitted and untracked files, because the
# Maven and npm steps build the working tree
# ------------------------------------------------------------
CHANGED="$LOG_DIR/changed-paths.txt"
DIFF_FILE="$LOG_DIR/changes.diff"
{
  git -c core.quotepath=off diff --name-only --no-renames "$MERGE_BASE" --
  lci_untracked
} | sort -u > "$CHANGED"
git -c core.quotepath=off diff -U0 --no-color --no-ext-diff --no-renames --src-prefix=a/ --dst-prefix=b/ "$MERGE_BASE" -- > "$DIFF_FILE"
lci_untracked | while IFS= read -r untracked; do
  git -c core.quotepath=off diff -U0 --no-color --no-index --src-prefix=a/ --dst-prefix=b/ /dev/null "$untracked" 2>/dev/null
done >> "$DIFF_FILE"
DIRTY=$(git status --porcelain | wc -l | tr -d ' ')

# The rules:if expressions are evaluated with the variables of a merge request from this branch into the base branch
LOCAL_CI_SOURCE_BRANCH=$(git rev-parse --abbrev-ref HEAD) LOCAL_CI_TARGET_BRANCH=${BASE#origin/} \
  node "$TOOL" select "$CI_FILE" "$CHANGED" $([ "$FAST" = 1 ] && echo --fast) > "$LOG_DIR/selection.tsv" || exit 2
node "$TOOL" jobs "$CI_FILE" > "$LOG_DIR/jobs.tsv" || exit 2

in_list() { # "$1" id, "$2" comma separated list
  case ",$2," in *",$1,"*) return 0 ;; *) return 1 ;; esac
}
job_field() { # "$1" id, "$2" column of jobs.tsv
  awk -F '\t' -v id="$1" -v col="$2" '$1 == id { print $col }' "$LOG_DIR/jobs.tsv"
}
selection_field() {
  awk -F '\t' -v id="$1" -v col="$2" '$1 == id { print $col }' "$LOG_DIR/selection.tsv"
}

# Decides whether a job runs: prints "run|skip<TAB>reason"
plan() {
  local id=$1 selected reason
  selected=$(selection_field "$id" 2)
  reason=$(selection_field "$id" 3)
  if [ -n "$ONLY" ]; then
    if in_list "$id" "$ONLY"; then selected=yes; reason="--only"; else selected=no; reason="not in --only"; fi
  fi
  if [ -n "$SKIP" ] && in_list "$id" "$SKIP"; then
    printf 'skip\t--skip\n'; return
  fi
  if [ "$selected" != yes ]; then
    printf 'skip\tnot triggered: %s\n' "$reason"; return
  fi
  case "$id" in
    playwright-*) [ "$NO_BROWSER" = 1 ] && { printf 'skip\t--no-browser\n'; return; } ;;
    db-*) [ "$NO_DB" = 1 ] && { printf 'skip\t--no-db-containers\n'; return; } ;;
    title) [ -z "$TITLE" ] && { printf 'skip\tno --title given (the title becomes the squash commit on develop)\n'; return; } ;;
    secret-detection) command -v gitleaks >/dev/null 2>&1 \
      || { printf 'skip\tgitleaks is not installed: not mirrored locally (CI runs GitLab'"'"'s secret detection)\n'; return; } ;;
  esac
  printf 'run\t%s\n' "$reason"
}

if [ "$LIST" = 1 ]; then
  echo "Base: $BASE (merge base $(git rev-parse --short "$MERGE_BASE")), $(wc -l < "$CHANGED" | tr -d ' ') changed paths"
  [ "$DIRTY" -gt 0 ] && echo "Uncommitted or untracked files are included ($DIRTY): the merge request pipeline sees only commits."
  echo
  printf '%-20s %-4s %s\n' JOB RUN WHY
  while IFS="$(printf '\t')" read -r id label blocking ci warn; do
    decision=$(plan "$id")
    printf '%-20s %-4s %s\n' "$id" "$(printf '%s' "$decision" | cut -f1 | sed 's/run/yes/;s/skip/no/')" \
      "$(printf '%s' "$decision" | cut -f2-)"
  done < "$LOG_DIR/jobs.tsv"
  rm -rf "$LOG_DIR"
  exit 0
fi

# A hard-killed website step of an earlier run may have left website/docs swapped with the 4.x docs: put it back now when the
# injection that swapped is dead (nothing is deleted; a live injection or an unmarked swap is only reported)
lci_heal_website_swap "$ROOT"

# ------------------------------------------------------------
# Environment
# ------------------------------------------------------------

running() { [ "$(plan "$1" | cut -f1)" = run ]; }

# The jobs that run Maven, and so need mvn and the JDK of the build
MAVEN_JOBS=spotless,build,all-ut,embedded-db,db-mysql,db-postgresql,db-sqlserver,db-oracle,frontend-ut,frontend-ut-react,javadoc,coverage,playwright-angular,playwright-react
MAVEN_NEEDED=0
for id in $(printf '%s' "$MAVEN_JOBS" | tr ',' ' '); do
  running "$id" && MAVEN_NEEDED=1
done

# The JDK is the one the build asks for (java.version of awe-dependencies: 21 since #783), found as LOCAL_CI_JDK<n> or among
# the installed ones
JAVA_MAJOR=$(lci_required_java "$ROOT")
JAVA_MAJOR=${JAVA_MAJOR:-21}
JDK=$(lci_find_jdk "$JAVA_MAJOR")
# Without mvn or the JDK only the Maven jobs are refused (each with the reason); the other jobs still run
MAVEN_UNAVAILABLE=
if [ "$MAVEN_NEEDED" = 1 ]; then
  if ! lci_check_commands mvn; then
    MAVEN_UNAVAILABLE="mvn is not installed"
  elif [ -z "$JDK" ]; then
    lci_jdk_help "$JAVA_MAJOR" >&2
    MAVEN_UNAVAILABLE="no JDK $JAVA_MAJOR found (set LOCAL_CI_JDK$JAVA_MAJOR)"
  fi
  [ -n "$MAVEN_UNAVAILABLE" ] && echo "local-ci: the Maven jobs will not run: $MAVEN_UNAVAILABLE" >&2
fi

# The Maven repository of this checkout (see lci_maven_repo): seeded once with third-party artifacts, never another
# worktree's AWE modules
MAVEN_REPO=$(lci_maven_repo "$ROOT")
[ "$MAVEN_NEEDED" = 1 ] && [ -z "$MAVEN_UNAVAILABLE" ] && lci_seed_maven_repo "$HOME/.m2/repository" "$MAVEN_REPO"

if [ -n "$JDK" ]; then
  export JAVA_HOME="$JDK"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Linux and WSL: the Playwright browsers need system libraries that CI installs as root (--with-deps). The packages are those
# Playwright itself would install for the selected browsers (install-deps --dry-run of the pinned version, from the jars that
# CI uses too); the missing ones are reported with the command that installs them. Advisory only: sudo is the user's call,
# and a step fails only when a browser really cannot start (run_row then names the same command).
PLAYWRIGHT_VERSION=$(sed -n 's:.*<playwright.version>\(.*\)</playwright.version>.*:\1:p' awe-framework/awe-dependencies/pom.xml)
PLAYWRIGHT_JARS="$MAVEN_REPO/com/microsoft/playwright"
PLAYWRIGHT_CP="$PLAYWRIGHT_JARS/playwright/$PLAYWRIGHT_VERSION/playwright-$PLAYWRIGHT_VERSION.jar"
PLAYWRIGHT_CP="$PLAYWRIGHT_CP:$PLAYWRIGHT_JARS/driver/$PLAYWRIGHT_VERSION/driver-$PLAYWRIGHT_VERSION.jar"
PLAYWRIGHT_CP="$PLAYWRIGHT_CP:$PLAYWRIGHT_JARS/driver-bundle/$PLAYWRIGHT_VERSION/driver-bundle-$PLAYWRIGHT_VERSION.jar"
# Chromium always (the adapter tests of All UT use it), plus the browsers of the suites
playwright_browsers() { printf '%s' "chromium,$BROWSERS" | tr ',' '\n' | awk 'NF && !seen[$0]++' | tr '\n' ' ' | sed 's/ $//'; }
playwright_deps_hint() { lci_playwright_deps_command "${JAVA_HOME:-/usr}/bin/java" "$PLAYWRIGHT_CP" "$(playwright_browsers)"; }
if [ "$(lci_os)" != mac ] && { running all-ut || running playwright-angular || running playwright-react; } \
  && [ -f "$PLAYWRIGHT_JARS/driver-bundle/$PLAYWRIGHT_VERSION/driver-bundle-$PLAYWRIGHT_VERSION.jar" ] && [ -n "$JDK" ]; then
  # shellcheck disable=SC2046 # the browser names are single words
  "$JAVA_HOME/bin/java" -cp "$PLAYWRIGHT_CP" com.microsoft.playwright.CLI install-deps --dry-run $(playwright_browsers) \
    > "$LOG_DIR/playwright-install-deps.txt" 2>&1 < /dev/null
  # shellcheck disable=SC2046
  missing_packages=$(lci_missing_packages $(node "$TOOL" apt-packages "$LOG_DIR/playwright-install-deps.txt") | tr '\n' ' ')
  if [ -n "$missing_packages" ]; then
    echo "local-ci: the Playwright browsers ($(playwright_browsers)) need packages that are not installed: $missing_packages"
    echo "local-ci: install them once with: $(playwright_deps_hint)"
    echo
  fi
fi
export JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -Dapple.awt.UIElement=true -Duser.language=en -Duser.country=GB"
MVN=(mvn -s "$ROOT/.m2/settings.xml" "-Dsettings.security=$ROOT/.m2/settings-security.xml" "-Dmaven.repo.local=$MAVEN_REPO"
  '-P!devtools' --batch-mode -Dstyle.color=never -Dskip.lint=true)
# UT_FLAGS are the flags of every unit-testing run; UT_OPTS (All UT, coverage) add the modules of CI. The unit profiles use UT_FLAGS and
# choose their modules themselves (unit_modules: only awe-boot, as CI)
UT_FLAGS=(-Dskip.frontend=true -DgenerateUnitReport=true)
UT_OPTS=("${UT_FLAGS[@]}" -pl '!awe-framework/awe-boot-angular-archetype' -pl '!awe-framework/awe-boot-react-archetype')

# ------------------------------------------------------------
# Step runner: each step runs in a process group of its own (job control), with a timeout, its output in a log file
# ------------------------------------------------------------
CURRENT_PID=
INTERRUPTED=0
STARTED_APP=0

kill_group() { # "$1" pid of the group leader
  local pid=$1 i
  [ -n "$pid" ] || return 0
  kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
  for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
    kill -0 "$pid" 2>/dev/null || return 0
    sleep 1
  done
  kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null
}

# The pids listening on a port (lsof, else ss, else fuser); only numeric ones can be stopped
port_pids() { lci_port_pids "$1" | grep -E '^[0-9]+$'; }

# The pids listening on the picked ports that this run started (a descendant of the script, or an orphan of this checkout). The
# port was free when picked, but another process may have taken it since: that one is never stopped.
own_listeners() {
  local pid
  for pid in $(port_pids "$JMX_PORT"; port_pids "$APP_PORT"); do
    lci_is_ours "$pid" "$ROOT" "$$" && echo "$pid"
  done
}

# Stops the test application when a browser step started it and Maven did not stop it (killed, timed out)
stop_app() {
  local pids
  if [ "$STARTED_APP" = 1 ] && [ -n "$APP_PORT" ]; then
    pids=$(own_listeners)
    if [ -n "$pids" ]; then
      echo "Stopping the test application left on ports $JMX_PORT/$APP_PORT (pids $(echo $pids))"
      kill $pids 2>/dev/null
      sleep 3
      pids=$(own_listeners)
      [ -n "$pids" ] && kill -9 $pids 2>/dev/null
    fi
    STARTED_APP=0
  fi
}

# Before the website step: a snapshot of exactly the paths the 4.x docs injection writes (lci_website_injection_paths)
WEBSITE_SNAPSHOT=
# Returns 1 (and the website step must not run) when a swap of another run is still there: the injection would refuse it.
snapshot_website() {
  # An orphan swap of a hard-killed earlier run goes back first, so the snapshot sees the developer's docs
  lci_heal_website_swap "$ROOT"
  lci_swap_pending "$ROOT" && return 1
  WEBSITE_SNAPSHOT="$LOG_DIR/website-snapshot"
  # shellcheck disable=SC2046 # the paths have no spaces or newlines (fixed names under website/)
  lci_snapshot "$WEBSITE_SNAPSHOT" $(lci_website_injection_paths "${MAINTENANCE_LABEL:-4.x}")
  # The injection marks its swap with its own pid (website_swap_mark), so a later run never takes the swap of an injection
  # that outlived this run for an orphan
  return 0
}

# After the website step (also when it was interrupted): undoes what the injection wrote, from the snapshot, and nothing
# else. The developer's own edits under website/ during the step are never reverted or deleted.
restore_website() {
  # The injection swaps website/docs and website/sidebars.js back in its own exit trap; a kill of the step can skip that
  # trap, and then this run puts back its own swap (moving what it finds aside, never deleting it) and drops its marker
  lci_heal_website_swap "$ROOT" own
  [ -n "$WEBSITE_SNAPSHOT" ] && lci_restore "$WEBSITE_SNAPSHOT"
  WEBSITE_SNAPSHOT=
}

cleanup() {
  # Nothing interrupts the cleanup: a second Ctrl-C or TERM (a parent that sends INT, then TERM) must not leave it half done
  trap '' INT TERM
  type pool_stop >/dev/null 2>&1 && pool_stop
  if [ -n "$CURRENT_PID" ]; then
    kill_group "$CURRENT_PID"
    CURRENT_PID=
  fi
  stop_app
  [ "${WEBSITE_TOUCHED:-0}" = 1 ] && restore_website
  lci_locks_release force
  lci_lock_release "$LOCK_DIR"
}

# The first Ctrl-C (INT) is graceful: the steps are stopped and recorded as interrupted, the summary is printed. A TERM, or a
# second signal, ends the run now: the exit runs the cleanup, which stops every worker and what it started (TERM, then KILL
# after LOCAL_CI_POOL_GRACE seconds). Workers (LCI_WORKER=1) only flag it: the pool tells them and stops them if they linger.
INTERRUPT_COUNT=0
INTERRUPT_AT=0
on_interrupt() { # [signal name]
  INTERRUPTED=1
  INTERRUPT_COUNT=$((INTERRUPT_COUNT + 1))
  [ "$INTERRUPT_AT" = 0 ] && INTERRUPT_AT=$(date +%s)
  echo
  echo "Interrupted: stopping the running step..."
  if [ "${LCI_WORKER:-0}" != 1 ] && { [ "${1:-}" = TERM ] || [ "$INTERRUPT_COUNT" -ge 2 ]; }; then
    echo "Terminated: stopping everything the run started..."
    exit 130
  fi
}
trap 'on_interrupt INT' INT
trap 'on_interrupt TERM' TERM
trap cleanup EXIT

timeout_seconds() { # minutes -> seconds, times the factor
  awk -v m="$1" -v f="$TIMEOUT_FACTOR" 'BEGIN { printf "%d", m * f * 60 }'
}

# Sums the Maven test summaries of a log ("Tests run: N, Failures: F, Errors: E, Skipped: S" without "Time elapsed")
test_counts() { lci_test_counts "$1"; }

record() { # id label status blocking seconds note
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$4" "$5" "$6" >> "$RESULTS"
}

# run_step <row id> <label> <blocking yes|no> <timeout minutes> <function> [args...]
run_step() {
  local id=$1 label=$2 blocking=$3 minutes=$4
  shift 4
  local log="$LOG_DIR/$id.log" start end rc=0 limit status note
  if [ "$INTERRUPTED" = 1 ]; then
    record "$id" "$label" SKIP "$blocking" 0 "interrupted"
    return
  fi
  limit=$(timeout_seconds "$minutes")
  start=$(date +%s)
  printf '%s  %-44s ' "$(date +%H:%M:%S)" "$label"
  export STEP_NOTE="$LOG_DIR/$id.note"
  rm -f "$STEP_NOTE"
  set -m
  ( "$@" ) > "$log" 2>&1 < /dev/null &
  CURRENT_PID=$!
  set +m
  local timed_out=0
  while kill -0 "$CURRENT_PID" 2>/dev/null; do
    if [ "$INTERRUPTED" = 1 ]; then
      kill_group "$CURRENT_PID"
      break
    fi
    if [ $(( $(date +%s) - start )) -ge "$limit" ]; then
      echo "local-ci: timeout after $minutes minutes (x$TIMEOUT_FACTOR): stopping the step" >> "$log"
      kill_group "$CURRENT_PID"
      timed_out=1
      break
    fi
    sleep 2
  done
  wait "$CURRENT_PID" 2>/dev/null
  rc=$?
  CURRENT_PID=
  end=$(date +%s)
  note=$(cat "$STEP_NOTE" 2>/dev/null | head -1)
  [ -z "$note" ] && note=$(test_counts "$log")
  # The warning exit codes are those of the job of this row (STEP_WARN, set by run_row from the CI allow_failure)
  status=$(lci_status "$rc" "${STEP_WARN:-}" "$timed_out" "$INTERRUPTED")
  if [ "$INTERRUPTED" = 1 ]; then
    note="interrupted"
  elif [ "$timed_out" = 1 ]; then
    note="timeout after ${minutes}m${note:+; $note}"
  fi
  if [ "$status" != PASS ]; then
    # Without a note of the step, the first line of its log says what happened (the MR checks print their verdict first)
    [ -z "$note" ] && [ "$status" = WARN ] && note=$(grep -m1 -v '^[[:space:]]*$' "$log" | cut -c1-100)
    note="${note:+$note; }log: ${log#"$ROOT"/}"
  fi
  record "$id" "$label" "$status" "$blocking" $((end - start)) "$note"
  printf '%-5s %s\n' "$status" "$(awk -v s=$((end - start)) 'BEGIN { if (s < 60) printf "%ds", s; else printf "%dm%02ds", s / 60, s % 60 }')"
}

skip_step() { # id label reason
  record "$1" "$2" SKIP no 0 "$3"
}

note() { printf '%s\n' "$*" > "$STEP_NOTE"; }

missing_node_modules() { # "$1" directory with a package.json
  if [ ! -d "$1/node_modules" ]; then
    echo "note: $1/node_modules is missing; this step installs it with npm ci as CI does (network, a few minutes)."
  fi
}

# ------------------------------------------------------------
# Steps (each runs in its own subshell; its exit code is the result, 64 = warning)
# ------------------------------------------------------------

step_title() { bin/lint-mr-title.sh "$TITLE"; }

step_mr_size() { bin/mr-size.sh "$MERGE_BASE" HEAD; }

step_mr_docs() {
  CI_MERGE_REQUEST_TITLE="$TITLE" CI_MERGE_REQUEST_DESCRIPTION="$DESCRIPTION" bin/mr-docs.sh "$BASE" HEAD
}

step_mr_translations() { bin/mr-translations.sh "$BASE" HEAD; }

step_mr_checks_tests() { sh bin/test-mr-checks.sh; }

step_sonar_rules() {
  node "$TOOL" sonar --diff "$DIFF_FILE" --root "$ROOT"
  local rc=$?
  [ "$rc" -eq 64 ] && note "$(grep -c '^WARN ' "$LOG_DIR/sonar-rules.log") possible issues (S1192/S125/S5778/S1130)"
  return "$rc"
}

# "Secret detection" runs GitLab's analyzer, which is gitleaks: the commits of the branch are scanned with the local gitleaks
# (only when it is installed; a finding is a warning here, the pipeline decides)
step_secret_detection() {
  gitleaks detect --source . --log-opts "$MERGE_BASE..HEAD" --redact --no-banner
}

step_spotless() { "${MVN[@]}" spotless:check "-Dspotless.ratchetFrom=$MERGE_BASE"; }

# As the "Lint frontend" job: npm ci of both clients, then both lints (one failure does not hide the other)
step_lint_frontend() {
  local client status=0
  for client in awe-framework/awe-client-angular awe-framework/awe-client-react; do
    missing_node_modules "$client"
    echo "+ npm --prefix $client ci --include=dev --ignore-scripts --no-audit --no-fund"
    npm --prefix "$client" ci --include=dev --ignore-scripts --no-audit --no-fund || return 1
  done
  for client in awe-framework/awe-client-angular awe-framework/awe-client-react; do
    echo "+ npm --prefix $client run lint:ci  (eslint $(node -p "require('./$client/node_modules/eslint/package.json').version"))"
    npm --prefix "$client" run lint:ci || status=1
  done
  return "$status"
}

step_build() {
  "${MVN[@]}" install -DskipTests || return 1
  note "version $("${MVN[@]}" -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null), JDK $(java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -1)"
}


# Exec files of the previous runs of the same job would be appended to (JaCoCo append=true)
clean_exec() { find . -name node_modules -prune -o -path '*/target/jacoco-'"$1"'.exec' -type f -print -exec rm -f {} + 2>/dev/null; }

# awe-boot and awe-boot-react keep file HSQLDB databases: their tests in target/tests/db, the test applications the browser
# suites start in target/db. A stale one breaks the next run: a run cut halfway leaves its rows (a scheduler calendar left in
# target/db breaks the next run with a unique key violation)
AWE_BOOT_DB_DIRS=(
  awe-tests/awe-boot/target/tests/db awe-tests/awe-boot-react/target/tests/db
  awe-tests/awe-boot/target/db awe-tests/awe-boot-react/target/db
)
clean_awe_boot_db() {
  rm -rf "${AWE_BOOT_DB_DIRS[@]}"
}

# The unit-testing jobs (All UT, the embedded DB jobs, the database jobs) run in parallel (run_pool), and the Maven build writes
# target/ (classes, reports, JaCoCo data, the embedded HSQLDB of awe-boot). So with more than one at a time each runs in a clone of
# the checkout of its own (target/local-ci/<run>/work/<row>: sources and the build output, without .git, node_modules, website/build and website/.docusaurus
# and the private Maven repository, which stays shared and is only read) and brings its JaCoCo execution file back to the checkout
# for the coverage step. A clone that passed is deleted, a failed one is kept for the developer.
# in_work <row id> <JaCoCo exec file> <command...>
in_work() {
  local row=$1 exec_file=$2 work rc
  shift 2
  if [ "$DB_PARALLEL" -le 1 ]; then
    "$@"
    return
  fi
  work="$LOG_DIR/work/$row"
  echo "+ clone of the checkout for this job: ${work#"$ROOT"/}"
  lci_clone_work "$ROOT" "$work" || return 1
  (cd "$work" && "$@")
  rc=$?
  lci_bring_back "$work" "$ROOT" "$exec_file"
  [ "$rc" -eq 0 ] && rm -rf "$work"
  return "$rc"
}

# The Maven modules of the database rows: only awe-boot, as the database jobs of CI (#863). The database profiles exist only in
# awe-boot, so the other modules would run the same tests as All UT again; awe-boot resolves them from the build step's install
unit_modules() {
  printf '%s\n' -pl awe-tests/awe-boot
}

step_all_ut() { in_work all-ut jacoco-all-ut.exec all_ut_maven; }

# The Maven arguments (after "verify") of the unit-testing rows, one per line, the ones of the CI jobs (bin/test-local-ci.sh pins
# them): All UT runs the reactor of CI, without the two archetypes; every database profile runs only awe-boot.
all_ut_args() {
  printf '%s\n' "${UT_OPTS[@]}" -Djacoco.file.name=jacoco-all-ut.exec -Dawe.test.playwright.required=true
}
unit_profile_args() {
  printf '%s\n' "${UT_FLAGS[@]}"
  unit_modules
  printf '%s\n' "-P$1" "-Djacoco.file.name=jacoco-$1-ut.exec" -Doracle.jdbc.J2EE13Compliant=true -Doracle.jdbc.timezoneAsRegion=false
}

all_ut_maven() {
  local args=()
  clean_exec all-ut
  clean_awe_boot_db
  while IFS= read -r arg; do args+=("$arg"); done < <(all_ut_args)
  TZ=Europe/Madrid "${MVN[@]}" verify "${args[@]}"
}

# One profile of the ".unit-testing" jobs (Embedded DB Tests and the database container jobs)
step_unit_profile() { in_work "$2" "jacoco-$1-ut.exec" unit_profile_maven "$1"; }
unit_profile_maven() {
  local profile=$1 args=()
  clean_exec "$profile-ut"
  clean_awe_boot_db
  if [ "$profile" = sqlserver ] || [ "$profile" = sqlserver-flyway ]; then
    export AWE_TESTING_SQLSERVER_ACCEPT_EULA=true   # as the "SQL Server Tests" job (Microsoft EULA)
  fi
  while IFS= read -r arg; do args+=("$arg"); done < <(unit_profile_args "$profile")
  TZ=Europe/Madrid "${MVN[@]}" verify "${args[@]}"
}

step_frontend_ut() {
  missing_node_modules "$1"
  BABEL_ENV=test NODE_ENV=test "${MVN[@]}" -f "$1/pom.xml" test || return 1
  local counts
  counts=$(sed -n 's/.*Tests: *\([0-9].*total\)$/\1/p' "$LOG_DIR/$2.log" | tail -1)
  [ -n "$counts" ] && note "jest: $counts"
  return 0
}

# As "Javadoc test": the Javadoc of each module's delombok sources (as develop builds it), then the aggregate
step_javadoc() {
  "${MVN[@]}" -f awe-framework/pom.xml javadoc:javadoc@delombok-javadoc javadoc:aggregate-no-fork -Dskip.javadoc=false \
    -Ptest-javadoc
}

# New-code coverage: what the SonarCloud quality gate measures (lines + conditions of the new code, >= 80%)
step_coverage() {
  local jacoco_xml=awe-tests/awe-boot/target/coverage-reports/jacoco.xml
  local angular_lcov=awe-framework/awe-client-angular/target/reports/jest/coverage/lcov.info
  local react_lcov=awe-framework/awe-client-react/target/reports/jest/coverage/lcov.info
  local scope exec_files fresh_exec stale_dir f rc args=()
  # What Sonar measures among the changed files (the same rules as the coverage itself: local-ci-tool.mjs coverageSource)
  scope=$(node "$TOOL" scope "$CHANGED")
  if printf '%s\n' "$scope" | grep -qx jacoco; then
    # Only the execution data of All UT of this very run: an earlier run describes other code
    if [ "$ALL_UT_OK" != 1 ]; then
      echo "Java sources changed: the coverage needs All UT to run and pass in this run (e.g. --only all-ut,coverage)."
      note "Java coverage needs all-ut passing in this run"
      return 1
    fi
    exec_files=$(find . -name node_modules -prune -o -path '*/target/jacoco-*.exec' -type f -print 2>/dev/null)
    # shellcheck disable=SC2086 # the paths of target/ have no spaces
    fresh_exec=$(lci_fresh "$STARTED_MARKER" $exec_files)
    if [ -z "$fresh_exec" ]; then
      echo "All UT wrote no JaCoCo execution data in this run (target/jacoco-*.exec): the agent did not attach."
      note "no JaCoCo data from this run"
      return 1
    fi
    echo "Exec files of this run:"
    printf '%s\n' "$fresh_exec" | sed 's/^/  /'
    # report-aggregate reads every target/*.exec: the older ones are set aside while it runs, then put back
    stale_dir="$LOG_DIR/stale-exec"
    for f in $exec_files; do
      printf '%s\n' "$fresh_exec" | grep -qxF "$f" && continue
      mkdir -p "$stale_dir/$(dirname "$f")" && mv "$f" "$stale_dir/$f"
    done
    # The aggregate report of awe-boot, the one Sonar reads (sonar.coverage.jacoco.xmlReportPaths)
    rm -rf awe-tests/awe-boot/target/coverage-reports
    "${MVN[@]}" -DgenerateCoverageReport=true -Dskip.frontend=true "${UT_OPTS[@]}" \
      org.jacoco:jacoco-maven-plugin:report-aggregate@report-integration
    rc=$?
    if [ -d "$stale_dir" ]; then
      (cd "$stale_dir" && find . -type f -print) | while IFS= read -r f; do mv "$stale_dir/$f" "$f"; done
    fi
    [ "$rc" -eq 0 ] || return 1
    [ -f "$jacoco_xml" ] || { echo "The aggregate JaCoCo report was not written ($jacoco_xml)"; return 1; }
    args+=(--jacoco "$jacoco_xml")
  fi
  # The Jest reports count only when the frontend unit tests wrote them in this run; otherwise their new lines have no data
  for f in "awe-framework/awe-client-angular=$angular_lcov" "awe-framework/awe-client-react=$react_lcov"; do
    if [ -n "$(lci_fresh "$STARTED_MARKER" "${f#*=}")" ]; then
      args+=(--lcov "$f")
    elif printf '%s\n' "$scope" | grep -qx "lcov:${f%%=*}"; then
      echo "No Jest coverage of ${f%%=*} from this run: run its frontend unit tests too (frontend-ut / frontend-ut-react)."
    fi
  done
  echo
  node "$TOOL" coverage --diff "$DIFF_FILE" --root "$ROOT" --threshold "$THRESHOLD" ${args[@]+"${args[@]}"}
  rc=$?
  note "$(grep -E '^Coverage on new code|^No new executable' "$LOG_DIR/coverage.log" | tail -1)"
  return "$rc"
}

# "Spanish docs lint": MDX syntax, parity with the English page and relative links of the Spanish translation
step_spanish_docs_lint() {
  missing_node_modules website
  npm --prefix website ci || return 1
  npm --prefix website run check:translations
}

# "Build website" (test:scripts, 4.x injection, anchors) with every locale: a merge request builds English only
# (--locale en), develop and master build every locale ("Build website with i18n"), so the Spanish site is checked here too
step_website() {
  local real_git shim status
  missing_node_modules website
  npm --prefix website ci || return 1
  if ! npm --prefix website run test:scripts; then
    # That test compares the committed properties snapshot with the target/classes of the modules when they are built, which
    # CI never has (it skips the test): a build older than the sources gives a false failure here
    if grep -q 'spring-configuration-metadata.json is out of date' "$LOG_DIR/website.log"; then
      note "properties snapshot vs target/classes: rebuild (--only build) if target/ is older than your branch; CI skips this test"
    fi
    return 1
  fi
  # bin/website-maintenance-docs.sh fetches support/4.x with --depth=1, which can make a full clone shallow: fetch it whole
  # first and let a git shim drop --depth for this run
  git fetch -q origin support/4.x || true
  real_git=$(command -v git)
  shim="$LOG_DIR/git-shim"
  mkdir -p "$shim"
  cat > "$shim/git" <<EOF
#!/bin/bash
if [ "\$1" = fetch ]; then
  args=()
  for a in "\$@"; do case "\$a" in --depth|--depth=*) ;; *) args+=("\$a") ;; esac; done
  exec "$real_git" "\${args[@]}"
fi
exec "$real_git" "\$@"
EOF
  chmod +x "$shim/git"
  PATH="$shim:$PATH" bin/website-maintenance-docs.sh || return 1
  npm --prefix website run build 2>&1 | tee "$LOG_DIR/website-build.log"
  status=${PIPESTATUS[0]}
  [ "$status" -eq 0 ] || return "$status"
  bin/check-website-anchors.sh "$LOG_DIR/website-build.log"
}

# One entry of a Playwright matrix: <module> <selection> <name> <browser>. The application, its JMX port and the JaCoCo port are
# the free ports of this suite (lci_pick_ports), not the fixed 8090/9001 and the TEST_PORT of the CI job
step_playwright() {
  local module=$1 selection=$2 name=$3 browser=$4
  clean_awe_boot_db
  rm -f "$module/target/jacoco-$name-it.exec"
  SERVER_PORT=$APP_PORT "${MVN[@]}" -f "$module/pom.xml" verify -Dskip.junit=true -Dskip.selenium=false \
    -DgenerateIntegrationReport=true "-Dawe.test.browser=$browser" "-Dawe.test.screenshot-path=$LOG_DIR/browser-evidence/$name/" \
    "$selection" -Dawe.test.tool=playwright -Dawe.test.allowed-recording=false "-Djacoco.file.name=jacoco-$name-it.exec" \
    "-Djacoco.port=$JACOCO_PORT" "-Dawe.test.jmx-port=$JMX_PORT" "-Dspring-boot.run.arguments=--server.port=$APP_PORT" \
    "-Dawe.test.server-port=$APP_PORT" -Dawe.test.server-host=localhost "-Dawe.test.start-url=http://localhost:$APP_PORT/"
}

step_react_dist() { "${MVN[@]}" -f awe-framework/awe-client-react/pom.xml compile; }

# ------------------------------------------------------------
# Run
# ------------------------------------------------------------

echo "Local CI of $(git rev-parse --abbrev-ref HEAD) against $BASE (merge base $(git rev-parse --short "$MERGE_BASE")): $(wc -l < "$CHANGED" | tr -d ' ') changed paths"
[ "$DIRTY" -gt 0 ] && echo "Note: $DIRTY uncommitted or untracked files are included; the merge request pipeline sees only commits."
echo "JDK $JAVA_MAJOR: ${JDK:-NOT FOUND}   Logs: ${LOG_DIR#"$ROOT"/}"
echo

decision() { plan "$1" | cut -f1; }
reason() { plan "$1" | cut -f2-; }
label_of() { job_field "$1" 2; }
blocking_of() { job_field "$1" 3; }


# run_row <job id> <row id> <row label> <timeout minutes> <function> [args]: one row of a job (a job, a database profile, a
# browser suite), with the guards of the job (selected, JDK of the build) and its warning exit codes; returns 1 when it did not pass
run_row() {
  local id=$1 row=$2 label=$3 minutes=$4
  shift 4
  if [ "$(decision "$id")" != run ]; then
    skip_step "$row" "$label" "$(reason "$id")"
    return 0
  fi
  if [ -n "$MAVEN_UNAVAILABLE" ] && in_list "$id" "$MAVEN_JOBS"; then
    record "$row" "$label" FAIL yes 0 "$MAVEN_UNAVAILABLE"
    return 1
  fi
  STEP_WARN=$(job_field "$id" 5)
  run_step "$row" "$label" "$(blocking_of "$id")" "$minutes" "$@"
  STEP_WARN=
  [ "$(tail -1 "$RESULTS" | cut -f3)" = PASS ] && return 0
  # A browser that cannot start for missing system libraries says so in the log: name the fix in the summary
  if lci_browser_libs_failure "$LOG_DIR/$row.log"; then
    append_note "browser libraries missing: $(playwright_deps_hint)"
  fi
  return 1
}

# run_job <id> <timeout minutes> <function> [args]: one row for the job
run_job() {
  local id=$1 minutes=$2
  shift 2
  run_row "$id" "$id" "$(label_of "$id")" "$minutes" "$@"
}

# Adds a text to the note of the last row of the results
append_note() {
  local last
  last=$(tail -1 "$RESULTS")
  sed '$d' "$RESULTS" > "$RESULTS.tmp" && mv "$RESULTS.tmp" "$RESULTS"
  printf '%s; %s\n' "$last" "$1" >> "$RESULTS"
}

# run_locked <oracle yes|no> <job id> <row id> <row label> <timeout minutes> <function> [args]: run_row for a row that may start
# an Oracle container. An Oracle row holds the machine-wide Oracle lock for that row only (one container at a time, across runs;
# the Oracle rows of this run queue on it too), waits while another run holds it, and never runs without it. Any other row needs
# no lock: its ports are free ones and awe-boot's test context listens on a random port.
run_locked() {
  local oracle=$1 id=$2 row=$3 label=$4 minutes=$5 rc
  shift 5
  if [ "$oracle" != yes ] || [ "$(decision "$id")" != run ]; then
    run_row "$id" "$row" "$label" "$minutes" "$@"
    return
  fi
  if [ "${INTERRUPTED:-0}" = 1 ]; then   # after Ctrl-C no lock is waited for: the row is skipped, as run_step does
    record "$row" "$label" SKIP "$(blocking_of "$id")" 0 "interrupted"
    return 1
  fi
  LOCK_ERROR=
  lci_with_oracle_lock "local-ci $(basename "$ROOT")" run_row "$id" "$row" "$label" "$minutes" "$@"
  rc=$?
  if [ "$rc" -eq "$LCI_LOCK_FAILED" ] && [ -n "$LOCK_ERROR" ]; then   # the lock could not be taken: the row did not run
    if [ "${INTERRUPTED:-0}" = 1 ]; then   # Ctrl-C arrived during the wait
      record "$row" "$label" SKIP "$(blocking_of "$id")" 0 "interrupted"
    else
      record "$row" "$label" FAIL "$(blocking_of "$id")" 0 "$LOCK_ERROR"
    fi
    return 1
  fi
  return "$rc"
}

# ------------------------------------------------------------
# The pool: run_pool <max> <function> <item>... runs "function item" for each item in a subshell of its own, at most <max> at a
# time. A worker writes its rows to a results file of its own and its output to a file of its own (so two workers never write the
# same line or read each other's last row); the pool shows an output when its worker ends and, at the end, adds the results to
# RESULTS in the order of the items, whatever the order of completion. A failing worker does not stop the others. On Ctrl-C
# (INTERRUPTED) every running worker is told (its own handler stops its step and records it as interrupted), the items not
# started yet are launched interrupted (a worker records them as SKIP at once) and the pool waits for all of them.
# ------------------------------------------------------------
POOL_PIDS=()   # pid of the worker of item i (index from 1), empty once it ended
POOL_N=0       # items launched
POOL_DIR=
POOL_TOLD=0

pool_running() { # how many workers have not ended (the ones that ended are reaped by pool_tick)
  local i count=0
  for ((i = 1; i <= POOL_N; i++)); do
    [ -n "${POOL_PIDS[$i]:-}" ] && count=$((count + 1))
  done
  printf '%s\n' "$count"
}

# Reaps the workers that ended (showing their output), and tells the running ones about Ctrl-C once
pool_tick() {
  local i pid
  for ((i = 1; i <= POOL_N; i++)); do
    pid=${POOL_PIDS[$i]:-}
    [ -n "$pid" ] || continue
    if kill -0 "$pid" 2>/dev/null; then
      [ "${INTERRUPTED:-0}" = 1 ] && [ "$POOL_TOLD" = 0 ] && kill -TERM "$pid" 2>/dev/null
    else
      wait "$pid" 2>/dev/null
      POOL_PIDS[$i]=
      cat "$POOL_DIR/$i.out" 2>/dev/null
    fi
  done
  [ "${INTERRUPTED:-0}" = 1 ] && POOL_TOLD=1
  # Workers that did not stop their steps within the grace period after the interruption (a trap deferred behind a foreground
  # command, or ignored): stop them and everything they started now, instead of waiting for them for ever
  if [ "${INTERRUPTED:-0}" = 1 ] && [ "${INTERRUPT_AT:-0}" != 0 ] && [ $(( $(date +%s) - INTERRUPT_AT )) -ge "$(lci_pool_grace)" ]; then
    pool_stop
  fi
  return 0
}

# Stops the workers still running (cleanup: exit and signals): TERM, so their handlers stop their steps, then after
# LOCAL_CI_POOL_GRACE seconds (default 25) KILL for the ones left, with all their descendants, and wait: nothing outlives the script
pool_stop() {
  local i pid waited=0 grace=$(lci_pool_grace) known= p
  # What the workers started, before anything is told to stop: a child whose parent dies first is adopted by init and could not
  # be found from the workers afterwards
  for ((i = 1; i <= POOL_N; i++)); do
    pid=${POOL_PIDS[$i]:-}
    [ -n "$pid" ] && known="$known $(lci_descendants "$pid")"
  done
  for ((i = 1; i <= POOL_N; i++)); do
    pid=${POOL_PIDS[$i]:-}
    [ -n "$pid" ] && kill -TERM "$pid" 2>/dev/null
  done
  while [ "$waited" -lt "$grace" ] && [ "$(pool_running)" -gt 0 ]; do
    for ((i = 1; i <= POOL_N; i++)); do
      pid=${POOL_PIDS[$i]:-}
      [ -n "$pid" ] && ! kill -0 "$pid" 2>/dev/null && POOL_PIDS[$i]=
    done
    [ "$(pool_running)" -gt 0 ] && sleep 1
    waited=$((waited + 1))
  done
  for ((i = 1; i <= POOL_N; i++)); do
    pid=${POOL_PIDS[$i]:-}
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      echo "local-ci: a worker did not stop in ${grace}s: killing it and what it started"
      lci_kill_tree KILL "$pid"
      wait "$pid" 2>/dev/null
    fi
    POOL_PIDS[$i]=
  done
  for p in $known; do   # the ones left after their worker is gone
    kill -0 "$p" 2>/dev/null && kill -KILL "$p" 2>/dev/null
  done
  POOL_N=0
  return 0
}

run_pool() {
  local max=$1 fn=$2 n=0 item i poll=${LOCAL_CI_POOL_POLL:-1}
  shift 2
  POOL_DIR=$(mktemp -d "$LOG_DIR/pool.XXXXXX")
  POOL_PIDS=()
  POOL_N=0
  POOL_TOLD=0
  for item in "$@"; do
    n=$((n + 1))
    POOL_N=$n
    while [ "$(pool_running)" -ge "$max" ]; do
      sleep "$poll"
      pool_tick
    done
    echo "$(date +%H:%M:%S)  started: $item"
    (
      LCI_WORKER=1
      trap on_interrupt INT TERM
      INTERRUPT_COUNT=0
      RESULTS="$POOL_DIR/$n.tsv"
      : > "$RESULTS"
      "$fn" "$item"
    ) > "$POOL_DIR/$n.out" 2>&1 < /dev/null &
    POOL_PIDS[$n]=$!
  done
  while [ "$(pool_running)" -gt 0 ]; do
    sleep "$poll"
    pool_tick
  done
  for ((i = 1; i <= n; i++)); do
    [ -f "$POOL_DIR/$i.tsv" ] && cat "$POOL_DIR/$i.tsv" >> "$RESULTS"
  done
  rm -rf "$POOL_DIR"
  POOL_PIDS=()
  POOL_N=0
  return 0
}

run_job title 5 step_title
run_job mr-size 5 step_mr_size
run_job mr-docs 5 step_mr_docs
run_job mr-translations 5 step_mr_translations
run_job mr-checks-tests 10 step_mr_checks_tests
run_job sonar-rules 5 step_sonar_rules
run_job spotless 10 step_spotless
run_job secret-detection 10 step_secret_detection
run_job lint-frontend 15 step_lint_frontend
run_job spanish-docs-lint 10 step_spanish_docs_lint

BUILD_OK=1
run_job build 30 step_build || BUILD_OK=0

# The test jobs need the build ("needs: Build project" in CI)
after_build() { # id: false when the build failed and the job would run
  if [ "$BUILD_OK" = 0 ] && [ "$(decision "$1")" = run ]; then
    skip_step "$1" "$(label_of "$1")" "Build project failed"
    return 1
  fi
  return 0
}

# One item of the unit-testing pool, "<kind>|<job id>|<profile>": All UT, the profiles of the embedded DB jobs and of the database
# jobs, and the rows that only report (a job that is not selected, no Docker). Each item runs in a worker (run_pool) and writes its rows to
# the results of the worker.
db_item() {
  local kind job profile row minutes oracle
  IFS='|' read -r kind job profile <<< "$1"
  case "$kind" in
    all-ut)
      after_build all-ut && run_locked no all-ut all-ut "$(label_of all-ut)" 40 step_all_ut ;;
    skipped)   # a job that is not selected: its row says why
      after_build "$job" && run_job "$job" 40 true ;;
    nodocker)
      after_build "$job" && record "$job" "$(label_of "$job")" FAIL yes 0 "Docker is not running: start it, or pass --no-db-containers (then this job is NOT verified)" ;;
    embedded)
      row="embedded-db-$profile"
      after_build embedded-db && run_locked no embedded-db "$row" "Embedded DB Tests: $profile" 40 step_unit_profile "$profile" "$row" ;;
    database)
      row="$job-$profile"
      minutes=40
      oracle=no
      case "$job" in db-oracle) minutes=60; oracle=yes ;; esac
      after_build "$job" && run_locked "$oracle" "$job" "$row" "$(label_of "$job"): $profile" "$minutes" step_unit_profile "$profile" "$row" ;;
  esac
}

# The items, in the order of the summary: All UT, the embedded DB profiles, then each database with its flyway profile
DOCKER_OK=0
docker info >/dev/null 2>&1 && DOCKER_OK=1
DB_ITEMS=("all-ut||")
if [ "$(decision embedded-db)" = run ]; then
  for profile in h2 h2-flyway hsql-flyway; do DB_ITEMS+=("embedded|embedded-db|$profile"); done
else
  DB_ITEMS+=("skipped|embedded-db|")
fi
for database in mysql postgresql sqlserver oracle; do
  id=db-$database
  if [ "$(decision "$id")" != run ]; then
    DB_ITEMS+=("skipped|$id|")
  elif [ "$DOCKER_OK" = 0 ]; then
    DB_ITEMS+=("nodocker|$id|")
  else
    DB_ITEMS+=("database|$id|$database" "database|$id|$database-flyway")
  fi
done
lci_prune_work "$ROOT/target/local-ci" "$LOG_DIR"   # clones of failed earlier runs were kept for the developer; those of runs that are over go
echo "Unit-testing jobs: ${#DB_ITEMS[@]} rows, $DB_PARALLEL at a time (LOCAL_CI_DB_PARALLEL)$([ "$DB_PARALLEL" -gt 1 ] && echo ", each in a clone of the checkout")"
run_pool "$DB_PARALLEL" db_item "${DB_ITEMS[@]}"
ALL_UT_OK=0
[ "$(awk -F '\t' '$1 == "all-ut" { status = $3 } END { print status }' "$RESULTS")" = PASS ] && ALL_UT_OK=1

run_job frontend-ut 20 step_frontend_ut awe-framework/awe-client-angular frontend-ut
run_job frontend-ut-react 20 step_frontend_ut awe-framework/awe-client-react frontend-ut-react
after_build javadoc && run_job javadoc 25 step_javadoc
run_job coverage 20 step_coverage

if [ "$(decision website)" = run ]; then
  if snapshot_website; then
    WEBSITE_TOUCHED=1
    run_job website 30 step_website
    restore_website
    WEBSITE_TOUCHED=0
  else
    record website "$(label_of website)" FAIL yes 0 "a 4.x docs swap is still in website/ ($WEBSITE_DOCS_BACKUP): see the note above"
  fi
else
  run_job website 30 true
fi

suite_selected() { # "$1" TEST_NAME of a suite: is it in --suites (or is there no --suites)?
  local word IFS=,
  [ -z "$SUITES" ] && return 0
  for word in $SUITES; do
    case "$1" in *"$word"*) return 0 ;; esac
  done
  return 1
}

# Browser suites: one application at a time, one row per CI job of the suite template of the browser
playwright_suites() { # <job id> <module> <browser name> <awe.test.browser>
  local id=$1 module=$2 browser=$3 test_browser=$4 tags classes ci_port name job selection
  in_list "$browser" "$BROWSERS" || return 0
  while IFS="$(printf '\t')" read -r tags classes ci_port name job; do
    [ -n "$name" ] && [ "$name" != - ] || continue
    suite_selected "$name" || continue
    PW_ROWS=$((PW_ROWS + 1))
    if [ "$classes" != - ]; then selection="-Dit.test=$classes"; else selection="-Dgroups=$tags"; fi
    if [ -n "$BROWSER_ERROR" ]; then
      record "$name" "$job" FAIL yes 0 "$BROWSER_ERROR"
      continue
    fi
    if ! lci_pick_ports; then
      record "$name" "$job" FAIL yes 0 "no free port for the application (tried 200 random ports)"
      continue
    fi
    STARTED_APP=1
    run_row "$id" "$name" "$job" 45 step_playwright "$module" "$selection" "$name" "$test_browser"
    stop_app
  done < <(node "$TOOL" suites "$CI_FILE" "$id" "$browser")
}

# The browser phase: each suite starts its application on free ports of its own, so no ports lock is needed. The machine limits
# how many browser applications run at once (LOCAL_CI_BROWSER_PARALLEL, a slot each: about 2 GB each): this run holds one slot from
# the first suite to the last and waits while the others are held. Its suites run one after another: they share the embedded
# databases of awe-boot (AWE_BOOT_DB_DIRS) and target/ (JaCoCo data, failsafe reports) of this checkout.
BROWSER_ERROR=
if { running playwright-angular || running playwright-react; } && [ "$BUILD_OK" = 1 ] && [ "$INTERRUPTED" = 0 ]; then
  if ! lci_browser_acquire "local-ci $(basename "$ROOT")"; then
    BROWSER_ERROR="no browser slot: all $(lci_browser_parallel) stayed held by other runs (LOCAL_CI_BROWSER_PARALLEL)"
    [ "$INTERRUPTED" = 1 ] && BROWSER_ERROR="interrupted while waiting for a browser slot"
  fi
fi

if after_build playwright-angular; then
  if [ "$(decision playwright-angular)" = run ]; then
    PW_ROWS=0
    playwright_suites playwright-angular awe-tests/awe-boot chromium headless-chrome
    playwright_suites playwright-angular awe-tests/awe-boot firefox headless-firefox
    [ "$PW_ROWS" -gt 0 ] || skip_step playwright-angular "$(label_of playwright-angular)" "no suite matches --browsers/--suites"
  else
    run_job playwright-angular 45 true
  fi
fi
if after_build playwright-react; then
  if [ "$(decision playwright-react)" = run ]; then
    if [ ! -f awe-framework/awe-client-react/dist/package.json ]; then
      run_step react-dist "React client dist (for awe-boot-react)" yes 15 step_react_dist
    fi
    PW_ROWS=0
    playwright_suites playwright-react awe-tests/awe-boot-react chromium headless-chrome
    playwright_suites playwright-react awe-tests/awe-boot-react firefox headless-firefox
    [ "$PW_ROWS" -gt 0 ] || skip_step playwright-react "$(label_of playwright-react)" "no suite matches --browsers/--suites"
  else
    run_job playwright-react 45 true
  fi
fi
lci_browser_release

cleanup
trap - EXIT
echo
node "$TOOL" summary "$RESULTS" | tee "$LOG_DIR/summary.txt"
rc=${PIPESTATUS[0]}
echo "Logs: ${LOG_DIR#"$ROOT"/}"
echo "Not reproduced locally: SonarCloud itself (only its coverage gate and four rules are approximated), secret detection \
without gitleaks, dependency scanning (gemnasium), DAST, container scanning, the Selenium suites (develop schedule, master, support/*) and the \
quarantine jobs."
[ "$INTERRUPTED" = 1 ] && exit 130
exit "$rc"
