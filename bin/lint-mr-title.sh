#!/bin/sh
set -eu

# ============================================================
# lint-mr-title.sh
#
# Checks that a merge request title follows Conventional Commits:
#
#     type(scope): description (#issue Ttask)
#
# It is the "MR title lint" job of the pipeline (#762). The title is the commit message that
# lands on develop (squash merge), so a title that does not follow the format breaks the history
# conventions and the changelog.
#
# Usage:
#   bin/lint-mr-title.sh "feat(awe-model): new option model (#795 T1a)"
#   CI_MERGE_REQUEST_TITLE="..." bin/lint-mr-title.sh      (what the CI job does)
#
# Exit code: 0 when the title is valid, 1 when it is not.
#
# Rules live in .gitlab/commitlint/commitlint.config.mjs; the pinned tool versions in
# .gitlab/commitlint/package.json and package-lock.json (Renovate updates them). commitlint is
# installed with `npm ci --include=dev` on first use (the CI sets NODE_ENV=production, which would skip
# devDependencies). It needs Node.js 22.12 or later.
# ============================================================

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LINT_DIR="$ROOT/.gitlab/commitlint"
COMMITLINT="$LINT_DIR/node_modules/.bin/commitlint"

TITLE=${1-${CI_MERGE_REQUEST_TITLE-}}

# GitLab keeps the "Draft:" prefix in the title while the merge request is a draft: it is not part
# of the future commit message, so it is ignored.
TITLE=$(printf '%s' "$TITLE" | sed -e 's/^[Dd][Rr][Aa][Ff][Tt]: *//' -e 's/^\[[Dd][Rr][Aa][Ff][Tt]\] *//' -e 's/^([Dd][Rr][Aa][Ff][Tt]) *//')

explain() {
  cat >&2 <<EOF

The merge request title must follow Conventional Commits:

    type(scope): description (#issue Ttask)

  type   feat, fix, chore, ci, docs, test, refactor, build, perf, style or revert
  scope  optional, the module or area: awe-client-react, awe-model, deps, ci...
  end    the issue reference goes last, e.g. (#794 T7) or (#795)

Examples:
    feat(awe-model): ECharts option model for charts (#795 T1a)
    fix: guard null dates in DateUtil.asLocalTime
    chore(deps): update dependency postcss to v8.5.28 (develop)

Edit the title of the merge request and the check runs again.
EOF
}

if [ -z "$TITLE" ]; then
  echo "The merge request title is empty." >&2
  explain
  exit 1
fi

if [ ! -x "$COMMITLINT" ]; then
  npm ci --prefix "$LINT_DIR" --include=dev --ignore-scripts --no-audit --no-fund --loglevel=error >&2
fi

if printf '%s\n' "$TITLE" | "$COMMITLINT" --config "$LINT_DIR/commitlint.config.mjs" >&2; then
  echo "Merge request title OK: $TITLE"
else
  explain
  exit 1
fi
