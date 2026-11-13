// Run with: node --test website/scripts
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const {checkMdx, headingIds, anchors, countFences, frontMatterKeys, relativeLinks, run} = require('./check-translations');

const ES = 'i18n/es/docusaurus-plugin-content-docs/current';

function site(files) {
	const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'check-translations-'));
	Object.entries(files).forEach(([name, content]) => {
		const target = path.join(dir, name);
		fs.mkdirSync(path.dirname(target), {recursive: true});
		fs.writeFileSync(target, content);
	});
	return dir;
}

const messages = (result) => result.errors.map((e) => `${e.file}: ${e.message}`).join('\n');

test('explicit heading ids and fenced blocks are extracted, ignoring code', () => {
	const md = '# Title {#title}\n\n```md\n## Fake {#fake}\n```\n\n## Other {#other-id}\n\n~~~xml\n<a/>\n~~~\n';
	assert.deepStrictEqual(headingIds(md), ['title', 'other-id']);
	assert.strictEqual(countFences(md), 2);
});

test('front matter keeps id and slug only', () => {
	assert.deepStrictEqual(frontMatterKeys('---\nid: a\nslug: /b\ntitle: T\n---\nbody'), {id: 'a', slug: '/b'});
	assert.deepStrictEqual(frontMatterKeys('no front matter'), {});
});

test('relative markdown links are listed with their line, code is ignored', () => {
	const md = 'See [a](./a.md) and [b](../b.md#x).\n\n```\n[c](./c.md)\n```\n[web](https://x.org/y.md) [img](a.png)\n';
	assert.deepStrictEqual(relativeLinks(md), [
		{url: './a.md', line: 1}, {url: '../b.md#x', line: 1}]);
});

test('valid MDX compiles; an unclosed tag reports file line and message', async () => {
	assert.strictEqual(await checkMdx('# Hi {#hi}\n\nText <b>bold</b>\n'), null);
	const error = await checkMdx('---\ntitle: T\n---\n\nText\n\n<div>\n');
	assert.ok(error);
	assert.ok(error.line >= 7, `line ${error.line}`);
	assert.match(error.message, /div|closing/i);
});

test('a clean translation passes', async () => {
	const dir = site({
		'docs/a.md': '---\nid: a\n---\n# A {#a}\n```js\n1\n```\n[b](./b.md)\n',
		'docs/b.md': '# B {#b}\n',
		[`${ES}/a.md`]: '---\nid: a\n---\n# A {#a}\n```js\n1\n```\n[b](./b.md)\n'
	});
	const result = await run({siteDir: dir});
	assert.strictEqual(result.errors.length, 0, messages(result));
	assert.strictEqual(result.checked, 1);
});

test('differences with the English page are reported', async () => {
	const dir = site({
		'docs/a.md': '---\nid: a\nslug: /a\n---\n# A {#a}\n## S {#s}\n```js\n1\n```\n',
		[`${ES}/a.md`]: '---\nid: z\n---\n# A {#a}\n## T {#extra}\n'
	});
	const text = messages(await run({siteDir: dir}));
	assert.match(text, /missing heading id "s"/);
	assert.match(text, /extra heading id "extra"/);
	assert.match(text, /code blocks: 0 .* 1/);
	assert.match(text, /front matter id/);
	assert.match(text, /front matter slug/);
});

test('a Spanish page without an English page is an error, an untranslated English page is not', async () => {
	const dir = site({
		'docs/only-en.md': '# E\n',
		[`${ES}/only-es.md`]: '# S\n'
	});
	const result = await run({siteDir: dir});
	assert.strictEqual(result.errors.length, 1);
	assert.match(result.errors[0].message, /no English page/);
});

test('generated pages are skipped', async () => {
	const dir = site({[`${ES}/properties.md`]: '<div>\n', [`${ES}/rest-api.md`]: '<div>\n'});
	const result = await run({siteDir: dir});
	assert.strictEqual(result.errors.length, 0);
	assert.strictEqual(result.checked, 0);
});

test('broken links are reported, links to untranslated pages fall back to English', async () => {
	const dir = site({
		'docs/a.md': '# A\n',
		'docs/guides/g.md': '# G\n',
		[`${ES}/a.md`]: '# A\n\n[ok](./guides/g.md) [bad](./nope.md#x)\n'
	});
	const result = await run({siteDir: dir});
	assert.strictEqual(result.errors.length, 1, messages(result));
	assert.match(result.errors[0].message, /nope\.md/);
	assert.strictEqual(result.errors[0].line, 3);
});

test('links to the generated pages count as valid although the files exist only after a build', async () => {
	const dir = site({
		'docs/rest-module.md': '# R\n',
		[`${ES}/rest-module.md`]: '# R\n\n[api](rest-api.md) [props](./properties.md#awe.x)\n'
	});
	const result = await run({siteDir: dir});
	assert.deepStrictEqual(result.errors, [], messages(result));
});

test('only the given files are checked when paths are passed', async () => {
	const dir = site({
		'docs/a.md': '# A\n', 'docs/b.md': '# B\n',
		[`${ES}/a.md`]: '<div>\n', [`${ES}/b.md`]: '# B\n'
	});
	const result = await run({siteDir: dir, files: [`${ES}/b.md`]});
	assert.strictEqual(result.errors.length, 0);
	assert.strictEqual(result.checked, 1);
});

test('a translated heading keeps the English anchor through an explicit id', () => {
	assert.deepStrictEqual(anchors('# Hello `x`\n\n## Hello `x`\n\n## Titulo {#title-en}\n'), ['hello-x', 'hello-x-1', 'title-en']);
});

test('an English heading without explicit id is matched by the explicit id of the translation', async () => {
	const dir = site({
		'docs/a.md': '# A\n\n## Some Title\n',
		[`${ES}/a.md`]: '# A\n\n## Un titulo {#some-title}\n'
	});
	const result = await run({siteDir: dir});
	assert.strictEqual(result.errors.length, 0, messages(result));
});
