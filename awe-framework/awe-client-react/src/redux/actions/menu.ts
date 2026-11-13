import type {MenuOption, MenuState} from "../types";

/*
 * action types
 */

export const UPDATE_MENU = 'UPDATE_MENU' as const;
export const UPDATE_OPTION = 'UPDATE_OPTION' as const;
export const UPDATE_OPTIONS = 'UPDATE_OPTIONS' as const;
export const UPDATE_ALL_OPTIONS = 'UPDATE_ALL_OPTIONS' as const;
export const UPDATE_STATUS = 'UPDATE_STATUS' as const;
export const SELECT_OPTION = 'SELECT_OPTION' as const;
export const CLEAR_MENU = 'CLEAR_MENU' as const;
export const UPDATE_BREADCRUMBS = 'UPDATE_BREADCRUMBS' as const;

/*
 * action creators
 */

export function updateMenu(data: MenuOption[]) {
  return { type: UPDATE_MENU, data };
}

export function updateOption(option: string, data: Partial<MenuOption>) {
  return { type: UPDATE_OPTION, option, data };
}

export function updateOptions(options: string[], data: Partial<MenuOption>) {
  return { type: UPDATE_OPTIONS, options, data };
}

export function updateAllOptions(data: Partial<MenuOption>) {
  return { type: UPDATE_ALL_OPTIONS, data };
}

export function updateStatus(data: Partial<MenuState["status"]>) {
  return { type: UPDATE_STATUS, data };
}

export function selectOption(data: Record<string, unknown>) {
  return { type: SELECT_OPTION, data };
}

export function clearMenu() {
  return { type: CLEAR_MENU };
}

export function updateBreadcrumbs(option: string, items: unknown[]) {
  return { type: UPDATE_BREADCRUMBS, option, items };
}