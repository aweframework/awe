import {useDispatch} from "react-redux";
import {acceptAction} from "../redux/actions/actions";
import type {ActionItem} from "../redux/actions/actions";
import {updateSettings} from "../redux/actions/settings";
import type {AppDispatch} from "../redux/types";
import type {ClientActionHandler} from "../utilities/actionRegistry";
import type {ClientAction} from "../contract";

/**
 * Session service: the state of the session and its expiry (#935, S5)
 * An expired session is answered with the `session-expired` client action, which marks the settings so the app shell
 * renders the blocking session-expired panel over them
 */
const useSessionService = () => {
  const dispatch: AppDispatch = useDispatch();

  /**
   * The session has expired: mark the settings (the panel is rendered from the app shell) and accept the action
   * @param {object} action Action received
   */
  const sessionExpired = (action: ClientAction): void => {
    dispatch(updateSettings({sessionExpired: true}));
    dispatch(acceptAction(action as ActionItem));
  };

  const getActions = (): Record<string, ClientActionHandler> => ({
    "session-expired": sessionExpired
  });
  return {getActions};
};

export default useSessionService;
