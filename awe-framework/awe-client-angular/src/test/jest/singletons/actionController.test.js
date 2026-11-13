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

  it('passes the action to the method that resolves it, so the method can read its attributes (value)', function() {
    const method = jest.fn();
    const scope = {component: {address: {view: "base", component: "grid"}}};
    const action = {accept: jest.fn(), attr: name => ({parameters: {row: 1}, async: false, silent: true, value: "source", callbackTarget: {view: "base", component: "grid"}})[name]};

    $actionController.resolveAction(action, {method: "copy", service: {copy: method}, scope});

    expect(method).toHaveBeenCalledWith({row: 1, async: false, silent: true}, scope, {view: "base", component: "grid"}, action);
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

  // The client actions of a grid are silent by default: they are immediate and never disable the buttons. A server
  // action on a grid disables them unless it is marked silent
  it.each([
    ["a client grid action", "select-all-rows", false, false],
    ["a server action on a grid", "server", false, true],
    ["a silent server action on a grid", "server", true, false]
  ])("disables the buttons on %s: %s silent %s -> %s", function(_name, type, silent, disabled) {
    jest.spyOn($actionController, "disableButtons").mockImplementation(() => null);
    const action = {attr: name => ({type, silent})[name], run: () => new Promise(() => null)};

    $actionController.runAction(action);

    expect($actionController.disableButtons).toHaveBeenCalledTimes(disabled ? 1 : 0);
  });

  it("treats as grid actions only the client actions of the grid that no other component shares", function() {
    expect($actionController.isGridAction("unselect-all-rows")).toBe(true);
    expect($actionController.isGridAction("tree-branch")).toBe(true);
    expect($actionController.isGridAction("save-row")).toBe(true);
    expect($actionController.isGridAction("filter")).toBe(false);
    expect($actionController.isGridAction("server")).toBe(false);
    expect($actionController.isGridAction("reset")).toBe(false);
  });

  it("manages null data on action list", function() {
    // Mock
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
