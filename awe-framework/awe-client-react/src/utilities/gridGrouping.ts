import {calculateFooterValue, extractCellLabels, extractCellValue, getCellModel, getDirection, getTranslateFunction, keySort, sortRow} from "./grid";
import type {CellOption, GridColumn, GridRow} from "./grid";
import {ComponentType, isEmpty} from "./general";

/**
 * Row grouping of the grids (#833): the group and subtotal rows a grid shows between its data rows. The model of the
 * grid keeps its data rows (selection, send, print and export do not change); the display rows are built here from them.
 * @category Utilities
 * @namespace GridGrouping
 */

/** Translator of a text */
type Translate = (key: string) => string;

/** Aggregate of a column over the data rows of a group: its value and its formatted label */
export interface GroupAggregate {
  value: number | null;
  label: string;
}

/** Aggregates of a group by column name (the columns with summary type) */
export type GroupAggregates = Record<string, GroupAggregate>;

/** Group of a group row */
export interface GroupInfo {
  /** Grouping level, from 1 */
  level: number;
  /** Values of the levels of the group, encoded and joined by "/" */
  path: string;
  /** Column that groups this level */
  column: string;
  /** Value of the group ("" when empty) */
  value: string;
  /** Text of the group: the label of the cell, or "(empty)" */
  label: string;
  /** Data rows of the group */
  count: number;
  expanded: boolean;
  aggregates: GroupAggregates;
}

/** Subtotal that closes an expanded group */
export interface SubtotalInfo {
  level: number;
  path: string;
  label: string;
  aggregates: GroupAggregates;
}

/** Group row of the display list */
export interface GroupRow {
  id: string;
  $group: GroupInfo;
}

/** Subtotal row of the display list */
export interface SubtotalRow {
  id: string;
  $subtotal: SubtotalInfo;
}

/** A row of the display list: a data row, a group row or a subtotal row */
export type DisplayRow = GridRow | GroupRow | SubtotalRow;

/** Sort of a column, as the grid keeps it */
export interface GroupingSort {
  id: string;
  order?: number;
  direction?: string;
}

/** Expanded groups: every group follows "all" unless its path says otherwise */
export interface GroupExpansion {
  all?: boolean;
  paths?: Record<string, boolean>;
}

/** Options of {@link buildGroupedRows} */
export interface GroupingOptions {
  /** Columns that group the rows, one level each, in order */
  groupBy: readonly string[];
  /** Column model of the grid */
  columns?: readonly GridColumn[];
  /** Sort of the grid */
  sort?: readonly GroupingSort[];
  expansion?: GroupExpansion;
  /** Close each expanded group with a subtotal row (default true) */
  subtotals?: boolean;
  t?: Translate;
}

/** Grouping of a grid, read from its attributes */
export interface GroupingState {
  /** Columns that group the rows now */
  levels: string[];
  /** The rows are grouped */
  active: boolean;
  /** The grid can group its rows (not a tree grid, nor an editable, send-operations, move-rows or row-detail one) */
  groupable: boolean;
  /** The toolbar shows the "Group by" control */
  selector: boolean;
  subtotals: boolean;
  expansion: Required<GroupExpansion>;
}

/** Group as it is built, before it becomes display rows */
interface GroupNode {
  info: GroupInfo;
  rows: GridRow[];
  children: GroupNode[] | null;
}

const {COMPONENT_NUMERIC, COMPONENT_SELECT, COMPONENT_SUGGEST} = ComponentType;
const GROUP_PREFIX = "$group-";
const SUBTOTAL_PREFIX = "$subtotal-";

/**
 * Check if a row of the display list is a group row
 * @param {object} row Row
 * @returns {boolean} Row is a group row
 */
export function isGroupRow(row: unknown): row is GroupRow {
  return !!row && typeof row === "object" && "$group" in row && !!(row as GroupRow).$group;
}

/**
 * Check if a row of the display list is a subtotal row
 * @param {object} row Row
 * @returns {boolean} Row is a subtotal row
 */
export function isSubtotalRow(row: unknown): row is SubtotalRow {
  return !!row && typeof row === "object" && "$subtotal" in row && !!(row as SubtotalRow).$subtotal;
}

/**
 * Check if a row of the display list is a group or subtotal row (not a data row of the model)
 * @param {object} row Row
 * @returns {boolean} Row is a display row
 */
export function isDisplayRow(row: unknown): row is GroupRow | SubtotalRow {
  return isGroupRow(row) || isSubtotalRow(row);
}

/**
 * Style classes of a group or subtotal row: grid-group-row grid-group-level-{n} or grid-subtotal-row grid-subtotal-level-{n}
 * @param {object} row Display row
 * @returns {string} Style classes
 */
export function getDisplayRowClassName(row: GroupRow | SubtotalRow): string {
  const type = isGroupRow(row) ? "group" : "subtotal";
  const {level} = isGroupRow(row) ? row.$group : row.$subtotal;
  return `grid-${type}-row grid-${type}-level-${level}`;
}

/**
 * Path of a group: the values of its levels, each one encoded, joined by "/"
 * @param {string[]} values Values of the levels of the group
 * @returns {string} Group path
 */
export function getGroupPath(values: readonly string[]): string {
  return values.map(value => encodeURIComponent(value)).join("/");
}

/**
 * Columns that group the rows: the comma separated names (or a list), trimmed, without duplicates and without the
 * names that are not columns of the grid
 * @param {string|string[]} groupBy Group-by columns
 * @param {object[]} columns Column model of the grid
 * @returns {string[]} Grouping levels
 */
export function getGroupLevels(groupBy: string | readonly string[] | null | undefined, columns: readonly GridColumn[] = []): string[] {
  const names = Array.isArray(groupBy) ? groupBy : String(groupBy ?? "").split(",");
  const known = new Set(columns.map(column => column.name));
  return [...new Set(names.map(name => String(name).trim()).filter(name => known.has(name)))];
}

/**
 * Grouping of a grid from its attributes: the group-by levels (or the ones chosen at runtime with the selector), whether
 * it can group and the expanded groups. The expanded groups are kept while the levels stay the same; with other levels
 * only the last expand or collapse all is kept.
 * @param {object} attributes Grid attributes
 * @returns {object} Grouping state
 */
export function getGroupingState(attributes: Record<string, unknown> = {}): GroupingState {
  const {treegrid, editable, multioperation, moveRows, rowDetail, groupBy, groupSelector, groupSubtotals,
    groupLevels, groupExpansion, columnModel} = attributes;
  const groupable = !treegrid && !editable && !multioperation && !moveRows && !rowDetail;
  const columns = (columnModel ?? []) as GridColumn[];
  const levels = groupable ? getGroupLevels((groupLevels ?? groupBy) as string | string[] | undefined, columns) : [];
  const stored = (groupExpansion ?? {}) as GroupExpansion & {levels?: string[]};
  const sameLevels = (stored.levels ?? []).join(",") === levels.join(",");
  return {
    levels,
    active: levels.length > 0,
    groupable,
    selector: groupable && groupSelector === true,
    subtotals: groupSubtotals !== false,
    expansion: {all: stored.all ?? true, paths: sameLevels ? {...stored.paths} : {}}
  };
}

/**
 * Value and label of the cell that groups a row: the label of select and suggest cells, the formatted number of
 * numeric cells and the label or value of the others
 */
function getGroupKey(row: GridRow, column: GridColumn, t: Translate): {value: string; label: string} {
  const cell = row[column.name];
  const raw = extractCellValue(cell);
  if (isEmpty(raw) || raw === "") {
    return {value: "", label: t("GRID_GROUP_EMPTY")};
  }
  const value = String(raw);
  let label: unknown;
  if (Array.isArray(cell)) {
    label = extractCellLabels(cell as CellOption[]);
  } else {
    const model = getCellModel(cell, column as {model?: {values?: CellOption[]}});
    const component = column.component;
    label = component === COMPONENT_SELECT || component === COMPONENT_SUGGEST || component === COMPONENT_NUMERIC ?
      getTranslateFunction(column, model, t)(model.value) : model.label ?? model.value;
  }
  return {value, label: isEmpty(label) || label === "" ? value : String(label)};
}

/** Aggregates of the columns with summary type over some data rows */
function getAggregates(rows: readonly GridRow[], summaryColumns: readonly GridColumn[]): GroupAggregates {
  return Object.fromEntries(summaryColumns.map(column => [column.name, calculateFooterValue(column, rows)]));
}

/** Direction of a sort: its order or, without it, its direction */
const sortOrder = (sort: GroupingSort): number => sort.order ?? getDirection(sort.direction ?? "asc");

/**
 * Comparator of the groups of a level: by its value when the first sort is on its column or on no column of the
 * grouping, by an aggregate when it is on a column with summary type. The empty group goes last when sorted by value.
 */
function groupComparator(column: string, firstSort: GroupingSort | undefined, summaryColumns: readonly GridColumn[]) {
  const byValue = (direction: number) => (a: GroupNode, b: GroupNode): number => {
    const emptyA = a.info.value === "";
    const emptyB = b.info.value === "";
    if (emptyA || emptyB) {
      return Number(emptyA) - Number(emptyB);
    }
    return keySort(a.info.value, b.info.value, direction);
  };
  if (firstSort && summaryColumns.some(summary => summary.name === firstSort.id)) {
    const direction = sortOrder(firstSort);
    return (a: GroupNode, b: GroupNode): number =>
      keySort(a.info.aggregates[firstSort.id]?.value, b.info.aggregates[firstSort.id]?.value, direction) || byValue(1)(a, b);
  }
  return byValue(firstSort?.id === column ? sortOrder(firstSort) : 1);
}

/** Build the groups of a level (and of the levels under it) from its data rows */
function buildGroups(rows: readonly GridRow[], levels: readonly GridColumn[], depth: number, parentValues: string[],
                     context: {summaryColumns: readonly GridColumn[]; firstSort?: GroupingSort; expansion: GroupExpansion; t: Translate}): GroupNode[] {
  const column = levels[depth];
  const byValue = new Map<string, {label: string; rows: GridRow[]}>();
  rows.forEach(row => {
    const {value, label} = getGroupKey(row, column, context.t);
    const group = byValue.get(value) ?? {label, rows: []};
    group.rows.push(row);
    byValue.set(value, group);
  });
  const groups = [...byValue.entries()].map(([value, group]): GroupNode => {
    const values = [...parentValues, value];
    const path = getGroupPath(values);
    const isLast = depth === levels.length - 1;
    return {
      info: {
        level: depth + 1, path, column: column.name, value, label: group.label, count: group.rows.length,
        expanded: context.expansion.paths?.[path] ?? context.expansion.all ?? true,
        aggregates: getAggregates(group.rows, context.summaryColumns)
      },
      rows: group.rows,
      children: isLast ? null : buildGroups(group.rows, levels, depth + 1, values, context)
    };
  });
  return groups.sort(groupComparator(column.name, context.firstSort, context.summaryColumns));
}

/** Display rows of some groups: each group row, then, when it is expanded, its content and its subtotal */
function flattenGroups(groups: readonly GroupNode[], subtotals: boolean): DisplayRow[] {
  return groups.flatMap(({info, rows, children}) => {
    const groupRow: GroupRow = {id: `${GROUP_PREFIX}${info.path}`, $group: info};
    if (!info.expanded) {
      return [groupRow];
    }
    const content: DisplayRow[] = children ? flattenGroups(children, subtotals) : rows;
    const subtotal: SubtotalRow[] = subtotals ? [{
      id: `${SUBTOTAL_PREFIX}${info.path}`,
      $subtotal: {level: info.level, path: info.path, label: info.label, aggregates: info.aggregates}
    }] : [];
    return [groupRow, ...content, ...subtotal];
  });
}

/**
 * Build the rows a grouped grid shows: for each group of the first level its group row, then, when it is expanded, its
 * sub-groups (or its data rows on the last level) and its subtotal row. The data rows inside each group follow the
 * sort; the groups of a level go by their value ascending (the empty one last), unless the first sort is on the column
 * of the level (its direction applies) or on a column with summary type (the groups of every level go by that aggregate).
 * @param {object[]} values Data rows of the grid
 * @param {object} options Grouping options
 * @returns {object[]} Display rows: the data rows are the same objects
 */
export function buildGroupedRows(values: readonly GridRow[], options: GroupingOptions): DisplayRow[] {
  const {groupBy, columns = [], sort = [], expansion = {}, subtotals = true, t = (key: string) => key} = options;
  const byName = new Map(columns.map(column => [column.name, column]));
  const levels = getGroupLevels(groupBy, columns).map(name => byName.get(name) as GridColumn);
  if (levels.length === 0) {
    return [...values];
  }
  const sortList = sort.map(item => ({id: item.id, order: sortOrder(item)}));
  const sorted = sortList.length > 0 ? [...values].sort((a, b) => sortRow(a, b, sortList)) : values;
  const summaryColumns = columns.filter(column => !!column.summaryType);
  const groups = buildGroups(sorted, levels, 0, [], {summaryColumns, firstSort: sort[0], expansion, t});
  return flattenGroups(groups, subtotals);
}
