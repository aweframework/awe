import {Client} from "@stomp/stompjs";
import {acceptAction, addActionsTop} from "../actions/actions";
import {getContextPath, getCookie, translateLabel} from "../../utilities";
import {MessageType} from "../actions/messages";

const {MESSAGE_WARNING} = MessageType;

const onMessage = (response, dispatch) => {
  dispatch(addActionsTop(JSON.parse(response.body))); // Despachar la acción para manejar el mensaje
};

// Manejar la desconexión del WebSocket
const handleWebSocketClose = (evn, t, dispatch, client) => {
  switch (evn.code) {
    case 1008:
      if (client) {
        console.warn("Session disconnected. Returning to signin screen", evn);
        dispatch(addActionsTop([
          {
            type: "message",
            async: true,
            silent: true,
            parameters: {
              type: MESSAGE_WARNING,
              title: translateLabel("ERROR_TITLE_SESSION_EXPIRED", t),
              message: translateLabel("ERROR_MESSAGE_SESSION_EXPIRED", t)
            }
          },
          {type: "screen", target: "", force: true, parameters: {}},
        ]));
      }
      break;
    case 1001:
      console.warn("Server disconnected, trying to reconnect", evn);
      break;
    case 1006:
      break;
    default:
      console.info("Graceful WebSocket disconnection", evn);
  }
};

/**
 * Connect to websocket
 * @param {Action} action Action received
 * @param {Object} t Translate function
 */
export const connectWebSocketAction = (action, t) => {
  return (dispatch, getState) => {
    const {settings} = getState();

    let client;
    if (action) {
      dispatch(acceptAction(action));
    }

    const {token} = settings;

    // Crear un nuevo cliente WebSocket
    client = new Client({
      brokerURL: `${location.origin.replace("http", "ws")}${getContextPath()}/websocket`,
      connectHeaders: {
        'Authorization': token,
        "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Configurar el comportamiento al conectar
    client.onConnect = (evn) => {
      client.subscribe('/topic/broadcast', (m) => onMessage(m, dispatch));
      client.subscribe(`/topic/${token}`, (m) => onMessage(m, dispatch));
      console.info("WebSocket connected", evn);
    };

    // Configurar el comportamiento cuando el WebSocket se cierre
    client.onWebSocketClose = (evn) => {
      handleWebSocketClose(evn, t, dispatch, client);
    };

    client.onStompError = (evn) => {
      console.log('WebSocket error', evn);
    };

    // Activar la conexión
    client.activate();

    // Devolver el cliente
    return client;
  };
};

/**
 * Disconnect from websocket
 * @param {Action} action Action received
 * @param {Object} client Websocket client
 */
export const disconnectWebSocketAction = (action, client) => {
  return (dispatch) => {
    if (action) {
      dispatch(acceptAction(action));
    }

    if (client) {
      client.deactivate();
      client = null;
    }
  };
};

