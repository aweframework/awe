import {Client} from "@stomp/stompjs";
import {connectWebSocketAction, disconnectWebSocketAction} from "../../../../src/redux/thunks/websocket";

jest.mock("@stomp/stompjs", () => ({
  Client: jest.fn().mockImplementation(function Client() {
    this.activate = jest.fn();
    this.subscribe = jest.fn();
    this.deactivate = jest.fn();
  })
}));

describe("awe-react-client/test/js/redux/thunks/websocketThunkTest.js", () => {
  let warn;

  const connect = () => connectWebSocketAction({parameters: {token: "token"}}, text => text)(jest.fn(), () => ({settings: {}}));

  beforeEach(() => {
    warn = jest.spyOn(console, "warn").mockImplementation(() => null);
  });

  afterEach(() => {
    warn.mockRestore();
  });

  it.each([1000, 1005, 1006])("should close quietly with code %s", (code) => {
    connect().onWebSocketClose({code});

    expect(warn).not.toHaveBeenCalled();
  });

  it("should warn when the server disconnects", () => {
    connect().onWebSocketClose({code: 1001});

    expect(warn).toHaveBeenCalledWith("Server disconnected, trying to reconnect", {code: 1001});
  });

  it("should warn about any other close code", () => {
    connect().onWebSocketClose({code: 1011});

    expect(warn).toHaveBeenCalledWith("WebSocket closed", {code: 1011});
  });

  describe("connection", () => {
    let error;

    beforeEach(() => {
      error = jest.spyOn(console, "error").mockImplementation(() => null);
    });

    afterEach(() => {
      error.mockRestore();
    });

    it("should accept the action, activate the client and return it", () => {
      const dispatch = jest.fn();
      const action = {type: "connectWebsocket", parameters: {token: "token"}};

      const client = connectWebSocketAction(action, text => text)(dispatch, () => ({settings: {}}));

      expect(dispatch).toHaveBeenCalledWith({type: "ACCEPT_ACTION", payload: action});
      expect(client.activate).toHaveBeenCalledTimes(1);
      expect(Client).toHaveBeenLastCalledWith(expect.objectContaining({
        connectHeaders: expect.objectContaining({Authorization: "token"}),
        reconnectDelay: 5000
      }));
    });

    it("should take the token from the settings when the action has no parameters", () => {
      connectWebSocketAction({type: "connectWebsocket"}, text => text)(jest.fn(), () => ({settings: {token: "settings-token"}}));

      expect(Client).toHaveBeenLastCalledWith(expect.objectContaining({
        connectHeaders: expect.objectContaining({Authorization: "settings-token"})
      }));
    });

    it("should subscribe to the broadcast and the token topics and dispatch what they send", () => {
      const dispatch = jest.fn();
      const client = connectWebSocketAction({parameters: {token: "token"}}, text => text)(dispatch, () => ({settings: {}}));
      dispatch.mockClear();

      client.onConnect();
      const [[broadcast, onBroadcast], [own, onOwn]] = client.subscribe.mock.calls;
      onBroadcast({body: JSON.stringify([{type: "message"}])});
      onOwn({body: JSON.stringify([{type: "screen"}])});

      expect([broadcast, own]).toEqual(["/topic/broadcast", "/topic/token"]);
      expect(dispatch).toHaveBeenNthCalledWith(1, {type: "ADD_ACTIONS_TOP", actions: [{type: "message"}], payload: [{type: "message"}]});
      expect(dispatch).toHaveBeenNthCalledWith(2, {type: "ADD_ACTIONS_TOP", actions: [{type: "screen"}], payload: [{type: "screen"}]});
    });

    it("should go back to the sign in screen when the session is disconnected", () => {
      const dispatch = jest.fn();
      const client = connectWebSocketAction({parameters: {token: "token"}}, text => text)(dispatch, () => ({settings: {}}));
      dispatch.mockClear();

      client.onWebSocketClose({code: 1008});

      expect(warn).toHaveBeenCalledWith("Session disconnected. Returning to signin screen", {code: 1008});
      const [{payload}] = dispatch.mock.calls[0];
      expect(payload).toEqual([
        expect.objectContaining({type: "message", async: true, silent: true}),
        {type: "screen", target: "/", force: true, parameters: {}}
      ]);
    });

    it("should log a stomp error", () => {
      const client = connect();

      client.onStompError({headers: {message: "denied"}});

      expect(error).toHaveBeenCalledWith("WebSocket error", {headers: {message: "denied"}});
    });
  });

  describe("disconnection", () => {
    it("should accept the action, deactivate the client and return nothing", () => {
      const dispatch = jest.fn();
      const client = {deactivate: jest.fn()};
      const action = {type: "disconnectWebsocket"};

      const result = disconnectWebSocketAction(action, client)(dispatch);

      expect(dispatch).toHaveBeenCalledWith({type: "ACCEPT_ACTION", payload: action});
      expect(client.deactivate).toHaveBeenCalledTimes(1);
      expect(result).toBeNull();
    });

    it("should only accept the action when there is no client", () => {
      const dispatch = jest.fn();

      const result = disconnectWebSocketAction({type: "disconnectWebsocket"}, undefined)(dispatch);

      expect(dispatch).toHaveBeenCalledTimes(1);
      expect(result).toBeUndefined();
    });
  });
});
