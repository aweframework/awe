import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe("Control", () => {
  let $control;
  let $storage;
  let $utilities;
  let $log;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $control = $injector.get("Control");
      $storage = $injector.get("Storage");
      $utilities = $injector.get("AweUtilities");
      $log = $injector.get("$log");
    }]);
  });

  it("classifies address shapes by the fields required to resolve a target", () => {
    expect($control.getAddressType({component: "grid", view: "report", column: "Des", row: "2"})).toBe("cell");
    expect($control.getAddressType({component: "grid", view: "report"})).toBe("viewAndComponent");
    expect($control.getAddressType({component: "grid"})).toBe("component");
    expect($control.getAddressType({})).toBe("invalid");
  });

  it("resolves cell, view/component, and component-only targets from storage", () => {
    jest.spyOn($storage, "has").mockReturnValue(true);
    jest.spyOn($storage, "get").mockReturnValue({
      report: {
        grid: {cells: {"grid-Des-2": "cell-value"}},
        other: {id: "other"}
      }
    });
    jest.spyOn($utilities, "getCellId").mockReturnValue("grid-Des-2");

    expect($control.getTarget({component: "grid", view: "report", column: "Des", row: "2"}, "model")).toBe("cell-value");
    expect($control.getTarget({component: "grid", view: "report"}, "model")).toEqual({cells: {"grid-Des-2": "cell-value"}});
    expect($control.getTarget({component: "other"}, "model")).toEqual({id: "other"});
    expect($control.getTarget({}, "model")).toBeNull();
  });

  it("returns null when storage or the requested target is missing", () => {
    jest.spyOn($storage, "has").mockReturnValue(false);
    jest.spyOn($storage, "get");

    expect($control.getTarget({component: "grid", view: "report", column: "Des", row: "2"}, "model")).toBeNull();
    expect($control.getTarget({component: "grid", view: "report"}, "model")).toBeNull();
    expect($control.getTarget({component: "grid"}, "model")).toBeNull();
    expect($storage.get).not.toHaveBeenCalled();
  });

  it("sets cell and view/component targets while rejecting unsupported address shapes", () => {
    const storedModel = {report: {grid: {cells: {}}}};
    const target = {selected: "value"};
    jest.spyOn($storage, "has").mockImplementation((action) => action === "model");
    jest.spyOn($storage, "get").mockReturnValue(storedModel);
    jest.spyOn($utilities, "getCellId").mockReturnValue("grid-Des-2");

    expect($control.setTarget({component: "grid", view: "report", column: "Des", row: "2"}, "model", target)).toBe(target);
    expect(storedModel.report.grid.cells["grid-Des-2"]).toBe(target);
    expect($control.setTarget({component: "other", view: "report"}, "model", target)).toBe(target);
    expect(storedModel.report.other).toBe(target);
    expect($control.setTarget({component: "grid"}, "model", target)).toBeNull();
    expect($control.setTarget({component: "grid", view: "report"}, "api", target)).toBeNull();
  });

  it("keeps model attributes in sync directly until an updateModelValues API is available", () => {
    const model = {previous: null, selected: null, values: []};
    jest.spyOn($control, "getAddressModel").mockReturnValue(model);
    jest.spyOn($control, "getAddressApi").mockReturnValue({});
    jest.spyOn($storage, "get").mockReturnValue({report: "loaded"});
    $log.warn = jest.fn();

    $control.changeModelAttribute({component: "suggest", view: "report"}, {selected: "test"}, false);

    expect(model.selected).toBe("test");
    expect(model.previous).toBe("test");
    expect($log.warn).toHaveBeenCalledWith(
      "[WARNING] Method 'updateModelValues' not found in api",
      {address: {component: "suggest", view: "report"}}
    );
  });

  it("delegates model synchronization to updateModelValues when the component API exists", () => {
    const model = {previous: null, selected: null, values: []};
    const updateModelValues = jest.fn((data) => {
      model.selected = data.selected;
    });
    jest.spyOn($control, "getAddressModel").mockReturnValue(model);
    jest.spyOn($control, "getAddressApi").mockReturnValue({updateModelValues});
    jest.spyOn($storage, "get").mockReturnValue({report: "loaded"});

    $control.changeModelAttribute({component: "suggest", view: "report"}, {selected: "test"}, false);

    expect(updateModelValues).toHaveBeenCalledWith({selected: "test"});
    expect(model.selected).toBe("test");
    expect(model.previous).toBe("test");
  });

  it("returns null when setting a target for a missing storage namespace", () => {
    jest.spyOn($storage, "has").mockReturnValue(false);

    expect($control.setTarget({component: "grid", view: "report"}, "model", {selected: "x"})).toBeNull();
  });

  it("warns while applying direct model updates when the API update hook is missing", () => {
    const model = {selected: null};
    jest.spyOn($control, "getAddressModel").mockReturnValue(model);
    jest.spyOn($control, "getAddressApi").mockReturnValue({});
    jest.spyOn($storage, "get").mockReturnValue({report: "loading"});
    $log.warn = jest.fn();

    $control.changeModelAttribute({component: "suggest", view: "report"}, {selected: "test"}, false);

    expect(model.selected).toBe("test");
    expect($log.warn).toHaveBeenCalledWith(
      "[WARNING] Method 'updateModelValues' not found in api",
      {address: {component: "suggest", view: "report"}}
    );
  });
});
