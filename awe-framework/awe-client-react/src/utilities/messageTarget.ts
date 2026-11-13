import type {ComponentAddress} from "../contract";

/**
 * Address of a target message: the view and the component, and the row and the column for the cell of a grid
 */
export type MessageTargetAddress = Omit<ComponentAddress, "row" | "column"> & {
  row?: string | number | null;
  column?: string | number | null;
};

/**
 * Quote a value for an attribute selector. Line breaks are not allowed inside a CSS string: they go as hex escapes.
 * @param {string} value Value
 * @return {string} Quoted value
 */
const quote = (value: unknown): string => `"${String(value)
  .replace(/["\\]/g, "\\$&")
  .replace(/[\n\r\f]/g, (character) => `\\${character.charCodeAt(0).toString(16)} `)}"`;

/**
 * Find the element of a component in a view. The views (and the dialogs, rendered outside them) carry a "view-id"
 * marker: the component belongs to the closest one. An element outside any marker belongs to every view.
 * @param {string} view View name
 * @param {string} selector Component selector
 * @return {Element|null} Component element
 */
function findInView(view: string, selector: string): Element | null {
  const elements = Array.from(document.querySelectorAll(selector));
  const viewOf = (element: Element) => element.closest("[view-id]")?.getAttribute("view-id");
  return elements.find((element) => viewOf(element) === view) ||
    elements.find((element) => viewOf(element) === undefined) || null;
}

/**
 * Find the element a target message points at, as the AngularJS client does with the callback target of the action:
 * the cell of a grid when the address has a row and a column, otherwise the component (criterion, grid, tree grid or
 * any element with that id) of the view of the address
 * @param {object} address Target address (view, component and, for a cell, row and column)
 * @return {Element|null} Target element, null when the address has no component or it is not rendered
 */
export function findMessageTarget(address: MessageTargetAddress | null | undefined): Element | null {
  const {view, component, row, column} = address || {};
  if (!view || !component) {
    return null;
  }
  const id = quote(component);
  const element = findInView(view, `[criterion-id=${id}], [grid-id=${id}], [tree-grid-id=${id}], [id=${id}]`);
  if (element && row !== undefined && row !== null && column !== undefined && column !== null) {
    const cell = `[row-id=${quote(row)}][column-id=${quote(column)}]`;
    const cellInRow = `[row-id=${quote(row)}] [column-id=${quote(column)}]`;
    return element.querySelector(`${cell}, ${cellInRow}`) || element;
  }
  return element;
}
