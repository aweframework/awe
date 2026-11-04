import React from "react";
import { act, fireEvent } from "@testing-library/react";

import i18n from "../i18nForTests";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { ADD_ACTIONS_TOP } from "../../../src/redux/actions/actions";
import { ButtonTypes } from "../../../src/redux/actions/components";
import { selectRowGridAction } from "../../../src/redux/thunks/grid";
import { getGridData } from "../../../src/utilities/grid";
import { renderWithProviders } from "../test-utils";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

const hook = (id) => `[data-testid="${id}"]`;

// A tree of three options: a parent with two children, all loaded and the parent open
const ROWS = [
  { id: "Root", parent: "", name: "Root", screen: "root-screen", status: "Allowed", tone: "success", $row: { expanded: true } },
  { id: "Child1", parent: "Root", name: "Child 1", screen: "child-1", status: "Denied", tone: "danger" },
  { id: "Child2", parent: "Root", name: "Child 2", screen: "child-2", status: "Allowed", tone: "success" }
];

const selectionButton = (id, extra = {}) => ({ id, label: `LABEL_${id}`, icon: "trash", visible: true, ...extra });

/**
 * State of a screen with a tree grid (the options of a menu, as the access window shows them), its toolbar search and
 * quick filter, its buttons and the rows selected
 * @param {object} attributes Tree grid attributes
 * @param {string[]} selected Identifiers of the selected rows
 * @returns {object} State
 */
function treeState(attributes = {}, selected = []) {
  const buttons = attributes.buttonModel || [];
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    actions: { running: false },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "access", option: "access" } },
    components: {
      GrdOpt: {
        address: { component: "GrdOpt", view: "report" },
        model: { values: ROWS.map(row => ({ ...row, selected: selected.includes(row.id) })), records: ROWS.length },
        attributes: {
          id: "GrdOpt", component: "grid", screenId: "Scr", treegrid: true, treeId: "id", treeParent: "parent",
          expandColumn: "name", loadAll: true, max: 30, visible: true, headerModel: [], buttonModel: [],
          columnModel: [
            { name: "name", label: "NAME", sortField: "name", secondaryField: "screen", sendable: true },
            { name: "status", label: "STATUS", sortField: "status", statusField: "tone", sendable: false },
            { name: "EdtBtn", component: "button", icon: "lucide:pencil", title: "Edit", style: "btn-ghost no-label",
              sendable: false, actions: [{ type: "server", parameters: { serverAction: "data", targetAction: "OptWhy" } },
                { type: "dialog", target: "PnlWhy" }] },
            { name: "screen", hidden: true, sendable: false },
            { name: "tone", hidden: true, sendable: false }
          ],
          ...attributes
        },
        specificAttributes: { sort: [] }
      },
      CrtTxt: {
        address: { component: "CrtTxt", view: "report" },
        model: { values: [] },
        attributes: { id: "CrtTxt", component: "grid-search", visible: true },
        specificAttributes: {}
      },
      CrtFlt: {
        address: { component: "CrtFlt", view: "report" },
        model: { values: [{ value: "", label: "All", count: 3, selected: true }, { value: "DEN", label: "Denied", count: 1 }] },
        attributes: { id: "CrtFlt", component: "quick-filter", visible: true },
        specificAttributes: {}
      },
      ...Object.fromEntries(buttons.map(definition => [definition.id, {
        address: { component: definition.id, view: "report" },
        attributes: { ...definition, buttonType: ButtonTypes.BUTTON_NORMAL },
        actions: [{ type: "server", parameters: { serverAction: "maintain", targetAction: `Mnt${definition.id}` } }]
      }]))
    }
  };
}

const render = (attributes, selected) => renderWithProviders(<AweTreeGrid id="GrdOpt" />,
  { preloadedState: treeState(attributes, selected), spyDispatch: true });

const rowOf = (container, id) => container.querySelector(`[tree-grid-id='GrdOpt'] [data-testid='grid-row'][row-id='${id}']`);
const cellOf = (container, id, column) => rowOf(container, id).querySelector(`${hook("grid-cell")}[column-id='${column}']`);

// Actions added to the top of the action stack
const addedActions = (dispatchSpy) => dispatchSpy.mock.calls
  .map(([action]) => action)
  .filter(action => action?.type === ADD_ACTIONS_TOP)
  .flatMap(action => action.actions);
const selectRowActions = (dispatchSpy) => addedActions(dispatchSpy).filter(action => action.type === "select-row");

// Runs a select-row action of the grid as the action queue does
const runSelectRow = (store, action) => act(() => {
  store.dispatch(selectRowGridAction(action));
});
const selectedIds = (store) => store.getState().components.GrdOpt.model.values.filter(row => row.selected).map(row => row.id);

const bar = () => document.querySelector(hook("grid-selection-bar"));
const rowCheckbox = (container, id) => rowOf(container, id).querySelector(hook("grid-row-checkbox"));
const clickCheckbox = (checkbox) => fireEvent.click(checkbox.querySelector("input"));

// The tree grid of the access window (#791): toolbar, rich cells, row buttons and checkbox multiselect, as the flat grid
describe("awe-react-client/test/js/components/AweTreeGridSelectionTest.jsx", () => {
  beforeAll(() => {
    i18n.addResourceBundle("en", "translation", {
      SCREEN_TEXT_GRID_SELECTED_ROW: "{{count}} selected (one)",
      SCREEN_TEXT_GRID_SELECTED_ROWS: "{{count}} selected"
    }, true, true);
  });

  beforeEach(() => {
    jest.spyOn(console, "error").mockImplementation(() => {});
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]), text: () => Promise.resolve("[]") }));
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe("toolbar and cells", () => {
    it("shows the search and the quick filter with its counts in the toolbar", () => {
      render({ search: "CrtTxt", quickFilter: "CrtFlt" });

      const toolbar = document.querySelector(hook("grid-toolbar"));
      expect(toolbar.querySelector(hook("grid-search")).getAttribute("data-testid-owner")).toBe("GrdOpt");
      expect(toolbar.querySelector(hook("grid-quick-filter")).getAttribute("data-testid-owner")).toBe("GrdOpt");
      const options = Array.from(toolbar.querySelectorAll(hook("grid-quick-filter-option")));
      expect(options.map(option => option.textContent)).toEqual([expect.stringContaining("3"), expect.stringContaining("1")]);
    });

    it("shows the status of a row with its tone and the secondary line under the main value", () => {
      const { container } = render();

      const status = cellOf(container, "Child1", "status").querySelector("[data-tone]");
      expect(status.getAttribute("data-tone")).toBe("danger");
      expect(status.textContent).toBe("Denied");
      expect(cellOf(container, "Child1", "name").textContent).toContain("Child 1");
      expect(cellOf(container, "Child1", "name").textContent).toContain("child-1");
    });

    it("launches the actions of a row button after selecting only its row", () => {
      const { container, dispatchSpy, store } = render({ multiselect: true }, ["Root", "Child2"]);

      const button = cellOf(container, "Child1", "EdtBtn").querySelector("button");
      expect(button.getAttribute("aria-label")).toBe("Edit");
      act(() => button.click());

      expect(selectedIds(store)).toEqual(["Child1"]);
      const added = addedActions(dispatchSpy);
      expect(added.map(action => action.type)).toEqual(["server", "dialog"]);
      expect(added[0].address).toEqual({ component: "GrdOpt", view: "report", column: "EdtBtn", row: "Child1" });
    });
  });

  describe("without multiselect", () => {
    it("shows no selection checkbox and selects only the clicked row", () => {
      const { container, dispatchSpy, store } = render({}, ["Root"]);

      expect(container.querySelector(hook("grid-row-checkbox"))).toBeNull();
      expect(container.querySelector(hook("grid-header-checkbox"))).toBeNull();
      fireEvent.click(cellOf(container, "Child2", "status"));

      const [action] = selectRowActions(dispatchSpy);
      expect(action.parameters).toEqual({ values: ["Child2"] });
      runSelectRow(store, action);
      expect(selectedIds(store)).toEqual(["Child2"]);
    });
  });

  describe("with multiselect", () => {
    it("shows a checkbox per row and a header checkbox, checked for the selected rows", () => {
      const { container } = render({ multiselect: true }, ["Child1"]);

      expect(container.querySelector(hook("grid-header-checkbox")).getAttribute("data-selected")).toBe("false");
      expect(["Root", "Child1", "Child2"].map(id => rowCheckbox(container, id).getAttribute("data-selected")))
        .toEqual(["false", "true", "false"]);
      expect(rowOf(container, "Child1").getAttribute("data-selected")).toBe("true");
    });

    it("adds a row to the selection with its checkbox and removes it with a second click", () => {
      const { container, dispatchSpy, store } = render({ multiselect: true }, ["Child1"]);

      clickCheckbox(rowCheckbox(container, "Child2"));
      // The click on the checkbox is not a click on the row as well
      expect(selectRowActions(dispatchSpy)).toHaveLength(1);
      const [add] = selectRowActions(dispatchSpy);
      expect(add.parameters).toEqual({ values: ["Child1", "Child2"] });
      runSelectRow(store, add);
      expect(selectedIds(store)).toEqual(["Child1", "Child2"]);

      clickCheckbox(rowCheckbox(container, "Child1"));
      const remove = selectRowActions(dispatchSpy)[1];
      expect(remove.parameters).toEqual({ values: ["Child2"] });
      runSelectRow(store, remove);
      expect(selectedIds(store)).toEqual(["Child2"]);
    });

    it("adds the clicked row to the selection, as the flat grid does", () => {
      const { container, dispatchSpy } = render({ multiselect: true }, ["Child1"]);

      fireEvent.click(cellOf(container, "Child2", "status"));

      expect(selectRowActions(dispatchSpy)[0].parameters).toEqual({ values: ["Child1", "Child2"] });
    });

    it("selects a parent without its children", () => {
      const { container, dispatchSpy, store } = render({ multiselect: true });

      clickCheckbox(rowCheckbox(container, "Root"));
      const [action] = selectRowActions(dispatchSpy);
      expect(action.parameters).toEqual({ values: ["Root"] });
      runSelectRow(store, action);

      expect(["Root", "Child1", "Child2"].map(id => rowCheckbox(container, id).getAttribute("data-selected")))
        .toEqual(["true", "false", "false"]);
    });

    it("selects every row with the header checkbox and clears the selection when all of them are selected", () => {
      const { container, dispatchSpy, store } = render({ multiselect: true }, ["Child1"]);

      clickCheckbox(container.querySelector(hook("grid-header-checkbox")));
      const [all] = selectRowActions(dispatchSpy);
      expect(all.parameters).toEqual({ values: ["Root", "Child1", "Child2"] });
      runSelectRow(store, all);
      expect(container.querySelector(hook("grid-header-checkbox")).getAttribute("data-selected")).toBe("true");

      clickCheckbox(container.querySelector(hook("grid-header-checkbox")));
      expect(selectRowActions(dispatchSpy)[1].parameters).toEqual({ values: [] });
    });

    it("shows the selection buttons in the selection bar with the number of selected rows", () => {
      const { container, dispatchSpy, store } = render({ multiselect: true,
        buttonModel: [selectionButton("ButNew"), selectionButton("ButAlw", { selection: true })] }, ["Root"]);

      clickCheckbox(rowCheckbox(container, "Child2"));
      runSelectRow(store, selectRowActions(dispatchSpy)[0]);

      expect(bar().getAttribute("data-count")).toBe("2");
      expect(bar().querySelector(hook("grid-selection-count")).textContent).toBe("2 selected");
      expect(Array.from(bar().querySelectorAll("button[id]")).map(button => button.id)).toEqual(["ButAlw"]);
    });

    it("sends the values of the selected rows of a column, as the flat grid does", () => {
      const { container, dispatchSpy, store } = render({ multiselect: true }, ["Root"]);

      clickCheckbox(rowCheckbox(container, "Child2"));
      runSelectRow(store, selectRowActions(dispatchSpy)[0]);

      const grid = store.getState().components.GrdOpt;
      const data = getGridData(grid, grid.model, {});
      expect(data["name.selected"]).toEqual(["Root", "Child 2"]);
      expect(data["GrdOpt.selected"]).toEqual(["Root", "Child2"]);
    });
  });
});
