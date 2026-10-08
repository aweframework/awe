import {
  asArray,
  generateServerAction,
  getActionAddress,
  getActionSource,
  getComponent,
  isInsideContext
} from "../../utilities";
import { acceptAction, addActionsTop } from "../actions/actions";
import { ButtonTypes, keepModel, updateAttributes } from "../actions/components";
import { getFormValues } from "../selectors/form";
import { getAllComponents } from "../selectors/componentSelectors";
import {
  resetMultipleModelWithDependencies,
  restoreMultipleModelWithDependencies,
  updateModelWithDependencies
} from "./components";
import { getComponentIdentifierKey } from "../../utilities/components";
const { BUTTON_SUBMIT } = ButtonTypes;

export function submitAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const submitButton = Object.values(components).find(
      (component) =>
        component.attributes.buttonType === BUTTON_SUBMIT &&
        isInsideContext(
          component.context,
          address.view,
          getActionSource(action, components)
        )
    );
    if (submitButton) {
      dispatch(addActionsTop([
        {
          type: "click",
          address: submitButton.address,
          target: submitButton.address.component,
        },
      ]));
    }
    dispatch(acceptAction(action));
  };
}

export function resetAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    // Check reset target
    const address = getActionAddress(action);
    dispatch(resetMultipleModelWithDependencies(Object.values(components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, components)))));

    // Finish action
    dispatch(acceptAction(action));
  };
}

/**
 * Restore the models of the components in the action context.
 * @param {object} action Action received
 * @param {boolean} initial True to restore the first loaded values, false to restore the default ones
 * @return {function} Thunk
 */
function restoreModels(action, initial) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    // Check restore target
    const address = getActionAddress(action);
    dispatch(restoreMultipleModelWithDependencies(Object.values(components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, components))), initial));

    // Finish action
    dispatch(acceptAction(action));
  };
}

/**
 * Restore the default values (the ones defined in the screen) like the AngularJS client "restore" action
 * @param {object} action Action received
 */
export function restoreAction(action) {
  return restoreModels(action, false);
}

/**
 * Restore the first loaded values like the AngularJS client "restore-target" action
 * @param {object} action Action received
 */
export function restoreTargetAction(action) {
  return restoreModels(action, true);
}

export function filterAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { settings } = getState();
    // Define server and target action
    const address = getActionAddress(action);

    // Get component
    let component = getComponent(components, address);

    // If the component is not found (i.e. a broadcast filter of a dialog which is not open), the action is ignored
    // like the AngularJS garbage action collector does
    if (component) {
      // Start loading component
      dispatch(updateAttributes(address, { loading: true }));

      // Add action to actions stack
      const serverAction = component.attributes[settings.serverActionKey] || "data";
      const targetAction = component.attributes[settings.targetActionKey];
      let values = {
        ...getFormValues(getState()),
        ...(component.specificAttributes || {})
      };

      // Generate server action
      let filterAction = generateServerAction(values, serverAction, targetAction, address, action.async, action.silent, settings);

      // Send action list
      dispatch(addActionsTop([filterAction]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}

export function fillAction(action) {
  return (dispatch, getState) => {
    // Retrieve parameters
    const { parameters } = action;
    const address = getActionAddress(action);
    const { datalist } = parameters;
    const components = getAllComponents(getState());
    const component = getComponent(components, address);
    const identifierKey = getComponentIdentifierKey(component);
    const previousSelected = new Set(
      (component?.model?.values || [])
        .filter((item) => item?.selected)
        .map((item) => item?.[identifierKey])
        .filter((value) => value !== null && value !== undefined)
        .map((value) => String(value))
    );
    const values = (datalist?.rows || []).map((row) => {
      if (!previousSelected.size) {
        return row;
      }
      const rowId = row?.[identifierKey];
      const isPreviouslySelected = rowId !== null && rowId !== undefined && previousSelected.has(String(rowId));
      return { ...row, selected: row?.selected || isPreviouslySelected };
    });

    // Generate model
    let model = { ...datalist, values };
    delete model.rows;

    // Publish model change
    dispatch(updateModelWithDependencies(address, model));

    // Publish end loading
    dispatch(updateAttributes(address, { loading: false }));

    // Finish action
    dispatch(acceptAction(action));
  };
}

export function updateControllerAction(action) {
  return (dispatch) => {
    // Get values
    const address = getActionAddress(action);
    const values = [...((action.parameters.datalist || {}).rows || [{}])];

    // Change controller
    dispatch(updateAttributes(address, { [action.parameters.attribute]: action.parameters.value || values[0].value }));

    // Finish action
    dispatch(acceptAction(action));
  };
}

export function selectAction(action) {
  return (dispatch) => {
    // Retrieve parameters
    const address = getActionAddress(action);
    let selected = asArray(action.parameters.values)
      .map((item) => {
        if (item && typeof item === "object") {
          return item.value;
        }
        return item;
      })
      .filter((item) => item !== undefined && item !== null);

    // Call the method update selected value from API
    dispatch(updateModelWithDependencies(address, { selected }));

    // Publish end loading
    dispatch(updateAttributes(address, { loading: false }));

    // Finish action
    dispatch(acceptAction(action));
  };
}

/**
 * Start loading
 * @param {object} action Action received
 */
export function startLoadAction(action) {
  return (dispatch) => {
    // Start loading
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, { loading: true }));

    // Accept action
    dispatch(acceptAction(action));
  };
}

/**
 * Finish loading
 * @param {object} action Action received
 */
export function endLoadAction(action) {
  return (dispatch) => {
    // Start loading
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, { loading: false }));

    // Close action
    dispatch(acceptAction(action));
  };
}

/**
 * Keep criteria values after initial initialization
 * @param {object} action Action received
 */
export function keepAction(action) {
  return (dispatch) => {
    // Start loading
    const address = getActionAddress(action);

    // Keep model
    dispatch(keepModel(address));

    // Close action
    dispatch(acceptAction(action));
  };
}
