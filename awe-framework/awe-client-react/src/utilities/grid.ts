import _ from "lodash";
import type {ComponentAddress, ScreenColumn} from "../contract";
import {getDataDependingOnList, getVisibleTextData} from "./index";
import {formatNumber, isNumber} from "./numbers";
import type {NumberFormat} from "./numbers";
import {ComponentType, isEmpty} from "./general";
import {matchesShortcut} from "./keyboard";
import {isActionColumn} from "./gridLayout";

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

/** Translator of a text */
type Translate = (key: string) => string;

/** An option of a cell or of a list of options: its value, its label and whether it is selected */
export interface CellOption {
  value?: unknown;
  label?: unknown;
  selected?: unknown;
  style?: unknown;
  [key: string]: unknown;
}

/** A row of a grid: its identifier, the operation it is going through and the cells by column */
export interface GridRow {
  id?: unknown;
  selected?: unknown;
  $row?: {editing?: boolean; operation?: string};
  [column: string]: unknown;
}

/** A column of a grid: the columns always have a name, and their number format is already evaluated */
export type GridColumn = Omit<ScreenColumn, "numberFormat"> & {name: string; numberFormat?: NumberFormat};

/** Attributes of a grid */
export interface GridAttributes {
  id: string;
  treegrid?: boolean;
  treeId?: string;
  sendAll?: boolean;
  editable?: boolean;
  multioperation?: boolean;
  showTotals?: boolean;
  columnModel?: GridColumn[];
  [key: string]: unknown;
}

/** Model of a grid */
export interface GridModel {
  values: GridRow[];
}

/** A grid in the state: its attributes, its model and where it is */
export interface GridComponent {
  attributes: GridAttributes;
  address?: ComponentAddress;
  model: GridModel;
}

/** Properties of a component that prints its data: the translator */
interface PrintProps {
  t?: Translate;
}

/** Position a row moves to */
export type RowPosition = "BEFORE" | "AFTER" | "FIRST" | "LAST";

/**
 * Grid identifier
 */
const GRID_ID = "id";
export function getGridIdentifier(attributes: {treegrid?: boolean; treeId?: string; [key: string]: unknown} = {}): string {
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
} as const;

/**
 * Client action that moves the selected row of a grid to each position
 */
export const MoveRowActionType = {
  [RowPositionType.FIRST]: "move-row-top",
  [RowPositionType.BEFORE]: "move-row-up",
  [RowPositionType.AFTER]: "move-row-down",
  [RowPositionType.LAST]: "move-row-bottom"
} as const;

/**
 * Shortcuts that move the selected row of an editable grid, with the position they move it to. Alt must be held and no
 * other modifier.
 */
export const MOVE_ROW_SHORTCUTS = Object.freeze([
  {shortcut: {key: "ArrowUp", alt: true}, position: RowPositionType.BEFORE},
  {shortcut: {key: "ArrowDown", alt: true}, position: RowPositionType.AFTER},
  {shortcut: {key: "Home", alt: true}, position: RowPositionType.FIRST},
  {shortcut: {key: "End", alt: true}, position: RowPositionType.LAST}
] as const);

/**
 * Retrieve the position a row moves to for a keyboard event
 * @param {KeyboardEvent} event Keyboard event
 * @returns {string|null} Row position (BEFORE, AFTER, FIRST or LAST) or null if the key does not move rows
 */
export function getMoveRowKeyPosition(event: KeyboardEvent): RowPosition | null {
  return MOVE_ROW_SHORTCUTS.find(({shortcut}) => matchesShortcut(event, shortcut))?.position ?? null;
}

/** Identifier of a grid row, absent when there is no row */
type RowIdentifier = string | number | null | undefined;

/**
 * Move a row of a list of values to a new position
 * @param {Array} values Rows of the grid
 * @param {string} gridId Row identifier field of the grid
 * @param {string|number} rowId Identifier of the row to move
 * @param {string} position Where to move the row: FIRST, BEFORE (one up), AFTER (one down) or LAST
 * @returns {Array} New list of rows, or the same list when the row does not exist or cannot move that way
 */
export function moveRowInValues<R extends Record<string, unknown>>(
  values: R[],
  gridId: string,
  rowId: RowIdentifier,
  position: string
): R[] {
  const index = values.findIndex(row => rowId != null && String(row[gridId]) === String(rowId));
  const lastIndex = values.length - 1;
  const targets: Partial<Record<string, number>> = {
    [RowPositionType.FIRST]: 0,
    [RowPositionType.BEFORE]: index - 1,
    [RowPositionType.AFTER]: index + 1,
    [RowPositionType.LAST]: lastIndex
  };
  const target = targets[position];
  if (index < 0 || target === undefined || target < 0 || target > lastIndex || target === index) {
    return values;
  }
  const moved = [...values];
  moved.splice(target, 0, moved.splice(index, 1)[0]);
  return moved;
}

/**
 * Move a row of a list of values next to another row (drag and drop)
 * @param {Array} values Rows of the grid
 * @param {string} gridId Row identifier field of the grid
 * @param {string|number} rowId Identifier of the row to move
 * @param {string|number} targetRowId Identifier of the row the moved row ends up next to
 * @param {string} position BEFORE (just above the target row) or AFTER (just below it)
 * @returns {Array} New list of rows, or the same list when a row does not exist, the order would not change or the
 * position is not BEFORE or AFTER
 */
export function moveRowNextToRow<R extends Record<string, unknown>>(
  values: R[],
  gridId: string,
  rowId: RowIdentifier,
  targetRowId: RowIdentifier,
  position: string
): R[] {
  const matches = (row: R, id: RowIdentifier) => id != null && String(row[gridId]) === String(id);
  const index = values.findIndex(row => matches(row, rowId));
  const others = values.filter(row => !matches(row, rowId));
  const targetIndex = others.findIndex(row => matches(row, targetRowId));
  if (index < 0 || targetIndex < 0 || (position !== RowPositionType.BEFORE && position !== RowPositionType.AFTER)) {
    return values;
  }
  const insertAt = position === RowPositionType.BEFORE ? targetIndex : targetIndex + 1;
  if (insertAt === index) {
    return values;
  }
  const moved = [...others];
  moved.splice(insertAt, 0, values[index]);
  return moved;
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
} as const;

/**
 * Operation icon
 * @type {{INSERT: string, UPDATE: string, DELETE: string}}
 * @memberOf Grid
 */
export const OperationIcon = {
  "INSERT": "pi-user-plus text-success",
  "UPDATE": "pi-user-edit text-info",
  "DELETE": "pi-user-minus text-danger"
} as const;

/**
 * Name of the operation icon, as the test hooks expose it (the same names the AngularJS client uses)
 * @type {{INSERT: string, UPDATE: string, DELETE: string}}
 * @memberOf Grid
 */
export const OperationIconName = {
  "INSERT": "plus",
  "UPDATE": "edit",
  "DELETE": "trash"
} as const;

/**
 * Retrieve cell attribute
 * @param {Object} model
 * @param {String} attribute
 * @return {*} value
 * @memberOf Utilities
 */
export function getCellAttribute(model: unknown, attribute: string): unknown {
  return _.isPlainObject(model) && model != null ? (model as Record<string, unknown>)[attribute] : model;
}

/**
 * Get cell
 * @param {array} values Values
 * @param {number} rowIndex Row index
 * @param {string} columnId Column id
 * @memberOf Utilities
 */
export function getCell(values: readonly GridRow[], rowIndex: number, columnId: string): unknown {
  return rowIndex !== -1 ? values[rowIndex][columnId] || null : null;
}

/**
 * Get cell value
 * @param {mixed} cell Cell to extract
 * @return {mixed} Cell value
 * @memberOf Utilities
 */
export function extractCellValue<T>(cell: T): T extends readonly CellOption[] ? string : unknown;
export function extractCellValue(cell: unknown): unknown {
  if (Array.isArray(cell)) {
    return cell
      .filter(data => data.selected)
      .map(data => data.value).join(", ");
  } else if (_.isPlainObject(cell)) {
    return (cell as CellOption).value;
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
export function extractCellValues<T>(cell: T): T extends readonly CellOption[] ? unknown[] : unknown;
export function extractCellValues(cell: unknown): unknown {
  if (Array.isArray(cell)) {
    const selected = cell.filter(data => data.selected);
    const items = selected.length > 0 ? selected : cell;
    return items.map(data => data.value);
  } else if (_.isPlainObject(cell)) {
    return (cell as CellOption).value;
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
export function extractCellLabels<T>(cell: T): T extends readonly CellOption[] ? string : unknown;
export function extractCellLabels(cell: unknown): unknown {
  if (Array.isArray(cell)) {
    const selected = cell.filter(data => data.selected);
    const items = selected.length > 0 ? selected : cell;
    return items
      .map(data => data.label || data.value)
      .filter(value => !isEmpty(value))
      .join(", ");
  } else if (_.isPlainObject(cell)) {
    return (cell as CellOption).label || (cell as CellOption).value;
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
export function extractCellModel(cell: unknown): CellOption {
  if (Array.isArray(cell)) {
    return cell.reduce<CellOption>((prev, data) => data.selected ? data : prev, {value: null});
  } else if (_.isPlainObject(cell)) {
    return cell as CellOption;
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
export function getCellValue(values: readonly GridRow[], rowIndex: number, columnId: string): unknown {
  return extractCellValue(getCell(values, rowIndex, columnId));
}

/**
 * Get footer value
 * @param {Object} footer
 * @param {String} columnId
 * @memberOf Utilities
 */
export function getFooterValue(footer: Record<string, unknown> | undefined = {}, columnId: string): unknown {
  return extractCellValue(footer[columnId] ?? null);
}

/**
 * Get cell model
 * @param {mixed} value Value
 * @param {object} column Column definition
 * @returns {Object}
 * @memberOf Utilities
 */
export function getCellModel(value: unknown = null, column: {model?: {values?: CellOption[]}} | null = {}): CellOption {
  const colModel = column?.model || {values: []};
  if (Array.isArray(value)) {
    return value.find(item => item.selected) || {value: null};
  } else if (_.isPlainObject(value)) {
    return {value: null, ...(value as CellOption)};
  } else if ((colModel.values as CellOption[]).length > 0) {
    return (colModel.values as CellOption[]).find(item => String(item.value) === String(value)) || {value: value, label: value};
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
export function getRowIndex(values: readonly GridRow[], rowId: string | number): number {
  return values.findIndex(row => String(row.id) === String(rowId));
}

/**
 * Get selected row index
 * @param {array} values Values
 * @returns {number} Row index
 * @memberOf Utilities
 */
export function getSelectedRowIndex(values: readonly GridRow[]): number {
  return values.findIndex(row => row.selected);
}

/**
 * Get editing row index
 * @param {array} values Values
 * @returns {number} Row index
 * @memberOf Utilities
 */
export function getEditingRowIndex(values: readonly GridRow[]): number {
  return values.findIndex(row => row.$row?.editing);
}

/**
 * Get existing index in a list of indexes
 * @param values Index values
 * @returns {number} Existing index
 */
export function getExistingIndex(values: readonly unknown[]): number {
  const value = values
    .filter((v): v is number => typeof v === 'number')
    .filter(v => v > -1)[0];
  return isEmpty(value) ? -1 : value;
}

/**
 * Get editing row
 * @param {array} values Values
 * @returns {object} Found row or empty object
 * @memberOf Utilities
 */
export function getEditingRow<R extends GridRow>(values: readonly R[]): Partial<R> {
  return values.find(row => row.$row?.editing) || {};
}

/**
 * Filter a row with some filters
 * @param {object} row Row to filter
 * @param {object[]} filters Filters to apply
 * @return {boolean} Row is filtered
 * @memberOf Grid
 */
export function filterRow(row: Record<string, unknown>, filters: Record<string, {matchMode: string; value: string}>): boolean {
  return Object.keys(filters).reduce((isValid, filter) => {
    const cellValue = extractCellValue(row[filter]);
    const {matchMode, value} = filters[filter];
    // The match mode is the name of a method of the text (startsWith, endsWith...)
    return isValid && (('' + cellValue) as unknown as Record<string, (value: string) => boolean>)[matchMode](value);
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
export function keySort(a: unknown, b: unknown, direction: number = 1): number {
  const normalize = (value: unknown): {type: "nil" | "number" | "string"; value: number | string | null} => {
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

  // Both values are normalized here: they are compared as JavaScript does, numbers or texts
  if ((aNorm.value as number) < (bNorm.value as number)) return -direction;
  if ((aNorm.value as number) > (bNorm.value as number)) return direction;
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
export function sortRow(row1: Record<string, unknown>, row2: Record<string, unknown>, sortList: Array<{id: string; order?: number}>): number {
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
export function getDirection(direction: string): 1 | -1 {
  return direction === "asc" ? 1 : -1;
}

/**
 * Check if a button of a grid is a selection button (selection="true"): it is shown in the selection bar of the grid
 * while it has selected rows, not in its footer
 * @param button Button of the button model of the grid
 * @returns The button is a selection button
 */
export function isSelectionButton(button: {selection?: boolean | string} | null | undefined): boolean {
  return button?.selection === true || button?.selection === "true";
}

/** Width of the selection (multiselect) pre-column */
export const SELECTION_COLUMN_WIDTH = "40px";

/** Width of the row edit column: its edit button and the cell padding of the column (the edited row makes it grow) */
export const EDITOR_COLUMN_WIDTH = "calc(var(--awe-size-control-sm) + 2 * var(--awe-space-2))";

/** Style that gives a column its width */
interface WidthStyle {
  flex: string;
  width: string;
  minWidth: string;
  maxWidth: string;
  overflow: string;
}

// Width of a column that takes the rest of the width of the grid (width="*")
const FILL_WIDTH = "*";

/**
 * Width style of the row number pre-column: it grows with the digits of the last row number
 * @param {number} [first] Index of the first row of the page
 * @param {number} [rows] Rows of the page
 * @return {{}|{width: string, flex: string}}
 * @memberOf Grid
 */
export function getRowNumberWidthStyle(first: number = 0, rows: number = 0): Partial<WidthStyle> {
  return getWidthStyle(String((first || 0) + (rows || 0)).length - 2);
}

/**
 * Retrieve width style
 * @param charLength Length in chars
 * @param width Width in pixels, or "*" for the rest of the width of the grid
 * @param fixedWidth Width in other measures
 * @return {{}|{flex: string, width: string, minWidth: string}|{width: string, maxWidth: string, overflow: string}} The
 * minimum width keeps the column at its width: the table of a grid (automatic layout) would squeeze it to fit, and the
 * grid scrolls instead, as the prototype. A column with width "*" takes the rest of the width (whatever its charlength,
 * as in the AngularJS engine) and shrinks below its content (its text ends in an ellipsis), so the other columns stay
 * in view
 */
export function getWidthStyle(charLength: number | null = null, width: number | string | null = null, fixedWidth: string | null = null): Partial<WidthStyle> {
  if (width === FILL_WIDTH) {
    return {width: "100%", maxWidth: "0", overflow: "hidden"};
  }
  let calculatedWidth: string;
  if (isNumber(charLength) || isNumber(width)) {
    calculatedWidth = `${getWidth(charLength, isNumber(width) ? width : null)}px`;
  } else if (fixedWidth != null) {
    calculatedWidth = fixedWidth;
  } else {
    return {};
  }
  return {flex: `0 0 ${calculatedWidth}`, width: calculatedWidth, minWidth: calculatedWidth};
}

/**
 * Retrieve width style
 * @param charLength Length in chars
 * @param width Width in pixels
 * @return width in pixels
 */
export function getWidth(charLength: number | null = null, width: number | null = null): number {
  let calculatedWidth = 0;
  if (isNumber(charLength)) {
    calculatedWidth = (charLength + 4) * 8;
  } else if (isNumber(width)) {
    calculatedWidth = width + 30;
  }
  return calculatedWidth;
}

/**
 * Retrieve how the paginator of a grid is shown. As in the AngularJS client, a grid with pagination disabled has no
 * paginator bar unless it has buttons, which live in the bar; then the bar shows only the buttons and every row.
 * @param {{disablePagination: (boolean|undefined), buttonModel: (Array|undefined)}} attributes Grid attributes
 * @param {number} rows Rows per page
 * @param {number} count Number of rows loaded in the grid
 * @return {{paginator: boolean, pageRows: number}} Whether the paginator is rendered and the rows of each page
 * @memberOf Grid
 */
export function getPaginatorSettings(
  attributes: {disablePagination?: boolean; buttonModel?: unknown[]} = {},
  rows: number,
  count: number
): {paginator: boolean; pageRows: number} {
  if (!attributes.disablePagination) {
    return {paginator: true, pageRows: rows};
  }
  return {paginator: (attributes.buttonModel || []).length > 0, pageRows: Math.max(rows || 0, count)};
}

/**
 * Retrieve how the text of the cells of a column wraps. A column wraps when it declares wrap, or a maximum number of
 * lines (which implies it) unless wrap is explicitly false.
 * @param {{wrap: (boolean|undefined), maxLines: (number|undefined)}} column Column definition
 * @return {{wrap: boolean, maxLines: (number|null)}} Wrap flag and maximum number of lines (null when unlimited)
 * @memberOf Grid
 */
export function getColumnWrap(column: {wrap?: boolean; maxLines?: number} = {}): {wrap: boolean; maxLines: number | null} {
  const {wrap, maxLines} = column;
  const limit = isNumber(maxLines) && maxLines > 0 ? Math.floor(maxLines) : null;
  if (wrap === false || (wrap !== true && limit === null)) {
    return {wrap: false, maxLines: null};
  }
  return {wrap: true, maxLines: limit};
}

/**
 * Arrange the frozen columns of a grid. Like the pinned columns of AngularJS they go to the left, in their relative
 * order, whatever their position in the column model. A grid with grouped headers ignores them, as the pinned groups
 * cannot be kept aligned with a multi-line header.
 * @param {Object[]} columns Visible columns
 * @param {Object[]} [headerModel] Grouped headers of the grid
 * @return {{columns: Object[], hasFrozen: boolean}} Arranged columns and whether the grid has frozen columns
 * @memberOf Grid
 */
export function arrangeFrozenColumns<C extends {frozen?: boolean}>(
  columns: C[] = [],
  headerModel: unknown[] = []
): {columns: C[]; hasFrozen: boolean} {
  const hasFrozen = headerModel.length === 0 && columns.some(column => column.frozen);
  if (!hasFrozen) {
    return {columns, hasFrozen: false};
  }
  return {columns: [...columns.filter(column => column.frozen), ...columns.filter(column => !column.frozen)], hasFrozen};
}

/**
 * Calculate footer value
 * @param {Object} column Column to calculate
 * @param {Object[]} values Value list
 * @return {object|null} Value formatted
 */
export function calculateFooterValue(
  column: {summaryType?: string; numberFormat?: NumberFormat; name: string},
  values: readonly GridRow[]
): {value: number | null; label: string} {
  const {summaryType, numberFormat, name} = column;
  const extractCellValues = (list: readonly GridRow[]) => list.map(row => parseFloat(String(extractCellValue(row[name])))).filter(isNumber);
  const result: {value: number | null; label: string} = {value: null, label: ""};

  switch (summaryType) {
    case "sum":
      result.value = extractCellValues(values).reduce((prev, current) => current + prev, 0);
      break;
    case "avg":
      result.value = values.length > 0 ? extractCellValues(values).reduce((prev, current) => current + prev, 0) / values.length : null;
      break;
    case "max":
      result.value = extractCellValues(values).reduce<number | null>(
        // The first comparison is made against null, which Math.max reads as 0, as it always did
        (prev, current) => Math.max(prev as number, current), null);
      break;
    case "min":
      result.value = extractCellValues(values).reduce<number | null>((prev, current) => prev === null ? current : Math.min(prev, current), null);
      break;
    default:
      return result;
  }
  result.label = formatNumber(result.value, numberFormat);
  return result;
}

/**
 * Get row values (by the identifier of the grid, which is not "id" in tree grids)
 * @param {object} grid Grid
 * @param {number|string} rowId Row id
 * @returns {object} Row values
 * @memberOf Utilities
 */
export function getRow<R extends GridRow>(
  grid: {model: {values: R[]}; attributes?: {treegrid?: boolean; treeId?: string; [key: string]: unknown}},
  rowId: string | number
): R | undefined {
  const {values} = grid.model;
  const gridId = getGridIdentifier(grid.attributes);
  return values.find(row => String(row[gridId]) === String(rowId));
}

/**
 * Retrieve column definition
 * @param grid Grid to retrieve column definition
 * @param columnName Column name to retrieve
 * @returns {object} Column definition
 */
export function getColumnDefinition(
  grid: {attributes?: {columnModel?: GridColumn[]}} | null | undefined,
  columnName: string
): GridColumn | undefined {
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
export function getGridData(
  grid: GridComponent,
  model: GridModel,
  props: PrintProps,
  forPrinting?: boolean
): Record<string, unknown> {
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
      .reduce<Record<string, unknown>>((prevColumns, name) => ({
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
export function getGridPrintData(
  // The grid and the model are always given; their defaults are kept as they were
  grid: GridComponent = {} as GridComponent,
  model: GridModel = {} as GridModel,
  props: PrintProps = {}
): Record<string, unknown> {
  const {attributes} = grid;
  const {values} = model;
  const {id, columnModel = [], showTotals = false} = attributes;
  const {t} = props;
  // The columns sent to the report, as in the AngularJS engine: never the action columns (component="button"), and
  // the declared printable value of each column (see isPrintableColumn)
  const visibleColumns = columnModel.filter(isPrintableColumn);
  return {
    ...columnModel
      .filter(column => column.sendable)
      .map(column => column.name)
      .reduce<Record<string, unknown>>((prevColumns, name) => ({
        ...prevColumns,
        [name]: values.map(value => {
          const attr = columnModel.find(column => column.name === name) as GridColumn;
          if (Array.isArray(value[name])) {
            return extractCellValues(value[name]);
          }
          const cellModel = getCellModel(value[name], attr);
          return cellModel.value;
        }),
        [`${name}.data`]: values.map(value => {
          const attr = columnModel.find(column => column.name === name) as GridColumn;
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
export function getTranslateFunction(
  attributes: {component?: string; numberFormat?: NumberFormat},
  model: CellOption,
  t: Translate = ((f) => f)
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- the translator takes text, the others take any value of a cell
): (value: any) => unknown {
  const {component, numberFormat} = attributes;
  switch (component) {
    case COMPONENT_NUMERIC:
      return (v) => formatNumber(v, numberFormat);
    case COMPONENT_TIME:
      return (v) => v;
    case COMPONENT_SUGGEST:
      return () => model.label;
    case COMPONENT_SELECT:
      return () => t(model.label as string);
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
function getCellModelForPrinting(cellModel: CellOption, attributes: GridColumn, t: Translate | undefined): CellOption {
  const translate = getTranslateFunction(attributes, cellModel, t);
  const extraAttributes = { label: translate(cellModel.value) };

  return {...cellModel, ...extraAttributes};
}

/**
 * Extract a cell model data in array format
 * @param cellModel
 * @param cellData
 */
export function getCellSuggestData(cellModel: {values: CellOption[]}, cellData: unknown): CellOption[] {
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
function getFooterData(columnModel: GridColumn[], values: GridRow[]) {
  return columnModel.reduce<Record<string, unknown>>((all, column) => ({...all, [column.name]: calculateFooterValue(column, values)}), {});
}

/**
 * Retrieve visible column data
 * @param {object} column Column
 * @param {function} t Translator
 * @return {object} Visible column data
 */
/**
 * Check whether a column goes to the report, according to its declared `printable` value, as the AngularJS engine:
 * never an action column (component="button") nor a column with `printable="false"`; always (also hidden) with
 * `printable="true"` or `printable="excel"` (the server keeps the excel ones for spreadsheets); else only when visible
 * @param column Column
 * @return Column is printed
 */
function isPrintableColumn(column: GridColumn): boolean {
  if (isActionColumn(column) || column.printable === false) {
    return false;
  }
  switch (String(column.printMode ?? "").toLowerCase()) {
    case "false":
      return false;
    case "true":
    case "excel":
      return true;
    default:
      return !column.hidden;
  }
}

function getVisibleColumnData(column: GridColumn, t: Translate | undefined) {
  const {name, label, type, component, width, charlength, align, secondaryField, exportSecondary, secondaryLabel, printMode} = column;
  return {
    name, type, component, width, charlength, align,
    label: getVisibleTextData(label, t),
    // The declared printable value, so the server keeps the spreadsheet only columns (excel) out of documents
    ...(printMode != null ? {printable: printMode} : {}),
    // The secondary line of the column (#832), so that the export can write it in its own column
    ...(secondaryField ? {
      secondaryField,
      exportSecondary: exportSecondary === true,
      secondaryLabel: secondaryLabel ? getVisibleTextData(secondaryLabel, t) : null
    } : {})
  };
}
