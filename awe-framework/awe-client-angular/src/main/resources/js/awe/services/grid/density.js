// Sizes of the AngularJS grids from the AWE 5 density tokens (#885). ui-grid places its rows by a row height in pixels,
// so the default one is read from the tokens of <html> (one line of base text plus the vertical cell padding, as the
// React grid) and read again when the density changes. The previous fixed sizes stay when the tokens are missing

// Row height and tree indent before the density tokens
export const DEFAULT_ROW_HEIGHT = 27;
export const DEFAULT_TREE_INDENT = 10;

const DENSITY_ATTRIBUTE = "data-awe-density";

/**
 * Read a token of an element as a number (pixels, or a unitless factor)
 * @param {CSSStyleDeclaration} style Computed style
 * @param {string} token Token name
 * @returns {number} Value (NaN when the token is missing)
 */
const tokenValue = (style, token) => parseFloat(style.getPropertyValue(token));

/**
 * Retrieve the row height that the screen sets on a grid
 * @param {object} controller Grid controller
 * @returns {number|null} Row height in pixels, null when the screen does not set it
 */
export function getExplicitRowHeight(controller = {}) {
  return parseInt(controller.rowHeight, 10) || null;
}

/**
 * Retrieve the row height of the current density: one line of --awe-line-height-base text plus twice
 * --awe-grid-cell-padding-y
 * @param {HTMLElement} root Element with the tokens (<html>)
 * @returns {number} Row height in pixels
 */
export function getDensityRowHeight(root = document.documentElement) {
  const style = getComputedStyle(root);
  const fontSize = tokenValue(style, "--awe-font-size-base");
  const lineHeight = style.getPropertyValue("--awe-line-height-base").trim();
  const line = lineHeight.endsWith("px") ? parseFloat(lineHeight) : parseFloat(lineHeight) * fontSize;
  const height = Math.round(line + 2 * tokenValue(style, "--awe-grid-cell-padding-y"));
  return height > 0 ? height : DEFAULT_ROW_HEIGHT;
}

/**
 * Retrieve the row height of a grid: the one of the screen, or the one of the density
 * @param {object} controller Grid controller
 * @returns {number} Row height in pixels
 */
export function getGridRowHeight(controller) {
  return getExplicitRowHeight(controller) || getDensityRowHeight();
}

/**
 * Retrieve the indent of each level of a tree grid (--awe-space-6)
 * @param {HTMLElement} root Element with the tokens (<html>)
 * @returns {number} Indent in pixels
 */
export function getTreeIndent(root = document.documentElement) {
  const indent = tokenValue(getComputedStyle(root), "--awe-space-6");
  return indent > 0 ? indent : DEFAULT_TREE_INDENT;
}

/**
 * Call back when the density of an element (data-awe-density of <html>) changes
 * @param {function} onChange Callback
 * @param {HTMLElement} root Observed element
 * @returns {function} Stop observing
 */
export function observeDensity(onChange, root = document.documentElement) {
  if (typeof MutationObserver === "undefined") {
    return () => undefined;
  }
  let previous = root.getAttribute(DENSITY_ATTRIBUTE);
  const observer = new MutationObserver(() => {
    const current = root.getAttribute(DENSITY_ATTRIBUTE);
    if (current !== previous) {
      previous = current;
      onChange();
    }
  });
  observer.observe(root, {attributes: true, attributeFilter: [DENSITY_ATTRIBUTE]});
  return () => observer.disconnect();
}
