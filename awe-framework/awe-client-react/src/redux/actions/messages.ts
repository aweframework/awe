import type {MessageItem} from "../types";

/*
 * action types
 */

export const ADD_MESSAGE = 'ADD_MESSAGE' as const;
export const SHOW_MESSAGES = 'SHOW_MESSAGES' as const;
export const REMOVE_MESSAGE = 'REMOVE_MESSAGE' as const;
export const UPDATE_MESSAGES = 'UPDATE_MESSAGES' as const;
export const CONFIRM_MESSAGE = 'CONFIRM_MESSAGE' as const;
export const REMOVE_CONFIRM = 'REMOVE_CONFIRM' as const;
export const SHOW_POPOVER = 'SHOW_POPOVER' as const;
export const REMOVE_POPOVER = 'REMOVE_POPOVER' as const;
export const SHOW_SIGNIN_ALERT = 'SHOW_SIGNIN_ALERT' as const;
export const REMOVE_SIGNIN_ALERT = 'REMOVE_SIGNIN_ALERT' as const;

export const MessageType = {
  MESSAGE_OK: 'ok',
  MESSAGE_WARNING: 'warning',
  MESSAGE_ERROR: 'error',
  MESSAGE_INFO: 'info',
  MESSAGE_WRONG: 'wrong'
} as const;

/*
 * action creators
 */

export function addMessage(data: MessageItem) {
  return {type: ADD_MESSAGE, data};
}

export function showMessages(data: MessageItem[]) {
  return {type: SHOW_MESSAGES, data};
}

export function removeMessage(data: MessageItem) {
  return {type: REMOVE_MESSAGE, data};
}

export function updateMessages(view: string, data: Record<string, unknown>) {
  return {type: UPDATE_MESSAGES, view, data};
}

export function confirmMessage(data: MessageItem) {
  return {type: CONFIRM_MESSAGE, data};
}

export function removeConfirm() {
  return {type: REMOVE_CONFIRM};
}

export function showPopover(data: MessageItem) {
  return {type: SHOW_POPOVER, data};
}

export function removePopover(data: MessageItem) {
  return {type: REMOVE_POPOVER, data};
}

/** Show the message of a failed login in the alert of the sign-in page, instead of a toast */
export function showSigninAlert(data: MessageItem) {
  return {type: SHOW_SIGNIN_ALERT, data};
}

export function removeSigninAlert() {
  return {type: REMOVE_SIGNIN_ALERT};
}
