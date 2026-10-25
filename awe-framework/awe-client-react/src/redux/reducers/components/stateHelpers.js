import _ from 'lodash';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import {ComponentAddressType, getAddressType, getComponentId} from "../../../utilities/components";
import {getGridIdentifier} from "../../../utilities/grid";
import {isEmpty} from "../../../utilities/general";

const { ADDRESS_CELL, ADDRESS_COLUMN, ADDRESS_COMPONENT } = ComponentAddressType;

/**
 * ============================================================================
 * IMMUTABLE STATE UPDATE UTILITIES
 * ============================================================================
 * These helper functions provide a cleaner, more readable way to update
 * nested state properties immutably, reducing code duplication.
 */

/**
 * Update a component entry by merging new data (registry-aware).
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} updates Partial data to merge into the component
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateComponentInState(state, componentId, updates, settings) {
  const base = ComponentRegistry.get(componentId);
  if (base) {
    const replaceObjectKeys = new Set(["validationRules", "storedValidationRules"]);
    // Get current merged state (base + existing delta)
    const currentFull = mergeComponentState(base, state[componentId]);
    // Apply updates to the full state (arrays replace instead of merging)
    const updatedFull = _.mergeWith({}, currentFull, updates, (objValue, srcValue, key) => {
      if (Array.isArray(srcValue)) {
        return srcValue;
      }
      if (replaceObjectKeys.has(key)) {
        return srcValue;
      }
      return undefined;
    });
    // Calculate new delta against base
    const newDelta = calculateDeltas(base, updatedFull);
    return {
      ...state,
      [componentId]: newDelta
    };
  }

  // Component not registered: operate directly on the Redux entry
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      ...updates
    }
  };
}

/**
 * Merge updates into a component property bag (e.g. attributes, validationRules).
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {string} propertyName Target property name
 * @param {Object} updates Partial updates for the property
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateComponentProperty(state, componentId, propertyName, updates, settings) {
  const base = ComponentRegistry.get(componentId);
  if (base) {
    // Get current merged state
    const currentFull = mergeComponentState(base, state[componentId]);
    // Apply property update
    currentFull[propertyName] = mergeComponentPropertyValue(propertyName, currentFull?.[propertyName], updates);
    // Calculate new delta
    const newDelta = calculateDeltas(base, currentFull);
    return {
      ...state,
      [componentId]: newDelta
    };
  }

  // Component not registered: operate directly on the Redux entry
  if (!state[componentId]) return state;
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      [propertyName]: mergeComponentPropertyValue(propertyName, state[componentId][propertyName], updates)
    }
  };
}

/**
 * Merge updates for a component property while preserving nested numberFormat defaults.
 * @param {string} propertyName Target property name
 * @param {Object} currentValue Current property value
 * @param {Object} updates Partial updates
 * @returns {Object} Merged property value
 */
export function mergeComponentPropertyValue(propertyName, currentValue = {}, updates = {}) {
  if (propertyName !== 'attributes') {
    return {
      ...currentValue,
      ...updates
    };
  }

  const hasNumberFormatUpdate = Object.prototype.hasOwnProperty.call(updates, 'numberFormat')
    && typeof updates.numberFormat === 'object'
    && updates.numberFormat !== null;

  if (!hasNumberFormatUpdate) {
    return {
      ...currentValue,
      ...updates
    };
  }

  return {
    ...currentValue,
    ...updates,
    numberFormat: {
      ...(currentValue?.numberFormat || {}),
      ...updates.numberFormat
    }
  };
}

/**
 * Update component attributes.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} attributeUpdates Attribute changes
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateComponentAttributes(state, componentId, attributeUpdates, settings) {
  return updateComponentProperty(state, componentId, 'attributes', attributeUpdates, settings);
}

/**
 * Update component validation rules.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} validationUpdates Validation rule changes
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateComponentValidation(state, componentId, validationUpdates, settings) {
  return updateComponentProperty(state, componentId, 'validationRules', validationUpdates, settings);
}

/**
 * Get the merged component (base + delta).
 * @param {string} componentId Component identifier
 * @param {Object} state Current state
 * @returns {Object|undefined} Merged component, or undefined when the component is neither registered nor in the state
 */
export function getMergedComponent(componentId, state) {
  const base = ComponentRegistry.get(componentId);
  const delta = state[componentId];
  if (!base && !delta) return undefined;
  return mergeComponentState(base, delta);
}

/**
 * ============================================================================
 * COMPONENT TYPE CHECKS
 * ============================================================================
 */

/**
 * Check whether a component is a grid.
 * @param {Object} component Component to inspect
 * @returns {boolean} True if the component has a column model
 */
export function isGridComponent(component) {
  return !isEmpty(component) && "columnModel" in (component.attributes || {});
}

/**
 * Check whether a grid component has a footer.
 * @param {Object} component Component to inspect
 * @returns {boolean} True when grid totals footer is enabled
 */
export function hasFooter(component) {
  return isGridComponent(component) && (component.attributes || {}).showTotals;
}

/**
 * Check whether a component belongs to a radio group.
 * @param {Object} component Component to inspect
 * @returns {boolean} True if it is a grouped radio component
 */
export function isGroup(component) {
  let attributes = component.attributes || {};
  return "group" in attributes && (attributes.component || "").includes("radio");
}

/**
 * Route a model/action update to the proper handler by address type.
 * @param {Function} cellFunction Handler for cell-level updates
 * @param {Function} columnFunction Handler for column-level updates
 * @param {Function} componentFunction Handler for component-level updates
 * @param {Object} state Current state
 * @param {Object} action Action payload with address/data
 * @param {Object} settings Settings
 * @returns {*} Handler result or unchanged state
 */
export function launchAddressFunction(cellFunction, columnFunction, componentFunction, state, action, settings) {
  switch (getAddressType(action.address)) {
    case ADDRESS_CELL:
      return cellFunction(state, action.address, action.data, settings);
    case ADDRESS_COLUMN:
      return columnFunction(state, action.address, action.data, settings);
    case ADDRESS_COMPONENT:
      return componentFunction(state, action.address, action.data, settings);
    default:
      return state;
  }
}

/**
 * ============================================================================
 * GRID ROW HELPERS
 * ============================================================================
 */

export const memoizedGetComponentId = _.memoize(getComponentId);
export const memoizedGetGridIdentifier = _.memoize(getGridIdentifier);

/**
 * Resolve grid row context from an address.
 * @param {Object} state Current state
 * @param {Object} address Address with component/row/column
 * @param {Object} settings Settings
 * @returns {Object|null} Grid row context or null when not found
 */
export function getGridRowContext(state = {}, address = {}, settings = {}) {
  const componentId = address.component;
  const base = ComponentRegistry.get(componentId);

  if (!base) return null;

  const component = mergeComponentState(base, state[componentId]);

  if (!component) return null;
  const { model, attributes } = component;
  if (!model || !attributes) return null;

  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) return null;

  const rowData = values[rowIndex] || {};

  return {
    componentId,
    base,
    component,
    model,
    attributes,
    values,
    rowIndex,
    rowData
  };
}

/**
 * Update grid row values using a resolved context.
 * @param {Object} state Current state
 * @param {Object} context Grid row context from getGridRowContext
 * @param {Array} nextValues Updated grid values array
 * @returns {Object} Updated state
 */
export function updateGridRowValues(state, context, nextValues) {
  const { componentId, base, component } = context;

  const currentFull = {
    ...component,
    model: {
      ...component?.model,
      values: nextValues
    }
  };

  return {
    ...state,
    [componentId]: calculateDeltas(base, currentFull)
  };
}

/**
 * Build a component model update at component scope.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} model Model updates
 * @param {Object} settings Settings
 * @returns {Object} Component update map
 */
export function getModelUpdate(state, address, model, settings) {
  let componentId = memoizedGetComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    // Get current merged state
    const currentFull = mergeComponentState(base, state[componentId]);
    // Apply model update
    currentFull.model = {
      ...currentFull?.model,
      ...model,
      changed: true
    };
    // Reset error on update
    currentFull.attributes = {
      ...currentFull?.attributes,
      error: null
    };

    // Calculate new delta
    const newDelta = calculateDeltas(base, currentFull);
    return {
      [componentId]: newDelta
    };
  }

  // Component not registered: operate directly on the Redux entry
  return {
    [componentId]: {
      ...state[componentId],
      model: {
        ...state[componentId]?.model,
        ...model,
        changed: true
      },
      attributes: {
        ...state[componentId]?.attributes,
        error: null
      }
    }
  };
}

/**
 * Build the model snapshot update for a component.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @param {boolean} initial True when the snapshot is the initial model, so it is also the default one (restore)
 * @param {Object} defaultModel Default model to store with the initial one (the snapshot itself when it is not given)
 * @returns {Object} State diff for the snapshot
 */
export function getKeepModelComponent(state, componentId, settings, initial = false, defaultModel = null) {
  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeComponentState(base, state[componentId]);
    currentFull.storedModel = {
      ...currentFull?.model,
      values: (currentFull.model?.values || []).map(value => ({ ...value }))
    };
    if (initial) {
      const source = defaultModel || currentFull.storedModel;
      currentFull.defaultModel = { ...source, values: (source.values || []).map(value => ({ ...value })) };
    }
    return {
      [componentId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  if (!state[componentId]) return {};
  const storedModel = {
    ...state[componentId].model,
    values: state[componentId].model.values.map(value => ({ ...value }))
  };
  return {
    [componentId]: {
      ...state[componentId],
      storedModel,
      ...(initial ? { defaultModel: { ...(defaultModel || storedModel), values: ((defaultModel || storedModel).values || []).map(value => ({ ...value })) } } : {})
    }
  };
}
