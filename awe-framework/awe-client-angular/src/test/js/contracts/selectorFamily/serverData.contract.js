const contractCases = require("./selectorContractCases.js");
const {
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/selectorFamily/serverData.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let $selector;
  let $serverData;
  let $storage;
  let $connection;
  let Action;
  let currentModels;
  let currentControllers;
  let currentApis;
  let originalTimeout;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};
    currentApis = {};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      $control = $injector.get("Control");
      $selector = $injector.get("Selector");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $connection = $injector.get("Connection");
      Action = $injector.get("Action");

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "setAddressModel").and.callFake((address, model) => {
        currentModels[address.component] = model;
      });
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "getAddressApi").and.callFake(address => currentApis[address.component]);
      spyOn($control, "changeControllerAttribute");
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
      case "suggestMultiple":
        component.asSuggestMultiple();
        break;
      default:
        throw new Error(`Unknown selector mode: ${mode}`);
    }

    currentApis[id] = component;

    return {
      id,
      component,
      model: currentModels[id]
    };
  }

  it("aggregates selector family payloads through ServerData.getFormValues without altering extras", function () {
    const select = createSelector("select", {
      id: "CrtStatus",
      model: {
        values: contractCases.selectSimple.values,
        selected: "B"
      }
    });
    const suggest = createSelector("suggestMultiple", {
      id: "CrtTags",
      model: {
        values: contractCases.suggestMultipleCommaValue.values,
        storedValues: contractCases.suggestMultipleCommaValue.storedValues,
        selected: ["a,test"]
      }
    });

    select.component.getExtraData = jasmine.createSpy("getExtraDataSelect").and.returnValue({text: "Beta"});
    suggest.component.getExtraData = jasmine.createSpy("getExtraDataSuggest").and.returnValue({text: ["a,test"]});

    spyOn($storage, "get").and.callFake(key => key === "api" ? createApiStorage("report", [select, suggest]) : {});

    expect($serverData.getFormValues()).toEqual(mergeComponentData([select, suggest]));
  });

  it("preserves selector payloads and target-specific fields in launchServerAction final message", function () {
    const select = createSelector("select", {
      id: "CrtStatus",
      model: {
        values: contractCases.selectSimple.values,
        selected: "B"
      }
    });
    const suggest = createSelector("suggestMultiple", {
      id: "CrtTags",
      model: {
        values: contractCases.suggestMultipleCommaValue.values,
        storedValues: contractCases.suggestMultipleCommaValue.storedValues,
        selected: ["a,test"]
      }
    });
    const callbackTarget = {view: "report", component: suggest.id};
    const action = createServerAction(Action, callbackTarget, {
      serverAction: "data",
      targetAction: "loadSelectorContract"
    });

    suggest.component.getSpecificFields = jasmine.createSpy("getSpecificFields").and.returnValue({
      max: 25,
      suggest: ["contract-term"]
    });

    spyOn($connection, "sendMessage").and.returnValue({reject: jasmine.createSpy("reject")});

    const selectorPayload = mergeComponentData([select, suggest]);

    $serverData.launchServerAction(action, selectorPayload);

    expect($control.changeControllerAttribute).toHaveBeenCalledWith(callbackTarget, {loading: true});
    expect($connection.sendMessage).toHaveBeenCalledWith(buildLaunchRequestExpectation({
      action,
      target: callbackTarget,
      values: {
        serverAction: "data",
        targetAction: "loadSelectorContract",
        CrtStatus: "B",
        CrtTags: ["a,test"],
        max: 25,
        suggest: ["contract-term"]
      }
    }));
  });
});
