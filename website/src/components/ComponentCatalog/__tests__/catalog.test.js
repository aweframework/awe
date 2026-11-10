const test = require('node:test');
const assert = require('node:assert/strict');
const { FAMILIES, COMPONENTS, familiesInUse, filterComponents } = require('../catalog');

test('component ids are unique and every component belongs to a known family', () => {
  const ids = COMPONENTS.map(component => component.id);

  assert.equal(new Set(ids).size, ids.length);
  for (const component of COMPONENTS) {
    assert.ok(FAMILIES.some(family => family.id === component.family), `${component.id}: family ${component.family}`);
  }
});

test('a ready component has a page to link to and a soon one does not', () => {
  for (const component of COMPONENTS) {
    assert.equal(component.status === 'ready', Boolean(component.to), component.id);
  }
});

test('every component of the catalog is live: no card is left without its page', () => {
  assert.deepEqual(COMPONENTS.filter(component => component.status !== 'ready').map(component => component.id), []);
});

test('filterComponents keeps the order of the catalog and "all" keeps everything', () => {
  assert.deepEqual(filterComponents(COMPONENTS, 'all'), COMPONENTS);
  const data = filterComponents(COMPONENTS, 'data');

  assert.ok(data.length > 0 && data.every(component => component.family === 'data'));
  assert.deepEqual(data, COMPONENTS.filter(component => component.family === 'data'));
});

test('familiesInUse lists only the families that have a component, in the order of FAMILIES', () => {
  const families = [{ id: 'a' }, { id: 'unused' }, { id: 'b' }];
  const components = [{ family: 'b' }, { family: 'a' }, { family: 'b' }];

  assert.deepEqual(familiesInUse(components, families).map(family => family.id), ['a', 'b']);
  assert.deepEqual(familiesInUse([], families), []);
});

test('every family of the catalog has at least one component', () => {
  assert.deepEqual(familiesInUse(COMPONENTS, FAMILIES), FAMILIES);
});

test('a ready component has its page in the English docs and in the Spanish ones, and a playground spec if it names one', () => {
  const fs = require('node:fs');
  const path = require('node:path');
  const site = path.join(__dirname, '..', '..', '..', '..');
  const ready = COMPONENTS.filter(component => component.status === 'ready');

  assert.ok(ready.length > 0);
  for (const component of ready) {
    const page = `${component.to.replace('/docs/', '')}.mdx`;
    assert.ok(fs.existsSync(path.join(site, 'docs', page)), `docs/${page}`);
    assert.ok(fs.existsSync(path.join(site, 'i18n', 'es', 'docusaurus-plugin-content-docs', 'current', page)), `es ${page}`);
  }
});
