import {
  KEEP_VALIDATION,
  RESTORE_MULTIPLE_VALIDATION,
  RESTORE_VALIDATION,
  UPDATE_MULTIPLE_VALIDATION,
  UPDATE_VALIDATION,
  VALIDATE_COMPONENTS,
  VALIDATE_ROW
} from '../../actions/components';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import {updateArrayElement} from "../../../utilities";
import {validateComponent, validateRow} from "../validation";
import {
  getGridRowContext,
  getMergedComponent,
  launchAddressFunction,
  memoizedGetComponentId,
  updateComponentInState,
  updateComponentValidation,
  updateGridRowValues
} from './stateHelpers';

/**
 * Dispatch validation updates based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateValidationAction(state = {}, action = {}, settings = {}) {
  return launchAddressFunction(updateValidationCell, updateValidationColumn, updateValidationComponent, state, action, settings);
}

/**
 * Restore validation rules based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function restoreValidationAction(state = {}, action = {}, settings = {}) {
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
export function updateValidationComponent(state = {}, address = {}, data = {}, settings = {}) {
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
export function updateValidationColumn(state = {}, address = {}, data = {}, settings = {}) {
  const componentId = address.component;

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
export function updateValidationCell(state = {}, address = {}, data = {}, settings = {}) {
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
export function keepValidationComponent(state, componentId, settings) {
  const component = getMergedComponent(componentId, state);

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
export function restoreValidationComponent(state, address, settings) {
  const componentId = memoizedGetComponentId(address);
  const component = getMergedComponent(componentId, state);
  if (component?.storedValidationRules === undefined) {
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
export function restoreValidationColumn(state = {}, address = {}, _data = {}, settings = {}) {
  const componentId = address.component;

  const buildNextColumn = (columnModel = [], storedAttributes = {}) => {
    const columnIndex = columnModel.findIndex(column => column.name === address.column);
    if (columnIndex < 0) return null;

    const storedColumn = storedAttributes.columnModel?.[columnIndex] || {};
    if (storedColumn.validationRules !== undefined) {
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

  // Component not registered: operate directly on the Redux entry
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
export function restoreValidationCell(state = {}, address = {}, _data = {}, settings = {}) {
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
 * Validation rule action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const validationRuleHandlers = {
  [UPDATE_VALIDATION]: (state, action) => updateValidationAction(state, action, action.settings),

  [UPDATE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateValidationAction(newState, _action, action.settings), state),

  [KEEP_VALIDATION]: (state, action) =>
    keepValidationComponent(state, memoizedGetComponentId(action.address), action.settings),

  [RESTORE_VALIDATION]: (state, action) =>
    restoreValidationAction(state, action, action.settings),

  [RESTORE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreValidationAction(newState, _action, action.settings),
      state
    ),

  [VALIDATE_COMPONENTS]: (state, action) =>
    action.componentList.reduce((newState, _action) => validateComponent(newState, _action, action.settings), state),

  [VALIDATE_ROW]: (state, action) => validateRow(state, action.address, action.settings),
};
