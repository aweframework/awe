#!/bin/bash
set -uo pipefail

# ============================================================
# test-release-changelog.sh
#
# Offline tests of the changelog functions of bin/lib/changelog.sh, used by bin/release.sh:
#   - S1: the leading "(unreleased)" section is replaced by the generated release section
#   - S3: CHANGELOG-<M>.x.md of the older lines is regenerated from the latest vM.y.z tag
#   - S5: [HAS IMPACTS] is not repeated and MRs without a section label are classified by their prefix
# No GitLab access: the merge requests come from bin/test-fixtures/changelog/mrs.json.
#
# Run it from anywhere: bash bin/test-release-changelog.sh
# ============================================================

BIN_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
FIXTURES="$BIN_DIR/test-fixtures/changelog"
# shellcheck source=lib/changelog.sh
source "$BIN_DIR/lib/changelog.sh"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT INT TERM

PASSED=0
FAILED=0
MRS=$(cat "$FIXTURES/mrs.json")
DATE="13/11/2026"

pass() {
  PASSED=$((PASSED + 1))
  printf 'ok   - %s\n' "$1"
}

fail() {
  FAILED=$((FAILED + 1))
  printf 'FAIL - %s\n' "$1"
  if [[ -n "${2:-}" ]]; then
    printf '%s\n' "$2" | sed 's/^/         /'
  fi
}

# Runs a command and stores its exit code in $RC and its output in $OUT
run() {
  OUT=$("$@" 2>&1)
  RC=$?
}

# Compares a file with the expected one
same_file() {
  local name="$1" actual="$2" expected="$3" diff_out
  if diff_out=$(diff -u "$expected" "$actual" 2>&1); then pass "$name"; else fail "$name" "$diff_out"; fi
}

# ------------------------------------------------------------
# S1: release section in CHANGELOG.md
# ------------------------------------------------------------

cp "$FIXTURES/released.md" "$WORK/CHANGELOG.md"
run changelog_update "$WORK/CHANGELOG.md" "AWE" "5.0.0" "$DATE" "$MRS"
[[ "$RC" -eq 0 ]] || fail "S1 without unreleased header: update ends with 0 (rc=$RC)" "$OUT"
same_file "S1 without unreleased header: the generated section is prepended as today" \
  "$WORK/CHANGELOG.md" "$FIXTURES/expected-prepended.md"

cp "$FIXTURES/unreleased.md" "$WORK/CHANGELOG.md"
run changelog_update "$WORK/CHANGELOG.md" "AWE" "5.0.0" "$DATE" "$MRS"
[[ "$RC" -eq 0 ]] || fail "S1 with unreleased header: update ends with 0 (rc=$RC)" "$OUT"
same_file "S1 with unreleased header: the unreleased section is replaced by the generated one, older sections intact" \
  "$WORK/CHANGELOG.md" "$FIXTURES/expected-prepended.md"
count=$(grep -c '^# Release notes for AWE 5.0.0$' "$WORK/CHANGELOG.md")
unreleased=$(grep -c '(unreleased)' "$WORK/CHANGELOG.md")
if [[ "$count" -eq 1 && "$unreleased" -eq 0 ]]; then
  pass "S1 with unreleased header: one 5.0.0 header and no (unreleased) header left"
else
  fail "S1 with unreleased header: one 5.0.0 header and no (unreleased) header left ($count headers, $unreleased unreleased)"
fi

cp "$FIXTURES/unreleased-pointer.md" "$WORK/CHANGELOG.md"
run changelog_update "$WORK/CHANGELOG.md" "AWE" "5.0.0" "$DATE" "$MRS"
same_file "S1 with unreleased header: the closing pointer to CHANGELOG-4.x.md is kept" \
  "$WORK/CHANGELOG.md" "$FIXTURES/expected-unreleased-pointer.md"

cp "$FIXTURES/expected-prepended.md" "$WORK/CHANGELOG.md"
run changelog_update "$WORK/CHANGELOG.md" "AWE" "5.0.0" "$DATE" "$MRS"
if [[ "$RC" -ne 0 ]] && cmp -s "$WORK/CHANGELOG.md" "$FIXTURES/expected-prepended.md" \
  && [[ "$OUT" == *"5.0.0"* ]]; then
  pass "S1 version already released: fails, names the version and leaves CHANGELOG.md unchanged"
else
  fail "S1 version already released: fails, names the version and leaves CHANGELOG.md unchanged (rc=$RC)" "$OUT"
fi

# ------------------------------------------------------------
# S5: entry lines and sections
# ------------------------------------------------------------

mr() {
  jq -c --arg t "$1" --argjson l "$2" \
    '{iid: 1, title: $t, labels: $l, web_url: "https://gitlab.example/mr/1", merged_by: {name: "Dev"}}' <<< 'null'
}

entry_case() {
  local name="$1" title="$2" labels="$3" section="$4" line="$5" actual
  actual=$(changelog_mr_entry "$(mr "$title" "$labels")")
  if [[ "$actual" == "${section}"$'\t'"- ${line}. [MR #1](https://gitlab.example/mr/1) (Dev)" ]]; then
    pass "S5 $name"
  else
    fail "S5 $name" "$actual"
  fi
}

entry_case "has impacts label adds the prefix" "refactor: a change" '["feature","has impacts"]' \
  feature "**[HAS IMPACTS]** refactor: a change"
entry_case "has impacts already in the title is not repeated" "[HAS IMPACTS] feat: a change" '["feature","has impacts"]' \
  feature "[HAS IMPACTS] feat: a change"
entry_case "has impacts in the title in any case is not repeated" "fix: [Has Impacts] a change" '["bug","has impacts"]' \
  bug "fix: [Has Impacts] a change"
entry_case "feat prefix without label goes to features" "feat(awe-model): a feature" '[]' feature "feat(awe-model): a feature"
entry_case "breaking feat prefix goes to features" "feat!: a breaking feature" '[]' feature "feat!: a breaking feature"
entry_case "fix prefix without label goes to bug fixes" "fix: a fix" '[]' bug "fix: a fix"
entry_case "docs prefix without label goes to documentation" "docs(website): a guide" '[]' documentation "docs(website): a guide"
entry_case "test prefix without label goes to tests" "test: a test" '[]' test "test: a test"
entry_case "other prefix without label goes to bug fixes" "chore(deps): a bump" '["update-dependencies"]' bug \
  "chore(deps): a bump"
entry_case "prefix after a leading [tag] is classified" "[HAS IMPACTS] docs: a guide" '["has impacts"]' documentation \
  "[HAS IMPACTS] docs: a guide"
entry_case "section label wins over the prefix" "feat: labelled" '["documentation"]' documentation "feat: labelled"
entry_case "Resolve and quotes are stripped as today" 'Resolve "Fix the grid"' '["bug"]' bug "Fix the grid"

# ------------------------------------------------------------
# S3: CHANGELOG-<M>.x.md regenerated from the latest vM.y.z tag
# ------------------------------------------------------------

REPO="$WORK/repo"
git init --quiet "$REPO"
git -C "$REPO" config user.email "test@example.com"
git -C "$REPO" config user.name "Test"
git -C "$REPO" config commit.gpgsign false
git -C "$REPO" config tag.gpgsign false

tag_changelog() {
  printf '%s' "$2" > "$REPO/CHANGELOG.md"
  git -C "$REPO" add CHANGELOG.md
  git -C "$REPO" commit --quiet -m "$1"
  git -C "$REPO" tag "$1"
}

tag_changelog v3.0.0 $'# Release notes for AWE 3.0.0\n*01/01/2020*\n'
tag_changelog v4.9.0 $'# Release notes for AWE 4.9.0\n*01/01/2025*\n'
tag_changelog v4.13.1 $'# Release notes for AWE 4.13.2 (unreleased)\n\n- Hand-written.\n\n# Release notes for AWE 4.13.1\n*09/10/2026*\n \n- Fix.\n'
tag_changelog v4.13.2-rc1 $'# Release notes for AWE 4.13.2-rc1\n'
tag_changelog v5.0.0 $'# Release notes for AWE 5.0.0\n'

reset_lines() {
  printf 'stale 4\n' > "$REPO/CHANGELOG-4.x.md"
  printf 'stale 3\n' > "$REPO/CHANGELOG-3.x.md"
  printf 'stale 2\n' > "$REPO/CHANGELOG-2.x.md"
  printf 'stale 5\n' > "$REPO/CHANGELOG-5.x.md"
  printf 'current\n' > "$REPO/CHANGELOG.md"
}

reset_lines
run changelog_sync_previous_lines "5.0.1" "$REPO"
[[ "$RC" -eq 0 ]] || fail "S3 sync ends with 0 (rc=$RC)" "$OUT"
expected=$'# Release notes for AWE 4.13.1\n*09/10/2026*\n \n- Fix.'
if [[ "$(cat "$REPO/CHANGELOG-4.x.md")" == "$expected" ]]; then
  pass "S3 CHANGELOG-4.x.md comes from the latest v4 tag without its unreleased section"
else
  fail "S3 CHANGELOG-4.x.md comes from the latest v4 tag without its unreleased section" "$(cat "$REPO/CHANGELOG-4.x.md")"
fi
if [[ "$(cat "$REPO/CHANGELOG-3.x.md")" == $'# Release notes for AWE 3.0.0\n*01/01/2020*' ]]; then
  pass "S3 every older line with a file is regenerated (CHANGELOG-3.x.md)"
else
  fail "S3 every older line with a file is regenerated (CHANGELOG-3.x.md)" "$(cat "$REPO/CHANGELOG-3.x.md")"
fi
if [[ "$(cat "$REPO/CHANGELOG-2.x.md")" == "stale 2" && "$OUT" == *"CHANGELOG-2.x.md"* ]]; then
  pass "S3 a line without tags keeps its file and is reported"
else
  fail "S3 a line without tags keeps its file and is reported" "$OUT"
fi
if [[ "$(cat "$REPO/CHANGELOG-5.x.md")" == "stale 5" && "$(cat "$REPO/CHANGELOG.md")" == "current" ]]; then
  pass "S3 the released line and CHANGELOG.md are not touched"
else
  fail "S3 the released line and CHANGELOG.md are not touched"
fi
if [[ ! -e "$REPO/CHANGELOG-1.x.md" ]]; then
  pass "S3 no file is created for a line without one"
else
  fail "S3 no file is created for a line without one"
fi

reset_lines
run changelog_sync_previous_lines "4.13.2" "$REPO"
if [[ "$RC" -eq 0 && "$(cat "$REPO/CHANGELOG-4.x.md")" == "stale 4" \
  && "$(cat "$REPO/CHANGELOG-3.x.md")" == $'# Release notes for AWE 3.0.0\n*01/01/2020*' ]]; then
  pass "S3 a 4.x release does not regenerate CHANGELOG-4.x.md, only lower lines"
else
  fail "S3 a 4.x release does not regenerate CHANGELOG-4.x.md, only lower lines (rc=$RC)" "$OUT"
fi

stripped=$(printf '# Release notes for AWE 4.13.1\n*09/10/2026*\n' | changelog_strip_unreleased)
if [[ "$stripped" == $'# Release notes for AWE 4.13.1\n*09/10/2026*' ]]; then
  pass "S3 a changelog without unreleased section is kept as is"
else
  fail "S3 a changelog without unreleased section is kept as is" "$stripped"
fi

# S1: a horizontal rule inside the hand-written draft is not the closing pointer: the whole draft goes
printf '# Release notes for AWE 5 (unreleased)\n\nDraft A\n\n---\n\nDraft B\n\n---\n\nRelease notes of AWE 4 and older releases: [CHANGELOG-4.x.md](CHANGELOG-4.x.md).\n' > "$WORK/CHANGELOG.md"
run changelog_update "$WORK/CHANGELOG.md" "AWE" "5.0.0" "$DATE" '[]'
if [[ "$RC" -eq 0 ]] && ! grep -q 'Draft' "$WORK/CHANGELOG.md" && grep -q 'CHANGELOG-4.x.md' "$WORK/CHANGELOG.md"; then
  pass "S1 a --- inside the draft does not keep the rest of the draft"
else
  fail "S1 a --- inside the draft does not keep the rest of the draft" "$(cat "$WORK/CHANGELOG.md")"
fi

# S1: on a support line the draft is removed and the released sections stay byte-identical
printf '# Release notes for AWE 4.13.2 (unreleased)\n\n**[HAS IMPACTS]** Changed:\n- Text.\n\n# Release notes for AWE 4.13.1\n*09/10/2026*\n \n- Fix.\n' > "$WORK/CHANGELOG.md"
run changelog_update "$WORK/CHANGELOG.md" "AWE" "4.13.2" "$DATE" '[]'
if [[ "$RC" -eq 0 ]] && ! grep -q 'Changed:' "$WORK/CHANGELOG.md" \
  && [[ "$(sed -n '/^# Release notes for AWE 4.13.1$/,$p' "$WORK/CHANGELOG.md")" == $'# Release notes for AWE 4.13.1\n*09/10/2026*\n \n- Fix.' ]]; then
  pass "S1 support line: draft removed, 4.13.1 intact"
else
  fail "S1 support line: draft removed, 4.13.1 intact" "$(cat "$WORK/CHANGELOG.md")"
fi

printf '\n%s passed, %s failed\n' "$PASSED" "$FAILED"
[[ "$FAILED" -eq 0 ]]
