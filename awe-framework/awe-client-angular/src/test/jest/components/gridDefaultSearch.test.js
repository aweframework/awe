// Default search of a grid (#945): the default saved search of a grid (of the user or of a profile) is applied by the
// server before its initial load. The grid seeds its column filters and its sort from it without a reload, names it in
// a chip, tags it in the menu and sets it as the window does; Clear all of the column filters drops its filters and its
// sort
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";
import {hook, serviceCall, text} from "./windowSearchTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {disposeModal} from "../../../main/resources/js/awe/services/modal";

const COLUMNS = [
  {name: "InlNam", id: "InlNam", label: "Name", sortField: "InlNam", sortable: true, filter: "text"},
  {name: "InlSta", id: "InlSta", label: "Status", sortField: "InlSta", sortable: true, filter: "values"},
  {name: "InlDat", id: "InlDat", label: "Created", sortField: "InlDatSrt", sortable: true, filter: "date", filterFields: "InlDat"}
];
const STATUS_FILTER = Object.freeze({id: "InlSta", fields: ["InlSta"], type: "values", values: ["1"]});
// The default search the server applied before the initial load: the rows came filtered and sorted
const DEFAULT_SEARCH = Object.freeze({id: 1, name: "Active", columnFilters: [STATUS_FILTER],
  columnFilterLabels: {InlSta: {1: "Active"}}, sort: [{id: "InlDatSrt", direction: "desc"}], quickFilter: "LCK"});

describe("awe-framework/awe-client-angular/src/test/jest/components/gridDefaultSearch.test.js", () => {
  let refs, grid, actionController, connection, $q, reload, searches, profiles, models;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ActionController", "Connection", "$q", "Component", (_ActionController_, _Connection_, _$q_, Component) => {
      actionController = _ActionController_;
      connection = _Connection_;
      $q = _$q_;
      // Every reload of a component, also the ones while the grid is mounted
      reload = jest.spyOn(Component.prototype, "reload").mockImplementation(() => undefined);
    }]);
    searches = [
      {id: 1, name: "Active", values: JSON.stringify(DEFAULT_SEARCH), profile: "", own: 1, default: 1, profileDefault: 0},
      {id: 2, name: "Recent", values: JSON.stringify({sort: [{id: "InlDatSrt", direction: "asc"}]}), profile: "", own: 1, default: 0, profileDefault: 0},
      {id: 3, name: "Locked", values: JSON.stringify({quickFilter: "LCK"}), profile: "OPE", own: 0, default: 0, profileDefault: 1}
    ];
    profiles = [];
  });

  afterEach(() => {
    $(".modal").each((index, modal) => disposeModal(modal));
    $(document.body).removeClass("modal-open").empty();
    cleanupHeavyComponentTest();
  });

  const digest = () => refs.$rootScope.$digest();
  const applied = () => grid.element.find(hook(TestIds.windowSavedSearchAppliedDefault));
  const filterChip = id => grid.element.find(`${hook(TestIds.gridColumnFilterChip)}[column-id='${id}']`);
  const item = id => grid.element.find(`${hook(TestIds.windowSavedSearchItem)}[data-saved-search-id='${id}']`);
  const row = id => $(`${hook(TestIds.windowSavedSearchRow)}[data-saved-search-id='${id}']`);
  const call = (target, index) => serviceCall(actionController, target, index);
  const parametersOf = (target, index) => call(target, index)[0].attr("parameters");
  const succeed = actions => {
    actions[1].launchAction();
    digest();
  };

  /**
   * Mount a grid with saved searches, a quick filter and a search, and a server that answers the saved search queries
   * @param {object} controller Grid controller attributes
   */
  const mount = (controller = {}) => {
    jest.spyOn(actionController, "addActionList").mockImplementation(() => undefined);
    grid = mountGrid(refs, {columnModel: COLUMNS.map(column => ({...column})), label: "Customers", savedSearches: true, ...controller});
    models = {
      QckSta: {selected: "LCK", defaultValues: "", values: [{value: "", label: "All"}, {value: "LCK", label: "Locked"}]},
      SchUsr: {selected: "jo", defaultValues: "", values: []}
    };
    // The quick filter and the search are criteria of the screen: only their models are read and changed
    grid.controller.quickFilter = "QckSta";
    grid.controller.search = "SchUsr";
    refs.$control.getAddressModel.mockImplementation(address => models[address.component] || grid.model);
    jest.spyOn(refs.$control, "changeModelAttribute").mockImplementation((address, attributes) => {
      Object.assign(models[address.component], attributes);
    });
    refs.$storage.get.mockReturnValue({base: {name: "UsrLst"}});
    jest.spyOn(connection, "post").mockImplementation(url => $q.resolve({data: [{type: "fill", parameters: {datalist: {
      rows: url.split("/").pop() === "getSavedSearchProfiles" ? profiles : searches}}}, {type: "end-load"}]}));
  };
  const mountWithDefault = (defaultSearch = DEFAULT_SEARCH) => mount({defaultSearch: _.cloneDeep(defaultSearch)});

  const open = () => {
    grid.element.find(hook(TestIds.windowSavedSearches)).trigger("click");
    digest();
  };
  const openManage = () => {
    open();
    grid.element.find(hook(TestIds.windowSavedSearchManage)).trigger("click");
    digest();
  };

  it("seeds the column filters with their chips and the sort of the default search, without a reload (S2)", () => {
    mountWithDefault();
    digest();

    expect(reload).not.toHaveBeenCalled();
    expect(grid.component.getSpecificFields()).toEqual({max: 10, page: 1, sort: [{id: "InlDatSrt", direction: "desc"}],
      columnFilters: [STATUS_FILTER]});
    expect(text(filterChip("InlSta"))).toContain("Status: Active");
    expect(grid.component.grid.api.grid.getColumn("InlDat").sort.direction).toBe("desc");
    // The quick filter and the search already have the values of the default search
    expect(models.QckSta.selected).toBe("LCK");
    expect(models.SchUsr.selected).toBe("jo");
  });

  it("names the default search applied in a chip next to its Saved searches button (S3)", () => {
    mountWithDefault();
    digest();

    expect(applied().attr("data-testid-owner")).toBe("Grid");
    expect(text(applied())).toContain("Active");
    expect(applied().prev().is(".window-saved-searches")).toBe(true);
  });

  it("applies the default search without the columns the grid no longer has (S5)", () => {
    mountWithDefault({...DEFAULT_SEARCH,
      columnFilters: [STATUS_FILTER, {id: "Gone", fields: ["Gone"], type: "values", values: [1]}],
      columnFilterLabels: {InlSta: {1: "Active"}, Gone: {1: "One"}},
      sort: [{id: "Gone", direction: "asc"}, {id: "InlDatSrt", direction: "desc"}]});
    digest();

    expect(grid.component.columnFilters).toEqual([STATUS_FILTER]);
    expect(grid.component.columnFilterLabels).toEqual({InlSta: {1: "Active"}});
    expect(grid.component.getSpecificFields().sort).toEqual([{id: "InlDatSrt", direction: "desc"}]);
    expect(filterChip("Gone").length).toBe(0);
  });

  it("tags the default search of the user and the default of a profile in the menu (S3)", () => {
    mount();
    open();

    expect(text(item(1).find(hook(TestIds.windowSavedSearchDefaultTag)))).toBe("SCREEN_TEXT_SAVED_SEARCH_DEFAULT");
    expect(item(1).find(hook(TestIds.windowSavedSearchDefaultTag)).attr("data-testid-owner")).toBe("Grid");
    expect(text(item(3).find(hook(TestIds.windowSavedSearchProfileDefaultTag)))).toBe("SCREEN_TEXT_SAVED_SEARCH_PROFILE_DEFAULT");
    expect(item(2).find(hook(TestIds.windowSavedSearchDefaultTag)).length).toBe(0);
  });

  it("sets and unsets the default search of the user with the grid id, and no profile default without publishing (S1)", () => {
    mount();
    openManage();
    const setDefault = id => row(id).find(hook(TestIds.windowSavedSearchSetDefault));

    expect(row(3).find(hook(TestIds.windowSavedSearchSetProfileDefault)).length).toBe(0);
    expect(setDefault(1).attr("data-selected")).toBe("true");
    setDefault(2).trigger("click");
    digest();
    expect(parametersOf("setWindowDefaultSearch")).toEqual(expect.objectContaining({
      savedSearchScreen: "UsrLst", savedSearchWindow: "Grid", savedSearchId: 2}));

    setDefault(1).trigger("click");
    digest();
    expect(parametersOf("setWindowDefaultSearch", 1).savedSearchId).toBe("");
  });

  it("lets a user who may publish set the default of a profile on the published searches of the grid (S1)", () => {
    profiles = [{value: "OPE", label: "Operator"}];
    mount();
    openManage();
    const setProfileDefault = id => row(id).find(hook(TestIds.windowSavedSearchSetProfileDefault));

    expect(setProfileDefault(1).length).toBe(0);
    setProfileDefault(3).trigger("click");
    digest();
    expect(parametersOf("setProfileDefaultSearch")).toEqual(expect.objectContaining({
      savedSearchScreen: "UsrLst", savedSearchWindow: "Grid", savedSearchId: 3, savedSearchDefault: "0"}));
  });

  it("offers to open the screen with the search it saves, and neither keeping the layout nor the values (S1)", () => {
    searches.push({id: 4, name: "Mine", values: "{}", profile: "", own: 1, default: 0, profileDefault: 0});
    mount();
    open();
    grid.element.find(hook(TestIds.windowSavedSearchSave)).trigger("click");
    digest();

    expect($(hook(TestIds.windowSavedSearchKeepLayout)).length).toBe(0);
    expect($(hook(TestIds.windowSavedSearchKeepValues)).length).toBe(0);
    $(hook(TestIds.windowSavedSearchName)).val("Mine").trigger("input").trigger("change");
    $(hook(TestIds.windowSavedSearchOpenWith)).trigger("click");
    digest();
    $(hook(TestIds.windowSavedSearchAccept)).trigger("click");
    digest();
    succeed(call("saveWindowSearch"));

    expect(parametersOf("setWindowDefaultSearch")).toEqual(expect.objectContaining({savedSearchWindow: "Grid", savedSearchId: 4}));
  });

  it("returns to the XML with Clear all and hides the chip; applying the default from the menu shows it again (S4)", () => {
    mountWithDefault();
    digest();

    grid.element.find(hook(TestIds.gridColumnFilterClearAll)).trigger("click");
    digest();

    expect(applied().length).toBe(0);
    expect(reload).toHaveBeenCalledTimes(1);
    expect(grid.component.getSpecificFields()).toEqual({max: 10, page: 1, sort: []});
    expect(grid.component.grid.api.grid.getColumn("InlDat").sort.direction).toBeUndefined();

    open();
    item(1).trigger("click");
    digest();

    expect(text(applied())).toContain("Active");
    expect(grid.component.columnFilters).toEqual([STATUS_FILTER]);
  });

  it("keeps the chip and the sort of the default search when one column filter is removed (S4)", () => {
    mountWithDefault();
    digest();

    filterChip("InlSta").find(hook(TestIds.gridColumnFilterChipRemove)).trigger("click");
    digest();

    expect(grid.component.columnFilters).toEqual([]);
    expect(grid.component.getSpecificFields().sort).toEqual([{id: "InlDatSrt", direction: "desc"}]);
    expect(text(applied())).toContain("Active");
  });

  it("opens a grid without a default search as before: no chip, no filters and no sort (S6)", () => {
    mount();
    digest();

    expect(applied().length).toBe(0);
    expect(reload).not.toHaveBeenCalled();
    expect(grid.component.getSpecificFields()).toEqual({max: 10, page: 1, sort: []});
  });
});
