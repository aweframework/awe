import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {mergeComponentState} from '../../../../../src/utilities/mergeUtils';
import {clearComponents, lifecycleHandlers} from '../../../../../src/redux/reducers/components/lifecycle';
import {
  CLEAR_ALL_COMPONENTS,
  CLEAR_COMPONENTS,
  REGISTER_COMPONENTS,
  UNREGISTER_COMPONENTS,
  UPDATE_COMPONENT,
  UPDATE_MULTIPLE_COMPONENTS,
  UPDATE_VIEW_COMPONENTS
} from '../../../../../src/redux/actions/components';

const component = (view, id, extra = {}) => ({address: {view, component: id}, attributes: {}, model: {values: []}, ...extra});

describe('lifecycle handlers', () => {
  beforeEach(() => ComponentRegistry.clearAll());

  it('handles exactly the lifecycle actions', () => {
    expect(Object.keys(lifecycleHandlers).sort()).toEqual([
      CLEAR_ALL_COMPONENTS, CLEAR_COMPONENTS, REGISTER_COMPONENTS, UNREGISTER_COMPONENTS, UPDATE_COMPONENT,
      UPDATE_MULTIPLE_COMPONENTS, UPDATE_VIEW_COMPONENTS
    ].sort());
  });

  it('clears the components of a view only', () => {
    const state = {a: component('v1', 'a'), b: component('v2', 'b')};

    const result = lifecycleHandlers[CLEAR_COMPONENTS](state, {view: 'v1'});

    expect(Object.keys(result)).toEqual(['b']);
    expect(Object.keys(clearComponents(state, 'v2'))).toEqual(['a']);
  });

  it('clears everything and the registry', () => {
    ComponentRegistry.register('a', component('v1', 'a'));

    const result = lifecycleHandlers[CLEAR_ALL_COMPONENTS]({a: component('v1', 'a')}, {});

    expect(result).toEqual({});
    expect(ComponentRegistry.get('a')).toBeFalsy();
  });

  it('registers the bases and stores the deltas when updating a view', () => {
    const data = {a: component('v1', 'a', {model: {values: [{id: 1}]}})};

    const result = lifecycleHandlers[UPDATE_VIEW_COMPONENTS]({old: component('v1', 'old')}, {view: 'v1', data});

    expect(ComponentRegistry.get('a')).toBeDefined();
    expect(result.old).toBeUndefined();
    expect(Object.keys(result)).toEqual(['a']);
  });

  it('keeps the other views and replaces everything for the base view', () => {
    const state = {other: component('v2', 'other'), old: component('base', 'old')};

    const kept = lifecycleHandlers[UPDATE_VIEW_COMPONENTS](state, {view: 'v1', data: {}});
    const base = lifecycleHandlers[UPDATE_VIEW_COMPONENTS](state, {view: 'base', data: {}});

    expect(Object.keys(kept).sort()).toEqual(['old', 'other']);
    expect(base).toEqual({});
  });

  it('updates a component and a list of components', () => {
    const address = {view: 'base', component: 'c'};
    ComponentRegistry.register('c', component('base', 'c'));

    const one = lifecycleHandlers[UPDATE_COMPONENT]({}, {address, data: {attributes: {title: 'x'}}, settings: {}});
    const many = lifecycleHandlers[UPDATE_MULTIPLE_COMPONENTS]({}, {
      componentList: [{address, attributes: {title: 'y'}}], settings: {}
    });

    expect(mergeComponentState(ComponentRegistry.get('c'), one.c).attributes.title).toBe('x');
    expect(mergeComponentState(ComponentRegistry.get('c'), many.c).attributes.title).toBe('y');
  });
});
