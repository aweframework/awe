import React, {useCallback, useEffect, useState} from "react";
import {createPortal} from "react-dom";
import PropTypes from "prop-types";

const CLAMPED_CELL = ".p-cell-clamp";

/**
 * Check if the text of a cell is cut by its maximum number of lines
 * @param {HTMLElement} element Cell text
 * @return {boolean} The text does not fit
 */
export function isCut(element) {
  return element.scrollHeight > element.clientHeight + 1;
}

/**
 * Tooltip with the full text of the cells of a grid whose text is cut by their maximum number of lines. One instance
 * listens to the whole grid (hover and keyboard focus) instead of one tooltip per cell, so a page with many cells
 * costs the same. Whether a cell is cut is measured only when the tooltip is about to show (the cells are focusable
 * from the start, so the keyboard reaches them): measuring every cell on every render forces one layout per cell.
 * @category Components
 * @subcategory Grid
 */
function AweGridCellTooltip({containerRef}) {
  const [tooltip, setTooltip] = useState(null);

  const show = useCallback((event) => {
    const cell = event.target?.closest?.(CLAMPED_CELL);
    if (cell && isCut(cell)) {
      const rect = cell.getBoundingClientRect();
      setTooltip({text: cell.getAttribute("data-full-text"), top: rect.bottom, left: rect.left, width: rect.width});
    }
  }, []);
  const hide = useCallback(() => setTooltip(null), []);
  const onKeyDown = useCallback((event) => event.key === "Escape" && hide(), [hide]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return undefined;
    }
    container.addEventListener("mouseover", show);
    container.addEventListener("focusin", show);
    container.addEventListener("mouseout", hide);
    container.addEventListener("focusout", hide);
    container.addEventListener("keydown", onKeyDown);
    return () => {
      container.removeEventListener("mouseover", show);
      container.removeEventListener("focusin", show);
      container.removeEventListener("mouseout", hide);
      container.removeEventListener("focusout", hide);
      container.removeEventListener("keydown", onKeyDown);
    };
  }, [containerRef, show, hide, onKeyDown]);

  if (!tooltip) {
    return null;
  }
  return createPortal(
    <div className="p-tooltip p-component p-tooltip-bottom p-tooltip-active grid-cell-tooltip" role="tooltip"
      style={{top: tooltip.top, left: tooltip.left, minWidth: Math.min(tooltip.width, 320)}}>
      <div className="p-tooltip-arrow"/>
      <div className="p-tooltip-text">{tooltip.text}</div>
    </div>,
    document.body);
}

AweGridCellTooltip.propTypes = {
  containerRef: PropTypes.shape({current: PropTypes.any}).isRequired,
};

export default AweGridCellTooltip;
