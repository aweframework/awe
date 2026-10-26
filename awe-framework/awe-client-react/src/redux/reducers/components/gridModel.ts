import {
  KEEP_ROW_MODEL,
  UPDATE_ROW_MODEL
} from '../../actions/components';
import type {keepRowModel as keepRowModelCreator, updateRowModel as updateRowModelCreator} from '../../actions/components';
import _ from 'lodash';
import type {ComponentAddress} from '../../../contract';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas} from '../../../utilities/mergeUtils';
import {asArray, updateArrayElement} from "../../../utilities";
import {extractCellModel, extractCellValue, getCellModel, getGridIdentifier} from "../../../utilities/grid";
import type {CellOption} from "../../../utilities/grid";
import {getFirstDefinedValue, isMultipleComponent} from "../../../utilities/general";
import type {ComponentsState} from '../../types';
import {updateGridFooter} from './gridFooter';
import {
  addressColumnName,
  addressComponentId,
  asStateComponent,
  getGridRowContext,
  getKeepModelComponent,
  getMergedComponent,
  getModelUpdate,
  hasFooter,
  mergeStateComponent,
  resolveComponentId,
  updateGridRowValues
} from './stateHelpers';
import type {ActionHandlers, ReducerSettings, StateColumn, StateComponent, StateModel, StateRow} from './stateHelpers';

/** Actions the grid model handlers take */
export type GridModelAction = ReturnType<typeof updateRowModelCreator | typeof keepRowModelCreator>;

/**
 * Normalize a cell model by applying selected values.
 * @param {Array|Object} selected Selected data
 * @param {Object} model Existing cell model definition
 * @returns {Object} Normalized cell model
 */
export function fixCellModel(selected: unknown, model?: {values?: CellOption[]} | null): {values: CellOption[] | CellOption} {
  const selectedData = [...asArray(selected)];
  if (model && model.values && model.values.length) {
    const selectedString = selectedData.map(value => String(extractCellValue(value)));
    return {
      ...model,
      values: model.values.map(value => ({ ...value, selected: selectedString.includes(String(value.value)) }))
    };
  } else {
    return {
      values: selectedData.reduce<CellOption>((prev, value) => ({ ...extractCellModel(value) }), {})
    };
  }
}

/**
 * Update child cell models for a grid based on row values.
 * @param {Object} state Current state
 * @param {Object} grid Grid component
 * @returns {Object} Updated state for cell components
 */
export function updateCellsModel(state: ComponentsState, grid: StateComponent): ComponentsState {
  const { values } = grid.model as StateModel;
  const columns = grid.attributes?.columnModel?.filter(column => column.component) || [];
  if (columns.length === 0) {
    return {};
  }
  const cellComponents: ComponentsState = {};
  const gridId = getGridIdentifier(grid.attributes);
  values!.forEach(row => {
    columns.forEach(column => {
      // The identifier of a row is a string or a number: the address keeps it as it is
      const cellAddress = { ...grid.address, row: row[gridId] as string, column: column.name };
      const cellComponentId = resolveComponentId(cellAddress);
      if (cellComponentId in state || ComponentRegistry.get(cellComponentId)) {
        Object.assign(cellComponents,
          getModelUpdate(state, cellAddress, fixCellModel(row[column.name], column.model))
        );
      }
    });
  });
  return cellComponents;
}

/**
 * Build model updates for all components in a radio group.
 * @param {Object} state Current state
 * @param {string} view View identifier
 * @param {string} group Group identifier
 * @param {Object} model Model containing values/selection
 * @returns {Object} Component update map
 */
export function getGroupModelUpdate(
  state: ComponentsState,
  view: string | undefined,
  group: string | undefined,
  model: StateModel
): ComponentsState {
  // Check equality to avoid update state if there are no changes
  const selected = model.values!.map(item => item.value);
  return updateSelectedGroup(state, model.values!, view, group, selected);
}

/**
 * Build a grid model update and optionally refresh cell models.
 * @param {Object} state Current state
 * @param {Object} address Grid address
 * @param {Object} model Model updates
 * @param {boolean} update Whether to update cell models
 * @returns {Object} Component update map
 */
export function getGridModelUpdate(state: ComponentsState, address: ComponentAddress, model: StateModel, update = true): ComponentsState {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);
  if (!component) return state;

  const nextModelVersion = (component?.model?.modelVersion || 0) + 1;
  const modelWithVersion = {
    ...model,
    modelVersion: nextModelVersion
  };
  const gridComponent = getModelUpdate(state, address, modelWithVersion);

  // Para updateCellsModel necesitamos el componente mergeado con los cambios que acabamos de calcular
  const updatedFull = mergeStateComponent(component, gridComponent[componentId]);

  const cellsState = model.values && update ? updateCellsModel({ ...state, ...gridComponent }, updatedFull) : {};
  return {
    ...gridComponent,
    ...cellsState
  };
}

/**
 * Update a column model definition inside a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Model updates
 * @returns {Object} Updated state
 */
export function updateColumnModel(state: ComponentsState = {}, address: ComponentAddress = {}, data: StateModel = {}): ComponentsState {
  const componentId = addressComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    const attributes = currentFull.attributes!;
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
 * @returns {Object} Updated state
 */
export function updateCellModel(state: ComponentsState, address: ComponentAddress, data: StateModel): ComponentsState {
  const context = getGridRowContext(state, address);
  if (!context) return state;

  const { values, rowIndex, rowData, attributes } = context;
  const columnName = addressColumnName(address);
  const column: Partial<StateColumn> = attributes.columnModel?.find(column => column.name === address.column) || {};
  const rawValue = getFirstDefinedValue(data.values, values[rowIndex][columnName]);
  const isMultiple = isMultipleComponent(column.component);
  const newData = isMultiple
    ? (Array.isArray(rawValue) ? rawValue : [])
    : getCellModel(rawValue, column);
  const cellAttrs = rowData.$attrs?.[columnName] || {};
  const otherAttrs = {...cellAttrs};
  delete otherAttrs.error;
  const nextValues = updateArrayElement(values, rowIndex, {
    [columnName]: newData,
    $attrs: { ...rowData.$attrs, [columnName]: otherAttrs }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update selected values inside a cell model.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Selection payload
 * @returns {Object} Updated state
 */
export function updateCellSelected(state: ComponentsState, address: ComponentAddress, data: {selected: unknown[]}): ComponentsState {
  const context = getGridRowContext(state, address);
  if (!context) return state;

  const { values, rowIndex } = context;
  const columnName = addressColumnName(address);
  const sameValue = checkSelected(data.selected, values[rowIndex][columnName]);
  if (sameValue) return state;

  const nextValues = updateArrayElement(values, rowIndex, {
    [columnName]: fixCellModel(data.selected, values[rowIndex][columnName] as {values?: CellOption[]}).values
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update selected rows in a grid by ids.
 * @param {Object} state Current state
 * @param {Array} values Grid values
 * @param {Object} address Grid address
 * @param {Object} data Selection payload
 * @returns {Object} Updated state
 */
export function updateSelectedGrid(state: ComponentsState, values: StateRow[], address: ComponentAddress, data: StateModel): ComponentsState {
  const componentId = resolveComponentId(address);
  const component = getMergedComponent(componentId, state);

  if (!component || !component.attributes) return state;

  const { selected = [] } = data;
  const gridId = getGridIdentifier(component.attributes);
  let filtered = [...values];
  const toUnselect = values
    .filter(row => row.selected)
    .filter(row => !selected.map(String).includes(String(row[gridId])))
    .map(row => ({ id: row[gridId], selected: false }));
  const toSelect = selected
    .map(value => ({ id: value, selected: true }));
  // Unselect values
  [...toUnselect, ...toSelect].forEach(item => {
    const index = filtered.findIndex((row) => String(row[gridId]) === String(item.id));
    // Safeguard for index not found
    if (index >= 0) {
      filtered = updateArrayElement(filtered, index, { selected: item.selected });
    }
  });

  // Update values
  return getGridModelUpdate(state, address, { ...data, values: filtered }, false);
}

/**
 * Build a values array with selection flags applied.
 * @param {Array} values Current values
 * @param {Array} selected Selected values
 * @returns {Array} Values with selection applied
 */
export function getFilteredValues(values: CellOption[], selected: unknown[]): CellOption[] {
  const filtered = values.map((value) => ({ ...value, selected: selected.includes(value.value) }));
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
 * @returns {Object} Updated state
 */
export function updateSelectedGroup(
  state: ComponentsState,
  values: CellOption[],
  view: string | undefined,
  group: string | undefined,
  selected: unknown[]
): ComponentsState {

  // Mergear TODO el estado actual para filtrar por grupo
  const allIds = [...new Set([...ComponentRegistry.getAllIds(), ...Object.keys(state)])];
  const groupUpdates = allIds
    .map(id => {
      const base = ComponentRegistry.get(id);
      const delta = state[id] || {};
      const full = mergeStateComponent(base, delta);
      if (full.address?.view === view && full.attributes?.group === group) {
        const updatedFull: StateComponent = {
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

  return groupUpdates.reduce<ComponentsState>((acc, u) => ({ ...acc, [u.id]: u.delta }), {});
}

/**
 * Check whether selected values match a cell value.
 * @param {Array} selectedValues Selected values
 * @param {*} cellValue Cell value
 * @returns {boolean} True when selection matches the cell value
 */
export function checkSelected(selectedValues: unknown[], cellValue: unknown): boolean {
  const selectedAsString = selectedValues.map(item => String(item));
  if (Array.isArray(cellValue)) {
    return cellValue.filter(item => item.selected && selectedAsString.includes(String(item.value))).length === 1;
  } else if (_.isPlainObject(cellValue) && "value" in (cellValue as object)) {
    return selectedAsString.includes(String((cellValue as CellOption).value));
  } else {
    return selectedAsString.includes(String(cellValue));
  }
}

/**
 * Update a full grid row with new data.
 * @param {Object} state Current state
 * @param {Object} address Row address
 * @param {Object} data Row updates
 * @returns {Object} Updated state
 */
export function updateRowModel(state: ComponentsState, address: ComponentAddress, data?: StateRow): ComponentsState {
  const context = getGridRowContext(state, address);
  if (!context) return state;

  const { values, rowIndex, component } = context;
  // The data of a row update is always given: a missing one spreads to nothing, as it always did
  const nextValues = updateArrayElement(values, rowIndex, data as StateRow);
  const newState = updateGridRowValues(state, context, nextValues);

  // Update footer if is grid and show totals
  if (hasFooter(component)) {
    return updateGridFooter(newState, address);
  }

  return newState;
}

/**
 * Store the current model snapshot for a grid row and its cell components.
 * @param {Object} state Current state
 * @param {Object} address Row address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function keepRowModel(state: ComponentsState, address: ComponentAddress, settings?: ReducerSettings): ComponentsState {
  const componentId = addressComponentId(address);
  const component = getMergedComponent(componentId, state);

  const { model, attributes } = component as StateComponent;
  const values = model!.values!;
  const gridId = getGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) return state;

  const rowValues = values[rowIndex];
  let cellModel: ComponentsState = {};
  Object.keys(rowValues).forEach(column => {
    const cellAddress = { ...address, column: column };
    const cellComponentId = resolveComponentId(cellAddress);
    if (cellComponentId in state || ComponentRegistry.get(cellComponentId)) {
      cellModel = { ...cellModel, ...getKeepModelComponent(state, cellComponentId, settings) };
    }
  });

  const base = ComponentRegistry.get(componentId);
  const rowKey = address.row as string;
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    currentFull.storedModel = {
      ...currentFull?.storedModel,
      storedRows: {
        ...currentFull.storedModel?.storedRows,
        [rowKey]: { ...values[rowIndex] }
      }
    };
    return {
      ...state,
      ...cellModel,
      [componentId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  const current = asStateComponent(state[componentId]);
  return {
    ...state,
    ...cellModel,
    [componentId]: {
      ...state[componentId],
      storedModel: {
        ...current.storedModel,
        storedRows: {
          ...current.storedModel?.storedRows,
          [rowKey]: {
            ...values[rowIndex]
          }
        }
      }
    }
  };
}

/**
 * Reset a grid cell selection to the column defaults.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @returns {Object} Updated state
 */
export function resetCellModel(state: ComponentsState, address: ComponentAddress): ComponentsState {
  const componentId = addressComponentId(address);
  const component = getMergedComponent(componentId, state);

  const { attributes } = component || {};
  if (!attributes) return state;
  const gridId = getGridIdentifier(attributes);
  const columnModel: Partial<StateColumn> = (attributes.columnModel || []).filter(value => value[gridId] === address.column)[0] || {};
  return updateCellSelected(state, address, { selected: asArray(columnModel.model?.values) });
}

/**
 * Grid model action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const gridModelHandlers: ActionHandlers<GridModelAction> = {
  [UPDATE_ROW_MODEL]: (state, action) => updateRowModel(state, action.address, action.data),

  [KEEP_ROW_MODEL]: (state, action) => keepRowModel(state, action.address, action.settings),
};
