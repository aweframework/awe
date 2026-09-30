export const SET_VIEW = 'SET_SCREEN_VIEW';
export const UPDATE_VIEW = 'UPDATE_SCREEN_VIEW';
export const CLEAR_VIEW = 'CLEAR_SCREEN_VIEW';
export const CLEAR_ALL_VIEWS = 'CLEAR_ALL_VIEWS';

/*
 * action creators
 */

export function setView(view, data) {
  return { type: SET_VIEW, view, data };
}

export function updateView(view, data) {
  return { type: UPDATE_VIEW, view, data };
}

export function clearView(view) {
  return { type: CLEAR_VIEW, view };
}

export function clearAllViews() {
  return { type: CLEAR_ALL_VIEWS };
}