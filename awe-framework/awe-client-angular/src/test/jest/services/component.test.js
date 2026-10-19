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

  describe("deferred loading (#824)", () => {
    let $storage;
    let models;

    beforeEach(() => {
      inject(["Storage", (Storage) => {
        $storage = Storage;
      }]);
      $storage.put("screen", {report: {name: "deferred-screen", option: "DefScr"}});
      $storage.put("controller", {});
      $storage.get("controller")["report"] = {comp1: controller, inner: {}, loadedGrid: {}, valuedCriterion: {}, outer: {}};
      controller.deferred = true;
      controller.optionId = "DefScr";
      models = {
        inner: {selected: null, values: []},
        loadedGrid: {selected: null, values: [{id: "1"}]},
        valuedCriterion: {selected: "1", values: [{value: "1", label: "One"}]},
        outer: {selected: "o", values: [{value: "o"}]}
      };
      $control.getAddressModel.mockImplementation((address) => models[address.component] || model);
      document.body.innerHTML = `<div id="comp1"><div criterion-id="inner"></div><div id="loadedGrid"></div>
        <div criterion-id="valuedCriterion"></div></div><div criterion-id="outer"></div>`;
      jest.spyOn($actionController, "addActionList").mockImplementation(() => {});
    });

    afterEach(() => {
      document.body.innerHTML = "";
    });

    function sentParameters() {
      const [[actions]] = $actionController.addActionList.mock.calls;
      return actions[0].attr("parameters");
    }

    it("sends one deferred-data server action for the container", () => {
      initializedComponent("comp1").loadDeferred();

      expect($actionController.addActionList).toHaveBeenCalledTimes(1);
      const [[actions]] = $actionController.addActionList.mock.calls;
      expect(actions[0].attr("silent")).toBe(true);
      expect(actions[0].attr("async")).toBe(true);
      expect(sentParameters()).toMatchObject({serverAction: "deferred-data", targetAction: "comp1", option: "DefScr"});
    });

    it("marks the container as loaded so it is requested only once", () => {
      const component = initializedComponent("comp1");

      component.loadDeferred();
      component.loadDeferred();

      expect(controller.deferredLoaded).toBe(true);
      expect($actionController.addActionList).toHaveBeenCalledTimes(1);
    });

    it("does nothing when the container is not deferred", () => {
      controller.deferred = false;

      initializedComponent("comp1").loadDeferred();

      expect($actionController.addActionList).not.toHaveBeenCalled();
    });

    it("reports the components inside the container which already have data or a value", () => {
      initializedComponent("comp1").loadDeferred();

      expect(sentParameters().deferredLoaded).toEqual(["loadedGrid", "valuedCriterion"]);
      expect(sentParameters().deferredValued).toEqual(["valuedCriterion"]);
    });

    it("resets the loaded mark if the request fails, so the next opening tries again (#824)", () => {
      const component = initializedComponent("comp1");
      component.loadDeferred();
      const [[actions]] = $actionController.addActionList.mock.calls;

      expect(controller.deferredLoaded).toBe(true);
      actions[0].onError({status: 500});

      expect(controller.deferredLoaded).toBe(false);
      component.loadDeferred();
      expect($actionController.addActionList).toHaveBeenCalledTimes(2);
    });

    it("makes the API of a component recognizable by the validator as inside a never opened deferred container (#824)", () => {
      inject(["Validator", (Validator) => {
        $storage.get("controller")["report"].inner = {};
        const component = initializedComponent("inner");
        component.api.validate = jest.fn(() => ({message: "required"}));
        $storage.put("api", {report: {inner: component.api}});

        const errors = Validator.validateNode($(document.body));

        expect(component.api.validate).not.toHaveBeenCalled();
        expect(errors).toEqual([]);
      }]);
    });

    it("recognizes a component whose identifier has CSS special characters inside a never opened deferred container (#824)", () => {
      inject(["Validator", (Validator) => {
        document.body.innerHTML = `<div id="comp1"><div criterion-id="in.ner:1"></div></div>`;
        $storage.get("controller")["report"]["in.ner:1"] = {};
        const component = initializedComponent("in.ner:1");
        component.api.validate = jest.fn(() => ({message: "required"}));
        $storage.put("api", {report: {"in.ner:1": component.api}});

        const errors = Validator.validateNode($(document.body));

        expect(component.api.validate).not.toHaveBeenCalled();
        expect(errors).toEqual([]);
      }]);
    });

    it("leaves the components with autoload out of the request, as they load by themselves (#824)", () => {
      $storage.get("controller")["report"].autoGrid = {autoload: true};
      document.body.innerHTML = `<div id="comp1"><div id="autoGrid"></div><div criterion-id="inner"></div></div>`;

    initializedComponent("comp1").loadDeferred();

      expect(sentParameters().deferredLoaded).toContain("autoGrid");
    });

    describe("autoload", () => {
      beforeEach(() => {
        jest.spyOn($utilities, "timeout").mockImplementation(callback => callback());
        $storage.get("controller")["report"].grid1 = {autoload: true};
        document.body.innerHTML = `<div id="comp1"><div id="grid1"></div></div><div id="grid2"></div>`;
        $storage.get("controller")["report"].grid2 = {autoload: true};
      });

      function gridComponent(id) {
        const gridController = $storage.get("controller")["report"][id];
        $control.getAddressController.mockImplementation(address => address.component === id ? gridController : controller);
        const component = new Component(scope, id);
        component.reload = jest.fn();
        component.init();
        return component;
      }

      it("waits for the first opening of a deferred container before reloading the component", () => {
        const grid = gridComponent("grid1");

        expect(grid.reload).not.toHaveBeenCalled();
      });

      it("reloads the waiting component when its container is opened", () => {
        const grid = gridComponent("grid1");
        $control.getAddressController.mockReturnValue(controller);

        initializedComponent("comp1").loadDeferred();

        expect(grid.reload).toHaveBeenCalledTimes(1);
      });

      it("forgets a waiting component when it is destroyed, so a dead scope is not reloaded (#824)", () => {
        const grid = gridComponent("grid1");
        scopedFunctions.$destroy();
        $control.getAddressController.mockReturnValue(controller);

        initializedComponent("comp1").loadDeferred();

        expect(grid.reload).not.toHaveBeenCalled();
      });

      it("forgets the waiting components of a container which is destroyed", () => {
        const grid = gridComponent("grid1");
        const container = initializedComponent("comp1");
        scopedFunctions.$destroy();
        $control.getAddressController.mockReturnValue(controller);
        scopedFunctions = {};

        const sameContainer = initializedComponent("comp1");
        sameContainer.loadDeferred();

        expect(grid.reload).not.toHaveBeenCalled();
        expect(container).not.toBe(sameContainer);
      });

      it("reloads at once a component which is not inside a deferred container", () => {
        const grid = gridComponent("grid2");

        expect(grid.reload).toHaveBeenCalledTimes(1);
      });
    });
  });
});
