#!/usr/bin/env bash
# Installs the Playwright browsers (Chromium and Firefox, with their system libraries) in the version that
# awe-dependencies declares, as the GitLab CI does. Selenium browsers are not installed: they run as docker containers.
# Usage: post-create.sh [path-to-pom]   (relative to the repository root; defaults to awe-framework/awe-dependencies/pom.xml)
set -euo pipefail

cd "$(dirname "$0")/.."

POM=${1:-awe-framework/awe-dependencies/pom.xml}
if [[ ! -r "$POM" ]]; then
  echo "Cannot read $POM (paths are relative to the repository root)" >&2
  exit 1
fi

# Exactly one value: the first match. It must be a plain version, not a Maven expression such as ${...}
PLAYWRIGHT_VERSION=$(sed -n 's:.*<playwright\.version>\(.*\)</playwright\.version>.*:\1:p' "$POM" | head -n 1)
if [[ ! "$PLAYWRIGHT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "playwright.version in $POM must be a plain version such as 1.63.0, found: '${PLAYWRIGHT_VERSION}'" >&2
  exit 1
fi

npx --yes "playwright@${PLAYWRIGHT_VERSION}" install --with-deps chromium firefox
