import React from "react";
import {act, fireEvent} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGridToolbar from "../../../src/components/AweGridToolbar";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";

// Saved searches of a grid (#912, S20-S21): the Saved searches menu of the toolbar of a grid with saved-searches="true"
describe("awe-react-client/test/js/components/AweGridSavedSearchesTest.jsx", () => {
  const originalFetch = global.fetch;
  const VIEW = "report";
  let server;

  const byTestId = (id) => document.querySelector(`[data-testid="${id}"]`);
  const button = () => byTestId("window-saved-searches");
  const item = (id) => document.querySelector(`[data-testid="window-saved-search-item"][data-saved-search-id="${id}"]`);
  const fill = (rows) => [{type: "fill", parameters: {datalist: {rows, total: 1, page: 1, records: rows.length}}}];
  const answer = (body) => Promise.resolve({ok: true, status: 200, json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body))});
  const calls = (target) => global.fetch.mock.calls.filter(([url]) => url.endsWith(`/${target}`))
    .map(([, options]) => JSON.parse(options.body));
  const queued = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async.flat()];
  const selected = (store, id) => store.getState().components[id].model.values
    .filter(option => option.selected).map(option => option.value);
  const flush = async () => {
    for (let index = 0; index < 6; index++) {
      await act(async () => {
        await Promise.resolve();
      });
    }
  };
  const openMenu = async () => {
    fireEvent.click(button());
    await flush();
  };

  // Grid with a text, a values and a plain column, a quick filter and a search, filtered by status, sorted by amount
  const gridState = (attributes = {savedSearches: true}) => ({
    settings: {...DEFAULT_SETTINGS, token: "token"},
    components: {
      grid: {
        address: {component: "grid", view: VIEW},
        model: {values: []},
        attributes: {
          columnModel: [
            {name: "Nam", label: "NAME", sortField: "Nam", filter: "text"},
            {name: "Sta", label: "STATUS", sortField: "Sta", filter: "values"},
            {name: "Amt", label: "AMOUNT", sortField: "Amt"}
          ],
          quickFilter: "QckSta",
          search: "SchUsr",
          columnFilterLabels: {Sta: {ACT: "Active"}},
          visible: true,
          ...attributes
        },
        specificAttributes: {
          columnFilters: [{id: "Sta", fields: ["Sta"], type: "values", values: ["ACT"]}],
          sort: [{id: "Amt", order: -1, direction: "desc"}],
          page: 3
        }
      },
      QckSta: {
        address: {component: "QckSta", view: VIEW},
        model: {values: [{value: "", label: "All"}, {value: "LCK", label: "Locked", selected: true}]},
        attributes: {visible: true},
        specificAttributes: {}
      },
      SchUsr: {
        address: {component: "SchUsr", view: VIEW},
        model: {values: [{value: "jo", selected: true}]},
        attributes: {visible: true},
        specificAttributes: {}
      }
    }
  });

  beforeEach(() => {
    server = {searches: []};
    global.fetch = jest.fn((url) => {
      const target = url.split("/").pop();
      if (target === "getWindowSavedSearches") {
        return answer(fill(server.searches));
      }
      return answer(target === "getSavedSearchProfiles" ? fill([]) : []);
    });
    ViewRegistry.setView(VIEW, {name: "UsrLst", loading: false});
  });

  afterEach(() => {
    global.fetch = originalFetch;
    ViewRegistry.clearAll();
  });

  it("is in the toolbar, before Columns, only for a grid with savedSearches", () => {
    const {unmount} = renderWithProviders(<AweGridToolbar id="grid"/>, {preloadedState: gridState({})});
    expect(button()).toBeNull();
    unmount();

    renderWithProviders(<AweGridToolbar id="grid"/>, {preloadedState: gridState()});

    expect(button().getAttribute("data-testid-owner")).toBe("grid");
    expect(button().nextElementSibling).toBe(byTestId("grid-columns-button"));
  });

  it("saves the column filters with their labels, the sort, the quick filter and the search of the grid", async () => {
    renderWithProviders(<AweGridToolbar id="grid"/>, {preloadedState: gridState()});
    await openMenu();
    fireEvent.click(byTestId("window-saved-search-save"));
    await flush();

    fireEvent.change(byTestId("window-saved-search-name"), {target: {value: "Locked active"}});
    fireEvent.click(byTestId("window-saved-search-accept"));
    await flush();

    expect(calls("saveWindowSearch")).toEqual([{savedSearchScreen: "UsrLst", savedSearchWindow: "grid",
      savedSearchName: "Locked active", savedSearchValues: expect.any(String)}]);
    expect(JSON.parse(calls("saveWindowSearch")[0].savedSearchValues)).toEqual({
      columnFilters: [{id: "Sta", fields: ["Sta"], type: "values", values: ["ACT"]}],
      columnFilterLabels: {Sta: {ACT: "Active"}},
      sort: [{id: "Amt", direction: "desc"}],
      quickFilter: "LCK",
      search: "jo"
    });
  });

  it("restores the state of a search on the columns the grid has and reloads the grid from its first page", async () => {
    server.searches = [{id: 1, name: "Ana", own: 1, profile: "", values: JSON.stringify({
      columnFilters: [{id: "Nam", fields: ["Nam"], type: "text", conditions: [{operator: "contains", value: "an"}]},
        {id: "Gone", fields: ["Gone"], type: "values", values: [1]}],
      columnFilterLabels: {Gone: {1: "One"}},
      sort: [{id: "Nam", direction: "asc"}, {id: "Gone", direction: "desc"}],
      quickFilter: "",
      search: "ana"
    })}];
    const {store} = renderWithProviders(<AweGridToolbar id="grid"/>, {preloadedState: gridState()});
    await openMenu();

    fireEvent.click(item(1));
    await flush();

    expect(selected(store, "QckSta")).toEqual([""]);
    expect(selected(store, "SchUsr")).toEqual(["ana"]);
    const grid = {component: "grid", view: VIEW};
    expect(queued(store).map(action => [action.type, action.address, action.parameters])).toEqual([
      ["change-sort", grid, {sort: [{id: "Nam", direction: "asc", order: 1}]}],
      ["change-column-filters", grid, {columnFilters: [{id: "Nam", fields: ["Nam"], type: "text",
        conditions: [{operator: "contains", value: "an"}]}], labels: {}}],
      ["filter", grid, undefined]
    ]);
  });
});
