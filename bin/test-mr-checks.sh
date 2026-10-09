#!/bin/sh
set -eu

# ============================================================
# test-mr-checks.sh
#
# Tests of the merge request checks of the pipeline (#762, #784):
#   - bin/lint-mr-title.sh  (job "MR title lint", blocking)
#   - bin/mr-size.sh        (job "MR size warning", non-blocking)
#   - bin/mr-docs.sh        (job "MR docs warning", non-blocking)
#   - bin/mr-translations.sh (job "MR translation drift warning", non-blocking)
#
# Run it from anywhere: sh bin/test-mr-checks.sh
# The title tests install commitlint on first use (npm ci in .gitlab/commitlint), so they need
# Node.js 22.12+ and network access (or a warm npm cache) the first time.
# ============================================================

BIN_DIR=$(cd "$(dirname "$0")" && pwd)
LINT="$BIN_DIR/lint-mr-title.sh"
SIZE="$BIN_DIR/mr-size.sh"
DOCS="$BIN_DIR/mr-docs.sh"
FIXTURES="$BIN_DIR/test-fixtures"

# The scripts read the merge request from these variables. A run inside a real merge request pipeline has them set, so every
# one is cleared here: a case that needs one sets it itself, and no result depends on the pipeline that runs the tests.
for variable in CI_MERGE_REQUEST_TITLE CI_MERGE_REQUEST_DESCRIPTION CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED \
  CI_MERGE_REQUEST_DIFF_BASE_SHA CI_MERGE_REQUEST_TARGET_BRANCH_NAME MR_SIZE_THRESHOLD; do
  unset "$variable"
done

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
# Title lint
# ------------------------------------------------------------

title_ok() {
  run "$LINT" "$1"
  if [ "$RC" -eq 0 ]; then pass "valid title: $1"; else fail "should be valid: $1" "$OUT"; fi
}

title_bad() {
  run "$LINT" "$1"
  if [ "$RC" -ne 0 ]; then pass "invalid title rejected: $1"; else fail "should be rejected: $1" "$OUT"; fi
}

while IFS= read -r line; do
  [ -n "$line" ] && title_ok "$line"
done < "$FIXTURES/valid-mr-titles.txt"

title_ok "feat(awe-model): new option model (#795 T4)"
title_ok "fix: guard null dates (#795)"
title_ok "chore(deps): update dependency postcss to v8.5.28 (develop)"
title_ok "feat(awe-client-react,awe-testing): both modules at once"
title_ok "feat!: remove the deprecated API"
title_ok "revert: feat: charts with ECharts (#795 T2)"
title_ok "Draft: feat: work in progress (#762)"
title_ok "[Draft] fix: work in progress (#762)"

title_bad "Update stuff"
title_bad "feat add a thing"
title_bad "feature: unknown type"
title_bad "FIX: uppercase type"
title_bad "feat:"
title_bad "feat: "
title_bad "feat: ends with a full stop."
title_bad "feat: $(printf 'a%.0s' $(seq 1 160))"

run "$LINT" ""
if [ "$RC" -ne 0 ]; then pass "empty title rejected"; else fail "empty title should be rejected" "$OUT"; fi

# The failure message must tell the author what format is expected
run "$LINT" "Update stuff"
case "$OUT" in
  *"type(scope): description"*) pass "failure message explains the format" ;;
  *) fail "failure message should explain the format" "$OUT" ;;
esac

# ------------------------------------------------------------
# Size warning
# ------------------------------------------------------------

REPO="$WORK/repo"
mkdir -p "$REPO"
(
  cd "$REPO"
  git init -q -b main .
  git config user.email test@example.com
  git config user.name Test
  git config commit.gpgsign false
  printf 'seed\n' > README.md
  git add -A
  git commit -q -m "seed"
) >/dev/null

# Writes N numbered lines to a file, creating the parent directories
lines() {
  mkdir -p "$(dirname "$2")"
  seq 1 "$1" | sed "s/^/line /" > "$2"
}

# Resets the repository to the seed commit and returns to it
reset_repo() {
  (cd "$REPO" && git checkout -q -f main && git clean -fdq && git reset -q --hard "$SEED")
}
SEED=$(cd "$REPO" && git rev-parse HEAD)

# Commits whatever is in the working tree and prints nothing
commit_all() {
  (cd "$REPO" && git add -A && git commit -q -m "change")
}

# Expects the size script to end with the given code and to mention a text in its output
size_case() {
  name=$1
  expected_rc=$2
  expected_text=$3
  shift 3
  set +e
  OUT=$(cd "$REPO" && "$@" "$SIZE" "$SEED" HEAD 2>&1)
  RC=$?
  set -e
  case "$OUT" in
    *"$expected_text"*) text_ok=1 ;;
    *) text_ok=0 ;;
  esac
  if [ "$RC" -eq "$expected_rc" ] && [ "$text_ok" -eq 1 ]; then
    pass "size: $name"
  else
    fail "size: $name (exit $RC, expected $expected_rc, text '$expected_text')" "$OUT"
  fi
}

# Below the threshold
reset_repo
lines 100 "$REPO/src/Main.java"
commit_all
size_case "100 changed lines is under the threshold" 0 "100 changed lines" env

# Exactly the threshold is still fine, one more is not
reset_repo
lines 400 "$REPO/src/Main.java"
commit_all
size_case "exactly 400 changed lines passes" 0 "400 changed lines" env
reset_repo
lines 401 "$REPO/src/Main.java"
commit_all
size_case "401 changed lines warns" 64 "401 changed lines" env

# Additions plus deletions count
reset_repo
lines 300 "$REPO/src/Old.java"
commit_all
SEED=$(cd "$REPO" && git rev-parse HEAD)
(cd "$REPO" && git rm -q src/Old.java)
lines 150 "$REPO/src/New.java"
commit_all
size_case "additions plus deletions are added up" 64 "450 changed lines" env

# Exclusions: lockfiles, generated files, website docs and binaries do not count
reset_repo
lines 100 "$REPO/src/Main.java"
lines 5000 "$REPO/package-lock.json"
lines 5000 "$REPO/awe-client/yarn.lock"
lines 5000 "$REPO/Gemfile.lock"
lines 5000 "$REPO/skills-lock.json"
lines 5000 "$REPO/website/docs/guides/guide.md"
lines 5000 "$REPO/website/i18n/es/page.md"
lines 5000 "$REPO/web/app.min.js"
lines 5000 "$REPO/web/flag.svg"
lines 5000 "$REPO/module/generated/Model.java"
lines 5000 "$REPO/CHANGELOG.md"
head -c 20000 /dev/urandom > "$REPO/image.png"
commit_all
size_case "lockfiles, generated files, website docs and binaries are excluded" 0 "100 changed lines" env

# The same changes in a regular path do count
reset_repo
lines 5000 "$REPO/docs/guide.md"
commit_all
size_case "docs outside website/docs count" 64 "5000 changed lines" env

# The summary lists the biggest files first
reset_repo
lines 450 "$REPO/src/Big.java"
lines 20 "$REPO/src/Small.java"
commit_all
set +e
OUT=$(cd "$REPO" && "$SIZE" "$SEED" HEAD 2>&1)
set -e
FIRST=$(printf '%s\n' "$OUT" | grep -n 'src/Big.java' | head -1 | cut -d: -f1)
SECOND=$(printf '%s\n' "$OUT" | grep -n 'src/Small.java' | head -1 | cut -d: -f1)
if [ -n "$FIRST" ] && [ -n "$SECOND" ] && [ "$FIRST" -lt "$SECOND" ]; then
  pass "size: summary lists the biggest files first"
else
  fail "size: summary should list the biggest files first" "$OUT"
fi

# The threshold can be changed
reset_repo
lines 60 "$REPO/src/Main.java"
commit_all
size_case "threshold can be lowered" 64 "60 changed lines" env MR_SIZE_THRESHOLD=50

# Base from the CI variable and from the target branch
reset_repo
lines 450 "$REPO/src/Main.java"
commit_all
set +e
OUT=$(cd "$REPO" && CI_MERGE_REQUEST_DIFF_BASE_SHA="$SEED" "$SIZE" 2>&1)
RC=$?
set -e
if [ "$RC" -eq 64 ]; then pass "size: base from CI_MERGE_REQUEST_DIFF_BASE_SHA"; else fail "size: CI_MERGE_REQUEST_DIFF_BASE_SHA" "$OUT"; fi

(cd "$REPO" && git branch -q -f target "$SEED")
set +e
OUT=$(cd "$REPO" && env -u CI_MERGE_REQUEST_DIFF_BASE_SHA CI_MERGE_REQUEST_TARGET_BRANCH_NAME=target "$SIZE" 2>&1)
RC=$?
set -e
if [ "$RC" -eq 64 ]; then pass "size: base from the target branch merge base"; else fail "size: target branch merge base" "$OUT"; fi

# Without a base the script explains what is missing instead of guessing
set +e
OUT=$(cd "$REPO" && env -u CI_MERGE_REQUEST_DIFF_BASE_SHA -u CI_MERGE_REQUEST_TARGET_BRANCH_NAME "$SIZE" 2>&1)
RC=$?
set -e
if [ "$RC" -eq 2 ]; then pass "size: missing base is a usage error (exit 2)"; else fail "size: missing base" "$OUT"; fi

# ------------------------------------------------------------
# Docs warning
# ------------------------------------------------------------

# Expects the docs script to end with the given code and to mention a text in its output. The description and the
# title come from the environment, as in the CI job: "$1" name, "$2" exit code, "$3" text, then the command prefix
# (env plus assignments).
docs_case() {
  name=$1
  expected_rc=$2
  expected_text=$3
  shift 3
  set +e
  OUT=$(cd "$REPO" && env -u CI_MERGE_REQUEST_DESCRIPTION -u CI_MERGE_REQUEST_TITLE \
    -u CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED -u CI_MERGE_REQUEST_DIFF_BASE_SHA -u CI_MERGE_REQUEST_TARGET_BRANCH_NAME \
    "$@" "$DOCS" "$SEED" HEAD 2>&1)
  RC=$?
  set -e
  case "$OUT" in
    *"$expected_text"*) text_ok=1 ;;
    *) text_ok=0 ;;
  esac
  if [ "$RC" -eq "$expected_rc" ] && [ "$text_ok" -eq 1 ]; then
    pass "docs: $name"
  else
    fail "docs: $name (exit $RC, expected $expected_rc, text '$expected_text')" "$OUT"
  fi
}

DOCS_TICKED='- [x] Docs updated (`website/docs`, `CONTRIBUTING.md` or `README.md`)'
NODOCS_TEMPLATE='- [ ] No docs needed because: …'

reset_repo
lines 20 "$REPO/awe-framework/awe-model/src/main/java/Foo.java"
commit_all
docs_case "code without docs or reason warns" 64 "MR docs warning" env
docs_case "the template as it is warns" 64 "MR docs warning" env "CI_MERGE_REQUEST_DESCRIPTION=- [ ] Docs updated
$NODOCS_TEMPLATE"
docs_case "a ticked Docs updated passes" 0 "says the docs are updated" env "CI_MERGE_REQUEST_DESCRIPTION=$DOCS_TICKED"
docs_case "an uppercase tick passes" 0 "says the docs are updated" env "CI_MERGE_REQUEST_DESCRIPTION=- [X] Docs updated"
docs_case "a tick among other lines passes" 0 "says the docs are updated" env "CI_MERGE_REQUEST_DESCRIPTION=## What
- [ ] Other item
$DOCS_TICKED
$NODOCS_TEMPLATE
### Description"
docs_case "No docs needed with a reason passes" 0 "gives a reason" env "CI_MERGE_REQUEST_DESCRIPTION=- [x] No docs needed because: internal refactor, no behavior change"
docs_case "a ticked No docs needed with the placeholder still warns" 64 "MR docs warning" env "CI_MERGE_REQUEST_DESCRIPTION=- [x] No docs needed because: …"
docs_case "a ticked No docs needed with three dots still warns" 64 "MR docs warning" env "CI_MERGE_REQUEST_DESCRIPTION=- [x] No docs needed because: ..."
docs_case "a ticked No docs needed without a reason still warns" 64 "MR docs warning" env "CI_MERGE_REQUEST_DESCRIPTION=- [x] No docs needed because:"
docs_case "an unticked No docs needed with a reason warns" 64 "MR docs warning" env "CI_MERGE_REQUEST_DESCRIPTION=- [ ] No docs needed because: internal refactor"
docs_case "a dependency update passes" 0 "dependency update" env "CI_MERGE_REQUEST_TITLE=chore(deps): update dependency foo to v2"
docs_case "a Draft dependency update passes" 0 "dependency update" env "CI_MERGE_REQUEST_TITLE=Draft: build(deps): bump foo"
docs_case "a truncated description is not judged" 0 "cannot be read" env CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED=true
docs_case "a truncated description with a reason still passes" 0 "gives a reason" env CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED=true "CI_MERGE_REQUEST_DESCRIPTION=- [x] No docs needed because: tooling"

reset_repo
lines 20 "$REPO/awe-framework/awe-model/src/main/java/Foo.java"
lines 5 "$REPO/website/docs/guides/guide.md"
commit_all
docs_case "code with a docs change passes" 0 "documentation changed" env

reset_repo
lines 20 "$REPO/awe-framework/awe-model/src/main/java/Foo.java"
lines 5 "$REPO/CONTRIBUTING.md"
commit_all
docs_case "code with a CONTRIBUTING change passes" 0 "documentation changed" env

reset_repo
lines 20 "$REPO/awe-framework/awe-model/src/test/java/FooTest.java"
lines 20 "$REPO/awe-framework/awe-client-angular/src/test/jest/foo.test.js"
lines 20 "$REPO/awe-tests/awe-boot/src/main/java/FooIT.java"
lines 20 "$REPO/awe-framework/awe-client-angular/package-lock.json"
lines 20 "$REPO/awe-framework/awe-model/NOTES.md"
commit_all
docs_case "tests, lockfiles and markdown are not code" 0 "no code changed" env

reset_repo
lines 20 "$REPO/.gitlab-ci.yml"
commit_all
docs_case "a CI-only change is not code" 0 "no code changed" env

reset_repo
lines 20 "$REPO/bin/tool.sh"
commit_all
docs_case "a script under bin counts as code" 64 "MR docs warning" env

reset_repo
lines 5 "$REPO/pom.xml"
commit_all
docs_case "pom.xml counts as code" 64 "MR docs warning" env

# Base from the CI variable
set +e
OUT=$(cd "$REPO" && CI_MERGE_REQUEST_DIFF_BASE_SHA="$SEED" "$DOCS" 2>&1)
RC=$?
set -e
if [ "$RC" -eq 64 ]; then pass "docs: base from CI_MERGE_REQUEST_DIFF_BASE_SHA"; else fail "docs: CI_MERGE_REQUEST_DIFF_BASE_SHA" "$OUT"; fi

# The check is advisory: without a base, or with a base that is not in the clone, it says so and ends with 0
set +e
OUT=$(cd "$REPO" && "$DOCS" 2>&1)
RC=$?
set -e
case "$OUT" in *"no base to compare with"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "docs: a missing base is a notice, exit 0"; else fail "docs: missing base (exit $RC)" "$OUT"; fi

set +e
OUT=$(cd "$REPO" && CI_MERGE_REQUEST_DIFF_BASE_SHA=0123456789012345678901234567890123456789 "$DOCS" 2>&1)
RC=$?
set -e
case "$OUT" in *"could not compare"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "docs: a base that is not in the clone is a notice, exit 0"; else fail "docs: unknown base (exit $RC)" "$OUT"; fi

# An explicit base that moved on: only what the head changed since the merge base counts (three-dot diff)
reset_repo
(cd "$REPO" && git checkout -q -b feature)
lines 20 "$REPO/awe-framework/awe-model/src/test/java/FooTest.java"
commit_all
(cd "$REPO" && git checkout -q main)
lines 20 "$REPO/awe-framework/awe-model/src/main/java/Other.java"
commit_all
set +e
OUT=$(cd "$REPO" && "$DOCS" main feature 2>&1)
RC=$?
set -e
case "$OUT" in *"no code changed"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "docs: code that only the base has does not count"; else fail "docs: moved base (exit $RC)" "$OUT"; fi
(cd "$REPO" && git branch -q -D feature)

# The same the other way round: docs that only the base has are not the docs of the head
reset_repo
(cd "$REPO" && git checkout -q -b feature)
lines 20 "$REPO/awe-framework/awe-model/src/main/java/Foo.java"
commit_all
(cd "$REPO" && git checkout -q main)
lines 5 "$REPO/website/docs/guides/other.md"
commit_all
set +e
OUT=$(cd "$REPO" && "$DOCS" main feature 2>&1)
RC=$?
set -e
case "$OUT" in *"MR docs warning"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 64 ] && [ "$text_ok" -eq 1 ]; then pass "docs: docs that only the base has do not count"; else fail "docs: moved base, docs (exit $RC)" "$OUT"; fi
(cd "$REPO" && git branch -q -D feature)

# ------------------------------------------------------------
# Translation drift warning (#784)
# ------------------------------------------------------------

TRANSLATIONS="$BIN_DIR/mr-translations.sh"
EN_PAGE="website/docs/api/button.md"
ES_PAGE="website/i18n/es/docusaurus-plugin-content-docs/current/api/button.md"

# A repository where the English page and its translation exist at the base (BASE_TR), ready for the change under test
translations_repo() {
  reset_repo
  lines 5 "$REPO/$EN_PAGE"
  lines 5 "$REPO/$ES_PAGE"
  lines 5 "$REPO/website/docs/api/untranslated.md"
  commit_all
  BASE_TR=$(cd "$REPO" && git rev-parse HEAD)
}

# "$1" name, "$2" exit code, "$3" text in the output
translations_case() {
  set +e
  OUT=$(cd "$REPO" && env -u CI_MERGE_REQUEST_DIFF_BASE_SHA -u CI_MERGE_REQUEST_TARGET_BRANCH_NAME "$TRANSLATIONS" "$BASE_TR" HEAD 2>&1)
  RC=$?
  set -e
  case "$OUT" in
    *"$3"*) text_ok=1 ;;
    *) text_ok=0 ;;
  esac
  if [ "$RC" -eq "$2" ] && [ "$text_ok" -eq 1 ]; then
    pass "translations: $1"
  else
    fail "translations: $1 (exit $RC, expected $2, text '$3')" "$OUT"
  fi
}

translations_repo
lines 9 "$REPO/$EN_PAGE"
commit_all
translations_case "an English page changed without its translation warns" 64 "MR translation drift warning"
case "$OUT" in *"$EN_PAGE -> $ES_PAGE"*) pass "translations: the warning names the page and its translation" ;; *) fail "translations: the warning should name the pair" "$OUT" ;; esac

translations_repo
lines 9 "$REPO/$EN_PAGE"
lines 9 "$REPO/$ES_PAGE"
commit_all
translations_case "the page and its translation changed together passes" 0 "no translated page"

translations_repo
lines 9 "$REPO/website/docs/api/untranslated.md"
commit_all
translations_case "an English page without translation passes" 0 "no translated page"

translations_repo
lines 9 "$REPO/$ES_PAGE"
commit_all
translations_case "a translation changed alone passes" 0 "no translated page"

translations_repo
(cd "$REPO" && git rm -q "$EN_PAGE")
commit_all
translations_case "a deleted English page passes" 0 "no translated page"

translations_repo
lines 9 "$REPO/website/versioned_docs/version-4.12.0/api/button.md"
lines 9 "$REPO/awe-framework/awe-model/src/main/java/Foo.java"
commit_all
translations_case "frozen versions and code are not checked" 0 "no translated page"

# Several pages: only the ones without their translation are named
translations_repo
lines 5 "$REPO/website/docs/api/grids.md"
lines 5 "$REPO/website/i18n/es/docusaurus-plugin-content-docs/current/api/grids.md"
commit_all
BASE_TR=$(cd "$REPO" && git rev-parse HEAD)
lines 9 "$REPO/$EN_PAGE"
lines 9 "$REPO/website/docs/api/grids.md"
lines 9 "$REPO/website/i18n/es/docusaurus-plugin-content-docs/current/api/grids.md"
commit_all
translations_case "only the pages without their translation are named" 64 "1 English page(s)"
case "$OUT" in *"api/grids.md"*) fail "translations: grids.md was translated in the same change" "$OUT" ;; *) pass "translations: a page changed with its translation is not named" ;; esac

# Advisory: no base, or a base that is not in the clone, is a notice with exit 0
set +e
OUT=$(cd "$REPO" && env -u CI_MERGE_REQUEST_DIFF_BASE_SHA -u CI_MERGE_REQUEST_TARGET_BRANCH_NAME "$TRANSLATIONS" 2>&1)
RC=$?
set -e
case "$OUT" in *"no base to compare with"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "translations: a missing base is a notice, exit 0"; else fail "translations: missing base (exit $RC)" "$OUT"; fi

set +e
OUT=$(cd "$REPO" && CI_MERGE_REQUEST_DIFF_BASE_SHA=0123456789012345678901234567890123456789 "$TRANSLATIONS" 2>&1)
RC=$?
set -e
case "$OUT" in *"could not compare"*) text_ok=1 ;; *) text_ok=0 ;; esac
if [ "$RC" -eq 0 ] && [ "$text_ok" -eq 1 ]; then pass "translations: a base that is not in the clone is a notice, exit 0"; else fail "translations: unknown base (exit $RC)" "$OUT"; fi

# Base from the CI variable
translations_repo
lines 9 "$REPO/$EN_PAGE"
commit_all
set +e
OUT=$(cd "$REPO" && CI_MERGE_REQUEST_DIFF_BASE_SHA="$BASE_TR" "$TRANSLATIONS" 2>&1)
RC=$?
set -e
if [ "$RC" -eq 64 ]; then pass "translations: base from CI_MERGE_REQUEST_DIFF_BASE_SHA"; else fail "translations: CI_MERGE_REQUEST_DIFF_BASE_SHA" "$OUT"; fi

# ------------------------------------------------------------
# Broken anchors of the website build
# ------------------------------------------------------------

ANCHORS="$BIN_DIR/check-website-anchors.sh"

# Expects the anchors script to end with the given code and to mention a text in its output ("$4" is the log content)
anchors_case() {
  name=$1
  expected_rc=$2
  expected_text=$3
  printf '%s\n' "$4" > "$WORK/website-build.log"
  set +e
  OUT=$("$ANCHORS" "$WORK/website-build.log" 2>&1)
  RC=$?
  set -e
  case "$OUT" in
    *"$expected_text"*) text_ok=1 ;;
    *) text_ok=0 ;;
  esac
  if [ "$RC" -eq "$expected_rc" ] && [ "$text_ok" -eq 1 ]; then
    pass "anchors: $name"
  else
    fail "anchors: $name (exit $RC, expected $expected_rc, text '$expected_text')" "$OUT"
  fi
}

HEAD_LOG='[INFO] [en] Creating an optimized production build...
[WARNING] Docusaurus found broken anchors!'
FROZEN_BLOCK='- Broken anchor on source page path = /docs/4.12.0/session:
   -> linking to properties#session-properties (resolved as: /docs/4.12.0/properties#session-properties)
- Broken anchor on source page path = /es/docs/4.x/api/queues:
   -> linking to #response-message-element (resolved as: /es/docs/4.x/api/queues#response-message-element)
   -> linking to #message-parameter-element (resolved as: /es/docs/4.x/api/queues#message-parameter-element)'
CURRENT_BLOCK='- Broken anchor on source page path = /docs/session:
   -> linking to properties#session-properties (resolved as: /docs/properties#session-properties)'

anchors_case "a log without broken anchors passes" 0 "none in the current docs" "[INFO] [en] Creating an optimized production build...
[SUCCESS] Generated static files in \"build\"."
anchors_case "an empty log passes" 0 "none in the current docs" ""
anchors_case "broken anchors only in frozen versions pass" 0 "3 in 2 pages of the frozen versions" "$HEAD_LOG
$FROZEN_BLOCK
[SUCCESS] Generated static files in \"build\"."
anchors_case "a broken anchor in a current page fails and names it" 1 "/docs/session" "$HEAD_LOG
$FROZEN_BLOCK
$CURRENT_BLOCK
[SUCCESS] Generated static files in \"build\"."
anchors_case "the failure says what Docusaurus registers" 1 "headings, list items, and" "$HEAD_LOG
$CURRENT_BLOCK"
anchors_case "a broken anchor in a localized current page fails" 1 "/es/docs/logging" "$HEAD_LOG
- Broken anchor on source page path = /es/docs/logging:
   -> linking to /docs/properties#awe.scheduler.remote-enabled
[SUCCESS] Generated static files in \"build\"."
anchors_case "a broken anchor in the blog fails" 1 "/blog/release" "$HEAD_LOG
- Broken anchor on source page path = /blog/release:
   -> linking to #nothing (resolved as: /blog/release#nothing)"
anchors_case "a current page under /docs/4-something is not mistaken for a version" 1 "/docs/4-guide" "$HEAD_LOG
- Broken anchor on source page path = /docs/4-guide:
   -> linking to #nothing (resolved as: /docs/4-guide#nothing)"
anchors_case "an unreadable report format is not a pass" 1 "format changed" "$HEAD_LOG
Something else entirely"
anchors_case "the count of current links is reported" 1 "2 in 1 pages" "$HEAD_LOG
- Broken anchor on source page path = /docs/a:
   -> linking to #one (resolved as: /docs/a#one)
   -> linking to #two (resolved as: /docs/a#two)"

set +e
OUT=$("$ANCHORS" "$WORK/does-not-exist.log" 2>&1)
RC=$?
set -e
if [ "$RC" -eq 2 ]; then pass "anchors: a missing log is a usage error (exit 2)"; else fail "anchors: missing log" "$OUT"; fi

printf '\n%s passed, %s failed\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
