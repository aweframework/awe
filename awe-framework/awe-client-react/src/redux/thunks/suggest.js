import {isEmpty} from "../../utilities/general";
import {fetchAction} from "../../utilities";
import {updateModelWithDependencies} from "./components";
import {keepModel} from "../actions/components";
import {getFormValues} from "../selectors/form";


export const suggestAction = (event, text, props) => {
  return async (dispatch, getState) => {
    const { settings } = getState();
    const {serverAction = "data", targetAction, strict = true, signal} = props;

    let defaultValue = [];
    if (!strict && !isEmpty(text)) {
      defaultValue = [{ value: text, label: text }];
    }

    const response = await fetchAction(
      serverAction,
      targetAction,
      { ...getFormValues(getState()), suggest: text, max: 0 },
      settings.token,
      signal
    );

    const datalist = manageFillAction(response);
    return _.uniqBy([...(datalist.rows || []), ...defaultValue], "label");
  };
};

/**
 * Initial suggest
 * @param {string} suggest   Suggestion text
 * @param props Suggest properties
 * @memberOf Components
 */
export const initialSuggestAction = (suggest, { address, serverAction, targetAction, checkTarget } = {}) => {
  return async (dispatch, getState) => {
    const { settings } = getState();

    const response = await fetchAction(
      serverAction,
      checkTarget || targetAction,
      { suggest, max: 0 },
      settings.token
    );

    const datalist = manageFillAction(response);
    const selectedItems = (datalist.rows || [])
      .filter(item => String(item.value) === String(suggest))
      .map(item => ({ ...item, selected: true }));

    // Redux updates
    dispatch(updateModelWithDependencies(address, { values: selectedItems }));
    dispatch(keepModel(address));

    // Devuelve las sugerencias, por si el hook las quiere usar
    return datalist.rows || [];
  };
};

/**
 * Manage a fill action in response
 * @param {object} response Response
 * @return {object} datalist
 * @memberOf Components
 */
const manageFillAction =  (response) => {
  let fillAction = response.filter(action => action.type === "fill").shift() || {};
  const {datalist} = fillAction.parameters || {datalist: {rows: []}};
  return datalist;
};