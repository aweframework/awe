const test = require('node:test');
const assert = require('node:assert/strict');
const {resolveValues, toScene, toXml} = require('../playgroundModel');
const {window: windowSpec} = require('../specs');

test('the window playground offers maximize: the XML writes it and the node of the stage carries it', () => {
  const on = resolveValues(windowSpec, {maximize: true});
  const off = resolveValues(windowSpec, {});

  assert.match(toXml(windowSpec, on), /maximize="true"/);
  assert.equal(toScene(windowSpec, on).nodes[0].maximize, true);
  assert.doesNotMatch(toXml(windowSpec, off), /maximize/);
  assert.equal('maximize' in toScene(windowSpec, off).nodes[0], false);
});

test('the window stage has a height, so the target a maximized window covers is not collapsed', () => {
  assert.ok(windowSpec.height);
});
