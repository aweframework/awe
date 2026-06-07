// Source-traceable focused Jest parity for controllers/screen.js.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe("controllers/screen.js", function() {
  let scope, controller, settings, actionController, dependencyController, screen;

  beforeEach(function() {
    angular.mock.module("aweApplication", function($provide) {
      $provide.value("Connection", {
        disconnect: jest.fn().mockReturnValue({then: callback => callback()}),
        getActionUrl: jest.fn().mockReturnValue("http://server/action/logout")
      });
    });
    inject(["$rootScope", "$controller", "AweSettings", "ActionController", "DependencyController", "Screen",
      function($rootScope, $controller, AweSettings, ActionController, DependencyController, Screen) {
        scope = $rootScope.$new();
        settings = AweSettings;
        actionController = ActionController;
        dependencyController = DependencyController;
        screen = Screen;
        controller = $controller("ScreenController", {
          $scope: scope,
          AweSettings: settings,
          ActionController: actionController,
          DependencyController: dependencyController,
          Screen: screen
        });
      }]);
  });

  it("reports whether the configured action stack is visible", function() {
    jest.spyOn(settings, "get").mockReturnValueOnce(0).mockReturnValueOnce(2000);

    expect(controller.showActions()).toBe(false);
    expect(controller.showActions()).toBe(true);
  });

  it("routes screen and dependency events to their services with event payloads", function() {
    const launchers = [{id: "criterion"}];
    const view = "base";
    const address = {view: "base", component: "cell"};
    const screenAction = {attr: jest.fn().mockReturnValue({screen: "target-screen"})};
    jest.spyOn(dependencyController, "checkAndLaunch").mockImplementation(() => null);
    jest.spyOn(dependencyController, "start").mockImplementation(() => null);
    jest.spyOn(dependencyController, "restart").mockImplementation(() => null);
    jest.spyOn(dependencyController, "unregisterView").mockImplementation(() => null);
    jest.spyOn(screen, "screen").mockImplementation(() => null);

    scope.$emit("modelChanged", launchers);
    scope.$emit("compiled", view);
    scope.$emit("initialize-cell", address);
    scope.$emit("unload", view);
    scope.$emit("/action/screen", screenAction);

    expect(dependencyController.checkAndLaunch).toHaveBeenCalledWith(launchers);
    expect(dependencyController.start).toHaveBeenCalledWith(view);
    expect(dependencyController.restart).toHaveBeenCalledWith(address);
    expect(dependencyController.unregisterView).toHaveBeenCalledWith(view);
    expect(screen.screen).toHaveBeenCalledWith(screenAction);
  });

  it("exposes synchronous and asynchronous action stacks from the action controller", function() {
    actionController.actionStackList.push({id: "sync"});
    actionController.asyncStackList.push({id: "async"});

    expect(controller.syncStacks).toBe(actionController.actionStackList);
    expect(controller.asyncStack).toBe(actionController.asyncStackList);
  });

  it("logs action information through the controller info hook", function() {
    const action = {type: "screen"};
    jest.spyOn(console, "info").mockImplementation(() => null);

    controller.showInfo(action);

    expect(console.info).toHaveBeenCalledWith(action);
  });
});
