import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";
import "../../../../main/resources/webpack/locals-eu-ES.config";
import "../../../../main/resources/webpack/locals-fr-FR.config";

const contractCases = require("./selectorContractCases.js");
const {createContractScope} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/jest/contracts/selectorFamily/selector.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let $selector;
  let $translate;
  let currentModels;
  let currentControllers;

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

      jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
      jest.spyOn($control, "setAddressModel").mockImplementation((address, model) => {
        currentModels[address.component] = model;
      });
      jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($translate, "instant").mockImplementation(value => value);
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function () {
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

    it("filters select query results by translated text and initializes the selected option", function () {
      const select = createSelector("select", {
        id: "CrtTranslatedSelect",
        model: {
          values: contractCases.selectSimple.values,
          selected: "B"
        }
      });
      const queryCallback = jest.fn().mockName("queryCallback");
      const initCallback = jest.fn().mockName("initCallback");

      select.scope.aweSelectOptions.query({term: "bet", callback: queryCallback});
      select.scope.aweSelectOptions.initSelection({}, initCallback);

      expect(queryCallback).toHaveBeenCalledWith({results: [{id: "B", text: "Beta"}]});
      expect(initCallback).toHaveBeenCalledWith({id: "B", text: "Beta"});
      expect(select.model.selected).toBe("B");
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
      jest.spyOn(suggest.component, "reload").mockReturnValue(true);

      suggest.component.api.updateModelValues({selected: contractCases.suggestStrictUnresolved.selected});

      expect(suggest.model.selected).toBe("missing");
      expect(suggest.model.pendingSelection).toBe("missing");
      expect(suggest.model.shouldReload).toBe(true);
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

    it("creates non-strict search choices and deduplicates them into the visible option list", function () {
      const suggest = createSelector("suggest", {
        id: "CrtAdHocSuggest",
        controller: {strict: false},
        model: {
          values: [{value: "existing", label: "Existing"}],
          storedValues: [],
          selected: null
        }
      });

      expect(suggest.scope.aweSelectOptions.createSearchChoice("  new-term  ")).toEqual({
        id: "new-term",
        text: "new-term"
      });
      expect(suggest.scope.aweSelectOptions.createSearchChoice("   ")).toBeNull();
      expect(suggest.model.values).toEqual([
        {value: "existing", label: "Existing"},
        {value: "new-term", label: "new-term", __adHoc: true}
      ]);
    });
  });

  describe("suggestMultiple contract", function () {
    it("preserves comma-containing values as exact array entries", function () {
      const suggest = createSelector("suggestMultiple", {
        id: "CrtCommaSuggest",
        model: {...contractCases.suggestMultipleCommaValue}
      });
      suggest.component.modelChange = jest.fn().mockName("modelChange");

      suggest.component.onPluginChange({val: "a,test"});

      expect(suggest.model.selected).toEqual(["a,test"]);
      expect(suggest.component.getData()).toEqual({CrtCommaSuggest: ["a,test"]});
      expect(suggest.component.modelChange).toHaveBeenCalled();
    });
  });
});
