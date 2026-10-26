import {
  KEEP_VALIDATION,
  RESTORE_MULTIPLE_VALIDATION,
  RESTORE_VALIDATION,
  UPDATE_MULTIPLE_VALIDATION,
  UPDATE_VALIDATION,
  VALIDATE_COMPONENTS,
  VALIDATE_ROW
} from '../../actions/components';
import type {
  ComponentData,
  keepValidation,
  restoreMultipleValidation,
  restoreValidation,
  updateMultipleValidation,
  updateValidation
} from '../../actions/components';
import type {ComponentAddress} from '../../../contract';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas} from '../../../utilities/mergeUtils';
import {updateArrayElement} from "../../../utilities";
import type {ComponentsState} from '../../types';
import {validateComponent, validateRow} from "../validation";
import {
  addressColumnName,
  addressComponentId,
  asStateComponent,
  getGridRowContext,
  getMergedComponent,
  launchAddressFunction,
  mergeStateComponent,
  NO_ACTION,
  resolveComponentId,
  updateComponentInState,
  updateComponentValidation,
  updateGridRowValues
} from './stateHelpers';
import type {ActionHandlers, ReducerSettings, StateAttributes, StateColumn, StateComponent} from './stateHelpers';

/**
 * Validate a list of components. The thunks dispatch it as it is, there is no creator
 */
export interface ValidateComponentsAction {
  type: typeof VALIDATE_COMPONENTS;
  componentList: StateComponent[];
}

/**
 * Validate a row of a grid. The thunks dispatch it as it is, there is no creator
 */
export interface ValidateRowAction {
  type: typeof VALIDATE_ROW;
  address: ComponentAddress;
}

/** Actions the validation rule handlers take */
export type ValidationRuleAction = ReturnType<
  typeof updateValidation | typeof updateMultipleValidation | typeof keepValidation |
  typeof restoreValidation | typeof restoreMultipleValidation> | ValidateComponentsAction | ValidateRowAction;

/** An action that points to an address with data */
type AddressAction = {address: ComponentAddress; data?: ComponentData};

/**
 * Dispatch validation updates based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateValidationAction(
  state: ComponentsState = {},
  action: AddressAction = NO_ACTION,
  settings: ReducerSettings = {}
): ComponentsState {
  return launchAddressFunction(updateValidationCell, updateValidationColumn, updateValidationComponent, state, action, settings);
}

/**
 * Restore validation rules based on address type.
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function restoreValidationAction(
  state: ComponentsState = {},
  action: AddressAction = NO_ACTION,
  settings: ReducerSettings = {}
): ComponentsState {
  return launchAddressFunction(restoreValidationCell, restoreValidationColumn, restoreValidationComponent, state, action, settings);
}

/**
 * Update component-level validation rules.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} data Validation rule updates
 * @returns {Object} Updated state
 */
export function updateValidationComponent(state: ComponentsState = {}, address: ComponentAddress = {}, data: ComponentData = {}): ComponentsState {
  return updateComponentValidation(state, addressComponentId(address), data);
}

/**
 * Update column-level validation rules in a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Validation rule updates
 * @returns {Object} Updated state
 */
export function updateValidationColumn(state: ComponentsState = {}, address: ComponentAddress = {}, data: ComponentData = {}): ComponentsState {
  const componentId = addressComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    const attributes = currentFull.attributes!;
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
  const { attributes } = asStateComponent(state[componentId] || {});
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
 * @returns {Object} Updated state
 */
export function updateValidationCell(state: ComponentsState = {}, address: ComponentAddress = {}, data: ComponentData = {}): ComponentsState {
  const context = getGridRowContext(state, address);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const columnName = addressColumnName(address);
  const cellAttrs = rowData.$attrs?.[columnName] || {};
  const currentValidation = cellAttrs.validationRules || {};
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData?.$attrs,
      [columnName]: {
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
 * @returns {Object} Updated state
 */
export function keepValidationComponent(state: ComponentsState, componentId: string): ComponentsState {
  const component = getMergedComponent(componentId, state);

  return updateComponentInState(state, componentId, {
    storedValidationRules: {
      ...component?.validationRules
    }
  });
}

/**
 * Restore previously kept validation rules.
 * @param {Object} state Current state
 * @param {Object} address Component addres
 * @returns {Object} Updated state
 */
export function restoreValidationComponent(state: ComponentsState, address: ComponentAddress): ComponentsState {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);
  if (component?.storedValidationRules === undefined) {
    return state;
  }

  return updateComponentInState(state, componentId, {
    validationRules: {
      ...component?.storedValidationRules
    }
  });
}

/**
 * Restore column-level validation rules in a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @returns {Object} Updated state
 */
export function restoreValidationColumn(state: ComponentsState = {}, address: ComponentAddress = {}): ComponentsState {
  const componentId = addressComponentId(address);

  const buildNextColumn = (columnModel: StateColumn[] = [], storedAttributes: StateAttributes = {}) => {
    const columnIndex = columnModel.findIndex(column => column.name === address.column);
    if (columnIndex < 0) return null;

    const storedColumn: Partial<StateColumn> = storedAttributes.columnModel?.[columnIndex] || {};
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
    const currentFull = mergeStateComponent(base, state[componentId]);
    const { storedAttributes = {} } = currentFull;
    const attributes = currentFull.attributes!;
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
  const { attributes, storedAttributes = {} } = asStateComponent(state[componentId] || {});
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
 * @returns {Object} Updated state
 */
export function restoreValidationCell(state: ComponentsState = {}, address: ComponentAddress = {}): ComponentsState {
  const context = getGridRowContext(state, address);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const columnName = addressColumnName(address);
  const cellAttrs = rowData.$attrs?.[columnName] || {};
  if (!("validationRules" in cellAttrs)) {
    return state;
  }

  const restCellAttrs = {...cellAttrs};
  delete restCellAttrs.validationRules;
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData?.$attrs,
      [columnName]: restCellAttrs
    }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Validation rule action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const validationRuleHandlers: ActionHandlers<ValidationRuleAction> = {
  [UPDATE_VALIDATION]: (state, action) => updateValidationAction(state, action, action.settings),

  [UPDATE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateValidationAction(newState, _action, action.settings), state),

  [KEEP_VALIDATION]: (state, action) =>
    keepValidationComponent(state, resolveComponentId(action.address)),

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
