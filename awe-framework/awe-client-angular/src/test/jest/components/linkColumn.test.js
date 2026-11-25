// Link column (#993): without actions the value of the cell is an external address opened in a new tab; with actions
// the link selects its row and launches them, as the button column does
import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe("link column", () => {
  const address = {component: "GrdTsk", view: "report", row: "2", column: "FlwLnk"};
  let $compile, $rootScope, ActionController, cellModel, cellController, gridComponent, calls;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$compile", "$rootScope", "$httpBackend", "$translate", "Control", "ActionController",
      (_$compile_, _$rootScope_, $httpBackend, $translate, Control, _ActionController_) => {
        jest.spyOn(Control, "checkComponent").mockReturnValue(true);
        jest.spyOn($translate, "instant")
          .mockImplementation(key => key === "LINK_OPENS_IN_NEW_TAB" ? "(opens in a new tab)" : key);
        ActionController = _ActionController_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $rootScope.status = {loading: false};
        $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
      }]);
    calls = [];
    cellModel = {selected: "F12", values: [{value: "F12", label: "Nightly flow"}]};
    cellController = {align: "left", visible: true, dependencies: [], actions: []};
    gridComponent = {
      model: {selected: []},
      scope: {onSaveRow: jest.fn()},
      checkInitialized: jest.fn(() => true),
      getModel: jest.fn(() => cellModel),
      getController: jest.fn(() => cellController),
      updateCellModel: jest.fn(),
      selectButtonRow: jest.fn(row => calls.push(["select", row]))
    };
    jest.spyOn(ActionController, "addActionList").mockImplementation(actions => calls.push(["actions", actions]));
  });

  afterEach(() => {
    $("body").empty();
    jest.restoreAllMocks();
  });

  const compileCell = () => {
    const parent = angular.element("<div id=\"scope-GrdTsk\"></div>");
    angular.element(document.body).append(parent);
    parent.data("$scope", Object.assign($rootScope.$new(), {component: gridComponent}));
    const scope = Object.assign($rootScope.$new(), {view: "report", context: "screen", cellAddress: JSON.stringify(address)});
    const element = $compile("<awe-column-link cell-address='{{cellAddress}}'></awe-column-link>")(scope);
    $rootScope.$digest();
    return element.find("a");
  };

  it("opens the value of a cell without actions as an external address in a new tab", () => {
    cellModel = {selected: "https://www.almis.com", values: [{value: "https://www.almis.com", label: "Almis"}]};

    const link = compileCell();

    expect(link.length).toBe(1);
    expect(link.attr("href")).toBe("https://www.almis.com");
    expect(link.attr("target")).toBe("_blank");
    expect(link.attr("rel")).toBe("noreferrer");
    expect(link.find(".column-link-label").text()).toBe("Almis");
  });

  it("marks a cell without actions as opening a new tab with a decorative icon and a hidden text", () => {
    cellModel = {selected: "https://www.almis.com", values: [{value: "https://www.almis.com", label: "Almis"}]};

    const link = compileCell();

    expect(link.find(".sr-only").text().trim()).toBe("(opens in a new tab)");
    expect(link.text().replace(/\s+/g, " ").trim()).toBe("Almis (opens in a new tab)");
    const icon = link.find("svg");
    expect(icon.length).toBe(1);
    expect(icon.attr("aria-hidden")).toBe("true");
    expect(icon.find("use").attr("href")).toMatch(/#external-link$/);
  });

  describe("with actions", () => {
    beforeEach(() => {
      cellController.actions = [{type: "screen", target: "flow-run"}];
    });

    it("draws the label as a focusable link without an external address", () => {
      const link = compileCell();

      expect(link.length).toBe(1);
      expect(link.attr("href")).toBeUndefined();
      expect(link.attr("role")).toBe("link");
      expect(link.attr("tabindex")).toBe("0");
      expect(link.text().trim()).toBe("Nightly flow");
      expect(link.attr("title")).toBeUndefined();
    });

    it("has no new tab icon nor hidden new tab text", () => {
      const link = compileCell();

      expect(link.find("svg").length).toBe(0);
      expect(link.find(".sr-only").length).toBe(0);
      expect(link.text().trim()).toBe("Nightly flow");
    });

    it("shows the title of the column as its tooltip when the cell has none", () => {
      cellController.title = "Open the run detail";

      expect(compileCell().attr("title")).toBe("Open the run detail");
    });

    it("selects its row and then launches the actions of the column when clicked", () => {
      compileCell().triggerHandler("click");

      expect(calls).toEqual([["select", "2"], ["actions", cellController.actions]]);
    });

    it("selects its row and then launches the actions of the column when Enter is pressed", () => {
      compileCell().triggerHandler({type: "keydown", key: "Enter"});

      expect(calls).toEqual([["select", "2"], ["actions", cellController.actions]]);
    });

    it("launches nothing and leaves the tab order when its column is disabled", () => {
      cellController.disabled = true;

      const link = compileCell();
      link.triggerHandler("click");
      link.triggerHandler({type: "keydown", key: "Enter"});

      expect(calls).toEqual([]);
      expect(link.attr("aria-disabled")).toBe("true");
      expect(link.attr("tabindex")).toBe("-1");
    });
  });
});
