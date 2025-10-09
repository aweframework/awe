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
 * Returns true if a variable is null or empty
 * @param {Object} n Variable to test
 * @return {boolean} String is null or undefined
 * @memberOf Utilities
 */
export function isEmpty(n) {
  return n === null || n === undefined || String(n).trim() === "";
}

/**
 * Returns true if a variable is null or empty
 * @param {Object} n Variable to test
 * @return {boolean} String is null or undefined
 * @memberOf Utilities
 */
export function isEmptyCell(n) {
  return isEmpty(n) || (typeof n === "object" && isEmpty(n.value));
}