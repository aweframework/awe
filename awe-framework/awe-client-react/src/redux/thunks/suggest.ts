import type {ComponentAddress} from "../../contract";
import {isEmpty} from "../../utilities/general";
import {fetchAction} from "../../utilities";
import type {Suggestion} from "../../utilities/suggest";
import {runRefusal} from "./refusal";
import {updateModelWithDependencies} from "./components";
import {keepModel} from "../actions/components";
import {getFormValues} from "../selectors/form";
import {addActionsTop} from "../actions/actions";
import type {AppDispatch, AppThunk} from "../types";
import type {ClientAction} from "./types";

// The application provides lodash as a global, so this thunk reads it from there
declare const _: typeof import("lodash");

/** Properties of a suggest that the thunks read */
interface SuggestProps {
  address?: ComponentAddress;
  serverAction?: string;
  targetAction?: string;
  checkTarget?: string;
  strict?: boolean;
  signal?: AbortSignal;
}

/** The datalist a fill action carries */
interface Datalist {
  rows?: Suggestion[];
}

export const suggestAction = (event: unknown, text: string, props: SuggestProps): AppThunk<Promise<Suggestion[]>> => {
  return async (dispatch, getState) => {
    const { settings } = getState();
    const {serverAction = "data", targetAction, strict = true, signal} = props;

    let defaultValue: Suggestion[] = [];
    if (!strict && !isEmpty(text)) {
      defaultValue = [{ value: text, label: text }];
    }

    const response = await fetchAction<ClientAction[]>(
      serverAction,
      targetAction,
      { ...getFormValues(getState()), suggest: text, max: 0 },
      settings.token,
      signal
    );
    // The refusal of the server runs what its status demands (#935, S6), from the rule every client request
    // shares: the panel on a 401, the client actions of a 403 body, the access-denied message on any other 403
    // and the not-found message on a 404. The open suggest keeps its neutral answer: the default value it builds
    if (runRefusal(response, dispatch) !== null) {
      return defaultValue;
    }
    // A guarded answer is a list of actions from here: the guard of runRefusal read every answer that is not one
    const datalist = manageFillAction(response as ClientAction[], dispatch);
    // One option per label in each group (the options of a suggest without groups are one group)
    return _.uniqBy([...(datalist.rows || []), ...defaultValue], item => item.group ? `${item.group}:${item.label}` : item.label);
  };
};

/**
 * Initial suggest
 * @param {string|string[]} suggest   Suggestion text (or the values of a multiple suggest)
 * @param props Suggest properties
 * @memberOf Components
 */
export const initialSuggestAction = (
  suggest: string | string[],
  { address, serverAction, targetAction, checkTarget }: SuggestProps = {}
): AppThunk<Promise<Suggestion[]>> => {
  return async (dispatch, getState) => {
    const { settings } = getState();

    const response = await fetchAction<ClientAction[]>(
      serverAction,
      checkTarget || targetAction,
      { suggest, max: 0 },
      settings.token
    );
    // The refusal of the server runs what its status demands (#935, S6): its actions and its message, and the
    // initial suggest keeps the component empty instead of reading a datalist that is not one
    if (runRefusal(response, dispatch) !== null) {
      return [];
    }
    // A guarded answer is a list of actions from here: the guard of runRefusal read every answer that is not one
    const datalist = manageFillAction(response as ClientAction[], dispatch);
    // A multiple suggest checks all its values at once
    const suggested = [suggest].flat().map(String);
    const selectedItems = (datalist.rows || [])
      .filter(item => suggested.includes(String(item.value)))
      .map(item => ({ ...item, selected: true }));

    // Redux updates
    // A suggest always has the address of its component
    dispatch(updateModelWithDependencies(address as ComponentAddress, { values: selectedItems }));
    dispatch(keepModel(address as ComponentAddress));

    // Devuelve las sugerencias, por si el hook las quiere usar
    return datalist.rows || [];
  };
};

/**
 * Manage a fill action in response
 * @param {object} response Response
 * @param {function} dispatch Dispatch function
 * @return {object} datalist
 * @memberOf Components
 */
const manageFillAction = (response: ClientAction[], dispatch: AppDispatch): Datalist => {
  const fillAction: Partial<ClientAction> = response.filter(action => action.type === "fill").shift() || {};
  const otherActions = response.filter(action => action.type !== "fill") || [];

  // Launch other actions if retrieved
  if (otherActions.length > 0) {
    dispatch(addActionsTop(otherActions));
  }

  // Return datalist from fill action
  const {datalist} = (fillAction.parameters || {datalist: {rows: []}}) as { datalist: Datalist };
  return datalist;
};