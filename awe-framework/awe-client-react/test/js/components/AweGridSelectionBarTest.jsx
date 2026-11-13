import React from "react";
import { act, fireEvent, screen } from "@testing-library/react";

import i18n from "../i18nForTests";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { ADD_ACTIONS_TOP } from "../../../src/redux/actions/actions";
import { ButtonTypes, updateModel } from "../../../src/redux/actions/components";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

const hook = (id) => `[data-testid="${id}"]`;

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

const quickFilterValues = (chosen) => [{ value: "", label: "All" }, { value: "no2fa", label: "Without 2FA" }]
  .map(option => ({ ...option, selected: option.value === chosen }));

/**
 * Add the quick filter (CrtSta) and the search (CrtSch) of the grid to a state
 * @param {object} state State
 * @param {string|null} quickFilter Chosen option of the quick filter, null without quick filter
 * @param {string} search Text of the search
 * @returns {object} State
 */
const withCriteria = (state, quickFilter, search) => {
  const criterion = (component, values) => ({ address: { component, view: "report" }, attributes: { visible: true }, model: { values } });
  return { ...state, components: {
    ...state.components,
    ...(quickFilter === null ? {} : { CrtSta: criterion("CrtSta", quickFilterValues(quickFilter)) }),
    CrtSch: criterion("CrtSch", [{ value: search, selected: true }])
  } };
};

const context = () => bar()?.querySelector(hook("grid-selection-context")) || null;
const addedActions = (dispatchSpy) => dispatchSpy.mock.calls.map(([action]) => action)
  .filter(action => action?.type === ADD_ACTIONS_TOP).flatMap(action => action.actions);
const rowActions = (dispatchSpy, type) => addedActions(dispatchSpy).filter(action => action.type === type);
const clearActions = (dispatchSpy) => rowActions(dispatchSpy, "unselect-all-rows");

// The rows are selected as a click on them does
const selectRows = (store, selected) => act(() => {
  store.dispatch(updateModel({ component: "grid", view: "report" }, { selected }));
});

// Selection bar of a grid (#793): under the toolbar while there are selected rows, with the selection buttons of the grid
describe("grid selection bar", () => {
  beforeAll(() => {
    i18n.addResourceBundle("en", "translation", {
      SCREEN_TEXT_GRID_SELECTED_ROW: "{{count}} selected (one)",
      SCREEN_TEXT_GRID_SELECTED_ROWS: "{{count}} selected",
      SCREEN_TEXT_GRID_CLEAR_SELECTION: "Clear selection",
      SCREEN_TEXT_FILTERED_BY: "Filtered by"
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

  it("draws the bar in the band of the toolbar, keeping the Columns and Maximize buttons", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [1]) });

    const toolbar = document.querySelector(hook("grid-toolbar"));
    expect(toolbar.contains(bar())).toBe(true);
    expect(toolbar.querySelector(hook("grid-columns-button"))).not.toBeNull();
    expect(toolbar.querySelector(hook("grid-maximize-button"))).not.toBeNull();
  });

  it("draws the bar over the column header of a grid without toolbar, leaving the select-all checkbox", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [1],
      { showToolbar: false }) });

    expect(document.querySelector(hook("grid-toolbar"))).toBeNull();
    const contents = document.querySelector("[role='grid-container']");
    expect(contents.contains(bar())).toBe(true);
    expect(contents.querySelector("thead " + hook("grid-header-checkbox"))).not.toBeNull();
    expect(bar().querySelector(hook("grid-selection-count")).textContent).toBe("1 selected (one)");
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

  it("is a region named by its count, which is announced politely, with a decorative check icon", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [1]) });

    const region = screen.getByRole("region", { name: "1 selected (one)" });
    expect(region).toBe(bar());
    expect(bar().querySelector(hook("grid-selection-count")).getAttribute("aria-live")).toBe("polite");
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
    expect(reset.className).not.toContain("awe-button-spaced");
    expect(remove.className).toContain("p-button-danger");
    expect(remove.className).not.toContain("p-button-text");
  });

  it("leaves a grid without selection buttons as it was: no bar, every button in the footer", () => {
    renderWithProviders(<AweGrid id="grid" />, { preloadedState: selectionState([selectionButton("ButNew"), selectionButton("ButDel")], [1]) });

    expect(bar()).toBeNull();
    expect(footerButtons()).toEqual(["ButNew", "ButDel"]);
    expect(document.querySelector("#ButNew").className).toContain("awe-button-spaced");
    expect(document.querySelector("#ButNew").className).toContain("p-button-outlined");
  });

  it("shows the active quick filter, but not the all option, and the search text as the context of the selection", () => {
    const state = withCriteria(selectionState([selectionButton("ButDel", { selection: true })], [1],
      { quickFilter: "CrtSta", search: "CrtSch" }), "no2fa", "john");
    const { store } = renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });

    expect(context().textContent).toBe("Filtered by Without 2FA “john”");

    act(() => {
      store.dispatch(updateModel({ component: "CrtSta", view: "report" }, { values: quickFilterValues("") }));
      store.dispatch(updateModel({ component: "CrtSch", view: "report" }, { values: [{ value: "", selected: true }] }));
    });
    expect(context()).toBeNull();
  });

  it("clears the whole selection with its clear button", () => {
    const { dispatchSpy, store } = renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: selectionState([selectionButton("ButDel", { selection: true })], [1, 2]), spyDispatch: true });

    fireEvent.click(screen.getByRole("button", { name: "Clear selection (Esc)" }));

    expect(clearActions(dispatchSpy)).toEqual([{ type: "unselect-all-rows", address: { component: "grid", view: "report" } }]);
    // Clearing the selection is immediate: it does not disable the buttons of the screen
    expect(store.getState().actions.running).toBe(false);
  });

  it.each([[[2]], [[1, 3]]])("clears the selection of %j with Esc in the grid when no row is being edited", (selected) => {
    const { dispatchSpy } = renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: selectionState([selectionButton("ButDel", { selection: true })], selected), spyDispatch: true });

    fireEvent.keyDown(document.querySelector("[role='grid-container']"), { key: "Escape" });

    expect(clearActions(dispatchSpy)).toEqual([{ type: "unselect-all-rows", address: { component: "grid", view: "report" } }]);
    expect(rowActions(dispatchSpy, "cancel-row")).toEqual([]);
  });

  it.each([
    ["the search", { search: "CrtSch" }, () => document.querySelector(hook("grid-search") + " input")],
    ["the select-all checkbox", {}, () => document.querySelector(hook("grid-header-checkbox") + " input")]
  ])("gives the focus to %s once the selection is cleared, and not when a row is selected", (_, attributes, target) => {
    const { store } = renderWithProviders(<AweGrid id="grid" />, {
      preloadedState: withCriteria(selectionState([selectionButton("ButDel", { selection: true })], [], attributes), null, "") });
    const focused = document.activeElement;

    selectRows(store, [2]);
    expect(document.activeElement).toBe(focused);

    fireEvent.click(screen.getByRole("button", { name: "Clear selection (Esc)" }));
    selectRows(store, []);
    expect(target()).not.toBeNull();
    expect(document.activeElement).toBe(target());
  });

  it("shows the bar in a tree grid too", () => {
    renderWithProviders(<AweTreeGrid id="grid" />, { preloadedState: selectionState(
      [selectionButton("ButNew"), selectionButton("ButDel", { selection: true })], [2], { treegrid: true }) });

    expect(bar().classList.contains("is-visible")).toBe(true);
    expect(barButtons()).toEqual(["ButDel"]);
    expect(footerButtons()).toEqual(["ButNew"]);
  });
});
