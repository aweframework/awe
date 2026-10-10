'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {
	MODULES,
	cleanDescription,
	simplifyType,
	formatDefault,
	snapshotFromFiles,
	normalize,
	metadataFiles,
} = require('../metadata');

function tempDir() {
	return fs.mkdtempSync(path.join(os.tmpdir(), 'props-ref-'));
}

test('cleanDescription strips the hand-written "Default value" sentence and keeps it as legacy default', () => {
	assert.deepEqual(cleanDescription('Base encoding for all files. Default value UTF-8'), {
		text: 'Base encoding for all files.',
		legacyDefault: 'UTF-8',
	});
	assert.deepEqual(cleanDescription('Xml file name of actions definition. Default value Actions.'), {
		text: 'Xml file name of actions definition.',
		legacyDefault: 'Actions',
	});
	assert.deepEqual(cleanDescription('Port. The Default value is 25.'), {text: 'Port.', legacyDefault: '25'});
	assert.deepEqual(cleanDescription('Timeout. See {@link Duration} The Default value is 60m'), {
		text: 'Timeout. See Duration',
		legacyDefault: '60m',
	});
});

test('cleanDescription keeps a "Default value" phrase that is part of the description', () => {
	const mid = 'The Default value of the pool is shown in the log. Change it with care.';
	assert.deepEqual(cleanDescription(mid), {text: mid, legacyDefault: undefined});
	const both = 'Sets the Default value for new rows. Used by imports. Default value 5';
	assert.deepEqual(cleanDescription(both), {text: 'Sets the Default value for new rows. Used by imports.', legacyDefault: '5'});
});

test('cleanDescription never empties a description that is only the default sentence', () => {
	assert.deepEqual(cleanDescription('Default value x'), {text: 'Default value x', legacyDefault: undefined});
});

test('cleanDescription leaves a description without a default sentence untouched', () => {
	assert.deepEqual(cleanDescription('Plain text.'), {text: 'Plain text.', legacyDefault: undefined});
	assert.equal(cleanDescription(undefined).text, '');
});

test('cleanDescription turns javadoc inline tags and html into markdown', () => {
	assert.equal(cleanDescription('Use {@code false} to disable it.').text, 'Use `false` to disable it.');
	assert.equal(cleanDescription('See {@link com.almis.Foo#bar} now').text, 'See com.almis.Foo#bar now');
	assert.equal(cleanDescription('Go to <a href="https://x.io/a">the site</a>.').text, 'Go to [the site](https://x.io/a).');
	assert.equal(
		cleanDescription('Mode. <ul> <li>{@code strict} (default): throws.</li> <li>{@code warn}: logs.</li> </ul> Default value strict').text,
		'Mode.<br/>- `strict` (default): throws.<br/>- `warn`: logs.',
	);
	assert.equal(cleanDescription('Two\n  lines   here').text, 'Two lines here');
});

test('simplifyType drops packages and keeps generics', () => {
	assert.equal(simplifyType('java.lang.String'), 'String');
	assert.equal(simplifyType('java.util.List<java.lang.String>'), 'List<String>');
	assert.equal(simplifyType('java.util.Map<java.lang.String,java.lang.Integer>'), 'Map<String,Integer>');
	assert.equal(simplifyType('java.time.Duration'), 'Duration');
	assert.equal(simplifyType('com.almis.awe.config.Foo$Bar'), 'Bar');
	assert.equal(simplifyType(undefined), '');
});

test('formatDefault renders scalars, lists and the empty value', () => {
	assert.equal(formatDefault('abc'), 'abc');
	assert.equal(formatDefault(25), '25');
	assert.equal(formatDefault(false), 'false');
	assert.equal(formatDefault(['en', 'es', 'fr']), 'en, es, fr');
	assert.equal(formatDefault(''), '');
	assert.equal(formatDefault(undefined), '');
	assert.equal(formatDefault(null), '');
	assert.equal(formatDefault([]), '');
});

test('snapshotFromFiles merges the metadata files, sorted by name and without build-only fields', () => {
	const dir = tempDir();
	const a = path.join(dir, 'a.json');
	const b = path.join(dir, 'b.json');
	fs.writeFileSync(a, JSON.stringify({groups: [{name: 'x'}], properties: [
		{name: 'awe.b', type: 'java.lang.String', description: 'B', sourceType: 'X', defaultValue: 'b'},
	]}));
	fs.writeFileSync(b, JSON.stringify({properties: [
		{name: 'awe.a', type: 'java.lang.Integer', description: 'A', sourceType: 'Y'},
	]}));
	const snapshot = snapshotFromFiles([b, a]);
	assert.deepEqual(snapshot, {properties: [
		{name: 'awe.a', type: 'java.lang.Integer', description: 'A'},
		{name: 'awe.b', type: 'java.lang.String', description: 'B', defaultValue: 'b'},
	]});
});

test('snapshotFromFiles is deterministic whatever the file order', () => {
	const dir = tempDir();
	const a = path.join(dir, 'a.json');
	const b = path.join(dir, 'b.json');
	fs.writeFileSync(a, JSON.stringify({properties: [{name: 'awe.z', type: 'java.lang.String'}]}));
	fs.writeFileSync(b, JSON.stringify({properties: [{name: 'awe.y', type: 'java.lang.String'}]}));
	assert.equal(JSON.stringify(snapshotFromFiles([a, b])), JSON.stringify(snapshotFromFiles([b, a])));
});

test('snapshotFromFiles rejects a property defined in two modules', () => {
	const dir = tempDir();
	const a = path.join(dir, 'a.json');
	const b = path.join(dir, 'b.json');
	fs.writeFileSync(a, JSON.stringify({properties: [{name: 'awe.same', type: 'java.lang.String'}]}));
	fs.writeFileSync(b, JSON.stringify({properties: [{name: 'awe.same', type: 'java.lang.String'}]}));
	assert.throws(() => snapshotFromFiles([a, b]), /awe\.same/);
});

test('normalize cleans descriptions, falls back to the legacy default and hides internal properties', () => {
	const entries = normalize({properties: [
		{name: 'awe.b', type: 'java.lang.String', description: 'Has default. Default value x'},
		{name: 'awe.a', type: 'java.lang.Integer', description: 'Described.', defaultValue: 5},
		{name: 'awe.c', type: 'java.lang.Boolean', description: ''},
		{name: 'spring.datasource.hikari.data-sources', type: 'java.util.Map', description: 'Internal.'},
	]}, {hidden: ['spring.datasource.hikari.data-sources']});
	assert.deepEqual(entries.map((e) => e.name), ['awe.a', 'awe.b', 'awe.c']);
	assert.deepEqual(entries[0], {name: 'awe.a', type: 'Integer', description: 'Described.', defaultValue: '5'});
	assert.equal(entries[1].defaultValue, 'x');
	assert.equal(entries[1].description, 'Has default.');
	assert.equal(entries[2].description, '');
	assert.equal(entries[0].deprecated, undefined);
	assert.equal(entries[2].defaultValue, '');
});

test('normalize keeps the deprecation with its replacement and reason', () => {
	const [entry] = normalize({properties: [
		{name: 'awe.old', type: 'java.lang.String', description: 'Old.', deprecation: {replacement: 'awe.new', reason: 'Renamed.'}},
	]});
	assert.deepEqual(entry.deprecated, {replacement: 'awe.new', reason: 'Renamed.'});
});

test('normalize prefers the metadata default over the legacy sentence', () => {
	const [entry] = normalize({properties: [
		{name: 'awe.a', type: 'java.lang.String', description: 'Text. Default value old', defaultValue: 'new'},
	]});
	assert.equal(entry.defaultValue, 'new');
});

test('metadataFiles finds the target/classes metadata of the 8 modules', () => {
	assert.equal(MODULES.length, 8);
	const root = tempDir();
	for (const module of MODULES) {
		const dir = path.join(root, module, 'target', 'classes', 'META-INF');
		fs.mkdirSync(dir, {recursive: true});
		fs.writeFileSync(path.join(dir, 'spring-configuration-metadata.json'), '{}');
	}
	const {files, missing} = metadataFiles(root);
	assert.equal(files.length, 8);
	assert.deepEqual(missing, []);
	fs.rmSync(path.join(root, MODULES[0], 'target'), {recursive: true});
	assert.deepEqual(metadataFiles(root).missing, [MODULES[0]]);
});
