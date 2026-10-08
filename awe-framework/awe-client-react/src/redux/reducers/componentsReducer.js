import {
  CLEAR_ALL_COMPONENTS,
  CLEAR_COMPONENTS,
  KEEP_ATTRIBUTE,
  KEEP_MODEL,
  KEEP_ROW_MODEL,
  KEEP_VALIDATION,
  RESET_MODEL,
  RESET_MULTIPLE_MODEL,
  RESTORE_ATTRIBUTE,
  RESTORE_MODEL,
  RESTORE_MULTIPLE_ATTRIBUTES,
  RESTORE_MULTIPLE_MODEL,
  RESTORE_MULTIPLE_VALIDATION,
  RESTORE_VALIDATION,
  UPDATE_ATTRIBUTES,
  UPDATE_COMPONENT,
  UPDATE_MODEL,
  UPDATE_MULTIPLE_ATTRIBUTES,
  UPDATE_MULTIPLE_COMPONENTS,
  UPDATE_MULTIPLE_MODELS,
  UPDATE_MULTIPLE_VALIDATION,
  UPDATE_ROW_MODEL,
  UPDATE_SPECIFIC_ATTRIBUTES,
  UPDATE_VALIDATION,
  UPDATE_VIEW_COMPONENTS,
  VALIDATE_COMPONENTS,
  VALIDATE_ROW,
} from '../actions/components';
import ComponentRegistry from '../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../utilities/mergeUtils';

import {
  calculateFooterValue,
  extractCellModel,
  extractCellValue,
  getCellModel,
  getGridIdentifier
} from "../../utilities/grid";

import _ from 'lodash';
import {validateComponent, validateRow} from "./validation";
import {asArray, updateArrayElement} from "../../utilities";
import {ComponentAddressType, getAddressType, getComponentId, warnMalformedComponent} from "../../utilities/components";
import {getFirstDefinedValue, isEmpty, isMultipleComponent} from "../../utilities/general";

const { ADDRESS_CELL, ADDRESS_COLUMN, ADDRESS_COMPONENT } = ComponentAddressType;

const memoizedGetComponentId = _.memoize(getComponentId);
const memoizedGetGridIdentifier = _.memoize(getGridIdentifier);

const InitialState = {};

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
function updateComponentInState(state, componentId, updates, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
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
  }

  // Legacy behavior
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
function updateComponentProperty(state, componentId, propertyName, updates, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
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
  }

  // Legacy behavior
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
function mergeComponentPropertyValue(propertyName, currentValue = {}, updates = {}) {
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
function updateComponentAttributes(state, componentId, attributeUpdates, settings) {
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
function updateComponentValidation(state, componentId, validationUpdates, settings) {
  return updateComponentProperty(state, componentId, 'validationRules', validationUpdates, settings);
}

/**
 * Resolve grid row context from an address.
 * @param {Object} state Current state
 * @param {Object} address Address with component/row/column
 * @param {Object} settings Settings
 * @returns {Object|null} Grid row context or null when not found
 */
function getGridRowContext(state = {}, address = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  const base = useRegistry ? ComponentRegistry.get(componentId) : null;

  if (useRegistry && !base) return null;

  const component = useRegistry
    ? mergeComponentState(base, state[componentId])
    : state[componentId];

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
    useRegistry,
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
function updateGridRowValues(state, context, nextValues) {
  const { componentId, useRegistry, base, component } = context;

  if (useRegistry) {
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

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      model: {
        ...state[componentId].model,
        values: nextValues
      }
    }
  };
}

/**
 * Get the merged component (base + delta) when using the registry.
 * @param {boolean} useRegistry Whether the component registry is enabled
 * @param {string} componentId Component identifier
 * @param {Object} state Current state
 * @returns {Object|undefined} Merged component or undefined
 */
function getMergedComponent(useRegistry, componentId, state) {
  let component;
  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId]);
  } else {
    component = state[componentId];
  }
  return component;
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
function isGridComponent(component) {
  return !isEmpty(component) && "columnModel" in (component.attributes || {});
}

/**
 * Check whether a grid component has a footer.
 * @param {Object} component Component to inspect
 * @returns {boolean} True when grid totals footer is enabled
 */
function hasFooter(component) {
  return isGridComponent(component) && (component.attributes || {}).showTotals;
}

/**
 * Check whether a component belongs to a radio group.
 * @param {Object} component Component to inspect
 * @returns {boolean} True if it is a grouped radio component
 */
function isGroup(component) {
  let attributes = component.attributes || {};
  return "group" in attributes && (attributes.component || "").includes("radio");
}

/**
 * Normalize a cell model by applying selected values.
 * @param {Array|Object} selected Selected data
 * @param {Object} model Existing cell model definition
 * @returns {Object} Normalized cell model
 */
function fixCellModel(selected, model) {
  let selectedData = [...asArray(selected)];
  if (model && model.values && model.values.length) {
    let selectedString = selectedData.map(value => String(extractCellValue(value)));
    return {
      ...model,
      values: model.values.map(value => ({ ...value, selected: selectedString.includes(String(value.value)) }))
    };
  } else {
    return {
      values: selectedData.reduce((prev, value) => ({ ...extractCellModel(value) }), {})
    };
  }
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
function launchAddressFunction(cellFunction, columnFunction, componentFunction, state, action, settings) {
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
 * Update component attributes at component scope.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} data Attribute updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateAttributeComponent(state = {}, address = {}, data = {}, settings) {
  return updateComponentAttributes(state, address.component, data, settings);
}

/**
 * Merge a partial number format into the one of a column, so a format that only changes the sign or the decimals keeps
 * the separators the column was created with.
 * @param {Object} column Current column
 * @param {Object} data Column attribute changes
 * @returns {Object} Column attribute changes
 */
function mergeColumnNumberFormat(column = {}, data = {}) {
  const hasNumberFormatUpdate = typeof data.numberFormat === 'object' && data.numberFormat !== null;
  return hasNumberFormatUpdate ? {...data, numberFormat: {...(column.numberFormat || {}), ...data.numberFormat}} : data;
}

/**
 * Update column attributes inside a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Column attribute updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateAttributeColumn(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(col => col.name === address.column);

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, mergeColumnNumberFormat(columnModel[columnIndex], data))
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  const { attributes } = state[componentId] || {};
  if (!attributes) return state;
  const columnModel = attributes.columnModel || [];
  const columnIndex = columnModel.findIndex(col => col.name === address.column);

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, mergeColumnNumberFormat(columnModel[columnIndex], data))
      }
    }
  };
}

/**
 * Update cell attributes stored in the grid row $attrs.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Cell attribute updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateAttributeCell(state = {}, address = {}, data = {}, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData?.$attrs,
      [address.column]: { ...cellAttrs, ...data }
    }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Dispatch attribute updates based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateAttributeAction(state = {}, action = {}, settings) {
  const nextState = launchAddressFunction(updateAttributeCell, updateAttributeColumn, updateAttributeComponent, state, action, settings);
  const baseAddress = action.address?.component ? { view: action.address.view, component: action.address.component } : action.address;
  const componentId = memoizedGetComponentId(baseAddress);
  if (!componentId) return nextState;
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, nextState);

  if ( !isGridComponent(component) ) return nextState;
  return hasFooter(component) ? updateGridFooter(nextState, baseAddress, settings) : nextState;
}

/**
 * Update component-specific attributes (stored separately from normal attributes).
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateSpecificAttributes(state = {}, action = {}, settings) {
  const { address, data } = action;
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      currentFull.specificAttributes = {
        ...currentFull?.specificAttributes,
        ...data
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  return !(componentId in state) ? state : {
    ...state,
    [componentId]: {
      ...state[componentId],
      specificAttributes: {
        ...state[componentId].specificAttributes,
        ...data
      }
    }
  };
}

/**
 * Snapshot a single attribute value for later restore.
 * @param {Object} state Current state
 * @param {string} component Component identifier
 * @param {string} data Attribute name to keep
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function keepAttributeComponent(state, component, data, settings) {
  if (!state[component]) return state;

  return updateComponentInState(state, component, {
    storedAttributes: {
      ...state[component].storedAttributes,
      [data]: {
        ...state[component].attributes[data]
      }
    }
  }, settings);
}

/**
 * Restore a previously kept component attribute.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {string} data Attribute name to restore
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreAttributeComponent(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      currentFull.attributes = {
        ...currentFull.attributes,
        [data]: currentFull.storedAttributes?.[data]
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...state[componentId].attributes,
        [data]: state[componentId].storedAttributes[data]
      }
    }
  };
}

/**
 * Restore a previously kept column attribute.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {string} data Attribute name to restore
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreAttributeColumn(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(column => column.name === address.column);
      if (columnIndex < 0) return state;

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, { [data]: currentFull.storedAttributes?.columnModel?.[columnIndex]?.[data] })
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  const { attributes } = state[componentId] || {};
  if (!attributes) return state;
  const columnModel = attributes.columnModel || [];
  const columnIndex = columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, { [data]: state[componentId].storedAttributes.columnModel[columnIndex][data] })
      }
    }
  };
}

/**
 * Restore a previously kept cell attribute.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {string} data Attribute name to restore
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreAttributeCell(state, address, data, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      const { model, attributes, storedAttributes = {} } = currentFull;
      const { values } = model;
      const gridId = memoizedGetGridIdentifier(attributes);
      const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
      if (rowIndex < 0) return state;

      const columnIndex = (attributes.columnModel || []).findIndex(column => column.name === address.column);
      const storedColumn = storedAttributes.columnModel?.[columnIndex] || {};
      const defaultValue = storedColumn?.[data];
      const rowData = values[rowIndex] || {};
      const cellAttrs = rowData.$attrs?.[address.column] || {};
      const prevAttrs = rowData.$attrs || {};

      let newAttr;
      if (typeof defaultValue !== 'undefined') {
        newAttr = { ...cellAttrs, [data]: defaultValue };
      } else {
        const { [data]: _removed, ...rest } = cellAttrs;
        newAttr = rest;
      }

      currentFull.model = {
        ...model,
        values: updateArrayElement(values, rowIndex, { ...rowData, $attrs: { ...prevAttrs, [address.column]: newAttr } })
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  const { model, attributes, storedAttributes = {} } = state[componentId] || {};
  if (!model || !attributes) return state;
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) {
    return state;
  }
  const columnIndex = (attributes.columnModel || []).findIndex(column => column.name === address.column);
  const storedColumn = storedAttributes.columnModel?.[columnIndex] || {};
  const defaultValue = storedColumn?.[data];
  const rowData = values[rowIndex] || {};
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const prevAttrs = rowData.$attrs || {};

  let newAttr;
  if (typeof defaultValue !== 'undefined') {
    newAttr = {
      ...cellAttrs,
      [data]: defaultValue
    };
  } else {
    const { [data]: _removed, ...rest } = cellAttrs;
    newAttr = rest;
  }

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      model: {
        ...state[componentId].model,
        values: updateArrayElement(values, rowIndex, { ...rowData, $attrs: { ...prevAttrs, [address.column]: newAttr } })
      }
    }
  };
}

/**
 * Restore attributes based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreAttributeAction(state = {}, action = {}, settings) {
  return launchAddressFunction(restoreAttributeCell, restoreAttributeColumn, restoreAttributeComponent, state, action, settings);
}

/**
 * Dispatch validation updates based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateValidationAction(state = {}, action = {}, settings) {
  return launchAddressFunction(updateValidationCell, updateValidationColumn, updateValidationComponent, state, action, settings);
}

/**
 * Restore validation rules based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreValidationAction(state = {}, action = {}, settings) {
  return launchAddressFunction(restoreValidationCell, restoreValidationColumn, restoreValidationComponent, state, action, settings);
}

/**
 * Update component-level validation rules.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} data Validation rule updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateValidationComponent(state = {}, address = {}, data = {}, settings) {
  return updateComponentValidation(state, address.component, data, settings);
}

/**
 * Update column-level validation rules in a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Validation rule updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateValidationColumn(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(col => col.name === address.column);
      if (columnIndex < 0) return state;

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, {
          validationRules: {
            ...columnModel[columnIndex]?.validationRules,
            ...data
          }
        })
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  const { attributes } = state[componentId] || {};
  if (!attributes) return state;
  const columnModel = attributes.columnModel || [];
  const columnIndex = columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, { validationRules: { ...columnModel[columnIndex]?.validationRules, ...data } })
      }
    }
  };
}

/**
 * Update cell-level validation rules stored in $attrs.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Validation rule updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateValidationCell(state = {}, address = {}, data = {}, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const currentValidation = cellAttrs.validationRules || {};
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData?.$attrs,
      [address.column]: {
        ...cellAttrs,
        validationRules: {
          ...currentValidation,
          ...data
        }
      }
    }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Snapshot validation rules for later restore.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function keepValidationComponent(state, componentId, settings) {
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

  return updateComponentInState(state, componentId, {
    storedValidationRules: {
      ...component?.validationRules
    }
  }, settings);
}

/**
 * Restore previously kept validation rules.
 * @param {Object} state Current state
 * @param {Object} address Component addres
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreValidationComponent(state, address, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);
  if (typeof component?.storedValidationRules === "undefined") {
    return state;
  }

  return updateComponentInState(state, componentId, {
    validationRules: {
      ...component?.storedValidationRules
    }
  }, settings);
}

/**
 * Restore column-level validation rules in a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreValidationColumn(state = {}, address = {}, _data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  const buildNextColumn = (columnModel = [], storedAttributes = {}) => {
    const columnIndex = columnModel.findIndex(column => column.name === address.column);
    if (columnIndex < 0) return null;

    const storedColumn = storedAttributes.columnModel?.[columnIndex] || {};
    if (typeof storedColumn.validationRules !== "undefined") {
      return {
        columnIndex,
        nextColumn: {
          ...columnModel[columnIndex],
          validationRules: storedColumn.validationRules
        }
      };
    }

    return {
      columnIndex,
      nextColumn: {
        ...columnModel[columnIndex],
        validationRules: undefined
      }
    };
  };

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      const { attributes, storedAttributes = {} } = currentFull;
      const { columnModel = [] } = attributes;
      const result = buildNextColumn(columnModel, storedAttributes);
      if (!result) return state;
      const { columnIndex, nextColumn } = result;

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, nextColumn)
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  const { attributes, storedAttributes = {} } = state[componentId] || {};
  if (!attributes) return state;
  const { columnModel = [] } = attributes;
  const result = buildNextColumn(columnModel, storedAttributes);
  if (!result) return state;
  const { columnIndex, nextColumn } = result;

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, nextColumn)
      }
    }
  };
}

/**
 * Restore cell-level validation rules stored in $attrs.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function restoreValidationCell(state = {}, address = {}, _data = {}, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  if (!("validationRules" in cellAttrs)) {
    return state;
  }

  const { validationRules: _removed, ...restCellAttrs } = cellAttrs;
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData?.$attrs,
      [address.column]: restCellAttrs
    }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update or register a component and its model/attributes.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} data Component data or partial update
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateComponentData(state, componentId, data, settings) {
  if (componentId == null) {
    warnMalformedComponent(data, {
      origin: 'reducer',
      operation: 'updateComponentData',
      extra: {
        useComponentRegistry: !!settings?.useComponentRegistry
      }
    });
    return state;
  }

  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
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
  }

  // Comportamiento normal o actualización parcial
  return updateComponentInState(state, componentId, data, settings);
}

/**
 * Update child cell models for a grid based on row values.
 * @param {Object} state Current state
 * @param {Object} grid Grid component
 * @param {Object} settings Settings
 * @returns {Object} Updated state for cell components
 */
function updateCellsModel(state, grid, settings) {
  const { values } = grid.model;
  let columns = grid.attributes?.columnModel?.filter(column => column.component) || [];
  let cellComponents = {};
  const gridId = memoizedGetGridIdentifier(grid.attributes);
  const useRegistry = settings?.useComponentRegistry;
  values
    .forEach(row => columns
      .filter(column => {
        const cellComponentId = memoizedGetComponentId({ ...grid.address, row: row[gridId], column: column.name });
        return cellComponentId in state || (useRegistry && ComponentRegistry.get(cellComponentId));
      })
      .forEach(column => Object.assign(cellComponents,
        getModelUpdate(state,
          { ...grid.address, row: row[gridId], column: column.name },
          fixCellModel(row[column.name], column.model),
          settings
        )
      ))
    );
  return cellComponents;
}

/**
 * Build a component model update at component scope.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} model Model updates
 * @param {Object} settings Settings
 * @returns {Object} Component update map
 */
function getModelUpdate(state, address, model, settings) {
  let componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
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
  }

  // Legacy behavior
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
 * Build model updates for all components in a radio group.
 * @param {Object} state Current state
 * @param {string} view View identifier
 * @param {string} group Group identifier
 * @param {Object} model Model containing values/selection
 * @param {Object} settings Settings
 * @returns {Object} Component update map
 */
function getGroupModelUpdate(state, view, group, model, settings) {
  // Check equality to avoid update state if there are no changes
  const selected = model.values.map(item => item.value);
  return updateSelectedGroup(state, model.values, view, group, selected, settings);
}

/**
 * Build a grid model update and optionally refresh cell models.
 * @param {Object} state Current state
 * @param {Object} address Grid address
 * @param {Object} model Model updates
 * @param {boolean} update Whether to update cell models
 * @param {Object} settings Settings
 * @returns {Object} Component update map
 */
function getGridModelUpdate(state, address, model, update = true, settings) {
  let componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);
  if (!component) return state;

  const nextModelVersion = (component?.model?.modelVersion || 0) + 1;
  const modelWithVersion = {
    ...model,
    modelVersion: nextModelVersion
  };
  let gridComponent = getModelUpdate(state, address, modelWithVersion, settings);

  // Para updateCellsModel necesitamos el componente mergeado con los cambios que acabamos de calcular
  const updatedFull = mergeComponentState(component, gridComponent[componentId]);

  let cellsState = model.values && update ? updateCellsModel({ ...state, ...gridComponent }, updatedFull, settings) : {};
  return {
    ...gridComponent,
    ...cellsState
  };
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
function updateModel(state, address, data, update = true, settings) {
  let componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

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
 * Update a column model definition inside a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Model updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateColumnModel(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(column => column.name === address.column);

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, {
          model: {
            ...columnModel[columnIndex]?.model,
            ...data
          }
        })
      };

      return {
        ...state,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  const { attributes } = state[componentId] || {};
  if (!attributes) return state;
  const columnModel = attributes.columnModel || [];
  const columnIndex = columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, {
          model: {
            ...columnModel[columnIndex]?.model,
            ...data
          }
        })
      }
    }
  };
}

/**
 * Update a single cell model and clear cell error.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Model updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateCellModel(state, address, data, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData, attributes } = context;
  const column = attributes.columnModel?.find(column => column.name === address.column) || {};
  const rawValue = getFirstDefinedValue(data.values, values[rowIndex][address.column]);
  const isMultiple = isMultipleComponent(column.component);
  const newData = isMultiple
    ? (Array.isArray(rawValue) ? rawValue : [])
    : getCellModel(rawValue, column);
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const { error, ...otherAttrs } = cellAttrs;
  const nextValues = updateArrayElement(values, rowIndex, {
    [address.column]: newData,
    $attrs: { ...rowData.$attrs, [address.column]: otherAttrs }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update selected values inside a cell model.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Selection payload
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateCellSelected(state, address, data, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex } = context;
  const sameValue = checkSelected(data.selected, values[rowIndex][address.column]);
  if (sameValue) return state;

  const nextValues = updateArrayElement(values, rowIndex, {
    [address.column]: fixCellModel(data.selected, values[rowIndex][address.column]).values
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update selected rows in a grid by ids.
 * @param {Object} state Current state
 * @param {Array} values Grid values
 * @param {Object} address Grid address
 * @param {Object} data Selection payload
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateSelectedGrid(state, values, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

  if (!component || !component.attributes) return state;

  const { selected = [] } = data;
  const gridId = memoizedGetGridIdentifier(component.attributes);
  let filtered = [...values];
  let toUnselect = values
    .filter(row => row.selected)
    .filter(row => !selected.map(String).includes(String(row[gridId])))
    .map(row => ({ id: row[gridId], selected: false }));
  let toSelect = selected
    .map(value => ({ id: value, selected: true }));
  // Unselect values
  [...toUnselect, ...toSelect].forEach(item => {
    let index = filtered.findIndex((row) => String(row[gridId]) === String(item.id));
    // Safeguard for index not found
    if (index >= 0) {
      filtered = updateArrayElement(filtered, index, { selected: item.selected });
    }
  });

  // Update values
  return getGridModelUpdate(state, address, { ...data, values: filtered }, false, settings);
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
function updateSelectedComponent(state, values, address, selected, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

  if (!component) return state;

  let filtered = getFilteredValues(values, selected);
  if (_.isEqual(filtered, (component.model?.values))) {
    return null;
  }

  // Update values
  return getModelUpdate(state, address, { values: filtered }, settings);
}

/**
 * Build a values array with selection flags applied.
 * @param {Array} values Current values
 * @param {Array} selected Selected values
 * @returns {Array} Values with selection applied
 */
function getFilteredValues(values, selected) {
  let filtered = values.map((value) => ({ ...value, selected: selected.includes(value.value) }));
  return filtered.filter(value => value.selected).length === 0 ?
    selected.map(value => ({ ...extractCellModel(value), selected: true })) : filtered;
}

/**
 * Update selection for all components in a group.
 * @param {Object} state Current state
 * @param {Array} values Current values
 * @param {Object} view View identifier
 * @param {Object} group Group identifier
 * @param {Array} selected Selected values
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateSelectedGroup(state, values, view, group, selected, settings) {
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    // Mergear TODO el estado actual para filtrar por grupo
    const allIds = [...new Set([...ComponentRegistry.getAllIds(), ...Object.keys(state)])];
    const groupUpdates = allIds
      .map(id => {
        const base = ComponentRegistry.get(id);
        const delta = state[id] || {};
        const full = mergeComponentState(base, delta);
        if (full.address?.view === view && full.attributes?.group === group) {
          const updatedFull = {
            ...full,
            model: {
              ...full.model,
              values: (full.model?.values || []).map(value => ({ ...value, selected: selected.includes(value.value) })),
              changed: true
            }
          };
          return { id, delta: calculateDeltas(base, updatedFull) };
        }
        return null;
      })
      .filter(u => u !== null);

    return groupUpdates.reduce((acc, u) => ({ ...acc, [u.id]: u.delta }), {});
  }

  // Legacy behavior
  return {
    ...Object.entries(state)
      .filter(([name, component]) => component.address?.view === view && component.attributes?.group === group)
      .map(([name, component]) => ({
        name: name,
        value: {
          ...component,
          model: {
            ...component.model,
            values: component.model.values.map(value => ({ ...value, selected: selected.includes(value.value) })),
            changed: true
          }
        }
      }))
      .reduce((current, entry) => ({ ...current, [entry.name]: entry.value }), {})
  };
}

/**
 * Update selection at the correct scope for a component.
 * @param {Object} state Current state
 * @param {Object} address Target address
 * @param {Object} data Selection payload
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateSelected(state, address, data, settings) {
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
function getSelectedUpdate(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

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
 * Check whether selected values match a cell value.
 * @param {Array} selectedValues Selected values
 * @param {*} cellValue Cell value
 * @returns {boolean} True when selection matches the cell value
 */
function checkSelected(selectedValues, cellValue) {
  let selectedAsString = selectedValues.map(item => String(item));
  if (Array.isArray(cellValue)) {
    return cellValue.filter(item => item.selected && selectedAsString.includes(String(item.value))).length === 1;
  } else if (_.isPlainObject(cellValue) && "value" in cellValue) {
    return selectedAsString.includes(String(cellValue.value));
  } else {
    return selectedAsString.includes(String(cellValue));
  }
}

/**
 * Update a full grid row with new data.
 * @param {Object} state Current state
 * @param {Object} address Row address
 * @param {Object} data Row updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateRowModel(state, address, data, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, component } = context;
  const nextValues = updateArrayElement(values, rowIndex, data);
  const newState = updateGridRowValues(state, context, nextValues);

  // Update footer if is grid and show totals
  if (hasFooter(component)) {
    return updateGridFooter(newState, address, settings);
  }

  return newState;
}

/**
 * Store the current model snapshot for a component.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function keepModelComponent(state, componentId, settings) {
  return {
    ...state,
    ...getKeepModelComponent(state, componentId, settings)
  };
}

/**
 * Build the model snapshot update for a component.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @returns {Object} State diff for the snapshot
 */
function getKeepModelComponent(state, componentId, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      currentFull.storedModel = {
        ...currentFull?.model,
        values: (currentFull.model?.values || []).map(value => ({ ...value }))
      };
      return {
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  if (!state[componentId]) return {};
  return {
    [componentId]: {
      ...state[componentId],
      storedModel: {
        ...state[componentId].model,
        values: state[componentId].model.values.map(value => ({ ...value }))
      }
    }
  };
}



/**
 * Store the current model snapshot for a grid row and its cell components.
 * @param {Object} state Current state
 * @param {Object} address Row address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function keepRowModel(state, address, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(settings, componentId, state);

  const { model, attributes } = component;
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) return state;

  let rowValues = values[rowIndex];
  let cellModel = {};
  Object.keys(rowValues).forEach(column => {
    let cellAddress = { ...address, column: column };
    let cellComponentId = memoizedGetComponentId(cellAddress);
    if (cellComponentId in state || (useRegistry && ComponentRegistry.get(cellComponentId))) {
      cellModel = { ...cellModel, ...getKeepModelComponent(state, cellComponentId, settings) };
    }
  });

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId]);
      currentFull.storedModel = {
        ...currentFull?.storedModel,
        storedRows: {
          ...currentFull.storedModel?.storedRows,
          [address.row]: { ...values[rowIndex] }
        }
      };
      return {
        ...state,
        ...cellModel,
        [componentId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  return {
    ...state,
    ...cellModel,
    [componentId]: {
      ...state[componentId],
      storedModel: {
        ...state[componentId].storedModel,
        storedRows: {
          ...state[componentId].storedModel?.storedRows,
          [address.row]: {
            ...values[rowIndex]
          }
        }
      }
    }
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
function restoreModelComponent(state, componentId, settings, initial = false) {
  const newState = {
    ...state,
    ...getRestoreModelComponent(state, componentId, settings, initial)
  };

  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, newState);

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
function getRestoredModel(component, initial) {
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
function getRestoreModelComponent(state, componentId, settings, initial = false) {
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
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
  }

  // Legacy behavior
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
function resetModel(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

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

  if (useRegistry) {
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
  }

  // Legacy behavior
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
 * Reset a grid cell selection to the column defaults.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Unused payload
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function resetCellModel(state, address, data, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, state);

  const { attributes } = component || {};
  if (!attributes) return state;
  const gridId = memoizedGetGridIdentifier(attributes);
  let columnModel = (attributes.columnModel || []).filter(value => value[gridId] === address.column)[0] || {};
  return updateCellSelected(state, address, { selected: asArray(columnModel.model?.values) }, settings);
}

/**
 * Update grid footer values based on current grid data.
 * @param {Object} state Current state
 * @param {Object} address Grid address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateGridFooter(state, address, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      component = mergeComponentState(base, state[componentId]);
      const updatedFull = generateGridFooter(component);
      return {
        ...state,
        [componentId]: calculateDeltas(base, updatedFull)
      };
    }
  }

  // Legacy behavior
  component = state[componentId];
  return {
    ...state,
    [componentId]: generateGridFooter(component)
  };
}

/**
 * Attach footer data to a grid component model.
 * @param {Object} component Grid component
 * @returns {Object} Updated component with footer model
 */
function generateGridFooter(component) {
  return {
    ...component,
    model: {
      ...component.model,
      footer: generateFooter(component)
    }
  };
}

/**
 * Compute footer values for all grid columns.
 * @param {Object} component Grid component
 * @returns {Object} Footer values by column name
 */
function generateFooter(component) {
  const { columnModel = [] } = component?.attributes || {};
  return columnModel.reduce((prev, column) => ({ ...prev, [column.name]: calculateFooterValue(column, component.model?.values || []) }), {});
}

/**
 * Update footer values for all components that support footers.
 * @param {Object} data Components map
 * @returns {Object} Components map with updated footers
 */
function updateComponentFooters(data) {
  return Object.entries(data).reduce((prev, [componentId, component]) => ({ ...prev, [componentId]: hasFooter(component) ? generateGridFooter(component) : component }), {});
}

/**
 * Handle UPDATE_MODEL actions, including selection and footer refresh.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function updateModelAction(state = {}, action = {}, settings) {
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
  const useRegistry = settings?.useComponentRegistry;
  const component = getMergedComponent(useRegistry, componentId, modelState);

  if (hasFooter(component)) {
    modelState = updateGridFooter(modelState, action.address, settings);
  }

  return modelState;
}

/**
 * Remove all components for a given view.
 * @param {Object} state Current state
 * @param {string} view View identifier
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
function clearComponents(state, view, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
    const clearedIds = new Set(ComponentRegistry.clear(view));
    return Object.fromEntries(Object.entries(state)
      .filter(([id, component]) => !clearedIds.has(id) && (component?.address?.view ?? component?.context?.view) !== view));
  }

  // Legacy behavior
  return Object.fromEntries(Object.entries(state)
    .filter(([, component]) => (component?.address?.view ?? component?.context?.view) !== view));
}

/**
 * Action handlers map.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
const actionHandlers = {
  [CLEAR_COMPONENTS]: (state, action) => clearComponents(state, action.view, action.settings),

  [CLEAR_ALL_COMPONENTS]: () => {
    ComponentRegistry.clearAll();
    return {};
  },

  [UPDATE_VIEW_COMPONENTS]: (state, action) => {
    const clearedState = action.view === "base" ? {} : clearComponents(state, action.view, action.settings);
    const updatedData = updateComponentFooters(action.data);
    const useRegistry = action.settings && action.settings.useComponentRegistry;

    Object.entries(updatedData || {}).forEach(([id, component]) => {
      if (getAddressType(component?.address) === ComponentAddressType.ADDRESS_INVALID) {
        warnMalformedComponent({...component, uid: component?.uid ?? id}, {
          origin: 'reducer',
          operation: 'updateViewComponents',
          componentKey: id,
          extra: {
            registryComponentId: id,
            useComponentRegistry: !!useRegistry,
            targetView: action.view
          }
        });
      }
    });

    if (useRegistry) {
      // Registrar componentes en el Registry y calcular deltas para Redux
      const deltas = Object.entries(updatedData).reduce((acc, [id, component]) => {
        // Registrar en Registry (estado base)
        ComponentRegistry.register(id, component);

        // Calcular deltas (lo que realmente guardamos en Redux)
        const base = ComponentRegistry.get(id);
        acc[id] = calculateDeltas(base, component);

        return acc;
      }, {});

      return {
        ...clearedState,
        ...deltas
      };
    }

    // Modo legacy: guardar todo en Redux
    return {
      ...clearedState,
      ...updatedData
    };
  },

  [UPDATE_COMPONENT]: (state, action) =>
    updateComponentData(state, memoizedGetComponentId(action.address), action.data, action.settings),

  [UPDATE_MULTIPLE_COMPONENTS]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => updateComponentData(newState, memoizedGetComponentId(_action.address), _action, action.settings),
      state
    ),

  [UPDATE_MULTIPLE_MODELS]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateModelAction(newState, _action, action.settings), state),

  [UPDATE_ATTRIBUTES]: (state, action) => updateAttributeAction(state, action, action.settings),

  [UPDATE_SPECIFIC_ATTRIBUTES]: (state, action) => updateSpecificAttributes(state, action, action.settings),

  [UPDATE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateAttributeAction(newState, _action, action.settings), state),

  [UPDATE_MODEL]: (state, action) => updateModelAction(state, action, action.settings),

  [UPDATE_ROW_MODEL]: (state, action) => updateRowModel(state, action.address, action.data, action.settings),

  [UPDATE_VALIDATION]: (state, action) => updateValidationAction(state, action, action.settings),

  [UPDATE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateValidationAction(newState, _action, action.settings), state),

  [KEEP_VALIDATION]: (state, action) =>
    keepValidationComponent(state, memoizedGetComponentId(action.address), action.settings),

  [KEEP_ATTRIBUTE]: (state, action) =>
    keepAttributeComponent(state, memoizedGetComponentId(action.address), action.data, action.settings),

  [KEEP_MODEL]: (state, action) =>
    keepModelComponent(state, memoizedGetComponentId(action.address), action.settings),

  [KEEP_ROW_MODEL]: (state, action) => keepRowModel(state, action.address, action.settings),

  [RESTORE_VALIDATION]: (state, action) =>
    restoreValidationAction(state, action, action.settings),

  [RESTORE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreValidationAction(newState, _action, action.settings),
      state
    ),

  [RESTORE_ATTRIBUTE]: (state, action) => restoreAttributeAction(state, action, action.settings),

  [RESTORE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => restoreAttributeAction(newState, _action, action.settings), state),

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

  [VALIDATE_COMPONENTS]: (state, action) =>
    action.componentList.reduce((newState, _action) => validateComponent(newState, _action, action.settings), state),

  [VALIDATE_ROW]: (state, action) => validateRow(state, action.address, action.settings),
};

/**
 * Components reducer.
 * @param {Object} state Current Redux state
 * @param {Object} action Redux action with type and payload
 * @returns {Object} New state
 */
export function components(state = InitialState, action = {}) {
  const handler = actionHandlers[action.type];
  return handler ? handler(state, action) : state;
}
