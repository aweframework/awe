import { getColumnFilterType, getFilterFields } from "./columnFilters";
import type { ColumnFilter, FilterableColumn } from "./columnFilters";

/**
 * Saved searches of a grid (#912, S20-S21): the state of the grid a search keeps (its column filters with the labels of
 * their values, its sort, the value of its quick filter and the text of its search), read from the grid and restored on
 * it. Shared by the React and the AngularJS clients.
 */

/** Sort of a grid column */
export interface GridSearchSort {
  id: string;
  direction: "asc" | "desc";
}

/** State of a grid a saved search keeps */
export interface GridSearchState {
  columnFilters: ColumnFilter[];
  // Label of each value key of the filters of values, by column (the chips show them)
  columnFilterLabels: Record<string, Record<string, string>>;
  sort: GridSearchSort[];
  // Value of the quick filter, only when the grid has one
  quickFilter?: unknown;
  // Text of the search, only when the grid has one
  search?: string;
}

/** A column of the column model, as the saved searches read it */
export interface SavedSearchColumn extends FilterableColumn {
  sortable?: boolean;
}

/** What a grid holds that a saved search reads */
export interface GridSearchSource {
  columnFilters?: ColumnFilter[] | null;
  columnFilterLabels?: Record<string, Record<string, string>> | null;
  sort?: { id: string; direction?: string }[] | null;
  // Whether the grid has a quick filter and its value
  hasQuickFilter?: boolean;
  quickFilter?: unknown;
  // Whether the grid has a search and its text
  hasSearch?: boolean;
  search?: string | null;
}

const isObject = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === "object" && !Array.isArray(value);

const toDirection = (direction: unknown): "asc" | "desc" | null => {
  const text = String(direction ?? "").toLowerCase();
  return text === "asc" || text === "desc" ? text : null;
};

/**
 * Labels of the filters of values of some columns
 * @param {object} labels Labels by column
 * @param {object[]} columnFilters Column filters whose labels are kept
 * @returns {object} Labels of the columns of the filters
 */
const keepLabels = (labels: unknown, columnFilters: readonly ColumnFilter[]): Record<string, Record<string, string>> => {
  const source = isObject(labels) ? labels : {};
  return Object.fromEntries(columnFilters
    .filter(filter => isObject(source[filter.id]))
    .map(filter => [filter.id, Object.fromEntries(Object.entries(source[filter.id] as Record<string, unknown>)
      .filter((entry): entry is [string, string] => typeof entry[1] === "string"))]));
};

/**
 * State of a grid a saved search keeps: its column filters with their labels, its sort, and the value of its quick filter
 * and the text of its search when it has them
 * @param {object} source What the grid holds
 * @returns {object} State to save
 */
export function getGridSearchState(source: GridSearchSource): GridSearchState {
  const columnFilters = [...(source.columnFilters || [])];
  return {
    columnFilters,
    columnFilterLabels: keepLabels(source.columnFilterLabels, columnFilters),
    sort: (source.sort || [])
      .map(item => ({ id: item.id, direction: toDirection(item.direction) }))
      .filter((item): item is GridSearchSort => Boolean(item.id) && item.direction !== null),
    ...(source.hasQuickFilter ? { quickFilter: source.quickFilter ?? null } : {}),
    ...(source.hasSearch ? { search: source.search ?? "" } : {})
  };
}

/**
 * Read the values of a saved search of a grid
 * @param {string|object} json Values (JSON)
 * @returns {object} Values, empty when they are not an object
 */
export function parseGridSearchValues(json: unknown): Partial<GridSearchState> {
  try {
    const values = typeof json === "string" ? JSON.parse(json) : json;
    return isObject(values) ? values as Partial<GridSearchState> : {};
  } catch {
    return {};
  }
}

/**
 * State of a grid to restore from a saved search, on the columns the grid has now: the filters of columns that no longer
 * declare that filter and the sorts of columns that can no longer be sorted are dropped, and each filter takes the fields
 * its column filters now. The quick filter and the search are restored only when the grid has them (to their default
 * when the search does not keep them)
 * @param {object} values Values of the saved search
 * @param {object[]} columns Column model of the grid
 * @param {{hasQuickFilter: boolean, hasSearch: boolean}} grid What the grid has
 * @returns {object} State to restore
 */
export function getGridSearchRestore(values: Partial<GridSearchState>, columns: readonly SavedSearchColumn[],
                                     { hasQuickFilter = false, hasSearch = false } = {}): GridSearchState {
  const byName = new Map((columns || []).map(column => [column.name, column]));
  const columnFilters = (Array.isArray(values.columnFilters) ? values.columnFilters : [])
    .filter(filter => isObject(filter) && byName.has(filter.id)
      && getColumnFilterType(byName.get(filter.id)) === filter.type)
    .map(filter => ({ ...filter, fields: getFilterFields(byName.get(filter.id) as SavedSearchColumn) }));
  const sortable = (columns || []).filter(column => column.sortable !== false);
  const sort = (Array.isArray(values.sort) ? values.sort : [])
    .filter(item => isObject(item) && sortable.some(column => item.id === column.name || item.id === column.sortField))
    .map(item => ({ id: item.id, direction: toDirection(item.direction) }))
    .filter((item): item is GridSearchSort => item.direction !== null);
  return {
    columnFilters,
    columnFilterLabels: keepLabels(values.columnFilterLabels, columnFilters),
    sort,
    ...(hasQuickFilter ? { quickFilter: values.quickFilter ?? null } : {}),
    ...(hasSearch ? { search: typeof values.search === "string" ? values.search : "" } : {})
  };
}

/**
 * Value with its object keys sorted, so two values with the same content compare equal
 * @param {*} value Value
 * @returns {*} Value with sorted keys
 */
const sortKeys = (value: unknown): unknown => {
  if (Array.isArray(value)) {
    return value.map(sortKeys);
  }
  if (isObject(value)) {
    return Object.fromEntries(Object.keys(value).sort((first, second) => first.localeCompare(second)).map(key => [key, sortKeys(value[key])]));
  }
  return value;
};

/**
 * What decides the rows of a grid state: its filters, its sort, its quick filter and its search (not the labels)
 * @param {object} state Grid state
 * @returns {string} Comparable text
 */
const comparable = (state: Partial<GridSearchState>): string => JSON.stringify(sortKeys({
  // The fields come from the column, and a filter with conditions joins them with AND unless it says otherwise
  columnFilters: (state.columnFilters || []).map(filter => ({
    ...Object.fromEntries(Object.entries(filter).filter(([key]) => key !== "fields")),
    ...(filter.conditions?.length ? { union: filter.union || "and" } : {})
  })),
  sort: (state.sort || []).map(item => ({ id: item.id, direction: toDirection(item.direction) })),
  quickFilter: state.quickFilter === null || state.quickFilter === undefined ? "" : String(state.quickFilter),
  search: state.search || ""
}));

/**
 * Check whether the state of a grid is still the one of a saved search: same filters, sort, quick filter and search
 * @param {object} current State of the grid
 * @param {object} saved Values of the saved search
 * @returns {boolean} The grid shows the saved search
 */
export function isGridSearchInEffect(current: Partial<GridSearchState>, saved: Partial<GridSearchState>): boolean {
  return comparable(current) === comparable(saved);
}
