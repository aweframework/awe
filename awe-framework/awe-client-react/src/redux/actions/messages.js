/*
 * action types
 */

export const ADD_MESSAGE = 'ADD_MESSAGE';
export const SHOW_MESSAGES = 'SHOW_MESSAGES';
export const REMOVE_MESSAGE = 'REMOVE_MESSAGE';
export const UPDATE_MESSAGES = 'UPDATE_MESSAGES';
export const CONFIRM_MESSAGE = 'CONFIRM_MESSAGE';
export const REMOVE_CONFIRM = 'REMOVE_CONFIRM';

export const MessageType = {
  MESSAGE_OK: 'ok',
  MESSAGE_WARNING: 'warning',
  MESSAGE_ERROR: 'error',
  MESSAGE_INFO: 'info',
  MESSAGE_WRONG: 'wrong'
};

/*
 * action creators
 */

export function addMessage(data) {
  return {type: ADD_MESSAGE, data};
}

export function showMessages(data) {
  return {type: SHOW_MESSAGES, data};
}

export function removeMessage(data) {
  return {type: REMOVE_MESSAGE, data};
}

export function updateMessages(view, data) {
  return {type: UPDATE_MESSAGES, view, data};
}

export function confirmMessage(data) {
  return {type: CONFIRM_MESSAGE, data};
}

export function removeConfirm() {
  return {type: REMOVE_CONFIRM};
}