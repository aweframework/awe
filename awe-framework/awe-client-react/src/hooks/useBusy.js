import { useCallback, useEffect, useRef, useState } from "react";
import { getIconNode } from "../utilities";
import { classNames } from "../utilities/components";

/**
 * Time (ms) a button waits before it shows its busy state: a work that ends before shows nothing. The same value in the
 * AngularJS client
 * @memberOf Hooks
 */
export const BUSY_THRESHOLD = 400;

/**
 * Busy state of a work: true once the work has been running for the threshold, false again when it ends
 * @param {boolean} active The work runs
 * @returns {boolean} The work is busy
 * @memberOf Hooks
 */
export function useDelayedBusy(active) {
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!active) {
      return undefined;
    }
    const timer = setTimeout(() => setBusy(true), BUSY_THRESHOLD);
    return () => {
      clearTimeout(timer);
      setBusy(false);
    };
  }, [active]);
  return busy;
}

/**
 * Busy state of the button that launches the actions: true when the actions still run the threshold after the click,
 * false again when they end. A click whose actions are not running at the threshold shows nothing
 * @param {boolean} running The actions run
 * @returns {[boolean, function]} The button is busy, and the function to call on its click
 * @memberOf Hooks
 */
export function useLaunchBusy(running) {
  const [busy, setBusy] = useState(false);
  const timer = useRef(null);
  const runningRef = useRef(running);
  const stop = useCallback(() => {
    clearTimeout(timer.current);
    timer.current = null;
  }, []);

  useEffect(() => {
    runningRef.current = running;
    if (!running) {
      stop();
      setBusy(false);
    }
  }, [running, stop]);
  useEffect(() => stop, [stop]);

  const launch = useCallback(() => {
    stop();
    timer.current = setTimeout(() => {
      timer.current = null;
      setBusy(runningRef.current);
    }, BUSY_THRESHOLD);
  }, [stop]);
  return [busy, launch];
}

/**
 * Spinner of a busy button (lucide loader-circle), in the place of its icon. It stops when the user prefers reduced
 * motion (awe-icon-spin)
 * @param {string} [className] Extra classes of the spinner
 * @returns {React.ReactNode} Spinner
 * @memberOf Hooks
 */
export const getBusyIcon = className => getIconNode("lucide:loader-circle", {
  className: classNames("p-button-icon p-button-icon-left awe-icon-spin", className), "aria-hidden": "true"
});
