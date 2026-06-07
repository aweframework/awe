import "./gridTestUtils";

describe("GridMultioperation", () => {
  let GridMultioperation;
  let Control;

  const getDefaultMultioperationComponent = () => ({
    constants: {ROW_IDENTIFIER: "id", CELL_VALUE: "value", CELL_LABEL: "label", CELL_TITLE: "title", CELL_STYLE: "style", CELL_ICON: "icon", CELL_IMAGE: "image", ROW_CLASS_FIELD: "RowCls"},
    controller: {columnModel: []},
    enableSorting: true,
    scope: {$on: jest.fn(), charSize: 7, gridOptions: {}},
    listeners: {},
    api: {},
    model: {records: 6, total: 1, page: 1, values: [{id: 1, value: "tutu", RowTyp: "INSERT"}, {id: 2, value: "lala"}, {id: 4, value: "lele", other: "asda", RowTyp: "UPDATE"}, {id: 5, value: "lili"}, {id: 6, value: "lolo", RowTyp: "DELETE"}, {id: 7, value: "lulu"}], selected: [4]},
    address: {view: "viewId", component: "componentId"},
    resetSelection: jest.fn(),
    selectRow: jest.fn(),
    storeEvent: jest.fn(),
    attributeMethods: {},
    getSelectedRows: jest.fn(() => [4]),
    hideContextMenu: jest.fn(),
    deleteRowCells: jest.fn(),
    deleteRowSpecific: jest.fn(),
    addRowStyle: jest.fn()
  });

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      GridMultioperation = $injector.get("GridMultioperation");
      Control = $injector.get("Control");
    }]);
  });

  function initMultioperation(component = getDefaultMultioperationComponent()) {
    const multioperation = new GridMultioperation(component);
    jest.spyOn(component.gridEditable, "init").mockReturnValue(true);
    expect(multioperation.init()).toBe(true);
    return component;
  }

  it("physically deletes inserted rows and marks persisted rows as deleted", () => {
    const component = initMultioperation();
    jest.spyOn(Control, "changeModelAttribute").mockImplementation(jest.fn());

    component.deleteRow(1);
    component.deleteRow(4);

    expect(component.hideContextMenu).toHaveBeenCalledTimes(2);
    expect(component.deleteRowCells).toHaveBeenCalledWith(1);
    expect(component.deleteRowSpecific).toHaveBeenCalledWith(1);
    expect(component.model.values[2].RowTyp).toBe("DELETE");
    expect(component.model.values[2].RowIco).toEqual({value: "DELETE", label: "Deleted", icon: "fa-trash"});
    expect(component.addRowStyle).toHaveBeenCalledWith(4, "DELETE");
    expect(Control.changeModelAttribute).toHaveBeenCalledWith({view: "viewId", component: "componentId", column: "RowIco", row: 4}, {values: [{value: "DELETE", label: "Deleted", icon: "fa-trash"}], selected: {value: "DELETE", label: "Deleted", icon: "fa-trash"}});
  });

  it("exports changed row identifiers and only includes selected row address for one selection", () => {
    const component = initMultioperation();

    expect(component.getIdentifierColumnData()).toEqual({"componentId-id": [1, 4, 6], "componentId.selectedRowAddress": {view: "viewId", component: "componentId", row: 4}});

    component.model.selected = [1, 4];

    expect(component.getIdentifierColumnData()).toEqual({"componentId-id": [1, 4, 6]});
  });

  it("initializes the multioperation grid through the editable base", () => {
    const component = getDefaultMultioperationComponent();
    const multioperation = new GridMultioperation(component);
    jest.spyOn(component.gridEditable, "init").mockReturnValue(true);

    expect(multioperation.init()).toBe(true);
    expect(component.gridEditable.init).toHaveBeenCalled();
  });

  it("deletes the currently selected row when no explicit row is provided", () => {
    const component = initMultioperation();
    jest.spyOn(Control, "changeModelAttribute").mockImplementation(jest.fn());

    component.deleteRow();

    expect(component.hideContextMenu).toHaveBeenCalled();
    expect(component.addRowStyle).toHaveBeenCalledWith(4, "DELETE");
  });

  it("ignores delete requests for rows that are not present", () => {
    const component = initMultioperation();
    jest.spyOn(Control, "changeModelAttribute").mockImplementation(jest.fn());

    component.deleteRow(99);

    expect(component.deleteRowCells).not.toHaveBeenCalled();
    expect(component.addRowStyle).not.toHaveBeenCalled();
  });

  it("includes selectedRowAddress when exactly one row is selected", () => {
    const component = initMultioperation();
    component.model.selected = [4];

    expect(component.getIdentifierColumnData()["componentId.selectedRowAddress"]).toEqual({view: "viewId", component: "componentId", row: 4});
  });

  it("omits selectedRowAddress when multiple rows are selected", () => {
    const component = initMultioperation();
    component.model.selected = [1, 4];

    expect(component.getIdentifierColumnData()["componentId.selectedRowAddress"]).toBeUndefined();
  });

  it("omits selectedRowAddress when no rows are selected", () => {
    const component = initMultioperation();
    component.model.selected = [];

    expect(component.getIdentifierColumnData()["componentId.selectedRowAddress"]).toBeUndefined();
  });
});
