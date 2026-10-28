import {
  extractCellLabels,
  extractCellValues,
  filterRow,
  getCell,
  getCellAttribute,
  getCellModel,
  getDirection,
  getGridData,
  getGridPrintData,
  getMoveRowKeyPosition,
  getWidthStyle,
  moveRowInValues,
  moveRowNextToRow,
  RowPositionType,
  sortRow
} from '../../../src/utilities/grid';

describe('awe-react-client/test/js/utilities/gridTest.jsx', () => {
  it('should filter a row with includes as true', () => {
    expect(filterRow({"col1": "aaaasada", "col2": 3}, {col1: {matchMode: "includes", value: "as"}})).toBe(true);
  });

  it('should filter a row with includes as false', () => {
    expect(filterRow({"col1": "aaaasada", "col2": 3}, {col1: {matchMode: "includes", value: "bbb"}})).toBe(false);
  });

  it('should filter a row with startsWith as true', () => {
    expect(filterRow({"col1": "aaaasada", "col2": 3}, {col1: {matchMode: "startsWith", value: "aa"}})).toBe(true);
  });

  it('should filter a row with startsWith as false', () => {
    expect(filterRow({"col1": "aaaasada", "col2": 3}, {col1: {matchMode: "startsWith", value: "bbb"}})).toBe(false);
  });

  it('should filter a row with endsWith as true', () => {
    expect(filterRow({"col1": "aaaasada", "col2": 3}, {col1: {matchMode: "endsWith", value: "da"}})).toBe(true);

  });

  it('should filter a row with endsWith as false', () => {
    expect(filterRow({"col1": "aaaasada", "col2": 3}, {col1: {matchMode: "endsWith", value: "bbb"}})).toBe(false);
  });

  it('should filter a row with some columns as true', () => {
    expect(filterRow({"col1": "aaaasada", "col2": "aadasfe"},
      {
        col1: {matchMode: "endsWith", value: "da"},
        col2: {matchMode: "includes", value: "dasfe"}
      })).toBe(true);

  });

  it('should filter a row with some columns as false', () => {
    expect(filterRow({"col1": "aaaasada", "col2": "vsacfvs"}, {
      col1: {matchMode: "endsWith", value: "das"},
      col2: {matchMode: "includes", value: "dasfe"}
    })).toBe(false);
  });

  it('should sort a row asc', () => {
    expect(sortRow({"col1": "aaa", "col2": 3}, {"col1": "bbb", "col2": 1},
      [{id: "col1", direction: "asc", order: 1}])).toBe(-1);
  });

  it('should sort a row desc', () => {
    expect(sortRow({"col1": "aaa", "col2": 3}, {"col1": "bbb", "col2": 1},
      [{id: "col1", direction: "desc", order: -1}])).toBe(1);
  });

  it('should sort a row equals', () => {
    expect(sortRow({"col1": "aaa", "col2": 5}, {"col1": "aaa", "col2": 5},
      [{id: "col1", direction: "asc", order: 1}])).toBe(0);
  });

  it('should sort a row asc with two sorters', () => {
    expect(sortRow({"col1": "aaa", "col2": 3}, {"col1": "bbb", "col2": 1},
      [{id: "col1", direction: "asc", order: 1}, {id: "col2", direction: "asc", order: 1}])).toBe(-1);
  });

  it('should sort a row desc with two sorters', () => {
    expect(sortRow({"col1": "aaa", "col2": 3}, {"col1": "aaa", "col2": 1},
      [{id: "col1", direction: "desc", order: -1}, {id: "col2", direction: "desc", order: -1}])).toBe(-1);
  });

  it('should sort a row equals with two sorters', () => {
    expect(sortRow({"col1": "aaa", "col2": 5}, {"col1": "aaa", "col2": 5},
      [{id: "col1", direction: "asc", order: 1}, {id: "col2", direction: "desc", order: -1}])).toBe(0);
  });

  it('should get width style in px', () => {
    expect(getWidthStyle(undefined, 200)).toEqual({flex: '0 0 230px', width: "230px", minWidth: "230px"});
  });

  it('should get width style in ch', () => {
    expect(getWidthStyle(3)).toEqual({flex: '0 0 56px', width: "56px", minWidth: "56px"});
  });

  it('keeps a column at least as wide as its width, so a grid scrolls instead of squeezing its columns', () => {
    expect(getWidthStyle(null, null, "32px")).toEqual({flex: '0 0 32px', width: "32px", minWidth: "32px"});
  });

  it('should get width style empty', () => {
    expect(getWidthStyle()).toEqual({});
  });

  it('should get sort direction', () => {
    expect(getDirection("asc")).toBe(1);
    expect(getDirection("desc")).toBe(-1);
  });

  it('should get cell attribute', () => {
    expect(getCellAttribute({value: "value"}, "value")).toBe("value");
    expect(getCellAttribute({icon: "value"}, "icon")).toBe("value");
    expect(getCellAttribute("value", "value")).toBe("value");
    expect(getCellAttribute(null, "value")).toBe(null);
  });

  it('should get cell', () => {
    const values = [
      {id: "row1", value: "value1"},
      {id: "row2", value: "value2"},
      {id: "row3", value: "value3"}
    ];
    expect(getCell(values, 1, "value")).toBe("value2");
    expect(getCell(values, 0, "id")).toBe("row1");
    expect(getCell(values, 2, "id")).toBe("row3");
    expect(getCell(values, -1, "value")).toBe(null);
  });

  it('should get cell model', () => {
    const colModel = {
      model: {
        values: [
          {id: "row1", value: "value1", label: "label1"},
          {id: "row2", value: "value2", label: "label2"},
          {id: "row3", value: "value3", label: "label3"}
        ]
      }
    };
    expect(getCellModel("test", null)).toEqual({value: "test", label: "test"});
    expect(getCellModel([{value:"1"}, {value: "2", selected: true}, {value: "3"}], colModel)).toEqual({value: "2", selected: true});
    expect(getCellModel([{value:"1"}, {value: "2"}, {value: "3"}], null)).toEqual({value: null});
    expect(getCellModel({value:"1", label: "test"}, null)).toEqual({value: "1", label: "test"});
    expect(getCellModel("value1", colModel)).toEqual( {id: "row1", value: "value1", label: "label1"});
  });

  it('should extract cell values for multiple cells', () => {
    expect(extractCellValues([
      {value: "1", selected: true},
      {value: "2", selected: true}
    ])).toEqual(["1", "2"]);
    expect(extractCellValues({value: "x"})).toBe("x");
  });

  it('should extract cell labels for multiple cells', () => {
    expect(extractCellLabels([
      {value: "1", label: "One", selected: true},
      {value: "2", label: "Two", selected: true}
    ])).toBe("One, Two");
    expect(extractCellLabels({value: "x", label: "Ex"})).toBe("Ex");
  });

  it('should return list values in getGridData for multiple columns', () => {
    const grid = {
      address: {component: "grid", view: "base"},
      attributes: {
        id: "id",
        sendAll: true,
        columnModel: [
          {name: "tags", sendable: true, component: "select-multiple"}
        ]
      }
    };
    const model = {
      values: [
        {id: 1, tags: [{value: "a", selected: true}, {value: "b", selected: true}]}
      ]
    };
    const data = getGridData(grid, model, {}, false);
    expect(data.tags).toEqual([["a", "b"]]);
  });

  describe('moveRowInValues', () => {
    const rows = () => [{id: 1}, {id: 2}, {id: 3}, {id: 4}];
    const ids = (values) => values.map(row => row.id);

    it('moves a row to the top', () => {
      expect(ids(moveRowInValues(rows(), "id", 3, RowPositionType.FIRST))).toEqual([3, 1, 2, 4]);
    });

    it('moves a row one position up', () => {
      expect(ids(moveRowInValues(rows(), "id", 3, RowPositionType.BEFORE))).toEqual([1, 3, 2, 4]);
    });

    it('moves a row one position down', () => {
      expect(ids(moveRowInValues(rows(), "id", 2, RowPositionType.AFTER))).toEqual([1, 3, 2, 4]);
    });

    it('moves a row to the bottom', () => {
      expect(ids(moveRowInValues(rows(), "id", 2, RowPositionType.LAST))).toEqual([1, 3, 4, 2]);
    });

    it('compares the identifiers as text', () => {
      expect(ids(moveRowInValues(rows(), "id", "4", RowPositionType.FIRST))).toEqual([4, 1, 2, 3]);
    });

    it.each([
      [1, RowPositionType.FIRST],
      [1, RowPositionType.BEFORE],
      [4, RowPositionType.AFTER],
      [4, RowPositionType.LAST],
      [99, RowPositionType.FIRST],
      [null, RowPositionType.FIRST]
    ])('keeps the same list when the row %s cannot move %s', (rowId, position) => {
      const values = rows();
      expect(moveRowInValues(values, "id", rowId, position)).toBe(values);
    });

    it('does not modify the original list', () => {
      const values = rows();
      moveRowInValues(values, "id", 3, RowPositionType.FIRST);
      expect(ids(values)).toEqual([1, 2, 3, 4]);
    });
  });

  describe('moveRowNextToRow', () => {
    const rows = () => [{id: 1}, {id: 2}, {id: 3}, {id: 4}];
    const ids = (values) => values.map(row => row.id);

    it.each([
      [1, 3, RowPositionType.BEFORE, [2, 1, 3, 4]],
      [1, 3, RowPositionType.AFTER, [2, 3, 1, 4]],
      [4, 2, RowPositionType.BEFORE, [1, 4, 2, 3]],
      [4, 1, RowPositionType.BEFORE, [4, 1, 2, 3]],
      [2, 4, RowPositionType.AFTER, [1, 3, 4, 2]]
    ])('moves row %s next to row %s (%s)', (rowId, targetRowId, position, expected) => {
      expect(ids(moveRowNextToRow(rows(), "id", rowId, targetRowId, position))).toEqual(expected);
    });

    it('compares the identifiers as text', () => {
      expect(ids(moveRowNextToRow(rows(), "id", "4", "1", RowPositionType.BEFORE))).toEqual([4, 1, 2, 3]);
    });

    it.each([
      [2, 2, RowPositionType.BEFORE],
      [2, 3, RowPositionType.BEFORE],
      [3, 2, RowPositionType.AFTER],
      [99, 2, RowPositionType.BEFORE],
      [2, 99, RowPositionType.BEFORE],
      [null, 2, RowPositionType.BEFORE],
      [2, null, RowPositionType.AFTER],
      [2, 3, RowPositionType.FIRST]
    ])('keeps the same list when moving row %s next to row %s (%s) changes nothing', (rowId, targetRowId, position) => {
      const values = rows();
      expect(moveRowNextToRow(values, "id", rowId, targetRowId, position)).toBe(values);
    });

    it('does not modify the original list', () => {
      const values = rows();
      moveRowNextToRow(values, "id", 4, 1, RowPositionType.BEFORE);
      expect(ids(values)).toEqual([1, 2, 3, 4]);
    });
  });

  describe('getMoveRowKeyPosition', () => {
    it.each([
      [{key: "ArrowUp", altKey: true}, RowPositionType.BEFORE],
      [{key: "ArrowDown", altKey: true}, RowPositionType.AFTER],
      [{key: "Home", altKey: true}, RowPositionType.FIRST],
      [{key: "End", altKey: true}, RowPositionType.LAST]
    ])('maps %j to %s', (event, position) => {
      expect(getMoveRowKeyPosition(event)).toBe(position);
    });

    it.each([
      [{key: "ArrowUp"}],
      [{key: "ArrowUp", altKey: true, ctrlKey: true}],
      [{key: "ArrowUp", altKey: true, shiftKey: true}],
      [{key: "ArrowUp", altKey: true, metaKey: true}],
      [{key: "Enter", altKey: true}]
    ])('ignores %j', (event) => {
      expect(getMoveRowKeyPosition(event)).toBeNull();
    });
  });

  describe('radio and button-radio columns', () => {
    const values = [{value: 'A', label: 'Option A'}, {value: 'B', label: 'Option B'}, {value: 'C', label: 'Option C'}];
    const columnModel = [
      {name: 'name', sendable: true},
      {name: 'choice', component: 'radio', sendable: true, model: {values}},
      {name: 'mode', component: 'button-radio', sendable: true, model: {values}}
    ];
    const grid = {address: {component: 'grid', view: 'base'}, attributes: {id: 'id', sendAll: true, columnModel}};
    const model = {
      values: [
        {id: 1, name: 'one', choice: {value: 'B', label: 'Option B'}, mode: {value: 'A'}},
        {id: 2, name: 'two', choice: {value: null}},
        {id: 3, name: 'three', choice: 'C', mode: {value: 'C'}}
      ]
    };

    it('should send the value of every row like a select column does', () => {
      const data = getGridData(grid, model, {}, false);

      expect(data.choice).toEqual(['B', null, 'C']);
      expect(data.mode).toEqual(['A', undefined, 'C']);
      expect(data.name).toEqual(['one', 'two', 'three']);
    });

    it('should send the choice of the selected rows as the selected value of the column', () => {
      const selectedModel = {values: model.values.map(row => ({...row, selected: row.id === 3}))};

      const data = getGridData(grid, selectedModel, {}, false);

      expect(data['choice.selected']).toBe('C');
    });

    it('should send the choice of the editing rows of an editable grid as the editing value of the column', () => {
      const editable = {...grid, attributes: {...grid.attributes, editable: true}};
      const editingModel = {values: model.values.map(row => ({...row, $row: {editing: row.id === 3}}))};

      const data = getGridData(editable, editingModel, {}, false);

      expect(data['choice.editing']).toBe('C');
      expect(data['mode.editing']).toBe('C');
    });

    it('should not send the editing value when the grid is not editable', () => {
      expect(getGridData(grid, model, {}, false)).not.toHaveProperty('choice.editing');
    });

    it('should send only the sendable columns', () => {
      const notSendable = {...grid, attributes: {...grid.attributes, columnModel: columnModel.map(column => ({...column, sendable: false}))}};

      expect(getGridData(notSendable, model, {}, false)).not.toHaveProperty('choice');
    });
  });
  describe('getGridPrintData visible columns', () => {
    const printColumns = (columns) => {
      const grid = {attributes: {id: 'Grd', columnModel: columns}};
      const t = (key) => `t(${key})`;
      return getGridPrintData(grid, {values: []}, {t})['Grd.data'].visibleColumns;
    };

    it('should send the secondary field, its export flag and its translated label when the column has them', () => {
      const [column] = printColumns([{name: 'name', label: 'NAME', secondaryField: 'email', exportSecondary: true, secondaryLabel: 'EMAIL'}]);

      expect(column).toMatchObject({name: 'name', label: 't(NAME)', secondaryField: 'email', exportSecondary: true, secondaryLabel: 't(EMAIL)'});
    });

    it('should send a null secondary label when the column has no label for it', () => {
      const [column] = printColumns([{name: 'name', label: 'NAME', secondaryField: 'email', exportSecondary: true}]);

      expect(column.secondaryLabel).toBeNull();
    });

    it('should not send the secondary attributes of a column without them', () => {
      const [column] = printColumns([{name: 'name', label: 'NAME', align: 'left'}]);

      expect(column).not.toHaveProperty('secondaryField');
      expect(column).not.toHaveProperty('exportSecondary');
      expect(column).not.toHaveProperty('secondaryLabel');
      expect(Object.keys(column).sort()).toEqual(['align', 'charlength', 'component', 'label', 'name', 'type', 'width']);
    });
  });
});
