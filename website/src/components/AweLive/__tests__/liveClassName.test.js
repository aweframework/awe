const test = require('node:test');
const assert = require('node:assert/strict');
const {liveClassName} = require('../liveClassName');

test('the stage of an example is the maximize target, so a maximized window covers it and not the page', () => {
  assert.ok(liveClassName({}).split(' ').includes('maximize-target'));
  assert.ok(liveClassName({bare: true, height: '20rem'}).split(' ').includes('maximize-target'));
});

test('a bare example drops the frame and a sized one lays its children out in a column', () => {
  const classes = liveClassName({bare: true, height: '20rem'}).split(' ');

  assert.ok(classes.includes('awe-live--bare'));
  assert.ok(classes.includes('awe-live--sized'));
  assert.deepEqual(liveClassName({}).split(' '), ['awe-live', 'maximize-target']);
});
