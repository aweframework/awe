import ServerService from "../../../src/services/WebsocketService";
import {Server} from "mock-socket";

describe('awe-react-client/test/js/services/WebsocketServiceTest.jsx', function () {
  const service = new ServerService();
  let props;

  const base = document.createElement("base");
  const websocketServer = new Server('ws://localhost:5000');

  beforeAll(function () {
    base.href = "http://localhost:5000/"
    document.head.appendChild(base);
  });

  beforeEach(function () {
    props = {updateSettings:jasmine.createSpy('updateSettings'), acceptAction: jasmine.createSpy('accept'), settings: {}};

    websocketServer.on('connection', (socket) => {
      socket.on('message', (message) => {
        console.log('Received a message from the client', message);
      });
      socket.send('Sending a message to the client');
    });
  })

  afterEach(function() {
    websocketServer.close();
  })

  afterAll(function() {
    websocketServer.stop();
    document.head.removeChild(base);
  })

  it('should connect a websocket', function() {
    let actions = service.connectWebsocket(null, props);
    expect(Object.keys(actions).length).toBe(26);
  });

});
