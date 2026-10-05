import _ from "lodash";
import {getDataDependingOnList, getVisibleTextData} from "./index";
import {formatNumber, isNumber} from "./numbers";
import {ComponentType, isEmpty} from "./general";

const {
  COMPONENT_NUMERIC,
  COMPONENT_TIME,
  COMPONENT_SUGGEST,
  COMPONENT_SELECT
} = ComponentType;

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
};

/**
 * Operation type
 * @type {{DELETE: string, INSERT: string, UPDATE: string}}
 * @memberOf Grid
 */
export const OperationType = {
  INSERT: "INSERT",
  UPDATE: "UPDATE",
  DELETE: "DELETE"
};

/**
 * Operation icon
 * @type {{INSERT: string, UPDATE: string, DELETE: string}}
 * @memberOf Grid
 */
export const OperationIcon = {
  "INSERT": "pi pi-user-plus text-success",
  "UPDATE": "pi pi-user-edit text-info",
  "DELETE": "pi pi-user-minus text-danger"
};

/**
 * Name of the operation icon, as the test hooks expose it (the same names the AngularJS client uses)
 * @type {{INSERT: string, UPDATE: string, DELETE: string}}
 * @memberOf Grid
 */
export const OperationIconName = {
  "INSERT": "plus",
  "UPDATE": "edit",
  "DELETE": "trash"
};

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
 * Get cell values as list or single value
 * @param {mixed} cell Cell to extract
 * @return {mixed} Cell values
 * @memberOf Utilities
 */
export function extractCellValues(cell) {
  if (Array.isArray(cell)) {
    const selected = cell.filter(data => data.selected);
    const items = selected.length > 0 ? selected : cell;
    return items.map(data => data.value);
  } else if (_.isPlainObject(cell)) {
    return cell.value;
  } else {
    return cell;
  }
}

/**
 * Get cell labels as a printable string
 * @param {mixed} cell Cell to extract
 * @return {string} Cell labels
 * @memberOf Utilities
 */
export function extractCellLabels(cell) {
  if (Array.isArray(cell)) {
    const selected = cell.filter(data => data.selected);
    const items = selected.length > 0 ? selected : cell;
    return items
      .map(data => data.label || data.value)
      .filter(value => !isEmpty(value))
      .join(", ");
  } else if (_.isPlainObject(cell)) {
    return cell.label || cell.value;
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
 * Get footer value
 * @param {Object} footer
 * @param {String} columnId
 * @memberOf Utilities
 */
export function getFooterValue(footer = {}, columnId) {
  return extractCellValue(footer[columnId] ?? null);
}

/**
 * Get cell model
 * @param {mixed} value Value
 * @param {object} column Column definition
 * @returns {Object}
 * @memberOf Utilities
 */
export function getCellModel(value = null, column = {}) {
  let colModel = column?.model || {values: []};
  if (Array.isArray(value)) {
    return value.find(item => item.selected) || {value: null};
  } else if (_.isPlainObject(value)) {
    return {value: null, ...value};
  } else if (colModel.values.length > 0) {
    return colModel.values.find(item => String(item.value) === String(value)) || {value: value, label: value};
  } else {
    return {value: value, label: isEmpty(value) ? "" : value};
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
    .filter(v => v > -1)[0];
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
    return isValid && ('' + cellValue)[matchMode](value);
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
  const normalize = (value) => {
    if (value === null || value === undefined) {
      return { type: "nil", value: null };
    }
    const numeric = Number(value);
    if (!Number.isNaN(numeric) && String(value).trim() !== "") {
      return { type: "number", value: numeric };
    }
    return { type: "string", value: String(value).toLowerCase() };
  };

  const aNorm = normalize(a);
  const bNorm = normalize(b);

  if (aNorm.type === "nil" && bNorm.type !== "nil") return -direction;
  if (aNorm.type !== "nil" && bNorm.type === "nil") return direction;

  if (aNorm.value < bNorm.value) return -direction;
  if (aNorm.value > bNorm.value) return direction;
  return 0;
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
  const {attributes, address} = grid;
  const {values} = model;
  const {sendAll, editable, multioperation, columnModel, id} = attributes;
  const selected = values.filter((value) => value.selected);
  const editing = values.filter((value) => value?.$row?.editing);
  let sendable = values;
  if (multioperation) {
    sendable = values.filter((value) => value?.$row?.operation);
  } else if (!sendAll) {
    sendable = selected;
  }
  return {
    ...(columnModel || [])
      .filter(column => column.sendable)
      .map(column => column.name)
      .reduce((prevColumns, name) => ({
        ...prevColumns,
        [name]: sendable.map(value => extractCellValues(value[name])),
        [`${name}.selected`]: getDataDependingOnList(selected.map(value => extractCellValues(value[name]))),
        ...(editable || multioperation ? {[`${name}.editing`]: getDataDependingOnList(editing.map(value => extractCellValues(value[name])))} : {})
      }), {}),
    ...forPrinting ? getGridPrintData(grid, model, props) : {},
    [id]: sendable.map(value => value.id),
    ...(selected.length > 0 ? {[`${id}.selected`]: getDataDependingOnList(selected.map(value => value.id))} : {}),
    ...(selected.length > 0 ? {[`${id}.selectedRowAddress`]: getDataDependingOnList(selected.map(value => value.id).map(value => ({...address, row: value})))} : {}),
    ...(editable || multioperation ? {[`${id}.editing`]: getDataDependingOnList(editing.map(value => value.id))} : {}),
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
          if (Array.isArray(value[name])) {
            return extractCellValues(value[name]);
          }
          const cellModel = getCellModel(value[name], attr);
          return cellModel.value;
        }),
        [`${name}.data`]: values.map(value => {
          const attr = columnModel.find(column => column.name === name);
          if (Array.isArray(value[name])) {
            const label = extractCellLabels(value[name]);
            return { value: extractCellValues(value[name]), label };
          }
          const cellModel = getCellModel(value[name], attr);
          return getCellModelForPrinting(cellModel, attr, t);
        }),
        [`${name}.selected`]: getDataDependingOnList(values.filter(row => row.selected).map(value => extractCellValues(value[name])))
      }), {}),
    [`${id}.data`]: {
      visibleColumns: visibleColumns.map(column => getVisibleColumnData(column, t)),
      ...(showTotals ? {footer: getFooterData(visibleColumns, values)} : {})
    }
  };
}

/**
 * Retrieve translate function depending on
 * @param attributes Attributes
 * @param model Model
 * @param t Translate text function
 */
export function getTranslateFunction(attributes, model, t = ((f) => f)) {
  const {component, numberFormat} = attributes;
  switch (component) {
    case COMPONENT_NUMERIC:
      return (v) => formatNumber(v, numberFormat);
    case COMPONENT_TIME:
      return (v) => v;
    case COMPONENT_SUGGEST:
      return () => model.label;
    case COMPONENT_SELECT:
      return () => t(model.label);
    default:
      return t;
  }
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
 * Extract a cell model data in array format
 * @param cellModel
 * @param cellData
 */
export function getCellSuggestData(cellModel, cellData) {
  const cellValue = extractCellModel(cellData);
  return [...cellModel.values, ...[{...cellValue, selected: true, label: cellValue.label || cellValue.value}]
    .filter(d => !isEmpty(d.value))];
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
