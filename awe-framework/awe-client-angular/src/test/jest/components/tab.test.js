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
});
