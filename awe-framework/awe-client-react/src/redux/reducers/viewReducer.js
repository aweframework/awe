import {CLEAR_VIEW, SET_VIEW, UPDATE_VIEW, CLEAR_ALL_VIEWS} from "../actions/view";

const InitialState = {
  base: {loading: true},
  report: {loading: true},
};

/**
 * Views reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
export function view(state = InitialState, action = {}) {
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
          ...state[action.view],
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