// Source-traceable focused Jest parity for controllers/message.js.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe("controllers/message.js", function() {
  let scope, controller, settings, utilities, control, actionController;

  function actionWith({parameters, view = "base", callbackTarget}) {
    return {
      attr: jest.fn(key => ({parameters, view, callbackTarget})[key]),
      isAlive: jest.fn().mockReturnValue(true)
    };
  }

  beforeEach(function() {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$controller", "AweSettings", "AweUtilities", "Control", "ActionController",
      function($rootScope, $controller, AweSettings, AweUtilities, Control, ActionController) {
        scope = $rootScope.$new();
        settings = AweSettings;
        utilities = AweUtilities;
        control = Control;
        actionController = ActionController;
        controller = $controller("MessageController", {
          $scope: scope,
          AweSettings: settings,
          AweUtilities: utilities,
          Control: control,
          ActionController: actionController
        });
      }]);
    jest.spyOn(settings, "get").mockReturnValue({ok: 0, error: 0, warning: 0});
  });

  it("adds translated alert messages from inline parameters and accepts them when closed", function() {
    const action = actionWith({parameters: {type: "ok", title: "Saved", message: "Done"}});
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    controller.MessageActions.message(action);

    expect(controller.alerts).toEqual([{type: "success", action, title: "Saved", msg: "Done"}]);
    controller.closeAlert(0);
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
    expect(controller.alerts).toHaveLength(0);
  });

  it("resolves targeted message text through the control boundary", function() {
    const action = actionWith({parameters: {type: "error", target: "RequiredMessage"}, view: "base"});
    jest.spyOn(control, "getMessageFromScope").mockReturnValue({title: "Invalid", message: "Required"});

    controller.MessageActions.message(action);

    expect(control.getMessageFromScope).toHaveBeenCalledWith("base", "RequiredMessage");
    expect(controller.alerts[0]).toEqual(expect.objectContaining({type: "danger", title: "Invalid", msg: "Required"}));
  });

  it("stores confirm title, message and action on scope", function() {
    const action = actionWith({parameters: {type: "warning", title: "Delete", message: "Confirm?"}});

    controller.MessageActions.confirm(action);

    expect(scope.confirmTitle).toBe("Delete");
    expect(scope.confirmMessage).toBe("Confirm?");
    expect(scope.confirmAction).toBe(action);
  });

  it("creates and destroys target popovers without browser plugin fidelity", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    const action = actionWith({
      parameters: {type: "warning", title: "Heads up", message: "Check field"},
      callbackTarget: {view: "base", component: "field"}
    });
    jest.spyOn($.fn, "popover").mockImplementation(function() { return this; });
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    controller.MessageActions.targetMessage(action);
    controller.startPopover(controller.popover);
    expect(controller.popover.visible).toBe(true);

    controller.hidePopover(controller.popover);
    expect(controller.popover.visible).toBe(false);

    controller.destroyPopover(controller.popover);
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
    expect(controller.popover).toBe(null);
  });

  it("maps error and wrong message types to danger alerts", function() {
    controller.MessageActions.message(actionWith({parameters: {type: "error", title: "Error"}}));
    controller.MessageActions.message(actionWith({parameters: {type: "wrong", title: "Wrong"}}));

    expect(controller.alerts.map(alert => alert.type)).toEqual(["danger", "danger"]);
    expect(controller.alerts.map(alert => alert.title)).toEqual(["Error", "Wrong"]);
  });

  it("keeps warning message types unchanged", function() {
    controller.MessageActions.message(actionWith({parameters: {type: "warning", message: "Careful"}}));

    expect(controller.alerts[0]).toEqual(expect.objectContaining({type: "warning", msg: "Careful"}));
  });

  it("auto-closes timed alerts and cancels the alert timer", function() {
    const timer = {id: "timer-1"};
    jest.spyOn(settings, "get").mockReturnValue({ok: 25, error: 0, warning: 0});
    jest.spyOn(utilities, "timeout").mockReturnValue(timer);
    utilities.timeout.cancel = jest.fn();
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    const action = actionWith({parameters: {type: "ok", message: "Saved"}});

    controller.MessageActions.message(action);
    controller.closeAlert(0);

    expect(controller.alerts).toHaveLength(0);
    expect(utilities.timeout).toHaveBeenCalledWith(expect.any(Function), 25);
    expect(utilities.timeout.cancel).toHaveBeenCalledWith(timer);
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
  });

  it("does nothing when closing an alert index without an alert", function() {
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    controller.closeAlert(3);

    expect(actionController.acceptAction).not.toHaveBeenCalled();
    expect(controller.alerts).toEqual([]);
  });

  it("resolves confirm text from a target message definition", function() {
    jest.spyOn(control, "getMessageFromScope").mockReturnValue({title: "Target title", message: "Target body"});
    const action = actionWith({parameters: {target: "ConfirmMessage"}, view: "center"});

    controller.MessageActions.confirm(action);

    expect(control.getMessageFromScope).toHaveBeenCalledWith("center", "ConfirmMessage");
    expect(scope.confirmTitle).toBe("Target title");
    expect(scope.confirmMessage).toBe("Target body");
    expect(scope.confirmAction).toBe(action);
  });

  it("uses body and base view as target-message fallback without a component address", function() {
    document.body.innerHTML = '<div ui-view="base"></div>';
    jest.spyOn($.fn, "popover").mockImplementation(function() { return this; });
    const action = actionWith({parameters: {type: "ok", title: "Body", message: "Base"}});

    controller.MessageActions.targetMessage(action);

    expect(controller.popover.target[0]).toBe(document.body);
    expect(controller.popover.view.attr("ui-view")).toBe("base");
    expect(controller.popover.background.hasClass("popover-success")).toBe(true);
  });

  it("targets component addresses for target-message popovers", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    jest.spyOn($.fn, "popover").mockImplementation(function() { return this; });
    const action = actionWith({
      parameters: {type: "warning", title: "Field", message: "Check"},
      callbackTarget: {view: "base", component: "field"}
    });

    controller.MessageActions.targetMessage(action);

    expect(controller.popover.target.attr("id")).toBe("field");
    expect(controller.popover.view.attr("ui-view")).toBe("base");
  });

  it("targets grid cell addresses for target-message popovers", function() {
    document.body.innerHTML = '<div ui-view="base"><div id="grid"><div row-id="1"><span column-id="name"></span></div></div></div>';
    jest.spyOn($.fn, "popover").mockImplementation(function() { return this; });
    const action = actionWith({
      parameters: {type: "error", title: "Cell", message: "Invalid"},
      callbackTarget: {view: "base", component: "grid", row: "1", column: "name"}
    });

    controller.MessageActions.targetMessage(action);

    expect(controller.popover.target.attr("column-id")).toBe("name");
    expect(controller.popover.background.hasClass("popover-danger")).toBe(true);
  });

  it("destroys the previous target-message before creating a new one", function() {
    const previousAction = actionWith({parameters: {type: "warning"}});
    controller.popover = {
      action: previousAction,
      background: {remove: jest.fn()},
      target: {popover: jest.fn()},
      visible: true
    };
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    jest.spyOn($.fn, "popover").mockImplementation(function() { return this; });

    controller.MessageActions.targetMessage(actionWith({parameters: {type: "ok", title: "New"}}));

    expect(actionController.acceptAction).toHaveBeenCalledWith(previousAction);
    expect(controller.popover.type).toBe("success");
  });
});
