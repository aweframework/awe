import {ActionStatus, type ActionItem} from "../actions/actions";
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

// A server action running or started (waiting for the answer of the server)
const isServerActionRunning = (action: ActionItem): boolean => action.type === "server"
  && (action.status === ActionStatus.STATUS_RUNNING || action.status === ActionStatus.STATUS_STARTED);

/**
 * Actions running flag: true while a sync action that is not silent runs. A dialog runs its actions in a stack of its
 * own, so the flag of its empty stack is false while a sync server action of a stack below still waits for the server
 * (the value dependency of a criterion of the dialog, launched by the row selection of the button that opened it): it
 * also counts, as the AngularJS client queues the actions of that button behind it
 * @param state Redux state
 * @returns Actions are running
 */
export const selectActionsRunning = (state: RootState): ActionsState["running"] => state.actions.running
  || (state.actions.sync ?? []).slice(0, -1).some(stack => stack.some(action =>
    !action.silent && !action.async && isServerActionRunning(action)));

// Server actions of the current stack of sync actions, running or started (waiting for the answer of the server)
const runningServerActions = (state: RootState) => {
  const {sync} = state.actions;
  return (sync[sync.length - 1] ?? []).filter(isServerActionRunning);
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

