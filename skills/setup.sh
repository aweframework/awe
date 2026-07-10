#!/usr/bin/env bash
# Configure AWE Agent Skills for the AI coding assistant(s) you use.
#
# The single source of truth for skills lives in this `skills/` directory
# (agentskills.io standard). This script creates per-tool symlinks pointing back
# here so every assistant finds them in its expected location, without duplicating
# skill content. The symlinks are git-ignored; only `skills/` is versioned.
#
# Usage:
#   ./skills/setup.sh                 # Claude Code + Agents (AWE defaults)
#   ./skills/setup.sh --all           # every supported assistant
#   ./skills/setup.sh --claude --codex
#
# Supported: --claude --agents --gemini --codex   (or --all)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

# tool -> directory that should contain a `skills` symlink
declare -A TOOL_DIR=(
  [claude]=".claude"
  [agents]=".agents"
  [gemini]=".gemini"
  [codex]=".codex"
)

usage() { grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

link_tool() {
  local tool="$1" dir="${TOOL_DIR[$1]:-}"
  [ -n "$dir" ] || { echo "Unknown tool: $tool" >&2; return 1; }
  mkdir -p "$REPO_ROOT/$dir"
  rm -rf "$REPO_ROOT/$dir/skills"
  ln -s "../skills" "$REPO_ROOT/$dir/skills"
  add_to_gitignore "/$dir/skills"
  echo "  ✓ $dir/skills -> ../skills"
}

add_to_gitignore() {
  local pattern="$1" gi="$REPO_ROOT/.gitignore" header="# AI assistant skill symlinks (agentskills.io — source in /skills)"
  [ -f "$gi" ] || touch "$gi"
  grep -qxF "$pattern" "$gi" && return 0
  grep -qxF "$header" "$gi" || printf '\n%s\n' "$header" >> "$gi"
  echo "$pattern" >> "$gi"
}

# Resolve which tools to configure
tools=()
if [ "$#" -eq 0 ]; then
  tools=(claude agents)
else
  for arg in "$@"; do
    case "$arg" in
      --all) tools=(claude agents gemini codex) ;;
      --claude|--agents|--gemini|--codex) tools+=("${arg#--}") ;;
      -h|--help) usage 0 ;;
      *) echo "Unknown option: $arg" >&2; usage 1 ;;
    esac
  done
fi

echo "Linking skills for: ${tools[*]}"
for t in "${tools[@]}"; do link_tool "$t"; done
echo "Done. Restart your assistant to load skills from ./skills."
