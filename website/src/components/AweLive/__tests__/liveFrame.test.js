const test = require('node:test');
const assert = require('node:assert/strict');
const {LIVE_FRAME_ELEMENT, liveFrameProps} = require('../liveFrame');

test('the frame is a form, so the radio groups of an example are its own', () => {
  assert.equal(LIVE_FRAME_ELEMENT, 'form');
});

test('a submit of the frame stays on the page', () => {
  const calls = [];
  liveFrameProps().onSubmit({preventDefault: () => calls.push('default'), stopPropagation: () => calls.push('propagation')});
  assert.deepEqual(calls, ['default', 'propagation']);
});

test('the frame keeps its classes and forced colour mode', () => {
  const {className, style} = liveFrameProps({height: '20rem', colorScheme: 'dark'});
  assert.equal(className, 'awe-live maximize-target awe-live--sized');
  assert.deepEqual(style, {colorScheme: 'dark', height: '20rem'});
  assert.equal(liveFrameProps().style, undefined);
});
