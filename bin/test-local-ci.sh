#!/bin/sh
set -eu

# ============================================================
# test-local-ci.sh
#
# Tests of the pure logic of bin/local-ci.sh (bin/local-ci-tool.mjs): the job selection from the changed paths and the
# rules of .gitlab-ci.yml, the coverage of the new lines, the Sonar rules guard and the summary with its exit code.
# No Maven, Docker or browser: Node.js only. bin/test-mr-checks.sh runs it, so the "MR checks tests" job does too.
#
# Run it from anywhere: sh bin/test-local-ci.sh
# ============================================================

BIN_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$BIN_DIR/.." && pwd)
TOOL="$BIN_DIR/local-ci-tool.mjs"
CI_FILE="$ROOT/.gitlab-ci.yml"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT INT TERM

PASSED=0
FAILED=0

pass() {
  PASSED=$((PASSED + 1))
  printf 'ok   - %s\n' "$1"
}

fail() {
  FAILED=$((FAILED + 1))
  printf 'FAIL - %s\n' "$1"
  if [ -n "${2:-}" ]; then
    printf '%s\n' "$2" | sed 's/^/         /'
  fi
}

# Runs a command and stores its exit code in $RC and its output in $OUT
run() {
  set +e
  OUT=$("$@" 2>&1)
  RC=$?
  set -e
}

# ------------------------------------------------------------
# Job selection against the real .gitlab-ci.yml
# ------------------------------------------------------------

# The local jobs selected for a change set ("$1": paths, one per line), as " id id ... "
selected() {
  printf '%s\n' "$1" > "$WORK/changed.txt"
  printf ' %s ' "$(node "$TOOL" select "${2:-$CI_FILE}" "$WORK/changed.txt" | awk -F '\t' '$2 == "yes" { printf "%s ", $1 }')"
}

# "$1" name, "$2" changed paths, "$3" jobs that must be selected, "$4" jobs that must not
selection_case() {
  got=$(selected "$2" "${5:-}")
  problems=
  for job in $3; do
    case "$got" in *" $job "*) ;; *) problems="$problems missing:$job" ;; esac
  done
  for job in $4; do
    case "$got" in *" $job "*) problems="$problems unexpected:$job" ;; esac
  done
  if [ -z "$problems" ]; then pass "select: $1"; else fail "select: $1 ($problems)" "selected:$got"; fi
}

selection_case "a doc page runs the website build, the Spanish lint and no Maven job" "website/docs/guides/a.md" \
  "website spanish-docs-lint mr-size mr-docs mr-translations" "build all-ut spotless coverage lint-frontend playwright-angular"
selection_case "a nested Java file of the backend runs the Java jobs" "awe-framework/awe-model/src/main/java/com/almis/A.java" \
  "build spotless all-ut embedded-db db-mysql db-postgresql db-sqlserver db-oracle javadoc coverage sonar-rules playwright-angular playwright-react" \
  "frontend-ut frontend-ut-react lint-frontend website"
selection_case "an AngularJS template runs All UT (templates rule) but no database job" \
  "awe-framework/awe-client-angular/src/main/resources/templates/grid/grid.html" \
  "all-ut frontend-ut lint-frontend build playwright-angular" "embedded-db db-oracle frontend-ut-react playwright-react javadoc"
selection_case "a React client file runs the React jobs, not the AngularJS unit tests" "awe-framework/awe-client-react/src/components/A.jsx" \
  "frontend-ut-react lint-frontend playwright-react build" "frontend-ut all-ut embedded-db"
selection_case "a merge request check script runs its tests" "bin/mr-size.sh" "mr-checks-tests" "build website"
selection_case "the local pipeline scripts run their tests in CI" "bin/local-ci-platform.sh" "mr-checks-tests" "build website"
selection_case "the local pipeline tool runs its tests in CI" "bin/local-ci-tool.mjs" "mr-checks-tests" "build"
selection_case "the local pipeline tests run in CI" "bin/test-local-ci.sh" "mr-checks-tests" "build"
selection_case "the commitlint config (nested) runs the check tests" ".gitlab/commitlint/rules/a.mjs" "mr-checks-tests" "build"
selection_case "a root README runs only the merge request checks" "README.md" "mr-size mr-docs" \
  "build mr-checks-tests website spotless secret-detection"
selection_case "Secret detection runs for code changes (lint stage)" "awe-framework/awe-model/src/main/java/A.java" \
  "secret-detection spotless" ""
selection_case "Secret detection runs for website changes" "website/docs/a.md" "secret-detection" "spotless"

# Secret detection is mirrored only with gitleaks, and never blocks (CI runs GitLab's own analyzer)
if [ "$(node "$TOOL" jobs "$CI_FILE" | awk -F '\t' '$1 == "secret-detection" { print $3 "/" $5 }')" = "no/any" ]; then
  pass "secret detection: a local, non-blocking check"
else
  fail "secret detection: blocking and warning codes" "$(node "$TOOL" jobs "$CI_FILE" | grep secret)"
fi

# The local order follows the stages: every lint-stage job runs before the build
order=$(node "$TOOL" jobs "$CI_FILE" | cut -f1 | tr '\n' ' ')
case "$order" in
  *spotless*secret-detection*lint-frontend*spanish-docs-lint*build\ *) pass "order: the lint stage (Spotless, secrets, lint, Spanish docs) runs before the build" ;;
  *) fail "order: lint stage before the build" "$order" ;;
esac
selection_case "the CI file runs everything" ".gitlab-ci.yml" \
  "build all-ut embedded-db frontend-ut frontend-ut-react lint-frontend website mr-checks-tests playwright-react" ""

# Every local job names CI jobs that exist and have a merge-request rule
node "$TOOL" jobs "$CI_FILE" > "$WORK/jobs.tsv"
missing=
while IFS="$(printf '\t')" read -r id label blocking ci_jobs warn_codes; do
  [ "$ci_jobs" != - ] || continue   # a local check with no CI job (title, size, translations)
  old_ifs=$IFS
  IFS='+'
  for ci_job in $ci_jobs; do
    IFS=$old_ifs
    ci_job=$(printf '%s' "$ci_job" | sed 's/^ *//; s/ *$//')
    rules=$(node "$TOOL" rules "$CI_FILE" "$ci_job" 2>&1 || true)
    if ! printf '%s' "$rules" | grep -q 'CI_MERGE_REQUEST_IID'; then
      missing="$missing $id->$ci_job"
    fi
  done
  IFS=$old_ifs
done < "$WORK/jobs.tsv"
if [ -z "$missing" ]; then pass "every local job maps to a CI job with merge-request rules"; else fail "unmapped local jobs:$missing"; fi

# The Maven goals and options of a CI job's script are those of its local step (a changed CI command shows up here)
maven_parity() { # "$1" CI job, "$2" local step function
  ci_line=$(node "$TOOL" script "$CI_FILE" "$1" | grep -m1 '^mvn ')
  local_line=$(sed -n "/^$2() *{/,/^}/p" "$BIN_DIR/local-ci.sh")
  missing=
  for token in $(printf '%s\n' "$ci_line" | tr ' ' '\n' | grep -E '^([a-z.-]+:[a-zA-Z.-]+(@[a-z-]+)?|-P[^!$][^ ]*|-D[^ $]+)$'); do
    case "$local_line" in *"$token"*) ;; *) missing="$missing $token" ;; esac
  done
  if [ -n "$ci_line" ] && [ -z "$missing" ]; then
    pass "parity: the local $2 runs the goals and options of \"$1\""
  else
    fail "parity: $2 misses$missing of \"$1\"" "$ci_line"
  fi
}
maven_parity "Javadoc test" step_javadoc
maven_parity "Spotless" step_spotless

# The browser suites are the CI jobs of the suite templates, split as CI splits them
run node "$TOOL" suites "$CI_FILE" playwright-react chromium
if [ "$(printf '%s\n' "$OUT" | grep -c 'playwright-react-chromium-')" -eq 4 ] && printf '%s' "$OUT" | grep -q 'SchedulerCalendarTestsIT'; then
  pass "suites: the React Chromium suites come from the CI file (4 jobs, classes)"
else
  fail "suites: React Chromium suites" "$OUT"
fi
run node "$TOOL" suites "$CI_FILE" playwright-angular chromium
if printf '%s\n' "$OUT" | grep -q "^SchedulerIT	-	6320	playwright-chromium-scheduler	AngularJS · Scheduler$"; then
  pass "suites: tag, port, name and CI job of an AngularJS suite"
else
  fail "suites: AngularJS suite" "$OUT"
fi
run node "$TOOL" suites "$CI_FILE" playwright-angular firefox
if [ "$(printf '%s\n' "$OUT" | grep -c 'playwright-firefox-')" -eq 4 ]; then
  pass "suites: the Firefox suites are found (for --browsers firefox)"
else
  fail "suites: AngularJS Firefox suites" "$OUT"
fi
# A merge request runs the Chromium suites only: the Firefox templates have no merge-request rule
for template in .playwright-angularjs-firefox .playwright-react-firefox; do
  rules=$(node "$TOOL" rules "$CI_FILE" "$template" 2>&1 || true)
  case "$rules" in
    *CI_MERGE_REQUEST_IID*) fail "rules: $template should not run in merge requests" "$rules" ;;
    *CI_COMMIT_REF_NAME*) pass "rules: $template runs on develop/master only, not in merge requests" ;;
    *) fail "rules: $template not found" "$rules" ;;
  esac
done

# ------------------------------------------------------------
# Rules semantics on a CI fixture: globs as Ruby fnmatch (FNM_PATHNAME), first matching rule, extends, !reference, manual
# ------------------------------------------------------------
cat > "$WORK/ci.yml" <<'EOF'
.if-mr-shallow: &if-mr-shallow
  if: '$CI_MERGE_REQUEST_IID'
  changes:
    - shallow/**
    - "*.md"
    - "{one,two}/file.txt"
.paths-deep: &paths-deep
  - deep/**/*
.rules-deep: &rules-deep
  - if: '$CI_MERGE_REQUEST_IID'
    changes: *paths-deep
  - if: '$CI_COMMIT_REF_NAME == "develop"'
.template:
  rules:
    - <<: *if-mr-shallow

Spotless:
  extends: .template

Build website:
  rules:
    - !reference [.rules-deep]

Build project:
  rules:
    - if: '$CI_MERGE_REQUEST_IID'
      changes:
        - manual/**/*
      when: manual
    - if: '$CI_MERGE_REQUEST_IID'
      changes: [manual/**/*, other/**/*]
EOF
FIXTURE="$WORK/ci.yml"
selection_case "fixture: dir/** matches one segment below dir" "shallow/a.txt" "spotless" "website" "$FIXTURE"
selection_case "fixture: dir/** does not match two segments below dir" "shallow/a/b.txt" "" "spotless" "$FIXTURE"
selection_case "fixture: * does not cross a directory" "docs/readme.md" "" "spotless" "$FIXTURE"
selection_case "fixture: * matches at the root" "readme.md" "spotless" "" "$FIXTURE"
selection_case "fixture: {a,b} alternatives (extglob)" "two/file.txt" "spotless" "" "$FIXTURE"
selection_case "fixture: dir/**/* matches any depth (through !reference)" "deep/a/b/c.txt" "website" "spotless" "$FIXTURE"
selection_case "fixture: dir/**/* matches directly below dir" "deep/c.txt" "website" "" "$FIXTURE"
selection_case "fixture: the first matching rule decides (manual: not run)" "manual/a/b" "" "build" "$FIXTURE"
selection_case "fixture: a later rule selects when the earlier does not match" "other/x/y" "build" "" "$FIXTURE"

# !reference [.template, rules] (list item and inline), and the "if" expressions evaluated as in a merge request pipeline
cat > "$WORK/ci2.yml" <<'EOF'
.template:
  rules:
    - if: '$CI_MERGE_REQUEST_IID'
      changes:
        - src/**/*
.rules-list: &rules-list
  - if: '$CI_MERGE_REQUEST_IID'
    changes:
      - lib/**/*

Reference item:
  rules:
    - !reference [.template, rules]

Reference inline:
  rules: !reference [.template, rules]

Reference list inline:
  rules: !reference [.rules-list]

MR only on develop:
  rules:
    - if: '$CI_MERGE_REQUEST_IID && $CI_COMMIT_REF_NAME == "develop"'

Not a merge request:
  rules:
    - if: '$CI_MERGE_REQUEST_IID == null'

Negated:
  rules:
    - if: '!$CI_MERGE_REQUEST_IID'

Pipeline source:
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'

Mixed or:
  rules:
    - if: '$RENOVATE_RUN != "true" && ($CI_COMMIT_REF_NAME == "master" || $CI_MERGE_REQUEST_IID)'

Branch variable:
  rules:
    - if: '$CI_COMMIT_BRANCH'

Target develop:
  rules:
    - if: '$CI_MERGE_REQUEST_TARGET_BRANCH_NAME =~ /^develop$/'
EOF
match_case() { # name, CI job, changed path, expected yes|no
  printf '%s\n' "$3" > "$WORK/changed2.txt"
  run node "$TOOL" match "$WORK/ci2.yml" "$2" "$WORK/changed2.txt"
  if [ "$(printf '%s' "$OUT" | cut -f1)" = "$4" ]; then pass "rules: $1"; else fail "rules: $1 (expected $4)" "$OUT"; fi
}
match_case "!reference [.template, rules] as a list item" "Reference item" src/a/b.java yes
match_case "!reference [.template, rules] keeps its changes" "Reference item" docs/a.md no
match_case "rules: !reference [.template, rules] inline" "Reference inline" src/a/b.java yes
match_case "rules: !reference [.rules-list] inline" "Reference list inline" lib/x/y.js yes
match_case "CI_MERGE_REQUEST_IID with a branch condition is false in a feature merge request" "MR only on develop" a no
match_case "CI_MERGE_REQUEST_IID == null is not a merge request rule" "Not a merge request" a no
match_case "a negated CI_MERGE_REQUEST_IID is not a merge request rule" "Negated" a no
match_case "CI_PIPELINE_SOURCE == merge_request_event is a merge request rule" "Pipeline source" a yes
match_case "a merge request inside an || of other conditions" "Mixed or" a yes
match_case "CI_COMMIT_BRANCH is not set in a merge request pipeline" "Branch variable" a no
match_case "the target branch of the merge request is the base branch (develop)" "Target develop" a yes

# ------------------------------------------------------------
# New-code coverage
# ------------------------------------------------------------
JAVA=awe-framework/awe-controller/src/main/java/com/almis/awe/Foo.java
cat > "$WORK/cover.diff" <<EOF
diff --git a/$JAVA b/$JAVA
--- a/$JAVA
+++ b/$JAVA
@@ -10,0 +11,4 @@ class Foo {
+  int a() {
+    return 1;
+  }
+  // only a comment
@@ -40 +44 @@ class Foo {
+    if (x) { y(); }
diff --git a/awe-framework/awe-controller/src/test/java/com/almis/awe/FooTest.java b/awe-framework/awe-controller/src/test/java/com/almis/awe/FooTest.java
--- /dev/null
+++ b/awe-framework/awe-controller/src/test/java/com/almis/awe/FooTest.java
@@ -0,0 +1,2 @@
+class FooTest {
+}
diff --git a/awe-framework/awe-testing/src/main/java/com/almis/awe/testing/T.java b/awe-framework/awe-testing/src/main/java/com/almis/awe/testing/T.java
--- /dev/null
+++ b/awe-framework/awe-testing/src/main/java/com/almis/awe/testing/T.java
@@ -0,0 +1 @@
+class T {}
EOF
# Line 11 not executable, 12 covered, 13 missed, 44 covered with 1 of 2 conditions
printf '%s' '<?xml version="1.0"?><report name="r"><package name="com/almis/awe"><class name="com/almis/awe/Foo"/><sourcefile name="Foo.java"><line nr="12" mi="0" ci="3" mb="0" cb="0"/><line nr="13" mi="2" ci="0" mb="0" cb="0"/><line nr="44" mi="0" ci="4" mb="1" cb="1"/><line nr="80" mi="5" ci="0" mb="0" cb="0"/></sourcefile></package></report>' > "$WORK/jacoco.xml"

run node "$TOOL" coverage --diff "$WORK/cover.diff" --jacoco "$WORK/jacoco.xml" --threshold 80
# (2 covered lines + 1 covered condition) / (3 lines + 2 conditions) = 60%
case "$OUT" in
  *"Coverage on new code: 60.0% (FAIL"*) text_ok=1 ;;
  *) text_ok=0 ;;
esac
if [ "$RC" -eq 1 ] && [ "$text_ok" -eq 1 ]; then pass "coverage: lines and conditions as Sonar counts them, below 80% fails"; else fail "coverage: below threshold (exit $RC)" "$OUT"; fi
case "$OUT" in *"uncovered new lines: 13"*) pass "coverage: the uncovered new line is listed" ;; *) fail "coverage: uncovered line list" "$OUT" ;; esac
case "$OUT" in *"44 (1/2 conditions)"*) pass "coverage: a partly covered branch is listed" ;; *) fail "coverage: partial branches" "$OUT" ;; esac
case "$OUT" in *FooTest*|*testing/T.java*) fail "coverage: tests and excluded modules must not count" "$OUT" ;; *) pass "coverage: tests and Sonar-excluded modules (awe-testing) do not count" ;; esac

run node "$TOOL" coverage --diff "$WORK/cover.diff" --jacoco "$WORK/jacoco.xml" --threshold 60
if [ "$RC" -eq 0 ]; then pass "coverage: at the threshold passes"; else fail "coverage: at the threshold (exit $RC)" "$OUT"; fi

run node "$TOOL" coverage --diff "$WORK/cover.diff" --threshold 80
case "$OUT" in *"NO DATA"*"Foo.java"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 1 ] && [ "$text_ok" -eq 1 ]; then pass "coverage: a changed source without coverage data fails and is named"; else fail "coverage: no data (exit $RC)" "$OUT"; fi

# Jest lcov of a client, with paths relative to the client and absolute
cat > "$WORK/js.diff" <<'EOF'
+++ b/awe-framework/awe-client-angular/src/main/resources/js/awe/services/a.js
@@ -1,0 +2,2 @@
+  const a = 1;
+  return a;
+++ b/awe-framework/awe-client-react/src/b.js
@@ -0,0 +1 @@
+export const b = () => 1;
EOF
printf 'SF:src/main/resources/js/awe/services/a.js\nDA:2,5\nDA:3,0\nend_of_record\n' > "$WORK/angular.lcov"
printf 'SF:%s\nDA:1,2\nBRDA:1,0,0,1\nBRDA:1,0,1,-\nend_of_record\n' "$ROOT/awe-framework/awe-client-react/src/b.js" > "$WORK/react.lcov"
run node "$TOOL" coverage --diff "$WORK/js.diff" --root "$ROOT" --threshold 50 \
  --lcov "awe-framework/awe-client-angular=$WORK/angular.lcov" --lcov "awe-framework/awe-client-react=$WORK/react.lcov"
# (2 covered lines + 1 covered condition) / (3 lines + 2 conditions) = 60%
case "$OUT" in *"Coverage on new code: 60.0% (PASS"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "coverage: Jest lcov of both clients (relative and absolute paths)"; else fail "coverage: lcov (exit $RC)" "$OUT"; fi

# The per-line JaCoCo record keeps what the coverage uses: covered instructions (ci) and branches (mb, cb). Missed
# instructions (mi) are not kept: a line is covered when ci > 0, whatever mi says
run node --input-type=module -e "import { parseJacoco } from '$TOOL';
  const lines = parseJacoco('<report><package name=\"p\"><sourcefile name=\"A.java\"><line nr=\"3\" mi=\"4\" ci=\"1\" mb=\"1\" cb=\"1\"/></sourcefile></package></report>');
  console.log(JSON.stringify(lines.get('p/A.java').get(3)));"
if [ "$OUT" = '{"ci":1,"mb":1,"cb":1}' ]; then
  pass "coverage: a JaCoCo line keeps ci, mb and cb (mi is not used)"
else
  fail "coverage: shape of a JaCoCo line" "$OUT"
fi

# allow_failure: the exit codes that make a job a warning, as GitLab reads them
cat > "$WORK/ci3.yml" <<'EOF'
.warn-64:
  allow_failure:
    exit_codes: 64
.warn-list:
  allow_failure:
    exit_codes: [64, 65]
.warn-block:
  allow_failure:
    exit_codes:
      - 70
      - 71

Any:
  allow_failure: true

Never:
  allow_failure: false

List:
  extends: .warn-list

Single:
  extends: .warn-64

Block list:
  extends: .warn-block

Last template wins:
  extends:
    - .warn-64
    - .warn-list

Own value wins:
  extends: .warn-list
  allow_failure: false

Nothing:
  script: [true]
EOF
allow_case() { # name, job, expected codes (space separated, "" for none)
  run node --input-type=module -e "import fs from 'node:fs'; import { parseCi, allowFailureCodes } from '$TOOL';
    console.log(allowFailureCodes(parseCi(fs.readFileSync('$WORK/ci3.yml', 'utf8')), process.argv[1]).join(' '));" "$2"
  if [ "$OUT" = "$3" ]; then pass "allow_failure: $1"; else fail "allow_failure: $1 (expected '$3')" "$OUT"; fi
}
allow_case "true gives any exit code" Any any
allow_case "false gives none" Never ""
allow_case "exit_codes as a flow list" List "64 65"
allow_case "exit_codes as a single code" Single 64
allow_case "exit_codes as a block list" "Block list" "70 71"
allow_case "through extends, the last template wins" "Last template wins" "64 65"
allow_case "the job's own value overrides its templates" "Own value wins" ""
allow_case "not set gives none" Nothing ""

# Two reports of the same line (e.g. two modules) merge per line: covered when either covers it, and the branches of the line
# counted once (covered = the best of the two, total = the branches of the line), never added up across reports
printf '+++ b/%s\n@@ -9,0 +10 @@\n+    if (a && b) { c(); }\n' "$JAVA" > "$WORK/merge.diff"
printf '%s' '<report><package name="com/almis/awe"><sourcefile name="Foo.java"><line nr="10" mi="3" ci="0" mb="2" cb="0"/></sourcefile></package></report>' > "$WORK/jacoco-a.xml"
printf '%s' '<report><package name="com/almis/awe"><sourcefile name="Foo.java"><line nr="10" mi="0" ci="3" mb="1" cb="1"/></sourcefile></package></report>' > "$WORK/jacoco-b.xml"
run node "$TOOL" coverage --diff "$WORK/merge.diff" --jacoco "$WORK/jacoco-a.xml" --jacoco "$WORK/jacoco-b.xml" --threshold 50
# (1 covered line + 1 covered branch) / (1 line + 2 branches) = 66.7%
case "$OUT" in
  *"conditions: 1/2"*"Coverage on new code: 66.7%"*) pass "coverage: two reports of a line merge per line (1/2 branches, not 1/3)" ;;
  *) fail "coverage: merge of two reports" "$OUT" ;;
esac

# The coverage scope (what the coverage step needs) uses the same rules as the coverage itself
printf '%s\n' "$JAVA" awe-framework/awe-controller/src/test/java/com/almis/awe/FooTest.java \
  awe-framework/awe-testing/src/main/java/com/almis/awe/testing/T.java awe-framework/awe-client-react/src/b.js \
  awe-framework/awe-client-react/test/js/b.test.js website/docs/a.md > "$WORK/scope.txt"
run node "$TOOL" scope "$WORK/scope.txt"
if [ "$OUT" = "jacoco
lcov:awe-framework/awe-client-react" ]; then
  pass "coverage scope: main Java sources (jacoco) and the React sources (lcov); tests, awe-testing and docs need nothing"
else
  fail "coverage scope" "$OUT"
fi

printf '+++ b/README.md\n@@ -1,0 +2 @@\n+text\n' > "$WORK/docs.diff"
run node "$TOOL" coverage --diff "$WORK/docs.diff" --threshold 80
case "$OUT" in *"nothing to measure"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "coverage: no new executable line passes"; else fail "coverage: nothing to measure (exit $RC)" "$OUT"; fi

# ------------------------------------------------------------
# Sonar rules guard
# ------------------------------------------------------------
SONAR_ROOT="$WORK/sonar"
TEST_FILE=awe-framework/awe-controller/src/test/java/com/almis/awe/BarTest.java
mkdir -p "$SONAR_ROOT/$(dirname "$TEST_FILE")"
cat > "$SONAR_ROOT/$TEST_FILE" <<'EOF'
class BarTest {
  @Test
  void literals() {
    assertEquals("repeated value", a("repeated value"));
    assertEquals("repeated value", b());
    // service.call(argument);
    assertThrows(AWException.class, () -> service.run(builder.build()));
    assertThrows(AWException.class, () -> service.run(prepared));
  }
  @Test
  void onlyAssertions() throws Exception {
    assertEquals(1, value);
  }
  void chain() { q("ab").table("cd"); q("ef").table("gh"); q("ij").table("kl"); }
}
EOF
{
  echo "+++ b/$TEST_FILE"
  echo "@@ -0,0 +1,15 @@"
  sed 's/^/+/' "$SONAR_ROOT/$TEST_FILE"
} > "$WORK/sonar.diff"
run node "$TOOL" sonar --diff "$WORK/sonar.diff" --root "$SONAR_ROOT"
for expected in "S1192 $TEST_FILE:4" "S125 $TEST_FILE:6" "S5778 $TEST_FILE:7" "S1130 $TEST_FILE:11"; do
  case "$OUT" in *"$expected"*) pass "sonar: $expected" ;; *) fail "sonar: expected $expected" "$OUT" ;; esac
done
case "$OUT" in *"$TEST_FILE:8"*) fail "sonar: a lambda with one call is fine" "$OUT" ;; *) pass "sonar: a lambda with one call is not reported" ;; esac
case "$OUT" in *'").table("'*) fail "sonar: the code between two literals is not a literal" "$OUT" ;; *) pass "sonar: the code between two literals is not a literal" ;; esac
if [ "$RC" -eq 64 ]; then pass "sonar: findings end with 64 (warning)"; else fail "sonar: exit code with findings ($RC)" "$OUT"; fi

cat > "$SONAR_ROOT/$TEST_FILE" <<'EOF'
class BarTest {
  @Test
  void clean() throws Exception {
    // Builds the expected value
    // Returns { selected, reason }
    service.load(path);
  }
}
EOF
{
  echo "+++ b/$TEST_FILE"
  echo "@@ -0,0 +1,8 @@"
  sed 's/^/+/' "$SONAR_ROOT/$TEST_FILE"
} > "$WORK/sonar.diff"
run node "$TOOL" sonar --diff "$WORK/sonar.diff" --root "$SONAR_ROOT"
if [ "$RC" -eq 0 ]; then pass "sonar: a prose comment and a throwing call are fine (exit 0)"; else fail "sonar: clean file (exit $RC)" "$OUT"; fi

# ------------------------------------------------------------
# Platforms (bin/local-ci-platform.sh): every probe of the machine is injected through a variable or a stub in PATH, so
# these cases simulate Windows, WSL and a Linux without lsof on any machine
# ------------------------------------------------------------
PLATFORM="$BIN_DIR/local-ci-platform.sh"
if ! command -v bash >/dev/null 2>&1; then
  pass "platform: skipped (no bash on this machine; bin/local-ci.sh needs bash anyway)"
else

# Runs "$1" (bash code) with the platform functions loaded; $RC and $OUT as run()
platform() {
  run bash -c ". \"$PLATFORM\"; $1"
}
# "$1" name, "$2" expected exit, "$3" expected text ("" for none), "$4" code, "$5" text that must NOT appear
platform_case() {
  platform "$4"
  text_ok=1
  [ -n "$3" ] && case "$OUT" in *"$3"*) ;; *) text_ok=0 ;; esac
  [ -n "${5:-}" ] && case "$OUT" in *"$5"*) text_ok=0 ;; esac
  if [ "$RC" -eq "$2" ] && [ "$text_ok" -eq 1 ]; then pass "platform: $1"; else fail "platform: $1 (exit $RC, expected $2)" "$OUT"; fi
}

WSL_VERSION='Linux version 5.15.153.1-microsoft-standard-WSL2 (root@1c602f52c2e4) (gcc (GCC) 11.2.0) #1 SMP'
UBUNTU_VERSION='Linux version 6.8.0-45-generic (buildd@lcy02-amd64-075) (x86_64-linux-gnu-gcc-13) #45-Ubuntu SMP'

platform_case "Git Bash (MINGW) is refused with exit 2 and points to WSL2" 2 "inside WSL2" \
  "LOCAL_CI_UNAME=MINGW64_NT-10.0-22631 lci_platform_check /c/Users/dev/awe"
platform_case "MSYS2 is refused" 2 "inside WSL2" "LOCAL_CI_UNAME=MSYS_NT-10.0-22631 lci_platform_check /home/dev/awe"
platform_case "Cygwin is refused" 2 "inside WSL2" "LOCAL_CI_UNAME=CYGWIN_NT-10.0 lci_platform_check /home/dev/awe"
platform_case "WSL with the repository under /mnt/c warns and suggests the WSL home" 0 "clone it inside the WSL home" \
  "LOCAL_CI_UNAME=Linux LOCAL_CI_PROC_VERSION='$WSL_VERSION' lci_platform_check /mnt/c/Users/dev/awe"
platform_case "WSL with the repository in the WSL home does not warn" 0 "" \
  "LOCAL_CI_UNAME=Linux LOCAL_CI_PROC_VERSION='$WSL_VERSION' lci_platform_check /home/dev/awe" "WARNING"
platform_case "a plain Linux under /mnt does not warn (not WSL)" 0 "" \
  "LOCAL_CI_UNAME=Linux LOCAL_CI_PROC_VERSION='$UBUNTU_VERSION' lci_platform_check /mnt/c/awe" "WARNING"
platform_case "operating systems: mac, wsl, linux, windows" 0 "mac wsl linux windows" \
  "printf '%s ' \$(LOCAL_CI_UNAME=Darwin lci_os) \$(LOCAL_CI_UNAME=Linux LOCAL_CI_PROC_VERSION='$WSL_VERSION' lci_os) \
   \$(LOCAL_CI_UNAME=Linux LOCAL_CI_PROC_VERSION='$UBUNTU_VERSION' lci_os) \$(LOCAL_CI_UNAME=MINGW64_NT-10.0 lci_os)"
# env, not an assignment before a function: in POSIX sh that assignment would stay set after the call
run env LOCAL_CI_UNAME=MINGW64_NT-10.0-22631 "$BIN_DIR/local-ci.sh" --list
case "$OUT" in *WSL2*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 2 ] && [ "$text_ok" -eq 1 ]; then pass "platform: bin/local-ci.sh stops at once in Git Bash (exit 2)"; else fail "platform: local-ci.sh in Git Bash (exit $RC)" "$OUT"; fi

# Ports: stubs of ss and fuser in PATH, and LOCAL_CI_PORT_TOOLS to choose which tools "exist"
STUBS="$WORK/stubs"
mkdir -p "$STUBS"
cat > "$STUBS/ss" <<'EOF'
#!/bin/sh
# ss -ltnpH "sport = :<port>": a listener with its pid on 8090, one of another user (no pid) on 9001, nothing else
case "$*" in
  *":8090"*) echo 'LISTEN 0 100 *:8090 *:* users:(("java",pid=4242,fd=393))' ;;
  *":9001"*) echo 'LISTEN 0 50 *:9001 *:*' ;;
esac
EOF
cat > "$STUBS/fuser" <<'EOF'
#!/bin/sh
case "$*" in *8090*) echo "8090/tcp:" >&2; echo " 4343" ;; esac
EOF
chmod +x "$STUBS/ss" "$STUBS/fuser"
platform_case "no lsof, no ss, no fuser: a port is never assumed free" 0 "cannot check port 8090: install lsof or iproute2" \
  "LOCAL_CI_PORT_TOOLS= lci_port_busy 8090"
platform_case "without lsof, ss finds the pid on the port" 0 "port 8090 is in use (pid 4242)" \
  "PATH=\"$STUBS:\$PATH\" LOCAL_CI_PORT_TOOLS=ss lci_port_busy 8090"
platform_case "ss: a listener whose pid cannot be read still makes the port busy" 0 "port 9001 is in use (pid ?)" \
  "PATH=\"$STUBS:\$PATH\" LOCAL_CI_PORT_TOOLS=ss lci_port_busy 9001"
platform_case "ss: a free port is free" 1 "" "PATH=\"$STUBS:\$PATH\" LOCAL_CI_PORT_TOOLS=ss lci_port_busy 8091"
platform_case "fuser when there is neither lsof nor ss" 0 "pid 4343" \
  "PATH=\"$STUBS:\$PATH\" LOCAL_CI_PORT_TOOLS=fuser lci_port_busy 8090"
platform_case "lsof comes first when it is installed" 0 "lsof ss" \
  "PATH=\"$STUBS:\$PATH\"; lsof() { :; }; LOCAL_CI_PORT_TOOLS='lsof ss' lci_port_tools"

# JDK search: fake JDK homes (only their release file matters)
mkdir -p "$WORK/jvm/temurin-21" "$WORK/jvm/openjdk-17" "$WORK/nojvm"
echo 'JAVA_VERSION="21.0.4"' > "$WORK/jvm/temurin-21/release"
echo 'JAVA_VERSION="17.0.12"' > "$WORK/jvm/openjdk-17/release"
# The JDK the build needs is the java.version of awe-dependencies (21 since #783), not a fixed one
mkdir -p "$WORK/javaroot/awe-framework/awe-dependencies"
printf '<project>\n  <properties>\n    <otp-java.version>2.1.0</otp-java.version>\n    <java.version>21</java.version>\n  </properties>\n</project>\n' \
  > "$WORK/javaroot/awe-framework/awe-dependencies/pom.xml"
platform_case "the required JDK is the java.version of the build" 0 "21" "lci_required_java '$WORK/javaroot'" "2.1.0"
platform_case "the real build asks for a JDK too" 0 "" "[ -n \"\$(lci_required_java '$ROOT')\" ]"
if node "$TOOL" jobs "$CI_FILE" | grep -q '^build-jdk'; then
  fail "jobs: no local job for a removed CI job (Build project:jdk 21)"
else
  pass "jobs: no local job for a removed CI job (Build project:jdk 21)"
fi

platform_case "the JDK 17 is found among other JDKs" 0 "$WORK/jvm/openjdk-17" \
  "LOCAL_CI_JDK_DIRS='$WORK/jvm/*' lci_find_jdk 17"
platform_case "LOCAL_CI_JDK17 wins" 0 "$WORK/jvm/openjdk-17" \
  "LOCAL_CI_JDK17='$WORK/jvm/openjdk-17' LOCAL_CI_JDK_DIRS='$WORK/nojvm/*' lci_find_jdk 17"
platform_case "no JDK 17: the help says where it looked and the apt package on WSL" 0 "sudo apt install openjdk-17-jdk" \
  "[ -z \"\$(LOCAL_CI_JDK_DIRS='$WORK/nojvm/*' lci_find_jdk 17)\" ] && LOCAL_CI_UNAME=Linux \
   LOCAL_CI_PROC_VERSION='$WSL_VERSION' LOCAL_CI_JDK_DIRS='$WORK/nojvm/*' lci_jdk_help 17"
platform_case "no JDK 17: the searched directories are named" 0 "$WORK/nojvm/*" \
  "LOCAL_CI_JDK_DIRS='$WORK/nojvm/*' lci_jdk_help 17"

# Prerequisites and the Playwright libraries
platform_case "a missing command is named with its install hint (WSL: apt)" 1 "'mvn' is not installed: sudo apt install maven" \
  "LOCAL_CI_UNAME=Linux LOCAL_CI_PROC_VERSION='$WSL_VERSION'; PATH='$WORK/nojvm'; lci_check_commands mvn"
platform_case "present commands pass" 0 "" "lci_check_commands sh"
# Playwright libraries: the packages come from Playwright itself ("install-deps --dry-run" of the pinned version, per
# browser), so the printed command always clears the check; the check only advises, a run fails only if a browser cannot start
cat > "$WORK/install-deps-dry-run.txt" <<'EOF'
sudo -- sh -c "apt-get update&& apt-get install -y --no-install-recommends libasound2t64 libatk-bridge2.0-0t64 libnss3 libxkbcommon0 xvfb fonts-liberation"
EOF
run node "$TOOL" apt-packages "$WORK/install-deps-dry-run.txt"
if [ "$(printf '%s' "$OUT" | tr '\n' ' ')" = "libasound2t64 libatk-bridge2.0-0t64 libnss3 libxkbcommon0 xvfb fonts-liberation" ]; then
  pass "playwright: the packages of install-deps --dry-run are read (and nothing else, e.g. no libgtk-3 for chromium)"
else
  fail "playwright: apt packages of the dry run" "$OUT"
fi
printf 'sh -c "apt-get update&& apt-get install -y --no-install-recommends libnss3"\n' > "$WORK/install-deps-root.txt"
run node "$TOOL" apt-packages "$WORK/install-deps-root.txt"
if [ "$OUT" = libnss3 ]; then pass "playwright: the dry run as root (no sudo) is read too"; else fail "playwright: dry run as root" "$OUT"; fi
cat > "$STUBS/dpkg-query" <<'EOF'
#!/bin/sh
# installed: libnss3 and xvfb; the rest is not
for last; do :; done
case "$last" in libnss3|xvfb) echo installed ;; *) echo "dpkg-query: no packages found matching $last" >&2; exit 1 ;; esac
EOF
chmod +x "$STUBS/dpkg-query"
platform_case "playwright: the packages not installed are listed (dpkg)" 0 "libasound2t64 fonts-liberation" \
  "PATH=\"$STUBS:\$PATH\"; printf '%s ' \$(lci_missing_packages libasound2t64 libnss3 xvfb fonts-liberation)" "libnss3"
platform_case "playwright: without dpkg (not Debian/Ubuntu) nothing is guessed" 3 "" \
  "PATH='$WORK/nojvm'; lci_missing_packages libnss3"
printf '[pid=1][err] /home/u/.cache/ms-playwright/chromium_headless_shell-1243/chrome: error while loading shared libraries: libnss3.so: cannot open shared object file\n' > "$WORK/libs-failure.log"
printf 'Host system is missing dependencies to run browsers.\n' > "$WORK/libs-failure2.log"
printf 'Tests run: 3, Failures: 1\nexpected <1> but was <2>\n' > "$WORK/test-failure.log"
platform_case "playwright: a browser that cannot load its libraries is recognised in the log" 0 "" \
  "lci_browser_libs_failure '$WORK/libs-failure.log' && lci_browser_libs_failure '$WORK/libs-failure2.log'"
platform_case "playwright: an ordinary test failure is not taken for missing libraries" 1 "" \
  "lci_browser_libs_failure '$WORK/test-failure.log'"
platform_case "playwright: the install command runs Playwright's own install-deps for the selected browsers, with sudo" 0 \
  "sudo \"/jdk/bin/java\" -cp \"/m2/playwright.jar:/m2/driver.jar:/m2/driver-bundle.jar\" com.microsoft.playwright.CLI install-deps chromium firefox" \
  "lci_playwright_deps_command /jdk/bin/java '/m2/playwright.jar:/m2/driver.jar:/m2/driver-bundle.jar' 'chromium firefox'"

# An inherited SERVER_PORT would move the Spring test contexts of All UT and the database jobs off their ports (23
# RestControllerTest failures with SERVER_PORT=8090): the run drops it, and only the browser steps set it, for their app
platform_case "environment: an inherited SERVER_PORT is dropped" 0 "after=[none]" \
  "export SERVER_PORT=8090; lci_clean_env >/dev/null; echo \"after=[\${SERVER_PORT-none}]\""
platform_case "environment: dropping SERVER_PORT is noted" 0 "SERVER_PORT=8090 is ignored" \
  "export SERVER_PORT=8090; lci_clean_env"
platform_case "environment: nothing to say without SERVER_PORT" 0 "" "unset SERVER_PORT; lci_clean_env" "SERVER_PORT"

# Test counts of a Maven log: the summaries after "Results:", also with the flakes that a rerun absorbed (failsafe prints
# ", Flakes: N" then, and a suite that ran 71 tests must not read "tests 0")
cat > "$WORK/maven.log" <<'EOF'
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 74.41 s -- in MatrixTestsIT
[WARNING] Tests run: 71, Failures: 0, Errors: 0, Skipped: 0, Flakes: 1
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
[ERROR] Tests run: 5, Failures: 1, Errors: 1, Skipped: 2
EOF
platform_case "counts: summaries with and without flakes, per-class lines left out" 0 "tests 76, failed 2, skipped 2, flaky 1" \
  "lci_test_counts '$WORK/maven.log'"
printf '[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0\n' > "$WORK/maven2.log"
platform_case "counts: no flakes, no flaky note" 0 "tests 3, failed 0, skipped 0" "lci_test_counts '$WORK/maven2.log'" "flaky"

# Step status: exit 64 is a warning only for the jobs whose CI allow_failure lists it
platform_case "status: 0 passes" 0 "PASS" "lci_status 0 '' 0 0"
platform_case "status: 64 of a job that allows it is a warning" 0 "WARN" "lci_status 64 '64' 0 0"
platform_case "status: 64 of any other job is a failure" 0 "FAIL" "lci_status 64 '' 0 0"
platform_case "status: allow_failure: true makes any failure a warning" 0 "WARN" "lci_status 1 'any' 0 0"
platform_case "status: a timeout fails even with exit 0" 0 "FAIL" "lci_status 0 '64' 1 0"
platform_case "status: an interrupted step fails" 0 "FAIL" "lci_status 64 '64' 0 1"

# Coverage reports: only those written by this run count
touch "$WORK/old.exec"
sleep 1
touch "$WORK/started"
sleep 1
touch "$WORK/new.exec"
platform_case "fresh reports: only files newer than the start of the run" 0 "$WORK/new.exec" \
  "lci_fresh '$WORK/started' '$WORK/old.exec' '$WORK/new.exec' '$WORK/missing.exec'" "old.exec"

# A hard-killed website step (SIGKILL, closed terminal, WSL shutdown) leaves the swap of the injection behind: website/docs
# holds the 4.x pages and the developer's docs wait in the backup. The next run heals it only when the run that owned the
# swap (its marker: pid and start time) is dead, never deletes anything (the current docs are moved aside), and otherwise
# only explains how to restore by hand.
# "$1": the repository; "$2": the owner written in the marker ("dead", "live" or "none" for no marker)
make_swap() {
  rm -rf "$1"
  mkdir -p "$1/website/docs" "$1/website/.docs-current" "$1/sub/dir"
  echo '4.x page' > "$1/website/docs/intro.md"
  echo 'edited after the kill' > "$1/website/docs/new.md"
  echo 'my develop page' > "$1/website/.docs-current/intro.md"
  echo 'module.exports = "4.x";' > "$1/website/sidebars.js"
  echo 'module.exports = "develop";' > "$1/website/.sidebars-current.js"
  case "$2" in
    dead)
      sh -c 'exit 0' &
      dead_pid=$!
      wait "$dead_pid"
      printf '%s\n%s\n' "$dead_pid" 'Mon Oct 12 09:00:00 2026' > "$1/website/.local-ci-swap"
      ;;
    live)
      bash -c ". \"$PLATFORM\"; printf '%s\n%s\n' \$PPID \"\$(website_proc_start \$PPID)\"" > "$1/website/.local-ci-swap"
      ;;
  esac
}
HEAL="$WORK/heal"

make_swap "$HEAL" dead
platform "cd '$HEAL/sub/dir' && lci_heal_website_swap '$HEAL'"
problems=
grep -q 'my develop page' "$HEAL/website/docs/intro.md" || problems="$problems docs-not-restored"
grep -q develop "$HEAL/website/sidebars.js" || problems="$problems sidebars-not-restored"
[ -e "$HEAL/website/.docs-current" ] && problems="$problems docs-backup-left"
[ -e "$HEAL/website/.sidebars-current.js" ] && problems="$problems sidebars-backup-left"
[ -e "$HEAL/website/.local-ci-swap" ] && problems="$problems marker-left"
moved=$(find "$HEAL/website" -maxdepth 1 -name '.local-ci-orphan-*' | head -1)
[ -n "$moved" ] && grep -q '4.x page' "$moved/docs/intro.md" && grep -q 'edited after the kill' "$moved/docs/new.md" \
  && grep -q '4.x' "$moved/sidebars.js" || problems="$problems moved-copy-incomplete"
case "$OUT" in *".local-ci-orphan-"*) ;; *) problems="$problems no-note-of-the-moved-copy" ;; esac
if [ "$RC" -eq 0 ] && [ -z "$problems" ]; then
  pass "website heal: a swap of a dead run is healed from a subdirectory, and the docs found there are moved aside, not deleted"
else
  fail "website heal: dead owner:$problems (exit $RC)" "$OUT"
fi

make_swap "$HEAL" live
platform "lci_heal_website_swap '$HEAL'"
if [ "$RC" -eq 0 ] && grep -q '4.x page' "$HEAL/website/docs/intro.md" && [ -d "$HEAL/website/.docs-current" ] \
  && [ -f "$HEAL/website/.local-ci-swap" ] && [ -z "$(find "$HEAL/website" -maxdepth 1 -name '.local-ci-orphan-*')" ]; then
  case "$OUT" in
    *"is still running"*) pass "website heal: the live swap of another run is never touched" ;;
    *) fail "website heal: live owner, no note" "$OUT" ;;
  esac
else
  fail "website heal: live owner was touched (exit $RC)" "$OUT"
fi

make_swap "$HEAL" none
platform "lci_heal_website_swap '$HEAL'"
if [ "$RC" -eq 0 ] && grep -q '4.x page' "$HEAL/website/docs/intro.md" && [ -d "$HEAL/website/.docs-current" ] \
  && [ -z "$(find "$HEAL/website" -maxdepth 1 -name '.local-ci-orphan-*')" ]; then
  case "$OUT" in
    *"mv website/.docs-current website/docs"*) pass "website heal: a backup without a marker is untouched, with the manual restore commands" ;;
    *) fail "website heal: no marker, no restore commands" "$OUT" ;;
  esac
else
  fail "website heal: a backup without a marker was touched (exit $RC)" "$OUT"
fi
platform "lci_swap_pending '$HEAL'"
if [ "$RC" -eq 0 ]; then pass "website heal: an unhealed swap is reported, so the website step does not run over it"; else fail "website heal: pending swap (exit $RC)" "$OUT"; fi

rm -rf "$HEAL"
mkdir -p "$HEAL/website/docs"
echo 'my develop page' > "$HEAL/website/docs/intro.md"
platform "lci_heal_website_swap '$HEAL' && lci_swap_pending '$HEAL'"
if [ "$RC" -eq 1 ] && [ -z "$OUT" ] && grep -q 'my develop page' "$HEAL/website/docs/intro.md"; then
  pass "website heal: nothing to heal is silent and changes nothing"
else
  fail "website heal: no orphan (exit $RC)" "$OUT"
fi
# A run cleaning up its own interrupted swap never puts the backups back while the injection named in the marker is alive (its
# own trap restores the swap): it waits a moment, then leaves it with a note
make_swap "$HEAL" live
platform "LOCAL_CI_OWN_WAIT=1 lci_heal_website_swap '$HEAL' own"
if [ "$RC" -eq 0 ] && grep -q '4.x page' "$HEAL/website/docs/intro.md" && [ -d "$HEAL/website/.docs-current" ] \
  && [ -z "$(find "$HEAL/website" -maxdepth 1 -name '.local-ci-orphan-*')" ]; then
  case "$OUT" in
    *"still running"*) pass "website heal: a run's own swap is not put back while its injection is still alive" ;;
    *) fail "website heal: own swap with a live injection, no note" "$OUT" ;;
  esac
else
  fail "website heal: own swap was touched while its injection lives (exit $RC)" "$OUT"
fi

# Once its injection is dead, a run puts back its own interrupted swap by moving what it finds aside, never deleting it
make_swap "$HEAL" dead
platform "lci_heal_website_swap '$HEAL' own"
moved=$(find "$HEAL/website" -maxdepth 1 -name '.local-ci-orphan-*' | head -1)
if [ "$RC" -eq 0 ] && grep -q 'my develop page' "$HEAL/website/docs/intro.md" && [ -n "$moved" ] \
  && grep -q '4.x page' "$moved/docs/intro.md" && grep -q 'edited after the kill' "$moved/docs/new.md" \
  && [ ! -e "$HEAL/website/.local-ci-swap" ]; then
  pass "website heal: a run puts back its own interrupted swap by moving what it finds aside, not deleting it"
else
  fail "website heal: own swap (exit $RC)" "$OUT"
fi

# The injection itself (bin/website-maintenance-docs.sh), with git and npm stubbed: it marks the swap with its own pid (the
# process that swaps, so a parent that dies first never makes it look dead), removes the marker when it swaps back, and
# refuses to run over an existing backup
INJ="$WORK/injection"
rm -rf "$INJ"
mkdir -p "$INJ/repo/website/docs" "$INJ/fixture/website/docs" "$INJ/bin"
echo 'develop page' > "$INJ/repo/website/docs/intro.md"
echo 'module.exports = "develop";' > "$INJ/repo/website/sidebars.js"
printf '["4.12.0"]\n' > "$INJ/repo/website/versions.json"
echo '4.x page' > "$INJ/fixture/website/docs/intro.md"
echo 'module.exports = "4.x";' > "$INJ/fixture/website/sidebars.js"
cat > "$INJ/bin/git" <<EOF
#!/bin/sh
case "\$1" in
  ls-remote|fetch) exit 0 ;;
  archive) tar -C "$INJ/fixture" -cf - website/docs website/sidebars.js ;;
  *) exit 1 ;;
esac
EOF
cat > "$INJ/bin/npm" <<EOF
#!/bin/sh
# docs:version: records what the swap looks like while the injection runs
cat website/.local-ci-swap > "$INJ/marker-during" 2>/dev/null
cat website/docs/intro.md > "$INJ/docs-during"
mkdir -p website/versioned_docs/version-4.x
EOF
chmod +x "$INJ/bin/git" "$INJ/bin/npm"
run sh -c "cd '$INJ/repo' && PATH='$INJ/bin':\"\$PATH\" bash '$BIN_DIR/website-maintenance-docs.sh'"
marker_pid=$(sed -n 1p "$INJ/marker-during" 2>/dev/null)
if [ "$RC" -eq 0 ] && grep -q '4.x page' "$INJ/docs-during" && [ -n "$marker_pid" ] && [ "$marker_pid" != "$$" ] \
  && grep -q 'develop page' "$INJ/repo/website/docs/intro.md" && [ ! -e "$INJ/repo/website/.local-ci-swap" ] \
  && [ ! -e "$INJ/repo/website/.docs-current" ]; then
  pass "injection: it marks the swap with its own pid while it runs and removes the marker when it swaps back"
else
  fail "injection: marker of the swap (exit $RC, marker pid '$marker_pid')" "$OUT"
fi
mkdir -p "$INJ/repo/website/.docs-current"
echo 'kept develop page' > "$INJ/repo/website/.docs-current/intro.md"
run sh -c "cd '$INJ/repo' && PATH='$INJ/bin':\"\$PATH\" bash '$BIN_DIR/website-maintenance-docs.sh'"
case "$OUT" in *"already exists"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -ne 0 ] && [ "$text_ok" -eq 1 ] && grep -q 'kept develop page' "$INJ/repo/website/.docs-current/intro.md" \
  && grep -q 'develop page' "$INJ/repo/website/docs/intro.md"; then
  pass "injection: it refuses to run over an existing backup and touches neither the backup nor website/docs"
else
  fail "injection: existing backup (exit $RC)" "$OUT"
fi

# One local-ci run per checkout: a lock in the git dir of the worktree, with the pid and start time of its holder
LOCK="$WORK/gitdir/local-ci.lock"
mkdir -p "$WORK/gitdir"
platform "lci_lock_acquire '$LOCK' && sed -n 1p '$LOCK/owner'"
case "$OUT" in [0-9]*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "lock: a free lock is taken"; else fail "lock: take (exit $RC)" "$OUT"; fi
# held by a live run (this test shell)
bash -c ". \"$PLATFORM\"; printf '%s\n%s\n' \$PPID \"\$(website_proc_start \$PPID)\"" > "$LOCK/owner"
platform "lci_lock_acquire '$LOCK'"
case "$OUT" in *"another local-ci run"*"is running in this checkout"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 2 ] && [ "$text_ok" -eq 1 ] && [ "$(sed -n 1p "$LOCK/owner")" = "$$" ]; then
  pass "lock: a second run while the first holds the lock exits 2 and leaves the lock alone"
else
  fail "lock: second run (exit $RC)" "$OUT"
fi
platform "lci_lock_release '$LOCK'"
if [ -d "$LOCK" ]; then pass "lock: a run never releases the lock of another run"; else fail "lock: released another run's lock"; fi
# a lock whose owner is not written yet (another run between its mkdir and its owner file) is held while it is young
rm -rf "$LOCK"
mkdir "$LOCK"
platform "lci_lock_acquire '$LOCK'"
if [ "$RC" -eq 2 ] && [ -d "$LOCK" ] && [ ! -s "$LOCK/owner" ]; then
  pass "lock: a fresh lock without an owner yet is held, not stolen"
else
  fail "lock: fresh lock without owner (exit $RC)" "$OUT"
fi
# ... and taken over once it is older than the grace period (a run killed between mkdir and its owner file)
touch -t 202601010000 "$LOCK"
platform "lci_lock_acquire '$LOCK' && sed -n 1p '$LOCK/owner'"
case "$OUT" in *"taking over"*[0-9]*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ] && [ -z "$(find "$WORK/gitdir" -maxdepth 1 -name 'local-ci.lock.stale*')" ]; then
  pass "lock: an old lock without an owner is taken over (moved aside atomically, then removed)"
else
  fail "lock: old lock without owner (exit $RC)" "$OUT"
fi
rm -rf "$LOCK"
mkdir "$LOCK"
# held by a dead run: taken over
sh -c 'exit 0' &
dead_pid=$!
wait "$dead_pid"
printf '%s\n%s\n' "$dead_pid" 'Mon Oct 12 09:00:00 2026' > "$LOCK/owner"
platform "lci_lock_acquire '$LOCK' && sed -n 1p '$LOCK/owner' && lci_lock_release '$LOCK' && [ ! -d '$LOCK' ] && echo released"
case "$OUT" in *"taking over"*released*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "lock: the lock of a dead run is taken over, and released by its new holder"; else fail "lock: takeover (exit $RC)" "$OUT"; fi
platform "lci_lock_path"
case "$OUT" in */local-ci.lock) pass "lock: it lives in the git dir of this worktree" ;; *) fail "lock: path" "$OUT" ;; esac

# ------------------------------------------------------------
# Machine-wide locks. Only the Oracle container (heavy, slow to start) and the number of browser applications are limited
# across checkouts: every test application listens on ports that bin/local-ci.sh picks free per run, and the awe-boot Spring
# test context listens on a random port, so nothing else needs a lock. Owner file on four lines: owner, pid, epoch, start time
# (ps -o lstart=), the format of the shared scratchpad lock (v2).
# ------------------------------------------------------------
ORACLE="$WORK/oracle.lock"
write_lock_owner() { # "$1" lock dir, "$2" owner name, "$3" pid, "$4" start time
  mkdir -p "$1"
  printf '%s\n%s\n%s\n%s\n' "$2" "$3" "$(date +%s)" "$4" > "$1/owner"
}
LIVE_START=$(bash -c ". \"$PLATFORM\"; website_proc_start $$")
rm -rf "$ORACLE"

# The start time must not depend on a ps that has -p and lstart (BusyBox has neither): a live pid has one, it is stable, and
# a pid that does not exist has none
mkdir -p "$WORK/stub-ps"
printf '#!/bin/sh\nexit 1\n' > "$WORK/stub-ps/ps"
chmod +x "$WORK/stub-ps/ps"
if [ -r /proc/self/stat ]; then
  platform_case "start time: read from /proc, so a ps without -p or lstart does not make a live process look dead" 0 "same" \
    "PATH='$WORK/stub-ps':\$PATH; a=\$(website_proc_start \$\$); b=\$(website_proc_start \$\$); [ -n \"\$a\" ] && [ \"\$a\" = \"\$b\" ] && echo same"
else
  pass "platform: start time: no /proc here, ps -o lstart= is used (checked by the lock tests)"
fi
platform_case "start time: a pid that does not exist has none" 0 "none" "[ -z \"\$(website_proc_start 999999)\" ] && echo none"

# lci_with_oracle_lock <owner> <command...> takes the Oracle lock, runs the command, releases it, returns its exit code
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE'; lci_with_oracle_lock me sh -c 'test -f $ORACLE/owner && exit 7'; echo rc=\$?; [ ! -d '$ORACLE' ] && echo released"
case "$OUT" in *rc=7*released*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$text_ok" -eq 1 ]; then pass "oracle lock: the job holds the lock while it runs, passes its exit code on and releases it"; else fail "oracle lock: hold and release" "$OUT"; fi
write_lock_owner "$ORACLE" other-oracle "$$" "$LIVE_START"
platform "LOCAL_CI_ORACLE_LOCK='$ORACLE' LOCAL_CI_PORTS_WAIT=2 LOCAL_CI_PORTS_POLL=1 lci_with_oracle_lock me sh -c 'echo JOB-RAN'"
case "$OUT" in *"waiting for the Oracle lock"*"other-oracle"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -ne 0 ] && [ "$text_ok" -eq 1 ] && ! printf '%s' "$OUT" | grep -q JOB-RAN && [ "$(sed -n 1p "$ORACLE/owner")" = other-oracle ]; then
  pass "oracle lock: a live holder is waited for (bounded), never run without the lock, never taken over"
else
  fail "oracle lock: live holder (exit $RC)" "$OUT"
fi
start=$(date +%s)
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE' LOCAL_CI_PORTS_WAIT=2 LOCAL_CI_PORTS_POLL=1
  lci_with_oracle_lock me true; echo a=\$?; lci_with_oracle_lock me true; echo b=\$? \$LOCK_ERROR"
if case "$OUT" in *a=99*b=99*"Oracle lock"*"stayed held"*) true ;; *) false ;; esac && [ $(($(date +%s) - start)) -lt 5 ]; then
  pass "oracle lock: once the wait timed out the later Oracle jobs fail at once with the same reason"
else
  fail "oracle lock: give-up" "$OUT"
fi
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE' LOCAL_CI_PORTS_WAIT=2 LOCAL_CI_PORTS_POLL=1 INTERRUPTED=1; lci_with_oracle_lock me sh -c 'echo JOB-RAN'; rc=\$?; echo rc=\$rc expected=\$LCI_LOCK_FAILED \$LOCK_ERROR"
rcline=$(printf '%s\n' "$OUT" | sed -n 's/^rc=\([0-9]*\) expected=\([0-9]*\) .*/\1=\2/p')
if [ -n "$rcline" ] && [ "${rcline%=*}" = "${rcline#*=}" ] && ! printf '%s' "$OUT" | grep -q JOB-RAN && printf '%s' "$OUT" | grep -q 'interrupted' && ! printf '%s' "$OUT" | grep -q 'stayed held'; then
  pass "oracle lock: a wait cut short by Ctrl-C says interrupted, with the named lock-failure code"
else
  fail "oracle lock: interrupted wait" "$OUT"
fi
rm -rf "$ORACLE"
write_lock_owner "$ORACLE" dead-oracle 999999 "Mon Oct 12 09:00:00 2026"
platform "LOCAL_CI_ORACLE_LOCK='$ORACLE' lci_with_oracle_lock me sh -c 'echo JOB-RAN'"
case "$OUT" in *JOB-RAN*) pass "oracle lock: the lock of a dead holder is taken over" ;; *) fail "oracle lock: dead holder" "$OUT" ;; esac
platform "unset LOCAL_CI_ORACLE_LOCK; TMPDIR=/var/tmp lci_oracle_lock_path"
if [ "$OUT" = /var/tmp/awe-local-ci-oracle.lock ]; then pass "oracle lock: it lives under TMPDIR"; else fail "oracle lock: path" "$OUT"; fi
rm -rf "$ORACLE"

# run_locked (bin/local-ci.sh) is run here as extracted from the script, with stub rows: "$1" sets stub variables (DEC, ROWRC,
# INTERRUPTED), "$2" is the code to run. It records its rows in $WORK/rows.tsv and the stub row prints ROW-RAN <row id>.
run_locked_with() {
  : > "$WORK/rows.tsv"
  body=$(sed -n '/^run_locked() *{/,/^}/p' "$BIN_DIR/local-ci.sh")
  platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE' LOCAL_CI_PORTS_WAIT=2 LOCAL_CI_PORTS_POLL=1
    ROOT=/x/checkout INTERRUPTED=0 DEC=run ROWRC=0; $1
    decision() { echo \"\$DEC\"; }
    blocking_of() { echo yes; }
    record() { printf '%s\\t%s\\t%s\\t%s\\t%s\\t%s\\n' \"\$@\" >> '$WORK/rows.tsv'; }
    run_row() { echo \"ROW-RAN \$2 (\$1)\"; return \$ROWRC; }
    $body
    $2"
}
rm -rf "$ORACLE"
run_locked_with '' 'run_locked no all-ut all-ut "All UT" 40 step_all_ut; echo rc=$?; [ ! -d "$LOCAL_CI_ORACLE_LOCK" ] && echo no-lock'
case "$OUT" in *"ROW-RAN all-ut (all-ut)"*rc=0*no-lock*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$text_ok" -eq 1 ] && [ ! -s "$WORK/rows.tsv" ]; then pass "run_locked: a row that is not Oracle runs at once, without any lock, even while the Oracle lock is held"; else fail "run_locked: plain row" "$OUT"; fi
write_lock_owner "$ORACLE" other-oracle "$$" "$LIVE_START"
run_locked_with '' 'run_locked no embedded-db r1 "R1" 40 s; echo rc=$?'
case "$OUT" in *"ROW-RAN r1"*rc=0*) pass "run_locked: a plain row is not held up by the Oracle lock of another run" ;; *) fail "run_locked: plain row with the Oracle lock held" "$OUT" ;; esac
run_locked_with '' 'run_locked yes db-oracle db-oracle-oracle "Oracle" 60 s; echo rc=$?'
if [ "$(cut -f1,3,4 "$WORK/rows.tsv")" = "$(printf 'db-oracle-oracle\tFAIL\tyes')" ] && ! printf '%s' "$OUT" | grep -q ROW-RAN \
  && case "$OUT" in *rc=1*) true ;; *) false ;; esac && grep -q 'stayed held by another run' "$WORK/rows.tsv"; then
  pass "run_locked: an Oracle row whose lock stays held is recorded as a blocking FAIL with the reason and does not run"
else
  fail "run_locked: Oracle lock failure" "$OUT $(cat "$WORK/rows.tsv")"
fi
start=$(date +%s)
run_locked_with '' 'run_locked yes db-oracle o1 "O1" 60 s; run_locked yes db-oracle o2 "O2" 60 s'
if [ "$(wc -l < "$WORK/rows.tsv" | tr -d ' ')" -eq 2 ] && [ "$(cut -f6 "$WORK/rows.tsv" | sort -u | wc -l | tr -d ' ')" -eq 1 ] && [ $(($(date +%s) - start)) -lt 5 ]; then
  pass "run_locked: after one Oracle wait timed out the next Oracle row fails fast with the same reason"
else
  fail "run_locked: later Oracle rows wait again" "$(cat "$WORK/rows.tsv")"
fi
rm -rf "$ORACLE"
run_locked_with 'DEC=skip' 'run_locked yes db-oracle o1 "O1" 60 s; echo rc=$?; [ ! -d "$LOCAL_CI_ORACLE_LOCK" ] && echo no-lock'
case "$OUT" in *"ROW-RAN o1"*rc=0*no-lock*) pass "run_locked: an Oracle job that is not selected goes straight to run_row (no lock)" ;; *) fail "run_locked: skipped Oracle" "$OUT" ;; esac
run_locked_with 'ROWRC=1' 'run_locked no all-ut all-ut "All UT" 40 s; echo rc=$?'
case "$OUT" in *rc=1*) pass "run_locked: the exit code of run_row is passed on" ;; *) fail "run_locked: exit code" "$OUT" ;; esac
run_locked_with '' 'run_row() { test -f "$LOCAL_CI_ORACLE_LOCK/owner" && echo HELD; return 0; }; run_locked yes db-oracle o1 "O1" 60 s; echo rc=$?; [ ! -d "$LOCAL_CI_ORACLE_LOCK" ] && echo released'
case "$OUT" in *HELD*rc=0*released*) pass "run_locked: an Oracle row runs holding the Oracle lock, released after" ;; *) fail "run_locked: held" "$OUT" ;; esac
run_locked_with 'INTERRUPTED=1' 'run_locked yes db-oracle o1 "O1" 60 s; run_locked yes db-oracle o2 "O2" 60 s'
if [ "$(cut -f3 "$WORK/rows.tsv" | sort -u)" = SKIP ] && [ "$(wc -l < "$WORK/rows.tsv" | tr -d ' ')" -eq 2 ] && grep -q interrupted "$WORK/rows.tsv"; then
  pass "run_locked: after Ctrl-C the Oracle rows left are skipped as interrupted, not recorded as lock failures"
else
  fail "run_locked: interrupted" "$(cat "$WORK/rows.tsv")"
fi
run_locked_with '' 'run_row() { INTERRUPTED=1; return $LCI_LOCK_FAILED; }; run_locked yes db-oracle o1 "O1" 60 s; echo rc=$?'
if [ ! -s "$WORK/rows.tsv" ]; then pass "run_locked: a job interrupted after it ran (exit equal to the lock-failure code) records no SKIP row"; else fail "run_locked: SKIP after a job that ran" "$(cat "$WORK/rows.tsv")"; fi
rm -rf "$ORACLE"

# The clone of the checkout for a job of the parallel pool: sources and build output, no .git, node_modules, website, logs of
# the run, private Maven repository; the JaCoCo data comes back, nothing else
CLONE_SRC="$WORK/clone-src"
rm -rf "$CLONE_SRC" "$WORK/clone-dst"
mkdir -p "$CLONE_SRC/.git" "$CLONE_SRC/node_modules/x" "$CLONE_SRC/website/docs" "$CLONE_SRC/website/scripts/rest-reference/data" "$CLONE_SRC/website/node_modules/z" "$CLONE_SRC/website/build" "$CLONE_SRC/website/.docusaurus" "$CLONE_SRC/target/local-ci/run" "$CLONE_SRC/.m2/repository/a" \
  "$CLONE_SRC/awe-tests/awe-boot/src" "$CLONE_SRC/awe-tests/awe-boot/target/classes" "$CLONE_SRC/awe-framework/awe-client-angular/node_modules/y"
for f in .git/HEAD node_modules/x/i.js website/docs/a.md website/scripts/rest-reference/data/openapi.json website/node_modules/z/i.js website/build/b.html website/.docusaurus/d.json target/local-ci/run/log .m2/repository/a/x.jar pom.xml awe-tests/awe-boot/src/A.java \
  awe-tests/awe-boot/target/classes/A.class awe-framework/awe-client-angular/node_modules/y/i.js awe-framework/awe-client-angular/package.json; do
  echo x > "$CLONE_SRC/$f"
done
touch -t 202001010000 "$CLONE_SRC/awe-tests/awe-boot/target/classes/A.class"
if command -v rsync >/dev/null 2>&1; then   # the CI image has no rsync (the run then goes one job at a time)
platform "lci_clone_work '$CLONE_SRC' '$WORK/clone-dst'"
cloned=$(cd "$WORK/clone-dst" && find . -type f | sort | tr '\n' ' ')
if [ "$RC" -eq 0 ] && [ "$cloned" = "./awe-framework/awe-client-angular/package.json ./awe-tests/awe-boot/src/A.java ./awe-tests/awe-boot/target/classes/A.class ./pom.xml ./website/docs/a.md ./website/scripts/rest-reference/data/openapi.json " ]; then
  pass "clone: sources, the whole website (tests read website/scripts/rest-reference/data/openapi.json) and build output are cloned, without .git, node_modules, website/build, website/.docusaurus, the logs of the run and the Maven repository"
else
  fail "clone: contents (exit $RC)" "$cloned $OUT"
fi
if [ "$(stat -f %m "$WORK/clone-dst/awe-tests/awe-boot/target/classes/A.class" 2>/dev/null || stat -c %Y "$WORK/clone-dst/awe-tests/awe-boot/target/classes/A.class")" = "$(stat -f %m "$CLONE_SRC/awe-tests/awe-boot/target/classes/A.class" 2>/dev/null || stat -c %Y "$CLONE_SRC/awe-tests/awe-boot/target/classes/A.class")" ]; then
  pass "clone: modification times are kept (Maven's incremental build sees the classes up to date)"
else
  fail "clone: modification times"
fi
mkdir -p "$WORK/clone-dst/awe-tests/awe-boot/target/reports" "$WORK/clone-dst/awe-framework/awe-model/target"
echo data > "$WORK/clone-dst/awe-tests/awe-boot/target/jacoco-h2-ut.exec"
echo data > "$WORK/clone-dst/awe-framework/awe-model/target/jacoco-h2-ut.exec"
echo other > "$WORK/clone-dst/awe-tests/awe-boot/target/jacoco-mysql-ut.exec"
echo report > "$WORK/clone-dst/awe-tests/awe-boot/target/reports/r.xml"
platform "lci_bring_back '$WORK/clone-dst' '$CLONE_SRC' jacoco-h2-ut.exec"
back=$(cd "$CLONE_SRC" && find . -name 'jacoco-*' -o -path '*/reports/*' | sort | tr '\n' ' ')
if [ "$RC" -eq 0 ] && [ "$back" = "./awe-framework/awe-model/target/jacoco-h2-ut.exec ./awe-tests/awe-boot/target/jacoco-h2-ut.exec " ]; then
  pass "clone: only the JaCoCo data of the job comes back to the checkout (every module, no other file)"
else
  fail "clone: bring back (exit $RC)" "$back $OUT"
fi
else
  pass "clone: skipped, rsync is not installed here (lci_parallel_width runs the jobs one at a time then)"
fi
rm -rf "$CLONE_SRC" "$WORK/clone-dst"

# bin/local-ci.sh wiring
if grep -q 'run_pool "\$DB_PARALLEL" db_item' "$BIN_DIR/local-ci.sh" && grep -q 'in_work all-ut jacoco-all-ut.exec' "$BIN_DIR/local-ci.sh"; then
  pass "pool: All UT, the embedded DB jobs and the database jobs run through run_pool, each in its own clone"
else
  fail "pool: local-ci.sh does not run the unit-testing jobs through run_pool"
fi
if grep -q '^DB_PARALLEL=\${LOCAL_CI_DB_PARALLEL:-3}' "$BIN_DIR/local-ci.sh"; then pass "pool: LOCAL_CI_DB_PARALLEL sets the width, 3 by default"; else fail "pool: default width"; fi
if grep -A12 '^cleanup()' "$BIN_DIR/local-ci.sh" | grep -q 'pool_stop' && grep -A12 '^cleanup()' "$BIN_DIR/local-ci.sh" | grep -q 'lci_locks_release'; then
  pass "pool: the cleanup (exit, signals) stops the workers and releases the locks"
else
  fail "pool: cleanup does not stop the workers"
fi
if grep -q -- '--fast) FAST=1' "$BIN_DIR/local-ci.sh" && grep -q 'LOCAL_CI_SCOPE:-}" = affected' "$BIN_DIR/local-ci.sh"; then
  pass "fast scope: --fast and LOCAL_CI_SCOPE=affected turn it on, and the default is the pipeline"
else
  fail "fast scope: option or variable missing"
fi
if grep -q '^FAST=0$' "$BIN_DIR/local-ci.sh"; then pass "fast scope: off by default"; else fail "fast scope: default"; fi
if grep -q 'unit_modules' "$BIN_DIR/local-ci.sh" && sed -n '/^unit_modules() {/,/^}/p' "$BIN_DIR/local-ci.sh" | grep -q 'pl awe-tests/awe-boot$'; then
  pass "fast scope: the database jobs run only the awe-boot module (default: the whole reactor as CI)"
else
  fail "fast scope: unit_modules"
fi
# The browser suites pass their own free ports to the application, the JMX goals and JaCoCo; no fixed 8090/9001 is left
step=$(sed -n '/^step_playwright() {/,/^}/p' "$BIN_DIR/local-ci.sh")
ports_ok=1
for token in '"-Djacoco.port=$JACOCO_PORT"' '"-Dawe.test.jmx-port=$JMX_PORT"' '--server.port=$APP_PORT' 'SERVER_PORT=$APP_PORT' '"-Dawe.test.server-port=$APP_PORT"'; do
  case "$step" in *"$token"*) ;; *) ports_ok=0 ;; esac
done
if [ "$ports_ok" -eq 1 ]; then pass "browser ports: the suite passes its application, JMX and JaCoCo ports to Maven"; else fail "browser ports: step_playwright" "$step"; fi
if ! grep -nE '(^|[^0-9])(8090|9001|18081)([^0-9]|$)' "$BIN_DIR/local-ci.sh" | grep -v '^[0-9]*:#'; then
  pass "browser ports: no fixed 8090, 9001 or 18081 left in the code of local-ci.sh"
else
  fail "browser ports: a fixed port is still in local-ci.sh" "$(grep -nE '(^|[^0-9])(8090|9001|18081)([^0-9]|$)' "$BIN_DIR/local-ci.sh" | grep -v '^[0-9]*:#')"
fi
# The poms of the application modules take the JMX port from a property (the plugin has none of its own for start and stop)
missing=
for pom in awe-tests/awe-boot/pom.xml awe-tests/awe-boot-react/pom.xml; do
  grep -q '<jmxPort>${awe.test.jmx-port}</jmxPort>' "$ROOT/$pom" && grep -q '<awe.test.jmx-port>9001</awe.test.jmx-port>' "$ROOT/$pom" || missing="$missing $pom"
done
if [ -z "$missing" ]; then pass "browser ports: awe-boot and awe-boot-react take the JMX port of spring-boot:start/stop from awe.test.jmx-port (9001 by default)"; else fail "browser ports: jmxPort property missing in$missing"; fi
# The browser phase holds a capacity slot, not a ports lock, and the suites stay serial in a run
if grep -q 'lci_browser_acquire' "$BIN_DIR/local-ci.sh" && grep -q 'lci_pick_ports' "$BIN_DIR/local-ci.sh"; then
  pass "browser phase: a capacity slot for the run and free ports for each suite"
else
  fail "browser phase: slot or ports"
fi

# --- Stopping only what the run started ---
# stop_app only stops what the run started: a listener that is a descendant of the script, or whose command line names the
# checkout (an orphaned test application), never a foreign process that grabbed the picked port
cat > "$WORK/listener2.mjs" <<'EOF'
import net from 'node:net';
const server = net.createServer().listen(Number(process.argv[2]), '127.0.0.1', () => console.log('listening'));
setTimeout(() => server.close(), 20000);
EOF
node "$WORK/listener2.mjs" 20011 > /dev/null 2>&1 &
FOREIGN=$!
sleep 1
platform_case "own process: a foreign listener is not ours" 1 "" "lci_is_ours $FOREIGN /nowhere/checkout 999999"
platform_case "own process: a descendant of the run is ours" 0 "" "sleep 5 & child=\$!; lci_is_ours \$child /nowhere/checkout \$\$; rc=\$?; kill \$child; exit \$rc"
mkdir -p "$WORK/co-root"
bash -c 'exec -a "$0" bash -c "sleep 5; true"' "java -cp $WORK/co-root/target/classes App" 2>/dev/null &
ORPHAN=$!
sleep 1
platform_case "own process: an orphan whose command line names the checkout is ours" 0 "" "lci_is_ours $ORPHAN '$WORK/co-root' 1"
kill "$ORPHAN" "$FOREIGN" 2>/dev/null || true
if sed -n '/^own_listeners() {/,/^}/p' "$BIN_DIR/local-ci.sh" | grep -q 'lci_is_ours' \
  && sed -n '/^stop_app() {/,/^}/p' "$BIN_DIR/local-ci.sh" | grep -q 'own_listeners' \
  && ! sed -n '/^stop_app() {/,/^}/p' "$BIN_DIR/local-ci.sh" | grep -q 'port_pids'; then
  pass "stop_app: it filters the listeners of the picked ports through lci_is_ours before any kill"
else
  fail "stop_app: no ownership check"
fi

# --- The Oracle lock between the workers of a run ---
# The Oracle lock is released only by the worker that took it
rm -rf "$ORACLE"
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE'; ( lci_with_oracle_lock a sh -c 'sleep 2' ) & sleep 1; ( lci_oracle_release ); [ -d '$ORACLE' ] && echo still-held; wait"
case "$OUT" in *still-held*) pass "oracle lock: a worker that did not take the lock never releases the lock of another worker of the run" ;; *) fail "oracle lock: foreign release in a run" "$OUT" ;; esac
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE' LOCAL_CI_PORTS_POLL=1 LOCAL_CI_PORTS_WAIT=10; ( lci_with_oracle_lock a sh -c 'sleep 2; echo A-done >> $WORK/ora.out' ) & sleep 0.5; ( lci_with_oracle_lock b sh -c 'echo B-ran >> $WORK/ora.out' ); wait; cat $WORK/ora.out"
if [ "$(printf '%s\n' "$OUT" | grep -E '^(A-done|B-ran)$' | tr '\n' ' ')" = "A-done B-ran " ]; then pass "oracle lock: two workers of one run take turns"; else fail "oracle lock: two workers" "$OUT"; fi
rm -rf "$ORACLE" "$WORK/ora.out"

# The give-up is shared by the workers through a file of the run: the first worker waits 2 s and gives up, the second would wait 30 s
# by itself, so returning at once proves that it read the first one's give-up
GIVEN="$WORK/oracle-given-up"
rm -f "$GIVEN"
write_lock_owner "$ORACLE" other-oracle "$$" "$LIVE_START"
start=$(date +%s)
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE' LOCAL_CI_PORTS_POLL=1 LCI_ORACLE_GIVEN_UP_FILE='$GIVEN'; ( export LOCAL_CI_PORTS_WAIT=2; lci_with_oracle_lock w1 true ); echo first=\$?; export LOCAL_CI_PORTS_WAIT=30; ( lci_with_oracle_lock w2 true; echo second=\$? \$LOCK_ERROR )"
elapsed=$(($(date +%s) - start))
if case "$OUT" in *first=99*second=99*"stayed held"*) true ;; *) false ;; esac && [ -s "$GIVEN" ] && [ "$elapsed" -lt 10 ]; then
  pass "oracle lock: a timeout in one worker makes the other Oracle rows fail at once, shared through a file of the run (${elapsed}s, the second had 30s)"
else
  fail "oracle lock: give-up shared across workers (${elapsed}s)" "$OUT"
fi
rm -rf "$ORACLE" "$GIVEN"

# --- The pool stops everything ---
# pool_stop escalates to KILL and waits: nothing outlives the script
body=$(awk '/^POOL_PIDS=/ { p = 1 } p { print } /^run_pool\(\)/ { inrun = 1 } inrun && /^}/ { exit }' "$BIN_DIR/local-ci.sh")
run bash -c ". '$PLATFORM'
  LOG_DIR='$WORK' LOCAL_CI_POOL_GRACE=2 INTERRUPTED=0
  $body
  ( trap '' TERM; sleep 30 & wait ) > /dev/null 2>&1 &
  POOL_PIDS[1]=\$!; POOL_N=1
  sleep 1
  wk=\${POOL_PIDS[1]}
  child=\$(ps -ax -o pid=,ppid= | awk -v p=\$wk '\$2 == p { print \$1 }' | head -1)
  start=\$SECONDS
  pool_stop
  echo elapsed=\$((SECONDS - start))
  kill -0 \$wk 2>/dev/null && echo worker-alive
  [ -n \"\$child\" ] && kill -0 \$child 2>/dev/null && echo child-alive
  echo done"
case "$OUT" in *worker-alive*|*child-alive*) fail "pool_stop: a worker that ignores TERM outlived the pool" "$OUT" ;; *done*) pass "pool_stop: a worker that ignores TERM (and its child) is killed after the grace period, and waited for" ;; *) fail "pool_stop: harness" "$OUT" ;; esac

# A run that is terminated (INT, TERM, or INT then TERM) stops every worker and everything the workers started before the main
# script exits. The driver below runs the real on_interrupt, cleanup, kill_group and pool functions of local-ci.sh with stub
# workers: one that ignores INT and TERM with a child of its own, one that waits for a foreground child (bash runs its trap
# only when the child ends), and one that exits on TERM.
DRIVER="$WORK/term-driver.sh"
{
  printf '. "%s"\n' "$PLATFORM"
  printf 'LOG_DIR="%s/term" LOCAL_CI_POOL_GRACE=2 LOCAL_CI_POOL_POLL=0.2 INTERRUPTED=0 CURRENT_PID= LOCK_DIR="%s/term/lock" WEBSITE_TOUCHED=0\n' "$WORK" "$WORK"
  printf 'PIDS="%s/term/pids"\n' "$WORK"
  printf 'stop_app() { :; }\nlci_locks_release() { :; }\nlci_lock_release() { :; }\nrecord() { :; }\n'
  awk '/^kill_group\(\)/ { p = 1 } p { print } p && /^}/ { p = 0 }' "$BIN_DIR/local-ci.sh"
  awk '/^cleanup\(\)/ { p = 1 } p { print } p && /^trap cleanup EXIT/ { exit }' "$BIN_DIR/local-ci.sh"
  awk '/^POOL_PIDS=/ { p = 1 } p { print } /^run_pool\(\)/ { inrun = 1 } inrun && /^}/ { exit }' "$BIN_DIR/local-ci.sh"
  cat <<'EOS'
me() { sh -c 'echo $PPID'; }   # the pid of this shell (bash 3.2 of macOS has no BASHPID)
w() {
  case "$1" in
    ignore) trap '' INT TERM; sleep 60 & echo "$(me) $!" >> "$PIDS"; wait ;;
    foreground) me >> "$PIDS"; sleep 60 ;;
    polite) me >> "$PIDS"; while [ "$INTERRUPTED" != 1 ]; do sleep 0.2; done ;;
  esac
}
run_pool 3 w ignore foreground polite
cleanup
trap - EXIT
EOS
} > "$DRIVER"
term_case() { # "$1" name, "$2" the signals, one every 0.3 seconds
  rm -rf "$WORK/term"
  mkdir -p "$WORK/term"
  set -m   # a background job without job control ignores INT: as a command in a terminal, the driver must receive it
  bash "$DRIVER" > "$WORK/term/out" 2>&1 < /dev/null &
  drv=$!
  set +m
  sleep 1.5
  if [ "$2" = INT ] && [ -r "/proc/$drv/status" ]; then   # an INT that is ignored from the start cannot be trapped (BusyBox ash has no job control here)
    ign=$(sed -n 's/^SigIgn:[[:space:]]*//p' "/proc/$drv/status")
    if [ $(( 0x$ign & 2 )) -ne 0 ]; then
      kill -KILL "$drv" 2>/dev/null; pkill -KILL -f 'sleep 60' 2>/dev/null
      pass "terminate: $1: skipped, this shell starts background jobs with INT ignored"
      return 0
    fi
  fi
  for sig in $2; do kill "-$sig" "$drv" 2>/dev/null; sleep 0.3; done
  t0=$(date +%s)
  while kill -0 "$drv" 2>/dev/null && [ $(( $(date +%s) - t0 )) -lt 12 ]; do sleep 0.2; done
  hung=no
  kill -0 "$drv" 2>/dev/null && { hung=yes; kill -KILL "$drv" 2>/dev/null; }
  sleep 0.3
  alive=
  for pid in $(cat "$WORK/term/pids" 2>/dev/null); do
    state=$(sed 's/.*) //' "/proc/$pid/stat" 2>/dev/null | cut -c1)
    if kill -0 "$pid" 2>/dev/null && [ "$state" != Z ]; then alive="$alive $pid"; kill -KILL "$pid" 2>/dev/null; fi
  done
  if [ "$hung" = no ] && [ -z "$alive" ] && [ "$(wc -w < "$WORK/term/pids" | tr -d ' ')" -ge 4 ]; then
    pass "terminate: $1: every worker and its children are gone when the script exits"
  else
    fail "terminate: $1 (still running:${alive:- none}, script hung: $hung)" "$(cat "$WORK/term/out")"
  fi
}
term_case "TERM" "TERM"
term_case "INT" "INT"
term_case "INT then TERM" "INT TERM"
term_case "TERM twice" "TERM TERM"

# --- Parallel width ---
# DB_PARALLEL without rsync falls back to serial, with a warning
mkdir -p "$WORK/emptybin"
platform "PATH='$WORK/emptybin'; command -v rsync >/dev/null 2>&1 && echo has-rsync; echo width=\$(lci_parallel_width 3)"
case "$OUT" in *has-rsync*) fail "parallel width: the PATH of the case still has rsync" "$OUT" ;; *width=1*) pass "parallel width: without rsync the unit-testing jobs run one at a time" ;; *) fail "parallel width: without rsync" "$OUT" ;; esac
cat > "$STUBS/rsync" <<'EOF'
#!/bin/sh
exit 0
EOF
chmod +x "$STUBS/rsync"
platform_case "parallel width: with rsync the width asked for is kept" 0 "width=3" "PATH=\"$STUBS:\$PATH\"; echo width=\$(lci_parallel_width 3)"
platform_case "parallel width: one is one, rsync or not" 0 "width=1" "PATH='/nonexistent'; echo width=\$(lci_parallel_width 1)"
platform_case "parallel width: the warning names rsync and the serial fallback" 0 "rsync" "PATH='/nonexistent'; lci_parallel_width 3 2>&1 >/dev/null"

# --- Clones, Docker rows, test port ---
if sed -n '/^db_item() {/,/^}/p' "$BIN_DIR/local-ci.sh" | sed -n '/nodocker)/,/;;/p' | grep -q 'after_build'; then pass "db_item: the no-Docker row goes through after_build"; else fail "db_item: nodocker without after_build"; fi
if ! grep -q 'System.getProperty' "$ROOT/awe-tests/awe-boot/src/test/java/com/almis/awe/config/TestServerPortEnvironmentPostProcessor.java"; then pass "test port: no redundant System.getProperty guard"; else fail "test port: guard still there"; fi

# --- The Maven arguments of the unit-testing rows are the ones of the CI jobs ---
# All UT runs the reactor of CI and every database row only awe-boot (#863): a test count that differs from the CI job is a
# regression. The builders are run here as extracted from the script.
ut_args_with() { # "$1" FAST value, "$2" code
  run bash -c "
    FAST=$1
    UT_FLAGS=(-Dskip.frontend=true -DgenerateUnitReport=true)
    UT_OPTS=(\"\${UT_FLAGS[@]}\" -pl '!awe-framework/awe-boot-angular-archetype' -pl '!awe-framework/awe-boot-react-archetype')
    $(sed -n '/^unit_modules() {/,/^}/p;/^all_ut_args() {/,/^}/p;/^unit_profile_args() {/,/^}/p' "$BIN_DIR/local-ci.sh")
    $2"
}
ut_args_with 0 'all_ut_args | tr "\n" " "'
if [ "$OUT" = "-Dskip.frontend=true -DgenerateUnitReport=true -pl !awe-framework/awe-boot-angular-archetype -pl !awe-framework/awe-boot-react-archetype -Djacoco.file.name=jacoco-all-ut.exec -Dawe.test.playwright.required=true " ]; then
  pass "unit rows: All UT runs the CI module set (the reactor without the two archetypes) with the flags it always had"
else
  fail "unit rows: All UT arguments" "$OUT"
fi
for profile in h2 mysql oracle-flyway sqlserver; do
  ut_args_with 0 "unit_profile_args $profile | tr '\\n' ' '"
  if [ "$OUT" = "-Dskip.frontend=true -DgenerateUnitReport=true -pl awe-tests/awe-boot -P$profile -Djacoco.file.name=jacoco-$profile-ut.exec -Doracle.jdbc.J2EE13Compliant=true -Doracle.jdbc.timezoneAsRegion=false " ]; then
    pass "unit rows: the $profile row runs only awe-boot, as the database job of CI (#863)"
  else
    fail "unit rows: $profile arguments" "$OUT"
  fi
done
ut_args_with 1 'unit_profile_args mysql | tr "\n" " "'
case "$OUT" in *"-pl awe-tests/awe-boot -Pmysql"*) pass "unit rows: --fast keeps the database rows on awe-boot" ;; *) fail "unit rows: fast modules" "$OUT" ;; esac
ut_args_with 1 'all_ut_args | tr "\n" " "'
case "$OUT" in *"-pl !awe-framework/awe-boot-angular-archetype"*) pass "unit rows: --fast leaves All UT on the CI module set" ;; *) fail "unit rows: All UT under fast" "$OUT" ;; esac
# The database rows mirror the database jobs of .gitlab-ci.yml: they run awe-boot alone, never the reactor
if grep -q -- '- mvn -f awe-tests/awe-boot/pom.xml $MAVEN_CLI_OPTS verify -Dskip.frontend=true -DgenerateUnitReport=true -P$TEST_NAME' "$ROOT/.gitlab-ci.yml"; then
  pass "unit rows: the database jobs of CI run only awe-boot too"
else
  fail "unit rows: the database jobs of .gitlab-ci.yml no longer run only awe-boot"
fi

# --- Whose process is it: the checkout as a whole path component ---
OWN_ROOT="$WORK/co-root"
mkdir -p "$OWN_ROOT"
own_case() { # "$1" name, "$2" command line of the process, "$3" 0 when it must be ours, 1 when not
  bash -c 'exec -a "$0" bash -c "sleep 6; true"' "$2" 2>/dev/null &
  pid=$!
  sleep 0.5
  platform "lci_is_ours $pid '$OWN_ROOT' 1"
  kill "$pid" 2>/dev/null || true
  if [ "$RC" -eq "$3" ]; then pass "own process: $1"; else fail "own process: $1 (exit $RC, expected $3)"; fi
}
own_case "a test application of the checkout is ours" "java -cp $OWN_ROOT/target/classes App" 0
own_case "a test application of a worktree nested under the checkout is not ours" "java -cp $OWN_ROOT/.claude/worktrees/w1/target/classes App" 1
own_case "a sibling that only shares the prefix of the checkout path is not ours" "java -cp $OWN_ROOT-2/target/classes App" 1
own_case "a checkout path that is only the tail of a longer path is not ours" "java -cp /elsewhere$OWN_ROOT/target/classes App" 1
own_case "a process with a nested worktree and the checkout itself in its command line is ours" "java -cp $OWN_ROOT/.claude/worktrees/w1/a.jar:$OWN_ROOT/target/classes App" 0

# --- The locks of a run: only the top-level cleanup forces a release ---
rm -rf "$ORACLE"
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE'; ( lci_with_oracle_lock a sh -c 'sleep 2' ) & sleep 1; ( LCI_WORKER=1; lci_locks_release ); [ -d '$ORACLE' ] && echo still-held; ( LCI_WORKER=1; lci_locks_release force ); [ -d '$ORACLE' ] && echo still-held-after-force; wait"
case "$OUT" in *still-held*still-held-after-force*) pass "oracle lock: a worker that ends its job does not release the lock of a sibling, not even with force" ;; *) fail "oracle lock: worker release" "$OUT" ;; esac
platform "export LOCAL_CI_ORACLE_LOCK='$ORACLE'; mkdir '$ORACLE'; printf 'a\n%s\n' \$\$ > '$ORACLE/owner'; lci_locks_release; [ -d '$ORACLE' ] && echo kept; lci_locks_release force; [ ! -d '$ORACLE' ] && echo cleaned"
case "$OUT" in *kept*cleaned*) pass "oracle lock: the top-level cleanup (force, no worker left) removes the lock of this run" ;; *) fail "oracle lock: top-level force" "$OUT" ;; esac
rm -rf "$ORACLE"

# --- Clones of earlier runs: only the ones of dead runs are pruned ---
LCI_DIR="$WORK/lcidir"
rm -rf "$LCI_DIR"
for run in live dead current noowner; do mkdir -p "$LCI_DIR/$run/work/row" "$LCI_DIR/$run/keep"; done
printf '%s\n%s\n' "$$" "$LIVE_START" > "$LCI_DIR/live/owner"
printf '%s\n%s\n' 999999 "Mon Oct 12 09:00:00 2026" > "$LCI_DIR/dead/owner"
printf '%s\n%s\n' "$$" "$LIVE_START" > "$LCI_DIR/current/owner"
platform "lci_prune_work '$LCI_DIR' '$LCI_DIR/current'"
if [ "$RC" -eq 0 ] && [ -d "$LCI_DIR/live/work" ] && [ -d "$LCI_DIR/current/work" ] && [ ! -d "$LCI_DIR/dead/work" ] && [ ! -d "$LCI_DIR/noowner/work" ] \
  && [ -d "$LCI_DIR/dead/keep" ]; then
  pass "clones: the clones of a live run and of this run stay; those of dead runs (or without an owner) are deleted, their logs are kept"
else
  fail "clones: pruning (exit $RC)" "$(find "$LCI_DIR" -maxdepth 2 | sort | tr '\n' ' ')"
fi
if grep -q 'lci_prune_work' "$BIN_DIR/local-ci.sh" && ! grep -q 'rm -rf "\$ROOT"/target/local-ci/\*/work' "$BIN_DIR/local-ci.sh" \
  && grep -q 'lci_lock_write_owner "\$LOG_DIR"' "$BIN_DIR/local-ci.sh"; then
  pass "clones: a run records its owner in its folder and prunes through lci_prune_work"
else
  fail "clones: local-ci.sh does not record the owner or prune through lci_prune_work"
fi

# --- Small settings ---
platform_case "pool grace: 25 seconds by default" 0 "grace=25" "unset LOCAL_CI_POOL_GRACE; echo grace=\$(lci_pool_grace)"
platform_case "pool grace: LOCAL_CI_POOL_GRACE sets it" 0 "grace=5" "LOCAL_CI_POOL_GRACE=5; echo grace=\$(lci_pool_grace)"
platform_case "pool grace: a nonsense value falls back to the default" 0 "grace=25" "LOCAL_CI_POOL_GRACE=soon; echo grace=\$(lci_pool_grace)"
if [ "$(grep -c '^LCI_ORACLE_GIVEN_UP_FILE=$' "$BIN_DIR/local-ci.sh")" -eq 0 ] && grep -q 'LOCAL_CI_POOL_GRACE' "$ROOT/CONTRIBUTING.md"; then
  pass "settings: no redundant empty LCI_ORACLE_GIVEN_UP_FILE, and LOCAL_CI_POOL_GRACE is documented"
else
  fail "settings: LCI_ORACLE_GIVEN_UP_FILE or LOCAL_CI_POOL_GRACE docs"
fi

# No job takes a ports lock any more
if ! grep -q 'lci_ports_\|lci_with_machine_locks' "$BIN_DIR/local-ci.sh" "$BIN_DIR/local-ci-platform.sh"; then
  pass "locks: no machine-wide ports lock is left (the ports are chosen free per run)"
else
  fail "locks: the ports lock is still referenced" "$(grep -n 'lci_ports_\|lci_with_machine_locks' "$BIN_DIR/local-ci.sh" "$BIN_DIR/local-ci-platform.sh")"
fi

# Browser capacity: at most LOCAL_CI_BROWSER_PARALLEL browser applications at a time on the machine (slot locks, one per
# application, in any checkout). A run holds one slot for its whole browser phase.
BROWSER="$WORK/browser"
slots_env="export LOCAL_CI_BROWSER_LOCK='$BROWSER' LOCAL_CI_PORTS_WAIT=2 LOCAL_CI_PORTS_POLL=1"
rm -rf "$BROWSER".*.lock
platform "$slots_env; lci_browser_acquire me && echo slot=\$(basename \"\$BROWSER_SLOT\") && lci_browser_release && [ ! -d \"$BROWSER.1.lock\" ] && echo released"
case "$OUT" in *"slot=browser.1.lock"*released*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "browser slots: a free slot is taken and released by its holder"; else fail "browser slots: take and release (exit $RC)" "$OUT"; fi
write_lock_owner "$BROWSER.1.lock" other-run "$$" "$LIVE_START"
platform "$slots_env; LOCAL_CI_BROWSER_PARALLEL=2 lci_browser_acquire me && echo slot=\$(basename \"\$BROWSER_SLOT\")"
case "$OUT" in *"slot=browser.2.lock"*) pass "browser slots: with a slot held by another run the next one is taken (capacity 2)" ;; *) fail "browser slots: second slot" "$OUT" ;; esac
rm -rf "$BROWSER.2.lock"
start=$(date +%s)
platform "$slots_env; LOCAL_CI_BROWSER_PARALLEL=1 lci_browser_acquire me"
case "$OUT" in *"waiting for a browser slot"*"other-run"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -ne 0 ] && [ "$text_ok" -eq 1 ] && [ ! -d "$BROWSER.2.lock" ] && [ "$(sed -n 1p "$BROWSER.1.lock/owner")" = other-run ]; then
  pass "browser slots: with every slot held (capacity 1) a run waits (bounded) and never takes a live holder's slot"
else
  fail "browser slots: capacity reached (exit $RC)" "$OUT"
fi
platform "$slots_env; LOCAL_CI_BROWSER_PARALLEL=1 INTERRUPTED=1 lci_browser_acquire me"
if [ "$RC" -ne 0 ] && [ $(($(date +%s) - start)) -lt 8 ]; then pass "browser slots: Ctrl-C ends the wait"; else fail "browser slots: interrupted wait" "$OUT"; fi
rm -rf "$BROWSER".*.lock
write_lock_owner "$BROWSER.1.lock" dead-run 999999 "Mon Oct 12 09:00:00 2026"
platform "$slots_env; LOCAL_CI_BROWSER_PARALLEL=1 lci_browser_acquire me && echo got"
case "$OUT" in *got*) pass "browser slots: the slot of a dead run is taken over" ;; *) fail "browser slots: dead holder" "$OUT" ;; esac
rm -rf "$BROWSER".*.lock
platform "$slots_env; lci_browser_release; echo ok"
write_lock_owner "$BROWSER.1.lock" other-run "$$" "$LIVE_START"
platform "$slots_env; BROWSER_SLOT='$BROWSER.1.lock'; lci_browser_release"
if [ -d "$BROWSER.1.lock" ]; then pass "browser slots: a run never releases a slot another run holds"; else fail "browser slots: foreign release"; fi
rm -rf "$BROWSER".*.lock
platform_case "browser slots: two at a time by default" 0 "capacity=2" "unset LOCAL_CI_BROWSER_PARALLEL; echo capacity=\$(lci_browser_parallel)"
platform_case "browser slots: the capacity is LOCAL_CI_BROWSER_PARALLEL" 0 "capacity=4" "LOCAL_CI_BROWSER_PARALLEL=4; echo capacity=\$(lci_browser_parallel)"
platform_case "browser slots: a nonsense capacity falls back to the default" 0 "capacity=2" "LOCAL_CI_BROWSER_PARALLEL=lots; echo capacity=\$(lci_browser_parallel)"
platform_case "browser slots: a zero capacity falls back to the default" 0 "capacity=2" "LOCAL_CI_BROWSER_PARALLEL=0; echo capacity=\$(lci_browser_parallel)"

# Free ports per run: the application, its JMX port (spring-boot:start) and the JaCoCo port of a browser suite. Never 8080
# (the port of the application the developer runs), never a port in use, three different ones.
platform_case "ports: a draw of 8080 is never accepted" 0 "port=20001" \
  "lci_random_port() { if [ -f '$WORK/drawn' ]; then echo 20001; else touch '$WORK/drawn'; echo 8080; fi; }; lci_port_free() { return 0; }; echo port=\$(lci_pick_port)"
rm -f "$WORK/drawn"
rm -f "$WORK/n"
platform_case "ports: a port in use is skipped" 0 "port=20002" \
  "lci_random_port() { n=\$(cat '$WORK/n' 2>/dev/null || echo 0); n=\$((n + 1)); echo \$n > '$WORK/n'; echo \$((20000 + n)); }; lci_port_free() { [ \"\$1\" -ge 20002 ]; }; echo port=\$(lci_pick_port)"
rm -f "$WORK/n"
platform_case "ports: a port to avoid is skipped" 0 "port=20003" \
  "lci_random_port() { n=\$(cat '$WORK/n' 2>/dev/null || echo 0); n=\$((n + 1)); echo \$n > '$WORK/n'; echo \$((20000 + n)); }; lci_port_free() { return 0; }; echo port=\$(lci_pick_port 20001 20002)"
platform_case "ports: no free port is an error, not a loop" 1 "" \
  "lci_random_port() { echo 20000; }; lci_port_free() { return 1; }; lci_pick_port"
platform "lci_pick_ports"'; echo "$APP_PORT $JMX_PORT $JACOCO_PORT"'
set -- $OUT
if [ "$#" -eq 3 ] && [ "$1" != "$2" ] && [ "$1" != "$3" ] && [ "$2" != "$3" ] && [ "$1" -ne 8080 ] && [ "$2" -ne 8080 ] && [ "$3" -ne 8080 ] \
  && [ "$1" -ge 20000 ] && [ "$3" -lt 30000 ]; then
  pass "ports: the application, JMX and JaCoCo ports are three different free ports (not 8080)"
else
  fail "ports: lci_pick_ports" "$OUT"
fi
platform 'for i in $(seq 1 40); do lci_pick_ports; echo "$APP_PORT $JMX_PORT $JACOCO_PORT"; done'
if [ "$(printf '%s\n' "$OUT" | tr ' ' '\n' | grep -cx 8080)" -eq 0 ] && [ "$(printf '%s\n' "$OUT" | awk '{ if ($1 == $2 || $1 == $3 || $2 == $3) bad++ } END { print bad + 0 }')" -eq 0 ]; then
  pass "ports: 40 draws, never 8080 and never two equal ports of one run"
else
  fail "ports: repeated draws" "$OUT"
fi
cat > "$WORK/listener.mjs" <<'EOF'
import net from 'node:net';
const server = net.createServer().listen(Number(process.argv[2]), '127.0.0.1', () => console.log('listening'));
setTimeout(() => server.close(), 15000);
EOF
node "$WORK/listener.mjs" 20007 > "$WORK/listener.out" 2>&1 &
LISTENER=$!
sleep 1
platform_case "ports: a port that something listens on is not free" 1 "" "lci_port_free 20007"
platform_case "ports: a port nothing listens on is free" 0 "" "lci_port_free 20008"
kill "$LISTENER" 2>/dev/null || true

# The pool (bin/local-ci.sh run_pool) is run here as extracted from the script, with stub workers. Each worker records
# itself in $RESULTS; the pool runs them in subshells with a results file and an output file of their own.
pool_with() { # "$1" bash code run after the stubs
  : > "$WORK/pool-results.tsv"
  rm -rf "$WORK/pool" "$WORK/running"
  mkdir -p "$WORK/pool" "$WORK/running"
  body=$(awk '/^POOL_PIDS=/ { p = 1 } p { print } /^run_pool\(\)/ { inrun = 1 } inrun && /^}/ { exit }' "$BIN_DIR/local-ci.sh")
  run bash -c "
    LOG_DIR='$WORK/pool' RESULTS='$WORK/pool-results.tsv' INTERRUPTED=0 LOCAL_CI_POOL_POLL=0.2
    on_interrupt() { INTERRUPTED=1; }
    trap on_interrupt INT TERM
    record() { printf '%s\\t%s\\t%s\\t%s\\t%s\\t%s\\n' \"\$@\" >> \"\$RESULTS\"; }
    $body
    $1"
}
start=$(date +%s)
pool_with 'w() { touch "'$WORK'/running/$1"; ls "'$WORK'/running" | wc -l >> "'$WORK'/concurrency"; sleep 1; rm -f "'$WORK'/running/$1"; record "$1" "$1" PASS yes 1 ""; echo "out-$1"; }
  run_pool 2 w a b c d e f; echo rc=$?'
elapsed=$(($(date +%s) - start))
peak=$(sort -n "$WORK/concurrency" | tail -1)
if [ "$peak" -eq 2 ] && [ "$elapsed" -lt 5 ] && [ "$(wc -l < "$WORK/pool-results.tsv" | tr -d ' ')" -eq 6 ] && case "$OUT" in *rc=0*) true ;; *) false ;; esac; then
  pass "pool: six workers run two at a time (peak $peak, ${elapsed}s for six one-second workers) and all six results are collected"
else
  fail "pool: bounded concurrency (peak $peak, ${elapsed}s)" "$OUT $(cat "$WORK/pool-results.tsv")"
fi
rm -f "$WORK/concurrency"
for item in a b c d e f; do printf '%s\n' "$OUT" | grep -qx "out-$item" || { fail "pool: output of worker $item shown"; break; }; done
pool_with 'w() { [ "$1" = slow ] && sleep 2; record "$1" "$1" PASS yes 1 ""; }; run_pool 2 w slow fast'
if [ "$(cut -f1 "$WORK/pool-results.tsv" | tr '\n' ' ')" = "slow fast " ]; then
  pass "pool: the results keep the order of the items, not the order of completion"
else
  fail "pool: result order" "$(cat "$WORK/pool-results.tsv")"
fi
pool_with 'w() { if [ "$1" = bad ]; then record bad bad FAIL yes 1 boom; return 1; fi; sleep 1; record "$1" "$1" PASS yes 1 ""; }; run_pool 3 w ok1 bad ok2; echo rc=$?'
if [ "$(cut -f1,3 "$WORK/pool-results.tsv" | tr '\t\n' ': ')" = "ok1:PASS bad:FAIL ok2:PASS " ] && case "$OUT" in *rc=0*) true ;; *) false ;; esac; then
  pass "pool: a failing worker does not stop the others, and each result is its own"
else
  fail "pool: failure isolation" "$OUT $(cat "$WORK/pool-results.tsv")"
fi
pool_with 'w() { echo "start-$1" >> "'$WORK'/order"; record "$1" "$1" PASS yes 1 ""; }; run_pool 1 w x y z'
if [ "$(cut -f1 "$WORK/pool-results.tsv" | tr '\n' ' ')" = "x y z " ]; then pass "pool: with one slot the workers run one after another, in order"; else fail "pool: serial" "$(cat "$WORK/pool-results.tsv")"; fi
rm -f "$WORK/order"
# Ctrl-C: the pool tells every running worker (their own handler stops their step) and waits for them
pool_with 'w() { trap "INTERRUPTED=1" INT TERM; sh -c "echo \$PPID" >> "'$WORK'/pool/worker-pids"; n=0; while [ "$INTERRUPTED" != 1 ] && [ $n -lt 100 ]; do sleep 0.2; n=$((n + 1)); done;
    [ "$INTERRUPTED" = 1 ] && record "$1" "$1" SKIP yes 0 interrupted || record "$1" "$1" PASS yes 20 ""; }
  ( sleep 1.5; kill -TERM $$ ) &
  run_pool 3 w p q r s; echo rc=$?'
if [ "$(grep -c "interrupted" "$WORK/pool-results.tsv")" -eq 4 ] && [ "$(wc -l < "$WORK/pool-results.tsv" | tr -d ' ')" -eq 4 ] \
  && [ "$(wc -l < "$WORK/pool/worker-pids" | tr -d ' ')" -eq 4 ] && ! kill -0 $(cat "$WORK/pool/worker-pids") 2>/dev/null; then
  pass "pool: Ctrl-C stops every running worker, the rest are launched interrupted, all four rows recorded, no child left"
else
  fail "pool: interrupt" "$OUT $(cat "$WORK/pool-results.tsv")"
fi

# The change set against the "fast" scope (--fast, LOCAL_CI_SCOPE=affected): opt-in, narrower than the merge request pipeline
fast_selected() { # "$1" paths
  printf '%s\n' "$1" > "$WORK/changed.txt"
  printf ' %s ' "$(node "$TOOL" select "$CI_FILE" "$WORK/changed.txt" --fast | awk -F '\t' '$2 == "yes" { printf "%s ", $1 }')"
}
fast_case() { # "$1" name, "$2" paths, "$3" must be selected, "$4" must not
  got=$(fast_selected "$2")
  problems=
  for job in $3; do case "$got" in *" $job "*) ;; *) problems="$problems missing:$job" ;; esac; done
  for job in $4; do case "$got" in *" $job "*) problems="$problems unexpected:$job" ;; esac; done
  if [ -z "$problems" ]; then pass "fast scope: $1"; else fail "fast scope: $1 ($problems)" "selected:$got"; fi
}
fast_case "a change of .gitlab-ci.yml alone keeps the checks and drops the heavy jobs" ".gitlab-ci.yml" \
  "mr-size mr-docs mr-checks-tests spotless" "embedded-db db-mysql db-postgresql db-sqlserver db-oracle playwright-angular playwright-react all-ut javadoc"
fast_case "a React client change runs the React suites, not the AngularJS ones nor the databases" "awe-framework/awe-client-react/src/A.jsx" \
  "playwright-react frontend-ut-react build" "playwright-angular embedded-db db-oracle all-ut"
fast_case "an AngularJS client change runs the AngularJS suites, not the React ones nor the databases" "awe-framework/awe-client-angular/src/main/js/a.js" \
  "playwright-angular frontend-ut build" "playwright-react embedded-db db-mysql"
fast_case "a backend change keeps everything the merge request pipeline runs" "awe-framework/awe-controller/src/main/java/A.java" \
  "all-ut embedded-db db-mysql db-postgresql db-sqlserver db-oracle playwright-angular playwright-react javadoc" ""
fast_case "a sample application change skips the databases (they test awe-boot and the framework)" "awe-samples/a/src/main/java/A.java" \
  "all-ut build" "embedded-db db-mysql db-postgresql db-sqlserver db-oracle"
fast_case "a change of awe-boot test code runs the databases and the AngularJS suites only" "awe-tests/awe-boot/src/test/java/A.java" \
  "embedded-db db-mysql playwright-angular" "playwright-react"
fast_case "a docs change still runs nothing heavy" "website/docs/a.md" "website" "embedded-db playwright-angular all-ut build"

# The default is the merge request pipeline: the same paths select more without --fast
selection_case "default: the CI file alone runs the databases and both browser engines, as the pipeline does" ".gitlab-ci.yml" \
  "embedded-db db-mysql db-postgresql db-sqlserver db-oracle playwright-angular playwright-react all-ut javadoc" ""
selection_case "default: a change of bin/ scripts and docs only (no Java) runs no database job" \
  "bin/local-ci.sh
bin/local-ci-platform.sh
CONTRIBUTING.md
website/docs/guides/a.md" \
  "mr-checks-tests website" "embedded-db db-mysql db-postgresql db-sqlserver db-oracle all-ut build playwright-angular"
selection_case "default: the CI file with docs and scripts runs the databases, because the pipeline does (it is in their rules:changes)" \
  ".gitlab-ci.yml
bin/local-ci.sh
CONTRIBUTING.md" "embedded-db db-mysql db-oracle build" ""
selection_case "default: an AngularJS client file runs the AngularJS suites and no React ones" "awe-framework/awe-client-angular/src/main/js/a.js" \
  "playwright-angular" "playwright-react embedded-db"
selection_case "default: a React-only client change runs the AngularJS browser suites as well (the pipeline does: .rules-playwright)" \
  "awe-framework/awe-client-react/src/A.jsx" "playwright-angular playwright-react" "embedded-db all-ut"
selection_case "default: an awe-boot-react test change runs both engines and the databases (awe-tests/**)" \
  "awe-tests/awe-boot-react/src/test/A.java" "playwright-angular playwright-react embedded-db db-mysql" ""
selection_case "default: a samples change runs the backend jobs" "awe-samples/a/src/main/java/A.java" \
  "all-ut embedded-db db-oracle build" "frontend-ut"
selection_case "default: a website-only change runs no Maven job" "website/docs/a.md" "website" "build all-ut embedded-db db-mysql playwright-angular"


# The change set leaves out the orphan folders of a healed swap (they stay visible in git status)
GITREPO="$WORK/gitrepo"
mkdir -p "$GITREPO/website/.local-ci-orphan-20261014-090000/docs" "$GITREPO/src" "$GITREPO/.m2/repository/org/x"
echo moved > "$GITREPO/website/.local-ci-orphan-20261014-090000/docs/intro.md"
echo new > "$GITREPO/src/new.java"
echo jar > "$GITREPO/.m2/repository/org/x/x.jar"
(cd "$GITREPO" && git init -q . 2>/dev/null)
platform "cd '$GITREPO' && lci_untracked"
if [ "$OUT" = "src/new.java" ]; then
  pass "change set: the orphan folders of a healed swap and the private Maven repository select no job"
else
  fail "change set: untracked files" "$OUT"
fi

# The Maven repository of a checkout is its own, as in CI (MAVEN_OPTS -Dmaven.repo.local=.m2/repository): the test
# applications started from a module pom resolve the AWE modules from it, never the snapshots another worktree installed
case "$(grep -m1 'MAVEN_OPTS:' "$CI_FILE")" in
  *"-Dmaven.repo.local=.m2/repository"*) pass "maven repo: CI builds on the project's .m2/repository" ;;
  *) fail "maven repo: the CI no longer sets -Dmaven.repo.local=.m2/repository (keep the local one in step)" ;;
esac
platform "unset LOCAL_CI_MAVEN_REPO; lci_maven_repo /repo/root"
if [ "$OUT" = /repo/root/.m2/repository ]; then pass "maven repo: the checkout's .m2/repository by default"; else fail "maven repo: default" "$OUT"; fi
platform "LOCAL_CI_MAVEN_REPO=/elsewhere lci_maven_repo /repo/root"
if [ "$OUT" = /elsewhere ]; then pass "maven repo: LOCAL_CI_MAVEN_REPO overrides it"; else fail "maven repo: override" "$OUT"; fi
if grep -q -- '-Dmaven.repo.local=\$MAVEN_REPO' "$BIN_DIR/local-ci.sh"; then
  pass "maven repo: every Maven step of local-ci.sh runs with -Dmaven.repo.local"
else
  fail "maven repo: local-ci.sh does not pass -Dmaven.repo.local"
fi
# Seeding: the third-party artifacts of the user repository are copied once (cloned on APFS), the AWE modules never
SEED_SRC="$WORK/m2src"
SEED_DST="$WORK/m2dst"
mkdir -p "$SEED_SRC/com/almis/awe/awe-model/5.0.0-SNAPSHOT" "$SEED_SRC/com/almis/other" "$SEED_SRC/com/google/guava" "$SEED_SRC/org/springframework"
echo other-worktree > "$SEED_SRC/com/almis/awe/awe-model/5.0.0-SNAPSHOT/awe-model.jar"
echo g > "$SEED_SRC/com/google/guava/g.jar"
echo s > "$SEED_SRC/org/springframework/s.jar"
echo o > "$SEED_SRC/com/almis/other/o.jar"
platform "lci_seed_maven_repo '$SEED_SRC' '$SEED_DST'"
if [ -f "$SEED_DST/com/google/guava/g.jar" ] && [ -f "$SEED_DST/org/springframework/s.jar" ] && [ -f "$SEED_DST/com/almis/other/o.jar" ] \
  && [ ! -e "$SEED_DST/com/almis/awe" ]; then
  pass "maven repo: seeded with the third-party artifacts, without the AWE modules of other worktrees"
else
  fail "maven repo: seed (exit $RC)" "$OUT"
fi
echo kept > "$SEED_DST/org/springframework/own.jar"
platform "lci_seed_maven_repo '$SEED_SRC' '$SEED_DST'"
if [ -f "$SEED_DST/org/springframework/own.jar" ] && [ -z "$OUT" ]; then pass "maven repo: an existing private repository is not seeded again"; else fail "maven repo: reseed" "$OUT"; fi

# Without the JDK of the build only the Maven jobs are refused: the other jobs still run (a checkout lock of its own, so a
# parent local-ci that runs these tests keeps its own)
mkdir -p "$WORK/stub-mvn"   # the CI image has no Maven: a stub, so that the JDK is what the run finds missing
printf '#!/bin/sh\nexit 0\n' > "$WORK/stub-mvn/mvn"
chmod +x "$WORK/stub-mvn/mvn"
run env PATH="$WORK/stub-mvn:$PATH" LOCAL_CI_JDK_DIRS="$WORK/nojvm/*" LOCAL_CI_LOCK="$WORK/test-checkout.lock" LOCAL_CI_JDK21= LOCAL_CI_JDK17= \
  "$BIN_DIR/local-ci.sh" HEAD --only mr-size,spotless
case "$OUT" in *"MR size warning"*"PASS"*|*"MR size warning"*"WARN"*) mr_ok=1 ;; *) mr_ok=0 ;; esac
if [ "$RC" -eq 1 ] && [ "$mr_ok" -eq 1 ] && printf '%s' "$OUT" | grep -q 'Spotless .*FAIL .*no JDK'; then
  pass "no JDK: the Maven jobs fail with the reason, the others still run"
else
  fail "no JDK: whole run refused or wrong rows (exit $RC)" "$(printf '%s\n' "$OUT" | grep -E 'JDK|Spotless|MR size|RESULT')"
fi



# Website restore: only what the 4.x injection wrote goes back; the developer's own work is never touched
SITE="$WORK/site"
mkdir -p "$SITE/website/docs" "$SITE/website/i18n/es/docusaurus-plugin-content-docs" "$SITE/website/versioned_sidebars"
printf '["4.12.0"]\n' > "$SITE/website/versions.json"
printf '{"label":"es before"}\n' > "$SITE/website/i18n/es/docusaurus-plugin-content-docs/version-4.x.json"
printf 'developer page\n' > "$SITE/website/docs/page.md"
platform "cd '$SITE' && printf '%s\n' \$(lci_website_injection_paths 4.x) | sort"
expected_paths="website/i18n/es/docusaurus-plugin-content-docs/version-4.x
website/i18n/es/docusaurus-plugin-content-docs/version-4.x.json
website/versioned_docs/version-4.x
website/versioned_sidebars/version-4.x-sidebars.json
website/versions.json"
if [ "$OUT" = "$expected_paths" ]; then
  pass "website restore: exactly the paths the injection writes (versions.json, the 4.x snapshot, the locale copies)"
else
  fail "website restore: injection paths" "$OUT"
fi
platform "cd '$SITE' && lci_snapshot '$WORK/snap' \$(lci_website_injection_paths 4.x) &&
  printf '[\"4.x\",\"4.12.0\"]\n' > website/versions.json &&
  mkdir -p website/versioned_docs/version-4.x && echo injected > website/versioned_docs/version-4.x/a.md &&
  echo '{}' > website/versioned_sidebars/version-4.x-sidebars.json &&
  printf '{\"label\":\"es after\"}\n' > website/i18n/es/docusaurus-plugin-content-docs/version-4.x.json &&
  echo 'edited during the build' >> website/docs/page.md &&
  echo 'new page' > 'website/docs/my new page.md' &&
  echo dev > website/i18n/es/docusaurus-plugin-content-docs/notes.md &&
  lci_restore '$WORK/snap'"
problems=
[ "$(cat "$SITE/website/versions.json")" = '["4.12.0"]' ] || problems="$problems versions.json"
[ -e "$SITE/website/versioned_docs/version-4.x" ] && problems="$problems versioned_docs-left"
[ -e "$SITE/website/versioned_sidebars/version-4.x-sidebars.json" ] && problems="$problems sidebars-left"
grep -q 'es before' "$SITE/website/i18n/es/docusaurus-plugin-content-docs/version-4.x.json" || problems="$problems i18n-json"
grep -q 'edited during the build' "$SITE/website/docs/page.md" || problems="$problems developer-edit-lost"
[ -f "$SITE/website/docs/my new page.md" ] || problems="$problems developer-file-lost"
[ -f "$SITE/website/i18n/es/docusaurus-plugin-content-docs/notes.md" ] || problems="$problems developer-i18n-file-lost"
if [ "$RC" -eq 0 ] && [ -z "$problems" ]; then
  pass "website restore: the injection is undone and the developer's edits and new files (with spaces) are kept"
else
  fail "website restore:$problems (exit $RC)" "$OUT"
fi
platform "cd '$SITE' && lci_restore '$WORK/snap' && lci_restore '$WORK/no-snapshot'"
if [ "$RC" -eq 0 ] && [ -z "$OUT" ]; then pass "website restore: nothing to do twice, and no snapshot is a no-op"; else fail "website restore: idempotent (exit $RC)" "$OUT"; fi

fi

# A value option given last is a usage error (exit 2), never a loop; perl's alarm bounds the run if it ever loops again
if command -v perl >/dev/null 2>&1; then
  for option in --title --only --skip --browsers --suites --threshold --timeout-factor --description-file; do
    run perl -e 'alarm 30; exec @ARGV' "$BIN_DIR/local-ci.sh" "$option"
    case "$OUT" in *"$option needs a value"*) text_ok=1 ;; *) text_ok=0 ;; esac
    if [ "$RC" -eq 2 ] && [ "$text_ok" -eq 1 ]; then pass "options: $option without a value exits 2"; else fail "options: $option last (exit $RC)" "$OUT"; fi
  done
fi

# Exit 64 is a warning only where CI allows it: the CI job's allow_failure exit_codes, or a local warning check
job_warn() { awk -F '\t' -v id="$1" '$1 == id { print $5 }' "$WORK/jobs.tsv"; }
node "$TOOL" jobs "$CI_FILE" > "$WORK/jobs.tsv"
if [ "$(job_warn mr-docs)" = 64 ] && [ "$(job_warn mr-size)" = 64 ] && [ "$(job_warn sonar-rules)" = 64 ] \
  && [ "$(job_warn build)" = - ] && [ "$(job_warn all-ut)" = - ]; then
  pass "warnings: 64 is a warning for MR docs (CI allow_failure), the size and Sonar guards, not for the build"
else
  fail "warnings: exit codes per job" "$(cut -f1,5 "$WORK/jobs.tsv")"
fi

# The default browsers are those of a merge request: Chromium only (Firefox runs on develop and master)
if grep -q '^BROWSERS=chromium$' "$BIN_DIR/local-ci.sh"; then pass "browsers: Chromium only by default, as a merge request"; else fail "browsers: the default should be chromium"; fi

# ------------------------------------------------------------
# Summary and exit code
# ------------------------------------------------------------
summary_case() { # name, expected exit, expected text, rows (TSV)
  printf '%s\n' "$4" > "$WORK/results.tsv"
  run node "$TOOL" summary "$WORK/results.tsv"
  case "$OUT" in *"$3"*) text_ok=1 ;; *) text_ok=0 ;; esac
  if [ "$RC" -eq "$2" ] && [ "$text_ok" -eq 1 ]; then pass "summary: $1"; else fail "summary: $1 (exit $RC, expected $2, text '$3')" "$OUT"; fi
}
TAB=$(printf '\t')
summary_case "all passed is exit 0" 0 "RESULT: PASS" "build${TAB}Build project${TAB}PASS${TAB}yes${TAB}754${TAB}
website${TAB}Build website${TAB}SKIP${TAB}no${TAB}0${TAB}not triggered"
summary_case "a blocking failure is exit 1 and names the job" 1 "RESULT: FAIL (all-ut)" "build${TAB}Build project${TAB}PASS${TAB}yes${TAB}60${TAB}
all-ut${TAB}All UT${TAB}FAIL${TAB}yes${TAB}61${TAB}tests 10, failed 1, skipped 0"
summary_case "a warning does not fail" 0 "1 warnings" "mr-size${TAB}MR size warning${TAB}WARN${TAB}yes${TAB}1${TAB}"
summary_case "a non-blocking failure shows as a warning" 0 "Sonar rules guard (local)  WARN" "sonar-rules${TAB}Sonar rules guard (local)${TAB}FAIL${TAB}no${TAB}1${TAB}"
summary_case "durations are added up" 0 "1h01m" "a${TAB}A${TAB}PASS${TAB}yes${TAB}3600${TAB}
b${TAB}B${TAB}PASS${TAB}yes${TAB}65${TAB}"
summary_case "a duration under an hour" 0 "1m05s" "b${TAB}B${TAB}PASS${TAB}yes${TAB}65${TAB}"

# The browser suites start the test applications on their file HSQLDB (target/db/awe-boot, target/db/awe-react, see the
# application.properties of the test apps): a run cut halfway leaves its rows there, so the next suite must start without them.
# The list and the function are run as local-ci.sh defines them; the rest of target/ (classes, reports) must be kept
clean_db=$(sed -n '/^AWE_BOOT_DB_DIRS=(/,/)$/p;/^clean_awe_boot_db() {/,/^}/p' "$BIN_DIR/local-ci.sh")
DBT=$(mktemp -d)
expected=
for module in awe-boot awe-boot-react; do
  for db in target/db target/tests/db; do expected="$expected awe-tests/$module/$db"; done
  mkdir -p "$DBT/awe-tests/$module/target/classes" "$DBT/awe-tests/$module/target/tests/reports"
done
OUT=$(cd "$DBT" && bash -c "$clean_db
for dir in $expected; do case \" \${AWE_BOOT_DB_DIRS[*]} \" in *\" \$dir \"*) ;; *) echo \"not in AWE_BOOT_DB_DIRS: \$dir\" ;; esac; done
mkdir -p \"\${AWE_BOOT_DB_DIRS[@]}\" && clean_awe_boot_db" 2>&1) || OUT="exit $? $OUT"
left=$(cd "$DBT" && find . -type d -name db)
kept=$(cd "$DBT" && find . -type d \( -name classes -o -name reports \) | wc -l | tr -d ' ')
case "$clean_db" in *'clean_awe_boot_db() {'*'AWE_BOOT_DB_DIRS'*) found=1 ;; *) found=0 ;; esac
if [ "$found" = 1 ] && [ -z "$OUT" ] && [ -z "$left" ] && [ "$kept" = 4 ]; then
  pass "clean db: the HSQLDB files of the test applications and of the tests are removed, the rest of target/ is kept"
else
  fail "clean db: a database of a previous run is left, or more than the databases is removed" \
    "found=$found left=[$left] kept=$kept $OUT"
fi
rm -rf "$DBT"
# Every suite that starts awe-boot cleans the databases before its Maven run
clean_first=
for fn in all_ut_maven unit_profile_maven step_playwright; do
  sed -n "/^$fn() {/,/^}/p" "$BIN_DIR/local-ci.sh" | awk '/clean_awe_boot_db/ { clean = NR } /MVN\[@\]/ { mvn = NR }
    END { exit !(clean && mvn && clean < mvn) }' || clean_first="$clean_first $fn"
done
if [ -z "$clean_first" ]; then
  pass "clean db: the unit, embedded database and browser suites clean the databases before Maven starts"
else
  fail "clean db: a suite starts Maven without cleaning the databases first" "$clean_first"
fi

printf '\n%s passed, %s failed\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
