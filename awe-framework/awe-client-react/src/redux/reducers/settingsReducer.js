import {DEFAULT_SETTINGS, UPDATE_SETTINGS} from '../actions/settings';
const InitialState = DEFAULT_SETTINGS;
/**
 * Settings reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
export function settings(state = InitialState, action = {}) {
  switch (action.type) {
    case UPDATE_SETTINGS:
      return {
        ...state,
        ...action.payload
      };
    default:
      return state;
  }
}