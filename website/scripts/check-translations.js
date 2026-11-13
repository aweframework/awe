#!/usr/bin/env node
'use strict';

/**
 * Fast lint of the Spanish translation of the docs, without building the site.
 *
 * Merge requests build the English site only, so a broken Spanish page would reach develop unnoticed. For every page of
 * i18n/es/docusaurus-plugin-content-docs/current this checks that:
 * 1. it compiles as MDX, the way Docusaurus compiles it (reports the line of the syntax error);
 * 2. it matches the English page of the same path: same heading anchors (explicit {#id} or generated), same number of fenced code blocks
 *    and same front matter id and slug. A page without an English counterpart is an error; an English page without
 *    translation is fine (Docusaurus falls back to English);
 * 3. its relative links to other pages point to an existing file, in the Spanish tree or in the English one.
 *
 * Usage: node scripts/check-translations.js [file ...]   (files relative to website/ or absolute; default: all pages)
 * Exits with 1 when any error is found.
 */

const fs = require('fs');
const path = require('path');

const ES_ROOT = 'i18n/es/docusaurus-plugin-content-docs/current';
const EN_ROOT = 'docs';
// Pages generated at build time from the sources: they are not translated by hand
const GENERATED = new Set(['properties.md', 'rest-api.md', 'guides/docs-audit.md']);

const FENCE = /^\s*(`{3,}|~{3,})/;

// Calls fn(line, lineNumber) for every line outside fenced code blocks
function forEachProseLine(markdown, fn) {
	let fence = null;
	markdown.split('\n').forEach((line, index) => {
		const match = FENCE.exec(line);
		if (match) {
			if (!fence) fence = match[1];
			else if (match[1][0] === fence[0] && match[1].length >= fence.length) fence = null;
			return;
		}
		if (!fence) fn(line, index + 1);
	});
}

function headingIds(markdown) {
	const ids = [];
	forEachProseLine(markdown, (line) => {
		const match = /^#{1,6}\s.*\{#([^}\s]+)\}\s*$/.exec(line);
		if (match) ids.push(match[1]);
	});
	return ids;
}

// Anchors of the page, in order: the explicit {#id} of a heading, or the slug Docusaurus generates from its text
function anchors(markdown) {
	const seen = new Map();
	// Same algorithm as github-slugger, which Docusaurus uses: repeated slugs get -1, -2...
	const slug = (text) => {
		// A trailing hyphen (left by a trailing emoji) is ignored: the translation writes the anchor without it
		const base = text.toLowerCase().replace(/[^\p{L}\p{M}\p{N}\p{Pc}\- ]/gu, '').replace(/ /g, '-').replace(/-+$/, '');
		let candidate = base;
		for (let n = seen.get(base) || 0; seen.has(candidate); n++) {
			candidate = `${base}-${n + 1}`;
			seen.set(base, n + 1);
		}
		seen.set(candidate, 0);
		return candidate;
	};
	const result = [];
	forEachProseLine(markdown, (line) => {
		const match = /^#{1,6}\s+(.*?)\s*$/.exec(line);
		if (!match) return;
		const explicit = /^(.*?)\s*\{#([^}\s]+)\}$/.exec(match[1]);
		if (explicit) result.push(explicit[2]);
		else result.push(slug(match[1].replace(/<[^>]+>|[`*]|!?\[([^\]]*)\]\([^)]*\)/g, '$1')));
	});
	return result;
}

function countFences(markdown) {
	let count = 0;
	let fence = null;
	markdown.split('\n').forEach((line) => {
		const match = FENCE.exec(line);
		if (!match) return;
		if (!fence) {
			fence = match[1];
			count++;
		} else if (match[1][0] === fence[0] && match[1].length >= fence.length) fence = null;
	});
	return count;
}

const FRONT_MATTER = /^---\r?\n([\s\S]*?)\r?\n---[ \t]*(\r?\n|$)/;

function frontMatterKeys(markdown) {
	const match = FRONT_MATTER.exec(markdown);
	const keys = {};
	if (!match) return keys;
	match[1].split('\n').forEach((line) => {
		const entry = /^(id|slug):\s*(.*?)\s*$/.exec(line);
		if (entry) keys[entry[1]] = entry[2].replace(/^(['"])(.*)\1$/, '$2');
	});
	return keys;
}

// Front matter replaced by blank lines, so that the line numbers of the errors stay the ones of the file
function stripFrontMatter(markdown) {
	const match = FRONT_MATTER.exec(markdown);
	if (!match) return markdown;
	return '\n'.repeat(match[0].split('\n').length - 1) + markdown.slice(match[0].length);
}

function relativeLinks(markdown) {
	const links = [];
	forEachProseLine(markdown, (line, lineNumber) => {
		const pattern = /\]\(\s*<?([^)\s>]+)/g;
		let match;
		while ((match = pattern.exec(line))) {
			const url = match[1];
			if (/^([a-z][a-z0-9+.-]*:|\/|#)/i.test(url)) continue;
			if (/\.mdx?([#?].*)?$/.test(url)) links.push({url, line: lineNumber});
		}
	});
	return links;
}

let processor;

// Compiles the page like Docusaurus does for .md files with the default `mdx` format
async function checkMdx(markdown) {
	if (!processor) {
		const {createProcessor} = await import('@mdx-js/mdx');
		const remarkGfm = (await import('remark-gfm')).default;
		const remarkDirective = (await import('remark-directive')).default;
		processor = createProcessor({format: 'mdx', remarkPlugins: [remarkGfm, remarkDirective]});
	}
	const {escapeMarkdownHeadingIds, unwrapMdxCodeBlocks} = require('@docusaurus/utils');
	const source = escapeMarkdownHeadingIds(unwrapMdxCodeBlocks(stripFrontMatter(markdown)));
	try {
		await processor.process(source);
		return null;
	} catch (error) {
		const position = error.position || error.place;
		const start = position && (position.start || position);
		const inMessage = /\((\d+):\d+/.exec(error.message);
		return {line: error.line || (start && start.line) || (inMessage && Number(inMessage[1])) || 1,
			message: (error.reason || error.message).replace(/\s*\(\d+:\d+(-\d+:\d+)?\)$/, '')};
	}
}

function listPages(dir, base = dir) {
	if (!fs.existsSync(dir)) return [];
	return fs.readdirSync(dir, {withFileTypes: true}).flatMap((entry) => {
		const full = path.join(dir, entry.name);
		if (entry.isDirectory()) return listPages(full, base);
		return /\.mdx?$/.test(entry.name) ? [path.relative(base, full).split(path.sep).join('/')] : [];
	});
}

function linkExists(siteDir, fromRelative, url) {
	const target = url.replace(/[#?].*$/, '');
	const fromDir = path.posix.dirname(fromRelative);
	const candidates = [path.posix.normalize(path.posix.join(fromDir, target))];
	// A link that does not start with ./ or ../ is also tried from the content root
	if (!/^\.\.?\//.test(target)) candidates.push(path.posix.normalize(target));
	// The generated pages exist only after a build (they are git-ignored), and the lint runs without one
	return candidates.some((candidate) => !candidate.startsWith('..') && (GENERATED.has(candidate) || [ES_ROOT, EN_ROOT]
		.some((root) => fs.existsSync(path.join(siteDir, root, candidate)))));
}

async function run({siteDir = process.cwd(), files} = {}) {
	const errors = [];
	const esDir = path.join(siteDir, ES_ROOT);
	const pages = files && files.length
		? files.map((f) => path.relative(esDir, path.resolve(siteDir, f)).split(path.sep).join('/'))
			.filter((p) => !p.startsWith('..'))
		: listPages(esDir);
	let checked = 0;
	for (const page of pages) {
		if (GENERATED.has(page)) continue;
		const file = `${ES_ROOT}/${page}`;
		const add = (message, line = 1) => errors.push({file, line, message});
		if (!fs.existsSync(path.join(siteDir, file))) continue;
		checked++;
		const es = fs.readFileSync(path.join(siteDir, file), 'utf8');

		const syntax = await checkMdx(es);
		if (syntax) add(`MDX does not compile: ${syntax.message}`, syntax.line);

		const enFile = path.join(siteDir, EN_ROOT, page);
		if (!fs.existsSync(enFile)) {
			add(`no English page ${EN_ROOT}/${page}`);
		} else {
			const en = fs.readFileSync(enFile, 'utf8');
			// The anchors of the English page must exist in the translation (a translated heading keeps them through an
			// explicit {#id}), and an explicit id of the translation must be an anchor of the English page
			const esAnchors = anchors(es);
			const enAnchors = anchors(en);
			enAnchors.filter((id) => !esAnchors.includes(id)).forEach((id) => add(`missing heading id "${id}"`));
			headingIds(es).filter((id) => !enAnchors.includes(id)).forEach((id) => add(`extra heading id "${id}"`));
			const esFences = countFences(es);
			const enFences = countFences(en);
			if (esFences !== enFences) add(`code blocks: ${esFences} (es) vs ${enFences} (en)`);
			const esFront = frontMatterKeys(es);
			const enFront = frontMatterKeys(en);
			['id', 'slug'].forEach((key) => {
				if (esFront[key] !== enFront[key]) {
					add(`front matter ${key}: ${esFront[key] === undefined ? 'missing' : esFront[key]} (es) vs `
						+ `${enFront[key] === undefined ? 'missing' : enFront[key]} (en)`);
				}
			});
		}

		relativeLinks(es).forEach(({url, line}) => {
			if (!linkExists(siteDir, page, url)) add(`broken link ${url}`, line);
		});
	}
	return {errors, checked};
}

async function main() {
	const siteDir = path.resolve(__dirname, '..');
	const {errors, checked} = await run({siteDir, files: process.argv.slice(2)});
	errors.forEach((e) => console.error(`${e.file}:${e.line}  ${e.message}`));
	console.log(`Lint · Spanish docs: ${checked} pages checked, ${errors.length} errors`);
	process.exit(errors.length ? 1 : 0);
}

module.exports = {checkMdx, headingIds, anchors, countFences, frontMatterKeys, relativeLinks, run};

if (require.main === module) {
	main().catch((error) => {
		console.error(error);
		process.exit(1);
	});
}
