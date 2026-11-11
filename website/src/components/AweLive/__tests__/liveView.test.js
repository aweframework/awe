const test = require('node:test');
const assert = require('node:assert/strict');
const {liveView} = require('../liveView');

test('two examples on a page get different views, so a radio group of one never reaches the other', () => {
  assert.notEqual(liveView(undefined, 1), liveView(undefined, 2));
});

test('the view keeps the name the example gives it as its prefix', () => {
  assert.equal(liveView('orders', 3), 'orders-3');
  assert.equal(liveView(undefined, 3), 'live-3');
});
