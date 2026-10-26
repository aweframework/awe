import type { ComponentAddress } from "../../contract";
import { fetchAction } from "../../utilities";
import { addActionsTop } from "../actions/actions";
import type { AppThunk } from "../types";
import type { ClientAction } from "./types";

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

const ERROR_MESSAGE_TYPES: unknown[] = ["error", "danger"];

const isErrorMessage = (action: ClientAction): boolean =>
  action.type === "message" && ERROR_MESSAGE_TYPES.includes(action.parameters?.type);

/**
 * Call a grid layout service. The user is never sent: the server takes it from the session.
 * The thunk resolves to true when the service worked. When the call fails the user gets an error message.
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
    // An error answer has no list of actions: reading it as one fails and ends in the catch, as it always did
    const response = await fetchAction("maintain", target, parameters, settings.token, null) as ClientAction[];
    dispatch(addActionsTop(response.map(action => ({ ...action, address: { ...address, ...(action.address || {}) } }))));
    // An error of the server is already shown by its own message
    return !response.some(isErrorMessage);
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error(`Error in the grid layout service ${target}:`, error);
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
