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

  it("lets a screen claim a message to show it itself: no alert, the action ends at once", function() {
    const callbackTarget = {view: "base", component: "ButLogIn"};
    const action = actionWith({parameters: {type: "warning", title: "Invalid", message: "Check it"}, callbackTarget});
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    const claims = [];
    const stop = scope.$root.$on("claim-message", (event, message) => {
      claims.push(message);
      event.preventDefault();
    });

    controller.MessageActions.message(action);
    stop();

    expect(claims).toEqual([{type: "warning", title: "Invalid", message: "Check it", target: callbackTarget, view: "base"}]);
    expect(controller.alerts).toEqual([]);
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
  });

  it("adds the alert when nobody claims the message", function() {
    const action = actionWith({parameters: {type: "warning", title: "Invalid", message: "Check it"}});
    const listener = jest.fn();
    const stop = scope.$root.$on("claim-message", listener);

    controller.MessageActions.message(action);
    stop();

    expect(listener).toHaveBeenCalledTimes(1);
    expect(controller.alerts).toEqual([{type: "warning", action, title: "Invalid", msg: "Check it"}]);
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

  it("creates, hides and destroys target popovers", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    const action = actionWith({
      parameters: {type: "warning", title: "Heads up", message: "Check field"},
      callbackTarget: {view: "base", component: "field"}
    });
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    controller.MessageActions.targetMessage(action);
    expect($("body > .popover").length).toBe(1);
    expect($("body > .popover .popover-title").text()).toBe("Heads up");
    expect($("body > .popover .popover-content").text()).toBe("Check field");
    expect(controller.popover.visible).toBe(true);

    const message = controller.popover;
    controller.hidePopover(message);
    expect(message.visible).toBe(false);
    expect($("body > .popover").length).toBe(0);
  });

  it("destroys the popover and its mask when it is hidden, and finishes the action", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    const action = actionWith({
      parameters: {type: "warning", title: "Heads up", message: "Check field"},
      callbackTarget: {view: "base", component: "field"}
    });
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    controller.MessageActions.targetMessage(action);
    expect($("body > .target-message").length).toBe(1);

    controller.hidePopover(controller.popover);

    expect($("body > .target-message").length).toBe(0);
    expect($("body > .popover").length).toBe(0);
    expect(actionController.acceptAction).toHaveBeenCalledTimes(1);
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
    expect(controller.popover).toBe(null);
  });

  it("hides the popover with a click on its mask", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    controller.MessageActions.targetMessage(actionWith({
      parameters: {type: "warning", title: "Heads up", message: "Check field"},
      callbackTarget: {view: "base", component: "field"}
    }));

    $("body > .target-message").trigger("click");

    expect($("body > .popover").length).toBe(0);
    expect(controller.popover).toBe(null);
  });

  it("shows a malicious title and message as text", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    controller.MessageActions.targetMessage(actionWith({
      parameters: {type: "error", title: "<img src=x onerror=\"window.hacked = true\">", message: "<script>window.hacked = true</script>"},
      callbackTarget: {view: "base", component: "field"}
    }));

    expect($("body > .popover").find("img, script").length).toBe(0);
    expect($("body > .popover .popover-title").text()).toBe("<img src=x onerror=\"window.hacked = true\">");
    expect(window.hacked).toBeUndefined();
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
    const action = actionWith({parameters: {type: "ok", title: "Body", message: "Base"}});

    controller.MessageActions.targetMessage(action);

    expect(controller.popover.target[0]).toBe(document.body);
    expect(controller.popover.view.attr("ui-view")).toBe("base");
    expect(controller.popover.background.hasClass("popover-success")).toBe(true);
  });

  it("targets component addresses for target-message popovers", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    const action = actionWith({
      parameters: {type: "warning", title: "Field", message: "Check"},
      callbackTarget: {view: "base", component: "field"}
    });

    controller.MessageActions.targetMessage(action);

    expect(controller.popover.target.attr("id")).toBe("field");
    expect(controller.popover.view.attr("ui-view")).toBe("base");
  });

  it("shows the popover with the test hooks and names the component it points at", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    const action = actionWith({
      parameters: {type: "error", title: "Field", message: "Check"},
      callbackTarget: {view: "base", component: "field"}
    });

    controller.MessageActions.targetMessage(action);

    const popover = $("body > .popover");
    expect(popover.attr("data-testid")).toBe("popover");
    expect(popover.attr("data-type")).toBe("danger");
    expect(popover.attr("data-testid-owner")).toBe("field");
    expect(popover.find("[data-testid='popover-title']").text()).toBe("Field");
    expect(popover.find("[data-testid='popover-content']").text()).toBe("Check");
  });

  it("clears the owner when a popover that is not bound to a component follows a bound one", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    controller.MessageActions.targetMessage(actionWith({
      parameters: {type: "error", title: "Field", message: "Check"},
      callbackTarget: {view: "base", component: "field"}
    }));
    expect(controller.popover.owner).toBe("field");

    controller.MessageActions.targetMessage(actionWith({parameters: {type: "info", title: "Body", message: "Base"}}));

    expect(controller.popover.owner).toBeUndefined();
    expect($("body > .popover").length).toBe(1);
    expect($("body > .popover").attr("data-testid-owner")).toBeUndefined();
  });

  it("does not name an owner for a popover that is not bound to a component", function() {
    document.body.innerHTML = '<div ui-view="base"></div>';

    controller.MessageActions.targetMessage(actionWith({parameters: {type: "info", title: "Body", message: "Base"}}));

    expect($("body > .popover").attr("data-testid-owner")).toBeUndefined();
  });

  it("targets grid cell addresses for target-message popovers", function() {
    document.body.innerHTML = '<div ui-view="base"><div id="grid"><div row-id="1"><span column-id="name"></span></div></div></div>';
    const action = actionWith({
      parameters: {type: "error", title: "Cell", message: "Invalid"},
      callbackTarget: {view: "base", component: "grid", row: "1", column: "name"}
    });

    controller.MessageActions.targetMessage(action);

    expect(controller.popover.target.attr("column-id")).toBe("name");
    expect(controller.popover.background.hasClass("popover-danger")).toBe(true);
  });

  it("destroys the previous target-message before creating a new one", function() {
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    const previousAction = actionWith({parameters: {type: "warning", title: "Old", message: "Old"}, callbackTarget: {view: "base", component: "field"}});
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    controller.MessageActions.targetMessage(previousAction);

    controller.MessageActions.targetMessage(actionWith({parameters: {type: "ok", title: "New", message: "New"}, callbackTarget: {view: "base", component: "field"}}));

    expect(actionController.acceptAction).toHaveBeenCalledWith(previousAction);
    expect(controller.popover.type).toBe("success");
    expect($("body > .popover").length).toBe(1);
    expect($("body > .popover .popover-title").text()).toBe("New");
    expect($("body > .target-message").length).toBe(1);
  });

  it("does not destroy a new popover when the one it replaced finishes fading out", function() {
    jest.useFakeTimers();
    const computed = window.getComputedStyle.bind(window);
    jest.spyOn(window, "getComputedStyle").mockImplementation((element, pseudo) => new Proxy(computed(element, pseudo), {
      get: (style, property) => property === "transitionDuration" ? "0.15s" : (typeof style[property] === "function" ? style[property].bind(style) : style[property])
    }));
    document.body.innerHTML = '<div ui-view="base"></div><button id="field"></button>';
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);
    const address = {view: "base", component: "field"};
    controller.MessageActions.targetMessage(actionWith({parameters: {type: "warning", title: "Old", message: "Old"}, callbackTarget: address}));
    jest.advanceTimersByTime(150);

    controller.MessageActions.targetMessage(actionWith({parameters: {type: "ok", title: "New", message: "New"}, callbackTarget: address}));
    jest.advanceTimersByTime(300);

    expect(controller.popover).not.toBe(null);
    expect(controller.popover.text.title).toBe("New");
    expect($("body > .popover .popover-title").text()).toBe("New");
  });

  it("hides and destroys safely a popover which was never created", function() {
    expect(() => controller.hidePopover(null)).not.toThrow();
    expect(() => controller.destroyPopover(null)).not.toThrow();
    expect(controller.popover).toBe(null);
  });
});
