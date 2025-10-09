export const ADD_ACTION = 'ADD_ACTION';
export const REMOVE_ACTION = 'REMOVE_ACTION';
export const ADD_ACTIONS = 'ADD_ACTIONS';
export const ADD_ACTIONS_TOP = 'ADD_ACTIONS_TOP';
export const ADD_STACK = 'ADD_STACK';
export const REMOVE_STACK = 'REMOVE_STACK';
export const DELETE_STACK = 'DELETE_STACK';
export const RUN_ACTION = 'RUN_ACTION';
export const START_ACTION = 'START_ACTION';
export const ACCEPT_ACTION = 'ACCEPT_ACTION';
export const REJECT_ACTION = 'REJECT_ACTION';
export const ABORT_ACTION = 'ABORT_ACTION';
export const CLOSE_ALL_ACTIONS = 'CLOSE_ALL_ACTIONS';
export const TOGGLE_ACTIONS_RUNNING = 'TOGGLE_ACTIONS_RUNNING';

/**
 * Delay actions to show actions stack
 * @param time Time to delay
 * @param fn Function to launch
 */
export function delayActions(time, fn) {
  time ? setTimeout(fn, time) : fn();
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
}

/*
 * action creators
 */
export function addAction(payload) {
  return{type: ADD_ACTION, payload};
}

export function removeAction(payload) {
  return {type: REMOVE_ACTION, payload};
}

export function addActions(payload) {
  return {type: ADD_ACTIONS, payload};
}

export function addActionsTop(payload) {
  return {type: ADD_ACTIONS_TOP, payload};
}

export function addStack() {
  return {type: ADD_STACK};
}

export function removeStack() {
  return {type: REMOVE_STACK};
}

export function deleteStack() {
  return {type: DELETE_STACK};
}

export function startAction(payload) {
  return {type: START_ACTION, payload};
}

export function runAction(payload) {
  return {type: RUN_ACTION, payload};
}

export function acceptAction(payload) {
  return {type: ACCEPT_ACTION, payload};
}

export function rejectAction(payload) {
  return {type: REJECT_ACTION, payload};
}

export function abortAction(payload) {
  return {type: ABORT_ACTION, payload};
}

export function closeAllActions() {
  return {type: CLOSE_ALL_ACTIONS};
}

export function toggleActionsRunning(running) {
  return {type: TOGGLE_ACTIONS_RUNNING, running: running};
}
