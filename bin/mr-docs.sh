#!/bin/sh
set -eu

# ============================================================
# mr-docs.sh
#
# Warns when a merge request changes code but neither changes documentation nor says why it is not
# needed. It is the "MR docs warning" job of the pipeline (#784): the job is allowed to fail with
# exit code 64, so a merge request without docs shows a warning in the pipeline, never a failure.
#
# It is advisory: it ends with 0 or 64 and nothing else. When it cannot do its job (no base to compare with, a base commit
# that is not in the clone, a failing git command) it prints a notice and ends with 0.
#
# The merge request passes when any of these is true:
#   - it changes no code (only docs, tests, lockfiles, CI or dependency bumps);
#   - it changes documentation: website/ (docs, README, blog), CONTRIBUTING.md, README.md or AGENTS.md;
#   - its description ticks "[x] Docs updated" (the merge request template), or ticks "[x] No docs needed because:" with a
#     reason written after it (the ellipsis placeholder of the template is not a reason);
#   - its title is a dependency update (chore(deps) / build(deps)), which has no docs to write.
#
# Code is anything under awe-framework/, awe-samples/ or bin/, plus pom.xml, except test sources
# (src/test, __tests__, *.test.js, *.spec.js), lockfiles, markdown and generated files. awe-tests/ is test code and
# is not counted.
#
# Usage:
#   bin/mr-docs.sh                  base from CI_MERGE_REQUEST_DIFF_BASE_SHA (what the CI job does),
#                                   or the merge base with CI_MERGE_REQUEST_TARGET_BRANCH_NAME
#   bin/mr-docs.sh <base> [head]    explicit range; head defaults to HEAD. The files are those the head changed since its
#                                   merge base with <base> (git diff <base>...<head>), as a merge request shows them
#
# Environment:
#   CI_MERGE_REQUEST_DESCRIPTION   the description of the merge request (set by GitLab in merge request pipelines)
#   CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED   "true" when GitLab cut the description (it keeps 2700 characters);
#                                  the items cannot be read then, so the check does not warn
#   CI_MERGE_REQUEST_TITLE         the title of the merge request
#
# Exit codes: 0 fine or not checked, 64 warning (code changed, no docs, no reason).
# ============================================================

WARNING_EXIT=64
BASE=${1:-${CI_MERGE_REQUEST_DIFF_BASE_SHA:-}}
HEAD_REF=${2:-HEAD}

if [ -z "$BASE" ] && [ -n "${CI_MERGE_REQUEST_TARGET_BRANCH_NAME:-}" ]; then
  for target in "origin/$CI_MERGE_REQUEST_TARGET_BRANCH_NAME" "$CI_MERGE_REQUEST_TARGET_BRANCH_NAME"; do
    if BASE=$(git merge-base "$target" "$HEAD_REF" 2>/dev/null); then
      break
    fi
    BASE=
  done
fi

if [ -z "$BASE" ]; then
  echo "MR docs: no base to compare with (pass it as the first argument or set CI_MERGE_REQUEST_DIFF_BASE_SHA or"
  echo "CI_MERGE_REQUEST_TARGET_BRANCH_NAME). Not checked."
  exit 0
fi

TITLE=${CI_MERGE_REQUEST_TITLE:-}
DESCRIPTION=${CI_MERGE_REQUEST_DESCRIPTION:-}

case "$TITLE" in
  "chore(deps"*|"build(deps"*|"Draft: chore(deps"*|"Draft: build(deps"*|"[Draft] chore(deps"*|"[Draft] build(deps"*)
    echo "MR docs: a dependency update, no docs expected."
    exit 0
    ;;
esac

# The files the merge request changes. In a merge request pipeline CI_MERGE_REQUEST_DIFF_BASE_SHA already is the merge
# base, so it is compared directly (no merge-base computation, which a shallow clone may not afford). An explicit local
# base is compared through its merge base (three dots), so a base that moved on does not count other branches' changes.
# A base git cannot use (e.g. not in a shallow clone) is not a failure: the check is advisory. Git's messages stay on
# stderr so they never reach the list of changed files.
if [ -n "${1:-}" ]; then
  RANGE="$BASE...$HEAD_REF"
else
  RANGE="$BASE..$HEAD_REF"
fi
GIT_ERRORS=$(mktemp)
if ! CHANGED=$(git -c core.quotepath=off diff --name-only "$RANGE" -- 2>"$GIT_ERRORS"); then
  echo "MR docs: git could not compare $RANGE (is the base commit in a shallow clone?). Not checked."
  head -3 "$GIT_ERRORS" | sed 's/^/  /'
  rm -f "$GIT_ERRORS"
  exit 0
fi
rm -f "$GIT_ERRORS"

# Documentation touched
DOCS=$(printf '%s\n' "$CHANGED" | grep -E '^(website/|CONTRIBUTING\.md$|README\.md$|AGENTS\.md$)' || true)
if [ -n "$DOCS" ]; then
  echo "MR docs: documentation changed ($(printf '%s\n' "$DOCS" | wc -l | tr -d ' ') files)."
  exit 0
fi

# Code touched: product paths, minus tests, lockfiles, markdown and generated files
CODE=$(printf '%s\n' "$CHANGED" \
  | grep -E '^(awe-framework/|awe-samples/|bin/|pom\.xml$)' \
  | grep -vE '(^|/)(src/test|__tests__|generated|target|node_modules)/' \
  | grep -vE '\.(test|spec)\.(js|jsx|ts|tsx)$' \
  | grep -vE '(^|/)(package-lock\.json|yarn\.lock|pnpm-lock\.yaml|skills-lock\.json)$|\.(lock|md|svg|min\.js|min\.css)$' \
  || true)
if [ -z "$CODE" ]; then
  echo "MR docs: no code changed, no docs expected."
  exit 0
fi

# The items of the merge request template
if printf '%s\n' "$DESCRIPTION" | grep -qiE '^[[:space:]]*[-*][[:space:]]+\[x\][[:space:]]+docs updated'; then
  echo "MR docs: the description says the docs are updated."
  exit 0
fi

REASON=$(printf '%s\n' "$DESCRIPTION" \
  | sed -n 's/^[[:space:]]*[-*][[:space:]]*\[[xX]\][[:space:]]*[Nn]o docs needed because:\(.*\)$/\1/p' \
  | head -1)
# The placeholder of the template (an ellipsis) is not a reason
if [ -n "$(printf '%s' "$REASON" | sed 's/…//g; s/\.//g; s/[[:space:]]//g')" ]; then
  echo "MR docs: the description gives a reason why no docs are needed."
  exit 0
fi

if [ "${CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED:-false}" = "true" ]; then
  echo "MR docs: the description is too long for GitLab to pass it whole, so the docs items cannot be read. Not checked."
  exit 0
fi

echo "MR docs warning: this merge request changes code but no documentation, and says nothing about it."
echo
echo "Code changed ($(printf '%s\n' "$CODE" | wc -l | tr -d ' ') files), for example:"
printf '%s\n' "$CODE" | head -5 | sed 's/^/  /'
echo
echo "Either update the docs (website/docs, CONTRIBUTING.md or README.md) in this merge request, or tick one item of the"
echo "description: \"Docs updated\", or \"No docs needed because:\" followed by the reason (a placeholder is not a reason)."
echo "This is a warning only: the pipeline keeps going. See \"Definition of done\" in CONTRIBUTING.md."
exit "$WARNING_EXIT"
