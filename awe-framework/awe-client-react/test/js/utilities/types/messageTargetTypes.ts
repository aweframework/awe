// Type-level assertions for utilities/messageTarget, checked by `npm run typecheck` (not run by Jest)
import {findMessageTarget, MessageTargetAddress} from "../../../../src/utilities/messageTarget";

// The address of a component, or of a cell of a grid (the row and the column may be numbers)
const component: MessageTargetAddress = {view: "base", component: "Txt"};
const cell: MessageTargetAddress = {view: "base", component: "Grd", row: 3, column: "name"};
const nullCell: MessageTargetAddress = {view: "base", component: "Grd", row: null, column: null};
// @ts-expect-error the view is text
const wrongView: MessageTargetAddress = {view: 1};

// The element is null when there is no target
const element: Element | null = findMessageTarget(component);
const noAddress: Element | null = findMessageTarget(null);
const missingAddress: Element | null = findMessageTarget(undefined);
// @ts-expect-error the target can be null
const alwaysElement: Element = findMessageTarget(cell);
// @ts-expect-error an address is an object
findMessageTarget("Txt");

export {component, cell, nullCell, wrongView, element, noAddress, missingAddress, alwaysElement};
