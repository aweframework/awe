// Grid loading skeleton: the grid default (awe.application.component.grid-icon-loading) and the explicit icon-loading
import fs from "fs";
import path from "path";
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const TEMPLATES = path.join(__dirname, "../../../main/resources/templates/angular");
const GRID_LOADER = "[data-testid='grid-loader']";

describe("grid loading skeleton", () => {
  let refs;
  let settings;

  const baseController = {
    id: "Grid",
    visible: true,
    loading: true,
    targetAction: "getRows",
    loadAll: false,
    multiselect: false,
    editable: false,
    enableFilters: false,
    showTotals: false,
    disablePagination: false,
    max: 30,
    pagerValues: [10, 30],
    columnModel: [
      {name: "name", label: "Name", width: 120},
      {name: "hidden", label: "Hidden", width: 80, hidden: true},
      {name: "date", label: "Date", charlength: 10}
    ],
    buttonModel: [],
    headerModel: []
  };

  const compileGrid = (controllerOverrides = {}) => {
    const controller = {...baseController, columnModel: baseController.columnModel.map(column => ({...column})), ...controllerOverrides};
    const model = {page: 1, records: 0, total: 1, selected: [], values: controllerOverrides.rows || []};
    delete controller.rows;
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
    jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    const element = refs.$compile("<awe-grid grid-id='Grid'></awe-grid>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    const scope = element.isolateScope() || element.scope();
    // The component initialization resets the loading flag: set it as a running request does
    scope.controller.loading = controllerOverrides.loading === undefined ? true : controllerOverrides.loading;
    refs.$rootScope.$digest();
    return {element, scope, controller: scope.controller};
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
    refs.$httpBackend.whenGET(/template\/angular\/loader\//).respond("<div></div>");
    inject(["AweSettings", AweSettings => {
      settings = AweSettings;
    }]);
  });

  afterEach(cleanupHeavyComponentTest);

  it("uses the skeleton as the client default for the grids", () => {
    expect(DefaultSettings.gridIconLoading).toBe("skeleton");
  });

  it("shows placeholder rows instead of the spinner when the grid does not set icon-loading", () => {
    const {element, scope} = compileGrid();

    const loader = element.find(GRID_LOADER);
    expect(scope.iconLoader).toBe("skeleton");
    expect(loader.length).toBe(1);
    expect(loader[0].tagName).toBe("AWE-GRID-SKELETON");
    expect(element.find("awe-loader").length).toBe(0);
  });

  it("draws one row per record of the page, capped at 10, and one bar per visible column", () => {
    const {element} = compileGrid();

    const rows = element.find(".grid-skeleton-row");
    expect(rows.length).toBe(10);
    expect(rows.eq(0).find(".grid-skeleton-cell").length).toBe(2);
    expect(rows.eq(0).find(".skeleton-bar").length).toBe(2);
  });

  it("draws as many rows as the page size when it is smaller than the cap", () => {
    const {element} = compileGrid({max: 4});

    expect(element.find(".grid-skeleton-row").length).toBe(4);
  });

  it("uses the widths of the column model", () => {
    const {element, scope} = compileGrid();

    const cells = element.find(".grid-skeleton-row").eq(0).find(".grid-skeleton-cell");
    expect(cells[0].style.width).toBe("120px");
    expect(cells[1].style.width).toBe(`${10 * scope.charSize}px`);
  });

  describe("pre-columns", () => {
    const widths = element => [...element.find(".grid-skeleton-row").eq(0).find(".grid-skeleton-cell")].map(cell => cell.style.width);

    it.each([
      ["row numbers", {rowNumbers: true}, ["30px", "120px"]],
      ["selection", {multiselect: true}, ["26px", "120px"]],
      ["row numbers and selection, in the order of the header", {rowNumbers: true, multiselect: true}, ["30px", "26px", "120px"]]
    ])("starts the rows with a placeholder per shown pre-column: %s", (_case, overrides, expected) => {
      const {element} = compileGrid({...overrides, columnModel: [{name: "name", label: "Name", width: 120}]});

      const cells = element.find(".grid-skeleton-row").eq(0).find(".grid-skeleton-cell");
      expect(widths(element)).toEqual(expected);
      expect(cells.find(".skeleton-bar").length).toBe(expected.length);
    });

    it("draws the pre-columns once even when the column model already has them", () => {
      const {element} = compileGrid({rowNumbers: true, multiselect: true});

      expect(element.find(".grid-skeleton-row").eq(0).find(".grid-skeleton-cell").length).toBe(4);
    });

    it("uses the drawn width of a pre-column when the grid is rendered", () => {
      const {scope} = compileGrid({rowNumbers: true});
      scope.$destroy();
      const drawn = compileGrid({rowNumbers: true, loading: false});
      drawn.scope.component.grid.api.grid.columns.push({name: "rowNum", drawnWidth: 44});
      drawn.scope.controller.loading = true;
      refs.$rootScope.$digest();

      expect(widths(drawn.element)[0]).toBe("44px");
    });
  });

  it("marks the grid as busy while it loads and removes the skeleton when loading ends", () => {
    const {element, scope} = compileGrid();
    expect(element.attr("aria-busy")).toBe("true");

    scope.controller.loading = false;
    refs.$rootScope.$digest();

    expect(element.find(GRID_LOADER).length).toBe(0);
    expect(element.attr("aria-busy")).toBeUndefined();
  });

  it("keeps the spinner when the grid sets icon-loading='spinner'", () => {
    const {element, scope} = compileGrid({iconLoading: "spinner"});

    const loader = element.find(GRID_LOADER);
    expect(scope.iconLoader).toBe("spinner");
    expect(loader.length).toBe(1);
    expect(loader[0].tagName).toBe("AWE-LOADER");
    expect(element.find("awe-grid-skeleton").length).toBe(0);
  });

  it("keeps the spinner on every grid when the application default is spinner", () => {
    settings.update({gridIconLoading: "spinner"});
    const {element, scope} = compileGrid();

    expect(scope.iconLoader).toBe("spinner");
    expect(element.find(GRID_LOADER)[0].tagName).toBe("AWE-LOADER");
  });

  it("uses the skeleton when the grid sets icon-loading='skeleton' even if the default is spinner", () => {
    settings.update({gridIconLoading: "spinner"});
    const {element} = compileGrid({iconLoading: "skeleton"});

    expect(element.find(GRID_LOADER)[0].tagName).toBe("AWE-GRID-SKELETON");
  });

  it.each(["grid.html", "treegrid.html"])("renders the skeleton with the grid-loader hook in the %s template", template => {
    const html = fs.readFileSync(path.join(TEMPLATES, template), "utf8");

    expect(html).toMatch(/<awe-grid-skeleton[^>]*data-testid="grid-loader"[^>]*ng-if="[^"]*iconLoader === 'skeleton'/);
    expect(html).toMatch(/<awe-loader[^>]*data-testid="grid-loader"[^>]*ng-if="[^"]*iconLoader !== 'skeleton'/);
    expect(html).toMatch(/ng-attr-aria-busy="\{\{controller\.loading \|\| undefined\}\}"/);
  });

  describe("reload with rows", () => {
    const ROWS = [{name: "a", date: "2026-01-01"}, {name: "b", date: "2026-01-02"}];

    it("dims the current rows instead of drawing the skeleton or the spinner when the default loader reloads", () => {
      const {element} = compileGrid({rows: ROWS});

      const loader = element.find(GRID_LOADER);
      expect(loader.length).toBe(1);
      expect(loader[0].tagName).toBe("AWE-LOADER");
      expect(loader.hasClass("grid-reload")).toBe(true);
      expect(element.find("awe-grid-skeleton").length).toBe(0);
      expect(element.hasClass("grid-reloading")).toBe(true);
      expect(element.attr("aria-busy")).toBe("true");
    });

    it("restores the rows when the reload ends", () => {
      const {element, scope} = compileGrid({rows: ROWS});

      scope.controller.loading = false;
      refs.$rootScope.$digest();

      expect(element.find(GRID_LOADER).length).toBe(0);
      expect(element.hasClass("grid-reloading")).toBe(false);
      expect(element.attr("aria-busy")).toBeUndefined();
    });

    it("still draws the skeleton on the first load, with no rows", () => {
      const {element} = compileGrid();

      expect(element.find(GRID_LOADER)[0].tagName).toBe("AWE-GRID-SKELETON");
      expect(element.hasClass("grid-reloading")).toBe(false);
    });

    it.each(["spinner", "dots"])("keeps the %s icon on the first load and on the reload of a grid with a custom icon-loading", icon => {
      const first = compileGrid({iconLoading: icon});
      expect(first.element.find(GRID_LOADER)[0].tagName).toBe("AWE-LOADER");
      expect(first.element.find("awe-grid-skeleton").length).toBe(0);
      first.scope.$destroy();

      const reload = compileGrid({iconLoading: icon, rows: ROWS});
      expect(reload.element.find(GRID_LOADER)[0].tagName).toBe("AWE-LOADER");
      expect(reload.element.find(GRID_LOADER).hasClass("grid-reload")).toBe(false);
      expect(reload.element.hasClass("grid-reloading")).toBe(false);
      expect(reload.element.attr("aria-busy")).toBe("true");
    });
  });
});
