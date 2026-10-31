import type { ComponentAddress } from "../../contract";
import type { AppThunk } from "../types";
import type { ClientAction } from "./types";
import { type AdvancedStateErrorMessage, isWindowServiceDone, launchWindowService } from "./windowAdvancedState";

/** A saved search of a window, as the query getWindowSavedSearches returns it */
export interface SavedSearch {
  id: string | number;
  name: string;
  // Values of the search (JSON)
  values: string;
  // Profile the search is published for (empty for an own search)
  profile?: string;
  // 1 for an own search of the user, 0 for one published for its profile
  own: number | string | boolean;
}

/** A profile the user may publish a search for */
export interface SavedSearchProfile {
  value: string;
  label: string;
}

/** Search to save */
export interface SavedSearchData {
  name: string;
  values: string;
  // Profile to publish the search for; none for an own search
  profile?: string;
}

/** Rows of the data of a query, null when it failed */
const queryRows = <T>(response: ClientAction[] | null): T[] | null => {
  if (response === null) {
    return null;
  }
  const fill = response.find(action => action.type === "fill");
  const datalist = (fill?.parameters as { datalist?: { rows?: T[] } } | undefined)?.datalist;
  return datalist?.rows || [];
};

/**
 * Load the saved searches of a window (#790): the own ones of the user and the ones published for its profile. The
 * thunk resolves to the searches, or null when they could not be loaded (an error message is shown).
 *
 * @param {object} address Address of the window
 * @param {string} screenId Screen that holds the window
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
export const loadWindowSavedSearches = (
  address: ComponentAddress,
  screenId: string,
  errorMessage?: AdvancedStateErrorMessage
): AppThunk<Promise<SavedSearch[] | null>> => async (dispatch) => queryRows<SavedSearch>(
  await dispatch(launchWindowService("getWindowSavedSearches", address, {
    savedSearchScreen: screenId,
    savedSearchWindow: address.component,
    max: 0
  }, errorMessage, "data")));

/**
 * Load the profiles the user may publish a search for (none when the user may not publish)
 *
 * @param {object} address Address of the window
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
export const loadSavedSearchProfiles = (
  address: ComponentAddress,
  errorMessage?: AdvancedStateErrorMessage
): AppThunk<Promise<SavedSearchProfile[] | null>> => async (dispatch) => queryRows<SavedSearchProfile>(
  await dispatch(launchWindowService("getSavedSearchProfiles", address, { max: 0 }, errorMessage, "data")));

/**
 * Save a search of a window: an own search, or one published for a profile. A search with the same name is replaced.
 * The thunk resolves to true when it was saved.
 *
 * @param {object} address Address of the window
 * @param {string} screenId Screen that holds the window
 * @param {{name: string, values: string, profile: string}} search Search to save
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
export const saveWindowSearch = (
  address: ComponentAddress,
  screenId: string,
  { name, values, profile }: SavedSearchData,
  errorMessage?: AdvancedStateErrorMessage
): AppThunk<Promise<boolean>> => async (dispatch) => isWindowServiceDone(
  await dispatch(launchWindowService("saveWindowSearch", address, {
    savedSearchScreen: screenId,
    savedSearchWindow: address.component,
    savedSearchName: name,
    savedSearchValues: values,
    ...(profile ? { savedSearchProfile: profile } : {})
  }, errorMessage)));

/**
 * Rename a saved search. The thunk resolves to true when it was renamed.
 *
 * @param {object} address Address of the window
 * @param {string|number} id Search id
 * @param {string} name New name
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
export const renameWindowSearch = (
  address: ComponentAddress,
  id: SavedSearch["id"],
  name: string,
  errorMessage?: AdvancedStateErrorMessage
): AppThunk<Promise<boolean>> => async (dispatch) => isWindowServiceDone(
  await dispatch(launchWindowService("renameWindowSearch", address, {
    savedSearchId: id,
    savedSearchName: name
  }, errorMessage)));

/**
 * Delete a saved search. The thunk resolves to true when it was deleted.
 *
 * @param {object} address Address of the window
 * @param {string|number} id Search id
 * @param {{title: string, message: string}} [errorMessage] Message shown when the call fails
 */
export const deleteWindowSearch = (
  address: ComponentAddress,
  id: SavedSearch["id"],
  errorMessage?: AdvancedStateErrorMessage
): AppThunk<Promise<boolean>> => async (dispatch) => isWindowServiceDone(
  await dispatch(launchWindowService("deleteWindowSearch", address, { savedSearchId: id }, errorMessage)));
