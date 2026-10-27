import {ActionStatus} from "../actions/actions";
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

// Server actions of the current stack of sync actions, running or started (waiting for the answer of the server)
const runningServerActions = (state: RootState) => {
  const {sync} = state.actions;
  return (sync[sync.length - 1] ?? []).filter(action => action.type === "server"
    && (action.status === ActionStatus.STATUS_RUNNING || action.status === ActionStatus.STATUS_STARTED));
};

/**
 * Check whether a server action runs: it is in the current stack of sync actions, running or started (waiting for the
 * answer of the server). It ends when the action is accepted (the answer arrived) or the stack is rejected
 * @param state Redux state
 * @param serverAction Server action (login, data...)
 * @returns The server action runs
 */
export const selectServerActionRunning = (state: RootState, serverAction: string): boolean =>
  runningServerActions(state).some(action =>
    (action.parameters as {serverAction?: string} | undefined)?.serverAction === serverAction);

/**
 * Component that launched the server action that runs (as selectServerActionRunning)
 * @param state Redux state
 * @returns Id of the component, or undefined when no server action runs
 */
export const selectServerActionComponent = (state: RootState): string | undefined => runningServerActions(state)
  .map(action => (action.address as {component?: string} | undefined)?.component)
  .find(Boolean);

