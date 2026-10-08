#!/bin/sh
set -eu

# ============================================================
# test-mr-checks.sh
#
# Tests of the two merge request checks of the pipeline (#762):
#   - bin/lint-mr-title.sh  (job "MR title lint", blocking)
#   - bin/mr-size.sh        (job "MR size warning", non-blocking)
#
# Run it from anywhere: sh bin/test-mr-checks.sh
# The title tests install commitlint on first use (npm ci in .gitlab/commitlint), so they need
# Node.js 22.12+ and network access (or a warm npm cache) the first time.
# ============================================================

BIN_DIR=$(cd "$(dirname "$0")" && pwd)
LINT="$BIN_DIR/lint-mr-title.sh"
SIZE="$BIN_DIR/mr-size.sh"
FIXTURES="$BIN_DIR/test-fixtures"
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

printf '\n%s passed, %s failed\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
