'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const {loadSchemas, buildReference} = require('../xsd-model');

const FIXTURES = path.join(__dirname, '..', 'fixtures');

function fixtureReference() {
	return buildReference(loadSchemas(FIXTURES, ['main.xsd', 'base.xsd']));
}

function schema(reference, file) {
	return reference.schemas.find((s) => s.file === file);
}

function element(reference, file, name) {
	return schema(reference, file).elements.find((e) => e.name === name);
}

function attribute(el, name) {
	return el.attributes.find((a) => a.name === name);
}

test('resolves an absolute schemaLocation include to the local file', () => {
	const schemas = loadSchemas(FIXTURES, ['main.xsd']);
	assert.deepEqual(schemas.map((s) => s.file).sort(), ['base.xsd', 'main.xsd']);
	assert.deepEqual(schemas.find((s) => s.file === 'main.xsd').includes, ['base.xsd']);
});

test('fails with a clear message when an include cannot be resolved locally', () => {
	assert.throws(() => loadSchemas(path.join(FIXTURES, 'broken'), ['missing-include.xsd']), /does-not-exist\.xsd/);
});

test('reads the schema title and documentation', () => {
	const main = schema(fixtureReference(), 'main.xsd');
	assert.equal(main.title, 'Fixture Main Schema');
	assert.equal(main.documentation, 'Describes a {box} with <items> and a | pipe.');
});

test('expands attribute groups declared in an included schema', () => {
	const box = element(fixtureReference(), 'main.xsd', 'box');
	const id = attribute(box, 'id');
	assert.ok(id, 'id comes from commonAttr');
	assert.equal(id.required, true);
	assert.equal(attribute(box, 'visible').default, 'true');
	assert.equal(attribute(box, 'visible').required, false);
});

test('collects the allowed values of a named enumeration, inline or from another schema', () => {
	const box = element(fixtureReference(), 'main.xsd', 'box');
	assert.deepEqual(attribute(box, 'visible').type.values, ['true', 'false']);
	assert.equal(attribute(box, 'visible').type.name, 'booleanEnum');
	assert.deepEqual(attribute(box, 'mode').type.values, ['fast', 'slow']);
	assert.equal(attribute(box, 'mode').type.name, null);
});

test('reports patterns and the member types of a union', () => {
	const reference = fixtureReference();
	const box = element(reference, 'main.xsd', 'box');
	assert.deepEqual(attribute(box, 'id').type.patterns, ['([0-9a-zA-Z_\\-]+)']);
	const item = element(reference, 'main.xsd', 'item');
	assert.deepEqual(attribute(item, 'mixed').type.values, ['true', 'false']);
	assert.deepEqual(attribute(item, 'mixed').type.patterns, ['([0-9a-zA-Z_\\-]+)']);
});

test('keeps built-in types as such', () => {
	const item = element(fixtureReference(), 'main.xsd', 'item');
	assert.equal(attribute(item, 'count').type.builtin, 'int');
	assert.equal(attribute(item, 'count').default, '1');
});

test('collects xs:documentation on attributes and elements, collapsing whitespace', () => {
	const reference = fixtureReference();
	assert.equal(attribute(element(reference, 'main.xsd', 'box'), 'id').documentation, 'Unique identifier of the element.');
	assert.equal(element(reference, 'main.xsd', 'box').documentation, 'The root box.');
	assert.equal(attribute(element(reference, 'main.xsd', 'item'), 'count').documentation, 'How many.');
	assert.equal(attribute(element(reference, 'main.xsd', 'item'), 'visible').documentation, null);
});

test('falls back to the documentation of the element type', () => {
	assert.equal(element(fixtureReference(), 'base.xsd', 'note').documentation, 'A note with free text.');
});

test('flattens children through groups, choices and sequences and links them to the declaring schema', () => {
	const box = element(fixtureReference(), 'main.xsd', 'box');
	assert.deepEqual(
		box.children.map((c) => [c.name, c.file, c.min, c.max]),
		[
			['note', 'base.xsd', 0, 1],
			['item', 'main.xsd', 0, 'unbounded'],
			['special-item', 'main.xsd', 0, 'unbounded'],
		],
	);
});

test('inherits attributes through complexContent extension and adds its own', () => {
	const special = element(fixtureReference(), 'main.xsd', 'special-item');
	assert.deepEqual(special.attributes.map((a) => a.name).sort(), ['count', 'id', 'level', 'mixed', 'visible']);
	assert.equal(attribute(special, 'level').required, true);
});

test('keeps simpleContent text and extension attributes', () => {
	const note = element(fixtureReference(), 'base.xsd', 'note');
	assert.equal(note.textContent, 'string');
	assert.ok(attribute(note, 'id'));
});

test('computes which elements contain each element', () => {
	const reference = fixtureReference();
	assert.deepEqual(
		element(reference, 'main.xsd', 'item').parents.map((p) => [p.name, p.file]),
		[['box', 'main.xsd']],
	);
	assert.deepEqual(element(reference, 'main.xsd', 'box').parents, []);
	assert.deepEqual(
		element(reference, 'base.xsd', 'note').parents.map((p) => p.name),
		['box'],
	);
});

test('lists the simple types declared by each schema', () => {
	const base = schema(fixtureReference(), 'base.xsd');
	const names = base.simpleTypes.map((t) => t.name);
	assert.deepEqual(names, ['booleanEnum', 'sizeEnum', 'identifierType', 'flexibleType']);
	assert.deepEqual(base.simpleTypes.find((t) => t.name === 'booleanEnum').values, ['true', 'false']);
});

test('counts declared and documented attributes per schema', () => {
	const reference = fixtureReference();
	// base: id (documented), visible ; main: size, mode, count (documented), mixed, level
	assert.deepEqual(schema(reference, 'base.xsd').stats, {attributes: 2, documented: 1});
	assert.deepEqual(schema(reference, 'main.xsd').stats, {attributes: 5, documented: 1});
});

function extrasReference() {
	return buildReference(loadSchemas(FIXTURES, ['extras.xsd']));
}

test('resolves xs:attribute ref to the global attribute declaration, keeping the use of the reference', () => {
	const panel = element(extrasReference(), 'extras.xsd', 'panel');
	const lang = attribute(panel, 'lang');
	assert.ok(lang, 'lang comes from the global attribute');
	assert.equal(lang.required, true);
	assert.equal(lang.type.builtin, 'string');
	assert.equal(lang.documentation, 'Language code.');
});

test('turns a local element declaration into a reference entry with its own attributes', () => {
	const reference = extrasReference();
	assert.deepEqual(schema(reference, 'extras.xsd').elements.map((e) => e.name), ['panel', 'cell']);
	const cell = element(reference, 'extras.xsd', 'cell');
	assert.deepEqual(cell.attributes.map((a) => a.name), ['span']);
	assert.deepEqual(cell.parents.map((p) => p.name), ['panel']);
	const [child] = element(reference, 'extras.xsd', 'panel').children;
	assert.equal(child.anchor, cell.anchor);
	assert.notEqual(cell.anchor, 'element-cell');
});

test('counts only particles as the alternatives of an xs:choice, not its annotation', () => {
	const [cell] = element(extrasReference(), 'extras.xsd', 'panel').children;
	assert.deepEqual([cell.min, cell.max], [1, 'unbounded']);
});

test('counts an attribute reference once: only the global declaration is a declared attribute', () => {
	// lang (documented, global) and span
	assert.deepEqual(schema(extrasReference(), 'extras.xsd').stats, {attributes: 2, documented: 1});
});
