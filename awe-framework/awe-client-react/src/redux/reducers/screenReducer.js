import {CLEAR_ALL_VIEWS, CLEAR_SCREEN_VIEW, SET_SCREEN_VIEW, UPDATE_SCREEN, UPDATE_SCREEN_VIEW, UPDATE_BREADCRUMBS} from '../actions/screen';

const InitialState = {
  view: "base",
  base: {},
  report: {},
  breadcrumbs: []
};

/**
 * Views reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
export function screen(state = InitialState, action = {}) {
  switch (action.type) {
    case UPDATE_SCREEN:
      return {
        ...state,
        ...action.data
      };
    case SET_SCREEN_VIEW:
      return {
        ...state,
        view: action.view,
        [action.view]: action.data
      };
    case UPDATE_SCREEN_VIEW:
      return {
        ...state,
        [action.view]: {
          ...state[action.view],
          ...action.data
        }
      };
    case CLEAR_SCREEN_VIEW:
      return {
        ...state,
        [action.view]: {}
      };
    case CLEAR_ALL_VIEWS:
      return {
        ...state,
        base: {},
        report: {}
      };
    case UPDATE_BREADCRUMBS:
      return {
        ...state,
        breadcrumbs: {
          items: [].concat(action.items),
          option: action.option
        }
      };
    default:
      return state;
  }
}