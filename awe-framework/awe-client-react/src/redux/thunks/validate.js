import { acceptAction, addActionsTop, rejectAction } from "../actions/actions";
import { updateAttributes, VALIDATE_COMPONENTS, VALIDATE_ROW } from "../actions/components";
import {
  generateMessageAction,
  generateServerAction,
  getActionAddress,
  getActionSource,
  isInsideContext
} from "../../utilities";
import { getModelValidation } from "../selectors/modelValidation";
import { getAllComponents } from "../selectors/componentSelectors";
import { updateModelWithDependencies } from "./components";
import { isInPendingDeferredContainer } from "./deferred";
import ViewRegistry from "../registry/ViewRegistry";
import { findFirstInvalid, focusComponent, getRevealSelections, getStructureOrder } from "../../utilities/reveal";

/**
 * Bring the first invalid field into view: open the tabs, wizard steps and accordion items that hide it,
 * focus it and scroll to it
 */
export function revealFirstInvalidAction() {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const order = getStructureOrder(ViewRegistry.get(ViewRegistry.getCurrentView())?.structure);
    const target = findFirstInvalid(components, order);
    if (target) {
      getRevealSelections(components, target)
        .forEach(({ address, values }) => dispatch(updateModelWithDependencies(address, { values })));
      focusComponent(target.address.component);
    }
  };
}

export function validateComponents(componentList) {
  return (dispatch, getState) => {
    const { settings } = getState();

    dispatch({
      type: VALIDATE_COMPONENTS,
      settings: settings,
      componentList,
    });

    dispatch(addActionsTop([{ type: "verify-validation", parameters: {} }]));
  };
}

export function validateRow(address) {
  return (dispatch, getState) => {
    const { settings } = getState();

    dispatch({
      type: VALIDATE_ROW,
      settings: settings,
      address,
    });

    dispatch(addActionsTop([{ type: "verify-row-validation", address, parameters: {} }]));
  };
}

/**
 * Validate the components of the view. A validate with a target validates the components inside the target; one
 * without it validates the whole view, dialogs included (as the AngularJS client does with the whole document), even
 * if the dialog is closed. The content of a deferred container which was never opened is always skipped.
 *
 * @param {object} action Validate action
 * @returns {function} Thunk
 */
export function validateAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    // A dialog resets the context of its components, so the context of the caller does not reach them
    const isInScope = action.target
      ? (component) => isInsideContext(component.context, address.view, getActionSource(action, components))
      : (component) => component.context?.view === address.view;
    dispatch(validateComponents(
      Object.values(components).filter((component) =>
        isInScope(component) && !isInPendingDeferredContainer(component, components)
      )
    ));
    dispatch(acceptAction(action));
  };
}

export function verifyValidationAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    if (Object.values(components).some((component) => component.attributes.error)) {
      dispatch(rejectAction(action));
      dispatch(revealFirstInvalidAction());
    } else {
      dispatch(acceptAction(action));
    }
  };
}

export function setValidAction(action) {
  return (dispatch) => {
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, { error: null }));
    dispatch(acceptAction(action));
  };
}

export function setInvalidAction(action) {
  return (dispatch) => {
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, { error: action.parameters }));
    dispatch(acceptAction(action));
  };
}

/**
 * Check if model has been modified
 * @param {object} action
 */
export function checkModelUpdatedAction(action) {
  return (dispatch, getState) => {
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
      dispatch(addActionsTop([{type: "confirm", parameters: {...values, address}}]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}

export function checkModelNoUpdatedAction(action) {
  return (dispatch, getState) => {
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
      dispatch(addActionsTop([{type: "confirm", parameters: {...values, address}}]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}

export function checkModelEmptyAction(action) {
  return (dispatch, getState) => {
    const { isEmptyModel, isUpdatedModel } = getModelValidation(getState());

    // Define server and target action
    const address = getActionAddress(action);

    // If model is empty, launch confirm screen
    if (isEmptyModel) {
      const values = {
        title: 'CONFIRM_TITLE_EMPTY_DATA',
        message: 'CONFIRM_MESSAGE_EMPTY_DATA'
      };

      // Send action list
      dispatch(addActionsTop([{type: "confirm", parameters: {...values, address}}]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}