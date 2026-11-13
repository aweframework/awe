import React, {useEffect, useRef} from "react";
import {shallowEqual, useDispatch, useSelector, useStore} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {StackList} from "../components/Actions";
import {updateSettings} from "../redux/actions/settings";
import {checkActions} from "../utilities";
import useClientActions from "../services/ClientActionsService";
import {ActionStatus} from "../redux/actions/actions";
import useKeyboardShortcut from "../hooks/useKeyboardShortcut";
import {ANY, DIGIT_CODES} from "../utilities/keyboard";

const {STATUS_RUNNING} = ActionStatus;
// Alt+Shift+digit toggles the actions stack. Ctrl and Meta never mattered for it, so they are not checked
const ACTIONS_STACK_SHORTCUT = {code: DIGIT_CODES, alt: true, shift: true, ctrl: ANY, meta: ANY};

// Actions whose handlers read values of the last render (location, settings, websocket client): the actions after them
// wait for the next render, as every action did before #951. A new handler that reads a hook value (useSelector,
// useLocation, useState...) instead of the store has to be added here
export const RENDER_BOUND_ACTIONS = new Set([
  "screen", "reload", "back", "change-language", "reload-language", "change-theme",
  "connectWebsocket", "disconnectWebsocket"
]);

// Runs of the chain in one call: past them the chain yields to a render and goes on from the next effect, so an action
// that keeps launching itself at once cannot block the page
export const MAX_CHAINED_RUNS = 1000;

// Actions queued by addActions get increasing "action-N" ids; any other id keeps its place after them
const actionOrder = (action) => {
  const order = Number(String(action.id).replace("action-", ""));
  return Number.isNaN(order) ? Number.MAX_SAFE_INTEGER : order;
};

/**
 * Actions ready to run: the running actions of the current sync stack and of the async list, in the order they were
 * received (an async action leaves the sync stack before the next sync action starts, but it came first)
 * @param {object} state Store state
 * @return {object[]} Running actions
 */
const selectRunningActions = (state) => [
  ...state.actions.sync[state.actions.sync.length - 1].filter((a) => a.status === STATUS_RUNNING),
  ...state.actions.async.filter((a) => a.status === STATUS_RUNNING)
].sort((first, second) => actionOrder(first) - actionOrder(second));

/**
 * Lists of the actions stack. Only mounted while the stack is shown: the queue changes on every client action of a
 * response, and a hidden list would render all its actions for each change (#962)
 */
function ActionsStack() {
  const syncStack = useSelector((state) => state.actions.sync);
  const asyncStack = useSelector((state) => state.actions.async);
  return <>
    <StackList type={"async"} elements={asyncStack}/>
    <StackList type={"sync"} stacks={syncStack}/>
  </>;
}

/**
 * Actions container
 * @category Containers
 */
function ActionsContainer() {
  const dispatch = useDispatch();
  const store = useStore();
  const prevRunningActionsRef = useRef([]);
  const isShowing = useSelector((state) => state.settings.actionsStack > 0);
  const settings = useSelector(selectSettings);

  // The list is rebuilt on every call, but its actions are the ones of the state: shallowEqual keeps the previous list
  const runningActions = useSelector(selectRunningActions, shallowEqual);

  const combinedActions = useClientActions();

  useEffect(() => {
    // Run the actions ready to run and, while the ones they leave ready differ, run those too in the same call: an action
    // that finishes at once (fill, select, show...) starts the next one without a render in between, so the client
    // actions of one response commit a single render. Each action still updates the store and checks the dependencies
    // on the state the previous one left. The chain stops at an action that waits (server, dialog, confirm, message...)
    let running = runningActions;
    let runs = 0;
    while (running.length && !shallowEqual(prevRunningActionsRef.current, running)) {
      if (runs++ === MAX_CHAINED_RUNS) {
        // eslint-disable-next-line no-console
        console.warn(`[ACTIONS] ${MAX_CHAINED_RUNS} actions run in a row: rendering before going on`, running);
        break;
      }
      const actions = {prevRunningActions: prevRunningActionsRef.current, runningActions: running};
      checkActions(combinedActions, actions, {dispatch, settings});
      prevRunningActionsRef.current = running;
      if (running.some((action) => RENDER_BOUND_ACTIONS.has(action.type))) {
        break;
      }
      running = selectRunningActions(store.getState());
    }
  }, [runningActions]);

  // Toggle the actions stack
  useKeyboardShortcut([{
    shortcut: ACTIONS_STACK_SHORTCUT,
    handler: (event) => dispatch(updateSettings({actionsStack: DIGIT_CODES.indexOf(event.code) * 1000}))
  }]);

  return <div
      className="actions-zone" style={{display: isShowing ? "block" : "none"}}>
      {isShowing && <ActionsStack/>}
    </div>;
}

export default ActionsContainer;
