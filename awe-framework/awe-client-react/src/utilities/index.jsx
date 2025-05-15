import _ from "lodash";
import {delayActions} from "../redux/actions/actions";
import {fromDate} from "./dates";
import download from "downloadjs";
import React from "react";
import {Tooltip} from "primereact/tooltip";
import {getVisibleTextData} from "./components";

/**
 * Utilities functions
 * @category Utilities
 * @namespace Utilities
 */

/**
 * Combine service actions
 * @returns {*} Combined actions
 * @memberOf Utilities
 */
export function combineActions() {
  return [...arguments]
    .map(service => service.getActions())
    .reduce((allActions, actions) => ({...allActions, ...actions}), {});
}

/**
 * Compare if two values are equal
 * @param {type} value1
 * @param {type} value2
 * @returns {Boolean}
 * @memberOf Utilities
 */
export function compareEqualValues(value1, value2) {
  let equals;
  if (typeof value1 === typeof value2) {
    equals = value1 === value2;
  } else {
    equals = (isEmpty(value1) && isEmpty(value2)) ||
      String(value1) === String(value2);
  }
  return equals;
}


/**
 * Get first defined value
 * @returns {*}
 * @memberOf Utilities
 */
export function getFirstDefinedValue() {
  return getFirstDefined(arguments, [undefined]);
}

/**
 * Get first defined value and not null
 * @returns {*}
 * @memberOf Utilities
 */
export function getFirstDefinedAndNotNullValue() {
  return getFirstDefined(arguments, [undefined, null]);
}

/**
 * Get first defined value
 * @param {array} values Values to check
 * @param {array} exclude Excluded values
 * @return {*}
 */
function getFirstDefined(values, exclude) {
  return [...values].reduce((items, item) => exclude.includes(items) ? item : items, undefined);
}

/**
 * Get REST url
 * @returns {*}
 * @memberOf Utilities
 */
export function getRestUrl() {
  return [...arguments].reduce((items, item) => isEmpty(item) ? items : `${items}/${item}`, "");
}

/**
 * Generate an address
 * @param {string} view View
 * @param {string} component Component
 * @param {string} column Column
 * @param {string} row Row
 * @memberOf Utilities
 */
export function generateAddress(view, component, column, row) {
  let address = {view: view, component: component};
  address = column !== undefined ? {...address, column: column} : address;
  return row !== undefined ? {...address, row: row} : address;
}

/**
 * Manage parameter as array
 * @param {Array|Object|String|Number} value
 * @return {Array} value as array
 * @memberOf Utilities
 */
export function asArray(value) {
  return isEmpty(value) ? [] : [...(Array.isArray(value) ? value : [value])];
}

/**
 * Get component value depending on size: 0 -> null, 1 -> value, n -> list
 * @param {Array} list
 * @returns {*}
 * @memberOf Utilities
 */
export function componentValue(list) {
  return list.reduce((items, item) => items == null ? item : [...asArray(items), item], null);
}

/**
 * Retrieve cell attribute
 * @param {Object} model
 * @param {String} attribute
 * @return {*} value
 * @memberOf Utilities
 */
export function getCellAttribute(model, attribute) {
  return _.isPlainObject(model) && model != null ? model[attribute] : model;
}

/**
 * Get cell
 * @param {array} values Values
 * @param {number} rowIndex Row index
 * @param {string} columnId Column id
 * @memberOf Utilities
 */
export function getCell(values, rowIndex, columnId) {
  return rowIndex !== -1 ? values[rowIndex][columnId] || null : null;
}

/**
 * Get cell value
 * @param {mixed} cell Cell to extract
 * @return {mixed} Cell value
 * @memberOf Utilities
 */
export function extractCellValue(cell) {
  if (Array.isArray(cell)) {
    return cell
      .filter(data => data.selected)
      .map(data => data.value).join(", ");
  } else if (_.isPlainObject(cell)) {
    return cell.value;
  } else {
    return cell;
  }
}

/**
 * Get cell model
 * @param {mixed} cell Cell to extract
 * @return {object} Cell model
 * @memberOf Utilities
 */
export function extractCellModel(cell) {
  if (Array.isArray(cell)) {
    return cell.reduce((prev, data) => data.selected ? data : prev, {value: null});
  } else if (_.isPlainObject(cell)) {
    return cell;
  } else {
    return {value: cell};
  }
}

/**
 * Get cell value
 * @param values
 * @param rowIndex
 * @param columnId
 * @memberOf Utilities
 */
export function getCellValue(values, rowIndex, columnId) {
  return extractCellValue(getCell(values, rowIndex, columnId));
}

/**
 * Get cell model
 * @param {mixed} value Value
 * @param {object} column Column definition
 * @returns {Object}
 * @memberOf Utilities
 */
export function getCellModel(value = "", column= {}) {
  let colModel = column?.model || {values: []};
  if (Array.isArray(value)) {
    return value.find(item => item.selected) || {value: null};
  } else if (_.isPlainObject(value)) {
    return {...value};
  } else if (colModel.values.length > 0) {
    return colModel.values.find(item => String(item.value) === String(value)) || {value: value, label: value};
  } else {
    return {value: value, label: value};
  }
}

/**
 * Get row index
 * @param {array} values Values
 * @param {number|string} rowId Row id
 * @returns {number} Row index
 * @memberOf Utilities
 */
export function getRowIndex(values, rowId) {
  return values.findIndex(row => String(row.id) === String(rowId));
}

/**
 * Get selected row index
 * @param {array} values Values
 * @returns {number} Row index
 * @memberOf Utilities
 */
export function getSelectedRowIndex(values) {
  return values.findIndex(row => row.selected);
}

/**
 * Get editing row index
 * @param {array} values Values
 * @returns {number} Row index
 * @memberOf Utilities
 */
export function getEditingRowIndex(values) {
  return values.findIndex(row => row.$row?.editing);
}

/**
 * Get existing index in a list of indexes
 * @param values Index values
 * @returns {number} Existing index
 */
export function getExistingIndex(values) {
  let value = values
    .filter(v => typeof v === 'number')
    .filter(v => v > -1)[0]
  return isEmpty(value) ? -1 : value;
}

/**
 * Get editing row
 * @param {array} values Values
 * @returns {object} Found row or empty object
 * @memberOf Utilities
 */
export function getEditingRow(values) {
  return values.find(row => row.$row?.editing) || {};
}

/**
 * Evaluates a expression
 * @param {string} expression string
 * @returns {Object} expression evaluated
 * @memberOf Utilities
 */
export function evaluateExpression(expression) {
  let ctor = "constructor";
  return window.constructor[ctor][ctor]("return " + expression + ";")();
}

/**
 * Evaluates a formule and retrieves the result
 * @param {string} expression Formule expression
 * @param {Object} values
 * @returns {Object} expression evaluated
 * @memberOf Utilities
 */
export function formule(expression, values) {
  // Replace formule parameters
  _.each(values, function (value, valueId) {
    expression = expression.replace(new RegExp(`\\[${valueId}\\]`, "ig"), value);
  });
  expression = expression.replace(/#/ig, "\"");

  // Eval formule
  try {
    return evaluateExpression(expression);
  } catch (exc) {
    console.error("[FORMULE] Formule: " + expression, {params: values, exception: exc});
  }

  return expression;
}

/**
 * Check actions
 * @param {object} actions Object actions
 * @param {object} prevProps Previous properties
 * @param {object} props Current properties
 * @memberOf Utilities
 */
export function checkActions(actions, prevProps, props) {
  if (!_.isEqual(prevProps.runningActions, props.runningActions) && props.runningActions.length) {
    props.runningActions.forEach(a => {
      const {type} = a;
      if (type in actions) {
        console.info("Running action: ", a);
        props.startAction(a);
        delayActions(props.settings.actionsStack, () => actions[type](a, props));
      }
    });
  }
}

/**
 * Format a message with parameters
 * @param {object} error
 * @param {function} t translate function
 * @returns {string|object} Message formatted
 * @memberOf Utilities
 */
export function formatMessage(error, t) {
  if (error) {
    const {message, values = {}} = error;
    const fixDate = (value) => !isEmpty(value) && _.isDate(value) ? fromDate(value) : String(value);
    return Object.entries(values).reduce((formatted, [key, value]) => formatted.replace(`{${key}}`, fixDate(value)), t(message));
  } else {
    return undefined;
  }
}

/**
 * Returns true if a variable is null or empty
 * @param {Object} n Variable to test
 * @return {boolean} String is null or undefined
 * @memberOf Utilities
 */
export function isEmpty(n) {
  return n === null || n === undefined || String(n).trim() === ""
}

/**
 * Retrieve component values
 * @param {object} component Component
 * @memberOf Utilities
 */
export function getComponentValue(component) {
  return getSelectedValues(component).reduce((_lastValue, value) => value, null);
}

/**
 * Retrieve component values
 * @param {object} component Component
 */
export function getSelectedValues(component) {
  return (component?.model?.values || [])
    .filter((value) => value.selected)
    .map((value) => value.value);
}

/**
 * Retrieve group selected values
 * @param {object} component Component
 * @param {object} components Components
 */
export function getGroupSelectedValues(component, components) {
  const {group} = component.attributes;
  return Object.values(components)
    .filter(item => item.attributes.group === group)
    .reduce((result, item) => [
      ...result,
      ...getSelectedValues(item)
    ], []);
}

/**
 * Check if a component is inside context
 * @param {object} searchContext Search context
 * @param {string} view View
 * @param {string} source Context source
 * @return {boolean|*}
 * @memberOf Utilities
 */
export function isInsideContext(searchContext, view, source) {
  return searchContext.view === view && searchContext.source.includes(source);
}

/**
 * Retrieve action source
 * @param {object} action Action
 * @param {object} components Component list
 * @return {*}
 * @memberOf Utilities
 */
export function getActionSource(action, components) {
  // If target, get target
  const {address, target} = action;
  let source = target;

  // If target is defined get components from target, else get all components from component inner source
  if (!target) {
    let callerSource = components[address.component].context.source;
    source = callerSource[callerSource.length - 2];
  }

  return source;
}

/**
 * Retrieve cookie value
 * @param cookieName Cookie name
 * @return {*}
 * @memberOf Utilities
 */
export function getCookie(cookieName) {
  let cookieList = document.cookie.split(";");
  const cookieMap = cookieList.reduce((allCookies, cookie) => ({
    ...allCookies,
    [cookie.split("=")[0]]: cookie.split("=")[1]
  }), {});
  return cookieMap[cookieName];
}

/**
 * Fetch an action
 * @param serverAction Server action
 * @param targetAction Target action
 * @param data Action data
 * @param token Authorization token
 * @param signal Cancellation signal
 * @return {Promise<unknown>}
 * @memberOf Utilities
 */
export function fetchAction(serverAction, targetAction, data, token, signal = undefined) {
  return fetchJson("POST", getRestUrl("action", serverAction, targetAction), data, token, signal);
}

/**
 * Fetch an screen
 * @param option Option name
 * @param view View name
 * @param token Authorization token
 * @param data Data sent to other screen
 * @return {Promise<unknown>}
 * @memberOf Utilities
 */
export function fetchScreen(option, view, token, data) {
  return fetchJson("POST", getRestUrl("screen-data", option), {
    ...data,
    "serverAction": "screen-data",
    "view": view,
    "template": false
  }, token);
}

/**
 * Fetch json data
 * @param method Method
 * @param url Url
 * @param body Data to send
 * @param token Authorization token
 * @param signal Cancellation signal
 * @return {Promise<Response>}
 * @memberOf Utilities
 */
export function fetchJson(method, url, body, token, signal = undefined) {
  return fetch(`${getContextPath()}${url}`, {
    signal,
    method: method,
    body: JSON.stringify(body),
    headers: {
      "Accept": "application/json,text/plain,*/*",
      "Accept-Encoding": "gzip",
      "Authorization": token,
      "Content-Type": "application/json;charset=UTF-8",
      "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
    }
  })
    .then(response => response.ok ?
      response.json() :
      response.text().then(message => ({message, status: response.status})));
}

/**
 * Fetch file
 * @param url File url
 * @param data File data
 * @param token Access token
 * @returns {Promise}
 * @memberOf Utilities
 */
export function fetchFile(url, data, token) {
  let filename = "";
  return fetch(`${getContextPath()}${url}`, {
    method: 'POST',
    body: JSON.stringify(data),
    headers: {
      "Accept": "*/*",
      "Authorization": token,
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
    }
  }).then((response) => {
    filename = response.headers.get("Filename");
    return response.blob();
  }).then((blob) => download(blob, filename));
}

/**
 * Fetch html
 * @param url File url
 * @param token Access token
 * @returns {Promise}
 * @memberOf Utilities
 */
export function fetchHtml(url, token) {
  return fetch(`${getContextPath()}${url}`, {
    method: 'GET',
    headers: {
      "Accept": "text/html",
      "Authorization": token,
      "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
    }
  }).then(response => response.text());
}

/**
 * Retrieve context path from html
 */
export function getContextPath() {
  let contextPath = (document.getElementsByTagName('base')[0] || {href: "/"}).href
    .replace(location.origin || "", "")
    .replaceAll("/", "");
  return isEmpty(contextPath) ? "" : `/${contextPath}`;
}

export function getBaseHref() {
  return (document.getElementsByTagName('base')[0] || {href: "/"}).href
}

/**
 * Bind methods to object
 * @param bindObject Bind object
 * @param methods Methods to bind
 * @memberOf Utilities
 */
export function bindMethods(bindObject, methods = []) {
  methods.forEach(method => bindObject[method] = bindObject[method].bind(bindObject));
}

/**
 * Generate server action
 * @param {object} values
 * @param {string} serverAction
 * @param {string} targetAction
 * @param {object} address
 * @param {boolean} async
 * @param {boolean} silent
 * @param {object} settings
 * @return {object} Server action
 * @memberOf Utilities
 */
export function generateServerAction(values, serverAction, targetAction, address, async = false, silent = false, settings = {}) {
  return {
    type: 'server',
    async, silent, address,
    parameters: {
      ...values,
      address,
      component: address.component,
      [settings.serverActionKey]: serverAction,
      [settings.targetActionKey]: targetAction
    }
  };
}

/**
 * Generate message action
 * @param type Message type
 * @param title Message title
 * @param message Message content
 * @returns {object} Message action
 * @memberOf Utilities
 */
export function generateMessageAction(type = "ok", title = null, message = "") {
  return {
    type: 'message',
    parameters: {type, title, message}
  };
}

/**
 * Get size string
 * @param {number} size File size
 * @return {string} Size string
 * @memberOf Utilities
 */
export function getSizeString(size) {
  return ["B", "KB", "MB", "GB", "TB", "PB"]
    .reduce((prev, current, index) => size >= Math.pow(1024, index) && size < Math.pow(1024, index + 1) ?
      `${(Math.floor(size * 10 / Math.pow(1024, index)) / 10)} ${current}` : prev, `${size}`);
}

/**
 * Get component with address
 * @param {object[]} components Component list
 * @param {object} address Component address
 * @return component found
 * @memberOf Utilities
 */
export function getComponent(components, address) {
  return Object.values(components).find(component => _.isEqual(component.address, address));
}

/**
 * Get action address
 * @param {object} action
 * @return Action address
 * @memberOf Utilities
 */
export function getActionAddress(action) {
  const {target, address} = action;
  return target ? {view: address.view, component: target} : {...address};
}

/**
 * Extract icon name and family
 * @param icon
 */
export function extractIcon(icon = "") {
  const [key, value] = icon.replace("fa-", "").split(":");
  return {name: value || key, family: value ? key : "fa"};
}

/**
 * Get icon HTML template
 * @param icon Icon to generate
 * @param extraClasses Extra classes to add to the icon
 */
export function getIconCode(icon, extraClasses = "") {
  const iconValues = extractIcon(icon);
  if (icon) {
    switch (iconValues.family) {
      case "mdi":
        return <i role="icon" className={`material-icons ${extraClasses}`}>{iconValues.name}</i>;
      default:
        return <i role="icon"
                  className={`${iconValues.family} ${iconValues.family}-${iconValues.name} ${extraClasses}`}/>;
    }
  }
  return null;
}

/**
 * Add an event listener to a node to launch a function, and if timeout, launch timeout function
 * @param node Node
 * @param event Event to check
 * @param fn Function
 * @param fnTimeout Function in case of timeout
 * @param timeout Timeout (in milliseconds)
 */
export function addEventListenerTimeout(node, event, fn, fnTimeout, timeout) {
  const timer = setTimeout(() => {
    node.removeEventListener(event, eventFn);
    fnTimeout();
  }, timeout);
  const eventFn = () => {
    timer && clearTimeout(timer);
    node.removeEventListener(event, eventFn);
    fn();
  };
  node.addEventListener(event, eventFn);
}

/**
 * Generate tooltip node for components
 * @param help Help text
 * @param helpImage Help image
 * @param t Translator function
 * @param target Target node class
 * @returns {JSX.Element}
 */
export function getHelpTooltipNode(help, helpImage, t, target) {
  const imageNode = helpImage ? <img src={t(helpImage)} alt={t(help)}/> : null;
  if (help || helpImage) {
    return <Tooltip target={target}>
      {getVisibleTextData(help, t)}
      {imageNode}
    </Tooltip>;
  }
  return null;
}

/**
 * Parse string to boolean
 * @param value Value to parse
 */
export function parseBoolean(value = "") {
  return value.toLowerCase() === "true";
}

/**
 * Translate label
 * @param label Label to translate
 * @param t translate function
 * @return {string} Label translated
 */
export function translateLabel(label = "", t = (l) => l) {
  return label.split(" ").map(t).join(" ");
}
