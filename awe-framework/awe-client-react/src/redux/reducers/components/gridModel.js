import {
  KEEP_ROW_MODEL,
  UPDATE_ROW_MODEL
} from '../../actions/components';
import _ from 'lodash';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import {asArray, updateArrayElement} from "../../../utilities";
import {extractCellModel, extractCellValue, getCellModel, getGridIdentifier} from "../../../utilities/grid";
import {getComponentId} from "../../../utilities/components";
import {getFirstDefinedValue, isMultipleComponent} from "../../../utilities/general";
import {updateGridFooter} from './gridFooter';
import {
  getGridRowContext,
  getKeepModelComponent,
  getMergedComponent,
  getModelUpdate,
  hasFooter,
  updateGridRowValues
} from './stateHelpers';

/**
 * Normalize a cell model by applying selected values.
 * @param {Array|Object} selected Selected data
 * @param {Object} model Existing cell model definition
 * @returns {Object} Normalized cell model
 */
export function fixCellModel(selected, model) {
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
 * Update child cell models for a grid based on row values.
 * @param {Object} state Current state
 * @param {Object} grid Grid component
 * @param {Object} settings Settings
 * @returns {Object} Updated state for cell components
 */
export function updateCellsModel(state, grid, settings) {
  const { values } = grid.model;
  const columns = grid.attributes?.columnModel?.filter(column => column.component) || [];
  if (columns.length === 0) {
    return {};
  }
  const cellComponents = {};
  const gridId = getGridIdentifier(grid.attributes);
  values.forEach(row => {
    columns.forEach(column => {
      const cellAddress = { ...grid.address, row: row[gridId], column: column.name };
      const cellComponentId = getComponentId(cellAddress);
      if (cellComponentId in state || ComponentRegistry.get(cellComponentId)) {
        Object.assign(cellComponents,
          getModelUpdate(state, cellAddress, fixCellModel(row[column.name], column.model), settings)
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
 * @param {Object} settings Settings
 * @returns {Object} Component update map
 */
export function getGroupModelUpdate(state, view, group, model, settings) {
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
export function getGridModelUpdate(state, address, model, update = true, settings) {
  let componentId = getComponentId(address);
  const component = getMergedComponent(componentId, state);
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
 * Update a column model definition inside a grid.
 * @param {Object} state Current state
 * @param {Object} address Column address
 * @param {Object} data Model updates
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateColumnModel(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;

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
export function updateCellModel(state, address, data, settings) {
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
export function updateCellSelected(state, address, data, settings) {
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
export function updateSelectedGrid(state, values, address, data, settings) {
  const componentId = getComponentId(address);
  const component = getMergedComponent(componentId, state);

  if (!component || !component.attributes) return state;

  const { selected = [] } = data;
  const gridId = getGridIdentifier(component.attributes);
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
 * Build a values array with selection flags applied.
 * @param {Array} values Current values
 * @param {Array} selected Selected values
 * @returns {Array} Values with selection applied
 */
export function getFilteredValues(values, selected) {
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
export function updateSelectedGroup(state, values, view, group, selected, settings) {

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

/**
 * Check whether selected values match a cell value.
 * @param {Array} selectedValues Selected values
 * @param {*} cellValue Cell value
 * @returns {boolean} True when selection matches the cell value
 */
export function checkSelected(selectedValues, cellValue) {
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
export function updateRowModel(state, address, data, settings) {
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
 * Store the current model snapshot for a grid row and its cell components.
 * @param {Object} state Current state
 * @param {Object} address Row address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function keepRowModel(state, address, settings) {
  const componentId = address.component;
  const component = getMergedComponent(componentId, state);

  const { model, attributes } = component;
  const { values } = model;
  const gridId = getGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) return state;

  let rowValues = values[rowIndex];
  let cellModel = {};
  Object.keys(rowValues).forEach(column => {
    let cellAddress = { ...address, column: column };
    let cellComponentId = getComponentId(cellAddress);
    if (cellComponentId in state || ComponentRegistry.get(cellComponentId)) {
      cellModel = { ...cellModel, ...getKeepModelComponent(state, cellComponentId, settings) };
    }
  });

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

  // Component not registered: operate directly on the Redux entry
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
 * Reset a grid cell selection to the column defaults.
 * @param {Object} state Current state
 * @param {Object} address Cell address
 * @param {Object} data Unused payload
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function resetCellModel(state, address, data, settings) {
  const componentId = address.component;
  const component = getMergedComponent(componentId, state);

  const { attributes } = component || {};
  if (!attributes) return state;
  const gridId = getGridIdentifier(attributes);
  let columnModel = (attributes.columnModel || []).filter(value => value[gridId] === address.column)[0] || {};
  return updateCellSelected(state, address, { selected: asArray(columnModel.model?.values) }, settings);
}

/**
 * Grid model action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const gridModelHandlers = {
  [UPDATE_ROW_MODEL]: (state, action) => updateRowModel(state, action.address, action.data, action.settings),

  [KEEP_ROW_MODEL]: (state, action) => keepRowModel(state, action.address, action.settings),
};
