import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

const contractCases = require("./dateTimeContractCases.js");
const {createContractScope} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/dateTimeFamily/dateTime.contract.js", function () {
  let $injector;
  let $rootScope;
  let $httpBackend;
  let $control;
  let DateTime;
  let currentModels;
  let currentControllers;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      $httpBackend = $injector.get("$httpBackend");
      $control = $injector.get("Control");
      DateTime = $injector.get("DateTime");

      $httpBackend.when("POST", "settings").respond(DefaultSettings);

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "setAddressApi");
    }]);
  });

  function createDateTime(options = {}) {
    const id = options.id || "CrtDateTime";
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

    const component = new DateTime(scope, id, angular.element("<div></div>"));

    if (options.mode === "time") {
      component.asTime();
    } else {
      component.asDate();
    }

    return {
      id,
      component,
      model: currentModels[id]
    };
  }

  it("serializes a normal date value without altering its payload", function () {
    const date = createDateTime({
      id: "CrtDate",
      mode: contractCases.dateValue.mode,
      controller: contractCases.dateValue.controller
    });

    date.component.api.updateModelValues(contractCases.dateValue.update);

    expect(date.model.selected).toBe(contractCases.dateValue.expected.selected);
    expect(date.component.getData()).toEqual({CrtDate: contractCases.dateValue.expected.selected});
    expect(date.component.getVisibleValue()).toBe(contractCases.dateValue.expected.visible);
  });

  it("serializes a normal time value without altering its payload", function () {
    const time = createDateTime({
      id: "CrtTime",
      mode: contractCases.timeValue.mode
    });

    time.component.api.updateModelValues(contractCases.timeValue.update);

    expect(time.model.selected).toBe(contractCases.timeValue.expected.selected);
    expect(time.component.getData()).toEqual({CrtTime: contractCases.timeValue.expected.selected});
    expect(time.component.getVisibleValue()).toBe(contractCases.timeValue.expected.visible);
  });

  it("keeps an explicit empty date value as an empty serialized payload", function () {
    const date = createDateTime({
      id: "CrtEmptyDate",
      mode: contractCases.emptyValue.mode
    });

    date.component.api.updateModelValues(contractCases.emptyValue.update);

    expect(date.model.selected).toBe(contractCases.emptyValue.expected.selected);
    expect(date.component.getData()).toEqual({CrtEmptyDate: contractCases.emptyValue.expected.selected});
    expect(date.component.getVisibleValue()).toBe(contractCases.emptyValue.expected.visible);
  });

  it("supports null time values by preserving null in data and exposing an empty visible value", function () {
    const time = createDateTime({
      id: "CrtNullableTime",
      mode: contractCases.nullValue.mode
    });

    time.component.api.updateModelValues(contractCases.nullValue.update);

    expect(time.model.selected).toBeNull();
    expect(time.component.getData()).toEqual({CrtNullableTime: null});
    expect(time.component.getVisibleValue()).toBe(contractCases.nullValue.expected.visible);
  });

  it("normalizes wrapped date selections to the scalar value used by the contract", function () {
    const date = createDateTime({
      id: "CrtWrappedDate",
      mode: contractCases.wrappedSelection.mode,
      model: {selected: contractCases.wrappedSelection.selected}
    });

    date.component.api.updateModelValues({selected: contractCases.wrappedSelection.selected});

    expect(date.model.selected).toBe(contractCases.wrappedSelection.expected.selected);
    expect(date.component.getData()).toEqual({CrtWrappedDate: contractCases.wrappedSelection.expected.selected});
    expect(date.component.getVisibleValue()).toBe(contractCases.wrappedSelection.expected.visible);
  });
});
