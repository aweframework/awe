#!/bin/sh
set -eu

# ============================================================
# check-website-anchors.sh
#
# Fails when the build of the website reports broken anchors in the CURRENT documentation (#784).
#
# Docusaurus has one setting for broken anchors (onBrokenAnchors in docusaurus.config.js) for the whole site, and it
# cannot tell the current docs from the frozen versions (website/versioned_docs and the injected 4.x maintenance line).
# The frozen versions have broken anchors that must stay as they are, so the setting stays at "warn" and this script reads
# the build log instead: it lists every page that has a broken anchor, ignores the pages of a versioned doc
# (/docs/4.12.0/..., /docs/4.x/..., with or without a locale prefix such as /es/) and fails for any other page: the
# current docs, the blog and the site pages.
#
# Usage:
#   npm --prefix website run build > website-build.log 2>&1
#   bin/check-website-anchors.sh website-build.log
#
# Exit codes: 0 no broken anchor outside the versioned docs, 1 there is at least one (or the log has an unknown format),
# 2 the log cannot be read.
# ============================================================

LOG=${1:-}

if [ -z "$LOG" ] || [ ! -r "$LOG" ]; then
  echo "check-website-anchors: pass the build log of the website as the first argument (readable file)." >&2
  exit 2
fi

# A page block of the Docusaurus report looks like this:
#   - Broken anchor on source page path = /docs/session:
#      -> linking to properties#session-properties (resolved as: /docs/properties#session-properties)
RESULT=$(awk '
  /^- Broken anchor on source page path = / {
    page = $0
    sub(/^- Broken anchor on source page path = /, "", page)
    sub(/:[ \t]*$/, "", page)
    pages++
    # versioned: /docs/<x.y.z|x.y|x.x>/... with an optional locale prefix
    versioned = (page ~ /^(\/[A-Za-z][A-Za-z-]*)?\/docs\/[0-9]+\.(x|[0-9]+)(\.[0-9]+)?(\/|$)/)
    if (!versioned) {
      current_pages++
    } else {
      versioned_pages++
    }
    next
  }
  /^[ \t]+-> linking to / {
    if (page != "" && !versioned) {
      print "  " page "   " $0
      current_links++
    } else if (page != "") {
      versioned_links++
    }
    next
  }
  /^[^ \t-]/ { page = "" }
  END {
    printf "SUMMARY %d %d %d %d %d\n", pages + 0, current_pages + 0, current_links + 0, versioned_pages + 0, versioned_links + 0
  }
' "$LOG")

SUMMARY=$(printf '%s\n' "$RESULT" | grep '^SUMMARY ' | tail -1)
DETAIL=$(printf '%s\n' "$RESULT" | grep -v '^SUMMARY ' || true)
set -- $SUMMARY
PAGES=$2
CURRENT_PAGES=$3
CURRENT_LINKS=$4
VERSIONED_PAGES=$5
VERSIONED_LINKS=$6

# The report header without a single parsed page means the log format changed: do not pass silently
if grep -q 'Docusaurus found broken anchors' "$LOG" && [ "$PAGES" -eq 0 ]; then
  echo "check-website-anchors: the log reports broken anchors but no page could be read from it (format changed?)." >&2
  exit 1
fi

if [ "$CURRENT_LINKS" -eq 0 ]; then
  echo "Broken anchors: none in the current docs ($VERSIONED_LINKS in $VERSIONED_PAGES pages of the frozen versions, ignored)."
  exit 0
fi

echo "Broken anchors in the current docs: $CURRENT_LINKS in $CURRENT_PAGES pages ($VERSIONED_LINKS in the frozen versions, ignored):"
printf '%s\n' "$DETAIL" | head -50
if [ "$CURRENT_LINKS" -gt 50 ]; then
  echo "  ... and $((CURRENT_LINKS - 50)) more"
fi
echo
echo "Fix the link, or add the anchor. Docusaurus only knows the anchors it renders itself: headings, list items, and"
echo "<Link id=\"...\"> or the Anchor component (src/components/Anchor, in an MDX page). A raw <a name=\"...\">, <a id=\"...\"> or"
echo "<span id=\"...\"> is not registered, so a link to it is reported as broken. See \"Writing docs\" in website/README.md."
echo "The frozen versions (website/versioned_docs and the 4.x maintenance line) are not checked."
exit 1
