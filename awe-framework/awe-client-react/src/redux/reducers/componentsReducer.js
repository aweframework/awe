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
import { calculateDeltas, mergeComponentState } from '../../utilities/mergeUtils';

import {
  calculateFooterValue,
  extractCellModel,
  extractCellValue,
  getCellModel,
  getGridIdentifier
} from "../../utilities/grid";

import _ from 'lodash';
import { validateComponent, validateRow } from "./validation";
import { asArray, updateArrayElement } from "../../utilities";
import { ComponentAddressType, getAddressType, getComponentId } from "../../utilities/components";
import { getFirstDefinedValue, isEmpty } from "../../utilities/general";

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
function updateComponentInState(state, componentId, updates, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      // Get current merged state (base + existing delta)
      const currentFull = mergeComponentState(base, state[componentId] || {});
      // Apply updates to the full state
      const updatedFull = _.merge({}, currentFull, updates);
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

function updateComponentProperty(state, componentId, propertyName, updates, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      // Get current merged state
      const currentFull = mergeComponentState(base, state[componentId] || {});
      // Apply property update
      currentFull[propertyName] = {
        ...(currentFull[propertyName] || {}),
        ...updates
      };
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
      [propertyName]: {
        ...state[componentId][propertyName],
        ...updates
      }
    }
  };
}

function updateComponentAttributes(state, componentId, attributeUpdates, settings) {
  return updateComponentProperty(state, componentId, 'attributes', attributeUpdates, settings);
}

function updateComponentValidation(state, componentId, validationUpdates, settings) {
  return updateComponentProperty(state, componentId, 'validationRules', validationUpdates, settings);
}

function getGridRowContext(state = {}, address = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  const base = useRegistry ? ComponentRegistry.get(componentId) : null;

  if (useRegistry && !base) return null;

  const component = useRegistry
    ? mergeComponentState(base, state[componentId] || {})
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

function updateGridRowValues(state, context, nextValues) {
  const { componentId, useRegistry, base, component } = context;

  if (useRegistry) {
    const currentFull = {
      ...component,
      model: {
        ...(component.model || {}),
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
 * ============================================================================
 * COMPONENT TYPE CHECKS
 * ============================================================================
 */

/**
 * Check if component is a grid or not
 * @param {Object} component
 */
function isGridComponent(component) {
  return !isEmpty(component) && "columnModel" in (component.attributes || {});
}

/**
 * Check if component is a grid and has footer
 * @param {Object} component
 */
function hasFooter(component) {
  return isGridComponent(component) && (component.attributes || {}).showTotals;
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
 * Update attributes for component
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateAttributeComponent(state = {}, address = {}, data = {}, settings) {
  return updateComponentAttributes(state, address.component, data, settings);
}

/**
 * Update attributes for column
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateAttributeColumn(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(col => col.name === address.column);

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, data)
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
function updateAttributeCell(state = {}, address = {}, data = {}, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const nextValues = updateArrayElement(values, rowIndex, {
    ...rowData,
    $attrs: {
      ...rowData.$attrs || {},
      [address.column]: { ...cellAttrs, ...data }
    }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateAttributeAction(state = {}, action = {}, settings) {
  const nextState = launchAddressFunction(updateAttributeCell, updateAttributeColumn, updateAttributeComponent, state, action, settings);
  const baseAddress = action.address?.component ? { view: action.address.view, component: action.address.component } : action.address;
  const componentId = memoizedGetComponentId(baseAddress);
  if (!componentId) return nextState;
  const useRegistry = settings?.useComponentRegistry;
  const component = useRegistry
    ? mergeComponentState(ComponentRegistry.get(componentId), nextState[componentId] || {})
    : nextState[componentId];
  return hasFooter(component) ? updateGridFooter(nextState, baseAddress, settings) : nextState;
}

function updateSpecificAttributes(state = {}, action = {}, settings) {
  const { address, data } = action;
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
      currentFull.specificAttributes = {
        ...(currentFull.specificAttributes || {}),
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
 * Keep validation
 * @param {Object} state
 * @param {Object} component
 * @param {Object} data
 * @param {Object} settings Settings
 * @return {Object} updated state
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
 * Restore application
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeComponent(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
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
 * Update attributes for column
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeColumn(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
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
 * Restore attributes for cell
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function restoreAttributeCell(state, address, data, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
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
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function restoreAttributeAction(state = {}, action = {}, settings) {
  return launchAddressFunction(restoreAttributeCell, restoreAttributeColumn, restoreAttributeComponent, state, action, settings);
}

/**
 * Update attributes
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
 */
function updateValidationAction(state = {}, action = {}, settings) {
  return launchAddressFunction(updateValidationCell, updateValidationColumn, updateValidationComponent, state, action, settings);
}

/**
 * Update validation
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateValidationComponent(state = {}, address = {}, data = {}, settings) {
  return updateComponentValidation(state, address.component, data, settings);
}

/**
 * Update validation for column
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateValidationColumn(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(col => col.name === address.column);
      if (columnIndex < 0) return state;

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, {
          validationRules: {
            ...(columnModel[columnIndex]?.validationRules || {}),
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
function updateValidationCell(state = {}, address = {}, data = {}, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData } = context;
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const currentValidation = cellAttrs.validationRules || {};
  const nextValues = updateArrayElement(values, rowIndex, {
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
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Keep validation
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function keepValidationComponent(state, componentId, settings) {
  const useRegistry = settings?.useComponentRegistry;
  let validationRules;
  if (useRegistry) {
    const full = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
    validationRules = full.validationRules;
  } else {
    validationRules = state[componentId]?.validationRules;
  }

  return updateComponentInState(state, componentId, {
    storedValidationRules: {
      ...(validationRules || {})
    }
  }, settings);
}

/**
 * Restore validation
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function restoreValidationComponent(state, componentId, settings) {
  const useRegistry = settings?.useComponentRegistry;
  let storedValidationRules;
  if (useRegistry) {
    const full = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
    storedValidationRules = full.storedValidationRules;
  } else {
    storedValidationRules = state[componentId]?.storedValidationRules;
  }

  return updateComponentInState(state, componentId, {
    validationRules: {
      ...(storedValidationRules || {})
    }
  }, settings);
}

/**
 * Update attributes
 * @param {Object} state
 * @param {Object} component
 * @param {Object} data
 * @return {Object} updated state
 */
function updateComponentData(state, componentId, data, settings) {
  if (componentId == null) return state;
  const useRegistry = settings && settings.useComponentRegistry;

  if (useRegistry) {
    // Si data contiene address y attributes, podría ser un componente completo
    // Si no, es una actualización parcial que debe ir directamente a deltas
    if (data.address && data.attributes) {
      // Registrar en Registry (actualizar estado base)
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
 * Update cells model for a grid
 * @param {object} state State
 * @param {object} grid Grid component
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
 * Get cell model update
 * @param {object} state State
 * @param {object} address Address
 * @param {object} model Cell model
 * @returns {object} Component update
 */
function getModelUpdate(state, address, model, settings) {
  let componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      // Get current merged state
      const currentFull = mergeComponentState(base, state[componentId] || {});
      // Apply model update
      currentFull.model = {
        ...(currentFull.model || {}),
        ...model,
        changed: true
      };
      // Reset error on update
      currentFull.attributes = {
        ...(currentFull.attributes || {}),
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
 * Get group model update
 * @param {object} state State
 * @param {string} view Component view
 * @param {string} group Group
 * @param {object} model Cell model
 * @param {object} settings Settings
 * @returns {object} Component update
 */
function getGroupModelUpdate(state, view, group, model, settings) {
  // Check equality to avoid update state if there are no changes
  const selected = model.values.map(item => item.value);
  return updateSelectedGroup(state, model.values, view, group, selected, settings);
}

/**
 * Get cell model update
 * @param {object} state State
 * @param {object} address Address
 * @param {object} model Cell model
 * @param {boolean} update Update cells model
 * @returns {object} Component update
 */
function getGridModelUpdate(state, address, model, update = true, settings) {
  let componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

  if (!component) return state;

  let gridComponent = getModelUpdate(state, address, model, settings);

  // Para updateCellsModel necesitamos el componente mergeado con los cambios que acabamos de calcular
  const updatedFull = mergeComponentState(component, gridComponent[componentId] || {});

  let cellsState = model.values && update ? updateCellsModel({ ...state, ...gridComponent }, updatedFull, settings) : {};
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
function updateModel(state, address, data, update = true, settings) {
  let componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    component = mergeComponentState(base, state[componentId] || {});
  } else {
    component = state[componentId];
  }

  if (component === null || component === undefined) return state;

  // Debug log
  // console.error('updateModel component:', component, typeof component, 'isGrid:', isGridComponent(component), 'data.selected:', data.selected);

  let newModel;
  if (isGridComponent(component) && data.values && update) {
    newModel = getGridModelUpdate(state, address, data, update, settings);
  } else if (isGridComponent(component) && data.selected) {
    // console.error('Entering updateSelectedGrid path, values:', component.model?.values?.length);
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
 * Update column model (definition) in a grid
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function updateColumnModel(state = {}, address = {}, data = {}, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
      const { attributes } = currentFull;
      const { columnModel = [] } = attributes;
      const columnIndex = columnModel.findIndex(column => column.name === address.column);

      currentFull.attributes = {
        ...attributes,
        columnModel: updateArrayElement(columnModel, columnIndex, {
          model: {
            ...(columnModel[columnIndex]?.model || {}),
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
function updateCellModel(state, address, data, settings) {
  const context = getGridRowContext(state, address, settings);
  if (!context) return state;

  const { values, rowIndex, rowData, attributes } = context;
  const newData = getCellModel(
    getFirstDefinedValue(data.values, values[rowIndex][address.column]),
    attributes.columnModel?.find(column => column.name === address.column)
  );
  const cellAttrs = rowData.$attrs?.[address.column] || {};
  const { error, ...otherAttrs } = cellAttrs;
  const nextValues = updateArrayElement(values, rowIndex, {
    [address.column]: newData,
    $attrs: { ...rowData.$attrs, [address.column]: otherAttrs }
  });

  return updateGridRowValues(state, context, nextValues);
}

/**
 * Update cell selected
 * @param {object} state
 * @param {object} address
 * @param {object} data
 * @return {object} updated state
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
 * Update selected
 * @param {object} state
 * @param {object} values
 * @param {object} address
 * @param {object} address
 * @param {object} data
 * @return {object} updated state
 */
function updateSelectedGrid(state, values, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

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
 * Update selected
 * @param {object} state
 * @param {array} values
 * @param {object} address
 * @param {array} selected
 * @return {object} updated state
 */
function updateSelectedComponent(state, values, address, selected, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

  if (!component) return state;

  let filtered = getFilteredValues(values, selected);
  if (_.isEqual(filtered, (component.model?.values))) {
    return null;
  }

  // Update values
  return getModelUpdate(state, address, { values: filtered }, settings);
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
 * Update selected
 * @param {object} state
 * @param {object} address
 * @param {object} data
 * @return {object} updated state
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
 * Get update to be done
 * @param {object} state State
 * @param {object} address Address
 * @param {object} data Data
 * @return {object|null} Update to apply
 */
function getSelectedUpdate(state, address, data, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

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
 * Keep model component
 * @param {Object} state
 * @param {Object} component
 * @return {Object} updated state
 */
function keepModelComponent(state, componentId, settings) {
  return {
    ...state,
    ...getKeepModelComponent(state, componentId, settings)
  };
}

/**
 * Get changes for keep model component
 * @param state
 * @param component
 * @returns {{}}
 */
function getKeepModelComponent(state, componentId, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
      currentFull.storedModel = {
        ...(currentFull.model || {}),
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
 * Keep row model
 * @param {Object} state
 * @param {Object} address
 * @return {Object} updated state
 */
function keepRowModel(state, address, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  let component;
  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

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
      const currentFull = mergeComponentState(base, state[componentId] || {});
      currentFull.storedModel = {
        ...(currentFull.storedModel || {}),
        storedRows: {
          ...(currentFull.storedModel?.storedRows || {}),
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
 * Keep model component
 * @param {Object} state
 * @param {String} componentId
 * @return {Object} updated state
 */
function restoreModelComponent(state, componentId, settings) {
  const newState = {
    ...state,
    ...getRestoreModelComponent(state, componentId, settings)
  };

  const useRegistry = settings?.useComponentRegistry;
  let component;
  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), newState[componentId] || {});
  } else {
    component = newState[componentId];
  }

  // Update footer if is grid and show totals
  if (hasFooter(component)) {
    return updateGridFooter(newState, componentId, settings);
  }

  return newState;
}

/**
 * Get changes for restore model component
 * @param state
 * @param component
 * @returns {{}}
 */
function getRestoreModelComponent(state, componentId, settings) {
  const useRegistry = settings?.useComponentRegistry;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = mergeComponentState(base, state[componentId] || {});
      currentFull.model = {
        ...(currentFull.storedModel || currentFull.model || {}),
        values: (currentFull.storedModel?.values || currentFull.model?.values || []).map(value => ({ ...value })),
        changed: false
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
      model: {
        ...state[componentId].storedModel,
        values: state[componentId].storedModel.values.map(value => ({ ...value })),
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
function resetModel(state, address, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

  let emptyModel = { values: [] };
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
      const currentFull = mergeComponentState(base, state[componentId] || {});
      currentFull.attributes = {
        ...(currentFull.attributes || {}),
        error: null
      };
      currentFull.model = {
        ...(currentFull.model || {}),
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
 * Update cell model
 * @param {Object} state
 * @param {Object} address
 * @param {Object} data
 * @return {Object} updated state
 */
function resetCellModel(state, address, data, settings) {
  const componentId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), state[componentId] || {});
  } else {
    component = state[componentId];
  }

  const { attributes } = component || {};
  if (!attributes) return state;
  const gridId = memoizedGetGridIdentifier(attributes);
  let columnModel = (attributes.columnModel || []).filter(value => value[gridId] === address.column)[0] || {};
  return updateCellSelected(state, address, { selected: asArray(columnModel.model?.values) }, settings);
}

/**
 * Updates the grid footer section based on the provided state and address.
 *
 * @param {Object} state - The current state object containing relevant data and properties for the grid.
 * @param {Object} address - The address information used to update the footer details.
 */
function updateGridFooter(state, address, settings) {
  const componentId = memoizedGetComponentId(address);
  const useRegistry = settings?.useComponentRegistry;
  let component;

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      component = mergeComponentState(base, state[componentId] || {});
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
 * Generates a grid footer by enhancing the provided component object.
 *
 * @param {Object} component - The component object to which the footer will be appended.
 * @return {Object} The updated component object with the generated footer added to the model.
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
 * Generates the footer values for each column in the component based on the provided values.
 *
 * @param {Object} component - The component containing attributes and column model information.
 * @return {Array} The computed footer values for each column.
 */
function generateFooter(component) {
  const { columnModel = [] } = component?.attributes || {};
  return columnModel.reduce((prev, column) => ({ ...prev, [column.name]: calculateFooterValue(column, component.model?.values || []) }), {});
}

/**
 * Update component footers
 * @param data
 * @returns {{}}
 */
function updateComponentFooters(data) {
  return Object.entries(data).reduce((prev, [componentId, component]) => ({ ...prev, [componentId]: hasFooter(component) ? generateGridFooter(component) : component }), {});
}

/**
 * Update model
 * @param {object} state State
 * @param {object} action Action
 * @returns {*} Action updated
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
  let component;
  if (useRegistry) {
    component = mergeComponentState(ComponentRegistry.get(componentId), modelState[componentId] || {});
  } else {
    component = modelState[componentId];
  }

  if (hasFooter(component)) {
    modelState = updateGridFooter(modelState, action.address, settings);
  }

  return modelState;
}

function clearComponents(state, view, settings) {
  const useRegistry = settings?.useComponentRegistry;
  if (useRegistry) {
    const clearedIds = new Set(ComponentRegistry.clear(view));
    return Object.fromEntries(Object.entries(state)
      .filter(([id]) => !clearedIds.has(id)));
  }

  // Legacy behavior
  return Object.fromEntries(Object.entries(state)
    .filter((entry) => entry[1].address?.view !== view));
}

/**
 * Action handlers map
 * Each handler is a pure function that takes (state, action) and returns new state
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
    restoreValidationComponent(state, memoizedGetComponentId(action.address), action.settings),

  [RESTORE_MULTIPLE_VALIDATION]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreValidationComponent(newState, memoizedGetComponentId(_action.address), action.settings),
      state
    ),

  [RESTORE_ATTRIBUTE]: (state, action) => restoreAttributeAction(state, action, action.settings),

  [RESTORE_MULTIPLE_ATTRIBUTES]: (state, action) =>
    action.componentList.reduce((newState, _action) => restoreAttributeAction(newState, _action, action.settings), state),

  [RESTORE_MODEL]: (state, action) =>
    restoreModelComponent(state, memoizedGetComponentId(action.address), action.settings),

  [RESTORE_MULTIPLE_MODEL]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => restoreModelComponent(newState, memoizedGetComponentId(_action.address), action.settings),
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
 * Components reducer
 * @param {Object} state - Current Redux state
 * @param {Object} action - Redux action with type and payload
 * @returns {Object} New state
 */
export function components(state = InitialState, action = {}) {
  const handler = actionHandlers[action.type];
  return handler ? handler(state, action) : state;
}
