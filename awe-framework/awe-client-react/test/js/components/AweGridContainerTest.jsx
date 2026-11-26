import React from "react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

/**
 * State of a grid with a toolbar (a search) and one row
 * @param {object} attributes Attributes of the grid
 * @returns {object} Preloaded state
 */
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
          search: { id: "GrdSch" },
          visible: true,
          ...attributes
        },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const container = () => document.querySelector('[role="grid-container"]').parentElement;

describe("awe-react-client/test/js/components/AweGridContainerTest.jsx", () => {
  beforeEach(() => {
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // Two grids of one window shown one at a time (a dependency hides the other): the hidden one must not keep the
  // space of the window, so its whole container hides, not only its table
  it.each([["grid", AweGrid], ["tree grid", AweTreeGrid]])("hides the whole container of a hidden %s", (_, Grid) => {
    renderWithProviders(<Grid id="grid" />, { preloadedState: gridState({ visible: false }) });

    expect(container()).not.toBeVisible();
  });

  it.each([["grid", AweGrid], ["tree grid", AweTreeGrid]])("shows the container of a visible %s", (_, Grid) => {
    renderWithProviders(<Grid id="grid" />, { preloadedState: gridState() });

    expect(container()).toBeVisible();
  });
});
