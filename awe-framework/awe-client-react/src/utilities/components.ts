import _ from "lodash";
import type {SyntheticEvent} from "react";
import type {ComponentAddress, DependencyElement} from "../contract";
import {
  asArray,
  evaluateExpression,
  getDataDependingOnList, translateLabel
} from "./index";
import {formatNumber, getFirstDefinedValueAsNumber} from "./numbers";
import type {NumberFormat} from "./numbers";
import {
  ComponentType, DATE_RANGE_FROM_SUFFIX, DATE_RANGE_TO_SUFFIX, getFirstDefinedAndNotNullValue, isEmpty
} from "./general";
import {getGridData, getGridIdentifier} from "./grid";
import type {GridComponent, GridModel} from "./grid";
import {getChartImage, getPrintOrientation} from "./chartRegistry";
import type {OrientationComponents} from "./chartRegistry";
import {isRowDetailComponent} from "./gridRowDetail";

/** Translator of a text */
type Translate = (key: string) => string;

/** An option of the model of a component */
interface ModelOption {
  value?: unknown;
  label?: unknown;
  selected?: unknown;
  id?: unknown;
  [key: string]: unknown;
}

/** Model of a component: the options it can take and the ones that are selected */
export interface ComponentModelData {
  selected?: unknown;
  values: ModelOption[];
  [key: string]: unknown;
}

/** Attributes of a component, as the criteria and the groups read them */
interface CriterionAttributes {
  id?: string;
  component?: string;
  numberFormat?: NumberFormat;
  [key: string]: unknown;
}

/** A component that has attributes and a model */
interface CriterionComponent {
  attributes?: CriterionAttributes;
}

/** A model that only has options */
interface OptionsModel {
  values?: ModelOption[];
}

/** A component as the form utilities read it */
export interface FormComponent {
  address?: ComponentAddress;
  attributes?: CriterionAttributes & {checkEmpty?: boolean};
  model?: OptionsModel;
  storedModel?: OptionsModel;
  context?: {view?: string};
  uid?: unknown;
  rowDetailInstance?: unknown;
}

/** Properties the form utilities need: the components of the screen, the settings and the translator */
export interface FormProps {
  components?: Record<string, FormComponent>;
  settings?: {token?: string; tokenKey?: string};
  t?: Translate;
}

/** Diagnostic metadata of a malformed component */
interface TraceMetadata {
  componentKey?: unknown;
  extra?: Record<string, unknown>;
  origin?: string;
  operation?: string;
  reason?: string;
  [key: string]: unknown;
}

/** Controller of a component, as the server sends it */
export interface ControllerData {
  max?: unknown;
  loadAll?: boolean;
  address?: ComponentAddress;
  columnModel?: ControllerData[];
  name?: string;
  validation?: string;
  numberFormat?: string;
  size?: string;
  [key: string]: unknown;
}

/** Controller of a component once it is fixed: its number format is evaluated and it has a size and a maximum */
export interface FixedControllerData {
  columnModel?: FixedControllerData[];
  numberFormat: NumberFormat;
  size?: string;
  max?: number;
  validationRules?: Record<string, unknown>;
  [key: string]: unknown;
}

/** Settings the controllers are fixed with */
interface ControllerSettings {
  recordsPerPage?: number;
  numericOptions?: NumberFormat;
  defaultComponentSize?: string;
}

/** An element of the structure of a screen */
export interface StructureNode {
  id?: string;
  elementType?: string;
  treegrid?: boolean;
  elementList?: StructureNode[];
}

/** Something that gives a class: a text, a number, a list of classes or an object whose true keys are the classes */
type ClassValue = string | number | readonly string[] | Record<string, unknown> | null | undefined;

/**
 * HTML void elements: React refuses to render them with children (not even an empty label)
 * @type {Set<string>}
 */
const VOID_ELEMENTS = new Set(["area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "source", "track", "wbr"]);

/**
 * Check whether an HTML tag is a void element, which cannot have children
 * @param {string} type Tag name
 * @returns {boolean} True when the tag is a void element
 */
export function isVoidElement(type: unknown): boolean {
  return typeof type === "string" && VOID_ELEMENTS.has(type.toLowerCase());
}

/**
 * Upload status
 * @type {{UPLOADING: string, INITIAL: string, UPLOADED: string}}
 */
export const UploadStatus = {
  INITIAL: 'initial',
  UPLOADING: 'uploading',
  UPLOADED: 'uploaded'
} as const;

/**
 * Address type
 * @type {{ADDRESS_VIEW: string, ADDRESS_CELL: string, ADDRESS_COMPONENT: string, ADDRESS_COLUMN: string, ADDRESS_INVALID: string}}
 */

export const ComponentAddressType = {
  ADDRESS_CELL: 'cell',
  ADDRESS_COLUMN: 'column',
  ADDRESS_COMPONENT: 'component',
  ADDRESS_VIEW: 'view',
  ADDRESS_INVALID: 'invalid'
} as const;

const {
  COMPONENT_GRID,
  COMPONENT_TAB,
  COMPONENT_CHECKBOX,
  COMPONENT_DATE_RANGE,
  COMPONENT_NUMERIC,
  COMPONENT_TIME,
  COMPONENT_PICKLIST,
  COMPONENT_SELECT_MULTIPLE,
  COMPONENT_SUGGEST_MULTIPLE,
  COMPONENT_OTHER
} = ComponentType;
const {ADDRESS_CELL, ADDRESS_COLUMN, ADDRESS_COMPONENT, ADDRESS_VIEW, ADDRESS_INVALID} = ComponentAddressType;

/**
 * Component utility functions
 * @category Utilities
 * @namespace Components
 */

/**
 * Check if a component is a grid (it has a column model)
 * @param {object} component Component
 * @returns {boolean} The component is a grid
 */
export function isGridComponent(component?: {attributes?: object} | null): boolean {
  return "columnModel" in (component?.attributes || {});
}

export function getComponentIdentifierKey(component: {attributes?: {treegrid?: boolean; treeId?: string; [key: string]: unknown}}): string {
  return isGridComponent(component) ? getGridIdentifier(component.attributes) : "value";
}

/**
 * Build diagnostic payload for malformed components
 * @param {object} component Component data
 * @param {object} metadata Diagnostic metadata
 * @returns {object} Diagnostic payload
 */
export function getComponentTraceContext(component: FormComponent = {}, metadata: TraceMetadata = {}): Record<string, unknown> {
  const attributes = component?.attributes || {};
  const address = component?.address;
  const context = component?.context || {};
  const {componentKey = null, extra = {}, ...restMetadata} = metadata;

  return {
    ...restMetadata,
    componentKey,
    componentUid: component?.uid ?? null,
    componentId: address?.component ?? null,
    attributesId: attributes?.id ?? null,
    view: address?.view ?? context?.view ?? null,
    address,
    context,
    ...extra
  };
}

/**
 * Log malformed component diagnostics without breaking execution
 * @param {object} component Component data
 * @param {object} metadata Diagnostic metadata
 * @returns {boolean} Always false to simplify filter guards
 */
export function warnMalformedComponent(component: FormComponent = {}, metadata: TraceMetadata = {}): false {
  const {origin = "unknown", operation = "unknown", reason = "Missing or invalid component.address"} = metadata;
  console.warn(
    `[AWE] Malformed component detected in ${origin}:${operation} - ${reason}`,
    getComponentTraceContext(component, metadata)
  );
  return false;
}

/**
 * Check whether component can be processed as a top-level form component
 * @param {object} component Component data
 * @param {object} metadata Diagnostic metadata
 * @returns {boolean} True when the component is valid, not grid scoped and not part of a grid row detail instance
 */
export function isTopLevelFormComponent(component: FormComponent = {}, metadata: TraceMetadata = {}): boolean {
  const address = component?.address;
  if (getAddressType(address) === ADDRESS_INVALID) {
    return warnMalformedComponent(component, metadata);
  }

  return !("row" in (address as ComponentAddress) || "column" in (address as ComponentAddress)) && !isRowDetailComponent(component);
}

/**
 * Retrieve the validation nodes
 * @param {object|string} rule
 * @param {object} address
 * @returns {object} rule parsed
 * @memberOf Components
 */
export function parseRule(rule: string | Record<string, unknown>, address?: ComponentAddress): Record<string, unknown> {
  let newRule: Record<string, unknown> | undefined;
  if (_.isPlainObject(rule)) {
    newRule = rule as Record<string, unknown>;
  } else if ((rule as string).indexOf("{") > -1) {
    try {
      newRule = evaluateExpression("(" + rule + ")") as Record<string, unknown>;
    } catch (exc) {
      console.error("[ERROR] Parsing validation rule to JSON", {rule: rule, address: address, exception: exc});
    }
  } else {
    newRule = {[rule as string]: true};
  }
  return {...newRule};
}


/**
 * Retrieve the validation nodes
 * @param {*} validationRules Rules
 * @param {object} address Component address
 * @returns {object} Rules as object
 * @memberOf Components
 */
export function parseValidationRules(
  validationRules: string | Record<string, unknown> | null | undefined,
  address?: ComponentAddress
): Record<string, unknown> {
  let rulesParsed: Record<string, unknown> = {};
  if (validationRules) {
    if (_.isPlainObject(validationRules)) {
      rulesParsed = validationRules as Record<string, unknown>;
    } else {
      const rules = validationRules as string;
      const rulesList = rules.indexOf("{") > -1 ? [rules] : rules.split(" ");
      rulesParsed = rulesList.reduce<Record<string, unknown>>((parsed, rule) => ({...parsed, ...parseRule(rule, address)}), {});
    }
  }
  return rulesParsed;
}

/**
 * Check if model is empty
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function checkModelIsEmpty(props: FormProps): boolean {
  return checkModelEmpty(props);
}

/**
 * Check if model has been updated
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function checkModelIsUpdated(props: FormProps): boolean {
  return checkModelUpdated(props);
}

/**
 * Check if model has not changed
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function checkModelIsUnchanged(props: FormProps): boolean {
  return checkModelUnchanged(props);
}

/**
 * Get the value of a component to compare (a date range sends two parameters, so it is compared as a whole)
 * @param {object} component Component
 * @param {object} values Component data
 * @return {*} Value to compare
 */
function getComparableValue(component: FormComponent, values: Record<string, unknown>): unknown {
  const {id, component: type} = component.attributes as CriterionAttributes;
  if (type !== COMPONENT_DATE_RANGE) {
    return values[id as string];
  }
  const [from, to] = [values[`${id}${DATE_RANGE_FROM_SUFFIX}`], values[`${id}${DATE_RANGE_TO_SUFFIX}`]].map(value => value || "");
  return from || to ? `${from}|${to}` : "";
}

/**
 * Check if model is empty
 * @param {object} props  Properties
 * @return {object} Form values
 * @memberOf Components
 */
function checkModelEmpty(props: FormProps): boolean {
  const {components = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: "checkModelEmpty",
      componentKey
    }))
    .filter(([, component]) => component.attributes?.checkEmpty)
    .reduce((result, [, component]) => {
      const values = getComponentData(component, props, false);
      return result && isEmpty(getComparableValue(component, values));
    }, true);
}

/**
 * Check if model has been updated
 * @param {object} props  Properties
 * @return {object} Form values
 * @memberOf Components
 */
function checkModelUpdated(props: FormProps): boolean {
  const {components = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: "checkModelUpdated",
      componentKey
    }))
    .reduce((result, [, component]) => {
      const values = getComponentData(component, props, false);
      const storedValues = getComponentData(component, props, false, "storedModel");
      return result || getComparableValue(component, values) !== getComparableValue(component, storedValues);
    }, false);
}

/**
 * Check if model has not changed
 * @param {object} props  Properties
 * @return {object} Form values
 * @memberOf Components
 */
function checkModelUnchanged(props: FormProps): boolean {
  const {components = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: "checkModelUnchanged",
      componentKey
    }))
    .reduce((result, [, component]) => {
      const values = getComponentData(component, props, false);
      const storedValues = getComponentData(component, props, false, "storedModel");
      return result && getComparableValue(component, values) === getComparableValue(component, storedValues);
    }, true);
}

/**
 * Retrieve component values
 * @param {object} model Model
 * @memberOf Components
 */
export function getSelectedValues(model: OptionsModel = {}): unknown[] {
  const {values = []} = model;
  return values
    .filter((value) => value.selected)
    .map((value) => value.value);
}

/**
 * Retrieve the checkbox data
 * @param {Object} criterion Criterion data
 * @param {Object} model Checkbox model
 * @param {Object} props Properties
 * @param {boolean} forPrinting Data is for printing
 * @returns {object} model data
 * @memberOf Components
 */
export function getCheckboxData(criterion: CriterionComponent, model: OptionsModel | undefined, props: FormProps, forPrinting?: boolean): Record<string, unknown> {
  const {attributes = {}} = criterion;
  return {
    [attributes.id as string]: getDataDependingOnList(getSelectedValues(model)) || 0,
    ...forPrinting ? getCriterionPrintData(criterion, model, props) : {},
  };
}

/**
 * Retrieve the criterion data
 * @param {Object} criterion Criterion data
 * @param {Object} model Criterion model
 * @param {Object} props Properties
 * @param {boolean} forPrinting Data is for printing
 * @returns {object} model data
 * @memberOf Components
 */
export function getCriterionData(criterion: CriterionComponent, model: OptionsModel | undefined, props: FormProps, forPrinting?: boolean): Record<string, unknown> {
  const {attributes = {}} = criterion;
  const value = getDataDependingOnList(getSelectedValues(model));
  return {
    // An empty criterion is sent with a null value (as the AngularJS client does), services read it as empty
    ...(attributes.id !== undefined ? {[attributes.id]: value ?? null} : {}),
    ...forPrinting ? getCriterionPrintData(criterion, model, props) : {},
  };
}

/**
 * Retrieve the criterion data as list
 * @param {Object} criterion Criterion data
 * @param {Object} model Criterion model
 * @param {Object} props Properties
 * @param {boolean} forPrinting Data is for printing
 * @returns {object} model data
 * @memberOf Components
 */
export function getCriterionDataAsList(criterion: CriterionComponent, model: OptionsModel | undefined, props: FormProps, forPrinting?: boolean): Record<string, unknown> {
  const {attributes = {}} = criterion;
  const value = getSelectedValues(model);
  return {
    // An empty criterion is sent with a null value (as the AngularJS client does), services read it as empty
    ...(attributes.id !== undefined ? {[attributes.id]: value ?? null} : {}),
    ...forPrinting ? getCriterionPrintData(criterion, model, props) : {},
  };
}

/**
 * Retrieve the date range criterion data: the start and the end of the range are sent as two parameters
 * ("id.from" and "id.to"), null when empty
 * @param {Object} criterion Criterion data
 * @param {Object} model Criterion model
 * @param {Object} props Properties
 * @param {boolean} forPrinting Data is for printing
 * @returns {object} model data
 * @memberOf Components
 */
export function getDateRangeData(criterion: CriterionComponent, model: OptionsModel | undefined, props: FormProps, forPrinting?: boolean): Record<string, unknown> {
  const {attributes = {}} = criterion;
  const [from, to] = getSelectedValues(model);
  return {
    [`${attributes.id}${DATE_RANGE_FROM_SUFFIX}`]: from || null,
    [`${attributes.id}${DATE_RANGE_TO_SUFFIX}`]: to || null,
    ...forPrinting ? {[`${attributes.id}.data`]: {text: [from, to].filter(value => !isEmpty(value)).join(" - ")}} : {},
  };
}

/**
 * Retrieve the criterion print data
 * @param {object} criterion Criterion data
 * @param {object} model Criterion model
 * @param {object} props Properties
 * @returns {object} model data
 * @memberOf Components
 */
export function getCriterionPrintData(criterion: CriterionComponent = {}, model: OptionsModel = {}, props: FormProps = {}): Record<string, unknown> {
  const {t} = props;
  const {attributes = {}} = criterion;
  const {values = []} = model;
  const {id, component, numberFormat} = attributes;
  let translateFunction: Translate | undefined;
  switch (component) {
    case COMPONENT_NUMERIC:
      translateFunction = (v) => formatNumber(v, numberFormat);
      break;
    case COMPONENT_TIME:
      translateFunction = (v) => v;
      break;
    default:
      translateFunction = t;
      break;
  }

  return {
    [`${id}.data`]: {
      text: getTextData(values, translateFunction)
    }
  };
}

/**
 * Get text data from values
 * @param {object[]} values Value list
 * @param {function} t Translator
 * @return {*}
 */
function getTextData(values: ModelOption[], t: Translate | undefined): string {
  return values
    .filter(value => value.selected && !isEmpty(value.value))
    .map(value => translateLabel(getFirstDefinedAndNotNullValue(value.label, value.value, ""), t))
    .join(", ");
}

/**
 * Retrieve the tab data
 * @param {object} tab Tab data
 * @param {object} model Tab model
 * @param {object} props Properties
 * @param {boolean} forPrinting Data is for printing
 * @returns {object} model data
 * @memberOf Components
 */
export function getTabData(tab: CriterionComponent, model: OptionsModel | undefined, props: FormProps, forPrinting?: boolean): Record<string, unknown> {
  const {attributes = {}} = tab;
  return {
    [attributes.id as string]: getDataDependingOnList(getSelectedValues(model)),
    ...forPrinting ? getTabPrintData(tab, model, props) : {},
  };
}

/**
 * Retrieve the tab print data
 * @param {object} tab Tab data
 * @param {object} model Tab model
 * @param {object} props Properties
 * @returns {object} model data
 * @memberOf Components
 */
export function getTabPrintData(tab: CriterionComponent = {}, model: OptionsModel = {}, props: FormProps = {}): Record<string, unknown> {
  const {t} = props;
  const {attributes = {}} = tab;
  const {values = []} = model;
  const {id} = attributes;
  return {
    [`${id}.data`]: {
      text: getTextData(values, t),
      all: [...values]
    }
  };
}

/**
 * Retrieve the updated model
 * @param {Object} component Component to check
 * @param {Object} props Properties
 * @param {Boolean} forPrinting Data is for printing
 * @param {String} model Model to use (model or storedModel)
 * @returns {object} model updated
 * @memberOf Components
 */
export function getComponentData(
  component: FormComponent,
  props: FormProps,
  forPrinting?: boolean,
  model: "model" | "storedModel" = "model"
): Record<string, unknown> {
  const {attributes = {}} = component;
  const chartImage = forPrinting ? getChartImage(component.address?.component as string, getPrintOrientation(props?.components as OrientationComponents)) : undefined;
  if (chartImage !== undefined) {
    return {[(component.address as ComponentAddress).component as string]: {image: chartImage}};
  }
  switch (attributes.component || "") {
    case COMPONENT_GRID:
      // A component of the grid type has the attributes and the model of a grid
      return getGridData(component as unknown as GridComponent, component[model] as GridModel, props, forPrinting);
    case COMPONENT_PICKLIST:
    case COMPONENT_SELECT_MULTIPLE:
    case COMPONENT_SUGGEST_MULTIPLE:
      return getCriterionDataAsList(component, component[model], props, forPrinting);
    case COMPONENT_DATE_RANGE:
      return getDateRangeData(component, component[model], props, forPrinting);
    case COMPONENT_TAB:
      return getTabData(component, component[model], props, forPrinting);
    case COMPONENT_CHECKBOX:
      return getCheckboxData(component, component[model], props, forPrinting);
    case COMPONENT_OTHER:
      return {};
    default:
      return getCriterionData(component, component[model], props, forPrinting);
  }
}

/**
 * Fixes component model
 * @param {object} model component model
 * @param {boolean} isGrid component is a grid
 * @return {object} Model fixed
 */
export function fixModel(model: ComponentModelData, isGrid: boolean): ComponentModelData & {selected: null} {
  // Multiple values loaded with the ARRAY transform arrive as a list inside the selected list
  const selected = asArray(model.selected).flat(Infinity).map(value => fixSelectedModel(value));
  let values = model.values;
  const selectedValues = [...selected.map(value => value?.value), ...values
    .filter(value => value.selected)
    .map(value => isGrid ? value?.id : value?.value)
    .filter(value => value !== undefined && value !== null)
    .map(value => String(value))
  ];
  if (isGrid) {
    values = values.map(value => ({...value, selected: selectedValues.includes(String(value.id))}));
  } else {
    values = values.map(value => ({...value, selected: selectedValues.includes(String(value.value))}));
    if (selected.length > 0 && values.length === 0) {
      values = selected.map(value => ({...value, selected: true}));
    }
  }
  return {
    ...model,
    selected: null,
    values: values
  };
}

/**
 * Fixes the model a "restore" action goes back to: the options of the component with the XML default values selected.
 * The server sends the loaded selection and the default one apart ("selected" and "defaultValues"). A component
 * without default values (a radio only has the loaded selection of its "checked" attribute) goes back to its loaded one.
 * @param {object} model Component model, already fixed
 * @param {Array} defaultValues Default values the server sent, undefined when it did not send them
 * @param {boolean} isGrid Component is a grid (a grid has no default selection)
 * @return {object} Model with the default values selected
 */
export function fixDefaultModel(model: ComponentModelData, defaultValues: unknown, isGrid: boolean): ComponentModelData {
  if (isGrid || !Array.isArray(defaultValues) || defaultValues.length === 0) {
    return model;
  }
  const defaults = asArray(defaultValues).flat(Infinity).map(value => fixSelectedModel(value));
  const keys = new Set(defaults.map(value => String(value?.value)));
  const values = (model.values || []).map(value => ({...value, selected: keys.has(String(value.value))}));
  const present = new Set(values.map(value => String(value.value)));
  return {
    ...model,
    values: [...values, ...defaults.filter(value => !present.has(String(value?.value))).map(value => ({...value, selected: true}))]
  };
}

export function fixSelectedModel(selected: unknown): ModelOption | null {
  if (typeof selected === "object") {
    return selected as ModelOption | null;
  } else if (typeof selected === "string") {
    return {value: selected};
  } else {
    return {value: String(selected)};
  }
}

/**
 * Fix controller values
 * @param {object} controller Controller data
 * @param {boolean} isGrid Controller is from a grid
 * @param {object} settings Application settings
 * @return {object} Fixed controller values
 */
export function fixController(controller: ControllerData, isGrid: boolean, settings: ControllerSettings): FixedControllerData {
  // Init size and charSize
  const max = getFirstDefinedValueAsNumber(controller.max, settings.recordsPerPage);
  const columnModel = isGrid ? {
    columnModel: (controller.columnModel as ControllerData[]).map(column => ({
      ...fixController(column, false, settings),
      validationRules: parseValidationRules(column.validation, {...controller.address, column: column.name})
    }))
  } : {};
  // Only a grid has a column model, and it is fixed above
  return {
    ...controller,
    ...columnModel,
    numberFormat: {...settings.numericOptions, ...(controller.numberFormat ? evaluateExpression(controller.numberFormat) as NumberFormat : {})},
    size: controller.size || settings.defaultComponentSize,
    max: max
  } as FixedControllerData;
}

/**
 * Get specific attributes
 * @param {object} controller Controller data
 * @param {boolean} isGrid Controller is from a grid
 * @param {object} settings Application settings
 * @return {object} Fixed controller values
 */
export function getSpecificAttributes(controller: ControllerData, isGrid: boolean, settings: ControllerSettings): {
  max: number | undefined;
  rows: number | undefined;
  sort: unknown[];
  page: number;
  first: number;
} {
  const max = getFirstDefinedValueAsNumber(controller.max, settings.recordsPerPage);
  return {
    max: controller.loadAll ? 0 : max,
    rows: max,
    sort: [],
    page: 1,
    first: 0
  };
}

/**
 * Inspect component structure
 * @param element
 * @param context
 * @param inspected
 * @return {*}
 */
export function inspectComponentStructure(element: StructureNode, context: string[], inspected: Record<string, string[]>): Record<string, string[]> {
  let nextContext: string[] = [...context];
  const {elementList, id, elementType} = element;

  // If dialog, reset context
  if (elementType === "Dialog") {
    nextContext = [];
  } else if (elementType === "Grid") {
    // If treegrid, change element type
    element.elementType = element.treegrid ? "TreeGrid" : elementType;
  }

  // Dependency elements only reference components, they are not part of the structure
  if (id && elementType !== "DependencyElement" && !(id in inspected)) {
    nextContext = [...nextContext, id];
    inspected[id] = nextContext;
  }

  (elementList || []).forEach(child => inspectComponentStructure(child, nextContext, inspected));
  return inspected;
}

export function classNames(...args: ClassValue[]): string {
  return (args || [])
    .filter((className): className is NonNullable<ClassValue> => !isEmpty(className))
    .flatMap<string | number>(className => {
      switch (typeof className) {
        case "string":
        case "number":
          return [className];
        case "object":
          if (Array.isArray(className)) {
            return className;
          } else {
            return Object.entries(className)
              .filter(([, value]) => !!value)
              .map(([key]) => key);
          }
        default:
          return [];
      }
    })
    .join(" ");
}

/**
 * Get component id
 */
export function getComponentId(address: ComponentAddress | null | undefined): string | null {
  switch (getAddressType(address)) {
    case ADDRESS_CELL:
      return `${address?.component}-${address?.row}-${address?.column}`;
    case ADDRESS_COLUMN:
      return `${address?.component}-${address?.column}`;
    case ADDRESS_COMPONENT:
      return address?.component as string;
    default:
      return null;
  }
}

/**
 * Get component id
 */
export function getDependencyComponentId(
  address: ComponentAddress | null | undefined,
  dependency: {column?: unknown; row?: unknown; index?: unknown} = {}
): string {
  const column = !isEmpty(dependency.column) ? `-${dependency.column}` : "";
  const row = !isEmpty(dependency.row) ? `-${dependency.row}` : "";
  const index = !isEmpty(dependency.index) ? `-${dependency.index}` : "";
  return `${getComponentId(address)}${column}${row}${index}`;
}

/**
 * Get component id
 */
export function getTriggerId(trigger: DependencyElement, dependency: {address: ComponentAddress}): string {
  const address = {
    view: dependency.address.view,
    component: trigger.id,
    ...(trigger.column1 ? {column: trigger.column1} : {}),
    ...(dependency.address?.row ? {row: dependency.address?.row} : {})
  };
  const attribute = !isEmpty(trigger.attribute1) ? `-${trigger.attribute1}` : "";
  const event = !isEmpty(trigger.event) ? `-${trigger.event}` : "";
  return trigger.alias || `${getComponentId(address)}${attribute}${event}`;
}

/**
 * Check address type
 */
export function getAddressType(address: unknown): typeof ADDRESS_CELL | typeof ADDRESS_COLUMN | typeof ADDRESS_COMPONENT | typeof ADDRESS_VIEW | typeof ADDRESS_INVALID {
  if (_.isPlainObject(address)) {
    const {view, component, column, row} = address as ComponentAddress;
    if (row && column && component && view) {
      return ADDRESS_CELL;
    } else if (column && component && view) {
      return ADDRESS_COLUMN;
    } else if (component && view) {
      return ADDRESS_COMPONENT;
    } else if (view) {
      return ADDRESS_VIEW;
    }
  }
  return ADDRESS_INVALID;
}

/**
 * Click on dropdown
 * @param e Event
 * @param props Properties
 * @param dropdown Dropdown
 */
export function clickDropdown<A, B>(
  e: SyntheticEvent,
  props: {
    addActionsTop: (actions: Array<object>) => A;
    updateModelWithDependencies: (address: ComponentAddress, data: {event: string}) => B;
    actions: object[];
    address: ComponentAddress;
    dispatch: (action: A | B) => unknown;
  },
  dropdown: {toggle: (e: SyntheticEvent) => void}
): void {
  const {addActionsTop, updateModelWithDependencies, actions, address, dispatch} = props;
  dropdown.toggle(e);

  // Change click event
  dispatch(updateModelWithDependencies(address, {event: "click"}));

  // Send actions to action container
  dispatch(addActionsTop(actions.map(action => ({...action, address}))));
}
