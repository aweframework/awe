const contractCases = require("./gridContractCases.js");
const {createGridHarness} = require("./gridContractTestUtils.js");
const {
  buildLaunchRequestExpectation,
  createApiStorage,
  createServerAction
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/gridFamily/serverData.contract.js", function () {
  let $injector;
  let $rootScope;
  let GridCommons;
  let $serverData;
  let $storage;
  let $control;
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
      GridCommons = $injector.get("GridCommons");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $control = $injector.get("Control");
      $connection = $injector.get("Connection");
      Action = $injector.get("Action");

      spyOn($control, "getAddressApi").and.callFake(address => currentApis[address.component]);
      spyOn($control, "changeControllerAttribute");
    }]);
  });

  function createGrid(options = {}) {
    return createGridHarness({
      $rootScope,
      GridCommons,
      currentModels,
      currentControllers,
      currentApis,
      options
    });
  }

  it("aggregates grid payloads through ServerData.getFormValues preserving selected ids and metadata", function () {
    const grid = createGrid(contractCases.serverAggregation);

    spyOn($storage, "get").and.callFake(key => key === "api" ? createApiStorage("report", [grid]) : {});

    expect($serverData.getFormValues()).toEqual(contractCases.serverAggregation.expectedData);
  });

  it("preserves grid payloads in launchServerAction while merging target-specific query fields", function () {
    const grid = createGrid(contractCases.serverAggregation);
    const callbackTarget = {view: "report", component: grid.id};
    const action = createServerAction(Action, callbackTarget, {
      serverAction: "data",
      targetAction: "loadGridContract"
    });

    grid.component.getSpecificFields = jasmine.createSpy("getSpecificFields").and.returnValue(
      contractCases.serverAggregation.targetSpecificFields
    );
    currentApis[grid.id] = grid.component;

    spyOn($connection, "sendMessage").and.returnValue({reject: jasmine.createSpy("reject")});

    $serverData.launchServerAction(action, contractCases.serverAggregation.expectedData);

    expect($control.changeControllerAttribute).toHaveBeenCalledWith(callbackTarget, {loading: true});
    expect($connection.sendMessage).toHaveBeenCalledWith(buildLaunchRequestExpectation({
      action,
      target: callbackTarget,
      values: {
        serverAction: "data",
        targetAction: "loadGridContract",
        GrdServerContract: [4],
        "GrdServerContract.data": contractCases.serverAggregation.expectedData["GrdServerContract.data"],
        "GrdServerContract-id": [4],
        value: ["lele"],
        "value.selected": "lele",
        other: ["asda"],
        "other.selected": "asda",
        id: [4],
        "id.selected": 4,
        max: 15,
        page: 9,
        sort: [{id: "other", direction: "asc"}]
      }
    }));
  });
});
