// Type-level assertions for utilities/contextMenuRegistry, checked by `npm run typecheck` (not run by Jest)
import {
  ContextMenuAddress,
  ContextMenuHandle,
  getContextMenuAnchor,
  hasContextMenu,
  registerContextMenu,
  registerContextMenuAnchor,
  showContextMenuUnder
} from "../../../../src/utilities/contextMenuRegistry";

// The address of a component, or of a cell of a grid (the row and the column may be numbers)
const address: ContextMenuAddress = {view: "base", component: "Grd", row: 2, column: "name"};
const nullCell: ContextMenuAddress = {view: "base", component: "Grd", row: null, column: null};
// @ts-expect-error the component is text
const wrongAddress: ContextMenuAddress = {component: 1};

// A menu says if it can be opened and opens under an element
const handle: ContextMenuHandle = {show: (anchor: Element) => anchor.isConnected, canOpen: () => true};
// @ts-expect-error a menu must tell if it can be opened
const notHandle: ContextMenuHandle = {show: () => true};

// Registering answers with the function that unregisters
const unregisterMenu: () => void = registerContextMenu(address, handle);
const unregisterAnchor: () => void = registerContextMenuAnchor(address, () => document.body);
const nullAnchor: () => void = registerContextMenuAnchor(address, () => null);
// @ts-expect-error the address is an object
registerContextMenu("Grd", handle);
// @ts-expect-error the element of the button comes from a function
registerContextMenuAnchor(address, document.body);

// The element of a button is null when it is not on screen
const anchor: Element | null = getContextMenuAnchor(address);
// @ts-expect-error the element can be null
const alwaysAnchor: Element = getContextMenuAnchor(address);
const can: boolean = hasContextMenu(address);
const shown: boolean = showContextMenuUnder(address, document.body);
// @ts-expect-error the answer is a boolean
const shownText: string = showContextMenuUnder(address, document.body);

export {nullCell, wrongAddress, notHandle, unregisterMenu, unregisterAnchor, nullAnchor, anchor, alwaysAnchor, can, shown, shownText};
