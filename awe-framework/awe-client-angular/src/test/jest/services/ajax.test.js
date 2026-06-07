import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe("Ajax", () => {
  let $ajax;
  let $actionController;
  let $utilities;
  let Action;
  let loadingBar;

  beforeEach(() => {
    loadingBar = {
      startTask: jest.fn(),
      endTask: jest.fn()
    };

    angular.mock.module("aweApplication", {
      LoadingBar: loadingBar
    });

    inject(["$injector", ($injector) => {
      $ajax = $injector.get("Ajax");
      $actionController = $injector.get("ActionController");
      $utilities = $injector.get("AweUtilities");
      Action = $injector.get("Action");

      jest.spyOn($utilities, "getContextPath").mockReturnValue("/awe");
      $ajax.init("/api", false);
    }]);
  });

  it("sends non-silent action messages, starts and ends loading, and enqueues returned actions", async () => {
    const action = new Action();
    action.attr("silent", false);
    action.attr("context", "center");
    const actions = [{type: "message", parameters: {message: "Saved"}}];
    jest.spyOn($ajax, "send").mockReturnValue(Promise.resolve({data: actions}));
    jest.spyOn($actionController, "addActionList");
    jest.spyOn($actionController, "acceptAction");

    await $ajax.sendMessage({
      target: {view: "center", component: "ButSave"},
      values: {serverAction: "data", targetAction: "save"},
      action
    });

    expect(action.attr("callbackTarget")).toEqual({view: "center", component: "ButSave"});
    expect($ajax.send).toHaveBeenCalledWith({
      target: {view: "center", component: "ButSave"},
      values: {serverAction: "data", targetAction: "save"},
      action
    });
    expect(loadingBar.startTask).toHaveBeenCalled();
    expect(loadingBar.endTask).toHaveBeenCalled();
    expect($actionController.acceptAction).toHaveBeenCalledWith(action);
    expect($actionController.addActionList).toHaveBeenCalledWith(actions, false, {
      address: {view: "center", component: "ButSave"},
      context: "center"
    });
  });

  it("keeps silent failed messages out of the loading start path and routes REST errors", async () => {
    const action = new Action();
    action.attr("silent", true);
    jest.spyOn($ajax, "send").mockReturnValue(Promise.reject({
      status: 403,
      data: {error: "Forbidden", message: "Forbidden access"}
    }));
    jest.spyOn($utilities, "manageRestError").mockReturnValue([{type: "message"}]);
    jest.spyOn($actionController, "closeAllActions");
    jest.spyOn($actionController, "addActionList");

    await $ajax.sendMessage({
      target: {view: "center"},
      values: {serverAction: "data", targetAction: "fail"},
      action
    });

    expect(loadingBar.startTask).not.toHaveBeenCalled();
    expect(loadingBar.endTask).toHaveBeenCalled();
    expect($actionController.closeAllActions).toHaveBeenCalled();
    expect($utilities.manageRestError).toHaveBeenCalledWith(
      {status: 403, title: "Forbidden", message: "Forbidden access"},
      {view: "center"}
    );
    expect($actionController.addActionList).toHaveBeenCalledWith([{type: "message"}], false, {address: {view: "base"}});
  });

  it("builds raw/action URLs and delegates request helpers with expected request parameters", () => {
    jest.spyOn($ajax, "httpRequest").mockImplementation((parameters, expectedContent) => ({parameters, expectedContent}));

    expect($ajax.getRawUrl()).toBe("/awe/api");
    expect($ajax.getActionUrl("data", "load")).toBe("/awe/api/action/data/load");
    expect($ajax.serializeParameters({CrtStatus: "ACTIVE"})).toEqual({CrtStatus: "ACTIVE"});

    expect($ajax.get("/awe/api/template", "text/html")).toEqual({
      parameters: {method: "GET", url: "/awe/api/template"},
      expectedContent: "text/html"
    });
    expect($ajax.post("/awe/api/report", {CrtStatus: "ACTIVE"}, "application/json")).toEqual({
      parameters: {method: "POST", url: "/awe/api/report", data: {CrtStatus: "ACTIVE"}},
      expectedContent: "application/json"
    });
    expect($ajax.getFile("/awe/api/file", {id: 1}, "application/pdf", "blob")).toEqual({
      parameters: {method: "POST", url: "/awe/api/file", data: {id: 1}, responseType: "blob"},
      expectedContent: "application/pdf"
    });
  });

  it("accepts alive actions and enqueues array response data with callback context", () => {
    const action = new Action();
    const responseActions = [{type: "screen"}];
    action.attr("silent", true);
    action.attr("async", true);
    action.attr("context", "base");
    action.attr("callbackTarget", {view: "base", component: "button"});
    jest.spyOn($utilities, "decodeData").mockReturnValue(responseActions);
    jest.spyOn($actionController, "acceptAction");
    jest.spyOn($actionController, "addActionList");

    $ajax.manageMessage({data: responseActions}, action);

    expect($utilities.decodeData).toHaveBeenCalledWith(responseActions, false);
    expect($actionController.acceptAction).toHaveBeenCalledWith(action);
    expect($actionController.addActionList).toHaveBeenCalledWith(responseActions, false, {
      address: {view: "base", component: "button"},
      context: "base"
    });
  });

  it("does not enqueue non-array response data or actions that are no longer alive", () => {
    const aliveAction = new Action();
    const deadAction = new Action();
    deadAction.destroy();
    jest.spyOn($utilities, "decodeData").mockImplementation(data => data);
    jest.spyOn($actionController, "acceptAction");
    jest.spyOn($actionController, "addActionList");

    $ajax.manageMessage({data: {type: "message"}}, aliveAction);
    $ajax.manageMessage({data: [{type: "message"}]}, deadAction);

    expect($actionController.acceptAction).toHaveBeenCalledWith(aliveAction);
    expect($actionController.acceptAction).not.toHaveBeenCalledWith(deadAction);
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it("cleans modal backdrops and maps missing REST error data to base-address actions", () => {
    document.body.innerHTML = '<div class="modal-backdrop"></div>';
    const action = new Action();
    action.attr("callbackTarget", {view: "base", component: "grid"});
    jest.spyOn($utilities, "manageRestError").mockReturnValue([{type: "message", parameters: {message: "Error"}}]);
    jest.spyOn($actionController, "closeAllActions");
    jest.spyOn($actionController, "addActionList");

    $ajax.manageError({}, action);

    expect(document.querySelector(".modal-backdrop")).toBeNull();
    expect($actionController.closeAllActions).toHaveBeenCalled();
    expect($utilities.manageRestError).toHaveBeenCalledWith(
      {status: undefined, title: undefined, message: undefined},
      {view: "base", component: "grid"}
    );
    expect($actionController.addActionList).toHaveBeenCalledWith(
      [{type: "message", parameters: {message: "Error"}}],
      false,
      {address: {view: "base"}}
    );
  });

  it("reports connection state and reinitializes the raw server URL", () => {
    expect($ajax.isConnected()).toBe(true);
    expect($ajax.getRawUrl()).toBe("/awe/api");

    $ajax.init("/rest", true);

    expect($ajax.isConnected()).toBe(true);
    expect($ajax.getRawUrl()).toBe("/awe/rest");
    expect($ajax.getActionUrl("data")).toBe("/awe/rest/action/data");
  });

  it("sends POST action messages through the action URL helper", () => {
    jest.spyOn($ajax, "httpRequest").mockReturnValue(Promise.resolve({data: []}));

    $ajax.send({values: {serverAction: "maintain", targetAction: "SaveRow", row: 1}});

    expect($ajax.httpRequest).toHaveBeenCalledWith({
      method: "POST",
      url: "/awe/api/action/maintain/SaveRow",
      data: {serverAction: "maintain", targetAction: "SaveRow", row: 1}
    });
  });

  it("preserves nested parameter objects during serialization", () => {
    const parameters = {criteria: {status: "ACTIVE"}, page: 1};

    expect($ajax.serializeParameters(parameters)).toBe(parameters);
    expect($ajax.serializeParameters(parameters)).toEqual({criteria: {status: "ACTIVE"}, page: 1});
  });

  it("passes request parameters to the angular HTTP boundary and returns its promise", () => {
    const request = {method: "GET", url: "/awe/api/status"};
    const promise = {then: jest.fn()};
    const $http = jest.fn().mockReturnValue(promise);
    inject(["$injector", ($injector) => {
      const ajax = $injector.instantiate($injector.get("Ajax").constructor || function() {});
      void ajax;
    }]);
    jest.spyOn($ajax, "httpRequest").mockImplementation(parameters => ({sent: parameters}));

    expect($ajax.get(request.url)).toEqual({sent: request});
    expect($http).not.toHaveBeenCalled();
  });

  it("builds an action URL without target when target action is empty", () => {
    expect($ajax.getActionUrl("data", "")).toBe("/awe/api/action/data");
    expect($ajax.getActionUrl("data", null)).toBe("/awe/api/action/data");
  });

  it("uses an empty callback target when sendMessage receives no target", async () => {
    const action = new Action();
    action.attr("silent", false);
    jest.spyOn($ajax, "send").mockReturnValue(Promise.resolve({data: []}));
    jest.spyOn($ajax, "manageMessage");

    await $ajax.sendMessage({values: {serverAction: "data"}, action});

    expect(action.attr("callbackTarget")).toEqual({});
    expect(loadingBar.startTask).toHaveBeenCalled();
    expect(loadingBar.endTask).toHaveBeenCalled();
    expect($ajax.manageMessage).toHaveBeenCalledTimes(1);
    expect($ajax.manageMessage.mock.calls[0][0].data).toHaveLength(0);
    expect($ajax.manageMessage.mock.calls[0][1]).toBe(action);
  });

  it("routes sendMessage rejections through manageError", async () => {
    const action = new Action();
    const error = {status: 500, data: {error: "Boom", message: "Failed"}};
    jest.spyOn($ajax, "send").mockReturnValue(Promise.reject(error));
    jest.spyOn($ajax, "manageError");

    await $ajax.sendMessage({target: {view: "center"}, values: {serverAction: "data"}, action});

    expect($ajax.manageError).toHaveBeenCalledWith(error, action);
    expect(loadingBar.endTask).toHaveBeenCalled();
  });

  it("decodes managed messages with the current transmission encoding flag", () => {
    const action = new Action();
    const responseActions = [{type: "message"}];
    $ajax.init("/api", true);
    jest.spyOn($utilities, "decodeData").mockReturnValue(responseActions);
    jest.spyOn($actionController, "acceptAction");
    jest.spyOn($actionController, "addActionList");

    $ajax.manageMessage({data: "encoded"}, action);

    expect($utilities.decodeData).toHaveBeenCalledWith("encoded", true);
    expect($actionController.addActionList).toHaveBeenCalledWith(responseActions, false, {address: undefined, context: undefined});
  });

  it("does not start the loading bar for silent sendMessage calls", async () => {
    const action = new Action();
    action.attr("silent", true);
    jest.spyOn($ajax, "send").mockReturnValue(Promise.resolve({data: []}));

    await $ajax.sendMessage({values: {serverAction: "data"}, action});

    expect(loadingBar.startTask).not.toHaveBeenCalled();
    expect(loadingBar.endTask).toHaveBeenCalled();
  });

  it("preserves existing silent and async flags on decoded message arrays", () => {
    const action = new Action();
    action.attr("silent", true);
    action.attr("async", true);
    const responseActions = [{type: "message", silent: false, async: false}];
    jest.spyOn($utilities, "decodeData").mockReturnValue(responseActions);
    jest.spyOn($actionController, "acceptAction");
    jest.spyOn($actionController, "addActionList");

    $ajax.manageMessage({data: responseActions}, action);

    expect(responseActions.silent).toBe(true);
    expect(responseActions.async).toBe(true);
    expect($actionController.acceptAction).toHaveBeenCalledWith(action);
  });

  it("passes REST error details and target address to the utilities boundary", () => {
    const action = new Action();
    action.attr("callbackTarget", {view: "dialog", component: "save"});
    jest.spyOn($utilities, "manageRestError").mockReturnValue([{type: "message"}]);
    jest.spyOn($actionController, "closeAllActions");
    jest.spyOn($actionController, "addActionList");

    $ajax.manageError({status: 401, data: {error: "Unauthorized", message: "Login"}}, action);

    expect($utilities.manageRestError).toHaveBeenCalledWith(
      {status: 401, title: "Unauthorized", message: "Login"},
      {view: "dialog", component: "save"}
    );
  });

});
