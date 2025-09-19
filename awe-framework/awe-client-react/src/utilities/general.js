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