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

  it("shows the bar in a tree grid too", () => {
    renderWithProviders(<AweTreeGrid id="grid" />, { preloadedState: selectionState(
      [selectionButton("ButNew"), selectionButton("ButDel", { selection: true })], [2], { treegrid: true }) });

    expect(bar().classList.contains("is-visible")).toBe(true);
    expect(barButtons()).toEqual(["ButDel"]);
    expect(footerButtons()).toEqual(["ButNew"]);
  });
});
