// Saved searches of a grid (#912, S20-S21): the Saved searches menu of the toolbar of a grid with saved-searches="true"
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {disposeModal} from "../../../main/resources/js/awe/services/modal";

const hook = id => `[data-testid='${id}']`;

const COLUMNS = [
  {name: "InlNam", id: "InlNam", label: "Name", sortField: "InlNam", sortable: true, filter: "text"},
  {name: "InlSta", id: "InlSta", label: "Status", sortField: "InlSta", sortable: true, filter: "values"},
  {name: "InlDat", id: "InlDat", label: "Created", sortField: "InlDatSrt", sortable: true, filter: "date", filterFields: "InlDat"}
];

describe("awe-framework/awe-client-angular/src/test/jest/components/gridSavedSearches.test.js", () => {
  let refs, grid, actionController, connection, $q, searches, models;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ActionController", "Connection", "$q", (_ActionController_, _Connection_, _$q_) => {
      actionController = _ActionController_;
      connection = _Connection_;
      $q = _$q_;
    }]);
    searches = [];
  });

  afterEach(() => {
    $(".modal").each((index, modal) => disposeModal(modal));
    $(document.body).removeClass("modal-open").empty();
    cleanupHeavyComponentTest();
  });

  const digest = () => refs.$rootScope.$digest();
  const button = () => grid.element.find(hook(TestIds.windowSavedSearches));
  const item = id => grid.element.find(`${hook(TestIds.windowSavedSearchItem)}[data-saved-search-id='${id}']`);
  const actionLists = () => actionController.addActionList.mock.calls.map(([actions]) => actions);
  const serviceCall = target => actionLists().find(actions => actions[0].attr && actions[0].attr("parameters")?.targetAction === target);

  /**
   * Mount a grid with a quick filter and a search, and the models of both
   * @param {object} controller Grid controller attributes
   */
  const mount = (controller = {savedSearches: true}) => {
    grid = mountGrid(refs, {columnModel: COLUMNS.map(column => ({...column})), label: "Customers", ...controller});
    models = {
      QckSta: {selected: "LCK", values: [{value: "", label: "All"}, {value: "LCK", label: "Locked"}]},
      SchUsr: {selected: "jo", values: []}
    };
    // The quick filter and the search are criteria of the screen: only their models are read and changed
    grid.controller.quickFilter = "QckSta";
    grid.controller.search = "SchUsr";
    refs.$control.getAddressModel.mockImplementation(address => models[address.component] || grid.model);
    jest.spyOn(refs.$control, "changeModelAttribute").mockImplementation((address, attributes) => {
      Object.assign(models[address.component], attributes);
    });
    jest.spyOn(grid.component, "reload").mockImplementation(() => undefined);
    jest.spyOn(actionController, "addActionList").mockImplementation(() => undefined);
    refs.$storage.get.mockReturnValue({base: {name: "UsrLst"}});
    jest.spyOn(connection, "post").mockImplementation(() =>
      $q.resolve({data: [{type: "fill", parameters: {datalist: {rows: searches}}}, {type: "end-load"}]}));
  };

  const open = () => {
    button().trigger("click");
    digest();
  };

  it("is in the toolbar, before Columns, only for a grid with savedSearches", () => {
    mount({});
    expect(button().length).toBe(0);
    grid.element.remove();

    mount();

    expect(button().attr("data-testid-owner")).toBe("Grid");
    expect(button().closest(".window-saved-searches").next().is(hook(TestIds.gridColumnsButton))).toBe(true);
  });

  it("saves the column filters with their labels, the sort, the quick filter and the search of the grid", () => {
    mount();
    grid.component.columnFilters = [{id: "InlSta", fields: ["InlSta"], type: "values", values: ["1"]}];
    grid.component.columnFilterLabels = {InlSta: {1: "Active"}};
    grid.component.grid.sorting = [{id: "InlDatSrt", direction: "desc"}];
    open();

    grid.element.find(hook(TestIds.windowSavedSearchSave)).trigger("click");
    digest();
    $(hook(TestIds.windowSavedSearchName)).val("Active").trigger("input").trigger("change");
    $(hook(TestIds.windowSavedSearchAccept)).trigger("click");
    digest();

    const parameters = serviceCall("saveWindowSearch")[0].attr("parameters");
    expect(parameters).toMatchObject({savedSearchScreen: "UsrLst", savedSearchWindow: "Grid", savedSearchName: "Active"});
    expect(JSON.parse(parameters.savedSearchValues)).toEqual({
      columnFilters: [{id: "InlSta", fields: ["InlSta"], type: "values", values: ["1"]}],
      columnFilterLabels: {InlSta: {1: "Active"}},
      sort: [{id: "InlDatSrt", direction: "desc"}],
      quickFilter: "LCK",
      search: "jo"
    });
  });

  it("restores the state of a search on the columns the grid has and reloads the grid once from its first page", () => {
    searches = [{id: 4, name: "Ana", own: 1, profile: "", values: JSON.stringify({
      columnFilters: [{id: "InlNam", fields: ["InlNam"], type: "text", conditions: [{operator: "contains", value: "an"}]},
        {id: "Gone", fields: ["Gone"], type: "values", values: [1]}],
      columnFilterLabels: {Gone: {1: "One"}},
      sort: [{id: "InlDat", direction: "desc"}, {id: "Gone", direction: "asc"}],
      quickFilter: "",
      search: "ana"
    })}];
    mount();
    grid.component.currentPage = 3;
    open();

    item(4).trigger("click");
    digest();

    expect(grid.component.reload).toHaveBeenCalledTimes(1);
    expect(grid.component.getSpecificFields()).toEqual({max: 10, page: 1, sort: [{id: "InlDatSrt", direction: "desc"}],
      columnFilters: [{id: "InlNam", fields: ["InlNam"], type: "text", conditions: [{operator: "contains", value: "an"}]}]});
    expect(grid.component.columnFilterLabels).toEqual({});
    expect(grid.component.grid.api.grid.getColumn("InlDat").sort.direction).toBe("desc");
    expect(models.QckSta.selected).toBe("");
    expect(models.SchUsr.selected).toBe("ana");
    expect(button().text()).toContain("Ana");
  });
});
