import type { ComponentAddress } from "../../contract";
import { acceptAction, addActionsTop, rejectAction } from "../actions/actions";
import { updateAttributes, VALIDATE_COMPONENTS, VALIDATE_ROW } from "../actions/components";
import {
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
import type { RevealComponent, StructureElement } from "../../utilities/reveal";
import type { ComponentState } from "../../utilities/mergeUtils";
import type { ValidateComponentsAction, ValidateRowAction } from "../reducers/components/validationRules";
import type { StateComponent, WithSettings } from "../reducers/components/stateHelpers";
import type { AppThunk } from "../types";
import type { ClientAction, ContextComponents } from "./types";

/** A component of the state: every one has its attributes, reading them fails otherwise, as it always did */
type ValidatedComponent = ComponentState & { attributes: { error?: unknown } };

/**
 * Bring the first invalid field into view: open the tabs, wizard steps and accordion items that hide it,
 * focus it and scroll to it
 */
export function revealFirstInvalidAction(): AppThunk {
  return (dispatch, getState) => {
    // The components that are on screen have what the reveal reads: their address, their context and their model
    const components = getAllComponents(getState()) as Record<string, RevealComponent>;
    const order = getStructureOrder(ViewRegistry.get(ViewRegistry.getCurrentView())?.structure as StructureElement | undefined);
    const target = findFirstInvalid(components, order);
    if (target) {
      getRevealSelections(components, target)
        .forEach(({ address, values }) => dispatch(updateModelWithDependencies(address, { values })));
      focusComponent(target.address.component as string); // A component on screen has its name
    }
  };
}

export function validateComponents(componentList: StateComponent[]): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();

    dispatch({
      type: VALIDATE_COMPONENTS,
      settings: settings,
      componentList,
    } satisfies WithSettings<ValidateComponentsAction>);

    dispatch(addActionsTop([{ type: "verify-validation", parameters: {} }]));
  };
}

export function validateRow(address: ComponentAddress): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();

    dispatch({
      type: VALIDATE_ROW,
      settings: settings,
      address,
    } satisfies WithSettings<ValidateRowAction>);

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
export function validateAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState()) as ContextComponents;
    const address = getActionAddress(action);
    // A dialog resets the context of its components, so the context of the caller does not reach them
    const isInScope = action.target
      ? (component: ContextComponents[string]) =>
        isInsideContext(component.context, address.view, getActionSource(action, components))
      : (component: ContextComponents[string]) => component.context?.view === address.view;
    dispatch(validateComponents(
      Object.values(components).filter((component) =>
        isInScope(component) && !isInPendingDeferredContainer(component, components)
      )
    ));
    dispatch(acceptAction(action));
  };
}

export function verifyValidationAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    if ((Object.values(components) as ValidatedComponent[]).some((component) => component.attributes.error)) {
      dispatch(rejectAction(action));
      dispatch(revealFirstInvalidAction());
    } else {
      dispatch(acceptAction(action));
    }
  };
}

export function setValidAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, { error: null }));
    dispatch(acceptAction(action));
  };
}

export function setInvalidAction(action: ClientAction): AppThunk {
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
export function checkModelUpdatedAction(action: ClientAction): AppThunk {
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

export function checkModelNoUpdatedAction(action: ClientAction): AppThunk {
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

export function checkModelEmptyAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const { isEmptyModel } = getModelValidation(getState());

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