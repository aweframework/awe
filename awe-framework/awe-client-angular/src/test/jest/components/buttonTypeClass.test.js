// Class of a button by its type (#937): submit draws as primary and reset as default (Bootstrap secondary), as React
// draws them, so neither takes the default button tokens; a normal button takes no btn-* class of its own
import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe("button class by type", () => {
  let $compile, $rootScope, controllers;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    controllers = {};
    inject(["$compile", "$rootScope", "$httpBackend", "Control", "ActionController",
      (_$compile_, _$rootScope_, $httpBackend, Control, ActionController) => {
        jest.spyOn(Control, "checkComponent").mockReturnValue(true);
        jest.spyOn(Control, "getAddressModel").mockReturnValue({selected: null, values: []});
        jest.spyOn(Control, "getAddressController").mockImplementation(address => controllers[address.component]);
        jest.spyOn(ActionController, "addActionList").mockImplementation(() => null);
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $rootScope.status = {loading: false};
        $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
      }]);
  });

  afterEach(() => {
    $("body").empty();
    jest.restoreAllMocks();
  });

  const compileButton = buttonType => {
    const id = "But" + buttonType;
    controllers[id] = {id, component: "button", buttonType, label: "Label", visible: true, actions: [],
      dependencies: [], contextMenu: []};
    const element = $compile(`<awe-button button-id='${id}'></awe-button>`)($rootScope.$new());
    $("body").append(element);
    $rootScope.$digest();
    return element.find("button");
  };

  it("draws a reset button as default", () => {
    const button = compileButton("reset");

    expect(button.hasClass("btn-default")).toBe(true);
    expect(button.hasClass("btn-primary")).toBe(false);
  });

  it("draws a submit button as primary", () => {
    expect(compileButton("submit").hasClass("btn-primary")).toBe(true);
  });

  it("gives a normal button no btn-* class of its own", () => {
    const button = compileButton("button");

    expect(button.hasClass("btn-default")).toBe(false);
    expect(button.hasClass("btn-primary")).toBe(false);
  });
});
