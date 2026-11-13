import { useEffect, useRef } from "react";
import { useDispatch } from "react-redux";
import { loadDeferredAction } from "../redux/thunks/deferred";

/**
 * Ask for the data of a deferred container (a dialog or a tab page with the deferred attribute) the first time it is
 * shown. A container which is not deferred, or which was already requested, does nothing.
 * If the load failed the container is pending again, but it is requested again on its next opening, not while it is
 * still shown (that would repeat a failing request in a loop).
 *
 * @param {object} address Container address (undefined while the container is not registered)
 * @param {object} attributes Container attributes
 * @param {boolean} shown The container is shown to the user
 */
export function useDeferredLoad(address, attributes, shown) {
  const dispatch = useDispatch();
  const pending = !!attributes?.deferred && !attributes?.deferredLoaded;
  const requestedWhileShown = useRef(false);

  useEffect(() => {
    if (!shown) {
      requestedWhileShown.current = false;
    } else if (pending && address && !requestedWhileShown.current) {
      requestedWhileShown.current = true;
      dispatch(loadDeferredAction(address));
    }
  }, [shown, pending, address, dispatch]);
}
