import fs from "fs";
import path from "path";
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates, mountGrid, readTemplate} from "./gridTestUtils";

// AngularJS grids on the density tokens (#885, L94): the default row height of the grids and the tree grids is one line
// of base text plus the vertical cell padding of the density (as the React grid), recalculated when the density of
// <html> changes; a row height set by the application is kept. The tree indents each level by --awe-space-6 and its
// expander is a keyboard button
const SHARED_TOKENS = path.join(__dirname, "..", "..", "..", "..", "..", "awe-design-tokens", "tokens.css");

// One line of --awe-line-height-base text plus 2 x --awe-grid-cell-padding-y: 14 x 1.5 + 2 x 11 and 13 x 1.5 + 2 x 6
const COMFORTABLE_ROW_HEIGHT = 43;
const COMPACT_ROW_HEIGHT = 32;
const TREE_INDENT = 24;

describe("AngularJS grids on the density tokens (#885)", () => {
  let refs;
  let tokens;

  // Let the MutationObserver of the density run and the digest it schedules
  const settle = async () => {
    await Promise.resolve();
    refs.$rootScope.$digest();
  };

  const setDensity = density => {
    if (density) {
      document.documentElement.setAttribute("data-awe-density", density);
    } else {
      document.documentElement.removeAttribute("data-awe-density");
    }
  };

  beforeEach(() => {
    tokens = document.createElement("style");
    tokens.textContent = fs.readFileSync(SHARED_TOKENS, "utf8");
    document.head.appendChild(tokens);
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    tokens.remove();
    setDensity(null);
    document.body.innerHTML = "";
  });

  describe("grid row height", () => {
    it("takes one line of base text plus the vertical cell padding of the comfortable density", () => {
      const {scope, element} = mountGrid(refs);

      expect(scope.gridOptions.rowHeight).toBe(COMFORTABLE_ROW_HEIGHT);
      expect(element[0].style.getPropertyValue("--grid-row-height")).toBe(`${COMFORTABLE_ROW_HEIGHT}px`);
    });

    it("takes the compact tokens when the page starts compact", () => {
      setDensity("compact");
      const {scope} = mountGrid(refs);

      expect(scope.gridOptions.rowHeight).toBe(COMPACT_ROW_HEIGHT);
    });

    it("is calculated again when the density of the page changes", async () => {
      const {scope, element, component} = mountGrid(refs, {}, [{id: 1, name: "One"}, {id: 2, name: "Two"}]);
      const uiGrid = component.grid.api.grid;

      setDensity("compact");
      await settle();

      expect(scope.gridOptions.rowHeight).toBe(COMPACT_ROW_HEIGHT);
      expect(uiGrid.options.rowHeight).toBe(COMPACT_ROW_HEIGHT);
      expect(uiGrid.rows.map(row => row.height)).toEqual([COMPACT_ROW_HEIGHT, COMPACT_ROW_HEIGHT]);
      expect(element[0].style.getPropertyValue("--grid-row-height")).toBe(`${COMPACT_ROW_HEIGHT}px`);

      setDensity("comfortable");
      await settle();

      expect(scope.gridOptions.rowHeight).toBe(COMFORTABLE_ROW_HEIGHT);
    });

    it("keeps the row height that the screen sets, in any density", async () => {
      const {scope, element} = mountGrid(refs, {rowHeight: "30"});

      expect(scope.gridOptions.rowHeight).toBe(30);
      expect(element[0].style.getPropertyValue("--grid-row-height")).toBe("30px");

      setDensity("compact");
      await settle();

      expect(scope.gridOptions.rowHeight).toBe(30);
    });

    it("stops following the density when the grid is destroyed", async () => {
      const {scope} = mountGrid(refs);

      scope.$destroy();
      setDensity("compact");
      await settle();

      expect(scope.gridOptions.rowHeight).toBe(COMFORTABLE_ROW_HEIGHT);
    });

    it("keeps the previous fixed row height when the page has no density tokens", () => {
      tokens.remove();
      const {scope} = mountGrid(refs);

      expect(scope.gridOptions.rowHeight).toBe(27);
    });
  });

  describe("tree grid", () => {
    const mountTreeGrid = (controller = {}) => {
      const treeController = {
        id: "Tree", visible: true, loading: false, targetAction: "getRows", loadAll: true, multiselect: false,
        editable: false, enableFilters: false, showTotals: false, max: 10, pagerValues: [10, 20],
        columnModel: [{name: "name", label: "Name", sendable: true}], buttonModel: [], headerModel: [], ...controller
      };
      const model = {page: 1, records: 0, total: 1, selected: [], values: []};
      inject(["$templateCache", "ServerData", ($templateCache, ServerData) => {
        cacheGridTemplates($templateCache);
        $templateCache.put(ServerData.getAngularTemplateUrl("treegrid"), readTemplate("treegrid.html"));
      }]);
      jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
      jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
      jest.spyOn(refs.$control, "getAddressController").mockReturnValue(treeController);
      jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
      refs.$utilities.timeout.cancel = jest.fn();
      jest.spyOn(console, "error").mockImplementation(() => undefined);
      const element = refs.$compile("<awe-tree-grid tree-grid-id='Tree'></awe-tree-grid>")(refs.$rootScope.$new());
      $(document.body).append(element);
      refs.$rootScope.$digest();
      return {element, scope: element.isolateScope() || element.scope()};
    };

    it("takes the row height of the density and follows its changes", async () => {
      const {scope} = mountTreeGrid();

      expect(scope.gridOptions.rowHeight).toBe(COMFORTABLE_ROW_HEIGHT);

      setDensity("compact");
      await settle();

      expect(scope.gridOptions.rowHeight).toBe(COMPACT_ROW_HEIGHT);
    });

    it("keeps the row height that the screen sets", () => {
      const {scope} = mountTreeGrid({rowHeight: "30"});

      expect(scope.gridOptions.rowHeight).toBe(30);
    });

    it("indents each level by --awe-space-6", () => {
      const {scope} = mountTreeGrid();

      expect(scope.gridOptions.treeIndent).toBe(TREE_INDENT);
    });

    it("keeps the previous indent when the page has no spacing tokens", () => {
      tokens.remove();
      const {scope} = mountTreeGrid();

      expect(scope.gridOptions.treeIndent).toBe(10);
    });
  });

  describe("tree expander", () => {
    const compileExpander = entity => {
      const treeButtonClick = jest.fn();
      const scope = Object.assign(refs.$rootScope.$new(), {
        row: {entity, treeLevel: 1},
        col: {name: "name"},
        grid: {options: {treeIndent: TREE_INDENT}, appScope: {component: {treeButtonClick}}}
      });
      const element = refs.$compile($("<div></div>").html(readTemplate("grid/cellTreeIcons.html")))(scope);
      scope.$digest();
      return {icon: element.find("[data-testid='tree-icon']"), cell: element.find("[data-testid='grid-cell']"), treeButtonClick, scope};
    };

    const keydown = (icon, key) => {
      const event = $.Event("keydown", {key});
      icon.trigger(event);
      return event;
    };

    it("indents the cell by the indent of the grid for each level", () => {
      const {cell} = compileExpander({$$expanded: false});

      expect(cell[0].style.paddingLeft).toBe(`${TREE_INDENT}px`);
    });

    it.each([
      [{$$expanded: true}, "true"],
      [{$$expanded: false}, "false"]
    ])("is a focusable button of a branch %j with its expanded state", (entity, expanded) => {
      const {icon} = compileExpander(entity);

      expect(icon.attr("tabindex")).toBe("0");
      expect(icon.attr("role")).toBe("button");
      expect(icon.attr("aria-expanded")).toBe(expanded);
      expect(icon.attr("data-expanded")).toBe(expanded);
    });

    it("is not a button on a leaf", () => {
      const {icon} = compileExpander({$$isLeaf: true});

      expect(icon.attr("tabindex")).toBeUndefined();
      expect(icon.attr("role")).toBeUndefined();
      expect(icon.attr("aria-expanded")).toBeUndefined();
    });

    it.each(["Enter", " "])("toggles the branch with the %p key", key => {
      const {icon, treeButtonClick, scope} = compileExpander({$$expanded: false});

      const event = keydown(icon, key);

      expect(treeButtonClick).toHaveBeenCalledTimes(1);
      expect(treeButtonClick.mock.calls[0][0]).toBe(scope.row);
      expect(event.isDefaultPrevented()).toBe(true);
    });

    it("ignores the other keys", () => {
      const {icon, treeButtonClick} = compileExpander({$$expanded: false});

      const event = keydown(icon, "ArrowDown");

      expect(treeButtonClick).not.toHaveBeenCalled();
      expect(event.isDefaultPrevented()).toBe(false);
    });

    it("keeps toggling the branch on click", () => {
      const {icon, treeButtonClick, scope} = compileExpander({$$expanded: false});

      icon.trigger("click");

      expect(treeButtonClick).toHaveBeenCalledTimes(1);
      expect(treeButtonClick.mock.calls[0][0]).toBe(scope.row);
    });
  });
});
