import {extractCellValue} from "./index";
import {formatNumber, isNumber} from "./numbers";

/**
 * Grid utility functions
 * @category Utilities
 * @namespace Grid
 */

/**
 * Grid identifier
 */
const GRID_ID = "id";
export function getGridIdentifier(attributes = {}) {
  const {treegrid, treeId = "id"} = attributes;
  return treegrid ? treeId : GRID_ID;
}

/**
 * Row position type
 * @type {{BEFORE: string, LAST: string, AFTER: string, FIRST: string, CHILD: string}}
 * @memberOf Grid
 */
export const RowPositionType = {
  BEFORE: "BEFORE",
  AFTER: "AFTER",
  FIRST: "FIRST",
  LAST: "LAST",
  CHILD: "CHILD"
}

/**
 * Operation type
 * @type {{DELETE: string, INSERT: string, UPDATE: string}}
 * @memberOf Grid
 */
export const OperationType = {
  INSERT: "INSERT",
  UPDATE: "UPDATE",
  DELETE: "DELETE"
}

/**
 * Operation icon
 * @type {{INSERT: string, UPDATE: string, DELETE: string}}
 * @memberOf Grid
 */
export const OperationIcon = {
  "INSERT": "pi pi-user-plus text-success",
  "UPDATE": "pi pi-user-edit text-info",
  "DELETE": "pi pi-user-minus text-danger"
}

/**
 * Filter a row with some filters
 * @param {object} row Row to filter
 * @param {object[]} filters Filters to apply
 * @return {boolean} Row is filtered
 * @memberOf Grid
 */
export function filterRow(row, filters) {
  return Object.keys(filters).reduce((isValid, filter) => {
    const cellValue = extractCellValue(row[filter]);
    const {matchMode, value} = filters[filter];
    return isValid && ('' + cellValue)[matchMode](value)
  }, true);
}

/**
 * Row sorter
 * @param {mixed} a First value
 * @param {mixed} b Second value
 * @param {number} direction Sort direction
 * @return {number} Sort result
 * @memberOf Grid
 */
export function keySort(a, b, direction = 1) {
  if (a < b) return -direction;
  else if (a > b) return direction;
  else return 0;
}

/**
 * Sort a row
 * @param {object} row1 First row
 * @param {object} row2 Second row
 * @param {object[]} sortList Sort list
 * @return {number} Sort order
 * @memberOf Grid
 */
export function sortRow(row1, row2, sortList) {
  // Loop until sorted (-1 or 1) or until the sort keys have been processed.
  return sortList.reduce((sorted, sort) => sorted === 0 ?
    keySort(extractCellValue(row1[sort.id]), extractCellValue(row2[sort.id]), sort.order) : sorted, 0);
}

/**
 * Get sort order based on direction
 * @param {string} direction Sort direction
 * @return {number} Sort order
 * @memberOf Grid
 */
export function getDirection(direction) {
  return direction === "asc" ? 1 : -1;
}

/**
 * Retrieve width style
 * @param charLength Length in chars
 * @param width Width in pixels
 * @param fixedWidth Width in other measures
 * @return {{}|{width: string, minWidth: string}}
 */
export function getWidthStyle(charLength = null, width = null, fixedWidth = null) {
  let calculatedWidth = 0;
  if (isNumber(charLength) || isNumber(width)) {
    calculatedWidth = `${getWidth(charLength, width)}px`;
  } else if (fixedWidth != null) {
    calculatedWidth = fixedWidth;
  } else {
    return {};
  }
  return {flex: `0 0 ${calculatedWidth}`, width: calculatedWidth};
}

/**
 * Retrieve width style
 * @param charLength Length in chars
 * @param width Width in pixels
 * @return width in pixels
 */
export function getWidth(charLength = null, width = null) {
  let calculatedWidth = 0;
  if (isNumber(charLength)) {
    calculatedWidth = (charLength + 4) * 8;
  } else if (isNumber(width)) {
    calculatedWidth = width + 30;
  }
  return calculatedWidth;
}

/**
 * Calculate footer value
 * @param {Object} column Column to calculate
 * @param {Object[]} values Value list
 * @return {string|null} Value formatted
 */
export function calculateFooterValue(column, values) {
  const {summaryType, numberFormat, name} = column;
  const extractCellValues = list => list.map(row => parseFloat(extractCellValue(row[name]))).filter(isNumber);

  switch (summaryType) {
    case "sum":
      return formatNumber(extractCellValues(values).reduce((prev, current) => current + prev, 0), numberFormat);
    case "avg":
      return formatNumber(values.length > 0 ? extractCellValues(values).reduce((prev, current) => current + prev, 0) / values.length : null, numberFormat);
    case "max":
      return formatNumber(extractCellValues(values).reduce((prev, current) => Math.max(prev, current), null), numberFormat);
    case "min":
      return formatNumber(extractCellValues(values).reduce((prev, current) => prev === null ? current : Math.min(prev, current), null), numberFormat);
    default:
      return null;
  }
}

/**
 * Get row values
 * @param {object} grid Grid
 * @param {number|string} rowId Row id
 * @returns {object} Row values
 * @memberOf Utilities
 */
export function getRow(grid, rowId) {
  const {values} = grid.model;
  return values.find(row => String(row.id) === String(rowId));
}

/**
 * Retrieve column definition
 * @param grid Grid to retrieve column definition
 * @param columnName Column name to retrieve
 * @returns {object} Column definition
 */
export function getColumnDefinition(grid, columnName) {
  return grid?.attributes?.columnModel?.find(column => column.name === columnName);
}
