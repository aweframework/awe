const contractCases = require("./booleanContractCases.js");
const {createContractScope} = require("../shared/contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/booleanFamily/boolean.contract.js", function () {
  let $injector;
  let $rootScope;
  let $control;
  let CheckboxRadio;
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

      spyOn($control, "getAddressModel").and.callFake(address => currentModels[address.component]);
      spyOn($control, "getAddressController").and.callFake(address => currentControllers[address.component]);
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "setAddressApi").and.callFake((address, api) => {
        currentApis[address.component] = api;
        currentViewApis[address.view][address.component] = api;
      });
      spyOn($control, "getAddressViewModel").and.callFake(address => currentViewModels[address.view]);
      spyOn($control, "getAddressViewApi").and.callFake(address => currentViewApis[address.view]);
      spyOn($control, "changeModelAttribute").and.callFake(() => true);
      spyOn($control, "publishModelChanged").and.callFake(() => true);
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
      scope,
      component,
      model: currentModels[id],
      api: currentApis[id],
      groupModel: currentViewModels[view][group],
      groupApi: currentViewApis[view][group]
    };
  }

  describe("checkbox contract", function () {
    it("serializes a checked checkbox as the active value 1", function () {
      const checkbox = createBoolean("checkbox", contractCases.checkbox.checked);

      checkbox.scope.updateSelected(true);

      expect(checkbox.model.selected).toBe(1);
      expect(checkbox.model.values).toEqual([{value: 1, label: 1}]);
      expect(checkbox.component.getData()).toEqual({CrtBooleanChecked: 1});
    });

    it("serializes an unchecked checkbox as the inactive value 0", function () {
      const checkbox = createBoolean("checkbox", contractCases.checkbox.unchecked);

      checkbox.scope.updateSelected(false);

      expect(checkbox.model.selected).toBe(0);
      expect(checkbox.model.values).toEqual([{value: 0, label: 0}]);
      expect(checkbox.component.getData()).toEqual({CrtBooleanUnchecked: 0});
    });
  });

  describe("buttonCheckbox contract", function () {
    it("restores a custom default value when buttonCheckbox has one", function () {
      const buttonCheckbox = createBoolean("buttonCheckbox", contractCases.checkbox.defaulted);

      buttonCheckbox.component.onRestore();

      expect(buttonCheckbox.model.selected).toBe("Y");
      expect(buttonCheckbox.model.values).toEqual([{value: "Y", label: "Y"}]);
      expect(buttonCheckbox.component.getData()).toEqual({ButBooleanDefault: "Y"});
    });

    it("treats string-matched payloads as checked without coercing the serialized value", function () {
      const buttonCheckbox = createBoolean("buttonCheckbox", contractCases.checkbox.stringChecked);

      buttonCheckbox.component.api.updateModelValues(contractCases.checkbox.stringChecked.update);

      expect(buttonCheckbox.scope.checked).toBeTrue();
      expect(buttonCheckbox.model.selected).toBe("1");
      expect(buttonCheckbox.component.getData()).toEqual({ButBooleanString: "1"});
    });
  });

  describe("radio contract", function () {
    it("serializes an active radio through its group payload", function () {
      const radio = createBoolean("radio", contractCases.radio.active);

      radio.groupModel.selected = radio.component.value;
      radio.component.modelChange();

      expect(radio.groupModel.selected).toBe("YES");
      expect(radio.groupApi.getData()).toEqual({GrpBooleanState: "YES"});
    });

    it("keeps an inactive radio group as null when no option is selected", function () {
      const radio = createBoolean("radio", contractCases.radio.inactive);

      expect(radio.groupModel.selected).toBeNull();
      expect(radio.groupApi.getData()).toEqual({GrpBooleanInactive: null});
    });
  });

  describe("buttonRadio contract", function () {
    it("normalizes single-value updates to the scalar group payload and keeps individual payload empty", function () {
      const buttonRadio = createBoolean("buttonRadio", contractCases.radio.normalized);

      buttonRadio.groupApi.updateModelValues(contractCases.radio.normalized.update);

      expect(buttonRadio.groupModel.selected).toBe("ON");
      expect(buttonRadio.groupApi.getData()).toEqual({GrpBooleanMode: "ON"});
      expect(buttonRadio.api.getData()).toEqual({});
    });
  });
});
