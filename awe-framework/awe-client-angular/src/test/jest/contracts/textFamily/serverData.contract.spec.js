import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";
import "../../../../main/resources/webpack/locals-eu-ES.config";
import "../../../../main/resources/webpack/locals-fr-FR.config";

const contractCases = require("./textContractCases.js");
const {
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/jest/contracts/textFamily/serverData.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let Criterion;
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
      Criterion = $injector.get("Criterion");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $connection = $injector.get("Connection");
      Action = $injector.get("Action");

      jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
      jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "setAddressApi");
      jest.spyOn($control, "getAddressApi").mockImplementation(address => currentApis[address.component]);
      jest.spyOn($control, "changeControllerAttribute");
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
    currentApis[id] = component;

    return {
      id,
      component,
      api: component.api,
      model: currentModels[id]
    };
  }

  it("aggregates text family payloads through ServerData.getFormValues preserving empty, null and multiline semantics", function () {
    const text = createText({id: contractCases.textValue.id});
    const empty = createText({id: contractCases.emptyValue.id});
    const nullable = createText({id: contractCases.nullValue.id});
    const textarea = createText({
      id: contractCases.textareaValue.id,
      controller: contractCases.textareaValue.controller
    });

    text.api.updateModelValues(contractCases.textValue.update);
    empty.api.updateModelValues(contractCases.emptyValue.update);
    nullable.api.updateModelValues(contractCases.nullValue.update);
    textarea.api.updateModelValues(contractCases.textareaValue.update);

    jest.spyOn($storage, "get").mockImplementation(key => key === "api" ? createApiStorage("report", [text, empty, nullable, textarea]) : {});

    expect($serverData.getFormValues()).toEqual(mergeComponentData([text, empty, nullable, textarea]));
  });

  it("preserves text family payloads in launchServerAction while merging target-specific fields", function () {
    const text = createText({id: contractCases.textValue.id});
    const textarea = createText({
      id: contractCases.textareaValue.id,
      controller: contractCases.textareaValue.controller
    });
    const callbackTarget = {view: "report", component: textarea.id};
    const action = createServerAction(Action, callbackTarget, {
      serverAction: "data",
      targetAction: "loadTextContract"
    });

    text.api.updateModelValues(contractCases.textValue.update);
    textarea.api.updateModelValues(contractCases.textareaValue.update);
    textarea.component.getSpecificFields = jest.fn().mockName("getSpecificFields").mockReturnValue({max: 15, textFamily: true});

    jest.spyOn($connection, "sendMessage").mockReturnValue({reject: jest.fn().mockName("reject")});

    $serverData.launchServerAction(action, mergeComponentData([text, textarea]));

    expect($control.changeControllerAttribute).toHaveBeenCalledWith(callbackTarget, {loading: true});
    expect($connection.sendMessage).toHaveBeenCalledWith(buildLaunchRequestExpectation({
      action,
      target: callbackTarget,
      values: {
        serverAction: "data",
        targetAction: "loadTextContract",
        CrtText: contractCases.textValue.expected.selected,
        TxtNarrative: contractCases.textareaValue.expected.selected,
        max: 15,
        textFamily: true
      }
    }));
  });
});
