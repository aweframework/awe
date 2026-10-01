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

  describe("test id hooks", () => {
    const escapeMarkup = text => text;

    let originalDefaults;

    beforeEach(() => {
      originalDefaults = select2Spy.defaults;
    });

    afterEach(() => {
      select2Spy.defaults = originalDefaults;
    });

    // Behaves like select2 3.5.7: the instance is stored in the element data synchronously while the plugin initializes
    function stubSelect2Instance(container, dropdown) {
      select2Spy.mockImplementation(function select2(method) {
        if (typeof method === "object") {
          $(this).data("select2", {container, dropdown});
          return this;
        }
        if (method === "container") {
          return container;
        }
        if (method === "dropdown") {
          return dropdown;
        }
        return this;
      });
      select2Spy.defaults = {
        formatResult: function formatResult(result, label, query, escape) {
          return escape(result.text);
        },
        formatSelection: function formatSelection(data, container, escape) {
          return data ? escape(data.text) : undefined;
        }
      };
    }

    it("wraps the default formatters so options and chosen values carry the vocabulary", () => {
      stubSelect2Instance($("<div></div>"), $("<div></div>"));
      compileWithScope("<input id='country' ui-select2='aweSelectOptions' initialized='initialized'/>",
        {component: {id: "country", onPluginInit: jest.fn()}, aweSelectOptions: {}, initialized: true});

      const options = select2Spy.mock.calls[0][0];
      const label = $("<div></div>");
      const chosen = $("<span class='select2-chosen'></span>");

      expect(options.formatResult({text: "Spain"}, label, {term: ""}, escapeMarkup)).toBe("Spain");
      expect(options.formatSelection({text: "Spain"}, chosen, escapeMarkup)).toBe("Spain");
      expect(label.attr("data-testid")).toBe("select-option");
      expect(label.attr("data-testid-owner")).toBe("country");
      expect(chosen.attr("data-testid")).toBe("select-value");
    });

    it("tags the container and its search input when the plugin is created, and the dropdown on select2-open", () => {
      const container = $("<div class='select2-container'><input class='select2-input'/></div>");
      const dropdown = $("<div class='select2-drop'><input class='select2-input'/></div>");
      stubSelect2Instance(container, dropdown);
      const {element} = compileWithScope("<input id='country' ui-select2='aweSelectOptions' initialized='initialized'/>",
        {component: {id: "country", onPluginInit: jest.fn()}, aweSelectOptions: {}, initialized: true});

      expect(container.attr("data-testid")).toBe("select");
      expect(container.attr("data-testid-owner")).toBe("country");
      expect(container.find(".select2-input").attr("data-testid")).toBe("select-search");
      expect(dropdown.attr("data-testid")).toBeUndefined();

      element.trigger($.Event("select2-open"));

      expect(dropdown.attr("data-testid")).toBe("select-dropdown");
      expect(dropdown.attr("data-testid-owner")).toBe("country");
      expect(dropdown.find(".select2-input").attr("data-testid")).toBe("select-search");

      element.trigger($.Event("select2-close"));

      expect(dropdown.attr("data-testid")).toBeUndefined();
    });
  });
});
