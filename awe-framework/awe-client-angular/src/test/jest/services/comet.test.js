import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe("Comet", () => {
  let $comet;
  let $utilities;
  let $actionController;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $comet = $injector.get("Comet");
      $utilities = $injector.get("AweUtilities");
      $actionController = $injector.get("ActionController");
    }]);
  });

  it("connects through an injected STOMP client boundary and subscribes to broadcast and token topics", () => {
    const client = {activate: jest.fn()};
    jest.spyOn($comet, "getWebsocketClient").mockReturnValue(client);
    jest.spyOn($comet, "subscribe").mockImplementation(jest.fn());

    const promise = $comet.init(true, "token-1");
    client.onConnect({});

    expect($comet.getConnection()).toBe(client);
    expect(client.activate).toHaveBeenCalled();
    expect($comet.subscribe).toHaveBeenCalledWith("broadcast");
    expect($comet.subscribe).toHaveBeenCalledWith("token-1");
    expect(promise).toBeDefined();
  });

  it("disconnects or reconnects using the active connection without activating a real websocket", () => {
    const activeConnection = {active: true, deactivate: jest.fn(() => ({then: callback => callback()}))};
    jest.spyOn($comet, "getConnection").mockReturnValue(activeConnection);
    jest.spyOn($comet, "_connect").mockReturnValue("connected");

    expect($comet.disconnect()).toBe(activeConnection.deactivate.mock.results[0].value);
    $comet._reconnect();

    expect(activeConnection.deactivate).toHaveBeenCalledTimes(2);
    expect($comet._connect).toHaveBeenCalled();
  });

  it("decodes broadcast actions and only queues action arrays", () => {
    const actions = [{type: "message", target: {view: "center"}}];
    jest.spyOn($utilities, "decodeData").mockImplementation(body => body === "encoded-actions" ? actions : {type: "not-array"});
    jest.spyOn($actionController, "addActionList");

    $comet.manageBroadcast({body: "encoded-actions"});
    $comet.manageBroadcast({body: "encoded-object"});

    expect($actionController.addActionList).toHaveBeenCalledTimes(1);
    expect($actionController.addActionList).toHaveBeenCalledWith(actions, true, {address: undefined, context: ""});
  });

  it("sets and returns the current websocket connection", () => {
    const connection = {active: false};

    $comet.setConnection(connection);

    expect($comet.getConnection()).toBe(connection);
    expect($comet.isConnected()).toBe(false);
  });

  it("reports a connected state only for active connections", () => {
    $comet.setConnection({active: true});

    expect($comet.isConnected()).toBe(true);

    $comet.setConnection({active: false});
    expect($comet.isConnected()).toBe(false);
  });

  it("returns a resolved promise when disconnecting without an active connection", () => {
    const resolved = {then: jest.fn()};
    $utilities.q = {resolve: jest.fn(() => resolved)};
    $comet.setConnection(null);

    expect($comet.disconnect()).toBe(resolved);
    expect($utilities.q.resolve).toHaveBeenCalled();
  });

  it("subscribes a token topic on the active connection", () => {
    const connection = {active: true, subscribe: jest.fn()};
    $comet.setConnection(connection);

    $comet.subscribe("user-token");

    expect(connection.subscribe).toHaveBeenCalledWith("/topic/user-token", $comet.manageBroadcast);
  });

  it("reconnects by connecting immediately when there is no active connection", () => {
    $comet.setConnection({active: false});
    jest.spyOn($comet, "_connect").mockReturnValue("connected");

    expect($comet._reconnect()).toBe("connected");
    expect($comet._connect).toHaveBeenCalled();
  });

  it("restarts by storing the token and reconnecting", () => {
    jest.spyOn($comet, "_reconnect").mockReturnValue("reconnected");

    $comet.restart("new-token");

    expect($comet._reconnect).toHaveBeenCalled();
  });

  it("queues a session-closed action list when websocket closes with policy violation", () => {
    const client = {active: true, activate: jest.fn()};
    jest.spyOn($comet, "getWebsocketClient").mockReturnValue(client);
    jest.spyOn($actionController, "addActionList");
    jest.spyOn(console, "warn").mockImplementation(() => null);

    $comet.init(false, "token-1");
    client.onWebSocketClose({code: 1008});

    expect($actionController.addActionList).toHaveBeenCalledWith([
      {type: "screen", async: true, silent: true},
      {
        type: "message",
        async: true,
        silent: true,
        parameters: {
          type: "warn",
          title: "ERROR_TITLE_SESSION_CLOSED",
          message: "ERROR_MESSAGE_SESSION_CLOSED"
        }
      }
    ], true, {});
  });

  it("rejects the connection promise and logs broker details on stomp errors", () => {
    const client = {activate: jest.fn()};
    jest.spyOn($comet, "getWebsocketClient").mockReturnValue(client);
    jest.spyOn(console, "log").mockImplementation(() => null);

    const promise = $comet.init(false, "token-1");
    client.onStompError({headers: {message: "denied"}, body: "details"});

    expect(promise).toBeDefined();
    expect(console.log).toHaveBeenCalledWith("Broker reported error: denied");
    expect(console.log).toHaveBeenCalledWith("Additional details: details");
  });
});
