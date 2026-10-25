import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {
  getRestoredModel,
  getRestoreModelComponent,
  getSelectedUpdate,
  keepModelComponent,
  resetModel,
  restoreModelComponent,
  updateComponentData,
  updateModel,
  updateModelAction,
  updateSelected,
  updateSelectedComponent
} from '../../../../../src/redux/reducers/components/model';
import {getMergedComponent} from '../../../../../src/redux/reducers/components/stateHelpers';

const address = {view: 'base', component: 'grid'};
const selectAddress = {view: 'base', component: 'sel'};
const options = [{value: 'a', label: 'A'}, {value: 'b', label: 'B'}, {value: 'c', label: 'C'}];

const gridComponent = () => ({
  address,
  attributes: {columnModel: [{name: 'label'}], error: 'old'},
  model: {values: [{id: 1, label: 'a'}, {id: 2, label: 'b'}]}
});

const selectComponent = () => ({
  address: selectAddress,
  attributes: {},
  model: {values: options.map(option => ({...option, selected: option.value === 'a'}))}
});

const get = (id, state) => getMergedComponent(id, state);
const selectedValues = (id, state) => get(id, state).model.values.filter(value => value.selected).map(value => value.value);

describe('awe-react-client/test/js/redux/reducers/components/modelTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
    ComponentRegistry.register('grid', gridComponent());
    ComponentRegistry.register('sel', selectComponent());
  });

  describe('updateComponentData', () => {
    it('returns the state when there is no component id', () => {
      const state = {};

      expect(updateComponentData(state, null, {attributes: {}}, {})).toBe(state);
    });

    it('registers a full component and keeps the deltas against the base', () => {
      const data = {address: {view: 'base', component: 'new'}, attributes: {title: 'T'}, model: {values: [{value: 1}]}};

      const state = updateComponentData({}, 'new', data, {});

      expect(ComponentRegistry.get('new')).toBeDefined();
      expect(get('new', state).attributes.title).toBe('T');
      expect(get('new', state).model.values).toEqual([{value: 1}]);
    });

    it('applies a partial update to the state entry', () => {
      const state = updateComponentData({}, 'grid', {attributes: {title: 'X'}}, {});

      expect(get('grid', state).attributes.title).toBe('X');
    });
  });

  describe('updateModel', () => {
    it('updates the model of a component and flags it as changed', () => {
      const state = updateModel({}, selectAddress, {values: [{value: 'z', label: 'Z'}]});

      expect(get('sel', state).model.values).toEqual([{value: 'z', label: 'Z'}]);
      expect(get('sel', state).model.changed).toBe(true);
    });

    it('updates a grid model, bumping its version', () => {
      const state = updateModel({}, address, {values: [{id: 3, label: 'c'}]});

      expect(get('grid', state).model.values).toEqual([{id: 3, label: 'c'}]);
      expect(get('grid', state).model.modelVersion).toBe(1);
      expect(get('grid', state).attributes.error).toBeNull();
    });

    it('updates the whole radio group when the component is a grouped radio', () => {
      ComponentRegistry.register('r1', {address: {view: 'base', component: 'r1'}, attributes: {group: 'g', component: 'radio'}, model: {values: [{value: 'r1'}]}});
      ComponentRegistry.register('r2', {address: {view: 'base', component: 'r2'}, attributes: {group: 'g', component: 'radio'}, model: {values: [{value: 'r2'}]}});

      const state = updateModel({}, {view: 'base', component: 'r1'}, {values: [{value: 'r2'}]});

      expect(selectedValues('r1', state)).toEqual([]);
      expect(selectedValues('r2', state)).toEqual(['r2']);
    });
  });

  describe('selection', () => {
    it('updateSelectedComponent flags the selected options', () => {
      const update = updateSelectedComponent({}, get('sel', {}).model.values, selectAddress, ['b']);

      expect(selectedValues('sel', update)).toEqual(['b']);
    });

    it('updateSelectedComponent returns null when nothing changes', () => {
      expect(updateSelectedComponent({}, get('sel', {}).model.values, selectAddress, ['a'])).toBeNull();
    });

    it('getSelectedUpdate selects the rows of a grid', () => {
      const update = getSelectedUpdate({}, address, {selected: 2});

      expect(get('grid', update).model.values.map(row => !!row.selected)).toEqual([false, true]);
    });

    it('getSelectedUpdate selects on a component', () => {
      const update = getSelectedUpdate({}, selectAddress, {selected: ['c']});

      expect(selectedValues('sel', update)).toEqual(['c']);
    });

    it('updateSelected merges the update into the state', () => {
      const state = updateSelected({other: {x: 1}}, selectAddress, {selected: ['b']});

      expect(state.other).toEqual({x: 1});
      expect(selectedValues('sel', state)).toEqual(['b']);
    });

    it('updateSelected returns the same state when nothing changes', () => {
      const state = {};

      expect(updateSelected(state, selectAddress, {selected: ['a']})).toBe(state);
    });
  });

  describe('keep, restore and reset', () => {
    it('keeps a snapshot that restore goes back to', () => {
      const kept = keepModelComponent({}, 'grid', {}, true);
      const changed = updateModel(kept, address, {values: [{id: 9, label: 'z'}]});

      const restored = restoreModelComponent(changed, 'grid', {}, true);

      expect(get('grid', restored).model.values).toEqual([{id: 1, label: 'a'}, {id: 2, label: 'b'}]);
      expect(get('grid', restored).model.changed).toBe(false);
    });

    it('stores the initial snapshot as the default model', () => {
      const state = keepModelComponent({}, 'sel', {}, true);

      expect(get('sel', state).defaultModel.values).toEqual(get('sel', {}).model.values);
    });

    it('stores the given default model with the initial snapshot', () => {
      const defaultModel = {values: [{value: 'c', label: 'C', selected: true}]};

      const state = keepModelComponent({}, 'sel', {}, true, defaultModel);

      expect(get('sel', state).defaultModel.values).toEqual(defaultModel.values);
    });

    it('restore goes back to the default selection keeping the current options', () => {
      const kept = keepModelComponent({}, 'sel', {}, true);
      const changed = updateSelected(kept, selectAddress, {selected: ['b']});

      const restored = restoreModelComponent(changed, 'sel', {});

      expect(selectedValues('sel', restored)).toEqual(['a']);
      expect(get('sel', restored).model.values.map(value => value.value)).toEqual(['a', 'b', 'c']);
    });

    it('restore-target goes back to the first loaded model', () => {
      const kept = keepModelComponent({}, 'sel', {}, true);
      const changed = updateSelected(kept, selectAddress, {selected: ['b']});

      const restored = restoreModelComponent(changed, 'sel', {}, true);

      expect(selectedValues('sel', restored)).toEqual(['a']);
    });

    it('getRestoreModelComponent operates on the state entry when the component is not registered', () => {
      const state = {other: {storedModel: {values: [{value: 1}]}, model: {values: [{value: 2}]}}};

      expect(getRestoreModelComponent(state, 'other', {}, true).other.model).toEqual({values: [{value: 1}], changed: false});
      expect(getRestoreModelComponent({}, 'other', {})).toEqual({});
    });

    it('getRestoredModel uses the stored model for a grid, the initial restore and no default model', () => {
      const stored = {values: [{value: 'x'}]};
      const model = {values: [{value: 'y'}]};

      expect(getRestoredModel({storedModel: stored, model}, false)).toBe(stored);
      expect(getRestoredModel({model, defaultModel: {values: []}}, true)).toBe(model);
      expect(getRestoredModel({model, attributes: {columnModel: []}, defaultModel: {values: []}}, false)).toBe(model);
    });

    it('getRestoredModel adds the defaults that are no longer among the options', () => {
      const component = {
        model: {values: [{value: 'a'}]},
        defaultModel: {values: [{value: 'a', selected: true}, {value: 'q', selected: true}, {value: 'r'}]}
      };

      expect(getRestoredModel(component, false).values).toEqual([
        {value: 'a', selected: true},
        {value: 'q', selected: true}
      ]);
    });

    it('resets a component to unselected values', () => {
      const state = resetModel({}, selectAddress, {}, {});

      expect(selectedValues('sel', state)).toEqual([]);
      expect(get('sel', state).model.changed).toBe(true);
    });

    it('resets a grid to an empty model and clears its error', () => {
      const state = resetModel({}, address, {}, {});

      expect(get('grid', state).model).toMatchObject({values: [], page: 1, total: 1, records: 0, changed: true});
      expect(get('grid', state).attributes.error).toBeNull();
    });

    it('resets an unregistered component on its state entry', () => {
      const state = resetModel({other: {model: {values: [{value: 1, selected: true}]}}}, {view: 'base', component: 'other'}, {}, {});

      expect(state.other.model.values).toEqual([{value: 1, selected: false}]);
    });
  });

  describe('updateModelAction', () => {
    it('updates the model of the address', () => {
      const state = updateModelAction({}, {address: selectAddress, data: {values: [{value: 'z'}]}}, {});

      expect(get('sel', state).model.values).toEqual([{value: 'z'}]);
    });

    it('updates the selection when only selected comes in the data', () => {
      const state = updateModelAction({}, {address: selectAddress, data: {selected: ['b']}}, {});

      expect(selectedValues('sel', state)).toEqual(['b']);
    });

    it('updates the model and then the selection', () => {
      const state = updateModelAction({}, {address: selectAddress, data: {values: options, selected: ['c']}}, {});

      expect(selectedValues('sel', state)).toEqual(['c']);
    });

    it('updates a cell model through the address type', () => {
      const cell = {...address, row: 2, column: 'label'};

      const state = updateModelAction({}, {address: cell, data: {values: 'z'}}, {});

      expect(get('grid', state).model.values[1].label).toEqual({value: 'z', label: 'z'});
    });

    it('updates a column model through the address type', () => {
      const state = updateModelAction({}, {address: {...address, column: 'label'}, data: {values: [{value: 1}]}}, {});

      expect(get('grid', state).attributes.columnModel[0].model.values).toEqual([{value: 1}]);
    });

    it('refreshes the footer of a grid with totals', () => {
      ComponentRegistry.register('totals', {
        address: {view: 'base', component: 'totals'},
        attributes: {showTotals: true, columnModel: [{name: 'amount', summaryType: 'sum'}]},
        model: {values: [{id: 1, amount: 1}]}
      });

      const state = updateModelAction({}, {
        address: {view: 'base', component: 'totals'},
        data: {values: [{id: 1, amount: 2}, {id: 2, amount: 3}]}
      }, {});

      expect(get('totals', state).model.footer).toEqual({amount: {label: '5', value: 5}});
    });
  });
});
