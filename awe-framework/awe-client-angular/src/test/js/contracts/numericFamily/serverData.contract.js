import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

const contractCases = require("./numericContractCases.js");
const {
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/numericFamily/serverData.contract.js", function () {
  let $injector;
  let $rootScope;
  let $compile;
  let $httpBackend;
  let $control;
  let Numeric;
  let $serverData;
  let $storage;
  let $connection;
  let Action;
  let currentModels;
  let currentControllers;
  let currentApis;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};
    currentApis = {};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      $compile = $injector.get("$compile");
      $httpBackend = $injector.get("$httpBackend");
      $control = $injector.get("Control");
      Numeric = $injector.get("Numeric");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $connection = $injector.get("Connection");
      Action = $injector.get("Action");

      $httpBackend.when("POST", "settings").respond(DefaultSettings);

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "setAddressApi");
      spyOn($control, "getAddressApi").and.callFake(address => currentApis[address.component]);
      spyOn($control, "changeControllerAttribute");
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
    currentApis[id] = component;

    return {
      id,
      component,
      model: currentModels[id]
    };
  }

  it("aggregates numeric payloads through ServerData.getFormValues preserving decimals and nulls", function () {
    const amount = createNumeric({
      id: "CrtAmount",
      controller: contractCases.decimalValue.controller
    });
    const optional = createNumeric({
      id: "CrtOptional",
      controller: contractCases.nullValue.controller
    });

    amount.component.api.updateModelValues(contractCases.decimalValue.update);
    optional.component.api.updateModelValues(contractCases.nullValue.update);

    spyOn($storage, "get").and.callFake(key => key === "api" ? createApiStorage("report", [amount, optional]) : {});

    expect($serverData.getFormValues()).toEqual(mergeComponentData([amount, optional]));
  });

  it("preserves numeric zero in launchServerAction while merging target-specific fields", function () {
    const amount = createNumeric({
      id: "CrtAmount",
      controller: contractCases.decimalValue.controller
    });
    const zero = createNumeric({
      id: "CrtZero",
      controller: contractCases.zeroValue.controller
    });
    const callbackTarget = {view: "report", component: amount.id};
    const action = createServerAction(Action, callbackTarget, {
      serverAction: "data",
      targetAction: "loadNumericContract"
    });

    amount.component.api.updateModelValues(contractCases.decimalValue.update);
    zero.component.api.updateModelValues(contractCases.zeroValue.update);
    amount.component.getSpecificFields = jasmine.createSpy("getSpecificFields").and.returnValue({max: 25});

    spyOn($connection, "sendMessage").and.returnValue({reject: jasmine.createSpy("reject")});

    $serverData.launchServerAction(action, mergeComponentData([amount, zero]));

    expect($control.changeControllerAttribute).toHaveBeenCalledWith(callbackTarget, {loading: true});
    expect($connection.sendMessage).toHaveBeenCalledWith(buildLaunchRequestExpectation({
      action,
      target: callbackTarget,
      values: {
        serverAction: "data",
        targetAction: "loadNumericContract",
        CrtAmount: contractCases.decimalValue.expected.selected,
        CrtZero: contractCases.zeroValue.expected.selected,
        max: 25
      }
    }));
  });
});
