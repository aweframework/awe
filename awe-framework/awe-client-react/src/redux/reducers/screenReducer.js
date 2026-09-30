import {UPDATE_SCREEN} from '../actions/screen';
const InitialState = {};

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
    default:
      return state;
  }
}