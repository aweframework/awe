// Source-traceable focused Jest parity for components/wizard.js.
import {
  cleanupHeavyComponentTest,
  createAction,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  panelValues,
  stubPanelComponent
} from "./heavyComponentTestUtils";

require("../../js/components/wizard.js");

describe("components/wizard.js", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it("migrates guarded backward clicks and action-driven step changes", () => {
    const model = createPanelModel("2");
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    expect(element.find(".wizard-wrapper").length).toBe(1);
    expect(element.find(".wizard-steps").length).toBe(1);

    const scope = element.isolateScope() || element.scope();
    const controllerApi = element.controller("aweInputWizard");

    expect(controllerApi.isActive("2")).toBe(true);
    scope.clickTab("3");
    expect(model.selected).toBe("2");

    scope.clickTab("1");
    expect(model.selected).toBe("1");

    refs.$rootScope.$broadcast("/action/last-step", createAction());
    expect(model.selected).toBe("3");

    refs.$rootScope.$broadcast("/action/nth-step", createAction({value: "2"}));
    expect(model.selected).toBe("2");
    expect(refs.$utilities.publishDelayedFromScope).toHaveBeenCalledWith("resize-action", {}, scope);
  });

  it("moves through next, previous, first, and last action boundaries", () => {
    const model = createPanelModel(null);
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    refs.$rootScope.$broadcast("/action/next-step", createAction());
    expect(model.selected).toBe("2");

    refs.$rootScope.$broadcast("/action/prev-step", createAction());
    expect(model.selected).toBe("1");

    refs.$rootScope.$broadcast("/action/last-step", createAction());
    expect(model.selected).toBe("3");

    refs.$rootScope.$broadcast("/action/first-step", createAction());
    expect(model.selected).toBe("1");
  });

  it("keeps empty wizard boundaries stable while nth-step can select an explicit value", () => {
    const model = {...createPanelModel(null), records: 0, total: 0, values: []};
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    refs.$rootScope.$broadcast("/action/next-step", createAction());
    expect(model.selected).toBeNull();

    refs.$rootScope.$broadcast("/action/prev-step", createAction());
    expect(model.selected).toBeNull();

    refs.$rootScope.$broadcast("/action/nth-step", createAction({value: "2"}));
    expect(model.selected).toBe("2");
  });

  it("returns printable wizard data only when the controller is printable", () => {
    const model = createPanelModel("1");
    const controller = createPanelController("wizardId", "wizard", {printable: true});
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    const scope = element.isolateScope() || element.scope();

    expect(scope.component.getPrintData()).toEqual({
      wizardId: "1",
      "wizardId.data": {text: "Step 1", all: panelValues}
    });

    controller.printable = false;
    expect(scope.component.getPrintData()).toEqual({wizardId: "1"});
  });

  it("keeps first and last boundaries from moving past available steps", () => {
    const model = createPanelModel("1");
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    refs.$rootScope.$broadcast("/action/prev-step", createAction());
    expect(model.selected).toBe("1");
    expect(model.selectedIndex).toBe(0);

    refs.$rootScope.$broadcast("/action/last-step", createAction());
    refs.$rootScope.$broadcast("/action/next-step", createAction());
    expect(model.selected).toBe("3");
    expect(model.selectedIndex).toBe(2);
  });

  it("updates selectedIndex for direct nth-step selections", () => {
    const model = createPanelModel("1");
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    refs.$rootScope.$broadcast("/action/nth-step", createAction({value: "3"}));

    expect(model.selected).toBe("3");
    expect(model.selectedIndex).toBe(2);
    expect(refs.$utilities.publishDelayedFromScope).toHaveBeenCalledWith("resize-action", {}, expect.any(Object));
  });

  it("exposes active panel state through the directive controller", () => {
    const model = createPanelModel("2");
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    const controllerApi = element.controller("aweInputWizard");

    expect(controllerApi.isActive("1")).toBe(false);
    expect(controllerApi.isActive("2")).toBe(true);
  });

  it("does not reselect or publish resize when clicking the active tab", () => {
    const model = createPanelModel("2");
    model.selectedIndex = 1;
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    const scope = element.isolateScope() || element.scope();

    scope.clickTab("2");

    expect(model.selected).toBe("2");
    expect(model.selectedIndex).toBe(1);
    expect(refs.$utilities.publishDelayedFromScope).not.toHaveBeenCalled();
  });

  it("keeps forward tab clicks guarded until an action selects that step", () => {
    const model = createPanelModel("1");
    model.selectedIndex = 0;
    const controller = createPanelController("wizardId", "wizard");
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    const scope = element.isolateScope() || element.scope();

    scope.clickTab("3");
    expect(model.selected).toBe("1");
    expect(model.selectedIndex).toBe(0);

    refs.$rootScope.$broadcast("/action/nth-step", createAction({value: "3"}));
    expect(model.selected).toBe("3");
    expect(model.selectedIndex).toBe(2);
  });

  it("uses the selected panel label in print data for non-first selections", () => {
    const model = createPanelModel("3");
    const controller = createPanelController("wizardId", "wizard", {printable: true});
    stubPanelComponent(refs, model, controller);

    const element = refs.$compile("<awe-input-wizard input-wizard-id='wizardId'></awe-input-wizard>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    const scope = element.isolateScope() || element.scope();

    expect(scope.component.getPrintData()).toEqual({
      wizardId: "3",
      "wizardId.data": {text: "Step 3", all: panelValues}
    });
  });
});
