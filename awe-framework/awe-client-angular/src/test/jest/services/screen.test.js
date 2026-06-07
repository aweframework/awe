import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe("Screen", () => {
  let $screen;
  let $settings;
  let $utilities;
  let $actionController;
  let $dependencyController;
  let $control;
  let $serverData;
  let $storage;
  let $state;
  let $windowMock;

  beforeEach(() => {
    $windowMock = {
      close: jest.fn(),
      history: {back: jest.fn()},
      location: {href: ""},
      open: jest.fn(),
      print: jest.fn()
    };
    angular.mock.module("aweApplication", {$window: $windowMock});

    inject(["$injector", ($injector) => {
      $screen = $injector.get("Screen");
      $settings = $injector.get("AweSettings");
      $utilities = $injector.get("AweUtilities");
      $actionController = $injector.get("ActionController");
      $dependencyController = $injector.get("DependencyController");
      $control = $injector.get("Control");
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $state = $injector.get("$state");

      jest.spyOn($actionController, "acceptAction").mockImplementation((receivedAction) => receivedAction.accept());
    }]);
  });

  function action(attributes = {}) {
    const actionAttributes = {
      callbackTarget: {view: "base", component: "dialog"},
      context: "base",
      parameters: {},
      target: "signin",
      view: "base",
      ...attributes
    };
    return {
      accept: jest.fn(),
      attr: jest.fn((attribute) => actionAttributes[attribute])
    };
  }

  it("navigates to target screens, stores received tokens, and accepts the action", () => {
    jest.spyOn($settings, "setToken").mockReturnValue(undefined);
    jest.spyOn($utilities, "sameUrl").mockReturnValue(false);
    jest.spyOn($utilities, "getState").mockReturnValue({to: "base.signin", parameters: {screen: "signin"}});
    jest.spyOn($state, "transitionTo").mockReturnValue(undefined);
    const screenAction = action({parameters: {target: "signin", token: "abc"}});

    $screen.screen(screenAction);

    expect($settings.setToken).toHaveBeenCalledWith("abc", true);
    expect($utilities.getState).toHaveBeenCalledWith("/base/signin", false);
    expect($state.transitionTo).toHaveBeenCalledWith(
      "base.signin",
      {screen: "signin"},
      {reload: false, inherit: true, notify: true, location: true}
    );
    expect($actionController.acceptAction).toHaveBeenCalledWith(screenAction);
  });

  it("reloads current screen only when same URL reload is enabled", () => {
    jest.spyOn($utilities, "sameUrl").mockReturnValue(true);
    jest.spyOn($settings, "get").mockImplementation((key) => key === "reloadCurrentScreen");
    jest.spyOn($screen, "reload").mockReturnValue(undefined);
    const screenAction = action({parameters: {screen: "current"}, context: "base"});

    $screen.screen(screenAction);

    expect($screen.reload).toHaveBeenCalledWith(screenAction);
    expect($actionController.acceptAction).not.toHaveBeenCalled();
  });

  it("reloads and returns through the navigation boundaries", () => {
    jest.spyOn($state, "transitionTo").mockReturnValue(undefined);
    const reloadAction = action();
    const backAction = action();

    $screen.reload(reloadAction);
    $screen.back(backAction);

    expect($state.transitionTo).toHaveBeenCalledWith(
      $state.current,
      {},
      expect.objectContaining({reload: false, inherit: true, notify: true, location: false})
    );
    expect($windowMock.history.back).toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(reloadAction);
    expect($actionController.acceptAction).toHaveBeenCalledWith(backAction);
  });

  it("waits through AweUtilities timeout before accepting", () => {
    jest.spyOn($utilities, "timeout").mockImplementation((fn) => fn());
    const waitAction = action({parameters: {target: 5}});

    $screen.wait(waitAction);

    expect($utilities.timeout).toHaveBeenCalledWith(expect.any(Function), 5);
    expect($actionController.acceptAction).toHaveBeenCalledWith(waitAction);
  });

  it("changes language and theme from storage-backed selected values", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {language: {selected: "fr-FR"}, theme: {selected: "clean"}}});
    jest.spyOn($settings, "changeLanguage");
    jest.spyOn($settings, "update");

    const languageAction = action({target: "language"});
    const themeAction = action({target: "theme"});
    $screen.changeLanguage(languageAction);
    $screen.changeTheme(themeAction);

    expect($settings.changeLanguage).toHaveBeenCalledWith("fr-FR");
    expect($settings.update).toHaveBeenCalledWith({theme: "clean"});
    expect($actionController.acceptAction).toHaveBeenCalledWith(languageAction);
    expect($actionController.acceptAction).toHaveBeenCalledWith(themeAction);
  });

  it("stores screen data and queues server-provided actions", () => {
    const screenDataStore = {};
    jest.spyOn($storage, "get").mockReturnValue(screenDataStore);
    jest.spyOn($actionController, "addActionList");
    const actions = [{type: "reload"}];
    const screenAction = action({parameters: {view: "base", screenData: {actions, components: [], screen: {name: "TEST"}, messages: []}}});

    $screen.screenData(screenAction);

    expect(screenDataStore.base).toEqual({actions, components: [], screen: {name: "TEST"}, messages: []});
    expect($actionController.addActionList).toHaveBeenCalledWith(actions, false, {});
    expect($actionController.acceptAction).toHaveBeenCalledWith(screenAction);
  });

  it("opens, closes, and ends dialog/load/dependency actions through service boundaries", () => {
    jest.spyOn($control, "changeControllerAttribute");
    jest.spyOn($control, "getAddressApi").mockReturnValue({endLoad: jest.fn()});
    jest.spyOn($utilities, "timeout").mockImplementation((fn) => fn());
    jest.spyOn($dependencyController, "finishDependency");
    const dialogAction = action();
    const dependency = {finish: jest.fn()};
    const dependencyAction = action({parameters: {dependency}});

    $screen.openDialog(dialogAction);
    $screen.closeDialog(dialogAction);
    $screen.closeDialogAndCancel(dialogAction);
    $screen.endLoad(dialogAction);
    $screen.endDependency(dependencyAction);

    expect($control.changeControllerAttribute).toHaveBeenCalledWith({view: "base", component: "dialog"}, {opened: true, openAction: dialogAction});
    expect($control.changeControllerAttribute).toHaveBeenCalledWith({view: "base", component: "dialog"}, {opened: false, accept: true});
    expect($control.changeControllerAttribute).toHaveBeenCalledWith({view: "base", component: "dialog"}, {opened: false, accept: false});
    expect($control.getAddressApi({view: "base", component: "dialog"}).endLoad).toHaveBeenCalled();
    expect($dependencyController.finishDependency).toHaveBeenCalledWith(dependency, dependencyAction);
  });

  it("applies class, dependency, file, print, redirect, and close-window side effects", () => {
    document.body.innerHTML = '<div id="target"></div>';
    jest.spyOn($dependencyController, "toggleDependencies");
    jest.spyOn($serverData, "getFileData").mockReturnValue({url: "download"});
    jest.spyOn($utilities, "downloadFile");
    jest.spyOn($settings, "get").mockImplementation((key) => key === "targetActionKey" ? "targetAction" : key);

    const cssAction = action({target: "#target", parameters: {targetAction: "active"}});
    const fileAction = action({parameters: {name: "report.pdf"}});
    const redirectAction = action({target: "http://example.test"});
    const redirectWindowAction = action({target: "http://example.test/new", parameters: {newWindow: true}});
    const windowAction = action();

    $screen.enableDependencies(windowAction);
    $screen.disableDependencies(windowAction);
    $screen.addClass(cssAction);
    expect($("#target").hasClass("active")).toBe(true);
    $screen.removeClass(cssAction);
    expect($("#target").hasClass("active")).toBe(false);
    $screen.getFile(fileAction);
    $screen.screenPrint(windowAction);
    $screen.redirect(redirectAction);
    $screen.redirect(redirectWindowAction);
    $screen.closeWindow(windowAction);

    expect($dependencyController.toggleDependencies).toHaveBeenCalledWith(true);
    expect($dependencyController.toggleDependencies).toHaveBeenCalledWith(false);
    expect($utilities.downloadFile).toHaveBeenCalledWith({url: "download", action: fileAction});
    expect($windowMock.print).toHaveBeenCalled();
    expect($windowMock.location.href).toBe("http://example.test");
    expect($windowMock.open).toHaveBeenCalledWith("http://example.test/new", "_blank");
    expect($windowMock.close).toHaveBeenCalled();
  });

  it("redirects only when the requested screen is the current screen", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {name: "otherScreen"}, report: {name: "currentScreen"}});
    const skippedAction = action({target: "http://example.test/skipped", parameters: {screen: "missingScreen"}});
    const redirectedAction = action({target: "http://example.test/current", parameters: {screen: "currentScreen"}});

    $screen.redirectScreen(skippedAction);
    expect($windowMock.location.href).toBe("");
    $screen.redirectScreen(redirectedAction);

    expect($windowMock.location.href).toBe("http://example.test/current");
    expect($actionController.acceptAction).toHaveBeenCalledWith(skippedAction);
    expect($actionController.acceptAction).toHaveBeenCalledWith(redirectedAction);
  });

  it("skips current-screen reload when reload is disabled and accepts the action", () => {
    jest.spyOn($utilities, "sameUrl").mockReturnValue(true);
    jest.spyOn($settings, "get").mockImplementation((key) => key === "reloadCurrentScreen" ? false : undefined);
    jest.spyOn($screen, "reload").mockReturnValue(undefined);
    jest.spyOn($state, "transitionTo").mockReturnValue(undefined);
    const screenAction = action({parameters: {screen: "current"}, context: "base"});

    $screen.screen(screenAction);

    expect($screen.reload).not.toHaveBeenCalled();
    expect($state.transitionTo).not.toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(screenAction);
  });

  it("ends load safely when the target api has no endLoad function", () => {
    jest.spyOn($control, "getAddressApi").mockReturnValue({});
    const endLoadAction = action({callbackTarget: {view: "base", component: "grid"}});

    $screen.endLoad(endLoadAction);

    expect($control.getAddressApi).toHaveBeenCalledWith({view: "base", component: "grid"});
    expect($actionController.acceptAction).toHaveBeenCalledWith(endLoadAction);
  });

  it("changes theme from action parameters when there is no selected theme in storage", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    jest.spyOn($settings, "update");
    const themeAction = action({context: "base", parameters: {theme: "sky"}});

    $screen.changeTheme(themeAction);

    expect($settings.update).toHaveBeenCalledWith({theme: "sky"});
    expect($actionController.acceptAction).toHaveBeenCalledWith(themeAction);
  });

  it("changes language from action parameters when storage has no selected language", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    jest.spyOn($settings, "changeLanguage");
    const languageAction = action({context: "base", parameters: {language: "eu-ES"}});

    $screen.changeLanguage(languageAction);

    expect($settings.changeLanguage).toHaveBeenCalledWith("eu-ES");
    expect($actionController.acceptAction).toHaveBeenCalledWith(languageAction);
  });

  it("toggles every class requested by targetAction on the selected element", () => {
    document.body.innerHTML = '<div id="toggle-target" class="base"></div>';
    jest.spyOn($settings, "get").mockImplementation((key) => key === "targetActionKey" ? "targetAction" : key);
    const cssAction = action({target: "#toggle-target", parameters: {targetAction: "active highlighted"}});

    $screen.toggleClass(cssAction);
    expect($("#toggle-target").hasClass("active")).toBe(true);
    expect($("#toggle-target").hasClass("highlighted")).toBe(true);

    $screen.toggleClass(cssAction);
    expect($("#toggle-target").hasClass("active")).toBe(false);
    expect($("#toggle-target").hasClass("highlighted")).toBe(false);
    expect($actionController.acceptAction).toHaveBeenCalledWith(cssAction);
  });

  it("reloads theme variable stylesheets with a timestamp query string", () => {
    document.head.innerHTML = '<link rel="stylesheet" id="themeVariables" href="/themes/vars.css?old=1">';
    jest.spyOn(Date.prototype, "getTime").mockReturnValue(12345);
    const themeAction = action();

    $screen.updateTheme(themeAction);

    expect(document.querySelector("#themeVariables").getAttribute("href")).toBe("http://localhost/themes/vars.css?reload=12345");
    expect($actionController.acceptAction).toHaveBeenCalledWith(themeAction);
  });

  it("uses the action target when parameters do not provide screen or target", () => {
    jest.spyOn($utilities, "sameUrl").mockReturnValue(false);
    jest.spyOn($utilities, "getState").mockReturnValue({to: "base.report", parameters: {screen: "report"}});
    jest.spyOn($state, "transitionTo").mockReturnValue(undefined);
    const screenAction = action({parameters: {}, context: "base", target: "report"});

    $screen.screen(screenAction);

    expect($utilities.getState).toHaveBeenCalledWith("/base/report", false);
    expect($state.transitionTo).toHaveBeenCalledWith(
      "base.report",
      {screen: "report"},
      {reload: false, inherit: true, notify: true, location: true}
    );
    expect($actionController.acceptAction).toHaveBeenCalledWith(screenAction);
  });

  it("transitions even on the same URL when the action explicitly requests reload", () => {
    jest.spyOn($utilities, "sameUrl").mockReturnValue(true);
    jest.spyOn($utilities, "getState").mockReturnValue({to: "base.current", parameters: {screen: "current"}});
    jest.spyOn($state, "transitionTo").mockReturnValue(undefined);
    jest.spyOn($screen, "reload").mockReturnValue(undefined);
    const screenAction = action({parameters: {screen: "current", reload: true}, context: "base"});

    $screen.screen(screenAction);

    expect($utilities.getState).toHaveBeenCalledWith("/base/current", true);
    expect($state.transitionTo).toHaveBeenCalledWith(
      "base.current",
      {screen: "current"},
      {reload: false, inherit: true, notify: true, location: true}
    );
    expect($screen.reload).not.toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(screenAction);
  });

  it("stores screen data without queueing action lists when no server actions are provided", () => {
    const screenDataStore = {};
    jest.spyOn($storage, "get").mockReturnValue(screenDataStore);
    jest.spyOn($actionController, "addActionList");
    const screenData = {actions: [], components: [], screen: {name: "EMPTY"}, messages: []};
    const screenAction = action({parameters: {view: "base", screenData}});

    $screen.screenData(screenAction);

    expect(screenDataStore.base).toEqual(screenData);
    expect($actionController.addActionList).not.toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(screenAction);
  });

  it("adds the get-file server action before downloading file data", () => {
    jest.spyOn($settings, "get").mockImplementation((key) => key === "serverActionKey" ? "serverAction" : key);
    jest.spyOn($serverData, "getFileData").mockReturnValue({url: "download?file=report"});
    jest.spyOn($utilities, "downloadFile");
    const fileAction = action({parameters: {name: "report.pdf", type: "pdf"}});

    $screen.getFile(fileAction);

    expect($serverData.getFileData).toHaveBeenCalledWith("download", {
      name: "report.pdf",
      serverAction: "get-file",
      type: "pdf"
    });
    expect($utilities.downloadFile).toHaveBeenCalledWith({url: "download?file=report", action: fileAction});
  });

  it("redirects from base screen data when there is no report screen override", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {name: "currentScreen"}});
    const redirectAction = action({target: "http://example.test/current", parameters: {screen: "currentScreen"}});

    $screen.redirectScreen(redirectAction);

    expect($windowMock.location.href).toBe("http://example.test/current");
    expect($actionController.acceptAction).toHaveBeenCalledWith(redirectAction);
  });

  it("uses a one millisecond default wait when no target delay is provided", () => {
    jest.spyOn($utilities, "timeout").mockImplementation(callback => callback());
    const waitAction = action({parameters: {}});

    $screen.wait(waitAction);

    expect($utilities.timeout).toHaveBeenCalledWith(expect.any(Function), 1);
    expect($actionController.acceptAction).toHaveBeenCalledWith(waitAction);
  });

  it("changes class through the configured target-action parameter", () => {
    document.body.innerHTML = '<section id="panel"></section>';
    jest.spyOn($settings, "get").mockImplementation(key => key === "targetActionKey" ? "targetAction" : key);
    const classAction = action({target: "#panel", parameters: {targetAction: "visible"}});

    $screen.changeClass(classAction, true);
    expect($("#panel").hasClass("visible")).toBe(true);

    $screen.changeClass(classAction, false);
    expect($("#panel").hasClass("visible")).toBe(false);
  });

  it("accepts add-class actions after mutating the target element", () => {
    document.body.innerHTML = '<section id="panel"></section>';
    jest.spyOn($settings, "get").mockImplementation(key => key === "targetActionKey" ? "targetAction" : key);
    const classAction = action({target: "#panel", parameters: {targetAction: "visible"}});

    $screen.addClass(classAction);

    expect($("#panel").hasClass("visible")).toBe(true);
    expect($actionController.acceptAction).toHaveBeenCalledWith(classAction);
  });

  it("accepts remove-class actions after mutating the target element", () => {
    document.body.innerHTML = '<section id="panel" class="visible"></section>';
    jest.spyOn($settings, "get").mockImplementation(key => key === "targetActionKey" ? "targetAction" : key);
    const classAction = action({target: "#panel", parameters: {targetAction: "visible"}});

    $screen.removeClass(classAction);

    expect($("#panel").hasClass("visible")).toBe(false);
    expect($actionController.acceptAction).toHaveBeenCalledWith(classAction);
  });

  it("accepts enable dependency actions through the toggle boundary", () => {
    jest.spyOn($dependencyController, "toggleDependencies");
    const dependencyAction = action();

    $screen.enableDependencies(dependencyAction);

    expect($dependencyController.toggleDependencies).toHaveBeenCalledWith(true);
    expect($actionController.acceptAction).toHaveBeenCalledWith(dependencyAction);
  });

  it("accepts disable dependency actions through the toggle boundary", () => {
    jest.spyOn($dependencyController, "toggleDependencies");
    const dependencyAction = action();

    $screen.disableDependencies(dependencyAction);

    expect($dependencyController.toggleDependencies).toHaveBeenCalledWith(false);
    expect($actionController.acceptAction).toHaveBeenCalledWith(dependencyAction);
  });

  it("prints the current window and accepts the action", () => {
    const printAction = action();

    $screen.screenPrint(printAction);

    expect($windowMock.print).toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(printAction);
  });

  it("redirects in the current window by default", () => {
    const redirectAction = action({target: "http://example.test/current", parameters: {}});

    $screen.redirect(redirectAction);

    expect($windowMock.location.href).toBe("http://example.test/current");
    expect($windowMock.open).not.toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(redirectAction);
  });

  it("redirects in a new window when requested", () => {
    const redirectAction = action({target: "http://example.test/new", parameters: {newWindow: true}});

    $screen.redirect(redirectAction);

    expect($windowMock.open).toHaveBeenCalledWith("http://example.test/new", "_blank");
    expect($actionController.acceptAction).toHaveBeenCalledWith(redirectAction);
  });

  it("closes the current window after accepting the action", () => {
    const closeAction = action();

    $screen.closeWindow(closeAction);

    expect($actionController.acceptAction).toHaveBeenCalledWith(closeAction);
    expect($windowMock.close).toHaveBeenCalled();
  });

  it("accepts change-language actions without updating settings when no language is available", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    jest.spyOn($settings, "changeLanguage");
    const languageAction = action({parameters: {}, target: "language", view: "base"});

    $screen.changeLanguage(languageAction);

    expect($settings.changeLanguage).not.toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(languageAction);
  });

  it("accepts change-theme actions without updating settings when no theme is available", () => {
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    jest.spyOn($settings, "update");
    const themeAction = action({parameters: {}, target: "theme", view: "base"});

    $screen.changeTheme(themeAction);

    expect($settings.update).not.toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(themeAction);
  });
});
