/*
 * action types
 */

export const UPDATE_SIZE = 'UPDATE_SIZE' as const;

/*
 * action creators
 */

export function updateSize(data: Record<string, unknown>) {
  return { type: UPDATE_SIZE, data };
}