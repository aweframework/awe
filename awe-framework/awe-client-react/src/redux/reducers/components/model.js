import {
  KEEP_MODEL,
  RESET_MODEL,
  RESET_MULTIPLE_MODEL,
  RESTORE_MODEL,
  RESTORE_MULTIPLE_MODEL,
  UPDATE_MODEL,
  UPDATE_MULTIPLE_MODELS
} from '../../actions/components';
import _ from 'lodash';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import {asArray} from "../../../utilities";
import {warnMalformedComponent} from "../../../utilities/components";
import {isEmpty} from "../../../utilities/general";
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
  memoizedGetComponentId,
  updateComponentInState
} from './stateHelpers';

/**
 * Update or register a component and its model/attributes.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} data Component data or partial update
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateComponentData(state, componentId, data, settings) {
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
  return updateComponentInState(state, componentId, data, settings);
}

/**
 * Update a model at component/column/cell scope depending on address.
 * @param {Object} state Current state
 * @param {Object} address Target address
 * @param {Object} data Model updates
 * @param {boolean} update Whether to update grid cell models
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateModel(state, address, data, update = true, settings) {
  let componentId = memoizedGetComponentId(address);
  const component = getMergedComponent(componentId, state);

  if (isEmpty(component)) return state;

  let newModel;
  if (isGridComponent(component) && data.values && update) {
    newModel = getGridModelUpdate(state, address, data, update, settings);
  } else if (isGridComponent(component) && data.selected) {
    newModel = updateSelectedGrid(state, component.model?.values || [], address, data, settings);
  } else if (isGroup(component)) {
    newModel = getGroupModelUpdate(state, address?.view, component.attributes?.group, data, settings);
  } else {
    newModel = getModelUpdate(state, address, data, settings);
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
 * @param {Object} settings Settings
 * @returns {Object|null} Updated state or null if no changes
 */
export function updateSelectedComponent(state, values, address, selected, settings) {
  const componentId = memoizedGetComponentId(address);
  const component = getMergedComponent(componentId, state);

  if (!component) return state;

  let filtered = getFilteredValues(values, selected);
  if (_.isEqual(filtered, (component.model?.values))) {
    return null;
  }

  // Update values
  return getModelUpdate(state, address, { values: filtered }, settings);
}

/**
 * Update selection at the correct scope for a component.
 * @param {Object} state Current state
 * @param {Object} address Target address
 * @param {Object} data Selection payload
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateSelected(state, address, data, settings) {
  let update = getSelectedUpdate(state, address, data, settings);
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
 * @param {Object} settings Settings
 * @returns {Object|null} Update to apply
 */
export function getSelectedUpdate(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const component = getMergedComponent(componentId, state);

  let values = (component?.model?.values) || [];
  let selected = asArray(data.selected);
  if (isGridComponent(component)) {
    return updateSelectedGrid(state, values, address, { selected }, settings);
  } else if (isGroup(component)) {
    return updateSelectedGroup(state, values, address?.view, component.attributes?.group, selected, settings);
  } else {
    return updateSelectedComponent(state, values, address, selected, settings);
  }
}

/**
 * Store the current model snapshot for a component.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function keepModelComponent(state, componentId, settings, initial = false, defaultModel = null) {
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
export function restoreModelComponent(state, componentId, settings, initial = false) {
  const newState = {
    ...state,
    ...getRestoreModelComponent(state, componentId, settings, initial)
  };

  const component = getMergedComponent(componentId, newState);

  // Update footer if is grid and show totals
  if (hasFooter(component)) {
    return updateGridFooter(newState, componentId, settings);
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
export function getRestoredModel(component, initial) {
  // The component registry merges a missing default model into an empty object: treat it as absent
  if (initial || isGridComponent(component) || !Array.isArray(component?.defaultModel?.values)) {
    return component?.storedModel || component?.model;
  }
  const defaults = (component.defaultModel.values || []).filter(value => value.selected);
  const keys = new Set(defaults.map(value => String(value.value)));
  const values = (component.model?.values || []).map(value => ({ ...value, selected: keys.has(String(value.value)) }));
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
export function getRestoreModelComponent(state, componentId, settings, initial = false) {

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeComponentState(base, state[componentId]);
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

  const source = getRestoredModel(state[componentId], initial);
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
 * @param {Object} data Reset data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function resetModel(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const component = getMergedComponent(componentId, state);

  let emptyModel;
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
    const currentFull = mergeComponentState(base, state[componentId]);
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
export function updateModelAction(state = {}, action = {}, settings) {
  let modelState = state;
  let data = { ...action.data };
  let selectedData = [...asArray(data.selected)];

  // Update model if there are properties other than just 'selected'
  if (Object.keys(data).length > 1 || !('selected' in data)) {
    modelState = launchAddressFunction(updateCellModel, updateColumnModel, updateModel, modelState, {
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
  const componentId = memoizedGetComponentId(action.address);
  const component = getMergedComponent(componentId, modelState);

  if (hasFooter(component)) {
    modelState = updateGridFooter(modelState, action.address, settings);
  }

  return modelState;
}

/**
 * Model action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const modelHandlers = {
  [UPDATE_MODEL]: (state, action) => updateModelAction(state, action, action.settings),

  [UPDATE_MULTIPLE_MODELS]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateModelAction(newState, _action, action.settings), state),

  [KEEP_MODEL]: (state, action) =>
    keepModelComponent(state, memoizedGetComponentId(action.address), action.settings, !!action.data?.initial, action.data?.defaultModel),

  [RESTORE_MODEL]: (state, action) =>
    restoreModelComponent(state, memoizedGetComponentId(action.address), action.settings, action.initial),

  [RESTORE_MULTIPLE_MODEL]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreModelComponent(newState, memoizedGetComponentId(_action.address), action.settings, action.initial),
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
