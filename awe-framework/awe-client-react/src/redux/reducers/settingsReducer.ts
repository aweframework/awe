import {DEFAULT_SETTINGS, UPDATE_SETTINGS} from '../actions/settings';
import type {updateSettings} from '../actions/settings';
import type {SettingsState} from '../types';

const InitialState: SettingsState = DEFAULT_SETTINGS;

/** Action the reducer handles */
export type SettingsAction = ReturnType<typeof updateSettings>;


/**
 * Settings reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function settings(
  state: SettingsState = InitialState,
  action: SettingsAction = {} as SettingsAction
): SettingsState {
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