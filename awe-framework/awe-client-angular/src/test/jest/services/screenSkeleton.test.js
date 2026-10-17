// Screen loading skeleton: shown between the state change start and the screen initialisation
import fs from "fs";
import path from "path";
import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/webpack/locals-en-GB.config";

describe("screen loading skeleton", () => {
  let $rootScope, $compile, $controller, ScreenSkeleton, Storage, LoadingBar;

  const INDEX = {name: "index", views: {base: {}}};
  const PRIVATE_SCREEN = {name: "private.screen", views: {report: {}}};

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$controller", "$httpBackend", "ScreenSkeleton", "Storage", "LoadingBar",
      (_$rootScope_, _$compile_, _$controller_, $httpBackend, _ScreenSkeleton_, _Storage_, _LoadingBar_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $controller = _$controller_;
        ScreenSkeleton = _ScreenSkeleton_;
        Storage = _Storage_;
        LoadingBar = _LoadingBar_;
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
      }]);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    document.body.innerHTML = "";
  });

  describe("ScreenSkeleton service", () => {
    it("is hidden until a screen starts loading", () => {
      expect(ScreenSkeleton.isVisible()).toBe(false);

      ScreenSkeleton.show("report");
      expect(ScreenSkeleton.isVisible()).toBe(true);
      expect(ScreenSkeleton.getView()).toBe("report");

      ScreenSkeleton.hide();
      expect(ScreenSkeleton.isVisible()).toBe(false);
    });

    it("covers the report view when only the sub screen changes", () => {
      expect(ScreenSkeleton.getLoadingView(PRIVATE_SCREEN, {screenId: "main", subScreenId: "b"},
        PRIVATE_SCREEN, {screenId: "main", subScreenId: "a"})).toBe("report");
    });

    it("covers the base view when the whole screen changes", () => {
      expect(ScreenSkeleton.getLoadingView(PRIVATE_SCREEN, {screenId: "main", subScreenId: "a"}, INDEX, {})).toBe("base");
      expect(ScreenSkeleton.getLoadingView(PRIVATE_SCREEN, {screenId: "other", subScreenId: "a"},
        PRIVATE_SCREEN, {screenId: "main", subScreenId: "a"})).toBe("base");
      expect(ScreenSkeleton.getLoadingView(INDEX, {}, {name: ""}, {})).toBe("base");
    });
  });

  describe("application controller", () => {
    let scope;

    beforeEach(() => {
      jest.spyOn(LoadingBar, "startTask").mockImplementation(() => null);
      jest.spyOn(LoadingBar, "endTask").mockImplementation(() => null);
      jest.spyOn(LoadingBar, "end").mockImplementation(() => null);
      jest.spyOn(console, "warn").mockImplementation(() => null);
      scope = $rootScope.$new();
      $controller("AppController", {$scope: scope});
      Storage.putRoot("cometUID", "uid");
    });

    it("shows the skeleton when a screen starts loading", () => {
      $rootScope.$broadcast("$stateChangeStart", PRIVATE_SCREEN, {screenId: "main", subScreenId: "b"},
        PRIVATE_SCREEN, {screenId: "main", subScreenId: "a"});

      expect(ScreenSkeleton.isVisible()).toBe(true);
      expect(ScreenSkeleton.getView()).toBe("report");
    });

    it("does not show the skeleton while the settings are not loaded", () => {
      Storage.putRoot("cometUID", null);

      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});

      expect(ScreenSkeleton.isVisible()).toBe(false);
    });

    it("removes the skeleton when the screen is initialised", () => {
      $rootScope.$broadcast("$stateChangeStart", PRIVATE_SCREEN, {screenId: "main"}, INDEX, {});
      expect(ScreenSkeleton.isVisible()).toBe(true);

      $rootScope.$broadcast("initialised");

      expect(ScreenSkeleton.isVisible()).toBe(false);
    });

    it.each(["$stateChangeError", "$stateNotFound", "$stateChangeCancel"])("removes the skeleton on %s", event => {
      $rootScope.$broadcast("$stateChangeStart", PRIVATE_SCREEN, {screenId: "main"}, INDEX, {});
      expect(ScreenSkeleton.isVisible()).toBe(true);

      $rootScope.$broadcast(event, PRIVATE_SCREEN, {screenId: "main"}, INDEX, {}, "error");

      expect(ScreenSkeleton.isVisible()).toBe(false);
    });

    it("keeps the global loading bar task", () => {
      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});

      expect(LoadingBar.startTask).toHaveBeenCalled();
    });
  });

  describe("initial load", () => {
    const splash = () => document.getElementById("splash");
    const isSplashShown = () => splash() !== null && splash().style.display !== "none";

    beforeEach(() => {
      // Finish the splash animation at once
      $.fx.off = true;
      document.body.innerHTML = "<div id='splash'><div id='splashBackground'></div><div class='spinner'></div></div>";
      jest.spyOn(LoadingBar, "startTask").mockImplementation(() => null);
      jest.spyOn(LoadingBar, "endTask").mockImplementation(() => null);
      jest.spyOn(LoadingBar, "end").mockImplementation(() => null);
      jest.spyOn(console, "warn").mockImplementation(() => null);
      $controller("AppController", {$scope: $rootScope.$new()});
      Storage.putRoot("cometUID", "uid");
    });

    afterEach(() => {
      $.fx.off = false;
    });

    it("keeps only the splash animation, and no skeleton, while the first screen loads", () => {
      expect(isSplashShown()).toBe(true);

      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});

      expect(isSplashShown()).toBe(true);
      expect(ScreenSkeleton.isVisible()).toBe(false);
      expect(LoadingBar.startTask).toHaveBeenCalled();
    });

    it("removes the splash animation when the first screen is initialised", () => {
      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});

      $rootScope.$broadcast("initialised");

      expect(splash()).toBeNull();
    });

    it.each(["$stateChangeError", "$stateNotFound"])("removes the splash animation when the first screen fails on %s", event => {
      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});

      $rootScope.$broadcast(event, INDEX, {}, {name: ""}, {}, "error");

      expect(splash()).toBeNull();
    });

    it("keeps the splash animation while the first screen waits for the settings", () => {
      Storage.putRoot("cometUID", null);

      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});
      $rootScope.$broadcast("$stateChangeCancel", INDEX, {}, {name: ""}, {});

      expect(isSplashShown()).toBe(true);
      expect(ScreenSkeleton.isVisible()).toBe(false);
    });

    it("shows the skeleton, and no splash, when a screen loads after the first one", () => {
      $rootScope.$broadcast("$stateChangeStart", INDEX, {}, {name: ""}, {});
      $rootScope.$broadcast("initialised");

      $rootScope.$broadcast("$stateChangeStart", PRIVATE_SCREEN, {screenId: "main"}, INDEX, {});

      expect(ScreenSkeleton.isVisible()).toBe(true);
      expect(ScreenSkeleton.getView()).toBe("base");
      expect(splash()).toBeNull();
    });
  });

  describe("awe-screen-skeleton directive", () => {
    const compile = () => {
      document.body.innerHTML = "<div ui-view='base'><div ui-view='report'></div></div>";
      const element = $compile("<awe-screen-skeleton></awe-screen-skeleton>")($rootScope.$new());
      angular.element(document.body).append(element);
      $rootScope.$digest();
      return element;
    };

    it("renders nothing while no screen is loading", () => {
      const element = compile();

      expect(element.find(".screen-skeleton").length).toBe(0);
    });

    it("renders a title bar, a row of criteria and a grid block while a screen loads", () => {
      const element = compile();

      ScreenSkeleton.show("report");
      $rootScope.$digest();

      const skeleton = element.find(".screen-skeleton");
      expect(skeleton.length).toBe(1);
      expect(skeleton.attr("aria-busy")).toBe("true");
      expect(skeleton.find(".screen-skeleton-title .skeleton-bar").length).toBe(1);
      expect(skeleton.find(".screen-skeleton-criteria .skeleton-bar").length).toBeGreaterThan(1);
      expect(skeleton.find(".screen-skeleton-grid .skeleton-bar").length).toBeGreaterThan(1);
      expect(document.querySelector("[ui-view='report']").getAttribute("aria-busy")).toBe("true");
    });

    it("removes the skeleton and the busy state when the screen is rendered", () => {
      const element = compile();
      ScreenSkeleton.show("report");
      $rootScope.$digest();

      ScreenSkeleton.hide();
      $rootScope.$digest();

      expect(element.find(".screen-skeleton").length).toBe(0);
      expect(document.querySelector("[ui-view='report']").hasAttribute("aria-busy")).toBe(false);
    });

    it("is placed once in the application page", () => {
      const html = fs.readFileSync(path.join(__dirname, "../../../main/resources/index.html"), "utf8");

      expect(html.match(/<awe-screen-skeleton/g)).toHaveLength(1);
    });

    it("does not carry any loader test hook so the existing waits are unchanged", () => {
      const element = compile();
      ScreenSkeleton.show("base");
      $rootScope.$digest();

      expect(element.find("[data-testid]").length).toBe(0);
    });
  });
});
