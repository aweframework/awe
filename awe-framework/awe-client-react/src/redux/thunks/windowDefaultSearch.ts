import type { ComponentAddress, DefaultSearch } from "../../contract";
import { getSavedModelValues, parseSearchValues, type SavedValues } from "../../utilities/advancedSearch";
import type { StateComponent } from "../reducers/components/stateHelpers";
import { updateAttributes } from "../actions/components";
import { getAllComponents } from "../selectors/componentSelectors";
import type { AppThunk } from "../types";
import { restoreMultipleModelWithDependencies, updateMultipleModelsWithDependencies } from "./components";

/** Attributes of a window that keeps saved searches (#790, #915), as the store holds them */
export interface SavedSearchesWindowAttributes {
  savedSearches?: boolean;
  // Default search the server applied on load: the target of the restores of the window for its whole life
  defaultSearch?: DefaultSearch;
  // Advanced criteria shown with the essential ones (the layout of the search in effect): the layout of the default
  // search until a search or a restore sets it
  searchLayout?: string[];
  // The default search is the one applied (its chip is shown): true on load when the server applied one
  defaultSearchApplied?: boolean;
}

/**
 * Default search of a window that keeps saved searches, null when the server applied none
 * @param {object} attributes Attributes of the window
 * @return {object|null} Default search
 */
export const getWindowDefaultSearch = (attributes?: SavedSearchesWindowAttributes): DefaultSearch | null =>
  attributes?.savedSearches === true && attributes.defaultSearch ? attributes.defaultSearch : null;

/**
 * Advanced criteria a window shows with the essential ones: the layout of the search in effect
 * @param {object} attributes Attributes of the window
 * @return {string[]} Criteria ids (not checked against the window)
 */
export const getWindowSearchLayout = (attributes?: SavedSearchesWindowAttributes): string[] =>
  attributes?.searchLayout ?? getWindowDefaultSearch(attributes)?.layout ?? [];

/**
 * Check whether the default search of a window is the search applied
 * @param {object} attributes Attributes of the window
 * @return {boolean} The default search is applied
 */
export const isDefaultSearchApplied = (attributes?: SavedSearchesWindowAttributes): boolean =>
  Boolean(getWindowDefaultSearch(attributes)) && attributes?.defaultSearchApplied !== false;

/**
 * Set the values of a saved search on criteria of a window: each criterion goes back to its default first (unless they
 * were just restored), then the ones the search carries take its values. Criteria the screen no longer has are ignored.
 * @param {string[]} criteria Criteria ids
 * @param {object} values Values of the search by criterion id
 * @param {boolean} [restore] Restore the criteria to their defaults first
 */
export const applyWindowSearchValues = (criteria: string[], values: SavedValues, restore = true): AppThunk =>
  (dispatch, getState) => {
    if (restore) {
      const components = getAllComponents(getState()) as Record<string, StateComponent | undefined>;
      const list = criteria.map(id => components[id]).filter(component => component?.address);
      if (list.length) {
        dispatch(restoreMultipleModelWithDependencies(list.map(component => ({ address: component!.address! })), false));
      }
    }
    const restored = getAllComponents(getState()) as Record<string, StateComponent | undefined>;
    const updates = criteria
      .filter(id => id in values && restored[id]?.address)
      .map(id => ({ address: restored[id]!.address!, data: { values: getSavedModelValues(restored[id]!, values[id]) } }));
    if (updates.length) {
      dispatch(updateMultipleModelsWithDependencies(updates));
    }
  };

/**
 * Set the layout of the search in effect of a window and whether it is its default search
 * @param {object} address Address of the window
 * @param {string[]} layout Advanced criteria shown with the essential ones
 * @param {boolean} defaultApplied The search is the default search of the window
 */
export const setWindowSearchLayout = (address: ComponentAddress, layout: string[], defaultApplied: boolean): AppThunk =>
  (dispatch) => {
    dispatch(updateAttributes(address, { searchLayout: layout, defaultSearchApplied: defaultApplied }));
  };

/**
 * Bring the restored criteria of the windows of a view that have a default search back to it (#915, D7): its values
 * (unless the restore went back to the first loaded values, which already are its values) and its layout, and show its
 * chip again. Windows without a default search keep the values of their definition.
 * @param {string} view View of the restore
 * @param {string[]} restoredIds Restored components
 * @param {boolean} initial The restore went back to the first loaded values
 */
export const restoreWindowDefaultSearches = (view: string | undefined, restoredIds: string[], initial: boolean): AppThunk =>
  (dispatch, getState) => {
    const components = getAllComponents(getState()) as Record<string, StateComponent | undefined>;
    Object.values(components)
      .filter(component => component?.address?.view === view &&
        getWindowDefaultSearch(component?.attributes as SavedSearchesWindowAttributes))
      .forEach(window => {
        const windowId = window!.address!.component as string;
        const restored = restoredIds.filter(id => id !== windowId && components[id]?.context?.source?.includes(windowId));
        if (!restored.length) {
          return;
        }
        const defaultSearch = getWindowDefaultSearch(window!.attributes as SavedSearchesWindowAttributes)!;
        if (!initial) {
          dispatch(applyWindowSearchValues(restored, parseSearchValues(defaultSearch.values), false));
        }
        dispatch(setWindowSearchLayout(window!.address!, defaultSearch.layout || [], true));
      });
  };
