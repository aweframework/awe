import {
  generateMessageAction,
  generateServerAction,
  getActionAddress,
  getActionSource,
  getComponent,
  isInsideContext, translateLabel
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

export function restoreAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    // Check reset target
    const address = getActionAddress(action);
    dispatch(restoreMultipleModelWithDependencies(Object.values(components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, components)))));

    // Finish action
    dispatch(acceptAction(action));
  };
}

export function filterAction(action, t = (o) => o) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { settings, view } = getState();
    // Define server and target action
    const address = getActionAddress(action);

    // Get component
    let component = getComponent(components, address);

    // If component not found, send an error message
    if (!component) {
      dispatch(addActionsTop([generateMessageAction("error", translateLabel('ERROR_TITLE_NOT_DEFINED', t), translateLabel('ERROR_MESSAGE_NOT_DEFINED_IN', t, address.component, view[address.view].option))]));
    } else {
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
  return (dispatch) => {
    // Retrieve parameters
    const { parameters } = action;
    const address = getActionAddress(action);
    const { datalist } = parameters;

    // Generate model
    let model = { ...datalist, values: [...datalist.rows] };
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
    let selected = [...action.parameters.values];

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