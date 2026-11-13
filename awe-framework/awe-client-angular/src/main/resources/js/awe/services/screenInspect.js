import {revealHiddenColumns} from "awe-client-react/utilities/screenPersonalization";

/** Inspect mode of the visual screen personalization editor (#792): the real screen drawn with every element selectable */

// The view the screen under edit is loaded in
export const INSPECT_VIEW = "personalizer";

/**
 * Show what the screen hides so it can be selected: hidden elements are made visible and the hidden columns of a grid
 * are drawn narrow. The caller draws them dimmed, so nothing stored changes
 * @param {Array} components Components of the screen data ({id, controller})
 * @param {Set<string>} configurable Identifiers of the elements that can be personalized
 * @returns {{hidden: Set<string>, hiddenColumns: Set<string>}} What was hidden
 */
export function revealHiddenElements(components, configurable) {
  const hidden = new Set();
  const hiddenColumns = new Set();
  components.filter(component => configurable.has(component.id)).forEach(({id, controller}) => {
    if (controller.visible === false) {
      controller.visible = true;
      hidden.add(id);
    }
    if (Array.isArray(controller.columnModel)) {
      const revealed = revealHiddenColumns(controller.columnModel);
      controller.columnModel = revealed.columnModel;
      revealed.hidden.forEach(name => hiddenColumns.add(name));
    }
  });
  return {hidden, hiddenColumns};
}

// A component node carries its identifier as <type>-id (criterion-id, button-id...) or id; a column cell as column-id
const identifiersOf = node => [...node.attributes]
  .filter(({name}) => name === "id" || (name.endsWith("-id") && name !== "column-id" && !name.endsWith("testid-owner") && name !== "data-testid"))
  .map(({value}) => value);

const STATE_CLASSES = ["awe-inspect-node", "awe-inspect-column", "awe-inspect-selected", "awe-inspect-hidden", "awe-inspect-personalized"];

/**
 * Mark the elements of the rendered screen so the styles draw them: selectable, selected, hidden and personalized
 * @param {Element} root Rendered screen
 * @param {{configurable: Set<string>, selected: string|null, personalized: Set<string>, hidden: Set<string>, hiddenColumns: Set<string>}} state Inspect state
 */
export function markInspect(root, {configurable, selected, personalized, hidden, hiddenColumns}) {
  const claimed = new Map();
  root.querySelectorAll("*").forEach(node => {
    const column = node.getAttribute("column-id");
    const id = column !== null ? column : identifiersOf(node).find(identifier => configurable.has(identifier) && ![...claimed.values()].includes(identifier));
    if (id !== undefined && configurable.has(id) && (column !== null || !claimed.has(node))) {
      claimed.set(node, id);
    }
  });
  root.querySelectorAll(`.${STATE_CLASSES[0]}, .${STATE_CLASSES[1]}`).forEach(node => {
    if (!claimed.has(node)) {
      node.classList.remove(...STATE_CLASSES);
    }
  });
  claimed.forEach((id, node) => {
    const isColumn = node.hasAttribute("column-id");
    node.classList.add(isColumn ? "awe-inspect-column" : "awe-inspect-node");
    node.classList.toggle("awe-inspect-selected", selected === id);
    node.classList.toggle("awe-inspect-hidden", (isColumn ? hiddenColumns : hidden).has(id));
    node.classList.toggle("awe-inspect-personalized", personalized.has(id));
  });
}

/**
 * Configurable element under a click: the nearest column cell or component of the rendered screen
 * @param {Element} root Rendered screen
 * @param {Element} target Clicked node
 * @param {Set<string>} configurable Identifiers of the elements that can be personalized
 * @returns {string|null} Element identifier, or null when nothing configurable is under the click
 */
export function elementIdAt(root, target, configurable) {
  for (let node = target; node && node !== root; node = node.parentElement) {
    const column = node.getAttribute("column-id");
    const id = column !== null && configurable.has(column) ? column : identifiersOf(node).find(identifier => configurable.has(identifier));
    if (id !== undefined) {
      return id;
    }
  }
  return null;
}

// Events the elements react to: an element that is selected never sees them (a button arms a pending click on mousedown and runs
// its actions later, ui-grid sorts on click, a focused button clicks on Enter or Space)
const SWALLOWED_EVENTS = ["pointerdown", "mousedown", "mouseup", "click", "dblclick"];
const SELECT_KEYS = ["Enter", " "];

/**
 * Make the rendered screen inert and selectable: the pointer events and Enter or Space never reach its elements, a click
 * (or Enter or Space) selects the configurable element under it, and a click where there is none does nothing
 * @param {Element} root Rendered screen
 * @param {function(): Set<string>} configurable Identifiers of the elements that can be personalized (read at each event)
 * @param {function(string): void} onSelect Called with the identifier of the element to select
 * @returns {function(): void} Remove the listeners
 */
export function swallowInspectEvents(root, configurable, onSelect) {
  const stop = event => {
    event.preventDefault();
    event.stopPropagation();
  };
  const select = event => {
    const id = elementIdAt(root, event.target, configurable());
    if (id !== null) {
      onSelect(id);
    }
  };
  const onPointer = event => {
    stop(event);
    if (event.type === "click") {
      select(event);
    }
  };
  const onKey = event => {
    if (SELECT_KEYS.includes(event.key)) {
      stop(event);
      if (event.type === "keydown") {
        select(event);
      }
    }
  };
  SWALLOWED_EVENTS.forEach(type => root.addEventListener(type, onPointer, true));
  ["keydown", "keyup"].forEach(type => root.addEventListener(type, onKey, true));
  return () => {
    SWALLOWED_EVENTS.forEach(type => root.removeEventListener(type, onPointer, true));
    ["keydown", "keyup"].forEach(type => root.removeEventListener(type, onKey, true));
  };
}
