const test = require('node:test');
const assert = require('node:assert/strict');
const { stableKey } = require('../stableKey');

test('the key does not depend on the order of the keys, at any depth', () => {
  assert.equal(
    stableKey({a: 1, b: {c: [1, {y: 2, x: 1}], d: 'z'}}),
    stableKey({b: {d: 'z', c: [1, {x: 1, y: 2}]}, a: 1}),
  );
});

test('a new object with the same content has the same key, a changed value another one', () => {
  assert.equal(stableKey({a: {label: 'A'}}), stableKey({a: {label: 'A'}}));
  assert.notEqual(stableKey({a: {label: 'A'}}), stableKey({a: {label: 'B'}}));
});

test('functions and undefined values are ignored, as in JSON', () => {
  assert.equal(stableKey({a: 1, f: () => 1, u: undefined}), stableKey({a: 1}));
});

test('array order matters', () => {
  assert.notEqual(stableKey([1, 2]), stableKey([2, 1]));
});
