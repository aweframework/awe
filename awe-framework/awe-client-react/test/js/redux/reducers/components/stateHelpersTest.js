import * as stateHelpers from '../../../../../src/redux/reducers/components/stateHelpers';
import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {
  getGridRowContext,
  getMergedComponent,
  hasFooter,
  isGridComponent,
  isGroup,
  launchAddressFunction,
  mergeComponentPropertyValue,
  updateComponentAttributes,
  updateComponentInState,
  updateComponentProperty,
  updateComponentValidation,
  updateGridRowValues
} from '../../../../../src/redux/reducers/components/stateHelpers';

const address = {view: 'base', component: 'comp'};

describe('awe-react-client/test/js/redux/reducers/components/stateHelpersTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
  });

  describe('component id helpers', () => {
    it('does not keep a cache of the addresses it has seen', () => {
      expect(stateHelpers.memoizedGetComponentId).toBeUndefined();
      expect(stateHelpers.memoizedGetGridIdentifier).toBeUndefined();
    });

    it('resolves the current component when the same address object is reused with another row', () => {
      ['grid-1-name', 'grid-2-name'].forEach(id => ComponentRegistry.register(id, {model: {values: []}}));
      const cellAddress = {view: 'base', component: 'grid', row: 1, column: 'name'};

      const first = stateHelpers.getModelUpdate({}, cellAddress, {values: [{value: 'a'}]});
      cellAddress.row = 2;
      const second = stateHelpers.getModelUpdate({}, cellAddress, {values: [{value: 'b'}]});

      expect(Object.keys(first)).toEqual(['grid-1-name']);
      expect(Object.keys(second)).toEqual(['grid-2-name']);
    });
  });

  describe('updateComponentInState', () => {
    it('stores only the delta of a registered component', () => {
      ComponentRegistry.register('comp', {address, attributes: {foo: 'bar', other: 1}});

      const state = updateComponentInState({}, 'comp', {attributes: {foo: 'baz'}});

      expect(state.comp).toEqual({attributes: {foo: 'baz'}});
    });

    it('replaces arrays and validation rules instead of merging them', () => {
      ComponentRegistry.register('comp', {
        address,
        attributes: {items: [1, 2, 3]},
        validationRules: {required: true, min: 1}
      });

      const state = updateComponentInState({}, 'comp', {
        attributes: {items: [9]},
        validationRules: {max: 5}
      });
      const merged = getMergedComponent('comp', state);

      expect(merged.attributes.items).toEqual([9]);
      expect(merged.validationRules).toEqual({max: 5});
    });

    it('merges shallowly into the Redux entry when the component is not registered', () => {
      const state = updateComponentInState({comp: {a: 1}, other: {b: 2}}, 'comp', {c: 3});

      expect(state).toEqual({comp: {a: 1, c: 3}, other: {b: 2}});
    });
  });

  describe('updateComponentProperty', () => {
    it('merges the updates into the property of a registered component', () => {
      ComponentRegistry.register('comp', {address, attributes: {foo: 'bar', other: 1}});

      const state = updateComponentProperty({}, 'comp', 'attributes', {foo: 'baz'});

      expect(getMergedComponent('comp', state).attributes).toEqual({foo: 'baz', other: 1});
    });

    it('merges into the Redux entry of an unregistered component', () => {
      const state = updateComponentProperty({comp: {attributes: {a: 1}}}, 'comp', 'attributes', {b: 2});

      expect(state.comp.attributes).toEqual({a: 1, b: 2});
    });

    it('returns the same state when an unregistered component has no entry', () => {
      const state = {};

      expect(updateComponentProperty(state, 'missing', 'attributes', {a: 1})).toBe(state);
    });
  });

  describe('mergeComponentPropertyValue', () => {
    it('keeps the current numberFormat keys when attributes update only some of them', () => {
      const merged = mergeComponentPropertyValue(
        'attributes',
        {numberFormat: {decimals: 2, grouping: true}, label: 'a'},
        {numberFormat: {decimals: 3}}
      );

      expect(merged).toEqual({numberFormat: {decimals: 3, grouping: true}, label: 'a'});
    });

    it('does a plain shallow merge for properties other than attributes', () => {
      const merged = mergeComponentPropertyValue('validationRules', {numberFormat: {a: 1}}, {numberFormat: {b: 2}});

      expect(merged).toEqual({numberFormat: {b: 2}});
    });
  });

  describe('updateComponentAttributes and updateComponentValidation', () => {
    it('merge into attributes and validationRules respectively', () => {
      ComponentRegistry.register('comp', {address, attributes: {foo: 'bar'}, validationRules: {required: true}});

      const withAttributes = updateComponentAttributes({}, 'comp', {foo: 'baz'});
      const withValidation = updateComponentValidation(withAttributes, 'comp', {min: 2});
      const merged = getMergedComponent('comp', withValidation);

      expect(merged.attributes.foo).toBe('baz');
      expect(merged.validationRules).toEqual({required: true, min: 2});
    });
  });

  describe('getMergedComponent', () => {
    it('merges the registered base with the delta kept in the state', () => {
      ComponentRegistry.register('comp', {address, attributes: {foo: 'bar', other: 1}});

      const merged = getMergedComponent('comp', {comp: {attributes: {foo: 'baz'}}});

      expect(merged.attributes).toEqual({foo: 'baz', other: 1});
      expect(merged.address).toEqual(address);
    });
  });

  describe('component type checks', () => {
    it('isGridComponent is true only when the attributes hold a column model', () => {
      expect(isGridComponent({attributes: {columnModel: []}})).toBe(true);
      expect(isGridComponent({attributes: {}})).toBe(false);
      expect(isGridComponent({})).toBe(false);
      expect(isGridComponent(undefined)).toBe(false);
    });

    it('hasFooter requires a grid with showTotals enabled', () => {
      expect(hasFooter({attributes: {columnModel: [], showTotals: true}})).toBe(true);
      expect(hasFooter({attributes: {columnModel: [], showTotals: false}})).toBeFalsy();
      expect(hasFooter({attributes: {showTotals: true}})).toBe(false);
    });

    it('isGroup is true only for radio components with a group attribute', () => {
      expect(isGroup({attributes: {group: 'g', component: 'radio'}})).toBe(true);
      expect(isGroup({attributes: {group: 'g', component: 'checkbox'}})).toBe(false);
      expect(isGroup({attributes: {component: 'radio'}})).toBe(false);
      expect(isGroup({})).toBe(false);
    });
  });

  describe('launchAddressFunction', () => {
    const settings = {};
    const state = {};
    const handlers = () => ({cell: jest.fn(() => 'cell'), column: jest.fn(() => 'column'), component: jest.fn(() => 'component')});

    it.each([
      ['cell', {view: 'base', component: 'grid', row: 1, column: 'col'}],
      ['column', {view: 'base', component: 'grid', column: 'col'}],
      ['component', {view: 'base', component: 'grid'}]
    ])('dispatches a %s address only to its own function', (type, addr) => {
      const fns = handlers();
      const action = {address: addr, data: {x: 1}};

      const result = launchAddressFunction(fns.cell, fns.column, fns.component, state, action, settings);

      expect(result).toBe(type);
      Object.entries(fns).forEach(([name, fn]) => {
        if (name === type) {
          expect(fn).toHaveBeenCalledWith(state, addr, {x: 1}, settings);
        } else {
          expect(fn).not.toHaveBeenCalled();
        }
      });
    });

    it('returns the state untouched for an invalid address', () => {
      const fns = handlers();

      const result = launchAddressFunction(fns.cell, fns.column, fns.component, state, {address: {}}, settings);

      expect(result).toBe(state);
      Object.values(fns).forEach(fn => expect(fn).not.toHaveBeenCalled());
    });
  });

  describe('grid row helpers', () => {
    const gridAddress = {view: 'base', component: 'grid'};
    const grid = () => ({
      address: gridAddress,
      attributes: {columnModel: [{name: 'name'}]},
      model: {values: [{id: 1, name: 'a'}, {id: 2, name: 'b'}]}
    });

    it('resolves the context of a row of a registered grid', () => {
      ComponentRegistry.register('grid', grid());

      const context = getGridRowContext({}, {...gridAddress, row: 2, column: 'name'});

      expect(context.componentId).toBe('grid');
      expect(context.rowIndex).toBe(1);
      expect(context.rowData).toEqual({id: 2, name: 'b'});
    });

    it('returns null for an unregistered grid or an unknown row', () => {
      expect(getGridRowContext({}, {...gridAddress, row: 1})).toBeNull();

      ComponentRegistry.register('grid', grid());
      expect(getGridRowContext({}, {...gridAddress, row: 9})).toBeNull();
    });

    it('stores the updated values as a delta', () => {
      ComponentRegistry.register('grid', grid());
      const context = getGridRowContext({}, {...gridAddress, row: 1});

      const state = updateGridRowValues({}, context, [{id: 1, name: 'z'}, {id: 2, name: 'b'}]);

      expect(getMergedComponent('grid', state).model.values[0].name).toBe('z');
    });
  });

  describe('defaults and fallbacks', () => {
    it('getGridRowContext resolves nothing without arguments', () => {
      expect(getGridRowContext()).toBeNull();
    });

    it('getGridRowContext returns null when the merged grid has no model or no attributes', () => {
      ComponentRegistry.register('noModel', {address: {view: 'base', component: 'noModel'}, attributes: {columnModel: []}});
      ComponentRegistry.register('noAttributes', {address: {view: 'base', component: 'noAttributes'}, model: {values: []}});

      expect(getGridRowContext({}, {component: 'noModel', row: 1})).toBeNull();
      expect(getGridRowContext({}, {component: 'noAttributes', row: 1})).toBeNull();
    });

    it('getGridRowContext returns the data of the row it finds', () => {
      ComponentRegistry.register('grid', {
        address: {view: 'base', component: 'grid'},
        attributes: {columnModel: [{name: 'name'}]},
        model: {values: [{id: 1}]}
      });

      expect(getGridRowContext({}, {component: 'grid', row: 1}).rowData).toEqual({id: 1});
    });

    it('updateGridRowValues stores the values of a context without model', () => {
      ComponentRegistry.register('grid', {address: {view: 'base', component: 'grid'}, attributes: {columnModel: []}});
      const context = {componentId: 'grid', base: ComponentRegistry.get('grid'), component: undefined};

      const state = updateGridRowValues({}, context, [{id: 1}]);

      expect(state.grid.model.values).toEqual([{id: 1}]);
    });

    it('mergeComponentPropertyValue merges without current value or updates', () => {
      expect(mergeComponentPropertyValue('attributes')).toEqual({});
      expect(mergeComponentPropertyValue('other')).toEqual({});
    });

    it('mergeComponentPropertyValue merges a number format when there is no current number format', () => {
      expect(mergeComponentPropertyValue('attributes', undefined, {numberFormat: {a: 1}})).toEqual({numberFormat: {a: 1}});
      expect(mergeComponentPropertyValue('attributes', {label: 'x'}, {numberFormat: {a: 1}}))
        .toEqual({label: 'x', numberFormat: {a: 1}});
    });

    it('mergeComponentPropertyValue replaces a primitive or null number format as a plain update', () => {
      const current = {numberFormat: {a: 1}};

      expect(mergeComponentPropertyValue('attributes', current, {numberFormat: null})).toEqual({numberFormat: null});
      expect(mergeComponentPropertyValue('attributes', current, {numberFormat: 'x'})).toEqual({numberFormat: 'x'});
    });

    it('getMergedComponent returns undefined for a component that is neither registered nor in the state', () => {
      expect(getMergedComponent('missing', {})).toBeUndefined();
    });

    it('getMergedComponent returns the entry of an unregistered component in the state', () => {
      const merged = getMergedComponent('plain', {plain: {attributes: {a: 1}}});

      expect(merged.attributes).toEqual({a: 1});
    });

    it('hasFooter is falsy for a grid without showTotals and for an empty component', () => {
      expect(hasFooter({attributes: {columnModel: []}})).toBeFalsy();
      expect(hasFooter({})).toBeFalsy();
    });

    it('isGroup tolerates a radio group without component attribute', () => {
      expect(isGroup({attributes: {group: 'g'}})).toBe(false);
    });
  });
});
