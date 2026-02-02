import { components } from '../../../../src/redux/reducers/componentsReducer';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';
import {
  CLEAR_COMPONENTS,
  UPDATE_ATTRIBUTES,
  UPDATE_COMPONENT,
  UPDATE_VALIDATION,
  UPDATE_VIEW_COMPONENTS
} from '../../../../src/redux/actions/components';

describe('awe-react-client/test/js/redux/reducers/componentsReducerRegistryTest.jsx', () => {
  const settings = { useComponentRegistry: true };

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
      data: { comp: baseComponent },
      settings
    });

    expect(ComponentRegistry.get('comp')).toEqual(baseComponent);
    expect(next.comp).toEqual({});
  });

  it('stores only attribute deltas when updating a component with registry enabled', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { foo: 'bar' }
    });

    const next = components({}, {
      type: UPDATE_COMPONENT,
      address: { component: 'comp', view: 'base' },
      data: { attributes: { foo: 'bar', extra: true } },
      settings
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
      data: { foo: 'baz' },
      settings
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
      data: { model: { values: [] } },
      settings
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
      data: { width: 250 },
      settings
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
      data: { pattern: '^[0-9]$' },
      settings
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
      data: { placeholder: 'new' },
      settings
    });

    expect(next.grid.model.values[0].$attrs.colA.placeholder).toBe('new');
  });

  it('clears registry entries when clearing components with registry enabled', () => {
    ComponentRegistry.register('a', { address: { component: 'a', view: 'base' } });
    ComponentRegistry.register('b', { address: { component: 'b', view: 'base' } });
    ComponentRegistry.register('c', { address: { component: 'c', view: 'other' } });

    const state = { a: { attributes: { x: 1 } }, b: { attributes: { y: 2 } }, c: { attributes: { z: 3 } } };
    const next = components(state, { type: CLEAR_COMPONENTS, view: 'base', settings });

    expect(next).toEqual({ c: { attributes: { z: 3 } } });
    expect(ComponentRegistry.get('a')).toBeNull();
    expect(ComponentRegistry.get('b')).toBeNull();
    expect(ComponentRegistry.get('c')).not.toBeNull();
  });
});
