import {UPDATE_SIZE} from '../actions/size';
import type {updateSize} from '../actions/size';
import type {SizeState} from '../types';

const InitialState: SizeState = {};

/** Action the reducer handles */
export type SizeAction = ReturnType<typeof updateSize>;

/**
 * Views reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function size(state: SizeState = InitialState, action: SizeAction = {} as SizeAction): SizeState {
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