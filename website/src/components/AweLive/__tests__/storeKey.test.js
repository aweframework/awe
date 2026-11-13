const test = require('node:test');
const assert = require('node:assert/strict');
const { storeKey } = require('../storeKey');

const base = { components: { a: { attributes: { label: 'A' } } }, nodes: [{ elementType: 'Card', id: 'a' }], view: 'live' };

test('the key is the same for a new object with the same content', () => {
  assert.equal(storeKey(base), storeKey(structuredClone(base)));
});

test('a change of components, nodes, view, settings, files, messages or answers gives another key', () => {
  const keys = [
    storeKey(base),
    storeKey({ ...base, components: { a: { attributes: { label: 'B' } } } }),
    storeKey({ ...base, nodes: [{ elementType: 'Card', id: 'b' }] }),
    storeKey({ ...base, view: 'other' }),
    storeKey({ ...base, settings: { helpTimeout: 1 } }),
    storeKey({ ...base, files: { Log: '/log.txt' } }),
    storeKey({ ...base, messages: { Done: { title: 'Done' } } }),
    storeKey({ ...base, answers: { Save: [{ type: 'message' }] } }),
  ];

  assert.equal(new Set(keys).size, keys.length);
});

test('the colour scheme and the frame do not rebuild the store: they are not part of its state', () => {
  assert.equal(storeKey(base), storeKey({ ...base, colorScheme: 'dark', bare: true }));
});
