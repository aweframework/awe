import _ from "lodash";
import {getTranslateFunction} from "./index";
import {formatNumber, isNumber} from "./numbers";
import {getDataDependingOnList, getVisibleTextData} from "./components";
import {isEmpty} from "./general";

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
export function getCellModel(value = "", column = {}) {
  let colModel = column?.model || {values: []};
  if (Array.isArray(value)) {
    return value.find(item => item.selected) || {value: null};
  } else if (_.isPlainObject(value)) {
    return {value: null, ...value};
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
 * @return {object|null} Value formatted
 */
export function calculateFooterValue(column, values) {
  const {summaryType, numberFormat, name} = column;
  const extractCellValues = list => list.map(row => parseFloat(extractCellValue(row[name]))).filter(isNumber);
  const result = {value: null, label: ""};

  switch (summaryType) {
    case "sum":
      result.value = extractCellValues(values).reduce((prev, current) => current + prev, 0);
      break;
    case "avg":
      result.value = values.length > 0 ? extractCellValues(values).reduce((prev, current) => current + prev, 0) / values.length : null;
      break;
    case "max":
      result.value = extractCellValues(values).reduce((prev, current) => Math.max(prev, current), null);
      break;
    case "min":
      result.value = extractCellValues(values).reduce((prev, current) => prev === null ? current : Math.min(prev, current), null);
      break;
    default:
      return result;
  }
  result.label = formatNumber(result.value, numberFormat);
  return result;
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

/**
 * Retrieve the grid data
 * @param {object} grid Grid data
 * @param {object} model Grid model
 * @param {object} props Properties
 * @param {boolean} forPrinting Data is for printing
 * @returns {object} model data
 * @memberOf Components
 */
export function getGridData(grid, model, props, forPrinting) {
  const {attributes} = grid;
  const {values} = model;
  const {sendAll, editable, multioperation, columnModel, id} = attributes;
  const selected = values.filter((value) => value.selected);
  const editing = values.filter((value) => value?.$row?.editing);
  let sendable = values;
  if (multioperation) {
    sendable = values.filter((value) => value?.$row?.operation);
  } else if (!sendAll) {
    sendable = values.filter((value) => value.selected);
  }
  return {
    ...(columnModel || [])
      .filter(column => column.sendable)
      .map(column => column.name)
      .reduce((prevColumns, name) => ({
        ...prevColumns,
        [name]: sendable.map(value => getCellModel(value[name], columnModel.find(column => column.name === name)).value),
        [`${name}.selected`]: getDataDependingOnList(selected.map(value => getCellModel(value[name], columnModel.find(column => column.name === name)).value)),
        ...(editable || multioperation ? {[`${name}.editing`]: getDataDependingOnList(editing.map(value => getCellModel(value[name], columnModel.find(column => column.name === name)).value))} : {})
      }), {}),
    ...forPrinting ? getGridPrintData(grid, model, props) : {},
    [id]: sendable.map(value => value.id),
    ...(editable || multioperation ? {[`${id}.editing`]: editing.map(value => value.id)} : {}),
    ...(multioperation ? {[`${id}-RowTyp`]: sendable.map(value => value?.$row?.operation)} : {})
  };
}

/**
 * Retrieve the grid print data
 * @param {object} grid Grid data
 * @param {object} model Grid model
 * @param {object} props Properties
 * @returns {object} model data
 * @memberOf Components
 */
export function getGridPrintData(grid = {}, model = {}, props = {}) {
  const {attributes} = grid;
  const {values} = model;
  const {id, columnModel = [], showTotals = false} = attributes;
  const {t} = props;
  const visibleColumns = columnModel.filter(column => !column.hidden);
  return {
    ...columnModel
      .filter(column => column.sendable)
      .map(column => column.name)
      .reduce((prevColumns, name) => ({
        ...prevColumns,
        [name]: values.map(value => {
          const attr = columnModel.find(column => column.name === name);
          const cellModel = getCellModel(value[name], attr);
          return cellModel.value;
        }),
        [`${name}.data`]: values.map(value => {
          const attr = columnModel.find(column => column.name === name);
          const cellModel = getCellModel(value[name], attr);
          return getCellModelForPrinting(cellModel, attr, t);
        }),
        [`${name}.selected`]: getDataDependingOnList(values.filter(row => row.selected).map(value => getCellModel(value[name], columnModel.find(column => column.name === name)).value))
      }), {}),
    [`${id}.data`]: {
      visibleColumns: visibleColumns.map(column => getVisibleColumnData(column, t)),
      ...(showTotals ? {footer: getFooterData(visibleColumns, values)} : {})
    }
  };
}

/**
 * Retrieve cell model formatted
 * @param cellModel Cell Model
 * @param attributes Attributes
 * @param t Translate function
 */
function getCellModelForPrinting(cellModel, attributes, t) {
  const translate = getTranslateFunction(attributes, cellModel, t);
  const extraAttributes = { label: translate(cellModel.value) };

  return {...cellModel, ...extraAttributes};
}

/**
 * Retrieve footer data if show totals
 * @param columnModel Visible columns
 * @param values Grid values
 * @returns {object} footer data
 */
function getFooterData(columnModel, values) {
  return columnModel.reduce((all, column) => ({...all, [column.name]: calculateFooterValue(column, values)}), {});
}

/**
 * Retrieve visible column data
 * @param {object} column Column
 * @param {function} t Translator
 * @return {object} Visible column data
 */
function getVisibleColumnData(column, t) {
  const {name, label, type, component, width, charlength, align} = column;
  return {
    name, type, component, width, charlength, align,
    label: getVisibleTextData(label, t)
  };
}