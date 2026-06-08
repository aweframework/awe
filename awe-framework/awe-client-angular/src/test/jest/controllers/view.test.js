// Source-traceable focused Jest parity for controllers/view.js.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe("controllers/view.js", function() {
  let scope, rootScope, controller, serverData, storage, actionController, utilities, Load, loadingBar, log;

  function createController(screenData) {
    storage.get.mockReturnValue(screenData);
    controller = null;
    inject(["$controller", function($controller) {
      controller = $controller("ViewController", {
        $scope: scope,
        ServerData: serverData,
        Storage: storage,
        ActionController: actionController,
        AweUtilities: utilities,
        Load,
        LoadingBar: loadingBar,
        $log: log,
        screenData: "base",
        context: ""
      });
    }]);
    return controller;
  }

  beforeEach(function() {
    angular.mock.module("aweApplication", function($provide) {
      $provide.service("Load", function() {
        return jest.fn().mockImplementation(function() {
          this.start = jest.fn();
        });
      });
    });
    inject(["$rootScope", "ServerData", "Storage", "ActionController", "AweUtilities", "Load", "LoadingBar", "$log",
      function($rootScope, ServerData, Storage, ActionController, AweUtilities, LoadService, LoadingBar, $log) {
        rootScope = $rootScope;
        scope = $rootScope.$new();
        serverData = ServerData;
        storage = Storage;
        actionController = ActionController;
        utilities = AweUtilities;
        Load = LoadService;
        loadingBar = LoadingBar;
        log = $log;
        jest.spyOn(storage, "get");
        jest.spyOn(loadingBar, "end").mockImplementation(() => null);
        jest.spyOn(loadingBar, "startTask").mockImplementation(() => null);
      }]);
  });

  it("stores valid screen data and starts the loader without invoking real scroll behavior", function() {
    const data = {screen: {label: "Home"}, components: [{id: "field"}]};
    jest.spyOn(serverData, "storeScreenData").mockImplementation(() => null);

    createController(data);

    expect(loadingBar.end).toHaveBeenCalled();
    expect(loadingBar.startTask).toHaveBeenCalled();
    expect(scope.view).toBe("base");
    expect(rootScope.screen).toEqual({label: "Home"});
    expect(serverData.storeScreenData).toHaveBeenCalledWith(data, "base");
    expect(Load).toHaveBeenCalledWith(scope, "base", data.components);
    expect(Load.mock.instances[0].start).toHaveBeenCalled();
  });

  it("publishes initialised for empty or invalid screen data and reports invalid payloads", function() {
    jest.spyOn(utilities, "publish").mockImplementation(() => null);
    jest.spyOn(log, "error").mockImplementation(() => null);
    jest.spyOn(actionController, "sendMessage").mockImplementation(() => null);

    createController("bad payload");
    expect(log.error).toHaveBeenCalledWith(
      "[ERROR] Loading view (screen data is not an object)",
      {view: "base", data: "bad payload", context: ""}
    );
    expect(actionController.sendMessage).toHaveBeenCalledWith(
      {view: "base", context: ""},
      "error",
      "ERROR_TITLE_SCREEN_GENERATION_ERROR",
      "bad payload"
    );
    expect(utilities.publish).toHaveBeenCalledWith("initialised");
  });

  it("hides the matching view only for Internet Explorer unload events", function() {
    jest.spyOn(utilities, "getBrowser").mockReturnValue("ie");
    createController({components: []});

    scope.visible = true;
    scope.$emit("unload", "base");
    expect(scope.visible).toBe(false);

    scope.visible = true;
    utilities.getBrowser.mockReturnValue("chrome");
    scope.$emit("unload", "base");
    expect(scope.visible).toBe(true);
  });

  it("publishes initialised when screen data is empty", function() {
    jest.spyOn(utilities, "publish").mockImplementation(() => null);

    createController(null);

    expect(utilities.publish).toHaveBeenCalledWith("initialised");
  });

  it("does not start a loader when components are missing", function() {
    jest.spyOn(serverData, "storeScreenData").mockImplementation(() => null);

    createController({screen: {label: "Home"}});

    expect(Load).not.toHaveBeenCalled();
    expect(scope.view).toBeUndefined();
  });

  it("does not hide other views during Internet Explorer unload events", function() {
    jest.spyOn(utilities, "getBrowser").mockReturnValue("ie");
    createController({components: []});
    scope.visible = true;

    scope.$emit("unload", "modal");

    expect(scope.visible).toBe(true);
  });

  it("stores the screen on root scope when screen metadata is present", function() {
    createController({screen: {label: "Orders"}, components: []});

    expect(rootScope.screen).toEqual({label: "Orders"});
    expect(scope.view).toBe("base");
  });
});
