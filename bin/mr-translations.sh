#!/bin/sh
set -eu

# ============================================================
# mr-translations.sh
#
# Warns when a merge request changes an English page of the current docs (website/docs) whose Spanish
# translation exists (website/i18n/es/docusaurus-plugin-content-docs/current, same path) and does not change
# that translation in the same merge request (#784). It is the "MR translation drift warning" job of the
# pipeline: the job is allowed to fail with exit code 64, so a stale translation shows a warning, never a failure.
#
# Spanish lives in the repository (no Crowdin), and a page without a translation falls back to English, so only a
# page that is translated can drift. Frozen versions (versioned_docs) are history and are not checked.
#
# It is advisory: it ends with 0 or 64 and nothing else. When it cannot do its job (no base to compare with, a base
# commit that is not in the clone, a failing git command) it prints a notice and ends with 0.
#
# Usage:
#   bin/mr-translations.sh                  base from CI_MERGE_REQUEST_DIFF_BASE_SHA (what the CI job does),
#                                           or the merge base with CI_MERGE_REQUEST_TARGET_BRANCH_NAME
#   bin/mr-translations.sh <base> [head]    explicit range; head defaults to HEAD (git diff <base>...<head>)
#
# Exit codes: 0 fine or not checked, 64 warning (a translated page changed in English only).
# ============================================================

WARNING_EXIT=64
DOCS_DIR=website/docs
TRANSLATIONS_DIR=website/i18n/es/docusaurus-plugin-content-docs/current
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
  echo "MR translations: no base to compare with (pass it as the first argument or set CI_MERGE_REQUEST_DIFF_BASE_SHA or"
  echo "CI_MERGE_REQUEST_TARGET_BRANCH_NAME). Not checked."
  exit 0
fi

# In a merge request pipeline CI_MERGE_REQUEST_DIFF_BASE_SHA already is the merge base; an explicit local base is
# compared through its merge base (three dots), as in mr-docs.sh.
if [ -n "${1:-}" ]; then
  RANGE="$BASE...$HEAD_REF"
else
  RANGE="$BASE..$HEAD_REF"
fi

GIT_ERRORS=$(mktemp)
trap 'rm -f "$GIT_ERRORS"' EXIT INT TERM
# Names of the files the merge request changes. Renames are not followed: a moved page has no translation at its new
# path, and the old translation is a different matter than drift.
if ! CHANGED=$(git -c core.quotepath=off diff --name-only --no-renames "$RANGE" -- 2>"$GIT_ERRORS"); then
  echo "MR translations: git could not compare $RANGE (is the base commit in a shallow clone?). Not checked."
  head -3 "$GIT_ERRORS" | sed 's/^/  /'
  exit 0
fi

# English pages that the merge request modified or added and the translations it changed; a deleted page has nothing
# to translate, so it is skipped by asking for the file in the head
STALE=""
COUNT=0
IFS="
"
for page in $(printf '%s\n' "$CHANGED" | grep -E "^$DOCS_DIR/.+\.mdx?\$" || true); do
  relative=${page#"$DOCS_DIR"/}
  translation="$TRANSLATIONS_DIR/$relative"
  if ! git cat-file -e "$HEAD_REF:$page" 2>/dev/null; then
    continue
  fi
  if ! git cat-file -e "$HEAD_REF:$translation" 2>/dev/null; then
    continue
  fi
  if printf '%s\n' "$CHANGED" | grep -qxF "$translation"; then
    continue
  fi
  STALE="$STALE$page -> $translation
"
  COUNT=$((COUNT + 1))
done

if [ "$COUNT" -eq 0 ]; then
  echo "MR translations: no translated page is changed in English only."
  exit 0
fi

echo "MR translation drift warning: this merge request changes $COUNT English page(s) whose Spanish translation exists"
echo "and is not changed in the same merge request."
echo
printf '%s' "$STALE" | sed 's/^/  /'
echo
echo "Update the Spanish page too (website/i18n/es/docusaurus-plugin-content-docs/current/...), or leave it: until it is"
echo "updated, the Spanish site shows the old text. This is a warning only: the pipeline keeps going. See"
echo "\"Translations\" in website/README.md."
exit "$WARNING_EXIT"
