import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

const MARKUP = "<div ui-date-range='dateOptions' initialized='initialized'><input id='from'/><input id='to'/></div>";
const OPTIONS = {format: "dd/mm/yyyy", language: "en"};

describe("uiDateRange", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let datepickerCalls;

  function compileWithScope(scopeData = {}) {
    const scope = $rootScope.$new();
    Object.assign(scope, {dateOptions: {...OPTIONS}, initialized: true}, scopeData);
    const element = $compile(MARKUP)(scope);
    scope.$digest();
    return {scope, element};
  }

  // Calls made to one of the inputs, as [command, value]
  const callsOf = (inputId) => datepickerCalls.filter(call => call.target === inputId).map(call => [call.command, call.value]);

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
    $.fn.datepicker = jest.fn(function datepicker(command, value) {
      this.each((index, input) => datepickerCalls.push({target: input.id, command, value}));
      return this;
    });
  });

  afterEach(() => {
    try {
      $httpBackend && $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend && $httpBackend.verifyNoOutstandingRequest();
    } catch {
      // These focused specs do not flush settings; keep cleanup best-effort.
    }
    delete $.fn.datepicker;
  });

  it("initializes a datepicker on each input", () => {
    const component = {id: "period", model: {selected: [], values: []}};

    compileWithScope({component});

    expect(component.pluginInitialized).toBe(true);
    expect(datepickerCalls.filter(call => typeof call.command === "object").map(call => call.target)).toEqual(["from", "to"]);
    expect(datepickerCalls[0].command).toEqual(OPTIONS);
  });

  it("does not initialize the datepickers until the criterion is initialized", () => {
    const component = {id: "period", model: {selected: [], values: []}};

    compileWithScope({component, initialized: false});

    expect(datepickerCalls).toEqual([]);
    expect(component.pluginInitialized).toBeUndefined();
  });

  it("shows the dates of the model when the plugin starts", () => {
    const component = {id: "period", model: {selected: ["01/01/2026", "31/01/2026"], values: []}};

    compileWithScope({component});

    expect(callsOf("from")).toContainEqual(["setDate", "01/01/2026"]);
    expect(callsOf("to")).toContainEqual(["setDate", "31/01/2026"]);
  });

  it("limits the end to the start and the start to the end", () => {
    const component = {id: "period", model: {selected: ["01/01/2026", "31/01/2026"], values: []}};

    compileWithScope({component});

    expect(callsOf("from")).toContainEqual(["setEndDate", "31/01/2026"]);
    expect(callsOf("to")).toContainEqual(["setStartDate", "01/01/2026"]);
  });

  it("removes the limits and the dates of an empty range", () => {
    const component = {id: "period", model: {selected: null, values: []}};

    compileWithScope({component});

    expect(callsOf("from")).toContainEqual(["setEndDate", null]);
    expect(callsOf("to")).toContainEqual(["setStartDate", null]);
  });

  it("keeps the datepickers synchronized with the model through the component update API", () => {
    const component = {id: "period", model: {selected: [], values: []}};
    compileWithScope({component});
    datepickerCalls.length = 0;

    component.model.selected = ["05/02/2026", "20/02/2026"];
    component.updateModelSelected();

    expect(callsOf("from")).toContainEqual(["setDate", "05/02/2026"]);
    expect(callsOf("to")).toContainEqual(["setDate", "20/02/2026"]);
  });

  it("does not set a date the input already shows", () => {
    const component = {id: "period", model: {selected: [], values: []}};
    const {element} = compileWithScope({component});
    element.find("#from").val("05/02/2026");
    datepickerCalls.length = 0;

    component.model.selected = ["05/02/2026", ""];
    component.updateModelSelected();

    expect(callsOf("from").filter(call => call[0] === "setDate")).toEqual([]);
  });

  it("updates the limits when one of the dates changes", () => {
    const component = {id: "period", model: {selected: [], values: []}};
    const {element} = compileWithScope({component});
    datepickerCalls.length = 0;

    element.find("#from").val("10/03/2026");
    element.find("#to").val("25/03/2026");
    element.find("#from").trigger("changeDate");

    expect(callsOf("from")).toContainEqual(["setEndDate", "25/03/2026"]);
    expect(callsOf("to")).toContainEqual(["setStartDate", "10/03/2026"]);
  });

  it("tags the popup of each input with the owner criterion", () => {
    const popup = $("<div class='datepicker'><div class='datepicker-days'><table><tbody><tr><td class='day'>1</td></tr></tbody></table></div></div>");
    const component = {id: "period", model: {selected: [], values: []}};
    const {element} = compileWithScope({component});
    element.find("#to").data("datepicker", {picker: popup});

    element.find("#to").trigger("show");

    expect(popup.attr("data-testid")).toBe("datepicker");
    expect(popup.attr("data-testid-owner")).toBe("period");
  });

  it("starts the datepickers again with the new language", () => {
    const component = {id: "period", model: {selected: [], values: []}};
    const {scope} = compileWithScope({component});
    datepickerCalls.length = 0;

    scope.$broadcast("languageChanged", "es");

    expect(callsOf("from").map(call => call[0])).toContain("destroy");
    expect(datepickerCalls.filter(call => typeof call.command === "object").map(call => call.command.language)).toEqual(["es", "es"]);
  });

  it("destroys the datepickers when the element is destroyed", () => {
    const component = {id: "period", model: {selected: [], values: []}};
    const {element} = compileWithScope({component});
    datepickerCalls.length = 0;

    element.triggerHandler("$destroy");

    expect([callsOf("from"), callsOf("to")]).toEqual([[["destroy", undefined]], [["destroy", undefined]]]);
  });
});
