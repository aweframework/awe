import React, {useCallback, useEffect, useLayoutEffect, useState} from "react";
import {createPortal} from "react-dom";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import "./GridColumnOverlays.less";

/**
 * Transparent overlays over the columns of a grid in the inspect canvas of the screen personalization editor (#792):
 * one per selectable column, covering header and body, placed from the header cells and measured again when the grid
 * resizes or scrolls. The overlay takes the click, so the grid does not sort.
 * @returns {React.ReactPortal} Overlays inside the grid
 */
export default function GridColumnOverlays({grid, columns, hidden, selected, onSelect}) {
  const {t} = useTranslation();
  const [boxes, setBoxes] = useState([]);

  const measure = useCallback(() => {
    const origin = grid.getBoundingClientRect();
    setBoxes([...grid.querySelectorAll("th[column-id]")]
      .filter(cell => columns.has(cell.getAttribute("column-id")))
      .map(cell => {
        const box = cell.getBoundingClientRect();
        return {name: cell.getAttribute("column-id"), text: cell.textContent.trim(), left: box.left - origin.left, width: box.width, height: origin.height};
      }));
  }, [grid, columns]);

  useLayoutEffect(measure, [measure]);
  useEffect(() => {
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(measure);
    observer?.observe(grid);
    grid.addEventListener("scroll", measure, true);
    window.addEventListener("resize", measure);
    return () => {
      observer?.disconnect();
      grid.removeEventListener("scroll", measure, true);
      window.removeEventListener("resize", measure);
    };
  }, [grid, measure]);

  return createPortal(<>{boxes.map(box => (
    <button type="button" key={box.name} aria-label={box.text || box.name} style={{left: box.left, top: 0, width: box.width, height: box.height}}
      className={classNames("awe-column-overlay", {"awe-column-overlay-hidden": hidden.has(box.name), "awe-column-overlay-selected": selected === box.name})}
      onClick={event => {
        event.preventDefault();
        event.stopPropagation();
        onSelect(box.name);
      }}>
      {hidden.has(box.name) && <span className="awe-inspect-tag">{t("SCR_PER_HIDDEN")}</span>}
    </button>
  ))}</>, grid);
}

GridColumnOverlays.propTypes = {
  grid: PropTypes.object.isRequired,
  columns: PropTypes.instanceOf(Set).isRequired,
  hidden: PropTypes.instanceOf(Set).isRequired,
  selected: PropTypes.string,
  onSelect: PropTypes.func.isRequired
};
