import {
  getContextMenuAnchor, hasContextMenu, registerContextMenu, registerContextMenuAnchor, showContextMenuUnder
} from "../../../src/utilities/contextMenuRegistry";

describe("awe-react-client/test/js/utilities/contextMenuRegistryTest.js", () => {
  const owner = {view: "report", component: "grid"};
  const cell = {...owner, row: "3", column: "btn"};

  it("finds the menu of an owner by view and component and opens it under an anchor", () => {
    const show = jest.fn(() => true);
    const unregister = registerContextMenu(owner, {show, canOpen: () => true});
    const anchor = document.createElement("button");

    expect(hasContextMenu(cell)).toBe(true);
    expect(showContextMenuUnder(cell, anchor)).toBe(true);
    expect(show).toHaveBeenCalledWith(anchor);

    unregister();
    expect(hasContextMenu(owner)).toBe(false);
    expect(showContextMenuUnder(owner, anchor)).toBe(false);
  });

  it("does not open a menu that cannot open", () => {
    const show = jest.fn();
    const unregister = registerContextMenu(owner, {show, canOpen: () => false});

    expect(hasContextMenu(owner)).toBe(false);
    expect(showContextMenuUnder(owner, document.createElement("button"))).toBe(false);
    expect(show).not.toHaveBeenCalled();
    unregister();
  });

  it("keeps the anchors of two cells of the same grid apart", () => {
    const first = document.createElement("button");
    const second = document.createElement("button");
    const off1 = registerContextMenuAnchor(cell, () => first);
    const off2 = registerContextMenuAnchor({...cell, row: "4"}, () => second);

    expect(getContextMenuAnchor(cell)).toBe(first);
    expect(getContextMenuAnchor({...cell, row: "4"})).toBe(second);

    off1();
    expect(getContextMenuAnchor(cell)).toBeNull();
    off2();
  });

  it("does not remove the registration of a newer owner when an older one unregisters", () => {
    const old = registerContextMenu(owner, {show: jest.fn(), canOpen: () => true});
    const newer = registerContextMenu(owner, {show: jest.fn(), canOpen: () => true});

    old();

    expect(hasContextMenu(owner)).toBe(true);
    newer();
  });

  it("ignores addresses without a component", () => {
    expect(registerContextMenu({}, {show: jest.fn(), canOpen: () => true})()).toBeNull();
    expect(hasContextMenu(undefined)).toBe(false);
  });
});
