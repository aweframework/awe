import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";

// Column autofilter of the AngularJS grids (#912, S18): the funnel of the columns that declare a filter and its menu
// (sort, clear, the filters of the type with the custom filter, and the values of the column with their search)

const hook = id => `[data-testid='${id}']`;

const STATUS_VALUES = [
  {value: "1", label: "Active", count: 16},
  {value: "0", label: "Inactive", count: 4},
  {value: null, label: null, count: 2}
];

const COLUMNS = [
  {name: "InlNam", id: "InlNam", label: "Name", sortField: "InlNam", sortable: true, filter: "text"},
  {name: "InlSta", id: "InlSta", label: "Status", sortField: "InlSta", sortable: true, filter: "values"},
  {name: "InlCtr", id: "InlCtr", label: "Country", sortable: true},
  {name: "InlAmt", id: "InlAmt", label: "Amount", sortable: true, filter: "number"},
  {name: "InlDat", id: "InlDat", label: "Created", sortField: "InlDatSrt", sortable: true, filter: "date", filterFields: "InlDat"}
];

const ROWS = [{id: 1, InlNam: "Atlas", InlSta: "Active", InlCtr: "Spain", InlAmt: 10, InlDat: "01/10/2026"}];

describe("grid column autofilter", () => {
  let refs;
  let grid;
  let requests;
  let answer;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    requests = [];
    answer = null;
    // The values of a column come from the column-values action: the requests are kept and answered with the values
    inject(["Ajax", "$q", (Ajax, $q) => {
      jest.spyOn(Ajax, "httpRequest").mockImplementation(request => {
        requests.push(request);
        return answer ? $q.resolve({data: answer}) : $q.defer().promise;
      });
    }]);
  });

  afterEach(() => {
    $(document.body).empty();
    cleanupHeavyComponentTest();
  });

  const digest = () => refs.$rootScope.$digest();

  /**
   * Answer the column-values requests with some values, and keep the requests
   * @param {object[]} values Values of the answer
   * @param {boolean} [more] The column has more values
   */
  const answerValues = (values = STATUS_VALUES, more = false) => {
    answer = [{type: "column-values", parameters: {datalist: {rows: values}, more}}, {type: "end-load"}];
  };

  const mount = (controller = {}) => {
    grid = mountGrid(refs, {columnModel: COLUMNS.map(column => ({...column})), ...controller}, ROWS.map(row => ({...row})));
    jest.spyOn(grid.component, "reload").mockImplementation(() => undefined);
    return grid;
  };

  const funnel = column => grid.element.find(`${hook("grid-column-filter")}[column-id='${column}']`);
  const menu = () => $(document.body).find(hook("grid-column-filter-menu"));
  const option = key => menu().find(`${hook("grid-column-filter-option")}[option-id='${key}']`);
  const value = key => menu().find(`${hook("grid-column-filter-value")}[option-id='${key}']`);
  const accept = () => menu().find(hook("grid-column-filter-accept"));
  const keyDown = (element, key) => {
    const event = new window.KeyboardEvent("keydown", {key, bubbles: true, cancelable: true});
    element[0].dispatchEvent(event);
    digest();
    return event;
  };

  /**
   * Open the menu of a column and answer its values
   * @param {string} column Column name
   */
  const open = column => {
    funnel(column).trigger("click");
    digest();
  };

  describe("funnel", () => {
    it("is shown only in the header of the columns that declare a filter", () => {
      mount();

      const columns = grid.element.find(hook("grid-column-filter")).map((index, node) => node.getAttribute("column-id")).get();
      expect(columns).toEqual(["InlNam", "InlSta", "InlAmt", "InlDat"]);
      expect(funnel("InlSta").attr("data-testid-owner")).toBe("Grid");
      expect(funnel("InlSta").attr("aria-haspopup")).toBe("dialog");
      expect(funnel("InlSta").attr("aria-expanded")).toBe("false");
      expect(funnel("InlSta").attr("aria-label")).toBe("GRID_FILTER_BUTTON");
      expect(funnel("InlSta").attr("data-active")).toBe("false");
    });

    it("replaces the native filter of enable-filters on its columns, which the other columns keep", () => {
      mount({enableFilters: true});

      const filterOf = column => grid.element.find(`[column-id='${column}'] .ui-grid-filter-input`);
      expect(filterOf("InlCtr").length).toBe(1);
      expect(filterOf("InlSta").length).toBe(0);
      expect(filterOf("InlNam").length).toBe(0);
    });

    it("does not sort the column when it is clicked", () => {
      answerValues();
      mount();

      open("InlSta");

      expect(grid.scope.gridOptions.columnDefs.find(column => column.name === "InlSta").sort).toBeUndefined();
      expect(grid.component.grid.sorting).toEqual([]);
      expect(funnel("InlSta").attr("aria-expanded")).toBe("true");
    });

    it("leaves a grid without filter columns as it was: no funnel and no column filters in its request", () => {
      mount({columnModel: [{name: "InlCtr", id: "InlCtr", label: "Country"}]});

      expect(grid.element.find(hook("grid-column-filter")).length).toBe(0);
      expect(grid.component.getSpecificFields()).toEqual({max: 10, page: 1, sort: []});
    });
  });

  describe("values", () => {
    it("asks the values of the column with the parameters of the grid, the column, its fields and its type", () => {
      answerValues();
      mount();
      grid.component.columnFilters = [{id: "InlNam", fields: ["InlNam"], type: "text", conditions: [{operator: "contains", value: "a"}]}];

      open("InlDat");

      expect(requests).toHaveLength(1);
      expect(requests[0].method).toBe("POST");
      expect(requests[0].url).toMatch(/\/action\/column-values\/getRows$/);
      expect(requests[0].data).toMatchObject({
        serverAction: "column-values", targetAction: "getRows", max: 10, page: 1, sort: [],
        columnFilters: [{id: "InlNam", fields: ["InlNam"], type: "text", conditions: [{operator: "contains", value: "a"}]}],
        columnValues: {id: "InlDat", fields: ["InlDat"], type: "date"}
      });
      expect(requests[0].data.columnValues).not.toHaveProperty("search");
    });

    it("lists Select all, a checkbox per value with its label and count, and Blanks, all checked without filter", () => {
      answerValues();
      mount();

      open("InlSta");

      const rows = menu().find(".grid-column-filter-value");
      expect(rows.map((index, node) => node.textContent.replace(/\s+/g, " ").trim()).get())
        .toEqual(["GRID_FILTER_SELECT_ALL", "Active 16", "Inactive 4", "GRID_FILTER_BLANKS 2"]);
      expect(rows.map((index, node) => node.getAttribute("aria-checked")).get()).toEqual(["true", "true", "true", "true"]);
      expect(menu().find(hook("grid-column-filter-select-all")).length).toBe(1);
      expect(menu().find(`${hook("grid-column-filter-value")}[data-type='blanks']`).attr("option-id")).toBe("");
      expect(value("1").attr("data-selected")).toBe("true");
    });

    it("shows Select all as mixed while some values are unchecked, and checks or unchecks them all", () => {
      answerValues();
      mount();
      open("InlSta");
      const selectAll = () => menu().find(hook("grid-column-filter-select-all"));

      value("1").trigger("click");
      digest();
      expect(selectAll().attr("aria-checked")).toBe("mixed");
      expect(value("1").attr("data-selected")).toBe("false");

      selectAll().trigger("click");
      digest();
      expect(menu().find(".grid-column-filter-value").map((index, node) => node.getAttribute("aria-checked")).get())
        .toEqual(["true", "true", "true", "true"]);

      selectAll().trigger("click");
      digest();
      expect(selectAll().attr("aria-checked")).toBe("false");
      expect(accept().prop("disabled")).toBe(true);
    });

    it("applies the checked values and the blanks, and reloads the grid from its first page", () => {
      answerValues();
      mount();
      grid.component.currentPage = 3;
      open("InlSta");

      value("1").trigger("click");
      digest();
      accept().trigger("click");
      digest();

      expect(menu().length).toBe(0);
      expect(grid.component.columnFilters).toEqual([{id: "InlSta", fields: ["InlSta"], type: "values", values: ["0"], empty: true}]);
      expect(grid.component.currentPage).toBe(1);
      expect(grid.component.reload).toHaveBeenCalledTimes(1);
      expect(grid.component.getSpecificFields()).toEqual({max: 10, page: 1, sort: [],
        columnFilters: [{id: "InlSta", fields: ["InlSta"], type: "values", values: ["0"], empty: true}]});
      expect(funnel("InlSta").attr("data-active")).toBe("true");
    });

    it("reloads a grid that loads all its rows from the server too", () => {
      answerValues();
      mount({loadAll: true});
      open("InlSta");

      value("0").trigger("click");
      digest();
      accept().trigger("click");
      digest();

      expect(grid.component.reload).toHaveBeenCalledTimes(1);
      expect(grid.component.getSpecificFields()).toMatchObject({max: 0, page: 1,
        columnFilters: [{id: "InlSta", values: ["1"], empty: true}]});
    });

    it("checks the values of the filter of the column when it opens again", () => {
      answerValues();
      mount();
      grid.component.columnFilters = [{id: "InlSta", fields: ["InlSta"], type: "values", values: ["0"]}];

      open("InlSta");

      expect(value("0").attr("data-selected")).toBe("true");
      expect(value("1").attr("data-selected")).toBe("false");
      expect(menu().find(`${hook("grid-column-filter-value")}[data-type='blanks']`).attr("data-selected")).toBe("false");
      expect(option("clear").prop("disabled")).toBe(false);
    });

    it("closes without changes when it is accepted untouched", () => {
      answerValues();
      mount();
      open("InlSta");

      accept().trigger("click");
      digest();

      expect(menu().length).toBe(0);
      expect(grid.component.reload).not.toHaveBeenCalled();
      expect(grid.component.columnFilters).toEqual([]);
    });

    it("searches after a pause, cancels the previous request, and accepts the checked values among the shown ones", () => {
      answerValues();
      mount();
      open("InlSta");
      const search = menu().find(hook("grid-column-filter-search"));
      const type = text => {
        search.val(text).trigger("input");
        digest();
      };
      const wait = time => inject(["$timeout", $timeout => $timeout.flush(time)]);

      // A search that is not answered yet
      answer = null;
      type("Ac");
      wait(299);
      expect(requests).toHaveLength(1);
      wait(1);
      expect(requests).toHaveLength(2);

      // A newer search cancels it
      answerValues();
      type("Act");
      wait(300);
      expect(requests).toHaveLength(3);
      expect(requests[1].timeout.$$state.status).toBe(1);
      expect(requests[2].data.columnValues).toEqual({id: "InlSta", fields: ["InlSta"], type: "values", search: "Act"});

      accept().trigger("click");
      digest();
      expect(grid.component.columnFilters).toEqual([{id: "InlSta", fields: ["InlSta"], type: "values", values: ["1", "0"], empty: true}]);
    });

    it("notes that the column has more values than the listed ones", () => {
      answerValues(STATUS_VALUES, true);
      mount();

      open("InlSta");

      expect(menu().find(".grid-column-filter-note").text()).toContain("GRID_FILTER_MORE_VALUES");
    });

    it("shows the dates as a year, month and day tree that checks the days under a node", () => {
      answerValues([{value: "01/10/2026", count: 2}, {value: "02/10/2026", count: 1}, {value: "05/11/2026", count: 3}]);
      mount();
      open("InlDat");

      const list = menu().find(".grid-column-filter-list");
      expect(list.attr("role")).toBe("tree");
      const labels = () => menu().find(".grid-column-filter-value-label").map((index, node) => node.textContent).get();
      expect(labels()).toEqual(["GRID_FILTER_SELECT_ALL", "2026", expect.any(String), expect.any(String)]);

      // The first month opens with the keyboard and checking it checks its days
      const rows = () => menu().find(".grid-column-filter-value");
      keyDown(list, "ArrowDown");
      keyDown(list, "ArrowDown");
      keyDown(list, "ArrowRight");
      expect(labels()).toHaveLength(6);
      expect(rows().eq(3).attr("aria-level")).toBe("3");
      keyDown(list, " ");
      expect(rows().eq(2).attr("aria-checked")).toBe("false");
      expect(rows().eq(1).attr("aria-checked")).toBe("mixed");

      accept().trigger("click");
      digest();
      expect(grid.component.columnFilters).toEqual([{id: "InlDat", fields: ["InlDat"], type: "date", values: ["05/11/2026"]}]);
    });
  });

  describe("menu", () => {
    it("sorts the column alone from the menu, replacing the sort of the grid", () => {
      answerValues();
      mount();
      const sortColumn = jest.spyOn(grid.component.grid.api.grid, "sortColumn");
      open("InlDat");

      option("sort-desc").trigger("click");
      digest();

      expect(menu().length).toBe(0);
      expect(sortColumn).toHaveBeenCalledWith(expect.objectContaining({name: "InlDat"}), "desc", false);
    });

    it("has no sort in a column that does not sort, and clear is disabled without a filter", () => {
      answerValues();
      mount({columnModel: [{...COLUMNS[1], sortable: false}]});

      open("InlSta");

      expect(option("sort-asc").length).toBe(0);
      expect(option("clear").prop("disabled")).toBe(true);
      expect(option("conditions").length).toBe(0);
    });

    it("clears the filter of the column", () => {
      answerValues();
      mount();
      grid.component.columnFilters = [{id: "InlSta", fields: ["InlSta"], type: "values", values: ["0"]}];
      open("InlSta");

      option("clear").trigger("click");
      digest();

      expect(grid.component.columnFilters).toEqual([]);
      expect(grid.component.reload).toHaveBeenCalledTimes(1);
    });

    it("applies a custom filter of two conditions joined by Or, in the same dialog", () => {
      answerValues([]);
      mount();
      open("InlNam");

      option("conditions").trigger("click");
      digest();
      option("custom").trigger("click");
      digest();

      expect(menu().find(".grid-column-filter-custom").length).toBe(1);
      const inputs = () => menu().find(".grid-column-filter-input");
      inputs().eq(0).val("mart").trigger("input");
      menu().find(".grid-column-filter-operator").eq(1).find("a[role='option']").eq(5).trigger("click");
      digest();
      inputs().eq(1).val("lop").trigger("input");
      menu().find(".grid-column-filter-union-option").eq(1).trigger("click");
      digest();
      accept().trigger("click");
      digest();

      expect(grid.component.columnFilters).toEqual([{id: "InlNam", fields: ["InlNam"], type: "text", union: "or",
        conditions: [{operator: "eq", value: "mart"}, {operator: "endsWith", value: "lop"}]}]);
    });

    it("reads a number of the custom filter with the decimal separator of the application", () => {
      answerValues([]);
      mount();
      inject(["AweSettings", $settings => jest.spyOn($settings, "get").mockImplementation(key =>
        (key === "numericOptions" ? {aDec: ",", aSep: "."} : jest.requireActual("../../../main/resources/js/awe/data/options").DefaultSettings[key]))]);
      open("InlAmt");

      option("conditions").trigger("click");
      digest();
      option("gt").trigger("click");
      digest();
      menu().find(".grid-column-filter-input").eq(0).val("1,5").trigger("input");
      digest();
      accept().trigger("click");
      digest();

      expect(grid.component.columnFilters).toEqual([{id: "InlAmt", fields: ["InlAmt"], type: "number",
        conditions: [{operator: "gt", value: "1.5"}]}]);
    });
  });

  describe("keyboard", () => {
    it("closes with Escape without changes and gives the focus back to the funnel", () => {
      answerValues();
      mount();
      funnel("InlSta")[0].focus();
      open("InlSta");
      expect(menu()[0].contains(document.activeElement)).toBe(true);

      keyDown(menu().find(hook("grid-column-filter-search")), "Escape");

      expect(menu().length).toBe(0);
      expect(document.activeElement).toBe(funnel("InlSta")[0]);
      expect(grid.component.reload).not.toHaveBeenCalled();
    });

    it("moves through the values with the arrows and checks them with Space", () => {
      answerValues();
      mount();
      open("InlSta");
      const list = menu().find(".grid-column-filter-list");

      keyDown(list, "ArrowDown");
      expect(document.activeElement).toBe(value("1")[0]);
      expect(value("1").attr("tabindex")).toBe("0");
      keyDown(list, " ");

      expect(value("1").attr("aria-checked")).toBe("false");
    });

    it("gives the focus back to the funnel when the values are accepted", () => {
      answerValues();
      mount();
      funnel("InlSta")[0].focus();
      open("InlSta");
      value("1").trigger("click");
      digest();
      accept().trigger("click");
      digest();

      expect(document.activeElement).toBe(funnel("InlSta")[0]);
    });

    it("closes an open operator list with Escape, keeps the menu open and keeps the key in the menu (a modal behind stays open)", () => {
      answerValues([]);
      mount();
      open("InlNam");
      option("conditions").trigger("click");
      digest();
      option("custom").trigger("click");
      digest();
      const toggle = menu().find("[data-toggle='dropdown']").eq(0);
      toggle.trigger("click");
      digest();
      const outside = jest.fn();
      document.body.addEventListener("keydown", outside);

      keyDown(toggle, "Escape");
      document.body.removeEventListener("keydown", outside);

      expect(toggle.attr("aria-expanded")).toBe("false");
      expect(menu().length).toBe(1);
      expect(outside).not.toHaveBeenCalled();
    });

    it("closes the type submenu with Escape and keeps the menu open", () => {
      answerValues([]);
      mount();
      open("InlNam");
      const trigger = option("conditions");

      keyDown(trigger, "ArrowRight");
      expect(trigger.attr("aria-expanded")).toBe("true");
      expect(document.activeElement).toBe(option("eq")[0]);
      keyDown(option("eq"), "Escape");

      expect(menu().length).toBe(1);
      expect(trigger.attr("aria-expanded")).toBe("false");
      expect(document.activeElement).toBe(trigger[0]);
    });
  });
});
