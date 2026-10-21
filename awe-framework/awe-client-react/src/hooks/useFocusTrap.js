import { useEffect, useRef } from "react";

const FOCUSABLE = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled]):not([type='hidden'])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex='-1'])",
  "[contenteditable='true']"
].join(",");

// Time to wait for a disabled opener to be enabled again before giving up giving it the focus back
const OPENER_ENABLE_TIMEOUT = 2000;

// Last element which received the focus: a button which opens a panel is disabled for a moment while its action runs,
// so it has already lost the focus when the panel is activated
let lastFocused = null;
let trackingFocus = false;

const trackFocus = () => {
  if (!trackingFocus) {
    trackingFocus = true;
    document.addEventListener("focusin", (event) => {
      lastFocused = event.target;
    }, true);
  }
};

/**
 * Focusable controls of a container, in tab order (the ones taken out of the tab order are skipped)
 * @param {HTMLElement} container Container
 * @returns {HTMLElement[]} Focusable controls
 */
const getFocusable = (container) => [...container.querySelectorAll(FOCUSABLE)]
  .filter(element => element.tabIndex !== -1 && !element.closest("[hidden],[aria-hidden='true']"));

/**
 * Control which opened the container: the focused one, or the one focused last when the focus is on the page body
 * @returns {HTMLElement|null} Opener
 */
const getOpener = () => {
  const active = document.activeElement;
  return active && active !== document.body ? active : lastFocused;
};

/**
 * Give the focus back to the opener, unless the user moved it to another control outside the container. A disabled
 * opener (its action is still running) gets the focus once it is enabled again.
 * @param {HTMLElement} opener Opener
 * @param {HTMLElement} container Container (it may be gone)
 */
const restoreFocus = (opener, container) => {
  if (!opener || opener === document.body || !opener.isConnected || typeof opener.focus !== "function") {
    return;
  }
  const focusIsFree = () => {
    const active = document.activeElement;
    return !active || active === document.body || !!container?.contains(active);
  };
  if (!focusIsFree()) {
    return;
  }
  if (!opener.disabled) {
    opener.focus();
    return;
  }
  const observer = new MutationObserver(() => {
    if (!opener.disabled) {
      observer.disconnect();
      if (focusIsFree()) {
        opener.focus();
      }
    }
  });
  observer.observe(opener, { attributes: true, attributeFilter: ["disabled"] });
  setTimeout(() => observer.disconnect(), OPENER_ENABLE_TIMEOUT);
};

/**
 * Keep the keyboard focus inside a container while it is active (a side panel), and give it back to the control which
 * opened it when it is deactivated. Tab and Shift+Tab cycle through the focusable controls of the container.
 *
 * @param {function(): HTMLElement} getContainer Returns the container element (it may not exist yet)
 * @param {boolean} active The container is shown
 */
export function useFocusTrap(getContainer, active) {
  // The latest getter, so a new function on each render does not restart the trap (nor give the focus back)
  const containerGetter = useRef(getContainer);
  containerGetter.current = getContainer;
  trackFocus();

  useEffect(() => {
    if (!active) {
      return undefined;
    }
    const opener = getOpener();

    const onKeyDown = (event) => {
      const container = containerGetter.current();
      if (event.key !== "Tab" || !container) {
        return;
      }
      const focusable = getFocusable(container);
      if (focusable.length === 0) {
        event.preventDefault();
        if (!container.hasAttribute("tabindex")) {
          container.setAttribute("tabindex", "-1");
        }
        container.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const current = document.activeElement;
      const outside = !container.contains(current);
      if (event.shiftKey && (outside || current === first)) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (outside || current === last)) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKeyDown, true);
    return () => {
      document.removeEventListener("keydown", onKeyDown, true);
      restoreFocus(opener, containerGetter.current());
    };
  }, [active]);
}
