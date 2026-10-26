// Retrieve default settings
import {getComponentValue, getGroupSelectedValues, getSelectedValues} from "../../utilities";
import {isDateRangeValid, toDate} from "../../utilities/dates";
import validateDate from "validate-date";
import type {ComponentAddress} from "../../contract";
import {extractCellValue, getCellModel, getGridIdentifier} from "../../utilities/grid";
import {ComponentType, isEmpty, isMultipleComponent} from "../../utilities/general";
import ComponentRegistry from '../registry/ComponentRegistry';
import {calculateDeltas} from '../../utilities/mergeUtils';
import type {ComponentsState} from '../types';
import {addressComponentId, mergeStateComponent, resolveComponentId} from './components/stateHelpers';
import type {CellAttributes, ReducerSettings, StateComponent, StateRow} from './components/stateHelpers';

/** A rule of a component: a value or a definition that takes its value from another criterion, a setting or itself */
interface ValidationRule {
  criterion?: string;
  setting?: string;
  value?: unknown;
  from?: ValidationRule;
  to?: ValidationRule;
  type?: string;
  message?: string;
  [key: string]: unknown;
}

/** The rule a component declares: a definition or just the value it compares with */
type Rule = ValidationRule | string | number | boolean;

/** The values of a rule to compare and the definition it comes from */
interface ValidationParameters {
  values: Record<string, unknown>;
  type?: string;
  message?: string;
  [key: string]: unknown;
}

/** What a rule reports when the value is not valid */
export interface ValidationError {
  values: Record<string, unknown>;
  message: string;
}

/** A rule method: it gets the parameters to compare and says what is wrong, or null when the value is valid */
type RuleFunction = (parameters: ValidationParameters, settings?: ReducerSettings) => ValidationError | null;

/** Grid column of the component to validate: its name, the rows of the grid and the cell */
interface ColumnContext {
  name: string;
  rows: StateRow[];
  cell: unknown;
}

/** The components the rules read from, merged with their base */
type Components = Record<string, StateComponent>;

/** The relational rules compare whatever the values are, as JavaScript does: they are read as numbers to say so */
const asComparable = (value: unknown) => value as number;

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
function retrieveExternalParameters(rule: ValidationRule, components: Components, settings?: ReducerSettings): unknown {
  const { criterion, setting, value } = rule;
  let parameterValue = null;
  if ("criterion" in rule) {
    parameterValue = getComponentValue(components[criterion as string]);
  } else if ("setting" in rule) {
    parameterValue = (settings as Record<string, unknown>)[setting as string];
  } else if ("value" in rule) {
    parameterValue = value;
  }
  return parameterValue;
}

/**
 * Check if a component is a date range criterion
 * @param {object} component Component
 * @return {boolean} Component is a date range
 */
function isDateRange(component?: StateComponent): boolean {
  return component?.attributes?.component === ComponentType.COMPONENT_DATE_RANGE;
}

/**
 * Retrieve a value or null if it is not valid
 * @return {object} Group name
 * @param {object} component
 * @param {mixed} value
 * @param {object} components
 */
function getRequiredValue(component: StateComponent, value: unknown, components: Components): unknown {
  // Get controller
  let requiredValue = null;
  const { group } = component.attributes!;

  if (group) {
    // Retrieve group values (for radio buttons)
    requiredValue = getGroupSelectedValues(
      component as Parameters<typeof getGroupSelectedValues>[0],
      components as Parameters<typeof getGroupSelectedValues>[1]
    );
  } else if (isDateRange(component)) {
    // A date range requires both of its dates
    requiredValue = getSelectedValues(component).filter(date => !isEmpty(date)).length === 2 ? value : null;
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
 * @param {object} [column] Grid column of the component: its name and the rows of the grid
 */
function extractParameters(
  ruleMethod: string,
  rule: Rule,
  component: StateComponent,
  value: unknown,
  components: Components,
  settings?: ReducerSettings,
  column?: ColumnContext
): ValidationParameters {
  // Get parameters from rule
  const parameters = getParameters(rule, value, components, settings);

  // Format comparison values
  return formatParameters(ruleMethod, component, value, components, parameters, column);
}

/**
 * Get parameters
 */
function getParameters(rule: Rule, value: unknown, components: Components, settings?: ReducerSettings): ValidationParameters {
  const values: Record<string, unknown> = { value1: value };
  // A rule that is only a value has no from and to
  const { from, to } = rule as ValidationRule;
  if (typeof rule === "object") {
    if ("from" in rule && "to" in rule) {
      return {
        ...rule,
        values: {
          ...values,
          from: retrieveExternalParameters(from as ValidationRule, components, settings),
          to: retrieveExternalParameters(to as ValidationRule, components, settings)
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
function formatParameters(
  method: string,
  component: StateComponent,
  value: unknown,
  components: Components,
  parameters: ValidationParameters,
  column?: ColumnContext
): ValidationParameters {
  // Format comparison values
  if (!resolveSpecificMethodParameters(method, component, value, components, parameters, column)) {
    return formatDefinedParameters(parameters);
  }
  return parameters;
}

/**
 * Format parameters which have an specific definition
 * @param parameters
 * @return {*}
 */
function formatDefinedParameters(parameters: ValidationParameters): ValidationParameters {
  switch ((parameters.type || "").toLowerCase()) {
    case "float":
      parameters.values = Object.entries(parameters.values)
        .reduce((prev, [key, value]) => ({ ...prev, [key]: isEmpty(value) ? null : parseFloat(String(value)) }), {});
      break;
    case "integer":
      parameters.values = Object.entries(parameters.values)
        .reduce((prev, [key, value]) => ({ ...prev, [key]: isEmpty(value) ? null : parseInt(String(value), 10) }), {});
      break;
    case "date":
      parameters.values = Object.entries(parameters.values)
        .reduce((prev, [key, value]) => ({ ...prev, [key]: isEmpty(value) ? null : toDate(value as string) }), {});
      break;
    default:
      break;
  }
  return parameters;
}

/**
 * Resolve parameters with specific methods
 */
function resolveSpecificMethodParameters(
  method: string,
  component: StateComponent,
  value: unknown,
  components: Components,
  parameters: ValidationParameters,
  column?: ColumnContext
): boolean {
  switch (method) {
    case "required":
      parameters.values.value1 = getRequiredValue(component, value, components);
      return true;
    case "checkAtLeast":
      parameters.values.value1 = component.attributes?.group ?
        getGroupSelectedValues(
          component as Parameters<typeof getGroupSelectedValues>[0],
          components as Parameters<typeof getGroupSelectedValues>[1]
        ).length :
        getSelectedValues(component).length;
      return true;
    case "maxRepeat":
      parameters.values.value1 = countRepeatedValue(value, column);
      // The limit is compared as it is written, as AngularJS does (an empty limit skips the rule)
      return true;
    case "dateRange":
      [parameters.values.from, parameters.values.to] = getSelectedValues(component);
      return true;
    case "equallength":
    case "maxlength":
    case "minlength":
      parameters.values.value1 = String(parameters.values.value1).length;
      parameters.values.value2 = !isEmpty(parameters.values.value2) ? parseInt(String(parameters.values.value2), 10) : null;
      return true;
    default:
      return false;
  }
}

/**
 * Count the rows of a grid column that have the same value (the row of the value included), as AngularJS does:
 * the values are compared strictly (no coercion, case or trimming), empty values included, and the cells with
 * several values are arrays that never equal each other
 * @param {*} value Value of the cell
 * @param {object} [column] Grid column: its name, the rows of the grid and the cell
 * @return {number|null} Number of rows with the value or null if it is not a cell of a grid
 */
function countRepeatedValue(value: unknown, column?: ColumnContext): number | null {
  if (!column) {
    return null;
  }
  if (Array.isArray(column.cell)) {
    return 0;
  }
  return column.rows.filter(row => extractCellValue(row?.[column.name]) === value).length;
}

const ValidationRules = {
  validate: function (
    ruleMethod: string,
    rule: Rule,
    component: StateComponent,
    value: unknown,
    components: Components,
    settings?: ReducerSettings,
    column?: ColumnContext
  ): ValidationError | null | undefined {
    const parameters = extractParameters(ruleMethod, rule, component, value, components, settings, column);
    if (ruleMethod in ValidationRules) {
      // The rule methods are the other members of this object
      return (ValidationRules as unknown as Record<string, RuleFunction>)[ruleMethod](parameters, settings);
    } else {
      console.error(`The method ${ruleMethod} doesn't exist in validation rules`, ValidationRules);
    }
  },
  required: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_REQUIRED" };
    return isEmpty(parameters.values.value1) && parameters.values.value2 ? error : null;
  },
  text: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_TEXT" };
    const passed = new RegExp(patterns.TEXT).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  textWithSpaces: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_TEXT_WHITESPACES" };
    const passed = new RegExp(patterns.TEXT_WHITESPACES).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  number: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_NUMBER" };
    const passed = new RegExp(patterns.NUMBER).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  integer: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_INTEGER" };
    const passed = new RegExp(patterns.INTEGER).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  digits: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DIGITS" };
    const passed = new RegExp(patterns.DIGITS).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  email: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_EMAIL" };
    const passed = new RegExp(patterns.EMAIL).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  date: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DATE" };
    const passed = new RegExp(patterns.DATE).test(String(parameters.values.value1)) &&
      validateDate(String(parameters.values.value1), "boolean", "dd/mm/yyyy");
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  time: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_TIME" };
    const passed = new RegExp(patterns.TIME).test(String(parameters.values.value1));
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  eq: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_EQUAL" };
    const passed = parameters.values.value1 === parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  ne: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_NOT_EQUAL" };
    const passed = parameters.values.value1 !== parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  lt: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_LESS_THAN" };
    const passed = asComparable(parameters.values.value1) < asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  le: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_LESS_OR_EQUAL" };
    const passed = asComparable(parameters.values.value1) <= asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  gt: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_GREATER_THAN" };
    const passed = asComparable(parameters.values.value1) > asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  ge: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_GREATER_OR_EQUAL" };
    const passed = asComparable(parameters.values.value1) >= asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  mod: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DIVISIBLE_BY" };
    const passed = asComparable(parameters.values.value1) % asComparable(parameters.values.value2) === 0;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  range: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_RANGE" };
    const passed = asComparable(parameters.values.value1) >= asComparable(parameters.values.from) &&
      asComparable(parameters.values.value1) <= asComparable(parameters.values.to);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  equallength: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_EQUAL_LENGTH" };
    const passed = parameters.values.value1 === parameters.values.value2;
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  maxlength: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_MAXLENGTH" };
    const passed = asComparable(parameters.values.value1) <= asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  minlength: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_MINLENGTH" };
    const passed = asComparable(parameters.values.value1) >= asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  pattern: function (parameters: ValidationParameters, settings?: ReducerSettings) {
    const pattern = parameters.values.value2 || settings!.passwordPattern;
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_PATTERN" };
    const passed = new RegExp(pattern as string).test(parameters.values.value1 as string);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  maxRepeat: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_MAX_REPEAT" };
    const passed = asComparable(parameters.values.value1) <= asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || isEmpty(parameters.values.value2) || passed ? null : error;
  },
  checkAtLeast: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_CHECK_AT_LEAST" };
    const passed = asComparable(parameters.values.value1) >= asComparable(parameters.values.value2);
    return isEmpty(parameters.values.value1) || passed ? null : error;
  },
  dateRange: function (parameters: ValidationParameters) {
    const error = { values: parameters.values, message: parameters.message || "VALIDATOR_MESSAGE_DATE_RANGE" };
    return isDateRangeValid(parameters.values.from as string, parameters.values.to as string) ? null : error;
  },
  invalid: function (parameters: ValidationParameters) {
    return { values: parameters.values, message: parameters.message || "" };
  }
};

function checkIfValid(
  component: StateComponent,
  value: unknown,
  components: Components,
  settings?: ReducerSettings,
  column?: ColumnContext
): ValidationError | null | undefined {
  const { validate } = ValidationRules;

  // The end of a date range cannot be before its start
  const rules = isDateRange(component) ? { ...component?.validationRules, dateRange: true } : component?.validationRules;

  return Object.entries(rules || {})
    .reduce<ValidationError | null | undefined>((errorMessage, [ruleMethod, rule]) => errorMessage === null ? validate(ruleMethod, rule as Rule, component, value, components, settings, column) : errorMessage, null);
}

/**
 * Merge the whole state with the registry: the full view the cross-component validations need.
 * Since we are in a reducer, we only have the current deltas (state)
 * @param state Current state
 * @return {object} Components by id
 */
function mergeAllComponents(state: ComponentsState): Components {
  return ComponentRegistry.getAllIds().reduce((acc, id) => {
    acc[id] = mergeStateComponent(ComponentRegistry.get(id), state[id] || {});
    return acc;
  }, { ...state } as Components);
}

/**
 * Validate a component
 * @param state Current state
 * @param component Component to validate
 * @param settings Application settings
 * @return {object} Check state
 */
export function validateComponent(state: ComponentsState, component: StateComponent, settings?: ReducerSettings): ComponentsState {
  const componentId = resolveComponentId(component.address);

  // We need a full view for cross-component validation
  // Since we are in a reducer, we only have the current deltas (state)
  // We merge them with registry to get the full view
  const mergedComponents = mergeAllComponents(state);

  const error = checkIfValid(component, getComponentValue(component), mergedComponents, settings);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId] || {});
    currentFull.attributes = {
      ...(currentFull.attributes || {}),
      error
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
export function validateRow(state: ComponentsState, address: ComponentAddress, settings?: ReducerSettings): ComponentsState {
  const gridId = addressComponentId(address);
  const grid = mergeStateComponent(ComponentRegistry.get(gridId), state[gridId] || {});
  // Full view for cross-component validation
  const mergedComponents = mergeAllComponents(state);

  if (!grid || !grid.model || !grid.attributes) {
    return state;
  }

  const { model } = grid;
  const values = model.values!;
  const columns = grid.attributes.columnModel?.filter(column => column.component) ?? [];
  const rowIndex = values.findIndex((row) => String(row[getGridIdentifier(grid.attributes)]) === String(address.row));
  if (rowIndex < 0) return state;

  const rowAttrs: Record<string, CellAttributes> = values[rowIndex]?.$attrs ?? {};
  const updatedRow: StateRow = {
    ...values[rowIndex],
    ...columns.reduce<Record<string, unknown>>((prev, column) => ({
      ...prev,
      [column.name]: (() => {
        const cellValue = values[rowIndex][column.name];
        const isMultiple = isMultipleComponent(column.component);
        const valid = !!((rowAttrs?.[column.name]?.error) ??
          checkIfValid({ ...column, attributes: column }, extractCellValue(cellValue), mergedComponents, settings, { name: column.name, rows: values, cell: values[rowIndex][column.name] }));
        if (isMultiple && Array.isArray(cellValue)) {
          return cellValue;
        }
        return {
          ...getCellModel(cellValue, column),
          valid
        };
      })()
    }), {}),
    $attrs: {
      ...rowAttrs,
      ...columns.reduce<Record<string, CellAttributes>>((prev, column) => ({
        ...prev,
        [column.name]: {
          ...(rowAttrs?.[column.name] ?? {}),
          error: (rowAttrs?.[column.name]?.error) ??
            checkIfValid({ ...column, attributes: (rowAttrs?.[column.name] ?? {}) }, extractCellValue(values[rowIndex][column.name]), mergedComponents, settings, { name: column.name, rows: values, cell: values[rowIndex][column.name] })
        }
      }), {})
    },
  };

  const updatedValues = [
    ...values.slice(0, rowIndex),
    updatedRow,
    ...values.slice(rowIndex + 1)
  ];

  const base = ComponentRegistry.get(gridId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[gridId] || {});
    currentFull.model = {
      ...currentFull.model,
      values: updatedValues
    };
    return {
      ...state,
      [gridId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  return {
    ...state,
    [gridId]: {
      ...state[gridId],
      model: {
        ...state[gridId].model,
        values: updatedValues
      }
    }
  };
}
