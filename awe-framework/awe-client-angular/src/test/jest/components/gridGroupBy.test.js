import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";

// Toolbar of a grid with row grouping (#833): Expand all / Collapse all while the rows are grouped, and the Group by
// panel of a grid with group-selector, which changes the group-by levels on the client
const hook = testId => `[data-testid='${testId}']`;
const COLUMNS = [
  {name: "key", label: "Key", hidden: true},
  {name: "country", label: "Country"},
  {name: "city", label: "City"},
  {name: "name", label: "Name"},
  {name: "code"},
  {name: "amount", label: "Amount", summaryType: "sum"}
];
const ROWS = [
  {id: "r1", key: "1", country: "ES", city: "Madrid", name: "Ann", amount: 100},
  {id: "r2", key: "2", country: "DE", city: "Berlin", name: "Bob", amount: 50},
  {id: "r3", key: "3", country: "ES", city: "Bilbao", name: "Carl", amount: 20}
];

describe("grid toolbar with row grouping (#833)", () => {
  let refs;
  let mounted;

  const mount = (controller = {}) => {
    mounted = mountGrid(refs, {loadAll: true, showToolbar: true, columnModel: COLUMNS.map(column => ({...column})), ...controller},
      ROWS.map(row => ({...row})));
    return mounted;
  };

  const digest = () => refs.$rootScope.$digest();
  const ids = rows => rows.map(row => row.id);
  const click = (element, testId) => {
    element.find(hook(testId))[0].click();
    digest();
  };
  const option = (element, name) => element.find(`${hook("grid-group-option")}[data-column='${name}']`);
  const openPanel = element => click(element, "grid-group-by");

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    mounted?.element.remove();
    mounted = null;
    cleanupHeavyComponentTest();
  });

  describe("expand and collapse all", () => {
    it("collapses and expands every group of every level, without a server call", () => {
      const {element, scope} = mount({groupBy: "country,city"});
      const reload = jest.spyOn(scope.component, "reload");

      const collapse = element.find(hook("grid-group-collapse-all"));
      expect(collapse.prop("tagName")).toBe("BUTTON");
      expect(collapse.attr("title")).toBe("GRID_GROUP_COLLAPSE_ALL");
      expect(collapse.attr("aria-label")).toBe("GRID_GROUP_COLLAPSE_ALL");
      click(element, "grid-group-collapse-all");
      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "$group-ES"]);

      scope.component.toggleGroup("ES");
      digest();
      expect(ids(scope.gridOptions.data)).toEqual(["$group-DE", "$group-ES", "$group-ES/Bilbao", "$group-ES/Madrid", "$subtotal-ES"]);

      expect(element.find(hook("grid-group-expand-all")).attr("title")).toBe("GRID_GROUP_EXPAND_ALL");
      click(element, "grid-group-expand-all");
      expect(ids(scope.gridOptions.data)).toEqual([
        "$group-DE", "$group-DE/Berlin", "r2", "$subtotal-DE/Berlin", "$subtotal-DE",
        "$group-ES", "$group-ES/Bilbao", "r3", "$subtotal-ES/Bilbao", "$group-ES/Madrid", "r1", "$subtotal-ES/Madrid", "$subtotal-ES"
      ]);
      expect(reload).not.toHaveBeenCalled();
    });

    it("shows the buttons only while the rows are grouped", () => {
      expect(mount().element.find(hook("grid-group-expand-all")).length).toBe(0);
      mounted.element.remove();
      const {element} = mount({groupSelector: true});
      expect(element.find(hook("grid-group-expand-all")).length).toBe(0);
      expect(element.find(hook("grid-group-collapse-all")).length).toBe(0);
    });
  });

  describe("place in the toolbar", () => {
    const PARTS = ["grid-search", "grid-quick-filter", "grid-group-expand-all", "grid-group-collapse-all", "grid-group-by",
      "grid-columns-button", "grid-maximize-button"].map(hook).concat(".grid-toolbar-spacer").join(", ");
    const parts = element => element.find(hook("grid-toolbar")).find(PARTS)
      .map((index, part) => part.getAttribute("data-testid") || "spacer").get();

    it("goes after the search and the quick filters, before the spacer and the Columns and Maximize buttons", () => {
      const {element} = mount({groupBy: "country", groupSelector: true, search: "CrtTxt", quickFilter: "CrtSta"});

      expect(parts(element)).toEqual(["grid-search", "grid-quick-filter", "grid-group-by", "grid-group-expand-all",
        "grid-group-collapse-all", "spacer", "grid-columns-button", "grid-maximize-button"]);
    });

    it("is hidden with the Columns and Maximize buttons when the grid opts out of the toolbar", () => {
      const {element} = mount({groupBy: "country", groupSelector: true, search: "CrtTxt", showToolbar: false});

      expect(parts(element)).toEqual(["grid-search"]);
    });
  });

  describe("group by selector", () => {
    it("is not offered without group-selector", () => {
      const {element} = mount({groupBy: "country"});

      expect(element.find(hook("grid-group-by")).length).toBe(0);
      expect(element.find(hook("grid-group-expand-all")).length).toBe(1);
    });

    it("opens a panel with No grouping and the visible columns with a label and without summary type", () => {
      const {element} = mount({groupSelector: true});
      const button = element.find(hook("grid-group-by"));
      expect(button.attr("aria-expanded")).toBe("false");
      expect(button.attr("aria-haspopup")).toBe("dialog");

      openPanel(element);

      const panel = element.find(hook("grid-group-by-panel"));
      expect(panel.length).toBe(1);
      expect(panel.attr("role")).toBe("dialog");
      expect(panel.attr("data-testid-owner")).toBe("Grid");
      expect(element.find(hook("grid-group-by")).attr("aria-expanded")).toBe("true");
      expect(panel.find(hook("grid-group-none")).attr("aria-pressed")).toBe("true");
      const options = panel.find(hook("grid-group-option"));
      expect(options.map((index, node) => node.getAttribute("data-column")).get()).toEqual(["country", "city", "name"]);
      expect(options.map((index, node) => node.type).get()).toEqual(["checkbox", "checkbox", "checkbox"]);
      expect(options.map((index, node) => node.checked).get()).toEqual([false, false, false]);
    });

    it("adds a level at the end when a column is checked and removes it when it is unchecked, live", () => {
      const {element, scope, model} = mount({groupSelector: true});
      const reload = jest.spyOn(scope.component, "reload");
      openPanel(element);

      option(element, "city")[0].click();
      digest();
      option(element, "country")[0].click();
      digest();

      expect(scope.component.getGroupLevels()).toEqual(["city", "country"]);
      expect(ids(scope.gridOptions.data).slice(0, 3)).toEqual(["$group-Berlin", "$group-Berlin/DE", "r2"]);
      expect(option(element, "city").prop("checked")).toBe(true);
      expect(option(element, "city").closest(".grid-group-by-option").find(".grid-group-by-level").text()).toBe("1");
      expect(option(element, "country").closest(".grid-group-by-option").find(".grid-group-by-level").text()).toBe("2");
      expect(option(element, "name").closest(".grid-group-by-option").find(".grid-group-by-level").length).toBe(0);
      expect(element.find(hook("grid-group-none")).attr("aria-pressed")).toBe("false");
      expect(scope.component.grouped).toBe(true);
      expect(scope.gridOptions.enablePagination).toBe(false);
      expect(element.find(hook("grid-group-expand-all")).length).toBe(1);

      option(element, "city")[0].click();
      digest();

      expect(scope.component.getGroupLevels()).toEqual(["country"]);
      expect(option(element, "country").closest(".grid-group-by-option").find(".grid-group-by-level").text()).toBe("1");
      expect(ids(model.values)).toEqual(["r1", "r2", "r3"]);
      expect(reload).not.toHaveBeenCalled();
    });

    it("removes every level with No grouping and shows the rows paginated again", () => {
      const {element, scope, model} = mount({groupSelector: true, groupBy: "country", enableFilters: true});
      expect(scope.gridOptions.enableFiltering).toBe(false);
      openPanel(element);
      expect(option(element, "country").prop("checked")).toBe(true);

      click(element, "grid-group-none");

      expect(scope.component.getGroupLevels()).toEqual([]);
      expect(scope.component.grouped).toBe(false);
      expect(scope.gridOptions.data).toBe(model.values);
      expect(scope.gridOptions.enablePagination).toBe(true);
      expect(scope.gridOptions.useExternalSorting).toBe(false);
      expect(scope.gridOptions.enableFiltering).toBe(true);
      expect(element.find(hook("grid-group-expand-all")).length).toBe(0);
      expect(element.find(hook("grid-pagination")).hasClass("ng-hide")).toBe(false);
      expect(option(element, "country").prop("checked")).toBe(false);
    });

    it("closes the panel with Escape and gives the focus back to its button", () => {
      const {element} = mount({groupSelector: true});
      const button = element.find(hook("grid-group-by"))[0];
      button.focus();
      openPanel(element);
      expect(document.activeElement).toBe(element.find(hook("grid-group-none"))[0]);

      element.find(hook("grid-group-by-panel"))[0].dispatchEvent(new window.KeyboardEvent("keydown", {key: "Escape", bubbles: true}));
      digest();

      expect(element.find(hook("grid-group-by-panel")).length).toBe(0);
      expect(element.find(hook("grid-group-by")).attr("aria-expanded")).toBe("false");
      expect(document.activeElement).toBe(button);
    });

    it("closes the panel with a click outside it", () => {
      const {element} = mount({groupSelector: true});
      openPanel(element);

      document.body.dispatchEvent(new MouseEvent("mousedown", {bubbles: true}));
      digest();

      expect(element.find(hook("grid-group-by-panel")).length).toBe(0);
    });

    it("is not offered by a grid which cannot group its rows", () => {
      const {element} = mount({groupSelector: true, editable: true});

      expect(element.find(hook("grid-group-by")).length).toBe(0);
    });
  });
});
