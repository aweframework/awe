import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe("AweSettings", () => {
  let $injector;
  let $utilities;
  let $settings;
  let $storage;
  let $translate;
  let $httpBackend;
  let $log;
  let $state;
  let $serverData;
  let $connection;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", (__$injector__) => {
      $injector = __$injector__;
      $utilities = $injector.get("AweUtilities");
      $settings = $injector.get("AweSettings");
      $storage = $injector.get("Storage");
      $translate = $injector.get("$translate");
      $httpBackend = $injector.get("$httpBackend");
      $log = $injector.get("$log");
      $state = $injector.get("$state");
      $serverData = $injector.get("ServerData");
      $connection = $injector.get("Connection");
    }]);
  });

  afterEach(() => {
    try {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    } catch (error) {
      // Some specs do not use $httpBackend; keep cleanup best-effort like the existing migrated AngularJS specs.
    }
  });

  it("loads settings from the backend and delegates to settingsLoaded", () => {
    $httpBackend.when("POST", "settings").respond(DefaultSettings);
    $settings.settingsLoaded = jest.fn();

    $settings.init();
    $httpBackend.flush();

    expect($settings.settingsLoaded).toHaveBeenCalledWith(DefaultSettings);
  });

  it("logs backend failures without resolving settings", () => {
    $httpBackend.when("POST", "settings").respond(404, {foo: "bar"});
    $log.error = jest.fn();

    $settings.init();
    $httpBackend.flush();

    expect($log.error).toHaveBeenCalledWith("FATAL ERROR: Application settings retrieval failure", expect.objectContaining({status: 404}));
  });

  it("initializes connection and navigates to the configured state", () => {
    $state.transitionTo = jest.fn(() => ({}));
    $connection.init = jest.fn(() => ({then: (fn) => fn()}));
    $utilities.getState = jest.fn(() => ({to: "screen", parameters: {view: "home"}}));

    $settings.settingsLoaded(DefaultSettings);

    expect($connection.init).toHaveBeenCalledWith(DefaultSettings.pathServer, DefaultSettings.encodeTransmission, DefaultSettings.cometUID);
    expect($state.transitionTo).toHaveBeenCalledWith("screen", {view: "home"}, expect.objectContaining({reload: false, inherit: true, notify: true, location: true}));
  });

  it("reloads the current screen when reloadCurrentScreen is enabled", () => {
    $state.transitionTo = jest.fn(() => ({}));
    $connection.init = jest.fn(() => ({then: (fn) => fn()}));
    $utilities.getState = jest.fn(() => ({to: "current", parameters: {id: "1"}}));

    $settings.settingsLoaded({...DefaultSettings, reloadCurrentScreen: true, initialURL: "screen/current"});

    expect($utilities.getState).toHaveBeenCalledWith("screen/current");
    expect($state.transitionTo).toHaveBeenCalledWith("current", {id: "1"}, expect.objectContaining({reload: false, inherit: true, notify: true, location: true}));
  });

  it("reads the session token before falling back to the settings token", () => {
    jest.spyOn($storage, "hasSession").mockReturnValue(true);
    jest.spyOn($storage, "getSession").mockReturnValue("session-token");
    jest.spyOn($storage, "get");

    expect($settings.getToken()).toBe("session-token");
    expect($storage.getSession).toHaveBeenCalledWith("token");
    expect($storage.get).not.toHaveBeenCalled();
  });

  it("stores, clears, and publishes token changes through storage", () => {
    jest.spyOn($storage, "putSession");
    jest.spyOn($storage, "removeSession");

    $settings.setToken("new-token");
    $settings.clearToken();

    expect($storage.putSession).toHaveBeenCalledWith("token", "new-token");
    expect($storage.removeSession).toHaveBeenCalledWith("token");
  });

  it("does not change a null or unchanged language unless forced", () => {
    jest.spyOn($settings, "get").mockReturnValue("es");
    jest.spyOn($settings, "update");

    $settings.changeLanguage(null, false);
    $settings.changeLanguage("es", false);

    expect($settings.update).not.toHaveBeenCalled();
  });

  it("changes language and publishes language events", () => {
    jest.spyOn($settings, "get").mockReturnValue("es");
    jest.spyOn($translate, "use").mockReturnValue({then: (fn) => fn()});
    jest.spyOn($settings, "update");
    jest.spyOn($utilities, "publish");

    $settings.changeLanguage("en", true);

    expect($settings.update).toHaveBeenCalledWith({language: "en"});
    expect($utilities.publish).toHaveBeenCalledWith("launchLocalFunctions");
    expect($utilities.publish).toHaveBeenCalledWith("languageChanged", "en");
  });

  it("preloads the Angular templates required by grids", () => {
    jest.spyOn($injector, "get").mockReturnValue($serverData);
    jest.spyOn($serverData, "preloadAngularTemplate");

    $settings.preloadTemplates();

    expect($serverData.preloadAngularTemplate).toHaveBeenCalledTimes(15);
  });

  it("merges undefined settings into the stored root settings object", () => {
    jest.spyOn($storage, "putRoot");

    $settings.update();

    expect($storage.putRoot).toHaveBeenCalledWith("settings", expect.any(Object));
  });

  it("falls back to the configured settings token when no session token exists", () => {
    jest.spyOn($storage, "hasSession").mockReturnValue(false);

    expect($settings.getToken()).toMatch(/^[0-9a-f-]+$/);
  });

  it("builds authentication headers from the current token", () => {
    jest.spyOn($settings, "getToken").mockReturnValue("token-1");

    expect($settings.getAuthenticationHeaders()).toEqual({Authorization: "token-1", "Content-Type": "application/json"});
  });

  it("updates settings by merging new values into existing root settings", () => {
    jest.spyOn($storage, "getRoot").mockReturnValue({language: "es", theme: "dark"});
    jest.spyOn($storage, "putRoot");

    $settings.update({language: "en"});

    expect($storage.putRoot).toHaveBeenCalledWith("settings", expect.objectContaining({language: "en", theme: "dark"}));
  });
});
