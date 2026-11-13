import "./gridTestUtils";

describe('awe-framework/awe-client-angular/src/test/jest/services/grid/wrap.js (wrapping cells of #825)', function () {
  let GridCommons;
  const LONG = "A long description that does not fit in the width of the column and needs several lines";

  const getComponent = (columnModel, extra = {}) => ({
    constants: {ROW_IDENTIFIER: "id", CELL_VALUE: "value", CELL_LABEL: "label", CELL_TITLE: "title", CELL_STYLE: "style"},
    controller: {columnModel, headerModel: [], rowHeight: "30"},
    enableSorting: true,
    scope: {$on: () => null, charSize: 7, gridOptions: {}},
    listeners: {},
    api: {},
    model: {values: []},
    address: {view: "viewId", component: "componentId"},
    attributeMethods: {},
    ...extra
  });

  const init = (columnModel, extra) => {
    const component = getComponent(columnModel, extra);
    new GridCommons(component).init();
    return component;
  };

  beforeEach(function () {
    angular.mock.module('aweApplication');
    inject(["$injector", function ($injector) {
      GridCommons = $injector.get('GridCommons');
    }]);
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  describe("wrapping columns", () => {
    it("wraps nothing in a grid without the attributes", () => {
      const component = init([{id: "desc", charlength: 20}]);
      const column = component.controller.columnModel[0];

      component.fixColumn(column);

      expect(component.wrapsText).toBe(false);
      expect(column.cellTemplate).toBe("grid/cell");
    });

    it("gives a wrapping column a cell that wraps over several lines", () => {
      const component = init([{id: "desc", charlength: 20, wrap: true}]);
      const column = component.controller.columnModel[0];

      component.fixColumn(column);

      expect(component.wrapsText).toBe(true);
      expect(column.cellTemplate).toContain("cell-wrap");
      expect(column.cellTemplate).not.toContain("cell-clamp");
      expect(column.cellTemplate).toContain("data-testid=\"grid-cell\"");
      expect(column.cellTemplate).toContain("ng-bind-html=\"col.grid.appScope.component.getCellValue(COL_FIELD)\"");
    });

    it("limits the lines of a column with a maximum", () => {
      const component = init([{id: "desc", charlength: 20, maxLines: 3}]);
      const column = component.controller.columnModel[0];

      component.fixColumn(column);

      expect(column.cellTemplate).toContain("cell-wrap cell-clamp");
      expect(column.cellTemplate).toContain("--cell-max-lines: 3");
    });

    it("keeps one line when wrap is false", () => {
      const component = init([{id: "desc", charlength: 20, wrap: false, maxLines: 3}, {id: "other", wrap: true}]);
      const column = component.controller.columnModel[0];

      component.fixColumn(column);

      expect(column.cellTemplate).toBe("grid/cell");
      expect(component.wrapsText).toBe(true);
    });

    it("leaves the component columns alone", () => {
      const component = init([{id: "desc", charlength: 20, wrap: true, component: "text"}]);
      const column = component.controller.columnModel[0];

      component.fixColumn(column);

      expect(column.cellTemplate).not.toContain("cell-wrap");
    });

    it("keeps single-line rows when the grid has frozen columns", () => {
      const component = init([{id: "desc", charlength: 20, wrap: true}, {id: "code", frozen: true}]);
      const column = component.controller.columnModel[0];

      component.fixColumn(column);

      expect(component.wrapsText).toBe(false);
      expect(column.cellTemplate).toBe("grid/cell");
    });

    it("keeps single-line rows in a tree grid", () => {
      const component = init([{id: "desc", charlength: 20, wrap: true}], {isTree: true});

      expect(component.wrapsText).toBe(false);
    });
  });

  describe("full text of a cut cell", () => {
    const build = () => {
      document.body.innerHTML = "<div id='grid'><div class='ui-grid-cell-contents cell-wrap cell-clamp'>" + LONG + "</div></div>";
      const component = init([{id: "desc", wrap: true, maxLines: 2}]);
      component.element = $("#grid");
      const cell = document.querySelector(".cell-clamp");
      return {component, cell};
    };
    const cut = (cell, isCut) => {
      Object.defineProperty(cell, "scrollHeight", {configurable: true, value: isCut ? 80 : 40});
      Object.defineProperty(cell, "clientHeight", {configurable: true, value: 40});
    };
    const fire = (cell, type, options) => cell.dispatchEvent(new (type === "keydown" ? window.KeyboardEvent : window.MouseEvent)(type, {bubbles: true, ...options}));

    it("makes the cut cells reachable with the keyboard and leaves the others out", () => {
      const {component, cell} = build();
      cut(cell, true);
      component.markCutCells();
      expect(cell.getAttribute("tabindex")).toBe("0");

      cut(cell, false);
      component.markCutCells();
      expect(cell.hasAttribute("tabindex")).toBe(false);
    });

    it("shows the full text on hover and hides it when the pointer leaves", () => {
      const {component, cell} = build();
      cut(cell, true);
      component.initWrapCells();

      fire(cell, "mouseover");
      expect(document.querySelector(".grid-cell-tooltip .tooltip-inner").textContent).toBe(LONG);

      fire(cell, "mouseout");
      expect(document.querySelector(".grid-cell-tooltip")).toBeNull();
    });

    it("shows the full text on keyboard focus and hides it on blur and with Escape", () => {
      const {component, cell} = build();
      cut(cell, true);
      component.initWrapCells();

      component.markCutCells();

      cell.focus();
      expect(document.querySelector(".grid-cell-tooltip .tooltip-inner").textContent).toBe(LONG);
      cell.blur();
      expect(document.querySelector(".grid-cell-tooltip")).toBeNull();

      cell.focus();
      fire(cell, "keydown", {key: "Escape"});
      expect(document.querySelector(".grid-cell-tooltip")).toBeNull();
    });

    it("lets the keys of the inputs of the grid reach them (the handler of Escape must not return false)", () => {
      const {component, cell} = build();
      component.initWrapCells();
      const event = new window.KeyboardEvent("keydown", {key: "Backspace", bubbles: true, cancelable: true});

      cell.dispatchEvent(event);

      expect(event.defaultPrevented).toBe(false);
    });

    it("shows no tooltip when the text fits", () => {
      const {component, cell} = build();
      cut(cell, false);
      component.initWrapCells();

      fire(cell, "mouseover");

      expect(document.querySelector(".grid-cell-tooltip")).toBeNull();
    });

    it("keeps the row height of the grid as the minimum height of the wrapping rows", () => {
      const {component} = build();
      component.initWrapCells();

      expect(document.querySelector("#grid").style.getPropertyValue("--grid-row-height")).toBe("30px");
    });
  });
});
