const contractCases = require("./booleanContractCases.js");
const {
  buildLaunchRequestExpectation,
  createContractScope,
  createServerAction
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/booleanFamily/serverData.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let CheckboxRadio;
  let $serverData;
  let $storage;
  let $connection;
  let Action;
  let currentModels;
  let currentControllers;
  let currentApis;
  let currentViewModels;
  let currentViewApis;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};
    currentApis = {};
    currentViewModels = {report: {}};
    currentViewApis = {report: {}};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      $control = $injector.get("Control");
      CheckboxRadio = $injector.get("CheckboxRadio");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $connection = $injector.get("Connection");
      Action = $injector.get("Action");

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "setAddressApi").and.callFake((address, api) => {
        currentApis[address.component] = api;
        currentViewApis[address.view][address.component] = api;
      });
      spyOn($control, "getAddressViewModel").and.callFake(address => currentViewModels[address.view]);
      spyOn($control, "getAddressViewApi").and.callFake(address => currentViewApis[address.view]);
      spyOn($control, "getAddressApi").and.callFake(address => currentViewApis[address.view]?.[address.component]);
      spyOn($control, "changeModelAttribute").and.callFake(() => true);
      spyOn($control, "publishModelChanged").and.callFake(() => true);
      spyOn($control, "changeControllerAttribute");
    }]);
  });

  function createBoolean(mode, options = {}) {
    const id = options.id || "CrtBoolean";
    const view = options.view || "report";
    const group = options.group || `Grp${id}`;
    const scope = createContractScope($rootScope, {
      ...options,
      view,
      scope: {size: options.size || "md"}
    });

    currentModels[id] = {
      values: [],
      selected: null,
      defaultValues: null,
      ...options.model
    };
    currentViewModels[view][id] = currentModels[id];

    currentControllers[id] = {
      id,
      group,
      visible: true,
      readonly: false,
      ...options.controller
    };

    const component = new CheckboxRadio(scope, id, angular.element("<div class='focus-target'></div>"));

    if (mode === "buttonCheckbox" || mode === "buttonRadio") {
      component.specialClass = `btn-${scope.size}`;
    }

    if (mode === "checkbox" || mode === "buttonCheckbox") {
      component.asCheckbox();
    } else if (mode === "radio" || mode === "buttonRadio") {
      component.asRadio();
    } else {
      throw new Error(`Unknown boolean mode: ${mode}`);
    }

    return {
      id,
      group,
      component,
      api: currentApis[id],
      groupApi: currentViewApis[view][group],
      groupModel: currentViewModels[view][group]
    };
  }

  it("aggregates checkbox and radio payloads through ServerData.getFormValues preserving group serialization", function () {
    const checkbox = createBoolean("checkbox", contractCases.checkbox.checked);
    const buttonRadio = createBoolean("buttonRadio", contractCases.radio.normalized);

    checkbox.component.scope.updateSelected(true);
    buttonRadio.groupApi.updateModelValues(contractCases.radio.normalized.update);

    spyOn($storage, "get").and.callFake(key => key === "api" ? currentViewApis : {});

    expect($serverData.getFormValues()).toEqual({
      CrtBooleanChecked: 1,
      GrpBooleanMode: "ON"
    });
  });

  it("preserves boolean family payloads in launchServerAction while merging target-specific fields", function () {
    const checkbox = createBoolean("buttonCheckbox", contractCases.checkbox.defaulted);
    const buttonRadio = createBoolean("buttonRadio", contractCases.radio.normalized);
    const callbackTarget = {view: "report", component: checkbox.id};
    const action = createServerAction(Action, callbackTarget, {
      serverAction: "data",
      targetAction: "loadBooleanContract"
    });

    checkbox.component.onRestore();
    buttonRadio.groupApi.updateModelValues(contractCases.radio.normalized.update);
    checkbox.api.getSpecificFields = jasmine.createSpy("getSpecificFields").and.returnValue({max: 5, booleanFamily: true});

    spyOn($connection, "sendMessage").and.returnValue({reject: jasmine.createSpy("reject")});

    $serverData.launchServerAction(action, {
      ...checkbox.component.getData(),
      ...buttonRadio.groupApi.getData()
    });

    expect($control.changeControllerAttribute).toHaveBeenCalledWith(callbackTarget, {loading: true});
    expect($connection.sendMessage).toHaveBeenCalledWith(buildLaunchRequestExpectation({
      action,
      target: callbackTarget,
      values: {
        serverAction: "data",
        targetAction: "loadBooleanContract",
        ButBooleanDefault: "Y",
        GrpBooleanMode: "ON",
        max: 5,
        booleanFamily: true
      }
    }));
  });
});
