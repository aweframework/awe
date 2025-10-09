import {acceptAction, addActionsTop, rejectAction} from "../actions/actions";
import {updateAttributes, VALIDATE_COMPONENTS, VALIDATE_ROW} from "../actions/components";
import {generateServerAction, getActionAddress, getActionSource, isInsideContext} from "../../utilities";
import {getModelValidation} from "../selectors/modelValidation";

export function validateComponents(componentList) {
  return (dispatch, getState) => {
    const {settings} = getState();

    dispatch({
      type: VALIDATE_COMPONENTS,
      settings: settings,
      componentList,
    });

    dispatch(addActionsTop([{type: "verify-validation", parameters: {}}]));
  };
}

export function validateRow(address) {
  return (dispatch, getState) => {
    const {settings} = getState();

    dispatch({
      type: VALIDATE_ROW,
      settings: settings,
      address,
    });

    dispatch(addActionsTop([{type: "verify-row-validation", address, parameters: {}}]));
  };
}

export function validateAction(action) {
  return (dispatch, getState) => {
    const {components} = getState();
    const address = getActionAddress(action);
    dispatch(validateComponents(
      Object.values(components).filter((component) =>
        isInsideContext(
          component.context,
          address.view,
          getActionSource(action, components)
        )
      )
    ));
    dispatch(acceptAction(action));
  };
}

export function verifyValidationAction(action) {
  return (dispatch, getState) => {
    const {components} = getState();
    if (Object.values(components).some((component) => component.attributes.error)) {
      dispatch(rejectAction(action));
    } else {
      dispatch(acceptAction(action));
    }
  };
}

export function setValidAction(action) {
  return (dispatch) => {
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, {error: null}));
    dispatch(acceptAction(action));
  };
}

export function setInvalidAction(action) {
  return (dispatch) => {
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, {error: action.parameters}));
    dispatch(acceptAction(action));
  };
}

/**
 * Check if model has been modified
 * @param {object} action
 */
export function checkModelUpdatedAction(action) {
  return (dispatch, getState) => {
    const {settings} = getState();
    const { isUpdatedModel } = getModelValidation(getState());
    // Define server and target action
    const address = getActionAddress(action);
    // If model has not changed
    if (isUpdatedModel) {
      const values = {
        title: 'CONFIRM_TITLE_UPDATED_DATA',
        message: 'CONFIRM_MESSAGE_UPDATED_DATA'
      };
      // Generate server action
      let confirmAction = generateServerAction(values, "confirm", null, address, false, false, settings);

      // Send action list
      dispatch(addActionsTop([confirmAction]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}

export function checkModelNoUpdatedAction(action) {
  return (dispatch, getState) => {
    const {settings} = getState();
    const { isUnchangedModel } = getModelValidation(getState());

    // Define server and target action
    const address = getActionAddress(action);

    // If model has not changed
    if (isUnchangedModel) {
      const values = {
        title: 'CONFIRM_TITLE_NOT_UPDATED_DATA',
        message: 'CONFIRM_MESSAGE_NOT_UPDATED_DATA'
      };
      // Generate server action
      let confirmAction = generateServerAction(values, "confirm", null, address, false, false, settings);

      // Send action list
      dispatch(addActionsTop([confirmAction]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}

export function checkModelEmptyAction(action) {
  return (dispatch, getState) => {
    const {settings} = getState();
    const { isEmptyModel, isUpdatedModel } = getModelValidation(getState());

    // Define server and target action
    const address = getActionAddress(action);

    // If model is empty, launch confirm screen
    if (isEmptyModel) {
      const values = {
        title: 'CONFIRM_TITLE_EMPTY_DATA',
        message: 'CONFIRM_MESSAGE_EMPTY_DATA'
      };
      // Generate server action
      let confirmAction = generateServerAction(values, "confirm", null, address, false, false, settings);

      // Send action list
      dispatch(addActionsTop([confirmAction]));
      // If model has not changed
      if (isUpdatedModel) {
        const values = {
          title: 'CONFIRM_TITLE_UPDATED_DATA',
          message: 'CONFIRM_MESSAGE_UPDATED_DATA'
        };
        // Generate server action
        let confirmAction = generateServerAction(values, "confirm", null, address, false, false, settings);

        // Send action list
        dispatch(addActionsTop([confirmAction]));
      }

      // Accept action
      dispatch(acceptAction(action));
    }
  };
}