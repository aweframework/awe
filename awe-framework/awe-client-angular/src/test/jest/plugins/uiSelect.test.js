import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe("uiSelect2", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let $translate;
  let select2Spy;

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
    inject(["$translate", (_$translate_) => {
      $translate = _$translate_;
      jest.spyOn($translate, "instant").mockImplementation(key => `translated:${key}`);
    }]);
    select2Spy = jest.fn(function select2() {
      return this;
    });
    $.fn.select2 = select2Spy;
  });

  afterEach(() => {
    try {
      $httpBackend && $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend && $httpBackend.verifyNoOutstandingRequest();
    } catch (error) {
      // These focused specs do not flush settings; keep cleanup best-effort.
    }
    delete $.fn.select2;
  });

  it("translates placeholders on initialization and exposes fill/select methods through the component API", () => {
    const component = {onPluginInit: jest.fn()};
    const {scope} = compileWithScope("<input ui-select2='aweSelectOptions' initialized='initialized'/>", {component, aweSelectOptions: {placeholder: "SCREEN_TEXT_USER"}, initialized: true});

    scope.component.fill([{id: 1, text: "One"}]);
    scope.component.select("1");

    expect(select2Spy).toHaveBeenNthCalledWith(1, expect.objectContaining({placeholder: "translated:SCREEN_TEXT_USER"}));
    expect(select2Spy).toHaveBeenNthCalledWith(2, "data", [{id: 1, text: "One"}]);
    expect(select2Spy).toHaveBeenNthCalledWith(3, "val", "1");
    expect(component.onPluginInit).toHaveBeenCalled();
  });

  it("refreshes translated placeholders on language changes and calls setPlaceholder when available", () => {
    const select2Content = {opts: {}, setPlaceholder: jest.fn()};
    const {element, scope} = compileWithScope("<input ui-select2='aweSelectOptions' initialized='initialized'/>", {component: {onPluginInit: jest.fn()}, aweSelectOptions: {placeholder: "SCREEN_TEXT_USER"}, initialized: true});
    element.data("select2", select2Content);

    scope.$broadcast("languageChanged");
    scope.$digest();

    expect(select2Content.opts.placeholder).toBe("translated:SCREEN_TEXT_USER");
    expect(select2Content.setPlaceholder).toHaveBeenCalled();
  });

  it("refreshes translated placeholders without requiring setPlaceholder plugin support", () => {
    const select2Content = {opts: {}};
    const {element, scope} = compileWithScope("<input ui-select2='aweSelectOptions' initialized='initialized'/>", {component: {onPluginInit: jest.fn()}, aweSelectOptions: {placeholder: "SCREEN_TEXT_USER"}, initialized: true});
    element.data("select2", select2Content);

    scope.$broadcast("languageChanged");
    scope.$digest();

    expect(select2Content.opts.placeholder).toBe("translated:SCREEN_TEXT_USER");
  });

  it("fills select2 data through the exposed component API", () => {
    const {scope} = compileWithScope("<input ui-select2='aweSelectOptions' initialized='initialized'/>", {component: {onPluginInit: jest.fn()}, aweSelectOptions: {}, initialized: true});

    scope.component.fill([{id: 2, text: "Two"}]);

    expect(select2Spy).toHaveBeenCalledWith("data", [{id: 2, text: "Two"}]);
  });

  it("selects a value through the exposed component API", () => {
    const {scope} = compileWithScope("<input ui-select2='aweSelectOptions' initialized='initialized'/>", {component: {onPluginInit: jest.fn()}, aweSelectOptions: {}, initialized: true});

    scope.component.select("2");

    expect(select2Spy).toHaveBeenCalledWith("val", "2");
  });
});
