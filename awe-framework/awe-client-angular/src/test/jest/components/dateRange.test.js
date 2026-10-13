import "./lightComponentTestUtils";

import dayjs from "dayjs";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {dateRangeInputTemplate} from "../../../main/resources/js/awe/services/dateRange";

const ADDRESS = {view: "base", component: "period"};
const FROM = "01/01/2026";
const TO = "31/01/2026";

describe('awe-framework/awe-client-angular/src/test/jest/components/dateRange.js', function () {
  let $rootScope, $compile, $httpBackend, $control, $storage, $settings;

  beforeEach(function () {
    angular.mock.module('aweApplication');
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage", "AweSettings",
      function (_$rootScope_, _$compile_, _$httpBackend_, _Control_, _Storage_, _AweSettings_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $control = _Control_;
        $storage = _Storage_;
        $settings = _AweSettings_;
        jest.spyOn($settings, "getLanguage").mockReturnValue("en");
        $rootScope.view = "base";
        $rootScope.context = "screen";
        $rootScope.firstLoad = true;
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
        $httpBackend.when("POST", "./settings").respond(DefaultSettings);
        $storage.put("controller", {base: {}});
        $storage.put("model", {base: {}});
        $storage.put("api", {base: {}});
        $storage.put("status", {base: "loaded"});
      }]);
  });

  /**
   * Register the criterion and render it through its directive
   * @param {Array} selected Dates of the model
   * @param {Object} controller Controller attributes
   * @returns {jQuery} Rendered element
   */
  function render(selected, controller = {}) {
    $control.setAddressController(ADDRESS, {
      component: "date-range", contextMenu: [], dependencies: [], id: "period", optional: false, readonly: false,
      required: false, size: "lg", visible: true, validation: "", label: "Period", ...controller
    });
    $control.setAddressModel(ADDRESS, {defaultValues: [], page: 1, records: 0, selected, total: 0, values: []});
    const element = $compile("<awe-input-date-range input-date-range-id='period'></awe-input-date-range>")($rootScope);
    $rootScope.$digest();
    return element;
  }

  const presets = (element) => element.find("[data-testid='date-range-preset']");

  it('renders the start and the end inputs with the test ids and the identifiers of the criterion', function () {
    const element = render([FROM, TO]);

    expect(element.attr("criterion-id")).toBe("period");
    expect(element.find("input[data-testid='date-range-from']").attr("id")).toBe("period");
    expect(element.find("input[data-testid='date-range-to']").attr("id")).toBe("period-to");
    expect(element.find("input[data-testid='date-range-from']").val()).toBe(FROM);
    expect(element.find("input[data-testid='date-range-to']").val()).toBe(TO);
    expect(element.find(".fa-calendar").length).toBe(1);
  });

  it('renders the inputs empty when the model has no dates', function () {
    const element = render([]);

    expect([element.find("input").eq(0).val(), element.find("input").eq(1).val()]).toEqual(["", ""]);
  });

  it('renders no presets when none is configured', function () {
    expect(presets(render([]))).toHaveLength(0);
  });

  it('renders a button for each configured preset with its code', function () {
    const element = render([], {presets: "1M, YTD, all, FOREVER"});

    expect(presets(element).toArray().map(button => button.getAttribute("option-id"))).toEqual(["1M", "YTD", "ALL"]);
    expect(presets(element).eq(0).text()).toBe("1M");
  });

  it('fills both inputs and the model when a preset is clicked', function () {
    const element = render([], {presets: "ALL, TODAY"});
    const today = dayjs().format("DD/MM/YYYY");

    presets(element).eq(1).click();
    $rootScope.$digest();

    expect($control.getAddressModel(ADDRESS).selected).toEqual([today, today]);
    expect(element.find("input").eq(0).val()).toBe(today);
    expect(element.find("input").eq(1).val()).toBe(today);
  });

  it('marks the preset applied as active and clears it when the model changes', function () {
    const element = render([], {presets: "TODAY"});

    presets(element).eq(0).click();
    $rootScope.$digest();
    expect(presets(element).eq(0).attr("data-active")).toBe("true");
    expect(presets(element).eq(0).hasClass("active")).toBe(true);

    $control.getAddressModel(ADDRESS).selected = [FROM, TO];
    element.isolateScope().component.onSelectedChanged();
    $rootScope.$digest();

    expect(presets(element).eq(0).attr("data-active")).toBe("false");
  });

  it('disables the inputs and the presets when it is readonly', function () {
    const element = render([], {presets: "TODAY", readonly: true});

    expect(element.find("input").eq(0).prop("disabled")).toBe(true);
    expect(presets(element).eq(0).prop("disabled")).toBe(true);
  });

  it('exposes the template with the date range directive plugin', function () {
    expect($("<div></div>").html(dateRangeInputTemplate).find("[ui-date-range]").length).toBe(1);
  });
});
