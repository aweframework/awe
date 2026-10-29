// Icon-only buttons (#793): style="no-label" draws only the icon of a button, its label becomes its tooltip and its
// accessible name; the button of a grid cell selects its row before its actions
import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe("icon-only buttons", () => {
  let $compile, $rootScope, ActionController, controllers;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    controllers = {};
    inject(["$compile", "$rootScope", "$httpBackend", "Control", "ActionController",
      (_$compile_, _$rootScope_, $httpBackend, Control, _ActionController_) => {
        jest.spyOn(Control, "checkComponent").mockReturnValue(true);
        jest.spyOn(Control, "getAddressModel").mockReturnValue({selected: null, values: []});
        jest.spyOn(Control, "getAddressController").mockImplementation(address => controllers[address.component]);
        ActionController = _ActionController_;
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

  const compileButton = (id, extra = {}) => {
    controllers[id] = {id, component: "button", buttonType: "button", label: "Help", icon: "question-circle",
      visible: true, actions: [], dependencies: [], contextMenu: [], ...extra};
    const element = $compile(`<awe-button button-id='${id}'></awe-button>`)($rootScope.$new());
    $("body").append(element);
    $rootScope.$digest();
    return element.find("button");
  };

  it("draws only the icon of a no-label button, with its label as tooltip and accessible name", () => {
    const button = compileButton("ButHlp", {style: "btn-ghost no-label"});

    expect(button.find(".button-text").length).toBe(0);
    expect(button.find(".button-icon").length).toBeGreaterThan(0);
    expect(button.attr("aria-label")).toBe("Help");
    expect(button.attr("title")).toBe("Help");
    expect(button.hasClass("no-label")).toBe(true);
    expect(button.hasClass("btn-ghost")).toBe(true);
  });

  it("keeps the title of a no-label button as its tooltip", () => {
    const button = compileButton("ButHlp", {style: "no-label", title: "Screen help"});

    expect(button.attr("title")).toBe("Screen help");
    expect(button.attr("aria-label")).toBe("Help");
  });

  it("leaves the tooltip to the help of a no-label button that has one", () => {
    const button = compileButton("ButHlp", {style: "no-label", help: "Opens the help of the screen"});

    expect(button.attr("title")).toBe("");
    expect(button.attr("aria-label")).toBe("Help");
  });

  it("keeps the label of a button without the no-label style and gives it no accessible name of its own", () => {
    const button = compileButton("ButPrn", {label: "Print", style: "btn-primary"});

    expect(button.find(".button-text").text()).toBe("Print");
    expect(button.attr("aria-label")).toBeUndefined();
    expect(button.attr("title")).toBe("");
  });

  describe("button of a grid cell", () => {
    const address = {component: "GrdUsr", view: "report", row: "2", column: "EdtBtn"};
    let cellModel, cellController, gridComponent, calls;

    beforeEach(() => {
      calls = [];
      cellModel = {selected: null, values: [{value: null}]};
      cellController = {align: "center", visible: true, dependencies: [], icon: "lucide:pencil", title: "Edit",
        style: "btn-ghost no-label", actions: [{type: "dialog", target: "PnlUsr"}]};
      gridComponent = {
        model: {selected: []},
        scope: {onSaveRow: jest.fn()},
        checkInitialized: jest.fn(() => true),
        getModel: jest.fn(() => cellModel),
        getController: jest.fn(() => cellController),
        updateCellModel: jest.fn(),
        selectButtonRow: jest.fn(row => calls.push(["select", row]))
      };
      ActionController.addActionList.mockImplementation(actions => calls.push(["actions", actions]));
    });

    const compileCell = () => {
      const parent = angular.element("<div id=\"scope-GrdUsr\"></div>");
      angular.element(document.body).append(parent);
      parent.data("$scope", Object.assign($rootScope.$new(), {component: gridComponent}));
      const scope = Object.assign($rootScope.$new(), {view: "report", context: "screen", cellAddress: JSON.stringify(address)});
      const element = $compile("<awe-column-button cell-address='{{cellAddress}}'></awe-column-button>")(scope);
      $rootScope.$digest();
      return element.find("button");
    };

    it("selects its row and then launches its actions", () => {
      const button = compileCell();

      button.triggerHandler("click");

      expect(calls).toEqual([["select", "2"], ["actions", cellController.actions]]);
    });

    it("takes the title of the column as tooltip and accessible name when the cell has no label", () => {
      const button = compileCell();

      expect(button.find(".button-text").length).toBe(0);
      expect(button.attr("title")).toBe("Edit");
      expect(button.attr("aria-label")).toBe("Edit");
    });

    it("gives no accessible name of its own to a cell button with a label", () => {
      cellModel = {selected: "Open", values: [{value: "Open", label: "Open", title: "Open the user"}]};
      const button = compileCell();

      expect(button.find(".button-text").text()).toBe("Open");
      expect(button.attr("title")).toBe("Open the user");
      expect(button.attr("aria-label")).toBeUndefined();
    });
  });
});
