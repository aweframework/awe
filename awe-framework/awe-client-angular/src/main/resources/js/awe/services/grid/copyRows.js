import {ROW_DETAIL_COLUMN} from "./rowDetail";
import {MOVE_ROWS_COLUMN} from "./rowDrag";

// Operation of a row of a send-operations grid (GridMultioperation)
const ROW_TYPE = "RowTyp";
const DELETED = "DELETE";
// Columns that the grid adds to the ones of the screen
const GRID_COLUMNS = new Set(["chkBox", "rowNum", "RowIco", ROW_DETAIL_COLUMN, MOVE_ROWS_COLUMN]);

/**
 * Value of a cell: its value when it is an object with a value, the cell otherwise
 * @param {*} cell Cell
 * @returns {*} Value
 */
const cellValue = cell => cell !== null && typeof cell === "object" && "value" in cell ? cell.value : cell;

/**
 * Columns of the screen that are shown, in their order
 * @param {Array} columnModel Columns of a grid
 * @returns {Array} Shown columns
 */
const shownColumns = columnModel => (columnModel || [])
  .filter(column => column?.name && !column.hidden && !GRID_COLUMNS.has(column.name));

/**
 * Copy of a cell: a cell of a column with options (select) is its value and the translated label of its option
 * @param {*} cell Cell
 * @param {Object} column Column of the cell
 * @param {Function} translate Translates a label
 * @returns {*} Copied cell
 */
const copyCell = (cell, column, translate) => {
  const value = cellValue(cell);
  const option = (column.model?.values || []).find(item => String(item.value) === String(value));
  return option ? {value, label: translate(option.label)} : _.cloneDeep(cell);
};

/**
 * Copy of the rows of a grid for the copy-rows action (#793): the rows that are not deleted, with their identifier, and
 * each shown column of the target with the cell of the shown column of the source in the same position (the names of
 * the columns of a screen are unique, so the target columns have their own names)
 * @param {Array} rows Rows of the source grid
 * @param {Array} sourceColumns Columns of the source grid
 * @param {Array} targetColumns Columns of the target grid
 * @param {Function} translate Translates a label
 * @param {String} identifier Row identifier field
 * @returns {Array} Copied rows
 */
export function copyGridRows(rows, sourceColumns, targetColumns, translate, identifier = "id") {
  const sources = shownColumns(sourceColumns);
  const pairs = shownColumns(targetColumns)
    .map((column, index) => ({name: column.name, source: sources[index]}))
    .filter(pair => pair.source);
  return (rows || [])
    .filter(row => cellValue(row[ROW_TYPE]) !== DELETED)
    .map(row => pairs.reduce((copy, pair) => ({...copy, [pair.name]: copyCell(row[pair.source.name], pair.source, translate)}),
      {[identifier]: row[identifier]}));
}
