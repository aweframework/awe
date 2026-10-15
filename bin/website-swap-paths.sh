# shellcheck shell=sh
# ============================================================
# website-swap-paths.sh
#
# The names of the swap that bin/website-maintenance-docs.sh makes while it injects the 4.x docs: develop's website/docs and
# website/sidebars.js wait in these backups, relative to the repository root. Sourced by that script and by
# bin/local-ci-platform.sh (which heals a swap left by a killed run), so both always agree on the names.
#   WEBSITE_DOCS_BACKUP       develop's website/docs during the swap
#   WEBSITE_SIDEBARS_BACKUP   develop's website/sidebars.js during the swap
#   WEBSITE_SWAP_MARKER       written by the injection while the swap lasts: the pid and the start time of the process
#                             that swaps, so a later run can tell an orphan swap (that process is dead) from a live one
# ============================================================

WEBSITE_DOCS_BACKUP="website/.docs-current"
WEBSITE_SIDEBARS_BACKUP="website/.sidebars-current.js"
WEBSITE_SWAP_MARKER="website/.local-ci-swap"

# The start time of process "$1" (with the pid, it tells a live process from a reused pid), or nothing. Where /proc exists
# (Linux: procps-ng and BusyBox alike, whose ps has no -p or lstart) it is field 22 of /proc/<pid>/stat, in clock ticks since
# boot, written "proc:<ticks>"; elsewhere (macOS) it is ps -o lstart= with the C locale. A start time is only compared with one
# recorded in the same format, and a different format never counts as equal.
website_proc_start() {
  if [ -r "/proc/$1/stat" ]; then
    # the command name (field 2) may hold spaces and parentheses: count from the last ")"
    sed 's/.*) //' "/proc/$1/stat" 2>/dev/null | awk '{ if (NF >= 20) print "proc:" $20 }'
    return 0
  fi
  LC_ALL=C ps -o lstart= -p "$1" 2>/dev/null | tr -s ' ' | sed 's/^ //; s/ $//'
}

# Writes the owner of the swap into the marker: "$1" the pid of the process that swaps (run from the repository root)
website_swap_mark() { printf '%s\n%s\n' "$1" "$(website_proc_start "$1")" > "$WEBSITE_SWAP_MARKER"; }
