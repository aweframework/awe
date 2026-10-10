'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {generate, ensureGenerated, parseArgs, refreshSnapshot, DEFAULT_SNAPSHOT, CONTENT_DIR, SKIP_ENV} = require('../generate');
const {MODULES, metadataFiles, snapshotFromFiles} = require('../metadata');
const {MARKER} = require('../render');

const REPO = path.resolve(__dirname, '..', '..', '..', '..');
const DOCS = path.resolve(REPO, 'website', 'docs');

function tempDir() {
	return fs.mkdtempSync(path.join(os.tmpdir(), 'props-gen-'));
}

function otherPagesLinks() {
	// Every "properties#anchor" link of the English docs, which the generated page must keep resolving
	const anchors = new Set();
	const walk = (dir) => {
		for (const entry of fs.readdirSync(dir, {withFileTypes: true})) {
			const file = path.join(dir, entry.name);
			if (entry.isDirectory()) {
				walk(file);
			} else if (entry.name.endsWith('.md') || entry.name.endsWith('.mdx')) {
				for (const [, anchor] of fs.readFileSync(file, 'utf8').matchAll(/properties(?:\.md)?#([A-Za-z0-9_.-]+)/g)) {
					anchors.add(anchor);
				}
			}
		}
	};
	walk(DOCS);
	return anchors;
}

test('generate writes the page from the committed snapshot', () => {
	const out = path.join(tempDir(), 'properties.md');
	const {report} = generate({out});
	const page = fs.readFileSync(out, 'utf8');
	assert.ok(page.startsWith('---\nid: properties\n'));
	assert.ok(page.includes(MARKER));
	assert.ok(report.total > 250);
	assert.ok(page.includes('<Anchor id="awe.application.name"/>'));
});

test('generate is deterministic', () => {
	const dir = tempDir();
	generate({out: path.join(dir, 'a.md')});
	generate({out: path.join(dir, 'b.md')});
	assert.equal(fs.readFileSync(path.join(dir, 'a.md'), 'utf8'), fs.readFileSync(path.join(dir, 'b.md'), 'utf8'));
});

test('every anchor that other pages link to exists in the generated page', () => {
	const out = path.join(tempDir(), 'properties.md');
	generate({out});
	const page = fs.readFileSync(out, 'utf8');
	const slug = (title) => title.toLowerCase().replace(/[^a-z0-9 -]/g, '').replace(/ /g, '-');
	const ids = new Set([...page.matchAll(/<Anchor id="([^"]+)"\/>/g)].map((m) => m[1]));
	for (const [, title, id] of page.matchAll(/^#{2,3} (.+?)(?: \{#([^}]+)\})?$/gm)) {
		ids.add(id || slug(title));
	}
	// The explicit heading ids are the ones other pages already link to
	for (const id of ['awe-rest-properties', 'awe-session-properties', 'awe-websocket-properties']) {
		assert.ok(ids.has(id), id);
	}
	const missing = [...otherPagesLinks()].filter((anchor) => !ids.has(anchor));
	assert.deepEqual(missing, []);
});

test('the generated page hides the internal hikari maps and has no leftover default sentence', () => {
	const out = path.join(tempDir(), 'properties.md');
	generate({out});
	const page = fs.readFileSync(out, 'utf8');
	assert.ok(!page.includes('spring.datasource.hikari.data-source-map'));
	assert.ok(!page.includes('spring.datasource.hikari.data-sources'));
	const rows = page.split('\n').filter((line) => line.startsWith('| <Anchor'));
	assert.ok(rows.length > 250);
	assert.deepEqual(rows.filter((row) => /default value/i.test(row)), []);
});

test('generate refuses to overwrite a hand-written page without the marker', () => {
	const out = path.join(tempDir(), 'properties.md');
	fs.writeFileSync(out, '# hand written\n');
	assert.throws(() => generate({out}), /not generated/);
	assert.equal(fs.readFileSync(out, 'utf8'), '# hand written\n');
});

test('generate overwrites a previously generated page', () => {
	const out = path.join(tempDir(), 'properties.md');
	generate({out});
	assert.doesNotThrow(() => generate({out}));
});

test('generate fails with a clear message when the snapshot is missing', () => {
	const out = path.join(tempDir(), 'properties.md');
	assert.throws(() => generate({out, snapshot: path.join(tempDir(), 'nope.json')}), /refresh/);
});

test('refreshSnapshot rewrites the snapshot from the module metadata', () => {
	const root = tempDir();
	for (const module of MODULES) {
		const dir = path.join(root, module, 'target', 'classes', 'META-INF');
		fs.mkdirSync(dir, {recursive: true});
		fs.writeFileSync(path.join(dir, 'spring-configuration-metadata.json'), JSON.stringify({properties: [
			{name: `awe.${path.basename(module)}.p`, type: 'java.lang.String', description: 'd'},
		]}));
	}
	const snapshot = path.join(tempDir(), 'snap.json');
	const {count} = refreshSnapshot({root, snapshot});
	assert.equal(count, MODULES.length);
	assert.equal(JSON.parse(fs.readFileSync(snapshot, 'utf8')).properties.length, MODULES.length);
});

test('refreshSnapshot names the modules that were not built', () => {
	const root = tempDir();
	assert.throws(() => refreshSnapshot({root, snapshot: path.join(tempDir(), 'snap.json')}), new RegExp(MODULES[0]));
});

test('the committed snapshot matches the metadata built from the Java sources (when the modules are built)', (t) => {
	const {files, missing} = metadataFiles(REPO);
	if (missing.length > 0) {
		t.skip(`modules not built here (${missing.length} of ${MODULES.length} without metadata)`);
		return;
	}
	const committed = JSON.parse(fs.readFileSync(DEFAULT_SNAPSHOT, 'utf8'));
	assert.deepEqual(
		committed,
		snapshotFromFiles(files),
		'website/scripts/properties-reference/data/spring-configuration-metadata.json is out of date: ' +
		'run "npm --prefix website run refresh:properties" after "mvn process-classes" and commit it',
	);
});

test('the hand-written content files exist', () => {
	for (const file of ['preface.md', 'appendix.md', 'groups.json', 'rest-examples.md']) {
		assert.ok(fs.existsSync(path.join(CONTENT_DIR, file)), file);
	}
});

test('ensureGenerated creates a missing page, leaves an up to date one and regenerates a stale generated one', () => {
	const out = path.join(tempDir(), 'properties.md');
	ensureGenerated({out});
	const fresh = fs.readFileSync(out, 'utf8');
	assert.ok(fresh.includes(MARKER));
	ensureGenerated({out});
	assert.equal(fs.readFileSync(out, 'utf8'), fresh);
	fs.writeFileSync(out, `${MARKER}\nstale\n`);
	ensureGenerated({out});
	assert.equal(fs.readFileSync(out, 'utf8'), fresh);
});

test('ensureGenerated refuses a page without the marker', () => {
	const out = path.join(tempDir(), 'properties.md');
	fs.writeFileSync(out, '# hand written\n');
	assert.throws(() => ensureGenerated({out}), /not generated/);
});

test('parseArgs reads the options and rejects unknown ones with the usage', () => {
	assert.equal(parseArgs(['--refresh']).refresh, true);
	assert.equal(parseArgs(['--out', 'x.md']).out, path.resolve('x.md'));
	assert.throws(() => parseArgs(['--nope']), /Usage/);
	assert.throws(() => parseArgs(['--out']), /Usage/);
});

test('ensureGenerated leaves the docs untouched while a maintenance line is being snapshotted', () => {
	const out = path.join(tempDir(), 'properties.md');
	fs.writeFileSync(out, '# hand written page of the maintenance line\n');
	ensureGenerated({out, env: {[SKIP_ENV]: '1'}});
	assert.equal(fs.readFileSync(out, 'utf8'), '# hand written page of the maintenance line\n');
});

test('ensureGenerated still generates when the skip variable has another value', () => {
	const out = path.join(tempDir(), 'properties.md');
	ensureGenerated({out, env: {[SKIP_ENV]: '0'}});
	assert.ok(fs.readFileSync(out, 'utf8').includes(MARKER));
});
