import {getActionAddress, getComponent} from "../../utilities";
import {abortAction} from "../actions/actions";
import {GRID_ACTIONS} from "../actions/gridActions";
import {getAllComponents} from "../selectors/componentSelectors";
import type {AppThunk} from "../types";
import type {ClientAction} from "./types";

/**
 * Client actions that work on a component of the screen (the groups that AngularJS discards when the target is not
 * there: component, chart, grid, pivot, uploader and wizard). The dialog actions (dialog, close, close-cancel) stay out, as AngularJS keeps them in its
 * screen group, and so do the menu actions that change the whole menu (toggle-navbar, change-menu).
 */
const COMPONENT_ACTIONS = new Set([
  // Button, uploader and menu
  "click", "show-context-menu", "file-uploaded", "clear-file", "toggle-menu",
  // Wizard
  "next-step", "prev-step", "first-step", "last-step", "nth-step",
  // Chart
  "add-points", "add-chart-series", "remove-chart-series", "replace-chart-series",
  // Map
  "polyline",
  // Pivot
  "set-pivot-sorters", "set-pivot-group-rows", "set-pivot-group-cols",
  // Grid
  ...GRID_ACTIONS
]);

/**
 * Check if an action type works on a component of the screen
 * @param {string} type Action type
 * @return {boolean} The action is a component action
 */
export const isComponentAction = (type: string): boolean => COMPONENT_ACTIONS.has(type);

/**
 * Run an action only when its target component is on screen. As the AngularJS client does, an action whose target
 * is not there (it was never rendered, belongs to a closed dialog or the screen changed meanwhile) is aborted, so the
 * action queue goes on, and only a debug trace is left
 * @param {object} action Action to run
 * @param {function} run Launches the action
 */
export const runActionOnTarget = (action: ClientAction, run: () => void): AppThunk => {
  return (dispatch, getState) => {
    const {view, component} = getActionAddress(action);
    // Only an action that names a target can be discarded; the others run as before. The component is looked up by
    // view and component only: a cell or column action (update-cell…) also carries row and column in its address
    if (!component || getComponent(getAllComponents(getState()), {view, component})) {
      run();
    } else {
      // Deliberate debug trace: a developer can find out why the action did nothing
      // eslint-disable-next-line no-console
      console.debug(`[ACTIONS] '${action.type}' ignored, its target is not on screen`, action);
      dispatch(abortAction(action));
    }
  };
};
