import { combineReducers } from '@reduxjs/toolkit';
import { components } from '../../../src/redux/reducers/componentsReducer';
import ComponentRegistry from '../../../src/redux/registry/ComponentRegistry';
import { mergeComponentState } from '../../../src/utilities/mergeUtils';
import { updateModel } from '../../../src/redux/actions/components';

// The jsdom environment has no structuredClone, which the website helper uses
global.structuredClone ??= value => JSON.parse(JSON.stringify(value));

const { buildPreloadedState } = require('../../../../../website/src/components/AweLive/buildPreloadedState');
const { registerLiveComponents } = require('../../../../../website/src/components/AweLive/registerComponents');

const radio = (id, selected) => ({
  attributes: { component: 'radio', group: 'plan', label: id },
  model: { values: [{ value: id, selected }] },
});

const description = () => ({ plan1: radio('plan1', true), plan2: radio('plan2', false), plan3: radio('plan3', false) });

// The catalog (website AweLive) builds its store the way a screen load leaves it: components in the state, bases in the registry
describe('website AweLive radio group', () => {
  let release;
  beforeEach(() => {
    ComponentRegistry.clearAll();
    release = registerLiveComponents(ComponentRegistry, buildPreloadedState(description()).components);
  });
  afterEach(() => release());

  it('keeps the address of every radio and selects only the clicked one', () => {
    const { components: state } = buildPreloadedState(description());
    const next = combineReducers({ components })({ components: state }, updateModel(
      { view: 'live', component: 'plan2' }, { selected: ['plan2'] }
    )).components;

    // What the client selectors show: the registered base plus the delta of the state
    const merged = Object.fromEntries(Object.keys(next).map(id => [id, mergeComponentState(ComponentRegistry.get(id) || {}, next[id])]));
    expect(Object.values(merged).map(c => c.address?.component)).toEqual(['plan1', 'plan2', 'plan3']);
    expect(Object.values(merged).map(c => c.model.values[0].selected)).toEqual([false, true, false]);
  });
});
