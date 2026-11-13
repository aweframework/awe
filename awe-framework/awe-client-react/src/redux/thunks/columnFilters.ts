import type {ComponentAddress} from "../../contract";
import {fetchAction, getComponent} from "../../utilities";
import {getColumnFilterType, getFilterFields} from "../../utilities/columnFilters";
import type {ColumnValue, FilterableColumn} from "../../utilities/columnFilters";
import {addActionsTop} from "../actions/actions";
import {runRefusal} from "./refusal";
import {getAllComponents} from "../selectors/componentSelectors";
import {getFormValues} from "../selectors/form";
import type {AppThunk} from "../types";
import type {ClientAction} from "./types";

/** Server action that lists the distinct values of a grid column (#912, S8) */
export const COLUMN_VALUES_ACTION = "column-values";

// Actions of the answer the menu reads itself or that have no component to work on
const OWN_ACTIONS = new Set([COLUMN_VALUES_ACTION, "end-load"]);

/** Values of a column for its autofilter menu, and whether the column has more values than the ones listed */
export interface ColumnValuesResult {
  values: ColumnValue[];
  more: boolean;
}

/**
 * Read a flag the server sends as a boolean or as its text
 * @param {*} value Value
 * @returns {boolean} True for true and "true"
 */
const isTrue = (value: unknown): boolean => value === true || String(value).toLowerCase() === "true";

/**
 * Load the distinct values of a grid column (S15): the column-values action of the query of the grid with the values of
 * the screen, the parameters of the grid (its column filters included; the server leaves out the filter of the column
 * itself) and the column, its fields, its type and the search. The other actions of the answer (an error message) are
 * launched as usual.
 * @param {object} address Grid address
 * @param {object} column Column
 * @param {string} [search] Text the labels contain
 * @param {AbortSignal} [signal] Cancels the request (a newer search, the menu closed)
 * @returns {Promise<{values: object[], more: boolean}>} Values (value, label and count) and whether more exist
 */
export const columnValuesAction = (address: ComponentAddress, column: FilterableColumn, search?: string,
                                   signal?: AbortSignal): AppThunk<Promise<ColumnValuesResult>> => {
  return async (dispatch, getState) => {
    const state = getState();
    const {settings} = state;
    const grid = getComponent(getAllComponents(state), address);
    const attributes = (grid?.attributes || {}) as Record<string, unknown>;
    const targetAction = attributes[settings.targetActionKey] as string;
    const text = String(search ?? "").trim();
    const columnValues = {
      id: column.name,
      fields: getFilterFields(column),
      type: getColumnFilterType(column),
      ...(text ? {search: text} : {})
    };

    const response = await fetchAction<ClientAction[]>(COLUMN_VALUES_ACTION, targetAction, {
      ...getFormValues(state, false, undefined, address),
      ...(grid?.specificAttributes || {}),
      columnValues
    }, settings.token, signal);

    // The refusal of the server runs what its status demands (#935, S6): the panel on a 401, the client actions of
    // a 403 body, the access-denied message on any other 403 and the not-found message on a 404, and the filter
    // menu keeps no values
    if (runRefusal(response, dispatch) !== null) {
      return {values: [], more: false};
    }
    // A failed request of any other kind has no list of actions: no values, as it always did
    if (!Array.isArray(response)) {
      return {values: [], more: false};
    }
    const others = response.filter(action => !OWN_ACTIONS.has(action.type));
    if (others.length > 0) {
      dispatch(addActionsTop(others));
    }
    const answer = response.find(action => action.type === COLUMN_VALUES_ACTION);
    const parameters = (answer?.parameters || {}) as { datalist?: { rows?: ColumnValue[] }; more?: unknown };
    return {values: parameters.datalist?.rows || [], more: isTrue(parameters.more)};
  };
};
