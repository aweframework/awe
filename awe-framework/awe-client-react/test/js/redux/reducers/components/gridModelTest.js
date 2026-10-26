import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {
  checkSelected,
  fixCellModel,
  getFilteredValues,
  getGridModelUpdate,
  getGroupModelUpdate,
  keepRowModel,
  resetCellModel,
  updateCellModel,
  updateCellSelected,
  updateCellsModel,
  updateColumnModel,
  updateRowModel,
  updateSelectedGrid,
  updateSelectedGroup
} from '../../../../../src/redux/reducers/components/gridModel';
import {
  getKeepModelComponent,
  getMergedComponent,
  getModelUpdate
} from '../../../../../src/redux/reducers/components/stateHelpers';

const address = {view: 'base', component: 'grid'};
const columnAddress = {...address, column: 'amount'};
const cellAddress = {...address, row: 2, column: 'status'};
const statusOptions = [{value: 'a', label: 'A'}, {value: 'b', label: 'B'}];

const gridComponent = () => ({
  address,
  attributes: {
    columnModel: [
      {name: 'amount', model: {values: [{value: 1}]}},
      {name: 'status', component: 'select', model: {values: statusOptions}},
      {name: 'tags', component: 'select-multiple'},
      {name: 'label'}
    ]
  },
  model: {
    values: [
      {id: 1, amount: 2, status: 'a', label: 'a'},
      {id: 2, amount: 3, status: 'a', label: 'b'}
    ]
  }
});

const getGrid = state => getMergedComponent('grid', state);
const getRow = (state, index = 1) => getGrid(state).model.values[index];

describe('awe-react-client/test/js/redux/reducers/components/gridModelTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
    ComponentRegistry.register('grid', gridComponent());
  });

  describe('fixCellModel', () => {
    it('marks the selected options of an existing model', () => {
      const result = fixCellModel(['b'], {values: statusOptions});

      expect(result.values).toEqual([{value: 'a', label: 'A', selected: false}, {value: 'b', label: 'B', selected: true}]);
    });

    it('builds a model from the selection when there are no options', () => {
      expect(fixCellModel('x', {}).values).toEqual({value: 'x'});
    });
  });

  describe('checkSelected', () => {
    it('compares a plain value as a string', () => {
      expect(checkSelected([1], 1)).toBe(true);
      expect(checkSelected([2], 1)).toBe(false);
    });

    it('compares the value of a cell object', () => {
      expect(checkSelected(['a'], {value: 'a'})).toBe(true);
    });

    it('needs exactly one selected match in a list of options', () => {
      expect(checkSelected(['a'], [{value: 'a', selected: true}, {value: 'b'}])).toBe(true);
      expect(checkSelected(['a'], [{value: 'a'}, {value: 'b', selected: true}])).toBe(false);
    });
  });

  describe('getFilteredValues', () => {
    it('flags the selected values', () => {
      expect(getFilteredValues([{value: 1}, {value: 2}], [2])).toEqual([
        {value: 1, selected: false},
        {value: 2, selected: true}
      ]);
    });

    it('builds the values from the selection when none matches', () => {
      expect(getFilteredValues([{value: 1}], [9])).toEqual([{value: 9, selected: true}]);
    });
  });

  describe('getModelUpdate', () => {
    it('merges the model, flags it as changed and clears the error', () => {
      const update = getModelUpdate({}, address, {page: 3}, {});

      const state = {...update};
      expect(getGrid(state).model.page).toBe(3);
      expect(getGrid(state).model.changed).toBe(true);
      expect(getGrid(state).attributes.error).toBeNull();
    });

    it('operates on the state entry when the component is not registered', () => {
      const update = getModelUpdate({other: {model: {values: [1]}}}, {view: 'base', component: 'other'}, {page: 2}, {});

      expect(update.other.model).toEqual({values: [1], page: 2, changed: true});
      expect(update.other.attributes).toEqual({error: null});
    });
  });

  describe('getGridModelUpdate', () => {
    it('bumps the model version and refreshes the cell components', () => {
      ComponentRegistry.register('grid-2-status', {address: cellAddress, attributes: {}, model: {values: []}});
      const values = [{id: 2, amount: 3, status: 'b', label: 'b'}];

      const update = getGridModelUpdate({}, address, {values});
      const state = {...update};

      expect(getGrid(state).model.modelVersion).toBe(1);
      expect(getMergedComponent('grid-2-status', state).model.values).toEqual([
        {value: 'a', label: 'A', selected: false},
        {value: 'b', label: 'B', selected: true}
      ]);
    });

    it('does not touch the cells when update is false', () => {
      const update = getGridModelUpdate({}, address, {values: [{id: 2}]}, false);

      expect(Object.keys(update)).toEqual(['grid']);
    });

    it('returns the same state for a grid that is neither registered nor in the state', () => {
      const state = {};

      expect(getGridModelUpdate(state, {view: 'base', component: 'missing'}, {values: []})).toBe(state);
    });
  });

  describe('updateCellsModel', () => {
    it('only updates the cells of columns with a component that exist', () => {
      ComponentRegistry.register('grid-2-status', {address: cellAddress, attributes: {}, model: {values: []}});

      const update = updateCellsModel({}, getGrid({}), {});

      expect(Object.keys(update)).toEqual(['grid-2-status']);
    });

    it('returns the same updates for every row and editable column that has a cell', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {
        address,
        attributes: {
          id: 'id',
          columnModel: [
            {name: 'status', component: 'select', model: {values: statusOptions}},
            {name: 'tags', component: 'select-multiple'},
            {name: 'label'}
          ]
        },
        model: {values: [{id: 1, status: 'a', tags: 'b'}, {id: 2, status: 'b', tags: 'a'}, {id: 3, status: 'a', tags: 'a'}]}
      });
      ComponentRegistry.register('grid-1-status', {address: {...address, row: 1, column: 'status'}, attributes: {}, model: {values: []}});
      ComponentRegistry.register('grid-3-tags', {address: {...address, row: 3, column: 'tags'}, attributes: {}, model: {values: []}});
      const state = {'grid-2-status': {attributes: {}, model: {values: []}}};

      const update = updateCellsModel(state, getGrid({}), {});

      expect(Object.keys(update).sort()).toEqual(['grid-1-status', 'grid-2-status', 'grid-3-tags']);
      expect(update['grid-2-status'].model.values).toEqual([
        {value: 'a', label: 'A', selected: false},
        {value: 'b', label: 'B', selected: true}
      ]);
      expect(update['grid-1-status'].model.changed).toBe(true);
    });

    it('returns nothing without touching the registry when no column has a component', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {
        address,
        attributes: {id: 'id', columnModel: [{name: 'label'}]},
        model: {values: [{id: 1, label: 'a'}, {id: 2, label: 'b'}]}
      });
      const grid = getGrid({});
      const get = jest.spyOn(ComponentRegistry, 'get');

      const update = updateCellsModel({}, grid, {});

      expect(update).toEqual({});
      expect(get).not.toHaveBeenCalled();
      get.mockRestore();
    });
  });

  describe('updateColumnModel', () => {
    it('merges the model of a column', () => {
      const state = updateColumnModel({}, columnAddress, {values: [{value: 5}]});

      expect(getGrid(state).attributes.columnModel[0].model.values).toEqual([{value: 5}]);
    });

    it('operates on the state entry when the component is not registered', () => {
      const state = {other: {attributes: {columnModel: [{name: 'amount', model: {a: 1}}]}}};

      const result = updateColumnModel(state, {view: 'base', component: 'other', column: 'amount'}, {b: 2});

      expect(result.other.attributes.columnModel[0].model).toEqual({a: 1, b: 2});
    });

    it('returns the state when the entry has no attributes', () => {
      const state = {other: {}};

      expect(updateColumnModel(state, {component: 'other', column: 'x'}, {})).toBe(state);
    });
  });

  describe('updateCellModel', () => {
    it('writes the cell value and clears the cell error', () => {
      const withError = updateRowModel({}, {...address, row: 2}, {$attrs: {label: {error: 'x', hidden: true}}});

      const state = updateCellModel(withError, {...address, row: 2, column: 'label'}, {values: 'z'});

      expect(getRow(state).label).toEqual({value: 'z', label: 'z'});
      expect(getRow(state).$attrs.label).toEqual({hidden: true});
    });

    it('stores a list for a multiple component', () => {
      const state = updateCellModel({}, {...address, row: 2, column: 'tags'}, {values: ['a', 'b']});

      expect(getRow(state).tags).toEqual(['a', 'b']);
    });

    it('returns the state when the row does not exist', () => {
      const state = {};

      expect(updateCellModel(state, {...address, row: 99, column: 'label'}, {})).toBe(state);
    });
  });

  describe('updateCellSelected', () => {
    it('replaces a plain cell value with the selected one', () => {
      const state = updateCellSelected({}, cellAddress, {selected: ['b']});

      expect(getRow(state).status).toEqual({value: 'b'});
    });

    it('does not change the state when the selection is already there', () => {
      const state = {};

      expect(updateCellSelected(state, cellAddress, {selected: ['a']})).toBe(state);
    });
  });

  describe('updateRowModel', () => {
    it('merges the row data', () => {
      const state = updateRowModel({}, {...address, row: 1}, {label: 'new'});

      expect(getRow(state, 0)).toEqual({id: 1, amount: 2, status: 'a', label: 'new'});
    });

    it('returns the state when the row does not exist', () => {
      const state = {};

      expect(updateRowModel(state, {...address, row: 99}, {label: 'x'})).toBe(state);
    });
  });

  describe('updateSelectedGrid', () => {
    it('selects the rows by id and unselects the rest', () => {
      const first = updateSelectedGrid({}, getGrid({}).model.values, address, {selected: [1]});
      const second = updateSelectedGrid(first, getGrid(first).model.values, address, {selected: [2]});

      expect(getGrid(first).model.values.map(row => !!row.selected)).toEqual([true, false]);
      expect(getGrid(second).model.values.map(row => !!row.selected)).toEqual([false, true]);
    });

    it('ignores an unknown row id', () => {
      const state = updateSelectedGrid({}, getGrid({}).model.values, address, {selected: [99]});

      expect(getGrid(state).model.values.map(row => !!row.selected)).toEqual([false, false]);
    });
  });

  describe('updateSelectedGroup and getGroupModelUpdate', () => {
    beforeEach(() => {
      const radio = value => ({
        address: {view: 'base', component: value},
        attributes: {group: 'g'},
        model: {values: [{value}]}
      });
      ComponentRegistry.register('r1', radio('r1'));
      ComponentRegistry.register('r2', radio('r2'));
    });

    it('flags the selected values of every component of the group', () => {
      const update = updateSelectedGroup({}, [], 'base', 'g', ['r2']);

      expect(Object.keys(update).sort()).toEqual(['r1', 'r2']);
      expect(getMergedComponent('r1', update).model.values[0].selected).toBe(false);
      expect(getMergedComponent('r2', update).model.values[0].selected).toBe(true);
    });

    it('builds the selection from the model values', () => {
      const update = getGroupModelUpdate({}, 'base', 'g', {values: [{value: 'r1'}]});

      expect(getMergedComponent('r1', update).model.values[0].selected).toBe(true);
      expect(getMergedComponent('r2', update).model.values[0].selected).toBe(false);
    });
  });

  describe('getKeepModelComponent', () => {
    it('stores a snapshot of the model', () => {
      const update = getKeepModelComponent({}, 'grid', {});

      expect(getMergedComponent('grid', update).storedModel.values).toEqual(getGrid({}).model.values);
    });

    it('stores the default model with the initial snapshot', () => {
      const update = getKeepModelComponent({}, 'grid', {}, true, {values: [{id: 9}]});

      expect(getMergedComponent('grid', update).defaultModel.values).toEqual([{id: 9}]);
    });

    it('operates on the state entry when the component is not registered', () => {
      const state = {other: {model: {values: [{a: 1}]}}};

      expect(getKeepModelComponent(state, 'other', {}, true).other.defaultModel).toEqual({values: [{a: 1}]});
      expect(getKeepModelComponent({}, 'other', {})).toEqual({});
    });
  });

  describe('keepRowModel', () => {
    it('stores the row snapshot with the snapshot of its cell components', () => {
      ComponentRegistry.register('grid-2-status', {address: cellAddress, attributes: {}, model: {values: [{value: 'a'}]}});

      const state = keepRowModel({}, {...address, row: 2}, {});

      expect(getGrid(state).storedModel.storedRows[2]).toEqual(getRow({}));
      expect(getMergedComponent('grid-2-status', state).storedModel.values).toEqual([{value: 'a'}]);
    });

    it('returns the state when the row does not exist', () => {
      const state = {};

      expect(keepRowModel(state, {...address, row: 99}, {})).toBe(state);
    });

    it('operates on the state entry when the grid is not registered', () => {
      const state = {other: {model: {values: [{id: 1}]}, attributes: {}}};

      const result = keepRowModel(state, {view: 'base', component: 'other', row: 1}, {});

      expect(result.other.storedModel.storedRows[1]).toEqual({id: 1});
    });
  });

  describe('resetCellModel', () => {
    it('looks the column up by its grid identifier, so a column without it resets the cell to an empty selection', () => {
      const state = resetCellModel({}, {...address, row: 2, column: 'amount'}, {}, {});

      expect(getRow(state).amount).toEqual({});
    });

    it('returns the state without attributes', () => {
      const state = {};

      expect(resetCellModel(state, {component: 'missing', column: 'x'}, {}, {})).toBe(state);
    });
  });
});
