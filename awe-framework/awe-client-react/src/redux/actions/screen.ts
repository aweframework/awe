/*
 * action types
 */

export const UPDATE_SCREEN = 'UPDATE_SCREEN' as const;

/*
 * action creators
 */

export function updateScreen(data: Record<string, unknown>) {
  return { type: UPDATE_SCREEN, data };
}

