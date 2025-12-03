import {useEffect, useMemo} from "react";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";

/**
 * useComponent hook
 * Manages component general actions
 */
export default function useComponent(id) {
  const dispatch = useDispatch();
  const { address = {}, autoload = false, autorefresh = 0 } = useSelector(state => ({
    address: state.components[id]?.address,
    autoload: state.components[id]?.attributes?.autoload,
    autorefresh: state.components[id]?.attributes?.autorefresh,
  }));

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
