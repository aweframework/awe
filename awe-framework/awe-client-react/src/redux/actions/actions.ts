export const ADD_ACTION = 'ADD_ACTION' as const;
export const REMOVE_ACTION = 'REMOVE_ACTION' as const;
export const ADD_ACTIONS = 'ADD_ACTIONS' as const;
export const ADD_ACTIONS_TOP = 'ADD_ACTIONS_TOP' as const;
export const ADD_STACK = 'ADD_STACK' as const;
export const REMOVE_STACK = 'REMOVE_STACK' as const;
export const DELETE_STACK = 'DELETE_STACK' as const;
export const RUN_ACTION = 'RUN_ACTION' as const;
export const START_ACTION = 'START_ACTION' as const;
export const ACCEPT_ACTION = 'ACCEPT_ACTION' as const;
export const REJECT_ACTION = 'REJECT_ACTION' as const;
export const ABORT_ACTION = 'ABORT_ACTION' as const;
export const CLOSE_ALL_ACTIONS = 'CLOSE_ALL_ACTIONS' as const;
export const TOGGLE_ACTIONS_RUNNING = 'TOGGLE_ACTIONS_RUNNING' as const;

/**
 * Delay actions to show actions stack
 * @param time Time to delay
 * @param fn Function to launch
 */
export function delayActions(time: number | null | undefined, fn: () => void): void {
  if (time) {
    setTimeout(fn, time);
  } else {
    fn();
  }
}

/*
 * action status
 */

export const ActionStatus = {
  STATUS_INITIAL: 'STATUS_INITIAL',
  STATUS_RUNNING: 'STATUS_RUNNING',
  STATUS_STARTED: 'STATUS_STARTED',
  STATUS_ACCEPTED: 'STATUS_ACCEPTED',
  STATUS_REJECTED: 'STATUS_REJECTED',
  STATUS_ABORTED: 'STATUS_ABORTED'
} as const;

export type ActionStatusValue = typeof ActionStatus[keyof typeof ActionStatus];

/** An action of the stacks of the actions reducer; the reducer only reads `id`, `async` and `status`, the rest is the action itself */
export interface ActionItem {
  id?: string;
  async?: boolean;
  status?: ActionStatusValue;
  [key: string]: unknown;
}

/*
 * action creators
 */
export function addAction(payload: ActionItem) {
  return { type: ADD_ACTION, payload };
}

export function removeAction(payload: ActionItem) {
  return { type: REMOVE_ACTION, payload };
}

export function addActions(payload: ActionItem[]) {
  return { type: ADD_ACTIONS, payload };
}

export function addActionsTop(payload: ActionItem[]) {
  return { type: ADD_ACTIONS_TOP, actions: payload, payload };
}

export function addStack() {
  return { type: ADD_STACK };
}

export function removeStack() {
  return { type: REMOVE_STACK };
}

export function deleteStack() {
  return { type: DELETE_STACK };
}

export function startAction(payload: ActionItem) {
  return { type: START_ACTION, payload };
}

export function runAction(payload: ActionItem) {
  return { type: RUN_ACTION, payload };
}

export function acceptAction(payload: ActionItem) {
  return { type: ACCEPT_ACTION, payload };
}

export function rejectAction(payload: ActionItem) {
  return { type: REJECT_ACTION, payload };
}

export function abortAction(payload: ActionItem) {
  return { type: ABORT_ACTION, payload };
}

export function closeAllActions() {
  return { type: CLOSE_ALL_ACTIONS };
}

export function toggleActionsRunning(running: boolean) {
  return { type: TOGGLE_ACTIONS_RUNNING, running: running };
}
