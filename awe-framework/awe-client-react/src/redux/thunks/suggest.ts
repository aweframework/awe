import type {ComponentAddress} from "../../contract";
import {isEmpty} from "../../utilities/general";
import {fetchAction} from "../../utilities";
import type {Suggestion} from "../../utilities/suggest";
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
    ) as ClientAction[]; // An error answer has no list of actions: reading it as one fails, as it always did

    const datalist = manageFillAction(response, dispatch);
    return _.uniqBy([...(datalist.rows || []), ...defaultValue], "label");
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
    ) as ClientAction[]; // An error answer has no list of actions: reading it as one fails, as it always did

    const datalist = manageFillAction(response, dispatch);
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