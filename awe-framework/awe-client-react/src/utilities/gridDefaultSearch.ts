import type { GridDefaultSearch } from "../contract";
import type { ColumnFilter } from "./columnFilters";
import { getGridSearchRestore, type GridSearchState, type SavedSearchColumn } from "./gridSavedSearches";

/**
 * Default saved search of a grid (#945): the server applies it before the initial load of the grid, so the rows come
 * already filtered and sorted, and its quick filter and search already hold its values. The client only seeds the state
 * the grid keeps on its own (its column filters with their labels and its sort) and shows the chip of the search.
 */

/** Attributes of a grid that keeps saved searches, as the store holds them */
export interface SavedSearchesGridAttributes {
  savedSearches?: boolean;
  // Default search the server applied on load
  defaultSearch?: GridDefaultSearch;
  // The default search is the one applied (its chip is shown): true on load when the server applied one
  defaultSearchApplied?: boolean;
  columnModel?: SavedSearchColumn[];
}

/** Sort of a grid as its state holds it */
export interface GridSortState {
  id: string;
  direction: "asc" | "desc";
  order: number;
}

/** State of a grid seeded from its default search */
export interface GridDefaultSearchSeed {
  attributes: { columnFilterLabels: Record<string, Record<string, string>> };
  specificAttributes: { columnFilters: ColumnFilter[]; sort: GridSortState[] };
}

/**
 * Default search of a grid that keeps saved searches, null when the server applied none
 * @param {object} attributes Attributes of the grid
 * @return {object|null} Default search
 */
export const getGridDefaultSearch = (attributes?: SavedSearchesGridAttributes): GridDefaultSearch | null =>
  attributes?.savedSearches === true && attributes.defaultSearch ? attributes.defaultSearch : null;

/**
 * Check whether the default search of a grid is the search applied
 * @param {object} attributes Attributes of the grid
 * @return {boolean} The default search is applied
 */
export const isGridDefaultSearchApplied = (attributes?: SavedSearchesGridAttributes): boolean =>
  Boolean(getGridDefaultSearch(attributes)) && attributes?.defaultSearchApplied !== false;

/**
 * Sort of a saved search as the grid state holds it
 * @param {object[]} sort Sort of a saved search
 * @return {object[]} Sort with its order
 */
export const toGridSort = (sort: GridSearchState["sort"]): GridSortState[] =>
  sort.map(item => ({ ...item, order: item.direction === "asc" ? 1 : -1 }));

/**
 * State of a grid seeded from the default search the server applied on load: its column filters with their labels and
 * its sort, on the columns the grid has now
 * @param {object} controller Controller of the grid
 * @return {object|null} State to seed, null without a default search
 */
export function getGridDefaultSearchSeed(controller: SavedSearchesGridAttributes): GridDefaultSearchSeed | null {
  const defaultSearch = getGridDefaultSearch(controller);
  if (!defaultSearch) {
    return null;
  }
  const { columnFilters, columnFilterLabels, sort } = getGridSearchRestore(defaultSearch as Partial<GridSearchState>,
    controller.columnModel || []);
  return {
    attributes: { columnFilterLabels },
    specificAttributes: { columnFilters, sort: toGridSort(sort) }
  };
}
