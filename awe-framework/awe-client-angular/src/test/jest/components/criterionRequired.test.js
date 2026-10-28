import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

// A required criterion without label takes no label row and shows the required state in the field (#898)
describe("required criterion without label", () => {
  let $rootScope, $compile, $httpBackend, $control, $storage;

  beforeEach(() => {
    // The calendar plugin needs a locale of its own, and it is not the subject of these tests
    jest.spyOn($.fn, "datepicker").mockImplementation(function () { return this; });
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage",
      (_$rootScope_, _$compile_, _$httpBackend_, _Control_, _Storage_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $control = _Control_;
        $storage = _Storage_;
        $rootScope.view = "base";
        $rootScope.context = "screen";
        $rootScope.firstLoad = true;
        $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
        $httpBackend.when("POST", "./settings").respond({...DefaultSettings, language: "en"});
        $storage.put("controller", {base: {}});
        $storage.put("model", {base: {}});
        $storage.put("api", {base: {}});
      }]);
  });

  function render(directive, component, attributes = {}) {
    $control.setAddressController({view: "base", component: "Field"}, {
      component, contextMenu: [], dependencies: [], id: "Field", optional: false, readonly: false, required: true,
      size: "lg", visible: true, validation: "required", placeholder: "Username", ...attributes
    });
    $control.setAddressModel({view: "base", component: "Field"}, {defaultValues: [], page: 1, records: 0, selected: [], total: 0, values: []});
    const element = $compile(`<${directive} ${directive.replace("awe-", "")}-id="Field"></${directive}>`)($rootScope);
    $rootScope.$digest();
    return element;
  }

  // Criteria whose focusable element is an input with the placeholder
  const inputCriteria = [
    ["text", "awe-input-text", "text", "input[type='text']"],
    ["password", "awe-input-password", "password", "input[type='password']"],
    ["textarea", "awe-input-textarea", "textarea", "textarea"],
    ["numeric", "awe-input-numeric", "numeric", "input[ui-numeric]"],
    ["date", "awe-input-date", "date", "[ui-date] > input[type='text']"],
    ["time", "awe-input-time", "time", "input[ui-time]"]
  ];

  describe.each(inputCriteria)("%s", (name, directive, component, control) => {
    it("draws no label row and appends the asterisk to the placeholder", () => {
      const element = render(directive, component);

      expect(element.find("label.control-label").length).toBe(0);
      expect(element.find(control).attr("placeholder")).toBe("Username *");
    });

    it("marks the focusable input as required for assistive technology", () => {
      const element = render(directive, component);

      expect(element.find(control).attr("aria-required")).toBe("true");
    });

    it("keeps the label and the plain placeholder when it has a label", () => {
      const element = render(directive, component, {label: "Name"});

      expect(element.find("label.control-label").text().trim()).toBe("Name");
      expect(element.find(control).attr("placeholder")).toBe("Username");
    });

    it("is not marked as required without the required flag", () => {
      const element = render(directive, component, {required: false});

      expect(element.find(control).attr("placeholder")).toBe("Username");
      expect(element.find(control).attr("aria-required")).toBeUndefined();
    });
  });

  it("keeps the label row of a criterion with a left label and no text", () => {
    const element = render("awe-input-text", "text", {leftLabel: "10"});

    expect(element.find("label.control-label").length).toBe(1);
  });

  it("keeps the label row of a criterion with help and no text, so the help icon stays", () => {
    const element = render("awe-input-text", "text", {help: "HELP_TEXT"});

    expect(element.find("label.control-label").length).toBe(1);
    expect(element.find("label.control-label .help-target").length).toBe(1);
  });

  it("keeps the label row of a criterion with a label hidden by the no-label style", () => {
    const element = render("awe-input-text", "text", {label: "Name", style: "no-label"});

    expect(element.find("label.control-label").length).toBe(1);
    expect(element.find("input[type='text']").attr("placeholder")).toBe("Username *");
  });

  // The required state of the select2 plugin itself (placeholder, aria-required) is covered in uiSelect.test.js
  describe.each([
    ["select", "awe-input-select", "select"],
    ["multiple select", "awe-input-select-multiple", "select-multiple"],
    ["suggest", "awe-input-suggest", "suggest"],
    ["multiple suggest", "awe-input-suggest-multiple", "suggest-multiple"]
  ])("%s", (name, directive, component) => {
    it("draws no label row", () => {
      const element = render(directive, component);

      expect(element.find("label.control-label").length).toBe(0);
    });

    it("keeps its label when it has one", () => {
      const element = render(directive, component, {label: "Name"});

      expect(element.find("label.control-label").text().trim()).toBe("Name");
    });
  });
});
