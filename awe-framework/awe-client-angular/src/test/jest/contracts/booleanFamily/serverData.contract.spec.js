import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";
import "../../../../main/resources/webpack/locals-eu-ES.config";
import "../../../../main/resources/webpack/locals-fr-FR.config";

const contractCases = require("./booleanContractCases.js");
const {
  buildLaunchRequestExpectation,
  createContractScope,
  createServerAction
} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/jest/contracts/booleanFamily/serverData.contract.js", function () {
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

      jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
      jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "setAddressApi").mockImplementation((address, api) => {
        currentApis[address.component] = api;
        currentViewApis[address.view][address.component] = api;
      });
      jest.spyOn($control, "getAddressViewModel").mockImplementation(address => currentViewModels[address.view]);
      jest.spyOn($control, "getAddressViewApi").mockImplementation(address => currentViewApis[address.view]);
      jest.spyOn($control, "getAddressApi").mockImplementation(address => currentViewApis[address.view] && currentViewApis[address.view][address.component]);
      jest.spyOn($control, "changeModelAttribute").mockImplementation(() => true);
      jest.spyOn($control, "publishModelChanged").mockImplementation(() => true);
      jest.spyOn($control, "changeControllerAttribute");
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

    jest.spyOn($storage, "get").mockImplementation(key => key === "api" ? currentViewApis : {});

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
    checkbox.api.getSpecificFields = jest.fn().mockName("getSpecificFields").mockReturnValue({max: 5, booleanFamily: true});

    jest.spyOn($connection, "sendMessage").mockReturnValue({reject: jest.fn().mockName("reject")});

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
