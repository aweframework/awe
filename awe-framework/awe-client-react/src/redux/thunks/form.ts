import type { ComponentAddress } from "../../contract";
import {
  asArray,
  generateServerAction,
  getActionAddress,
  getActionSource,
  getComponent,
  isInsideContext
} from "../../utilities";
import type { Address } from "../../utilities";
import type { ComponentState } from "../../utilities/mergeUtils";
import { acceptAction, addActionsTop } from "../actions/actions";
import { ButtonTypes, keepModel, updateAttributes } from "../actions/components";
import type { ComponentUpdate } from "../actions/components";
import { getFormValues } from "../selectors/form";
import { getAllComponents } from "../selectors/componentSelectors";
import {
  raiseEventOnDependencies,
  resetMultipleModelWithDependencies,
  restoreMultipleModelWithDependencies,
  updateModelWithDependencies
} from "./components";
import { getComponentIdentifierKey, isGridComponent } from "../../utilities/components";
import type { AppThunk } from "../types";
import type { ClientAction, ContextComponents } from "./types";
const { BUTTON_SUBMIT } = ButtonTypes;

/** A row of a model, or an option of it */
type ModelRow = Record<string, unknown>;

/** A cell that was pushed while the rows of its grid were loading */
interface PendingCellUpdate {
  row: string;
  column: string;
  data: unknown;
}

/** Parameters of a fill action: the datalist has the rows and the paging data */
interface FillParameters {
  datalist?: { rows?: ModelRow[]; [key: string]: unknown };
}

/** Parameters of an action that changes an attribute of the controller of a component */
interface UpdateControllerParameters {
  datalist?: { rows?: Array<{ value?: unknown }> };
  attribute?: string;
  value?: unknown;
}

/**
 * The options or rows of the model of a component
 * @param {object} component Component
 * @returns {object[]} Its values
 */
const getModelValues = (component: ComponentState | undefined): ModelRow[] =>
  (component?.model?.values as ModelRow[] | undefined) || [];

export function submitAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState()) as ContextComponents;
    const address = getActionAddress(action);
    const submitButton = Object.values(components).find(
      (component) =>
        (component.attributes as Record<string, unknown>).buttonType === BUTTON_SUBMIT &&
        isInsideContext(
          component.context,
          address.view,
          getActionSource(action, components)
        )
    );
    if (submitButton) {
      // Every component has its address
      const buttonAddress = submitButton.address as ComponentAddress;
      dispatch(addActionsTop([
        {
          type: "click",
          address: buttonAddress,
          target: buttonAddress.component,
        },
      ]));
    }
    dispatch(acceptAction(action));
  };
}

export function resetAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState()) as ContextComponents;
    // Check reset target
    const address = getActionAddress(action);
    const inContext = Object.values(components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, components)));
    // Every component has its address
    dispatch(resetMultipleModelWithDependencies(inContext as ComponentUpdate[]));

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
function restoreModels(action: ClientAction, initial: boolean): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState()) as ContextComponents;
    // Check restore target
    const address = getActionAddress(action);
    const inContext = Object.values(components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, components)));
    // Every component has its address
    dispatch(restoreMultipleModelWithDependencies(inContext as ComponentUpdate[], initial));

    // Finish action
    dispatch(acceptAction(action));
  };
}

/**
 * Restore the default values (the ones defined in the screen) like the AngularJS client "restore" action
 * @param {object} action Action received
 */
export function restoreAction(action: ClientAction): AppThunk {
  return restoreModels(action, false);
}

/**
 * Restore the first loaded values like the AngularJS client "restore-target" action
 * @param {object} action Action received
 */
export function restoreTargetAction(action: ClientAction): AppThunk {
  return restoreModels(action, true);
}

export function filterAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { settings } = getState();
    // Define server and target action
    const address = getActionAddress(action);

    // Get component
    const component = getComponent(components, address);

    // If the component is not found (i.e. a broadcast filter of a dialog which is not open), the action is ignored
    // like the AngularJS garbage action collector does
    if (component) {
      // Start loading component
      dispatch(updateAttributes(address, { loading: true }));

      // Add action to actions stack
      const attributes = component.attributes as Record<string, unknown>;
      const serverAction = (attributes[settings.serverActionKey] || "data") as string;
      const targetAction = attributes[settings.targetActionKey] as string;
      const values = {
        ...getFormValues(getState(), false, undefined, address),
        ...(component.specificAttributes || {})
      };

      // Generate server action
      // The component that is filtered has its view and its name
      const filterAction = generateServerAction(values, serverAction, targetAction, address as Address, action.async, action.silent, settings);

      // Send action list
      dispatch(addActionsTop([filterAction]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
}

export function fillAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    // Retrieve parameters
    const { parameters } = action;
    const address = getActionAddress(action);
    const { datalist } = parameters as FillParameters;
    const components = getAllComponents(getState());
    const component = getComponent(components, address);
    // A component that is not there is not a grid, so its key is the default one
    const identifierKey = getComponentIdentifierKey(component as ComponentState);
    const previousSelected = new Set(
      getModelValues(component)
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
    const model = { ...datalist, values };
    delete model.rows;

    // Publish model change
    dispatch(updateModelWithDependencies(address, model));

    // Publish end loading
    dispatch(updateAttributes(address, { loading: false }));

    // The loaded rows may predate cells pushed (update-cell) while the load was pending: apply them again
    const pendingCellUpdates = (getComponent(getAllComponents(getState()), address)?.attributes?.pendingCellUpdates as PendingCellUpdate[] | undefined) || [];
    if (pendingCellUpdates.length > 0) {
      dispatch(updateAttributes(address, { pendingCellUpdates: [] }));
      pendingCellUpdates.forEach(({ row, column, data }) => dispatch(updateModelWithDependencies({ ...address, row, column }, { values: data })));
    }

    // A grid raises change once its rows are loaded, like AngularJS: only the dependencies listening to it are checked
    if (isGridComponent(component)) {
      dispatch(raiseEventOnDependencies(address, "change"));
    }

    // Finish action
    dispatch(acceptAction(action));
  };
}

/**
 * Fill a suggest with the values of a fill-suggest action (FillSuggestActionBuilder) and select all of them.
 * It runs as a fill with every value marked as selected, so the labels are kept and no request is sent.
 * The builder that sends it (FillSuggestActionBuilder) is deprecated: new code sends a fill and a select instead.
 * @param {object} action Action received, with the suggest values (value and label) in parameters.values
 */
export function fillSuggestAction(action: ClientAction): AppThunk {
  const rows = asArray<ModelRow>(action.parameters?.values as ModelRow | ModelRow[] | undefined).map((value) => ({ ...value, selected: true }));
  return fillAction({ ...action, parameters: { ...action.parameters, datalist: { rows } } });
}

export function updateControllerAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    // Get values
    const address = getActionAddress(action);
    const parameters = action.parameters as UpdateControllerParameters;
    const values = [...((parameters.datalist || {}).rows || [{}])];

    // Change controller
    dispatch(updateAttributes(address, { [parameters.attribute as string]: parameters.value || values[0].value }));

    // Finish action
    dispatch(acceptAction(action));
  };
}

export function selectAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    // Retrieve parameters
    const address = getActionAddress(action);
    const selected = asArray((action.parameters as { values?: unknown }).values)
      .map((item) => {
        if (item && typeof item === "object") {
          return (item as { value?: unknown }).value;
        }
        return item;
      })
      .filter((item) => item !== undefined && item !== null);

    // Call the method update selected value from API
    dispatch(updateModelWithDependencies(address, { selected }));

    // Publish end loading (no load is pending any more, so no pushed cell is waiting for it)
    dispatch(updateAttributes(address, { loading: false, pendingCellUpdates: [] }));

    // Finish action
    dispatch(acceptAction(action));
  };
}

/**
 * Start loading
 * @param {object} action Action received
 */
export function startLoadAction(action: ClientAction): AppThunk {
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
export function endLoadAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    // Finish loading: a load which ends without rows (error, abort) leaves no pushed cell waiting for them
    const address = getActionAddress(action);
    dispatch(updateAttributes(address, { loading: false, pendingCellUpdates: [] }));

    // Close action
    dispatch(acceptAction(action));
  };
}

/**
 * Keep criteria values after initial initialization
 * @param {object} action Action received
 */
export function keepAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    // Start loading
    const address = getActionAddress(action);

    // Keep model
    dispatch(keepModel(address));

    // Close action
    dispatch(acceptAction(action));
  };
}
