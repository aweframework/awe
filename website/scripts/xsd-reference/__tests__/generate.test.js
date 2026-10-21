'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {generate, parseArgs, assertSafeOutput, run} = require('../generate');

const FIXTURES = path.join(__dirname, '..', 'fixtures');

function tempRoot() {
	return fs.mkdtempSync(path.join(os.tmpdir(), 'xsd-reference-'));
}

test('parseArgs resolves the options and has defaults', () => {
	const options = parseArgs(['--xsd-dir', 'some/dir', '--out', 'out']);
	assert.equal(options.xsdDir, path.resolve('some/dir'));
	assert.equal(options.out, path.resolve('out'));
	assert.ok(parseArgs([]).out.endsWith(path.join('website', 'reference')));
	assert.ok(parseArgs([]).thresholds.endsWith(path.join('xsd-reference', 'thresholds.json')));
	assert.equal(parseArgs(['--thresholds', 't.json']).thresholds, path.resolve('t.json'));
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

function thresholdsFile(root, maxUndocumented) {
	const file = path.join(root, 'thresholds.json');
	fs.writeFileSync(file, JSON.stringify({maxUndocumented}));
	return file;
}

test('run exits with 1 and lists the undocumented attributes above the threshold', () => {
	const root = tempRoot();
	const lines = [];
	const code = run({xsdDir: FIXTURES, out: path.join(root, 'reference'), root, thresholds: thresholdsFile(root, {'main.xsd': 4})}, (line) => lines.push(line));
	const output = lines.join('\n');
	assert.equal(code, 1);
	assert.match(output, /XSD attribute documentation coverage/);
	assert.match(output, /base\.xsd: 1 undocumented attributes, the threshold allows 0/);
	assert.match(output, /commonAttr@visible/);
	assert.ok(fs.existsSync(path.join(root, 'reference', 'index.mdx')), 'the pages are still written');
});

test('run exits with 0 when every schema is within its threshold', () => {
	const root = tempRoot();
	const thresholds = thresholdsFile(root, {'base.xsd': 1, 'main.xsd': 4, 'extras.xsd': 99, 'empty.xsd': 0});
	assert.equal(run({xsdDir: FIXTURES, out: path.join(root, 'reference'), root, thresholds}, () => {}), 0);
});

test('run fails with an actionable message when the thresholds file is missing or malformed', () => {
	const root = tempRoot();
	const out = path.join(root, 'reference');
	assert.throws(() => run({xsdDir: FIXTURES, out, root, thresholds: path.join(root, 'none.json')}, () => {}), /thresholds/);
	fs.writeFileSync(path.join(root, 'bad.json'), '{"other": {}}');
	assert.throws(() => run({xsdDir: FIXTURES, out, root, thresholds: path.join(root, 'bad.json')}, () => {}), /maxUndocumented/);
});

test('run rejects a threshold that is not a whole number of attributes, so the gate never passes by mistake', () => {
	const root = tempRoot();
	const out = path.join(root, 'reference');
	for (const value of ['12', -1, 1.5, null]) {
		const thresholds = thresholdsFile(root, {'main.xsd': value});
		assert.throws(() => run({xsdDir: FIXTURES, out, root, thresholds}, () => {}), /main\.xsd.*whole number/);
	}
});
