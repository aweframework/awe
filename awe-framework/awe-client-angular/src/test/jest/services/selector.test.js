// Focused source-traceable Selector coverage. Karma original remains callable.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";

const {createContractScope} = require("../contracts/shared/contractSharedTestUtils.js");

require("../../js/services/selector.js");

describe("Selector", function () {
  let $rootScope;
  let $selector;
  let $translate;
  let $storage;
  let $actionController;
  let $control;
  let currentModels;
  let currentControllers;

  beforeEach(function () {
    angular.mock.module("aweApplication");

    currentModels = {};
    currentControllers = {};

    inject(["$injector", function ($injector) {
      $rootScope = $injector.get("$rootScope");
      $selector = $injector.get("Selector");
      $translate = $injector.get("$translate");
      $storage = $injector.get("Storage");
      $actionController = $injector.get("ActionController");
      $control = $injector.get("Control");
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function () {
  });

  function installSelectorSpies(checkComponent = true) {
    jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
    jest.spyOn($control, "setAddressModel").mockImplementation((address, model) => {
      currentModels[address.component] = model;
    });
    jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
    jest.spyOn($control, "checkComponent").mockReturnValue(checkComponent);
    jest.spyOn($translate, "instant").mockImplementation(value => ({ALPHA: "Alpha", BETA: "Beta"}[value] || value));
  }

  function createSelector(mode, options = {}) {
    const id = options.id || "CrtSelector";
    const scope = createContractScope($rootScope, options);

    currentModels[id] = {values: [], selected: null, storedValues: [], ...options.model};
    currentControllers[id] = {id, optional: true, strict: true, ...options.controller};

    const component = new $selector(scope, id, {});

    switch (mode) {
      case "select":
        component.asSelect();
        break;
      case "selectMultiple":
        component.asSelectMultiple();
        break;
      case "suggest":
        component.asSuggest();
        break;
      case "suggestMultiple":
        component.asSuggestMultiple();
        break;
      default:
        throw new Error(`Unknown selector mode: ${mode}`);
    }

    return {id, scope, component, model: currentModels[id], controller: currentControllers[id]};
  }

  it("filters select callbacks, clears resolved missing selections, and serializes multiple selections as arrays", function () {
    installSelectorSpies();
    const select = createSelector("select", {id: "CrtTranslated", model: {values: [{value: "A", label: "ALPHA"}, {value: "", label: null}], selected: "missing"}});
    const emptyInitCallback = jest.fn().mockName("emptyInitCallback");
    const emptyQueryCallback = jest.fn().mockName("emptyQueryCallback");
    const multi = createSelector("selectMultiple", {id: "CrtMulti", model: {values: [{value: "A", label: "ALPHA"}], selected: "A"}});

    select.scope.aweSelectOptions.query({term: "zzz", callback: emptyQueryCallback});
    select.scope.aweSelectOptions.initSelection({}, emptyInitCallback);

    expect(emptyQueryCallback).toHaveBeenCalledWith({results: []});
    expect(emptyInitCallback).toHaveBeenCalledWith([]);
    expect(select.model.selected).toBeNull();
    expect(multi.component.getData()).toEqual({CrtMulti: ["A"]});
  });

  it("updates select state from plugin changes, listeners, and initialized fill/select calls", function () {
    installSelectorSpies();
    const select = createSelector("select", {id: "CrtStatus", model: {values: [{value: "A", label: "ALPHA"}, {value: "B", label: "BETA"}], selected: "A"}});
    select.component.modelChange = jest.fn().mockName("modelChange");
    select.component.fill = jest.fn().mockName("fill");
    select.component.select = jest.fn().mockName("select");
    select.component.updateVisibleValue = jest.fn().mockName("updateVisibleValue");
    const updateVisibleValue = select.component.updateVisibleValue;

    select.component.onPluginChange({val: "B"});
    select.component.onPluginChange({val: ""});
    select.component.onPluginInit();
    $rootScope.$broadcast("editing-cell", select.component.address);
    $rootScope.$broadcast("languageChanged");
    select.component.updateVisibleValue = null;
    $rootScope.$broadcast("languageChanged");

    expect(select.model.selected).toBeNull();
    expect(select.component.modelChange).toHaveBeenCalledTimes(2);
    expect(select.component.fill).toHaveBeenCalledWith([{id: "A", text: "Alpha"}, {id: "B", text: "Beta"}]);
    expect(select.component.select).toHaveBeenCalledWith(null);
    expect(updateVisibleValue).toHaveBeenCalled();
    expect(select.component.fill).toHaveBeenCalledTimes(4);
  });

  it("normalizes multiple plugin values from array, object, exact, split, and free-text inputs", function () {
    installSelectorSpies();
    const suggest = createSelector("suggestMultiple", {id: "CrtTags", model: {values: [{value: "A", label: "ALPHA"}, {value: "B", label: "BETA"}], storedValues: [{value: "C", label: "Gamma"}], selected: []}});
    suggest.component.modelChange = jest.fn().mockName("modelChange");

    suggest.component.onPluginChange({val: ["A", ""]});
    expect(suggest.model.selected).toEqual(["A"]);

    suggest.component.onPluginChange({val: {length: 1, value: "C"}});
    expect(suggest.model.selected).toEqual([{length: 1, value: "C"}]);

    suggest.component.onPluginChange({val: "C"});
    expect(suggest.model.selected).toEqual(["C"]);

    suggest.component.onPluginChange({val: "A,B"});
    expect(suggest.model.selected).toEqual(["A,B"]);

    suggest.component.onPluginChange({val: "A,free"});
    expect(suggest.model.selected).toEqual(["A,free"]);
    expect(suggest.component.modelChange).toHaveBeenCalledTimes(5);
  });

  it("returns false for invalid selector criteria without registering select or suggest options", function () {
    installSelectorSpies(false);
    currentModels.CrtInvalid = {values: [], selected: null, storedValues: []};
    currentControllers.CrtInvalid = {id: "CrtInvalid", optional: true, strict: true};

    const select = new $selector(createContractScope($rootScope), "CrtInvalid", {});
    const suggest = new $selector(createContractScope($rootScope), "CrtInvalid", {});

    expect(select.asSelect()).toBe(false);
    expect(suggest.asSuggest()).toBe(false);
  });

  it("loads unresolved suggest selections before initialization and completes the initial model-change listener", function () {
    installSelectorSpies();
    jest.spyOn($storage, "get").mockReturnValue({report: {name: "lazy-screen"}});
    jest.spyOn($actionController, "addActionList").mockReturnValue("queued");
    const suggest = createSelector("suggest", {id: "CrtLazySuggest", model: {values: [], selected: "pending", storedValues: []}, controller: {targetAction: "searchTags", checkTarget: "checkTags"}, scope: {initialized: false}});
    suggest.component.reload = jest.fn().mockName("reload").mockReturnValue("request");

    suggest.component.onStart();
    $rootScope.$broadcast("modelChanged", [suggest.component.address]);

    expect(suggest.controller.targetAction).toBe("checkTags");
    expect(suggest.component.reload).toHaveBeenCalledTimes(1);
    expect(suggest.scope.initialized).toBe(false);
  });
});
