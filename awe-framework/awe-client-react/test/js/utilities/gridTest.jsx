import {
  filterRow,
  getCell,
  getCellAttribute,
  getCellModel,
  getDirection,
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

});
