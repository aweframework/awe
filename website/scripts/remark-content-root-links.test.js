// Run with: node --test website/scripts
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const {rewrite} = require('./remark-content-root-links');

test('a sibling link becomes a link from the docs root', () => {
	assert.strictEqual(rewrite('screen.md', 'docs/api/include.md'), 'api/screen.md');
});

test('a parent link keeps its anchor', () => {
	assert.strictEqual(rewrite('../guides/x.md#a', 'docs/api/include.md'), 'guides/x.md#a');
});

test('works in versioned docs and in the Spanish tree', () => {
	assert.strictEqual(rewrite('screen.md', 'versioned_docs/version-4.12.0/api/include.md'), 'api/screen.md');
	assert.strictEqual(
		rewrite('screen.md', 'i18n/es/docusaurus-plugin-content-docs/version-4.12.0/api/include.md'), 'api/screen.md');
	assert.strictEqual(
		rewrite('../guides/x.md', 'i18n/es/docusaurus-plugin-content-docs/current/api/include.md'), 'guides/x.md');
});

test('external, absolute, anchor and non-markdown links are untouched', () => {
	for (const url of ['https://a.b/c.md', '/docs/x', '#top', 'image.png', 'mailto:a@b.c']) {
		assert.strictEqual(rewrite(url, 'docs/api/include.md'), url);
	}
});

test('a link that leaves the docs root or a file outside the docs is untouched', () => {
	assert.strictEqual(rewrite('../../x.md', 'docs/api/include.md'), '../../x.md');
	assert.strictEqual(rewrite('x.md', 'blog/post.md'), 'x.md');
});

test('a link that already is from the content root is kept', () => {
	const site = fs.mkdtempSync(path.join(os.tmpdir(), 'remark-links-'));
	fs.mkdirSync(path.join(site, 'docs/api'), {recursive: true});
	fs.writeFileSync(path.join(site, 'docs/api/x.md'), '');
	assert.strictEqual(rewrite('api/x.md', 'docs/guides/y.md', site), 'api/x.md');
	assert.strictEqual(
		rewrite('api/x.md', 'i18n/es/docusaurus-plugin-content-docs/current/guides/y.md', site), 'api/x.md');
	assert.strictEqual(rewrite('z.md', 'docs/guides/y.md', site), 'guides/z.md');
	fs.rmSync(site, {recursive: true});
});

test('leaves the links of a docs version without any translated tree untouched', () => {
	const site = fs.mkdtempSync(path.join(os.tmpdir(), 'site-'));
	fs.mkdirSync(path.join(site, 'versioned_docs', 'version-4.x', 'api'), {recursive: true});
	assert.strictEqual(rewrite('chart-format.md#tooltips', 'versioned_docs/version-4.x/api/chart.md', site), 'chart-format.md#tooltips');
});
