import { fetchAction } from "../../utilities";
import { addActionsTop } from "../actions/actions";

const isErrorMessage = action => action.type === "message" && ["error", "danger"].includes(action.parameters?.type);

/**
 * Call a grid layout service. The user is never sent: the server takes it from the session.
 * The thunk resolves to true when the service worked. When the call fails the user gets an error message.
 *
 * @param {string} target Maintain target of the service
 * @param {object} address Address of the grid
 * @param {object} parameters Service parameters
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
const launchLayoutService = (target, address, parameters, errorMessage = {}) => async (dispatch, getState) => {
  const { settings } = getState();
  try {
    const response = await fetchAction("maintain", target, parameters, settings.token, null);
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
export const saveGridLayout = (address, screenId, layout, errorMessage) => launchLayoutService("saveGridLayout", address, {
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
export const resetGridLayout = (address, screenId, errorMessage) => launchLayoutService("resetGridLayout", address, {
  gridLayoutScreen: screenId,
  gridLayoutGrid: address.component
}, errorMessage);
