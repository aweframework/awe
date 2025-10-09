import {useEffect, useMemo} from "react";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";

/**
 * useComponent hook
 * Manages component general actions
 */
export default function useComponent(id) {
  const dispatch = useDispatch();
  const { address, autoload = false } = useSelector(state => ({
    address: state.components[id]?.address,
    autoload: state.components[id]?.attributes?.autoload,
  }));

  // Add actions top
  useEffect(() => {
    if (autoload) {
      dispatch(addActionsTop([{type: "filter", address}]));
    }
  }, [autoload]);

  return useMemo(() => ({address}), [address]);
}
