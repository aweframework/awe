import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {
  keepValidationComponent,
  restoreValidationAction,
  restoreValidationCell,
  restoreValidationColumn,
  restoreValidationComponent,
  updateValidationAction,
  updateValidationCell,
  updateValidationColumn,
  updateValidationComponent
} from '../../../../../src/redux/reducers/components/validationRules';
import {getMergedComponent} from '../../../../../src/redux/reducers/components/stateHelpers';

const address = {view: 'base', component: 'grid'};
const columnAddress = {...address, column: 'amount'};
const cellAddress = {...columnAddress, row: 2};

const gridComponent = () => ({
  address,
  validationRules: {required: true},
  attributes: {
    columnModel: [
      {name: 'amount', validationRules: {required: true}},
      {name: 'label'}
    ]
  },
  model: {values: [{id: 1, amount: 2, label: 'a'}, {id: 2, amount: 3, label: 'b'}]}
});

const getRules = (state, key) => getMergedComponent('grid', state)[key];
const getColumn = (state, index = 0) => getMergedComponent('grid', state).attributes.columnModel[index];
const getRow = (state, index = 1) => getMergedComponent('grid', state).model.values[index];

describe('awe-react-client/test/js/redux/reducers/components/validationRulesTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
    ComponentRegistry.register('grid', gridComponent());
  });

  describe('update validation rules', () => {
    it('merges the rules of a component', () => {
      const state = updateValidationComponent({}, address, {min: 1});

      expect(getRules(state, 'validationRules')).toEqual({required: true, min: 1});
    });

    it('merges the rules of a column', () => {
      const state = updateValidationColumn({}, columnAddress, {min: 1});

      expect(getColumn(state).validationRules).toEqual({required: true, min: 1});
    });

    it('leaves the state untouched for an unknown column', () => {
      const state = {};

      expect(updateValidationColumn(state, {...address, column: 'missing'}, {min: 1})).toBe(state);
    });

    it('updates the rules of a column of an unregistered component directly on the state', () => {
      ComponentRegistry.clearAll();
      const state = updateValidationColumn({grid: gridComponent()}, columnAddress, {min: 1});

      expect(state.grid.attributes.columnModel[0].validationRules).toEqual({required: true, min: 1});
    });

    it('leaves the state untouched for an unregistered component without attributes', () => {
      ComponentRegistry.clearAll();
      const state = {};

      expect(updateValidationColumn(state, columnAddress, {min: 1})).toBe(state);
    });

    it('stores the rules of a cell in the row $attrs', () => {
      const state = updateValidationCell({}, cellAddress, {min: 1});

      expect(getRow(state).$attrs.amount.validationRules).toEqual({min: 1});
    });

    it('leaves the state untouched for an unknown row', () => {
      const state = {};

      expect(updateValidationCell(state, {...cellAddress, row: 99}, {min: 1})).toBe(state);
    });
  });

  describe('keep and restore validation rules', () => {
    it('restores the rules of a component after they were changed', () => {
      let state = keepValidationComponent({}, 'grid');
      state = updateValidationComponent(state, address, {required: false, min: 1});
      state = restoreValidationComponent(state, address);

      expect(getRules(state, 'storedValidationRules')).toEqual({required: true});
      expect(getRules(state, 'validationRules')).toEqual({required: true});
    });

    it('leaves the state untouched when nothing was kept', () => {
      const state = {};

      expect(restoreValidationComponent(state, address)).toBe(state);
    });

    it('restores the rules of a column from the stored attributes', () => {
      ComponentRegistry.clearAll();
      const stored = {attributes: {columnModel: [{name: 'amount', validationRules: {required: true}}]}};
      const state = {
        grid: {
          ...gridComponent(),
          attributes: {columnModel: [{name: 'amount', validationRules: {min: 1}}]},
          storedAttributes: stored.attributes
        }
      };

      const result = restoreValidationColumn(state, columnAddress);

      expect(result.grid.attributes.columnModel[0].validationRules).toEqual({required: true});
    });

    it('removes the rules of a column that had none when they were kept', () => {
      ComponentRegistry.clearAll();
      const state = {
        grid: {
          attributes: {columnModel: [{name: 'amount', validationRules: {min: 1}}]},
          storedAttributes: {columnModel: [{name: 'amount'}]}
        }
      };

      const result = restoreValidationColumn(state, columnAddress);

      expect(result.grid.attributes.columnModel[0].validationRules).toBeUndefined();
    });

    it('leaves the state untouched when restoring an unknown column', () => {
      const state = {};

      expect(restoreValidationColumn(state, {...address, column: 'missing'})).toBe(state);
    });

    it('removes the rules of a cell on restore', () => {
      let state = updateValidationCell({}, cellAddress, {min: 1});
      state = restoreValidationCell(state, cellAddress);

      expect(getRow(state).$attrs.amount).toEqual({});
    });

    it('leaves the state untouched when the cell has no rules', () => {
      const state = {};

      expect(restoreValidationCell(state, cellAddress)).toBe(state);
    });
  });

  describe('action wrappers', () => {
    it('update the rules by address type', () => {
      let state = updateValidationAction({}, {address, data: {min: 1}});
      state = updateValidationAction(state, {address: columnAddress, data: {max: 5}});
      state = updateValidationAction(state, {address: cellAddress, data: {step: 2}});

      expect(getRules(state, 'validationRules')).toEqual({required: true, min: 1});
      expect(getColumn(state).validationRules).toEqual({required: true, max: 5});
      expect(getRow(state).$attrs.amount.validationRules).toEqual({step: 2});
    });

    it('restore the rules by address type', () => {
      let state = updateValidationAction({}, {address: cellAddress, data: {step: 2}});
      state = restoreValidationAction(state, {address: cellAddress});

      expect(getRow(state).$attrs.amount).toEqual({});
    });

    it('leave the state untouched for an unknown address type', () => {
      const state = {};

      expect(updateValidationAction(state, {address: {}})).toBe(state);
      expect(restoreValidationAction(state, {address: {}})).toBe(state);
    });
  });

  describe('calls that omit the optional arguments', () => {
    it('leave an empty state untouched', () => {
      expect(updateValidationAction()).toEqual({});
      expect(restoreValidationAction()).toEqual({});
      expect(updateValidationComponent()).toEqual({});
      expect(updateValidationColumn()).toEqual({});
      expect(updateValidationCell()).toEqual({});
      expect(restoreValidationColumn()).toEqual({});
      expect(restoreValidationCell()).toEqual({});
    });
  });

  describe('update validation rules of incomplete components', () => {
    it('creates the rules of a column that has none', () => {
      const state = updateValidationColumn({}, {...address, column: 'label'}, {min: 1});

      expect(getColumn(state, 1).validationRules).toEqual({min: 1});
    });

    it('appends the rules when the column of an unregistered component is not in the model', () => {
      ComponentRegistry.clearAll();
      const state = updateValidationColumn({grid: {attributes: {}}}, columnAddress, {min: 1});

      expect(state.grid.attributes.columnModel).toEqual([{validationRules: {min: 1}}]);
    });

    it('creates the rules of a column of an unregistered component that has none', () => {
      ComponentRegistry.clearAll();
      const state = {grid: {attributes: {columnModel: [{name: 'amount'}]}}};

      expect(updateValidationColumn(state, columnAddress, {min: 1}).grid.attributes.columnModel[0].validationRules)
        .toEqual({min: 1});
    });

    it('ignores the column of a registered component without column model', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {address, attributes: {}});
      const state = {};

      expect(updateValidationColumn(state, columnAddress, {min: 1})).toBe(state);
    });

    it('merges the rules of a cell that already has attributes', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {
        ...gridComponent(),
        model: {values: [{id: 2, $attrs: {amount: {hidden: true, validationRules: {min: 1}}, label: {readonly: true}}}]}
      });
      const state = updateValidationCell({}, cellAddress, {max: 5});
      const row = getRow(state, 0);

      expect(row.$attrs.amount).toEqual({hidden: true, validationRules: {min: 1, max: 5}});
      expect(row.$attrs.label).toEqual({readonly: true});
    });
  });

  describe('keep and restore validation rules of incomplete components', () => {
    it('keeps empty rules for a component that is not registered nor in the state', () => {
      ComponentRegistry.clearAll();
      const state = keepValidationComponent({}, 'missing');

      expect(state.missing).toEqual({storedValidationRules: {}});
    });

    it('does nothing when restoring a component that is neither registered nor in the state', () => {
      ComponentRegistry.clearAll();
      const state = {};

      expect(restoreValidationComponent(state, {view: 'base', component: 'missing'})).toBe(state);
    });

    it('restores the rules of a component that is not registered', () => {
      ComponentRegistry.clearAll();
      const state = {grid: {validationRules: {min: 1}, storedValidationRules: {required: true}}};

      expect(restoreValidationComponent(state, address).grid.validationRules).toEqual({required: true});
    });

    it('restores the rules of a column of a registered grid', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {
        ...gridComponent(),
        storedAttributes: {columnModel: [{validationRules: {stored: true}}]}
      });
      const changed = updateValidationColumn({}, columnAddress, {min: 1});

      const restored = restoreValidationColumn(changed, columnAddress);

      expect(getColumn(restored).validationRules).toEqual({stored: true});
    });

    it('removes the rules of a column of a registered grid when none were stored', () => {
      const changed = updateValidationColumn({}, columnAddress, {min: 1});

      const restored = restoreValidationColumn(changed, columnAddress);

      expect(getColumn(restored).validationRules).toBeUndefined();
    });

    it('removes the rules of a column of a registered grid whose stored column model is shorter', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {...gridComponent(), storedAttributes: {columnModel: []}});

      const restored = restoreValidationColumn({}, columnAddress);

      expect(getColumn(restored).validationRules).toBeUndefined();
    });

    it('ignores the restore of a column of a registered grid without column model', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {address, attributes: {}});
      const state = {};

      expect(restoreValidationColumn(state, columnAddress)).toBe(state);
    });

    it('ignores the restore of a column of an unregistered component without attributes', () => {
      ComponentRegistry.clearAll();
      const missing = {};
      const noAttributes = {grid: {}};

      expect(restoreValidationColumn(missing, columnAddress)).toBe(missing);
      expect(restoreValidationColumn(noAttributes, columnAddress)).toBe(noAttributes);
    });

    it('ignores the restore of an unknown column of an unregistered component', () => {
      ComponentRegistry.clearAll();
      const withoutColumns = {grid: {attributes: {}}};
      const otherColumn = {grid: gridComponent()};

      expect(restoreValidationColumn(withoutColumns, columnAddress)).toBe(withoutColumns);
      expect(restoreValidationColumn(otherColumn, {...columnAddress, column: 'missing'})).toBe(otherColumn);
    });

    it('removes the rules of a column of an unregistered component without stored attributes', () => {
      ComponentRegistry.clearAll();
      const state = {grid: gridComponent()};

      const restored = restoreValidationColumn(state, columnAddress);

      expect(restored.grid.attributes.columnModel[0].validationRules).toBeUndefined();
    });

    it('keeps the other columns when restoring the rules of one', () => {
      ComponentRegistry.clearAll();
      const state = {
        grid: {
          attributes: {columnModel: [{name: 'amount', validationRules: {min: 1}}, {name: 'label', validationRules: {max: 2}}]},
          storedAttributes: {columnModel: [{validationRules: {required: true}}, {}]}
        }
      };

      const restored = restoreValidationColumn(state, {...address, column: 'label'});

      expect(restored.grid.attributes.columnModel[0].validationRules).toEqual({min: 1});
      expect(restored.grid.attributes.columnModel[1].validationRules).toBeUndefined();
    });

    it('keeps the other cell attributes when restoring the rules of a cell', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {
        ...gridComponent(),
        model: {values: [{id: 2, $attrs: {amount: {hidden: true, validationRules: {min: 1}}, label: {readonly: true}}}]}
      });

      const restored = restoreValidationCell({}, cellAddress);
      const row = getRow(restored, 0);

      expect(row.$attrs.amount).toEqual({hidden: true});
      expect(row.$attrs.label).toEqual({readonly: true});
    });

    it('ignores the restore of a cell without rules in a row that has other attributes', () => {
      ComponentRegistry.clearAll();
      ComponentRegistry.register('grid', {
        ...gridComponent(),
        model: {values: [{id: 2, $attrs: {label: {readonly: true}}}]}
      });
      const state = {};

      expect(restoreValidationCell(state, cellAddress)).toBe(state);
    });
  });
});
