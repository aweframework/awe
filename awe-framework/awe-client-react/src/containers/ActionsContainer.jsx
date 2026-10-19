import React, {useEffect, useRef} from "react";
import {shallowEqual, useDispatch, useSelector} from "react-redux";
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

/**
 * Actions container
 * @category Containers
 */
function ActionsContainer() {
  const dispatch = useDispatch();
  const prevRunningActionsRef = useRef([]);
  const isShowing = useSelector((state) => state.settings.actionsStack > 0);
  const syncStack = useSelector((state) => state.actions.sync);
  const asyncStack = useSelector((state) => state.actions.async);
  const settings = useSelector(selectSettings);

  // The list is rebuilt on every call, but its actions are the ones of the state: shallowEqual keeps the previous list
  const runningActions = useSelector((state) => [
    ...state.actions.sync[state.actions.sync.length - 1].filter((a) => a.status === STATUS_RUNNING),
    ...state.actions.async.filter((a) => a.status === STATUS_RUNNING)
  ], shallowEqual);

  const combinedActions = useClientActions();

  useEffect(() => {
    // Check generic actions
    checkActions(combinedActions, {prevRunningActions: prevRunningActionsRef.current, runningActions}, {dispatch, settings});
    prevRunningActionsRef.current = runningActions;
  }, [runningActions]);

  // Toggle the actions stack
  useKeyboardShortcut([{
    shortcut: ACTIONS_STACK_SHORTCUT,
    handler: (event) => dispatch(updateSettings({actionsStack: DIGIT_CODES.indexOf(event.code) * 1000}))
  }]);

  return <div
      className="actions-zone" style={{display: isShowing ? "block" : "none"}}>
      <StackList type={"async"} elements={asyncStack}/>
      <StackList type={"sync"} stacks={syncStack}/>
    </div>;
}

export default ActionsContainer;
