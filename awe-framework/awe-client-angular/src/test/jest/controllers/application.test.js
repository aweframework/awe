// Source-traceable focused Jest parity for controllers/application.js.
import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe("controllers/application.js", function() {
  let scope, rootScope, controller, settings, storage, serverData, utilities, loadingBar, log, actionController, httpBackend;

  beforeEach(function() {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$controller", "$log", "LoadingBar", "ServerData", "Storage", "AweUtilities", "AweSettings", "ActionController", "$httpBackend",
      function($rootScope, $controller, $log, LoadingBar, ServerData, Storage, AweUtilities, AweSettings, ActionController, $httpBackend) {
        rootScope = $rootScope;
        scope = $rootScope.$new();
        log = $log;
        loadingBar = LoadingBar;
        serverData = ServerData;
        storage = Storage;
        utilities = AweUtilities;
        settings = AweSettings;
        actionController = ActionController;
        httpBackend = $httpBackend;
        httpBackend.when("POST", "settings").respond(DefaultSettings);
        jest.spyOn(loadingBar, "end").mockImplementation(() => null);
        jest.spyOn(loadingBar, "startTask").mockImplementation(() => null);
        jest.spyOn(loadingBar, "endTask").mockImplementation(() => null);
        controller = $controller("AppController", {
          $scope: scope,
          $log: log,
          LoadingBar: loadingBar,
          ServerData: serverData,
          Storage: storage,
          AweUtilities: utilities,
          AweSettings: settings,
          ActionController: actionController,
          $rootScope: rootScope
        });
      }]);
  });

  it("updates action stack shortcuts and caps-lock status from key events", function() {
    jest.spyOn(settings, "update").mockImplementation(() => null);

    controller.onKeydown({altKey: true, shiftKey: true, which: 49, originalEvent: {getModifierState: () => false}});
    controller.onKeydown({altKey: true, shiftKey: true, which: 48, originalEvent: {getModifierState: () => true}});
    controller.onKeyup({originalEvent: {getModifierState: () => false}});

    expect(settings.update).toHaveBeenNthCalledWith(1, {actionsStack: 1000});
    expect(settings.update).toHaveBeenNthCalledWith(2, {actionsStack: 0});
    expect(rootScope.status.isCapsLockOn).toBe(false);
  });

  it("handles loading and route-error events through local LoadingBar spies", function() {
    jest.spyOn(log, "warn").mockImplementation(() => null);

    scope.$emit("cfpLoadingBar:started");
    expect(rootScope.status.loading).toBe(true);

    scope.$emit("cfpLoadingBar:completed");
    expect(rootScope.status.loading).toBe(false);

    scope.$emit("$stateChangeError", {name: "target"}, {}, {}, {}, "boom");
    expect(rootScope.status.loading).toBe(false);
    expect(scope.resizing).toBe(false);
    expect(loadingBar.end).toHaveBeenCalled();
    expect(log.warn).toHaveBeenCalledWith("State 'target' rejected: boom");
  });

  it("sends unload maintain actions with configured server keys when the tab closes", function() {
    rootScope.settings = {serverActionKey: "serverAction", targetActionKey: "targetAction"};
    jest.spyOn(storage, "get").mockReturnValue({base: {screen: "screen", onunload: "closeScreen"}});
    jest.spyOn(serverData, "send").mockImplementation(() => null);

    controller.beforeUnload();

    expect(serverData.send).toHaveBeenCalledWith({serverAction: "maintain-async", targetAction: "closeScreen"});
  });

  it("publishes resize actions and accepts the resize action after its delay", function() {
    const action = {attr: jest.fn().mockReturnValue({delay: 250})};
    jest.spyOn(utilities, "timeout").mockImplementation(fn => fn());
    jest.spyOn(utilities, "publish").mockImplementation(() => null);
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    scope.$emit("/action/resize", action);

    expect(utilities.publish).toHaveBeenCalledWith("resize-action");
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
  });

  it("reports browser identity using the controller IE helper", function() {
    jest.spyOn(utilities, "getBrowser").mockReturnValue("chrome");
    expect(controller.isIE()).toBe("not-ie chrome");

    utilities.getBrowser.mockReturnValue("ie 11");
    expect(controller.isIE()).toBe("ie 11");
  });

  it("resets loading state and warns when a target state is not found", function() {
    jest.spyOn(log, "warn").mockImplementation(() => null);
    rootScope.status.loading = true;

    scope.$emit("$stateNotFound", {to: "missing"}, {}, {});

    expect(rootScope.status.loading).toBe(false);
    expect(log.warn).toHaveBeenCalled();
  });

  it("ends initial loading tasks after the application initialised event", function() {
    jest.spyOn(utilities, "timeout").mockImplementation(fn => fn());

    scope.$emit("initialised");

    expect(loadingBar.endTask).toHaveBeenCalled();
  });

  it("accepts resize actions without delay parameters", function() {
    const action = {attr: jest.fn().mockReturnValue({})};
    jest.spyOn(utilities, "timeout").mockImplementation(fn => fn());
    jest.spyOn(utilities, "publish").mockImplementation(() => null);
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    scope.$emit("/action/resize", action);

    expect(utilities.publish).toHaveBeenCalledWith("resize-action");
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
  });

  it("prevents state changes while the comet connection identifier is missing", function() {
    jest.spyOn(storage, "getRoot").mockReturnValue(undefined);
    jest.spyOn(loadingBar, "startTask").mockImplementation(() => null);

    const event = scope.$emit("$stateChangeStart", {views: {base: {}}}, {}, {views: {base: {}}}, {});

    expect(event.defaultPrevented).toBe(true);
    expect(loadingBar.startTask).not.toHaveBeenCalled();
  });

  it("runs silent unload maintains for views leaving during a state change", function() {
    jest.spyOn(storage, "getRoot").mockReturnValue("comet-1");
    jest.spyOn(storage, "get").mockReturnValue({base: {screen: "orders", onunload: "closeOrders"}});
    jest.spyOn(serverData, "sendMaintain").mockImplementation(() => null);
    jest.spyOn(loadingBar, "startTask").mockImplementation(() => null);

    scope.$emit("$stateChangeStart", {views: {base: {}}}, {}, {views: {base: {}}}, {});

    expect(scope.resizing).toBe(true);
    expect(loadingBar.startTask).toHaveBeenCalled();
    expect(serverData.sendMaintain).toHaveBeenCalledWith({type: "maintain-silent", maintain: "closeOrders"}, false, true);
  });

  it("publishes resize when the browser window resize handler fires", function() {
    jest.spyOn(utilities, "publish").mockImplementation(() => null);

    $(window).trigger("resize");

    expect(utilities.publish).toHaveBeenCalledWith("resize");
  });

  it("initializes root storage buckets and loading status", function() {
    expect(storage.get("controller")).toEqual({});
    expect(storage.get("model")).toEqual({});
    expect(storage.get("messages")).toEqual({});
    expect(storage.get("api")).toEqual({});
    expect(rootScope.status.loading).toBe(true);
    expect(scope.resizing).toBe(true);
  });

  it("updates root status values without dropping existing flags", function() {
    controller.updateStatus("loading", false);
    controller.updateStatus("isCapsLockOn", true);

    expect(rootScope.status).toEqual({loading: false, isCapsLockOn: true});
  });

  it("ignores non shortcut keydown combinations while still updating caps-lock state", function() {
    jest.spyOn(settings, "update").mockImplementation(() => null);

    controller.onKeydown({altKey: false, shiftKey: true, which: 49, originalEvent: {getModifierState: () => true}});

    expect(settings.update).not.toHaveBeenCalled();
    expect(rootScope.status.isCapsLockOn).toBe(true);
  });

  it("does not change caps-lock status when the browser event has no modifier API", function() {
    controller.updateStatus("isCapsLockOn", false);

    controller.onKeyup({originalEvent: {}});

    expect(rootScope.status.isCapsLockOn).toBe(false);
  });

  it("clears resizing after successful state changes", function() {
    scope.resizing = true;

    scope.$emit("$stateChangeSuccess");

    expect(scope.resizing).toBe(false);
  });

  it("broadcasts unload for all views involved in a state change", function() {
    jest.spyOn(storage, "getRoot").mockReturnValue("comet-1");
    jest.spyOn(storage, "get").mockReturnValue({});
    jest.spyOn(scope, "$broadcast");

    scope.$emit("$stateChangeStart", {views: {base: {}, modal: {}}}, {}, {views: {base: {}, old: {}}}, {});

    expect(scope.$broadcast).toHaveBeenCalledWith("unload", "base");
    expect(scope.$broadcast).toHaveBeenCalledWith("unload", "modal");
    expect(scope.$broadcast).toHaveBeenCalledWith("unload", "old");
  });

  it("does not send unload maintain actions for views without onunload hooks", function() {
    rootScope.settings = {serverActionKey: "serverAction", targetActionKey: "targetAction"};
    jest.spyOn(storage, "get").mockReturnValue({base: {screen: "screen"}});
    jest.spyOn(serverData, "send").mockImplementation(() => null);

    controller.beforeUnload();

    expect(serverData.send).not.toHaveBeenCalled();
  });

  it("delays initial loading completion by 100ms", function() {
    jest.spyOn(utilities, "timeout").mockImplementation(fn => fn());

    scope.$emit("initialised");

    expect(utilities.timeout).toHaveBeenCalledWith(expect.any(Function), 100);
    expect(loadingBar.endTask).toHaveBeenCalled();
  });
});
