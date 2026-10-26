export const SET_VIEW = 'SET_SCREEN_VIEW' as const;
export const UPDATE_VIEW = 'UPDATE_SCREEN_VIEW' as const;
export const CLEAR_VIEW = 'CLEAR_SCREEN_VIEW' as const;
export const CLEAR_ALL_VIEWS = 'CLEAR_ALL_VIEWS' as const;

/*
 * action creators
 */

export function setView(view: string, data?: Record<string, unknown>) {
  return { type: SET_VIEW, view, data };
}

export function updateView(view: string, data?: Record<string, unknown>) {
  return { type: UPDATE_VIEW, view, data };
}

export function clearView(view: string) {
  return { type: CLEAR_VIEW, view };
}

export function clearAllViews() {
  return { type: CLEAR_ALL_VIEWS };
}