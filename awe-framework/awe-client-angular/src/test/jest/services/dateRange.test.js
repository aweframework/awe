import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {isDateRangeValid, parsePresets, resolvePreset} from "../../../main/resources/js/awe/services/dateRange";

const TODAY = new Date(2026, 9, 12);
const JAN_FIRST = "01/01/2026";
const JAN_LAST = "31/01/2026";

describe('awe-framework/awe-client-angular/src/test/jest/services/dateRange.js', function () {

  describe('parsePresets', function () {
    it('splits a list by commas or spaces, upper-casing and dropping the empty ones', function () {
      expect(parsePresets("1m, 3M  ytd,,all")).toEqual(["1M", "3M", "YTD", "ALL"]);
    });

    it('returns an empty list when there are no presets', function () {
      expect([parsePresets(undefined), parsePresets(null), parsePresets("")]).toEqual([[], [], []]);
    });
  });

  describe('resolvePreset', function () {
    it.each([
      ["TODAY", "12/10/2026", "12/10/2026"],
      ["7D", "05/10/2026", "12/10/2026"],
      ["2W", "28/09/2026", "12/10/2026"],
      ["1M", "12/09/2026", "12/10/2026"],
      ["3M", "12/07/2026", "12/10/2026"],
      ["12M", "12/10/2025", "12/10/2026"],
      ["3Y", "12/10/2023", "12/10/2026"],
      ["YTD", "01/01/2026", "12/10/2026"],
      ["all", "", ""],
    ])('resolves %s from the current date', function (code, from, to) {
      expect(resolvePreset(code, TODAY)).toEqual({from, to});
    });

    it.each([
      ["1D", "11/10/2026"], ["1W", "05/10/2026"], ["1M", "12/09/2026"], ["1Y", "12/10/2025"]
    ])('subtracts one unit of the period %s (days, weeks, months and years)', function (code, from) {
      expect(resolvePreset(code, TODAY)).toEqual({from, to: "12/10/2026"});
    });

    it('keeps the last day of a shorter month when subtracting months', function () {
      expect(resolvePreset("1M", new Date(2026, 2, 31))).toEqual({from: "28/02/2026", to: "31/03/2026"});
    });

    it('keeps the last day of February when subtracting years from a leap day', function () {
      expect(resolvePreset("1Y", new Date(2024, 1, 29))).toEqual({from: "28/02/2023", to: "29/02/2024"});
    });

    it('uses the system date when no date is given', function () {
      jest.useFakeTimers({now: new Date(2026, 4, 20), doNotFake: ["setTimeout", "clearTimeout", "setInterval", "clearInterval", "nextTick", "queueMicrotask"]});
      try {
        expect(resolvePreset("TODAY")).toEqual({from: "20/05/2026", to: "20/05/2026"});
      } finally {
        jest.useRealTimers();
      }
    });

    it('returns null for an unknown preset', function () {
      expect(resolvePreset("FOREVER", TODAY)).toBeNull();
    });
  });

  describe('isDateRangeValid', function () {
    it.each([
      [JAN_FIRST, JAN_LAST, true],
      ["15/01/2026", "15/01/2026", true],
      ["15/01/2026", "14/01/2026", false],
      ["", "14/01/2026", true],
      ["15/01/2026", "", true],
      ["", "", true],
      [null, undefined, true],
      ["31/12/2025", "01/01/2026", true],
      ["not a date", "14/01/2026", false],
    ])('checks that %s is not after %s', function (from, to, valid) {
      expect(isDateRangeValid(from, to)).toBe(valid);
    });
  });

  describe('DateRange component', function () {
    let $rootScope, $control, $translate, $rules, DateRange, scope, model, controller, component;

    beforeEach(function () {
      angular.mock.module('aweApplication');

      inject(["$injector", function ($injector) {
        $rootScope = $injector.get('$rootScope');
        $control = $injector.get('Control');
        $translate = $injector.get('$translate');
        $rules = $injector.get('ValidationRules');
        DateRange = $injector.get('DateRange');
        $injector.get('$httpBackend').when('POST', 'settings').respond(DefaultSettings);
      }]);

      model = {selected: [], values: []};
      controller = {id: "period", visible: true, component: "date-range", presets: "1M, YTD, ALL, FOREVER"};
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue(model);
      jest.spyOn($control, "getAddressController").mockReturnValue(controller);
      jest.spyOn($control, "changeModelAttribute").mockImplementation(() => null);
      jest.useFakeTimers({now: TODAY, doNotFake: ["setTimeout", "clearTimeout", "setInterval", "clearInterval", "nextTick", "queueMicrotask"]});

      scope = $rootScope.$new();
      scope.view = "report";
      scope.context = "screen";
      component = new DateRange(scope, "period", angular.element("<div></div>"));
    });

    afterEach(function () {
      jest.useRealTimers();
    });

    it('initializes the criterion with the presets that exist', function () {
      expect(component.asDateRange()).toBe(true);

      expect(scope.presets.map(preset => preset.code)).toEqual(["1M", "YTD", "ALL"]);
    });

    it('does not initialize when the criterion cannot be initialized', function () {
      $control.checkComponent.mockReturnValue(false);

      expect(component.asDateRange()).toBe(false);
    });

    it('shows the dates of the model as the start and the end of the range', function () {
      model.selected = [JAN_FIRST, JAN_LAST];

      component.asDateRange();

      expect(scope.range).toEqual({from: JAN_FIRST, to: JAN_LAST});
    });

    it('shows an empty range when the model has no dates', function () {
      model.selected = null;

      component.asDateRange();

      expect(scope.range).toEqual({from: "", to: ""});
    });

    it('sends the start and the end of the range as two parameters', function () {
      model.selected = [JAN_FIRST, JAN_LAST];
      component.asDateRange();

      expect(component.getData()).toEqual({"period.from": JAN_FIRST, "period.to": JAN_LAST});
    });

    it('sends null for the dates that are not set', function () {
      model.selected = ["", JAN_LAST];
      component.asDateRange();

      expect(component.getData()).toEqual({"period.from": null, "period.to": JAN_LAST});
      model.selected = [];
      expect(component.getData()).toEqual({"period.from": null, "period.to": null});
    });

    it('shows the range as the visible value', function () {
      model.selected = [JAN_FIRST, JAN_LAST];
      component.asDateRange();

      expect(component.getVisibleValue()).toBe(JAN_FIRST + " - " + JAN_LAST);
      model.selected = ["", JAN_LAST];
      expect(component.getVisibleValue()).toBe(JAN_LAST);
    });

    it('adds the text of the range to the data when it is printable', function () {
      controller.printable = true;
      model.selected = [JAN_FIRST, JAN_LAST];
      component.asDateRange();

      expect(component.getPrintData()).toEqual({
        "period.from": JAN_FIRST,
        "period.to": JAN_LAST,
        "period.data": {text: JAN_FIRST + " - " + JAN_LAST}
      });
    });

    it('stores the range in the model when a date is edited', function () {
      component.asDateRange();
      scope.range.from = "05/01/2026";
      scope.range.to = "20/01/2026";

      scope.onRangeChange();

      expect(model.selected).toEqual(["05/01/2026", "20/01/2026"]);
      expect($control.changeModelAttribute).toHaveBeenCalledWith(component.address, {selected: ["05/01/2026", "20/01/2026"]}, true);
    });

    it('does not publish a model change when the range is the same', function () {
      model.selected = [JAN_FIRST, JAN_LAST];
      component.asDateRange();
      model.previous = model.selected;

      scope.onRangeChange();

      expect($control.changeModelAttribute).not.toHaveBeenCalled();
    });

    it('fills both dates from the current date when a preset is applied', function () {
      component.asDateRange();

      scope.applyPreset("1M");

      expect(scope.range).toEqual({from: "12/09/2026", to: "12/10/2026"});
      expect(model.selected).toEqual(["12/09/2026", "12/10/2026"]);
    });

    it('clears both dates with the all preset', function () {
      model.selected = [JAN_FIRST, JAN_LAST];
      component.asDateRange();

      scope.applyPreset("ALL");

      expect(model.selected).toEqual(["", ""]);
    });

    it('marks the preset applied as active', function () {
      component.asDateRange();
      expect(scope.isPresetActive("1M")).toBe(false);

      scope.applyPreset("1M");

      expect([scope.isPresetActive("1M"), scope.isPresetActive("YTD")]).toEqual([true, false]);
    });

    it('clears the active preset when a date is edited', function () {
      component.asDateRange();
      scope.applyPreset("1M");

      scope.range.from = "15/09/2026";
      scope.onRangeChange();

      expect(scope.isPresetActive("1M")).toBe(false);
    });

    it('updates the range shown when the model changes', function () {
      component.asDateRange();
      scope.applyPreset("1M");

      model.selected = [JAN_FIRST, JAN_LAST];
      component.onSelectedChanged();

      expect(scope.range).toEqual({from: JAN_FIRST, to: JAN_LAST});
      expect(scope.isPresetActive("1M")).toBe(false);
    });

    it('updates the datepickers when the model changes and the plugin is initialized', function () {
      component.asDateRange();
      component.pluginInitialized = true;
      component.updateModelSelected = jest.fn();

      component.onSelectedChanged();

      expect(component.updateModelSelected).toHaveBeenCalled();
    });

    it('translates the presets and falls back to their code', function () {
      component.asDateRange();
      jest.spyOn($translate, "instant").mockImplementation(key => key === "DATE_RANGE_PRESET_1M" ? "1 month" : key);

      expect([scope.presetLabel("1M"), scope.presetLabel("YTD")]).toEqual(["1 month", "YTD"]);
    });

    it('validates that the end of the range is not before its start', function () {
      component.asDateRange();

      expect(controller.validationRules.dateRange).toBe(true);
      expect($rules.validate("dateRange", [JAN_FIRST, JAN_LAST], true, {view: "report", component: "period"})).toBeNull();
      expect($rules.validate("dateRange", ["15/01/2026", "14/01/2026"], true, {view: "report", component: "period"}))
        .toEqual({message: "VALIDATOR_MESSAGE_DATE_RANGE"});
      expect($rules.validate("dateRange", ["", JAN_LAST], true, {view: "report", component: "period"})).toBeNull();
    });

    it('requires both dates when the criterion is required', function () {
      component.asDateRange();

      expect($rules.validate("required", ["", JAN_LAST], {value: true}, {view: "report", component: "period"}))
        .toEqual({message: "VALIDATOR_MESSAGE_REQUIRED"});
      expect($rules.validate("required", [JAN_FIRST, JAN_LAST], {value: true}, {view: "report", component: "period"})).toBeNull();
    });
  });
});
