import { useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useComponentState } from "./useComponentState";
import { addActionsTop } from "../redux/actions/actions";

/**
 * useComponent hook
 * Manages component general actions
 */
export default function useComponent(id) {
  const dispatch = useDispatch();
  const component = useComponentState(id);
  const { address = {}, attributes = {} } = component;
  const { autoload = false, autorefresh = 0 } = attributes;

  // Initial autoload action
  useEffect(() => {
    if (autoload) {
      dispatch(addActionsTop([{ type: "filter", address }]));
    }
  }, [autoload]);

  // Auto-refresh action at an interval when autorefresh > 0
  useEffect(() => {
    if (!address) return;

    const intervalSeconds = Number(autorefresh) || 0;
    if (intervalSeconds > 0) {
      const intervalId = setInterval(() => {
        dispatch(addActionsTop([{ type: "filter", address }]));
      }, intervalSeconds * 1000);

      return () => clearInterval(intervalId);
    }
  }, [autorefresh, address, dispatch]);

  return useMemo(() => ({ address }), [address]);
}
