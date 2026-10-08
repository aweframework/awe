import {connectWebSocketAction} from "../../../../src/redux/thunks/websocket";

jest.mock("@stomp/stompjs", () => ({
  Client: jest.fn().mockImplementation(function Client() {
    this.activate = jest.fn();
    this.subscribe = jest.fn();
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
});
