import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";
import "../../../../main/resources/webpack/locals-eu-ES.config";
import "../../../../main/resources/webpack/locals-fr-FR.config";

import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

const contractCases = require("./dateTimeContractCases.js");
const {
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/jest/contracts/dateTimeFamily/serverData.contract.js", function () {
  let $injector;
  let $rootScope;
  let $httpBackend;
  let $control;
  let DateTime;
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
      $httpBackend = $injector.get("$httpBackend");
      $control = $injector.get("Control");
      DateTime = $injector.get("DateTime");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $connection = $injector.get("Connection");
      Action = $injector.get("Action");

      $httpBackend.when("POST", "settings").respond(DefaultSettings);

      jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
      jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "setAddressApi");
      jest.spyOn($control, "getAddressApi").mockImplementation(address => currentApis[address.component]);
      jest.spyOn($control, "changeControllerAttribute");
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

    currentApis[id] = component;

    return {
      id,
      component,
      model: currentModels[id]
    };
  }

  it("aggregates date and time payloads through ServerData.getFormValues preserving empty and null semantics", function () {
    const date = createDateTime({
      id: "CrtDate",
      mode: contractCases.dateValue.mode,
      controller: contractCases.dateValue.controller
    });
    const nullableTime = createDateTime({
      id: "CrtNullableTime",
      mode: contractCases.nullValue.mode
    });

    date.component.api.updateModelValues(contractCases.dateValue.update);
    nullableTime.component.api.updateModelValues(contractCases.nullValue.update);

    jest.spyOn($storage, "get").mockImplementation(key => key === "api" ? createApiStorage("report", [date, nullableTime]) : {});

    expect($serverData.getFormValues()).toEqual(mergeComponentData([date, nullableTime]));
  });

  it("preserves date and time payloads in launchServerAction while merging target-specific fields", function () {
    const date = createDateTime({
      id: "CrtDate",
      mode: contractCases.dateValue.mode,
      controller: contractCases.dateValue.controller
    });
    const time = createDateTime({
      id: "CrtTime",
      mode: contractCases.timeValue.mode
    });
    const callbackTarget = {view: "report", component: time.id};
    const action = createServerAction(Action, callbackTarget, {
      serverAction: "data",
      targetAction: "loadDateTimeContract"
    });

    date.component.api.updateModelValues(contractCases.dateValue.update);
    time.component.api.updateModelValues(contractCases.timeValue.update);
    time.component.getSpecificFields = jest.fn().mockName("getSpecificFields").mockReturnValue({max: 10});

    jest.spyOn($connection, "sendMessage").mockReturnValue({reject: jest.fn().mockName("reject")});

    $serverData.launchServerAction(action, mergeComponentData([date, time]));

    expect($control.changeControllerAttribute).toHaveBeenCalledWith(callbackTarget, {loading: true});
    expect($connection.sendMessage).toHaveBeenCalledWith(buildLaunchRequestExpectation({
      action,
      target: callbackTarget,
      values: {
        serverAction: "data",
        targetAction: "loadDateTimeContract",
        CrtDate: contractCases.dateValue.expected.selected,
        CrtTime: contractCases.timeValue.expected.selected,
        max: 10
      }
    }));
  });
});
