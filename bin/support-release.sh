#!/bin/bash
set -euo pipefail

# ============================================================
# support-release.sh
#
# Non-interactive release of a `support/*` maintenance branch (e.g. support/4.x)
# using the gitflow-maven-plugin hotfix flow, followed by an explicit bump of
# the support branch to the next patch snapshot.
#
# gitflow-maven-plugin 1.13.0 has no "release from support branch" goal: the
# hotfix flow is the documented way to cut a release off a non-development,
# non-production branch. After hotfix-finish the branch stays on the release
# version (no automatic snapshot bump), so this script performs that bump
# itself, mirroring the two pom.xml fields ("version" and the "revision"
# property) that a normal gitflow release updates together.
#
# Usage:
#   bin/support-release.sh [--dry-run] [-h|--help]
#
# Environment:
#   RELEASE_VERSION   Optional. Overrides the version parsed from pom.xml
#                      (which must otherwise end in -SNAPSHOT). Example: 4.12.10
#
# Options:
#   --dry-run   Print every command instead of running anything that mutates
#               state (mvn release goals, git commit, git push). Preconditions
#               and version computation still run normally.
#   -h, --help  Show this help and exit.
# ============================================================

DRY_RUN=0

usage() {
  cat <<'EOF'
Usage: bin/support-release.sh [--dry-run] [-h|--help]

Non-interactive release from a support/* maintenance branch, using the
gitflow-maven-plugin hotfix flow, followed by a bump of the branch to the
next patch snapshot.

Options:
  --dry-run   Print every command instead of executing anything that
              mutates state (mvn release goals, git commit, git push).
              Preconditions and version computation still run.
  -h, --help  Show this help and exit.

Environment:
  RELEASE_VERSION  Optional override of the version to release. pom.xml is
                   still read and must be a -SNAPSHOT version; the next
                   development version is derived from the override.
                   Example: RELEASE_VERSION=4.12.10
EOF
}

for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
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

# ============================================================
# Preconditions
# ============================================================

# GitLab runners check out the pipeline commit as a detached HEAD, so the branch
# name must come from CI_COMMIT_REF_NAME there and a local branch must exist for
# gitflow:hotfix-start -DfromBranch and for the final push. If the job already
# created that local branch (release.sh commits the generated CHANGELOG on it,
# and that commit is not on origin yet), keep it as is: recreating it from
# origin would silently discard those commits. Outside CI, use HEAD.
if [[ -n "${CI_COMMIT_REF_NAME:-}" ]]; then
  SUPPORT_BRANCH="$CI_COMMIT_REF_NAME"
  [[ "$SUPPORT_BRANCH" == support/* ]] || fail "CI_COMMIT_REF_NAME '$SUPPORT_BRANCH' does not match support/*; this script only releases from a support branch."
  git fetch --quiet origin "$SUPPORT_BRANCH" || fail "could not fetch origin/$SUPPORT_BRANCH."
  if git rev-parse -q --verify "refs/heads/$SUPPORT_BRANCH" >/dev/null; then
    git merge-base --is-ancestor "origin/$SUPPORT_BRANCH" "refs/heads/$SUPPORT_BRANCH" \
      || fail "local branch $SUPPORT_BRANCH lacks commits of origin/$SUPPORT_BRANCH (the branch moved after the pipeline started, or the workspace holds a leftover branch); start the release from a new pipeline."
    # The Start a new release job resets the branch to CI_COMMIT_SHA before release.sh, so in CI the
    # only commit ahead of the pipeline commit is this job's "Generated CHANGELOG". Leftovers from
    # earlier jobs are therefore removed by the job itself; this check catches a missing reset.
    if [[ -n "${CI_COMMIT_SHA:-}" ]]; then
      git merge-base --is-ancestor "$CI_COMMIT_SHA" "refs/heads/$SUPPORT_BRANCH" \
        || fail "local branch $SUPPORT_BRANCH does not contain the pipeline commit $CI_COMMIT_SHA; it is a leftover of an earlier job."
    fi
    git checkout --quiet "$SUPPORT_BRANCH" || fail "could not check out local branch $SUPPORT_BRANCH."
  else
    git checkout --quiet -B "$SUPPORT_BRANCH" "origin/$SUPPORT_BRANCH" || fail "could not check out $SUPPORT_BRANCH from origin."
  fi
else
  SUPPORT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
  [[ "$SUPPORT_BRANCH" == support/* ]] || fail "current branch '$SUPPORT_BRANCH' does not match support/*; this script only releases from a support branch."
fi

# Only tracked-file changes matter for a release: an untracked file sitting in the
# tree (a build artifact, an IDE file, this very script before its first commit)
# does not affect what gets tagged and released.
if ! git diff --quiet HEAD --; then
  fail "working tree has uncommitted changes to tracked files; commit or stash before releasing."
fi

POM_VERSION=$(grep -m1 -o '<version>[^<]*</version>' pom.xml | sed -e 's/<version>//' -e 's/<\/version>//')
[[ -n "$POM_VERSION" ]] || fail "could not read <version> from pom.xml."
[[ "$POM_VERSION" == *-SNAPSHOT ]] || fail "pom.xml version '$POM_VERSION' does not end in -SNAPSHOT; nothing to release."

# ============================================================
# Version computation
# ============================================================

if [[ -n "${RELEASE_VERSION:-}" ]]; then
  echo "Using RELEASE_VERSION override: $RELEASE_VERSION"
else
  RELEASE_VERSION="${POM_VERSION%-SNAPSHOT}"
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$RELEASE_VERSION"
[[ -n "${MAJOR:-}" && -n "${MINOR:-}" && -n "${PATCH:-}" ]] || fail "could not parse major.minor.patch from version '$RELEASE_VERSION'."
NEXT_PATCH=$((PATCH + 1))
NEXT_VERSION="${MAJOR}.${MINOR}.${NEXT_PATCH}-SNAPSHOT"

echo "Support branch:   $SUPPORT_BRANCH"
echo "Release version:  $RELEASE_VERSION"
echo "Next dev version: $NEXT_VERSION"

# ============================================================
# Hotfix-based release
# ============================================================

run mvn -B gitflow:hotfix-start \
  -DfromBranch="$SUPPORT_BRANCH" \
  -DhotfixVersion="$RELEASE_VERSION" \
  -DpushRemote=false \
  -DversionProperty=revision

# hotfix-start names the branch hotfix/<support branch>/<version> (hotfix/support/4.x/4.12.10), and
# hotfix-finish looks up hotfix/<hotfixVersion>, so the support branch must be part of hotfixVersion.
run mvn -B gitflow:hotfix-finish \
  -DhotfixVersion="$SUPPORT_BRANCH/$RELEASE_VERSION" \
  -DskipMergeProdBranch=true \
  -DskipMergeDevBranch=true \
  -DskipTestProject=true \
  -DpushRemote=false \
  -DversionProperty=revision

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY-RUN: would verify tag v$RELEASE_VERSION exists and branch is back on $SUPPORT_BRANCH"
else
  git rev-parse -q --verify "refs/tags/v${RELEASE_VERSION}" >/dev/null \
    || fail "expected tag v$RELEASE_VERSION was not created by gitflow:hotfix-finish."

  CURRENT_BRANCH_AFTER_HOTFIX=$(git rev-parse --abbrev-ref HEAD)
  [[ "$CURRENT_BRANCH_AFTER_HOTFIX" == "$SUPPORT_BRANCH" ]] \
    || fail "expected to be back on '$SUPPORT_BRANCH' after gitflow:hotfix-finish, but current branch is '$CURRENT_BRANCH_AFTER_HOTFIX'."
fi

# ============================================================
# Bump the support branch to the next patch snapshot
# (mirrors both pom.xml fields a normal release updates: the root <version>
# and the <revision> property; see v4.12.9 commits d0c0a38e5 / bbac1effb)
# ============================================================

run mvn -B versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false
run mvn -B versions:set-property -Dproperty=revision -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false

# Module POMs carry literal versions (versions:set rewrites them too), so stage every pom.xml.
run git add -- $(git ls-files 'pom.xml' '*/pom.xml')
run git commit -m "chore(release): prepare next development iteration $NEXT_VERSION"

# ============================================================
# Publish
# ============================================================

run git push --atomic origin "$SUPPORT_BRANCH" "refs/tags/v$RELEASE_VERSION"

echo "Done."
