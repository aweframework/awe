#!/bin/bash
set -euo pipefail

# ============================================================
# website-maintenance-docs.sh
#
# Injects the maintenance-line documentation (default: support/4.x) into the
# website as a Docusaurus versioned-docs snapshot before the site is built,
# so every develop/master website build publishes a live "<label>
# (maintenance)" version tracking that branch, without ever touching
# develop's own website/docs or website/sidebars.js.
#
# How it works: fetch the maintenance branch, export its website/docs and
# website/sidebars.js, temporarily swap them in as the *current* docs, run
# Docusaurus' own `docs:version <label>` to snapshot them into
# versioned_docs/version-<label> and versioned_sidebars/version-<label>-
# sidebars.json (and prepend <label> to versions.json), then restore
# develop's docs/sidebars.js. Those generated paths are the intended output
# of this script and are left in place for the following `npm run build`;
# they are gitignored and must never be committed (see website/.gitignore).
#
# Usage:
#   bin/website-maintenance-docs.sh [--branch support/4.x] [--label 4.x] [--dry-run] [-h|--help]
#
# Must be run from the repository root.
#
# Environment:
#   MAINTENANCE_BRANCH   Overrides the default branch (support/4.x).
#   MAINTENANCE_LABEL    Overrides the default version label (4.x).
#                         An explicit --branch/--label flag wins over these.
#
# Options:
#   --dry-run   Print every command instead of running anything that mutates
#               the working tree (the idempotency cleanup and the docs:version
#               generation). The origin fetch that checks whether the
#               maintenance branch exists still runs for real, so the script
#               can report accurately whether it would be a no-op.
#   -h, --help  Show this help and exit.
# ============================================================

BRANCH="${MAINTENANCE_BRANCH:-support/4.x}"
LABEL="${MAINTENANCE_LABEL:-4.x}"
DRY_RUN=0

usage() {
  cat <<'EOF'
Usage: bin/website-maintenance-docs.sh [--branch support/4.x] [--label 4.x] [--dry-run] [-h|--help]

Injects the maintenance-line documentation (default: support/4.x) into the
website as a Docusaurus versioned-docs snapshot before the site is built.
Must be run from the repository root.

Options:
  --branch BRANCH  Branch to fetch the maintenance docs from (default: support/4.x,
                    or $MAINTENANCE_BRANCH).
  --label LABEL    Docusaurus version label to generate (default: 4.x,
                    or $MAINTENANCE_LABEL).
  --dry-run        Print every command instead of executing anything that
                    mutates the working tree. The existence check against
                    origin still runs for real.
  -h, --help       Show this help and exit.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --branch)
      [[ $# -ge 2 ]] || { echo "Missing value for --branch" >&2; exit 1; }
      BRANCH="$2"
      shift 2
      ;;
    --label)
      [[ $# -ge 2 ]] || { echo "Missing value for --label" >&2; exit 1; }
      LABEL="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

run() {
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "DRY-RUN: $*"
  else
    echo "+ $*"
    "$@"
  fi
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

echo "Maintenance branch: $BRANCH"
echo "Version label:      $LABEL"

# ============================================================
# 1. Fetch the maintenance branch. A missing branch is a valid, expected
#    state (e.g. before the branch is cut, or after its support window
#    ends): the site still builds fine without the maintenance version.
# ============================================================

# Distinguish "the branch does not exist" (expected, skip) from "the remote could not be
# queried" (network or credentials): the latter must fail the build instead of silently
# publishing the site without the maintenance version.
# Exact ref (refs/heads/...) so a similarly named branch cannot match; stderr is kept so a
# network or credential problem is visible in the job log.
set +e
git ls-remote --exit-code --heads origin "refs/heads/$BRANCH" > /dev/null
ls_remote_status=$?
set -e
if [[ "$ls_remote_status" -eq 2 ]]; then
  echo "origin/$BRANCH does not exist; skipping maintenance docs injection. The site still builds fine without the maintenance version."
  exit 0
elif [[ "$ls_remote_status" -ne 0 ]]; then
  fail "could not query origin for $BRANCH (git ls-remote exit code $ls_remote_status); refusing to build the site without the maintenance version."
fi
git fetch --depth=1 origin "$BRANCH" || fail "could not fetch origin/$BRANCH."

# ============================================================
# 2. Export website/docs and website/sidebars.js from the fetched branch
#    into a scratch directory, and set up cleanup/restore for everything
#    from here on.
# ============================================================

EXPORT_DIR=$(mktemp -d)
DOCS_BACKUP="website/.docs-current"
SIDEBARS_BACKUP="website/.sidebars-current.js"
SWAPPED=0

restore() {
  local status=$?
  if [[ "$SWAPPED" == "1" ]]; then
    echo "Restoring develop's website/docs and website/sidebars.js..."
    rm -rf website/docs
    rm -f website/sidebars.js
    mv "$DOCS_BACKUP" website/docs
    mv "$SIDEBARS_BACKUP" website/sidebars.js
  fi
  rm -rf "$EXPORT_DIR"
  exit "$status"
}
trap restore EXIT

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY-RUN: git archive FETCH_HEAD website/docs website/sidebars.js | tar -x -C $EXPORT_DIR"
else
  echo "+ git archive FETCH_HEAD website/docs website/sidebars.js | tar -x -C $EXPORT_DIR"
  git archive FETCH_HEAD website/docs website/sidebars.js | tar -x -C "$EXPORT_DIR" \
    || fail "could not export website/docs and website/sidebars.js from origin/$BRANCH."
fi

if [[ "$DRY_RUN" != "1" ]]; then
  [[ -d "$EXPORT_DIR/website/docs" && -f "$EXPORT_DIR/website/sidebars.js" ]] \
    || fail "origin/$BRANCH does not contain website/docs and website/sidebars.js."
fi

# ============================================================
# 3. Idempotency: drop any previous version-$LABEL before regenerating it,
#    so a re-run never fails on stale output.
# ============================================================

VERSIONED_DOCS_DIR="website/versioned_docs/version-$LABEL"
VERSIONED_SIDEBAR_FILE="website/versioned_sidebars/version-$LABEL-sidebars.json"

run rm -rf "$VERSIONED_DOCS_DIR"
run rm -f "$VERSIONED_SIDEBAR_FILE"

remove_version_entry() {
  node -e '
    const fs = require("fs");
    const path = "website/versions.json";
    const label = process.argv[1];
    const versions = JSON.parse(fs.readFileSync(path, "utf8"));
    const filtered = versions.filter((v) => v !== label);
    fs.writeFileSync(path, JSON.stringify(filtered, null, 2) + "\n");
  ' "$LABEL"
}

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY-RUN: remove any existing \"$LABEL\" entry from website/versions.json"
else
  echo "+ remove any existing \"$LABEL\" entry from website/versions.json"
  remove_version_entry
fi

# ============================================================
# 4. Swap develop's current docs/sidebars.js out, put the maintenance
#    branch's docs/sidebars.js in, snapshot them with docs:version. The
#    EXIT trap installed above ALWAYS restores develop's docs/sidebars.js
#    afterwards, even on failure.
# ============================================================

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY-RUN: mv website/docs $DOCS_BACKUP"
  echo "DRY-RUN: mv website/sidebars.js $SIDEBARS_BACKUP"
  echo "DRY-RUN: cp -r $EXPORT_DIR/website/docs website/docs"
  echo "DRY-RUN: cp $EXPORT_DIR/website/sidebars.js website/sidebars.js"
  echo "DRY-RUN: npm --prefix website run docusaurus -- docs:version $LABEL"
  echo "DRY-RUN: restore develop's website/docs and website/sidebars.js"
else
  rm -rf "$DOCS_BACKUP" "$SIDEBARS_BACKUP"

  echo "+ mv website/docs $DOCS_BACKUP"
  mv website/docs "$DOCS_BACKUP"
  echo "+ mv website/sidebars.js $SIDEBARS_BACKUP"
  mv website/sidebars.js "$SIDEBARS_BACKUP"
  SWAPPED=1

  echo "+ cp -r $EXPORT_DIR/website/docs website/docs"
  cp -r "$EXPORT_DIR/website/docs" website/docs
  echo "+ cp $EXPORT_DIR/website/sidebars.js website/sidebars.js"
  cp "$EXPORT_DIR/website/sidebars.js" website/sidebars.js

  echo "+ npm --prefix website run docusaurus -- docs:version $LABEL"
  npm --prefix website run docusaurus -- docs:version "$LABEL"

  # docs:version also copies the translations of the current docs (website/i18n/<locale>/.../current) into the new
  # version. Those pages belong to develop, not to the maintenance line (they link pages the line does not have), so
  # they are removed: the maintenance version shows its own English pages in every locale.
  for translated in website/i18n/*/docusaurus-plugin-content-docs/version-"$LABEL"; do
    if [[ -d "$translated" ]]; then
      echo "+ rm -rf $translated (copy of the current translations, not of $LABEL)"
      rm -rf "$translated"
    fi
  done
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY-RUN: would create website/versioned_docs/version-$LABEL and website/versioned_sidebars/version-$LABEL-sidebars.json, and prepend \"$LABEL\" to website/versions.json."
else
  echo "Created website/versioned_docs/version-$LABEL and website/versioned_sidebars/version-$LABEL-sidebars.json."
  echo "Prepended \"$LABEL\" to website/versions.json."
fi
