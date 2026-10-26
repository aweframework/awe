import {getFirstDefinedAndNotNullValue, isEmpty} from "./general";

/**
 * Number format contract the server sends. It has the current names and the legacy (autoNumeric) ones
 */
export interface NumberFormat {
  precision?: number | string;
  mDec?: number | string;
  decimalPlaces?: number | string;
  pSign?: string;
  currencySymbol?: string;
  aSign?: string;
  digitGroupSeparator?: string;
  aSep?: string;
  decimalCharacter?: string;
  aDec?: string;
  allowDecimalPadding?: boolean;
  aPad?: boolean;
  min?: number | string;
  minimumValue?: number | string;
  vMin?: number | string;
  max?: number | string;
  maximumValue?: number | string;
  vMax?: number | string;
  step?: number;
}

/**
 * Number format translated to PrimeReact-compatible parts
 */
export interface NumberFormatParts {
  locale: string;
  useGrouping: boolean;
  maxFractionDigits: number | undefined;
  minFractionDigits: number | undefined;
  prefix: string;
  suffix: string;
  min: number | undefined;
  max: number | undefined;
  step: number | undefined;
}

interface NumberSeparators {
  decimalSeparator: string;
  groupSeparator: string;
}

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
export function getFirstDefinedValueAsNumber(...values: unknown[]): number | undefined {
  const value = getFirstDefinedAndNotNullValue(...values);
  return !isNaN(Number(value)) ? Number(value) : undefined;
}

const DEFAULT_GROUP_SEPARATOR = ",";
const DEFAULT_DECIMAL_SEPARATOR = ".";
const ENGLISH_LOCALE = "en-US";
const GERMAN_LOCALE = "de";

/**
 * Resolve decimal/grouping separators from the number format contract.
 * Partial formats keep the default English-style separators.
 * @param {object} numberFormat Number format contract
 * @returns {{decimalSeparator: string, groupSeparator: string}}
 * @memberOf Numbers
 */
function resolveNumberSeparators(numberFormat: NumberFormat = {}): NumberSeparators {
  return {
    groupSeparator: getFirstDefinedAndNotNullValue(numberFormat.digitGroupSeparator, numberFormat.aSep, DEFAULT_GROUP_SEPARATOR) as string,
    decimalSeparator: getFirstDefinedAndNotNullValue(numberFormat.decimalCharacter, numberFormat.aDec, DEFAULT_DECIMAL_SEPARATOR) as string
  };
}

/**
 * Resolve a PrimeReact locale from decimal/grouping separator semantics.
 * Unsupported combinations fall back to the closest built-in locale.
 * @param {{decimalSeparator: string, groupSeparator: string}} separators Number separators
 * @returns {string} Locale
 * @memberOf Numbers
 */
function getLocaleFromSeparators({ groupSeparator, decimalSeparator }: NumberSeparators): string {
  if (groupSeparator === "." && decimalSeparator === ",") {
    return GERMAN_LOCALE;
  }

  if (groupSeparator === "," && decimalSeparator === ".") {
    return ENGLISH_LOCALE;
  }

  return decimalSeparator === "," ? GERMAN_LOCALE : ENGLISH_LOCALE;
}

/**
 * Translate a number format into PrimeReact-compatible parts.
 * Decimal and grouping separators are resolved together so parsing/formatting
 * keeps the intended numeric magnitude even for partial numeric formats.
 * @param {object} numberFormat number format
 * @returns {{minFractionDigits: *, min: *, max: *, maxFractionDigits: *, locale: string, suffix: *, step: *}}
 * @memberOf Numbers
 */
export function translateNumberFormat(numberFormat: NumberFormat = {}): NumberFormatParts {
  const decimals = getFirstDefinedValueAsNumber(numberFormat.precision, numberFormat.mDec, numberFormat.decimalPlaces, 0);
  const isPrefix = numberFormat.pSign === "p";
  const prefixSuffix = getFirstDefinedAndNotNullValue(numberFormat.currencySymbol, numberFormat.aSign, "") as string;
  const separators = resolveNumberSeparators(numberFormat);
  const useGrouping = separators.groupSeparator !== "";
  return {
    locale: getLocaleFromSeparators(separators),
    useGrouping,
    maxFractionDigits: decimals,
    minFractionDigits: (getFirstDefinedAndNotNullValue(numberFormat.allowDecimalPadding, numberFormat.aPad, true) as boolean) ? decimals : 0,
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
export function formatNumber(number: unknown, numberFormat?: NumberFormat): string {
  const { maxFractionDigits, minFractionDigits, prefix, suffix, locale, useGrouping } = translateNumberFormat(numberFormat);
  const value = isNumber(number) ? number : parseFloat(String(number));
  return isEmpty(number) ? "" : prefix +
    value.toLocaleString(locale, { minimumFractionDigits: minFractionDigits, maximumFractionDigits: maxFractionDigits, useGrouping }) +
    suffix;
}

/**
 * Check if a value is a number
 * @param value Value to test
 * @return {boolean} Value is number
 */
export function isNumber(value: unknown): value is number {
  return !isEmpty(value) && typeof value === "number";
}
