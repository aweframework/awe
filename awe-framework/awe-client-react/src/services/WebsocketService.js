import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {connectWebSocketAction, disconnectWebSocketAction} from "../redux/thunks/websocket";

const useWebsocketService = () => {

  // Referencia para la conexión WebSocket
  let client = null;
  const dispatch = useDispatch();
  const {t} = useTranslation();

  /**
   * Connect to websocket
   * @param {Action} action Action received
   */
  const connectWebSocket = (action) => {
    client = dispatch(connectWebSocketAction(action, t));
  };

  /**
   * Disconnect from websocket
   * @param {Action} action Action received
   */
  const disconnectWebSocket = (action) => {
    dispatch(disconnectWebSocketAction(action, client));
    client = null;
  };

  const getActions = () => ({
    "connectWebsocket": connectWebSocket,
    "disconnectWebsocket": disconnectWebSocket
  });

  return {getActions};
};

export default useWebsocketService;
