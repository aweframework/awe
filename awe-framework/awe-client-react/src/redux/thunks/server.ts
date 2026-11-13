import type { ComponentAddress } from "../../contract";
import type { FetchError } from "../../utilities";
import { fetchAction, fetchFile, getActionAddress, getComponent, getRestUrl, isFetchError } from "../../utilities";
import type { ComponentState } from "../../utilities/mergeUtils";
import { acceptAction, addActionsTop } from "../actions/actions";
import type { ActionItem } from "../actions/actions";
import { runRefusal } from "./refusal";
import { loginLanguageParameters, loginModeParameters } from "../../utilities/signin";
import { updateAttributes } from "../actions/components";
import { getFormValues } from "../selectors/form";
import { getAllComponents } from "../selectors/componentSelectors";
import type { AppThunk } from "../types";
import { selectButtonRow } from "./grid";
import type { ClientAction, Translate } from "./types";

/** An action that launches a server action: its parameters name the server action and its target */
interface ServerClientAction extends ClientAction {
  parameters: { serverAction: string; targetAction: string; [key: string]: unknown };
  errorAttributes?: Record<string, unknown>;
}

let downloadFormIdentifier = 0;

/**
 * Value and address of the button that calls a server action, as the AngularJS client sends them (the notification
 * rows read their notification from buttonValue)
 * @param caller State of the component that calls the action
 * @param address Address of the component that calls the action
 * @return Parameters buttonValue and buttonAddress, or nothing when the caller is not a button or has no value
 */
function getButtonValue(caller: ComponentState | null | undefined, address?: ComponentAddress): Record<string, unknown> {
  const { value, buttonType, elementType } = (caller?.attributes || {}) as { value?: unknown; buttonType?: string; elementType?: string };
  // Only buttons send it, as the AngularJS client: a button has a button type, and the copy of a tag-list row its element type
  const isButton = buttonType !== undefined || elementType === "Button" || elementType === "InfoButton";
  return !isButton || value === undefined || value === null ? {} : { buttonValue: value, buttonAddress: address };
}

export const serverAction = (
  action: ServerClientAction,
  forPrinting = false,
  t: Translate = (o) => o
): AppThunk<Promise<boolean>> => {
  return async (dispatch, getState) => {
    const { settings } = getState();
    const { parameters, target, address, origin } = action;
    const component = address?.component;
    const { serverAction, targetAction } = parameters;
    // The target of the action loads until its response (fill, end-load...) arrives, as in the AngularJS client
    const targetAddress = target ? { ...address, component: target } : null;
    if (targetAddress) {
      dispatch(updateAttributes(targetAddress, { loading: true }));
    }

    // An action called from a cell of a grid acts on its row: it is selected (only it) before the parameters are read.
    // A multiselect grid keeps the rows the user selected: the caller cell is sent in its address anyway
    const callerGrid = component ? getAllComponents(getState())[component] : null;
    if (!callerGrid?.attributes?.multiselect) {
      dispatch(selectButtonRow(address as ComponentAddress & { row?: unknown }));
    }

    // Every action of the answer runs in the context the caller gave this one: its component (the target when
    // the action loads one), its view and its origin. The answer of a silent action is silent too, as in the
    // AngularJS client: applying the answer of a background lookup never disables the buttons
    const withCallerContext = (clientAction: ActionItem): ActionItem => ({
      ...clientAction,
      address: { ...address, component: target || component, ...(clientAction.address || {}) },
      ...(origin ? { origin } : {}),
      ...(action.silent ? { silent: true } : {})
    } as ClientAction);

    try {
      // An answer that failed has no list of actions: a non-ok answer is a refusal or a failure (#935) and the
      // catch below runs what its status demands
      const response = await fetchAction<ClientAction[]>(
        serverAction,
        targetAction,
        // Form values win over the action parameters (as in the AngularJS client): the action always carries
        // null target, value and label, which would otherwise wipe a component or column with that name. The
        // components of a grid row detail instance also send the values of their instance
        // The login also sends the colour mode and the language chosen on the sign-in page, saved for the user
        { ...parameters, ...getButtonValue(callerGrid as ComponentState | null | undefined, address), ...getFormValues(getState(), forPrinting, t, address, origin === "menu" ? "base" : undefined), ...loginModeParameters(serverAction), ...loginLanguageParameters(serverAction) },
        settings.token,
        null
      ) as ClientAction[];
      if (isFetchError(response)) {
        throw response as FetchError;
      }

      // Añadir nuevas acciones a la pila
      dispatch(addActionsTop(
        response.map(withCallerContext)
      ));

      // Aceptar la acción original
      dispatch(acceptAction(action));
      return true;
    } catch (error) {
      console.error("Error in server action call:", error);

      // The refusal of the server runs what its status demands (#935, S3, S5, S6, S7), from the rule every client
      // request shares: a 403 whose body is a list of client actions ran exactly like a 200 answer, so the action
      // succeeded; the other refused statuses end below, with the loading state of the target closed
      if (runRefusal(error, dispatch, targetAddress, withCallerContext) === "actions") {
        dispatch(acceptAction(action));
        return true;
      }

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