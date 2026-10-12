import {
  extractCellLabels,
  extractCellValues,
  filterRow,
  getCell,
  getCellAttribute,
  getCellModel,
  getDirection,
  getGridData,
  getWidthStyle,
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
    expect(getWidthStyle(undefined, 200)).toEqual({flex: '0 0 230px', width: "230px"});
  });

  it('should get width style in ch', () => {
    expect(getWidthStyle(3)).toEqual({flex: '0 0 56px', width: "56px"});
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
});
