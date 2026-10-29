import {
  KEEP_MODEL,
  RESET_MODEL,
  RESET_MULTIPLE_MODEL,
  RESTORE_MODEL,
  RESTORE_MULTIPLE_MODEL,
  UPDATE_MODEL,
  UPDATE_MULTIPLE_MODELS
} from '../../actions/components';
import type {
  keepModel,
  resetModel as resetModelCreator,
  resetMultipleModel,
  restoreModel,
  restoreMultipleModel,
  updateModel as updateModelCreator,
  updateMultipleModels
} from '../../actions/components';
import _ from 'lodash';
import type {ComponentAddress} from '../../../contract';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas} from '../../../utilities/mergeUtils';
import {asArray} from "../../../utilities";
import {warnMalformedComponent} from "../../../utilities/components";
import {isEmpty} from "../../../utilities/general";
import type {CellOption} from "../../../utilities/grid";
import type {ComponentsState} from '../../types';
import {updateGridFooter} from './gridFooter';
import {
  getFilteredValues,
  getGridModelUpdate,
  getGroupModelUpdate,
  resetCellModel,
  updateCellModel,
  updateCellSelected,
  updateColumnModel,
  updateSelectedGrid,
  updateSelectedGroup
} from './gridModel';
import {
  getKeepModelComponent,
  getMergedComponent,
  getModelUpdate,
  hasFooter,
  isGridComponent,
  isGroup,
  launchAddressFunction,
  mergeStateComponent,
  NO_ACTION,
  resolveComponentId,
  updateComponentInState
} from './stateHelpers';
import type {ActionHandlers, ReducerSettings, StateComponent, StateModel, StateRow} from './stateHelpers';

/** Actions the model handlers take */
export type ModelAction = ReturnType<
  typeof updateModelCreator | typeof updateMultipleModels | typeof keepModel | typeof restoreModel |
  typeof restoreMultipleModel | typeof resetModelCreator | typeof resetMultipleModel>;

/**
 * Update or register a component and its model/attributes.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} data Component data or partial update
 * @returns {Object} Updated state
 */
export function updateComponentData(state: ComponentsState, componentId: string | null, data: StateComponent): ComponentsState {
  if (componentId == null) {
    warnMalformedComponent(data, {
      origin: 'reducer',
      operation: 'updateComponentData'
    });
    return state;
  }

  // Si data contiene address y attributes, podría ser un componente completo
  // Si no, es una actualización parcial que debe ir directamente a deltas
  if (data.address && data.attributes) {
    // Registrar en Registry (actualizar estado base)
    if (!data.address) {
      warnMalformedComponent({...data, uid: data?.uid ?? componentId}, {
        origin: 'reducer',
        operation: 'registerComponentBase',
        extra: {
          registryComponentId: componentId
        }
      });
    }
    ComponentRegistry.register(componentId, data);

    // Calcular deltas
    const base = ComponentRegistry.get(componentId);
    const deltas = calculateDeltas(base, data);

    return {
      ...state,
      [componentId]: {
        ...state[componentId],
        ...deltas
      }
    };
  }

  // Comportamiento normal o actualización parcial
  return updateComponentInState(state, componentId, data);
}

/**
 * Update a model at component/column/cell scope depending on address.
 * @param {Object} state Current state
 * @param {Object} address Target address
 * @param {Object} data Model updates
 * @param {boolean} update Whether to update grid cell models
 * @returns {Object} Updated state
 */
export function updateModel(state: ComponentsState, address: ComponentAddress, data: StateModel, update = true): ComponentsState {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);

  if (isEmpty(component)) return state;

  let newModel;
  if (isGridComponent(component) && data.values && update) {
    newModel = getGridModelUpdate(state, address, data, update);
  } else if (isGridComponent(component) && data.selected) {
    newModel = updateSelectedGrid(state, component?.model?.values || [], address, data);
  } else if (isGroup(component)) {
    newModel = getGroupModelUpdate(state, address?.view, component?.attributes?.group, data);
  } else {
    newModel = getModelUpdate(state, address, data);
  }
  return {
    ...state,
    ...newModel
  };
}

/**
 * Update selection flags for a non-grid component.
 * @param {Object} state Current state
 * @param {Array} values Current values
 * @param {Object} address Component address
 * @param {Array} selected Selected values array
 * @returns {Object|null} Updated state or null if no changes
 */
export function updateSelectedComponent(
  state: ComponentsState,
  values: CellOption[],
  address: ComponentAddress,
  selected: unknown[]
): ComponentsState | null {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);

  if (!component) return state;

  const filtered = getFilteredValues(values, selected);
  if (_.isEqual(filtered, (component.model?.values))) {
    return null;
  }

  // Update values
  return getModelUpdate(state, address, { values: filtered });
}

/**
 * Update selection at the correct scope for a component.
 * @param {Object} state Current state
 * @param {Object} address Target address
 * @param {Object} data Selection payload
 * @returns {Object} Updated state
 */
export function updateSelected(state: ComponentsState, address: ComponentAddress, data: {selected?: unknown}): ComponentsState {
  const update = getSelectedUpdate(state, address, data);
  if (update) {
    return {
      ...state,
      ...update
    };
  } else {
    return state;
  }
}

/**
 * Build the selection update for a component.
 * @param {Object} state Current state
 * @param {Object} address Target address
 * @param {Object} data Selection payload
 * @returns {Object|null} Update to apply
 */
export function getSelectedUpdate(state: ComponentsState, address: ComponentAddress, data: {selected?: unknown}): ComponentsState | null {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);

  // As the AngularJS client does, a selection on a component that is not on the screen is ignored
  if (!component) {
    // Deliberate debug trace: a developer can find out why the selection did nothing
    // eslint-disable-next-line no-console
    console.debug(`[MODEL] Selection ignored, '${componentId}' is not on screen`, address);
    return null;
  }

  const values = (component.model?.values) || [];
  const selected = asArray(data.selected);
  if (isGridComponent(component)) {
    return updateSelectedGrid(state, values, address, { selected });
  } else if (isGroup(component)) {
    return updateSelectedGroup(state, values, address?.view, component.attributes?.group, selected);
  } else {
    return updateSelectedComponent(state, values, address, selected);
  }
}

/**
 * Store the current model snapshot for a component.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @param {boolean} initial True when the snapshot is the initial model, so it is also the default one
 * @param {Object} defaultModel Default model to store with the initial one
 * @returns {Object} Updated state
 */
export function keepModelComponent(
  state: ComponentsState,
  componentId: string,
  settings?: ReducerSettings,
  initial = false,
  defaultModel: StateModel | null = null
): ComponentsState {
  return {
    ...state,
    ...getKeepModelComponent(state, componentId, settings, initial, defaultModel)
  };
}

/**
 * Restore a component model from the stored snapshot.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @param {boolean} initial True to restore the first loaded model instead of the default one
 * @returns {Object} Updated state
 */
export function restoreModelComponent(
  state: ComponentsState,
  componentId: string,
  settings?: ReducerSettings,
  initial = false
): ComponentsState {
  const newState = {
    ...state,
    ...getRestoreModelComponent(state, componentId, settings, initial)
  };

  const component = getMergedComponent(componentId, newState);

  // Update footer if is grid and show totals
  if (hasFooter(component)) {
    // The footer is given the component identifier where it takes an address, as it always was
    return updateGridFooter(newState, componentId as ComponentAddress);
  }

  return newState;
}

/**
 * Get the model a restore action goes back to.
 * "restore-target" goes back to the first loaded model. "restore" goes back to the default selection registered with the
 * screen, keeping the options the component has now (like the AngularJS client, it restores only the selection).
 * Grids and components registered without a default model go back to the first loaded model.
 * @param {Object} component Component to restore
 * @param {boolean} initial True to restore the first loaded model
 * @returns {Object} Model the component goes back to
 */
export function getRestoredModel(component: StateComponent | undefined, initial?: boolean): StateModel | undefined {
  // The component registry merges a missing default model into an empty object: treat it as absent
  if (initial || isGridComponent(component) || !Array.isArray(component?.defaultModel?.values)) {
    return component?.storedModel || component?.model;
  }
  const defaults = (component.defaultModel.values || []).filter(value => value.selected);
  const keys = new Set(defaults.map(value => String(value.value)));
  const values: StateRow[] = (component.model?.values || []).map(value => ({ ...value, selected: keys.has(String(value.value)) }));
  const present = new Set(values.map(value => String(value.value)));
  return {
    ...component.model,
    values: [...values, ...defaults.filter(value => !present.has(String(value.value))).map(value => ({ ...value }))]
  };
}

/**
 * Build the restore update for a component model.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @param {boolean} initial True to restore the first loaded model instead of the default one
 * @returns {Object} State diff for the restore
 */
export function getRestoreModelComponent(
  state: ComponentsState,
  componentId: string,
  settings?: ReducerSettings,
  initial = false
): ComponentsState {

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    const source = getRestoredModel(currentFull, initial);
    currentFull.model = {
      ...source,
      values: (source?.values || []).map(value => ({ ...value })),
      changed: false
    };

    return {
      [componentId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  if (!state[componentId]) return {};

  const source = getRestoredModel(state[componentId] as StateComponent, initial);
  return {
    [componentId]: {
      ...state[componentId],
      model: {
        ...source,
        values: (source?.values || []).map(value => ({ ...value })),
        changed: false
      }
    }
  };
}

/**
 * Reset a component model to an empty/default state.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @returns {Object} Updated state
 */
export function resetModel(state: ComponentsState, address: ComponentAddress): ComponentsState {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);

  let emptyModel: StateModel;
  if (!isGridComponent(component)) {
    emptyModel = {
      values: (component?.model?.values || []).map(value => ({ ...value, selected: false }))
    };
  } else {
    emptyModel = {
      values: [],
      page: 1,
      total: 1,
      records: 0
    };
  }

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    currentFull.attributes = {
      ...currentFull?.attributes,
      error: null
    };
    currentFull.model = {
      ...currentFull?.model,
      ...emptyModel,
      changed: true
    };

    return {
      ...state,
      [componentId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...state[componentId]?.attributes,
        error: null
      },
      model: {
        ...state[componentId]?.model,
        ...emptyModel,
        changed: true
      }
    }
  };
}

/**
 * Handle UPDATE_MODEL actions, including selection and footer refresh.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateModelAction(
  state: ComponentsState = {},
  action: {address: ComponentAddress; data?: StateModel} = NO_ACTION,
  settings?: ReducerSettings
): ComponentsState {
  let modelState = state;
  const data: StateModel = { ...action.data };
  const selectedData = [...asArray(data.selected)];

  // Update model if there are properties other than just 'selected'
  if (Object.keys(data).length > 1 || !('selected' in data)) {
    modelState = launchAddressFunction(updateCellModel, updateColumnModel,
      (s, address, model) => updateModel(s, address, model!, true), modelState, {
      address: action.address,
      data
    }, settings);
  }

  // Update selected if present
  if ("selected" in data) {
    modelState = launchAddressFunction(updateCellSelected, (s) => s, updateSelected, modelState, {
      address: action.address,
      data: { selected: selectedData },
    }, settings);
  }

  // Update footer if is grid and show totals
  const componentId = resolveComponentId(action.address);
  const component = getMergedComponent(componentId, modelState);

  if (hasFooter(component)) {
    modelState = updateGridFooter(modelState, action.address);
  }

  return modelState;
}

/**
 * Model action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const modelHandlers: ActionHandlers<ModelAction> = {
  [UPDATE_MODEL]: (state, action) => updateModelAction(state, action, action.settings),

  [UPDATE_MULTIPLE_MODELS]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateModelAction(newState, _action, action.settings), state),

  [KEEP_MODEL]: (state, action) =>
    keepModelComponent(state, resolveComponentId(action.address), action.settings, !!action.data?.initial, action.data?.defaultModel),

  [RESTORE_MODEL]: (state, action) =>
    restoreModelComponent(state, resolveComponentId(action.address), action.settings, action.initial),

  [RESTORE_MULTIPLE_MODEL]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreModelComponent(newState, resolveComponentId(_action.address), action.settings, action.initial),
      state
    ),

  [RESET_MODEL]: (state, action) =>
    launchAddressFunction(resetCellModel, (s) => s, resetModel, state, { address: action.address, data: [] }, action.settings),

  [RESET_MULTIPLE_MODEL]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => launchAddressFunction(resetCellModel, (s) => s, resetModel, newState, { address: _action.address, data: [] }, action.settings),
      state
    ),
};
