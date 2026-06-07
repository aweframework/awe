import "./gridTestUtils";

describe("GridComponents", () => {
  let GridComponents;
  let Control;
  let AweUtilities;

  const getDefaultGridComponent = () => ({
    id: "gridId",
    address: {view: "view1", component: "gridId"},
    constants: {ROW_IDENTIFIER: "id"},
    controller: {},
    model: {values: [{id: 1, colA: "foo"}, {id: 2, colA: "bar"}], footer: {}},
    api: {},
    columnModelStringified: {},
    getCellObject: value => value,
    getColumnValueList: jest.fn(() => []),
    getColumn: jest.fn(() => ({})),
    getColumns: jest.fn(() => [{name: "colA"}, {name: "colB"}]),
    getSelectedRow: jest.fn(() => 2),
    deleteRowSpecific: jest.fn()
  });

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      GridComponents = $injector.get("GridComponents");
      Control = $injector.get("Control");
      AweUtilities = $injector.get("AweUtilities");
    }]);
  });

  function initGridComponents(component = getDefaultGridComponent()) {
    const grid = new GridComponents(component);
    expect(grid.init()).toBe(true);
    return component;
  }

  it("updates body cell values and publishes the changed grid model", () => {
    const component = initGridComponents();
    jest.spyOn(Control, "publishModelChanged").mockImplementation(jest.fn());

    component.updateCellModel({address: {component: "gridId", row: 2, column: "colA"}, model: {selected: "changed"}});

    expect(component.model.values[1].colA).toBe("changed");
    expect(Control.publishModelChanged).toHaveBeenCalledWith(component.address, {values: component.model.values});
  });

  it("leaves footer/missing rows untouched and removes cached cell state when deleting a row", () => {
    const component = initGridComponents();
    jest.spyOn(Control, "publishModelChanged").mockImplementation(jest.fn());
    jest.spyOn(AweUtilities, "getRowIndex").mockReturnValue(-1);
    component.model.cells = {"gridId-colA-2": {selected: "A"}, "gridId-colB-2": {selected: "B"}};
    component.controller.cells = {"gridId-colA-2": {label: "A"}, "gridId-colB-2": {label: "B"}};
    component.api.cells = {"gridId-colA-2": {refresh: jest.fn()}, "gridId-colB-2": {refresh: jest.fn()}};

    component.updateCellModel({address: {component: "gridId", row: "footer", column: "colA"}, model: {selected: "ignored"}});
    component.deleteRow();

    expect(component.model.values[0].colA).toBe("foo");
    expect(Control.publishModelChanged).not.toHaveBeenCalled();
    expect(component.model.cells).toEqual({});
    expect(component.controller.cells).toEqual({});
    expect(component.api.cells).toEqual({});
    expect(component.deleteRowSpecific).toHaveBeenCalledWith(2);
  });
});
