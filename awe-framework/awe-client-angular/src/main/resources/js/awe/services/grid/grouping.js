/**
 * Row grouping of the grids (#833): the display rows of a grid grouped by one or more columns. The data rows are never
 * changed: the group rows (a header per group with its count and aggregates) and the subtotal rows (closing each group)
 * are built around them, so the model of the grid keeps only the data rows (selection, send, print and export).
 */

// Summary types of the columns that are aggregated in the group rows (the ones of the footer of the grid)
export const GROUP_AGGREGATES = ["sum", "avg", "max", "min"];

// Number format of the aggregates when the column has none (the default numeric options of AWE)
const DEFAULT_NUMBER_FORMAT = {aSep: ",", aDec: ".", dGroup: 3, aSign: "", pSign: "p", vMin: "-9999999999.99", vMax: "9999999999.99", mDec: null, aPad: false};

const isNull = value => value === null || value === undefined;

/**
 * Check whether a group value is empty (its group is labelled "(empty)" and ordered last)
 * @param {*} value Cell value
 * @returns {boolean} Value is empty
 */
const isEmptyValue = value => isNull(value) || value === "" || (Array.isArray(value) && value.length === 0);

/**
 * Value of a cell: the cell itself, or its value when it is an object with one
 * @param {*} cell Cell
 * @returns {*} Value
 */
export function getCellValue(cell) {
  return cell !== null && typeof cell === "object" && !Array.isArray(cell) && "value" in cell ? cell.value : cell;
}

/**
 * Label of a cell: its label when it is an object with one, the joined values of a list, its value otherwise
 * @param {*} cell Cell
 * @returns {string} Label
 */
export function getCellLabel(cell) {
  if (isNull(cell)) {
    return "";
  }
  if (Array.isArray(cell)) {
    return cell.join(", ");
  }
  if (typeof cell === "object") {
    const label = "label" in cell ? cell.label : cell.value;
    return isNull(label) ? "" : String(label);
  }
  return String(cell);
}

/**
 * Check whether a row of the grid is a display row (a group or subtotal row) and not a data row
 * @param {object} row Row
 * @returns {boolean} Row is a group or subtotal row
 */
export function isDisplayRow(row) {
  return !!row && (!!row.$group || !!row.$subtotal);
}

/**
 * Group-by levels of a grid: the column names (a comma-separated list or an array) that are columns of the grid, in
 * order and without repetitions. Unknown names are ignored.
 * @param {string|string[]} groupBy Group-by column names
 * @param {object[]} columns Column model of the grid
 * @returns {string[]} Group-by levels
 */
export function getGroupLevels(groupBy, columns = []) {
  const names = Array.isArray(groupBy) ? groupBy : String(groupBy || "").split(",");
  const known = new Set(columns.map(column => column.name));
  return names.map(name => String(name).trim())
    .filter((name, index, list) => name !== "" && known.has(name) && list.indexOf(name) === index);
}

/**
 * Path of a group: the path of its parent group and its own value, encoded, joined by "/" (an empty value is "")
 * @param {string|null} parentPath Path of the parent group (null on the first level)
 * @param {*} value Value of the group
 * @returns {string} Path of the group
 */
export function getGroupPath(parentPath, value) {
  const segment = encodeURIComponent(isEmptyValue(value) ? "" : String(value));
  return isNull(parentPath) ? segment : `${parentPath}/${segment}`;
}

/**
 * Aggregate of a column over some rows, as the footer of the grid calculates it: the values are parsed as numbers and
 * the ones which are not numbers are skipped; the average divides by the number of rows
 * @param {string} summaryType Summary type (sum, avg, max, min)
 * @param {object[]} rows Data rows
 * @param {string} name Column name
 * @param {function} [getValue] Value of the cell of a row (row, name)
 * @returns {number|null} Aggregate (null for an unknown summary type or without numbers to compare)
 */
export function calculateAggregate(summaryType, rows, name, getValue = (row, column) => getCellValue(row[column])) {
  const numbers = rows.map(row => parseFloat(String(getValue(row, name)))).filter(number => !Number.isNaN(number));
  const sum = () => numbers.reduce((total, number) => total + number, 0);
  switch (summaryType) {
    case "sum":
      return sum();
    case "avg":
      return rows.length > 0 ? sum() / rows.length : null;
    case "max":
      return numbers.length > 0 ? Math.max(...numbers) : null;
    case "min":
      return numbers.length > 0 ? Math.min(...numbers) : null;
    default:
      return null;
  }
}

/**
 * Number of decimals of a limit of the number format ("-999.99" has two)
 * @param {*} limit Limit (vMin or vMax)
 * @returns {number} Decimals
 */
const decimalsOf = limit => {
  const text = isNull(limit) ? "" : String(limit);
  return text.includes(".") ? text.split(".")[1].length : 0;
};

/**
 * Format a number as the numeric cells of the grid show it (their autoNumeric options: separators, decimals, padding
 * and currency sign). The decimals are mDec (or precision), or the decimals of the limits (vMin, vMax) without it
 * @param {number|null} value Number
 * @param {object} [numberFormat] Number format of the column, over the default numeric options
 * @returns {string} Formatted number (empty without number)
 */
export function formatGroupNumber(value, numberFormat = {}) {
  if (isNull(value) || Number.isNaN(Number(value))) {
    return "";
  }
  const format = {...DEFAULT_NUMBER_FORMAT, ...numberFormat};
  // Aliases of the numeric criteria (see the uiNumeric directive)
  const vMin = "min" in numberFormat ? numberFormat.min : format.vMin;
  const vMax = "max" in numberFormat ? numberFormat.max : format.vMax;
  const precision = "precision" in numberFormat ? numberFormat.precision : format.mDec;
  const decimals = isNull(precision) ? Math.max(decimalsOf(vMin), decimalsOf(vMax)) : Math.max(0, Math.floor(Number(precision)));
  const absolute = Math.abs(Number(value));
  // Rounded half up in decimal notation (12.345 is 12.35, which a binary multiplication would round down)
  const rounded = String(absolute).includes("e") ? absolute : Number(`${Math.round(Number(`${absolute}e${decimals}`))}e-${decimals}`);
  let [integer, fraction = ""] = rounded.toFixed(decimals).split(".");
  if (!format.aPad) {
    fraction = fraction.replace(/0+$/, "");
  }
  const group = parseInt(format.dGroup, 10) || 3;
  integer = integer.replace(new RegExp(`\\B(?=(\\d{${group}})+(?!\\d))`, "g"), format.aSep || "");
  const number = integer + (fraction ? format.aDec + fraction : "");
  const sign = format.aSign || "";
  const negative = Number(value) < 0 && /[1-9]/.test(number) ? "-" : "";
  return format.pSign === "s" ? `${negative}${number}${sign}` : `${negative}${sign}${number}`;
}

/**
 * Compare two values as the grid sorts its loaded rows: null values last, values by their natural order
 * @param {*} a First value
 * @param {*} b Second value
 * @returns {number} Comparison
 */
const compareValues = (a, b) => {
  if (isNull(a) || isNull(b)) {
    return (isNull(a) ? 1 : 0) - (isNull(b) ? 1 : 0);
  }
  if (a < b) {
    return -1;
  }
  return a > b ? 1 : 0;
};

/**
 * Compare two group values: empty values last, the rest by their natural order in the given direction
 * @param {*} a First value
 * @param {*} b Second value
 * @param {number} direction 1 ascending, -1 descending
 * @returns {number} Comparison
 */
const compareGroupValues = (a, b, direction = 1) => {
  if (isEmptyValue(a) || isEmptyValue(b)) {
    return (isEmptyValue(a) ? 1 : 0) - (isEmptyValue(b) ? 1 : 0);
  }
  return direction * compareValues(a, b);
};

const directionOf = sort => (String(sort.direction).toLowerCase() === "desc" ? -1 : 1);

/**
 * Build the display rows of a grouped grid: for each group (recursively for every level) its group row, then, when it
 * is expanded, its children (the groups of the next level or its data rows) and its subtotal row.
 *
 * Ordering: the data rows inside each group follow the sort list. The groups of a level are ordered by their value
 * ascending (empty value last), unless the first sort is the group-by column of that level (its direction applies, empty
 * value last) or a column with summary type (the groups of every level are ordered by that aggregate, in its direction).
 *
 * @param {object[]} values Data rows of the grid (passed through unchanged)
 * @param {object} options Grouping options
 * @param {string|string[]} options.groupBy Group-by column names (unknown names are ignored)
 * @param {object[]} options.columns Column model of the grid (name, sortField, summaryType, numberFormat)
 * @param {{id: string, direction: string}[]} [options.sort] Sort list (id is the sort field or the name of a column)
 * @param {function|object} [options.expanded] Whether a group is expanded, by its path (a function or a map of paths;
 * groups are expanded by default)
 * @param {boolean} [options.subtotals] Close each expanded group with a subtotal row (true by default)
 * @param {function} [options.translate] Translation of a key (the label of the empty values)
 * @param {function} [options.getValue] Value of the cell of a data row (row, column name)
 * @param {function} [options.getLabel] Label of the cell of a data row (row, column)
 * @param {function} [options.format] Label of an aggregate (value, column)
 * @returns {object[]} Display rows
 */
export function buildGroupedRows(values, options) {
  const {
    groupBy, columns = [], sort = [], expanded = () => true, subtotals = true, translate = key => key,
    getValue = (row, name) => getCellValue(row[name]),
    getLabel = (row, column) => getCellLabel(row[column.name]),
    format = (value, column) => formatGroupNumber(value, typeof column.numberFormat === "object" ? column.numberFormat || {} : {})
  } = options;
  const levels = getGroupLevels(groupBy, columns);
  if (levels.length === 0) {
    return [...values];
  }

  const isExpanded = typeof expanded === "function" ? expanded : path => !expanded || expanded[path] !== false;
  const columnOf = id => columns.find(column => column.sortField === id) || columns.find(column => column.name === id) || null;
  const levelColumns = levels.map(name => columns.find(column => column.name === name));
  const aggregated = columns.filter(column => GROUP_AGGREGATES.includes(column.summaryType));

  // Data rows: the sort list, several columns, keeping the order of the rows on ties
  const sorts = sort.filter(item => item && !isNull(item.id))
    .map(item => ({name: (columnOf(item.id) || {name: item.id}).name, direction: directionOf(item)}));
  const sortRows = rows => rows.map((row, index) => ({row, index}))
    .sort((a, b) => {
      for (const {name, direction} of sorts) {
        const result = compareValues(getValue(a.row, name), getValue(b.row, name));
        if (result !== 0) {
          return direction * result;
        }
      }
      return a.index - b.index;
    })
    .map(item => item.row);

  // Groups: by the first sort when it is their group-by column or an aggregated column, by value otherwise
  const first = sorts.length > 0 ? columnOf(sort[0].id) : null;
  const firstDirection = sorts.length > 0 ? sorts[0].direction : 1;
  const byAggregate = first && GROUP_AGGREGATES.includes(first.summaryType) ? first.name : null;
  const compareGroups = (a, b, column) => {
    if (byAggregate) {
      const result = compareValues(a.aggregates[byAggregate].value, b.aggregates[byAggregate].value);
      if (result !== 0) {
        return firstDirection * result;
      }
    }
    return compareGroupValues(a.value, b.value, first && first.name === column.name ? firstDirection : 1);
  };

  const getAggregates = rows => aggregated.reduce((all, column) => {
    const value = calculateAggregate(column.summaryType, rows, column.name, getValue);
    return {...all, [column.name]: {value, label: isNull(value) ? "" : format(value, column)}};
  }, {});

  // Groups of a level (recursively): their group row, children and subtotal row
  const buildLevel = (rows, levelIndex, parentPath) => {
    const column = levelColumns[levelIndex];
    const groups = new Map();
    rows.forEach(row => {
      const value = getValue(row, column.name);
      const key = isEmptyValue(value) ? "" : `v:${String(value)}`;
      if (!groups.has(key)) {
        groups.set(key, {value: isEmptyValue(value) ? null : value, first: row, rows: []});
      }
      groups.get(key).rows.push(row);
    });

    return [...groups.values()]
      .map(group => ({...group, aggregates: getAggregates(group.rows)}))
      .sort((a, b) => compareGroups(a, b, column))
      .flatMap(group => {
        const level = levelIndex + 1;
        const path = getGroupPath(parentPath, group.value);
        const label = isNull(group.value) ? translate("GRID_GROUP_EMPTY") : getLabel(group.first, column) || String(group.value);
        const open = isExpanded(path) !== false;
        const groupRow = {
          id: `$group-${path}`,
          $group: {level, path, column: column.name, value: group.value, label, count: group.rows.length, expanded: open, aggregates: group.aggregates}
        };
        if (!open) {
          return [groupRow];
        }
        const children = levelIndex + 1 < levelColumns.length ? buildLevel(group.rows, levelIndex + 1, path) : sortRows(group.rows);
        const subtotal = subtotals === false ? [] : [{id: `$subtotal-${path}`, $subtotal: {level, path, label, aggregates: group.aggregates}}];
        return [groupRow, ...children, ...subtotal];
      });
  };

  return buildLevel(values, 0, null);
}
