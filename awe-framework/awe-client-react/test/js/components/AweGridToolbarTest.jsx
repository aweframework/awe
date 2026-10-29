import React from "react";
import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";

import i18n from "../i18nForTests";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { ADD_ACTIONS_TOP } from "../../../src/redux/actions/actions";
import { ButtonTypes, updateModel } from "../../../src/redux/actions/components";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

const hook = (id) => `[data-testid="${id}"]`;
const byHook = (id, root = document) => root.querySelectorAll(hook(id));

const column = (name, extra = {}) => ({ name, label: name.toUpperCase(), sortField: name, ...extra });

function gridState(attributes = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
    components: {
      grid: {
        address: { component: "grid", view: "report" },
        model: { values: [{ id: 1, a: "a1", b: "b1", c: "c1", d: "d1" }], records: 1 },
        attributes: {
          screenId: "Scr",
          columnModel: [column("a"), column("b"), column("c"), column("d")],
          headerModel: [],
          buttonModel: [],
          visible: true,
          ...attributes
        },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const headerNames = () => screen.getAllByRole("columnheader").map(header => header.textContent.trim()).filter(name => name && name !== "#");

const openDialog = async () => {
  fireEvent.click(document.querySelector(hook("grid-columns-button")));
  return await waitFor(() => {
    const dialog = document.querySelector(hook("grid-columns-dialog"));
    expect(dialog).not.toBeNull();
    return dialog;
  });
};

// The parts of the toolbar with a test hook, in their order (and its spacer, when asked)
const toolbarParts = (toolbar, withSpacer = false) => Array.from(toolbar.children)
  .map(child => child.getAttribute("data-testid") || (withSpacer && child.classList.contains("grid-toolbar-spacer") ? "spacer" : null))
  .filter(Boolean);

const itemOf = (dialog, name) => dialog.querySelector(`${hook("grid-column-item")}[column-id="${name}"]`);
const itemNames = (dialog) => Array.from(byHook("grid-column-item", dialog)).map(item => item.getAttribute("column-id"));

describe("awe-react-client/test/js/components/AweGridToolbarTest.jsx", () => {
  beforeEach(() => {
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]), text: () => Promise.resolve("[]") }));
  });

  it("shows the toolbar with the Columns and Maximize buttons by default", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });

    expect(document.querySelector(hook("grid-toolbar"))).not.toBeNull();
    expect(document.querySelector(hook("grid-columns-button"))).not.toBeNull();
    expect(document.querySelector(hook("grid-maximize-button"))).not.toBeNull();
  });

  it("does not show the toolbar when the grid opts out", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ showToolbar: false }) });

    expect(document.querySelector(hook("grid-toolbar"))).toBeNull();
  });

  const withQuickFilter = (attributes = {}) => {
    const state = gridState({ quickFilter: "CrtSta", ...attributes });
    state.components.CrtSta = {
      address: { component: "CrtSta", view: "report" },
      model: { values: [{ value: "", label: "All", count: 2, selected: true }, { value: "LCK", label: "Locked", count: 1 }] },
      attributes: { id: "CrtSta", component: "quick-filter", visible: true },
      specificAttributes: {}
    };
    return state;
  };

  it("shows the quick filters of the grid first in the toolbar, before Columns and Maximize", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: withQuickFilter() });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    const parts = toolbarParts(toolbar);
    expect(parts.slice(0, 3)).toEqual(["grid-quick-filter", "grid-columns-button", "grid-maximize-button"]);
    expect(toolbar.querySelector(hook("grid-quick-filter")).getAttribute("data-testid-owner")).toBe("grid");
    expect(byHook("grid-quick-filter-option", toolbar).length).toBe(2);
  });

  it("shows the toolbar with only its quick filters when the grid opts out of the toolbar", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: withQuickFilter({ showToolbar: false }) });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    expect(toolbar).not.toBeNull();
    expect(toolbar.querySelector(hook("grid-quick-filter"))).not.toBeNull();
    expect(toolbar.querySelector(hook("grid-columns-button"))).toBeNull();
    expect(toolbar.querySelector(hook("grid-maximize-button"))).toBeNull();
  });

  const withSearch = (attributes = {}) => {
    const state = withQuickFilter({ search: "CrtTxt", ...attributes });
    state.components.CrtTxt = {
      address: { component: "CrtTxt", view: "report" },
      model: { values: [] },
      attributes: { id: "CrtTxt", component: "grid-search", visible: true },
      specificAttributes: {}
    };
    return state;
  };

  it("shows the search of the grid first in the toolbar, before the quick filters, Columns and Maximize", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: withSearch() });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    const parts = toolbarParts(toolbar);
    expect(parts.slice(0, 4)).toEqual(["grid-search", "grid-quick-filter", "grid-columns-button", "grid-maximize-button"]);
    expect(toolbar.querySelector(hook("grid-search")).getAttribute("data-testid-owner")).toBe("grid");
  });

  it("shows the row grouping controls after the search and the quick filters, then a spacer, Columns and Maximize", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: withSearch({ loadAll: true, groupBy: "a", groupSelector: true }) });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    expect(toolbarParts(toolbar, true)).toEqual(["grid-search", "grid-quick-filter", "grid-group-expand-all",
      "grid-group-collapse-all", "grid-group-by", "spacer", "grid-columns-button", "grid-maximize-button"]);
  });

  it("hides the row grouping controls with Columns and Maximize when the grid opts out of the toolbar", () => {
    renderWithProviders(<AweGrid id="grid" />,
      { preloadedState: withSearch({ showToolbar: false, quickFilter: undefined, loadAll: true, groupBy: "a", groupSelector: true }) });

    expect(toolbarParts(document.querySelector(hook("grid-toolbar")), true)).toEqual(["grid-search"]);
  });

  it("shows the toolbar with only its search when the grid opts out of the toolbar", () => {
    const state = withSearch({ showToolbar: false, quickFilter: undefined });
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    expect(toolbar).not.toBeNull();
    expect(toolbar.querySelector(hook("grid-search"))).not.toBeNull();
    expect(toolbar.querySelector(hook("grid-quick-filter"))).toBeNull();
    expect(toolbar.querySelector(hook("grid-columns-button"))).toBeNull();
  });

  it("shows the toolbar of a grid whose visibility is not defined", () => {
    const state = gridState();
    delete state.components.grid.attributes.visible;
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });

    expect(document.querySelector(hook("grid-toolbar"))).not.toBeNull();
  });

  it("does not show the toolbar of a grid that is hidden", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ visible: false }) });

    expect(document.querySelector(hook("grid-toolbar"))).toBeNull();
  });

  it("lists the visible columns of the screen with their state and a counter, without the hidden ones", async () => {
    renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: gridState({ columnModel: [column("a"), column("key", { hidden: true }), column("b"), column("c")] })
    });

    const dialog = await openDialog();

    expect(itemNames(dialog)).toEqual(["a", "b", "c"]);
    expect(dialog.querySelector(hook("grid-columns-counter")).getAttribute("data-visible")).toBe("3");
    expect(dialog.querySelector(hook("grid-columns-counter")).getAttribute("data-total")).toBe("3");
  });

  it("does not list the button columns: they are actions and stay where the screen puts them (#793)", async () => {
    renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: gridState({
        columnModel: [column("a"), column("b"), column("edit", { component: "button" }), column("c")],
        columnLayout: { order: ["edit", "c", "b", "a"], hidden: ["edit", "b"] }
      })
    });
    expect(headerNames()).toEqual(["C", "EDIT", "A"]);

    const dialog = await openDialog();

    expect(itemNames(dialog)).toEqual(["c", "b", "a"]);
    expect(dialog.querySelector(hook("grid-columns-counter")).getAttribute("data-visible")).toBe("2");
    expect(dialog.querySelector(hook("grid-columns-counter")).getAttribute("data-total")).toBe("3");
  });

  it("hides a column from the grid when it is unchecked and shows it again when it is checked", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    expect(headerNames()).toEqual(["A", "B", "C", "D"]);

    const dialog = await openDialog();
    fireEvent.click(itemOf(dialog, "b").querySelector(hook("grid-column-checkbox")));

    expect(headerNames()).toEqual(["A", "C", "D"]);
    expect(dialog.querySelector(hook("grid-columns-counter")).getAttribute("data-visible")).toBe("3");
    expect(dialog.querySelector(hook("grid-columns-counter")).getAttribute("data-total")).toBe("4");

    fireEvent.click(itemOf(dialog, "b").querySelector(hook("grid-column-checkbox")));
    expect(headerNames()).toEqual(["A", "B", "C", "D"]);
  });

  it("does not let the user hide the last visible column", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnModel: [column("a"), column("b")] }) });
    const dialog = await openDialog();

    fireEvent.click(itemOf(dialog, "a").querySelector(hook("grid-column-checkbox")));
    fireEvent.click(itemOf(dialog, "b").querySelector(hook("grid-column-checkbox")));

    expect(headerNames()).toEqual(["B"]);
  });

  it("moves a column with the arrows", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    const dialog = await openDialog();

    fireEvent.click(itemOf(dialog, "c").querySelector(hook("grid-column-up")));

    expect(headerNames()).toEqual(["A", "C", "B", "D"]);
    expect(itemNames(dialog)).toEqual(["a", "c", "b", "d"]);

    fireEvent.click(itemOf(dialog, "a").querySelector(hook("grid-column-down")));
    expect(headerNames()).toEqual(["C", "A", "B", "D"]);
  });

  it("moves a column dragging its handle onto a sibling", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    const dialog = await openDialog();
    const dataTransfer = { setData: jest.fn(), getData: () => "d", effectAllowed: "", dropEffect: "" };

    fireEvent.dragStart(itemOf(dialog, "d").querySelector(hook("grid-column-handle")), { dataTransfer });
    fireEvent.dragOver(itemOf(dialog, "a"), { dataTransfer });
    fireEvent.drop(itemOf(dialog, "a"), { dataTransfer });

    expect(headerNames()).toEqual(["D", "A", "B", "C"]);
  });

  it("disables the arrows that cannot move a column", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    const dialog = await openDialog();

    expect(itemOf(dialog, "a").querySelector(hook("grid-column-up")).disabled).toBe(true);
    expect(itemOf(dialog, "d").querySelector(hook("grid-column-down")).disabled).toBe(true);
    expect(itemOf(dialog, "b").querySelector(hook("grid-column-up")).disabled).toBe(false);
  });

  it("does not offer Show all any more", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    const dialog = await openDialog();

    expect(dialog.querySelector(hook("grid-columns-show-all"))).toBeNull();
  });

  it("mutes the label of a hidden column", async () => {
    renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: gridState({ columnLayout: { order: ["a", "b", "c", "d"], hidden: ["b"] } })
    });
    const dialog = await openDialog();

    expect(itemOf(dialog, "b").classList.contains("is-hidden")).toBe(true);
    expect(itemOf(dialog, "a").classList.contains("is-hidden")).toBe(false);
  });

  it("applies the layout that comes from the server", () => {
    renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: gridState({ columnLayout: { order: ["d", "c", "b", "a"], hidden: ["c"] } })
    });

    expect(headerNames()).toEqual(["D", "B", "A"]);
  });

  describe("group headers", () => {
    const grouped = {
      columnModel: [column("a"), column("b"), column("c"), column("d")],
      headerModel: [{ startColumnName: "b", numberOfColumns: 2, label: "GRP", columnNames: ["b", "c"] }]
    };
    const spans = () => Array.from(document.querySelectorAll("thead th[colspan]")).map(th => `${th.textContent.trim()}:${th.getAttribute("colspan")}`);

    it("renders the layout of the user: order, hidden columns and the span of the group headers", () => {
      renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState({ ...grouped, columnLayout: { order: ["d", "c", "b", "a"], hidden: ["a"] } })
      });

      // d takes the first slot of the ungrouped columns, b and c swap inside their group, a is hidden
      expect(headerNames()).toEqual(["D", "GRP", "C", "B"]);
      expect(spans()).toEqual(["GRP:2"]);
    });

    it("keeps the header over the columns that are left when the layout hides the first of the group", () => {
      renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState({ ...grouped, columnLayout: { order: ["a", "b", "c", "d"], hidden: ["b"] } })
      });

      expect(spans()).toEqual(["GRP:1"]);
    });

    it("spans the header over the visible columns of the group", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState(grouped) });
      expect(spans()).toEqual(["GRP:2"]);

      const dialog = await openDialog();
      fireEvent.click(itemOf(dialog, "b").querySelector(hook("grid-column-checkbox")));
      expect(spans()).toEqual(["GRP:1"]);

      fireEvent.click(itemOf(dialog, "c").querySelector(hook("grid-column-checkbox")));
      expect(spans()).toEqual([]);
    });

    it("nests the grouped columns under their group in the dialog and never moves them out of it", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState(grouped) });
      const dialog = await openDialog();

      const group = dialog.querySelector(hook("grid-column-group"));
      expect(group.textContent).toContain("GRP");
      expect(itemNames(group)).toEqual(["b", "c"]);
      expect(itemOf(dialog, "b").querySelector(hook("grid-column-up")).disabled).toBe(true);
      expect(itemOf(dialog, "c").querySelector(hook("grid-column-down")).disabled).toBe(true);

      fireEvent.click(itemOf(dialog, "c").querySelector(hook("grid-column-up")));
      expect(headerNames()).toEqual(["A", "GRP", "D", "C", "B"]);
    });
  });

  describe("persistence failures", () => {
    const failing = () => {
      global.fetch = jest.fn(() => Promise.reject(new Error("down")));
      jest.spyOn(console, "error").mockImplementation(() => {});
    };

    it("rolls back Reset when the server fails and shows an error", async () => {
      failing();
      const layout = { order: ["d", "c", "b", "a"], hidden: ["c"] };
      const { store } = renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnLayout: layout }) });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-reset")));
      });

      expect(headerNames()).toEqual(["D", "B", "A"]);
      expect(store.getState().components.grid.attributes.columnLayout).toEqual(layout);
      await waitFor(() => expect(dialog.querySelector(hook("grid-columns-status")).getAttribute("data-type")).toBe("error"));
    });

    it("tells that the layout could not be saved", async () => {
      failing();
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });

      await waitFor(() => expect(dialog.querySelector(hook("grid-columns-status")).getAttribute("data-type")).toBe("error"));
    });

    it("saves silently: no message when the layout is saved", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });

      await waitFor(() => expect(global.fetch).toHaveBeenCalled());
      expect(dialog.querySelector(hook("grid-columns-status"))).toBeNull();
    });
  });

  describe("persistence", () => {
    const lastRequest = () => {
      const [url, options] = global.fetch.mock.calls[global.fetch.mock.calls.length - 1];
      return { url, body: JSON.parse(options.body) };
    };

    it("saves the layout as the default of the user", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();
      fireEvent.click(itemOf(dialog, "b").querySelector(hook("grid-column-checkbox")));
      fireEvent.click(itemOf(dialog, "c").querySelector(hook("grid-column-up")));

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });

      const { url, body } = lastRequest();
      expect(url).toContain("/action/maintain-silent/saveGridLayout");
      expect(body.gridLayoutScreen).toBe("Scr");
      expect(body.gridLayoutGrid).toBe("grid");
      expect(JSON.parse(body.gridLayoutJson)).toEqual({ order: ["a", "c", "b", "d"], hidden: ["b"] });
      expect(body.user).toBeUndefined();
    });

    it("resets to the definition of the screen and removes the stored layout", async () => {
      renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState({ columnLayout: { order: ["d", "c", "b", "a"], hidden: ["c"] } })
      });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-reset")));
      });

      expect(headerNames()).toEqual(["A", "B", "C", "D"]);
      const { url, body } = lastRequest();
      expect(url).toContain("/action/maintain-silent/resetGridLayout");
      expect(body).toEqual({ gridLayoutScreen: "Scr", gridLayoutGrid: "grid" });
    });
  });

  describe("maximize", () => {
    const rects = { target: { top: 40, left: 10, width: 800, height: 500 }, box: { top: 100, left: 300, width: 200, height: 200 } };
    const container = () => document.querySelector(".grid-container");
    const button = () => document.querySelector(hook("grid-maximize-button"));
    const finishAnimation = () => act(() => { jest.advanceTimersByTime(200 + 300); });

    // The grid sits in the content area of the template (.maximize-target), next to a sibling
    const renderInTarget = () => {
      renderWithProviders(<div className="maximize-target" id="target"><div id="sibling">other</div><AweGrid id="grid" /></div>,
        { preloadedState: gridState() });
      jest.spyOn(document.getElementById("target"), "getBoundingClientRect").mockReturnValue(rects.target);
      jest.spyOn(container(), "getBoundingClientRect").mockReturnValue(rects.box);
    };

    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    it("fills the maximize target like a window, with the texts and the hooks of the window maximize", () => {
      renderInTarget();
      expect(container().classList.contains("maximized")).toBe(false);
      expect(container().getAttribute("data-maximized")).toBe("false");
      expect(button().textContent).toBe("SCREEN_TEXT_MAXIMIZE");

      fireEvent.click(button());
      // The clone grows while the grid waits
      expect(document.querySelector(".maximize-clone")).not.toBeNull();
      finishAnimation();

      expect(document.querySelector(".maximize-clone")).toBeNull();
      expect(container().classList.contains("maximized")).toBe(true);
      expect(container().getAttribute("data-maximized")).toBe("true");
      expect(document.querySelector(hook("grid-toolbar")).getAttribute("data-maximized")).toBe("true");
      // It fills the content area (the target), not the viewport, so the header and the menu stay visible
      expect([container().style.position, container().style.top, container().style.left, container().style.width, container().style.height])
        .toEqual(["fixed", "40px", "10px", "800px", "500px"]);
      expect(document.getElementById("sibling").classList.contains("minimized")).toBe(true);
      expect(button().textContent).toBe("SCREEN_TEXT_RESTORE");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#minimize");

      fireEvent.click(button());
      // The state is exposed once it settles: until the restore animation ends the grid is still hidden under the clone
      expect(container().getAttribute("data-maximized")).toBe("true");
      expect(document.querySelector(hook("grid-toolbar")).getAttribute("data-maximized")).toBe("true");
      expect(document.getElementById("sibling").classList.contains("minimized")).toBe(false);
      act(() => { jest.advanceTimersByTime(150 + 300); });
      expect(container().getAttribute("data-maximized")).toBe("false");
      expect(document.querySelector(hook("grid-toolbar")).getAttribute("data-maximized")).toBe("false");
      expect(container().classList.contains("maximize-resizing")).toBe(false);
      expect(container().classList.contains("maximized")).toBe(false);
      expect(container().style.position).toBe("");
      expect(button().textContent).toBe("SCREEN_TEXT_MAXIMIZE");
    });

    it("measures the grid again when the animations end", () => {
      const reads = jest.fn(() => 100);
      const descriptor = Object.getOwnPropertyDescriptor(HTMLElement.prototype, "clientHeight");
      Object.defineProperty(HTMLElement.prototype, "clientHeight", { configurable: true, get: reads });
      try {
        renderInTarget();
        const before = reads.mock.calls.length;

        fireEvent.click(button());
        finishAnimation();
        const maximized = reads.mock.calls.length;
        fireEvent.click(button());
        act(() => { jest.advanceTimersByTime(150 + 300); });

        expect(maximized).toBeGreaterThan(before);
        expect(reads.mock.calls.length).toBeGreaterThan(maximized);
      } finally {
        if (descriptor) {
          Object.defineProperty(HTMLElement.prototype, "clientHeight", descriptor);
        } else {
          delete HTMLElement.prototype.clientHeight;
        }
      }
    });

    it("keeps the grid maximized on Escape, like an AngularJS window", () => {
      renderInTarget();
      fireEvent.click(button());
      finishAnimation();
      expect(container().classList.contains("maximized")).toBe(true);

      fireEvent.keyDown(document, { key: "Escape" });
      act(() => { jest.advanceTimersByTime(150 + 300); });

      expect(container().classList.contains("maximized")).toBe(true);
      expect(container().getAttribute("data-maximized")).toBe("true");
    });

    it("ignores Escape while the grid is not maximized", () => {
      renderInTarget();

      fireEvent.keyDown(document, { key: "Escape" });

      expect(container().classList.contains("maximized")).toBe(false);
      expect(document.querySelector(".maximize-clone")).toBeNull();
    });
  });

  it("keeps the dialog items within the dialog", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    const dialog = await openDialog();

    expect(within(dialog).getAllByRole("checkbox").length).toBeGreaterThanOrEqual(4);
  });

  describe("columns popover", () => {
    const columnsButton = () => document.querySelector(hook("grid-columns-button"));
    const isOpen = () => document.querySelector(hook("grid-columns-dialog")) !== null;
    const anchorAt = (rect) => jest.spyOn(columnsButton(), "getBoundingClientRect")
      .mockReturnValue({ width: rect.right - rect.left, height: rect.bottom - rect.top, x: rect.left, y: rect.top, ...rect });

    afterEach(() => jest.restoreAllMocks());

    it("opens the Columns as a popover anchored to the Columns button by default, without a modal mask", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      anchorAt({ top: 100, bottom: 130, left: 500, right: 600 });
      const popover = await openDialog();

      expect(popover.getAttribute("data-type")).toBe("popover");
      expect(popover.getAttribute("data-testid-owner")).toBe("grid");
      expect(popover.getAttribute("role")).toBe("dialog");
      expect(popover.getAttribute("aria-label")).toBe("GRID_COLUMNS_TITLE");
      expect(popover.parentElement).toBe(document.body);
      expect(document.querySelector(".p-dialog")).toBeNull();
      expect(document.querySelector(".p-dialog-mask")).toBeNull();
      expect(document.querySelector(".p-component-overlay")).toBeNull();
      expect(document.querySelector(".p-sidebar")).toBeNull();
      // Below the button, aligned to its right edge, and stacked over the screen and its dialogs
      expect(popover.style.position).toBe("fixed");
      expect(parseFloat(popover.style.top)).toBeGreaterThanOrEqual(130);
      expect(popover.style.left).toBe("600px");
      expect(popover.getAttribute("data-placement")).toBe("bottom");
      expect(Number(popover.style.zIndex)).toBeGreaterThan(0);
      expect(columnsButton().getAttribute("aria-expanded")).toBe("true");
    });

    it("fits the room under the Columns button, so a long list of columns scrolls inside it", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      // The button is near the top and the popover would measure 400 x 2000 (many columns)
      const button = columnsButton();
      const anchor = { top: 60, bottom: 90, left: 500, right: 600, width: 100, height: 30 };
      jest.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function measure() {
        return this === button ? anchor : { top: 0, bottom: 2000, left: 0, right: 400, width: 400, height: 2000, x: 0, y: 0 };
      });
      const popover = await openDialog();

      expect(popover.getAttribute("data-placement")).toBe("bottom");
      expect(parseFloat(popover.style.top)).toBe(94);
      expect(popover.style.maxHeight).toBe(`${window.innerHeight - 8 - 94}px`);
    });

    it("opens above the Columns button when there is no room below it", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      // The button is at the bottom of the viewport and the popover measures 400 x 300
      const button = columnsButton();
      const anchor = { top: window.innerHeight - 40, bottom: window.innerHeight - 10, left: 500, right: 600, width: 100, height: 30 };
      jest.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function measure() {
        return this === button ? anchor : { top: 0, bottom: 300, left: 0, right: 400, width: 400, height: 300, x: 0, y: 0 };
      });
      const popover = await openDialog();

      expect(popover.getAttribute("data-placement")).toBe("top");
      expect(parseFloat(popover.style.top) + 300).toBeLessThanOrEqual(window.innerHeight - 40);
      expect(popover.style.left).toBe("200px");
    });

    it("shows the title, the counter and the close button in the header", async () => {
      renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState({ columnLayout: { order: ["a", "b", "c", "d"], hidden: ["c"] } })
      });
      const popover = await openDialog();
      const header = popover.querySelector(".grid-columns-header");

      expect(header.textContent).toContain("GRID_COLUMNS_TITLE");
      const counter = header.querySelector(hook("grid-columns-counter"));
      expect([counter.getAttribute("data-visible"), counter.getAttribute("data-total")]).toEqual(["3", "4"]);
      expect(counter.textContent).toBe("3 SCREEN_TEXT_GRID_OF 4 GRID_COLUMNS_VISIBLE");
      const close = header.querySelector(hook("grid-columns-close"));
      expect(close.getAttribute("aria-label")).toBe("GRID_COLUMNS_CLOSE");
    });

    it("moves the focus inside when it opens", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const popover = await openDialog();

      await waitFor(() => expect(popover.contains(document.activeElement)).toBe(true));
    });

    it("keeps the focus inside while it is open", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const popover = await openDialog();
      const save = popover.querySelector(hook("grid-columns-save"));
      save.focus();

      fireEvent.keyDown(document, { key: "Tab" });

      expect(popover.contains(document.activeElement)).toBe(true);
      expect(document.activeElement).not.toBe(save);
    });

    it("cancels the live changes and closes", async () => {
      const layout = { order: ["a", "b", "c", "d"], hidden: ["d"] };
      const { store } = renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnLayout: layout }) });
      const popover = await openDialog();
      fireEvent.click(itemOf(popover, "b").querySelector(hook("grid-column-checkbox")));
      fireEvent.click(itemOf(popover, "c").querySelector(hook("grid-column-up")));
      expect(headerNames()).toEqual(["A", "C"]);

      fireEvent.click(popover.querySelector(hook("grid-columns-cancel")));

      expect(isOpen()).toBe(false);
      expect(headerNames()).toEqual(["A", "B", "C"]);
      expect(store.getState().components.grid.attributes.columnLayout).toEqual(layout);
    });

    it("cancels back to the layout of the last Save when it is opened again", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const popover = await openDialog();
      fireEvent.click(itemOf(popover, "b").querySelector(hook("grid-column-checkbox")));
      await act(async () => {
        fireEvent.click(popover.querySelector(hook("grid-columns-save")));
      });
      await waitFor(() => expect(isOpen()).toBe(false));
      const reopened = await openDialog();
      fireEvent.click(itemOf(reopened, "c").querySelector(hook("grid-column-checkbox")));

      fireEvent.click(reopened.querySelector(hook("grid-columns-cancel")));

      expect(headerNames()).toEqual(["A", "C", "D"]);
    });

    it("cancels back to the definition of the screen after Restore default", async () => {
      renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState({ columnLayout: { order: ["d", "c", "b", "a"], hidden: ["c"] } })
      });
      const popover = await openDialog();
      await act(async () => {
        fireEvent.click(popover.querySelector(hook("grid-columns-reset")));
      });
      await waitFor(() => expect(global.fetch).toHaveBeenCalled());
      fireEvent.click(itemOf(popover, "a").querySelector(hook("grid-column-checkbox")));

      fireEvent.click(popover.querySelector(hook("grid-columns-cancel")));

      expect(headerNames()).toEqual(["A", "B", "C", "D"]);
    });

    it("closes with its close button keeping the live changes", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const popover = await openDialog();
      fireEvent.click(itemOf(popover, "b").querySelector(hook("grid-column-checkbox")));

      fireEvent.click(popover.querySelector(hook("grid-columns-close")));

      expect(isOpen()).toBe(false);
      expect(headerNames()).toEqual(["A", "C", "D"]);
    });

    it("closes with Esc keeping the live changes and gives the focus back to the Columns button", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const popover = await openDialog();
      fireEvent.click(itemOf(popover, "b").querySelector(hook("grid-column-checkbox")));
      await waitFor(() => expect(popover.contains(document.activeElement)).toBe(true));

      fireEvent.keyDown(document.activeElement, { key: "Escape" });

      expect(isOpen()).toBe(false);
      expect(headerNames()).toEqual(["A", "C", "D"]);
      await waitFor(() => expect(document.activeElement).toBe(columnsButton()));
    });

    it("closes with a mouse down outside keeping the live changes", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const popover = await openDialog();
      fireEvent.click(itemOf(popover, "b").querySelector(hook("grid-column-checkbox")));

      fireEvent.mouseDown(popover.querySelector(".grid-columns-list"));
      expect(isOpen()).toBe(true);

      fireEvent.mouseDown(document.body);

      expect(isOpen()).toBe(false);
      expect(headerNames()).toEqual(["A", "C", "D"]);
    });

    it("closes with a mouse down on the Columns button and does not open again with its click", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      await openDialog();

      fireEvent.mouseDown(columnsButton());
      fireEvent.click(columnsButton());

      expect(isOpen()).toBe(false);
      expect(columnsButton().getAttribute("aria-expanded")).toBe("false");

      // The next click opens it again
      fireEvent.click(columnsButton());
      expect(isOpen()).toBe(true);
    });

    it("follows the Columns button when the page scrolls", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const rect = anchorAt({ top: 100, bottom: 130, left: 500, right: 600 });
      const popover = await openDialog();
      const top = parseFloat(popover.style.top);

      rect.mockReturnValue({ top: 50, bottom: 80, left: 400, right: 500, width: 100, height: 30, x: 400, y: 50 });
      act(() => { fireEvent.scroll(window); });

      expect(parseFloat(popover.style.top)).toBe(top - 50);
      expect(popover.style.left).toBe("500px");
    });

    it("does not let a mouse down inside it reach the popover or dialog where the grid is", async () => {
      const onOuterMouseDown = jest.fn();
      document.addEventListener("mousedown", onOuterMouseDown, true);
      try {
        renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
        const popover = await openDialog();

        fireEvent.mouseDown(itemOf(popover, "a").querySelector("label"));

        expect(onOuterMouseDown).not.toHaveBeenCalled();
        expect(isOpen()).toBe(true);
      } finally {
        document.removeEventListener("mousedown", onOuterMouseDown, true);
      }
    });

    it("closes only the popover with Esc when the grid is inside a popover of the screen", async () => {
      const onOuterKey = jest.fn();
      document.addEventListener("keydown", onOuterKey);
      try {
        renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
        const popover = await openDialog();

        fireEvent.keyDown(popover, { key: "Escape" });

        expect(isOpen()).toBe(false);
        expect(onOuterKey).not.toHaveBeenCalled();
      } finally {
        document.removeEventListener("keydown", onOuterKey);
      }
    });
  });

  // Save and Restore default are silent: the only sign of the call is aria-busy on the Columns, which the tests wait for
  describe("busy while the layout is saved or restored", () => {
    // The server answers when the test says so
    let answer;
    const pendingFetch = () => {
      global.fetch = jest.fn(() => new Promise((resolve, reject) => {
        answer = { resolve, reject };
      }));
    };
    const ok = (actions = []) => ({ ok: true, status: 200, json: () => Promise.resolve(actions), text: () => Promise.resolve(JSON.stringify(actions)) });
    const busy = () => document.querySelector(hook("grid-columns-dialog")).getAttribute("aria-busy");
    const isOpen = () => document.querySelector(hook("grid-columns-dialog")) !== null;
    const CASES = [["popover", "save"], ["popover", "reset"], ["dialog", "save"], ["side-panel", "reset"]];

    beforeEach(pendingFetch);

    it.each(CASES)("marks the %s busy until the server answers the %s", async (panel, button) => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: panel }) });
      const dialog = await openDialog();
      expect(busy()).toBeNull();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook(`grid-columns-${button}`)));
      });
      expect(busy()).toBe("true");

      await act(async () => answer.resolve(ok()));
      if (button === "save") {
        // A saved layout closes the Columns
        await waitFor(() => expect(isOpen()).toBe(false));
      } else {
        await waitFor(() => expect(busy()).toBeNull());
        expect(dialog.querySelector(hook("grid-columns-status"))).toBeNull();
      }
    });

    it.each(CASES)("marks the %s not busy when the %s call fails", async (panel, button) => {
      jest.spyOn(console, "error").mockImplementation(() => {});
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: panel }) });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook(`grid-columns-${button}`)));
      });
      expect(busy()).toBe("true");

      await act(async () => answer.reject(new Error("down")));
      await waitFor(() => expect(busy()).toBeNull());
      // The Columns stay open with the error
      expect(isOpen()).toBe(true);
      expect(document.querySelector(hook("grid-columns-status")).getAttribute("data-type")).toBe("error");
    });

    it("marks the popover not busy and keeps it open when the server answers with an error message", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });
      expect(busy()).toBe("true");

      await act(async () => answer.resolve(ok([{ type: "message", parameters: { type: "error", title: "T", message: "M" } }])));
      await waitFor(() => expect(busy()).toBeNull());
      expect(isOpen()).toBe(true);
      expect(dialog.querySelector(hook("grid-columns-status")).getAttribute("data-type")).toBe("error");
    });

    it.each(["popover", "dialog", "side-panel"])("closes the %s once the layout is saved and gives the focus back to the Columns button", async (panel) => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: panel }) });
      const button = document.querySelector(hook("grid-columns-button"));
      button.focus();
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });
      // Open until the server answers
      expect(isOpen()).toBe(true);

      await act(async () => answer.resolve(ok()));
      await waitFor(() => expect(isOpen()).toBe(false));
      await waitFor(() => expect(document.activeElement).toBe(button));
    });

    it("keeps the Columns open after Restore default", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-reset")));
      });
      await act(async () => answer.resolve(ok()));
      await waitFor(() => expect(busy()).toBeNull());

      expect(isOpen()).toBe(true);
    });

    it("keeps the Columns open when the server answers the Save with a warning", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });
      await act(async () => answer.resolve(ok([{ type: "message", parameters: { type: "warning", title: "T", message: "M" } }])));
      await waitFor(() => expect(busy()).toBeNull());

      expect(isOpen()).toBe(true);
      expect(dialog.querySelector(hook("grid-columns-status")).getAttribute("data-type")).toBe("error");
    });

    // While a Save waits for the server its button shows the loading look: a spinner, "Saving…" and disabled
    describe("Save button while the layout is saved", () => {
      const saveButton = () => document.querySelector(hook("grid-columns-save"));
      const isLoading = () => saveButton().classList.contains("is-loading");

      it.each(["popover", "dialog", "side-panel"])("shows Saving and is disabled in the %s until the server answers", async (panel) => {
        renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: panel }) });
        const dialog = await openDialog();
        expect(saveButton().textContent).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().disabled).toBe(false);
        expect(isLoading()).toBe(false);

        await act(async () => {
          fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
        });

        expect(saveButton().textContent).toBe("GRID_COLUMNS_SAVING");
        expect(saveButton().disabled).toBe(true);
        expect(isLoading()).toBe(true);
        expect(saveButton().querySelector(".awe-icon-spin")).not.toBeNull();
        // Disabled: a second click does not save again
        fireEvent.click(saveButton());
        expect(global.fetch).toHaveBeenCalledTimes(1);

        await act(async () => answer.resolve(ok()));
        await waitFor(() => expect(isOpen()).toBe(false));
      });

      it("goes back to Save and enabled when the Save fails", async () => {
        jest.spyOn(console, "error").mockImplementation(() => {});
        renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
        const dialog = await openDialog();

        await act(async () => {
          fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
        });
        expect(saveButton().textContent).toBe("GRID_COLUMNS_SAVING");

        await act(async () => answer.reject(new Error("down")));
        await waitFor(() => expect(busy()).toBeNull());

        expect(isOpen()).toBe(true);
        expect(saveButton().textContent).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().disabled).toBe(false);
        expect(isLoading()).toBe(false);
        expect(saveButton().querySelector(".awe-icon-spin")).toBeNull();
      });

      it("does not change the Save button while Restore default waits for the server", async () => {
        renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
        const dialog = await openDialog();

        await act(async () => {
          fireEvent.click(dialog.querySelector(hook("grid-columns-reset")));
        });
        expect(busy()).toBe("true");

        expect(saveButton().textContent).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().disabled).toBe(false);
        expect(isLoading()).toBe(false);
        await act(async () => answer.resolve(ok()));
      });
    });
  });

  describe("columns panel (#843)", () => {
    it("opens the Columns as a modal dialog with columns-panel dialog", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: "dialog" }) });
      const dialog = await openDialog();

      expect(dialog.classList.contains("p-dialog")).toBe(true);
      expect(dialog.getAttribute("data-type")).toBe("dialog");
      expect(dialog.querySelector(".grid-columns-header").textContent).toContain("GRID_COLUMNS_TITLE");
      expect(document.getElementById(dialog.getAttribute("aria-labelledby")).textContent).toBe("GRID_COLUMNS_TITLE");
      expect(document.querySelector(".p-sidebar")).toBeNull();
    });

    it("cancels the live changes in the dialog and closes it", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: "dialog" }) });
      const dialog = await openDialog();
      fireEvent.click(itemOf(dialog, "b").querySelector(hook("grid-column-checkbox")));

      fireEvent.click(dialog.querySelector(hook("grid-columns-cancel")));

      expect(headerNames()).toEqual(["A", "B", "C", "D"]);
      await waitFor(() => expect(document.querySelector(hook("grid-columns-dialog"))).toBeNull());
    });

    it("opens the Columns as a side panel on the right with columns-panel side-panel", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: "side-panel" }) });
      const panel = await openDialog();

      expect(panel.classList.contains("p-sidebar")).toBe(true);
      expect(document.querySelector(".p-sidebar-right")).not.toBeNull();
      expect(panel.getAttribute("data-type")).toBe("side-panel");
      expect(panel.getAttribute("data-testid-owner")).toBe("grid");
      expect(document.querySelector(".p-dialog")).toBeNull();
    });

    it.each(["popover", "dialog", "side-panel"])("lists the same columns and actions in the %s", async (columnsPanel) => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel }) });
      const panel = await openDialog();

      expect(panel.getAttribute("data-type")).toBe(columnsPanel);
      expect(itemNames(panel)).toEqual(["a", "b", "c", "d"]);
      ["grid-columns-counter", "grid-columns-reset", "grid-columns-cancel", "grid-columns-save", "grid-columns-close"]
        .forEach(id => expect(panel.querySelector(hook(id))).not.toBeNull());
      expect(panel.querySelector(hook("grid-columns-show-all"))).toBeNull();
      const footer = panel.querySelector(".grid-columns-actions");
      expect(Array.from(footer.querySelectorAll("button")).map(button => button.getAttribute("data-testid")))
        .toEqual(["grid-columns-reset", "grid-columns-cancel", "grid-columns-save"]);
      expect(itemOf(panel, "a").querySelector(".grid-column-handle")).not.toBeNull();
    });

    it("hides a column from the grid in the side panel", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: "side-panel" }) });
      const panel = await openDialog();

      fireEvent.click(itemOf(panel, "b").querySelector("input[type='checkbox']"));

      await waitFor(() => expect(headerNames()).toEqual(["A", "C", "D"]));
    });

    it("makes the popover the default of the application settings", () => {
      expect(DEFAULT_SETTINGS.gridColumnsPanel).toBe("popover");
    });

    it("opens the Columns of a grid without columns-panel as the application settings say", async () => {
      const state = gridState();
      state.settings = { ...state.settings, gridColumnsPanel: "side-panel" };
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });
      const panel = await openDialog();

      expect(panel.getAttribute("data-type")).toBe("side-panel");
    });

    it("lets the columns-panel of the grid win over the application settings", async () => {
      const state = gridState({ columnsPanel: "dialog" });
      state.settings = { ...state.settings, gridColumnsPanel: "side-panel" };
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });
      const dialog = await openDialog();

      expect(dialog.getAttribute("data-type")).toBe("dialog");
    });

    it("closes the side panel with its Close action and gives the focus back to the Columns button", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ columnsPanel: "side-panel" }) });
      const button = document.querySelector(hook("grid-columns-button"));
      button.focus();
      const panel = await openDialog();

      fireEvent.click(panel.querySelector(hook("grid-columns-close")));

      await waitFor(() => expect(document.querySelector(hook("grid-columns-dialog"))).toBeNull());
      expect(document.activeElement).toBe(button);
    });
  });

  it("sizes the Columns with the width token of the panel", async () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
    const popover = await openDialog();

    expect(popover.style.width).toBe("var(--awe-grid-columns-panel-width)");
  });
});

const bar = () => document.querySelector(hook("grid-selection-bar"));
const ids = (root) => Array.from(root?.querySelectorAll("button[id]") || []).map(button => button.id);
const footerButtons = () => ids(document.querySelector(".p-paginator-left-content"));
const barButtons = () => ids(bar()?.querySelector(".grid-selection-bar-actions"));

const selectionButton = (id, extra = {}) => ({ id, label: `LABEL_${id}`, icon: "trash", visible: true, ...extra });

const selectionButtonState = (definition) => ({
  address: { component: definition.id, view: "report" },
  attributes: { ...definition, buttonType: ButtonTypes.BUTTON_NORMAL },
  actions: [{ type: "server", parameters: { serverAction: "maintain", targetAction: `Mnt${definition.id}` } }]
});

/**
 * State of a screen with a grid and its buttons
 * @param {object[]} buttons Buttons of the grid
 * @param {number[]} selected Identifiers of the selected rows
 * @param {object} attributes Grid attributes
 * @returns {object} State
 */
function selectionState(buttons, selected = [], attributes = {}) {
  const values = [1, 2, 3].map(id => ({ id, name: `User ${id}`, selected: selected.includes(id) }));
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    actions: { running: false },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "users", option: "users" } },
    components: {
      grid: {
        address: { component: "grid", view: "report" },
        model: { values, records: values.length },
        attributes: {
          screenId: "Scr",
          multiselect: true,
          columnModel: [{ name: "name", label: "NAME", sortField: "name" }],
          headerModel: [],
          buttonModel: buttons,
          visible: true,
          ...attributes
        },
        specificAttributes: { sort: [] }
      },
      ...Object.fromEntries(buttons.map(definition => [definition.id, selectionButtonState(definition)]))
    }
  };
}

// The rows are selected as a click on them does
const selectRows = (store, selected) => act(() => {
  store.dispatch(updateModel({ component: "grid", view: "report" }, { selected }));
});

// Selection bar of a grid (#793): under the toolbar while there are selected rows, with the selection buttons of the grid
describe("grid selection bar", () => {
  beforeAll(() => {
    i18n.addResourceBundle("en", "translation", {
      SCREEN_TEXT_GRID_SELECTED_ROW: "{{count}} selected (one)",
      SCREEN_TEXT_GRID_SELECTED_ROWS: "{{count}} selected"
    }, true, true);
  });

  beforeEach(() => {
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]), text: () => Promise.resolve("[]") }));
  });

  it("shows the selection buttons in the bar, in the order of the screen, and keeps the others in the footer", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState(
      [selectionButton("ButNew"), selectionButton("ButRst", { selection: true }), selectionButton("ButExp"), selectionButton("ButDel", { selection: true })], [1]) });

    expect(footerButtons()).toEqual(["ButNew", "ButExp"]);
    expect(barButtons()).toEqual(["ButRst", "ButDel"]);
    expect(bar().getAttribute("data-testid-owner")).toBe("grid");
  });

  it("places the bar between the toolbar and the table", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [1]) });

    expect(bar().previousElementSibling.getAttribute("data-testid")).toBe("grid-toolbar");
    expect(bar().nextElementSibling.classList.contains("grid-contents")).toBe(true);
  });

  it("is hidden while no row is selected, and its buttons are not rendered", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })]) });

    expect(bar()).not.toBeNull();
    expect(bar().classList.contains("is-visible")).toBe(false);
    expect(bar().getAttribute("data-count")).toBe("0");
    expect(barButtons()).toEqual([]);
    expect(document.querySelector("#ButDel")).toBeNull();
  });

  it("shows the number of selected rows with the singular and plural texts, and hides when the selection is cleared", () => {
    const { store } = renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })]) });

    selectRows(store, [2]);
    expect(bar().classList.contains("is-visible")).toBe(true);
    expect(bar().getAttribute("data-count")).toBe("1");
    expect(bar().querySelector(hook("grid-selection-count")).textContent).toBe("1 selected (one)");

    selectRows(store, [1, 3]);
    expect(bar().querySelector(hook("grid-selection-count")).textContent).toBe("2 selected");

    // A reload of the grid clears the selection
    selectRows(store, []);
    expect(bar().classList.contains("is-visible")).toBe(false);
    expect(bar().querySelector(hook("grid-selection-count")).textContent).toBe("");
  });

  it("is a polite live region with a decorative check icon", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [1]) });

    expect(bar().getAttribute("aria-live")).toBe("polite");
    const icon = bar().querySelector(".grid-selection-bar-icon");
    expect(icon.getAttribute("aria-hidden")).toBe("true");
    expect(icon.innerHTML).toContain("circle-check");
  });

  it("shows the one selected row of a grid without multiselect", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [3], { multiselect: false }) });

    expect(bar().classList.contains("is-visible")).toBe(true);
    expect(bar().querySelector(hook("grid-selection-count")).textContent).toBe("1 selected (one)");
  });

  it("runs the actions of a button of the bar as from the footer", () => {
    const { dispatchSpy } = renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: selectionState([selectionButton("ButRst", { selection: true })], [1]), spyDispatch: true });

    fireEvent.click(bar().querySelector("#ButRst"));

    const added = dispatchSpy.mock.calls.map(([action]) => action).filter(action => action?.type === ADD_ACTIONS_TOP);
    expect(added).toHaveLength(1);
    expect(added[0].actions).toEqual([expect.objectContaining({ type: "server", address: { component: "ButRst", view: "report" } })]);
  });

  it("draws the buttons small and ghost, and a btn-danger button as the danger variant", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState(
      [selectionButton("ButRst", { selection: true }), selectionButton("ButDel", { selection: true, style: "btn-danger" })], [1]) });

    const reset = bar().querySelector("#ButRst");
    const remove = bar().querySelector("#ButDel");
    expect(reset.className).toContain("p-button-sm");
    expect(reset.className).toContain("p-button-text");
    expect(reset.className).not.toContain("p-button-outlined");
    expect(reset.className).not.toContain("mt-2");
    expect(remove.className).toContain("p-button-danger");
    expect(remove.className).not.toContain("p-button-text");
  });

  it("leaves a grid without selection buttons as it was: no bar, every button in the footer", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButNew"), selectionButton("ButDel")], [1]) });

    expect(bar()).toBeNull();
    expect(footerButtons()).toEqual(["ButNew", "ButDel"]);
    expect(document.querySelector("#ButNew").className).toContain("mt-2");
    expect(document.querySelector("#ButNew").className).toContain("p-button-outlined");
  });

  it("shows the bar in a tree grid too", () => {
    renderWithProviders(<AweTreeGrid id="grid" />, { preloadedState: selectionState(
      [selectionButton("ButNew"), selectionButton("ButDel", { selection: true })], [2], { treegrid: true }) });

    expect(bar().classList.contains("is-visible")).toBe(true);
    expect(barButtons()).toEqual(["ButDel"]);
    expect(footerButtons()).toEqual(["ButNew"]);
  });
});
