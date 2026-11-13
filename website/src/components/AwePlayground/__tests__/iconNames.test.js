const test = require('node:test');
const assert = require('node:assert/strict');
const {spriteId} = require('../iconNames');
const specs = require('../specs');

test('an alias of Lucide resolves to the current id of the sprite', () => {
  assert.equal(spriteId('lucide:filter'), 'funnel');
  assert.equal(spriteId('lucide:trash-2'), 'trash');
  assert.equal(spriteId('lucide:user'), 'user');
  assert.equal(spriteId('lucide:not-an-icon'), null);
});

test('every Lucide icon a playground offers resolves to an icon of the sprite', () => {
  for (const spec of Object.values(specs)) {
    for (const control of (spec.controls ?? []).filter(item => item.type === 'icon')) {
      for (const option of control.options.filter(Boolean)) {
        assert.ok(spriteId(option), `${spec.name}: ${option}`);
      }
    }
  }
});
