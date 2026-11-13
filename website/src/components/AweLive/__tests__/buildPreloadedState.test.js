const test = require('node:test');
const assert = require('node:assert/strict');
const { buildPreloadedState, completeMessages } = require('../buildPreloadedState');

test('registers every described component under its id with address, attributes and actions', () => {
  const state = buildPreloadedState({
    save: { attributes: { label: 'Save', buttonType: 'submit' }, actions: [{ type: 'server', server: 'x' }] },
  });

  assert.deepEqual(state.components.save, {
    address: { view: 'live', component: 'save' },
    attributes: { visible: true, label: 'Save', buttonType: 'submit' },
    actions: [{ type: 'server', server: 'x' }],
  });
});

test('defaults attributes and actions, and makes the component visible', () => {
  const state = buildPreloadedState({ plain: {} });

  assert.equal(state.components.plain.attributes.visible, true);
  assert.deepEqual(state.components.plain.actions, []);
});

test('attributes of the description win over the defaults', () => {
  const state = buildPreloadedState({ hidden: { attributes: { visible: false } } });

  assert.equal(state.components.hidden.attributes.visible, false);
});

test('uses the given view name and keeps the client settings and idle actions slice', () => {
  const state = buildPreloadedState({ a: {} }, { view: 'demo', settings: { helpTimeout: 5 } });

  assert.equal(state.components.a.address.view, 'demo');
  assert.equal(state.settings.helpTimeout, 5);
  assert.deepEqual(state.actions, { running: false });
});

test('does not share the attribute objects of the description', () => {
  const description = { a: { attributes: { label: 'A' } } };
  const state = buildPreloadedState(description);
  state.components.a.attributes.label = 'changed';

  assert.equal(description.a.attributes.label, 'A');
});

test('a missing components description or settings is an empty one', () => {
  const state = buildPreloadedState(null, { settings: null });

  assert.deepEqual(state.components, {});
  assert.deepEqual(state.settings, {});
});

test('a component with a null description is registered with the defaults', () => {
  const state = buildPreloadedState({ nothing: null });

  assert.equal(state.components.nothing.attributes.visible, true);
});

test('malformed descriptions fail with a message that names the component', () => {
  assert.throws(() => buildPreloadedState({ bad: { attributes: 'label' } }), /attributes.*"bad"/);
  assert.throws(() => buildPreloadedState({ bad: { actions: {} } }), /actions.*"bad"/);
  assert.throws(() => buildPreloadedState({ bad: 'x' }), /"bad"/);
});

test('a described model and validation rules are registered, without sharing them', () => {
  const description = {
    country: {
      attributes: { label: 'Country' },
      model: { values: [{ value: 'es', label: 'Spain', selected: true }] },
      validationRules: { required: true },
    },
  };
  const state = buildPreloadedState(description);
  state.components.country.model.values[0].label = 'changed';

  assert.deepEqual(state.components.country.validationRules, { required: true });
  assert.equal(description.country.model.values[0].label, 'Spain');
  assert.equal(state.components.country.model.values[0].value, 'es');
});

test('model and validation rules are only registered when they are described', () => {
  const state = buildPreloadedState({ plain: { attributes: {} } });

  assert.equal('model' in state.components.plain, false);
  assert.equal('validationRules' in state.components.plain, false);
});

test('model and validation rules must be objects', () => {
  assert.throws(() => buildPreloadedState({ bad: { model: [] } }), /model.*"bad"/);
  assert.throws(() => buildPreloadedState({ bad: { validationRules: 'required' } }), /validationRules.*"bad"/);
});

test('registers the specific attributes (first row, rows per page, sort...) only when they are described', () => {
  const state = buildPreloadedState({
    grid: { specificAttributes: { sort: [{ id: 'name', direction: 'asc' }] } },
    plain: {},
  });

  assert.deepEqual(state.components.grid.specificAttributes, { sort: [{ id: 'name', direction: 'asc' }] });
  assert.equal('specificAttributes' in state.components.plain, false);
});

test('the specific attributes must be an object and are not shared with the description', () => {
  assert.throws(() => buildPreloadedState({ grid: { specificAttributes: [] } }), /specificAttributes of the component "grid"/);
  const description = { grid: { specificAttributes: { first: 0 } } };
  buildPreloadedState(description).components.grid.specificAttributes.first = 10;

  assert.equal(description.grid.specificAttributes.first, 0);
});

test('the predefined messages are registered under the view', () => {
  const state = buildPreloadedState({}, { view: 'demo', messages: { Remove: { title: 'Remove?', message: 'It cannot be undone.' } } });

  assert.deepEqual(state.messages.defined.demo, { Remove: { title: 'Remove?', message: 'It cannot be undone.' } });
});

test('the slice is completed with what the reducer of the client starts with, whatever that is', () => {
  const state = buildPreloadedState({}, { view: 'demo', messages: { Remove: { title: 'Remove?' } } });
  const initial = { defined: { base: { Hi: {} }, report: {} }, showing: [], extra: 1 };
  const completed = completeMessages(state.messages, initial);

  assert.deepEqual(completed, { defined: { base: { Hi: {} }, report: {}, demo: { Remove: { title: 'Remove?' } } }, showing: [], extra: 1 });
  assert.equal(completeMessages(undefined, initial), undefined);
  assert.deepEqual(initial.defined.base, { Hi: {} });
});

test('the messages must be an object', () => {
  assert.throws(() => buildPreloadedState({}, { messages: ['Remove'] }), TypeError);
});

test('without messages the slice is not preloaded: the reducer starts it', () => {
  assert.equal('messages' in buildPreloadedState({}), false);
});

test('the messages of the description are not shared with the state', () => {
  const messages = { Remove: { title: 'Remove?' } };
  const state = buildPreloadedState({}, { messages });
  state.messages.defined.live.Remove.title = 'changed';

  assert.equal(messages.Remove.title, 'Remove?');
});
