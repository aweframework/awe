import React from "react";
import { cleanup, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

const hook = (testId) => `[data-testid='${testId}']`;
const EMPTY = hook("grid-empty");
const CLEAR_BUTTON = { id: "ClearFilters", label: "BUTTON_CLEAR", icon: "eraser", visible: true };

function gridState({ values = [], attributes = {}, buttons = [], model } = {}) {
  const components = {
    Grd: {
      address: { component: "Grd", view: "report" },
      ...(model === null ? {} : { model: model ?? { values, records: values.length } }),
      attributes: {
        columnModel: [{ name: "name", label: "Name" }],
        headerModel: [],
        buttonModel: buttons,
        visible: true,
        max: 30,
        ...attributes
      },
      specificAttributes: { sort: [] }
    }
  };
  buttons.forEach(button => {
    components[button.id] = {
      address: { component: button.id, view: "report" },
      attributes: button,
      actions: []
    };
  });
  return { settings: DEFAULT_SETTINGS, screen: { size: { width: 100, height: 100 } }, components };
}

describe("awe-react-client/test/js/components/AweGridEmptyStateTest.jsx", () => {
  afterEach(cleanup);

  it("shows the default empty state in a grid without rows", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    const empty = container.querySelector(EMPTY);
    expect(empty).not.toBeNull();
    expect(empty.querySelector(hook("grid-empty-title")).textContent).toBe("SCREEN_TEXT_GRID_EMPTY_TITLE");
    expect(empty.querySelector(hook("grid-empty-text")).textContent).toBe("SCREEN_TEXT_GRID_EMPTY_TEXT");
    expect(empty.querySelector(hook("grid-empty-action"))).toBeNull();
  });

  it("shows the configured icon, title and text", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, {
      preloadedState: gridState({ attributes: { emptyIcon: "filter", emptyTitle: "MY_TITLE", emptyText: "MY_TEXT" } })
    });

    const empty = container.querySelector(EMPTY);
    expect(empty.querySelector(hook("grid-empty-title")).textContent).toBe("MY_TITLE");
    expect(empty.querySelector(hook("grid-empty-text")).textContent).toBe("MY_TEXT");
    expect(empty.querySelector("i.grid-empty-icon > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#funnel");
  });

  it("does not show the empty state when the grid has rows", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, {
      preloadedState: gridState({ values: [{ id: "r1", name: "Ann" }] })
    });

    expect(container.querySelector(EMPTY)).toBeNull();
  });

  it("does not show the empty state while the grid is loading", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, {
      preloadedState: gridState({ attributes: { loading: true } })
    });

    expect(container.querySelector(EMPTY)).toBeNull();
  });

  it("shows an action that clicks the button of the grid it refers to", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, {
      preloadedState: gridState({ attributes: { emptyAction: "ClearFilters" }, buttons: [CLEAR_BUTTON] })
    });
    const onClick = jest.fn();
    container.querySelector("#ClearFilters").addEventListener("click", onClick);

    const action = container.querySelector(EMPTY).querySelector(hook("grid-empty-action"));
    expect(action.textContent).toBe("BUTTON_CLEAR");
    expect(action.querySelector("i.p-button-icon > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#eraser");
    fireEvent.click(action);

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("does not show an action for a button that is not in the grid", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, {
      preloadedState: gridState({ attributes: { emptyAction: "Unknown" }, buttons: [CLEAR_BUTTON] })
    });

    expect(container.querySelector(EMPTY)).not.toBeNull();
    expect(container.querySelector(hook("grid-empty-action"))).toBeNull();
  });

  it("shows the empty state in a tree grid without rows", () => {
    const { container } = renderWithProviders(<AweTreeGrid id="Grd" />, {
      preloadedState: gridState({ attributes: { treegrid: true, emptyTitle: "MY_TITLE" } })
    });

    expect(container.querySelector(EMPTY).querySelector(hook("grid-empty-title")).textContent).toBe("MY_TITLE");
  });

  it("only clicks the button of its own grid when another grid has a button with the same id", () => {
    const state = gridState({ attributes: { emptyAction: "ClearFilters" }, buttons: [CLEAR_BUTTON] });
    state.components.Other = {
      ...state.components.Grd,
      address: { component: "Other", view: "base" },
      attributes: { ...state.components.Grd.attributes, emptyAction: undefined }
    };
    const { container } = renderWithProviders(<><AweGrid id="Other" /><AweGrid id="Grd" /></>, { preloadedState: state });
    const [otherButton, ownButton] = Array.from(container.querySelectorAll("#ClearFilters"));
    const otherClick = jest.fn();
    const ownClick = jest.fn();
    otherButton.addEventListener("click", otherClick);
    ownButton.addEventListener("click", ownClick);

    fireEvent.click(container.querySelector("[grid-id='Grd']").querySelector(hook("grid-empty-action")));

    expect(ownClick).toHaveBeenCalledTimes(1);
    expect(otherClick).not.toHaveBeenCalled();
  });

  describe("before the first load", () => {
    it("is not shown while the initial load did not bring the model", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState({ model: null, attributes: { initialLoad: "query" } })
      });

      expect(container.querySelector(EMPTY)).toBeNull();
    });

    it("is shown once the model arrives", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState({ attributes: { initialLoad: "query" } })
      });

      expect(container.querySelector(EMPTY)).not.toBeNull();
    });

    it("is shown once a load finished", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState({ model: null, attributes: { initialLoad: "query", loading: false } })
      });

      expect(container.querySelector(EMPTY)).not.toBeNull();
    });

    it("is shown for a grid without initial load", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({ model: null }) });

      expect(container.querySelector(EMPTY)).not.toBeNull();
    });
  });
});
