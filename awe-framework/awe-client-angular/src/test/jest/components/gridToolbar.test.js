// Grid toolbar: Columns dialog and Maximize, with the layout of the user applied to the grid
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const hook = id => `[data-testid='${id}']`;

describe("grid toolbar", () => {
  let refs;
  let actionController;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ActionController", "ServerData", (_ActionController_, _ServerData_) => {
      actionController = _ActionController_;
      // The fixed header template of grids with group headers is served relative in the tests
      jest.spyOn(_ServerData_, "getAngularTemplateUrl").mockImplementation(template => template);
    }]);
  });

  afterEach(cleanupHeavyComponentTest);

  const column = (name, extra = {}) => ({name, id: name, label: name.toUpperCase(), field: name, ...extra});

  const compileGrid = (controllerOverrides = {}, parentScope = null) => {
    const controller = {
      id: "Grid",
      visible: true,
      loading: false,
      screenId: "Scr",
      showToolbar: true,
      targetAction: "getRows",
      loadAll: false,
      multiselect: false,
      editable: false,
      enableFilters: false,
      showTotals: false,
      disablePagination: false,
      max: 10,
      pagerValues: [10, 20],
      columnModel: [column("a"), column("b"), column("c"), column("d")],
      buttonModel: [],
      headerModel: [],
      ...controllerOverrides
    };
    const model = {page: 1, records: 0, total: 1, selected: [], values: []};
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
    jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    const element = refs.$compile("<awe-grid grid-id='Grid'></awe-grid>")(parentScope || refs.$rootScope.$new());
    refs.$rootScope.$digest();
    return {element, scope: element.isolateScope() || element.scope(), controller};
  };

  const names = defs => defs.map(def => def.name);
  const visibleNames = defs => defs.filter(def => !def.hidden).map(def => def.name);

  it("shows the toolbar by default and not when the grid opts out", () => {
    expect(compileGrid().element.find(hook("grid-toolbar")).length).toBe(1);
    expect(compileGrid({showToolbar: false}).element.find(hook("grid-toolbar")).length).toBe(0);
  });

  it("keeps the order of the screen in the model and orders the grid with the layout of the user", () => {
    const {scope, controller} = compileGrid({columnLayout: {order: ["d", "c", "b", "a"], hidden: ["c"]}});

    expect(names(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["d", "c", "b", "a"]);
    expect(visibleNames(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["d", "b", "a"]);
    expect(names(controller.columnModel).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "b", "c", "d"]);
  });

  it("does not offer the columns the screen hides", () => {
    const {scope} = compileGrid({columnModel: [column("a"), column("key", {hidden: true}), column("b")]});

    scope.component.refreshLayoutNodes();

    expect(scope.component.layoutNodes.map(node => node.column.name)).toEqual(["a", "b"]);
    expect(scope.component.layoutTotal).toBe(2);
  });

  it("lists the columns in the dialog, nested under their group", () => {
    const {element} = compileGrid({
      headerModel: [{startColumnName: "b", numberOfColumns: 2, label: "GRP", titleText: "GRP", columnNames: ["b", "c"]}]
    });

    const toolbarScope = angular.element(element.find(hook("grid-toolbar"))).scope();
    toolbarScope.openColumns();
    refs.$rootScope.$digest();

    const dialog = element.find(hook("grid-columns-dialog"));
    expect(dialog.length).toBe(1);
    expect(dialog.find(hook("grid-column-item")).map((i, item) => item.getAttribute("column-id")).get()).toEqual(["a", "b", "c", "d"]);
    expect(dialog.find(hook("grid-column-group")).find(hook("grid-column-item")).length).toBe(2);
    expect(dialog.find(hook("grid-columns-counter")).attr("data-visible")).toBe("4");
    expect(dialog.find(hook("grid-columns-counter")).attr("data-total")).toBe("4");
  });

  it("hides, shows and reorders columns, and never moves a column out of its group", () => {
    const {scope} = compileGrid({
      headerModel: [{startColumnName: "b", numberOfColumns: 2, label: "GRP", columnNames: ["b", "c"]}]
    });
    const component = scope.component;

    component.setColumnVisible("b", false);
    expect(visibleNames(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "c", "d"]);
    expect(component.layoutVisible).toBe(3);

    component.setColumnVisible("b", true);
    component.moveColumn("c", -1);
    expect(names(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "c", "b", "d"]);

    component.moveColumn("c", -1);
    expect(names(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "c", "b", "d"]);

    component.moveColumnTo("d", "a");
    expect(names(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["d", "c", "b", "a"]);
  });

  it("reorders the columns of ui-grid, and its cache of the column order, when a column moves", () => {
    const {scope} = compileGrid();
    const uiColumns = ["rowNum", "a", "b", "c", "d"].map(name => ({name, isRowHeader: name === "rowNum"}));
    const api = {
      grid: {columns: uiColumns.slice(), moveColumns: {orderCache: []}, queueGridRefresh: jest.fn()},
      core: {notifyDataChange: jest.fn()}
    };
    scope.component.grid.api = api;

    scope.component.moveColumn("c", -1);

    // The row header stays first and the other columns follow the new order, also in the cache that ui-grid restores
    const dataColumns = api.grid.columns.filter(col => !col.isRowHeader).map(col => col.name);
    expect(dataColumns.filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "c", "b", "d"]);
    expect(api.grid.columns[0].name).toBe("rowNum");
    expect(api.grid.moveColumns.orderCache.map(col => col.name).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "c", "b", "d"]);
    expect(api.core.notifyDataChange).toHaveBeenCalledWith("column");
  });

  it("shows all the columns", () => {
    const {scope} = compileGrid({columnLayout: {order: ["a", "b", "c", "d"], hidden: ["b", "c"]}});

    scope.component.showAllColumns();

    expect(visibleNames(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "b", "c", "d"]);
  });

  it("assigns the group headers by membership of the columns", () => {
    const {scope} = compileGrid({
      headerModel: [{startColumnName: "b", numberOfColumns: 2, label: "GRP", columnNames: ["b", "c"]}]
    });

    scope.component.reportChangedColumns();

    expect(Object.keys(scope.component.fixedHeaders)).toEqual(["b", "c"]);
    expect(scope.component.fixedHeaders.c.label).toBe("GRP");
  });

  describe("persistence", () => {
    const lastAction = () => {
      const [actions] = actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];
      return actions[0];
    };

    beforeEach(() => {
      jest.spyOn(actionController, "addActionList").mockImplementation(() => undefined);
    });

    it("saves the layout as the default of the user, without sending the user", () => {
      const {scope} = compileGrid();
      scope.component.setColumnVisible("b", false);
      scope.component.moveColumn("c", -1);

      scope.component.saveColumnLayout();

      const parameters = lastAction().parameters;
      expect(parameters.gridLayoutScreen).toBe("Scr");
      expect(parameters.gridLayoutGrid).toBe("Grid");
      expect(JSON.parse(parameters.gridLayoutJson)).toEqual({order: ["a", "c", "b", "d"], hidden: ["b"]});
      expect(parameters.targetAction).toBe("saveGridLayout");
      expect(parameters.user).toBeUndefined();
    });

    it("resets to the definition of the screen and removes the stored layout", () => {
      const {scope} = compileGrid({columnLayout: {order: ["d", "c", "b", "a"], hidden: ["c"]}});

      scope.component.resetColumnLayout();

      expect(visibleNames(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "b", "c", "d"]);
      const parameters = lastAction().parameters;
      expect(parameters.targetAction).toBe("resetGridLayout");
      expect(parameters.gridLayoutScreen).toBe("Scr");
      expect(parameters.gridLayoutGrid).toBe("Grid");
    });
  });

  it("maximizes the grid like a window: it fills the maximize target and the siblings fade out", () => {
    const target = $("<div class='maximize-target'><div class='sibling'>other</div></div>").appendTo(document.body);
    const {element, scope} = compileGrid();
    target.append(element);
    // jsdom has no layout: the sibling is visible for jQuery
    target.find(".sibling")[0].getClientRects = () => [{}];
    const toolbar = element.find(hook("grid-toolbar"));
    const toolbarScope = angular.element(toolbar).scope();
    const grid = element.is(hook("grid")) ? element : element.find(hook("grid")).first();
    expect(grid.length).toBe(1);
    const animate = jest.spyOn(refs.$utilities, "animateCSS").mockImplementation((node, size, time, end) => end());

    // Always set, also before the first toggle
    expect(grid.attr("data-maximized")).toBe("false");
    expect(toolbar.find(hook("grid-maximize-button")).find("i use").attr("href")).toBe("icons/lucide-sprite.svg#expand");

    toolbarScope.toggleMaximize();
    refs.$rootScope.$digest();
    // The clone animation of the Maximize service grows the grid, with the same time
    expect(animate).toHaveBeenCalledTimes(1);
    expect(animate.mock.calls[0][2]).toBe(300);
    expect(scope.maximized).toBe(true);
    expect(grid.hasClass("maximized")).toBe(true);
    expect(grid.hasClass("grid-maximized")).toBe(false);
    expect(grid.attr("data-maximized")).toBe("true");
    expect(toolbar.attr("data-maximized")).toBe("true");
    expect(target.find(".sibling").hasClass("minimized")).toBe(true);
    expect(toolbar.find(hook("grid-maximize-button")).find("i use").attr("href")).toBe("icons/lucide-sprite.svg#shrink");
    expect(toolbar.find(hook("grid-maximize-button")).text()).toContain("SCREEN_TEXT_RESTORE");

    toolbarScope.toggleMaximize();
    refs.$rootScope.$digest();
    expect(scope.maximized).toBe(false);
    expect(grid.hasClass("maximized")).toBe(false);
    expect(grid.attr("data-maximized")).toBe("false");
    expect(toolbar.attr("data-maximized")).toBe("false");
    expect(target.find(".sibling").hasClass("minimized")).toBe(false);
    expect(toolbar.find(hook("grid-maximize-button")).find("i use").attr("href")).toBe("icons/lucide-sprite.svg#expand");
    target.remove();
  });

  it("exposes the maximized state once the restore animation ends", () => {
    const target = $("<div class='maximize-target'></div>").appendTo(document.body);
    const {element, scope} = compileGrid();
    target.append(element);
    const toolbar = element.find(hook("grid-toolbar"));
    const toolbarScope = angular.element(toolbar).scope();
    const grid = element.is(hook("grid")) ? element : element.find(hook("grid")).first();
    let endAnimation;
    jest.spyOn(refs.$utilities, "animateCSS").mockImplementation((node, size, time, end) => {
      endAnimation = end;
    });

    toolbarScope.toggleMaximize();
    endAnimation();
    refs.$rootScope.$digest();
    expect(grid.attr("data-maximized")).toBe("true");

    // Restoring: the grid is still under the animation clone, so it is not settled yet
    toolbarScope.toggleMaximize();
    refs.$rootScope.$digest();
    expect(scope.maximized).toBe(false);
    expect(grid.attr("data-maximized")).toBe("true");
    expect(toolbar.attr("data-maximized")).toBe("true");

    endAnimation();
    refs.$rootScope.$digest();
    expect(grid.attr("data-maximized")).toBe("false");
    expect(toolbar.attr("data-maximized")).toBe("false");
    target.remove();
  });

  it("keeps the maximize state of the grid apart from the window that holds it", () => {
    const parent = refs.$rootScope.$new();
    parent.maximized = true;
    parent.iconMaximized = true;
    const {element, scope} = compileGrid({showToolbar: false}, parent);
    const grid = element.is(hook("grid")) ? element : element.find(hook("grid")).first();

    expect(scope.maximized).toBe(false);
    expect(grid.hasClass("maximized")).toBe(false);
    expect(grid.attr("data-maximized")).toBe("false");
  });

  it("maximizes a grid whose toolbar is shown after it was linked without it", () => {
    const target = $("<div class='maximize-target'></div>").appendTo(document.body);
    const {element, scope, controller} = compileGrid({showToolbar: false});
    target.append(element);
    const animate = jest.spyOn(refs.$utilities, "animateCSS").mockImplementation((node, size, time, end) => end());

    controller.showToolbar = true;
    refs.$rootScope.$digest();
    const toolbarScope = angular.element(element.find(hook("grid-toolbar"))).scope();

    expect(() => toolbarScope.toggleMaximize()).not.toThrow();
    refs.$rootScope.$digest();
    expect(animate).toHaveBeenCalledTimes(1);
    expect(scope.maximized).toBe(true);
    target.remove();
  });

  describe("columns panel (#843)", () => {
    // The Columns of the dialog mode live in the toolbar; the side panel is a modal appended to the page body
    const columns = () => $(document.body).find(hook("grid-columns-dialog"));
    const openColumns = (controllerOverrides = {}) => {
      const result = compileGrid(controllerOverrides);
      document.body.appendChild(result.element[0]);
      const toolbarScope = angular.element(result.element.find(hook("grid-toolbar"))).scope();
      const button = result.element.find(hook("grid-columns-button"))[0];
      button.focus();
      toolbarScope.openColumns();
      refs.$rootScope.$digest();
      return {...result, toolbarScope, button, dialog: columns()};
    };
    const escape = () => {
      const event = new window.KeyboardEvent("keydown", {key: "Escape", keyCode: 27, which: 27, bubbles: true, cancelable: true});
      document.activeElement.dispatchEvent(event);
      refs.$rootScope.$digest();
    };

    afterEach(() => {
      $(document.body).removeClass("modal-open").children().remove();
    });

    it("opens the Columns as a modal dialog by default", () => {
      const {dialog} = openColumns();

      expect(dialog.attr("data-type")).toBe("dialog");
      expect(dialog.hasClass("side-panel")).toBe(false);
    });

    it("opens the Columns as a side panel like the side-panel element: a modal on the right edge that slides in", () => {
      const {dialog} = openColumns({columnsPanel: "side-panel"});

      expect(dialog.attr("data-type")).toBe("side-panel");
      expect(dialog.attr("data-testid-owner")).toBe("Grid");
      ["modal", "fade", "in", "side-panel", "side-panel-right"].forEach(name => expect(dialog.hasClass(name)).toBe(true));
      expect(dialog.find(".modal-header .modal-title").text()).toContain("GRID_COLUMNS_TITLE");
      expect(dialog.find(".modal-header button.close").length).toBe(1);
    });

    it("shows the same list in its body and the actions in its footer", () => {
      const {dialog} = openColumns({columnsPanel: "side-panel"});

      expect(dialog.find(".side-panel-body " + hook("grid-column-item")).length).toBe(4);
      expect(dialog.find(".side-panel-body " + hook("grid-columns-counter")).attr("data-visible")).toBe("4");
      ["grid-columns-show-all", "grid-columns-reset", "grid-columns-save", "grid-columns-close"]
        .forEach(id => expect(dialog.find(".modal-footer " + hook(id)).length).toBe(1));
    });

    it("hides a column of the grid from the side panel", () => {
      const {dialog, scope} = openColumns({columnsPanel: "side-panel"});

      dialog.find(hook("grid-column-item") + "[column-id='b'] input[type='checkbox']").trigger("click");
      refs.$rootScope.$digest();

      expect(scope.gridOptions.columnDefs.find(def => def.name === "b").hidden).toBe(true);
    });

    it("moves the focus into the side panel and keeps Tab inside", () => {
      const {dialog} = openColumns({columnsPanel: "side-panel"});

      expect(dialog[0].contains(document.activeElement)).toBe(true);

      const close = dialog.find(hook("grid-columns-close"))[0];
      close.focus();
      close.dispatchEvent(new window.KeyboardEvent("keydown", {key: "Tab", bubbles: true, cancelable: true}));
      expect(dialog[0].contains(document.activeElement)).toBe(true);
    });

    it("closes the side panel with Esc and gives the focus back to the Columns button", () => {
      const {button} = openColumns({columnsPanel: "side-panel"});

      escape();

      expect(columns().length).toBe(0);
      expect(document.activeElement).toBe(button);
    });

    it("closes the side panel with a click outside it", () => {
      const {button} = openColumns({columnsPanel: "side-panel"});

      $(document.body).find(".modal.side-panel").trigger("click");
      refs.$rootScope.$digest();

      expect(columns().length).toBe(0);
      expect(document.activeElement).toBe(button);
    });

    it("gives the focus back to the Columns button when the side panel is closed with its Close action", () => {
      const {button, toolbarScope} = openColumns({columnsPanel: "side-panel"});

      toolbarScope.closeColumns();
      refs.$rootScope.$digest();

      expect(columns().length).toBe(0);
      expect(document.activeElement).toBe(button);
    });

    it("removes the side panel from the page with the grid", () => {
      const {scope} = openColumns({columnsPanel: "side-panel"});

      scope.$destroy();

      expect($(document.body).find(".modal.side-panel").length).toBe(0);
    });

    it("keeps the dialog as the default of the application settings", () => {
      expect(DefaultSettings.gridColumnsPanel).toBe("dialog");
    });

    it("opens the Columns of a grid without columns-panel as the application settings say", () => {
      inject(["AweSettings", settings => settings.update({gridColumnsPanel: "side-panel"})]);

      const {dialog} = openColumns();

      expect(dialog.attr("data-type")).toBe("side-panel");
    });

    it("lets the columns-panel of the grid win over the application settings", () => {
      inject(["AweSettings", settings => settings.update({gridColumnsPanel: "side-panel"})]);

      const {dialog} = openColumns({columnsPanel: "dialog"});

      expect(dialog.attr("data-type")).toBe("dialog");
    });

    it("keeps the dialog as it was: Esc does not close it", () => {
      openColumns();

      escape();

      expect(columns().length).toBe(1);
    });
  });

  it("uses the screen texts of the window maximize in the toolbar button", () => {
    const {element} = compileGrid();
    const button = element.find(hook("grid-maximize-button"));
    expect(button.attr("title")).toBe("SCREEN_TEXT_MAXIMIZE");
    expect(button.find(".button-text").text()).toBe("SCREEN_TEXT_MAXIMIZE");
  });
});
