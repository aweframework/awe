import React from "react";
import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

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

  it("shows all the columns", async () => {
    renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: gridState({ columnLayout: { order: ["a", "b", "c", "d"], hidden: ["b", "c"] } })
    });
    expect(headerNames()).toEqual(["A", "D"]);

    const dialog = await openDialog();
    fireEvent.click(dialog.querySelector(hook("grid-columns-show-all")));

    expect(headerNames()).toEqual(["A", "B", "C", "D"]);
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

    it("tells that the layout was saved", async () => {
      renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });
      const dialog = await openDialog();

      await act(async () => {
        fireEvent.click(dialog.querySelector(hook("grid-columns-save")));
      });

      await waitFor(() => expect(dialog.querySelector(hook("grid-columns-status")).getAttribute("data-type")).toBe("saved"));
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
      expect(url).toContain("/action/maintain/saveGridLayout");
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
      expect(url).toContain("/action/maintain/resetGridLayout");
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
      expect(button().querySelector("i").className).toContain("pi-window-minimize");

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
});
