/**
 * Shared selectors of whole state slices. A selector must return a value that is stable while its slice does not change
 * (the slice itself, never a new object literal), so the components that use it only render again when the slice changes.
 */

/**
 * Settings slice
 * @param {object} state Redux state
 * @returns {object} Settings
 */
export const selectSettings = (state) => state.settings;

/**
 * Messages slice
 * @param {object} state Redux state
 * @returns {object} Messages
 */
export const selectMessages = (state) => state.messages;

/**
 * Actions running flag: true while a sync action that is not silent runs
 * @param {object} state Redux state
 * @returns {boolean} Actions are running
 */
export const selectActionsRunning = (state) => state.actions.running;
