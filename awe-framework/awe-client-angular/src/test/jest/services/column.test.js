import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe("Column", () => {
  let $control;
  let Column;

  const address = {component: "grid", view: "report", row: "2", column: "value"};

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $control = $injector.get("Control");
      Column = $injector.get("Column");
    }]);
  });

  function createColumnFixture() {
    const gridComponent = {
      model: {selected: []},
      scope: {onSaveRow: jest.fn()},
      checkInitialized: jest.fn(() => false),
      getModel: jest.fn(() => ({values: [], selected: null})),
      getController: jest.fn(() => ({dependencies: [], align: "left"})),
      updateCellModel: jest.fn()
    };
    const component = {
      controller: {},
      model: {},
      scope: {$on: jest.fn(() => jest.fn())},
      col: {grid: {appScope: {component: gridComponent}}},
      api: {updateComponentModelValues: jest.fn()},
      updateVisibleValue: jest.fn()
    };
    const attributes = {
      cellAddress: JSON.stringify(address),
      $observe: jest.fn(() => jest.fn())
    };

    return {component, gridComponent, attributes};
  }

  it("generates a column from the serialized cell address", () => {
    const {attributes} = createColumnFixture();
    const column = new Column(attributes);

    expect(column.attributes).toBe(attributes);
    expect(column.id).toBe("grid-value-2");
    expect(column.address).toEqual(address);
  });

  it("initializes a cell component from the parent grid component", () => {
    const {component, gridComponent, attributes} = createColumnFixture();
    const column = new Column(attributes);
    jest.spyOn($control, "setAddressApi");

    column.init(component);

    expect(gridComponent.checkInitialized).toHaveBeenCalledWith(address);
    expect(gridComponent.getModel).toHaveBeenCalledWith(address);
    expect(gridComponent.getController).toHaveBeenCalledWith(address);
    expect(component.scope.model).toBe(component.model);
    expect(component.scope.controller).toBe(component.controller);
    expect(attributes.$observe).toHaveBeenCalledWith("cellAddress", expect.any(Function));
  });

  it("normalizes edited values, updates visible value, and publishes grid model changes", () => {
    const {component, gridComponent, attributes} = createColumnFixture();
    const column = new Column(attributes);
    jest.spyOn($control, "setAddressApi");
    jest.spyOn($control, "publishModelChanged");
    column.init(component);

    component.model.selected = "aaa";
    component.columnModelChange();
    expect(component.model.values).toEqual([{value: "aaa", label: "aaa"}]);

    component.model.selected = "";
    component.columnModelChange();
    expect(component.model.values).toEqual([{value: "", label: ""}]);

    component.model.selected = null;
    component.columnModelChange();
    expect(component.model.values).toEqual([{value: null, label: ""}]);
    expect(component.updateVisibleValue).toHaveBeenCalledTimes(3);
    expect(gridComponent.updateCellModel).toHaveBeenCalledWith(component);
    expect($control.publishModelChanged).toHaveBeenLastCalledWith(component.address, {selected: null});
  });
});
