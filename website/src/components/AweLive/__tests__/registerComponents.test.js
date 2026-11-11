const test = require('node:test');
const assert = require('node:assert/strict');
const { registerLiveComponents, liveRegistration } = require('../registerComponents');

const fakeRegistry = () => {
  const bases = {};
  return { bases, register: (id, base) => { bases[id] = base; }, unregister: id => { delete bases[id]; } };
};

test('registers every component as a base and removes only them on release', () => {
  const registry = fakeRegistry();
  registry.register('other', { label: 'x' });
  const release = registerLiveComponents(registry, { a: { label: 'A' }, b: { label: 'B' } });

  assert.deepEqual(Object.keys(registry.bases).sort(), ['a', 'b', 'other']);
  release();
  assert.deepEqual(Object.keys(registry.bases), ['other']);
});

test('gives back the base of the example that was covered by another with the same id', () => {
  const registry = fakeRegistry();
  const releaseFirst = registerLiveComponents(registry, { a: { label: 'first' } });
  const releaseSecond = registerLiveComponents(registry, { a: { label: 'second' } });

  assert.equal(registry.bases.a.label, 'second');
  releaseSecond();
  assert.equal(registry.bases.a.label, 'first');
  releaseFirst();
  assert.equal('a' in registry.bases, false);
});

test('a covered example that unmounts first leaves the one on top in place', () => {
  const registry = fakeRegistry();
  const releaseFirst = registerLiveComponents(registry, { a: { label: 'first' } });
  const releaseSecond = registerLiveComponents(registry, { a: { label: 'second' } });

  releaseFirst();
  assert.equal(registry.bases.a.label, 'second');
  releaseSecond();
  assert.equal('a' in registry.bases, false);
});

test('release is idempotent', () => {
  const registry = fakeRegistry();
  const releaseFirst = registerLiveComponents(registry, { a: { label: 'first' } });
  const releaseSecond = registerLiveComponents(registry, { a: { label: 'second' } });

  releaseSecond();
  releaseSecond();
  assert.equal(registry.bases.a.label, 'first');
  releaseFirst();
});

test('a new state of an example replaces its base at once, before the example renders again', () => {
  const registry = fakeRegistry();
  const registration = liveRegistration(registry);
  registration.use('with-icon', { demo: { attributes: { icon: 'user' } } });
  registration.use('no-icon', { demo: { attributes: {} } });

  assert.deepEqual(registry.bases.demo, { attributes: {} });
  registration.release();
  assert.equal('demo' in registry.bases, false);
});

test('using the same state again registers nothing new, and a released registration registers again', () => {
  const registry = fakeRegistry();
  const registration = liveRegistration(registry);
  registration.use('state', { demo: { label: 'A' } });
  registry.bases.demo = 'touched';
  registration.use('state', { demo: { label: 'A' } });
  assert.equal(registry.bases.demo, 'touched');

  registration.release();
  registration.use('state', { demo: { label: 'A' } });
  assert.deepEqual(registry.bases.demo, { label: 'A' });
});

test('an example covered by another with the same id takes the registry back when it raises its base', () => {
  const registry = fakeRegistry();
  const first = liveRegistration(registry);
  const second = liveRegistration(registry);
  first.use('k1', { demo: { label: 'side panel' } });
  second.use('k2', { demo: { label: 'popover' } });

  first.raise();
  assert.equal(registry.bases.demo.label, 'side panel');
  // The covered one unmounting leaves the raised base in place, and the raised one gives the other back
  second.release();
  assert.equal(registry.bases.demo.label, 'side panel');
  second.use('k3', { demo: { label: 'popover' } });
  first.release();
  assert.equal(registry.bases.demo.label, 'popover');
});
