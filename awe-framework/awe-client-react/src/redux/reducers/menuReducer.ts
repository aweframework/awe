import {
  CLEAR_MENU,
  SELECT_OPTION,
  UPDATE_ALL_OPTIONS, UPDATE_BREADCRUMBS,
  UPDATE_MENU,
  UPDATE_OPTION,
  UPDATE_OPTIONS,
  UPDATE_STATUS
} from '../actions/menu';
import type {
  clearMenu,
  selectOption,
  updateAllOptions as updateAllOptionsCreator,
  updateBreadcrumbs,
  updateMenu,
  updateOption as updateOptionCreator,
  updateOptions as updateOptionsCreator,
  updateStatus
} from '../actions/menu';
import type {MenuOption, MenuState} from '../types';

const InitialState: MenuState = {
  options: [],
  breadcrumbs: {},
  status: { minimized: false, animating: false, resolution: "desktop" }
};
/**
 * Check if option is valid or not
 */
function isValid(option: MenuOption): boolean | undefined {
  return option.visible && !option.restricted;
}

/**
 * Filter option tree
 * @param {Object} options Option tree
 * @return {Object} Option array
 */
function filterOptions(options: MenuOption[] = []): MenuOption[] {
  return options.length === 0 ? [] : options.map((option) => ({
    ...option,
    allowed: isValid(option),
    options: filterOptions(option.options)
  }));
}

/**
 * Update some options
 * @param {Object} options Option tree
 * @param optionIds Option ids
 * @param data Option data
 * @return {Object} Option array
 */
function updateOptions(
  options: MenuOption[] = [],
  optionIds: (string | undefined)[] = [],
  data: Partial<MenuOption> = {}
): MenuOption[] {
  return options.length === 0 ? [] : options.map((option) => (optionIds.includes(option.name) ?
    {...option, ...data, options: updateOptions(option.options, optionIds, data)} :
    {...option, options: updateOptions(option.options, optionIds, data)}));
}

/**
 * Update all options
 * @param {Object} options Option tree
 * @param {Object} data Option data
 * @return {Object} Option array
 */
function updateAllOptions(options: MenuOption[] = [], data: Partial<MenuOption> = {}): MenuOption[] {
  return options.length === 0 ? [] : options.map((option) => ({
    ...option,
    ...data,
    options: updateAllOptions(option.options, data)
  }));
}

/** Actions the reducer handles */
export type MenuAction = ReturnType<
  typeof updateMenu | typeof updateOptionCreator | typeof updateOptionsCreator | typeof updateAllOptionsCreator |
  typeof updateStatus | typeof selectOption | typeof clearMenu | typeof updateBreadcrumbs>;

/**
 * Menu reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function menu(state: MenuState = InitialState, action: MenuAction = {} as MenuAction): MenuState {
  switch (action.type) {
    case UPDATE_MENU:
      return {
        ...state,
        selected: {},
        options: filterOptions(action.data)
      };
    case UPDATE_OPTION:
      return {
        ...state,
        options: updateOptions(state.options, [action.option], action.data)
      };
    case UPDATE_OPTIONS:
      return {
        ...state,
        options: updateOptions(state.options, action.options, action.data)
      };
    case UPDATE_ALL_OPTIONS:
      return {
        ...state,
        options: updateAllOptions(state.options, action.data)
      };
    case UPDATE_STATUS:
      return {
        ...state,
        status: {
          ...state.status,
          ...action.data
        }
      };
    case SELECT_OPTION:
      return {
        ...state,
        selected: action.data
      };
    case CLEAR_MENU:
      return {
        ...state,
        options: [],
        selected: {}
      };
    case UPDATE_BREADCRUMBS:
      return {
        ...state,
        breadcrumbs: {
          items: [...action.items],
          option: action.option
        }
      };
    default:
      return state;
  }
}
