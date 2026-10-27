import fs from "fs";
import path from "path";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";

// The AWE 5 theme appearance rendered on <html> from the settings (themePrimary, themeSurface, themeMode, density,
// themePalette): the only theme of the page, the classic themes are presets that the server resolves (#885 T24)
describe("Theme appearance on <html>", () => {
  const root = document.documentElement;
  const HEX_PRIMARY = {
    "--awe-primary-50": "#eef2ff",
    "--awe-primary-500": "#4f46e5",
    "--awe-primary-fill": "#4f46e5",
    "--awe-primary-on-fill": "#ffffff"
  };
  const HEX_SURFACE = {"--awe-surface-0": "#ffffff", "--awe-surface-50": "#f4f1ea"};
  const appearance = {themePrimary: "indigo", themeSurface: "auto", themeMode: "system", density: "comfortable", themePalette: {}};
  const flushMutations = () => new Promise((resolve) => setTimeout(resolve, 0));
  const inlineProperties = () => Array.from(root.style).filter((name) => name.startsWith("--awe-"));

  let $settings;
  let $screen;
  let $httpBackend;
  let $actionController;
  let $storage;

  const resetRoot = () => {
    root.removeAttribute("class");
    root.removeAttribute("style");
    ["data-awe-primary", "data-awe-surface", "data-awe-density"].forEach((name) => root.removeAttribute(name));
  };

  const action = (attributes = {}) => ({attr: (name) => attributes[name]});

  beforeEach(() => {
    resetRoot();
    document.body.className = "base-theme theme-sky";
    angular.mock.module("aweApplication");
    inject(["AweSettings", "Screen", "$httpBackend", "ActionController", "Storage",
      (__$settings__, __$screen__, __$httpBackend__, __$actionController__, __$storage__) => {
        $settings = __$settings__;
        $screen = __$screen__;
        $httpBackend = __$httpBackend__;
        $actionController = __$actionController__;
        $storage = __$storage__;
      }]);
    jest.spyOn($actionController, "acceptAction").mockImplementation(() => undefined);

    // The application run block loads the settings: answer it without appearance fields
    $settings.settingsLoaded = jest.fn();
    $httpBackend.expect("POST", "settings").respond({});
    $httpBackend.flush();
  });

  afterEach(() => {
    resetRoot();
    document.body.className = "";
    document.head.innerHTML = "";
  });

  describe("attributes from the settings", () => {
    it("renders a palette name as data-awe-primary and data-awe-surface without inline properties", () => {
      $settings.update({...appearance, themeSurface: "stone"});

      expect(root.getAttribute("data-awe-primary")).toBe("indigo");
      expect(root.getAttribute("data-awe-surface")).toBe("stone");
      expect(root.getAttribute("data-awe-density")).toBe("comfortable");
      expect(root.hasAttribute("style")).toBe(false);
    });

    it("renders a hex as custom and its palette as inline custom properties", () => {
      $settings.update({...appearance, themePrimary: "#4f46e5", themeSurface: "#f4f1ea", themePalette: {...HEX_PRIMARY, ...HEX_SURFACE}});

      expect(root.getAttribute("data-awe-primary")).toBe("custom");
      expect(root.getAttribute("data-awe-surface")).toBe("custom");
      Object.entries({...HEX_PRIMARY, ...HEX_SURFACE}).forEach(([name, value]) => expect(root.style.getPropertyValue(name)).toBe(value));
    });

    it("removes the previous generated properties on every change", () => {
      $settings.update({...appearance, themePrimary: "#4f46e5", themePalette: HEX_PRIMARY});
      $settings.update({themePrimary: "#123456", themePalette: {"--awe-primary-500": "#123456"}});

      expect(inlineProperties()).toEqual(["--awe-primary-500"]);
      expect(root.style.getPropertyValue("--awe-primary-500")).toBe("#123456");

      $settings.update({themePrimary: "sky", themePalette: {}});

      expect(root.getAttribute("data-awe-primary")).toBe("sky");
      expect(inlineProperties()).toEqual([]);
      expect(root.hasAttribute("style")).toBe(false);
    });

    it("does not render data-awe-surface for the auto surface", () => {
      $settings.update({...appearance, themeSurface: "slate"});
      $settings.update({themeSurface: "auto"});

      expect(root.hasAttribute("data-awe-surface")).toBe(false);
    });

    it("renders the density", () => {
      $settings.update({...appearance, density: "compact"});

      expect(root.getAttribute("data-awe-density")).toBe("compact");
    });

    it.each([
      ["light", ["light"]],
      ["dark", ["dark"]],
      ["system", []]
    ])("renders the %s mode as the classes %p of <html>", (themeMode, classes) => {
      root.classList.add("pxajs", "light", "dark");

      $settings.update({...appearance, themeMode});

      expect(Array.from(root.classList)).toEqual(["pxajs", ...classes]);
    });

    it("does not touch <html> when the settings change without appearance fields", () => {
      $settings.update(appearance);
      root.setAttribute("data-awe-primary", "kept");

      $settings.update({language: "es-ES", theme: "clean"});

      expect(root.getAttribute("data-awe-primary")).toBe("kept");
    });
  });

  describe("updates", () => {
    it("renders the appearance of the settings loaded from the server", () => {
      $httpBackend.expect("POST", "settings").respond({...DefaultSettings, ...appearance, themeMode: "dark"});

      $settings.init();
      $httpBackend.flush();

      expect(root.getAttribute("data-awe-primary")).toBe("indigo");
      expect(root.classList.contains("dark")).toBe(true);
    });

    it("reads the appearance again from the server when the theme changes", () => {
      $settings.update({...DefaultSettings, ...appearance});
      jest.spyOn($storage, "get").mockReturnValue({});
      $httpBackend.expect("POST", "settings/appearance").respond({...appearance, themePrimary: "#4f46e5", themePalette: HEX_PRIMARY});
      const themeAction = action({parameters: {theme: "clean"}});

      $screen.changeTheme(themeAction);
      $httpBackend.flush();

      expect($settings.get("theme")).toBe("clean");
      expect(root.getAttribute("data-awe-primary")).toBe("custom");
      expect(root.style.getPropertyValue("--awe-primary-500")).toBe("#4f46e5");
      expect($actionController.acceptAction).toHaveBeenCalledWith(themeAction);
    });

    it("keeps only the response of the last request", () => {
      $settings.update({...DefaultSettings, ...appearance});
      $httpBackend.expect("POST", "settings/appearance").respond({themeMode: "light"});
      $httpBackend.expect("POST", "settings/appearance").respond({themeMode: "dark"});

      $settings.refreshAppearance();
      $settings.refreshAppearance();
      $httpBackend.flush();

      expect(root.classList.contains("dark")).toBe(true);
      expect(root.classList.contains("light")).toBe(false);
    });

    it("reads the appearance again from the server when the mode classes of the body change", async () => {
      $httpBackend.expect("POST", "settings").respond({...DefaultSettings, ...appearance});
      $settings.init();
      $httpBackend.flush();

      document.body.classList.add("page-home");
      await flushMutations();
      $httpBackend.verifyNoOutstandingRequest();

      $httpBackend.expect("POST", "settings/appearance").respond({...appearance, themeMode: "dark"});
      document.body.classList.add("dark");
      await flushMutations();
      $httpBackend.flush();

      expect(root.classList.contains("dark")).toBe(true);
    });
  });

  describe("classic theme name (a preset of the server)", () => {
    const INDEX = path.join(__dirname, "../../../main/resources/index.html");
    const index = fs.readFileSync(INDEX, "utf8");
    const bodyClass = /<body[^>]*\sclass="([^"]*)"/.exec(index)[1].split(/\s+/);

    it("does not load the themeVariables stylesheet any more", () => {
      expect(index).not.toMatch(/themeVariables/);
    });

    it("keeps the base-theme and theme-<name> classes of the body only as markers of the browser tests", () => {
      expect(bodyClass).toEqual(expect.arrayContaining(["base-theme", "theme-{{settings.theme}}"]));
    });

    it("does not touch the classes of the body when the appearance changes", () => {
      document.body.classList.add("dark");

      $settings.update({...appearance, themeMode: "light", themePrimary: "#4f46e5", themePalette: HEX_PRIMARY});

      expect(Array.from(document.body.classList)).toEqual(["base-theme", "theme-sky", "dark"]);
    });
  });
});
