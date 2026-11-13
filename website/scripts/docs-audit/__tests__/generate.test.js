'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {collect, generate, ruleFor, areaOf, escapeCell, MARKER, PAGE} = require('../generate');

const WEBSITE = path.resolve(__dirname, '..', '..', '..');
const REVIEWS = path.join(__dirname, '..', 'reviews.json');

const created = [];

// Every fixture lives in its own temporary folder, removed when the test ends
test.afterEach(() => {
	while (created.length) {
		fs.rmSync(created.pop(), {recursive: true, force: true});
	}
});

function fixture() {
	const root = fs.mkdtempSync(path.join(os.tmpdir(), 'docs-audit-'));
	created.push(root);
	const write = (file, content) => {
		fs.mkdirSync(path.dirname(path.join(root, file)), {recursive: true});
		fs.writeFileSync(path.join(root, file), content);
	};
	const es = 'i18n/es/docusaurus-plugin-content-docs/current';
	write('docs/intro.md', '---\nid: intro\ntitle: Intro | page\n---\nBody\n');
	write('docs/api/grid.md', '---\ntitle: "Grid"\n---\nBody\n');
	write('docs/guides/other.md', 'No front matter\n');
	write('docs/properties.md', '---\ntitle: Properties\n---\n');
	write(`${es}/intro.md`, '---\nid: intro\n---\n');
	const reviews = path.join(root, 'reviews.json');
	fs.writeFileSync(reviews, JSON.stringify({
		rules: [
			{paths: ['api/'], reviewed: '2026-01-01', ref: 'MR 1', notes: 'a | b'},
			{paths: ['properties.md'], reviewed: 'generated', ref: 'sources', notes: ''},
			{default: true, reviewed: '2026-02-02', ref: 'MR 2', notes: ''}
		]
	}));
	return {root, reviews};
}

test('the area comes from the folder and the Spanish status from the file tree', () => {
	const rows = collect(fixture());
	const byPage = Object.fromEntries(rows.map((row) => [row.page, row]));
	assert.deepEqual(rows.map((row) => row.page), ['api/grid.md', 'guides/other.md', 'intro.md', 'properties.md']);
	assert.equal(byPage['api/grid.md'].area, 'api');
	assert.equal(byPage['intro.md'].area, '');
	assert.equal(byPage['intro.md'].spanish, 'translated');
	assert.equal(byPage['api/grid.md'].spanish, 'fallback');
	assert.equal(byPage['properties.md'].spanish, 'generated');
	assert.equal(byPage['api/grid.md'].title, 'Grid');
	assert.equal(byPage['guides/other.md'].title, 'guides/other');
});

test('the first matching rule wins and the default covers the rest', () => {
	const {rules} = JSON.parse(fs.readFileSync(fixture().reviews, 'utf8'));
	assert.equal(ruleFor(rules, 'api/x.md').ref, 'MR 1');
	assert.equal(ruleFor(rules, 'intro.md').ref, 'MR 2');
	assert.equal(areaOf('training/awe-101.md'), 'training');
	assert.equal(areaOf('unknown/x.md'), '');
});

test('generates the English and Spanish pages with escaped cells and relative links', () => {
	const options = fixture();
	const {outputs} = generate(options);
	const en = fs.readFileSync(outputs.en, 'utf8');
	const es = fs.readFileSync(outputs.es, 'utf8');
	assert.ok(en.includes(MARKER) && es.includes(MARKER));
	assert.ok(en.includes('id: docs-audit'));
	assert.ok(en.includes('[Intro \\| page](../intro.md)'));
	assert.ok(en.includes('[Grid](../api/grid.md)'));
	assert.ok(en.includes('a \\| b'));
	assert.ok(es.includes('Auditoría de la documentación') && es.includes('Traducida'));
	assert.ok(en.includes('Translated'));
});

test('regenerating overwrites a generated page but never a hand-written one', () => {
	const options = fixture();
	generate(options);
	generate(options);
	const handWritten = path.join(options.root, 'docs', PAGE);
	fs.writeFileSync(handWritten, 'Hand written\n');
	assert.throws(() => generate(options), /not overwriting/);
});

test('every current page of the repository has a review date and a known area', () => {
	for (const row of collect({root: WEBSITE, reviews: REVIEWS})) {
		assert.notEqual(row.rule.reviewed, 'not reviewed', `${row.page} has no rule in reviews.json`);
		assert.match(row.rule.reviewed, /^(\d{4}-\d{2}-\d{2}|generated)$/, row.page);
		// A page in an unknown folder falls back to the empty area, so only top-level pages may have it
		const known = row.area === '' ? !row.page.includes('/') : areaOf(row.page) === row.area;
		assert.ok(known, `${row.page} is in a folder with no area in AREAS`);
	}
});

test('MDX-sensitive characters of titles and notes are escaped', () => {
	const cell = escapeCell('<Foo> {bar} [x] | a\nb');
	assert.ok(!/[<>{}]/.test(cell), cell);
	assert.ok(cell.includes('\\|') && !cell.includes('\n'));
	assert.ok(cell.includes('\\[x\\]'));
});

test('a title with MDX-sensitive characters does not reach the generated page raw', () => {
	const options = fixture();
	fs.writeFileSync(path.join(options.root, 'docs', 'guides', 'other.md'), '---\ntitle: "A <b> {c}"\n---\n');
	const {outputs} = generate(options);
	for (const output of [outputs.en, outputs.es]) {
		const content = fs.readFileSync(output, 'utf8');
		assert.ok(!content.includes('<b>') && !content.includes('{c}'));
		assert.ok(content.includes('A &#60;b&#62; &#123;c&#125;'));
	}
});

test('every page named in reviews.json exists', () => {
	const {rules} = JSON.parse(fs.readFileSync(REVIEWS, 'utf8'));
	const pages = new Set(collect({root: WEBSITE, reviews: REVIEWS}).map((row) => row.page));
	// Generated at build time, so absent from a fresh checkout
	const generated = new Set(['properties.md', 'rest-api.md']);
	for (const rule of rules) {
		for (const entry of rule.paths || []) {
			if (generated.has(entry)) continue;
			const found = entry.endsWith('/') ? [...pages].some((page) => page.startsWith(entry)) : pages.has(entry);
			assert.ok(found, `reviews.json names ${entry}, which is not a page`);
		}
	}
});
