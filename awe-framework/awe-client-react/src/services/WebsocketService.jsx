import {getContextPath, getCookie} from "../utilities";
import {Client} from "@stomp/stompjs";
import {MessageType} from "../redux/actions/messages";

const {MESSAGE_WARNING} = MessageType;

function onMessage(response, props) {
  props.addActionsTop(JSON.parse(response.body));
}

/**
 * Websocket service
 * @category Services
 */
class WebsocketService {

  /**
   * Create the service
   */
  constructor() {
    this.connection = null;
  }

  getActions() {
    return {
      "connectWebsocket": this.connectWebsocket.bind(this),
      "disconnectWebsocket": this.disconnectWebsocket.bind(this)
    }
  }

  /**
   * Connect to websocket
   */
  connectWebsocket(action, props) {

    // Accept action
    if (action) {
      props.acceptAction(action);
    }

    const {t} = props;
    const {token} = props.settings;
    const client = new Client({
      brokerURL: `${location.origin.replace("http", "ws")}${getContextPath()}/websocket`,
      connectHeaders: {
        'Authorization': token, "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = evn => {
      client.subscribe('/topic/broadcast', (m) => onMessage(m, props));
      client.subscribe(`/topic/${token}`, (m) => onMessage(m, props));
      console.info("Websocket connected successfully", evn);
    };

    client.onWebSocketClose = evn => {
      switch (evn.code) {
        // Session disconnected
        case 1008:
          if (this.connection != null) {
            console.warn("Session disconnected. Returning to signin screen", evn);
            props.addActionsTop([
              {
                type: "message",
                async: true,
                silent: true,
                parameters: {
                  type: MESSAGE_WARNING,
                  title: t("ERROR_TITLE_SESSION_EXPIRED"),
                  message: t("ERROR_MESSAGE_SESSION_EXPIRED")
                }
              },
              {type: "screen", target: "", force: true, parameters: {}},
            ]);
          }
          break;
        // Server disconnected
        case 1001:
          console.warn("Server disconnected, trying to reconnect", evn);
          break;
        // Server reconnection
        case 1006:
          break;
        // Graceful disconnection
        default:
          console.info("Graceful websocket disconnection", evn);
      }
    };

    client.onStompError = evn => {
      // Will be invoked in case of error encountered at Broker
      // Bad login/passcode typically will cause an error
      // Complaint brokers will set `message` header with a brief message. Body may contain details.
      // Compliant brokers will terminate the connection after any error
      console.log('Broker reported error: ' + evn.headers['message']);
      console.log('Additional details: ' + evn.body);
    };

    client.activate();

    this.connection = client;
    return client;
  }

  /**
   * Disconnect from websocket
   */
  disconnectWebsocket(action, props) {
    // Accept action
    if (action) {
      props.acceptAction(action);
    }

    if (this.connection != null) {
      this.connection.deactivate();
      this.connection = null;
    }
  }
}

export default WebsocketService;
