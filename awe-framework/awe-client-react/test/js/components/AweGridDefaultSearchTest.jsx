import React from "react";
import {act, fireEvent} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGridToolbar from "../../../src/components/AweGridToolbar";
import AweGridColumnFilterChips from "../../../src/components/AweGridColumnFilterChips";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";
import {buildComponent} from "../../../src/redux/thunks/screen";
import {changeColumnFiltersGridAction, changeSortGridAction} from "../../../src/redux/thunks/grid";

// Default saved search of a grid (#945, S2-S6): the grid opens with the default search the server applied before its
// initial load, shows its chip and tags it in its saved searches; Clear all of the column filters goes back to the XML
// state
describe("awe-react-client/test/js/components/AweGridDefaultSearchTest.jsx", () => {
  const originalFetch = global.fetch;
  const VIEW = "report";
  const STATUS_FILTER = {id: "Sta", fields: ["Sta"], type: "values", values: ["ACT"]};
  // The default search the server applied on load (the grid controller carries it)
  const DEFAULT_SEARCH = {id: 9, name: "Mine", columnFilters: [STATUS_FILTER],
    columnFilterLabels: {Sta: {ACT: "Active"}}, sort: [{id: "Nam", direction: "desc"}], quickFilter: "LCK", search: "jo"};
  let server;

  const byTestId = (id) => document.querySelector(`[data-testid="${id}"]`);
  const allByTestId = (id) => [...document.querySelectorAll(`[data-testid="${id}"]`)];
  const button = () => byTestId("window-saved-searches");
  const appliedChip = () => byTestId("window-saved-search-applied-default");
  const item = (id) => document.querySelector(`[data-testid="window-saved-search-item"][data-saved-search-id="${id}"]`);
  const row = (id) => document.querySelector(`[data-testid="window-saved-search-row"][data-saved-search-id="${id}"]`);
  const fill = (rows) => [{type: "fill", parameters: {datalist: {rows, total: 1, page: 1, records: rows.length}}}];
  const answer = (body) => Promise.resolve({ok: true, status: 200, json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body))});
  const calls = (target) => global.fetch.mock.calls.filter(([url]) => url.endsWith(`/${target}`))
    .map(([, options]) => JSON.parse(options.body));
  const queued = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async.flat()];
  const chips = () => allByTestId("grid-column-filter-chip").map(chip => chip.getAttribute("column-id"));
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
  // Run the sort and column filter changes queued for the grid, as the action stack would
  const runQueued = (store) => act(() => queued(store).forEach(action => {
    if (action.type === "change-sort") {
      store.dispatch(changeSortGridAction(action));
    } else if (action.type === "change-column-filters") {
      store.dispatch(changeColumnFiltersGridAction(action));
    }
  }));

  // Grid controller as the server sends it: a text, a values and a plain column, a quick filter and a search
  const controller = (attributes = {}) => ({
    id: "grid",
    component: "grid",
    columnModel: [
      {name: "Nam", label: "NAME", sortField: "Nam", filter: "text"},
      {name: "Sta", label: "STATUS", sortField: "Sta", filter: "values"},
      {name: "Amt", label: "AMOUNT", sortField: "Amt"}
    ],
    quickFilter: "QckSta",
    search: "SchUsr",
    savedSearches: true,
    defaultSearch: DEFAULT_SEARCH,
    visible: true,
    ...attributes
  });

  // State of the screen as it is loaded: the grid built from its controller, its quick filter and its search with the
  // values of the default search already in their models
  const loadedState = (attributes) => ({
    settings: {...DEFAULT_SETTINGS, token: "token"},
    components: {
      grid: buildComponent(VIEW, {id: "grid", controller: controller(attributes), model: {values: []}}, DEFAULT_SETTINGS),
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

  const renderGrid = (attributes) => renderWithProviders(<>
    <AweGridToolbar id="grid"/>
    <AweGridColumnFilterChips id="grid"/>
  </>, {preloadedState: loadedState(attributes)});

  beforeEach(() => {
    server = {searches: [], profiles: []};
    global.fetch = jest.fn((url) => {
      const target = url.split("/").pop();
      if (target === "getWindowSavedSearches") {
        return answer(fill(server.searches));
      }
      return answer(target === "getSavedSearchProfiles" ? fill(server.profiles) : []);
    });
    ViewRegistry.setView(VIEW, {name: "UsrLst", loading: false});
  });

  afterEach(() => {
    global.fetch = originalFetch;
    ViewRegistry.clearAll();
  });

  it("seeds the column filters with their labels and the sort of the default search on load, without a reload (S2)", () => {
    const {store} = renderGrid();

    expect(chips()).toEqual(["Sta"]);
    expect(byTestId("grid-column-filter-chip").textContent).toBe("STATUS: Active");
    expect(store.getState().components.grid.specificAttributes.sort).toEqual([{id: "Nam", direction: "desc", order: -1}]);
    expect(queued(store)).toEqual([]);
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("shows the chip of the applied default search next to the saved searches of the grid (S3)", () => {
    renderGrid();

    expect(appliedChip().getAttribute("data-testid-owner")).toBe("grid");
    expect(appliedChip().textContent).toBe("SCREEN_TEXT_SAVED_SEARCH_APPLIED_DEFAULT: Mine");
    expect(button().nextElementSibling).toBe(appliedChip());
  });

  it("tags the default searches in the menu (S3)", async () => {
    server.searches = [{id: 9, name: "Mine", own: 1, profile: "", default: 1, values: "{}"},
      {id: 4, name: "Team", own: 0, profile: "ADM", profileDefault: 1, values: "{}"}];
    renderGrid();

    await openMenu();

    expect(item(9).querySelector("[data-testid='window-saved-search-default-tag']")).not.toBeNull();
    expect(item(4).querySelector("[data-testid='window-saved-search-profile-default-tag']")).not.toBeNull();
    expect(item(4).querySelector("[data-testid='window-saved-search-default-tag']")).toBeNull();
  });

  it("sets the defaults of the grid from the manage dialog, the profile one only for a publisher (S3)", async () => {
    server.searches = [{id: 9, name: "Mine", own: 1, profile: "", default: 1, values: "{}"},
      {id: 4, name: "Team", own: 0, profile: "ADM", values: "{}"}];
    server.profiles = [{value: "ADM", label: "Administrator"}];
    renderGrid();
    await openMenu();
    fireEvent.click(byTestId("window-saved-search-manage"));
    await flush();

    expect(row(9).querySelector("[data-testid='window-saved-search-set-profile-default']")).toBeNull();
    fireEvent.click(row(4).querySelector("[data-testid='window-saved-search-set-default']"));
    await flush();
    fireEvent.click(row(4).querySelector("[data-testid='window-saved-search-set-profile-default']"));
    await flush();
    fireEvent.click(row(9).querySelector("[data-testid='window-saved-search-set-default']"));
    await flush();

    expect(calls("setWindowDefaultSearch")).toEqual([
      {savedSearchScreen: "UsrLst", savedSearchWindow: "grid", savedSearchId: 4},
      {savedSearchScreen: "UsrLst", savedSearchWindow: "grid", savedSearchId: ""}
    ]);
    expect(calls("setProfileDefaultSearch")).toEqual([
      {savedSearchScreen: "UsrLst", savedSearchWindow: "grid", savedSearchId: 4, savedSearchDefault: "1"}
    ]);
  });

  it("offers no profile default to a user who may not publish (S3)", async () => {
    server.searches = [{id: 4, name: "Team", own: 0, profile: "ADM", values: "{}"}];
    renderGrid();
    await openMenu();
    fireEvent.click(byTestId("window-saved-search-manage"));
    await flush();

    expect(row(4).querySelector("[data-testid='window-saved-search-set-default']")).not.toBeNull();
    expect(row(4).querySelector("[data-testid='window-saved-search-set-profile-default']")).toBeNull();
  });

  it("offers to open the screen with the search, but no layout or values to keep, in the save dialog of a grid", async () => {
    renderGrid();
    await openMenu();
    fireEvent.click(byTestId("window-saved-search-save"));
    await flush();

    expect(byTestId("window-saved-search-open-with")).not.toBeNull();
    expect(byTestId("window-saved-search-keep-layout")).toBeNull();
    expect(byTestId("window-saved-search-keep-values")).toBeNull();
  });

  it("goes back to the XML state on Clear all and hides the chip; applying the default again shows it (S4)", async () => {
    server.searches = [{id: 9, name: "Mine", own: 1, profile: "", default: 1, values: JSON.stringify(DEFAULT_SEARCH)},
      {id: 5, name: "Other", own: 1, profile: "", values: JSON.stringify({columnFilters: [], sort: []})}];
    const {store} = renderGrid();

    fireEvent.click(byTestId("grid-column-filter-clear-all"));
    const grid = {component: "grid", view: VIEW};
    expect(queued(store).map(action => [action.type, action.address, action.parameters])).toEqual([
      ["change-sort", grid, {sort: []}],
      ["change-column-filters", grid, {columnFilters: [], labels: {}, defaultSearchApplied: false}],
      ["filter", grid, undefined]
    ]);
    runQueued(store);
    expect(store.getState().components.grid.specificAttributes.sort).toEqual([]);
    expect(chips()).toEqual([]);
    expect(appliedChip()).toBeNull();

    await openMenu();
    fireEvent.click(item(9));
    await flush();
    runQueued(store);
    expect(appliedChip().textContent).toContain("Mine");
    expect(chips()).toEqual(["Sta"]);

    await openMenu();
    fireEvent.click(item(5));
    await flush();
    runQueued(store);
    expect(appliedChip()).toBeNull();
  });

  it("removes one column filter of the default search and keeps its chip and sort", () => {
    const {store} = renderGrid();

    fireEvent.click(byTestId("grid-column-filter-chip-remove"));

    expect(queued(store).map(action => [action.type, action.parameters])).toEqual([
      ["change-column-filters", {columnFilters: [], labels: {}}],
      ["filter", undefined]
    ]);
    runQueued(store);
    expect(appliedChip()).not.toBeNull();
  });

  it("ignores the filters and sorts of columns the grid no longer has (S5)", () => {
    const {store} = renderGrid({defaultSearch: {...DEFAULT_SEARCH,
      columnFilters: [STATUS_FILTER, {id: "Gone", fields: ["Gone"], type: "values", values: [1]}],
      columnFilterLabels: {Sta: {ACT: "Active"}, Gone: {1: "One"}},
      sort: [{id: "Gone", direction: "asc"}, {id: "Nam", direction: "desc"}]}});

    expect(chips()).toEqual(["Sta"]);
    expect(store.getState().components.grid.attributes.columnFilterLabels).toEqual({Sta: {ACT: "Active"}});
    expect(store.getState().components.grid.specificAttributes.sort).toEqual([{id: "Nam", direction: "desc", order: -1}]);
    expect(appliedChip()).not.toBeNull();
  });

  it("does not change a grid without saved searches, nor one without a default search (S6)", () => {
    const {store, unmount} = renderGrid({savedSearches: false});

    expect(store.getState().components.grid.specificAttributes).toEqual(expect.objectContaining({sort: []}));
    expect(store.getState().components.grid.specificAttributes.columnFilters).toBeUndefined();
    expect(chips()).toEqual([]);
    expect(appliedChip()).toBeNull();
    unmount();

    renderGrid({defaultSearch: undefined});
    expect(button()).not.toBeNull();
    expect(chips()).toEqual([]);
    expect(appliedChip()).toBeNull();
  });
});
