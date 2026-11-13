import {
  KEEP_ATTRIBUTE,
  RESTORE_ATTRIBUTE,
  RESTORE_MULTIPLE_ATTRIBUTES,
  UPDATE_ATTRIBUTES,
  UPDATE_MULTIPLE_ATTRIBUTES,
  UPDATE_SPECIFIC_ATTRIBUTES
} from '../../actions/components';
import type {
  AttributeReference,
  ComponentData,
  keepAttribute,
  restoreAttribute,
  restoreMultipleAttributes,
  updateAttributes,
  updateMultipleAttributes,
  updateSpecificAttributes as updateSpecificAttributesCreator
} from '../../actions/components';
import type {ComponentAddress} from '../../../contract';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas} from '../../../utilities/mergeUtils';
import {getGridIdentifier} from "../../../utilities/grid";
import {updateArrayElement} from "../../../utilities";
import type {ComponentsState} from '../../types';
import {updateGridFooter} from './gridFooter';
import {
  addressColumnName,
  addressComponentId,
  asStateComponent,
  getGridRowContext,
  getMergedComponent,
  hasFooter,
  isGridComponent,
  launchAddressFunction,
  mergeStateComponent,
  NO_ACTION,
  resolveComponentId,
  updateComponentAttributes,
  updateComponentInState,
  updateGridRowValues
} from './stateHelpers';
import type {ActionHandlers, ReducerSettings, StateColumn} from './stateHelpers';

/** Actions the attribute handlers take */
export type AttributeAction = ReturnType<
  typeof updateAttributes | typeof updateSpecificAttributesCreator | typeof updateMultipleAttributes |
  typeof keepAttribute | typeof restoreAttribute | typeof restoreMultipleAttributes>;

/** An action that points to an address with data (the attribute changes, or the name of the attribute to restore) */
type AddressAction<D> = {address: ComponentAddress; data?: D};

/**
 * Update component attributes at component scope.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} data Attribute updates
 * @returns {Object} Updated state
 */
export function updateAttributeComponent(state: ComponentsState = {}, address: ComponentAddress = {}, data: ComponentData = {}): ComponentsState {
  return updateComponentAttributes(state, addressComponentId(address), data);
}

/**
 * Merge a partial number format into the one of a column, so a format that only changes the sign or the decimals keeps
 * the separators the column was created with.
 * @param {Object} column Current column
 * @param {Object} data Column attribute changes
 * @returns {Object} Column attribute changes
 */
export function mergeColumnNumberFormat(column: Partial<StateColumn> = {}, data: ComponentData = {}): ComponentData {
  const hasNumberFormatUpdate = typeof data.numberFormat === 'object' && data.numberFormat !== null;
  // The check above proved it is an object
  return hasNumberFormatUpdate ? {...data, numberFormat: {...column.numberFormat, ...(data.numberFormat as object)}} : data;
}

/**
 * Update column attributes inside a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Column attribute updates
 * @returns {Object} Updated state
 */
export function updateAttributeColumn(state: ComponentsState = {}, address: ComponentAddress = {}, data: ComponentData = {}): ComponentsState {
  const componentId = addressComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    const attributes = currentFull.attributes!;
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
  const { attributes } = asStateComponent(state[componentId] || {});
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
 * @returns {Object} Updated state
 */
export function updateAttributeCell(state: ComponentsState = {}, address: ComponentAddress = {}, data: ComponentData = {}): ComponentsState {
  const context = getGridRowContext(state, address);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const column = addressColumnName(address);
  const cellAttrs = rowData.$attrs?.[column] || {};
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData?.$attrs,
      [column]: { ...cellAttrs, ...data }
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
export function updateAttributeAction(
  state: ComponentsState = {},
  action: AddressAction<ComponentData> = NO_ACTION,
  settings: ReducerSettings = {}
): ComponentsState {
  const nextState = launchAddressFunction(updateAttributeCell, updateAttributeColumn, updateAttributeComponent, state, action, settings);
  const baseAddress = action.address?.component ? { view: action.address.view, component: action.address.component } : action.address;
  const componentId = resolveComponentId(baseAddress);
  if (!componentId) return nextState;
  const component = getMergedComponent(componentId, nextState);

  if ( !isGridComponent(component) ) return nextState;
  return hasFooter(component) ? updateGridFooter(nextState, baseAddress) : nextState;
}

/**
 * Update component-specific attributes (stored separately from normal attributes).
 * @param {Object} state Current state
 * @param {Object} action Action with address/data
 * @returns {Object} Updated state
 */
export function updateSpecificAttributes(state: ComponentsState = {}, action: AddressAction<ComponentData> = NO_ACTION): ComponentsState {
  const { address, data } = action;
  const componentId = resolveComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
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
 * @returns {Object} Updated state
 */
export function keepAttributeComponent(state: ComponentsState, component: string, data: string): ComponentsState {
  if (!state[component]) return state;
  const current = asStateComponent(state[component]);

  return updateComponentInState(state, component, {
    storedAttributes: {
      ...current.storedAttributes,
      [data]: {
        ...(current.attributes![data] as object)
      }
    }
  });
}

/**
 * Restore a previously kept component attribute.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {string} data Attribute name to restore
 * @returns {Object} Updated state
 */
export function restoreAttributeComponent(state: ComponentsState, address: ComponentAddress, data: string): ComponentsState {
  const componentId = resolveComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
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
  const current = asStateComponent(state[componentId]);
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...current.attributes,
        [data]: current.storedAttributes![data]
      }
    }
  };
}

/**
 * Restore a previously kept column attribute.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {string} data Attribute name to restore
 * @returns {Object} Updated state
 */
export function restoreAttributeColumn(
  state: ComponentsState = {},
  address: ComponentAddress = {},
  // The default `{}` is a name no column has: the attribute is set to undefined, as it always was
  data: string = {} as string
): ComponentsState {
  const componentId = addressComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    const attributes = currentFull.attributes!;
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
  const current = asStateComponent(state[componentId] || {});
  const { attributes } = current;
  if (!attributes) return state;
  const columnModel = attributes.columnModel || [];
  const columnIndex = columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, { [data]: current.storedAttributes!.columnModel![columnIndex][data] })
      }
    }
  };
}

/**
 * Restore a previously kept cell attribute.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {string} data Attribute name to restore
 * @returns {Object} Updated state
 */
export function restoreAttributeCell(state: ComponentsState, address: ComponentAddress, data: string): ComponentsState {
  const componentId = addressComponentId(address);
  const columnName = addressColumnName(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    const { model, attributes, storedAttributes = {} } = currentFull;
    const values = model!.values!;
    const gridId = getGridIdentifier(attributes);
    const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
    if (rowIndex < 0) return state;

    const columnIndex = (attributes!.columnModel || []).findIndex(column => column.name === address.column);
    const storedColumn: Partial<StateColumn> = storedAttributes.columnModel?.[columnIndex] || {};
    const defaultValue = storedColumn?.[data];
    const rowData = values[rowIndex] || {};
    const cellAttrs = rowData.$attrs?.[columnName] || {};
    const prevAttrs = rowData.$attrs || {};

    let newAttr;
    if (typeof defaultValue !== 'undefined') {
      newAttr = { ...cellAttrs, [data]: defaultValue };
    } else {
      newAttr = {...cellAttrs};
      delete newAttr[data];
    }

    currentFull.model = {
      ...model,
      values: updateArrayElement(values, rowIndex, { ...rowData, $attrs: { ...prevAttrs, [columnName]: newAttr } })
    };

    return {
      ...state,
      [componentId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  const current = asStateComponent(state[componentId] || {});
  const { model, attributes, storedAttributes = {} } = current;
  if (!model || !attributes) return state;
  const values = model.values!;
  const gridId = getGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) {
    return state;
  }
  const columnIndex = (attributes.columnModel || []).findIndex(column => column.name === address.column);
  const storedColumn: Partial<StateColumn> = storedAttributes.columnModel?.[columnIndex] || {};
  const defaultValue = storedColumn?.[data];
  const rowData = values[rowIndex] || {};
  const cellAttrs = rowData.$attrs?.[columnName] || {};
  const prevAttrs = rowData.$attrs || {};

  let newAttr;
  if (typeof defaultValue !== 'undefined') {
    newAttr = {
      ...cellAttrs,
      [data]: defaultValue
    };
  } else {
    newAttr = {...cellAttrs};
    delete newAttr[data];
  }

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      model: {
        ...model,
        values: updateArrayElement(values, rowIndex, { ...rowData, $attrs: { ...prevAttrs, [columnName]: newAttr } })
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
export function restoreAttributeAction(
  state: ComponentsState = {},
  action: AddressAction<string> = NO_ACTION,
  settings: ReducerSettings = {}
): ComponentsState {
  return launchAddressFunction(restoreAttributeCell, restoreAttributeColumn, restoreAttributeComponent, state, action, settings);
}

/**
 * Attribute action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const attributeHandlers: ActionHandlers<AttributeAction> = {
  [UPDATE_ATTRIBUTES]: (state, action) => updateAttributeAction(state, action, action.settings),

  [UPDATE_SPECIFIC_ATTRIBUTES]: (state, action) => updateSpecificAttributes(state, action),

  [UPDATE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateAttributeAction(newState, _action, action.settings), state),

  // The attribute name is always given: a missing one keeps the attribute "undefined", as it always did
  [KEEP_ATTRIBUTE]: (state, action) =>
    keepAttributeComponent(state, resolveComponentId(action.address), action.data as string),

  [RESTORE_ATTRIBUTE]: (state, action) => restoreAttributeAction(state, action, action.settings),

  [RESTORE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce(
      (newState, _action: AttributeReference) => restoreAttributeAction(newState, _action, action.settings),
      state
    ),
};
