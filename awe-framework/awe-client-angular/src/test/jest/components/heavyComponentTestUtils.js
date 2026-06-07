import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

export const panelValues = [
  {label: "Step 1", value: "1"},
  {label: "Step 2", value: "2"},
  {label: "Step 3", value: "3"}
];

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

export function createPanelModel(selected = "1") {
  return {
    page: 1,
    records: 3,
    selected,
    total: 1,
    values: clone(panelValues)
  };
}

export function createPanelController(id, component, extra = {}) {
  return {
    id,
    component,
    contextMenu: [],
    dependencies: [],
    icon: "search",
    label: "SCREEN_TEXT_USER",
    printable: true,
    size: "lg",
    style: "no-label",
    visible: true,
    ...extra
  };
}

export function createAction(parameters = {}) {
  return {
    attr: jest.fn(name => {
      switch (name) {
        case "parameters": return parameters;
        case "id": return "componentId";
        case "async":
        case "silent": return true;
        default: return undefined;
      }
    }),
    accept: jest.fn()
  };
}

export function initHeavyComponentTest() {
  const refs = {};
  angular.mock.module("aweApplication");

  inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage", "AweUtilities",
    (_$rootScope_, _$compile_, _$httpBackend_, _Control_, _Storage_, _AweUtilities_) => {
      refs.$rootScope = _$rootScope_;
      refs.$compile = _$compile_;
      refs.$httpBackend = _$httpBackend_;
      refs.$control = _Control_;
      refs.$storage = _Storage_;
      refs.$utilities = _AweUtilities_;

      refs.$rootScope.view = "base";
      refs.$rootScope.context = "screen";
      refs.$rootScope.status = {loading: false};

      $.fn.tabdrop = $.fn.tabdrop || jest.fn(function () { return this; });

      refs.$httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en-GB"});
      refs.$httpBackend.when("GET", "grid/row").respond("<div></div>");
      refs.$httpBackend.when("GET", "grid/pagination").respond("<div></div>");
      refs.$httpBackend.whenGET(/^grid\//).respond("<div></div>");
    }]);
  return refs;
}

export function cleanupHeavyComponentTest() {
  angular.element(document.body).removeClass("mmc mme");
  jest.restoreAllMocks();
}

export function stubPanelComponent(refs, model, controller) {
  jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
  jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
  jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
  jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
  jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
  jest.spyOn(refs.$utilities, "checkAddress").mockReturnValue(true);
  jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
  refs.$utilities.timeout.cancel = jest.fn();
  jest.spyOn(refs.$utilities, "publishFromScope").mockImplementation(jest.fn());
  jest.spyOn(refs.$utilities, "publishDelayedFromScope").mockImplementation(jest.fn());
}
