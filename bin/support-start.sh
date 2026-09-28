#!/bin/bash
set -euo pipefail

# ============================================================
# support-start.sh
#
# Cuts a new `support/<major>.x` maintenance branch off a given ref (without
# changing its version), and prepares the `develop` bump to the next major
# `-SNAPSHOT` version on a separate branch, ready for a merge request.
#
# This script only ever pushes `support/<major>.x` and the bump branch, and
# only when `--push` is given. It never pushes to `develop` directly: the
# develop bump always goes through a merge request.
#
# Usage:
#   bin/support-start.sh <major> [--from <ref>] [--next-develop-version <version>]
#                                 [--push] [--dry-run] [-h|--help]
#
# Arguments:
#   <major>   The outgoing major version to freeze into a support branch,
#             e.g. "4" creates "support/4.x".
#
# Options:
#   --from <ref>                   Ref to cut both branches from.
#                                  Default: origin/develop
#   --next-develop-version <ver>   Version to bump the develop branch to.
#                                  Default: <major+1>.0.0-SNAPSHOT
#   --push                        Push support/<major>.x and the bump branch
#                                  to origin. Without it, nothing leaves the
#                                  machine.
#   --dry-run                     Print every command instead of running
#                                  anything that mutates state (git branch
#                                  creation, mvn versions:set, git commit,
#                                  git push). Preconditions still run.
#   -h, --help                    Show this help and exit.
# ============================================================

MAJOR=""
FROM_REF="origin/develop"
NEXT_DEVELOP_VERSION=""
PUSH=0
DRY_RUN=0

usage() {
  cat <<'EOF'
Usage: bin/support-start.sh <major> [--from <ref>] [--next-develop-version <version>] [--push] [--dry-run] [-h|--help]

Cuts support/<major>.x off a given ref (default: origin/develop) without
changing its version, then prepares a chore/bump-develop-<next> branch that
bumps develop to the next major -SNAPSHOT version, ready for a merge request.

Arguments:
  <major>   Outgoing major version, e.g. "4" creates "support/4.x".

Options:
  --from <ref>                   Ref to cut both branches from. Default: origin/develop
  --next-develop-version <ver>   Next develop version. Default: <major+1>.0.0-SNAPSHOT
  --push                        Push support/<major>.x and the bump branch to origin.
                                Without it, nothing leaves the machine.
  --dry-run                     Print every command instead of executing anything
                                that mutates state. Preconditions still run.
  -h, --help                    Show this help and exit.
EOF
}

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
# Argument parsing
# ============================================================

POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)
      [[ $# -ge 2 ]] || fail "--from requires a value."
      FROM_REF="$2"
      shift 2
      ;;
    --next-develop-version)
      [[ $# -ge 2 ]] || fail "--next-develop-version requires a value."
      NEXT_DEVELOP_VERSION="$2"
      shift 2
      ;;
    --push)
      PUSH=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
    *)
      POSITIONAL+=("$1")
      shift
      ;;
  esac
done

[[ ${#POSITIONAL[@]} -ge 1 ]] || { usage >&2; fail "missing required <major> argument."; }
[[ ${#POSITIONAL[@]} -le 1 ]] || fail "unexpected extra argument(s): ${POSITIONAL[*]:1}"
MAJOR="${POSITIONAL[0]}"
[[ "$MAJOR" =~ ^[0-9]+$ ]] || fail "<major> must be a plain integer (e.g. 4), got '$MAJOR'."

SUPPORT_BRANCH="support/${MAJOR}.x"
NEXT_MAJOR=$((MAJOR + 1))
[[ -n "$NEXT_DEVELOP_VERSION" ]] || NEXT_DEVELOP_VERSION="${NEXT_MAJOR}.0.0-SNAPSHOT"
BUMP_BRANCH="chore/bump-develop-${NEXT_DEVELOP_VERSION}"

echo "Support branch:       $SUPPORT_BRANCH"
echo "Cut from:             $FROM_REF"
echo "Bump branch:          $BUMP_BRANCH"
echo "Next develop version: $NEXT_DEVELOP_VERSION"
echo "Push:                 $([[ "$PUSH" == "1" ]] && echo yes || echo no)"
echo ""

# ============================================================
# Preconditions
# ============================================================

ORIGINAL_BRANCH=$(git rev-parse --abbrev-ref HEAD)

if ! git diff --quiet HEAD --; then
  fail "working tree has uncommitted changes to tracked files; commit or stash before cutting a support branch."
fi

git fetch --quiet origin || fail "could not fetch origin; check network access and remote configuration."

if git show-ref --verify --quiet "refs/heads/${SUPPORT_BRANCH}"; then
  fail "branch '${SUPPORT_BRANCH}' already exists locally."
fi
if git ls-remote --exit-code --heads origin "${SUPPORT_BRANCH}" >/dev/null 2>&1; then
  fail "branch '${SUPPORT_BRANCH}' already exists on origin."
fi

git rev-parse --quiet --verify "${FROM_REF}^{commit}" >/dev/null \
  || fail "ref '${FROM_REF}' does not resolve to a commit; fetch it or pass a different --from."

cleanup() {
  # Best-effort return to the original branch, even on early exit.
  git checkout --quiet "$ORIGINAL_BRANCH" 2>/dev/null || true
}
trap cleanup EXIT

# ============================================================
# Cut support/<major>.x from the ref, unchanged
# ============================================================

# Warn before anything is created or pushed: protected CI variables (Docker Hub, Maven
# repository, GPG, API token) are only injected into pipelines of protected branches, so
# Build package, Deploy snapshot and the release job fail on an unprotected support branch.
echo "NOTE: before the first pipeline runs on ${SUPPORT_BRANCH}, the branch pattern 'support/*' must be a"
echo "      protected branch in GitLab (Settings > Repository > Protected branches, same policy as develop)."
echo ""

echo "== Cutting ${SUPPORT_BRANCH} from ${FROM_REF} =="
run git branch "${SUPPORT_BRANCH}" "${FROM_REF}"

if [[ "$PUSH" == "1" ]]; then
  run git push origin "${SUPPORT_BRANCH}:${SUPPORT_BRANCH}"
fi

# ============================================================
# Prepare the develop bump on its own branch
# ============================================================

echo ""
echo "== Preparing ${BUMP_BRANCH} from ${FROM_REF} =="
run git checkout --quiet -B "${BUMP_BRANCH}" "${FROM_REF}"

run mvn -B versions:set -DnewVersion="${NEXT_DEVELOP_VERSION}" -DgenerateBackupPoms=false
run mvn -B versions:set-property -Dproperty=revision -DnewVersion="${NEXT_DEVELOP_VERSION}" -DgenerateBackupPoms=false

run git add -- $(git ls-files 'pom.xml' '*/pom.xml')
run git commit -m "chore(release): start the ${NEXT_MAJOR}.x development line (${NEXT_DEVELOP_VERSION})"

if [[ "$PUSH" == "1" ]]; then
  run git push origin "${BUMP_BRANCH}:${BUMP_BRANCH}"
fi

# ============================================================
# Follow-up
# ============================================================

echo ""
if [[ "$PUSH" == "1" ]]; then
  echo "Pushed ${SUPPORT_BRANCH} and ${BUMP_BRANCH} to origin."
  echo ""
  echo "Follow-up:"
  echo "  Open a merge request from ${BUMP_BRANCH} into develop (never push directly to develop)."
else
  echo "Nothing was pushed (run with --push to publish these branches). Local branches created:"
  echo "  ${SUPPORT_BRANCH}"
  echo "  ${BUMP_BRANCH}"
  echo ""
  echo "Follow-up commands:"
  echo "  git push origin ${SUPPORT_BRANCH}:${SUPPORT_BRANCH}"
  echo "  git push origin ${BUMP_BRANCH}:${BUMP_BRANCH}"
  echo "  Open a merge request from ${BUMP_BRANCH} into develop (never push directly to develop)."
fi

echo ""
echo "Reminder: the first pipeline on ${SUPPORT_BRANCH} needs the 'support/*' pattern to be a protected branch (see above)."
