import _ from "lodash";
import {
  asArray,
  evaluateExpression,
  getDataDependingOnList, translateLabel
} from "./index";
import {formatNumber, getFirstDefinedValueAsNumber} from "./numbers";
import {ComponentType, getFirstDefinedAndNotNullValue, isEmpty} from "./general";
import {extractCellModel, getGridData, getGridIdentifier} from "./grid";
import {getChartImage, getPrintOrientation} from "./chartRegistry";

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
export function isVoidElement(type) {
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
};

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
};

const {INITIAL, UPLOADING, UPLOADED} = UploadStatus;
const {
  COMPONENT_GRID,
  COMPONENT_TAB,
  COMPONENT_CHECKBOX,
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

export function getComponentIdentifierKey(component) {
  const attributes = component?.attributes || {};
  const isGrid = "columnModel" in attributes;
  return isGrid ? getGridIdentifier(attributes) : "value";
}

/**
 * Build diagnostic payload for malformed components
 * @param {object} component Component data
 * @param {object} metadata Diagnostic metadata
 * @returns {object} Diagnostic payload
 */
export function getComponentTraceContext(component = {}, metadata = {}) {
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
export function warnMalformedComponent(component = {}, metadata = {}) {
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
 * @returns {boolean} True when the component is valid and not grid scoped
 */
export function isTopLevelFormComponent(component = {}, metadata = {}) {
  const address = component?.address;
  if (getAddressType(address) === ADDRESS_INVALID) {
    return warnMalformedComponent(component, metadata);
  }

  return !("row" in address || "column" in address);
}

/**
 * Retrieve the validation nodes
 * @param {object|string} rule
 * @param {object} address
 * @returns {object} rule parsed
 * @memberOf Components
 */
export function parseRule(rule, address) {
  let newRule;
  if (_.isPlainObject(rule)) {
    newRule = rule;
  } else if (rule.indexOf("{") > -1) {
    try {
      newRule = evaluateExpression("(" + rule + ")");
    } catch (exc) {
      console.error("[ERROR] Parsing validation rule to JSON", {rule: rule, address: address, exception: exc});
    }
  } else {
    newRule = {[rule]: true};
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
export function parseValidationRules(validationRules, address) {
  let rulesParsed = {};
  if (validationRules) {
    if (_.isPlainObject(validationRules)) {
      rulesParsed = validationRules;
    } else {
      let rulesList = validationRules.indexOf("{") > -1 ? [validationRules] : validationRules.split(" ");
      rulesParsed = rulesList.reduce((parsed, rule) => ({...parsed, ...parseRule(rule, address)}), {});
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
export function checkModelIsEmpty(props) {
  return checkModelEmpty(props);
}

/**
 * Check if model has been updated
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function checkModelIsUpdated(props) {
  return checkModelUpdated(props);
}

/**
 * Check if model has not changed
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function checkModelIsUnchanged(props) {
  return checkModelUnchanged(props);
}

/**
 * Get all form values
 * @param {object} props  Properties
 * @param {boolean} forPrinting For printing
 * @return {object} Form values
 * @memberOf Components
 */
function getAllFormValues(props, forPrinting) {
  const {components = {}, settings = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: forPrinting ? "collectFormValuesForPrinting" : "collectFormValues",
      componentKey
    }))
    .reduce((result, [, component]) => {
      let values = getComponentData(component, props, forPrinting);
      checkDuplicates(getComponentId(component.address), result, values);
      return {
        ...result,
        ...values
      };
    }, {
      ...(settings.token ? {[settings.tokenKey]: settings.token} : {})
    });
}

/**
 * Check if model is empty
 * @param {object} props  Properties
 * @return {object} Form values
 * @memberOf Components
 */
function checkModelEmpty(props) {
  const {components = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: "checkModelEmpty",
      componentKey
    }))
    .filter(([, component]) => component.attributes.checkEmpty)
    .reduce((result, [, component]) => {
      let values = getComponentData(component, props, false);
      return result && isEmpty(values[component.attributes.id]);
    }, true);
}

/**
 * Check if model has been updated
 * @param {object} props  Properties
 * @return {object} Form values
 * @memberOf Components
 */
function checkModelUpdated(props) {
  const {components = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: "checkModelUpdated",
      componentKey
    }))
    .reduce((result, [, component]) => {
      let values = getComponentData(component, props, false);
      let storedValues = getComponentData(component, props, false, "storedModel");
      return result || values[component.attributes.id] !== storedValues[component.attributes.id];
    }, false);
}

/**
 * Check if model has not changed
 * @param {object} props  Properties
 * @return {object} Form values
 * @memberOf Components
 */
function checkModelUnchanged(props) {
  const {components = {}} = props;
  return Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "form-utilities",
      operation: "checkModelUnchanged",
      componentKey
    }))
    .reduce((result, [, component]) => {
      let values = getComponentData(component, props, false);
      let storedValues = getComponentData(component, props, false, "storedModel");
      return result && values[component.attributes.id] === storedValues[component.attributes.id];
    }, true);
}

/**
 * Check duplicates
 * @param {string} componentId Component id
 * @param {object} result Result
 * @param {object} values New values
 * @memberOf Components
 */
function checkDuplicates(componentId, result, values) {
  if (componentId in result) {
    console.warn(`[WARNING] Overwriting '${componentId}' duplicated parameter`, {
      'old': result[componentId],
      'new': values
    });
  }
}

/**
 * Retrieve component values
 * @param {object} model Model
 * @memberOf Components
 */
export function getSelectedValues(model = {}) {
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
export function getCheckboxData(criterion, model, props, forPrinting) {
  const {attributes = {}} = criterion;
  return {
    [attributes.id]: getDataDependingOnList(getSelectedValues(model)) || 0,
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
export function getCriterionData(criterion, model, props, forPrinting) {
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
export function getCriterionDataAsList(criterion, model, props, forPrinting) {
  const {attributes = {}} = criterion;
  const value = getSelectedValues(model);
  return {
    // An empty criterion is sent with a null value (as the AngularJS client does), services read it as empty
    ...(attributes.id !== undefined ? {[attributes.id]: value ?? null} : {}),
    ...forPrinting ? getCriterionPrintData(criterion, model, props) : {},
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
export function getCriterionPrintData(criterion = {}, model = {}, props = {}) {
  const {t} = props;
  const {attributes = {}} = criterion;
  const {values = []} = model;
  const {id, component, numberFormat} = attributes;
  let translateFunction;
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
function getTextData(values, t) {
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
export function getTabData(tab, model, props, forPrinting) {
  const {attributes = {}} = tab;
  return {
    [attributes.id]: getDataDependingOnList(getSelectedValues(model)),
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
export function getTabPrintData(tab = {}, model = {}, props = {}) {
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
export function getComponentData(component, props, forPrinting, model = "model") {
  const {attributes = {}} = component;
  const chartImage = forPrinting ? getChartImage(component.address?.component, getPrintOrientation(props?.components)) : undefined;
  if (chartImage !== undefined) {
    return {[component.address.component]: {image: chartImage}};
  }
  switch (attributes.component || "") {
    case COMPONENT_GRID:
      return getGridData(component, component[model], props, forPrinting);
    case COMPONENT_PICKLIST:
    case COMPONENT_SELECT_MULTIPLE:
    case COMPONENT_SUGGEST_MULTIPLE:
      return getCriterionDataAsList(component, component[model], props, forPrinting);
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
export function fixModel(model, isGrid) {
  // Multiple values loaded with the ARRAY transform arrive as a list inside the selected list
  let selected = asArray(model.selected).flat(Infinity).map(value => fixSelectedModel(value));
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
export function fixDefaultModel(model, defaultValues, isGrid) {
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

export function fixSelectedModel(selected) {
  if (typeof selected === "object") {
    return selected;
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
export function fixController(controller, isGrid, settings) {
  // Init size and charSize
  let max = getFirstDefinedValueAsNumber(controller.max, settings.recordsPerPage);
  let columnModel = isGrid ? {
    columnModel: controller.columnModel.map(column => ({
      ...fixController(column, false, settings),
      validationRules: parseValidationRules(column.validation, {...controller.address, column: column.name})
    }))
  } : {};
  return {
    ...controller,
    ...columnModel,
    numberFormat: {...settings.numericOptions, ...(controller.numberFormat ? evaluateExpression(controller.numberFormat) : {})},
    size: controller.size || settings.defaultComponentSize,
    max: max
  };
}

/**
 * Get specific attributes
 * @param {object} controller Controller data
 * @param {boolean} isGrid Controller is from a grid
 * @param {object} settings Application settings
 * @return {object} Fixed controller values
 */
export function getSpecificAttributes(controller, isGrid, settings) {
  let max = getFirstDefinedValueAsNumber(controller.max, settings.recordsPerPage);
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
export function inspectComponentStructure(element, context, inspected) {
  let nextContext = [...context];
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

export function classNames(...args) {
  return (args || [])
    .filter(className => !isEmpty(className))
    .flatMap(className => {
      switch (typeof className) {
        case "string":
        case "number":
          return [className];
        case "object":
          if (Array.isArray(className)) {
            return className;
          } else {
            return Object.entries(className)
              .filter(([key, value]) => !!value)
              .map(([key, value]) => key);
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
export function getComponentId(address) {
  switch (getAddressType(address)) {
    case ADDRESS_CELL:
      return `${address.component}-${address.row}-${address.column}`;
    case ADDRESS_COLUMN:
      return `${address.component}-${address.column}`;
    case ADDRESS_COMPONENT:
      return address.component;
    default:
      return null;
  }
}

/**
 * Get component id
 */
export function getDependencyComponentId(address, dependency = {}) {
  const column = !isEmpty(dependency.column) ? `-${dependency.column}` : "";
  const row = !isEmpty(dependency.row) ? `-${dependency.row}` : "";
  const index = !isEmpty(dependency.index) ? `-${dependency.index}` : "";
  return `${getComponentId(address)}${column}${row}${index}`;
}

/**
 * Get component id
 */
export function getTriggerId(trigger, dependency, index) {
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
export function getAddressType(address) {
  if (_.isPlainObject(address)) {
    const {view, component, column, row} = address;
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
export function clickDropdown(e, props, dropdown) {
  const {addActionsTop, updateModelWithDependencies, actions, address, dispatch} = props;
  dropdown.toggle(e);

  // Change click event
  dispatch(updateModelWithDependencies(address, {event: "click"}));

  // Send actions to action container
  dispatch(addActionsTop(actions.map(action => ({...action, address}))));
}
