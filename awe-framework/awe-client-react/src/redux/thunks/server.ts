import type { ComponentAddress } from "../../contract";
import { fetchAction, fetchFile, getActionAddress, getComponent, getRestUrl } from "../../utilities";
import type { ComponentState } from "../../utilities/mergeUtils";
import { acceptAction, addActionsTop } from "../actions/actions";
import { updateAttributes } from "../actions/components";
import { getFormValues } from "../selectors/form";
import { getAllComponents } from "../selectors/componentSelectors";
import type { AppThunk } from "../types";
import type { ClientAction, Translate } from "./types";

/** An action that launches a server action: its parameters name the server action and its target */
interface ServerClientAction extends ClientAction {
  parameters: { serverAction: string; targetAction: string; [key: string]: unknown };
  errorAttributes?: Record<string, unknown>;
}

let downloadFormIdentifier = 0;

export const serverAction = (
  action: ServerClientAction,
  forPrinting = false,
  t: Translate = (o) => o
): AppThunk<Promise<boolean>> => {
  return async (dispatch, getState) => {
    const { settings } = getState();
    const { parameters, target, address } = action;
    const component = address?.component;
    const { serverAction, targetAction } = parameters;
    // The target of the action loads until its response (fill, end-load...) arrives, as in the AngularJS client
    const targetAddress = target ? { ...address, component: target } : null;
    if (targetAddress) {
      dispatch(updateAttributes(targetAddress, { loading: true }));
    }

    try {
      // An error answer has no list of actions: reading it as one fails and ends in the catch, as it always did
      const response = await fetchAction<ClientAction[]>(
        serverAction,
        targetAction,
        // Form values win over the action parameters (as in the AngularJS client): the action always carries
        // null target, value and label, which would otherwise wipe a component or column with that name. The
        // components of a grid row detail instance also send the values of their instance
        { ...parameters, ...getFormValues(getState(), forPrinting, t, address) },
        settings.token,
        null
      ) as ClientAction[];

      // Añadir nuevas acciones a la pila
      dispatch(addActionsTop(
        response.map(a => ({
          ...a,
          address: { ...address, component: target || component, ...(a.address || {}) }
        }))
      ));

      // Aceptar la acción original
      dispatch(acceptAction(action));
      return true;
    } catch (error) {
      console.error("Error in server action call:", error);

      // No response will end the load of the target
      if (targetAddress) {
        dispatch(updateAttributes(targetAddress, { loading: false }));
      }

      // The action can ask to set some attributes on its component when the call fails (i.e. to try again)
      if (action.errorAttributes) {
        // The action that fails always comes from a component, so it has its address
        dispatch(updateAttributes(address as ComponentAddress, action.errorAttributes));
      }
      return false;
    }
  };
};

export function serverDownloadAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { settings } = getState();
    const address = getActionAddress(action);
    // As it always did, a download of a component that is not there fails reading its attributes
    const component = getComponent(components, address) as ComponentState;
    const { specificAttributes } = component;

    const parameters = {
      ...action.parameters,
      ...getFormValues(getState(), false, undefined, address),
      ...(specificAttributes || {}),
    };

    const targetAction = parameters[settings.targetActionKey];
    fetchFile(
      getRestUrl("file", "download", "maintain", targetAction),
      { ...parameters, d: downloadFormIdentifier++ },
      settings.token
    ).then(() => dispatch(acceptAction(action)))
      .catch((reason) => console.error("Error downloading file:", reason));
  };
}