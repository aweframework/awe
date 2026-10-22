// Source-traceable focused Jest parity for components/tab.js.
import {
  cleanupHeavyComponentTest,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  panelValues,
  stubPanelComponent
} from "./heavyComponentTestUtils";

describe("components/tab.js", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it("migrates selection, disabled click, active state, and print data behavior", () => {
    const model = createPanelModel("1");
    const controller = createPanelController("tabId", "tab");
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-tab input-tab-id='tabId'></awe-input-tab>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    expect(element.find(".nav-tabs").length).toBe(1);
    expect(element.find(".tab-content").length).toBe(1);

    const scope = element.isolateScope() || element.scope();
    const controllerApi = element.controller("aweInputTab");

    expect(controllerApi.isActive("1")).toBe(true);
    expect(controllerApi.isActive("3")).toBe(false);

    scope.isDisabled = () => false;
    scope.clickTab("3");
    expect(model.selected).toBe("3");
    expect(refs.$utilities.publishFromScope).toHaveBeenCalledWith("resize-action", {}, scope);

    scope.isDisabled = () => true;
    scope.clickTab("2");
    expect(model.selected).toBe("3");

    expect(scope.component.getPrintData()).toEqual({
      tabId: "3",
      "tabId.data": {text: "Step 3", all: panelValues}
    });
  });

  function compileTab(model = createPanelModel("1"), controller = createPanelController("tabId", "tab")) {
    stubPanelComponent(refs, model, controller);
    const element = refs.$compile("<awe-input-tab input-tab-id='tabId'></awe-input-tab>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    return {element, model, scope: element.isolateScope() || element.scope(), controllerApi: element.controller("aweInputTab")};
  }

  it("initializes the tab wrapper and keeps the configured selected panel", () => {
    const {element, model} = compileTab(createPanelModel("2"));

    expect(element.find(".nav-tabs").length).toBe(1);
    expect(element.find(".tab-content").length).toBe(1);
    expect(model.selected).toBe("2");
  });

  it("selects a tab and publishes a resize action", () => {
    const {model, scope} = compileTab(createPanelModel("1"));
    scope.isDisabled = () => false;

    scope.clickTab("3");

    expect(model.selected).toBe("3");
    expect(refs.$utilities.publishFromScope).toHaveBeenCalledWith("resize-action", {}, scope);
  });

  it("does not select a tab when the component is disabled", () => {
    const {model, scope} = compileTab(createPanelModel("1"));
    scope.isDisabled = () => true;

    scope.clickTab("2");

    expect(model.selected).toBe("1");
    expect(refs.$utilities.publishFromScope).not.toHaveBeenCalled();
  });

  it("reports active panel state through the directive controller", () => {
    const {controllerApi} = compileTab(createPanelModel("2"));

    expect(controllerApi.isActive("1")).toBe(false);
    expect(controllerApi.isActive("2")).toBe(true);
  });

  it("falls back to the first tab when the selected value is not in the value list", () => {
    const {model} = compileTab(createPanelModel("missing"));

    expect(model.selected).toBe("1");
  });

  it("returns printable tab data when the selected panel has a label", () => {
    const {scope} = compileTab(createPanelModel("2"), createPanelController("tabId", "tab", {printable: true}));

    expect(scope.component.getPrintData()).toEqual({
      tabId: "2",
      "tabId.data": {text: "Step 2", all: panelValues}
    });
  });

  it("omits label data from print output when the controller is not printable", () => {
    const {scope} = compileTab(createPanelModel("2"), createPanelController("tabId", "tab", {printable: false}));

    expect(scope.component.getPrintData()).toEqual({tabId: "2"});
  });

  it("publishes resize from the active tab scope after selecting a tab", () => {
    const {scope} = compileTab(createPanelModel("1"));
    scope.isDisabled = () => false;

    scope.clickTab("2");

    expect(refs.$utilities.publishFromScope).toHaveBeenCalledWith("resize-action", {}, scope);
  });

  it("marks the tab that holds invalid fields and clears the mark when they are fixed", () => {
    const {element, controllerApi} = compileTab(createPanelModel("1"));
    const tabs = () => [...element[0].querySelectorAll("li[data-testid='tab']")];

    controllerApi.setInvalid("2", true);
    refs.$rootScope.$digest();
    expect(tabs().map(tab => tab.getAttribute("data-invalid"))).toEqual(["false", "true", "false"]);
    expect(tabs()[1].classList.contains("tab-invalid")).toBe(true);

    controllerApi.setInvalid("2", false);
    refs.$rootScope.$digest();
    expect(tabs().map(tab => tab.getAttribute("data-invalid"))).toEqual(["false", "false", "false"]);
  });

  describe("maximize test hooks (#799)", () => {
    const hook = id => `[data-testid='${id}']`;
    const compileMaximizable = (maximize = true) =>
      compileTab(createPanelModel("1"), createPanelController("tabId", "tab", {maximize}));

    it("renders the tab component and its maximize button as test hooks that follow the maximized state", () => {
      const target = $("<div class='maximize-target'><div class='sibling'>other</div></div>").appendTo(document.body);
      const {element} = compileMaximizable();
      target.append(element);
      target.find(".sibling")[0].getClientRects = () => [{}];
      const container = element.is(hook("tabs")) ? element : element.find(hook("tabs")).first();
      const button = () => element.find(hook("tab-maximize-button"));
      jest.spyOn(refs.$utilities, "animateCSS").mockImplementation((node, size, time, end) => end());

      expect(container.length).toBe(1);
      expect(container.attr("data-testid-owner")).toBe("tabId");
      expect(container.attr("data-maximized")).toBe("false");
      expect(button().find("i").hasClass("fa-expand")).toBe(true);

      button().trigger("click");
      refs.$rootScope.$digest();
      expect(container.attr("data-maximized")).toBe("true");
      expect(container.hasClass("maximized")).toBe(true);
      expect(target.find(".sibling").hasClass("minimized")).toBe(true);
      expect(button().find("i").hasClass("fa-compress")).toBe(true);

      button().trigger("click");
      refs.$rootScope.$digest();
      expect(container.attr("data-maximized")).toBe("false");
      expect(target.find(".sibling").hasClass("minimized")).toBe(false);
      target.remove();
    });

    it("renders no maximize button when the tab is not maximizable", () => {
      const {element} = compileMaximizable(false);

      expect(element.find(hook("tab-maximize-button")).length).toBe(0);
      expect(element.is(hook("tabs")) || element.find(hook("tabs")).length === 1).toBe(true);
    });
  });
});
