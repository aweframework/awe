/*
 * action types
 */

export const UPDATE_SCREEN = 'UPDATE_SCREEN';

/*
 * action creators
 */

export function updateScreen(data) {
  return { type: UPDATE_SCREEN, data };
}

