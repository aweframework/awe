import type { ComponentAddress } from "../../contract";
import { fetchAction, isFetchError } from "../../utilities";
import { addActionsTop } from "../actions/actions";
import type { AppThunk } from "../types";
import type { ClientAction } from "./types";
import { runRefusal } from "./refusal";

// Messages of the server which tell that the save did not work
const FAILURE_MESSAGE_TYPES: unknown[] = ["error", "danger", "warning"];

const isFailureMessage = (action: ClientAction): boolean =>
  action.type === "message" && FAILURE_MESSAGE_TYPES.includes(action.parameters?.type);

/**
 * Check whether the answer of a service of a window tells that it worked: it reached the server and has no warning or
 * error message
 * @param {object[]|null} response Answer of the service (null when it did not reach the server)
 * @return {boolean} The service worked
 */
export const isWindowServiceDone = (response: ClientAction[] | null): boolean =>
  response !== null && !response.some(isFailureMessage);

/** Message shown when the save cannot reach the server */
export interface AdvancedStateErrorMessage {
  title?: string;
  message?: string;
}

/**
 * Call a service of a window (the advanced search, #790). The user is never sent: the server takes it from the session.
 * As the grid layout does, the answer of the server is launched (so its warnings and errors are shown; the data of a
 * query is not an action) and a call that fails shows an error message. The thunk resolves to the answer of the
 * server, or null when the call failed.
 *
 * @param {string} target Target of the service
 * @param {object} address Address of the window
 * @param {object} parameters Parameters of the service
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 * @param {string} [serverAction] Server action: maintain-silent (no message when it works) or data (a query)
 */
export const launchWindowService = (
  target: string,
  address: ComponentAddress,
  parameters: Record<string, unknown>,
  errorMessage: AdvancedStateErrorMessage = {},
  serverAction = "maintain-silent"
): AppThunk<Promise<ClientAction[] | null>> => async (dispatch, getState) => {
  const { settings } = getState();
  try {
    // An answer that failed is a refusal or a failure (#935): the catch runs what its status demands
    const response = await fetchAction(serverAction, target, parameters, settings.token, null) as ClientAction[];
    if (isFetchError(response)) {
      throw response;
    }
    dispatch(addActionsTop(response
      .filter(action => action.type !== "fill")
      .map(action => ({ ...action, address: { ...address, ...(action.address || {}) } }))));
    return response;
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error(`Error in the service ${target} of the window ${address.component}:`, error);
    // The refusal of the server (an expired session, denied access) answers with its own actions: the panel on a
    // 401, the client actions of the 403 body, the access-denied message on any other 403 and the not-found
    // message on a 404 (#935, S6)
    if (runRefusal(error, dispatch)) {
      return null;
    }
    dispatch(addActionsTop([{
      type: "message",
      address,
      parameters: { type: "error", title: errorMessage.title, message: errorMessage.message }
    }]));
    return null;
  }
};

/**
 * Save whether the advanced block of a window is open, as the state of the user (#790). The user is never sent: the
 * server takes it from the session.
 * As the grid layout does, the answer of the server is launched (so its warnings and errors are shown) and a call that
 * fails shows an error message. The thunk resolves to true when the save worked; the open state never changes.
 *
 * @param {object} address Address of the window
 * @param {string} screenId Screen that holds the window
 * @param {boolean} open Whether the block is open
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
export const saveWindowAdvancedState = (
  address: ComponentAddress,
  screenId: string,
  open: boolean,
  errorMessage: AdvancedStateErrorMessage = {}
): AppThunk<Promise<boolean>> => async (dispatch) => {
  const response = await dispatch(launchWindowService("saveWindowAdvancedState", address, {
    advancedStateScreen: screenId,
    advancedStateWindow: address.component,
    advancedStateOpen: open ? "1" : "0"
  }, errorMessage));
  // A warning or an error of the server is already shown by its own message
  return isWindowServiceDone(response);
};
