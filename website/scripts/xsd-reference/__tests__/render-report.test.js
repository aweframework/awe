'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const {loadSchemas, buildReference} = require('../xsd-model');
const {renderPages} = require('../render');
const {coverageReport, formatReport, checkThresholds, formatThresholdCheck} = require('../report');

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

test('coverage report lists the undocumented attributes of each schema with their owner', () => {
	const report = coverageReport(fixtureReference());
	assert.deepEqual(report.rows.map((r) => [r.file, r.undocumentedNames]), [
		['base.xsd', ['commonAttr@visible']],
		['main.xsd', ['boxType@size', 'boxType@mode', 'itemType@mixed', 'specialItemType@level']],
	]);
});

test('threshold check fails the schemas with more undocumented attributes than allowed', () => {
	const check = checkThresholds(coverageReport(fixtureReference()), {'base.xsd': 1, 'main.xsd': 3});
	assert.equal(check.passed, false);
	assert.deepEqual(check.failures.map((f) => [f.file, f.undocumented, f.max]), [['main.xsd', 4, 3]]);
	const text = formatThresholdCheck(check);
	assert.match(text, /main\.xsd: 4 undocumented attributes, the threshold allows 3/);
	assert.match(text, /boxType@size/);
	assert.match(text, /specialItemType@level/);
	assert.doesNotMatch(text, /commonAttr@visible/);
});

test('threshold check allows no undocumented attribute in a schema without a threshold', () => {
	const check = checkThresholds(coverageReport(fixtureReference()), {'main.xsd': 4});
	assert.deepEqual(check.failures.map((f) => [f.file, f.max]), [['base.xsd', 0]]);
});

test('threshold check passes at the threshold and asks to lower a threshold above the count', () => {
	const check = checkThresholds(coverageReport(fixtureReference()), {'base.xsd': 1, 'main.xsd': 6});
	assert.equal(check.passed, true);
	assert.deepEqual(check.failures, []);
	assert.deepEqual(check.lowerable.map((l) => [l.file, l.undocumented, l.max]), [['main.xsd', 4, 6]]);
	assert.match(formatThresholdCheck(check), /lower the threshold of main\.xsd from 6 to 4/);
});

test('the AWE schemas stay within the committed thresholds', () => {
	const thresholds = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'thresholds.json'), 'utf8')).maxUndocumented;
	const check = checkThresholds(coverageReport(buildReference(loadSchemas(REAL_XSDS))), thresholds);
	assert.deepEqual(check.failures, [], formatThresholdCheck(check));
});
