import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/services/settings.js', function() {
  let $injector, $utilities, $settings, $storage, $rootScope, $translate, $httpBackend, $log, $state, $serverData, $connection;
  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $utilities = $injector.get('AweUtilities');
      $settings = $injector.get('AweSettings');
      $storage = $injector.get('Storage');
      $translate = $injector.get('$translate');
      $httpBackend = $injector.get('$httpBackend');
      $log = $injector.get('$log');
      $state = $injector.get('$state');
      $serverData = $injector.get('ServerData');
      $connection = $injector.get('Connection');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  it('should init', function(done) {
    // backend definition common for all tests
    $httpBackend.when('POST', 'settings').respond(DefaultSettings);
    jest.spyOn($settings, "settingsLoaded").mockImplementation(done);
    $settings.init();
    $httpBackend.flush();
  });

  it('should init with errors', function(done) {
    // backend definition common for all tests
    $httpBackend.when('POST', 'settings').respond(404, { foo: 'bar' });
    jest.spyOn($log, "error").mockImplementation(done);
    $settings.init();
    $httpBackend.flush();
  });

  it('should init settings', function() {
    jest.spyOn($state, "go");
    jest.spyOn($connection, "init").mockReturnValue({then: (fn) => fn()});
    $settings.settingsLoaded(DefaultSettings);
    expect($state.go).toHaveBeenCalled();
  });

  it('should init settings reloading current screen', function() {
    jest.spyOn($state, "go");
    jest.spyOn($connection, "init").mockReturnValue({then: (fn) => fn()});
    $settings.settingsLoaded({...DefaultSettings, reloadCurrentScreen: true});
    expect($state.go).toHaveBeenCalled();
  });

  it('should get a token', function() {
    jest.spyOn($storage, "hasSession").mockReturnValue(true);
    jest.spyOn($storage, "getSession");
    jest.spyOn($storage, "get");
    $settings.getToken();
    expect($storage.getSession).toHaveBeenCalled();
    expect($storage.get).not.toHaveBeenCalled();
  });

  it('should set a token', function(done) {
    jest.spyOn($settings, "get").mockReturnValue("es");
    jest.spyOn($injector, "get").mockReturnValue({init: fn => null});
    jest.spyOn($storage, "putSession").mockImplementation(done);
    jest.spyOn($storage, "getSession").mockReturnValue("tutu");
    $settings.setToken("tutu");
    expect($storage.putSession).toHaveBeenCalled();
  });

  it('should clear a token', function() {
    jest.spyOn($storage, "removeSession");
    $settings.clearToken();
    expect($storage.removeSession).toHaveBeenCalled();
  });

  it('should try to change the same language without forcing', function() {
    jest.spyOn($settings, "get").mockReturnValue("es");
    jest.spyOn($settings, "update");
    $settings.changeLanguage("es", false);
    expect($settings.update).not.toHaveBeenCalled();
  });

  it('should try to change the same language forcing', function() {
    jest.spyOn($settings, "get").mockReturnValue("es");
    jest.spyOn($settings, "update");
    $settings.changeLanguage("es", true);
    expect($settings.update).toHaveBeenCalled();
  });

  it('should try to change language to null', function() {
    jest.spyOn($settings, "update");
    $settings.changeLanguage(null, false);
    expect($settings.update).not.toHaveBeenCalled();
  });

  it('should try to change language successfully', function() {
    jest.spyOn($settings, "get").mockReturnValue("es");
    jest.spyOn($translate, "use").mockReturnValue({then: fn => fn()});
    jest.spyOn($settings, "update");
    jest.spyOn($utilities, "publish");
    $settings.changeLanguage("en", true);
    expect($utilities.publish).toHaveBeenCalled();
  });

  it('should preload the templates', function() {
    jest.spyOn($injector, "get").mockReturnValue($serverData);
    jest.spyOn($serverData, "preloadAngularTemplate");
    $settings.preloadTemplates();
    expect($serverData.preloadAngularTemplate).toHaveBeenCalledTimes(15);
  });

  it('should update undefined settings', function() {
    jest.spyOn($storage, "putRoot");
    $settings.update();
    expect($storage.putRoot).toHaveBeenCalled();
  });
});