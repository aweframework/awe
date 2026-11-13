import type {NavigationState} from '../types';

const InitialState: NavigationState = {
  currentScreen: null,
};

/** Action the reducer handles: the one the navigateTo thunk dispatches */
export interface NavigationAction {
  type: 'NAVIGATE_TO';
  payload: string | number;
}

// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function navigation(
  state: NavigationState = InitialState,
  action: NavigationAction = {} as NavigationAction
): NavigationState {
  switch (action.type) {
    case 'NAVIGATE_TO':
      return {
        ...state,
        currentScreen: action.payload,
      };
    default:
      return state;
  }
}