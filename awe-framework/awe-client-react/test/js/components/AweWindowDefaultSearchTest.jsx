import {act, fireEvent} from "@testing-library/react";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";
import {restoreAction} from "../../../src/redux/thunks/form";
import * as fixtures from "./fixtures/windowAdvancedSearch";

// Default search and layout of the saved searches of a window (#915): the save dialog keeps the layout (the advanced
// criteria in use shown with the essential ones) and the values, and can make the search the default one; the menu and
// the manage dialog mark and change the default searches; the window opens with the default search the server applied,
// shows its chip, and its restores go back to it
describe("awe-react-client/test/js/components/AweWindowDefaultSearchTest.jsx", () => {
  const {VIEW, select, PROFILES, byTestId, field, badge, chip, selected, update, calls, queued, flush, renderWindow,
    savedState, renderSaved, item, openMenu, nameInput, openSaveDialog, openManageDialog, manageRow} = fixtures;
  const originalFetch = global.fetch;
  // The default search the server applied on load: Theme with the essential criteria, User bob and Theme Dark
  const DEFAULT_SEARCH = {id: 7, name: "Mine", layout: ["CrtThm"],
    values: {CrtUsr: [{value: "bob", label: "Bob"}], CrtThm: [{value: "2", label: "Dark"}]}};
  let server;

  // A window with saved searches (and the default search the server applied on load), with its context in the screen
  const windowState = (attributes = {}) => {
    const state = savedState({savedSearches: true, ...attributes});
    state.components.SearchWindow.context = {view: VIEW, source: ["SearchWindow"]};
    return state;
  };
  const renderDefault = (props = {}, attributes = {defaultSearch: DEFAULT_SEARCH}) =>
    renderWindow(props, {preloadedState: windowState(attributes)});
  // Criteria in the main row, in order: the ones of the window body out of the advanced block
  const mainRow = () => [...document.querySelectorAll("#SearchBody [criterion-id]")]
    .filter(element => !element.closest(".window-advanced"))
    .map(element => element.getAttribute("criterion-id"));
  const inRegion = (id) => Boolean(field(id).closest(".window-advanced-region"));
  const appliedChip = () => byTestId("window-saved-search-applied-default");
  const check = (testId) => fireEvent.click(byTestId(testId).querySelector("input"));
  const isChecked = (testId) => byTestId(testId).getAttribute("data-selected") === "true";
  const saveAs = async (name) => {
    fireEvent.change(nameInput(), {target: {value: name}});
    fireEvent.click(byTestId("window-saved-search-accept"));
    await flush();
  };
  const savedValues = () => JSON.parse(calls("saveWindowSearch")[0].savedSearchValues);
  const applySearch = async (id) => {
    await openMenu();
    fireEvent.click(item(id));
    await flush();
  };

  beforeEach(() => {
    server = fixtures.installSavedSearchServer();
  });

  afterEach(() => {
    global.fetch = originalFetch;
    ViewRegistry.clearAll();
  });

  describe("save dialog (S1, S2)", () => {
    it("keeps the advanced criteria in use as the layout only when asked, and only the layout without the values", async () => {
      renderSaved();
      await openSaveDialog();

      // Opt-in: a plain save keeps the values only, as before #915
      expect(isChecked("window-saved-search-keep-layout")).toBe(false);
      expect(isChecked("window-saved-search-keep-values")).toBe(true);
      expect(isChecked("window-saved-search-open-with")).toBe(false);
      expect(byTestId("window-saved-search-keep-layout").getAttribute("data-testid-owner")).toBe("SearchWindow");
      expect(byTestId("window-saved-search-dialog").textContent).toContain("Theme, Blocked. SCREEN_TEXT_SAVED_SEARCH_KEEP_LAYOUT_HELP");

      check("window-saved-search-keep-layout");
      check("window-saved-search-keep-values");
      expect(isChecked("window-saved-search-keep-layout")).toBe(true);
      await saveAs("Layout only");

      expect(savedValues()).toEqual({$layout: ["CrtThm", "CrtBlk"]});
    });

    it("keeps no layout when no advanced criterion is in use", async () => {
      const {store} = renderSaved();
      update(store, "CrtThm", select("1"));
      update(store, "CrtBlk", [{value: 1, selected: false}]);
      await openSaveDialog();

      expect(isChecked("window-saved-search-keep-layout")).toBe(false);
      await saveAs("Mine");

      expect(savedValues()).toEqual({CrtUsr: [{value: "ana", label: "ana"}], CrtThm: [{value: "1", label: "Light"}],
        CrtBlk: []});
    });

    it("makes the saved search the default search of the user with Open the screen with this search", async () => {
      server.searches = [...fixtures.SEARCHES, {id: 3, name: "Mine", own: 1, profile: "", values: "{}"}];
      renderSaved();
      await openSaveDialog();

      check("window-saved-search-open-with");
      await saveAs("Mine");
      await flush();

      expect(calls("setWindowDefaultSearch")).toEqual([
        {savedSearchScreen: "UsrLst", savedSearchWindow: "SearchWindow", savedSearchId: 3}]);
      expect(global.fetch.mock.calls.map(([url]) => url.split("/").slice(-2).join("/"))).toEqual([
        "data/getSavedSearchProfiles", "data/getWindowSavedSearches", "maintain-silent/saveWindowSearch",
        "data/getWindowSavedSearches", "maintain-silent/setWindowDefaultSearch", "data/getWindowSavedSearches"]);
    });
  });

  describe("default searches in the menu and the manage dialog (S2, S3)", () => {
    const flagged = () => [{...fixtures.SEARCHES[0], default: 1, profileDefault: 0},
      {...fixtures.SEARCHES[1], default: 0, profileDefault: 1}];

    it("tags the default search of the user and the default search of its profile with a text", async () => {
      server.searches = flagged();
      renderSaved();

      await openMenu();

      expect(byTestId("window-saved-search-default-tag", item(1)).textContent).toBe("SCREEN_TEXT_SAVED_SEARCH_DEFAULT");
      expect(byTestId("window-saved-search-profile-default-tag", item(1))).toBeNull();
      expect(byTestId("window-saved-search-profile-default-tag", item(2)).textContent)
        .toBe("SCREEN_TEXT_SAVED_SEARCH_PROFILE_DEFAULT");
      expect(byTestId("window-saved-search-default-tag", item(2))).toBeNull();
    });

    it("sets and unsets the default search of the user on any search, and reloads the searches", async () => {
      server.searches = flagged();
      renderSaved();
      await openManageDialog();
      const toggle = (id) => byTestId("window-saved-search-set-default", manageRow(id));

      expect([toggle(1).getAttribute("aria-label"), toggle(1).getAttribute("data-selected")])
        .toEqual(["SCREEN_TEXT_SAVED_SEARCH_UNSET_DEFAULT Dark themes", "true"]);
      expect([toggle(2).getAttribute("aria-label"), toggle(2).getAttribute("data-selected")])
        .toEqual(["SCREEN_TEXT_SAVED_SEARCH_SET_DEFAULT Blocked users", "false"]);
      // Only a user who may publish changes the default search of a profile
      expect(byTestId("window-saved-search-set-profile-default")).toBeNull();

      fireEvent.click(toggle(1));
      await flush();
      fireEvent.click(toggle(2));
      await flush();

      expect(calls("setWindowDefaultSearch")).toEqual([
        {savedSearchScreen: "UsrLst", savedSearchWindow: "SearchWindow", savedSearchId: ""},
        {savedSearchScreen: "UsrLst", savedSearchWindow: "SearchWindow", savedSearchId: 2}]);
      expect(calls("getWindowSavedSearches")).toHaveLength(3);
    });

    it("lets a publisher set and unset the default search of a profile on the published searches", async () => {
      server.searches = flagged();
      server.profiles = PROFILES;
      renderSaved();
      await openManageDialog();
      const toggle = byTestId("window-saved-search-set-profile-default", manageRow(2));

      expect(byTestId("window-saved-search-set-profile-default", manageRow(1))).toBeNull();
      expect(toggle.getAttribute("aria-label")).toBe("SCREEN_TEXT_SAVED_SEARCH_UNSET_PROFILE_DEFAULT Blocked users");
      fireEvent.click(toggle);
      await flush();

      expect(calls("setProfileDefaultSearch")).toEqual([{savedSearchScreen: "UsrLst", savedSearchWindow: "SearchWindow",
        savedSearchId: 2, savedSearchDefault: "0"}]);
      expect(calls("getWindowSavedSearches")).toHaveLength(2);
    });
  });

  describe("layout of a saved search (S1, S5)", () => {
    it("shows the criteria of its layout with the essential ones, as essential, and the screen layout without one", async () => {
      server.searches = [...fixtures.SEARCHES, {id: 3, name: "Theme at hand", own: 1, profile: "",
        values: JSON.stringify({CrtThm: [{value: "2", label: "Dark"}], CrtGone: [{value: "x"}],
          $layout: ["CrtGone", "CrtThm"]})}];
      const {store} = renderSaved();

      await applySearch(3);

      // The criteria the screen no longer has are ignored
      expect(mainRow()).toEqual(["CrtUsr", "CrtThm"]);
      expect(inRegion("CrtThm")).toBe(false);
      expect(inRegion("CrtBlk")).toBe(true);
      expect(selected(store, "CrtThm")).toEqual(["2"]);
      expect(queued(store, "submit")).toHaveLength(1);
      // Not counted nor hidden as an advanced criterion
      update(store, "CrtBlk", [{value: 1, selected: true}]);
      expect(badge().textContent).toBe("1");
      expect(chip("Theme").querySelector(".window-filter-chip-marker")).toBeNull();
      expect(chip("Blocked").querySelector(".window-filter-chip-marker")).not.toBeNull();

      await applySearch(1);

      expect(mainRow()).toEqual(["CrtUsr"]);
      expect(inRegion("CrtThm")).toBe(true);
      expect(badge().textContent).toBe("1");
    });

    it("leaves the criteria of its layout out of the side panel", async () => {
      renderDefault({advancedMode: "side-panel"});

      fireEvent.click(fixtures.toggle());
      await flush();

      const panel = byTestId("window-advanced-panel");
      expect(panel.contains(field("CrtBlk"))).toBe(true);
      expect(panel.contains(field("CrtThm"))).toBe(false);
      expect(mainRow()).toEqual(["CrtUsr", "CrtThm"]);
    });
  });

  describe("default search applied on load (S2, S4, S7)", () => {
    it("shows its layout and its chip, and neither sets its values again nor submits the window", async () => {
      const {store} = renderDefault();
      await flush();

      expect(mainRow()).toEqual(["CrtUsr", "CrtThm"]);
      expect(appliedChip().getAttribute("data-testid-owner")).toBe("SearchWindow");
      expect(appliedChip().textContent).toBe("SCREEN_TEXT_SAVED_SEARCH_APPLIED_DEFAULT: Mine");
      expect(fixtures.row().contains(appliedChip())).toBe(true);
      // The models carry the values the server loaded (a context variable won over User)
      expect(selected(store, "CrtUsr")).toEqual(["ana"]);
      expect(queued(store, "submit")).toEqual([]);
      expect(global.fetch).not.toHaveBeenCalled();
    });

    it("hides the chip when another search is applied", async () => {
      renderDefault();
      expect(appliedChip()).not.toBeNull();

      await applySearch(2);

      expect(appliedChip()).toBeNull();
      expect(mainRow()).toEqual(["CrtUsr"]);
    });

    it("goes back to the values of the definition with Clear all, hides the chip and keeps the layout", () => {
      const {store} = renderDefault();

      fireEvent.click(byTestId("window-filter-clear-all"));

      expect([selected(store, "CrtUsr"), selected(store, "CrtThm"), selected(store, "CrtBlk")]).toEqual([[], ["1"], []]);
      expect(appliedChip()).toBeNull();
      expect(mainRow()).toEqual(["CrtUsr", "CrtThm"]);
    });

    it("goes back to its values, its layout and its chip with a restore of the window, also after Clear all", async () => {
      const {store} = renderDefault();
      await applySearch(1);
      fireEvent.click(byTestId("window-filter-clear-all"));

      act(() => {
        store.dispatch(restoreAction({type: "restore", target: "SearchWindow", address: {view: VIEW}}));
      });

      expect([selected(store, "CrtUsr"), selected(store, "CrtThm"), selected(store, "CrtBlk")]).toEqual([["bob"], ["2"], []]);
      expect(mainRow()).toEqual(["CrtUsr", "CrtThm"]);
      expect(appliedChip()).not.toBeNull();
    });

    it("goes back to its values and its layout with Reset to defaults of the side panel", async () => {
      const {store} = renderDefault({advancedMode: "side-panel"});
      await applySearch(1);
      fireEvent.click(fixtures.toggle());
      await flush();

      fireEvent.click(byTestId("window-advanced-reset"));

      expect([selected(store, "CrtThm"), selected(store, "CrtBlk")]).toEqual([["2"], []]);
      expect(mainRow()).toEqual(["CrtUsr", "CrtThm"]);
    });

    it("is ignored by a window without saved searches, which keeps its look (S6)", () => {
      renderWindow({}, {preloadedState: windowState({savedSearches: undefined, defaultSearch: DEFAULT_SEARCH})});

      expect(appliedChip()).toBeNull();
      expect(byTestId("window-saved-searches")).toBeNull();
      expect([...document.getElementById("SearchBody").children]).toEqual([field("CrtUsr"), fixtures.block()]);
      expect(field("CrtThm").parentElement).toBe(fixtures.region());
    });
  });
});
