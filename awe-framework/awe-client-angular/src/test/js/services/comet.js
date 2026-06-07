import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/services/comet.js', function() {
  let $injector, $utilities, $actionController, $comet, $httpBackend;
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

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  it('should tell if it is connected', function() {
    expect($comet.isConnected()).toBe(false);
  });

  it('should init', function() {
    jest.spyOn($comet, "_connect");
    $comet.init();
    expect($comet._connect).toHaveBeenCalled();
  });

  it('should init with connection', function() {
    jest.spyOn($comet, "getConnection").mockReturnValue({});
    jest.spyOn($comet, "_connect");
    $comet.init();
    expect($comet._connect).toHaveBeenCalled();
  });

  it('should connect', function() {
    const client = {activate: jest.fn().mockName("activate")};
    jest.spyOn($comet, "getWebsocketClient").mockReturnValue(client);
    $comet._connect();
    expect(client.activate).toHaveBeenCalled();
  });

  it('should disconnect', function() {
    let connection = {deactivate: jest.fn().mockName("activate"), active: true};
    jest.spyOn($comet, "getConnection").mockReturnValue(connection);
    $comet.disconnect();
    expect(connection.deactivate).toHaveBeenCalled();
  });

  it('should disconnect (but not active)', function() {
    let connection = {deactivate: jest.fn().mockName("activate"), active: false};
    jest.spyOn($comet, "getConnection").mockReturnValue(connection);
    $comet.disconnect();
    expect(connection.deactivate).not.toHaveBeenCalled();
  });

  it('should disconnect and reconnect', function() {
    let connection = {deactivate: () => ({then: (fn) => fn()}), active: true};
    jest.spyOn($comet, "getConnection").mockReturnValue(connection);
    jest.spyOn($comet, "_connect");
    $comet._reconnect();
    expect($comet._connect).toHaveBeenCalled();
  });

  it('should reconnect', function() {
    let connection = {deactivate: jest.fn().mockName("activate"), active: false};
    jest.spyOn($comet, "getConnection").mockReturnValue(connection);
    jest.spyOn($comet, "_connect");
    $comet._reconnect();
    expect($comet._connect).toHaveBeenCalled();
    expect(connection.deactivate).not.toHaveBeenCalled();
  });

  it('should manage a broadcast call', function() {
    jest.spyOn($utilities, "decodeData").mockReturnValue("");
    jest.spyOn($actionController, "addActionList");
    $comet.manageBroadcast({});
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it('should manage a broadcast call with actions', function() {
    jest.spyOn($utilities, "decodeData").mockReturnValue([{}]);
    jest.spyOn($actionController, "addActionList");
    $comet.manageBroadcast({});
    expect($actionController.addActionList).toHaveBeenCalled();
  });

  it('should close the connection', function() {
    jest.spyOn($comet, "_disconnect");
    $comet._close();
    expect($comet._disconnect).toHaveBeenCalled();
  });
});