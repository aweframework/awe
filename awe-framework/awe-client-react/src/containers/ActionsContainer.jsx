import React, {useEffect, useMemo, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {StackList} from "../components/Actions";
import {updateSettings} from "../redux/actions/settings";
import {checkActions, combineActions} from "../utilities";
import useFormService from "../services/FormService";
import useMessageService from "../services/MessageService";
import useComponentService from "../services/ComponentService";
import useGridService from "../services/components/GridService";
import useWebsocketService from "../services/WebsocketService";
import {ActionStatus} from "../redux/actions/actions";
import useScreenService from "../services/ScreenService";

const {STATUS_RUNNING} = ActionStatus;
const DIGITS = ["Digit0", "Digit1", "Digit2", "Digit3", "Digit4", "Digit5", "Digit6", "Digit7", "Digit8", "Digit9"];

/**
 * Actions container
 * @category Containers
 */
function ActionsContainer() {
  const dispatch = useDispatch();
  const prevRunningActionsRef = useRef([]);
  const {isShowing, syncStack, asyncStack, settings} = useSelector((state) => ({
    isShowing: state.settings.actionsStack > 0,
    syncStack: state.actions.sync,
    asyncStack: state.actions.async,
    settings: state.settings
  }));

  const runningActions = useSelector((state) => [
    ...state.actions.sync[state.actions.sync.length - 1].filter((a) => a.status === STATUS_RUNNING),
    ...state.actions.async.filter((a) => a.status === STATUS_RUNNING)
  ]);

  const formService = useFormService();
  const screenService = useScreenService();
  const messageService = useMessageService();
  const componentService = useComponentService();
  const gridService = useGridService();
  const websocketService = useWebsocketService();

  const combinedActions = useMemo(
    () =>
      combineActions(
        formService,
        screenService,
        messageService,
        componentService,
        gridService,
        websocketService
      ),
    [formService, screenService, messageService, componentService, gridService, websocketService]
  );

  useEffect(() => {
    // Check generic actions
    checkActions(combinedActions, {prevRunningActions: prevRunningActionsRef.current, runningActions}, {dispatch, settings});
    prevRunningActionsRef.current = runningActions;
  }, [runningActions]);

  /**
   * Component was mounted
   */
  useEffect(() => {
    // Activate actions stack
    window.onkeydown = (event) => {
      if (event.altKey && event.shiftKey && DIGITS.includes(event.code)) {
        // Toggle stack
        dispatch(updateSettings({actionsStack: DIGITS.indexOf(event.code) * 1000}));
      }
    };
  }, []);

  return <div
      className="actions-zone" style={{display: isShowing ? "block" : "none"}}>
      <StackList type={"async"} elements={asyncStack}/>
      <StackList type={"sync"} stacks={syncStack}/>
    </div>;
}

export default ActionsContainer;