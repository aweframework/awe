import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe('awe-framework/awe-client-angular/src/test/jest/singletons/actionController.js', function() {
  let $injector, $actionController, $settings, $utilities;

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $actionController = $injector.get('ActionController');
      $settings = $injector.get('AweSettings');
      $utilities = $injector.get('AweUtilities');

      // Get settings
      jest.spyOn($settings, "get").mockReturnValue("");
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  it('should finish an action', function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let action = {accept: accept, reject: reject};

    // Launch and assert
    $actionController.finishAction(action, true);
    expect(action.accept).toHaveBeenCalled();

    $actionController.finishAction(action, false);
    expect(action.reject).toHaveBeenCalled();
  });

  it('should accept an action', function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let action = {accept: accept, reject: reject};

    // Launch and assert
    $actionController.acceptAction(action);
    expect(action.accept).toHaveBeenCalled();
  });

  it('should reject an action', function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let action = {accept: accept, reject: reject};

    // Launch and assert
    $actionController.rejectAction(action);
    expect(action.reject).toHaveBeenCalled();
  });

  it('should abort an action', function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let abort = jest.fn().mockName("abort");
    let action = {accept: accept, reject: reject, abort: abort};

    // Launch and assert
    $actionController.abortAction(action);
    expect(action.abort).toHaveBeenCalled();
  });

  it('should resolve an action', function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let abort = jest.fn().mockName("abort");
    let action = {accept: accept, reject: reject, abort: abort, attr: () => ({})};

    // Launch
    $actionController.resolveAction(action, {method: "lala", service: {"lala": () => null}});

    // Assert
    expect(action.accept).toHaveBeenCalled();
  });

  it("shouldn't resolve an action", function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let abort = jest.fn().mockName("abort");
    let action = {accept: accept, reject: reject, abort: abort, attr: () => ({})};
    jest.spyOn($utilities, "checkAddress").mockReturnValue(false);

    // Launch
    $actionController.resolveAction(action, {method: "lala", service: {"lala": () => null}});

    // Assert
    expect(action.accept).not.toHaveBeenCalled();
  });

  it("manages null data on action list", function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let abort = jest.fn().mockName("abort");
    let action = {accept: accept, reject: reject, abort: abort, attr: () => ({})};
    jest.spyOn($actionController, "runNext").mockReturnValue(false);

    // Launch
    $actionController.addActionList(null, true, {});

    // Assert
    expect($actionController.runNext).not.toHaveBeenCalled();
  });

  it("runs next action with action already running", function() {
    // Mock
    let accept = jest.fn().mockName("accept");
    let reject = jest.fn().mockName("reject");
    let abort = jest.fn().mockName("abort");
    let action = {accept: accept, reject: reject, abort: abort, attr: () => ({running: true, async: true})};
    $actionController.actionStackList[0] = [action];
    jest.spyOn($actionController, "runAction");

    // Launch
    $actionController.runNext();

    // Assert
    expect($actionController.runAction).not.toHaveBeenCalled();
    expect($actionController.actionStackList[0]).toEqual([]);
    expect($actionController.asyncStackList).toEqual([action]);
  });
});
