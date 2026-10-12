'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {generate, ensureGenerated, DEFAULT_SNAPSHOT, SKIP_ENV} = require('../generate');
const {MARKER} = require('../render');

function tempDir() {
	return fs.mkdtempSync(path.join(os.tmpdir(), 'rest-gen-'));
}

function snapshotIn(dir) {
	const file = path.join(dir, 'openapi.json');
	fs.writeFileSync(file, JSON.stringify({openapi: '3.1.0', info: {title: 'T', version: '1'}, paths: {}}));
	return file;
}

test('generate writes the page from the committed snapshot', () => {
	const out = path.join(tempDir(), 'rest-api.md');
	const {endpoints} = generate({out});
	const page = fs.readFileSync(out, 'utf8');
	assert.ok(page.includes(MARKER));
	assert.ok(endpoints >= 6);
	for (const endpoint of ['POST /api/authenticate', 'POST /api/data/{queryId}', 'POST /api/public/maintain/{maintainId}']) {
		assert.ok(page.includes(`### \`${endpoint}\``), endpoint);
	}
});

test('generate is deterministic', () => {
	const dir = tempDir();
	generate({out: path.join(dir, 'a.md')});
	generate({out: path.join(dir, 'b.md')});
	assert.equal(fs.readFileSync(path.join(dir, 'a.md'), 'utf8'), fs.readFileSync(path.join(dir, 'b.md'), 'utf8'));
});

test('generate refuses to overwrite a hand-written page', () => {
	const out = path.join(tempDir(), 'rest-api.md');
	fs.writeFileSync(out, '# hand written\n');
	assert.throws(() => generate({out}), /Refusing to overwrite/);
	assert.equal(fs.readFileSync(out, 'utf8'), '# hand written\n');
});

test('generate explains how to create a missing snapshot', () => {
	const dir = tempDir();
	assert.throws(() => generate({out: path.join(dir, 'x.md'), snapshot: path.join(dir, 'none.json')}), /OpenApiSnapshotTest/);
});

test('ensureGenerated creates a missing page and refreshes a stale one', () => {
	const dir = tempDir();
	const out = path.join(dir, 'rest-api.md');
	const snapshot = snapshotIn(dir);
	ensureGenerated({out, snapshot, env: {}});
	assert.ok(fs.readFileSync(out, 'utf8').includes(MARKER));
	fs.writeFileSync(out, `${fs.readFileSync(out, 'utf8')}stale`);
	ensureGenerated({out, snapshot, env: {}});
	assert.ok(!fs.readFileSync(out, 'utf8').endsWith('stale'));
});

test(`ensureGenerated does nothing when ${SKIP_ENV}=1`, () => {
	const out = path.join(tempDir(), 'rest-api.md');
	ensureGenerated({out, env: {[SKIP_ENV]: '1'}});
	assert.equal(fs.existsSync(out), false);
});

test('the committed snapshot is a sorted OpenAPI document without servers', () => {
	const spec = JSON.parse(fs.readFileSync(DEFAULT_SNAPSHOT, 'utf8'));
	assert.ok(spec.openapi && spec.paths);
	assert.equal(spec.servers, undefined);
	assert.deepEqual(Object.keys(spec.paths), [...Object.keys(spec.paths)].sort());
});

test('generate reports tags declared with different descriptions', () => {
	const dir = tempDir();
	const snapshot = path.join(dir, 'openapi.json');
	fs.writeFileSync(snapshot, JSON.stringify({info: {title: 'T'}, paths: {}, tags: [{name: 'X', description: 'a'}, {name: 'X', description: 'b'}]}));
	assert.deepEqual(generate({out: path.join(dir, 'p.md'), snapshot}).conflicts, ['X']);
});

test('the committed snapshot declares every tag with one description', () => {
	assert.deepEqual(generate({out: path.join(tempDir(), 'p.md')}).conflicts, []);
});
