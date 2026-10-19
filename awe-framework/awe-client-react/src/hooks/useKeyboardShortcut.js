import {useCallback, useEffect, useRef} from "react";
import {createKeyHandler} from "../utilities/keyboard";

/**
 * Resolve the event target of a shortcut: the window, a React ref or an event target
 * @param {object} [target] Undefined for the window, a ref (its current is used) or an EventTarget
 * @return {EventTarget|null} Event target, null when the ref is empty
 */
function resolveTarget(target) {
  if (target === undefined) {
    return typeof window === "undefined" ? null : window;
  }
  if (target && typeof target.addEventListener !== "function" && "current" in target) {
    return target.current ?? null;
  }
  return target ?? null;
}

/**
 * Register keyboard shortcuts while the component is mounted. It adds a listener (it never replaces the ones of
 * others, such as the window key down property) and removes it on unmount. The listener is not subscribed again when the
 * bindings change: it always runs the latest ones.
 * @param {object[]} bindings Bindings, see createKeyHandler
 * @param {object} [options] Options
 * @param {object} [options.target] Window by default, a React ref or an EventTarget
 * @param {boolean} [options.enabled] The shortcuts are active
 * @param {string} [options.eventName] Event to listen to
 * @param {boolean} [options.capture] Listen in the capture phase
 */
export default function useKeyboardShortcut(bindings, {target, enabled = true, eventName = "keydown", capture = false} = {}) {
  const bindingsRef = useRef(bindings);
  bindingsRef.current = bindings;
  const handler = useCallback((event) => createKeyHandler(bindingsRef.current)(event), []);

  useEffect(() => {
    const element = enabled ? resolveTarget(target) : null;
    if (!element) {
      return undefined;
    }
    element.addEventListener(eventName, handler, capture);
    return () => element.removeEventListener(eventName, handler, capture);
  }, [target, enabled, eventName, capture, handler]);
}
