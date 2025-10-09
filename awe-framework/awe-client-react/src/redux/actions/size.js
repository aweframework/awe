/*
 * action types
 */

export const UPDATE_SIZE = 'UPDATE_SIZE';

/*
 * action creators
 */

export function updateSize(data) {
  return { type: UPDATE_SIZE, data };
}