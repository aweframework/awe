import {useSyncExternalStore} from "react";

let capsLockOn = false;
const listeners = new Set();

/**
 * Store the caps lock state of a keyboard event (the AngularJS engine does it on every key down and key up of the page)
 * @param {KeyboardEvent} event Keyboard event
 */
function onKey(event) {
  if (typeof event?.getModifierState === "function") {
    const state = event.getModifierState("CapsLock");
    if (state !== capsLockOn) {
      capsLockOn = state;
      listeners.forEach(listener => listener());
    }
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("keydown", onKey, true);
  document.addEventListener("keyup", onKey, true);
}

const subscribe = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

/**
 * Know if the caps lock is on, from the last key pressed or released in the page
 * @returns {boolean} Caps lock is on
 */
export default function useCapsLock() {
  return useSyncExternalStore(subscribe, () => capsLockOn, () => false);
}
