import React from "react";
import { act, fireEvent } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import { updateSpecificAttributes } from "../../../src/redux/actions/components";

// Row grouping of the React grid (#833): group and subtotal rows built from the data rows of the model
const COLUMNS = [
  { name: "country", label: "Country", sortField: "country", sortable: true },
  { name: "city", label: "City", sortField: "city", sortable: true },
  { name: "order", label: "Order", sortField: "order", sortable: true },
  { name: "amount", label: "Amount", sortField: "amount", sortable: true, summaryType: "sum", align: "right" }
];

const ROWS = [
  { id: 1, country: "ES", city: "Madrid", order: "SO-1", amount: 10 },
  { id: 2, country: "DE", city: "Berlin", order: "SO-2", amount: 20 },
  { id: 3, country: "ES", city: "Bilbao", order: "SO-3", amount: 5 },
  { id: 4, country: "ES", city: "Madrid", order: "SO-4", amount: 7 },
  { id: 5, country: "DE", city: "Munich", order: "SO-5", amount: 30 }
];

function gridState(attributes = {}, { values = ROWS, specificAttributes = {} } = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
    components: {
      grid: {
        address: { component: "grid", view: "report" },
        model: { values, records: values.length, footer: { amount: { value: 72, label: "72" } } },
        attributes: {
          screenId: "Scr",
          columnModel: COLUMNS,
          headerModel: [],
          buttonModel: [],
          visible: true,
          loadAll: true,
          max: 2,
          groupBy: "country",
          ...attributes
        },
        specificAttributes: { sort: [], ...specificAttributes }
      }
    }
  };
}

const render = (attributes, options) => renderWithProviders(<AweGrid id="grid" />, {
  preloadedState: gridState(attributes, options), spyDispatch: true
});

// What the body shows, row by row: "G<level>:<path>" for groups, "S<level>:<path>" for subtotals and the row id
const bodyShape = (container) => [...container.querySelectorAll("tbody.p-datatable-tbody > tr")].map(row => {
  if (row.classList.contains("grid-group-row")) {
    return `G${row.getAttribute("data-group-level")}:${row.getAttribute("data-group-path")}`;
  }
  if (row.classList.contains("grid-subtotal-row")) {
    return `S${row.getAttribute("data-group-level")}:${row.getAttribute("data-group-path")}`;
  }
  return row.getAttribute("row-id");
});

const groupRow = (container, path) => container.querySelector(`tr.grid-group-row[data-group-path="${path}"]`);
const subtotalRow = (container, path) => container.querySelector(`tr.grid-subtotal-row[data-group-path="${path}"]`);
const cellOf = (row, column) => row.querySelector(`td.${column}`);
const toggleOf = row => row.querySelector('[data-testid="grid-group-toggle"]');
const clientActions = (dispatchSpy, type) => dispatchSpy.mock.calls
  .map(([action]) => action)
  .filter(action => action.type === "ADD_ACTIONS_TOP")
  .flatMap(action => action.payload)
  .filter(action => action.type === type);

describe("awe-react-client/test/js/components/AweGridGroupsTest.jsx", () => {
  beforeEach(() => {
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]), text: () => Promise.resolve("[]") }));
  });

  it("shows a group row, the data rows and a subtotal row per group, with their classes and data attributes", () => {
    const { container } = render();

    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);
    const group = groupRow(container, "ES");
    expect(group).toHaveClass("grid-group-row", "grid-group-level-1");
    expect(group.getAttribute("data-testid")).toBe("grid-group-row");
    expect(group.hasAttribute("row-id")).toBe(false);
    const subtotal = subtotalRow(container, "ES");
    expect(subtotal).toHaveClass("grid-subtotal-row", "grid-subtotal-level-1");
    expect(subtotal.getAttribute("data-testid")).toBe("grid-subtotal-row");
  });

  it("shows the toggle, the label and the count in the first visible column and the aggregates in their columns", () => {
    const { container } = render();
    const group = groupRow(container, "ES");

    const first = cellOf(group, "country");
    const toggle = toggleOf(group);
    expect(first.contains(toggle)).toBe(true);
    expect(toggle.tagName).toBe("BUTTON");
    expect(toggle.getAttribute("aria-expanded")).toBe("true");
    expect(toggle.getAttribute("aria-label")).toBe("GRID_GROUP_COLLAPSE");
    expect(toggle.getAttribute("title")).toBe("GRID_GROUP_COLLAPSE");
    expect(first.querySelector(".grid-group-text").textContent).toBe("ES");
    expect(first.querySelector(".grid-group-count").textContent).toBe("3");
    expect(cellOf(group, "amount").textContent).toBe("22");
    expect(cellOf(group, "city").textContent).toBe("");
    expect(cellOf(group, "order").textContent).toBe("");
    const subtotal = subtotalRow(container, "ES");
    expect(cellOf(subtotal, "country").textContent).toBe("GRID_GROUP_SUBTOTAL ES");
    expect(cellOf(subtotal, "amount").textContent).toBe("22");
    expect(cellOf(subtotal, "city").textContent).toBe("");
  });

  it("indents each level and closes each one with its subtotal", () => {
    const { container } = render({ groupBy: "country,city" });

    expect(bodyShape(container)).toEqual([
      "G1:DE", "G2:DE/Berlin", "2", "S2:DE/Berlin", "G2:DE/Munich", "5", "S2:DE/Munich", "S1:DE",
      "G1:ES", "G2:ES/Bilbao", "3", "S2:ES/Bilbao", "G2:ES/Madrid", "1", "4", "S2:ES/Madrid", "S1:ES"
    ]);
    expect(groupRow(container, "ES/Madrid")).toHaveClass("grid-group-level-2");
    expect(subtotalRow(container, "ES/Madrid")).toHaveClass("grid-subtotal-level-2");
    expect(cellOf(groupRow(container, "ES/Madrid"), "country").querySelector(".grid-group-label").style
      .getPropertyValue("--_group-level")).toBe("1");
  });

  it("collapses and expands a group with its toggle, without changing the model values", () => {
    const { container, store } = render();
    const values = store.getState().components.grid.model.values;

    fireEvent.click(toggleOf(groupRow(container, "ES")));

    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES"]);
    expect(toggleOf(groupRow(container, "ES")).getAttribute("aria-expanded")).toBe("false");
    expect(toggleOf(groupRow(container, "ES")).getAttribute("aria-label")).toBe("GRID_GROUP_EXPAND");
    expect(store.getState().components.grid.model.values).toBe(values);

    fireEvent.click(toggleOf(groupRow(container, "ES")));

    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);
  });

  it("keeps a toggled group when another group is toggled after it", () => {
    const { container } = render();

    fireEvent.click(toggleOf(groupRow(container, "ES")));
    fireEvent.click(toggleOf(groupRow(container, "DE")));

    expect(bodyShape(container)).toEqual(["G1:DE", "G1:ES"]);

    fireEvent.click(toggleOf(groupRow(container, "ES")));
    fireEvent.click(cellOf(groupRow(container, "DE"), "city"));

    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);
  });

  it("toggles a group with Enter and Space on its toggle", () => {
    const { container } = render();

    fireEvent.keyDown(toggleOf(groupRow(container, "DE")), { key: "Enter", code: "Enter" });
    expect(bodyShape(container)).toEqual(["G1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);

    fireEvent.keyDown(toggleOf(groupRow(container, "DE")), { key: " ", code: "Space" });
    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);
  });

  it("toggles a group with a click on its row and never selects it", () => {
    const { container, dispatchSpy } = render();

    fireEvent.click(cellOf(groupRow(container, "DE"), "city"));

    expect(bodyShape(container)).toEqual(["G1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);
    expect(clientActions(dispatchSpy, "select-row")).toEqual([]);
    fireEvent.click(cellOf(subtotalRow(container, "ES"), "city"));
    expect(clientActions(dispatchSpy, "select-row")).toEqual([]);
  });

  it("still selects the data rows", () => {
    const { container, dispatchSpy } = render();

    fireEvent.click(container.querySelector('tr[row-id="3"] td.city'));

    expect(clientActions(dispatchSpy, "select-row").map(action => action.parameters.values)).toEqual([[3]]);
  });

  it("keeps the expanded groups when the data reloads", () => {
    const { container, store } = render();
    fireEvent.click(toggleOf(groupRow(container, "ES")));

    act(() => store.dispatch({ type: "UPDATE_MODEL", address: { component: "grid", view: "report" },
      data: { values: [...ROWS, { id: 6, country: "ES", city: "Vigo", order: "SO-6", amount: 1 }] } }));

    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES"]);
  });

  it("shows every row without pagination while grouped", () => {
    const { container } = render();

    expect(container.querySelectorAll("tbody.p-datatable-tbody > tr[row-id]")).toHaveLength(5);
    expect(container.querySelector('[data-testid="grid-page-next"]')).toBeNull();
    expect(container.querySelector('[data-testid="grid-pagination-info"]')).toBeNull();
  });

  it("does not offer the column filters while grouped", () => {
    const { container } = render({ enableFilters: true });

    expect(container.querySelector(".p-column-filter-menu-button")).toBeNull();
  });

  it("orders the groups by the aggregate of a sorted column with summary type, and the rows inside by the sort", () => {
    const { container } = render({}, { specificAttributes: { sort: [{ id: "amount", direction: "asc", order: 1 }] } });

    expect(bodyShape(container)).toEqual(["G1:ES", "3", "4", "1", "S1:ES", "G1:DE", "2", "5", "S1:DE"]);
  });

  it("orders the groups by their value in the direction of the sort of the group column", () => {
    const { container } = render({}, { specificAttributes: { sort: [{ id: "country", direction: "desc", order: -1 }] } });

    expect(bodyShape(container)).toEqual(["G1:ES", "1", "3", "4", "S1:ES", "G1:DE", "2", "5", "S1:DE"]);
  });

  it("does not sort the rows by itself when a header is clicked: the groups follow the stored sort", () => {
    const { container, store, dispatchSpy } = render();

    fireEvent.click(container.querySelector('th[column-id="amount"]'));

    expect(clientActions(dispatchSpy, "change-sort").map(action => action.parameters.sort))
      .toEqual([[{ id: "amount", order: 1, direction: "asc" }]]);
    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "S1:DE", "G1:ES", "1", "3", "4", "S1:ES"]);

    // The change-sort action stores the sort, which orders the groups and their rows
    act(() => store.dispatch(updateSpecificAttributes({ component: "grid", view: "report" },
      { sort: [{ id: "amount", order: 1, direction: "asc" }] })));
    expect(bodyShape(container)).toEqual(["G1:ES", "3", "4", "1", "S1:ES", "G1:DE", "2", "5", "S1:DE"]);
  });

  it("does not show the subtotal rows with group-subtotals false", () => {
    const { container } = render({ groupSubtotals: false });

    expect(bodyShape(container)).toEqual(["G1:DE", "2", "5", "G1:ES", "1", "3", "4"]);
  });

  it("gives the group and subtotal rows no selection checkbox and no row number", () => {
    const { container } = render({ multiselect: true, rowNumbers: true });

    expect(groupRow(container, "ES").querySelector('[data-testid="grid-row-checkbox"]')).toBeNull();
    expect(subtotalRow(container, "ES").querySelector('[data-testid="grid-row-checkbox"]')).toBeNull();
    expect(container.querySelector('tr[row-id="1"] [data-testid="grid-row-checkbox"]')).not.toBeNull();
    expect(groupRow(container, "ES").querySelector(".p-row-number-cell").textContent).toBe("");
    // The data rows are numbered in the order they are shown
    const numbers = [...container.querySelectorAll("tr[row-id] .p-row-number-cell")].map(cell => cell.textContent);
    expect(numbers).toEqual(["1", "2", "3", "4", "5"]);
  });

  it("keeps the grand total in the footer", () => {
    const { container } = render({ showTotals: true });

    expect(container.querySelector("tfoot").textContent).toContain("72");
  });

  describe("grids that do not group", () => {
    it("shows the data rows with pagination when the grid has no group-by", () => {
      const { container } = render({ groupBy: undefined });

      expect(container.querySelector("tr.grid-group-row")).toBeNull();
      expect(bodyShape(container)).toEqual(["1", "2"]);
      expect(container.querySelector('[data-testid="grid-page-next"]')).not.toBeNull();
    });

    it("offers the column filters when the grid has no group-by", () => {
      const { container } = render({ groupBy: undefined, enableFilters: true });

      expect(container.querySelector(".p-column-filter-menu-button")).not.toBeNull();
    });

    it("shows the data rows when no grouping is chosen at runtime", () => {
      const { container } = render({ groupSelector: true, groupLevels: [] });

      expect(container.querySelector("tr.grid-group-row")).toBeNull();
      expect(bodyShape(container)).toEqual(["1", "2"]);
    });

    it.each([
      ["editable", { editable: true }],
      ["move-rows", { moveRows: true }],
      ["row-detail", { rowDetail: { columns: [] } }],
      ["send-operations", { multioperation: true }]
    ])("ignores group-by on %s grids", (_name, extra) => {
      const { container } = render(extra);

      expect(container.querySelector("tr.grid-group-row")).toBeNull();
      expect(container.querySelectorAll("tbody.p-datatable-tbody > tr[row-id]")).toHaveLength(2);
    });
  });
});
