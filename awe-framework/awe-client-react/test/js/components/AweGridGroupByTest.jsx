import React from "react";
import { fireEvent } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

// Toolbar of a grouped grid (#833): Expand all / Collapse all and the Group by selector, without server calls
const COLUMNS = [
  { name: "country", label: "Country" },
  { name: "city", label: "City" },
  { name: "order", label: "Order" },
  { name: "secret", label: "Secret", hidden: true },
  { name: "nolabel" },
  { name: "amount", label: "Amount", summaryType: "sum" }
];

const ROWS = [
  { id: 1, country: "ES", city: "Madrid", order: "SO-1", amount: 10 },
  { id: 2, country: "DE", city: "Berlin", order: "SO-2", amount: 20 },
  { id: 3, country: "ES", city: "Bilbao", order: "SO-3", amount: 5 }
];

function gridState(attributes = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
    components: {
      grid: {
        address: { component: "grid", view: "report" },
        model: { values: ROWS, records: ROWS.length },
        attributes: {
          screenId: "Scr",
          columnModel: COLUMNS,
          headerModel: [],
          buttonModel: [],
          visible: true,
          loadAll: true,
          groupBy: "country",
          ...attributes
        },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const render = (attributes) => {
  global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]), text: () => Promise.resolve("[]") }));
  return renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState(attributes), spyDispatch: true });
};

const hook = (id) => document.querySelector(`[data-testid="${id}"]`);
const groupPaths = (container) => [...container.querySelectorAll("tr.grid-group-row")].map(row => row.getAttribute("data-group-path"));
const expandedGroups = (container) => [...container.querySelectorAll("tr.grid-group-row")]
  .map(row => `${row.getAttribute("data-group-path")}:${row.getAttribute("data-expanded")}`);
const dataRows = (container) => container.querySelectorAll("tbody.p-datatable-tbody > tr[row-id]").length;
const option = (name) => document.querySelector(`[data-testid="grid-group-option"][data-column="${name}"]`);
const optionNames = () => [...document.querySelectorAll('[data-testid="grid-group-option"]')].map(item => item.getAttribute("data-column"));
const checkboxOf = (name) => option(name).querySelector('input[type="checkbox"]');
const levelOf = (name) => option(name).querySelector(".grid-group-level")?.textContent ?? null;
// Calls to the server: actions of the action stack and fetches
const serverCalls = (dispatchSpy) => dispatchSpy.mock.calls.map(([action]) => action)
  .filter(action => action.type === "ADD_ACTIONS_TOP" || action.type === "ADD_ACTIONS").length + global.fetch.mock.calls.length;

describe("awe-react-client/test/js/components/AweGridGroupByTest.jsx", () => {
  describe("expand all and collapse all", () => {
    it("collapses and expands every group of every level without a server call", () => {
      const { container, dispatchSpy } = render({ groupBy: "country,city" });

      fireEvent.click(hook("grid-group-collapse-all"));

      expect(expandedGroups(container)).toEqual(["DE:false", "ES:false"]);
      expect(dataRows(container)).toBe(0);

      fireEvent.click(hook("grid-group-expand-all"));

      expect(expandedGroups(container)).toEqual(["DE:true", "DE/Berlin:true", "ES:true", "ES/Bilbao:true", "ES/Madrid:true"]);
      expect(dataRows(container)).toBe(3);
      expect(serverCalls(dispatchSpy)).toBe(0);
    });

    it("are icon buttons with their label as tooltip and accessible name", () => {
      render();

      const expand = hook("grid-group-expand-all");
      expect(expand.tagName).toBe("BUTTON");
      expect(expand.getAttribute("aria-label")).toBe("GRID_GROUP_EXPAND_ALL");
      expect(expand.getAttribute("title")).toBe("GRID_GROUP_EXPAND_ALL");
      expect(hook("grid-group-collapse-all").getAttribute("aria-label")).toBe("GRID_GROUP_COLLAPSE_ALL");
    });

    it.each([
      ["a grid without group-by", { groupBy: undefined }],
      ["a grid with no grouping chosen", { groupSelector: true, groupLevels: [] }],
      ["an editable grid", { editable: true }]
    ])("are not shown on %s", (_name, attributes) => {
      render(attributes);

      expect(hook("grid-group-expand-all")).toBeNull();
      expect(hook("grid-group-collapse-all")).toBeNull();
    });
  });

  describe("group by selector", () => {
    const openSelector = () => {
      fireEvent.click(hook("grid-group-by"));
      return hook("grid-group-by-panel");
    };

    it("is not shown without group-selector", () => {
      render();

      expect(hook("grid-group-by")).toBeNull();
    });

    it("is not shown on a grid that cannot group", () => {
      render({ groupSelector: true, moveRows: true });

      expect(hook("grid-group-by")).toBeNull();
    });

    it("lists No grouping and the visible columns with a label and without summary type, the checked ones with their level", () => {
      render({ groupSelector: true, groupBy: "city,country" });

      const button = hook("grid-group-by");
      expect(button.getAttribute("aria-haspopup")).toBe("dialog");
      expect(button.textContent).toContain("GRID_GROUP_BY");
      const panel = openSelector();
      expect(panel.getAttribute("data-testid-owner")).toBe("grid");
      expect(button.getAttribute("aria-expanded")).toBe("true");
      expect(hook("grid-group-none").textContent).toBe("GRID_GROUP_NONE");
      expect(optionNames()).toEqual(["country", "city", "order"]);
      expect(checkboxOf("country").checked).toBe(true);
      expect(levelOf("city")).toBe("1");
      expect(levelOf("country")).toBe("2");
      expect(checkboxOf("order").checked).toBe(false);
      expect(levelOf("order")).toBeNull();
    });

    it("appends a level when a column is checked and removes it when it is unchecked, live and without a server call", () => {
      const { container, dispatchSpy } = render({ groupSelector: true });
      openSelector();

      fireEvent.click(checkboxOf("city"));

      expect(groupPaths(container)).toEqual(["DE", "DE/Berlin", "ES", "ES/Bilbao", "ES/Madrid"]);
      expect(levelOf("country")).toBe("1");
      expect(levelOf("city")).toBe("2");

      fireEvent.click(checkboxOf("country"));

      expect(groupPaths(container)).toEqual(["Berlin", "Bilbao", "Madrid"]);
      expect(levelOf("city")).toBe("1");
      expect(levelOf("country")).toBeNull();
      expect(serverCalls(dispatchSpy)).toBe(0);
    });

    it("removes the grouping with No grouping and shows the data rows", () => {
      const { container } = render({ groupSelector: true, groupBy: "country,city" });
      openSelector();

      fireEvent.click(hook("grid-group-none"));

      expect(container.querySelector("tr.grid-group-row")).toBeNull();
      expect(dataRows(container)).toBe(3);
      expect(hook("grid-group-none").getAttribute("aria-pressed")).toBe("true");
      expect(hook("grid-group-expand-all")).toBeNull();
      // The selector stays, to group again
      fireEvent.click(checkboxOf("order"));
      expect(groupPaths(container)).toEqual(["SO-1", "SO-2", "SO-3"]);
    });

    it("keeps the expanded groups while the levels do not change and the collapse all with other levels", () => {
      const { container } = render({ groupSelector: true });
      fireEvent.click(hook("grid-group-collapse-all"));
      openSelector();

      fireEvent.click(checkboxOf("city"));

      expect(expandedGroups(container)).toEqual(["DE:false", "ES:false"]);
    });

    it("closes with Escape and gives the focus back to its button", () => {
      render({ groupSelector: true });
      openSelector();

      fireEvent.keyDown(window, { key: "Escape" });

      expect(hook("grid-group-by-panel")).toBeNull();
      expect(hook("grid-group-by").getAttribute("aria-expanded")).toBe("false");
      expect(document.activeElement).toBe(hook("grid-group-by"));
    });

    it("closes with a mouse down outside", () => {
      render({ groupSelector: true });
      openSelector();

      fireEvent.mouseDown(document.body);

      expect(hook("grid-group-by-panel")).toBeNull();
    });
  });
});
