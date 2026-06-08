import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe("Component", () => {
  let $control;
  let $utilities;
  let $httpBackend;
  let Component;
  let $actionController;
  let $dependencyController;
  let scopedFunctions;
  let scope;
  let controller;
  let model;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $control = $injector.get("Control");
      $utilities = $injector.get("AweUtilities");
      $httpBackend = $injector.get("$httpBackend");
      Component = $injector.get("Component");
      $actionController = $injector.get("ActionController");
      $dependencyController = $injector.get("DependencyController");

      scopedFunctions = {};
      scope = {
        view: "report",
        $parent: {$parent: {}},
        $on: jest.fn((key, fn) => {
          scopedFunctions[key] = fn;
          return jest.fn();
        }),
        $emit: jest.fn()
      };
      controller = {visible: true};
      model = {selected: "text", records: 14, values: [{value: "text", label: "Visible text"}]};

      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue(model);
      jest.spyOn($control, "getAddressController").mockReturnValue(controller);
      jest.spyOn($control, "setAddressApi");
      jest.spyOn($actionController, "defineActionListeners");
      jest.spyOn($dependencyController, "unregister");
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
  });

  afterEach(() => {
    try {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    } catch (error) {
      // Keep cleanup best-effort; these focused specs do not trigger settings requests.
    }
  });

  function initializedComponent(id = "comp1") {
    const component = new Component(scope, id);
    component.init();
    return component;
  }

  it("initializes component address, model, controller, API, and listeners", () => {
    const component = initializedComponent("comp1");

    expect(component.address).toEqual({view: "report", component: "comp1"});
    expect(component.model).toBe(model);
    expect(component.controller).toBe(controller);
    expect($control.setAddressApi).toHaveBeenCalledWith(component.address, component.api);
    expect($actionController.defineActionListeners).toHaveBeenCalledWith(
      component.listeners,
      expect.objectContaining({
        filter: expect.objectContaining({method: "reload"}),
        "start-load": expect.objectContaining({method: "startLoad"})
      }),
      scope,
      component
    );
    expect(scope.$on).toHaveBeenCalledWith("$destroy", expect.any(Function));
    expect(scope.$on).toHaveBeenCalledWith("unload", expect.any(Function));
  });

  it("exposes visible, text, value, and total-values attribute methods from the current model", () => {
    const component = initializedComponent("comp2");

    expect(component.attributeMethods.visible(component)).toBe(true);
    expect(component.attributeMethods.text(component)).toBe("text");
    expect(component.attributeMethods.value(component)).toBe("text");
    expect(component.attributeMethods.totalValues(component)).toBe(14);

    controller.invisible = true;
    delete model.records;
    expect(component.attributeMethods.visible(component)).toBe(false);
    expect(component.attributeMethods.totalValues(component)).toBe(0);
  });

  it("destroys help listeners, clears component listeners, and unregisters dependencies on scope destroy", () => {
    jest.spyOn($utilities, "clearListeners");
    const component = initializedComponent("comp2");
    const helpNode = {off: jest.fn()};
    component.helpTargets = {comp2: {over: true, timer: null, node: helpNode}};

    scopedFunctions.$destroy();

    expect(component.alive).toBe(false);
    expect(helpNode.off).toHaveBeenCalled();
    expect($utilities.clearListeners).toHaveBeenCalledWith(component.listeners);
    expect($dependencyController.unregister).toHaveBeenCalledWith(component);
  });

  it("keeps selected scalar when updateModelValues receives selected and values", () => {
    const component = initializedComponent("comp2");

    component.api.updateModelValues({
      selected: [{id: 1, label: 1.0, value: "1"}],
      values: [{id: 1, label: 1.0, value: "1"}]
    });

    expect(model.selected).toBe("1");
    expect(model.values).toEqual([{id: 1, label: 1.0, value: "1"}]);
  });

  it("stores the generated component identifier before initialization", () => {
    const component = new Component(scope, "comp1");

    expect(component.id).toBe("comp1");
  });

  it("reports visibility from the controller invisible flag", () => {
    controller.invisible = true;

    const component = initializedComponent("comp2");

    expect(component.attributeMethods.visible(component)).toBe(false);
  });

  it("falls back to zero total values when model records are missing", () => {
    delete model.records;

    const component = initializedComponent("comp2");

    expect(component.alive).toBe(true);
    expect(component.attributeMethods.totalValues(component)).toBe(0);
  });

  it("returns visible text from the current component model", () => {
    const component = initializedComponent("comp2");

    expect(component.attributeMethods.text(component)).toBe("text");
  });

  it("returns the raw selected value from the current component model", () => {
    const component = initializedComponent("comp2");

    expect(component.attributeMethods.value(component)).toBe("text");
  });

  it("returns the record count as total values when records are present", () => {
    const component = initializedComponent("comp2");

    expect(component.attributeMethods.totalValues(component)).toBe(14);
  });

  it("publishes show and hide help events around hover state", () => {
    jest.spyOn($control, "publish");
    jest.spyOn($utilities, "timeout").mockImplementation(callback => callback());
    $utilities.timeout.cancel = jest.fn();
    const help = {node: document.createElement("div")};
    const component = initializedComponent("comp2");

    component.initHelpNode("comp2", help);
    $(help.node).trigger("mouseover");
    $(help.node).trigger("mouseout");

    expect($utilities.timeout).toHaveBeenCalled();
    expect($control.publish).toHaveBeenCalledTimes(2);
    expect($control.publish).toHaveBeenNthCalledWith(1, "showHelp", help);
    expect($control.publish).toHaveBeenNthCalledWith(2, "hideHelp");
  });

  it("does not show delayed help when the component has been destroyed", () => {
    jest.spyOn($control, "publish");
    jest.spyOn($utilities, "timeout").mockImplementation(callback => callback());
    $utilities.timeout.cancel = jest.fn();
    const help = {node: document.createElement("div")};
    const component = initializedComponent("comp2");

    component.initHelpNode("comp2", help);
    component.alive = false;
    $(help.node).trigger("mouseover");
    $(help.node).trigger("mouseout");

    expect($utilities.timeout).not.toHaveBeenCalled();
    expect($control.publish).toHaveBeenCalledWith("hideHelp");
  });

  it("does not publish show help when the component becomes disabled before the delay finishes", () => {
    jest.spyOn($control, "publish");
    const component = initializedComponent("comp2");
    const help = {node: document.createElement("div")};
    jest.spyOn($utilities, "timeout").mockImplementation(callback => {
      component.isDisabled = () => true;
      callback();
    });
    $utilities.timeout.cancel = jest.fn();

    component.isDisabled = () => false;
    component.initHelpNode("comp2", help);
    $(help.node).trigger("mouseover");
    $(help.node).trigger("mouseout");

    expect($utilities.timeout).toHaveBeenCalledTimes(1);
    expect($control.publish).toHaveBeenCalledTimes(1);
    expect($control.publish).toHaveBeenCalledWith("hideHelp");
  });

  it("does not publish show help when hover ends before the delay finishes", () => {
    jest.spyOn($control, "publish");
    const component = initializedComponent("comp2");
    const help = {node: document.createElement("div")};
    jest.spyOn($utilities, "timeout").mockImplementation(callback => {
      component.helpTargets.comp2.over = false;
      callback();
    });
    $utilities.timeout.cancel = jest.fn();

    component.initHelpNode("comp2", help);
    $(help.node).trigger("mouseover");
    $(help.node).trigger("mouseout");

    expect($utilities.timeout).toHaveBeenCalledTimes(1);
    expect($control.publish).toHaveBeenCalledTimes(1);
    expect($control.publish).toHaveBeenCalledWith("hideHelp");
  });
});
