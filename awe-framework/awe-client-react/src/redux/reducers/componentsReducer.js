import {
  AFTER_SAVE_ROW,
  CLEAR_ALL_COMPONENTS,
  CLEAR_COMPONENTS,
  ComponentStatus,
  GENERATE_CELL_COMPONENTS,
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

import { extractCellModel, extractCellValue, getCellModel, getGridIdentifier } from "../../utilities/grid";

import { getUID } from "../actions/settings";
import _ from 'lodash';
import { validateComponent, validateRow } from "./validation";
import { asArray, updateArrayElement } from "../../utilities";
import { ComponentAddressType, getAddressType, getComponentId } from "../../utilities/components";
import { getFirstDefinedValue } from "../../utilities/general";

const { STATUS_DEFINED, STATUS_INITIALIZED } = ComponentStatus;
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
 * Updates a component in state with new data
 * @param {Object} state - Current state
 * @param {string} componentId - Component identifier
 * @param {Object} updates - Updates to merge into the component
 * @returns {Object} New state with updated component
 */
function updateComponentInState(state, componentId, updates) {
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      ...updates
    }
  };
}

/**
 * Updates a nested property within a component
 * @param {Object} state - Current state
 * @param {string} componentId - Component identifier
 * @param {string} propertyName - Name of the property to update (e.g., 'attributes', 'model')
 * @param {Object} updates - Updates to merge into the property
 * @returns {Object} New state with updated property
 */
function updateComponentProperty(state, componentId, propertyName, updates) {
  if (!state[componentId]) return state;

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      [propertyName]: {
        ...state[componentId][propertyName],
        ...updates
      }
    }
  };
}

/**
 * Updates component attributes
 * @param {Object} state - Current state
 * @param {string} componentId - Component identifier
 * @param {Object} attributeUpdates - Attribute updates to merge
 * @returns {Object} New state with updated attributes
 */
function updateComponentAttributes(state, componentId, attributeUpdates) {
  return updateComponentProperty(state, componentId, 'attributes', attributeUpdates);
}

/**
 * Updates component model
 * @param {Object} state - Current state
 * @param {string} componentId - Component identifier
 * @param {Object} modelUpdates - Model updates to merge
 * @returns {Object} New state with updated model
 */
function updateComponentModel(state, componentId, modelUpdates) {
  return updateComponentProperty(state, componentId, 'model', modelUpdates);
}

/**
 * Updates component validation rules
 * @param {Object} state - Current state
 * @param {string} componentId - Component identifier
 * @param {Object} validationUpdates - Validation updates to merge
 * @returns {Object} New state with updated validation
 */
function updateComponentValidation(state, componentId, validationUpdates) {
  if (!state[componentId]) return state;

  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      validationRules: {
        ...(state[componentId]?.validationRules || {}),
        ...validationUpdates
      }
    }
  };
}

/**
 * ============================================================================
 * COMPONENT TYPE CHECKS
 * ============================================================================
 */

/**
 * Check if component is a grid or not
 * @param {Object} component
 */
function isGrid(component) {
  return "columnModel" in (component.attributes || {});
}

/**
 * Check if component is part of a group
 * @param {Object} component
 * @return {Boolean} component belongs a group
 */
function isGroup(component) {
  let attributes = component.attributes || {};
  return "group" in attributes && (attributes.component || "").includes("radio");
}

/**
 * Fix cell model
 * @param selected Selected data
 * @param model Select model
 * @returns {object} Model fixed
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
 * Get component id
 * @param cellFunction
 * @param columnFunction
 * @param componentFunction
 * @param state
 * @param action
 * @returns {null|*}
 */
function launchAddressFunction(cellFunction, columnFunction, componentFunction, state, action) {
  switch (getAddressType(action.address)) {
    case ADDRESS_CELL:
      return cellFunction(state, action.address, action.data);
    case ADDRESS_COLUMN:
      return columnFunction(state, action.address, action.data);
    case ADDRESS_COMPONENT:
      return componentFunction(state, action.address, action.data);
    default:
      return state;
  }
}

/**
 * Update attributes for component
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateAttributeComponent(state = {}, address = {}, data = {}) {
  return updateComponentAttributes(state, address.component, data);
}

/**
 * Update attributes for column
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateAttributeColumn(state = {}, address = {}, data = {}) {
  const component = address.component;
  const { attributes } = state[component];
  const { columnModel = [] } = attributes;

  const columnMap = new Map(columnModel.map((col, index) => [col.name, index]));
  const columnIndex = columnMap.get(address.column);

  return {
    ...state,
    [component]: {
      ...state[component],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, data)
      }
    }
  };
}

/**
 * Update attributes for cell
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateAttributeCell(state = {}, address = {}, data = {}) {
  const component = address.component;
  const { model, attributes } = state[component];
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const rowData = values[rowIndex] || {};
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: updateArrayElement(values, rowIndex, {
          ...values[rowIndex], $attrs: {
            ...rowData.$attrs || {},
            [address.column]: { ...cellAttrs, ...data }
          }
        })
      }
    }
  };
}

/**
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateAttributeAction(state = {}, action = {}) {
  return launchAddressFunction(updateAttributeCell, updateAttributeColumn, updateAttributeComponent, state, action);
}

function updateSpecificAttributes(state = {}, action = {}) {
  const { address, data } = action;
  const component = memoizedGetComponentId(address);
  return !(component in state) ? state : {
    ...state,
    [component]: {
      ...state[component],
      specificAttributes: {
        ...state[component].specificAttributes,
        ...data
      }
    }
  };
}

/**
 * Keep validation
 * @param {Object} state
 * @param {Object} component
 * @param {Object} data
 * @return {Object} updated state
 */
function keepAttributeComponent(state, component, data) {
  if (!state[component]) return state;

  return updateComponentInState(state, component, {
    storedAttributes: {
      ...state[component].storedAttributes,
      [data]: {
        ...state[component].attributes[data]
      }
    }
  });
}

/**
 * Restore application
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeComponent(state, address, data) {
  const component = memoizedGetComponentId(address);
  return {
    ...state,
    [component]: {
      ...state[component],
      attributes: {
        ...state[component].attributes,
        [data]: state[component].storedAttributes[data]
      }
    }
  };
}

/**
 * Update attributes for column
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeColumn(state = {}, address = {}, data = {}) {
  const component = address.component;
  const { attributes } = state[component];
  const { columnModel = [] } = attributes;
  const columnIndex = attributes.columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [component]: {
      ...state[component],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, { [data]: state[component].storedAttributes.columnModel[columnIndex][data] })
      }
    }
  };
}

/**
 * Restore attributes for cell
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeCell(state, address, data) {
  const component = address.component;
  const { model, attributes, storedAttributes = {} } = state[component];
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
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: updateArrayElement(values, rowIndex, { ...rowData, $attrs: { ...prevAttrs, [address.column]: newAttr } })
      }
    }
  };
}

/**
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function restoreAttributeAction(state = {}, action = {}) {
  return launchAddressFunction(restoreAttributeCell, restoreAttributeColumn, restoreAttributeComponent, state, action);
}

/**
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateValidationAction(state = {}, action = {}) {
  return launchAddressFunction(updateValidationCell, updateValidationColumn, updateValidationComponent, state, action);
}

/**
 * Update validation
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateValidationComponent(state = {}, address = {}, data = {}) {
  return updateComponentValidation(state, address.component, data);
}

/**
 * Update validation for column
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateValidationColumn(state = {}, address = {}, data = {}) {
  const component = address.component;
  const { attributes } = state[component];
  const { columnModel = [] } = attributes;
  const columnIndex = attributes.columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [component]: {
      ...state[component],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, { validationRules: { ...(columnModel[columnIndex]?.validationRules || {}), ...data } })
      }
    }
  };
}

/**
 * Update validation for cell
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateValidationCell(state = {}, address = {}, data = {}) {
  const component = address.component;
  const { model, attributes } = state[component];
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const rowData = values[rowIndex] || {};
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const currentValidation = cellAttrs.validationRules || {};
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: updateArrayElement(values, rowIndex, {
          ...rowData,
          $attrs: {
            ...(rowData.$attrs || {}),
            [address.column]: {
              ...cellAttrs,
              validationRules: {
                ...currentValidation,
                ...data
              }
            }
          }
        })
      }
    }
  };
}

/**
 * Keep validation
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function keepValidationComponent(state, component) {
  if (!state[component]) return state;

  return updateComponentInState(state, component, {
    storedValidationRules: {
      ...(state[component]?.validationRules || {})
    }
  });
}

/**
 * Restore validation
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function restoreValidationComponent(state, component) {
  if (!state[component]) return state;

  return updateComponentInState(state, component, {
    validationRules: {
      ...(state[component]?.storedValidationRules || {})
    }
  });
}

/**
 * Update attributes
 * @param {Object} state
 * @param {Object} component
 * @param {Object} data
 * @return {Object} updated state
 */
function updateComponentData(state, component, data) {
  if (component == null) return state;
  return updateComponentInState(state, component, data);
}

/**
 * Generate cell components for a grid
 * @param {object} state State
 * @param {object} grid Grid component
 * @param {array} rows Rows to generate
 */
function generateCellComponents(state, grid, rows) {
  let model = grid.model;
  let columns = grid.attributes.columnModel.filter(column => column.component);
  const gridId = memoizedGetGridIdentifier(grid.attributes);
  let cellComponents = {};
  model.values
    .filter(row => rows.includes(row[gridId]))
    .forEach(row => columns
      .filter(column => !(memoizedGetComponentId({ ...grid.address, row: row[gridId], column: column.name }) in state))
      .forEach(column => Object.assign(cellComponents, generateCellComponent(grid, {
        ...grid.address,
        row: row[gridId],
        column: column.name
      }, row[column.name], column))));
  return cellComponents;
}

/**
 * Generate a cell component in redux model
 * @param {object} grid Grid component
 * @param {object} address Address
 * @param {object} cellModel Cell model
 * @param {object} cellAttributes Cell attributes
 * @return {object} Component data
 */
function generateCellComponent(grid, address, cellModel, cellAttributes) {
  let model = fixCellModel(cellModel, cellAttributes.model);
  let componentId = memoizedGetComponentId(address);
  return {
    [componentId]: {
      uid: getUID(),
      address: { ...address },
      model: { ...model },
      storedModel: { ...model },
      attributes: { ...cellAttributes },
      storedAttributes: { ...cellAttributes },
      actions: cellAttributes.actions || [],
      dependencies: cellAttributes.dependencies || [],
      status: (cellAttributes.dependencies || []).length > 0 ? STATUS_DEFINED : STATUS_INITIALIZED
    }
  };
}

/**
 * Update cells model for a grid
 * @param {object} state State
 * @param {object} grid Grid component
 */
function updateCellsModel(state, grid) {
  const { values } = grid.model;
  let columns = grid.attributes.columnModel.filter(column => column.component);
  let cellComponents = {};
  const gridId = memoizedGetGridIdentifier(grid.attributes);
  values
    .forEach(row => columns
      .filter(column => memoizedGetComponentId({ ...grid.address, row: row[gridId], column: column.name }) in state)
      .forEach(column => Object.assign(cellComponents,
        getModelUpdate(state,
          { ...grid.address, row: row[gridId], column: column.name },
          fixCellModel(row[column.name], column.model)
        )
      ))
    );
  return cellComponents;
}

/**
 * Get cell model update
 * @param {object} state State
 * @param {object} address Address
 * @param {object} model Cell model
 * @returns {object} Component update
 */
function getModelUpdate(state, address, model) {
  let componentId = memoizedGetComponentId(address);
  return {
    [componentId]: {
      ...state[componentId],
      model: {
        ...state[componentId].model,
        ...model,
        changed: true
      },
      attributes: {
        ...state[componentId].attributes,
        error: null
      }
    }
  };
}

/**
 * Get group model update
 * @param {object} state State
 * @param {string} view Component view
 * @param {string} group Group
 * @param {object} model Cell model
 * @returns {object} Component update
 */
function getGroupModelUpdate(state, view, group, model) {
  // Check equality to avoid update state if there are no changes
  const selected = model.values.map(item => item.value);
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
 * Get cell model update
 * @param {object} state State
 * @param {object} address Address
 * @param {object} model Cell model
 * @param {boolean} update Update cells model
 * @returns {object} Component update
 */
function getGridModelUpdate(state, address, model, update = true) {
  let componentId = memoizedGetComponentId(address);
  let gridComponent = getModelUpdate(state, address, model);
  let cellsState = model.values && update ? updateCellsModel({ ...state, ...gridComponent }, gridComponent[componentId]) : {};
  return {
    ...gridComponent,
    ...cellsState
  };
}

/**
 * Update model
 * @param {object} state
 * @param {object} address
 * @param {object} data
 * @param {boolean} update Update grid components
 * @return {object} updated state
 */
function updateModel(state, address, data, update = true) {
  let componentId = memoizedGetComponentId(address);
  let component = state[componentId];
  if (component === null) return state;
  let newModel;
  if (isGrid(component) && data.values && update) {
    newModel = getGridModelUpdate(state, address, data, update);
  } else if (isGroup(component)) {
    newModel = getGroupModelUpdate(state, address.view, component.attributes.group, data);
  } else {
    newModel = getModelUpdate(state, address, data);
  }
  return {
    ...state,
    ...newModel
  };
}

/**
 * Update column model (definition) in a grid
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateColumnModel(state = {}, address = {}, data = {}) {
  const component = address.component;
  const { attributes } = state[component];
  const { columnModel = [] } = attributes;
  const columnIndex = attributes.columnModel.findIndex(column => column.name === address.column);
  return {
    ...state,
    [component]: {
      ...state[component],
      attributes: {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, {
          model: {
            ...(columnModel[columnIndex]?.model || {}),
            ...data
          }
        })
      }
    }
  };
}

/**
 * Update cell model
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateCellModel(state, address, data) {
  const component = address.component;
  const { model, attributes } = state[component];
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const newData = getCellModel(getFirstDefinedValue(data.values, values[rowIndex][address.column]), attributes.columnModel.find(column => column.name === address.column));
  const rowData = values[rowIndex] || {};
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const { error, ...otherAttrs } = cellAttrs;
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: updateArrayElement(values, rowIndex, {
          [address.column]: newData,
          $attrs: { ...rowData.$attrs, [address.column]: otherAttrs }
        })
      }
    }
  };
}

/**
 * Update cell selected
 * @param {object} state
 * @param {object} address
 * @param {object} data
 * @return {object} updated state
 */
function updateCellSelected(state, address, data) {
  const component = address.component;
  const { model, attributes } = state[component];
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  let sameValue = checkSelected(data.selected, values[rowIndex][address.column]);
  return sameValue ? state : {
    ...state,
    [component]: {
      ...state[component],
      model: {
        values: updateArrayElement(values, rowIndex, { [address.column]: fixCellModel(data.selected, values[rowIndex][address.column]).values })
      }
    }
  };
}

/**
 * Update selected
 * @param {object} state
 * @param {object} values
 * @param {object} address
 * @param {object} selected
 * @return {object} updated state
 */
function updateSelectedGrid(state, values, address, selected) {
  // Get values to unselect
  const component = state[memoizedGetComponentId(address)];
  const gridId = memoizedGetGridIdentifier(component.attributes);
  let filtered = values;
  let toUnselect = values
    .filter(row => row.selected)
    .filter(row => !selected.map(String).includes(String(row[gridId])))
    .map(row => ({ id: row[gridId], selected: false }));
  let toSelect = selected
    .map(value => ({ id: value, selected: true }));
  // Unselect values
  [...toUnselect, ...toSelect].forEach(item => {
    let index = filtered.findIndex((row) => String(row[gridId]) === String(item.id));
    filtered = updateArrayElement(filtered, index, { selected: item.selected });
  });

  // Update values
  return getGridModelUpdate(state, address, { values: filtered }, false);
}

/**
 * Update selected
 * @param {object} state
 * @param {array} values
 * @param {object} address
 * @param {array} selected
 * @return {object} updated state
 */
function updateSelectedComponent(state, values, address, selected) {
  let component = state[memoizedGetComponentId(address)];
  let filtered = getFilteredValues(values, selected);
  if (_.isEqual(filtered, (component.model?.values))) {
    return null;
  }

  // Update values
  return getModelUpdate(state, address, { values: filtered });
}

/**
 * Retrieve filtered values
 * @param {array} values Values
 * @param {array} selected Selected
 * @returns {*} Filtered values
 */
function getFilteredValues(values, selected) {
  let filtered = values.map((value) => ({ ...value, selected: selected.includes(value.value) }));
  return filtered.filter(value => value.selected).length === 0 ?
    selected.map(value => ({ ...extractCellModel(value), selected: true })) : filtered;
}

/**
 * Update selected
 * @param {object} state
 * @param {array} values
 * @param {object} view
 * @param {object} group * @param {array} selected
 * @return {Object} updated state
 */
function updateSelectedGroup(state, values, view, group, selected) {
  // Check equality to avoid update state if there are no changes
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
 * Update selected
 * @param {object} state
 * @param {object} address
 * @param {object} data
 * @return {object} updated state
 */
function updateSelected(state, address, data) {
  let update = getSelectedUpdate(state, address, data);
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
 * Get update to be done
 * @param {object} state State
 * @param {object} address Address
 * @param {object} data Data
 * @return {object|null} Update to apply
 */
function getSelectedUpdate(state, address, data) {
  let component = state[memoizedGetComponentId(address)];
  let values = (component.model?.values) || [];
  let selected = asArray(data.selected);
  if (isGrid(component)) {
    return updateSelectedGrid(state, values, address, selected);
  } else if (isGroup(component)) {
    return updateSelectedGroup(state, values, address.view, component.attributes.group, selected);
  } else {
    return updateSelectedComponent(state, values, address, selected);
  }
}

/**
 * Check if selected values are the same as cell values
 * @param {array} selectedValues Selected values
 * @param {*} cellValue Cell value
 * @return {boolean} Same selected value
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
 * Update cell values
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateRowModel(state, address, data) {
  const { model, attributes } = state[address.component];
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  return {
    ...state,
    [address.component]: {
      ...state[address.component],
      model: {
        values: updateArrayElement(values, rowIndex, data)
      }
    }
  };
}

/**
 * Keep model component
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function keepModelComponent(state, component) {
  return {
    ...state,
    ...getKeepModelComponent(state, component)
  };
}

/**
 * Get changes for keep model component
 * @param state
 * @param component
 * @returns {{}}
 */
function getKeepModelComponent(state, component) {
  if (!state[component]) return {};

  return {
    [component]: {
      ...state[component],
      storedModel: {
        ...state[component].model,
        values: state[component].model.values.map(value => ({ ...value }))
      }
    }
  };
}

/**
 * Keep row model
 * @param {Object} state
 * @param {Object} address
 * @return {Object} updated state
 */
function keepRowModel(state, address) {
  const { model, attributes } = state[address.component];
  const { values } = model;
  const gridId = memoizedGetGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  let rowValues = values[rowIndex];
  let cellModel = {};
  Object.keys(rowValues).forEach(column => {
    let cellAddress = { ...address, column: column };
    let componentId = memoizedGetComponentId(cellAddress);
    if (componentId in state) {
      cellModel = { ...cellModel, ...getKeepModelComponent(state, componentId) };
    }
  });
  return {
    ...state,
    ...cellModel,
    [address.component]: {
      ...state[address.component],
      storedModel: {
        ...state[address.component].storedModel,
        storedRows: {
          ...state[address.component].storedModel.storedRows,
          [address.row]: {
            ...values[rowIndex]
          }
        }
      }
    }
  };
}

/**
 * Keep model component
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function restoreModelComponent(state, component) {
  return {
    ...state,
    ...getRestoreModelComponent(state, component)
  };
}

/**
 * Get changes for restore model component
 * @param state
 * @param component
 * @returns {{}}
 */
function getRestoreModelComponent(state, component) {
  if (!state[component]) return {};

  return {
    [component]: {
      ...state[component],
      model: {
        ...state[component].storedModel,
        values: state[component].storedModel.values.map(value => ({ ...value })),
        changed: false
      }
    }
  };
}

/**
 * Reset model
 * @param {Object} state
 * @param {Object} address
 * @return {Object} updated state
 */
function resetModel(state, address) {
  let emptyModel = { values: [] };
  if (!isGrid(state[memoizedGetComponentId(address)])) {
    emptyModel = {
      values: (state[memoizedGetComponentId(address)]?.model?.values || []).map(value => ({ ...value, selected: false }))
    };
  } else {
    emptyModel = {
      values: [],
      page: 1,
      total: 1,
      records: 0
    };
  }
  return {
    ...state,
    [address.component]: {
      ...state[address.component],
      attributes: {
        ...state[address.component].attributes,
        error: null
      },
      model: {
        ...state[address.component].model,
        ...emptyModel,
        changed: true
      }
    }
  };
}

/**
 * Update cell model
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function resetCellModel(state, address, data) {
  const { attributes } = state[address.component];
  const gridId = memoizedGetGridIdentifier(attributes);
  let columnModel = attributes.columnModel.filter(value => value[gridId] === address.column)[0] || {};
  return updateCellSelected(state, address, { selected: asArray(columnModel.model?.values) });
}

/**
 * Update model
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateModelAction(state = {}, action = {}) {
  let modelState = state;
  let data = { ...action.data };
  let selectedData = [...asArray(data.selected)];

  // Update model if there are properties other than just 'selected'
  if (Object.keys(data).length > 1 || !('selected' in data)) {
    modelState = launchAddressFunction(updateCellModel, updateColumnModel, updateModel, modelState, {
      address: action.address,
      data
    });
  }

  // Update selected if present
  if ("selected" in data) {
    modelState = launchAddressFunction(updateCellSelected, (s) => s, updateSelected, modelState, {
      address: action.address,
      data: { selected: selectedData },
    });
  }

  return modelState;
}

function clearComponents(state, view) {
  return Object.entries(state)
    .filter((entry) => entry[1].address?.view !== view)
    .reduce((obj, entry) => ({ ...obj, [entry[0]]: entry[1] }), {});
}

/**
 * Action handlers map
 * Each handler is a pure function that takes (state, action) and returns new state
 */
const actionHandlers = {
  [CLEAR_COMPONENTS]: (state, action) => clearComponents(state, action.view),

  [CLEAR_ALL_COMPONENTS]: () => ({}),

  [UPDATE_VIEW_COMPONENTS]: (state, action) => ({
    ...(action.view === "base" ? {} : clearComponents(state, action.view)),
    ...action.data
  }),

  [GENERATE_CELL_COMPONENTS]: (state, action) => ({
    ...state,
    ...generateCellComponents(state, state[action.address.component], action.data)
  }),

  [UPDATE_COMPONENT]: (state, action) =>
    updateComponentData(state, memoizedGetComponentId(action.address), action.data),

  [UPDATE_MULTIPLE_COMPONENTS]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => updateComponentData(newState, memoizedGetComponentId(_action.address), _action),
      state
    ),

  [UPDATE_MULTIPLE_MODELS]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateModelAction(newState, _action), state),

  [UPDATE_ATTRIBUTES]: (state, action) => updateAttributeAction(state, action),

  [UPDATE_SPECIFIC_ATTRIBUTES]: (state, action) => updateSpecificAttributes(state, action),

  [UPDATE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateAttributeAction(newState, _action), state),

  [UPDATE_MODEL]: (state, action) => updateModelAction(state, action),

  [UPDATE_ROW_MODEL]: (state, action) => updateRowModel(state, action.address, action.data),

  [UPDATE_VALIDATION]: (state, action) => updateValidationAction(state, action),

  [UPDATE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce((newState, _action) => updateValidationAction(newState, _action), state),

  [KEEP_VALIDATION]: (state, action) =>
    keepValidationComponent(state, memoizedGetComponentId(action.address)),

  [KEEP_ATTRIBUTE]: (state, action) =>
    keepAttributeComponent(state, memoizedGetComponentId(action.address), action.data),

  [KEEP_MODEL]: (state, action) =>
    keepModelComponent(state, memoizedGetComponentId(action.address)),

  [KEEP_ROW_MODEL]: (state, action) => keepRowModel(state, action.address),

  [RESTORE_VALIDATION]: (state, action) =>
    restoreValidationComponent(state, memoizedGetComponentId(action.address)),

  [RESTORE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreValidationComponent(newState, memoizedGetComponentId(_action.address)),
      state
    ),

  [RESTORE_ATTRIBUTE]: (state, action) => restoreAttributeAction(state, action),

  [RESTORE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => restoreAttributeAction(newState, _action), state),

  [RESTORE_MODEL]: (state, action) =>
    restoreModelComponent(state, memoizedGetComponentId(action.address)),

  [RESTORE_MULTIPLE_MODEL]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreModelComponent(newState, memoizedGetComponentId(_action.address)),
      state
    ),

  [RESET_MODEL]: (state, action) =>
    launchAddressFunction(resetCellModel, (s) => s, resetModel, state, { address: action.address, data: [] }),

  [RESET_MULTIPLE_MODEL]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => launchAddressFunction(resetCellModel, (s) => s, resetModel, newState, { address: _action.address, data: [] }),
      state
    ),

  [VALIDATE_COMPONENTS]: (state, action) =>
    action.componentList.reduce((newState, _action) => validateComponent(newState, _action, action.settings), state),

  [VALIDATE_ROW]: (state, action) => validateRow(state, action.address, action.settings),
};

/**
 * Components reducer
 * @param {Object} state - Current Redux state
 * @param {Object} action - Redux action with type and payload
 * @returns {Object} New state
 */
export function components(state = InitialState, action = {}) {
  const handler = actionHandlers[action.type];
  return handler ? handler(state, action) : state;
}
