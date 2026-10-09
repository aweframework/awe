'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {generate, parseArgs, assertSafeOutput} = require('../generate');

const FIXTURES = path.join(__dirname, '..', 'fixtures');

function tempRoot() {
	return fs.mkdtempSync(path.join(os.tmpdir(), 'xsd-reference-'));
}

test('parseArgs resolves the options and has defaults', () => {
	const options = parseArgs(['--xsd-dir', 'some/dir', '--out', 'out']);
	assert.equal(options.xsdDir, path.resolve('some/dir'));
	assert.equal(options.out, path.resolve('out'));
	assert.ok(parseArgs([]).out.endsWith(path.join('website', 'reference')));
});

test('parseArgs rejects unknown options and options without a value', () => {
	assert.throws(() => parseArgs(['--nope', 'x']), /Usage/);
	assert.throws(() => parseArgs(['--out']), /Usage/);
});

test('writes the pages and a marker file into a new output folder', () => {
	const root = tempRoot();
	const out = path.join(root, 'reference');
	const {pages} = generate({xsdDir: FIXTURES, out, root});
	assert.ok(pages.includes('index.mdx'));
	assert.ok(fs.existsSync(path.join(out, 'main.mdx')));
	assert.ok(fs.existsSync(path.join(out, '.generated-by-xsd-reference')));
});

test('regenerates a folder it generated before, dropping stale pages', () => {
	const root = tempRoot();
	const out = path.join(root, 'reference');
	generate({xsdDir: FIXTURES, out, root});
	fs.writeFileSync(path.join(out, 'stale.mdx'), 'old');
	generate({xsdDir: FIXTURES, out, root});
	assert.ok(!fs.existsSync(path.join(out, 'stale.mdx')));
});

test('refuses to delete an output folder that it did not generate', () => {
	const root = tempRoot();
	const out = path.join(root, 'docs');
	fs.mkdirSync(out);
	fs.writeFileSync(path.join(out, 'precious.md'), 'keep me');
	assert.throws(() => generate({xsdDir: FIXTURES, out, root}), /not generated/);
	assert.ok(fs.existsSync(path.join(out, 'precious.md')));
});

test('accepts an existing empty output folder', () => {
	const root = tempRoot();
	const out = path.join(root, 'reference');
	fs.mkdirSync(out);
	assert.doesNotThrow(() => assertSafeOutput(out, root));
});

test('refuses an output folder outside the website folder or the website folder itself', () => {
	const root = tempRoot();
	assert.throws(() => assertSafeOutput(path.join(root, '..', 'elsewhere'), root), /must be a folder inside/);
	assert.throws(() => assertSafeOutput(root, root), /must be a folder inside/);
	assert.throws(() => assertSafeOutput(path.parse(root).root, root), /must be a folder inside/);
});

test('fails with an actionable message when the XSD directory is missing', () => {
	const root = tempRoot();
	const missing = path.join(root, 'no-schemas-here');
	assert.throws(
		() => generate({xsdDir: missing, out: path.join(root, 'reference'), root}),
		(error) => error.message.includes(missing) && /awe-generic-screens/.test(error.message) && /full checkout|--xsd-dir/.test(error.message),
	);
	assert.ok(!fs.existsSync(path.join(root, 'reference')), 'nothing is written or deleted');
});
