const contractCases = require("./selectorContractCases.js");
const {createContractScope} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/selectorFamily/selector.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let $selector;
  let $translate;
  let currentModels;
  let currentControllers;
  let originalTimeout;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      $control = $injector.get("Control");
      $selector = $injector.get("Selector");
      $translate = $injector.get("$translate");

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "setAddressModel").and.callFake((address, model) => {
        currentModels[address.component] = model;
      });
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($translate, "instant").and.callFake(value => value);
    }]);

    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function () {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  function createSelector(mode, options = {}) {
    const id = options.id || "CrtSelector";
    const scope = createContractScope($rootScope, options);

    currentModels[id] = {
      values: [],
      selected: null,
      storedValues: [],
      ...options.model
    };

    currentControllers[id] = {
      id,
      optional: true,
      strict: true,
      ...options.controller
    };

    const component = new $selector(scope, id, {});

    switch (mode) {
      case "select":
        component.asSelect();
        break;
      case "suggest":
        component.asSuggest();
        break;
      case "selectMultiple":
        component.asSelectMultiple();
        break;
      case "suggestMultiple":
        component.asSuggestMultiple();
        break;
      default:
        throw new Error(`Unknown selector mode: ${mode}`);
    }

    return {
      id,
      component,
      model: currentModels[id],
      controller: currentControllers[id],
      scope
    };
  }

  describe("select contract", function () {
    it("serializes a valid single select as the selected scalar", function () {
      const select = createSelector("select", {
        id: "CrtSimpleSelect",
        model: {selected: null}
      });

      select.component.api.updateModelValues(contractCases.selectSimple);

      expect(select.model.selected).toBe("B");
      expect(select.model.values).toEqual(contractCases.selectSimple.values);
      expect(select.component.getData()).toEqual({CrtSimpleSelect: "B"});
      expect(select.component.getVisibleValue()).toBe("Beta");
    });

    it("autocorrects an invalid required single select to the first option", function () {
      const select = createSelector("select", {
        id: "CrtRequiredSelect",
        controller: {optional: false},
        model: {
          values: contractCases.selectInvalidRequired.values,
          selected: contractCases.selectInvalidRequired.selected
        }
      });

      select.component.onStart();

      expect(select.model.selected).toBe("A");
      expect(select.model["initial-selected"]).toBe("A");
      expect(select.component.getData()).toEqual({CrtRequiredSelect: "A"});
    });
  });

  describe("multiple selector contract", function () {
    it("serializes selectMultiple as an array even when the model keeps a scalar", function () {
      const select = createSelector("selectMultiple", {
        id: "CrtMultiSelect",
        model: {
          values: contractCases.selectSimple.values,
          selected: "B"
        }
      });

      expect(select.component.getData()).toEqual({CrtMultiSelect: ["B"]});
    });
  });

  describe("suggest contract", function () {
    it("marks unresolved strict selections as pending and requests a reload", function () {
      const suggest = createSelector("suggest", {
        id: "CrtStrictSuggest",
        controller: {strict: true},
        model: {
          values: contractCases.suggestStrictUnresolved.values,
          storedValues: contractCases.suggestStrictUnresolved.storedValues,
          selected: "resolved"
        }
      });
      spyOn(suggest.component, "reload").and.returnValue(true);

      suggest.component.api.updateModelValues({selected: contractCases.suggestStrictUnresolved.selected});

      expect(suggest.model.selected).toBe("missing");
      expect(suggest.model.pendingSelection).toBe("missing");
      expect(suggest.model.shouldReload).toBeTrue();
      expect(suggest.model.values).toEqual([]);
      expect(suggest.model.storedValues).toEqual([]);
      expect(suggest.component.reload).toHaveBeenCalled();
      expect(suggest.component.getData()).toEqual({CrtStrictSuggest: "missing"});
    });

    it("preserves a free non-strict value as a visible and serializable option", function () {
      const suggest = createSelector("suggest", {
        id: "CrtFreeSuggest",
        controller: {strict: false},
        model: {
          ...contractCases.suggestFreeText,
          selected: null
        }
      });

      suggest.model.selected = contractCases.suggestFreeText.selected;

      suggest.component.onModelChangedValues();

      expect(suggest.model.values).toEqual([{value: "free-text", label: "free-text", __adHoc: true}]);
      expect(suggest.component.getData()).toEqual({CrtFreeSuggest: "free-text"});
      expect(suggest.component.getVisibleValue()).toBe("free-text");
    });
  });

  describe("suggestMultiple contract", function () {
    it("preserves comma-containing values as exact array entries", function () {
      const suggest = createSelector("suggestMultiple", {
        id: "CrtCommaSuggest",
        model: {...contractCases.suggestMultipleCommaValue}
      });
      suggest.component.modelChange = jasmine.createSpy("modelChange");

      suggest.component.onPluginChange({val: "a,test"});

      expect(suggest.model.selected).toEqual(["a,test"]);
      expect(suggest.component.getData()).toEqual({CrtCommaSuggest: ["a,test"]});
      expect(suggest.component.modelChange).toHaveBeenCalled();
    });
  });
});
