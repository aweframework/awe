/*
 * action types
 */

export const UPDATE_SCREEN = 'UPDATE_SCREEN';
export const SET_SCREEN_VIEW = 'SET_SCREEN_VIEW';
export const UPDATE_SCREEN_VIEW = 'UPDATE_SCREEN_VIEW';
export const CLEAR_SCREEN_VIEW = 'CLEAR_SCREEN_VIEW';
export const CLEAR_ALL_VIEWS = 'CLEAR_ALL_VIEWS';
export const UPDATE_BREADCRUMBS = 'UPDATE_BREADCRUMBS';

/*
 * action creators
 */

export function updateScreen(data) {
  return { type: UPDATE_SCREEN, data };
}

export function setScreenView(view, data) {
  return { type: SET_SCREEN_VIEW, view, data };
}

export function updateScreenView(view, data) {
  return { type: UPDATE_SCREEN_VIEW, view, data };
}

export function clearScreenView(view) {
  return { type: CLEAR_SCREEN_VIEW, view };
}

export function clearAllViews() {
  return { type: CLEAR_ALL_VIEWS };
}

export function updateBreadcrumbs(option, items) {
  return { type: UPDATE_BREADCRUMBS, option, items };
}