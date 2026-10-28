import {useCallback, useEffect, useRef, useState} from "react";
import "../components/Maximize.less";

/** Duration of the grow and restore animations (same as the AngularJS Maximize service) */
export const MAXIMIZE_ANIMATION_TIME = 300;
const MAXIMIZE_DELAY = 200;
const RESTORE_DELAY = 150;
const RESTORE_FADE_TIME = MAXIMIZE_ANIMATION_TIME + 150;
const MAXIMIZE_TARGET = ".maximize-target";

const isVisible = (node) => node.nodeType === 1 && window.getComputedStyle(node).display !== "none";
const visibleSiblings = (node) => Array.from(node.parentElement?.children || []).filter(sibling => sibling !== node && isVisible(sibling));

const getTargetRect = (element) => {
  const target = element.closest(MAXIMIZE_TARGET);
  if (!target) {
    return {top: 0, left: 0, width: window.innerWidth, height: window.innerHeight};
  }
  const {top, left, width, height} = target.getBoundingClientRect();
  return {top, left, width, height};
};

const getRect = (element) => {
  const {top, left, width, height} = element.getBoundingClientRect();
  return {top, left, width, height};
};

const applyRect = (node, rect) => Object.assign(node.style, {
  top: `${rect.top}px`, left: `${rect.left}px`, width: `${rect.width}px`, height: `${rect.height}px`
});

// The target as it is once the element is maximized: without the minimized nodes and with the element out of the flow.
// Measured before, a content taller than the screen would make the target, and the maximized element, taller than it
// The classes are only taken back from the nodes that did not have them, and the layout is put back even if it fails
const getMaximizedTargetRect = (element, {targets, parents}) => {
  const {position} = element.style;
  const added = [[targets, "minimized"], [parents, "maximizeParent"]].flatMap(([nodes, className]) =>
    nodes.filter(node => !node.classList.contains(className)).map(node => [node, className]));
  added.forEach(([node, className]) => node.classList.add(className));
  element.style.position = "fixed";
  try {
    return getTargetRect(element);
  } finally {
    element.style.position = position;
    added.forEach(([node, className]) => node.classList.remove(className));
  }
};

// Layer that grows from the initial size to the final one while the real element waits hidden
const createClone = (rect) => {
  const clone = document.createElement("div");
  clone.className = "maximize-clone";
  applyRect(clone, rect);
  document.body.appendChild(clone);
  return clone;
};

const animateClone = (clone, rect) => {
  // Reading the layout makes the browser apply the initial size before the transition
  clone.getBoundingClientRect();
  clone.style.transitionDuration = `${MAXIMIZE_ANIMATION_TIME}ms`;
  clone.classList.add("maximize-animated");
  applyRect(clone, rect);
};

// The siblings of the element and the siblings of its parents (up to the maximize target) are minimized
const getElementsToMinimize = (element) => {
  const target = element.closest(MAXIMIZE_TARGET);
  const parents = [];
  let targets = visibleSiblings(element);
  for (let parent = element.parentElement; parent && parent !== target; parent = parent.parentElement) {
    targets = targets.concat(visibleSiblings(parent));
    parents.push(parent);
  }
  return {targets, parents};
};

/**
 * Maximize an element over its closest `.maximize-target`, like the AngularJS Maximize service does with windows and tabs:
 * a clone grows over the target, the siblings fade out (`minimized`), the parents lose their positioning (`maximizeParent`)
 * and both are undone on restore. The element is fixed over the target while it is maximized and follows its size.
 * Reusable by any panel (window, tab...): put `ref` on the element, and `style` and `className` (maximized, maximize-resizing) in its props.
 *
 * @param {object} options Options: onResize, called when the size of the element changes (end of the animations)
 * @returns {{ref, maximized, iconMaximized, resizing, style, className, toggle}}
 */
export default function useMaximize({onResize} = {}) {
  const ref = useRef(null);
  const [maximized, setMaximized] = useState(false);
  const [iconMaximized, setIconMaximized] = useState(false);
  const [resizing, setResizing] = useState(false);
  const [bounds, setBounds] = useState(null);
  const minimize = useRef({targets: [], parents: []});
  const timers = useRef([]);
  const clone = useRef(null);
  const busy = useRef(false);
  const onResizeRef = useRef(onResize);
  onResizeRef.current = onResize;

  const later = useCallback((callback, time) => {
    const id = setTimeout(() => {
      timers.current = timers.current.filter(timer => timer !== id);
      callback();
    }, time);
    timers.current.push(id);
  }, []);

  const removeClone = useCallback(() => {
    clone.current?.remove();
    clone.current = null;
  }, []);

  const finish = useCallback((onEnd) => {
    removeClone();
    onEnd();
    busy.current = false;
    setResizing(false);
    onResizeRef.current && onResizeRef.current();
  }, [removeClone]);

  // End of the maximize animation: the grid takes its final bounds and the minimized nodes stay hidden by class
  const settleMaximized = useCallback((targetRect) => {
    minimize.current.parents.forEach(node => node.classList.add("maximizeParent"));
    minimize.current.targets.forEach(node => {
      node.classList.add("minimized");
      node.style.removeProperty("transition");
      node.style.removeProperty("opacity");
    });
    setBounds(targetRect);
    setMaximized(true);
    setIconMaximized(true);
  }, []);

  const maximize = useCallback((element) => {
    clone.current = createClone(getRect(element));
    setResizing(true);
    later(() => {
      minimize.current = getElementsToMinimize(element);
      const targetRect = getMaximizedTargetRect(element, minimize.current);
      minimize.current.targets.forEach(node => {
        node.style.transition = `opacity ${MAXIMIZE_ANIMATION_TIME}ms`;
        node.style.opacity = "0";
      });
      animateClone(clone.current, targetRect);
      later(() => finish(() => settleMaximized(targetRect)), MAXIMIZE_ANIMATION_TIME);
    }, MAXIMIZE_DELAY);
  }, [later, finish, settleMaximized]);

  const restore = useCallback((element) => {
    clone.current = createClone(getRect(element));
    setResizing(true);
    const {targets, parents} = minimize.current;
    parents.forEach(node => node.classList.remove("maximizeParent"));
    targets.forEach(node => {
      node.classList.remove("minimized");
      node.style.opacity = "0";
      later(() => {
        node.style.transition = `opacity ${RESTORE_FADE_TIME}ms`;
        node.style.opacity = "1";
      }, 0);
      later(() => {
        node.style.removeProperty("transition");
        node.style.removeProperty("opacity");
      }, RESTORE_FADE_TIME);
    });
    setBounds(null);
    setMaximized(false);
    later(() => {
      // The element is back in the layout: the clone shrinks to it
      animateClone(clone.current, getRect(element));
      later(() => finish(() => setIconMaximized(false)), MAXIMIZE_ANIMATION_TIME);
    }, RESTORE_DELAY);
  }, [later, finish]);

  const toggle = useCallback(() => {
    const element = ref.current;
    if (!element || busy.current) {
      return;
    }
    busy.current = true;
    if (maximized) {
      restore(element);
    } else {
      maximize(element);
    }
  }, [maximized, maximize, restore]);

  // A maximized element keeps filling the target when the window changes
  useEffect(() => {
    if (!maximized) {
      return undefined;
    }
    const update = () => ref.current && setBounds(getTargetRect(ref.current));
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, [maximized]);

  // Nothing is left behind when the element goes away in the middle of the animation
  useEffect(() => () => {
    timers.current.forEach(clearTimeout);
    removeClone();
    const {targets, parents} = minimize.current;
    parents.forEach(node => node.classList.remove("maximizeParent"));
    targets.forEach(node => {
      node.classList.remove("minimized");
      node.style.removeProperty("transition");
      node.style.removeProperty("opacity");
    });
  }, [removeClone]);

  const style = maximized && bounds ? {position: "fixed", ...bounds} : undefined;
  const className = [maximized && "maximized", resizing && "maximize-resizing"].filter(Boolean).join(" ");
  return {ref, maximized, iconMaximized, resizing, style, className, toggle};
}
