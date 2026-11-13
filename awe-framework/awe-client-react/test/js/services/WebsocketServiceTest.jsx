import useWebsocketService from "../../../src/services/WebsocketService";
import {Server} from "mock-socket";
import {renderWithProviders} from "../test-utils";
import React from "react";
import {act} from "@testing-library/react";

describe('awe-react-client/test/js/services/WebsocketServiceTest.jsx', function () {
  let props;
  let actions;
  let service;
  let store;

  const base = document.createElement("base");
  const websocketServer = new Server('ws://localhost:5000');

  function TestHarness() {
    service = useWebsocketService();
    actions = service.getActions();
    return null;
  }

  beforeAll(function () {
    base.href = "http://localhost:5000/";
    document.head.appendChild(base);
  });

  beforeEach(function () {
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    props = {
      updateSettings: jest.fn(),
      acceptAction: jest.fn(),
      settings: {}};

    const rendered = renderWithProviders(<TestHarness />, { spyDispatch: true });
    store = rendered.store;
    actions = service.getActions();

    websocketServer.on('connection', (socket) => {
      socket.on('message', (message) => {
        console.log('Received a message from the client', message);
      });
      socket.send('Sending a message to the client');
    });
  });

  afterEach(function() {
    websocketServer.close();
  });

  afterAll(function() {
    websocketServer.stop();
    document.head.removeChild(base);
  });

  afterEach(function () {
    jest.restoreAllMocks();
  });

  it('should connect a websocket', function() {
    act(() => {
      actions.connectWebsocket({}, props);
    });
    expect(store.dispatch).toHaveBeenCalled();
  });

});
