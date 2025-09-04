import _ from "lodash";
import {
  asArray,
  evaluateExpression,
  fetchAction,
  generateServerAction,
  getCookie,
  translateLabel
} from "./index";
import {getUID} from "../redux/actions/settings";
import {formatNumber, getFirstDefinedValueAsNumber} from "./numbers";
import {getGridData} from "./grid";
import {getFirstDefinedAndNotNullValue, isEmpty} from "./general";
import {addActionsTop} from "../redux/actions/actions";

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
 * Component type
 */

export const ComponentType = {
  COMPONENT_GRID: 'grid',
  COMPONENT_OTHER: 'other',
  COMPONENT_TEXT: 'text',
  COMPONENT_TEXTAREA: 'textarea',
  COMPONENT_NUMERIC: 'numeric',
  COMPONENT_SELECT: 'select',
  COMPONENT_SELECT_MULTIPLE: 'select-multiple',
  COMPONENT_SUGGEST: 'suggest',
  COMPONENT_SUGGEST_MULTIPLE: 'suggest-multiple',
  COMPONENT_DATE: 'date',
  COMPONENT_FILTERED_CALENDAR: 'filtered-calendar',
  COMPONENT_TIME: 'time',
  COMPONENT_HIDDEN: 'hidden',
  COMPONENT_PASSWORD: 'password',
  COMPONENT_FILE: 'file',
  COMPONENT_CHECKBOX: 'checkbox',
  COMPONENT_RADIO: 'radio',
  COMPONENT_BUTTON_CHECKBOX: 'button-checkbox',
  COMPONENT_BUTTON_RADIO: 'button-radio',
  COMPONENT_TAB: 'tab',
  COMPONENT_UPLOADER: 'uploader',
  COMPONENT_COLOR: 'color',
  COMPONENT_TEXT_VIEW: 'text-view',
  COMPONENT_WYSIWYG: 'wysiwyg',
  COMPONENT_MARKDOWN_EDITOR: 'markdown-editor',
  COMPONENT_ICON: 'icon',
  COMPONENT_IMAGE: 'image',
  COMPONENT_VIDEO: 'video',
  COMPONENT_PROGRESS: 'progress',
  COMPONENT_SPARKLINE: 'sparkline',
  COMPONENT_DIALOG: 'dialog',
  COMPONENT_ACCORDION: 'accordion',
  COMPONENT_WIZARD: 'wizard',
  COMPONENT_PICKLIST: 'picklist',
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
 * Suggest a value
 * @param {object} component Component
 * @param {object} event Event
 * @param {string} text Suggestion value
 * @memberOf Components
 */
export function suggest(component, event, text) {
  const {targetAction, settings, strict = true, serverAction = "data", addActionsTop, address} = component.props;
  const {type} = event;

  if (type === "init") {
    initialSuggest(component, text);
  } else {
    // Cancel previous fetch
    if (component.suggesting) {
      component.abortController.abort();
      component.abortController = new AbortController();
    }

    // Add strict value
    let suggestions = [];
    if (!strict && !isEmpty(text)) {
      suggestions = [{value: text, label: text}];
    }

    // Fetch server action
    const {signal} = component.abortController;
    component.suggesting = true;
    fetchAction(serverAction, targetAction, {
      ...getFormValues(component.props),
      suggest: text,
      max: 0
    }, settings.token, signal)
      .then(response => {
        component.suggesting = false;
        if (!signal.aborted) {
          const datalist = manageFillAction(response, addActionsTop, address);
          component.setState({suggestions: _.uniqBy([...(datalist.rows || []), ...suggestions], "label")});
        }
      })
      .catch(reason => console.debug("Suggest call rejected by user (search cancelled).", reason));
  }
}

/**
 * Initial suggest
 * @param {object} component Component
 * @param {string} suggest   Suggestion text
 * @memberOf Components
 */
export function initialSuggest(component, suggest) {
  const {
    serverAction,
    checkTarget,
    targetAction,
    settings,
    address,
    updateModelWithDependencies,
    keepModel
  } = component.props;

  // Fetch server action
  fetchAction(serverAction, (checkTarget || targetAction), {suggest, max: 0}, settings.token)
    .then(response => {
      const datalist = manageFillAction(response, addActionsTop, address);
      const selectedItems = [...(datalist.rows || [])]
        .filter(item => String(item.value) === String(suggest))
        .map(item => ({...item, selected: true}));
      updateModelWithDependencies(address, {values: selectedItems});
      keepModel(address);
      component.autocomplete.hide();
    });
}

/**
 * Manage a fill action in response
 * @param {object} response Response
 * @param {function} addActionsTop Add actions top
 * @param {object} address Component address
 * @return {object} datalist
 * @memberOf Components
 */
function manageFillAction(response, addActionsTop, address) {
  let fillAction = response.filter(action => action.type === "fill").shift() || {};
  const {datalist} = fillAction.parameters || {datalist: {rows: []}};
  addActionsTop(response.filter(action => action.type !== "fill").map(action => ({...action, address: address})));
  return datalist;
}

/**
 * Retrieve an uploaded file data
 * @param {object} component
 * @param {string} filename
 * @memberOf Components
 */
export function getInitialFileData(component, filename) {
  const {addActionsTop, address, settings, destination} = component.props;
  if (!isEmpty(filename)) {
    addActionsTop([generateServerAction({filename: filename, destination},
      "data", "getFileInfo", address, true, true, settings)]);
  }
  component.setState({status: isEmpty(filename) ? INITIAL : UPLOADED});
}

/**
 * Upload a file
 * @param {object} component
 * @param {object} uploader
 * @memberOf Components
 */
export function uploadFile(component, uploader) {
  const {token, uploadIdentifier} = component.props.settings;
  const {address, destination} = component.props;

  // Set headers
  uploader.xhr.setRequestHeader("Authorization", token);
  uploader.xhr.setRequestHeader("X-XSRF-TOKEN", getCookie("XSRF-TOKEN"));

  // Set form data
  uploader.formData.set("address", JSON.stringify(address));
  uploader.formData.set(uploadIdentifier, getUID());
  uploader.formData.set("destination", destination || "");

  // Change uploader state
  component.setState({status: UPLOADING});
}

/**
 * Delete an uploaded file
 * @param {object} component
 * @param {string} filename
 * @memberOf Components
 */
export function deleteFile(component, filename) {
  const {address, settings, addActionsTop, destination} = component.props;
  addActionsTop([generateServerAction({
    filename: filename,
    destination
  }, "delete-file", null, address, true, true, settings)]);
  component.setState({progress: 0, status: INITIAL});
}

/**
 * Retrieve form values
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function getFormValues(props) {
  return getAllFormValues(props, false);
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
 * Retrieve form values for printing
 * @param {object} props Properties
 * @return {object} Form values
 * @memberOf Components
 */
export function getFormValuesForPrinting(props) {
  return getAllFormValues(props, true);
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
  return Object.values(components)
    .filter(component => !("row" in component.address || "column" in component.address))
    .reduce((result, component) => {
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
  return Object.values(components)
    .filter(component => !("row" in component.address || "column" in component.address))
    .filter(component => component.attributes.checkEmpty)
    .reduce((result, component) => {
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
  return Object.values(components)
    .filter(component => !("row" in component.address || "column" in component.address))
    .reduce((result, component) => {
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
  return Object.values(components)
    .filter(component => !("row" in component.address || "column" in component.address))
    .reduce((result, component) => {
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
 * Get data depending of list length
 * @param {array} data Data
 * @return {null|*} null if length is 0, value if length is 1, array if length is higher
 * @memberOf Components
 */
export function getDataDependingOnList(data) {
  switch (data.length) {
    case 0:
      return null;
    case 1:
      return data[0];
    default:
      return data;
  }
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
    ...(value !== null ? {[attributes.id]: value} : {}),
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
    ...(value !== null ? {[attributes.id]: value} : {}),
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
    .map(value => t(getFirstDefinedAndNotNullValue(value.label, value.value, "")))
    .join(", ");
}

/**
 * Retrieve visible text data
 * @param {string} text Text to show
 * @param {function} t Translator
 * @return {object} Visible column data
 */
export function getVisibleTextData(text, t) {
  return translateLabel(text, t);
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
  let selected = asArray(model.selected).map(value => fixSelectedModel(value));
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
    values: values
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

  if (id && !(id in inspected)) {
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
  const {addActionsTop, updateModelWithDependencies, actions, address} = props;
  dropdown.toggle(e);

  // Change click event
  updateModelWithDependencies(address, {event: "click"});

  // Send actions to action container
  addActionsTop(actions.map(action => ({...action, address})));
}
