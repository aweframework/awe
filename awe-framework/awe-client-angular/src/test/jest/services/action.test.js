import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe('awe-framework/awe-client-angular/src/test/jest/services/action.js', function() {
  let $injector, Action, $log;

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      Action = $injector.get('Action');
      $log = $injector.get('$log');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  // Show action info
  it('should show action information', function() {
    $log.debug = jest.fn();
    new Action().showInfo();
    expect($log.debug).toHaveBeenCalled();
  });

  // Check action is alive
  it('should check action is alive', function() {
    expect(new Action().isAlive()).toBe(true);
  });

  // Check action accept
  it('should accept action', function() {
    let action = new Action();
    let deferred = action.attr("deferred");
    jest.spyOn(deferred, "resolve");
    action.accept();
    expect(deferred.resolve).toHaveBeenCalled();
  });

  // Check action reject
  it('should reject action', function() {
    // Reject first
    let action = new Action();
    let deferred = action.attr("deferred");
    jest.spyOn(deferred, "reject");
    action.reject();
    expect(deferred.reject).toHaveBeenCalled();

    // Reject with function
    action = new Action();
    action.onReject = () => null;
    jest.spyOn(action, "onReject");
    action.reject();
    expect(action.onReject).toHaveBeenCalled();
  });

  // Check action abort
  it('should abort action', function() {
    let action = new Action();
    let deferred = action.attr("deferred");
    jest.spyOn(deferred, "resolve");
    action.abort();
    expect(deferred.resolve).toHaveBeenCalled();
    expect(action.isAlive()).toBe(false);
  });

  // Check cancel abort
  it('should cancel action', function() {
    // Cancel first
    let action = new Action();
    action.cancel();
    expect(action.isAlive()).toBe(false);

    // Cancel with function
    action = new Action();
    action.onCancel = () => null;
    jest.spyOn(action, "onCancel");
    action.cancel();
    expect(action.onCancel).toHaveBeenCalled();
  });
});
