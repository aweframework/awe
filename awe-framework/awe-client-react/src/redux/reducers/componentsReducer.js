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

import {getGridIdentifier} from "../../utilities/grid";

import {getUID} from "../actions/settings";
import _ from 'lodash';
import {validateComponent, validateRow} from "./validation";
import {asArray, extractCellModel, extractCellValue, getCellModel, getFirstDefinedValue} from "../../utilities";
import {ComponentAddressType, getAddressType, getComponentId} from "../../utilities/components";

const {STATUS_DEFINED, STATUS_INITIALIZED} = ComponentStatus;
const {ADDRESS_CELL, ADDRESS_COMPONENT} = ComponentAddressType;

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
      values: model.values.map(value => ({...value, selected: selectedString.includes(String(value.value))}))
    };
  } else {
    return {
      values: selectedData.reduce((prev, value) => ({...extractCellModel(value)}), {})
    };
  }
}

/**
 * Get component id
 * @param cellFunction
 * @param componentFunction
 * @param state
 * @param action
 * @returns {null|*}
 */
function launchAddressFunction(cellFunction, componentFunction, state, action) {
  switch (getAddressType(action.address)) {
    case ADDRESS_CELL:
      return cellFunction(state, action.address, action.data);
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
  const component = address.component;
  return !(component in state) ? state : {
    ...state,
    [component]: {
      ...state[component],
      attributes: {
        ...state[component].attributes,
        ...data
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
  const {model, attributes} = state[component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const cellModel = getCellModel(values[rowIndex][address.column], attributes.columnModel.find(column => column.name === address.column));
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: [
          ...values.slice(0, rowIndex),
          {
            ...values[rowIndex],
            [address.column]: {
              ...cellModel,
              ...data
            }
          },
          ...values.slice(rowIndex + 1, values.length)
        ]
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
  return launchAddressFunction(updateAttributeCell, updateAttributeComponent, state, action);
}

function updateSpecificAttributes(state = {}, action = {}) {
  const {address, data} = action;
  const component = getComponentId(address);
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
  return {
    ...state,
    [component]: {
      ...state[component],
      storedAttributes: {
        ...state[component].storedAttributes,
        [data]: {
          ...state[component].attributes[data]
        }
      }
    }
  };
}

/**
 * Restore application
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeComponent(state, address, data) {
  const component = getComponentId(address);
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
 * Restore attributes for cell
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeCell(state, address, data) {
  const component = address.component;
  const {model, attributes} = state[component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const cellModel = getCellModel(values[rowIndex][address.column], attributes.columnModel.find(column => column.name === address.column));
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: [
          ...values.slice(0, rowIndex),
          {
            ...values[rowIndex],
            [address.column]: Object.entries(cellModel)
                .filter(([key]) => key !== data)
                .reduce((prev, [key, value]) => ({...prev, [key]: value}), {})
          },
          ...values.slice(rowIndex + 1, values.length)
        ]
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
  return launchAddressFunction(restoreAttributeCell, restoreAttributeComponent, state, action);
}

/**
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateValidationAction(state = {}, action = {}) {
  return launchAddressFunction(updateValidationCell, updateValidationComponent, state, action);
}

/**
 * Update validation
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateValidationComponent(state = {}, address = {}, data = {}) {
  const component = address.component;
  return !(component in state) ? state : {
    ...state,
    [component]: {
      ...state[component] || {},
      validationRules: {
        ...state[component]?.validationRules || {},
        ...data
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
  const {model, attributes} = state[component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const cellModel = getCellModel(values[rowIndex][address.column], attributes.columnModel.find(column => column.name === address.column));
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: [
          ...values.slice(0, rowIndex),
          {
            ...values[rowIndex],
            [address.column]: {
              ...cellModel,
              validationRules: {
                ...cellModel.validationRules || {},
                ...data
              }
            }
          },
          ...values.slice(rowIndex + 1, values.length)
        ]
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
  return {
    ...state,
    [component]: {
      ...state[component] || {},
      storedValidationRules: {
        ...state[component]?.validationRules || {}
      }
    }
  };
}

/**
 * Restore validation
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function restoreValidationComponent(state, component) {
  return {
    ...state,
    [component]: {
      ...state[component] || {},
      validationRules: {
        ...state[component]?.storedValidationRules || {}
      }
    }
  };
}

/**
 * Update attributes
 * @param {Object} state
 * @param {Object} component
 * @param {Object} data
 * @return {Object} updated state
 */
function updateComponentData(state, component, data) {
  return component == null ? state : {
    ...state,
    [component]: {
      ...state[component],
      ...data
    }
  };
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
  const gridId = getGridIdentifier(grid.attributes);
  let cellComponents = {};
  model.values
    .filter(row => rows.includes(row[gridId]))
    .forEach(row => columns
      .filter(column => !(getComponentId({...grid.address, row: row[gridId], column: column.name}) in state))
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
  let componentId = getComponentId(address);
  return {
    [componentId]: {
      uid: getUID(),
      address: {...address},
      model: {...model},
      storedModel: {...model},
      attributes: {...cellAttributes},
      storedAttributes: {...cellAttributes},
      actions: cellAttributes.actions || [],
      dependencies: cellAttributes.dependencies || [],
      status: cellAttributes.dependencies.length > 0 ? STATUS_DEFINED : STATUS_INITIALIZED
    }
  };
}

/**
 * Update cells model for a grid
 * @param {object} state State
 * @param {object} grid Grid component
 */
function updateCellsModel(state, grid) {
  const {values} = grid.model;
  let columns = grid.attributes.columnModel.filter(column => column.component);
  let cellComponents = {};
  const gridId = getGridIdentifier(grid.attributes);
  values
    .forEach(row => columns
      .filter(column => getComponentId({...grid.address, row: row[gridId], column: column.name}) in state)
      .forEach(column => Object.assign(cellComponents,
        getModelUpdate(state,
          {...grid.address, row: row[gridId], column: column.name},
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
  let componentId = getComponentId(address);
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
      .filter(([name, component]) => (component.address || {}).view === view && (component.attributes || {}).group === group)
      .map(([name, component]) => ({
        name: name,
        value: {
          ...component,
          model: {
            ...component.model,
            values: component.model.values.map(value => ({...value, selected: selected.includes(value.value)})),
            changed: true
          }
        }
      }))
      .reduce((current, entry) => ({...current, [entry.name]: entry.value}), {})
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
  let componentId = getComponentId(address);
  let gridComponent = getModelUpdate(state, address, model);
  let cellsState = model.values && update ? updateCellsModel({...state, ...gridComponent}, gridComponent[componentId]) : {};
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
  let componentId = getComponentId(address);
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
 * Update cell model
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateCellModel(state, address, data) {
  const component = address.component;
  const {model, attributes} = state[component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  const newData = getCellModel(getFirstDefinedValue(data.values, values[rowIndex][address.column]), attributes.columnModel.find(column => column.name === address.column));
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: [
          ...values.slice(0, rowIndex),
          {
            ...values[rowIndex],
            [address.column]: newData
          },
          ...values.slice(rowIndex + 1, values.length)
        ]
      }
    }
  };
}

/**
 * Update cell selected
 * @param {object} state
 * @param {object} address
 * @param {array} data
 * @return {object} updated state
 */
function updateCellSelected(state, address, data) {
  const component = address.component;
  const {model, attributes} = state[component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  let sameValue = checkSelected(data.selected, values[rowIndex][address.column]);
  return sameValue ? state : {
    ...state,
    [component]: {
      ...state[component],
      model: {
        values: [
          ...values.slice(0, rowIndex),
          {
            ...values[rowIndex],
            [address.column]: fixCellModel(data.selected, values[rowIndex][address.column]).values
          },
          ...values.slice(rowIndex + 1, values.length)
        ]
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
 * @param {string} event
 * @return {object} updated state
 */
function updateSelectedGrid(state, values, address, selected, event) {
  // Get values to unselect
  const component = state[getComponentId(address)];
  const gridId = getGridIdentifier(component.attributes);
  let filtered = values;
  let toUnselect = values
    .filter(row => row.selected)
    .filter(row => !selected.map(String).includes(String(row[gridId])))
    .map(row => ({id: row[gridId], selected: false}));
  let toSelect = selected
    .map(value => ({id: value, selected: true}));
  // Unselect values
  [...toUnselect, ...toSelect].forEach(item => {
    let index = filtered.findIndex((row) => String(row[gridId]) === String(item.id));
    filtered = [...filtered.slice(0, index), {
      ...filtered[index],
      selected: item.selected
    }, ...filtered.slice(index + 1, filtered.length)];
  });

  // Update values
  return getGridModelUpdate(state, address, {values: filtered, event}, false);
}

/**
 * Update selected
 * @param {object} state
 * @param {array} values
 * @param {object} address
 * @param {array} selected
 * @param {string} event
 * @return {object} updated state
 */
function updateSelectedComponent(state, values, address, selected, event) {
  let component = state[getComponentId(address)];
  let filtered = getFilteredValues(values, selected);
  if (_.isEqual(filtered, component.model?.values)) {
    return null;
  }

  // Update values
  return getModelUpdate(state, address, {values: filtered, event});
}

/**
 * Retrieve filtered values
 * @param {array} values Values
 * @param {array} selected Selected
 * @returns {*} Filtered values
 */
function getFilteredValues(values, selected) {
  let filtered = values.map((value) => ({...value, selected: selected.includes(value.value)}));
  return filtered.filter(value => value.selected).length === 0 ?
    selected.map(value => ({...extractCellModel(value), selected: true})) : filtered;
}

/**
 * Update selected
 * @param {object} state
 * @param {array} values
 * @param {object} view
 * @param {object} group
 * @param {array} selected
 * @param {string} event
 * @return {Object} updated state
 */
function updateSelectedGroup(state, values, view, group, selected, event) {
  // Check equality to avoid update state if there are no changes
  return {
    ...Object.entries(state)
      .filter(([name, component]) => (component.address || {}).view === view && (component.attributes || {}).group === group)
      .map(([name, component]) => ({
        name: name,
        value: {
          ...component,
          model: {
            ...component.model,
            values: component.model.values.map(value => ({...value, selected: selected.includes(value.value)})),
            changed: true,
            event
          }
        }
      }))
      .reduce((current, entry) => ({...current, [entry.name]: entry.value}), {})
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
  let component = state[getComponentId(address)];
  let values = component.model?.values || [];
  let selected = asArray(data.selected);
  if (isGrid(component)) {
    return updateSelectedGrid(state, values, address, selected, data.event || "");
  } else if (isGroup(component)) {
    return updateSelectedGroup(state, values, address.view, component.attributes.group, selected, data.event || "");
  } else {
    return updateSelectedComponent(state, values, address, selected, data.event || "");
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
  const {model, attributes} = state[address.component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  return {
    ...state,
    [address.component]: {
      ...state[address.component],
      model: {
        values: [
          ...values.slice(0, rowIndex),
          data,
          ...values.slice(rowIndex + 1, values.length)
        ]
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
  return {
    [component]: {
      ...state[component],
      storedModel: {
        ...state[component].model,
        values: state[component].model.values.map(value => ({...value}))
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
  const {model, attributes} = state[address.component];
  const {values} = model;
  const gridId = getGridIdentifier(attributes);
  let rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  let rowValues = values[rowIndex];
  let cellModel = {};
  Object.keys(rowValues).forEach(column => {
    let cellAddress = {...address, column: column};
    let componentId = getComponentId(cellAddress);
    if (componentId in state) {
      cellModel = {...cellModel, ...getKeepModelComponent(state, componentId)};
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
  return {
    [component]: {
      ...state[component],
      model: {
        ...state[component].storedModel,
        values: state[component].storedModel.values.map(value => ({...value})),
        changed: false,
        event: "restore"
      }
    }
  };
}

/**
 * Reset model
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function resetModel(state, address, data) {
  if (isGrid(state[getComponentId(address)])) {
    return {
      ...state,
      [address.component]: {
        ...state[address.component],
        model: {
          ...state[address.component].model,
          values: [],
          page: 1,
          total: 1,
          records: 0
        }
      }
    };
  } else {
    return updateSelected(state, address, {selected: state[address.component].model?.defaultValues || [], event: "reset"});
  }
}

/**
 * Update cell model
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function resetCellModel(state, address, data) {
  const {attributes} = state[address.component];
  const gridId = getGridIdentifier(attributes);
  let columnModel = attributes.columnModel.filter(value => value[gridId] === address.column)[0] || {};
  return updateCellSelected(state, address, asArray((columnModel.model || {}).values));
}

/**
 * Update model
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateModelAction(state = {}, action = {}) {
  let modelState = state;
  let data = {...action.data};
  let selectedData = [...asArray(data.selected)];
  let keysWithoutEvent = _.pullAll(Object.keys(data), ["event"]);
  if (keysWithoutEvent.length > 1 || !keysWithoutEvent.includes("selected")) {
    modelState = launchAddressFunction(updateCellModel, updateModel, modelState, {
      address: action.address,
      data
    });
  }
  if ("selected" in data) {
    modelState = launchAddressFunction(updateCellSelected, updateSelected, modelState, {
      address: action.address,
      data: {selected: selectedData, event: data.event || ""},
    });
  }
  return modelState;
}

function clearComponents(state, view) {
  return Object.entries(state)
    .filter((entry) => entry[1].address.view !== view)
    .reduce((obj, entry) => ({...obj, [entry[0]]: entry[1]}), {});
}

function afterSaveRow(state, component) {
  return {
    ...state,
    [component]: {
      ...state[component],
      model: {
        ...state[component].model,
        values: state[component].model.values.map(row => ({
          ...row,
          selected: false,
          $row: {
            ...(row.$row || {}),
            editing: false,
            editingRow: null,
          }
        })),
        event: "after-save-row"
      }
    }
  };
}

/**
 * Components reducer
 */
export function components(state = {}, action = {}) {
  switch (action.type) {
    case CLEAR_COMPONENTS:
      // Remove the current view components
      return clearComponents(state, action.view);
    case CLEAR_ALL_COMPONENTS:
      // Remove the current view components
      return {};
    case UPDATE_VIEW_COMPONENTS:
      return {
        ...action.view === "base" ? {} : clearComponents(state, action.view),
        ...action.data
      };
    case GENERATE_CELL_COMPONENTS:
      return {
        ...state,
        ...generateCellComponents(state, state[action.address.component], action.data)
      };
    case UPDATE_COMPONENT:
      return updateComponentData(state, getComponentId(action.address), action.data);
    case UPDATE_MULTIPLE_COMPONENTS:
      return action.componentList.reduce((newState, _action) => updateComponentData(newState, getComponentId(_action.address), _action), state);
    case UPDATE_MULTIPLE_MODELS:
      return action.componentList.reduce((newState, _action) => updateModelAction(newState, _action), state);
    case UPDATE_ATTRIBUTES:
      return updateAttributeAction(state, action);
    case UPDATE_SPECIFIC_ATTRIBUTES:
      return updateSpecificAttributes(state, action);
    case UPDATE_MULTIPLE_ATTRIBUTES:
      return action.componentList.reduce((newState, _action) => updateAttributeAction(newState, _action), state);
    case UPDATE_MODEL:
      return updateModelAction(state, action);
    case UPDATE_ROW_MODEL:
      return updateRowModel(state, action.address, action.data);
    case UPDATE_VALIDATION:
      return updateValidationAction(state, action);
    case UPDATE_MULTIPLE_VALIDATION:
      return action.componentList.reduce((newState, _action) => updateValidationAction(newState, _action), state);
    case KEEP_VALIDATION:
      return keepValidationComponent(state, getComponentId(action.address));
    case KEEP_ATTRIBUTE:
      return keepAttributeComponent(state, getComponentId(action.address), action.data);
    case KEEP_MODEL:
      return keepModelComponent(state, getComponentId(action.address));
    case KEEP_ROW_MODEL:
      return keepRowModel(state, action.address);
    case RESTORE_VALIDATION:
      return restoreValidationComponent(state, getComponentId(action.address));
    case RESTORE_MULTIPLE_VALIDATION:
      return action.componentList.reduce((newState, _action) => restoreValidationComponent(newState, getComponentId(_action.address)), state);
    case RESTORE_ATTRIBUTE:
      return restoreAttributeAction(state, action);
    case RESTORE_MULTIPLE_ATTRIBUTES:
      return action.componentList.reduce((newState, _action) => restoreAttributeAction(newState, _action), state);
    case RESTORE_MODEL:
      return restoreModelComponent(state, getComponentId(action.address));
    case RESTORE_MULTIPLE_MODEL:
      return action.componentList.reduce((newState, _action) => restoreModelComponent(newState, getComponentId(_action.address)), state);
    case RESET_MODEL:
      return launchAddressFunction(resetCellModel, resetModel, state, {address: action.address, data: []});
    case RESET_MULTIPLE_MODEL:
      return action.componentList.reduce((newState, _action) => launchAddressFunction(resetCellModel, resetModel, newState, {address: _action.address, data: []}), state);
    case VALIDATE_COMPONENTS:
      return action.componentList.reduce((newState, _action) => validateComponent(newState, _action, action.settings), state);
    case VALIDATE_ROW:
      return validateRow(state, action.address, action.settings);
    case AFTER_SAVE_ROW:
      return afterSaveRow(state, getComponentId(action.address));
    default:
      return state;
  }
}
