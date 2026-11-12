import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates, readTemplate} from "./gridTestUtils";

const hook = testId => `[data-testid='${testId}']`;
const VIEW = "base";
const GRID = "GrdOpt";
// A tree of three options: a parent with two children, all loaded
const ROWS = [
  {id: "Root", parent: "", name: "Root", screen: "root-screen", status: "Allowed", tone: "success", expanded: true},
  {id: "Child1", parent: "Root", name: "Child 1", screen: "child-1", status: "Denied", tone: "danger"},
  {id: "Child2", parent: "Root", name: "Child 2", screen: "child-2", status: "Allowed", tone: "success"}
];

const button = (id, extra = {}) => ({id, label: `LABEL_${id}`, icon: "trash", visible: true, actions: [
  {type: "server", parameters: {serverAction: "maintain", targetAction: `Mnt${id}`}}
], ...extra});

/**
 * The tree grid of the access window (#791): the toolbar, the rich cells, the row buttons and the checkbox multiselect
 * of the flat grid
 */
describe("tree grid toolbar, cells and selection", () => {
  let refs;
  let element;
  let scope;
  let $serverData;
  let $actionController;

  const storage = kind => refs.$storage.get(kind)[VIEW];
  const digest = () => refs.$rootScope.$digest();
  const row = id => element.find(`${hook("grid-row")}[row-id='${id}']`);
  const cell = (id, column) => row(id).find(`${hook("grid-cell")}[column-id='${column}']`);
  const rowCheckbox = id => row(id).find(hook("grid-row-checkbox"));
  const headerCheckbox = () => element.find(hook("grid-header-checkbox"));
  const bar = () => element.find(hook("grid-selection-bar"));
  const treeIcon = id => row(id).find(hook("tree-icon"));
  const expandCell = id => cell(id, "name").closest(".ui-grid-cell");

  const store = (id, controller, model = {}) => {
    $serverData.storeComponent({id, controller, model}, storage("model"), storage("controller"), storage("api"), "access");
  };

  /**
   * Mount a tree grid with its buttons, as the server renders it, with the real storage
   * @param {object} controller Tree grid controller attributes
   * @param {object[]} buttons Buttons of the grid
   * @param {object[]} values Rows of the tree
   * @returns {object} Tree grid component
   */
  const mount = (controller = {}, buttons = [], values = ROWS) => {
    inject(["$templateCache", $templateCache => {
      cacheGridTemplates($templateCache);
      $templateCache.put($serverData.getAngularTemplateUrl("treegrid"), readTemplate("treegrid.html"));
      $templateCache.put("ui-grid/treeBaseRowHeaderButtons", readTemplate("grid/cellTreeIcons.html"));
      $templateCache.put("ui-grid/treeBaseExpandAllButtons", readTemplate("grid/headerTreeIcons.html"));
    }]);
    ["controller", "model", "api", "messages", "screen", "status"].forEach(kind => refs.$storage.put(kind, {[VIEW]: {}}));
    refs.$storage.get("screen")[VIEW] = {name: "access", option: "access"};
    refs.$storage.get("status")[VIEW] = "loaded";
    refs.$storage.put("actions-running", false);
    store(GRID, {
      id: GRID, visible: true, loading: false, serverAction: "data", targetAction: "OptLst", loadAll: true,
      treegrid: true, treeId: "id", treeParent: "parent", expandColumn: "name", initialLevel: 3,
      multiselect: false, editable: false, enableFilters: false, showTotals: false, disablePagination: true, max: 30,
      pagerValues: [10, 20],
      columnModel: [
        {name: "name", id: "name", label: "Name", secondaryField: "screen", sendable: true},
        {name: "status", id: "status", label: "Status", statusField: "tone", sendable: false},
        {name: "EdtBtn", id: "EdtBtn", component: "button", icon: "lucide:pencil", title: "Edit", style: "btn-ghost no-label",
          sendable: false, actions: [{type: "server", parameters: {serverAction: "data", targetAction: "OptWhy"}},
            {type: "dialog", target: "PnlWhy"}]},
        {name: "screen", id: "screen", hidden: true, sendable: false},
        {name: "tone", id: "tone", hidden: true, sendable: false}
      ],
      buttonModel: buttons, headerModel: [], ...controller
    }, {page: 1, records: values.length, total: 1, selected: [], values: values.map(value => ({...value}))});
    buttons.forEach(definition => store(definition.id, {...definition}));
    store("CrtTxt", {id: "CrtTxt", component: "grid-search", visible: true, contextMenu: [], dependencies: []},
      {selected: [], values: [], page: 1, records: 0, total: 0});
    store("CrtFlt", {id: "CrtFlt", component: "quick-filter", visible: true, contextMenu: [], dependencies: []},
      {selected: "", values: [{value: "", label: "All", count: 3}, {value: "DEN", label: "Denied", count: 1}], page: 1, records: 2, total: 1});

    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    jest.spyOn(console, "error").mockImplementation(() => undefined);

    scope = refs.$rootScope.$new();
    const children = buttons.map(definition => `<awe-button button-id='${definition.id}'></awe-button>`).join("");
    const form = refs.$compile(`<form awe-form><awe-tree-grid tree-grid-id='${GRID}' style='height: 300px; width: 400px'>${children}</awe-tree-grid></form>`)(scope);
    $(document.body).append(form);
    digest();
    // ui-grid builds the tree and expands its rows in its own timeouts
    inject(["$timeout", $timeout => $timeout.flush()]);
    digest();
    element = form.find(".grid").first();
    return element.isolateScope().component;
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ServerData", "ActionController", "$translate", (ServerData, ActionController, $translate) => {
      $serverData = ServerData;
      $actionController = ActionController;
      const instant = $translate.instant.bind($translate);
      jest.spyOn($translate, "instant").mockImplementation((key, parameters) =>
        parameters ? `${key}(${parameters.count})` : instant(key));
    }]);
  });

  afterEach(() => {
    element?.closest("form").remove();
    scope?.$destroy();
    element = null;
    cleanupHeavyComponentTest();
  });

  describe("toolbar and cells", () => {
    it("shows the search and the quick filter with its counts in the toolbar", () => {
      mount({search: "CrtTxt", quickFilter: "CrtFlt"});

      const toolbar = element.find(hook("grid-toolbar"));
      expect(toolbar.find(hook("grid-search")).attr("data-testid-owner")).toBe(GRID);
      expect(toolbar.find(hook("grid-quick-filter")).attr("data-testid-owner")).toBe(GRID);
      expect(toolbar.find(hook("grid-quick-filter-option")).map((index, option) => option.textContent.replace(/\s+/g, " ").trim()).get())
        .toEqual(["All 3", "Denied 1"]);
    });

    it("shows the status of a row with its tone and the secondary line under the main value", () => {
      mount();

      const status = cell("Child1", "status").find("[data-tone]");
      expect(status.attr("data-tone")).toBe("danger");
      expect(status.text().trim()).toBe("Denied");
      expect(cell("Child1", "name").text()).toContain("Child 1");
      expect(cell("Child1", "name").text()).toContain("child-1");
    });

    it("launches the actions of a row button after selecting only its row", () => {
      const component = mount({multiselect: true});
      component.setSelection(["Root", "Child2"]);
      digest();
      const addActionList = jest.spyOn($actionController, "addActionList").mockImplementation(() => undefined);

      const edit = cell("Child1", "EdtBtn").find("button");
      expect(edit.attr("aria-label")).toBe("Edit");
      edit[0].click();
      digest();

      expect(component.model.selected).toEqual(["Child1"]);
      const [actions] = addActionList.mock.calls[addActionList.mock.calls.length - 1];
      expect(actions.map(action => action.type)).toEqual(["server", "dialog"]);
    });
  });

  describe("empty state", () => {
    it("shows the empty state of the grid in a tree grid without rows", () => {
      mount({emptyTitle: "MY_TITLE", emptyText: "MY_TEXT"}, [], []);

      const empty = element.find(hook("grid-empty"));
      expect(empty.length).toBe(1);
      expect(empty.find(hook("grid-empty-title")).attr("translate-multiple")).toBe("MY_TITLE");
      expect(empty.find(hook("grid-empty-text")).attr("translate-multiple")).toBe("MY_TEXT");
    });

    it("does not show the empty state in a tree grid with rows", () => {
      mount();

      expect(element.find(hook("grid-empty")).length).toBe(0);
    });
  });

  describe("tree icons", () => {
    it("draws the tree icon inside the cell of the expand column, before its content, and no tree row header", () => {
      mount();

      expect(element.find("[role='rowheader']").length).toBe(0);
      ["Root", "Child1", "Child2"].forEach(id => {
        expect(treeIcon(id).length).toBe(1);
        expect(expandCell(id).find(hook("tree-icon")).length).toBe(1);
        expect(!!(treeIcon(id)[0].compareDocumentPosition(cell(id, "name")[0]) & window.Node.DOCUMENT_POSITION_FOLLOWING)).toBe(true);
      });
    });

    it("draws the tree icon in the tree id column when the grid has no expand column", () => {
      mount({expandColumn: undefined, columnModel: [
        {name: "name", id: "name", label: "Name"},
        {name: "id", id: "id", label: "Id"},
        {name: "status", id: "status", label: "Status"}
      ]});

      expect(cell("Child1", "id").closest(".ui-grid-cell").find(hook("tree-icon")).length).toBe(1);
      expect(expandCell("Child1").find(hook("tree-icon")).length).toBe(0);
    });

    it("draws the tree icon in the first visible column when the expand column is not in the grid", () => {
      mount({expandColumn: "missing"});

      expect(expandCell("Child1").find(hook("tree-icon")).length).toBe(1);
    });

    it("collapses and expands a branch with its tree icon", () => {
      mount();
      const flush = () => {
        inject(["$timeout", $timeout => $timeout.flush()]);
        digest();
      };

      // ui-grid may keep the leaving rows in the DOM of jsdom: the rows shown are the distinct ones
      const shownRows = () => [...new Set(element.find(hook("grid-row")).map((index, node) => node.getAttribute("row-id")).get())];

      treeIcon("Root")[0].click();
      flush();
      expect(shownRows()).toEqual(["Root"]);
      expect(treeIcon("Root").attr("data-expanded")).toBe("false");

      treeIcon("Root")[0].click();
      flush();
      expect(shownRows()).toEqual(["Root", "Child1", "Child2"]);
      expect(treeIcon("Root").first().attr("data-expanded")).toBe("true");
    });

    it("asks the server for the children of a branch not loaded yet", () => {
      mount({loadAll: false}, [], [{id: "Root", parent: "", name: "Root", screen: "root-screen", status: "Allowed", tone: "success", isLeaf: false}]);
      const addActionList = jest.spyOn($actionController, "addActionList").mockImplementation(() => undefined);

      treeIcon("Root")[0].click();
      digest();

      const [actions] = addActionList.mock.calls[addActionList.mock.calls.length - 1];
      expect(actions[0].parameters.type).toBe("tree-branch");
      expect(actions[0].parameters.expandingBranch).toBe("Root");
    });
  });

  describe("without multiselect", () => {
    it("shows no selection checkbox and selects only the clicked row", () => {
      const component = mount();
      component.setSelection(["Root"]);
      digest();

      expect(element.find(hook("grid-row-checkbox")).length).toBe(0);
      expect(headerCheckbox().length).toBe(0);
      cell("Child2", "status")[0].click();
      digest();

      expect(component.model.selected).toEqual(["Child2"]);
    });

    it("renders no left pinned container, so each row is a single element with its row id", () => {
      mount();

      expect(element.find(".ui-grid-pinned-container-left").length).toBe(0);
      ["Root", "Child1", "Child2"].forEach(id => expect(row(id).length).toBe(1));
    });
  });

  describe("with multiselect", () => {
    it("shows a checkbox per row and a header checkbox, checked for the selected rows", () => {
      const component = mount({multiselect: true});
      component.setSelection(["Child1"]);
      digest();

      expect(headerCheckbox().attr("data-selected")).toBe("false");
      expect(["Root", "Child1", "Child2"].map(id => rowCheckbox(id).attr("data-selected"))).toEqual(["false", "true", "false"]);
    });

    it("shows the checkboxes in the first column, the only row header, before the expand and collapse icons of the tree", () => {
      mount({multiselect: true});

      const precedes = (first, second) => !!(first[0].compareDocumentPosition(second[0]) & window.Node.DOCUMENT_POSITION_FOLLOWING);
      ["Root", "Child1", "Child2"].forEach(id => {
        expect(row(id).find("[role='rowheader']").map((index, header) => header.querySelectorAll(hook("grid-row-checkbox")).length).get())
          .toEqual([1]);
        expect(precedes(rowCheckbox(id), treeIcon(id))).toBe(true);
        expect(rowCheckbox(id).closest(".ui-grid-pinned-container-left").length).toBe(1);
      });
    });

    it("adds the clicked rows to the selection and removes a row with a second click", () => {
      const component = mount({multiselect: true});

      cell("Child1", "status")[0].click();
      cell("Child2", "status")[0].click();
      digest();
      expect(component.model.selected).toEqual(["Child1", "Child2"]);

      cell("Child1", "status")[0].click();
      digest();
      expect(component.model.selected).toEqual(["Child2"]);
    });

    it("selects a parent without its children", () => {
      const component = mount({multiselect: true});

      cell("Root", "status")[0].click();
      digest();

      expect(component.model.selected).toEqual(["Root"]);
      expect(["Root", "Child1", "Child2"].map(id => rowCheckbox(id).attr("data-selected"))).toEqual(["true", "false", "false"]);
    });

    it("selects every row with the header checkbox and clears the selection when all of them are selected", () => {
      const component = mount({multiselect: true});

      headerCheckbox().find("input").click();
      digest();
      expect(component.model.selected).toEqual(["Root", "Child1", "Child2"]);

      headerCheckbox().find("input").click();
      digest();
      expect(component.getSelection()).toEqual([]);
      expect(headerCheckbox().attr("data-selected")).toBe("false");
    });

    it("shows the selection buttons in the selection bar with the number of selected rows", () => {
      const component = mount({multiselect: true}, [button("ButNew"), button("ButAlw", {selection: true})]);

      component.setSelection(["Root", "Child2"]);
      digest();

      expect(bar().attr("data-count")).toBe("2");
      expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("SCREEN_TEXT_GRID_SELECTED_ROWS(2)");
      expect(bar().find(".grid-selection-bar-actions button").map((index, node) => node.id).get()).toEqual(["ButAlw"]);
    });

    it("clears the selection of the tree with the clear button of the selection bar, in the band of the toolbar", () => {
      const component = mount({multiselect: true, search: "CrtTxt"}, [button("ButAlw", {selection: true})]);
      component.setSelection(["Root", "Child2"]);
      digest();

      expect(element.find(hook("grid-toolbar")).find(hook("grid-selection-bar")).length).toBe(1);
      bar().find(".grid-selection-bar-clear")[0].click();
      digest();

      expect(component.getSelection()).toEqual([]);
      expect(bar().attr("data-count")).toBe("0");
      expect(document.activeElement).toBe(element.find(`${hook("grid-search")} input`)[0]);
    });

    it("sends the values of the selected rows of a column, as the flat grid does", () => {
      const component = mount({multiselect: true});

      component.setSelection(["Root", "Child2"]);
      digest();

      const data = component.getData();
      expect(data.name).toEqual(["Root", "Child 2"]);
      expect(data["name.selected"]).toEqual(["Root", "Child 2"]);
      expect(data[GRID]).toEqual(["Root", "Child2"]);
      expect(data[`${GRID}-id`]).toEqual(["Root", "Child2"]);
    });
  });
});
