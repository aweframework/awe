import {UPDATE_SIZE} from '../actions/size';

const InitialState = {};

/**
 * Views reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
export function size(state = InitialState, action = {}) {
  switch (action.type) {
    case UPDATE_SIZE:
      return {
        ...state,
        ...action.data
      };
    default:
      return state;
  }
}