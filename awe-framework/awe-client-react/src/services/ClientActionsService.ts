import {useMemo} from "react";
import useFormService from "./FormService";
import useMessageService from "./MessageService";
import useComponentService from "./ComponentService";
import useGridService from "./components/GridService";
import useWebsocketService from "./WebsocketService";
import useScreenService from "./ScreenService";
import {buildActionRegistry} from "../utilities/actionRegistry";
import type {ClientActionHandler} from "../utilities/actionRegistry";

/**
 * Client actions service: the table of every client action, built from the services that own them.
 * Registering the same action name in two services fails.
 * @returns {Object<string, function>} Client actions by action name
 * @category Services
 */
export default function useClientActions(): Record<string, ClientActionHandler> {
  const formService = useFormService();
  const screenService = useScreenService();
  const messageService = useMessageService();
  const componentService = useComponentService();
  const gridService = useGridService();
  const websocketService = useWebsocketService();

  return useMemo(
    () => buildActionRegistry({
      form: formService,
      screen: screenService,
      message: messageService,
      component: componentService,
      grid: gridService,
      websocket: websocketService
    }),
    [formService, screenService, messageService, componentService, gridService, websocketService]
  );
}
