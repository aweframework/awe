import React from "react";
import { screen } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

const hook = (id) => `[data-testid="${id}"]`;

// A grid with a search and a toolbar criterion (a switch) and the switch registered as a criterion of the screen
function gridState(attributes = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
    components: {
      grid: {
        address: { component: "grid", view: "report" },
        model: { values: [{ id: 1, a: "a1" }], records: 1 },
        attributes: {
          screenId: "Scr",
          columnModel: [{ name: "a", label: "A", sortField: "a" }],
          headerModel: [],
          buttonModel: [],
          visible: true,
          search: "CrtTxt",
          toolbarCriteria: ["CrtGrp"],
          ...attributes
        },
        specificAttributes: { sort: [] }
      },
      CrtTxt: {
        address: { component: "CrtTxt", view: "report" },
        model: { values: [], selected: "" },
        attributes: { id: "CrtTxt", component: "grid-search", visible: true },
        specificAttributes: {}
      },
      CrtGrp: {
        address: { component: "CrtGrp", view: "report" },
        model: { values: [{ label: "Group by flow", value: "1", selected: false }] },
        attributes: { id: "CrtGrp", component: "checkbox", style: "switch no-label", label: "Group by flow", visible: true },
        validationRules: { required: false },
        specificAttributes: {}
      }
    }
  };
}

describe("awe-react-client/test/js/components/AweGridToolbarCriteriaTest.jsx", () => {
  beforeEach(() => {
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]), text: () => Promise.resolve("[]") }));
  });

  it("shows the toolbar criteria of the grid as their component at the end of the toolbar, just before Columns", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState() });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    const toggle = screen.getByRole("switch", { name: "Group by flow" });
    const criteria = toggle.closest(".grid-toolbar-criteria");
    expect(criteria).not.toBeNull();
    expect(criteria.parentElement).toBe(toolbar);
    expect(criteria.nextElementSibling).toBe(toolbar.querySelector(hook("grid-columns-button")));
    // The search stays at the start, in the band of the toolbar
    expect(toolbar.querySelector(hook("grid-search")).closest(".grid-toolbar-band")).not.toBeNull();
  });

  it("shows the toolbar for its criteria when the grid hides its buttons, and no criteria block without them", () => {
    const { unmount } = renderWithProviders(<AweGrid id="grid" />,
      { preloadedState: gridState({ showToolbar: false, search: undefined }) });

    expect(document.querySelector(hook("grid-columns-button"))).toBeNull();
    expect(screen.getByRole("switch", { name: "Group by flow" }).closest(hook("grid-toolbar"))).not.toBeNull();
    unmount();

    renderWithProviders(<AweGrid id="grid" />, { preloadedState: gridState({ toolbarCriteria: undefined }) });
    expect(document.querySelector(".grid-toolbar-criteria")).toBeNull();
  });
});
