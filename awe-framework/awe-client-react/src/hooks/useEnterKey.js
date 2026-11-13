import {useMemo, useRef} from "react";
import {createEnterHandler} from "../utilities/keyboard";

/**
 * Enter handler of a criterion, to use as its onKeyDown. On Enter it prevents the default action and stops the
 * propagation (as the AngularJS engine does), then runs onEnter to commit or submit. Other keys are left untouched.
 * The handler is stable: it always runs the latest onEnter and condition.
 * @param {function(KeyboardEvent)} [onEnter] Called with the event once it is cancelled
 * @param {object} [options] Options
 * @param {function(KeyboardEvent): boolean} [options.when] Extra condition (for example a closed overlay)
 * @return {function(KeyboardEvent): boolean} Key down handler. It returns true when it handled Enter
 */
export default function useEnterKey(onEnter, {when} = {}) {
  const latest = useRef({onEnter, when});
  latest.current = {onEnter, when};
  return useMemo(() => createEnterHandler(
    (event) => latest.current.onEnter?.(event),
    {when: (event) => !latest.current.when || latest.current.when(event)}
  ), []);
}
