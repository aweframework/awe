import {components} from '../../../../src/redux/reducers/componentsReducer';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';
import {
  CLEAR_COMPONENTS,
  REGISTER_COMPONENTS,
  RESET_MODEL,
  UNREGISTER_COMPONENTS,
  UPDATE_ATTRIBUTES,
  UPDATE_COMPONENT,
  UPDATE_MODEL,
  UPDATE_VALIDATION,
  UPDATE_VIEW_COMPONENTS
} from '../../../../src/redux/actions/components';

describe('awe-react-client/test/js/redux/reducers/componentsReducerRegistryTest.jsx', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
  });

  it('registers base components and stores deltas when using UPDATE_VIEW_COMPONENTS with registry enabled', () => {
    const baseComponent = {
      address: { component: 'comp', view: 'base' },
      attributes: { foo: 'bar' }
    };

    const next = components({}, {
      type: UPDATE_VIEW_COMPONENTS,
      view: 'base',
      data: { comp: baseComponent }
    });

    expect(ComponentRegistry.get('comp')).toEqual(baseComponent);
    expect(next.comp).toEqual({});
  });

  it('adds components to a loaded view keeping the ones it has, and removes them with all their state', () => {
    const view = { address: { component: 'comp', view: 'report' }, attributes: { foo: 'bar' } };
    const added = { address: { component: 'added', view: 'report' }, attributes: { foo: 'baz' } };
    const loaded = components({}, { type: UPDATE_VIEW_COMPONENTS, view: 'report', data: { comp: view } });

    const registered = components(loaded, { type: REGISTER_COMPONENTS, data: { added } });

    expect(Object.keys(registered)).toEqual(['comp', 'added']);
    expect(ComponentRegistry.get('comp')).toEqual(view);
    expect(ComponentRegistry.get('added')).toEqual(added);

    const updated = components(registered, { type: UPDATE_ATTRIBUTES, address: added.address, data: { foo: 'new' } });
    const unregistered = components(updated, { type: UNREGISTER_COMPONENTS, ids: ['added'] });

    expect(Object.keys(unregistered)).toEqual(['comp']);
    expect(ComponentRegistry.get('added')).toBeNull();
    expect(ComponentRegistry.getStats().componentsByView).toEqual({ report: 1 });
    expect(ComponentRegistry.get('comp')).toEqual(view);
  });

  it('does not bring back a component that is neither registered nor in the state when its model arrives late', () => {
    const state = { other: {} };

    const next = components(state, {
      type: UPDATE_MODEL,
      address: { view: 'report', component: 'gone' },
      data: { values: [{ id: 1 }], records: 1 }
    });

    expect(next).toBe(state);
    expect(next).not.toHaveProperty('gone');
  });

  it('stores only attribute deltas when updating a component with registry enabled', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { foo: 'bar' }
    });

    const next = components({}, {
      type: UPDATE_COMPONENT,
      address: { component: 'comp', view: 'base' },
      data: { attributes: { foo: 'bar', extra: true } }
    });

    expect(next.comp.attributes).toEqual({ extra: true });
  });

  it('updates component attributes via UPDATE_ATTRIBUTES with registry enabled', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { foo: 'bar' }
    });

    const next = components({}, {
      type: UPDATE_ATTRIBUTES,
      address: { component: 'comp', view: 'base' },
      data: { foo: 'baz' }
    });

    expect(next.comp.attributes).toEqual({ foo: 'baz' });
  });

  it('replaces array values when updating component model with registry enabled', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { foo: 'bar' },
      model: { values: [{ value: 'a', selected: true }] }
    });

    const next = components({}, {
      type: UPDATE_COMPONENT,
      address: { component: 'comp', view: 'base' },
      data: { model: { values: [] } }
    });

    expect(next.comp.model.values).toEqual([]);
  });

  it('updates column attributes and stores full columnModel in delta with registry enabled', () => {
    ComponentRegistry.register('grid', {
      address: { component: 'grid', view: 'base' },
      attributes: {
        id: 'id',
        columnModel: [{ name: 'colA', width: 100 }]
      },
      model: { values: [{ id: 1, colA: 'A' }] }
    });

    const next = components({}, {
      type: UPDATE_ATTRIBUTES,
      address: { component: 'grid', view: 'base', column: 'colA' },
      data: { width: 250 }
    });

    expect(next.grid.attributes.columnModel).toEqual([{ name: 'colA', width: 250 }]);
  });

  it('merges column validation rules with registry enabled', () => {
    ComponentRegistry.register('grid', {
      address: { component: 'grid', view: 'base' },
      attributes: {
        id: 'id',
        columnModel: [{ name: 'colA', validationRules: { required: false } }]
      },
      model: { values: [{ id: 1, colA: 'A' }] }
    });

    const next = components({}, {
      type: UPDATE_VALIDATION,
      address: { component: 'grid', view: 'base', column: 'colA' },
      data: { pattern: '^[0-9]$' }
    });

    expect(next.grid.attributes.columnModel[0].validationRules).toEqual({ required: false, pattern: '^[0-9]$' });
  });

  it('updates cell attributes through registry path when row and column are provided', () => {
    ComponentRegistry.register('grid', {
      address: { component: 'grid', view: 'base' },
      attributes: {
        id: 'id',
        columnModel: [{ name: 'colA' }]
      },
      model: {
        values: [
          { id: 1, colA: 'A', $attrs: { colA: { placeholder: 'old' } } }
        ]
      }
    });

    const next = components({}, {
      type: UPDATE_ATTRIBUTES,
      address: { component: 'grid', view: 'base', row: 1, column: 'colA' },
      data: { placeholder: 'new' }
    });

    expect(next.grid.model.values[0].$attrs.colA.placeholder).toBe('new');
  });

  it('clears registry entries when clearing components with registry enabled', () => {
    ComponentRegistry.register('a', { address: { component: 'a', view: 'base' } });
    ComponentRegistry.register('b', { address: { component: 'b', view: 'base' } });
    ComponentRegistry.register('c', { address: { component: 'c', view: 'other' } });

    const state = { a: { attributes: { x: 1 } }, b: { attributes: { y: 2 } }, c: { attributes: { z: 3 } } };
    const next = components(state, { type: CLEAR_COMPONENTS, view: 'base' });

    expect(next).toEqual({ c: { attributes: { z: 3 } } });
    expect(ComponentRegistry.get('a')).toBeNull();
    expect(ComponentRegistry.get('b')).toBeNull();
    expect(ComponentRegistry.get('c')).not.toBeNull();
  });

  it('resets non-grid model values by clearing selection instead of removing values when registry enabled', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { label: 'Test' },
      model: {
        values: [
          { value: 'a', selected: true },
          { value: 'b', selected: false }
        ]
      }
    });

    const next = components({}, {
      type: RESET_MODEL,
      address: { component: 'comp', view: 'base' }
    });

    expect(next.comp.model.values).toEqual([
      { value: 'a', selected: false },
      { value: 'b', selected: false }
    ]);
  });

  it('warns and ignores malformed component updates without address', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    const state = {
      comp: {
        address: { component: 'comp', view: 'base' },
        attributes: { foo: 'bar' }
      }
    };

    const next = components(state, {
      type: UPDATE_COMPONENT,
      data: {
        uid: 'broken-uid',
        attributes: { id: 'broken' },
        context: { view: 'broken-view' }
      }
    });

    expect(next).toEqual(state);
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('[AWE] Malformed component detected in reducer:updateComponentData'),
      expect.objectContaining({
        componentUid: 'broken-uid',
        attributesId: 'broken',
        view: 'broken-view'
      })
    );
  });

  it('warns when registering malformed components in UPDATE_VIEW_COMPONENTS', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    components({}, {
      type: UPDATE_VIEW_COMPONENTS,
      view: 'target-view',
      data: {
        broken: {
          uid: 'broken-list-uid',
          attributes: { id: 'broken-list' },
          context: { view: 'fallback-view' }
        }
      }
    });

    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('[AWE] Malformed component detected in reducer:updateViewComponents'),
      expect.objectContaining({
        componentKey: 'broken',
        componentUid: 'broken-list-uid',
        attributesId: 'broken-list',
        view: 'fallback-view',
        registryComponentId: 'broken',
        targetView: 'target-view'
      })
    );
  });

  it('cleans redux orphan components by context.view when clearing a view with registry enabled', () => {
    const state = {
      orphan: {
        uid: 'orphan-uid',
        attributes: { id: 'orphan' },
        context: { view: 'base' }
      },
      valid: {
        address: { component: 'valid', view: 'other' },
        attributes: { id: 'valid' }
      }
    };

    const next = components(state, { type: CLEAR_COMPONENTS, view: 'base' });

    expect(next).toEqual({
      valid: {
        address: { component: 'valid', view: 'other' },
        attributes: { id: 'valid' }
      }
    });
  });

  it('stores only the delta of a registered component updated by an action without settings', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { foo: 'bar', label: 'Label' },
      model: { values: [{ value: 1 }, { value: 2 }] }
    });

    const next = components({}, {
      type: UPDATE_ATTRIBUTES,
      address: { component: 'comp', view: 'base' },
      data: { foo: 'baz' }
    });

    expect(next.comp).toEqual({ attributes: { foo: 'baz' } });
    expect(ComponentRegistry.get('comp').attributes.foo).toBe('bar');
  });

  it('operates directly on the Redux entry when the component is not registered', () => {
    const state = { comp: { attributes: { foo: 'bar' } } };

    const next = components(state, {
      type: UPDATE_ATTRIBUTES,
      address: { component: 'comp', view: 'base' },
      data: { foo: 'baz' }
    });

    expect(next.comp.attributes).toEqual({ foo: 'baz' });
    expect(ComponentRegistry.get('comp')).toBeFalsy();
  });
});
