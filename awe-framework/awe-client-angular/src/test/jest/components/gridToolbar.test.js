// Grid toolbar: Columns panel (popover, dialog or side panel) and Maximize, with the layout of the user applied to the grid
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

  it("shows the quick filters of the grid first in the toolbar, before Columns and Maximize", () => {
    const toolbar = compileGrid({quickFilter: "CrtSta"}).element.find(hook("grid-toolbar"));

    const parts = toolbar.children().map((index, child) => child.getAttribute("data-testid")).get();
    expect(parts.slice(0, 3)).toEqual(["grid-quick-filter", "grid-columns-button", "grid-maximize-button"]);
    expect(toolbar.find(hook("grid-quick-filter")).attr("data-testid-owner")).toBe("Grid");
    expect(compileGrid().element.find(hook("grid-quick-filter")).length).toBe(0);
  });

  it("shows the toolbar with only its quick filters when the grid opts out of the toolbar", () => {
    const toolbar = compileGrid({quickFilter: "CrtSta", showToolbar: false}).element.find(hook("grid-toolbar"));

    expect(toolbar.length).toBe(1);
    expect(toolbar.find(hook("grid-quick-filter")).length).toBe(1);
    expect(toolbar.find(hook("grid-columns-button")).length).toBe(0);
    expect(toolbar.find(hook("grid-maximize-button")).length).toBe(0);
  });

  it("shows the search of the grid first in the toolbar, before the quick filters, Columns and Maximize", () => {
    const toolbar = compileGrid({search: "CrtTxt", quickFilter: "CrtSta"}).element.find(hook("grid-toolbar"));

    const parts = toolbar.children().map((index, child) => child.getAttribute("data-testid")).get();
    expect(parts.slice(0, 4)).toEqual(["grid-search", "grid-quick-filter", "grid-columns-button", "grid-maximize-button"]);
    expect(toolbar.find(hook("grid-search")).attr("data-testid-owner")).toBe("Grid");
    expect(compileGrid({quickFilter: "CrtSta"}).element.find(hook("grid-search")).length).toBe(0);
  });

  it("shows the toolbar with only its search when the grid opts out of the toolbar", () => {
    const toolbar = compileGrid({search: "CrtTxt", showToolbar: false}).element.find(hook("grid-toolbar"));

    expect(toolbar.length).toBe(1);
    expect(toolbar.find(hook("grid-search")).length).toBe(1);
    expect(toolbar.find(hook("grid-columns-button")).length).toBe(0);
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

  it("does not offer the button columns: they are actions, not data (#793)", () => {
    const {element, scope} = compileGrid({
      columnModel: [column("a"), column("b"), column("edit", {component: "button"}), column("c"), column("more", {component: "button"})],
      columnLayout: {order: ["more", "edit", "c", "b", "a"], hidden: ["edit", "b"]}
    });

    const toolbarScope = angular.element(element.find(hook("grid-toolbar"))).scope();
    toolbarScope.openColumns();
    refs.$rootScope.$digest();

    const dialog = element.find(hook("grid-columns-dialog"));
    expect(dialog.find(hook("grid-column-item")).map((i, item) => item.getAttribute("column-id")).get()).toEqual(["c", "b", "a"]);
    expect(dialog.find(hook("grid-columns-counter")).attr("data-visible")).toBe("2");
    expect(dialog.find(hook("grid-columns-counter")).attr("data-total")).toBe("3");
    // The button columns stay visible where the screen puts them, whatever the stored layout says
    const shown = ["a", "b", "c", "edit", "more"];
    expect(visibleNames(scope.gridOptions.columnDefs).filter(name => shown.includes(name))).toEqual(["c", "edit", "a", "more"]);

    scope.component.moveColumn("a", -1);
    expect(visibleNames(scope.gridOptions.columnDefs).filter(name => shown.includes(name))).toEqual(["c", "a", "edit", "more"]);
  });

  it("lists the columns in the Columns panel, nested under their group", () => {
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

      const parameters = lastAction().attr("parameters");
      expect(parameters.gridLayoutScreen).toBe("Scr");
      expect(parameters.gridLayoutGrid).toBe("Grid");
      expect(JSON.parse(parameters.gridLayoutJson)).toEqual({order: ["a", "c", "b", "d"], hidden: ["b"]});
      expect(parameters.targetAction).toBe("saveGridLayout");
      expect(parameters.user).toBeUndefined();
      // Silent: no loading bar and no message when it works (maintain-silent only answers with warnings and errors)
      expect(parameters.serverAction).toBe("maintain-silent");
      expect(lastAction().attr("silent")).toBe(true);
    });

    it("resets to the definition of the screen and removes the stored layout", () => {
      const {scope} = compileGrid({columnLayout: {order: ["d", "c", "b", "a"], hidden: ["c"]}});

      scope.component.resetColumnLayout();

      expect(visibleNames(scope.gridOptions.columnDefs).filter(name => ["a", "b", "c", "d"].includes(name))).toEqual(["a", "b", "c", "d"]);
      const parameters = lastAction().attr("parameters");
      expect(parameters.targetAction).toBe("resetGridLayout");
      expect(parameters.serverAction).toBe("maintain-silent");
      expect(lastAction().attr("silent")).toBe(true);
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
    // The Columns popover and dialog live in the toolbar; the side panel is a modal appended to the page body
    const columns = () => $(document.body).find(hook("grid-columns-dialog"));
    const KEY = ["a", "b", "c", "d"];
    const order = scope => names(scope.gridOptions.columnDefs).filter(name => KEY.includes(name));
    const shown = scope => visibleNames(scope.gridOptions.columnDefs).filter(name => KEY.includes(name));
    let opened = [];
    const openColumns = (controllerOverrides = {}) => {
      const result = compileGrid(controllerOverrides);
      opened.push(result.scope);
      document.body.appendChild(result.element[0]);
      const toolbarScope = angular.element(result.element.find(hook("grid-toolbar"))).scope();
      const button = result.element.find(hook("grid-columns-button"))[0];
      button.focus();
      toolbarScope.openColumns();
      refs.$rootScope.$digest();
      return {...result, toolbarScope, button, dialog: columns()};
    };
    const keyDown = (target, key) => {
      const code = key === "Escape" ? 27 : 9;
      const event = new window.KeyboardEvent("keydown", {key, keyCode: code, which: code, bubbles: true, cancelable: true});
      target.dispatchEvent(event);
      refs.$rootScope.$digest();
      return event;
    };
    const escape = () => keyDown(document.activeElement, "Escape");
    const mouseDown = target => {
      target.dispatchEvent(new window.MouseEvent("mousedown", {bubbles: true, cancelable: true}));
      refs.$rootScope.$digest();
    };
    const click = target => {
      target.click();
      refs.$rootScope.$digest();
    };
    const toggle = (dialog, name) => click(dialog.find(`${hook("grid-column-item")}[column-id='${name}'] ${hook("grid-column-checkbox")}`)[0]);

    // jsdom has no layout: the Columns button and the popover get their boxes (the window is 1024 x 768)
    const BUTTON_RECT = {top: 100, bottom: 130, left: 600, right: 700, width: 100, height: 30};
    const POPOVER_SIZE = {width: 432, height: 300};
    let buttonRect;
    const mockRects = (rect = BUTTON_RECT) => {
      buttonRect = rect;
      jest.spyOn(window.Element.prototype, "getBoundingClientRect").mockImplementation(function () {
        if (this.matches(hook("grid-columns-button"))) {
          return {...buttonRect, x: buttonRect.left, y: buttonRect.top};
        }
        if (this.matches(".grid-columns-popover")) {
          return {top: 0, left: 0, right: POPOVER_SIZE.width, bottom: POPOVER_SIZE.height, x: 0, y: 0, ...POPOVER_SIZE};
        }
        return {top: 0, left: 0, right: 0, bottom: 0, x: 0, y: 0, width: 0, height: 0};
      });
    };

    beforeEach(() => {
      opened = [];
      jest.spyOn(actionController, "addActionList").mockImplementation(() => undefined);
    });

    afterEach(() => {
      opened.forEach(scope => scope.$destroy());
      $(document.body).removeClass("modal-open").children().remove();
    });

    it("keeps the popover as the default of the application settings", () => {
      expect(DefaultSettings.gridColumnsPanel).toBe("popover");
    });

    it("opens the Columns by default as a popover in the toolbar, without a mask: the grid stays in view", () => {
      const {dialog, element} = openColumns();

      expect(dialog.length).toBe(1);
      expect(dialog.attr("data-type")).toBe("popover");
      expect(dialog.attr("data-testid-owner")).toBe("Grid");
      expect(dialog.attr("role")).toBe("dialog");
      expect(dialog.attr("aria-label")).toBe("GRID_COLUMNS_TITLE");
      expect(dialog.hasClass("grid-columns-popover")).toBe(true);
      expect(element.find(hook("grid-toolbar"))[0].contains(dialog[0])).toBe(true);
      expect($(document.body).find(".grid-columns-backdrop, .modal-backdrop").length).toBe(0);
      expect($(document.body).hasClass("modal-open")).toBe(false);
    });

    it("anchors the popover below the Columns button, aligned to its right edge", () => {
      mockRects();
      const {dialog} = openColumns();

      expect(dialog[0].style.top).toBe("138px");
      expect(dialog[0].style.left).toBe(`${700 - POPOVER_SIZE.width}px`);
    });

    it("fits the popover in the room on its side, so a long list of columns scrolls inside it", () => {
      mockRects();
      const {dialog} = openColumns();

      // Below the button: from its bottom plus the gap to the window bottom minus the margin
      expect(dialog[0].style.maxHeight).toBe(`${window.innerHeight - 8 - 138}px`);
    });

    it("keeps the popover inside the window: above the button without room below, off the left edge", () => {
      mockRects({top: 600, bottom: 630, left: 100, right: 200, width: 100, height: 30});
      const {dialog} = openColumns();

      expect(dialog[0].style.top).toBe(`${600 - 8 - POPOVER_SIZE.height}px`);
      expect(dialog[0].style.left).toBe("8px");
      // Above the button: from the window top plus the margin to the button top minus the gap
      expect(dialog[0].style.maxHeight).toBe(`${600 - 8 - 8}px`);
    });

    // The application animates the classes set by ng-class (ngAnimate adds them on the next animation frame): the popover
    // is placed and gets the focus on opening anyway
    it("places the popover and moves the focus into it when the page animates its classes", () => {
      mockRects();
      let $animate;
      let $rootElement;
      inject(["$animate", "$rootElement", (_$animate_, _$rootElement_) => {
        $animate = _$animate_;
        $rootElement = _$rootElement_;
      }]);
      document.body.appendChild($rootElement[0]);
      $animate.enabled(true);
      const result = compileGrid();
      opened.push(result.scope);
      $rootElement.append(result.element);
      refs.$rootScope.$digest();
      const toolbarScope = angular.element(result.element.find(hook("grid-toolbar"))).scope();
      result.element.find(hook("grid-columns-button"))[0].focus();

      toolbarScope.openColumns();
      refs.$rootScope.$digest();

      const dialog = columns();
      expect(dialog.hasClass("grid-columns-popover")).toBe(true);
      expect(dialog[0].style.top).toBe("138px");
      expect(dialog[0].style.left).toBe(`${700 - POPOVER_SIZE.width}px`);
      expect(dialog[0].style.maxHeight).toBe(`${window.innerHeight - 8 - 138}px`);
      expect(dialog[0].contains(document.activeElement)).toBe(true);
    });

    it("follows the Columns button when the page scrolls or the window is resized", () => {
      mockRects();
      const {dialog} = openColumns();

      buttonRect = {...BUTTON_RECT, top: 200, bottom: 230};
      window.dispatchEvent(new window.Event("resize"));
      expect(dialog[0].style.top).toBe("238px");

      buttonRect = {...BUTTON_RECT, top: 50, bottom: 80};
      document.dispatchEvent(new window.Event("scroll"));
      expect(dialog[0].style.top).toBe("88px");
    });

    it("shows the title, the counter as its subtitle and the close button in the header", () => {
      const {dialog} = openColumns({columnLayout: {order: KEY, hidden: ["b"]}});
      const header = dialog.find(".grid-columns-header");

      expect(header.find(".grid-columns-title").text()).toContain("GRID_COLUMNS_TITLE");
      expect(header.find(hook("grid-columns-counter")).attr("data-visible")).toBe("3");
      expect(header.find(hook("grid-columns-counter")).attr("data-total")).toBe("4");
      const close = header.find(hook("grid-columns-close"));
      expect(close.length).toBe(1);
      expect(close.attr("aria-label")).toBe("GRID_COLUMNS_CLOSE");
      expect(close.find("use").attr("href")).toBe("icons/lucide-sprite.svg#x");
    });

    it("draws each column with its handle, checkbox, label (muted when hidden) and arrows", () => {
      const {dialog} = openColumns({columnLayout: {order: KEY, hidden: ["b"]}});
      const item = name => dialog.find(`${hook("grid-column-item")}[column-id='${name}']`);

      [hook("grid-column-handle"), hook("grid-column-checkbox"), ".grid-column-label", hook("grid-column-up"), hook("grid-column-down")]
        .forEach(selector => expect(item("a").find(selector).length).toBe(1));
      expect(item("a").find(hook("grid-column-handle") + " use").attr("href")).toBe("icons/lucide-sprite.svg#menu");
      expect(item("b").find(".grid-column-label").hasClass("grid-column-hidden")).toBe(true);
      expect(item("a").find(".grid-column-label").hasClass("grid-column-hidden")).toBe(false);
      expect(item("a").find(hook("grid-column-up")).prop("disabled")).toBe(true);
      expect(item("d").find(hook("grid-column-down")).prop("disabled")).toBe(true);
    });

    it.each(["popover", "dialog", "side-panel"])("shows Restore default, Cancel and Save in the footer of the %s, and no Show all", panel => {
      const {dialog} = openColumns({columnsPanel: panel});
      const actions = dialog.find(".grid-columns-actions");

      expect(actions.children("button").map((i, button) => button.getAttribute("data-testid")).get())
        .toEqual(["grid-columns-reset", "grid-columns-cancel", "grid-columns-save"]);
      expect(actions.find(hook("grid-columns-reset") + " use").attr("href")).toBe("icons/lucide-sprite.svg#rotate-ccw");
      expect(dialog.find(hook("grid-columns-show-all")).length).toBe(0);
      expect(dialog.find(hook("grid-column-item")).length).toBe(4);
      expect(dialog.find(".grid-columns-header " + hook("grid-columns-counter")).attr("data-visible")).toBe("4");
      expect(dialog.find(".grid-columns-header " + hook("grid-columns-close")).length).toBe(1);
    });

    it("applies the changes to the grid while the popover is open", () => {
      const {dialog, scope} = openColumns();

      toggle(dialog, "b");

      expect(shown(scope)).toEqual(["a", "c", "d"]);
      expect(columns().find(hook("grid-columns-counter")).attr("data-visible")).toBe("3");
    });

    it("moves the focus into the popover and keeps Tab inside", () => {
      const {dialog} = openColumns();

      expect(dialog[0].contains(document.activeElement)).toBe(true);

      const save = dialog.find(hook("grid-columns-save"))[0];
      save.focus();
      keyDown(save, "Tab");
      expect(dialog[0].contains(document.activeElement)).toBe(true);
    });

    it("closes the popover with Esc and gives the focus back to the Columns button", () => {
      const {button} = openColumns();

      escape();

      expect(columns().length).toBe(0);
      expect(document.activeElement).toBe(button);
    });

    it("closes only the popover with Esc, not the window that holds the grid", () => {
      const {element} = openColumns();
      const parentKeyDown = jest.fn();
      element[0].parentNode.addEventListener("keydown", parentKeyDown);

      escape();

      expect(columns().length).toBe(0);
      expect(parentKeyDown).not.toHaveBeenCalled();
      element[0].parentNode.removeEventListener("keydown", parentKeyDown);
    });

    it("closes the popover with its close button, keeping the changes", () => {
      const {dialog, scope, button} = openColumns();
      toggle(dialog, "b");

      click(dialog.find(hook("grid-columns-close"))[0]);

      expect(columns().length).toBe(0);
      expect(shown(scope)).toEqual(["a", "c", "d"]);
      expect(document.activeElement).toBe(button);
    });

    it("closes the popover with a mouse down outside it, keeping the changes", () => {
      const {dialog, scope} = openColumns();
      toggle(dialog, "b");
      const outside = $("<div class='outside'></div>").appendTo(document.body)[0];

      mouseDown(dialog.find(".grid-columns-list")[0]);
      expect(columns().length).toBe(1);

      mouseDown(outside);
      expect(columns().length).toBe(0);
      expect(shown(scope)).toEqual(["a", "c", "d"]);
    });

    it("closes the popover with a mouse down on the Columns button, and its click does not open it again", () => {
      const {button} = openColumns();

      mouseDown(button);
      click(button);

      expect(columns().length).toBe(0);

      // The next click opens it again
      click(button);
      expect(columns().length).toBe(1);
    });

    it.each(["popover", "dialog", "side-panel"])("cancels the changes of the %s: the grid gets back the layout it had when it was opened", panel => {
      const {dialog, scope} = openColumns({columnsPanel: panel, columnLayout: {order: ["d", "c", "b", "a"], hidden: ["c"]}});
      toggle(dialog, "b");
      toggle(dialog, "c");
      click(dialog.find(`${hook("grid-column-item")}[column-id='a'] ${hook("grid-column-up")}`)[0]);
      expect(order(scope)).toEqual(["d", "c", "a", "b"]);

      click(dialog.find(hook("grid-columns-cancel"))[0]);

      expect(order(scope)).toEqual(["d", "c", "b", "a"]);
      expect(shown(scope)).toEqual(["d", "b", "a"]);
      expect(columns().length).toBe(0);
    });

    // The server answers the last Save: it accepts the server action and, as the answer has no cancel, the stack runs
    // the action queued after it
    const answerSave = () => {
      const [actions] = actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];
      actionController.acceptAction(actions[0]);
      refs.$rootScope.$digest();
      actionController.runAction(actions[1]);
      refs.$rootScope.$digest();
    };

    it("cancels back to the layout of the last Save when it is opened again", () => {
      const {dialog, scope, toolbarScope} = openColumns();
      toggle(dialog, "b");
      click(dialog.find(hook("grid-columns-save"))[0]);
      answerSave();
      expect(columns().length).toBe(0);
      toolbarScope.openColumns();
      refs.$rootScope.$digest();
      toggle(columns(), "d");

      click(columns().find(hook("grid-columns-cancel"))[0]);

      expect(shown(scope)).toEqual(["a", "c", "d"]);
    });

    it("cancels back to the layout it had when it was opened after a Save that failed", () => {
      const {dialog, scope} = openColumns();
      toggle(dialog, "b");
      click(dialog.find(hook("grid-columns-save"))[0]);
      actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1][0][0].onError({status: 500});
      refs.$rootScope.$digest();

      click(dialog.find(hook("grid-columns-cancel"))[0]);

      expect(shown(scope)).toEqual(KEY);
    });

    it.each(["popover", "dialog", "side-panel"])("closes the %s once the server answers that the layout was saved", panel => {
      const {dialog, scope, button} = openColumns({columnsPanel: panel});
      toggle(dialog, "b");

      click(dialog.find(hook("grid-columns-save"))[0]);
      // Open until the server answers
      expect(columns().length).toBe(1);

      answerSave();

      expect(columns().length).toBe(0);
      expect(shown(scope)).toEqual(["a", "c", "d"]);
      if (panel !== "dialog") {
        // As the close button: the popover and the side panel give the focus back to the Columns button
        expect(document.activeElement).toBe(button);
      }
    });

    it("keeps the Columns open when the Save call fails", () => {
      const {dialog} = openColumns();

      click(dialog.find(hook("grid-columns-save"))[0]);
      const [[save, close]] = actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];
      // Ajax.manageError tells the action and empties the action stack: the close queued after the call never runs
      save.onError({status: 500});
      close.destroy();
      refs.$rootScope.$digest();

      expect(columns().length).toBe(1);
      expect(columns().attr("aria-busy")).toBeUndefined();
    });

    it("keeps the Columns open when the server answers the Save with an error or a warning", () => {
      const {dialog} = openColumns();

      click(dialog.find(hook("grid-columns-save"))[0]);
      const [[save, close], addAtBottom] = actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];
      // maintain-silent answers an error or a warning with a cancel, which deletes the actions left in the stack
      actionController.acceptAction(save);
      close.destroy();
      refs.$rootScope.$digest();

      expect(addAtBottom).toBe(true);
      expect(columns().length).toBe(1);
      expect(columns().attr("aria-busy")).toBeUndefined();
    });

    // While a Save waits for the server its button shows the loading look: a spinner, "Saving…" and disabled
    describe("Save button while the layout is saved", () => {
      const saveButton = () => columns().find(hook("grid-columns-save"));
      const lastCall = () => actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];

      it.each(["popover", "dialog", "side-panel"])("shows Saving and is disabled in the %s until the server answers", panel => {
        const {dialog} = openColumns({columnsPanel: panel});
        expect(saveButton().text().trim()).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().prop("disabled")).toBe(false);
        expect(saveButton().hasClass("is-loading")).toBe(false);

        click(dialog.find(hook("grid-columns-save"))[0]);

        expect(saveButton().text().trim()).toBe("GRID_COLUMNS_SAVING");
        expect(saveButton().prop("disabled")).toBe(true);
        expect(saveButton().hasClass("is-loading")).toBe(true);
        expect(saveButton().find(".awe-icon-spin").length).toBe(1);
        // Disabled: the layout is not saved twice
        const calls = actionController.addActionList.mock.calls.length;
        columns().scope().saveColumns();
        expect(actionController.addActionList.mock.calls.length).toBe(calls);

        answerSave();
        expect(columns().length).toBe(0);
      });

      it("goes back to Save and enabled when the Save fails", () => {
        const {dialog} = openColumns();
        click(dialog.find(hook("grid-columns-save"))[0]);
        expect(saveButton().text().trim()).toBe("GRID_COLUMNS_SAVING");

        const [[save, close]] = lastCall();
        save.onError({status: 500});
        close.destroy();
        refs.$rootScope.$digest();

        expect(columns().length).toBe(1);
        expect(saveButton().text().trim()).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().prop("disabled")).toBe(false);
        expect(saveButton().hasClass("is-loading")).toBe(false);
        expect(saveButton().find(".awe-icon-spin").length).toBe(0);
      });

      it("goes back to Save when the server answers the Save with a warning", () => {
        const {dialog} = openColumns();
        click(dialog.find(hook("grid-columns-save"))[0]);

        const [[save, close]] = lastCall();
        actionController.acceptAction(save);
        close.destroy();
        refs.$rootScope.$digest();

        expect(columns().length).toBe(1);
        expect(saveButton().text().trim()).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().prop("disabled")).toBe(false);
      });

      it("does not change the Save button while Restore default waits for the server", () => {
        const {dialog} = openColumns();

        click(dialog.find(hook("grid-columns-reset"))[0]);

        expect(columns().attr("aria-busy")).toBe("true");
        expect(saveButton().text().trim()).toBe("GRID_COLUMNS_SAVE");
        expect(saveButton().prop("disabled")).toBe(false);
        expect(saveButton().hasClass("is-loading")).toBe(false);
      });
    });

    it("keeps the Columns open after Restore default", () => {
      const {dialog} = openColumns();

      click(dialog.find(hook("grid-columns-reset"))[0]);
      const [actions] = actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];
      actionController.acceptAction(actions[0]);
      refs.$rootScope.$digest();

      expect(actions.length).toBe(1);
      expect(columns().length).toBe(1);
    });

    it("cancels back to the layout of the last Restore default", () => {
      const {dialog, scope} = openColumns({columnLayout: {order: ["d", "c", "b", "a"], hidden: ["c"]}});
      click(dialog.find(hook("grid-columns-reset"))[0]);
      toggle(dialog, "a");

      click(dialog.find(hook("grid-columns-cancel"))[0]);

      expect(order(scope)).toEqual(KEY);
      expect(shown(scope)).toEqual(KEY);
    });

    it("saves and restores the default layout from the popover, which stays open until the server answers", () => {
      const {dialog, scope} = openColumns({columnLayout: {order: ["d", "c", "b", "a"], hidden: ["c"]}});
      const lastAction = () => actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1][0][0];

      click(dialog.find(hook("grid-columns-save"))[0]);
      expect(lastAction().attr("parameters").targetAction).toBe("saveGridLayout");
      expect(JSON.parse(lastAction().attr("parameters").gridLayoutJson)).toEqual({order: ["d", "c", "b", "a"], hidden: ["c"]});

      click(dialog.find(hook("grid-columns-reset"))[0]);
      expect(lastAction().attr("parameters").targetAction).toBe("resetGridLayout");
      expect(shown(scope)).toEqual(KEY);
      expect(columns().length).toBe(1);
    });

    // Save and Restore default are silent: the only sign of the call is aria-busy on the Columns, which the tests wait for
    describe("busy while the layout is saved or restored", () => {
      const lastAction = () => actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1][0][0];
      const busy = () => columns().attr("aria-busy");
      const CASES = [["popover", "save"], ["popover", "reset"], ["dialog", "save"], ["side-panel", "reset"]];

      it.each(CASES)("marks the %s busy until the server answers the %s", (panel, button) => {
        const {dialog} = openColumns({columnsPanel: panel});
        expect(busy()).toBeUndefined();

        click(dialog.find(hook(`grid-columns-${button}`))[0]);
        expect(busy()).toBe("true");

        // The answer of the server accepts the action (Ajax.manageMessage)
        actionController.acceptAction(lastAction());
        refs.$rootScope.$digest();
        expect(busy()).toBeUndefined();
      });

      it.each(CASES)("marks the %s not busy when the %s call fails", (panel, button) => {
        const {dialog} = openColumns({columnsPanel: panel});

        click(dialog.find(hook(`grid-columns-${button}`))[0]);
        expect(busy()).toBe("true");

        // A failed call tells the action (Ajax.manageError)
        lastAction().onError({status: 500});
        refs.$rootScope.$digest();
        expect(busy()).toBeUndefined();
      });

      it("keeps the Columns busy until the last of two calls ends", () => {
        const {dialog} = openColumns();
        click(dialog.find(hook("grid-columns-save"))[0]);
        const save = lastAction();
        click(dialog.find(hook("grid-columns-reset"))[0]);
        const reset = lastAction();

        actionController.acceptAction(save);
        refs.$rootScope.$digest();
        expect(busy()).toBe("true");

        actionController.rejectAction(reset);
        refs.$rootScope.$digest();
        expect(busy()).toBeUndefined();
      });

      it("sends the call as a silent maintain-silent server action of the base view", () => {
        const {dialog} = openColumns();

        click(dialog.find(hook("grid-columns-save"))[0]);

        const [actions, addAtBottom, addressData] = actionController.addActionList.mock.calls[actionController.addActionList.mock.calls.length - 1];
        // The server action, and the silent action after it that closes the Columns when the answer does not cancel it
        expect(actions.length).toBe(2);
        expect(actions[1].attr("silent")).toBe(true);
        expect(actions[1].attr("async")).toBe(false);
        expect(actions[0].attr("type")).toBe("server");
        expect(actions[0].attr("silent")).toBe(true);
        expect(actions[0].attr("async")).toBe(false);
        expect(actions[0].attr("parameters").serverAction).toBe("maintain-silent");
        expect(addAtBottom).toBe(true);
        expect(addressData).toEqual({address: {}, context: ""});
      });
    });

    it("removes the listeners of the popover with the grid", () => {
      const {scope, button} = openColumns();
      const close = jest.spyOn(angular.element(button).scope(), "closeColumns");

      scope.$destroy();
      mouseDown(document.body);

      expect(close).not.toHaveBeenCalled();
    });

    it("opens the Columns as a modal dialog over the mask when the grid says so", () => {
      const {dialog} = openColumns({columnsPanel: "dialog"});

      expect(dialog.attr("data-type")).toBe("dialog");
      expect(dialog.hasClass("grid-columns-dialog")).toBe(true);
      expect($(document.body).find(".grid-columns-backdrop").length).toBe(1);
    });

    it("keeps the dialog as it was: Esc does not close it", () => {
      openColumns({columnsPanel: "dialog"});

      escape();

      expect(columns().length).toBe(1);
    });

    it("closes the dialog with its close button, keeping the changes", () => {
      const {dialog, scope} = openColumns({columnsPanel: "dialog"});
      toggle(dialog, "b");

      click(dialog.find(hook("grid-columns-close"))[0]);

      expect(columns().length).toBe(0);
      expect(shown(scope)).toEqual(["a", "c", "d"]);
    });

    it("opens the Columns as a side panel like the side-panel element: a modal on the right edge that slides in", () => {
      const {dialog} = openColumns({columnsPanel: "side-panel"});

      expect(dialog.attr("data-type")).toBe("side-panel");
      expect(dialog.attr("data-testid-owner")).toBe("Grid");
      ["modal", "fade", "in", "side-panel", "side-panel-right"].forEach(name => expect(dialog.hasClass(name)).toBe(true));
      expect(dialog.find(".modal-header .grid-columns-title").text()).toContain("GRID_COLUMNS_TITLE");
      expect(dialog.find(".modal-header " + hook("grid-columns-close")).length).toBe(1);
    });

    it("shows the same list in its body and the actions in its footer", () => {
      const {dialog} = openColumns({columnsPanel: "side-panel"});

      expect(dialog.find(".side-panel-body " + hook("grid-column-item")).length).toBe(4);
      expect(dialog.find(".modal-header " + hook("grid-columns-counter")).attr("data-visible")).toBe("4");
      ["grid-columns-reset", "grid-columns-cancel", "grid-columns-save"]
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

      const save = dialog.find(hook("grid-columns-save"))[0];
      save.focus();
      save.dispatchEvent(new window.KeyboardEvent("keydown", {key: "Tab", bubbles: true, cancelable: true}));
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

    it("gives the focus back to the Columns button when the side panel is closed with its close button", () => {
      const {button, dialog} = openColumns({columnsPanel: "side-panel"});

      click(dialog.find(hook("grid-columns-close"))[0]);

      expect(columns().length).toBe(0);
      expect(document.activeElement).toBe(button);
    });

    it("removes the side panel from the page with the grid", () => {
      const {scope} = openColumns({columnsPanel: "side-panel"});

      scope.$destroy();

      expect($(document.body).find(".modal.side-panel").length).toBe(0);
    });

    it("opens the Columns of a grid without columns-panel as the application settings say", () => {
      inject(["AweSettings", settings => settings.update({gridColumnsPanel: "side-panel"})]);

      const {dialog} = openColumns();

      expect(dialog.attr("data-type")).toBe("side-panel");
    });

    it("lets the columns-panel of the grid win over the application settings", () => {
      inject(["AweSettings", settings => settings.update({gridColumnsPanel: "side-panel"})]);

      const {dialog} = openColumns({columnsPanel: "popover"});

      expect(dialog.attr("data-type")).toBe("popover");
    });
  });

  it("uses the screen texts of the window maximize in the toolbar button", () => {
    const {element} = compileGrid();
    const button = element.find(hook("grid-maximize-button"));
    expect(button.attr("title")).toBe("SCREEN_TEXT_MAXIMIZE");
    expect(button.find(".button-text").text()).toBe("SCREEN_TEXT_MAXIMIZE");
  });
});
