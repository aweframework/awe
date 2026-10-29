import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates, mountGrid, readTemplate} from "./gridTestUtils";

// Row grouping of the AngularJS grids (#833): an awe-grid with group-by shows its rows inside collapsible groups with
// their counts, aggregates and subtotals, without pagination, sorting them on the client; the model keeps the data rows
const hook = testId => `[data-testid='${testId}']`;
const COLUMNS = [
  {name: "country", label: "Country", sendable: true},
  {name: "city", label: "City", sendable: true},
  {name: "name", label: "Name", sendable: true},
  {name: "amount", label: "Amount", sendable: true, align: "right", summaryType: "sum", numberFormat: "{mDec: 2, aPad: true}"},
  {name: "lines", label: "Lines", sendable: true, align: "right", summaryType: "sum"}
];
const ROWS = [
  {id: "r1", country: "ES", city: "Madrid", name: "Ann", amount: 100, lines: 1},
  {id: "r2", country: "DE", city: "Berlin", name: "Bob", amount: 50.5, lines: 4},
  {id: "r3", country: "ES", city: "Bilbao", name: "Carl", amount: 20, lines: 2},
  {id: "r4", country: "ES", city: "Madrid", name: "Dora", amount: 1030, lines: 3}
];

describe("grid row grouping (awe-grid rendered by ui-grid, #833)", () => {
  let refs;
  let mounted;

  const mount = (controller = {}, values = ROWS) => {
    mounted = mountGrid(refs, {
      loadAll: true, enableFilters: true, columnModel: COLUMNS.map(column => ({...column})), groupBy: "country", ...controller
    }, values.map(row => ({...row})));
    return mounted;
  };

  const digest = () => refs.$rootScope.$digest();
  const ids = rows => rows.map(row => row.id);
  const body = element => element.find(".ui-grid-render-container-body");
  const rowsOf = (element, testId) => body(element).find(hook(testId));
  const cell = (row, column) => $(row).find(`[column-id='${column}']`);
  const groupRow = (element, path) => body(element).find(`${hook("grid-group-row")}[data-group-path='${path}']`);

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    mounted?.element.remove();
    mounted = null;
    cleanupHeavyComponentTest();
  });

  describe("grouped grid", () => {
    it("shows the display rows, keeps the data rows in the model and shows every row without pagination", () => {
      const {scope, model} = mount();

      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "r2", "$subtotal-DE", "$group-ES", "r1", "r3", "r4", "$subtotal-ES"]);
      expect(ids(model.values)).toEqual(["r1", "r2", "r3", "r4"]);
      expect(model.values.some(row => "$group" in row || "$subtotal" in row)).toBe(false);
      expect(scope.gridOptions.enablePagination).toBe(false);
      expect(scope.gridOptions.useExternalSorting).toBe(true);
      expect(scope.gridOptions.enableFiltering).toBe(false);
      expect(scope.component.grouped).toBe(true);
    });

    it("hides the pager while the rows are grouped", () => {
      const {element} = mount();

      expect(element.find(hook("grid-pagination")).hasClass("ng-hide")).toBe(true);
    });

    it("renders the group rows with their toggle, label and count in the first column and their aggregates", () => {
      const {element} = mount();

      const rows = rowsOf(element, "grid-group-row");
      expect(rows.length).toBe(2);
      const es = groupRow(element, "ES");
      expect(es.attr("data-group-level")).toBe("1");
      expect(es.attr("data-expanded")).toBe("true");
      expect(es.closest(".ui-grid-row").hasClass("grid-group-row")).toBe(true);
      expect(es.closest(".ui-grid-row").hasClass("grid-group-level-1")).toBe(true);

      const toggle = cell(es, "country").find(hook("grid-group-toggle"));
      expect(toggle.prop("tagName")).toBe("BUTTON");
      expect(toggle.attr("type")).toBe("button");
      expect(toggle.attr("aria-expanded")).toBe("true");
      expect(toggle.attr("aria-label")).toBe("GRID_GROUP_COLLAPSE");
      expect(toggle.attr("title")).toBe("GRID_GROUP_COLLAPSE");
      expect(toggle.find("svg.awe-icon > use").attr("href")).toMatch(/#chevron-right$/);
      expect(cell(es, "country").find(".grid-group-label").text()).toBe("ES");
      expect(cell(es, "country").find(".grid-group-count").text()).toBe("3");
      expect(cell(es, "amount").text().trim()).toBe("1,150.00");
      expect(cell(es, "lines").text().trim()).toBe("6");
      expect(cell(es, "city").text().trim()).toBe("");
      expect(cell(es, "name").text().trim()).toBe("");
    });

    it("closes each group with its subtotal row", () => {
      const {element} = mount();

      const subtotals = rowsOf(element, "grid-subtotal-row");
      expect(subtotals.length).toBe(2);
      const es = subtotals.filter("[data-group-path='ES']");
      expect(es.attr("data-group-level")).toBe("1");
      expect(es.closest(".ui-grid-row").hasClass("grid-subtotal-row")).toBe(true);
      expect(es.closest(".ui-grid-row").hasClass("grid-subtotal-level-1")).toBe(true);
      expect(cell(es, "country").find(".grid-subtotal-label").text()).toBe("GRID_GROUP_SUBTOTAL ES");
      expect(cell(es, "amount").text().trim()).toBe("1,150.00");
      expect(cell(es, "country").find(hook("grid-group-toggle")).length).toBe(0);
    });

    it("keeps the data rows as grid rows", () => {
      const {element} = mount();

      const rows = rowsOf(element, "grid-row");
      expect(rows.map((index, row) => row.getAttribute("row-id")).get()).toEqual(["r2", "r1", "r3", "r4"]);
      expect(rows.first().attr("data-group-path")).toBeUndefined();
      expect(cell(rows.first(), "name").text()).toBe("Bob");
    });

    it("leaves out the subtotal rows with group-subtotals false", () => {
      const {scope} = mount({groupSubtotals: false});

      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "r2", "$group-ES", "r1", "r3", "r4"]);
    });

    it("nests the levels of a comma-separated group-by and ignores the unknown columns", () => {
      const {scope, element} = mount({groupBy: "country, nope, city"});

      expect(ids(scope.gridOptions.data)).toEqual([
        "$group-DE", "$group-DE/Berlin", "r2", "$subtotal-DE/Berlin", "$subtotal-DE",
        "$group-ES", "$group-ES/Bilbao", "r3", "$subtotal-ES/Bilbao", "$group-ES/Madrid", "r1", "r4", "$subtotal-ES/Madrid", "$subtotal-ES"
      ]);
      expect(groupRow(element, "ES/Madrid").attr("data-group-level")).toBe("2");
      expect(groupRow(element, "ES/Madrid").closest(".ui-grid-row").hasClass("grid-group-level-2")).toBe(true);
    });

    it("shows the label of the group in the first visible column", () => {
      const {element} = mount({columnModel: [{name: "key", label: "Key", hidden: true}, ...COLUMNS.map(column => ({...column}))]});

      expect(cell(groupRow(element, "ES"), "country").find(hook("grid-group-toggle")).length).toBe(1);
      expect(cell(groupRow(element, "ES"), "key").find(hook("grid-group-toggle")).length).toBe(0);
    });

    it("shows the translated (empty) label for the rows without value", () => {
      const {element} = mount({}, [...ROWS, {id: "r5", country: "", name: "Eve", amount: 1, lines: 1}]);

      expect(cell(groupRow(element, ""), "country").find(".grid-group-label").text()).toBe("GRID_GROUP_EMPTY");
    });

    it("shows the labels of the select columns, translated", () => {
      const columnModel = COLUMNS.map(column => (column.name === "country" ?
        {...column, component: "select", model: {values: [{value: "ES", label: "SPAIN"}, {value: "DE", label: "GERMANY"}]}} : {...column}));
      const {element} = mount({columnModel});

      expect(cell(groupRow(element, "ES"), "country").find(".grid-group-label").text()).toBe("SPAIN");
    });

    it("gives the group and subtotal rows no checkbox and no row number, and numbers the data rows", () => {
      const {element} = mount({multiselect: true, rowNumbers: true});

      const es = groupRow(element, "ES");
      expect(cell(es, "chkBox").find(hook("grid-row-checkbox")).length).toBe(0);
      expect(cell(es, "rowNum").text().trim()).toBe("");
      expect(cell(es, "country").find(hook("grid-group-toggle")).length).toBe(1);
      const rows = rowsOf(element, "grid-row");
      expect(rows.map((index, row) => cell(row, "rowNum").text().trim()).get()).toEqual(["1", "2", "3", "4"]);
      expect(rows.map((index, row) => cell(row, "chkBox").find(hook("grid-row-checkbox")).length).get()).toEqual([1, 1, 1, 1]);
    });
  });

  describe("expand and collapse", () => {
    it("collapses and expands a group with its toggle, without a server call", () => {
      const {scope, element, model} = mount();
      const reload = jest.spyOn(scope.component, "reload");

      groupRow(element, "ES").find(hook("grid-group-toggle"))[0].click();
      digest();

      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "r2", "$subtotal-DE", "$group-ES"]);
      expect(groupRow(element, "ES").attr("data-expanded")).toBe("false");
      expect(groupRow(element, "ES").find(hook("grid-group-toggle")).attr("aria-expanded")).toBe("false");
      expect(groupRow(element, "ES").find(hook("grid-group-toggle")).attr("aria-label")).toBe("GRID_GROUP_EXPAND");
      expect(ids(model.values)).toEqual(["r1", "r2", "r3", "r4"]);

      groupRow(element, "ES").find(hook("grid-group-toggle"))[0].click();
      digest();

      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "r2", "$subtotal-DE", "$group-ES", "r1", "r3", "r4", "$subtotal-ES"]);
      expect(reload).not.toHaveBeenCalled();
    });

    it("toggles a group with a click on its row and never selects it", () => {
      const {scope, element, model} = mount({multiselect: true});

      cell(groupRow(element, "DE"), "amount")[0].click();
      digest();

      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "$group-ES", "r1", "r3", "r4", "$subtotal-ES"]);
      expect(scope.component.getSelection()).toEqual([]);
      expect(model.selected).toEqual([]);
      const display = scope.component.grid.api.grid.rows.filter(row => row.entity.$group || row.entity.$subtotal);
      expect(display.length).toBeGreaterThan(0);
      expect(display.every(row => scope.gridOptions.isRowSelectable(row) === false)).toBe(true);
    });

    it("does not select a group row on the context menu", () => {
      const {scope, element} = mount();
      const select = jest.spyOn(scope.component, "selectRow");
      const show = jest.spyOn(scope.component, "showContextMenu").mockImplementation(() => undefined);
      const row = scope.component.grid.api.grid.rows.find(item => item.entity.id === "$group-ES");

      scope.component.rowContextMenu({preventDefault: jest.fn()}, row);

      expect(select).not.toHaveBeenCalled();
      expect(show).not.toHaveBeenCalled();
      expect(element.find(hook("grid-group-row")).length).toBeGreaterThan(0);
    });

    it("keeps the selected rows of a collapsed group selected", () => {
      const {scope, model} = mount({multiselect: true});
      scope.component.setSelection(["r1"]);
      digest();
      expect(model.selected).toEqual(["r1"]);

      scope.component.toggleGroup("ES");
      digest();
      scope.component.toggleGroup("ES");
      digest();

      expect(model.selected).toEqual(["r1"]);
      expect(scope.component.grid.api.grid.rows.find(row => row.entity.id === "r1").isSelected).toBe(true);
    });

    it("keeps the expanded state by group path when the rows are reloaded and sorted", () => {
      const {scope} = mount();
      scope.component.toggleGroup("DE");
      digest();

      scope.component.api.updateModelValues({values: [...ROWS.map(row => ({...row})), {id: "r9", country: "DE", name: "Zoe", amount: 1, lines: 1}]});
      digest();
      expect(ids(scope.gridOptions.data).slice(0, 2)).toEqual(["$group-DE", "$group-ES"]);

      scope.component.grid.api.core.raise.sortChanged(scope.component.grid.api.grid, [{colDef: {sortField: "name"}, sort: {direction: "desc"}}]);
      digest();
      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "$group-ES", "r4", "r3", "r1", "$subtotal-ES"]);
    });
  });

  describe("sorting", () => {
    const sort = (scope, sortField, direction) => {
      scope.component.grid.api.core.raise.sortChanged(scope.component.grid.api.grid, [{colDef: {sortField}, sort: {direction}}]);
      digest();
    };

    it("sorts the rows inside the groups on the client", () => {
      const {scope} = mount();
      const reload = jest.spyOn(scope.component, "reload");

      sort(scope, "name", "desc");

      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "r2", "$subtotal-DE", "$group-ES", "r4", "r3", "r1", "$subtotal-ES"]);
      expect(reload).not.toHaveBeenCalled();
    });

    it("orders the groups by their group-by column and by an aggregate", () => {
      const {scope} = mount();

      sort(scope, "country", "desc");
      expect(ids(scope.gridOptions.data).filter(id => id.startsWith("$group"))).toEqual(["$group-ES", "$group-DE"]);

      sort(scope, "amount", "asc");
      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "r2", "$subtotal-DE", "$group-ES", "r3", "r1", "r4", "$subtotal-ES"]);
    });
  });

  describe("grids that are not grouped", () => {
    it("keeps a grid without group-by unchanged", () => {
      const {scope, element, model} = mount({groupBy: undefined, loadAll: false});

      expect(scope.gridOptions.data).toBe(model.values);
      expect(scope.gridOptions.rowTemplate).toBe("grid/row");
      expect(scope.gridOptions.isRowSelectable).toBe(angular.noop);
      expect(scope.gridOptions.enablePagination).toBe(true);
      expect(scope.gridOptions.useExternalSorting).toBe(true);
      expect(scope.gridOptions.useExternalPagination).toBe(true);
      expect(scope.gridOptions.enableFiltering).toBe(true);
      expect(scope.component.grouped).toBe(false);
      expect(scope.component.groupingConfigured).toBe(false);
      expect(element.find(hook("grid-group-row")).length).toBe(0);
      expect(element.find(hook("grid-pagination")).hasClass("ng-hide")).toBe(false);
      expect(rowsOf(element, "grid-row").length).toBe(4);
      expect(element.find(hook("grid-group-expand-all")).length).toBe(0);
      expect(element.find(hook("grid-group-by")).length).toBe(0);
    });

    it("keeps the client sorting and pagination of a load-all grid without group-by", () => {
      const {scope, model} = mount({groupBy: undefined});

      expect(scope.gridOptions.data).toBe(model.values);
      expect(scope.gridOptions.useExternalSorting).toBe(false);
      expect(scope.gridOptions.useExternalPagination).toBe(false);
      expect(scope.gridOptions.enablePagination).toBe(true);
    });

    it.each([
      ["editable", {editable: true}],
      ["send-operations", {multioperation: true}],
      ["move-rows", {moveRows: true}],
      ["row-detail", {rowDetail: {field: "name"}}]
    ])("ignores the group-by of a %s grid", (type, controller) => {
      // The operation column of a send-operations grid renders its icon with the column template
      inject(["$templateCache", "ServerData", ($templateCache, ServerData) => $templateCache.put(ServerData.getAngularTemplateUrl("column/icon"), "<span></span>")]);
      const {scope, element, model} = mount(controller);

      expect(scope.gridOptions.data).toBe(model.values);
      expect(scope.component.grouped).toBe(false);
      expect(scope.component.groupingConfigured).toBe(false);
      expect(element.find(hook("grid-group-row")).length).toBe(0);
      expect(element.find(hook("grid-group-expand-all")).length).toBe(0);
    });

    it("ignores the group-by of a tree grid", () => {
      const controller = {
        id: "Tree", visible: true, loading: false, targetAction: "getRows", loadAll: true, multiselect: false, editable: false,
        enableFilters: false, showTotals: false, max: 10, pagerValues: [10, 20], treegrid: true, treeId: "id", treeParent: "parent",
        groupBy: "country", groupSelector: true, columnModel: COLUMNS.map(column => ({...column})), buttonModel: [], headerModel: []
      };
      const model = {page: 1, records: 2, total: 1, selected: [], values: ROWS.slice(0, 2).map(row => ({...row}))};
      inject(["$templateCache", "ServerData", ($templateCache, ServerData) => {
        cacheGridTemplates($templateCache);
        $templateCache.put(ServerData.getAngularTemplateUrl("treegrid"), readTemplate("treegrid.html"));
      }]);
      jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
      jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
      jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
      jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
      refs.$utilities.timeout.cancel = jest.fn();
      jest.spyOn(console, "error").mockImplementation(() => undefined);
      const element = refs.$compile("<awe-tree-grid tree-grid-id='Tree'></awe-tree-grid>")(refs.$rootScope.$new());
      $(document.body).append(element);
      digest();
      const scope = element.isolateScope() || element.scope();

      expect(scope.gridOptions.data.some(row => row.$group || row.$subtotal)).toBe(false);
      expect(element.find(hook("grid-group-row")).length).toBe(0);
      expect(element.find(hook("grid-group-by")).length).toBe(0);
      element.remove();
    });
  });
});
