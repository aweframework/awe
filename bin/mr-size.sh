#!/bin/sh
set -eu

# ============================================================
# mr-size.sh
#
# Counts the lines a merge request changes (additions plus deletions) and warns when there are
# more than 400, the size a reviewer can still read with attention. It is the "MR size warning"
# job of the pipeline (#762): the job is allowed to fail with exit code 64, so a big merge
# request shows a warning in the pipeline instead of a failure.
#
# Not counted: lockfiles (package-lock.json, yarn.lock, pnpm-lock.yaml, *.lock, skills-lock.json),
# generated files (*.min.js, *.min.css, *.svg images, generated/ folders, target/ output), the changelog, the
# website documentation (website/docs and website/i18n) and binary files.
#
# Usage:
#   bin/mr-size.sh                  base from CI_MERGE_REQUEST_DIFF_BASE_SHA (what the CI job does),
#                                   or the merge base with CI_MERGE_REQUEST_TARGET_BRANCH_NAME
#   bin/mr-size.sh <base> [head]    explicit range; head defaults to HEAD
#
# Environment:
#   MR_SIZE_THRESHOLD   changed lines above which the warning is raised (default 400)
#
# Exit codes: 0 within the threshold, 64 above it (warning), 2 when the base cannot be found.
# ============================================================

THRESHOLD=${MR_SIZE_THRESHOLD:-400}
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
  echo "mr-size: no base to compare with. Pass it as the first argument or set" >&2
  echo "CI_MERGE_REQUEST_DIFF_BASE_SHA or CI_MERGE_REQUEST_TARGET_BRANCH_NAME." >&2
  exit 2
fi

# Binary files show up as "-" in numstat and are skipped by the awk below
NUMSTAT=$(git -c core.quotepath=off diff --numstat "$BASE" "$HEAD_REF" -- . \
  ':(exclude,glob)**/package-lock.json' \
  ':(exclude,glob)**/yarn.lock' \
  ':(exclude,glob)**/pnpm-lock.yaml' \
  ':(exclude,glob)**/*.lock' \
  ':(exclude,glob)**/skills-lock.json' \
  ':(exclude,glob)**/*.min.js' \
  ':(exclude,glob)**/*.min.css' \
  ':(exclude,glob)**/*.svg' \
  ':(exclude,glob)**/generated/**' \
  ':(exclude,glob)**/target/**' \
  ':(exclude,glob)CHANGELOG.md' \
  ':(exclude,glob)website/docs/**' \
  ':(exclude,glob)website/i18n/**')

TOTAL=$(printf '%s\n' "$NUMSTAT" | awk -F '\t' 'NF >= 3 && $1 != "-" { total += $1 + $2 } END { print total + 0 }')

if [ "$TOTAL" -le "$THRESHOLD" ]; then
  echo "MR size: $TOTAL changed lines (additions plus deletions), within the $THRESHOLD of a comfortable review."
  exit 0
fi

echo "MR size warning: $TOTAL changed lines (additions plus deletions), more than the $THRESHOLD of a comfortable review."
echo "Lockfiles, generated files, the changelog, website documentation and binary files are not counted."
echo
echo "Biggest files:"
printf '%s\n' "$NUMSTAT" \
  | awk -F '\t' 'NF >= 3 && $1 != "-" { printf "%d\t%d\t%d\t%s\n", $1 + $2, $1, $2, $3 }' \
  | sort -rn \
  | head -10 \
  | awk -F '\t' '{ printf "  %6d  (+%d -%d)  %s\n", $1, $2, $3, $4 }'
echo
echo "Consider splitting it into smaller merge requests (see \"Merge request title and size\" in website/docs/guides/release-lines-and-support.md)."
echo "This is a warning only: the pipeline keeps going."
exit "$WARNING_EXIT"
