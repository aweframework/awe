// Globals
import _ from "lodash";
import {
  resetMultipleModel,
  restoreMultipleAttributes,
  restoreMultipleValidation,
  updateMultipleAttributes,
  updateMultipleModels,
  updateMultipleValidation
} from "./components";

import { addActionsTop } from "./actions";
import { asArray, componentValue, formule, generateAddress, generateServerAction } from "../../utilities";
import { getAllComponents } from "../selectors/componentSelectors";
import { getDependencyComponentId, getTriggerId, parseValidationRules } from "../../utilities/components";
import ViewRegistry from "../registry/ViewRegistry";
import {
  getCellAttribute,
  getCellValue,
  getEditingRow,
  getEditingRowIndex,
  getExistingIndex,
  getFooterValue,
  getGridIdentifier,
  getRowIndex,
  getSelectedRowIndex,
  OperationType
} from "../../utilities/grid";
import { compareEqualValues, getFirstDefinedAndNotNullValue, isEmpty, isEmptyCell } from "../../utilities/general";

/**
 * Manage action list
 * @param actionList
 * @return {{payload: *, type: string}}
 */
function addActions(actionList) {
  return addActionsTop(actionList.flat());
}

const DISPATCH_FUNCTIONS = {
  updateAttributes: updateMultipleAttributes,
  updateValidation: updateMultipleValidation,
  updateModel: updateMultipleModels,
  resetModel: resetMultipleModel,
  restoreAttributes: restoreMultipleAttributes,
  restoreValidation: restoreMultipleValidation,
  addActions
};

const VALUE_DEFERRED = "[[ DEFERRED ]]";
const VALUE_NONE = "[[ NONE ]]";
const VALUE_RESET = "[[ RESET ]]";

const DEPENDENCY_VALUES = {};

const ConditionTest = {
  "eq": (v1, v2, def) => ({ "test": compareEqualValues(v1, v2), "string": def }),
  "ne": (v1, v2, def) => ({ "test": !compareEqualValues(v1, v2), "string": def }),
  "ge": (v1, v2, def) => ({ "test": v1 >= v2, "string": def }),
  "le": (v1, v2, def) => ({ "test": v1 <= v2, "string": def }),
  "gt": (v1, v2, def) => ({ "test": v1 > v2, "string": def }),
  "lt": (v1, v2, def) => ({ "test": v1 < v2, "string": def }),
  "in": (v1, v2, def) => ({
    "test": (_.isString(v2) ? v2.split(",") : asArray(v2)).includes(v1),
    "string": def
  }),
  "is not false": (v1) => ({ "test": Boolean(v1), "string": `'${v1}' is not false` }),
  "is empty": (v1) => ({ "test": isEmpty(v1), "string": `'${v1}' is empty` }),
  "is not empty": (v1) => ({ "test": !isEmpty(v1), "string": `'${v1}' is not empty` })
};

/**
 * Extracts all values from the given nested data structure and converts them to strings.
 *
 * @param {Array|Object} data - The nested data structure (array or object) to extract values from.
 * @param {Array} [acc=[]] - An accumulator array to collect the extracted string values.
 * @return {Array} An array containing all the string values extracted from the nested structure.
 */
function extractValues(data, acc = []) {
  if (Array.isArray(data)) {
    data.forEach(item => extractValues(item, acc));
  } else if (data !== null && typeof data === 'object') {
    Object.values(data).forEach(value => extractValues(value, acc));
  } else {
    acc.push(String(data));
  }
  return acc;
}

/**
 * Computes a hash code for the given string.
 *
 * @param {string} str - The input string for which the hash code is to be calculated.
 * @return {number} The computed hash code as a 32-bit integer.
 */
function hashCode(str) {
  return Array.from(str)
    .reduce((s, c) => Math.imul(31, s) + c.charCodeAt(0) | 0, 0);
}

/**
 * Generate a lightweight hash of the model values
 * @param {Array} values Model values
 * @param {Number} page Model page
 * @param {Number} max Model max elements per page
 * @returns {number} Hash representation
 */
function generateModelHash(values = [], page = 1, max = null) {
  if (max && max > 0 && values.length > max) {
    const offset = (page - 1) * max;
    const pageValues = values.slice(offset, offset + max);
    const code = extractValues(pageValues).join('');
    return hashCode(`${page}_${code}`);
  }

  // Si no, hashear todo (backward compatible)
  const code = extractValues(values).join('');
  return hashCode(`${page}_${code}`);

}

function hashContext(context) {
  if (!context || context.length === 0) {
    return null;
  }
  const code = extractValues(context).join('');
  return hashCode(code);
}

/**
 * Get text attribute from a component
 * @param {object} component
 * @param {object} trigger
 * @return {string|array|number} text value
 */
function getTextAttribute(component, trigger) {
  let modelAttribute = trigger.attribute === "value" ? "value" : "label";
  let rows;

  // Filter rows
  if (trigger.address.column && trigger.address.row) {
    const gridId = getGridIdentifier(component.attributes);
    // Grid cell attribute (defined row)
    rows = (component.model.values || []).filter(row => String(row[gridId]) === String(trigger.address.row));
  } else {
    // Selected rows
    rows = (component.model.values || []).filter(row => row.selected);
  }

  // Extract results
  if (trigger.address.column) {
    // Grid attributes
    return componentValue(rows.map(row => getCellAttribute(row[trigger.address.column], modelAttribute)));
  } else if (component.attributes?.columnModel) {
    // Grid rows length
    return rows.length;
  } else {
    // Criterion attribute
    return componentValue(rows.map(item => item[modelAttribute]));
  }
}

/**
 * Check if group exists
 * @param {string} group Group name
 * @param {object[]} components Components
 */
function isGroup(group, components) {
  return Object.values(components).filter(component => component.attributes?.group === group).length > 0;
}

/**
 * Get component from group or single component
 * @param {string} componentId Component id
 * @param {object[]} components Components
 * @returns {object}
 */
function getComponent(componentId, components) {
  if (isGroup(componentId, components)) {
    const groupComponents = Object.values(components).filter(component => component.attributes?.group === componentId);
    return {
      address: groupComponents.map(component => component.address).reduce((all, address) => ({
        ...all, ...address,
        component: componentId
      }), {}),
      attributes: groupComponents.map(component => component.attributes).reduce((all, attributes) => ({ ...all, ...attributes }), {}),
      model: { values: groupComponents.map(component => component.model.values).flat() }
    };
  } else {
    return components[componentId];
  }
}

/**
 * Check trigger launched
 * @param {object} trigger Triggered state
 * @param {object} state State
 * @return {*} Trigger attribute
 */
function getAttribute(trigger, state) {
  const allComponents = getAllComponents(state);
  let componentId = trigger.address.component;

  // Check if component is defined
  if (!(componentId in allComponents) && !isGroup(componentId, allComponents)) {
    console.warn("[Dependency] WARNING! " + componentId + " is not defined!");
    return null;
  }

  // Check if component exists
  let component = getComponent(componentId, allComponents);

  // First, check event
  if (trigger.event) {
    const runtime = state.runtime;
    const lastEvent = runtime?.lastEvent;
    const { event, address: eventAddress = {} } = lastEvent ?? {};

    // Check if the event matches the component
    const isSameComponent = eventAddress.component === trigger.address.component;
    const isSameView = !trigger.address.view || eventAddress.view === trigger.address.view;
    //const isSameRow = !component.address.row || !eventAddress.row || eventAddress.row === component.address.row;
    //const isSameColumn = !component.address.column || !eventAddress.column || eventAddress.column === component.address.column;

    return isSameComponent && isSameView && /*isSameRow && isSameColumn && */ trigger.event === event;
  }

  // Else, check attributes
  switch (trigger.attribute) {
    // Common attributes
    case "visible":
    case "unit":
    case "label":

    // Chart attributes
    case "xMin":
    case "xMax":
    case "yMin":
    case "yMax":
    case "x":
    case "y":
      return component.attributes[trigger.attribute];

    case "editable":
      return !component.attributes.readonly;

    case "required":
      return component.validationRules.required;

    case "totalValues":
    case "totalRows":
      return component.model.values.length;

    case "selectedValues":
    case "selectedRows":
      return component.model.values.filter(item => item.selected).length;

    case "currentRow":
      return getRowIndex(component.model.values, trigger.address.row);

    case "prevCurrentRow":
      return Math.max(getRowIndex(component.model.values, trigger.address.row) - 1, 0);

    case "nextCurrentRow":
      return Math.min(getRowIndex(component.model.values, trigger.address.row) + 1, component.model.values.length);

    case "prevRowValue":
      return getCellValue(component.model.values, Math.max(getEditingRowIndex(component.model.values) - 1, 0), trigger.address.column);

    case "nextRowValue":
      return getCellValue(component.model.values, Math.min(getEditingRowIndex(component.model.values) + 1, component.model.values.length), trigger.address.column);

    case "selectedRowValue":
      return getCellValue(component.model.values, getExistingIndex([getEditingRowIndex(component.model.values), getSelectedRowIndex(component.model.values)]), trigger.address.column);

    case "footerValue":
      return getFooterValue(component.model.footer, trigger.address.column);

    case "selectedRow":
      let index = getExistingIndex([getEditingRowIndex(component.model.values), getSelectedRowIndex(component.model.values)]);
      return index < 0 ? null : index;

    case "prevRow":
      return Math.max(getEditingRowIndex(component.model.values) - 1, 0);

    case "nextRow":
      return Math.min(getEditingRowIndex(component.model.values) + 1, component.model.values.length);

    case "hasDataColumn":
      return component.model.values.filter(row => !isEmptyCell(row[trigger.address.column])).length > 0;

    case "emptyDataColumn":
      return component.model.values.filter(row => !isEmptyCell(row[trigger.address.column])).length === 0;

    case "fullDataColumn":
      return component.model.values.filter(row => !isEmptyCell(row[trigger.address.column])).length === component.model.values.length;

    case "currentRowValue":
      return getCellValue(component.model.values, getRowIndex(component.model.values, trigger.address.row), trigger.address.column);

    case "prevCurrentRowValue":
      return getCellValue(component.model.values, Math.max(getRowIndex(component.model.values, trigger.address.row) - 1, 0), trigger.address.column);

    case "nextCurrentRowValue":
      return getCellValue(component.model.values, Math.min(getRowIndex(component.model.values, trigger.address.row) + 1, component.model.values.length), trigger.address.column);

    case "modifiedRows":
      return component.model.values.filter(row => !isEmpty(row["ROW_TYPE"])).length;

    case "value":
    case "text":
    default:
      return getTextAttribute(component, trigger);
  }
}

/**
 * Retrieve element triggers
 * @param {object} element Element to get the triggers from
 * @param {object} component Component where the dependency is
 */
function getTriggers(element, component) {
  // Calculate row
  let row = element.row || component.address.row || undefined;

  // Add first trigger
  let triggers = [];

  // Don't check changes if not defined, unless it's an event trigger
  if (element.checkChanges === false && !element.event) {
    return triggers;
  }

  // Add first trigger
  triggers.push({
    address: generateAddress(element.view1 || component.address?.view, element.id, element.column1 || undefined, element.row1 || row),
    attribute: element.attribute1 || "value",
    event: element.event || undefined
  });

  // Add second trigger if it exists
  if ("id2" in element) {
    triggers.push({
      address: generateAddress(element.view2 || component.address?.view, element.id2, element.column2 || undefined, element.row2 || row),
      attribute: element.attribute2 || "value"
    });
  }

  return triggers;
}

// Attributes that depend on the whole grid model (row set or selection), so any model update may change them.
// "currentRowValue" is not one of them: it is the value of a single cell, and its value is already compared
// on every check; launching it on any other model update would validate untouched cells again
// (e.g. the unique check of a key column each time another column of the same row is edited).
const MODEL_CONTEXT_ATTRIBUTES = new Set([
  "selectedRows",
  "selectedRowValue",
  "prevCurrentRowValue",
  "nextCurrentRowValue",
  "prevRowValue",
  "nextRowValue",
  "selectedRow",
  "currentRow",
  "prevCurrentRow",
  "nextCurrentRow",
  "prevRow",
  "nextRow"
]);

/**
 * Check if a trigger reads the default value of a grid (the number of selected rows)
 * @param {object} trigger Trigger
 * @param {object} component Component the trigger points to
 * @returns {boolean} The trigger is the selection count of a grid
 */
function isGridSelectionCount(trigger, component) {
  return Boolean(component?.attributes?.columnModel)
    && !trigger.address.column
    && ["value", "text"].includes(trigger.attribute);
}

function getTriggerModelContext(trigger, state) {
  const allComponents = getAllComponents(state);
  const componentId = trigger.address.component;
  if (!(componentId in allComponents) && !isGroup(componentId, allComponents)) {
    return null;
  }

  const component = getComponent(componentId, allComponents);

  // The value of a grid is the number of selected rows: moving the selection to another row keeps that count, but
  // the dependency must be launched again (AngularJS launches it on every selection change)
  if (isGridSelectionCount(trigger, component)) {
    const gridId = getGridIdentifier(component.attributes);
    return {
      componentId,
      attribute: trigger.attribute,
      selection: (component.model?.values || []).filter(row => row.selected).map(row => row[gridId])
    };
  }

  if (!MODEL_CONTEXT_ATTRIBUTES.has(trigger.attribute)) {
    return null;
  }

  const modelVersion = component?.model?.modelVersion ?? 0;

  return {
    componentId,
    attribute: trigger.attribute,
    modelVersion
  };
}

function getDependencyModelContext(dependency, component, state) {
  return (dependency.elements || [])
    .flatMap(element => getTriggers({ ...element, row: dependency.address?.row }, component))
    .map(trigger => getTriggerModelContext(trigger, state))
    .filter(context => context);
}

/**
 * Check launched trigger
 * @param {object} trigger Trigger
 * @param {object} component Component
 * @param {object} state State
 * @returns {{test: boolean, value: *, string: string}} Evaluation result
 */
function evaluateTrigger(trigger, component, state) {
  // Get triggers
  let triggers = getTriggers(trigger, component);

  // Get triggers attributes
  let [v1, v2] = triggers.map(item => getAttribute(item, state));
  v2 = getFirstDefinedAndNotNullValue(v2, trigger.value);

  // Evaluate trigger
  let condition = trigger.condition || ("event" in trigger ? "is not false" : "is not empty");
  if (condition in ConditionTest) {
    let test = ConditionTest[condition](v1, v2, `'${v1}' ${condition} '${v2}'`);
    test.value = v1;

    // Check optional
    if (trigger.optional) {
      test.test = true;
      test.string += " (optional)";
    }
    return test;
  } else {
    return { test: false, string: `invalid condition: ${condition}` };
  }
}

/**
 * Check full dependency
 * @param dependency Dependency
 * @param component Component
 * @param state State
 * @returns {{launch: boolean, values: {}, string: Array}}
 */
function evaluateDependency(dependency, component, state) {
  // Initialization
  const check = dependency.type === "and" ? (prev, current) => prev && current : (prev, current) => prev || current;
  let result = {
    launch: dependency.type === "and",
    values: {},
    string: []
  };

  // Lazy evaluation. On first failed check of and/or evaluation, return
  (dependency.elements || []).forEach((trigger, index) => {
    let triggerResult = evaluateTrigger({ ...trigger, row: dependency.address?.row }, component, state);
    result.launch = check(result.launch, triggerResult.test);
    result.values[getTriggerId(trigger, dependency, index)] = triggerResult.value;
    result.string.push(triggerResult.string);

    // Don`t trigger if a high priority trigger is not achieved
    if (trigger.cancel && !triggerResult.test) return { ...result, launch: false };

    // Lazy evaluation
    if (!result.launch && dependency.type === "and") return result;
    if (result.launch && dependency.type !== "and") return result;
  });

  // Return full evaluation
  return result;
}

/**
 * Retrieve dependency source
 * @param {Object} dependency Dependency
 * @param {Object} component Component
 * @param {Object} result Condition result
 * @param {Boolean} force Force check
 * @param {Object} state State
 * @param {Object[]} dispatchActions Dispatch actions
 */
function retrieveSource(dependency, component, result, force, state, dispatchActions) {
  let source = getFirstDefinedAndNotNullValue(dependency.source, "none");
  let target = getFirstDefinedAndNotNullValue(dependency.target, "none");
  // Search for source
  switch (source) {
    // Update model with query output
    case "query":
      return retrieveQuerySource(result, target, dependency, component, state, dispatchActions);
    // Update value with criteria value
    case "criteria-value":
    case "criteria-text":
    case "launcher":
      return result.values[dependency.query];
    // Update value with plain text
    case "value":
      return dependency.value;
    // Update value with label text
    case "label":
      return dependency.label;
    // Update value with formule
    case "formule":
      return formule(dependency.formule, result.values);
    // Reset value
    case "reset":
      if (target === "input") {
        return VALUE_RESET;
      }
      return null;
    default:
      return null;
  }
}

/**
 * Retrieve query source
 * @param {Object} result Condition result
 * @param {Object} target Dependency target
 * @param {Object} dependency Dependency
 * @param {Object} component Component
 * @param {Object} state State
 * @param {Object[]} dispatchActions Dispatch function
 */
function retrieveQuerySource(result, target, dependency, component, state, dispatchActions) {
  let values = { ...result.values };
  const { address } = dependency;
  if (result.launch) {
    switch (target) {
      case "label":
        values.controllerAttribute = "label";
        values.type = "update-controller";
        break;
      case "unit":
        values.controllerAttribute = "unit";
        values.type = "update-controller";
        break;
      case "format-number":
        values.controllerAttribute = "numberFormat";
        values.type = "update-controller";
        break;
      case "validate":
        values.controllerAttribute = "validation";
        values.type = "update-controller";
        break;
      case "input":
      default:
        values.type = dependency[state.settings.serverActionKey];
    }

    // Add component identifier and target action
    values.componentId = component.address.component;
    values[state.settings.targetActionKey] = dependency[state.settings.targetActionKey];

    // Launch action list
    const { async, silent } = dependency;
    dispatchActions.push({
      addActions: [generateServerAction({
        ...values,
        screen: ViewRegistry.get(ViewRegistry.getCurrentView())?.name
      },
        values.type || values[state.settings.serverActionKey] || "data",
        values[state.settings.targetActionKey], address, async, silent, state.settings)]
    });
    return VALUE_DEFERRED;
  } else {
    switch (target) {
      case "format-number":
        // Restore numberFormat
        dispatchActions.push({ restoreAttributes: { address, data: "numberFormat" } });
        break;
      case "validate":
        // Restore validation
        dispatchActions.push({ restoreValidation: { address } });
        break;
      default:
    }
  }
  return VALUE_NONE;
}

/**
 * Apply target type
 * @param {object} dependency Dependency
 * @param {object} component Component
 * @param {*} value Dependency value
 * @param {object} result Condition result
 */
function applyTarget(dependency, component, value, result) {
  let target = getFirstDefinedAndNotNullValue(dependency.target, "none");
  const { address } = dependency;
  const { launch } = result;
  const { row, ...addressWithoutRow } = address;

  // Launch can be false only on not filtered cases
  switch (`${target}-${launch}`) {
    case "unit-false":
    case "icon-false":
    case "label-false":
    case "chart-options-false":
    case "attribute-false":
    case "input-false":
      return null;

    case "unit-true":
    case "icon-true":
      return { updateAttributes: { address, data: { [target]: value } } };

    case "label-true":
      return { updateAttributes: { address, data: { [target]: value } } };

    case "chart-options-true":
      // The chart options are Highcharts options and the charts are drawn with ECharts: the result is stored but it
      // does not change the chart
      console.warn("[WARNING] The 'chart-options' dependency is not applied to charts drawn with ECharts");
      return { updateAttributes: { address, data: { chartModel: value } } };

    case "attribute-true":
      return { updateAttributes: { address, data: { [dependency.query]: value } } };

    case "input-true":
      if (value === VALUE_RESET) {
        return { resetModel: { address, data: null } };
      } else {
        return { updateModel: { address, data: { selected: value } } };
      }

    case "format-number-true":
      return { updateAttributes: { address, data: { numberFormat: value } } };

    case "format-number-false":
      return { restoreAttributes: { address, data: "numberFormat" } };

    case "validate-true":
      return { updateValidation: { address, data: parseValidationRules(value, address) } };

    case "validate-false":
      return { restoreValidation: { address, data: address } };

    case "set-required-true":
    case "set-required-false":
      return { updateValidation: { address, data: { required: launch } } };

    case "set-optional-true":
    case "set-optional-false":
      return { updateValidation: { address, data: { required: !launch } } };

    case "show-true":
    case "show-false":
      return { updateAttributes: { address, data: { visible: launch } } };

    case "hide-true":
    case "hide-false":
      return { updateAttributes: { address, data: { visible: !launch } } };

    case "show-column-true":
    case "show-column-false":
      return { updateAttributes: { address: addressWithoutRow, data: { hidden: !launch } } };

    case "hide-column-false":
    case "hide-column-true":
      return { updateAttributes: { address: addressWithoutRow, data: { hidden: launch } } };

    case "set-visible-true":
    case "set-visible-false":
      return { updateAttributes: { address, data: { invisible: !launch } } };

    case "set-invisible-true":
    case "set-invisible-false":
      return { updateAttributes: { address, data: { invisible: launch } } };

    case "enable-true":
    case "enable-false":
      return { updateAttributes: { address, data: { disabled: !launch } } };

    case "disable-true":
    case "disable-false":
      return { updateAttributes: { address, data: { disabled: launch } } };

    case "set-editable-true":
    case "set-editable-false":
      return { updateAttributes: { address, data: { readonly: !launch } } };

    case "set-readonly-true":
    case "set-readonly-false":
      return { updateAttributes: { address, data: { readonly: launch } } };

    case "enable-autorefresh-true":
    case "disable-autorefresh-false":
      return { updateAttributes: { address, data: { autorefreshEnabled: true, autorefresh: value } } };

    case "disable-autorefresh-true":
    case "enable-autorefresh-false":
      return { updateAttributes: { address, data: { autorefreshEnabled: false, autorefresh: 0 } } };

    case "none-true":
    case "none-false":
      return null;

    default:
      console.warn(`Dependency target "${target}" not defined`);
  }

  return null;
}

/**
 * Execute the dependency
 */
function executeDependency(dependency, component, result, state) {
  const { address } = dependency;
  let dispatchActions = [];

  // Launch dependency actions
  if (dependency.actions?.length > 0 && result.launch) {
    dispatchActions.push({ addActions: dependency.actions?.map(action => ({ ...action, address })) });
  }

  // Check force by target
  let target = dependency.target || "none";
  let force = false;

  switch (target) {
    case "label":
    case "columnLabel":
    case "unit":
    case "specific":
    case "input":
      // Do not force update
      break;
    default:
      // Force update
      force = true;
      break;
  }

  // Retrieve dependency source
  let value = retrieveSource(dependency, component, result, force, state, dispatchActions);

  // Set target dependency if value has been defined
  switch (value) {
    case VALUE_DEFERRED:
    case VALUE_NONE:
      break;
    default:
      dispatchActions.push(applyTarget(dependency, component, value, result));
  }

  return dispatchActions.filter(execution => execution);
}

function checkAndStoreResult(dependency, component, state) {
  let result = evaluateDependency(dependency, component, state);
  const modelContext = getDependencyModelContext(dependency, component, state);
  const modelHash = hashContext(modelContext);

  // Store check values
  const componentId = getDependencyComponentId(component.address, { ...dependency.address, index: dependency.index });

  DEPENDENCY_VALUES[component.address?.view] = {
    ...DEPENDENCY_VALUES[component.address?.view],
    [componentId]: {
      ...DEPENDENCY_VALUES[component.address?.view][componentId],
      ...result.values,
      modelHash: modelHash
    }
  };

  return result;
}


function checkDependency(dependency, component, state) {
  let result = checkAndStoreResult(dependency, component, state);

  // Fix result with invert
  result.launch = dependency.invert ? !result.launch : result.launch;

  if (state.settings.activeDependencies) {
    return executeDependency(dependency, component, result, state);
  }

  return [];
}

function initializeDependency(dependency, component, state) {
  checkAndStoreResult(dependency, component, state);
  return dependency.initial;
}

/**
 * Check if a row of a grid is a stored record: it is not one that the user has added or copied
 * @param {object} component Grid component
 * @param {string|number} rowId Row identifier
 * @returns {boolean} The row is a stored record
 */
function isStoredRow(component, rowId) {
  const gridId = getGridIdentifier(component.attributes);
  const row = (component.model?.values || []).find(value => String(value[gridId]) === String(rowId));
  const isNewId = /^(new|copied)-row-/.test(String(rowId));
  return !isNewId && row?.$row?.operation !== OperationType.INSERT;
}

function hasChanged(dependency, component, state) {
  const newValues = evaluateDependency(dependency, component, state).values;
  const componentId = getDependencyComponentId(component.address, { ...dependency.address, index: dependency.index });

  // A unique check validates what the user has just typed. When the row of a stored record shows up for the first
  // time (the user starts editing it) there is nothing typed yet: remember the values and wait for a change,
  // otherwise the row would be reported as a duplicate of itself. A new or copied row may already hold a value
  // (a default one, or the copied one), so it is checked at once.
  if (!DEPENDENCY_VALUES[component.address?.view][componentId] && dependency[state.settings.serverActionKey] === "unique"
    && isStoredRow(component, dependency.address?.row)) {
    checkAndStoreResult(dependency, component, state);
    return false;
  }

  const storedData = DEPENDENCY_VALUES[component.address?.view][componentId] || {};
  const modelContext = getDependencyModelContext(dependency, component, state);
  const currentModelHash = hashContext(modelContext);

  // Comparar valores de triggers
  const oldValues = Object.entries(storedData)
    .filter(([k, v]) => k in newValues && k !== 'modelHash')
    .reduce((p, [k, v]) => ({ ...p, [k]: v }), {});

  const triggerValuesChanged = !_.isEqual(oldValues, newValues);

  const modelHashChanged = storedData.modelHash !== currentModelHash;

  return triggerValuesChanged || modelHashChanged;
}

function getComponentDependencies(component) {
  const gridDependencies = (component.attributes?.columnModel || [])
    .flat()
    .filter(column => (column.dependencies || []).length > 0)
    .flatMap(column => (column.dependencies || [])
      .flatMap(dependency => getColumnDependencies(dependency, column, component))
    );

  return [
    ...((component.dependencies || []).map(dependency => ({ ...dependency, address: component.address }))),
    ...gridDependencies].map((dependency, index) => ({ ...dependency, index }));
}

function getColumnDependencies(dependency, column, component) {
  const { values } = component.model;
  const gridId = getGridIdentifier(component.attributes);
  if (isColumnDependency(dependency)) {
    return values.map(row => ({
      ...dependency,
      address: { ...component.address, column: column.id, row: row[gridId] },
      target: dependency.target
    }));
  } else {
    return [{
      ...dependency,
      address: { ...component.address, column: column.id, row: getEditingRow(component.model.values)[gridId] },
      target: ["show", "hide"].includes(dependency.target) ? dependency.target + "-column" : dependency.target
    }];
  }
}

function isColumnDependency(dependency) {
  return dependency.elements.flatMap(element => [element.attribute1 || "", element.attribute2 || ""])
    .reduce((prev, current) => ["currentRow", "prevCurrentRow", "nextCurrentRow"].includes(current) || prev, false);
}

function getDependenciesExecutions(state, initial, view) {
  const allComponents = getAllComponents(state);
  const executions = _.groupBy(Object.values(allComponents)
    .filter(component => (!initial || component.address?.view === view) && getComponentDependencies(component).length > 0)
    .map(component => getComponentDependencies(component)
      .filter(dependency => initial ? initializeDependency(dependency, component, state) : hasChanged(dependency, component, state))
      .map(dependency => checkDependency(dependency, component, state))
      .filter(e => e.length > 0)
    )
    .filter(e => e.length > 0)
    .flat(2), Object.keys);
  return Object.keys(executions).reduce((prev, key) => ({
    ...prev,
    [key]: executions[key].map(value => value[key])
  }), {});
}

function dispatchExecutions(executions, state, dispatch) {
  Object.keys(executions)
    .filter(key => key in DISPATCH_FUNCTIONS)
    .forEach(key => {
      if (key === 'addActions') {
        dispatch(DISPATCH_FUNCTIONS[key](executions[key]));
      } else {
        dispatch({ ...DISPATCH_FUNCTIONS[key](executions[key]), settings: state.settings });
      }
    });
}

export function checkDependencies(state, dispatch) {
  const executions = getDependenciesExecutions(state, false);
  dispatchExecutions(executions, state, dispatch);
}

export function initializeDependencies(view, state, dispatch) {
  DEPENDENCY_VALUES[view] = {};
  const executions = getDependenciesExecutions(state, true, view);
  dispatchExecutions(executions, state, dispatch);
}
