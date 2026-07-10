#!/usr/bin/env pwsh
# Configure AWE Agent Skills for the AI coding assistant(s) you use - Windows / PowerShell.
#
# Windows counterpart of setup.sh. The single source of truth for skills lives in this
# `skills/` directory (agentskills.io standard). This script creates per-tool symlinks
# pointing back here so every assistant finds them in its expected location, without
# duplicating skill content. The symlinks are git-ignored; only `skills/` is versioned.
#
# Usage (from the repo root, in PowerShell):
#   ./skills/setup.ps1                 # Claude Code + Agents (AWE defaults)
#   ./skills/setup.ps1 -All            # every supported assistant
#   ./skills/setup.ps1 -Claude -Codex  # a specific selection
#
# Supported switches: -Claude -Agents -Gemini -Codex   (or -All)
#
# NOTE: Creating symlinks on Windows requires either Developer Mode enabled
#       (Settings > Privacy & security > For developers) or an elevated
#       (Administrator) PowerShell session.

[CmdletBinding()]
param(
  [switch]$All,
  [switch]$Claude,
  [switch]$Agents,
  [switch]$Gemini,
  [switch]$Codex,
  [switch]$Help
)

$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $PSCommandPath
$RepoRoot  = Split-Path -Parent $ScriptDir

# tool -> directory that should contain a `skills` symlink
$ToolDir = [ordered]@{
  claude = '.claude'
  agents = '.agents'
  gemini = '.gemini'
  codex  = '.codex'
}

function Show-Usage {
  Get-Content -LiteralPath $PSCommandPath |
    Where-Object { $_ -match '^#' } |
    ForEach-Object { $_ -replace '^#\s?', '' }
}

function Add-ToGitignore {
  param([string]$Pattern)
  $gi = Join-Path $RepoRoot '.gitignore'
  $header = '# AI assistant skill symlinks (agentskills.io - source in /skills)'
  if (-not (Test-Path -LiteralPath $gi)) { New-Item -ItemType File -Path $gi | Out-Null }
  $lines = @(Get-Content -LiteralPath $gi)
  if ($lines -contains $Pattern) { return }
  if ($lines -notcontains $header) { Add-Content -LiteralPath $gi -Value @('', $header) }
  Add-Content -LiteralPath $gi -Value $Pattern
}

function New-SkillLink {
  param([string]$Tool)
  $dir = $ToolDir[$Tool]
  if (-not $dir) { Write-Warning "Unknown tool: $Tool"; return }
  $toolPath = Join-Path $RepoRoot $dir
  $linkPath = Join-Path $toolPath 'skills'
  if (-not (Test-Path -LiteralPath $toolPath)) { New-Item -ItemType Directory -Path $toolPath | Out-Null }
  if (Test-Path -LiteralPath $linkPath) { Remove-Item -LiteralPath $linkPath -Recurse -Force }
  try {
    New-Item -ItemType SymbolicLink -Path $linkPath -Target '../skills' -ErrorAction Stop | Out-Null
  } catch {
    Write-Warning "Could not create $dir\skills symlink. Enable Developer Mode or run PowerShell as Administrator. $($_.Exception.Message)"
    return
  }
  Add-ToGitignore "/$dir/skills"
  Write-Host "  + $dir\skills -> ..\skills"
}

if ($Help) { Show-Usage; exit 0 }

# Resolve which tools to configure
$tools = @()
if ($All) {
  $tools = @('claude', 'agents', 'gemini', 'codex')
} else {
  if ($Claude) { $tools += 'claude' }
  if ($Agents) { $tools += 'agents' }
  if ($Gemini) { $tools += 'gemini' }
  if ($Codex)  { $tools += 'codex' }
  if ($tools.Count -eq 0) { $tools = @('claude', 'agents') }  # AWE defaults
}

Write-Host "Linking skills for: $($tools -join ', ')"
foreach ($t in $tools) { New-SkillLink $t }
Write-Host "Done. Restart your assistant to load skills from ./skills."
