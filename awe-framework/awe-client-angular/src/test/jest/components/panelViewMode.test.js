import {
  cleanupHeavyComponentTest,
  createPanelController,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";

describe("awe-framework/awe-client-angular/src/test/jest/components/panelViewMode.test.js", () => {
  let refs, $actionController, Criterion, controllers;
  const criterion = {view: "base", component: "Nam"};

  const openAction = (viewMode) => ({attr: jest.fn(name => name === "viewMode" ? viewMode : undefined)});
  const compile = (tag) => {
    const component = tag === "awe-side-panel" ? "side-panel" : "dialog";
    stubPanelComponent(refs, {values: []}, createPanelController("Pnl", component));
    controllers = {
      Pnl: createPanelController("Pnl", component),
      Nam: {id: "Nam", component: "text", readonly: false},
      Lat: {id: "Lat", component: "text", readonly: false}
    };
    refs.$control.getAddressController.mockImplementation(address => controllers[address.component]);
    const attribute = tag === "awe-side-panel" ? "side-panel-id" : "dialog-id";
    const element = refs.$compile(`<${tag} ${attribute}='Pnl'><div class="criterion" criterion-id="Nam"></div>` +
      `<button>close</button></${tag}>`)(refs.$rootScope.$new());
    document.body.appendChild(element[0]);
    refs.$rootScope.$digest();
    return element;
  };
  const changeController = (element, controller) => {
    element.isolateScope().$broadcast("controllerChange", {address: element.isolateScope().component.address, controller});
  };
  const open = (element, viewMode) => {
    controllers.Pnl.openAction = openAction(viewMode);
    changeController(element, {opened: true});
  };
  const close = (element) => changeController(element, {opened: false});
  const setReadonly = (readonly) => refs.$control.changeControllerAttribute(criterion, {readonly});
  const readonly = () => controllers.Nam.readonly;
  // A criterion rendered inside the panel once it is opened (deferred load, compiled content)
  const renderLateCriterion = (element) => {
    const lateElement = $('<div class="criterion" criterion-id="Lat"></div>');
    element.find('[criterion-id="Nam"]').after(lateElement);
    new Criterion(refs.$rootScope.$new(), "Lat", lateElement[0]).asCriterion();
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ActionController", "Criterion", (_ActionController_, _Criterion_) => {
      $actionController = _ActionController_;
      Criterion = _Criterion_;
    }]);
    jest.spyOn($actionController, "addStack").mockImplementation(jest.fn());
    jest.spyOn($actionController, "removeStack").mockImplementation(jest.fn());
    jest.spyOn($actionController, "addActionList").mockImplementation(jest.fn());
    jest.spyOn($actionController, "finishAction").mockImplementation(jest.fn());
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  it("makes the criteria of a side panel opened in view mode read-only", () => {
    const element = compile("awe-side-panel");

    open(element, true);

    expect(readonly()).toBe(true);
  });

  it("makes the criteria of a dialog opened in view mode read-only", () => {
    const element = compile("awe-dialog");

    open(element, true);

    expect(readonly()).toBe(true);
  });

  it("makes a criterion rendered after the panel was opened in view mode read-only", () => {
    const element = compile("awe-side-panel");
    open(element, true);

    renderLateCriterion(element);

    expect(controllers.Lat.readonly).toBe(true);
  });

  it("keeps a criterion rendered in a panel opened without view mode editable", () => {
    const element = compile("awe-side-panel");
    open(element, true);
    close(element);
    open(element, undefined);

    renderLateCriterion(element);

    expect(controllers.Lat.readonly).toBe(false);
  });

  it("keeps the criteria read-only when a dependency sets them editable while the panel is in view mode", () => {
    const element = compile("awe-side-panel");
    open(element, true);

    setReadonly(false);

    expect(readonly()).toBe(true);
  });

  it("clears the view mode when the panel is closed", () => {
    const element = compile("awe-side-panel");
    open(element, true);

    close(element);

    expect(readonly()).toBe(false);
  });

  it("opens the panel editable again when an action without view mode opens it after a view mode opening", () => {
    const element = compile("awe-side-panel");
    open(element, true);
    close(element);

    open(element, undefined);

    expect(readonly()).toBe(false);
  });

  it("keeps the readonly a dependency set while the panel was in view mode once it is closed", () => {
    const element = compile("awe-side-panel");
    open(element, true);
    setReadonly(true);

    close(element);

    expect(readonly()).toBe(true);
  });

  it("keeps the readonly of the dependencies in a panel opened without view mode", () => {
    const element = compile("awe-side-panel");
    open(element, undefined);
    expect(readonly()).toBe(false);

    setReadonly(true);
    expect(readonly()).toBe(true);

    setReadonly(false);
    expect(readonly()).toBe(false);
  });
});
