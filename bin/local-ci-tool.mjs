#!/usr/bin/env node
// ============================================================
// local-ci-tool.mjs
//
// The pure logic of bin/local-ci.sh, kept apart so bin/test-local-ci.sh can test it without Maven, Docker or a browser.
// Node.js only, no dependencies (it runs in the "MR · Script tests" job image as well as on a laptop).
//
//   node bin/local-ci-tool.mjs jobs                                  the local jobs: id, label, blocking, CI jobs
//   node bin/local-ci-tool.mjs rules <ci-file> <ci-job>              the merge-request rules of a CI job, resolved
//   node bin/local-ci-tool.mjs select <ci-file> <changed-paths-file> [--fast]  one line per local job: id, yes|no, reason
//   node bin/local-ci-tool.mjs suites <ci-file> <job-id> <browser>    the CI jobs (suites) of a browser job (TSV)
//   node bin/local-ci-tool.mjs coverage --diff <file> [--jacoco <xml>]... [--lcov <client-dir>=<lcov>]... [--threshold 80]
//   node bin/local-ci-tool.mjs sonar --diff <file> [--root <dir>]     cheap guard for a few Sonar rules (exit 64 on findings)
//   node bin/local-ci-tool.mjs summary <results-tsv>                  summary table; exit 1 when a blocking job failed
//
// The changed-path rules are read from .gitlab-ci.yml itself (anchors, aliases, extends, !reference), never copied: a job
// is selected exactly when its first merge-request rule whose rules:changes globs match a changed path says so. The globs
// are matched as GitLab does, with Ruby's File.fnmatch and FNM_PATHNAME | FNM_DOTMATCH | FNM_EXTGLOB: "*" never crosses a
// "/", "dir/**" is one segment below dir, "dir/**/*" is any depth.
// ============================================================

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// ------------------------------------------------------------
// Local jobs, in the order bin/local-ci.sh runs them. "ci" names the GitLab jobs whose merge-request rules select the
// local job (several: any of them); a name that starts with "." is a hidden template whose rules its jobs share (the browser
// suites: one CI job per suite). An empty "ci" list is a local check that runs for every merge request (the title, size and
// translation checks stopped being pipeline jobs, so they never block here either). "warn" lists the exit codes that are a
// warning, not a failure; without it they come from the allow_failure of the CI job (exit_codes, or "any" for true), as in
// GitLab. The local checks that only borrow the rules of a CI job ("Analysis · Sonar") set their own.
// ------------------------------------------------------------
export const JOBS = [
  { id: 'title', label: 'MR title lint (local)', ci: [], blocking: false, warn: '' },
  { id: 'mr-size', label: 'MR size warning (local)', ci: [], blocking: false, warn: '64' },
  { id: 'mr-docs', label: 'MR · Docs warning', ci: ['MR · Docs warning'], blocking: true },
  { id: 'mr-translations', label: 'MR translation drift (local)', ci: [], blocking: false, warn: '64' },
  { id: 'mr-checks-tests', label: 'MR · Script tests', ci: ['MR · Script tests'], blocking: true },
  { id: 'sonar-rules', label: 'Sonar rules guard (local)', ci: ['Analysis · Sonar'], blocking: false, warn: '64' },
  { id: 'spotless', label: 'Lint · Spotless', ci: ['Lint · Spotless'], blocking: true },
  { id: 'secret-detection', label: 'Security · Secrets (gitleaks, local)', ci: ['Security · Secrets'], blocking: false, warn: 'any' },
  { id: 'lint-frontend', label: 'Lint · Frontend', ci: ['Lint · Frontend'], blocking: true },
  { id: 'spanish-docs-lint', label: 'Lint · Spanish docs', ci: ['Lint · Spanish docs'], blocking: true },
  { id: 'build', label: 'Build · Maven (JDK 17)', ci: ['Build · Maven'], blocking: true },
  { id: 'all-ut', label: 'Unit · Java', ci: ['Unit · Java'], blocking: true },
  { id: 'embedded-db', label: 'Database · H2 and HSQL', ci: ['Database · H2', 'Database · H2 · Flyway', 'Database · HSQL · Flyway'], blocking: true },
  { id: 'db-mysql', label: 'Database · MySQL', ci: ['Database · MySQL', 'Database · MySQL · Flyway'], blocking: true },
  { id: 'db-postgresql', label: 'Database · PostgreSQL', ci: ['Database · PostgreSQL', 'Database · PostgreSQL · Flyway'], blocking: true },
  { id: 'db-sqlserver', label: 'Database · SQL Server', ci: ['Database · SQL Server', 'Database · SQL Server · Flyway'], blocking: true },
  { id: 'db-oracle', label: 'Database · Oracle', ci: ['Database · Oracle', 'Database · Oracle · Flyway'], blocking: true },
  { id: 'frontend-ut', label: 'Unit · AngularJS', ci: ['Unit · AngularJS'], blocking: true },
  { id: 'frontend-ut-react', label: 'Unit · React', ci: ['Unit · React'], blocking: true },
  { id: 'javadoc', label: 'Docs · Javadoc check', ci: ['Docs · Javadoc check'], blocking: true },
  { id: 'coverage', label: 'New-code coverage (Sonar gate)', ci: ['Analysis · Sonar'], blocking: true, warn: '' },
  { id: 'website', label: 'Build · Website (+es, 4.x, anchors)', ci: ['Build · Website'], blocking: true },
  { id: 'playwright-angular', label: 'AngularJS browser suites', ci: ['.playwright-angularjs-chromium'], blocking: true },
  { id: 'playwright-react', label: 'React browser suites', ci: ['.playwright-react-chromium'], blocking: true },
];

// The Playwright suite templates of each local browser job and browser: their jobs are the suites (Firefox runs on develop
// and master only, so a merge request selects a browser job through its Chromium template)
export const SUITE_TEMPLATES = {
  'playwright-angular': { chromium: '.playwright-angularjs-chromium', firefox: '.playwright-angularjs-firefox' },
  'playwright-react': { chromium: '.playwright-react-chromium', firefox: '.playwright-react-firefox' },
};

// ------------------------------------------------------------
// A small reader for the subset of YAML that .gitlab-ci.yml uses: top-level blocks with anchors, maps and lists by
// indentation, aliases (*name), merge keys (<<: *name), extends and !reference. Enough to resolve rules; not a YAML parser.
// ------------------------------------------------------------

const indentOf = (line) => line.length - line.trimStart().length;
const isBlank = (line) => line.trim() === '' || line.trimStart().startsWith('#');
const unquote = (value) => value.trim().replace(/^'(.*)'$/, '$1').replace(/^"(.*)"$/, '$1');
// Strips a trailing comment ("value   # comment") that is not inside quotes
const stripComment = (value) => {
  let quote = null;
  for (let i = 0; i < value.length; i++) {
    const c = value[i];
    if (quote) {
      if (c === quote) quote = null;
    } else if (c === '\'' || c === '"') {
      quote = c;
    } else if (c === '#' && (i === 0 || /\s/.test(value[i - 1]))) {
      return value.slice(0, i).trimEnd();
    }
  }
  return value;
};

export function parseCi(text) {
  const lines = text.split('\n');
  const blocks = new Map(); // name -> body lines (raw)
  const anchors = new Map(); // anchor -> block name
  let current = null;
  for (const line of lines) {
    if (!isBlank(line) && indentOf(line) === 0) {
      const match = /^(.+?):(?:\s+(.*))?$/.exec(stripComment(line));
      if (!match) { current = null; continue; }
      const name = unquote(match[1]);
      current = { name, body: [], value: (match[2] || '').trim() };
      blocks.set(name, current);
      const anchor = /^&([\w-]+)/.exec(current.value);
      if (anchor) anchors.set(anchor[1], name);
      continue;
    }
    if (current) current.body.push(line);
  }
  return { blocks, anchors };
}

// The lines of the list items in "lines" (all at the indentation of their first "- "), as [{ first, rest }]
function listItems(lines) {
  const items = [];
  const meaningful = lines.filter((l) => !isBlank(l));
  if (meaningful.length === 0) return items;
  const itemIndent = indentOf(meaningful[0]);
  for (const line of meaningful) {
    const trimmed = line.trimStart();
    if (indentOf(line) === itemIndent && trimmed.startsWith('-')) {
      items.push({ first: stripComment(trimmed.replace(/^-\s*/, '')), rest: [], indent: itemIndent });
    } else if (items.length > 0 && indentOf(line) > itemIndent) {
      items[items.length - 1].rest.push(line);
    }
  }
  return items;
}

// The value of key "key" among the direct children of a map whose keys sit at "indent": { inline, children } or null
function mapEntry(lines, key, indent) {
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (isBlank(line) || indentOf(line) !== indent) continue;
    const match = new RegExp(`^${key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}:(?:\\s+(.*))?$`).exec(stripComment(line.trimStart()));
    if (!match) continue;
    const children = [];
    for (let j = i + 1; j < lines.length; j++) {
      if (isBlank(lines[j])) continue;
      // A list could sit at the indentation of its key ("key:\n- a"); .gitlab-ci.yml always indents it
      if (indentOf(lines[j]) <= indent) break;
      children.push(lines[j]);
    }
    return { inline: (match[1] || '').trim(), children };
  }
  return null;
}

// A list value: an alias (*name), a flow list ([a, b]) or block items. Returns strings for scalars.
function listValue(ci, entry) {
  if (!entry) return null;
  const alias = /^\*([\w-]+)$/.exec(entry.inline);
  if (alias) {
    const block = ci.blocks.get(ci.anchors.get(alias[1]));
    return block ? listItems(block.body).map((i) => unquote(i.first)) : [];
  }
  if (entry.inline.startsWith('[')) {
    return entry.inline.replace(/^\[|\]$/g, '').split(',').map((s) => unquote(s)).filter(Boolean);
  }
  if (entry.inline) return [unquote(entry.inline)]; // a scalar where a list may stand ("extends: .build")
  return listItems(entry.children).map((i) => unquote(i.first));
}

// Resolves one rule item into { if, changes, when }
function ruleFromItem(ci, item) {
  const rule = { if: null, changes: null, when: null };
  const own = [' '.repeat(item.indent + 2) + item.first, ...item.rest];
  const keyIndent = item.indent + 2;
  const merge = /^<<:\s*\*([\w-]+)$/.exec(item.first);
  if (merge) {
    const base = ci.blocks.get(ci.anchors.get(merge[1]));
    if (base) Object.assign(rule, mapRule(ci, base.body, 2));
  }
  const ownRule = mapRule(ci, own, keyIndent);
  for (const key of Object.keys(ownRule)) if (ownRule[key] !== null) rule[key] = ownRule[key];
  return rule;
}

function mapRule(ci, lines, indent) {
  const ifEntry = mapEntry(lines, 'if', indent);
  const whenEntry = mapEntry(lines, 'when', indent);
  const changesEntry = mapEntry(lines, 'changes', indent);
  return {
    if: ifEntry ? unquote(ifEntry.inline) : null,
    when: whenEntry ? unquote(whenEntry.inline) : null,
    changes: changesEntry ? listValue(ci, changesEntry) : null,
  };
}

// The rules a "!reference [...]" names: [.block] is a block that is itself a list of rules, [.block, rules] the rules of a
// job or template (with its extends); null when the text is not a reference
function referencedRules(ci, text) {
  const reference = /^!reference\s*\[\s*([^,\]]+?)\s*(?:,\s*([^,\]]+?)\s*)?\]$/.exec(text);
  if (!reference) return null;
  const name = unquote(reference[1]);
  if (reference[2]) {
    return unquote(reference[2]) === 'rules' ? jobRules(ci, name) || [] : [];
  }
  const block = ci.blocks.get(name);
  return block ? rulesFromItems(ci, listItems(block.body)) : [];
}

// The rules of a list (block items), expanding !reference [.x] / [.x, rules] and aliases of rule lists
function rulesFromItems(ci, items) {
  const rules = [];
  for (const item of items) {
    const referenced = referencedRules(ci, item.first);
    if (referenced) {
      rules.push(...referenced);
      continue;
    }
    rules.push(ruleFromItem(ci, item));
  }
  return rules;
}

// The rules of a job: its own "rules", else those of the last template of "extends" that has rules (GitLab merges extends
// in order and the last value of a key wins; arrays are replaced, not merged)
export function jobRules(ci, jobName, seen = new Set()) {
  const block = ci.blocks.get(jobName);
  if (!block || seen.has(jobName)) return null;
  seen.add(jobName);
  const entry = mapEntry(block.body, 'rules', 2);
  if (entry) {
    const alias = /^\*([\w-]+)$/.exec(entry.inline);
    if (alias) {
      const target = ci.blocks.get(ci.anchors.get(alias[1]));
      return target ? rulesFromItems(ci, listItems(target.body)) : [];
    }
    const referenced = referencedRules(ci, entry.inline); // rules: !reference [...]
    if (referenced) return referenced;
    return rulesFromItems(ci, listItems(entry.children));
  }
  const parents = listValue(ci, mapEntry(block.body, 'extends', 2)) || [];
  for (let i = parents.length - 1; i >= 0; i--) {
    const rules = jobRules(ci, parents[i], seen);
    if (rules) return rules;
  }
  return null;
}

// The exit codes that a job is allowed to fail with: ["any"] for allow_failure: true, the exit_codes of the map form, [] when
// it is not set (also through extends: the last template that sets it wins)
export function allowFailureCodes(ci, jobName, seen = new Set()) {
  const block = ci.blocks.get(jobName);
  if (!block || seen.has(jobName)) return [];
  seen.add(jobName);
  const entry = mapEntry(block.body, 'allow_failure', 2);
  if (entry) {
    if (entry.inline === 'true') return ['any'];
    if (entry.inline === 'false') return [];
    const codes = mapEntry(entry.children, 'exit_codes', 4);
    if (!codes) return [];
    return /^\d+$/.test(codes.inline) ? [codes.inline] : (listValue(ci, codes) || []).map(String);
  }
  const parents = listValue(ci, mapEntry(block.body, 'extends', 2)) || [];
  for (let i = parents.length - 1; i >= 0; i--) {
    const codes = allowFailureCodes(ci, parents[i], seen);
    if (codes.length) return codes;
  }
  return [];
}

// Ruby File.fnmatch(pattern, path, FNM_PATHNAME | FNM_DOTMATCH | FNM_EXTGLOB) as a regular expression
export function globToRegExp(glob) {
  let re = '';
  for (let i = 0; i < glob.length; i++) {
    const c = glob[i];
    if (c === '*') {
      if (glob[i + 1] === '*' && glob[i + 2] === '/' && (i === 0 || glob[i - 1] === '/')) {
        re += '(?:[^/]*/)*';
        i += 2;
      } else {
        while (glob[i + 1] === '*') i++;
        re += '[^/]*';
      }
    } else if (c === '?') {
      re += '[^/]';
    } else if (c === '{') {
      const end = glob.indexOf('}', i);
      if (end < 0) { re += '\\{'; continue; }
      re += '(?:' + glob.slice(i + 1, end).split(',').map((p) => globToRegExp(p).source.slice(1, -1)).join('|') + ')';
      i = end;
    } else if (c === '[') {
      const end = glob.indexOf(']', i + 1);
      if (end < 0) { re += '\\['; continue; }
      re += '[' + glob.slice(i + 1, end).replace(/^!/, '^').replace(/\\/g, '\\\\') + ']';
      i = end;
    } else {
      re += c.replace(/[.+^$()|\\/]/g, '\\$&');
    }
  }
  return new RegExp('^' + re + '$');
}

// The predefined variables of a merge-request pipeline for a branch: the ones the rules of this file read. A variable that is
// not set here (CI_COMMIT_BRANCH, CI_COMMIT_TAG, RENOVATE_RUN...) is null, as in GitLab.
export function mergeRequestVariables(source = process.env.LOCAL_CI_SOURCE_BRANCH || 'feature/local-change',
  target = process.env.LOCAL_CI_TARGET_BRANCH || 'develop') {
  return {
    CI_MERGE_REQUEST_IID: '1',
    CI_PIPELINE_SOURCE: 'merge_request_event',
    CI_COMMIT_REF_NAME: source,
    CI_MERGE_REQUEST_SOURCE_BRANCH_NAME: source,
    CI_MERGE_REQUEST_TARGET_BRANCH_NAME: target,
    CI_COMMIT_REF_PROTECTED: 'false',
  };
}

// Evaluates a rules:if expression as GitLab does: $VAR, 'string', "string", null, /regex/, ==, !=, =~, !~, &&, ||, !, ( )
export function evalIf(expression, variables) {
  const tokens = [];
  const re = /\s*(\$\w+|'[^']*'|"[^"]*"|\/(?:\\.|[^/\\])*\/[a-z]*|null\b|==|!=|=~|!~|&&|\|\||!|\(|\))/y;
  let match;
  let pos = 0;
  while (pos < expression.length) {
    re.lastIndex = pos;
    match = re.exec(expression);
    if (!match) {
      if (/^\s*$/.test(expression.slice(pos))) break;
      throw new Error(`cannot read the rules:if expression at "${expression.slice(pos)}"`);
    }
    tokens.push(match[1]);
    pos = re.lastIndex;
  }
  let i = 0;
  const value = (token) => {
    if (token.startsWith('$')) return Object.prototype.hasOwnProperty.call(variables, token.slice(1)) ? variables[token.slice(1)] : null;
    if (token === 'null') return null;
    if (token.startsWith('/')) {
      const end = token.lastIndexOf('/');
      return new RegExp(token.slice(1, end), token.slice(end + 1));
    }
    return token.slice(1, -1);
  };
  const truthy = (v) => v !== null && v !== '' && v !== false;
  const primary = () => {
    const token = tokens[i++];
    if (token === '(') {
      const v = or();
      i++; // ")"
      return v;
    }
    if (token === '!') return !truthy(primary());
    const left = value(token);
    const op = tokens[i];
    if (op === '==' || op === '!=' || op === '=~' || op === '!~') {
      i++;
      const right = value(tokens[i++]);
      if (op === '==') return left === right;
      if (op === '!=') return left !== right;
      const matches = right instanceof RegExp && left !== null && right.test(left);
      return op === '=~' ? matches : !matches;
    }
    return left;
  };
  const and = () => {
    let v = primary();
    while (tokens[i] === '&&') { i++; const r = primary(); v = truthy(v) && truthy(r); }
    return v;
  };
  const or = () => {
    let v = and();
    while (tokens[i] === '||') { i++; const r = and(); v = truthy(v) || truthy(r); }
    return v;
  };
  return truthy(or());
}

// Is the CI job created in a merge-request pipeline with these changed paths? { selected, reason }
export function evaluateJob(ci, ciJob, changed, variables = mergeRequestVariables()) {
  const rules = jobRules(ci, ciJob);
  if (!rules) return { selected: false, reason: `no rules found for "${ciJob}" in the CI file` };
  for (const rule of rules) {
    // A rule without "if" applies to every pipeline; one whose "if" is false in a merge request does not apply
    if (rule.if && !evalIf(rule.if, variables)) continue;
    let hit = null;
    if (rule.changes) {
      for (const glob of rule.changes) {
        const re = globToRegExp(glob);
        const file = changed.find((p) => re.test(p));
        if (file) { hit = { glob, file }; break; }
      }
      if (!hit) continue;
    }
    if (rule.when === 'never' || rule.when === 'manual') {
      return { selected: false, reason: `"${ciJob}": first matching rule is when: ${rule.when}` };
    }
    return {
      selected: true,
      reason: hit ? `"${ciJob}": ${hit.file} matches ${hit.glob}` : `"${ciJob}": every merge request`,
    };
  }
  return { selected: false, reason: `"${ciJob}": no changed path matches its rules:changes` };
}

// The "fast" scope (opt-in: bin/local-ci.sh --fast, LOCAL_CI_SCOPE=affected) narrows the heavy jobs below what the merge
// request pipeline runs, so a pass here no longer predicts a pass there for the jobs it drops:
//  - a change of .gitlab-ci.yml alone does not trigger them (the pipeline does: the file is in every rules:changes list);
//  - the database jobs run only when a module that their profiles exercise changed (the framework modules and awe-boot);
//  - the AngularJS browser suites do not run for a change that is only in awe-boot-react (the pipeline already skips them
//    for a change that is only in the React client), and the React suites do not run for a change that is only in awe-boot
//    (the pipeline already skips them for a change that is only in the AngularJS client).
export const FAST_HEAVY = ['all-ut', 'embedded-db', 'db-mysql', 'db-postgresql', 'db-sqlserver', 'db-oracle', 'javadoc',
  'playwright-angular', 'playwright-react'];
const FAST_DATABASE = ['embedded-db', 'db-mysql', 'db-postgresql', 'db-sqlserver', 'db-oracle'];
export const FAST_DATABASE_PATHS = [
  'pom.xml', '.m2/**/*', 'awe-framework/pom.xml', 'awe-framework/awe-dependencies/**/*', 'awe-framework/awe-model/**/*',
  'awe-framework/awe-generic-screens/**/*', 'awe-framework/awe-controller/**/*', 'awe-framework/awe-testing/**/*',
  'awe-framework/awe-modules/**/*', 'awe-framework/awe-starters/**/*', 'awe-tests/awe-boot/**/*',
];
export const FAST_REACT_ONLY = ['awe-framework/awe-client-react/**/*', 'awe-tests/awe-boot-react/**/*',
  'awe-framework/awe-boot-react-archetype/**/*'];
export const FAST_ANGULAR_ONLY = ['awe-framework/awe-client-angular/**/*', 'awe-tests/awe-boot/**/*',
  'awe-framework/awe-boot-angular-archetype/**/*'];

const matchesAny = (globs, path) => globs.some((glob) => globToRegExp(glob).test(path));

// Why the fast scope drops a heavy job (a string), or null when it keeps it
function fastDrop(ci, job, changed) {
  const code = changed.filter((p) => p !== '.gitlab-ci.yml');
  const jobDef = JOBS.find((j) => j.id === job);
  if (!jobDef.ci.some((name) => evaluateJob(ci, name, code).selected)) {
    return 'fast scope: only .gitlab-ci.yml changed (the pipeline runs it because the file is in its rules:changes)';
  }
  if (FAST_DATABASE.includes(job) && !code.some((p) => matchesAny(FAST_DATABASE_PATHS, p))) {
    return 'fast scope: no change in the modules that the database profiles exercise (framework modules, awe-boot)';
  }
  if (job === 'playwright-angular' && code.every((p) => matchesAny(FAST_REACT_ONLY, p))) {
    return 'fast scope: the change is only in the React client';
  }
  if (job === 'playwright-react' && code.every((p) => matchesAny(FAST_ANGULAR_ONLY, p))) {
    return 'fast scope: the change is only in the AngularJS client or awe-boot';
  }
  return null;
}

export function selectJobs(ci, changed, { fast = false } = {}) {
  return JOBS.map((job) => {
    if (job.ci.length === 0) return { id: job.id, selected: true, reason: 'local check for every merge request (no CI job)' };
    const results = job.ci.map((name) => evaluateJob(ci, name, changed));
    const hit = results.find((r) => r.selected);
    const selected = { id: job.id, selected: Boolean(hit), reason: (hit || results[0]).reason };
    if (fast && selected.selected && FAST_HEAVY.includes(job.id)) {
      const dropped = fastDrop(ci, job.id, changed);
      if (dropped) return { id: job.id, selected: false, reason: dropped };
    }
    return selected;
  });
}

// parallel:matrix entries of a job (only the plain "- KEY: value" form this file uses)
export function matrixEntries(ci, jobName) {
  const block = ci.blocks.get(jobName);
  if (!block) return [];
  const parallel = mapEntry(block.body, 'parallel', 2);
  if (!parallel) return [];
  const matrix = mapEntry(parallel.children, 'matrix', 4);
  if (!matrix) return [];
  return listItems(matrix.children).map((item) => {
    const entry = {};
    for (const line of [' '.repeat(item.indent + 2) + item.first, ...item.rest]) {
      const match = /^\s*([A-Z_]+):\s*(.*)$/.exec(line);
      if (match) entry[match[1]] = unquote(match[2]);
    }
    return entry;
  });
}

// The jobs that extend a template (the browser suites: one job per suite), in file order, with their own upper-case variables
// and their name as JOB: [{ JOB, TEST_TAGS, TEST_CLASSES, TEST_PORT, TEST_NAME }]
export function suitesOf(ci, template) {
  const suites = [];
  for (const [name, block] of ci.blocks) {
    if (name.startsWith('.')) continue;
    const parents = listValue(ci, mapEntry(block.body, 'extends', 2)) || [];
    if (!parents.includes(template)) continue;
    const entry = { JOB: name };
    const variables = mapEntry(block.body, 'variables', 2);
    for (const line of variables ? variables.children : []) {
      const match = /^\s*([A-Z_]+):\s*(.*)$/.exec(stripComment(line));
      if (match) entry[match[1]] = unquote(match[2]);
    }
    suites.push(entry);
  }
  return suites;
}

// ------------------------------------------------------------
// New-code coverage
// ------------------------------------------------------------

// The added or changed lines of a unified diff (git diff -U0): Map path -> Set of new line numbers
export function addedLines(diffText) {
  const result = new Map();
  let file = null;
  for (const line of diffText.split('\n')) {
    if (line.startsWith('+++ ')) {
      const target = line.slice(4).trim();
      file = target === '/dev/null' ? null : target.replace(/^b\//, '');
      if (file && !result.has(file)) result.set(file, new Set());
      continue;
    }
    const hunk = /^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@/.exec(line);
    if (hunk && file) {
      const start = Number(hunk[1]);
      const count = hunk[2] === undefined ? 1 : Number(hunk[2]);
      for (let n = start; n < start + count; n++) result.get(file).add(n);
    }
  }
  for (const [key, set] of result) if (set.size === 0) result.delete(key);
  return result;
}

// JaCoCo XML: Map "package/File.java" -> Map line -> { ci, mb, cb }: covered instructions and missed/covered branches. The
// missed instructions (mi) are not kept: a line counts as covered when ci > 0, whatever mi says, as Sonar counts it. The
// report has no line breaks, so it is split on "<".
export function parseJacoco(xml, into = new Map()) {
  let pkg = null;
  let source = null;
  for (const chunk of xml.split('<')) {
    if (chunk.startsWith('package ')) {
      pkg = /name="([^"]*)"/.exec(chunk)[1];
    } else if (chunk.startsWith('sourcefile ')) {
      const key = (pkg ? pkg + '/' : '') + /name="([^"]*)"/.exec(chunk)[1];
      source = into.get(key) || new Map();
      into.set(key, source);
    } else if (chunk.startsWith('/sourcefile')) {
      source = null;
    } else if (source && chunk.startsWith('line ')) {
      const attr = (name) => Number((new RegExp(`\\b${name}="(\\d+)"`).exec(chunk) || [0, 0])[1]);
      const nr = attr('nr');
      const prev = source.get(nr);
      if (!prev) {
        source.set(nr, { ci: attr('ci'), mb: attr('mb'), cb: attr('cb') });
      } else {
        // Several reports (or modules) may describe the same line. Merge per line: the line is covered when any report
        // covers it, and its branches count once: covered = the most any report covered, total = the branches of the
        // line (the same in every report), so missed = total - covered. Adding or taking the max of both counts would
        // count branches that do not exist.
        const total = Math.max(prev.mb + prev.cb, attr('mb') + attr('cb'));
        const covered = Math.min(total, Math.max(prev.cb, attr('cb')));
        source.set(nr, { ci: Math.max(prev.ci, attr('ci')), mb: total - covered, cb: covered });
      }
    }
  }
  return into;
}

// lcov: Map repo-relative path -> Map line -> { ci (hits), mb, cb }. clientDir prefixes relative SF paths; repoRoot
// strips absolute ones.
export function parseLcov(text, clientDir, repoRoot, into = new Map()) {
  let file = null;
  for (const raw of text.split('\n')) {
    const line = raw.trim();
    if (line.startsWith('SF:')) {
      let sf = line.slice(3);
      if (path.isAbsolute(sf)) {
        sf = repoRoot ? path.relative(repoRoot, sf) : sf;
      } else {
        sf = path.posix.join(clientDir, sf);
      }
      file = into.get(sf) || new Map();
      into.set(sf.split(path.sep).join('/'), file);
    } else if (line === 'end_of_record') {
      file = null;
    } else if (file && line.startsWith('DA:')) {
      const [nr, hits] = line.slice(3).split(',').map(Number);
      const prev = file.get(nr) || { ci: 0, mb: 0, cb: 0 };
      file.set(nr, { ...prev, ci: Math.max(prev.ci, hits) });
    } else if (file && line.startsWith('BRDA:')) {
      const parts = line.slice(5).split(',');
      const nr = Number(parts[0]);
      const taken = parts[3] === '-' ? 0 : Number(parts[3]);
      const prev = file.get(nr) || { ci: 0, mb: 0, cb: 0 };
      file.set(nr, { ...prev, mb: prev.mb + (taken > 0 ? 0 : 1), cb: prev.cb + (taken > 0 ? 1 : 0) });
    }
  }
  return into;
}

// Where Sonar takes the coverage of a file from, or null when Sonar does not measure it (tests, excluded modules)
export function coverageSource(file) {
  if (/\/src\/test\//.test(file) || /(^|\/)(__tests__|test)\//.test(file.replace(/^awe-framework\/awe-client-react\//, ''))) {
    return null;
  }
  if (file.endsWith('.java')) {
    if (!/^(awe-framework|awe-samples)\/.*\/src\/main\/java\//.test(file)) return null;
    if (/^awe-framework\/(awe-testing|awe-boot-angular-archetype|awe-boot-react-archetype)\//.test(file)) return null;
    return { kind: 'jacoco', key: file.replace(/^.*\/src\/main\/java\//, '') };
  }
  if (/^awe-framework\/awe-client-angular\/src\/main\/resources\/js\/awe\/.*\.js$/.test(file)) {
    return { kind: 'lcov', client: 'awe-framework/awe-client-angular' };
  }
  if (/^awe-framework\/awe-client-react\/src\/.*\.(js|jsx|ts|tsx|cjs|mjs)$/.test(file)
    && !/\/src\/(static\/css\/themes|static\/images|assets\/images)\//.test(file)
    && !/\.(test|spec)\.[jt]sx?$/.test(file)) {
    return { kind: 'lcov', client: 'awe-framework/awe-client-react' };
  }
  return null;
}

// The tokens of TypeScript source for isTypeOnly: words, punctuation and '"' for a string or template literal, each with
// whether a line break comes before it. Comments are dropped.
function tsTokens(text) {
  const tokens = [];
  const word = /[\w$]+/y;
  let nl = true;
  let i = 0;
  while (i < text.length) {
    const c = text[i];
    if (c === '\n') { nl = true; i++; continue; }
    if (/\s/.test(c)) { i++; continue; }
    if (text.startsWith('//', i)) {
      const end = text.indexOf('\n', i);
      if (end < 0) break;
      i = end;
      continue;
    }
    if (text.startsWith('/*', i)) {
      const end = text.indexOf('*/', i + 2);
      if (end < 0) break;
      nl = nl || text.slice(i, end).includes('\n');
      i = end + 2;
      continue;
    }
    let value;
    if (c === '"' || c === "'" || c === '`') {
      let j = i + 1;
      while (j < text.length && text[j] !== c) j += text[j] === '\\' ? 2 : 1;
      value = '"';
      i = j + 1;
    } else if (text.startsWith('=>', i)) {
      value = '=>';
      i += 2;
    } else {
      word.lastIndex = i;
      value = word.test(text) ? text.slice(i, word.lastIndex) : c;
      i += value.length;
    }
    tokens.push({ value, nl });
    nl = false;
  }
  return tokens;
}

// Tokens that carry a statement over a line break, before or after it (no automatic semicolon there)
const TS_CONTINUES = new Set(['|', '&', '=', ',', '?', ':', '.', '=>', '<', 'extends']);

// Whether a top-level TypeScript statement emits no JavaScript: ambient declarations, interfaces, type aliases, type-only
// imports and exports, and "export {}"
function isTypeStatement(t, i) {
  const v = (n) => (t[n] ? t[n].value : '');
  // "declare" and "type" are also plain identifiers ("declare.run()", "import type, {A} from"): a name, "{" or "*"
  // must follow them
  const named = (n) => /^[\w$]+$/.test(v(n)) || v(n) === '{' || v(n) === '*';
  if (v(i) === 'import') return v(i + 1) === 'type' && named(i + 2) && v(i + 2) !== 'from';
  let k = i;
  if (v(k) === 'export') {
    if (v(k + 1) === '{') return v(k + 2) === '}';
    k += v(k + 1) === 'default' ? 2 : 1;
  }
  if (v(k) === 'interface') return true;
  return (v(k) === 'declare' || v(k) === 'type') && named(k + 1);
}

// Whether a TypeScript file has no executable code: Jest never emits lcov data for it and Sonar counts no lines to cover
// (#890). A .d.ts file, or a .ts/.tsx file whose top-level statements are all type-only. Anything else, or anything the
// scan does not recognise, is code.
export function isTypeOnly(file, text) {
  if (file.endsWith('.d.ts')) return true;
  if (!/\.tsx?$/.test(file)) return false;
  const t = tsTokens(text);
  let start = 0;
  let depth = 0;
  for (let j = 0; j <= t.length; j++) {
    // a statement ends with a ";" at depth 0, or at a line break at depth 0 that nothing carries over
    const ends = j === t.length || (j > start && depth === 0 && t[j].nl
      && !TS_CONTINUES.has(t[j].value) && !TS_CONTINUES.has(t[j - 1].value));
    if (ends) {
      if (j > start && !isTypeStatement(t, start)) return false;
      start = j;
    }
    if (j === t.length) break;
    const v = t[j].value;
    if (v === '(' || v === '[' || v === '{') depth++;
    else if (v === ')' || v === ']' || v === '}') depth--;
    else if (v === ';' && depth === 0) {
      if (j > start && !isTypeStatement(t, start)) return false;
      start = j + 1;
    }
  }
  return true;
}

// Coverage of the new lines. Sonar's "Coverage on New Code" mixes lines and conditions:
//   (covered lines + covered conditions) / (lines to cover + conditions)
// typeOnly(file) says whether a file without coverage data has no executable code, and so nothing to cover
export function newCodeCoverage(added, jacoco, lcov, typeOnly = () => false) {
  const files = [];
  const missing = [];
  const types = [];
  let lines = 0;
  let coveredLines = 0;
  let conditions = 0;
  let coveredConditions = 0;
  for (const [file, numbers] of [...added.entries()].sort()) {
    const source = coverageSource(file);
    if (!source) continue;
    const data = source.kind === 'jacoco' ? jacoco.get(source.key) : lcov.get(file);
    if (!data) {
      if (typeOnly(file)) {
        types.push(file);
        continue;
      }
      missing.push({ file, lines: numbers.size, kind: source.kind });
      continue;
    }
    const uncovered = [];
    const partial = [];
    let fileLines = 0;
    let fileCovered = 0;
    for (const n of [...numbers].sort((a, b) => a - b)) {
      const line = data.get(n);
      if (!line) continue; // not executable
      fileLines++;
      lines++;
      if (line.ci > 0) { fileCovered++; coveredLines++; } else { uncovered.push(n); }
      conditions += line.mb + line.cb;
      coveredConditions += line.cb;
      if (line.ci > 0 && line.mb > 0) partial.push(`${n} (${line.cb}/${line.mb + line.cb} conditions)`);
    }
    if (fileLines > 0) files.push({ file, lines: fileLines, covered: fileCovered, uncovered, partial });
  }
  const total = lines + conditions;
  const percent = total === 0 ? null : (100 * (coveredLines + coveredConditions)) / total;
  return { files, missing, types, lines, coveredLines, conditions, coveredConditions, percent };
}

// The packages of Playwright's "install-deps --dry-run" output: [sudo -- ]sh -c "apt-get update&& apt-get install -y
// --no-install-recommends <packages>"
export function aptPackages(text) {
  const match = /apt-get install[^"\n]*?--no-install-recommends\s+([^"\n]*)/.exec(text);
  return match ? match[1].trim().split(/\s+/).filter(Boolean) : [];
}

// Compresses [1,2,3,7,9,10] into "1-3, 7, 9-10"
export function ranges(numbers) {
  const out = [];
  for (let i = 0; i < numbers.length; i++) {
    let j = i;
    while (j + 1 < numbers.length && numbers[j + 1] === numbers[j] + 1) j++;
    out.push(i === j ? `${numbers[i]}` : `${numbers[i]}-${numbers[j]}`);
    i = j;
  }
  return out.join(', ');
}

export function formatCoverage(result, threshold) {
  const out = [];
  const pct = (a, b) => (b === 0 ? '-' : `${((100 * a) / b).toFixed(1)}%`);
  out.push(`New-code coverage (Sonar formula, lines + conditions), threshold ${threshold}%`);
  for (const f of result.files) {
    out.push(`  ${pct(f.covered, f.lines).padStart(6)}  ${f.covered}/${f.lines} lines  ${f.file}`);
    if (f.uncovered.length) out.push(`          uncovered new lines: ${ranges(f.uncovered)}`);
    if (f.partial.length) out.push(`          partly covered branches: ${f.partial.join(', ')}`);
  }
  for (const file of result.types || []) out.push(`  TYPES    ${file} (type-only TypeScript: no lines to cover, as in Sonar)`);
  for (const m of result.missing) {
    out.push(`  NO DATA  ${m.lines} new lines  ${m.file} (no ${m.kind === 'jacoco' ? 'JaCoCo' : 'Jest lcov'} data: not run, or not loaded by any test)`);
  }
  out.push('');
  out.push(`Lines: ${result.coveredLines}/${result.lines} (${pct(result.coveredLines, result.lines)}), conditions: ${result.coveredConditions}/${result.conditions} (${pct(result.coveredConditions, result.conditions)})`);
  if (result.percent === null) {
    out.push('No new executable line with coverage data: nothing to measure.');
  } else {
    out.push(`Coverage on new code: ${result.percent.toFixed(1)}% (${result.percent >= threshold ? 'PASS' : 'FAIL'}, quality gate ${threshold}%)`);
  }
  return out.join('\n');
}

// ------------------------------------------------------------
// Sonar rules guard: a few rules that failed our quality gate, approximated on the changed files (warnings only)
// ------------------------------------------------------------

// Java source without comments and string contents, same length per line (so line numbers and columns stay)
function maskJava(text) {
  let out = '';
  let state = 'code';
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    const next = text[i + 1];
    if (state === 'code') {
      if (c === '/' && next === '/') { state = 'line'; out += '  '; i++; continue; }
      if (c === '/' && next === '*') { state = 'block'; out += '  '; i++; continue; }
      if (c === '"' && text.startsWith('"""', i)) { state = 'text'; out += '"""'; i += 2; continue; }
      if (c === '"') { state = 'string'; out += c; continue; }
      if (c === '\'') { state = 'char'; out += c; continue; }
      out += c;
    } else if (state === 'line') {
      if (c === '\n') { state = 'code'; out += c; } else out += ' ';
    } else if (state === 'block') {
      if (c === '*' && next === '/') { state = 'code'; out += '  '; i++; } else out += c === '\n' ? c : ' ';
    } else if (state === 'text') {
      if (text.startsWith('"""', i)) { state = 'code'; out += '"""'; i += 2; } else out += c === '\n' ? c : ' ';
    } else {
      const quote = state === 'string' ? '"' : '\'';
      if (c === '\\') { out += '  '; i++; continue; }
      if (c === quote) { state = 'code'; out += c; continue; }
      out += c === '\n' ? c : ' ';
    }
  }
  return out;
}

const lineAt = (text, index) => text.slice(0, index).split('\n').length;

// Text of the balanced parentheses group that starts at text[open] === '('
function balanced(text, open) {
  let depth = 0;
  for (let i = open; i < text.length; i++) {
    if (text[i] === '(') depth++;
    else if (text[i] === ')') { depth--; if (depth === 0) return text.slice(open + 1, i); }
  }
  return text.slice(open + 1);
}

const CALL = /\b([A-Za-z_$][\w$]*)\s*\(/g;
const KEYWORDS = new Set(['if', 'for', 'while', 'switch', 'catch', 'return', 'new', 'synchronized', 'super', 'this', 'throw']);

export function sonarFindings(file, text, newLines) {
  const findings = [];
  const isJava = file.endsWith('.java');
  const isJs = /\.(js|jsx|ts|tsx|mjs|cjs)$/.test(file);
  const isTest = /\/src\/test\/|__tests__|\.(test|spec)\.[jt]sx?$|Test\.java$|IT\.java$/.test(file);
  const lines = text.split('\n');
  const touches = (from, to) => { for (let n = from; n <= to; n++) if (newLines.has(n)) return true; return false; };

  // S125: commented-out code on a new line
  if (isJava || isJs) {
    for (const n of newLines) {
      const line = lines[n - 1];
      if (line === undefined) continue;
      const comment = /^\s*(?:\/\/|\/\*+|\*)\s?(.*)$/.exec(line);
      if (!comment) continue;
      const body = comment[1].trim();
      if (/^(@|TODO|FIXME|NOSONAR|eslint|http)/i.test(body)) continue;
      // Code-like: a statement end, a closing brace, a block opened after a call or an assignment, a keyword, a call
      if (/;\s*$/.test(body) || /^\}/.test(body) || (/\{\s*$/.test(body) && /[()=]/.test(body))
        || /^(if|for|while|return|import|public|private|protected|const|let|var)\b.*[({;=]/.test(body)
        || /^[\w.]+\s*\(.*\)\s*;?$/.test(body) || /^[\w.]+\s*=\s*.+;$/.test(body)) {
        findings.push({ rule: 'S125', line: n, message: 'commented-out code: remove it' });
      }
    }
  }
  if (!isJava) return findings;
  const masked = maskJava(text);

  // S1192: a string literal repeated 3 times or more (Sonar ignores literals shorter than 5 characters and annotations)
  const literals = new Map();
  const maskedLines = masked.split('\n');
  lines.forEach((line, i) => {
    if (/^\s*@/.test(line) || /"""/.test(line)) return;
    // The literals of the line: an opening quote of the masked line (comments are blank there) up to its closing quote
    const code = maskedLines[i] || '';
    for (let start = code.indexOf('"'); start >= 0; start = code.indexOf('"', start + 1)) {
      const end = code.indexOf('"', start + 1);
      if (end < 0) break;
      const literal = line.slice(start + 1, end);
      if (literal.length >= 5) {
        const list = literals.get(literal) || [];
        list.push(i + 1);
        literals.set(literal, list);
      }
      start = end;
    }
  });
  for (const [literal, where] of literals) {
    if (where.length >= 3 && where.some((n) => newLines.has(n))) {
      findings.push({ rule: 'S1192', line: where[0], message: `"${literal}" appears ${where.length} times (lines ${where.join(', ')}): use a constant` });
    }
  }

  if (!isTest) return findings;

  // S5778: the lambda of assertThrows / assertThatThrownBy / assertThatExceptionOfType().isThrownBy must have one call
  const throwsRe = /\b(assertThrows|assertThatThrownBy|isThrownBy|assertThatCode|assertDoesNotThrow)\s*\(/g;
  for (const match of masked.matchAll(throwsRe)) {
    if (match[1] === 'assertThatCode' || match[1] === 'assertDoesNotThrow') continue;
    const open = match.index + match[0].length - 1;
    const args = balanced(masked, open);
    const arrow = args.indexOf('->');
    if (arrow < 0) continue;
    const body = args.slice(arrow + 2);
    // Method calls and constructor calls ("new Foo(" matches as "Foo(")
    const count = [...body.matchAll(CALL)].filter((m) => !KEYWORDS.has(m[1])).length;
    const from = lineAt(masked, match.index);
    const to = lineAt(masked, open + args.length + 1);
    if (count > 1 && touches(from, to)) {
      findings.push({ rule: 'S5778', line: from, message: `${match[1]} lambda makes ${count} calls that can throw: prepare the arguments before, keep one call in the lambda` });
    }
  }

  // S1130: "throws Exception" on a test method whose body calls nothing that obviously throws a checked exception
  const methodRe = /\b(?:void|[\w<>[\],\s]+)\s+(\w+)\s*\(([^)]*)\)\s*throws\s+([\w.,\s]+?)\s*\{/g;
  const harmless = /^(assert\w*|assertThat\w*|is\w*|has\w*|contains\w*|starts\w*|ends\w*|verify\w*|when|then\w*|do\w*|mock|spy|any\w*|eq|get\w*|set\w*|equals|toString|size|of|asList|builder|build|with\w*|and|extracting|satisfies|matches|times|never|ofNullable|stream|map|filter|collect|toList|valueOf|format|singletonList|emptyList|put|add|length|trim|isEmpty|containsExactly|hasSize)$/;
  for (const match of masked.matchAll(methodRe)) {
    const declared = match[3].split(',').map((s) => s.trim());
    if (!declared.includes('Exception')) continue;
    const bodyStart = match.index + match[0].length - 1;
    let depth = 0;
    let end = bodyStart;
    for (let i = bodyStart; i < masked.length; i++) {
      if (masked[i] === '{') depth++;
      else if (masked[i] === '}') { depth--; if (depth === 0) { end = i; break; } }
    }
    const body = masked.slice(bodyStart + 1, end);
    if (/\bthrow\b/.test(body)) continue;
    const risky = [...body.matchAll(CALL)].map((m) => m[1]).filter((name) => !KEYWORDS.has(name) && !harmless.test(name));
    const from = lineAt(masked, match.index);
    const to = lineAt(masked, end);
    if (risky.length === 0 && touches(from, to)) {
      findings.push({ rule: 'S1130', line: lineAt(masked, match.index + match[0].indexOf('throws')), message: `${match[1]}() declares "throws Exception" but its body seems to throw none: remove it` });
    }
  }
  return findings.sort((a, b) => a.line - b.line);
}

// ------------------------------------------------------------
// Summary of a run: TSV lines id, label, status, blocking (yes|no), seconds, note
// ------------------------------------------------------------

export function formatDuration(seconds) {
  const s = Number(seconds) || 0;
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m${String(s % 60).padStart(2, '0')}s`;
  return `${Math.floor(m / 60)}h${String(m % 60).padStart(2, '0')}m`;
}

export function summarize(tsv) {
  const rows = tsv.split('\n').filter((l) => l.trim()).map((l) => {
    const [id, label, status, blocking, seconds, note] = l.split('\t');
    return { id, label, status, blocking: blocking === 'yes', seconds: Number(seconds) || 0, note: note || '' };
  });
  const failed = rows.filter((r) => r.status === 'FAIL' && r.blocking);
  const width = Math.max(10, ...rows.map((r) => r.label.length));
  const lines = [];
  lines.push(`${'JOB'.padEnd(width)}  STATUS  TIME     NOTE`);
  lines.push(`${'-'.repeat(width)}  ------  -------  ----`);
  let total = 0;
  for (const r of rows) {
    total += r.seconds;
    // A failure of a job that does not block (allow_failure) is a warning, as in the pipeline
    const status = r.status === 'FAIL' && !r.blocking ? 'WARN' : r.status;
    lines.push(`${r.label.padEnd(width)}  ${status.padEnd(6)}  ${formatDuration(r.seconds).padEnd(7)}  ${r.note}`);
  }
  lines.push(`${'-'.repeat(width)}  ------  -------`);
  lines.push(`${'Total'.padEnd(width)}          ${formatDuration(total)}`);
  const count = (s) => rows.filter((r) => (r.status === 'FAIL' && !r.blocking ? 'WARN' : r.status) === s).length;
  lines.push('');
  lines.push(`${count('PASS')} passed, ${count('FAIL')} failed, ${count('WARN')} warnings, ${count('SKIP')} skipped`);
  lines.push(failed.length ? `RESULT: FAIL (${failed.map((r) => r.id).join(', ')})` : 'RESULT: PASS');
  return { text: lines.join('\n'), exitCode: failed.length ? 1 : 0 };
}

// ------------------------------------------------------------
// Command line
// ------------------------------------------------------------

function options(args) {
  const out = { jacoco: [], lcov: [], threshold: 80, diff: null, root: process.cwd() };
  for (let i = 0; i < args.length; i++) {
    const key = args[i];
    const value = args[++i];
    if (key === '--jacoco') out.jacoco.push(value);
    else if (key === '--lcov') out.lcov.push(value);
    else if (key === '--threshold') out.threshold = Number(value);
    else if (key === '--diff') out.diff = value;
    else if (key === '--root') out.root = value;
    else throw new Error(`unknown option ${key}`);
  }
  return out;
}

function main(argv) {
  const [command, ...args] = argv;
  const read = (file) => fs.readFileSync(file, 'utf8');
  switch (command) {
    case 'jobs': {
      // jobs [ci-file]: id, label, blocking, CI jobs, warning exit codes (read from the CI file when it is given); "-" for
      // an empty field, as a shell "read" with a tab IFS would merge two tabs
      const ci = args[0] ? parseCi(read(args[0])) : null;
      for (const j of JOBS) {
        const warn = j.warn !== undefined ? j.warn : (ci ? [...new Set(j.ci.flatMap((name) => allowFailureCodes(ci, name)))].join(' ') : '');
        process.stdout.write(`${j.id}\t${j.label}\t${j.blocking ? 'yes' : 'no'}\t${j.ci.join(' + ') || '-'}\t${warn || '-'}\n`);
      }
      return 0;
    }
    case 'match': {
      // match <ci-file> <ci-job> <changed-paths-file>: is that CI job in a merge-request pipeline with these changes?
      const changed = read(args[2]).split('\n').map((s) => s.trim()).filter(Boolean);
      const r = evaluateJob(parseCi(read(args[0])), args[1], changed);
      process.stdout.write(`${r.selected ? 'yes' : 'no'}\t${r.reason}\n`);
      return 0;
    }
    case 'script': {
      // script <ci-file> <ci-job>: the script lines of a job (its own, else those of the last template of extends that has one)
      const ci = parseCi(read(args[0]));
      const scriptOf = (name, seen = new Set()) => {
        const block = ci.blocks.get(name);
        if (!block || seen.has(name)) return null;
        seen.add(name);
        const entry = mapEntry(block.body, 'script', 2);
        if (entry) return listItems(entry.children).map((i) => i.first);
        const parents = listValue(ci, mapEntry(block.body, 'extends', 2)) || [];
        for (let i = parents.length - 1; i >= 0; i--) {
          const lines = scriptOf(parents[i], seen);
          if (lines) return lines;
        }
        return null;
      };
      for (const line of scriptOf(args[1]) || []) process.stdout.write(`${line}\n`);
      return 0;
    }
    case 'apt-packages':
      // apt-packages <file>: the packages of the "apt-get install" that Playwright's "install-deps --dry-run" prints
      for (const p of aptPackages(read(args[0]))) process.stdout.write(`${p}\n`);
      return 0;
    case 'scope': {
      // scope <changed-paths-file>: what the coverage needs for these paths: "jacoco" and/or "lcov:<client dir>"
      const kinds = new Set();
      for (const file of read(args[0]).split('\n').map((s) => s.trim()).filter(Boolean)) {
        const source = coverageSource(file);
        if (source) kinds.add(source.kind === 'jacoco' ? 'jacoco' : `lcov:${source.client}`);
      }
      for (const k of [...kinds].sort()) process.stdout.write(`${k}\n`);
      return 0;
    }
    case 'rules': {
      const rules = jobRules(parseCi(read(args[0])), args[1]);
      if (!rules) { process.stderr.write(`no rules for ${args[1]}\n`); return 2; }
      for (const r of rules) process.stdout.write(`${JSON.stringify(r)}\n`);
      return 0;
    }
    case 'select': {
      const ci = parseCi(read(args[0]));
      const changed = read(args[1]).split('\n').map((s) => s.trim()).filter(Boolean);
      for (const r of selectJobs(ci, changed, { fast: args.includes('--fast') })) process.stdout.write(`${r.id}\t${r.selected ? 'yes' : 'no'}\t${r.reason}\n`);
      return 0;
    }
    case 'matrix':
    case 'suites': {
      // matrix <ci> <job>: the parallel:matrix entries of a job; suites <ci> <local browser job> <browser>: the CI jobs of
      // the suite template of that browser. Both print TEST_TAGS, TEST_CLASSES, TEST_PORT, TEST_NAME and the CI job name,
      // with "-" for an empty field: a shell "read" with a tab IFS would merge two tabs and shift the fields
      const ci = parseCi(read(args[0]));
      let entries;
      if (command === 'matrix') {
        entries = matrixEntries(ci, args[1]).map((e) => ({ ...e, JOB: args[1] }));
      } else {
        const template = (SUITE_TEMPLATES[args[1]] || {})[args[2]];
        if (!template) { process.stderr.write(`no suite template for ${args[1]} ${args[2]}\n`); return 2; }
        entries = suitesOf(ci, template);
      }
      const field = (value) => value || '-';
      for (const e of entries) {
        process.stdout.write(`${field(e.TEST_TAGS)}\t${field(e.TEST_CLASSES)}\t${field(e.TEST_PORT)}\t${field(e.TEST_NAME)}\t${e.JOB}\n`);
      }
      return 0;
    }
    case 'coverage': {
      const opts = options(args);
      const added = addedLines(read(opts.diff));
      const jacoco = new Map();
      for (const file of opts.jacoco) if (fs.existsSync(file)) parseJacoco(read(file), jacoco);
      const lcov = new Map();
      for (const spec of opts.lcov) {
        const [client, file] = spec.split('=');
        if (fs.existsSync(file)) parseLcov(read(file), client, opts.root, lcov);
      }
      const source = (file) => {
        const full = path.join(opts.root, file);
        return fs.existsSync(full) ? read(full) : null;
      };
      const typeOnly = (file) => {
        const text = source(file);
        return text !== null && isTypeOnly(file, text);
      };
      const result = newCodeCoverage(added, jacoco, lcov, typeOnly);
      process.stdout.write(formatCoverage(result, opts.threshold) + '\n');
      if (result.percent === null) return result.missing.length ? 1 : 0;
      return result.percent >= opts.threshold && result.missing.length === 0 ? 0 : 1;
    }
    case 'sonar': {
      const opts = options(args);
      const added = addedLines(read(opts.diff));
      let count = 0;
      for (const [file, numbers] of [...added.entries()].sort()) {
        const full = path.join(opts.root, file);
        if (!/\.(java|js|jsx|ts|tsx|mjs|cjs)$/.test(file) || !fs.existsSync(full)) continue;
        if (/(^|\/)(node_modules|target|dist|generated)\//.test(file) || /\.min\.js$/.test(file)) continue;
        for (const f of sonarFindings(file, read(full), numbers)) {
          count++;
          process.stdout.write(`WARN ${f.rule} ${file}:${f.line} ${f.message}\n`);
        }
      }
      process.stdout.write(count ? `${count} possible Sonar issues in the changed lines (a heuristic guard: check each one)\n`
        : 'No S1192, S125, S5778 or S1130 pattern in the changed lines\n');
      return count ? 64 : 0;
    }
    case 'summary': {
      const { text, exitCode } = summarize(read(args[0]));
      process.stdout.write(text + '\n');
      return exitCode;
    }
    default:
      process.stderr.write('usage: local-ci-tool.mjs jobs|rules|select|suites|matrix|coverage|sonar|summary ...\n');
      return 2;
  }
}

// Run as a command only when this file is the script (not when imported, e.g. by "node -e" in the tests, whose argv[1]
// is an argument, maybe not a file)
const isScript = () => {
  try {
    return Boolean(process.argv[1]) && fileURLToPath(import.meta.url) === fs.realpathSync(process.argv[1]);
  } catch {
    return false;
  }
};
if (isScript()) {
  process.exitCode = main(process.argv.slice(2));
}
