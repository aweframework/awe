import { generateServerAction, getActionAddress, getComponent } from "../../utilities";
import { acceptAction, addActionsTop } from "../actions/actions";
import { keepModel, updateAttributes } from "../actions/components";
import { getAllComponents } from "../selectors/componentSelectors";
import { fixDefaultModel, getComponentIdentifierKey } from "../../utilities/components";
import ViewRegistry from "../registry/ViewRegistry";
import { updateModelWithDependencies } from "./components";

const DEFERRED_ACTION = "deferred-data";

/**
 * Check if a component is inside a container (a dialog or a tab page), at any depth
 * @param {object} component Component
 * @param {object} container Container address
 * @returns {boolean} The component is inside the container
 */
const isInside = (component, container) =>
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
export const isInPendingDeferredContainer = (component, components) =>
  Object.values(components).some(container =>
    container.attributes?.deferred && !container.attributes.deferredLoaded
    && (component === container || isInside(component, container.address)));

/**
 * Load the data of a deferred dialog or tab page the first time it is shown.
 * The container is marked as loaded and one silent async server action asks the server for the initial load of its
 * components. If the call fails the mark is reset, so the next opening tries again. The components which already have rows, or a selected value, are reported so the server leaves them as
 * they are (an action or a dependency reached them before the first opening).
 *
 * @param {object} address Container address (dialog or tab page)
 * @returns {function} Thunk
 */
export function loadDeferredAction(address) {
  return (dispatch, getState) => {
    const state = getState();
    const components = getAllComponents(state);
    const container = getComponent(components, address);
    if (!container?.attributes?.deferred || container.attributes.deferredLoaded) {
      return;
    }

    // Request it only once, even if the container is shown again while the data is on its way
    dispatch(updateAttributes(address, { deferredLoaded: true }));

    const inside = Object.values(components).filter(component => isInside(component, address));
    // Components with rows, and components which load by themselves when they are shown (autoload)
    const deferredLoaded = inside
      .filter(component => (component.model?.values || []).length > 0 || component.attributes?.autoload)
      .map(component => component.address.component);
    const deferredValued = inside
      .filter(component => (component.model?.values || []).some(value => value?.selected))
      .map(component => component.address.component);

    const option = ViewRegistry.get(address.view)?.option;
    const { settings } = state;
    dispatch(addActionsTop([{
      ...generateServerAction({ option, deferredLoaded, deferredValued }, DEFERRED_ACTION, address.component, address, true, true, settings),
      errorAttributes: { deferredLoaded: false }
    }]));
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
export function initialModelAction(action) {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { datalist = {}, selected, defaultValues } = action.parameters || {};
    const component = getComponent(getAllComponents(getState()), address);

    if (component) {
      const { rows = [], ...paging } = datalist;
      const identifierKey = getComponentIdentifierKey(component);
      const previousSelected = new Set(
        (component.model?.values || [])
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

      // The selection the component had wins over the one the server sends
      if (selected !== undefined && previousSelected.size === 0) {
        dispatch(updateModelWithDependencies(address, { selected }));
      }

      // Same as the screen load: the loaded model is the stored one, and the default one has the default values
      const loaded = getComponent(getAllComponents(getState()), address);
      const isGrid = "columnModel" in (loaded?.attributes || {});
      const defaultModel = fixDefaultModel(loaded?.model || { values }, defaultValues, isGrid);
      dispatch(keepModel(address, { initial: true, defaultModel }));
    }

    dispatch(acceptAction(action));
  };
}
