import "./lightComponentTestUtils";

import fs from "fs";
import path from "path";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {templateInputColor} from "../../../main/resources/js/awe/services/criterion";
import {
  templateButtonCheckbox,
  templateButtonRadio,
  templateInputCheckbox,
  templateRadio
} from "../../../main/resources/js/awe/services/checkboxRadio";
import {calendarInputTemplate, timeInputTemplate} from "../../../main/resources/js/awe/services/dateTime";
import {templateNumeric} from "../../../main/resources/js/awe/services/numeric";
import {templateSelector} from "../../../main/resources/js/awe/services/selector";
import {
  passwordInputTemplate,
  textareaInputTemplate,
  textInputTemplate,
  textViewInputTemplate
} from "../../../main/resources/js/awe/services/text";
import {uploaderInputTemplate} from "../../../main/resources/js/awe/services/uploader";

const angularTemplates = path.join(__dirname, "../../../main/resources/templates/angular");
const readTemplate = name => fs.readFileSync(path.join(angularTemplates, name), "utf8");

/**
 * One row per criterion directive that renders a form control.
 * The "control" selector identifies the element that must carry the test id.
 * "hasId" is false for the controls that never rendered an id attribute (text view, uploader).
 * The "tab" and "wizard" directives are panels, not controls: their parts are covered with the other containers.
 */
const criterionTypes = [
  {directive: "aweInputText", template: textInputTemplate, control: "input[type='text']"},
  {directive: "aweInputPassword", template: passwordInputTemplate, control: "input[type='password']"},
  {directive: "aweInputTextarea", template: textareaInputTemplate, control: "textarea"},
  {directive: "aweInputTextView", template: textViewInputTemplate, control: "span.text-value", hasId: false},
  {directive: "aweInputNumeric", template: templateNumeric, control: "input[ui-numeric]"},
  {directive: "aweInputDate", template: calendarInputTemplate, control: "[ui-date] > input[type='text']"},
  {directive: "aweInputFilteredCalendar", template: calendarInputTemplate, control: "[ui-date] > input[type='text']"},
  {directive: "aweInputTime", template: timeInputTemplate, control: "input[ui-time]"},
  {directive: "aweInputColor", template: templateInputColor, control: ".colorpicker-element > input[type='text']"},
  {directive: "aweInputSelect", template: templateSelector, control: "input[type='hidden'][ui-select2]"},
  {directive: "aweInputSelectMultiple", template: templateSelector, control: "input[type='hidden'][ui-select2]"},
  {directive: "aweInputSuggest", template: templateSelector, control: "input[type='hidden'][ui-select2]"},
  {directive: "aweInputSuggestMultiple", template: templateSelector, control: "input[type='hidden'][ui-select2]"},
  {directive: "aweInputCheckbox", template: templateInputCheckbox, control: "input[type='checkbox']"},
  {directive: "aweInputRadio", template: templateRadio, control: "input[type='radio']"},
  {directive: "aweInputButtonCheckbox", template: templateButtonCheckbox, control: "input[type='checkbox']"},
  {directive: "aweInputButtonRadio", template: templateButtonRadio, control: "input[type='radio']"},
  {directive: "aweInputHidden", template: readTemplate("input/hidden.html"), control: "input[type='hidden']"},
  {directive: "aweInputMarkdownEditor", template: readTemplate("input/markdown-editor.html"), control: "textarea"},
  {directive: "aweInputUploader", template: uploaderInputTemplate, control: "[ngf-select]", hasId: false}
];

describe("criterion test ids", () => {
  it.each(criterionTypes)("$directive marks its real control with the criterion-input hook", ({template, control}) => {
    const root = $("<div></div>").html(template);

    const hooked = root.find(`[data-testid='${TestIds.criterionInput}']`);

    expect(hooked.length).toBe(1);
    expect(hooked.is(control)).toBe(true);
  });

  it.each(criterionTypes)("$directive keeps the criterion-id on the container and the legacy hooks", ({template, hasId}) => {
    const root = $("<div></div>").html(template);

    expect(root.find("[ng-attr-criterion-id]").length).toBe(1);
    expect(root.find("[data-testid] [ng-attr-criterion-id]").length).toBe(0);
    if (hasId !== false) {
      expect(root.find("[ng-attr-id='{{::controller.id}}']").length).toBeGreaterThan(0);
    }
  });

  it("uses the vocabulary constant for every hook it renders in the inline templates", () => {
    const rendered = new Set();
    criterionTypes.forEach(({template}) => {
      $("<div></div>").html(template).find("[data-testid]").each((index, node) => rendered.add(node.getAttribute("data-testid")));
    });

    const vocabulary = Object.keys(TestIds).map(name => TestIds[name]);
    rendered.forEach(value => expect(vocabulary).toContain(value));
  });

  it("exposes the uploaded file name and the clear action of the uploader", () => {
    const root = $("<div></div>").html(uploaderInputTemplate);

    expect(root.find(`.pfi-filename[data-testid='${TestIds.uploadFilename}']`).length).toBe(1);
    expect(root.find(`button.pfi-choose`).attr("data-testid")).toBeUndefined();
    expect(root.find(`button[data-testid='${TestIds.uploadClear}']`).length).toBe(1);
  });

  describe("rendered through the directives", () => {
    let $rootScope;
    let $compile;
    let $httpBackend;
    let $control;
    let $storage;

    beforeEach(() => {
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
          $httpBackend.when("POST", "settings").respond(DefaultSettings);
          $httpBackend.when("POST", "./settings").respond(DefaultSettings);
          $storage.put("controller", {base: {}});
          $storage.put("model", {base: {}});
          $storage.put("api", {base: {}});
        }]);
    });

    function register(id, component) {
      $control.setAddressController({view: "base", component: id}, {
        component, contextMenu: [], dependencies: [], id, optional: false, readonly: false, required: false,
        size: "lg", visible: true, validation: ""
      });
      $control.setAddressModel({view: "base", component: id}, {defaultValues: [], page: 1, records: 0, selected: [], total: 0, values: []});
    }

    it("renders the hook on the input of a text criterion next to the criterion-id", () => {
      $rootScope.firstLoad = true;
      register("UserName", "text");

      const element = $compile("<awe-input-text input-text-id='UserName'></awe-input-text>")($rootScope);
      $rootScope.$digest();

      const input = element.find("input[type='text']");
      expect(element.attr("criterion-id")).toBe("UserName");
      expect(input.attr("data-testid")).toBe("criterion-input");
      expect(input.attr("id")).toBe("UserName");
    });

    it("renders the hook on the hidden input of a select criterion", () => {
      $rootScope.firstLoad = true;
      register("Country", "select");

      const element = $compile("<awe-input-select input-select-id='Country'></awe-input-select>")($rootScope);
      $rootScope.$digest();

      const input = element.find("input[type='hidden'][ui-select2]");
      expect(element.attr("criterion-id")).toBe("Country");
      expect(input.attr("data-testid")).toBe("criterion-input");
    });
  });
});
