import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {
  keepAttributeComponent,
  mergeColumnNumberFormat,
  restoreAttributeAction,
  restoreAttributeCell,
  restoreAttributeColumn,
  restoreAttributeComponent,
  updateAttributeAction,
  updateAttributeCell,
  updateAttributeColumn,
  updateAttributeComponent,
  updateSpecificAttributes
} from '../../../../../src/redux/reducers/components/attributes';
import {getMergedComponent} from '../../../../../src/redux/reducers/components/stateHelpers';

const address = {view: 'base', component: 'grid'};
const columnAddress = {...address, column: 'amount'};
const cellAddress = {...columnAddress, row: 2};

const gridComponent = () => ({
  address,
  attributes: {
    showTotals: true,
    title: 'Grid',
    columnModel: [
      {name: 'amount', summaryType: 'sum', width: 10, numberFormat: {decimals: 2, sign: '$'}},
      {name: 'label'}
    ]
  },
  model: {values: [{id: 1, amount: 2, label: 'a'}, {id: 2, amount: 3, label: 'b'}]}
});

describe('awe-react-client/test/js/redux/reducers/components/attributesTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
    ComponentRegistry.register('grid', gridComponent());
  });

  describe('mergeColumnNumberFormat', () => {
    it('keeps the current separators when only some keys change', () => {
      const result = mergeColumnNumberFormat({numberFormat: {decimals: 2, sign: '$'}}, {numberFormat: {decimals: 4}});

      expect(result.numberFormat).toEqual({decimals: 4, sign: '$'});
    });

    it('returns the data untouched without a number format', () => {
      const data = {width: 5};

      expect(mergeColumnNumberFormat({}, data)).toBe(data);
    });
  });

  describe('update attributes', () => {
    it('updates a component attribute', () => {
      const state = updateAttributeComponent({}, address, {title: 'New'});

      expect(getMergedComponent('grid', state).attributes.title).toBe('New');
    });

    it('updates a column attribute and merges the number format', () => {
      const state = updateAttributeColumn({}, columnAddress, {width: 20, numberFormat: {decimals: 4}});
      const column = getMergedComponent('grid', state).attributes.columnModel[0];

      expect(column.width).toBe(20);
      expect(column.numberFormat).toEqual({decimals: 4, sign: '$'});
    });

    it('updates a column attribute of an unregistered component directly on the state', () => {
      ComponentRegistry.clearAll();
      const state = updateAttributeColumn({grid: gridComponent()}, columnAddress, {width: 30});

      expect(state.grid.attributes.columnModel[0].width).toBe(30);
    });

    it('updates a cell attribute in the row $attrs', () => {
      const state = updateAttributeCell({}, cellAddress, {readonly: true});
      const row = getMergedComponent('grid', state).model.values[1];

      expect(row.$attrs.amount.readonly).toBe(true);
    });

    it('ignores a cell of an unknown row', () => {
      const state = {};

      expect(updateAttributeCell(state, {...cellAddress, row: 99}, {readonly: true})).toBe(state);
    });

    it('dispatches by address type', () => {
      const state = updateAttributeAction({}, {address: columnAddress, data: {width: 99}});

      expect(getMergedComponent('grid', state).attributes.columnModel[0].width).toBe(99);
    });

    it('ignores an action with an invalid address', () => {
      const state = {};

      expect(updateAttributeAction(state, {address: {}, data: {width: 1}})).toBe(state);
    });
  });

  describe('updateSpecificAttributes', () => {
    it('merges the specific attributes of a registered component', () => {
      let state = updateSpecificAttributes({}, {address, data: {a: 1}});
      state = updateSpecificAttributes(state, {address, data: {b: 2}});

      expect(getMergedComponent('grid', state).specificAttributes).toEqual({a: 1, b: 2});
    });

    it('does nothing for an unknown unregistered component', () => {
      ComponentRegistry.clearAll();
      const state = {};

      expect(updateSpecificAttributes(state, {address, data: {a: 1}})).toBe(state);
    });
  });

  describe('keep and restore', () => {
    it('restores a component attribute to the kept (object) value', () => {
      const kept = keepAttributeComponent({grid: {attributes: {style: {color: 'red'}}}}, 'grid', 'style');
      const changed = updateAttributeComponent(kept, address, {style: {color: 'blue'}});
      const restored = restoreAttributeComponent(changed, address, 'style');

      expect(getMergedComponent('grid', changed).attributes.style).toEqual({color: 'blue'});
      expect(getMergedComponent('grid', restored).attributes.style).toEqual({color: 'red'});
    });

    it('does not keep an attribute of an unknown component', () => {
      const state = {};

      expect(keepAttributeComponent(state, 'missing', 'title')).toBe(state);
    });

    it('restores a column attribute from the stored column model', () => {
      const stored = {grid: {storedAttributes: {columnModel: [{width: 10}]}}};
      const changed = updateAttributeColumn(stored, columnAddress, {width: 50});
      const restored = restoreAttributeColumn(changed, columnAddress, 'width');

      expect(getMergedComponent('grid', changed).attributes.columnModel[0].width).toBe(50);
      expect(getMergedComponent('grid', restored).attributes.columnModel[0].width).toBe(10);
    });

    it('ignores the restore of an unknown column', () => {
      const state = {};

      expect(restoreAttributeColumn(state, {...columnAddress, column: 'nope'}, 'width')).toBe(state);
    });

    it('restores a cell attribute from the stored column or removes it', () => {
      const stored = {grid: {storedAttributes: {columnModel: [{readonly: false}]}}};
      const changed = updateAttributeCell(stored, cellAddress, {readonly: true, hidden: true});
      const restored = restoreAttributeCell(changed, cellAddress, 'readonly');
      const removed = restoreAttributeCell(restored, cellAddress, 'hidden');

      expect(getMergedComponent('grid', restored).model.values[1].$attrs.amount).toEqual({readonly: false, hidden: true});
      expect(getMergedComponent('grid', removed).model.values[1].$attrs.amount).toEqual({readonly: false});
    });

    it('restores a cell attribute of an unregistered component directly on the state', () => {
      ComponentRegistry.clearAll();
      const component = {
        ...gridComponent(),
        storedAttributes: {columnModel: [{readonly: false}]},
        model: {values: [{id: 2, amount: 3, $attrs: {amount: {readonly: true}}}]}
      };
      const state = restoreAttributeCell({grid: component}, cellAddress, 'readonly');

      expect(state.grid.model.values[0].$attrs.amount.readonly).toBe(false);
    });

    it('dispatches the restore by address type', () => {
      const stored = {grid: {storedAttributes: {columnModel: [{width: 10}]}}};
      const changed = updateAttributeColumn(stored, columnAddress, {width: 50});
      const restored = restoreAttributeAction(changed, {address: columnAddress, data: 'width'});

      expect(getMergedComponent('grid', restored).attributes.columnModel[0].width).toBe(10);
    });
  });

  describe('footer refresh', () => {
    it('regenerates the footer after updating an attribute of a grid with totals', () => {
      const state = updateAttributeAction({}, {address: columnAddress, data: {summaryType: 'max'}});

      expect(getMergedComponent('grid', state).model.footer.amount.value).toBe(3);
    });
  });

  describe('calls that omit the optional arguments', () => {
    it('leave an empty state untouched', () => {
      expect(updateAttributeComponent()).toEqual({});
      expect(updateAttributeColumn()).toEqual({});
      expect(updateAttributeCell()).toEqual({});
      expect(updateAttributeAction()).toEqual({});
      expect(updateSpecificAttributes()).toEqual({});
      expect(restoreAttributeColumn()).toEqual({});
      expect(restoreAttributeAction()).toEqual({});
    });

    it('merge a column number format when the column has none', () => {
      expect(mergeColumnNumberFormat(undefined, {numberFormat: {decimals: 1}})).toEqual({numberFormat: {decimals: 1}});
      expect(mergeColumnNumberFormat({}, {numberFormat: {sign: '%'}}).numberFormat).toEqual({sign: '%'});
      expect(mergeColumnNumberFormat(undefined, undefined)).toEqual({});
    });

    it('do not treat a null or non-object number format as an update', () => {
      const nullFormat = {numberFormat: null};
      const textFormat = {numberFormat: '0.00'};

      expect(mergeColumnNumberFormat({numberFormat: {decimals: 2}}, nullFormat)).toBe(nullFormat);
      expect(mergeColumnNumberFormat({numberFormat: {decimals: 2}}, textFormat)).toBe(textFormat);
    });
  });

  describe('update attributes of unregistered or incomplete components', () => {
    beforeEach(() => {
      ComponentRegistry.clearAll();
    });

    it('ignores a column of a component without state entry or attributes', () => {
      const missing = {};
      const noAttributes = {grid: {model: {}}};

      expect(updateAttributeColumn(missing, columnAddress, {width: 1})).toBe(missing);
      expect(updateAttributeColumn(noAttributes, columnAddress, {width: 1})).toBe(noAttributes);
    });

    it('appends the column attributes when the column is not in the model', () => {
      const state = updateAttributeColumn({grid: {attributes: {title: 'x'}}}, columnAddress, {width: 5});

      expect(state.grid.attributes.title).toBe('x');
      expect(state.grid.attributes.columnModel).toEqual([{width: 5}]);
    });

    it('merges the number format of a column of an unregistered component', () => {
      const state = updateAttributeColumn({grid: gridComponent()}, columnAddress, {numberFormat: {decimals: 6}});

      expect(state.grid.attributes.columnModel[0].numberFormat).toEqual({decimals: 6, sign: '$'});
    });

    it('creates the column model of a registered component that has none', () => {
      ComponentRegistry.register('grid', {address, attributes: {title: 'Plain'}});
      const state = updateAttributeColumn({}, columnAddress, {width: 7});
      const attributes = getMergedComponent('grid', state).attributes;

      expect(attributes.title).toBe('Plain');
      expect(attributes.columnModel).toEqual([{width: 7}]);
    });

    it('updates the specific attributes of an unregistered component entry', () => {
      const state = updateSpecificAttributes({grid: {specificAttributes: {a: 1}}}, {address, data: {b: 2}});

      expect(state.grid.specificAttributes).toEqual({a: 1, b: 2});
    });

    it('creates the specific attributes of an unregistered component entry that has none', () => {
      const state = updateSpecificAttributes({grid: {}}, {address, data: {b: 2}});

      expect(state.grid.specificAttributes).toEqual({b: 2});
    });

    it('merges the specific attributes of a registered component with specific attributes', () => {
      ComponentRegistry.register('grid', {...gridComponent(), specificAttributes: {keep: true}});
      const state = updateSpecificAttributes({}, {address, data: {more: 1}});

      expect(getMergedComponent('grid', state).specificAttributes).toEqual({keep: true, more: 1});
    });

    it('updates a cell that already has attributes in its row', () => {
      const component = {
        ...gridComponent(),
        model: {values: [{id: 2, amount: 3, $attrs: {amount: {hidden: true}, label: {readonly: true}}}]}
      };
      ComponentRegistry.register('grid', component);
      const state = updateAttributeCell({}, cellAddress, {readonly: true});
      const row = getMergedComponent('grid', state).model.values[0];

      expect(row.$attrs.amount).toEqual({hidden: true, readonly: true});
      expect(row.$attrs.label).toEqual({readonly: true});
    });
  });

  describe('footer refresh paths', () => {
    it('keeps the state when the address resolves to no component id', () => {
      const state = {};

      expect(updateAttributeAction(state, {address: {view: 'base'}, data: {}})).toBe(state);
    });

    it('ignores an update of a component that is neither registered nor in the state', () => {
      ComponentRegistry.clearAll();
      const state = updateAttributeAction({}, {address, data: {title: 'x'}});

      expect(state).toEqual({});
    });

    it('does not touch the footer of a component that is not a grid', () => {
      const plainAddress = {view: 'base', component: 'plain'};
      ComponentRegistry.register('plain', {address: plainAddress, attributes: {title: 'a'}});
      const state = updateAttributeAction({}, {address: plainAddress, data: {title: 'b'}});

      expect(getMergedComponent('plain', state).attributes.title).toBe('b');
      expect(getMergedComponent('plain', state).model?.footer).toBeUndefined();
    });

    it('does not touch the footer of a grid without totals', () => {
      ComponentRegistry.register('grid', {...gridComponent(), attributes: {...gridComponent().attributes, showTotals: false}});
      const state = updateAttributeAction({}, {address, data: {title: 'b'}});

      expect(getMergedComponent('grid', state).model.footer).toBeUndefined();
    });

    it('regenerates the footer of an unregistered grid with totals', () => {
      ComponentRegistry.clearAll();
      const state = updateAttributeAction({grid: gridComponent()}, {address, data: {title: 'b'}});

      expect(state.grid.attributes.title).toBe('b');
      expect(state.grid.model.footer.amount.value).toBe(5);
    });
  });

  describe('restore attributes of registered components', () => {
    it('restores a component attribute that was never kept as undefined', () => {
      const restored = restoreAttributeComponent({}, address, 'title');

      expect(getMergedComponent('grid', restored).attributes.title).toBeUndefined();
    });

    it('ignores the restore of a column attribute of a registered grid without column model', () => {
      ComponentRegistry.register('grid', {address, attributes: {}});
      const state = {};

      expect(restoreAttributeColumn(state, columnAddress, 'width')).toBe(state);
    });

    it('restores a column attribute as undefined when nothing was stored', () => {
      const restored = restoreAttributeColumn({}, columnAddress, 'width');

      expect(getMergedComponent('grid', restored).attributes.columnModel[0].width).toBeUndefined();
    });

    it('ignores the restore of a cell of an unknown row', () => {
      const state = {};

      expect(restoreAttributeCell(state, {...cellAddress, row: 99}, 'readonly')).toBe(state);
    });

    it('removes the cell attribute when the column has no stored attributes', () => {
      const changed = updateAttributeCell({}, cellAddress, {readonly: true, hidden: true});
      const restored = restoreAttributeCell(changed, cellAddress, 'readonly');

      expect(getMergedComponent('grid', restored).model.values[1].$attrs.amount).toEqual({hidden: true});
    });

    it('removes the cell attribute of a row that has no cell attributes', () => {
      const restored = restoreAttributeCell({}, cellAddress, 'readonly');

      expect(getMergedComponent('grid', restored).model.values[1].$attrs).toEqual({amount: {}});
    });

    it('restores a cell of a grid without column model', () => {
      ComponentRegistry.register('grid', {address, attributes: {}, model: {values: [{id: 2}]}});
      const restored = restoreAttributeCell({}, cellAddress, 'readonly');

      expect(getMergedComponent('grid', restored).model.values[0].$attrs).toEqual({amount: {}});
    });

    it('restores a cell of a column that is not in the stored column model', () => {
      const stored = {grid: {storedAttributes: {columnModel: []}}};
      const changed = updateAttributeCell(stored, cellAddress, {readonly: true});
      const restored = restoreAttributeCell(changed, cellAddress, 'readonly');

      expect(getMergedComponent('grid', restored).model.values[1].$attrs.amount).toEqual({});
    });
  });

  describe('restore attributes of unregistered components', () => {
    beforeEach(() => {
      ComponentRegistry.clearAll();
    });

    it('restores a component attribute from the stored attributes', () => {
      const state = {grid: {attributes: {title: 'new'}, storedAttributes: {title: 'old'}}};

      expect(restoreAttributeComponent(state, address, 'title').grid.attributes.title).toBe('old');
    });

    it('restores a column attribute from the stored column model', () => {
      const state = {grid: {...gridComponent(), storedAttributes: {columnModel: [{width: 1}]}}};

      expect(restoreAttributeColumn(state, columnAddress, 'width').grid.attributes.columnModel[0].width).toBe(1);
    });

    it('ignores the restore of a column of a component without attributes', () => {
      const missing = {};
      const noAttributes = {grid: {}};

      expect(restoreAttributeColumn(missing, columnAddress, 'width')).toBe(missing);
      expect(restoreAttributeColumn(noAttributes, columnAddress, 'width')).toBe(noAttributes);
    });

    it('ignores the restore of a cell of a component without model or attributes', () => {
      const missing = {};
      const noModel = {grid: {attributes: {}}};
      const noAttributes = {grid: {model: {values: []}}};

      expect(restoreAttributeCell(missing, cellAddress, 'readonly')).toBe(missing);
      expect(restoreAttributeCell(noModel, cellAddress, 'readonly')).toBe(noModel);
      expect(restoreAttributeCell(noAttributes, cellAddress, 'readonly')).toBe(noAttributes);
    });

    it('ignores the restore of a cell of an unknown row', () => {
      const state = {grid: gridComponent()};

      expect(restoreAttributeCell(state, {...cellAddress, row: 99}, 'readonly')).toBe(state);
    });

    it('removes the cell attribute when no value was stored', () => {
      const component = {
        ...gridComponent(),
        model: {values: [{id: 2, $attrs: {amount: {readonly: true, hidden: true}}}]}
      };
      const state = restoreAttributeCell({grid: component}, cellAddress, 'readonly');

      expect(state.grid.model.values[0].$attrs.amount).toEqual({hidden: true});
    });

    it('removes the cell attribute of a row without cell attributes and a grid without column model', () => {
      const component = {attributes: {}, model: {values: [{id: 2}]}};
      const state = restoreAttributeCell({grid: component}, cellAddress, 'readonly');

      expect(state.grid.model.values[0].$attrs).toEqual({amount: {}});
    });

    it('restores a cell attribute from the stored column model', () => {
      const component = {
        ...gridComponent(),
        storedAttributes: {columnModel: [{readonly: false}]},
        model: {values: [{id: 2, $attrs: {amount: {readonly: true}}}]}
      };
      const state = restoreAttributeCell({grid: component}, cellAddress, 'readonly');

      expect(state.grid.model.values[0].$attrs.amount).toEqual({readonly: false});
    });
  });
});
