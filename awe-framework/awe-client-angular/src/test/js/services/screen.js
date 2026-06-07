import {DefaultSettings} from "./../../../main/resources/js/awe/data/options";
import {launchScreenAction} from "../utils";

describe('awe-framework/awe-client-angular/src/test/js/services/screen.js', function() {
  let $injector, $utilities, $settings, $actionController, $windowMock, $control, $rootScope, $state, $storage, $httpBackend, $location;
  // Mock module
  beforeEach(function() {
    $windowMock = {print: () => null, close: () => null, sessionStorage: {removeItem: () => null}, history: {back: () => null}};
    angular.mock.module('aweApplication', {
      '$window': $windowMock
    });

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $utilities = $injector.get('AweUtilities');
      $settings = $injector.get('AweSettings');
      $control = $injector.get('Control');
      $state = $injector.get('$state');
      $actionController = $injector.get('ActionController');
      $storage = $injector.get('Storage');
      $httpBackend = $injector.get('$httpBackend');
      $location = $injector.get('$location');

      // backend definition common for all tests
      $httpBackend.when('POST', 'settings').respond(DefaultSettings);

      // Catch clearToken && load
      jest.spyOn($settings, "clearToken");
      jest.spyOn($settings, "setToken");
      jest.spyOn($state, "go");
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  // Launch screen action
  it('should launch a screen action', function(done) {
    return launchScreenAction($injector, "screen", "screen", {parameters:{language:"es", theme:"clean", token: null}, context: "screen", target: "signin"}, done);
  });

  // Launch screen action
  it('should launch a screen action with token', function(done) {
    return launchScreenAction($injector, "screen", "screen", {parameters:{language:"es", theme:"clean", token: "alallalala", target: "/"}}, done);
  });

  // Launch screen action
  it('should launch a screen action with screen', function(done) {
    jest.spyOn($settings, "get").mockReturnValue(true);
    jest.spyOn($location, "url").mockReturnValue("/epa/lala");
    launchScreenAction($injector, "screen", "screen", {parameters:{screen: "lala"}, context: "epa"}, done);
  });

  // Launch screen action
  it('should launch a screen action with screen and not reloading', function(done) {
    jest.spyOn($settings, "get").mockReturnValue(false);
    jest.spyOn($location, "url").mockReturnValue("/epa/lala");
    launchScreenAction($injector, "screen", "screen", {parameters:{screen: "lala"}, context: "epa"}, done);
  });

  // Launch screen action
  it('should launch a screen action with screen reloading', function(done) {
    jest.spyOn($location, "url").mockReturnValue("/epa/lala");
    launchScreenAction($injector, "screen", "screen", {parameters:{screen: "lala", reload: true}, context: "epa"}, done);
  });

  // Launch reload action
  it('should launch a reload action', function(done) {
    return launchScreenAction($injector, "reload", "reload", {parameters:{}}, done);
  });

  // Launch back action
  it('should launch a back action', function(done) {
    return launchScreenAction($injector, "back", "back", {parameters:{}}, done);
  });

  // Launch wait action
  it('should launch a wait action', function() {
    jest.useFakeTimers();
    jest.spyOn($utilities, "timeout");
    launchScreenAction($injector, "wait", "wait", {parameters:{target:5}}, () => null);
    jest.advanceTimersByTime(6);
    jest.useRealTimers();
    expect($utilities.timeout).toHaveBeenCalled();
  });

  // Launch change-language action
  it('should launch a change-language action', function(done) {
    jest.spyOn($storage, "get").mockReturnValue({base:{language:{selected:"es"}}});
    return launchScreenAction($injector, "change-language", "changeLanguage", {target: "language", context: "base"}, done);
  });

  // Launch change-language action
  it('should launch a change-language action with a defined language', function(done) {
    jest.spyOn($storage, "get").mockReturnValue({base:{language:{selected:"fr-FR"}}});
    //$control.changeComponent({component: "language", view: "base"}, {model: {values: [{selected: true, value: "fr", label: "Français"}]}});
    return launchScreenAction($injector, "change-language", "changeLanguage", {target: "language", context: "base"}, done);
  });

  // Launch change-theme action
  it('should launch a change-theme action', function(done) {
    jest.spyOn($settings, "get").mockReturnValue("sky");
    jest.spyOn($settings, "update");
    jest.spyOn($storage, "get").mockReturnValue({base:{}});
    return launchScreenAction($injector, "change-theme", "changeTheme", {context: "base"}, done);
  });

  // Launch change-theme action
  it('should launch a change-theme action with a defined theme', function(done) {
    jest.spyOn($settings, "get").mockReturnValue("sky");
    jest.spyOn($settings, "update");
    jest.spyOn($storage, "get").mockReturnValue({base:{theme:{selected:"default"}}});
    return launchScreenAction($injector, "change-theme", "changeTheme", {target: "theme", context: "base"}, done);
  });

  // Launch screen-data action
  it('should launch a screen-data action', function(done) {
    $rootScope.firstLoad = true;
    jest.spyOn($actionController, "addActionList");
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [{type: "reload"}], components: [{
          id: "cod_usr",
          controller: {checkInitial: true, checkTarget:false, checked:false, component:"text", contextMenu:[], dependencies:[], icon:"user signin-form-icon", id:"cod_usr", loadAll:false, optional:false, placeholder:"SCREEN_TEXT_USER", printable:true, readonly:false, required:true, size:"lg", strict:true, style:"no-label", validation:"required", visible:true},
          model: {defaultValues:[], page:1, records:0, selected:[], total:0, values:[]}
        },{
          id: "pwd_usr",
          controller: {checkInitial: true, checkTarget:false, checked:false, component:"text", contextMenu:[], dependencies:[], icon:"user signin-form-icon", id:"cod_usr", loadAll:false, optional:false, placeholder:"SCREEN_TEXT_USER", printable:true, readonly:false, required:true, size:"lg", strict:true, style:"no-label", validation:"required", visible:true},
          model: {defaultValues:[], page:1, records:0, selected:["test"], total:0, values:[]}
         },{
           id: "oth_usr",
           controller: {checkInitial: true, checkTarget:false, checked:false, component:"text", contextMenu:[], dependencies:[], icon:"user signin-form-icon", id:"cod_usr", loadAll:false, optional:false, placeholder:"SCREEN_TEXT_USER", printable:true, readonly:false, required:true, size:"lg", strict:true, style:"no-label", validation:"required", visible:true},
           model: {defaultValues:[], page:1, records:2, selected:["test"], total:1, values:[{value:"test", label:"test"}, {value: "oth", label: "other"}]}
       },{
         id: "grd_usr",
         controller: {columnModel: [], checkInitial: true, checkTarget:false, checked:false, component:"text", contextMenu:[], dependencies:[], icon:"user signin-form-icon", id:"cod_usr", loadAll:false, optional:false, placeholder:"SCREEN_TEXT_USER", printable:true, readonly:false, required:true, size:"lg", strict:true, style:"no-label", validation:"required", visible:true},
         model: {defaultValues:[], page:1, records:1, selected:[], total:1, values:[{id:1, tutu:"aasd", lala: "awsda"}]}
       }
      ], screen: {name: "TEST"}, messages: []}}}, () => {
      done();
    });
  });

  // Launch screen-data action
  it('should launch a screen-data action without actions', function(done) {
    $rootScope.firstLoad = true;
    jest.spyOn($actionController, "addActionList");
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [], components: [], screen: {name: "TEST"}, messages: []}}}, () => {
      done();
    });
  });

  // Launch end load action
  it('should launch an end-load action', function() {
    jest.spyOn($control, "getAddressApi").mockReturnValue({endLoad: () => null });
    return launchScreenAction($injector, "end-load", "endLoad", {parameters:{}}, () => null);
  });

  // Launch end load action without function
  it('should launch an end-load action without function', function() {
    jest.spyOn($control, "getAddressApi").mockReturnValue({});
    return launchScreenAction($injector, "end-load", "endLoad", {parameters:{}}, () => null);
  });

  // Launch dialog action
  it('should launch a dialog action', function() {
    return launchScreenAction($injector, "dialog", "openDialog", {parameters:{}}, () => null);
  });

  // Launch close action
  it('should launch a close action', function(done) {
    jest.spyOn($utilities, "timeout").mockImplementation(done);
    return launchScreenAction($injector, "close", "closeDialog", {parameters:{}}, () => null);
  });

  // Launch close-cancel action
  it('should launch a close-cancel action', function(done) {
    jest.spyOn($utilities, "timeout").mockImplementation(done);
    return launchScreenAction($injector, "close-cancel", "closeDialogAndCancel", {parameters:{}}, () => null);
  });

  // Launch get-file action
  it('should launch a get-file action', function() {
    return launchScreenAction($injector, "get-file", "getFile", {parameters:{}}, () => null);
  });

  // Launch enable-dependencies action
  it('should launch a enable-dependencies action', function(done) {
    return launchScreenAction($injector, "enable-dependencies", "enableDependencies", {parameters:{}}, done);
  });

  // Launch disable-dependencies action
  it('should launch a disable-dependencies action', function(done) {
    return launchScreenAction($injector, "disable-dependencies", "disableDependencies", {parameters:{}}, done);
  });

  // Launch add-class action
  it('should launch a add-class action', function(done) {
    return launchScreenAction($injector, "add-class", "addClass", {parameters:{}}, done);
  });

  // Launch remove-class action
  it('should launch a remove-class action', function(done) {
    return launchScreenAction($injector, "remove-class", "removeClass", {parameters:{}}, done);
  });

  // Launch print action
  it('should launch a print action', function(done) {
    jest.spyOn($windowMock, "print");
    return launchScreenAction($injector, "print", "screenPrint", {id: 1, parameters:{}}, () => {
      expect($windowMock.print).toHaveBeenCalled();
      done();
    });
  });

  // Launch redirect action
  it('should launch a redirect action', function(done) {
    $windowMock.location = { href : "" };
    launchScreenAction($injector, "redirect", "redirect", {id: 2, target: "http://alla.que.voy"}, () => {
      expect($windowMock.location.href).toBe("http://alla.que.voy");
      done();
    });
  });

  // Launch redirect action in a new window
  it('should launch a redirect action in a new window', function(done) {
    $windowMock.location = { href : "" };
    $windowMock.open = () => null;
    jest.spyOn($windowMock, "open");
    launchScreenAction($injector, "redirect", "redirect", {id: 2, target: "http://alla.que.voy", parameters: {newWindow: true}}, () => {
      expect($windowMock.open).toHaveBeenCalledTimes(1);
      done();
    });
  });

  it('should launch a redirect screen action on other screen', function(done) {
    $windowMock.location = { href : "" };
    jest.spyOn($storage, "get").mockReturnValue({base: {name: "otherScreen"}});
    launchScreenAction($injector, "redirect-screen", "redirectScreen", {id: 2, view: "base", target: "http://alla.que.voy", parameters: {screen: "currentScreen"}}, () => {
      expect($windowMock.location.href).toBe("");
      done();
    });
  });

  it('should launch a redirect screen action on current screen on report', function(done) {
    $windowMock.location = { href : "" };
    jest.spyOn($storage, "get").mockReturnValue({base: {name: "otherScreen"}, report: {name: "currentScreen"}});
    launchScreenAction($injector, "redirect-screen", "redirectScreen", {id: 2, view: "base", target: "http://alla.que.voy", parameters: {screen: "currentScreen"}}, () => {
      expect($windowMock.location.href).toBe("http://alla.que.voy");
      done();
    });
  });

  it('should launch a redirect screen action on current screen', function(done) {
    $windowMock.location = { href : "" };
    jest.spyOn($storage, "get").mockReturnValue({base: {name: "currentScreen"}});
    launchScreenAction($injector, "redirect-screen", "redirectScreen", {id: 2, view: "base", target: "http://alla.que.voy", parameters: {screen: "currentScreen"}}, () => {
      expect($windowMock.location.href).toBe("http://alla.que.voy");
      done();
    });
  });

  // Launch close-window action
  it('should launch a close-window action', function(done) {
    jest.spyOn($windowMock, "close").mockImplementation(() => {
      done();
    });
    launchScreenAction($injector, "close-window", "closeWindow", {id: 2, parameters:{}}, () => null);
  });

  // Launch end-dependency action
  it('should launch a end-dependency action', function(done) {
    return launchScreenAction($injector, "end-dependency", "endDependency", {parameters:{dependency: {finish: () => null}}}, done);
  });
});
