import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { loadDeferredAction } from "../redux/thunks/deferred";

/**
 * Ask for the data of a deferred container (a dialog or a tab page with the deferred attribute) the first time it is
 * shown. A container which is not deferred, or which was already requested, does nothing.
 *
 * @param {object} address Container address (undefined while the container is not registered)
 * @param {object} attributes Container attributes
 * @param {boolean} shown The container is shown to the user
 */
export function useDeferredLoad(address, attributes, shown) {
  const dispatch = useDispatch();
  const pending = !!attributes?.deferred && !attributes?.deferredLoaded;

  useEffect(() => {
    if (shown && pending && address) {
      dispatch(loadDeferredAction(address));
    }
  }, [shown, pending, address, dispatch]);
}
