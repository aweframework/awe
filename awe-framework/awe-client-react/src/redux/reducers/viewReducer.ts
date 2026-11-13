import {CLEAR_VIEW, SET_VIEW, UPDATE_VIEW, CLEAR_ALL_VIEWS} from "../actions/view";
import type {clearAllViews, clearView, setView, updateView} from "../actions/view";
import type {ViewData, ViewState} from "../types";

const InitialState: ViewState = {
  base: {loading: true},
  report: {loading: true},
};

/** Actions the reducer handles */
export type ViewAction = ReturnType<typeof setView | typeof updateView | typeof clearView | typeof clearAllViews>;

/**
 * Views reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function view(state: ViewState = InitialState, action: ViewAction = {} as ViewAction): ViewState {
  switch (action.type) {
    case SET_VIEW:
      return {
        ...state,
        view: action.view,
        [action.view]: action.data
      };
    case UPDATE_VIEW:
      return {
        ...state,
        [action.view]: {
          ...(state[action.view] as ViewData | undefined),
          ...action.data
        }
      };
    case CLEAR_VIEW:
      return {
        ...state,
        [action.view]: {loading: true}
      };
    case CLEAR_ALL_VIEWS:
      return {
        ...state,
        base: {loading: true},
        report: {loading: true}
      };
    default:
      return state;
  }
}