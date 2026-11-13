import {UPDATE_SCREEN} from '../actions/screen';
import type {updateScreen} from '../actions/screen';
import type {ScreenState} from '../types';

const InitialState: ScreenState = {};

/** Action the reducer handles */
export type ScreenAction = ReturnType<typeof updateScreen>;

/**
 * Views reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function screen(state: ScreenState = InitialState, action: ScreenAction = {} as ScreenAction): ScreenState {
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