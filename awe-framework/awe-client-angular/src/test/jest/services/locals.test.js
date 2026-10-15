import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";

void DefaultSettings;

describe("AweLocals locale functions script", () => {
  let $rootScope;
  let $settings;
  let Connection;

  const scriptSource = () => $("#localeFunctions").attr("src");

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $rootScope = $injector.get("$rootScope");
      $settings = $injector.get("AweSettings");
      Connection = $injector.get("Connection");
      // Instantiate the service so it listens to launchLocalFunctions
      $injector.get("AweLocals");
    }]);

    jest.spyOn($settings, "getLanguage").mockReturnValue("es-ES");
    jest.spyOn(Connection, "getRawUrl").mockReturnValue("/app");
  });

  afterEach(() => {
    $("#localeFunctions").remove();
    $("meta[name^='awe-locals-']").remove();
  });

  it("loads the versioned URL published by the page for the current language", () => {
    $("head").append("<meta name='awe-locals-es-ES' content='/app/js/locals-es-ES-0123abcd.js'/>");
    $("head").append("<meta name='awe-locals-en-GB' content='/app/js/locals-en-GB-4567cdef.js'/>");

    $rootScope.$broadcast("launchLocalFunctions");

    expect(scriptSource()).toBe("/app/js/locals-es-ES-0123abcd.js");
  });

  it("falls back to the plain URL when the page publishes no URL for the language", () => {
    $rootScope.$broadcast("launchLocalFunctions");

    expect(scriptSource()).toBe("/app/js/locals-es-ES.js");
  });

  it("falls back to the plain URL when the language is not a valid selector value", () => {
    $("head").append("<meta name='awe-locals-es-ES' content='/app/js/locals-es-ES-0123abcd.js'/>");
    $settings.getLanguage.mockReturnValue("es']");

    expect(() => $rootScope.$broadcast("launchLocalFunctions")).not.toThrow();
    expect(scriptSource()).toBe("/app/js/locals-es']" + ".js");
  });

  it("replaces the previous locale functions script instead of adding another one", () => {
    $rootScope.$broadcast("launchLocalFunctions");
    $settings.getLanguage.mockReturnValue("en-GB");

    $rootScope.$broadcast("launchLocalFunctions");

    expect($("script#localeFunctions").length).toBe(1);
    expect(scriptSource()).toBe("/app/js/locals-en-GB.js");
  });
});
