import type {ActionsState, MessagesState, RootState, SettingsState} from "../types";

/**
 * Shared selectors of whole state slices. A selector must return a value that is stable while its slice does not change
 * (the slice itself, never a new object literal), so the components that use it only render again when the slice changes.
 */

/**
 * Settings slice
 * @param state Redux state
 * @returns Settings
 */
export const selectSettings = (state: RootState): SettingsState => state.settings;

/**
 * Messages slice
 * @param state Redux state
 * @returns Messages
 */
export const selectMessages = (state: RootState): MessagesState => state.messages;

/**
 * Actions running flag: true while a sync action that is not silent runs
 * @param state Redux state
 * @returns Actions are running
 */
export const selectActionsRunning = (state: RootState): ActionsState["running"] => state.actions.running;
