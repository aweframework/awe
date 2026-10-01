import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe("uiDate", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let datepickerCalls;
  let dateEvents;

  function compileWithScope(markup, scopeData = {}) {
    const scope = $rootScope.$new();
    Object.assign(scope, scopeData);
    const element = $compile(markup)(scope);
    scope.$digest();
    return {scope, element};
  }

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", (_$rootScope_, _$compile_, _$httpBackend_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $rootScope.view = "base";
      $rootScope.context = "screen";
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
    datepickerCalls = [];
    dateEvents = {};
    $.fn.datepicker = jest.fn(function datepicker(command, value) {
      datepickerCalls.push([command, value]);
      return {on: (event, handler) => {
        dateEvents[event] = handler;
        return this;
      }};
    });
  });

  afterEach(() => {
    try {
      $httpBackend && $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend && $httpBackend.verifyNoOutstandingRequest();
    } catch (error) {
      // These focused specs do not flush settings; keep cleanup best-effort.
    }
    delete $.fn.datepicker;
  });

  it("initializes datepicker methods and maps changeDate values into the component model", () => {
    const component = {model: {selected: "23/10/1978", values: []}};
    compileWithScope("<input ui-date='dateOptions' initialized='initialized'/>", {component, dateOptions: {format: "dd/mm/yyyy", language: "en"}, initialized: true});

    dateEvents.changeDate({format: () => "15/11/2020"});

    expect(component.pluginInitialized).toBe(true);
    expect(component.model.selected).toBe("15/11/2020");
    expect(datepickerCalls[0][0]).toEqual({format: "dd/mm/yyyy", language: "en"});
  });

  it("keeps selected model values synchronized through the component update API", () => {
    const component = {model: {selected: null, values: []}};
    compileWithScope("<input ui-date='dateOptions' initialized='initialized'/>", {component, dateOptions: {format: "dd/mm/yyyy", language: "en"}, initialized: true});

    component.model.selected = "20/05/2026";
    component.updateModelSelected();
    component.updateModelValues();

    expect(datepickerCalls).toContainEqual(["setDate", "20/05/2026"]);
    expect(datepickerCalls).toContainEqual(["update", undefined]);
  });

  it("sets an empty date when selected model value is cleared", () => {
    const component = {model: {selected: null, values: []}};
    compileWithScope("<input ui-date='dateOptions' initialized='initialized'/>", {component, dateOptions: {format: "dd/mm/yyyy"}, initialized: true});

    component.updateModelSelected();

    expect(datepickerCalls).toContainEqual(["setDate", null]);
  });

  it("keeps plugin initialized when values update without a selected date", () => {
    const component = {model: {selected: null, values: []}};
    compileWithScope("<input ui-date='dateOptions' initialized='initialized'/>", {component, dateOptions: {format: "dd/mm/yyyy"}, initialized: true});

    component.updateModelValues();

    expect(component.pluginInitialized).toBe(true);
    expect(datepickerCalls).toContainEqual(["update", undefined]);
  });

  it("tags the datepicker popup with its owner when the plugin shows it", () => {
    const popup = $("<div class='datepicker'><div class='datepicker-days'><table><tbody><tr><td class='day active'>1</td></tr></tbody></table></div></div>");
    const component = {id: "birthDate", model: {selected: null, values: []}};
    const {element} = compileWithScope("<input id='birthDate' ui-date='dateOptions' initialized='initialized'/>", {component, dateOptions: {format: "dd/mm/yyyy"}, initialized: true});
    element.data("datepicker", {picker: popup});

    element.trigger("show");

    expect(popup.attr("data-testid")).toBe("datepicker");
    expect(popup.attr("data-testid-owner")).toBe("birthDate");
    expect(popup.find("td.day").attr("data-testid")).toBe("datepicker-day");
    expect(popup.find("td.day").attr("data-selected")).toBe("true");
  });
});
