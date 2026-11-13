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
  COMPONENT_DATE_RANGE: 'date-range',
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
} as const;

/**
 * Suffixes of the two parameters a date range criterion sends (start and end of the range)
 */
export const DATE_RANGE_FROM_SUFFIX = '.from';
export const DATE_RANGE_TO_SUFFIX = '.to';

/**
 * Check if a component type supports multiple selection
 * @param {string} component Component type
 * @returns {boolean}
 */
export function isMultipleComponent(component: string | undefined): boolean {
  return component === ComponentType.COMPONENT_SELECT_MULTIPLE
    || component === ComponentType.COMPONENT_SUGGEST_MULTIPLE
    || component === ComponentType.COMPONENT_PICKLIST;
}

/**
 * Compare if two values are equal
 * @param {type} value1
 * @param {type} value2
 * @returns {Boolean}
 * @memberOf Utilities
 */
export function compareEqualValues(value1: unknown, value2: unknown): boolean {
  let equals: boolean;
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
export function getFirstDefinedValue<T extends unknown[]>(...values: T): T[number] | undefined {
  return getFirstDefined(values, [undefined]);
}

/**
 * Get first defined value and not null
 * @returns {*}
 * @memberOf Utilities
 */
export function getFirstDefinedAndNotNullValue<T extends unknown[]>(...values: T): T[number] | undefined {
  return getFirstDefined(values, [undefined, null]);
}

/**
 * Get first defined value
 * @param {array} values Values to check
 * @param {array} exclude Excluded values
 * @return {*}
 */
function getFirstDefined<T>(values: readonly T[], exclude: unknown[]): T | undefined {
  return values.reduce<T | undefined>((items, item) => exclude.includes(items) ? item : items, undefined);
}

/**
 * Returns true if a variable is null or empty
 * @param {*} n Variable to test
 * @return {boolean} String is null or undefined
 * @memberOf Utilities
 */
export function isEmpty(n: unknown): boolean {
  return n === null || n === undefined || String(n).trim() === "";
}

/**
 * Returns true if a variable is null or empty
 * @param {Object} n Variable to test
 * @return {boolean} String is null or undefined
 * @memberOf Utilities
 */
export function isEmptyCell(n: unknown): boolean {
  // After isEmpty(n) an object is never null, so reading its value is safe
  return isEmpty(n) || (typeof n === "object" && isEmpty((n as {value?: unknown}).value));
}
