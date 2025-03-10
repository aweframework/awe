/*
 * action types
 */

export const UPDATE_MENU = 'UPDATE_MENU';
export const UPDATE_OPTION = 'UPDATE_OPTION';
export const UPDATE_OPTIONS = 'UPDATE_OPTIONS';
export const UPDATE_ALL_OPTIONS = 'UPDATE_ALL_OPTIONS';
export const UPDATE_STATUS = 'UPDATE_STATUS';
export const SELECT_OPTION = 'SELECT_OPTION';
export const CLEAR_MENU = 'CLEAR_MENU';

/*
 * action creators
 */

export function updateMenu(data) {
  return { type: UPDATE_MENU, data }
}

export function updateOption(option, data) {
  return { type: UPDATE_OPTION, option, data }
}

export function updateOptions(options, data) {
  return { type: UPDATE_OPTIONS, options, data }
}

export function updateAllOptions(data) {
  return { type: UPDATE_ALL_OPTIONS, data }
}

export function updateStatus(data) {
  return { type: UPDATE_STATUS, data }
}

export function selectOption(data) {
  return { type: SELECT_OPTION, data }
}

export function clearMenu() {
  return { type: CLEAR_MENU }
}
