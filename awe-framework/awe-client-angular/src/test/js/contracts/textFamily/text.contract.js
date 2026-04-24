const contractCases = require("./textContractCases.js");
const {createContractScope} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/textFamily/text.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let Criterion;
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
      Criterion = $injector.get("Criterion");

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "setAddressApi");
    }]);
  });

  function createText(options = {}) {
    const id = options.id || "CrtText";
    const scope = createContractScope($rootScope, options);

    currentModels[id] = {
      values: [],
      selected: null,
      ...options.model
    };

    currentControllers[id] = {
      id,
      optional: true,
      ...options.controller
    };

    const component = new Criterion(scope, id, angular.element("<div class='focus-target'></div>"));
    component.asCriterion();

    return {
      id,
      component,
      api: component.api,
      model: currentModels[id]
    };
  }

  it("serializes normal text without altering its payload", function () {
    const text = createText({
      id: contractCases.textValue.id
    });

    text.api.updateModelValues(contractCases.textValue.update);

    expect(text.model.selected).toBe(contractCases.textValue.expected.selected);
    expect(text.component.getData()).toEqual({CrtText: contractCases.textValue.expected.selected});
    expect(text.component.getVisibleValue()).toBe(contractCases.textValue.expected.visible);
  });

  it("preserves an explicit empty string as an empty serialized payload", function () {
    const text = createText({
      id: contractCases.emptyValue.id
    });

    text.api.updateModelValues(contractCases.emptyValue.update);

    expect(text.model.selected).toBe(contractCases.emptyValue.expected.selected);
    expect(text.component.getData()).toEqual({CrtEmptyText: contractCases.emptyValue.expected.selected});
    expect(text.component.getVisibleValue()).toBe(contractCases.emptyValue.expected.visible);
  });

  it("supports null text values by preserving null in data and exposing an empty visible value", function () {
    const text = createText({
      id: contractCases.nullValue.id
    });

    text.api.updateModelValues(contractCases.nullValue.update);

    expect(text.model.selected).toBeNull();
    expect(text.component.getData()).toEqual({CrtNullableText: null});
    expect(text.component.getVisibleValue()).toBe(contractCases.nullValue.expected.visible);
  });

  it("normalizes wrapped selections to the scalar text payload used by the contract", function () {
    const text = createText({
      id: contractCases.wrappedSelection.id
    });

    text.api.updateModelValues(contractCases.wrappedSelection.update);

    expect(text.model.selected).toBe(contractCases.wrappedSelection.expected.selected);
    expect(text.component.getData()).toEqual({CrtWrappedText: contractCases.wrappedSelection.expected.selected});
    expect(text.component.getVisibleValue()).toBe(contractCases.wrappedSelection.expected.visible);
  });

  it("preserves textarea multiline content without collapsing line breaks", function () {
    const textarea = createText({
      id: contractCases.textareaValue.id,
      controller: contractCases.textareaValue.controller
    });

    textarea.api.updateModelValues(contractCases.textareaValue.update);

    expect(textarea.model.selected).toBe(contractCases.textareaValue.expected.selected);
    expect(textarea.component.getData()).toEqual({TxtNarrative: contractCases.textareaValue.expected.selected});
    expect(textarea.component.getVisibleValue()).toBe(contractCases.textareaValue.expected.visible);
  });
});
