import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/services/comet.js', function() {
  let $injector, $utilities, $actionController, $comet, $httpBackend;
  let originalTimeout;

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $utilities = $injector.get('AweUtilities');
      $actionController = $injector.get('ActionController');
      $comet = $injector.get('Comet');
      $httpBackend = $injector.get('$httpBackend');
    }]);

    // backend definition common for all tests
    $httpBackend.when('GET', '/websocket').respond("{}");

    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function() {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  it('should tell if it is connected', function() {
    expect($comet.isConnected()).toBe(false);
  });

  it('should init', function() {
    spyOn($comet, "_connect");
    $comet.init();
    expect($comet._connect).toHaveBeenCalled();
  });

  it('should init with connection', function() {
    spyOn($comet, "getConnection").and.returnValue({});
    spyOn($comet, "_connect");
    $comet.init();
    expect($comet._connect).toHaveBeenCalled();
  });

  it('should connect', function() {
    const client = {activate: jasmine.createSpy("activate")};
    spyOn($comet, "getWebsocketClient").and.returnValue(client);
    $comet._connect();
    expect(client.activate).toHaveBeenCalled();
  });

  it('should disconnect', function() {
    let connection = {deactivate: jasmine.createSpy("activate"), active: true};
    spyOn($comet, "getConnection").and.returnValue(connection);
    $comet.disconnect();
    expect(connection.deactivate).toHaveBeenCalled();
  });

  it('should disconnect (but not active)', function() {
    let connection = {deactivate: jasmine.createSpy("activate"), active: false};
    spyOn($comet, "getConnection").and.returnValue(connection);
    $comet.disconnect();
    expect(connection.deactivate).not.toHaveBeenCalled();
  });

  it('should disconnect and reconnect', function() {
    let connection = {deactivate: () => ({then: (fn) => fn()}), active: true};
    spyOn($comet, "getConnection").and.returnValue(connection);
    spyOn($comet, "_connect");
    $comet._reconnect();
    expect($comet._connect).toHaveBeenCalled();
  });

  it('should reconnect', function() {
    let connection = {deactivate: jasmine.createSpy("activate"), active: false};
    spyOn($comet, "getConnection").and.returnValue(connection);
    spyOn($comet, "_connect");
    $comet._reconnect();
    expect($comet._connect).toHaveBeenCalled();
    expect(connection.deactivate).not.toHaveBeenCalled();
  });

  it('should manage a broadcast call', function() {
    spyOn($utilities, "decodeData").and.returnValue("");
    spyOn($actionController, "addActionList");
    $comet.manageBroadcast({});
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it('should manage a broadcast call with actions', function() {
    spyOn($utilities, "decodeData").and.returnValue([{}]);
    spyOn($actionController, "addActionList");
    $comet.manageBroadcast({});
    expect($actionController.addActionList).toHaveBeenCalled();
  });

  it('should close the connection', function() {
    spyOn($comet, "_disconnect");
    $comet._close();
    expect($comet._disconnect).toHaveBeenCalled();
  });
});