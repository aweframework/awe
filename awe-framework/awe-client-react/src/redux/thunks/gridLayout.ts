import type { ComponentAddress } from "../../contract";
import { fetchAction, isFetchError } from "../../utilities";
import { addActionsTop } from "../actions/actions";
import type { AppThunk } from "../types";
import type { ClientAction } from "./types";
import { runRefusal } from "./refusal";

/** Message shown when a layout service fails */
interface LayoutErrorMessage {
  title?: string;
  message?: string;
}

/** Layout of a grid: the order of its columns and the hidden ones */
interface GridLayout {
  order: string[];
  hidden: string[];
}

// Messages of the server which tell that the service did not work: a warning too, as in the AngularJS engine
const FAILURE_MESSAGE_TYPES: unknown[] = ["error", "danger", "warning"];

const isFailureMessage = (action: ClientAction): boolean =>
  action.type === "message" && FAILURE_MESSAGE_TYPES.includes(action.parameters?.type);

/**
 * Call a grid layout service. The user is never sent: the server takes it from the session.
 * The thunk resolves to true when the service worked. It uses maintain-silent: no message when it works (only the
 * warnings and errors of the server), so saving and restoring feel immediate. A warning or an error of the server is a
 * failure. When the call fails the user gets an error message.
 *
 * @param {string} target Maintain target of the service
 * @param {object} address Address of the grid
 * @param {object} parameters Service parameters
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
const launchLayoutService = (
  target: string,
  address: ComponentAddress,
  parameters: Record<string, unknown>,
  errorMessage: LayoutErrorMessage = {}
): AppThunk<Promise<boolean>> => async (dispatch, getState) => {
  const { settings } = getState();
  try {
    // An answer that failed is a refusal or a failure (#935): the catch runs what its status demands
    const response = await fetchAction("maintain-silent", target, parameters, settings.token, null) as ClientAction[];
    if (isFetchError(response)) {
      throw response;
    }
    dispatch(addActionsTop(response.map(action => ({ ...action, address: { ...address, ...(action.address || {}) } }))));
    // A warning or an error of the server is already shown by its own message
    return !response.some(isFailureMessage);
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error(`Error in the grid layout service ${target}:`, error);
    // The refusal of the server (an expired session, denied access) answers with its own actions: the panel on a
    // 401, the access-denied message on any other 403 and the not-found message on a 404 (#935, S6)
    if (runRefusal(error, dispatch)) {
      return false;
    }
    dispatch(addActionsTop([{
      type: "message",
      address,
      parameters: { type: "error", title: errorMessage.title, message: errorMessage.message }
    }]));
    return false;
  }
};

/**
 * Save the layout of a grid as the default of the user
 *
 * @param {object} address Address of the grid
 * @param {string} screenId Screen that holds the grid
 * @param {{order: string[], hidden: string[]}} layout Layout to save
 */
export const saveGridLayout = (
  address: ComponentAddress,
  screenId: string,
  layout: GridLayout,
  errorMessage?: LayoutErrorMessage
): AppThunk<Promise<boolean>> => launchLayoutService("saveGridLayout", address, {
  gridLayoutScreen: screenId,
  gridLayoutGrid: address.component,
  gridLayoutJson: JSON.stringify(layout)
}, errorMessage);

/**
 * Remove the layout saved by the user for a grid
 *
 * @param {object} address Address of the grid
 * @param {string} screenId Screen that holds the grid
 */
export const resetGridLayout = (
  address: ComponentAddress,
  screenId: string,
  errorMessage?: LayoutErrorMessage
): AppThunk<Promise<boolean>> => launchLayoutService("resetGridLayout", address, {
  gridLayoutScreen: screenId,
  gridLayoutGrid: address.component
}, errorMessage);
