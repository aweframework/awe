import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import PrimeReact from "primereact/api";
import { ZIndexUtils } from "primereact/utils";
import { Components } from "../utilities/structure";
import { classNames } from "../utilities/components";
import { getIconNode, translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useDeferredLoad } from "../hooks/useDeferredLoad";
import { useFocusTrap } from "../hooks/useFocusTrap";
import { addActionsTop } from "../redux/actions/actions";
import PropTypes from "prop-types";
import { TestIds, testHook } from "../utilities/testIds";
import "./AwePopover.less";

const DEFAULT_MAX_WIDTH = "20rem";
const DEFAULT_POSITION = "bottom";

// Attributes that carry the id of a component on its DOM: the whole criterion (not only its input), grids, tree grids
// and windows. Any other component (a button, a tag...) renders the id as the element id.
const ANCHOR_ATTRIBUTES = ["criterion-id", "grid-id", "tree-grid-id", "window-id"];

// Overlays of the controls inside the popover (dropdown panels, calendars...), which are appended to the body, and
// the dialogs and messages shown over it: a click on them is not a click outside the popover
export const OVERLAYS = [
  ".p-dropdown-panel", ".p-multiselect-panel", ".p-autocomplete-panel", ".p-datepicker", ".p-overlaypanel",
  ".p-colorpicker-panel", ".p-contextmenu", ".p-tieredmenu", ".p-dialog-mask", ".p-toast"
].join(",");

// Time to wait for the click which follows a mouse down on the anchor (it would open the popover again)
const CLICK_TIMEOUT = 1000;

/**
 * Absorb the click which follows a mouse down on the anchor: the mouse down closes the popover, and the action of the
 * anchor must not open it again (the dialog action is not accepted while the popover is shown, it would wait forever)
 * @param {HTMLElement} anchor Anchor element
 */
export function swallowAnchorClick(anchor) {
  const onClick = (event) => {
    document.removeEventListener("click", onClick, true);
    if (anchor.contains(event.target)) {
      event.preventDefault();
      event.stopPropagation();
    }
  };
  document.addEventListener("click", onClick, true);
  setTimeout(() => document.removeEventListener("click", onClick, true), CLICK_TIMEOUT);
}

/**
 * DOM element of the component the popover is anchored to
 * @param {string} anchor Component id
 * @returns {HTMLElement|null} Anchor element
 */
export function findAnchor(anchor) {
  if (!anchor) {
    return null;
  }
  const value = anchor.replace(/["\\]/g, "\\$&");
  for (const attribute of ANCHOR_ATTRIBUTES) {
    const element = document.querySelector(`[${attribute}="${value}"]`);
    if (element) {
      return element;
    }
  }
  return document.getElementById(anchor);
}

/**
 * Placement of the popover on a side of its anchor, in page coordinates. The popover is moved by its own size with a
 * transform when it opens above or at the left of the anchor, so it does not need to be measured.
 * @param {HTMLElement} anchor Anchor element
 * @param {string} position Side of the anchor (bottom, top, left or right)
 * @returns {object} Style of the popover
 */
function getPlacement(anchor, position) {
  const rect = anchor.getBoundingClientRect();
  const top = rect.top + window.scrollY;
  const left = rect.left + window.scrollX;
  switch (position) {
    case "top":
      return { top: `${top}px`, left: `${left}px`, transform: "translateY(-100%)" };
    case "left":
      return { top: `${top}px`, left: `${left}px`, transform: "translateX(-100%)" };
    case "right":
      return { top: `${top}px`, left: `${rect.right + window.scrollX}px` };
    default:
      return { top: `${rect.bottom + window.scrollY}px`, left: `${left}px` };
  }
}

/**
 * Popover: a dialog anchored to a control of the screen (#843). It is opened and closed as a dialog, opens on a side
 * of its anchor (the component with the id of its "anchor" attribute), closes with its close button, a click outside
 * or Esc, and keeps the keyboard focus inside while it is shown.
 */
function AwePopover(props) {
  const { id, elementList = [] } = props;
  const { address, attributes } = useComponentState(id);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const container = useRef(null);
  const [placement, setPlacement] = useState(null);
  const shown = !!attributes?.isShowing;
  const { anchor, position = DEFAULT_POSITION } = attributes || {};

  const hide = useCallback(() => {
    if (attributes.isShowing) {
      // on-close="reject" (acceptOnClose false) rejects the action that opened the popover, as a dialog does
      const type = attributes.acceptOnClose === false ? "close-cancel" : "close";
      dispatch(addActionsTop([{ address, type }]));
    }
  }, [dispatch, attributes?.isShowing, attributes?.acceptOnClose, address]);

  // A deferred popover loads the data of its components when it is shown for the first time
  useDeferredLoad(address, attributes, shown);

  // Declared before the focus is moved inside, so the trap keeps the control which had it to give it back
  useFocusTrap(() => container.current, shown);

  // Place the popover next to its anchor, and follow it while the page scrolls or is resized
  useLayoutEffect(() => {
    if (!shown) {
      return undefined;
    }
    const place = () => {
      const element = findAnchor(anchor);
      setPlacement(element ? getPlacement(element, position) : null);
    };
    place();
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [shown, anchor, position]);

  // Stack over the screen and its dialogs, under the overlays opened later from its controls
  useLayoutEffect(() => {
    const element = container.current;
    if (!shown || !element) {
      return undefined;
    }
    ZIndexUtils.set("overlay", element, PrimeReact.autoZIndex, PrimeReact.zIndex.overlay);
    return () => ZIndexUtils.clear(element);
  }, [shown]);

  // Move the focus inside when it is shown
  useEffect(() => {
    const element = container.current;
    if (shown && element && !element.contains(document.activeElement)) {
      const first = element.querySelector(".popover-body button, .popover-body input, .popover-body select, " +
        ".popover-body textarea, .popover-body a[href], .popover-close");
      (first || element).focus();
    }
  }, [shown]);

  // Close with Esc or a click outside, the anchor included (its click is absorbed so it does not open it again)
  useEffect(() => {
    if (!shown) {
      return undefined;
    }
    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        hide();
      }
    };
    const onMouseDown = (event) => {
      const target = event.target;
      const element = container.current;
      const overlay = target.closest?.(OVERLAYS);
      if (element?.contains(target) || (overlay && !overlay.contains(element))) {
        return;
      }
      const anchorElement = findAnchor(anchor);
      if (anchorElement?.contains(target)) {
        swallowAnchorClick(anchorElement);
      }
      hide();
    };
    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("mousedown", onMouseDown, true);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("mousedown", onMouseDown, true);
    };
  }, [shown, hide, anchor]);

  // If address is undefined or it is hidden, render nothing
  if (!address || !shown) {
    return <></>;
  }

  const { style, label, maxWidth = DEFAULT_MAX_WIDTH } = attributes;
  const title = translateLabel(label, t);
  return createPortal(
    <div
      ref={container}
      id={id}
      role="dialog"
      aria-label={title || undefined}
      tabIndex={-1}
      className={classNames("awe-popover", `popover-${position}`, placement ? null : "popover-unanchored", style)}
      style={{ position: placement ? "absolute" : "fixed", maxWidth, ...placement }}
      {...testHook(TestIds.popover, { owner: id, open: shown })}
    >
      <div className="popover-header">
        <span className="popover-title">{title}</span>
        <button type="button" className="popover-close p-link" onClick={hide}
          aria-label={translateLabel("BUTTON_CLOSE", t)} {...testHook(TestIds.popoverClose)}>
          {getIconNode("pi-times", { tag: "span", "aria-hidden": "true" })}
        </button>
      </div>
      <div className="popover-body">
        {elementList.map((node, index) => Components(node, index))}
      </div>
    </div>,
    document.body
  );
}

AwePopover.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AwePopover;
