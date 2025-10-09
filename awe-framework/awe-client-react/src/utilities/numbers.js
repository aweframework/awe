import {getFirstDefinedAndNotNullValue, isEmpty} from "./general";

/**
 * Number utility functions
 * @category Utilities
 * @namespace Numbers
 */

/**
 * Get first defined value as number
 * @returns {number} First number defined
 * @memberOf Numbers
 */
export function getFirstDefinedValueAsNumber() {
  let value = getFirstDefinedAndNotNullValue.apply(this, arguments);
  return !isNaN(Number(value)) ? Number(value) : undefined;
}

/**
 * Get locale from separator
 * @param separator Separator
 * @returns {string} Locale
 * @memberOf Numbers
 */
function getLocaleFromSeparator(separator) {
  switch (separator) {
    case ",":
      return "en-US";
    case ".":
    default:
      return "de";
  }
}

/**
 * Translate a number format into parts
 * @param {object} numberFormat number format
 * @returns {{minFractionDigits: *, min: *, max: *, maxFractionDigits: *, locale: string, suffix: *, step: *}}
 * @memberOf Numbers
 */
export function translateNumberFormat(numberFormat = {}) {
  let decimals = getFirstDefinedValueAsNumber(numberFormat.precision, numberFormat.mDec, numberFormat.decimalPlaces, 0);
  let isPrefix = numberFormat.pSign === "p";
  let prefixSuffix = getFirstDefinedAndNotNullValue(numberFormat.currencySymbol, numberFormat.aSign, "");
  return {
    locale: getLocaleFromSeparator(getFirstDefinedAndNotNullValue(numberFormat.digitGroupSeparator, numberFormat.aSep, undefined)),
    maxFractionDigits: decimals,
    minFractionDigits: getFirstDefinedAndNotNullValue(numberFormat.allowDecimalPadding, numberFormat.aPad, true) ? decimals : 0,
    prefix: isPrefix ? prefixSuffix : "",
    suffix: !isPrefix ? prefixSuffix : "",
    min: getFirstDefinedValueAsNumber(numberFormat.min, numberFormat.minimumValue, numberFormat.vMin, undefined),
    max: getFirstDefinedValueAsNumber(numberFormat.max, numberFormat.maximumValue, numberFormat.vMax, undefined),
    step: numberFormat.step
  };
}

/**
 * Format a number given a numberFormat
 * @param number Number to format
 * @param numberFormat Format parameters
 * @return {string} Formatted number
 */
export function formatNumber(number, numberFormat) {
  const {maxFractionDigits, minFractionDigits, prefix, suffix, locale} = translateNumberFormat(numberFormat);
  const value = isNumber(number) ? number : parseFloat(number);
  return isEmpty(number) ? "" : prefix +
    value.toLocaleString(locale, {minimumFractionDigits: minFractionDigits, maximumFractionDigits: maxFractionDigits}) +
    suffix;
}

/**
 * Check if a value is a number
 * @param value Value to test
 * @return {boolean} Value is number
 */
export function isNumber(value) {
  return !isEmpty(value) && typeof value === "number";
}
