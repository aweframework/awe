import {
  KEEP_ATTRIBUTE,
  RESTORE_ATTRIBUTE,
  RESTORE_MULTIPLE_ATTRIBUTES,
  UPDATE_ATTRIBUTES,
  UPDATE_MULTIPLE_ATTRIBUTES,
  UPDATE_SPECIFIC_ATTRIBUTES
} from '../../actions/components';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import {updateArrayElement} from "../../../utilities";
import {updateGridFooter} from './gridFooter';
import {
  getGridRowContext,
  getMergedComponent,
  hasFooter,
  isGridComponent,
  launchAddressFunction,
  memoizedGetComponentId,
  memoizedGetGridIdentifier,
  updateComponentAttributes,
  updateComponentInState,
  updateGridRowValues
} from './stateHelpers';

/**
 * Update component attributes at component scope.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} data Attribute updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateAttributeComponent(state = {}, address = {}, data = {}, settings = {}) {
  return updateComponentAttributes(state, address.component, data, settings);
}

/**
 * Merge a partial number format into the one of a column, so a format that only changes the sign or the decimals keeps
 * the separators the column was created with.
 * @param {Object} column Current column
 * @param {Object} data Column attribute changes
 * @returns {Object} Column attribute changes
 */
export function mergeColumnNumberFormat(column = {}, data = {}) {
  const hasNumberFormatUpdate = typeof data.numberFormat === 'object' && data.numberFormat !== null;
  return hasNumberFormatUpdate ? {...data, numberFormat: {...column.numberFormat, ...data.numberFormat}} : data;
}

/**
 * Update column attributes inside a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Column attribute updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateAttributeColumn(state = {}, address = {}, data = {}, settings = {}) {
  const componentId = address.component;

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

  // Component not registered: operate directly on the Redux entry
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
export function updateAttributeCell(state = {}, address = {}, data = {}, settings = {}) {
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
export function updateAttributeAction(state = {}, action = {}, settings = {}) {
  const nextState = launchAddressFunction(updateAttributeCell, updateAttributeColumn, updateAttributeComponent, state, action, settings);
  const baseAddress = action.address?.component ? { view: action.address.view, component: action.address.component } : action.address;
  const componentId = memoizedGetComponentId(baseAddress);
  if (!componentId) return nextState;
  const component = getMergedComponent(componentId, nextState);

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
export function updateSpecificAttributes(state = {}, action = {}, settings = {}) {
  const { address, data } = action;
  const componentId = memoizedGetComponentId(address);

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

  // Component not registered: operate directly on the Redux entry
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
export function keepAttributeComponent(state, component, data, settings) {
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
export function restoreAttributeComponent(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);

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

  // Component not registered: operate directly on the Redux entry
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
export function restoreAttributeColumn(state = {}, address = {}, data = {}, settings = {}) {
  const componentId = address.component;

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

  // Component not registered: operate directly on the Redux entry
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
export function restoreAttributeCell(state, address, data, settings) {
  const componentId = address.component;

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

  // Component not registered: operate directly on the Redux entry
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
export function restoreAttributeAction(state = {}, action = {}, settings = {}) {
  return launchAddressFunction(restoreAttributeCell, restoreAttributeColumn, restoreAttributeComponent, state, action, settings);
}

/**
 * Attribute action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const attributeHandlers = {
  [UPDATE_ATTRIBUTES]: (state, action) => updateAttributeAction(state, action, action.settings),

  [UPDATE_SPECIFIC_ATTRIBUTES]: (state, action) => updateSpecificAttributes(state, action, action.settings),

  [UPDATE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateAttributeAction(newState, _action, action.settings), state),

  [KEEP_ATTRIBUTE]: (state, action) =>
    keepAttributeComponent(state, memoizedGetComponentId(action.address), action.data, action.settings),

  [RESTORE_ATTRIBUTE]: (state, action) => restoreAttributeAction(state, action, action.settings),

  [RESTORE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => restoreAttributeAction(newState, _action, action.settings), state),
};
