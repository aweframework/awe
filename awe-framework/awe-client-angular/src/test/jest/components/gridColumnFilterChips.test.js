import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates, mountGrid, readTemplate} from "./gridTestUtils";

// Column filters in effect of the AngularJS grids (#912, S18): the "Filtered by" chips under the toolbar, and the
// column autofilter of the tree grids

const hook = id => `[data-testid='${id}']`;

const COLUMNS = [
  {name: "InlNam", id: "InlNam", label: "Name", sortable: true, filter: "text"},
  {name: "InlSta", id: "InlSta", label: "Status", sortable: true, filter: "values"},
  {name: "InlDat", id: "InlDat", label: "Created", sortable: true, filter: "date", hidden: true}
];

const STATUS = {id: "InlSta", fields: ["InlSta"], type: "values", values: ["0", "2", "3", "4"], empty: true};
const CREATED = {id: "InlDat", fields: ["InlDat"], type: "date", conditions: [{operator: "between", value: "01/10/2026", to: "31/10/2026"}]};

describe("grid column filter chips", () => {
  let refs;
  let grid;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    $(document.body).empty();
    cleanupHeavyComponentTest();
  });

  const digest = () => refs.$rootScope.$digest();
  const chips = () => grid.element.find(hook("grid-column-filter-chips"));
  const chip = column => grid.element.find(`${hook("grid-column-filter-chip")}[column-id='${column}']`);
  const text = element => element.text().replace(/\s+/g, " ").trim();

  const mount = () => {
    grid = mountGrid(refs, {columnModel: COLUMNS.map(column => ({...column}))}, [{id: 1, InlNam: "Atlas"}]);
    jest.spyOn(grid.component, "reload").mockImplementation(() => undefined);
    return grid;
  };

  /**
   * Filter some columns, as the menu does
   * @param {object[]} filters Column filters
   * @param {object} labels Labels of their values by column
   */
  const filter = (filters, labels = {}) => {
    filters.forEach(item => grid.component.applyColumnFilter(item.id, item, labels[item.id]));
    digest();
    grid.component.reload.mockClear();
  };

  it("are not shown while no column is filtered", () => {
    mount();

    expect(chips().length).toBe(0);
  });

  it("show a chip per filtered column with its label and the summary of its filter, also for a hidden column", () => {
    mount();

    filter([STATUS, CREATED], {InlSta: {0: "Inactive", 2: "Blocked", 3: "Pending", 4: "Closed"}});

    expect(chips().attr("role")).toBe("group");
    expect(chips().attr("aria-label")).toBe("SCREEN_TEXT_FILTERED_BY");
    expect(chips().attr("data-testid-owner")).toBe("Grid");
    expect(text(chip("InlSta"))).toBe("Status: Inactive, Blocked, Pending +2");
    expect(text(chip("InlDat"))).toBe("Created: 01/10/2026 – 31/10/2026");
    expect(chip("InlSta").find(hook("grid-column-filter-chip-remove")).attr("aria-label"))
      .toBe("SCREEN_TEXT_REMOVE_FILTER Status: Inactive, Blocked, Pending +2");
  });

  it("remove the filter of a chip, reload the grid and give the focus to the funnel of its column", () => {
    mount();
    filter([STATUS, CREATED]);

    chip("InlSta").find(hook("grid-column-filter-chip-remove")).trigger("click");
    digest();

    expect(grid.component.columnFilters).toEqual([CREATED]);
    expect(grid.component.reload).toHaveBeenCalledTimes(1);
    expect(chip("InlSta").length).toBe(0);
    expect(document.activeElement).toBe(grid.element.find(`${hook("grid-column-filter")}[column-id='InlSta']`)[0]);
  });

  it("clear every column filter with Clear all and reload the grid", () => {
    mount();
    filter([STATUS, CREATED]);

    grid.element.find(hook("grid-column-filter-clear-all")).trigger("click");
    digest();

    expect(grid.component.columnFilters).toEqual([]);
    expect(grid.component.getSpecificFields()).not.toHaveProperty("columnFilters");
    expect(grid.component.reload).toHaveBeenCalledTimes(1);
    expect(chips().length).toBe(0);
  });

  describe("tree grid", () => {
    const mountTreeGrid = () => {
      const controller = {
        id: "Tree", visible: true, loading: false, targetAction: "getRows", loadAll: true, multiselect: false,
        editable: false, enableFilters: true, showTotals: false, max: 10, pagerValues: [10, 20],
        columnModel: COLUMNS.slice(0, 2).map(column => ({...column})).concat({name: "InlCtr", id: "InlCtr", label: "Country"}),
        buttonModel: [], headerModel: []
      };
      inject(["$templateCache", "ServerData", ($templateCache, ServerData) => {
        cacheGridTemplates($templateCache);
        $templateCache.put(ServerData.getAngularTemplateUrl("treegrid"), readTemplate("treegrid.html"));
      }]);
      jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
      jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "getAddressModel").mockReturnValue({page: 1, records: 0, total: 1, selected: [], values: []});
      jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
      jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
      refs.$utilities.timeout.cancel = jest.fn();
      jest.spyOn(console, "error").mockImplementation(() => undefined);
      const element = refs.$compile("<awe-tree-grid tree-grid-id='Tree'></awe-tree-grid>")(refs.$rootScope.$new());
      $(document.body).append(element);
      digest();
      const scope = element.isolateScope() || element.scope();
      grid = {element, scope, component: scope.component};
      jest.spyOn(grid.component, "reload").mockImplementation(() => undefined);
      return grid;
    };

    it("shows the funnel of its filter columns and their filters reload it with the column filters", () => {
      mountTreeGrid();

      expect(grid.element.find(hook("grid-column-filter")).map((index, node) => node.getAttribute("column-id")).get())
        .toEqual(["InlNam", "InlSta"]);
      expect(grid.element.find("[column-id='InlCtr'] .ui-grid-filter-input").length).toBe(1);

      filter([STATUS]);
      grid.component.applyColumnFilter("InlNam", {id: "InlNam", fields: ["InlNam"], type: "text", conditions: [{operator: "contains", value: "a"}]});
      digest();

      expect(grid.component.reload).toHaveBeenCalledTimes(1);
      expect(grid.component.getSpecificFields()).toMatchObject({max: 0, page: 1, columnFilters: [STATUS,
        {id: "InlNam", fields: ["InlNam"], type: "text", conditions: [{operator: "contains", value: "a"}]}]});
      expect(text(chip("InlSta"))).toBe("Status: 0, 2, 3 +2");
    });
  });
});
