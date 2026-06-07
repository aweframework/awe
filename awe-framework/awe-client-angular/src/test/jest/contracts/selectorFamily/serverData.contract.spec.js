import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";
import "../../../../main/resources/webpack/locals-eu-ES.config";
import "../../../../main/resources/webpack/locals-fr-FR.config";

const contractCases = require("./selectorContractCases.js");
const {
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/jest/contracts/selectorFamily/serverData.contract.js", function () {
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

      jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
      jest.spyOn($control, "setAddressModel").mockImplementation((address, model) => {
        currentModels[address.component] = model;
      });
      jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "getAddressApi").mockImplementation(address => currentApis[address.component]);
      jest.spyOn($control, "changeControllerAttribute");
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

    select.component.getExtraData = jest.fn().mockName("getExtraDataSelect").mockReturnValue({text: "Beta"});
    suggest.component.getExtraData = jest.fn().mockName("getExtraDataSuggest").mockReturnValue({text: ["a,test"]});

    jest.spyOn($storage, "get").mockImplementation(key => key === "api" ? createApiStorage("report", [select, suggest]) : {});

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

    suggest.component.getSpecificFields = jest.fn().mockName("getSpecificFields").mockReturnValue({
      max: 25,
      suggest: ["contract-term"]
    });

    jest.spyOn($connection, "sendMessage").mockReturnValue({reject: jest.fn().mockName("reject")});

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
