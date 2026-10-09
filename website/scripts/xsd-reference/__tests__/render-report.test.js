'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const {loadSchemas, buildReference} = require('../xsd-model');
const {renderPages} = require('../render');
const {coverageReport, formatReport} = require('../report');

const FIXTURES = path.join(__dirname, '..', 'fixtures');
const REAL_XSDS = path.join(__dirname, '..', '..', '..', '..', 'awe-framework', 'awe-generic-screens', 'src', 'main', 'resources', 'schemas', 'awe');

function fixtureReference() {
	return buildReference(loadSchemas(FIXTURES, ['main.xsd', 'base.xsd']));
}

function fixturePages() {
	return renderPages(fixtureReference());
}

/** Checks that every markdown link of the pages targets an existing page and an existing heading id. */
function brokenLinks(pages) {
	const anchors = new Map();
	for (const [file, content] of Object.entries(pages)) {
		anchors.set(file, new Set([...content.matchAll(/\{#([A-Za-z0-9_-]+)\}/g)].map((m) => m[1])));
	}
	const broken = [];
	for (const [file, content] of Object.entries(pages)) {
		for (const [, target] of content.matchAll(/\]\(([^)\s]+)\)/g)) {
			if (/^https?:/.test(target)) {
				continue;
			}
			const [page, anchor] = target.split('#');
			const targetFile = page === '' ? file : page;
			if (!anchors.has(targetFile) || (anchor && !anchors.get(targetFile).has(anchor))) {
				broken.push(`${file} -> ${target}`);
			}
		}
	}
	return broken;
}

test('renders one page per schema plus an index', () => {
	assert.deepEqual(Object.keys(fixturePages()).sort(), ['base.mdx', 'index.mdx', 'main.mdx']);
});

test('page front matter carries the schema title', () => {
	const main = fixturePages()['main.mdx'];
	assert.match(main, /^---\nid: "main"\ntitle: "Fixture Main Schema"\nsidebar_label: "main\.xsd"/);
});

test('escapes characters that are special in MDX', () => {
	const main = fixturePages()['main.mdx'];
	assert.ok(main.includes('Describes a &#123;box&#125; with &lt;items&gt; and a \\| pipe.'));
	assert.ok(!main.includes('{box}'), 'no raw braces from the documentation');
});

test('renders elements with anchors, attributes, children and containers', () => {
	const main = fixturePages()['main.mdx'];
	assert.match(main, /### `box` \{#element-box\}/);
	assert.match(main, /\| `id` \| .* \| yes \| .* \| Unique identifier of the element\. \|/);
	assert.match(main, /\| `visible` \| .*`true`.*`false`.* \| no \| `true` \|/);
	assert.match(main, /\[`item`\]\(#element-item\)/);
	assert.match(main, /\[`note`\]\(base\.mdx#element-note\)/);
	assert.match(main, /Contained by.*\[`box`\]\(#element-box\)/s);
});

test('a long enumeration links to its type section instead of being inlined', () => {
	const pages = fixturePages();
	assert.match(pages['main.mdx'], /\[`sizeEnum`\]\(base\.mdx#type-sizeenum\)/);
	assert.match(pages['base.mdx'], /### `sizeEnum` \{#type-sizeenum\}/);
});

test('every generated link points at an existing page and anchor (fixtures)', () => {
	assert.deepEqual(brokenLinks(fixturePages()), []);
});

test('every generated link points at an existing page and anchor (real AWE schemas)', () => {
	const schemaCount = fs.readdirSync(REAL_XSDS).filter((f) => f.endsWith('.xsd')).length;
	const reference = buildReference(loadSchemas(REAL_XSDS));
	assert.equal(reference.schemas.length, schemaCount);
	const pages = renderPages(reference);
	assert.equal(Object.keys(pages).length, schemaCount + 1);
	assert.deepEqual(brokenLinks(pages), []);
});

test('every generated link points at an existing page and anchor (local elements and attribute refs)', () => {
	const pages = renderPages(buildReference(loadSchemas(FIXTURES, ['extras.xsd'])));
	assert.deepEqual(brokenLinks(pages), []);
	assert.match(pages['extras.mdx'], /### `cell` \{#element-panelType--cell\}/);
	assert.match(pages['extras.mdx'], /\[`cell`\]\(#element-panelType--cell\)/);
});

test('an undocumented attribute says so in the description column', () => {
	const main = fixturePages()['main.mdx'];
	assert.match(main, /\| `size` \| .* \| no \| {2}\| Not documented yet \|/);
});

test('coverage report counts documented attributes per schema', () => {
	const report = coverageReport(fixtureReference());
	assert.deepEqual(report.rows.map((r) => [r.file, r.attributes, r.documented, r.undocumented]), [
		['base.xsd', 2, 1, 1],
		['main.xsd', 5, 1, 4],
	]);
	assert.deepEqual([report.total.attributes, report.total.documented, report.total.undocumented], [7, 2, 5]);
});

test('formats the coverage report as a readable table', () => {
	const text = formatReport(coverageReport(fixtureReference()));
	assert.match(text, /schema\s+attributes\s+documented\s+undocumented\s+coverage/);
	assert.match(text, /main\.xsd\s+5\s+1\s+4\s+20%/);
	assert.match(text, /TOTAL\s+7\s+2\s+5\s+29%/);
});

test('shows no coverage percentage for a schema without declared attributes', () => {
	const reference = buildReference(loadSchemas(FIXTURES, ['empty.xsd']));
	assert.match(formatReport(coverageReport(reference)), /empty\.xsd\s+0\s+0\s+0\s+-/);
});
