import type { ComponentAddress } from "../../contract";
import { asArray, generateServerAction, getActionAddress, getComponent } from "../../utilities";
import type { Address } from "../../utilities";
import type { ComponentModelData } from "../../utilities/components";
import type { ComponentState } from "../../utilities/mergeUtils";
import { acceptAction, addActionsTop } from "../actions/actions";
import { keepModel, updateAttributes } from "../actions/components";
import { getAllComponents } from "../selectors/componentSelectors";
import { fixDefaultModel, getComponentIdentifierKey } from "../../utilities/components";
import ViewRegistry from "../registry/ViewRegistry";
import type { AppThunk } from "../types";
import { updateModelWithDependencies } from "./components";
import type { ClientAction } from "./types";

const DEFERRED_ACTION = "deferred-data";

/** A component of the state, with the structure path of its context */
type ContextComponent = ComponentState & { context?: { source?: string[] } };

/** An option of the model of a component, or a row of its grid */
interface ModelValue {
  selected?: unknown;
  [key: string]: unknown;
}

/** Parameters of the action that brings the initial model of a component */
interface InitialModelParameters {
  datalist?: { rows?: ModelValue[]; [key: string]: unknown };
  selected?: unknown;
  defaultValues?: unknown;
}

/**
 * Values of the model of a component
 * @param {object} component Component
 * @returns {object[]} Its options or rows
 */
const getModelValues = (component: ComponentState | undefined): ModelValue[] =>
  (component?.model?.values as ModelValue[] | undefined) || [];

/**
 * Name of a component: every component of the state has its address
 * @param {object} component Component
 * @returns {string} Component name
 */
const getComponentName = (component: ComponentState): string => (component.address as ComponentAddress).component as string;

/**
 * Check if a component is inside a container (a dialog or a tab page), at any depth
 * @param {object} component Component
 * @param {object} container Container address
 * @returns {boolean} The component is inside the container
 */
const isInside = (component: ContextComponent, container: Address): boolean =>
  component.address?.view === container.view
  && component.address?.component !== container.component
  && !component.address?.row
  && !component.address?.column
  && (component.context?.source || []).includes(container.component);

/**
 * Check if a component is inside a deferred dialog or tab page which was not opened yet. The content of such a
 * container is not validated: it was never loaded, and the user may not even be able to open it
 * @param {object} component Component
 * @param {object} components All the components
 * @returns {boolean} The component is inside a deferred container pending its first opening
 */
export const isInPendingDeferredContainer = (
  component: ComponentState,
  components: Record<string, ComponentState>
): boolean =>
  Object.values(components).some(container =>
    container.attributes?.deferred && !container.attributes.deferredLoaded
    && (component === container || isInside(component, container.address as Address)));

/**
 * Load the data of a deferred dialog or tab page the first time it is shown.
 * The container is marked as loaded and one silent async server action asks the server for the initial load of its
 * components. If the call fails, or the server answers that the load failed (deferred-failed action), the mark is
 * reset, so the next opening tries again. The components which already have rows, or a selected value, are reported so the server leaves them as
 * they are (an action or a dependency reached them before the first opening).
 *
 * @param {object} address Container address (dialog or tab page)
 * @returns {function} Thunk
 */
export function loadDeferredAction(address: ComponentAddress): AppThunk {
  return (dispatch, getState) => {
    const state = getState();
    const components = getAllComponents(state);
    const container = getComponent(components, address);
    if (!container?.attributes?.deferred || container.attributes.deferredLoaded) {
      return;
    }
    // A deferred container is a dialog or a tab page: its address has the view and the component
    const containerAddress = address as Address;

    // Request it only once, even if the container is shown again while the data is on its way
    dispatch(updateAttributes(address, { deferredLoaded: true }));

    const inside = Object.values(components).filter(component => isInside(component, containerAddress));
    // Components with rows, and components which load by themselves when they are shown (autoload)
    const deferredLoaded = inside
      .filter(component => getModelValues(component).length > 0 || component.attributes?.autoload)
      .map(getComponentName);
    const deferredValued = inside
      .filter(component => getModelValues(component).some(value => value?.selected))
      .map(getComponentName);

    const option = ViewRegistry.get(address.view as string)?.option;
    const { settings } = state;
    dispatch(addActionsTop([{
      ...generateServerAction({ option, deferredLoaded, deferredValued }, DEFERRED_ACTION, containerAddress.component, containerAddress, true, true, settings),
      errorAttributes: { deferredLoaded: false }
    }]));
  };
}

/**
 * The server could not run the initial load of a deferred container (the error is shown as a message).
 * The container is marked as not loaded, so the next opening asks for its data again.
 *
 * @param {object} action Action received: its address is the container
 * @returns {function} Thunk
 */
export function deferredFailedAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    dispatch(updateAttributes(getActionAddress(action), { deferredLoaded: false }));
    dispatch(acceptAction(action));
  };
}

/**
 * Apply the initial model of a component loaded when its deferred container was first shown.
 * It does what the screen load does with the model of each component: the rows and the selected values become the
 * model, and the loaded model is also the stored (restore target) and default (restore) one.
 * A component which already had a selected value keeps it, even if the server sends another one. The default model
 * has the default values the server sent selected (restore goes back to them).
 *
 * @param {object} action Action received: parameters datalist, selected and defaultValues
 * @returns {function} Thunk
 */
export function initialModelAction(action: ClientAction): AppThunk {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { datalist = {}, selected, defaultValues } = (action.parameters || {}) as InitialModelParameters;
    const component = getComponent(getAllComponents(getState()), address);

    if (component) {
      const { rows = [], ...paging } = datalist;
      const identifierKey = getComponentIdentifierKey(component);
      const previousSelected = new Set(
        getModelValues(component)
          .filter(item => item?.selected)
          .map(item => item?.[identifierKey])
          .filter(value => value !== null && value !== undefined)
          .map(String)
      );
      const values = rows.map(row => {
        const rowId = row?.[identifierKey];
        const wasSelected = rowId !== null && rowId !== undefined && previousSelected.has(String(rowId));
        return previousSelected.size ? { ...row, selected: row?.selected || wasSelected } : row;
      });

      dispatch(updateModelWithDependencies(address, { ...paging, values }));
      dispatch(updateAttributes(address, { loading: false }));

      // The selection the component had wins over the one the server sends. An empty selection is not sent to the model:
      // it would filter the loaded options out (the select could not pick its first option)
      if (previousSelected.size === 0 && asArray(selected).length > 0) {
        dispatch(updateModelWithDependencies(address, { selected }));
      }

      // Same as the screen load: the loaded model is the stored one, and the default one has the default values
      const loaded = getComponent(getAllComponents(getState()), address);
      const isGrid = "columnModel" in (loaded?.attributes || {});
      // The model of a component that was loaded has its options
      const defaultModel = fixDefaultModel((loaded?.model || { values }) as ComponentModelData, defaultValues, isGrid);
      dispatch(keepModel(address, { initial: true, defaultModel }));
    }

    dispatch(acceptAction(action));
  };
}
