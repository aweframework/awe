import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

const contractCases = require("./numericContractCases.js");
const {createContractScope} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/numericFamily/numeric.contract.js", function () {
  let $injector;
  let $rootScope;
  let $compile;
  let $httpBackend;
  let $control;
  let Numeric;
  let currentModels;
  let currentControllers;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      $compile = $injector.get("$compile");
      $httpBackend = $injector.get("$httpBackend");
      $control = $injector.get("Control");
      Numeric = $injector.get("Numeric");

      $httpBackend.when("POST", "settings").respond(DefaultSettings);

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "setAddressApi");
    }]);
  });

  function createNumeric(options = {}) {
    const id = options.id || "CrtNumeric";
    const scope = createContractScope($rootScope, options);

    currentModels[id] = {
      values: [],
      selected: null,
      ...options.model
    };

    currentControllers[id] = {
      id,
      optional: true,
      numberFormat: {min: 0, precision: 0},
      ...options.controller
    };

    const component = new Numeric(scope, id, {});
    component.asNumeric();
    $compile("<input ui-numeric='aweNumericOptions'/>")(scope);
    scope.$digest();

    return {
      id,
      component,
      model: currentModels[id],
      controller: currentControllers[id],
      scope
    };
  }

  it("serializes a formatted decimal value without altering its numeric payload", function () {
    const numeric = createNumeric({
      id: "CrtAmount",
      controller: contractCases.decimalValue.controller
    });

    numeric.component.api.updateModelValues(contractCases.decimalValue.update);

    expect(numeric.model.selected).toBe(contractCases.decimalValue.expected.selected);
    expect(numeric.model.values).toEqual(contractCases.decimalValue.expected.values);
    expect(numeric.component.getData()).toEqual({CrtAmount: contractCases.decimalValue.expected.selected});
    expect(numeric.component.getVisibleValue()).toBe(contractCases.decimalValue.expected.visible);
  });

  it("preserves numeric zero as data instead of collapsing it to empty", function () {
    const numeric = createNumeric({
      id: "CrtZero",
      controller: contractCases.zeroValue.controller
    });

    numeric.component.api.updateModelValues(contractCases.zeroValue.update);

    expect(numeric.model.selected).toBe(contractCases.zeroValue.expected.selected);
    expect(numeric.model.values).toEqual(contractCases.zeroValue.expected.values);
    expect(numeric.component.getData()).toEqual({CrtZero: contractCases.zeroValue.expected.selected});
    expect(numeric.component.getVisibleValue()).toBe(contractCases.zeroValue.expected.visible);
  });

  it("keeps an explicit empty value as an empty serialized payload", function () {
    const numeric = createNumeric({
      id: "CrtEmpty",
      controller: contractCases.emptyValue.controller
    });

    numeric.component.api.updateModelValues(contractCases.emptyValue.update);

    expect(numeric.model.selected).toBe(contractCases.emptyValue.expected.selected);
    expect(numeric.model.values).toEqual(contractCases.emptyValue.expected.values);
    expect(numeric.component.getData()).toEqual({CrtEmpty: contractCases.emptyValue.expected.selected});
    expect(numeric.component.getVisibleValue()).toBe(contractCases.emptyValue.expected.visible);
  });

  it("supports null values by exposing an empty visible value while preserving null in data", function () {
    const numeric = createNumeric({
      id: "CrtNullable",
      controller: contractCases.nullValue.controller
    });

    numeric.component.api.updateModelValues(contractCases.nullValue.update);

    expect(numeric.model.selected).toBeNull();
    expect(numeric.model.values).toEqual(contractCases.nullValue.expected.values);
    expect(numeric.component.getData()).toEqual({CrtNullable: null});
    expect(numeric.component.getVisibleValue()).toBe(contractCases.nullValue.expected.visible);
  });

  it("normalizes wrapped selections to the scalar numeric value used by the contract", function () {
    const numeric = createNumeric({
      id: "CrtWrapped",
      controller: contractCases.wrappedSelection.controller,
      model: {selected: contractCases.wrappedSelection.selected}
    });

    numeric.component.onModelChanged();

    expect(numeric.model.selected).toBe(contractCases.wrappedSelection.expected.selected);
    expect(numeric.model.values).toEqual(contractCases.wrappedSelection.expected.values);
    expect(numeric.component.getData()).toEqual({CrtWrapped: contractCases.wrappedSelection.expected.selected});
    expect(numeric.component.getVisibleValue()).toBe(contractCases.wrappedSelection.expected.visible);
  });
});
