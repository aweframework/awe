// Select options with a description and disabled options (#993): a row may bring a description (a line under its name)
// and a disabled field (the option cannot be chosen), as the select of the React engine.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {wrapSelect2FormatResult} from "../../../main/resources/js/awe/services/testIds";

const {createContractScope} = require("../contracts/shared/contractSharedTestUtils.js");

describe("Selector option description and disabled options", function () {
  let $rootScope;
  let $selector;
  let $translate;
  let $control;
  let currentModels;
  let currentControllers;

  beforeEach(function () {
    angular.mock.module("aweApplication");

    currentModels = {};
    currentControllers = {};

    inject(["$injector", function ($injector) {
      $rootScope = $injector.get("$rootScope");
      $selector = $injector.get("Selector");
      $translate = $injector.get("$translate");
      $control = $injector.get("Control");
      const $httpBackend = $injector.get("$httpBackend");
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      $httpBackend.when("POST", "./settings").respond(DefaultSettings);
    }]);

    jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
    jest.spyOn($control, "setAddressModel").mockImplementation((address, model) => {
      currentModels[address.component] = model;
    });
    jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    jest.spyOn($translate, "instant").mockImplementation(value => ({CONDITION_OK: "Launches on OK"}[value] || value));
  });

  afterEach(function () {
    $(".select2-drop, .select2-drop-mask, .select2-container, input.select2-test").remove();
  });

  it("shows the description of an option under its name and does not let a disabled option be chosen", function () {
    const id = "TskPrn";
    currentModels[id] = {
      values: [
        {value: "1", label: "Import", description: "CONDITION_OK"},
        {value: "2", label: "Send <all>", description: "Circular dependency", disabled: true},
        {value: "3", label: "Close"}
      ], selected: [], storedValues: []
    };
    currentControllers[id] = {id, optional: true};
    const scope = createContractScope($rootScope, {});
    new $selector(scope, id, {}).asSelectMultiple(true);
    const options = scope.aweSelectOptions;

    const input = $("<input type='hidden' class='select2-test'/>").appendTo(document.body);
    input.select2({...options, formatResult: wrapSelect2FormatResult(options.formatResult, id)});
    input.select2("open");
    const results = input.select2("dropdown").find(".select2-result");

    // The description goes in its own line under the name (translated and escaped); an option without one shows its name
    expect(results.map((index, result) => $(result).find(".select2-result-label").text()).get())
      .toEqual(["ImportLaunches on OK", "Send <all>Circular dependency", "Close"]);
    expect($(results[0]).find(".awe-select-option-description").text()).toBe("Launches on OK");
    // The disabled option is marked as such and cannot be chosen
    expect($(results[1]).hasClass("select2-disabled")).toBe(true);
    expect($(results[1]).find(".select2-result-label").attr("aria-disabled")).toBe("true");
    expect($(results[0]).hasClass("select2-disabled")).toBe(false);
  });
});
