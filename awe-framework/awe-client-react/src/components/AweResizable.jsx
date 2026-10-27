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
 * to change the size of the container itself. When the parent lays out along the axis of the handle, the sibling on
 * the handle side (next one for right/bottom, previous one for left/top) follows the pointer: it gives or takes the
 * size the container gains or loses, so the splitter stays under the pointer and the other panes do not move. The
 * children are stacked inside the container. The `no-resizable-*` styles turn the handles off at the responsive
 * breakpoints through the stylesheet.
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

  // Sibling that follows the handle, only when the layout runs along the axis and it takes space
  const findNeighbour = (container, direction, horizontal, alongParent) => {
    if (!alongParent) {
      return null;
    }
    const element = direction === "right" || direction === "bottom"
      ? container.nextElementSibling : container.previousElementSibling;
    const dimension = horizontal ? "width" : "height";
    const initialSize = element ? element.getBoundingClientRect()[dimension] : 0;
    if (!element || !(initialSize > 0)) {
      return null;
    }
    // The neighbour never gets below its own css minimum, nor below MIN_SIZE unless it started smaller
    const cssMin = parseFloat(window.getComputedStyle(element)[horizontal ? "minWidth" : "minHeight"]);
    return {element, initialSize, min: Math.max(Number.isFinite(cssMin) ? cssMin : 0, Math.min(MIN_SIZE, initialSize))};
  };

  const startDrag = (event, direction) => {
    const container = containerRef.current;
    const horizontal = isHorizontalAxis(direction);
    const rect = container.getBoundingClientRect();
    const parent = container.parentElement;
    const parentStyle = parent ? window.getComputedStyle(parent) : null;
    // Only a flex parent lays out along an axis: a block parent computes flex-direction: row too
    const flexParent = parentStyle && (parentStyle.display || "").includes("flex");
    const parentDirection = flexParent ? parentStyle.flexDirection : "";
    const alongParent = horizontal ? parentDirection.startsWith("row") : parentDirection.startsWith("column");
    const neighbour = findNeighbour(container, direction, horizontal, alongParent);
    dragRef.current = {
      container, horizontal, parent, neighbour,
      pointerId: event.pointerId,
      start: horizontal ? event.clientX : event.clientY,
      initialSize: horizontal ? rect.width : rect.height,
      // Right and bottom handles grow with the pointer, left and top ones against it
      sign: direction === "right" || direction === "bottom" ? 1 : -1,
      // The flex basis only matters when the parent lays its children out along the axis of the handle
      alongParent,
      size: horizontal ? rect.width : rect.height
    };

    // Prevent the transitions while dragging and keep receiving the events of this pointer (mouse, touch and pen)
    container.classList.add("no-transition");
    event.currentTarget.setPointerCapture?.(event.pointerId);
    event.preventDefault();
    event.stopPropagation();
  };

  // Each handle resizes the dimension of its own axis
  const applySize = (current) => {
    current.container.style[current.horizontal ? "width" : "height"] = `${current.size}px`;
    if (current.alongParent) {
      current.container.style.flexBasis = `${current.size}px`;
    }
  };

  const drag = (event) => {
    const current = dragRef.current;
    if (!current || event.pointerId !== current.pointerId) {
      return;
    }
    const position = current.horizontal ? event.clientX : event.clientY;
    current.size = clamp(current.initialSize + current.sign * (position - current.start), current.parent, current.horizontal);
    const {neighbour} = current;
    let neighbourSize = 0;
    if (neighbour) {
      // The neighbour takes what the container gains, at a fixed size from the first move on (a press without a move
      // leaves it fluid); when it hits its minimum the container stops growing
      neighbourSize = Math.round(Math.max(neighbour.min, neighbour.initialSize - (current.size - current.initialSize)));
      current.size = Math.round(current.initialSize + neighbour.initialSize - neighbourSize);
      neighbour.element.style.flex = `0 0 ${neighbourSize}px`;
    }
    applySize(current);
    if (neighbour) {
      // A flex item does not get smaller than its content (min-height: auto, a grid keeps its minimum rows area): when one
      // of the two kept more than it was given, the other one gives that back, so the outer edges do not move
      const dimension = current.horizontal ? "width" : "height";
      const total = Math.round(current.initialSize + neighbour.initialSize);
      const keptByNeighbour = Math.round(neighbour.element.getBoundingClientRect()[dimension]);
      const keptByContainer = Math.round(current.container.getBoundingClientRect()[dimension]);
      if (keptByNeighbour > neighbourSize) {
        current.size = total - keptByNeighbour;
        neighbour.element.style.flex = `0 0 ${keptByNeighbour}px`;
        applySize(current);
      } else if (keptByContainer > current.size) {
        current.size = keptByContainer;
        neighbour.element.style.flex = `0 0 ${total - keptByContainer}px`;
        applySize(current);
      }
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
