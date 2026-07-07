# AI Agent Skills

This directory contains **Agent Skills** following the [Agent Skills open standard](https://agentskills.io). Skills provide domain-specific patterns, conventions, and guardrails that help AI coding assistants (Claude Code, Codex, Gemini CLI, etc.) understand AWE-specific requirements.

## What Are Skills?

[Agent Skills](https://agentskills.io) is an open standard format for extending AI agent capabilities with specialized knowledge. Originally developed by Anthropic and released as an open standard, it is now adopted by multiple agent products.

Skills teach AI assistants how to perform specific tasks. When an AI loads a skill, it gains context about:

- Critical rules (what to always/never do)
- Code patterns and conventions
- Project-specific workflows
- References to detailed documentation

## Setup

Run the setup script to configure skills for the AI coding assistant(s) you use.

**Linux / macOS** (also works under WSL and Git Bash on Windows):

```bash
./skills/setup.sh            # Claude Code + Agents (AWE defaults)
./skills/setup.sh --all      # every supported assistant
./skills/setup.sh --claude   # a specific one
```

**Windows** (native PowerShell):

```powershell
./skills/setup.ps1           # Claude Code + Agents (AWE defaults)
./skills/setup.ps1 -All      # every supported assistant
./skills/setup.ps1 -Claude   # a specific one
```

> **Note:** Creating symlinks on Windows requires Developer Mode enabled
> (Settings > Privacy & security > For developers) or an elevated PowerShell session.

Both scripts create symlinks so each tool finds skills in its expected location:

| Tool | Created by setup |
|------|------------------|
| Claude Code | `.claude/skills/` symlink |
| Agents (AGENTS.md-based) | `.agents/skills/` symlink |
| Gemini CLI | `.gemini/skills/` symlink |
| Codex (OpenAI) | `.codex/skills/` symlink |

The symlinks are added to `.gitignore`; only this `skills/` directory is versioned. Re-run the
script after cloning, then restart your AI coding assistant to load the skills.

## How to Use Skills

Skills are automatically discovered by the AI agent. To manually load a skill during a session:

```text
Read skills/{skill-name}/SKILL.md
```

## Available Skills

Patterns tailored for AWE (Almis Web Engine) development:

| Skill | Description |
|-------|-------------|
| `awe-framework` | AWE framework conventions and patterns: XML descriptor authoring (screens, queries, maintain, services, actions), Java service patterns, project structure, and build workflows |
| `awe-integration-tests` | Commands to run AWE's Spring integration and Selenium test suites locally |

## Directory Structure

```text
skills/
├── {skill-name}/
│   ├── SKILL.md              # Required - main instructions and metadata
│   ├── scripts/              # Optional - executable code
│   ├── assets/               # Optional - templates, schemas, resources
│   └── references/           # Optional - links to local docs
├── setup.sh                  # Linux / macOS symlink setup
├── setup.ps1                 # Windows (PowerShell) symlink setup
└── README.md                 # This file
```

## Why Reference Skills Explicitly?

**Problem**: AI assistants don't reliably auto-invoke skills even when the `Trigger:` in the skill
description matches the user's request. They often treat skill suggestions as background noise and
proceed with their default approach.

**Solution**: AWE's [`AGENTS.md`](../AGENTS.md) references skills directly where they matter — for
example, the integration and Selenium test commands point at the `awe-integration-tests` skill. Adding
an explicit "load skill X before doing Y" pointer in the agent instructions is a reliable way to make
the assistant pick up the right skill first.

## Creating New Skills

### Quick Checklist

1. Create the directory: `skills/{skill-name}/`
2. Add `SKILL.md` with the required frontmatter (`name`, `description` with a `Trigger:` clause)
3. Keep content concise (under 500 lines); reference existing docs instead of duplicating them
4. Re-run `./skills/setup.sh` (or `./skills/setup.ps1` on Windows) so every assistant picks it up
5. Reference the new skill from [`AGENTS.md`](../AGENTS.md) where the assistant should load it first
6. Add it to the **Available Skills** table above

## Design Principles

- **Concise**: Only include what the AI doesn't already know
- **Progressive disclosure**: Point to detailed docs, don't duplicate
- **Critical rules first**: Lead with ALWAYS/NEVER patterns
- **Minimal examples**: Show patterns, not tutorials

## Resources

- [Agent Skills Standard](https://agentskills.io) - Open standard specification
- [Agent Skills GitHub](https://github.com/anthropics/skills) - Example skills
- [Claude Code Best Practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices) - Skill authoring guide
- [AWE AGENTS.md](../AGENTS.md) - AI agent general rules for this repository
