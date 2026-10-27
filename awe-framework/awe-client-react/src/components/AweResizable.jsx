import React, {useEffect, useRef} from "react";
import SizeRegistry from "../redux/registry/SizeRegistry";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import {getIconNode} from "../utilities";
import PropTypes from "prop-types";
import "./AweResizable.less";

const DIRECTIONS = ["top", "right", "bottom", "left"];
const MIN_SIZE = 160;
const MAX_RATIO = 0.99;
const isHorizontalAxis = (direction) => direction === "left" || direction === "right";

/**
 * Resizable container. As in the AngularJS engine, the sides listed in `directions` get a handle that the user drags
 * to change the size of the container itself. The children are stacked inside the container. The `no-resizable-*`
 * styles turn the handles off at the responsive breakpoints through the stylesheet.
 * @param {object} props Component properties
 * @returns {JSX.Element} Resizable
 */
function Resizable(props) {

  const {id, elementList = [], directions, style} = props;
  const containerRef = useRef(null);
  const dragRef = useRef(null);
  const handles = (directions || "").split(/\s+/).filter((direction, index, all) =>
    DIRECTIONS.includes(direction) && all.indexOf(direction) === index);

  const stopDragging = (event, notify) => {
    const drag = dragRef.current;
    if (!drag) {
      return;
    }
    dragRef.current = null;
    drag.container.classList.remove("no-transition");
    event?.currentTarget?.releasePointerCapture?.(drag.pointerId);
    if (notify) {
      SizeRegistry.setSize({width: window.innerWidth, height: window.innerHeight, current: drag.size});
    }
  };

  // Clamp to the limits of the stylesheet: at least MIN_SIZE and at most 99% of the parent
  const clamp = (size, parent, horizontal) => {
    const parentSize = parent ? parent.getBoundingClientRect()[horizontal ? "width" : "height"] : 0;
    const max = parentSize > 0 ? Math.max(MIN_SIZE, parentSize * MAX_RATIO) : Infinity;
    return Math.round(Math.max(MIN_SIZE, Math.min(max, size)));
  };

  const startDrag = (event, direction) => {
    const container = containerRef.current;
    const horizontal = isHorizontalAxis(direction);
    const rect = container.getBoundingClientRect();
    const parent = container.parentElement;
    const parentDirection = parent ? window.getComputedStyle(parent).flexDirection : "";
    dragRef.current = {
      container, horizontal, parent,
      pointerId: event.pointerId,
      start: horizontal ? event.clientX : event.clientY,
      initialSize: horizontal ? rect.width : rect.height,
      // Right and bottom handles grow with the pointer, left and top ones against it
      sign: direction === "right" || direction === "bottom" ? 1 : -1,
      // The flex basis only matters when the parent lays its children out along the axis of the handle
      alongParent: horizontal ? parentDirection.startsWith("row") : parentDirection.startsWith("column"),
      size: horizontal ? rect.width : rect.height
    };

    // Prevent the transitions while dragging and keep receiving the events of this pointer (mouse, touch and pen)
    container.classList.add("no-transition");
    event.currentTarget.setPointerCapture?.(event.pointerId);
    event.preventDefault();
    event.stopPropagation();
  };

  const drag = (event) => {
    const current = dragRef.current;
    if (!current || event.pointerId !== current.pointerId) {
      return;
    }
    const position = current.horizontal ? event.clientX : event.clientY;
    current.size = clamp(current.initialSize + current.sign * (position - current.start), current.parent, current.horizontal);
    // Each handle resizes the dimension of its own axis
    current.container.style[current.horizontal ? "width" : "height"] = `${current.size}px`;
    if (current.alongParent) {
      current.container.style.flexBasis = `${current.size}px`;
    }
  };

  // Release the drag if the resizable disappears while it is dragged
  useEffect(() => () => stopDragging(null, false), []);

  return <div id={id} ref={containerRef}
    className={classNames("resizable resizable-awe expandible-vertical", ...handles, style)}>
    <div className="expand expandible-vertical">
      {elementList.map((node, index) => Components(node, index))}
    </div>
    {handles.map(direction => <div key={`handle-${direction}`} className={`rg-${direction}`}
      onPointerDown={(event) => startDrag(event, direction)} onPointerMove={drag}
      onPointerUp={(event) => stopDragging(event, true)} onPointerCancel={(event) => stopDragging(event, false)}>
      {getIconNode(isHorizontalAxis(direction) ? "pi-ellipsis-v" : "pi-ellipsis-h", {tag: "span"})}
    </div>)}
  </div>;
}

Resizable.propTypes = {
  directions: PropTypes.any,
  elementList: PropTypes.any,
  id: PropTypes.string,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

export default Resizable;
