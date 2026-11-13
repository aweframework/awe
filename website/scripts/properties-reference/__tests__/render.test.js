'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const {renderPage, escapeCell, codeSpan, MARKER} = require('../render');
const {propertyReport, formatReport} = require('../report');

const GROUPS = [
	{id: 'awe-base-properties', title: 'AWE Base Properties', prefix: 'awe.application', intro: 'Base intro.'},
	{id: 'awe-rest-properties', title: 'AWE Rest Properties', prefix: 'awe.rest', intro: 'Rest intro.', outro: 'rest-examples.md'},
];

const ENTRIES = [
	{name: 'awe.application.name', type: 'String', description: 'Application name.', defaultValue: 'AWE'},
	{name: 'awe.rest.timeout', type: 'Duration', description: 'Pipe | and {brace} and <tag>.', defaultValue: ''},
	{name: 'other.thing', type: 'String', description: 'Unknown prefix.', defaultValue: '1'},
];

const OPTIONS = {
	preface: '---\nid: properties\n---\n\n## Introduction\n\nHello.\n',
	appendix: '## Overwriting properties\n\nText.\n',
	snippets: {'rest-examples.md': '### Examples\n\ncode\n'},
};

function render(entries = ENTRIES, groups = GROUPS) {
	return renderPage(entries, groups, OPTIONS);
}

test('escapeCell protects table pipes, MDX braces and angle brackets outside code', () => {
	assert.equal(escapeCell('a | b'), 'a \\| b');
	assert.equal(escapeCell('use {x} and <y>'), 'use \\{x\\} and &lt;y&gt;');
	assert.equal(escapeCell('keep `{x} <y>` literal'), 'keep `{x} <y>` literal');
	assert.equal(escapeCell('line<br/>- item'), 'line<br/>- item');
	assert.equal(escapeCell('[link](https://x.io/a_b)'), '[link](https://x.io/a_b)');
});

test('codeSpan survives backticks and pipes', () => {
	assert.equal(codeSpan('abc'), '`abc`');
	assert.equal(codeSpan('a|b'), '`a\\|b`');
	assert.equal(codeSpan('a`b'), '`` a`b ``');
	assert.equal(codeSpan(''), '');
});

test('the page starts with the preface, then the marker, the groups and the appendix', () => {
	const page = render();
	assert.ok(page.startsWith('---\nid: properties\n---\n'));
	assert.ok(page.includes(MARKER));
	assert.ok(page.indexOf('## Introduction') < page.indexOf('## AWE Base Properties'));
	assert.ok(page.indexOf('## AWE Base Properties') < page.indexOf('## AWE Rest Properties'));
	assert.ok(page.indexOf('## AWE Rest Properties') < page.indexOf('### Examples'));
	assert.ok(page.indexOf('### Examples') < page.indexOf('## Overwriting properties'));
});

test('the marker goes after the front matter and the import so the page stays valid MDX', () => {
	const page = renderPage(ENTRIES, GROUPS, {...OPTIONS, preface: '---\nid: properties\n---\n\nimport Anchor from \'@site/src/components/Anchor\';\n\n## Introduction\n'});
	const frontMatterEnd = page.indexOf('---', 4);
	assert.ok(page.indexOf(MARKER) > frontMatterEnd);
});

test('every property has an anchor row with the Anchor component and a self link', () => {
	const page = render();
	assert.ok(page.includes('<Anchor id="awe.application.name"/> [awe.application.name](#awe.application.name)'));
	assert.ok(page.includes('| `AWE` |'));
});

test('group headings keep the ids that other pages link to', () => {
	const page = render();
	assert.match(page, /^## AWE Rest Properties \{#awe-rest-properties\}$/m);
	assert.match(page, /^## Other properties \{#other-properties\}$/m);
	assert.ok(page.includes('Rest intro.'));
});

test('descriptions are escaped for MDX and tables', () => {
	const row = render().split('\n').find((line) => line.includes('awe.rest.timeout'));
	assert.ok(row.includes('Pipe \\| and \\{brace\\} and &lt;tag&gt;.'));
});

test('groups without properties are omitted and unmatched properties go to "Other properties"', () => {
	const noRest = render(ENTRIES.filter((e) => !e.name.startsWith('awe.rest')));
	assert.ok(!noRest.includes('## AWE Rest Properties'));
	assert.ok(!noRest.includes('### Examples'));
	const page = render();
	assert.match(page, /^## Other properties /m);
	assert.ok(page.includes('[other.thing](#other.thing)'));
	assert.ok(!render(ENTRIES.slice(0, 2)).includes('## Other properties'));
});

test('a property goes to the longest matching prefix', () => {
	const groups = [
		{id: 'a-props', title: 'A Properties', prefix: 'awe.x'},
		{id: 'b-props', title: 'B Properties', prefix: 'awe.x.sub'},
	];
	const page = renderPage([{name: 'awe.x.sub.k', type: 'String', description: 'd', defaultValue: ''}], groups, OPTIONS);
	assert.ok(page.includes('## B Properties'));
	assert.ok(!page.includes('## A Properties'));
});

test('a prefix only matches whole name segments', () => {
	const groups = [{id: 'a-props', title: 'A Properties', prefix: 'awe.x'}];
	const page = renderPage([{name: 'awe.xylophone', type: 'String', description: 'd', defaultValue: ''}], groups, OPTIONS);
	assert.match(page, /^## Other properties /m);
});

test('the output is deterministic and does not depend on the input order', () => {
	assert.equal(render(ENTRIES), render([...ENTRIES].reverse()));
});

test('deprecated properties get a visible marker with the replacement and the reason', () => {
	const entry = {name: 'awe.old', type: 'String', description: 'Old one.', defaultValue: '',
		deprecated: {replacement: 'awe.new', reason: 'Renamed.'}};
	const row = render([entry]).split('\n').find((line) => line.includes('awe.old'));
	assert.ok(row.includes('**Deprecated**: use `awe.new` instead. Renamed. Old one.'));
	const plain = render([{...entry, deprecated: {}}]).split('\n').find((line) => line.includes('awe.old'));
	assert.ok(plain.includes('**Deprecated**. Old one.'));
});

test('a missing snippet is an error', () => {
	assert.throws(() => renderPage(ENTRIES, GROUPS, {...OPTIONS, snippets: {}}), /rest-examples\.md/);
});

test('propertyReport lists properties without description or default', () => {
	const report = propertyReport([
		{name: 'awe.a', type: 'String', description: 'd', defaultValue: 'x'},
		{name: 'awe.b', type: 'String', description: '', defaultValue: 'x'},
		{name: 'awe.c', type: 'String', description: 'd', defaultValue: ''},
	]);
	assert.equal(report.total, 3);
	assert.deepEqual(report.withoutDescription, ['awe.b']);
	assert.deepEqual(report.withoutDefault, ['awe.c']);
	const text = formatReport(report);
	assert.ok(text.includes('3 properties'));
	assert.ok(text.includes('without description: 1'));
	assert.ok(text.includes('awe.b'));
	assert.ok(text.includes('without default: 1'));
});
