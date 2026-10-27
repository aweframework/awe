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

// Last element which received the focus: the control which opens a panel may have lost the focus when the panel is
// activated (it is disabled for a moment while its action runs)
let lastFocused = null;
let trackingFocus = false;

const trackFocus = () => {
  if (!trackingFocus) {
    trackingFocus = true;
    document.addEventListener("focusin", event => {
      lastFocused = event.target;
    }, true);
  }
};

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
    return !active || active === document.body || !!(container && container.contains(active));
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
  observer.observe(opener, {attributes: true, attributeFilter: ["disabled"]});
  setTimeout(() => observer.disconnect(), OPENER_ENABLE_TIMEOUT);
};

/**
 * Focusable controls of a container, in tab order (the ones taken out of the tab order are skipped)
 * @param {HTMLElement} container Container
 * @returns {HTMLElement[]} Focusable controls
 */
const getFocusable = container => [...container.querySelectorAll(FOCUSABLE)]
  .filter(element => element.tabIndex !== -1 && !element.closest("[hidden],[aria-hidden='true']"));

/**
 * Focus the container itself (when it has no focusable control)
 * @param {HTMLElement} container Container
 */
const focusContainer = container => {
  if (!container.hasAttribute("tabindex")) {
    container.setAttribute("tabindex", "-1");
  }
  container.focus();
};

/**
 * Keep the keyboard focus inside a container while it is active (a side panel), and give it back to the control which
 * had it when the trap was activated. Tab and Shift+Tab cycle through the focusable controls of the container, unless
 * trapTab is false (only the focus moved in and given back).
 *
 * @param {function(): HTMLElement} getContainer Returns the container element
 * @param {{trapTab: boolean}} [options] Options
 * @returns {{activate: function, focusFirst: function, deactivate: function}} Focus trap
 */
export function createFocusTrap(getContainer, {trapTab = true} = {}) {
  trackFocus();
  let opener = null;
  let active = false;

  const onKeyDown = event => {
    const container = getContainer();
    if (event.key !== "Tab" || !container) {
      return;
    }
    const focusable = getFocusable(container);
    if (focusable.length === 0) {
      event.preventDefault();
      focusContainer(container);
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

  return {
    /**
     * Start trapping Tab, remembering the control focused now (the one which opened the container)
     */
    activate() {
      if (!active) {
        active = true;
        opener = getOpener();
        if (trapTab) {
          document.addEventListener("keydown", onKeyDown, true);
        }
      }
    },
    /**
     * Move the focus to the first control of the container, unless it is already inside
     */
    focusFirst() {
      const container = getContainer();
      if (!container || container.contains(document.activeElement)) {
        return;
      }
      const [first] = getFocusable(container);
      if (first) {
        first.focus();
      } else {
        focusContainer(container);
      }
    },
    /**
     * Stop trapping Tab and give the focus back to the control which opened the container (if it is still in the page)
     */
    deactivate() {
      if (!active) {
        return;
      }
      active = false;
      document.removeEventListener("keydown", onKeyDown, true);
      restoreFocus(opener, getContainer());
      opener = null;
    }
  };
}
