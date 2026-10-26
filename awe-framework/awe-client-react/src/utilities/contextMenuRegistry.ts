/**
 * Registry that lets the show-context-menu client action reach the context menus and the buttons on screen.
 * An action carries no DOM node and no mouse event, so the components that own a context menu and the buttons that
 * can launch the action register themselves here, and the action looks them up by address.
 */
/**
 * Address of a component, or of a cell of a grid (a row and a column)
 */
export interface ContextMenuAddress {
  view?: string;
  component?: string;
  row?: string | number | null;
  column?: string | number | null;
}

/**
 * Context menu of a component: opens under an element and tells if it can be opened
 */
export interface ContextMenuHandle {
  show: (anchor: Element) => boolean;
  canOpen: () => boolean;
}

/**
 * Function that removes a registration
 */
type Unregister = () => void;

const menus = new Map<string | null, ContextMenuHandle>();
const anchors = new Map<string | null, () => Element | null>();

const valid = (value: unknown): boolean => value !== undefined && value !== null;

/**
 * Key of the component that owns a menu: a cell address (row and column) belongs to its grid
 * @param {object} address Address
 * @returns {string|null} Key, or null when the address has no component
 */
const ownerKey = (address: ContextMenuAddress | null | undefined): string | null => address?.component ? `${address.view}|${address.component}` : null;

/**
 * Key of one button: the cell is part of it, as every row of a grid has its own button
 * @param {object} address Address
 * @returns {string|null} Key, or null when the address has no component
 */
const anchorKey = (address: ContextMenuAddress | null | undefined): string | null => address?.component
  ? `${ownerKey(address)}|${valid(address.row) ? address.row : ""}|${valid(address.column) ? address.column : ""}`
  : null;

const register = <T>(registry: Map<string | null, T>, key: string | null, value: T): Unregister => {
  if (key === null) {
    return () => null;
  }
  registry.set(key, value);
  // A component that was replaced meanwhile keeps its registration
  return () => {
    if (registry.get(key) === value) {
      registry.delete(key);
    }
  };
};

/**
 * Register the context menu of a component
 * @param {object} address Address of the owner
 * @param {{show: function(Element): boolean, canOpen: function(): boolean}} handle Opens the menu under an element
 * @returns {function} Unregister function
 */
export const registerContextMenu = (address: ContextMenuAddress, handle: ContextMenuHandle): Unregister => register(menus, ownerKey(address), handle);

/**
 * Register the element a button hangs its menu from
 * @param {object} address Address of the button (a cell address for a button of a grid row)
 * @param {function(): Element} getElement Retrieves the element of the button
 * @returns {function} Unregister function
 */
export const registerContextMenuAnchor = (address: ContextMenuAddress, getElement: () => Element | null): Unregister => register(anchors, anchorKey(address), getElement);

/**
 * Retrieve the element of a button
 * @param {object} address Address of the button
 * @returns {Element|null} The element, if the button is on screen
 */
export const getContextMenuAnchor = (address: ContextMenuAddress): Element | null => anchors.get(anchorKey(address))?.() ?? null;

/**
 * Check if the owner of an address has a menu that can be opened
 * @param {object} address Address of the owner (or of a cell of it)
 * @returns {boolean} The menu can be opened
 */
export const hasContextMenu = (address: ContextMenuAddress): boolean => Boolean(menus.get(ownerKey(address))?.canOpen());

/**
 * Open the menu of the owner of an address under an element
 * @param {object} address Address of the owner (or of a cell of it)
 * @param {Element} anchor Element the menu hangs from
 * @returns {boolean} The menu was opened
 */
export const showContextMenuUnder = (address: ContextMenuAddress, anchor: Element): boolean => {
  const menu = menus.get(ownerKey(address));
  return Boolean(menu?.canOpen() && anchor && menu.show(anchor));
};
