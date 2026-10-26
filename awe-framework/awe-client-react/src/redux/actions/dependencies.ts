// Globals
import _ from "lodash";
import type {Dispatch, UnknownAction} from "@reduxjs/toolkit";
import type {ComponentAddress, Dependency, DependencyElement} from "../../contract";
import type {RootState} from "../types";
import {
  resetMultipleModel,
  restoreMultipleAttributes,
  restoreMultipleValidation,
  updateMultipleAttributes,
  updateMultipleModels,
  updateMultipleValidation
} from "./components";

import { addActionsTop } from "./actions";
import type { ActionItem } from "./actions";
import { componentValue, formule, generateAddress, generateServerAction, replaceWildcard } from "../../utilities";
import type { Address } from "../../utilities";
import { getAllComponents } from "../selectors/componentSelectors";
import { getDependencyComponentId, getTriggerId, parseValidationRules } from "../../utilities/components";
import ViewRegistry from "../registry/ViewRegistry";
import SpecificDependencyRegistry from "../registry/SpecificDependencyRegistry";
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

/** Value a dependency reads from a component or a trigger: anything the model or the attributes hold */
type Value = unknown;

/** A row of a component model; the grid keeps its own keys next to the cells */
interface ModelRow {
  selected?: boolean;
  $row?: {operation?: string};
  [key: string]: unknown;
}

/** Address of a component: the dependencies always know its view and its component */
type DependencyAddress = Address;

/** An element of a dependency, the trigger the dependency listens to */
interface TriggerElement extends DependencyElement {
  row?: string;
  row2?: string;
}

/** The merged state of a component, as the dependencies read it */
interface DependencyComponent {
  address: DependencyAddress;
  // Attributes keep the contract of every component: the keys are read by name, so they are not typed one by one
  attributes: Record<string, any>; // eslint-disable-line @typescript-eslint/no-explicit-any
  model: {values: ModelRow[]; footer?: Record<string, unknown>; modelVersion?: number};
  validationRules: {required?: boolean};
  dependencies?: Dependency[];
}

/** A dependency of a component, placed at an address and numbered among the ones of its component */
interface PlacedDependency extends Dependency {
  address: DependencyAddress;
  index?: number;
  [key: string]: unknown;
}

/** What a trigger of a dependency element points to */
interface Trigger {
  address: DependencyAddress;
  attribute: string;
  event?: string;
}

/** The evaluation of a whole dependency */
interface DependencyResult {
  launch: boolean;
  values: Record<string, Value>;
  string: string[];
}

/** Context of the model of a component that a dependency depends on */
type ModelContext = {componentId: string; attribute: string; modelVersion?: number; selection?: unknown[]} | null;

/** A column of a grid, as the dependencies read it */
interface GridColumn {
  id?: string;
  dependencies?: Dependency[];
}

/** Component that raises an event and the event */
interface EventLauncher {
  address: DependencyAddress;
  event: string;
}

/** An execution a dependency produces: the action to dispatch by its key */
type Execution = Record<string, unknown>;

/**
 * Address of a trigger. The view and the identifier of a trigger may be missing, and the address is made with them as they are
 */
function triggerAddress(view: string | undefined, id: string | undefined, column?: string, row?: string): DependencyAddress {
  return generateAddress(view as string, id as string, column, row);
}

/**
 * Components of the state, as the dependencies read them
 */
function getComponents(state: RootState): Record<string, DependencyComponent> {
  return getAllComponents(state) as unknown as Record<string, DependencyComponent>;
}

/**
 * Manage action list
 * @param actionList
 * @return {{payload: *, type: string}}
 */
function addActions(actionList: ActionItem[][]) {
  return addActionsTop(actionList.flat());
}

// The executions are built by this module from the targets of the dependencies, so each one has the list its creator takes
const DISPATCH_FUNCTIONS = {
  updateAttributes: updateMultipleAttributes,
  updateValidation: updateMultipleValidation,
  updateModel: updateMultipleModels,
  resetModel: resetMultipleModel,
  restoreAttributes: restoreMultipleAttributes,
  restoreValidation: restoreMultipleValidation,
  addActions
} as unknown as Record<string, (list: unknown[]) => UnknownAction>;

const VALUE_DEFERRED = "[[ DEFERRED ]]";
const VALUE_NONE = "[[ NONE ]]";
const VALUE_RESET = "[[ RESET ]]";

const DEPENDENCY_VALUES: Record<string, Record<string, Record<string, unknown>>> = {};

/**
 * Check if the text of a value contains the text of another one (case insensitive, like AngularJS)
 * @param {*} v1 Value
 * @param {*} v2 Searched value
 * @returns {boolean} Value contains the searched value
 */
function compareContainValues(v1: Value, v2: Value): boolean {
  const toText = (value: Value) => (isEmpty(value) ? "" : String(value)).toUpperCase();
  return toText(v1).indexOf(toText(v2)) > -1;
}

/**
 * Check if a value is one of the given ones (like AngularJS)
 * @param {*} v1 Value
 * @param {*} v2 Array, comma separated text or single value
 * @returns {boolean} Value is in the given values
 */
function compareInValues(v1: Value, v2: Value): boolean {
  if (Array.isArray(v2)) {
    return v2.includes(v1);
  }
  if (_.isString(v2)) {
    return v2.split(",").includes(String(v1));
  }
  return compareEqualValues(v1, v2);
}

type ConditionResult = {test: boolean; string: string; value?: Value};

// Relational operators compare whatever the values are, as JavaScript does: they are read as numbers to say so
const asComparable = (value: Value) => value as number;

const ConditionTest: Record<string, (v1: Value, v2: Value, def: string) => ConditionResult> = {
  "eq": (v1, v2, def) => ({ "test": compareEqualValues(v1, v2), "string": def }),
  "ne": (v1, v2, def) => ({ "test": !compareEqualValues(v1, v2), "string": def }),
  "ge": (v1, v2, def) => ({ "test": asComparable(v1) >= asComparable(v2), "string": def }),
  "le": (v1, v2, def) => ({ "test": asComparable(v1) <= asComparable(v2), "string": def }),
  "gt": (v1, v2, def) => ({ "test": asComparable(v1) > asComparable(v2), "string": def }),
  "lt": (v1, v2, def) => ({ "test": asComparable(v1) < asComparable(v2), "string": def }),
  "contains": (v1, v2, def) => ({ "test": compareContainValues(v1, v2), "string": def }),
  "not contains": (v1, v2, def) => ({ "test": !compareContainValues(v1, v2), "string": def }),
  "in": (v1, v2, def) => ({ "test": compareInValues(v1, v2), "string": def }),
  "not in": (v1, v2, def) => ({ "test": !compareInValues(v1, v2), "string": def }),
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
function extractValues(data: unknown, acc: string[] = []): string[] {
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
function hashCode(str: string): number {
  return Array.from(str)
    .reduce((s, c) => Math.imul(31, s) + c.charCodeAt(0) | 0, 0);
}

function hashContext(context: unknown[] | null | undefined): number | null {
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
function getTextAttribute(component: DependencyComponent, trigger: Trigger) {
  const modelAttribute = trigger.attribute === "value" ? "value" : "label";
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
    return componentValue(rows.map(row => getCellAttribute(row[trigger.address.column as string], modelAttribute)));
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
function isGroup(group: string, components: Record<string, DependencyComponent>): boolean {
  return Object.values(components).filter(component => component.attributes?.group === group).length > 0;
}

/**
 * Get component from group or single component
 * @param {string} componentId Component id
 * @param {object[]} components Components
 * @returns {object}
 */
function getComponent(componentId: string, components: Record<string, DependencyComponent>): DependencyComponent {
  if (isGroup(componentId, components)) {
    const groupComponents = Object.values(components).filter(component => component.attributes?.group === componentId);
    return {
      address: groupComponents.map(component => component.address).reduce((all, address) => ({
        ...all, ...address,
        component: componentId
      }), {} as DependencyAddress),
      attributes: groupComponents.map(component => component.attributes).reduce((all, attributes) => ({ ...all, ...attributes }), {}),
      model: { values: groupComponents.map(component => component.model.values).flat() }
      // A group has no validation rules: reading them fails, as it always did
    } as DependencyComponent;
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
function getAttribute(trigger: Trigger, state: RootState): Value {
  const allComponents = getComponents(state);
  const componentId = trigger.address.component;

  // Check if component is defined
  if (!(componentId in allComponents) && !isGroup(componentId, allComponents)) {
    console.warn("[Dependency] WARNING! " + componentId + " is not defined!");
    return null;
  }

  // Check if component exists
  const component = getComponent(componentId, allComponents);

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

  // Else, check attributes: the row and the column are only there for the attributes of a grid cell
  const rowId = trigger.address.row as string;
  const columnId = trigger.address.column as string;
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
      return getRowIndex(component.model.values, rowId);

    case "prevCurrentRow":
      return Math.max(getRowIndex(component.model.values, rowId) - 1, 0);

    case "nextCurrentRow":
      return Math.min(getRowIndex(component.model.values, rowId) + 1, component.model.values.length);

    case "prevRowValue":
      return getCellValue(component.model.values, Math.max(getEditingRowIndex(component.model.values) - 1, 0), columnId);

    case "nextRowValue":
      return getCellValue(component.model.values, Math.min(getEditingRowIndex(component.model.values) + 1, component.model.values.length), columnId);

    case "selectedRowValue":
      return getCellValue(component.model.values, getExistingIndex([getEditingRowIndex(component.model.values), getSelectedRowIndex(component.model.values)]), columnId);

    case "footerValue":
      return getFooterValue(component.model.footer, columnId);

    case "selectedRow":
      const index = getExistingIndex([getEditingRowIndex(component.model.values), getSelectedRowIndex(component.model.values)]);
      return index < 0 ? null : index;

    case "prevRow":
      return Math.max(getEditingRowIndex(component.model.values) - 1, 0);

    case "nextRow":
      return Math.min(getEditingRowIndex(component.model.values) + 1, component.model.values.length);

    case "hasDataColumn":
      return component.model.values.filter(row => !isEmptyCell(row[columnId])).length > 0;

    case "emptyDataColumn":
      return component.model.values.filter(row => !isEmptyCell(row[columnId])).length === 0;

    case "fullDataColumn":
      return component.model.values.filter(row => !isEmptyCell(row[columnId])).length === component.model.values.length;

    case "currentRowValue":
      return getCellValue(component.model.values, getRowIndex(component.model.values, rowId), columnId);

    case "prevCurrentRowValue":
      return getCellValue(component.model.values, Math.max(getRowIndex(component.model.values, rowId) - 1, 0), columnId);

    case "nextCurrentRowValue":
      return getCellValue(component.model.values, Math.min(getRowIndex(component.model.values, rowId) + 1, component.model.values.length), columnId);

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
 * @param {boolean} forChanges The triggers are used to detect changes (elements with check-changes="false" have none)
 */
function getTriggers(element: TriggerElement, component: DependencyComponent, forChanges = false): Trigger[] {
  // Calculate row
  const row = element.row || component.address.row || undefined;

  // Add first trigger
  const triggers: Trigger[] = [];

  // Don't check changes if not defined, unless it's an event trigger
  if (forChanges && element.checkChanges === false && !element.event) {
    return triggers;
  }

  // Add first trigger
  triggers.push({
    address: triggerAddress(element.view1 || component.address?.view, element.id, element.column1 || undefined, element.row1 || row),
    attribute: element.attribute1 || "value",
    event: element.event || undefined
  });

  // Add second trigger if it exists
  if ("id2" in element) {
    triggers.push({
      address: triggerAddress(element.view2 || component.address?.view, element.id2, element.column2 || undefined, element.row2 || row),
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
function isGridSelectionCount(trigger: Trigger, component: DependencyComponent): boolean {
  return Boolean(component?.attributes?.columnModel)
    && !trigger.address.column
    && ["value", "text"].includes(trigger.attribute);
}

function getTriggerModelContext(trigger: Trigger, state: RootState): ModelContext {
  const allComponents = getComponents(state);
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

function getDependencyModelContext(dependency: PlacedDependency, component: DependencyComponent, state: RootState): ModelContext[] {
  return (dependency.elements || [])
    .flatMap(element => getTriggers({ ...element, row: dependency.address?.row }, component, true))
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
function evaluateTrigger(trigger: TriggerElement, component: DependencyComponent, state: RootState): ConditionResult {
  // Get triggers
  const triggers = getTriggers(trigger, component);

  // Get triggers attributes (v2 is reassigned below, v1 is not)
  // eslint-disable-next-line prefer-const
  let [v1, v2] = triggers.map(item => getAttribute(item, state));
  v2 = getFirstDefinedAndNotNullValue(v2, trigger.value);

  // Evaluate trigger
  // An unknown condition is evaluated as "is not empty", like AngularJS does
  let condition = trigger.condition || ("event" in trigger ? "is not false" : "is not empty");
  if (!(condition in ConditionTest)) {
    condition = "is not empty";
  }
  const test = ConditionTest[condition](v1, v2, `'${v1}' ${condition} '${v2}'`);
  test.value = v1;

  // Check optional
  if (trigger.optional) {
    test.test = true;
    test.string += " (optional)";
  }
  return test;
}

/**
 * Check full dependency
 * @param dependency Dependency
 * @param component Component
 * @param state State
 * @returns {{launch: boolean, values: {}, string: Array}}
 */
function evaluateDependency(dependency: PlacedDependency, component: DependencyComponent, state: RootState): DependencyResult {
  // Initialization
  const check = dependency.type === "and" ? (prev: boolean, current: boolean) => prev && current : (prev: boolean, current: boolean) => prev || current;
  const result: DependencyResult = {
    launch: dependency.type === "and",
    values: {},
    string: []
  };

  // All the elements are evaluated, their values are needed by the dependency source
  let cancelled = false;
  (dependency.elements || []).forEach((trigger) => {
    const triggerResult = evaluateTrigger({ ...trigger, row: dependency.address?.row }, component, state);
    result.launch = check(result.launch, triggerResult.test);
    result.values[getTriggerId(trigger, dependency)] = triggerResult.value;
    result.string.push(triggerResult.string);

    // Don`t trigger if a high priority trigger is not achieved
    cancelled = cancelled || Boolean(trigger.cancel && !triggerResult.test);
  });
  result.launch = result.launch && !cancelled;

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
function retrieveSource(dependency: PlacedDependency, component: DependencyComponent, result: DependencyResult, force: boolean, state: RootState, dispatchActions: (Execution | null)[]): Value {
  const source = getFirstDefinedAndNotNullValue(dependency.source, "none");
  const target = getFirstDefinedAndNotNullValue(dependency.target, "none") as string;
  // Search for source
  switch (source) {
    // Update model with query output
    case "query":
      return retrieveQuerySource(result, target, dependency, component, state, dispatchActions);
    // Update value with criteria value
    case "criteria-value":
    case "criteria-text":
    case "launcher":
      return result.values[dependency.query as string];
    // Update value with plain text
    case "value":
      return dependency.value;
    // Update value with label text
    case "label":
      return dependency.label;
    // Update value with formule
    case "formule":
      return formule(dependency.formule as string, result.values);
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
function retrieveQuerySource(result: DependencyResult, target: string, dependency: PlacedDependency, component: DependencyComponent, state: RootState, dispatchActions: (Execution | null)[]): Value {
  const values: Record<string, Value> = { ...result.values };
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
        (values.type || values[state.settings.serverActionKey] || "data") as string,
        values[state.settings.targetActionKey] as string, address, async, silent, state.settings)]
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
function applyTarget(dependency: PlacedDependency, component: DependencyComponent, value: Value, result: DependencyResult): Execution | null {
  const target = getFirstDefinedAndNotNullValue(dependency.target, "none") as string;
  const { address } = dependency;
  const { launch } = result;
  // The row is left out of the address of a column
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
      // The chart applies the options it understands (the zoom) once they are stored
      return { updateAttributes: { address, data: { chartOptions: value } } };

    case "attribute-true":
      return { updateAttributes: { address, data: { [dependency.query as string]: value } } };

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
      return { updateValidation: { address, data: parseValidationRules(value as string, address) } };

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

    case "specific-true":
    case "specific-false": {
      // As AngularJS, the component which implements the dependency receives it; the others ignore it
      const handler = SpecificDependencyRegistry.get(component?.attributes?.component);
      return handler ? handler(dependency, value, launch) ?? null : null;
    }

    default:
      console.warn(`Dependency target "${target}" not defined`);
  }

  return null;
}

/**
 * Map the actions of a dependency replacing the wildcards of their target action, like AngularJS
 */
function mapDependencyActions(actions: NonNullable<Dependency["actions"]>, address: ComponentAddress, values: Record<string, Value>) {
  return actions.map(action => {
    if (action.targetAction === undefined || action.targetAction === null) {
      return { ...action, address };
    }
    const targetAction = replaceWildcard(action.targetAction, values);
    return { ...action, address, targetAction, parameters: { ...action.parameters, targetAction } };
  });
}

/**
 * Execute the dependency
 */
function executeDependency(dependency: PlacedDependency, component: DependencyComponent, result: DependencyResult, state: RootState): Execution[] {
  const { address } = dependency;
  const dispatchActions: (Execution | null)[] = [];

  // Launch dependency actions
  if ((dependency.actions?.length ?? 0) > 0 && result.launch) {
    dispatchActions.push({ addActions: mapDependencyActions(dependency.actions as NonNullable<Dependency["actions"]>, address, result.values) });
  }

  // Check force by target
  const target = dependency.target || "none";
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
  const value = retrieveSource(dependency, component, result, force, state, dispatchActions);

  // Set target dependency if value has been defined
  switch (value) {
    case VALUE_DEFERRED:
    case VALUE_NONE:
      break;
    default:
      dispatchActions.push(applyTarget(dependency, component, value, result));
  }

  return dispatchActions.filter((execution): execution is Execution => Boolean(execution));
}

function checkAndStoreResult(dependency: PlacedDependency, component: DependencyComponent, state: RootState): DependencyResult {
  const result = evaluateDependency(dependency, component, state);
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


function checkDependency(dependency: PlacedDependency, component: DependencyComponent, state: RootState): Execution[] {
  const result = checkAndStoreResult(dependency, component, state);

  // Fix result with invert
  result.launch = dependency.invert ? !result.launch : result.launch;

  if (state.settings.activeDependencies) {
    return executeDependency(dependency, component, result, state);
  }

  return [];
}

function initializeDependency(dependency: PlacedDependency, component: DependencyComponent, state: RootState) {
  checkAndStoreResult(dependency, component, state);
  return dependency.initial;
}

/**
 * Check if a row of a grid is a stored record: it is not one that the user has added or copied
 * @param {object} component Grid component
 * @param {string|number} rowId Row identifier
 * @returns {boolean} The row is a stored record
 */
function isStoredRow(component: DependencyComponent, rowId: string | number | undefined): boolean {
  const gridId = getGridIdentifier(component.attributes);
  const row = (component.model?.values || []).find(value => String(value[gridId]) === String(rowId));
  const isNewId = /^(new|copied)-row-/.test(String(rowId));
  return !isNewId && row?.$row?.operation !== OperationType.INSERT;
}

function hasChanged(dependency: PlacedDependency, component: DependencyComponent, state: RootState): boolean {
  // Elements with check-changes="false" are evaluated but their values do not launch the dependency
  const unchecked = (dependency.elements || [])
    .map((element) => (element.checkChanges === false && !element.event ? getTriggerId(element, dependency) : null));
  const newValues = _.omit(evaluateDependency(dependency, component, state).values, unchecked as string[]);
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
    .filter(([k]) => k in newValues && k !== 'modelHash')
    .reduce((p, [k, v]) => ({ ...p, [k]: v }), {});

  const triggerValuesChanged = !_.isEqual(oldValues, newValues);

  const modelHashChanged = storedData.modelHash !== currentModelHash;

  return triggerValuesChanged || modelHashChanged;
}

function getComponentDependencies(component: DependencyComponent): PlacedDependency[] {
  const gridDependencies = (component.attributes?.columnModel || [])
    .flat()
    .filter((column: GridColumn) => (column.dependencies || []).length > 0)
    .flatMap((column: GridColumn) => (column.dependencies || [])
      .flatMap((dependency: Dependency) => getColumnDependencies(dependency, column, component))
    );

  return [
    ...((component.dependencies || []).map(dependency => ({ ...dependency, address: component.address }))),
    ...gridDependencies].map((dependency, index) => ({ ...dependency, index }));
}

function getColumnDependencies(dependency: Dependency, column: GridColumn, component: DependencyComponent): PlacedDependency[] {
  const { values } = component.model;
  const gridId = getGridIdentifier(component.attributes);
  if (isColumnDependency(dependency)) {
    return values.map(row => ({
      ...dependency,
      address: { ...component.address, column: column.id, row: row[gridId] as string },
      target: dependency.target
    }));
  } else {
    return [{
      ...dependency,
      address: { ...component.address, column: column.id, row: getEditingRow(component.model.values)[gridId] as string },
      target: ["show", "hide"].includes(dependency.target as string) ? dependency.target + "-column" : dependency.target
    }];
  }
}

function isColumnDependency(dependency: Dependency): boolean {
  return dependency.elements!.flatMap(element => [element.attribute1 || "", element.attribute2 || ""])
    .reduce((prev, current) => ["currentRow", "prevCurrentRow", "nextCurrentRow"].includes(current) || prev, false);
}

/**
 * Check if a dependency listens to an event of a component: one of its elements has that event on that component
 * @param {object} dependency Dependency
 * @param {{address: object, event: string}} launcher Component that raises the event and the event
 * @returns {boolean} The dependency listens to the event
 */
function listensToEvent(dependency: PlacedDependency, { address, event }: EventLauncher): boolean {
  return (dependency.elements || []).some(element => element.event === event && element.id === address.component
    && (!element.view1 || element.view1 === address.view));
}

function getDependenciesExecutions(state: RootState, initial: boolean, view?: string, eventOnly?: EventLauncher) {
  const allComponents = getComponents(state);
  const executions = _.groupBy(Object.values(allComponents)
    .filter(component => (!initial || component.address?.view === view) && getComponentDependencies(component).length > 0)
    .map(component => getComponentDependencies(component)
      .filter(dependency => !eventOnly || listensToEvent(dependency, eventOnly))
      .filter(dependency => initial ? initializeDependency(dependency, component, state) : hasChanged(dependency, component, state))
      .map(dependency => checkDependency(dependency, component, state))
      .filter(e => e.length > 0)
    )
    .filter(e => e.length > 0)
    .flat(2), Object.keys);
  return Object.keys(executions).reduce((prev, key) => ({
    ...prev,
    [key]: executions[key].map(value => value[key])
  }), {} as Record<string, unknown[]>);
}

function dispatchExecutions(executions: Record<string, unknown[]>, state: RootState, dispatch: Dispatch<UnknownAction>): void {
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

/**
 * Forget the values stored for the dependencies that write the model of the given components (target "input").
 * A restore goes back to the default model of a component, which does not hold the value that one of these dependencies
 * had written (e.g. a suggest filled from another criterion). The stored values would make the next check believe that
 * nothing changed, so the dependency would not write the model again and the restored component would stay empty.
 * Forgetting them makes the next check launch these dependencies again.
 * @param {object[]} componentList Restored components
 */
export function forgetInputDependencies(componentList: Partial<DependencyComponent>[] = []): void {
  componentList.forEach(component => {
    // Components without a view are looked up as "undefined", as they always were
    const view = component?.address?.view as string;
    if (!DEPENDENCY_VALUES[view]) {
      return;
    }
    getComponentDependencies(component as DependencyComponent)
      .filter(dependency => dependency.target === "input")
      .forEach(dependency => {
        const componentId = getDependencyComponentId(component.address, { ...dependency.address, index: dependency.index });
        delete DEPENDENCY_VALUES[view][componentId];
      });
  });
}

/**
 * Check the dependencies
 * @param {object} state Redux state
 * @param {function} dispatch Dispatch function
 * @param {{address: object, event: string}} [eventOnly] Check only the dependencies that listen to this event, like
 * AngularJS does for the events that only notify (the column dependencies of the component are not checked)
 */
export function checkDependencies(state: RootState, dispatch: Dispatch<UnknownAction>, eventOnly?: EventLauncher): void {
  const executions = getDependenciesExecutions(state, false, undefined, eventOnly);
  dispatchExecutions(executions, state, dispatch);
}

export function initializeDependencies(view: string, state: RootState, dispatch: Dispatch<UnknownAction>): void {
  DEPENDENCY_VALUES[view] = {};
  const executions = getDependenciesExecutions(state, true, view);
  dispatchExecutions(executions, state, dispatch);
}
