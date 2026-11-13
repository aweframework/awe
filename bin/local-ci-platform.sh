# shellcheck shell=bash
# ============================================================
# local-ci-platform.sh
#
# The platform-dependent pieces of bin/local-ci.sh (sourced by it, and by bin/test-local-ci.sh): the operating system check
# (macOS, Linux, Windows through WSL2), the search for a JDK, the prerequisites, the listening ports and the system libraries
# of the Playwright browsers on Linux. Every probe of the machine goes through a variable a test can set, so the tests
# simulate another machine without changing this one:
#   LOCAL_CI_UNAME          output of "uname -s"
#   LOCAL_CI_PROC_VERSION   content of /proc/version (WSL writes "microsoft" in it)
#   LOCAL_CI_PORT_TOOLS     the port tools to consider, in order (default: those of lsof, ss and fuser that are installed)
#   LOCAL_CI_JDK_DIRS       the directories searched for JDKs (default: the usual ones of macOS and Linux)
#   (the Playwright packages are read from Playwright itself; tests stub dpkg-query in PATH)
# Bash 3.2 compatible (the bash of macOS).
# ============================================================

# The names of the website swap of the 4.x docs injection, shared with bin/website-maintenance-docs.sh
# shellcheck source=bin/website-swap-paths.sh
. "$(dirname "${BASH_SOURCE[0]:-$0}")/website-swap-paths.sh"

lci_uname() { printf '%s\n' "${LOCAL_CI_UNAME:-$(uname -s 2>/dev/null)}"; }

lci_proc_version() {
  if [ -n "${LOCAL_CI_PROC_VERSION+set}" ]; then
    printf '%s\n' "$LOCAL_CI_PROC_VERSION"
  else
    cat /proc/version 2>/dev/null
  fi
}

# mac, wsl, linux or windows (Git Bash, MSYS2, Cygwin: a native Windows shell, which this script does not support)
lci_os() {
  case "$(lci_uname)" in
    Darwin*) echo mac ;;
    MINGW*|MSYS*|CYGWIN*|*_NT*) echo windows ;;
    *)
      if lci_proc_version | grep -qiE 'microsoft|wsl'; then echo wsl; else echo linux; fi
      ;;
  esac
}

# Stops on a native Windows shell (exit 2); warns in WSL when the repository is on a Windows drive. "$1": the repository.
lci_platform_check() {
  local root=$1
  case "$(lci_os)" in
    windows)
      echo "local-ci: this is a native Windows shell ($(lci_uname)). Run it inside WSL2 (Ubuntu): Maven, the browsers," >&2
      echo "the ports and the process control of this script need Linux. See \"Windows (WSL2)\" in CONTRIBUTING.md." >&2
      return 2
      ;;
    wsl)
      case "$root" in
        /mnt/[a-zA-Z]/*|/mnt/[a-zA-Z])
          echo "local-ci: WARNING: the repository is on a Windows drive ($root). Builds there are many times slower in WSL2."
          echo "local-ci:          clone it inside the WSL home instead (e.g. ~/IdeaProjects/awe) and run it from there."
          ;;
      esac
      ;;
  esac
  return 0
}

# The JDK directories searched, in order (one per line)
lci_jdk_dirs() {
  if [ -n "${LOCAL_CI_JDK_DIRS:-}" ]; then
    printf '%s\n' $LOCAL_CI_JDK_DIRS
    return
  fi
  printf '%s\n' "$HOME/Library/Java/JavaVirtualMachines/*/Contents/Home" "/Library/Java/JavaVirtualMachines/*/Contents/Home" \
    "/usr/lib/jvm/*" "$HOME/.sdkman/candidates/java/*" "$HOME/.jdks/*" \
    "/opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home" "/usr/local/opt/openjdk*/libexec/openjdk.jdk/Contents/Home"
}

# Prints the home of an installed JDK of major version "$1" (LOCAL_CI_JDK<n> first, then java_home on macOS, then the dirs)
lci_find_jdk() {
  local major=$1 override pattern candidate version
  override=$(eval "printf '%s' \"\${LOCAL_CI_JDK$major:-}\"")
  {
    [ -n "$override" ] && printf '%s\n' "$override"
    if [ -z "${LOCAL_CI_JDK_DIRS:-}" ] && [ -x /usr/libexec/java_home ]; then
      /usr/libexec/java_home -v "$major" 2>/dev/null
    fi
    lci_jdk_dirs | while IFS= read -r pattern; do
      for candidate in $pattern; do printf '%s\n' "$candidate"; done
    done
  } | while IFS= read -r candidate; do
    [ -n "$candidate" ] && [ -f "$candidate/release" ] || continue
    version=$(sed -n 's/^JAVA_VERSION="\([0-9]*\).*/\1/p' "$candidate/release")
    if [ "$version" = "$major" ]; then
      printf '%s\n' "$candidate"
      break
    fi
  done
}

# The major Java version the build needs: <java.version> of awe-framework/awe-dependencies/pom.xml ("$1": the repository)
lci_required_java() {
  sed -n 's:.*<java.version>\([0-9][0-9]*\)</java.version>.*:\1:p' "$1/awe-framework/awe-dependencies/pom.xml" 2>/dev/null | head -1
}

# The explanation printed when no JDK of the version "$1" is found: where it looked and how to install one
lci_jdk_help() {
  local major=$1
  echo "No JDK $major found. Looked in: LOCAL_CI_JDK$major, $( [ -x /usr/libexec/java_home ] && echo '/usr/libexec/java_home, ')$(lci_jdk_dirs | tr '\n' ' ')"
  case "$(lci_os)" in
    mac) echo "Install one (e.g. Temurin $major or a JetBrains JDK) or set LOCAL_CI_JDK$major to its home." ;;
    *) echo "Install one (Ubuntu/WSL: sudo apt install openjdk-$major-jdk) or set LOCAL_CI_JDK$major to its home." ;;
  esac
}

# Prints the missing commands among "$@" with the way to install them, and returns 1 when one is missing
lci_check_commands() {
  local missing=0 tool hint
  for tool in "$@"; do
    command -v "$tool" >/dev/null 2>&1 && continue
    missing=1
    case "$tool:$(lci_os)" in
      mvn:mac) hint="brew install maven" ;;
      mvn:*) hint="sudo apt install maven" ;;
      node:*|npm:*) hint="install Node.js 22.12 or later (nvm, NodeSource or brew install node)" ;;
      git:mac) hint="xcode-select --install" ;;
      git:*) hint="sudo apt install git" ;;
      docker:wsl) hint="Docker Desktop with the WSL integration of this distribution turned on" ;;
      docker:*) hint="Docker Desktop, Colima or the docker engine" ;;
      *) hint="" ;;
    esac
    echo "local-ci: '$tool' is not installed${hint:+: $hint}." >&2
  done
  return "$missing"
}

# The port tools available, in order of preference: lsof (macOS and most Linux), ss (iproute2, in every Ubuntu and WSL),
# fuser (psmisc)
lci_port_tools() {
  if [ -n "${LOCAL_CI_PORT_TOOLS+set}" ]; then
    printf '%s\n' "$LOCAL_CI_PORT_TOOLS"
    return
  fi
  local tool found=
  for tool in lsof ss fuser; do
    command -v "$tool" >/dev/null 2>&1 || continue
    # the lsof of BusyBox lists every open file and ignores -i: it is no port tool (its fuser takes "port/tcp" and does work)
    [ "$tool" = lsof ] && case "$(readlink "$(command -v lsof)" 2>/dev/null)" in *busybox*) continue ;; esac
    found="$found $tool"
  done
  printf '%s\n' "${found# }"
}

# Prints the pids listening on TCP port "$1" ("?" when something listens but its pid cannot be read, e.g. a process of
# another user seen by ss). Returns 0 when it could tell, 3 when no port tool is installed.
lci_port_pids() {
  local port=$1 tool out
  tool=$(lci_port_tools | awk '{ print $1 }')
  case "$tool" in
    lsof)
      lsof -nP -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null
      return 0
      ;;
    ss)
      out=$(ss -ltnpH "sport = :$port" 2>/dev/null)
      [ -n "$out" ] || return 0
      if printf '%s\n' "$out" | grep -q 'pid='; then
        printf '%s\n' "$out" | grep -o 'pid=[0-9]*' | cut -d= -f2 | sort -u
      else
        echo '?'
      fi
      return 0
      ;;
    fuser)
      # fuser prints "8090/tcp:" on stderr and the pids on stdout
      fuser "$port/tcp" 2>/dev/null | tr -s ' \t' '\n' | grep -E '^[0-9]+$'
      return 0
      ;;
  esac
  return 3
}

# Why port "$1" cannot be used, or nothing (exit 1) when it is free
lci_port_busy() {
  local port=$1 pids rc
  pids=$(lci_port_pids "$port")
  rc=$?
  if [ "$rc" -eq 3 ]; then
    echo "cannot check port $port: install lsof or iproute2 (ss), e.g. sudo apt install lsof"
    return 0
  fi
  if [ -n "$pids" ]; then
    echo "port $port is in use (pid $(printf '%s' "$pids" | tr '\n' ' ' | sed 's/ $//')): stop that process first"
    return 0
  fi
  return 1
}

# Drops variables of the environment that would change the steps. SERVER_PORT (often exported for the browser suites of
# awe-boot) moves every Spring test context of Unit · Java and the database jobs to that port, and their REST and microservice
# tests then fail; the browser steps of this script set it themselves, for their own application only.
lci_clean_env() {
  if [ -n "${SERVER_PORT+set}" ]; then
    echo "local-ci: SERVER_PORT=$SERVER_PORT is ignored: the browser suites start their application on a free port of their own."
    unset SERVER_PORT
  fi
  return 0
}

# Sums the Maven test summaries of a log: "Tests run: N, Failures: F, Errors: E, Skipped: S" with an optional ", Flakes: K"
# (failsafe adds it when a rerun passed), leaving out the per-class lines ("Time elapsed")
lci_test_counts() {
  awk '/Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+(, Flakes: [0-9]+)?[[:space:]]*$/ && !/Time elapsed/ {
      line = $0; sub(/.*Tests run: /, "", line); split(line, f, /[^0-9]+/)
      run += f[1]; fail += f[2] + f[3]; skip += f[4]; flaky += f[5]; found = 1 }
    END { if (found) { printf "tests %d, failed %d, skipped %d", run, fail, skip; if (flaky) printf ", flaky %d", flaky } }' "$1"
}

# The status of a step: lci_status <exit code> <warning exit codes: "64", "64 65", "any" or ""> <timed out 0|1> <interrupted 0|1>
# Exit 64 (or another listed code) is a warning only for a job whose CI allow_failure lists it, as GitLab does
lci_status() {
  local rc=$1 warn=$2
  if [ "$3" = 1 ] || [ "$4" = 1 ]; then echo FAIL; return; fi
  if [ "$rc" -eq 0 ]; then echo PASS; return; fi
  case " $warn " in
    *" any "*|*" $rc "*) echo WARN ;;
    *) echo FAIL ;;
  esac
}

# lci_fresh <marker> <file>...: the files that exist and are newer than the marker (the reports written by this run)
lci_fresh() {
  local marker=$1 file
  shift
  for file in "$@"; do
    [ -f "$file" ] && [ "$file" -nt "$marker" ] && printf '%s\n' "$file"
  done
  return 0
}

# ------------------------------------------------------------
# Website: undo exactly what bin/website-maintenance-docs.sh writes, and nothing else
# ------------------------------------------------------------

# The paths the maintenance docs injection writes for label "$1" (run from the repository root), one per line: versions.json,
# the versioned docs and sidebars of the label, and per locale the copied docs and the labels file of the version. website/docs
# and website/sidebars.js are swapped and put back by the injection itself (see restore_website for an interrupted swap).
lci_website_injection_paths() {
  local label=$1 locale
  printf '%s\n' website/versions.json "website/versioned_docs/version-$label" \
    "website/versioned_sidebars/version-$label-sidebars.json"
  for locale in website/i18n/*/; do
    [ -d "$locale" ] || continue
    printf '%s\n' "${locale}docusaurus-plugin-content-docs/version-$label" \
      "${locale}docusaurus-plugin-content-docs/version-$label.json"
  done
}

# The swap of the 4.x docs injection (names in bin/website-swap-paths.sh): develop's website/docs and website/sidebars.js wait
# in backups while website/docs holds the 4.x pages, and the injection swaps them back in its exit trap. A hard kill (SIGKILL,
# a closed terminal, a WSL shutdown) skips every trap and leaves the swap behind. bin/website-maintenance-docs.sh writes the
# marker (WEBSITE_SWAP_MARKER) with its own pid and start time while the swap lasts, so a later run can tell an orphan swap
# (that injection is dead) from a live one, even when the injection outlives the local-ci run that started it.

# Is the process of an owner file ("$1": pid on line 1, start time on line 2) alive? The start time tells it from a reused pid.
lci_owner_alive() {
  local pid start
  pid=$(sed -n 1p "$1" 2>/dev/null)
  start=$(sed -n 2p "$1" 2>/dev/null)
  [ -n "$pid" ] && [ -n "$start" ] && [ "$(website_proc_start "$pid")" = "$start" ]  # ps, not kill -0: kill -0 fails across sandboxes
}

# The age in seconds of a file or directory (its modification time)
lci_age() {
  local mtime
  mtime=$(stat -c %Y "$1" 2>/dev/null || stat -f %m "$1" 2>/dev/null) || return 1
  echo $(( $(date +%s) - mtime ))
}

# ------------------------------------------------------------
# One local-ci run per checkout: a lock directory in the git dir of the worktree (each worktree has its own), holding the pid
# and start time of its run in an "owner" file. Of the runs that find the lock free, mkdir lets exactly one win. A lock is
# taken over only when its owner is dead, or when it still has no owner after LOCAL_CI_LOCK_GRACE seconds (60: a run killed
# between its mkdir and its owner file); the stale lock is first moved to a unique name, so of two takers only one mkdir wins.
# ------------------------------------------------------------
lci_lock_path() { printf '%s/local-ci.lock\n' "$(git rev-parse --absolute-git-dir 2>/dev/null || echo .git)"; }

lci_lock_held() { # message for a lock held by a live (or just starting) run; exit 2
  echo "local-ci: another local-ci run${1:+ (pid $1)} is running in this checkout: wait for it, or stop it." >&2
  return 2
}

# lci_lock_acquire <lock dir>: 0 when this run holds it, 2 (with a message) when another run does
lci_lock_acquire() {
  local lock=$1 seen stale age
  if mkdir "$lock" 2>/dev/null; then
    lci_lock_write_owner "$lock"
    return 0
  fi
  seen=$(cat "$lock/owner" 2>/dev/null)
  if [ -z "$seen" ]; then
    # No owner yet: another run is between its mkdir and its owner file, unless the lock is old
    age=$(lci_age "$lock") || age=0
    [ "$age" -lt "${LOCAL_CI_LOCK_GRACE:-60}" ] && { lci_lock_held; return 2; }
  elif lci_owner_alive "$lock/owner"; then
    lci_lock_held "$(sed -n 1p "$lock/owner")"
    return 2
  fi
  # Stale: move it aside under a unique name (atomic), and check that what moved is the lock judged stale; a lock that a
  # faster taker made meanwhile goes back
  stale="$lock.stale.$$"
  mv "$lock" "$stale" 2>/dev/null || { lci_lock_held; return 2; }
  if [ "$(cat "$stale/owner" 2>/dev/null)" != "$seen" ]; then
    mv "$stale" "$lock" 2>/dev/null || rm -rf "$stale"
    lci_lock_held
    return 2
  fi
  rm -rf "$stale"
  if ! mkdir "$lock" 2>/dev/null; then
    lci_lock_held
    return 2
  fi
  lci_lock_write_owner "$lock"
  echo "local-ci: taking over the lock of a local-ci run that is no longer running${seen:+ (pid $(printf '%s\n' "$seen" | sed -n 1p))}."
  return 0
}

# The owner file is written whole under a temporary name and renamed, so a reader never sees half of it
lci_lock_write_owner() {
  printf '%s\n%s\n' "$$" "$(website_proc_start $$)" > "$1/owner.$$"
  mv "$1/owner.$$" "$1/owner"
}

# ------------------------------------------------------------
# Machine-wide locks (one per machine, not per checkout). The test applications need none for their ports: bin/local-ci.sh picks
# free ports per run (lci_pick_ports), and the awe-boot Spring test context listens on a random one. Two things are limited:
# the Oracle container (heavy, slow to start: one at a time) and the browser applications (a slot each, LOCAL_CI_BROWSER_PARALLEL
# at a time). Owner file on four lines: owner, pid, epoch, start time (ps -o lstart=), the format of the shared lock of the
# agents. Same rules as the checkout lock: mkdir is atomic; a holder is alive when ps shows its pid with the same start time
# (ps, not kill -0, which fails across sandboxes); a live holder is never taken over, whatever its age; a dead one is moved aside
# under a unique name, then the lock is taken again by mkdir; only the holder releases.
# ------------------------------------------------------------

# Is the holder of an owner file ("$1") alive? (pid on line 2, start time on line 4)
lci_holder_alive() {
  local pid start
  pid=$(sed -n 2p "$1" 2>/dev/null)
  start=$(sed -n 4p "$1" 2>/dev/null)
  [ -n "$pid" ] && [ -n "$start" ] && [ "$(website_proc_start "$pid")" = "$start" ]
}

# lci_mlock_take <lock dir> <owner name> <what>: the machine-wide lock "$1".
# 0 when this run holds it, 1 after LOCAL_CI_PORTS_WAIT seconds (7200) of a live holder or on Ctrl-C. Waits every
# LOCAL_CI_PORTS_POLL seconds (10). Both variables set the wait of every machine-wide lock (the names are historical). It says
# so now and then ("waiting for <what>"). With LCI_MLOCK_TRY=1 it never waits: 1 at once when a live holder has the lock.
lci_mlock_take() {
  local lock=$1 owner=$2 what=$3 seen age stale waited=0 poll=${LOCAL_CI_PORTS_POLL:-10} limit=${LOCAL_CI_PORTS_WAIT:-7200}
  while :; do
    if mkdir "$lock" 2>/dev/null; then
      printf '%s\n%s\n%s\n%s\n' "$owner" "$$" "$(date +%s)" "$(website_proc_start $$)" > "$lock/owner.$$" \
        && mv "$lock/owner.$$" "$lock/owner"
      return 0
    fi
    seen=$(cat "$lock/owner" 2>/dev/null)
    stale=0
    if [ -z "$seen" ]; then
      age=$(lci_age "$lock") || age=0
      [ "$age" -ge 60 ] && stale=1          # no owner after 60 s: a holder killed between its mkdir and its write
    elif ! lci_holder_alive "$lock/owner"; then
      stale=1
    fi
    if [ "$stale" = 1 ] && mv "$lock" "$lock.stale.$$" 2>/dev/null; then
      if [ "$(cat "$lock.stale.$$/owner" 2>/dev/null)" = "$seen" ]; then
        echo "local-ci: the lock of $what, of a dead holder ($(printf '%s\n' "$seen" | sed -n 1p) pid $(printf '%s\n' "$seen" | sed -n 2p)), is taken over."
        rm -rf "$lock.stale.$$"
      else
        mv "$lock.stale.$$" "$lock" 2>/dev/null || rm -rf "$lock.stale.$$"   # a newer holder's lock: give it back
      fi
      continue
    fi
    [ "${LCI_MLOCK_TRY:-0}" = 1 ] && return 1
    [ "${INTERRUPTED:-0}" = 1 ] && return 1   # Ctrl-C while waiting (bin/local-ci.sh)
    if [ "$waited" -ge "$limit" ]; then
      echo "local-ci: $what is still held after ${limit}s by $(printf '%s' "$seen" | tr '\n' ' ')($lock)." >&2
      return 1
    fi
    if [ $(( waited % (poll * 30) )) -eq 0 ]; then
      echo "local-ci: waiting for $what, held by $(printf '%s\n' "$seen" | sed -n 1p) (pid $(printf '%s\n' "$seen" | sed -n 2p)); lock $lock"
    fi
    sleep "$poll"
    waited=$((waited + poll))
  done
}

# The Oracle container is heavy (memory, minutes to start) and the Oracle jobs of runs on different checkouts must not start
# one each at the same time: a machine-wide lock of its own. Inside one run the Oracle rows also queue on it.
lci_oracle_lock_path() { printf '%s\n' "${LOCAL_CI_ORACLE_LOCK:-${TMPDIR:-/tmp}/awe-local-ci-oracle.lock}" | sed 's://*:/:g'; }
# Only the worker (subshell) that took the lock releases it: ORACLE_HELD is set by lci_with_oracle_lock in that subshell only, while
# the owner pid in the file is the one of the main shell ($$ is the same in every subshell, and BASHPID needs bash 4). "force"
# (the cleanup of the run, when no worker is left) releases a lock of this run whoever took it.
lci_oracle_release() {
  local lock
  lock=$(lci_oracle_lock_path)
  if [ "${ORACLE_HELD:-0}" != 1 ]; then
    # force is for the top-level cleanup only: a pool worker (LCI_WORKER=1) never releases a lock another worker may hold
    [ "${1:-}" = force ] && [ "${LCI_WORKER:-0}" != 1 ] || return 0
  fi
  [ "$(sed -n 2p "$lock/owner" 2>/dev/null)" = "$$" ] && rm -rf "$lock"
  ORACLE_HELD=0
  return 0
}

# The exit code of lci_with_oracle_lock when the lock could not be taken (the command did not run); LOCK_ERROR says why
LCI_LOCK_FAILED=99
# Set to the reason once the wait for the Oracle lock timed out: the later Oracle jobs of the run fail fast with it instead of
# waiting again (the wait limit is per run, not per job). The Oracle rows run in different worker subshells, so the reason is
# also written to LCI_ORACLE_GIVEN_UP_FILE (a file of the run, set by bin/local-ci.sh) and read from it.
LCI_ORACLE_GIVEN_UP=
LCI_ORACLE_GIVEN_UP_FILE=

# lci_with_oracle_lock <owner> <command...>: runs the command (an Oracle container job) holding the Oracle lock. Waits while
# another run holds it (LOCAL_CI_PORTS_WAIT and LOCAL_CI_PORTS_POLL), never takes a live holder's, releases on return.
# LCI_LOCK_FAILED when the lock could not be taken: LOCK_ERROR says why ("interrupted" after Ctrl-C).
lci_with_oracle_lock() {
  local owner=$1 rc
  shift
  if [ -z "$LCI_ORACLE_GIVEN_UP" ] && [ -n "$LCI_ORACLE_GIVEN_UP_FILE" ] && [ -s "$LCI_ORACLE_GIVEN_UP_FILE" ]; then
    LCI_ORACLE_GIVEN_UP=$(cat "$LCI_ORACLE_GIVEN_UP_FILE")
  fi
  if [ -n "$LCI_ORACLE_GIVEN_UP" ]; then
    LOCK_ERROR=$LCI_ORACLE_GIVEN_UP
    return "$LCI_LOCK_FAILED"
  fi
  if ! lci_mlock_take "$(lci_oracle_lock_path)" "$owner" "the Oracle lock (one container at a time)"; then
    lci_lock_failed "the Oracle lock ($(lci_oracle_lock_path))"
    return "$LCI_LOCK_FAILED"
  fi
  ORACLE_HELD=1
  "$@"
  rc=$?
  lci_oracle_release
  return "$rc"
}

# lci_lock_failed <what>: sets LOCK_ERROR; a timeout (not Ctrl-C) also makes the later Oracle jobs fail fast
lci_lock_failed() {
  if [ "${INTERRUPTED:-0}" = 1 ]; then
    LOCK_ERROR="interrupted while waiting for $1"
    return 0
  fi
  LOCK_ERROR="$1 stayed held by another run"
  LCI_ORACLE_GIVEN_UP=$LOCK_ERROR
  [ -n "$LCI_ORACLE_GIVEN_UP_FILE" ] && printf '%s\n' "$LOCK_ERROR" > "$LCI_ORACLE_GIVEN_UP_FILE"
}

# Browser capacity. Every browser suite starts an application (JVM) and a browser: about 1.5-2 GB. At most
# LOCAL_CI_BROWSER_PARALLEL of them at a time on the machine (default 2), across runs of any checkout: a run holds one of that
# many slot locks for its whole browser phase (its suites run one after another).
lci_browser_parallel() {
  local n=${LOCAL_CI_BROWSER_PARALLEL:-2}
  case "$n" in ''|*[!0-9]*) n=2 ;; esac
  [ "$n" -ge 1 ] || n=2
  printf '%s\n' "$n"
}
lci_browser_lock_path() { printf '%s.%s.lock\n' "${LOCAL_CI_BROWSER_LOCK:-${TMPDIR:-/tmp}/awe-local-ci-browser}" "$1" | sed 's://*:/:g'; }

# lci_browser_acquire <owner name>: 0 when this run holds a slot (BROWSER_SLOT is its lock dir), 1 after LOCAL_CI_PORTS_WAIT
# seconds with every slot held by a live run, or on Ctrl-C
lci_browser_acquire() {
  local owner=$1 n capacity waited=0 poll=${LOCAL_CI_PORTS_POLL:-10} limit=${LOCAL_CI_PORTS_WAIT:-7200} holder said=0
  capacity=$(lci_browser_parallel)
  BROWSER_SLOT=
  while :; do
    n=1
    while [ "$n" -le "$capacity" ]; do
      if LCI_MLOCK_TRY=1 lci_mlock_take "$(lci_browser_lock_path "$n")" "$owner" "a browser slot" >/dev/null; then
        BROWSER_SLOT=$(lci_browser_lock_path "$n")
        return 0
      fi
      n=$((n + 1))
    done
    [ "${INTERRUPTED:-0}" = 1 ] && return 1
    if [ "$waited" -ge "$limit" ]; then
      echo "local-ci: every browser slot ($capacity) is still held after ${limit}s." >&2
      return 1
    fi
    if [ "$said" = 0 ] || [ $(( waited % (poll * 30) )) -eq 0 ]; then
      holder=$(sed -n 1p "$(lci_browser_lock_path 1)/owner" 2>/dev/null)
      echo "local-ci: waiting for a browser slot (all $capacity held, e.g. by ${holder:-another run}); LOCAL_CI_BROWSER_PARALLEL sets how many"
      said=1
    fi
    sleep "$poll"
    waited=$((waited + poll))
  done
}

# lci_browser_release: removes the slot lock only when this run took it
lci_browser_release() {
  [ -n "${BROWSER_SLOT:-}" ] || return 0
  [ "$(sed -n 2p "$BROWSER_SLOT/owner" 2>/dev/null)" = "$$" ] && rm -rf "$BROWSER_SLOT"
  BROWSER_SLOT=
  return 0
}

# lci_locks_release [force]: the machine-wide locks. Without "force" each is released only by the shell that took it. The cleanup of
# the run (exit, signals) passes "force" once no worker is left; a pool worker (LCI_WORKER=1) never forces.
lci_locks_release() {
  lci_browser_release
  lci_oracle_release "${1:-}"
  return 0
}

# ------------------------------------------------------------
# Free ports per run. The application of a browser suite, its JMX port (spring-boot:start) and the JaCoCo port of the suite are
# picked free at each suite, so runs of different checkouts never collide. Never 8080 (the developer's application), never a
# port in use, a different one for each role. A port can be taken by someone else between the check and its use: the suite
# then fails loudly, and a rerun picks others.
# ------------------------------------------------------------
lci_random_port() { printf '%s\n' $((20000 + (RANDOM * 32768 + RANDOM) % 10000)); }   # 20000-29999: below the ephemeral ranges

# Is nothing listening on port "$1"? Without a tool to ask (lsof, ss, fuser) the port is assumed free
lci_port_free() {
  local pids rc
  pids=$(lci_port_pids "$1")
  rc=$?
  [ "$rc" -eq 3 ] && return 0
  [ -z "$pids" ]
}

# lci_pick_port [avoid ports...]: prints a free port, 1 when none was found after 200 draws
lci_pick_port() {
  local avoid=" $* " port i=0
  while [ "$i" -lt 200 ]; do
    i=$((i + 1))
    port=$(lci_random_port)
    [ "$port" = 8080 ] && continue
    case "$avoid" in *" $port "*) continue ;; esac
    lci_port_free "$port" || continue
    printf '%s\n' "$port"
    return 0
  done
  return 1
}

# lci_pick_ports: sets APP_PORT, JMX_PORT and JACOCO_PORT to three different free ports
lci_pick_ports() {
  APP_PORT=$(lci_pick_port) || return 1
  JMX_PORT=$(lci_pick_port "$APP_PORT") || return 1
  JACOCO_PORT=$(lci_pick_port "$APP_PORT" "$JMX_PORT") || return 1
}

# lci_lock_release <lock dir>: only the run that holds it removes it
lci_lock_release() {
  [ "$(sed -n 1p "$1/owner" 2>/dev/null)" = "$$" ] && rm -rf "$1"
  return 0
}

# The untracked files of the change set: everything git does not ignore, except the orphan folders of a healed website swap
# (website/.local-ci-orphan-*: kept for the developer and visible in git status, but no change of the merge request) and
# the private Maven repository of the checkout (.m2/repository)
lci_untracked() {
  git -c core.quotepath=off ls-files --others --exclude-standard -- . ':(exclude,glob)website/.local-ci-orphan-*/**' \
    ':(exclude,glob).m2/repository/**'
}

# ------------------------------------------------------------
# The Maven repository of the checkout. Every worktree installs the same AWE snapshot version, so with the shared ~/.m2 the
# test applications started from a module pom (the browser suites) would run the modules another worktree installed last.
# As CI does (MAVEN_OPTS -Dmaven.repo.local=.m2/repository), each checkout has its own repository: .m2/repository at its
# root, or LOCAL_CI_MAVEN_REPO. It is seeded once from ~/.m2/repository with the third-party artifacts only (cloned
# copy-on-write on APFS), never with the AWE modules, which this checkout's own build installs.
# ------------------------------------------------------------
lci_maven_repo() { printf '%s\n' "${LOCAL_CI_MAVEN_REPO:-$1/.m2/repository}"; }

# Copies "$1" (a source tree) to "$2", cloning when the file system can (APFS) and copying otherwise
lci_clone_tree() {
  mkdir -p "$(dirname "$2")"
  cp -cR "$1" "$2" 2>/dev/null || cp -R "$1" "$2"
}

# lci_seed_maven_repo <user repository> <private repository>: only when the private one does not exist yet
lci_seed_maven_repo() {
  local src=$1 dst=$2 entry sub leaf
  [ -d "$dst" ] && return 0
  mkdir -p "$dst"
  [ -d "$src" ] || return 0
  echo "local-ci: seeding the Maven repository of this checkout ($dst) with the third-party artifacts of $src (once)"
  for entry in "$src"/*; do
    [ -e "$entry" ] || continue
    if [ "$(basename "$entry")" != com ]; then
      lci_clone_tree "$entry" "$dst/$(basename "$entry")"
      continue
    fi
    for sub in "$entry"/*; do
      if [ "$(basename "$sub")" != almis ]; then
        lci_clone_tree "$sub" "$dst/com/$(basename "$sub")"
        continue
      fi
      for leaf in "$sub"/*; do
        [ "$(basename "$leaf")" = awe ] && continue   # the AWE modules: built by this checkout, never another one's
        [ -e "$leaf" ] && lci_clone_tree "$leaf" "$dst/com/almis/$(basename "$leaf")"
      done
    done
  done
  return 0
}

# lci_swap_pending <root>: is a swap backup there (exit 0)?
lci_swap_pending() {
  [ -e "$1/$WEBSITE_DOCS_BACKUP" ] || [ -e "$1/$WEBSITE_SIDEBARS_BACKUP" ]
}

# lci_heal_website_swap <root> [own]: puts back the swap of a killed injection, only when the injection named in its marker is
# dead ("own": this run's own swap, after waiting for its injection to end). A live injection, or a backup without a marker
# (an injection older than the marker), is never touched: a note says why and how to restore it by hand. Nothing is deleted:
# website/docs and website/sidebars.js as found (the 4.x pages, and any edit made after the kill) are moved to
# website/.local-ci-orphan-<timestamp>/ first.
lci_heal_website_swap() {
  local root=$1 own=${2:-} marker orphan
  marker="$root/$WEBSITE_SWAP_MARKER"
  if ! lci_swap_pending "$root"; then
    # A marker without backups is stale (the swap was put back); remove it only when its process is gone
    lci_owner_alive "$marker" || rm -f "$marker"
    return 0
  fi
  if [ "$own" != own ]; then
    if [ ! -f "$marker" ]; then
      echo "local-ci: $WEBSITE_DOCS_BACKUP or $WEBSITE_SIDEBARS_BACKUP exists, but no injection marked it (an older run?)."
      echo "local-ci: Nothing is touched. It may hold your docs: check it, then restore by hand from $root:"
      echo "local-ci:   mv website/docs website/docs.4x && mv $WEBSITE_DOCS_BACKUP website/docs"
      echo "local-ci:   mv website/sidebars.js website/sidebars.4x.js && mv $WEBSITE_SIDEBARS_BACKUP website/sidebars.js"
      return 0
    fi
    # The marker names the injection process that swapped (not its parent run): while it lives, the swap is its own
    if lci_owner_alive "$marker"; then
      echo "local-ci: the website swap belongs to an injection (pid $(sed -n 1p "$marker")) that is still running: nothing is touched."
      return 0
    fi
  else
    # This run's own swap: its injection restores it in its exit trap, so wait for it to end (LOCAL_CI_OWN_WAIT seconds, 10)
    # and never put the backups back while it lives
    local waited=0
    while lci_owner_alive "$marker" && [ "$waited" -lt "${LOCAL_CI_OWN_WAIT:-10}" ]; do
      sleep 1
      waited=$((waited + 1))
    done
    if lci_owner_alive "$marker"; then
      echo "local-ci: the 4.x docs injection (pid $(sed -n 1p "$marker")) is still running: its swap is left to it; nothing is touched."
      return 0
    fi
    lci_swap_pending "$root" || { rm -f "$marker"; return 0; }
  fi
  orphan="$root/website/.local-ci-orphan-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$orphan"
  if [ -d "$root/$WEBSITE_DOCS_BACKUP" ]; then
    [ -e "$root/website/docs" ] && mv "$root/website/docs" "$orphan/docs"
    mv "$root/$WEBSITE_DOCS_BACKUP" "$root/website/docs"
  fi
  if [ -f "$root/$WEBSITE_SIDEBARS_BACKUP" ]; then
    [ -e "$root/website/sidebars.js" ] && mv "$root/website/sidebars.js" "$orphan/sidebars.js"
    mv "$root/$WEBSITE_SIDEBARS_BACKUP" "$root/website/sidebars.js"
  fi
  rm -f "$marker"
  echo "local-ci: put back website/docs and website/sidebars.js from the swap of an interrupted 4.x docs injection; what was"
  echo "local-ci: there (the 4.x pages, and any edit made since) is kept in ${orphan#"$root"/}"
  return 0
}

# lci_snapshot <dir> <path>...: before the injection, keeps a copy of each path that exists and remembers the ones that do not
lci_snapshot() {
  local dir=$1 path
  shift
  rm -rf "$dir"
  mkdir -p "$dir/copy"
  : > "$dir/paths"
  for path in "$@"; do
    printf '%s\n' "$path" >> "$dir/paths"
    if [ -e "$path" ]; then
      mkdir -p "$dir/copy/$(dirname "$path")"
      cp -Rp "$path" "$dir/copy/$path"
    fi
  done
}

# lci_restore <dir>: puts each snapshot path back as it was: its copy when its content changed, removed when it did not exist
# before (the injection created it). Any other path is left alone, whatever happened to it meanwhile.
lci_restore() {
  local dir=$1 path
  [ -f "$dir/paths" ] || return 0
  while IFS= read -r path; do
    [ -n "$path" ] || continue
    if [ -e "$dir/copy/$path" ]; then
      if ! diff -rq "$dir/copy/$path" "$path" >/dev/null 2>&1; then
        rm -rf "$path"
        mkdir -p "$(dirname "$path")"
        cp -Rp "$dir/copy/$path" "$path"
        echo "Restored $path (rewritten by the 4.x docs injection)"
      fi
    elif [ -e "$path" ]; then
      rm -rf "$path"
      echo "Removed $path (created by the 4.x docs injection)"
    fi
  done < "$dir/paths"
}


# ------------------------------------------------------------
# Playwright browser libraries (Linux, WSL). CI installs them as root with --with-deps. Here the check only advises: the
# packages come from Playwright itself ("install-deps --dry-run" of the pinned version, for the selected browsers), and a
# step fails only when a browser really cannot start; its summary then names the command that installs them.
# ------------------------------------------------------------

# The Debian packages among "$@" that are not installed (one per line). Returns 3 without dpkg-query (not Debian/Ubuntu).
lci_missing_packages() {
  local package status
  command -v dpkg-query >/dev/null 2>&1 || return 3
  for package in "$@"; do
    status=$(dpkg-query -W -f='${db:Status-Status}' "$package" 2>/dev/null)
    [ "$status" = installed ] || printf '%s\n' "$package"
  done
  return 0
}

# Does the log "$1" show a browser that could not start for missing system libraries?
lci_browser_libs_failure() {
  [ -f "$1" ] && grep -qE 'Host system is missing dependencies|error while loading shared libraries|missing dependencies to run browsers' "$1"
}

# The one-off command that installs the libraries, with Playwright's own CLI of the pinned version (as CI does):
# "$1" java, "$2" classpath of the playwright, driver and driver-bundle jars, "$3" browsers ("chromium firefox")
lci_playwright_deps_command() {
  echo "sudo \"$1\" -cp \"$2\" com.microsoft.playwright.CLI install-deps $3"
}

# ------------------------------------------------------------
# Clone of the checkout for a job that runs in parallel with others (see in_work in bin/local-ci.sh): the sources and the build
# output (target/ of the modules, so Maven finds the classes up to date), without .git, node_modules, the generated website/build and website/.docusaurus (the rest of website/ stays: tests read files
# of it, e.g. website/scripts/rest-reference/data/openapi.json), the logs of
# this run and the private Maven repository (shared, read only: the AWE modules of the build step are in it). rsync keeps the
# modification times, which Maven's incremental build reads.
# ------------------------------------------------------------
lci_clone_work() {
  mkdir -p "$2" || return 1
  rsync -a --exclude=.git --exclude=node_modules --exclude=/website/build --exclude=/website/.docusaurus --exclude=/target/local-ci --exclude=/.m2/repository \
    --exclude=/.claude "$1"/ "$2"/
}

# lci_bring_back <clone> <checkout> <file name>: the files of that name under the target/ directories of the clone go to the same
# place in the checkout (the JaCoCo execution data that the coverage step reads)
lci_bring_back() {
  rsync -a --prune-empty-dirs --include='*/' --include="$3" --exclude='*' "$1"/ "$2"/
}

# ------------------------------------------------------------
# Ownership of a process: the run stops only what it started. lci_is_ours <pid> <checkout> <ancestor pid>: 0 when the pid is a
# descendant of the ancestor (the script) or its command line names the checkout (a test application that Maven left orphaned:
# its classpath is under the checkout). A process that grabbed the picked port in the window between the pick and the start is
# neither, and is never stopped.
# ------------------------------------------------------------
lci_is_ours() {
  local pid=$1 root=${2%/} ancestor=$3 cur depth=0 cmd rest before after prev
  cur=$pid
  while [ -n "$cur" ] && [ "$cur" -gt 1 ] 2>/dev/null && [ "$depth" -lt 64 ]; do
    [ "$cur" = "$ancestor" ] && return 0
    cur=$(lci_ppid "$cur")
    depth=$((depth + 1))
  done
  # The checkout as a whole path component: "<root>/" not preceded by a path character (so /elsewhere<root> is no match), and
  # not followed by .claude/worktrees/ (a worktree nested under the checkout is another checkout). "<root>-2" has no "<root>/".
  cmd=$(lci_cmdline "$pid")
  rest=$cmd
  while [ -n "$root" ] && [[ "$rest" == *"$root/"* ]]; do
    before=${rest%%"$root/"*}
    after=${rest#*"$root/"}
    prev=${before: -1}
    case "$prev" in
      ''|' '|:|=|@)
        case "$after" in .claude/worktrees/*) ;; *) return 0 ;; esac ;;
    esac
    rest=$after
  done
  return 1
}

# The process table without depending on the flavour of ps (macOS, procps-ng, BusyBox: the last has neither -p nor -ax nor a
# "command" column): /proc where it exists, ps otherwise.
lci_ppid() { # the parent pid of "$1", or nothing
  if [ -r "/proc/$1/stat" ]; then
    sed 's/.*) //' "/proc/$1/stat" 2>/dev/null | awk '{ print $2 }'
    return 0
  fi
  ps -o ppid= -p "$1" 2>/dev/null | tr -d ' '
}

lci_cmdline() { # the command line of "$1" with its arguments separated by spaces, or nothing
  if [ -r "/proc/$1/cmdline" ]; then
    tr '\0' ' ' < "/proc/$1/cmdline" 2>/dev/null | sed 's/ $//'
    return 0
  fi
  ps -o command= -p "$1" 2>/dev/null
}

lci_pid_ppid_pairs() { # "pid ppid" of every process, one per line
  if [ -r /proc/self/stat ]; then
    local f
    for f in /proc/[0-9]*/stat; do
      sed 's/^\([0-9]*\) .*) [A-Za-z] \([0-9]*\) .*/\1 \2/' "$f" 2>/dev/null
    done
    return 0
  fi
  ps -ax -o pid=,ppid=
}

# lci_descendants <pid>: the pids of all its descendants, one per line (not the pid itself)
lci_descendants() {
  local child
  for child in $(lci_pid_ppid_pairs | awk -v p="$1" '$2 == p { print $1 }'); do
    echo "$child"
    lci_descendants "$child"
  done
}

# lci_kill_tree <signal> <pid>: the process and all its descendants (children first)
lci_kill_tree() {
  local sig=$1 pid=$2 child
  for child in $(lci_pid_ppid_pairs | awk -v p="$pid" '$2 == p { print $1 }'); do
    lci_kill_tree "$sig" "$child"
  done
  kill "-$sig" "$pid" 2>/dev/null
  return 0
}

# lci_parallel_width <n>: the width of the unit-testing pool. The jobs of a pool run in clones of the checkout made with rsync:
# without it the jobs run one at a time, in the checkout, and it says so.
lci_parallel_width() {
  if [ "$1" -gt 1 ] && ! command -v rsync >/dev/null 2>&1; then
    echo "local-ci: rsync is not installed: the unit-testing jobs run one at a time instead of $1 (install rsync to run them in parallel)." >&2
    echo 1
    return 0
  fi
  echo "$1"
}

# lci_pool_grace: seconds that pool_stop waits for the workers to stop before it kills them (LOCAL_CI_POOL_GRACE, default 25)
lci_pool_grace() {
  local n=${LOCAL_CI_POOL_GRACE:-25}
  case "$n" in ''|*[!0-9]*) n=25 ;; esac
  printf '%s\n' "$n"
}

# lci_prune_work <local-ci dir> <this run's folder>: deletes the clones (work/) of the runs that are over. A run folder whose owner
# file (pid and start time, as the locks) names a live process, and this run's own folder, keep theirs; the logs stay.
lci_prune_work() {
  local dir run
  for dir in "$1"/*/; do
    run=${dir%/}
    [ -d "$run/work" ] || continue
    [ "$run" = "${2%/}" ] && continue
    lci_owner_alive "$run/owner" && continue
    rm -rf "$run/work"
  done
  return 0
}
