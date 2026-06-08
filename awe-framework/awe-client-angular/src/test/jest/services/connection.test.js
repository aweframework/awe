import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe("Connection", () => {
  let $connection;
  let ajax;
  let comet;

  beforeEach(() => {
    ajax = {
      init: jest.fn(() => "ajax-init"),
      sendMessage: jest.fn(() => "message-sent"),
      send: jest.fn(() => "send-result"),
      get: jest.fn(() => "get-result"),
      post: jest.fn(() => "post-result"),
      getFile: jest.fn(() => "file-result"),
      getRawUrl: jest.fn(() => "/awe"),
      getActionUrl: jest.fn(() => "/awe/action/data/load"),
      serializeParameters: jest.fn(parameters => ({serialized: parameters}))
    };
    comet = {
      init: jest.fn(() => "comet-init"),
      restart: jest.fn(() => "restarted"),
      disconnect: jest.fn(() => "disconnected"),
      subscribe: jest.fn(() => "subscribed")
    };

    angular.mock.module("aweApplication", {
      Ajax: ajax,
      Comet: comet
    });

    inject(["$injector", ($injector) => {
      $connection = $injector.get("Connection");
    }]);
  });

  it("initializes Ajax first and delegates connection lifecycle to Comet", () => {
    expect($connection.init("/awe", true, "token-1")).toBe("comet-init");
    expect(ajax.init).toHaveBeenCalledWith("/awe", true);
    expect(comet.init).toHaveBeenCalledWith(true, "token-1");

    expect($connection.restart("token-2")).toBe("restarted");
    expect($connection.disconnect()).toBe("disconnected");
    $connection.subscribe("topic-token");

    expect(comet.restart).toHaveBeenCalledWith("token-2");
    expect(comet.disconnect).toHaveBeenCalled();
    expect(comet.subscribe).toHaveBeenCalledWith("topic-token");
  });

  it("delegates message, request, URL, and serialization methods to Ajax", () => {
    const message = {values: {serverAction: "data"}};
    const parameters = {CrtStatus: "ACTIVE"};

    $connection.sendMessage(message);

    expect(ajax.sendMessage).toHaveBeenCalledWith(message);
    expect($connection.send(message)).toBe("send-result");
    expect($connection.get("/awe/template", "text/html")).toBe("get-result");
    expect($connection.post("/awe/screen-data", parameters, "application/json")).toBe("post-result");
    expect($connection.getFile("/awe/file", parameters, "application/pdf", "blob")).toBe("file-result");
    expect($connection.getRawUrl()).toBe("/awe");
    expect($connection.getActionUrl("data", "load")).toBe("/awe/action/data/load");
    expect($connection.serializeParameters(parameters)).toEqual({serialized: parameters});
  });

  it("delegates sendMessage to Ajax", () => {
    const message = {values: {serverAction: "data"}};

    $connection.sendMessage(message);
    expect(ajax.sendMessage).toHaveBeenCalledWith(message);
  });

  it("delegates raw send requests to Ajax", () => {
    const message = {values: {serverAction: "maintain"}};

    expect($connection.send(message)).toBe("send-result");
    expect(ajax.send).toHaveBeenCalledWith(message);
  });

  it("delegates GET and POST requests with expected content types", () => {
    const parameters = {CrtStatus: "ACTIVE"};

    expect($connection.get("/awe/template", "text/html")).toBe("get-result");
    expect($connection.post("/awe/screen-data", parameters, "application/json")).toBe("post-result");
    expect(ajax.get).toHaveBeenCalledWith("/awe/template", "text/html");
    expect(ajax.post).toHaveBeenCalledWith("/awe/screen-data", parameters, "application/json");
  });

  it("delegates file, URL, and serialization helpers to Ajax", () => {
    const parameters = {id: 1};

    expect($connection.getFile("/awe/file", parameters, "application/pdf", "blob")).toBe("file-result");
    expect($connection.getRawUrl()).toBe("/awe");
    expect($connection.getActionUrl("data", "load")).toBe("/awe/action/data/load");
    expect($connection.serializeParameters(parameters)).toEqual({serialized: parameters});
  });
});
