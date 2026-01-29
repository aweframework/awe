// Retrieve default settings
import {
  getComponentValue,
  getGroupSelectedValues,
  getSelectedValues
} from "../../utilities";
import { toDate } from "../../utilities/dates";
import validateDate from "validate-date";
import { getComponentId } from "../../utilities/components";
import { extractCellValue, getCellModel, getGridIdentifier } from "../../utilities/grid";
import { isEmpty } from "../../utilities/general";
import ComponentRegistry from '../registry/ComponentRegistry';
import { calculateDeltas } from '../../utilities/mergeUtils';
import _ from 'lodash';

const patterns = {
  TEXT: /^[A-Za-z]+$/,
  TEXT_WHITESPACES: /^\w+\s+\w+$/,
  NUMBER: /^-?(?:\d+|\d{1,3}(?:,\d{3})+)?(?:\.\d+)?$/,
  INTEGER: /^-?\d+$/,
  DIGITS: /^\d+$/,
  EMAIL: /^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/,
  DATE: /^\d{1,2}\/\d{1,2}\/\d{4}$/,
  TIME: /^([0-1]\d|2[0-3]):([0-5]\d):([0-5]\d)$/
};

/**
 * Retrieve external parameter values
 * @param {type} rule Rule to retrieve the parameters
 * @param {object} components Components
 * @param {object} settings Settings
 */
function retrieveExternalParameters(rule, components, settings) {
  const { criterion, setting, value } = rule;
  let parameterValue = null;
  if ("criterion" in rule) {
    parameterValue = getComponentValue(components[criterion]);
  } else if ("setting" in rule) {
    parameterValue = settings[setting];
  } else if ("value" in rule) {
    parameterValue = value;
  }
  return parameterValue;
}

/**
 * Retrieve a value or null if it is not valid
 * @return {object} Group name
 * @param {object} component
 * @param {mixed} value
 * @param {object} components
 */
function getRequiredValue(component, value, components) {
  // Get controller
  let requiredValue = null;
  const { group } = component.attributes;

  if (group) {
    // Retrieve group values (for radio buttons)
    requiredValue = getGroupSelectedValues(component, components);
  } else if (Array.isArray(value)) {
    // Retrieve list values (for multiple)
    if (value.length !== 0) {
      requiredValue = value;
    }
    // Retrieve single values (default)
  } else {
    requiredValue = value;
  }
  return requiredValue;
}

/**
 * Extract the parameters to compare
 * @param {object} ruleMethod rule method
 * @param {object} rule Rule to retrieve the parameters
 * @param {object} component Component to validate
 * @param {object} value Component value
 * @param {object} components Other components
 * @param {object} settings Settings
 */
function extractParameters(ruleMethod, rule, component, value, components, settings) {
  // Get parameters from rule
  let parameters = getParameters(rule, value, components, settings);

  // Format comparison values
  return formatParameters(ruleMethod, component, value, components, parameters);
}

/**
 * Get parameters
 */
function getParameters(rule, value, components, settings) {
  let values = { value1: value };
  const { from, to } = rule;
  if (typeof rule === "object") {
    if ("from" in rule && "to" in rule) {
      return {
        ...rule,
        values: {
          ...values,
          from: retrieveExternalParameters(from, components, settings),
          to: retrieveExternalParameters(to, components, settings)
        }
      };
    } else if ("criterion" in rule || "setting" in rule || "value" in rule) {
      return {
        ...rule,
        values: {
          ...values,
          value2: retrieveExternalParameters(rule, components, settings)
        }
      };
    } else {
      return { ...rule, values };
    }
  } else {
    return {
      values: {
        ...values,
        value2: rule
      }
    };
  }
}

/**
 * Format parameters
 */
function formatParameters(method, component, value, components, parameters) {
  // Format comparison values
  if (!resolveSpecificMethodParameters(method, component, value, components, parameters)) {
    return formatDefinedParameters(parameters);
  }
  return parameters;
}

/**
 * Format parameters which have an specific definition
 * @param parameters
 * @return {*}
 */
function formatDefinedParameters(parameters) {
  switch ((parameters.type || "").toLowerCase()) {
    case "float":
      parameters.values = Object.entries(parameters.values)
        .reduce((prev, [key, value]) => ({ ...prev, [key]: isEmpty(value) ? null : parseFloat(value) }), {});
      break;
    case "integer":
      parameters.values = Object.entries(parameters.values)
        .reduce((prev, [key, value]) => ({ ...prev, [key]: isEmpty(value) ? null : parseInt(value, 10) }), {});
      break;
    case "date":
      parameters.values = Object.entries(parameters.values)
        .reduce((prev, [key, value]) => ({ ...prev, [key]: isEmpty(value) ? null : toDate(value) }), {});
      break;
    default:
      break;
  }
  return parameters;
}

/**
 * Resolve parameters with specific methods
 */
function resolveSpecificMethodParameters(method, component, value, components, parameters) {
  switch (method) {
    case "required":
      parameters.values.value1 = getRequiredValue(component, value, components);
      return true;
    case "checkAtLeast":
      parameters.values.value1 = component.attributes?.group ?
        getGroupSelectedValues(component, components).length :
        getSelectedValues(component).length;
      return true;
    case "equallength":
    case "maxlength":
    case "minlength":
      parameters.values.value1 = String(parameters.values.value1).length;
      parameters.values.value2 = !isEmpty(parameters.values.value2) ? parseInt(parameters.values.value2, 10) : null;
      return true;
    default:
      return false;
  }
}

const ValidationRules = {
  validate: function (ruleMethod, rule, component, value, components, settings) {
    let parameters = extractParameters(ruleMethod, rule, component, value, components, settings);
    if (ruleMethod in ValidationRules) {
      return ValidationRules[ruleMethod](parameters, settings);
    } else {
      console.error(`The method ${ruleMethod} doesn't exist in validation rules`, ValidationRules);
    }
  },
  required: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_REQUIRED" };
    return isEmpty(parameters.values.value1) && parameters.values.value2 ? error : null;
  },
  text: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_TEXT" };
    let passed = new RegExp(patterns.TEXT).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  textWithSpaces: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_TEXT_WHITESPACES" };
    let passed = new RegExp(patterns.TEXT_WHITESPACES).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  number: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_NUMBER" };
    let passed = new RegExp(patterns.NUMBER).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  integer: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_INTEGER" };
    let passed = new RegExp(patterns.INTEGER).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  digits: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DIGITS" };
    let passed = new RegExp(patterns.DIGITS).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  email: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_EMAIL" };
    let passed = new RegExp(patterns.EMAIL).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  date: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DATE" };
    let passed = new RegExp(patterns.DATE).test(String(parameters.values.value1)) &&
      validateDate(String(parameters.values.value1), "boolean", "dd/mm/yyyy");
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  time: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_TIME" };
    let passed = new RegExp(patterns.TIME).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  eq: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_EQUAL" };
    let passed = parameters.values.value1 === parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  ne: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_NOT_EQUAL" };
    let passed = parameters.values.value1 !== parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  lt: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_LESS_THAN" };
    let passed = parameters.values.value1 < parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  le: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_LESS_OR_EQUAL" };
    let passed = parameters.values.value1 <= parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  gt: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_GREATER_THAN" };
    let passed = parameters.values.value1 > parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  ge: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_GREATER_OR_EQUAL" };
    let passed = parameters.values.value1 >= parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  mod: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DIVISIBLE_BY" };
    let passed = parameters.values.value1 % parameters.values.value2 === 0;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  range: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_RANGE" };
    let passed = parameters.values.value1 >= parameters.values.from && parameters.values.value1 <= parameters.values.to;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  equallength: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_EQUAL_LENGTH" };
    let passed = parameters.values.value1 === parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  maxlength: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_MAXLENGTH" };
    let passed = parameters.values.value1 <= parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  minlength: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_MINLENGTH" };
    let passed = parameters.values.value1 >= parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  pattern: function (parameters, settings) {
    let pattern = parameters.values.value2 || settings.passwordPattern;
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_PATTERN" };
    let passed = new RegExp(pattern).test(parameters.values.value1);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  checkAtLeast: function (parameters) {
    let error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_CHECK_AT_LEAST" };
    let passed = parameters.values.value1 >= parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  invalid: function (parameters) {
    return { values: parameters.values, message: parameters.message || "" };
  }
};

function checkIfValid(component, value, components, settings) {
  const { validate } = ValidationRules;

  return Object.entries(component?.validationRules || {})
    .reduce((errorMessage, [ruleMethod, rule]) => errorMessage === null ? validate(ruleMethod, rule, component, value, components, settings) : errorMessage, null);
}

/**
 * Validate a component
 * @param state Current state
 * @param component Component to validate
 * @param settings Application settings
 * @return {object} Check state
 */
export function validateComponent(state, component, settings) {
  const componentId = getComponentId(component.address);
  const useRegistry = settings?.useComponentRegistry;
  let mergedComponents = state;

  if (useRegistry) {
    // We need a full view for cross-component validation
    // Since we are in a reducer, we only have the current deltas (state)
    // We merge them with registry to get the full view
    mergedComponents = ComponentRegistry.getAllIds().reduce((acc, id) => {
      acc[id] = _.merge({}, ComponentRegistry.get(id), state[id] || {});
      return acc;
    }, { ...state });
  }

  const error = checkIfValid(component, getComponentValue(component), mergedComponents, settings);

  if (useRegistry) {
    const base = ComponentRegistry.get(componentId);
    if (base) {
      const currentFull = _.merge({}, base, state[componentId] || {});
      currentFull.attributes = {
        ...(currentFull.attributes || {}),
        error
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
      ...component,
      attributes: {
        ...(component?.attributes || {}),
        error
      }
    }
  };
}

/**
 * Validate a grid row
 * @param state Current state
 * @param address Grid address
 * @param settings Application settings
 * @return {object} Check state
 */
export function validateRow(state, address, settings) {
  let gridId = address.component;
  const useRegistry = settings?.useComponentRegistry;
  let grid;
  let mergedComponents = state;

  if (useRegistry) {
    const base = ComponentRegistry.get(gridId);
    grid = _.merge({}, base, state[gridId] || {});
    // Full view for cross-component validation
    mergedComponents = ComponentRegistry.getAllIds().reduce((acc, id) => {
      acc[id] = _.merge({}, ComponentRegistry.get(id), state[id] || {});
      return acc;
    }, { ...state });
  } else {
    grid = state[gridId];
  }

  if (!grid || !grid.model || !grid.attributes) {
    return state;
  }

  const { model } = grid;
  const { values } = model;
  let columns = grid.attributes.columnModel?.filter(column => column.component) ?? [];
  let rowIndex = values.findIndex((row) => String(row[getGridIdentifier(grid.attributes)]) === String(address.row));
  if (rowIndex < 0) return state;

  const rowAttrs = values[rowIndex]?.$attrs ?? {};
  const updatedRow = {
    ...values[rowIndex],
    ...columns.reduce((prev, column) => ({
      ...prev,
      [column.name]: {
        ...getCellModel(values[rowIndex][column.name], column),
        valid: !!((rowAttrs?.[column.name]?.error) ?? checkIfValid({ ...column, attributes: column }, extractCellValue(values[rowIndex][column.name]), mergedComponents, settings))
      }
    }), {}),
    $attrs: {
      ...rowAttrs,
      ...columns.reduce((prev, column) => ({
        ...prev,
        [column.name]: {
          ...(rowAttrs?.[column.name] ?? {}),
          error: (rowAttrs?.[column.name]?.error) ??
            checkIfValid({ ...column, attributes: (rowAttrs?.[column.name] ?? {}) }, extractCellValue(values[rowIndex][column.name]), mergedComponents, settings)
        }
      }), {})
    },
  };

  if (useRegistry) {
    const base = ComponentRegistry.get(gridId);
    if (base) {
      const currentFull = _.merge({}, base, state[gridId] || {});
      currentFull.model = {
        ...currentFull.model,
        values: [
          ...values.slice(0, rowIndex),
          updatedRow,
          ...values.slice(rowIndex + 1)
        ]
      };
      return {
        ...state,
        [gridId]: calculateDeltas(base, currentFull)
      };
    }
  }

  // Legacy behavior
  return {
    ...state,
    [gridId]: {
      ...state[gridId],
      model: {
        ...state[gridId].model,
        values: [
          ...values.slice(0, rowIndex),
          updatedRow,
          ...values.slice(rowIndex + 1)
        ]
      }
    }
  };
}
