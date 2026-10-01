import "./lightComponentTestUtils";

import fs from "fs";
import path from "path";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {GRID_TEMPLATE} from "../../../main/resources/js/awe/directives/grid";
import {templateButton, templateColumnButton} from "../../../main/resources/js/awe/services/button";
import {templateColumnCheckbox} from "../../../main/resources/js/awe/services/checkboxRadio";
import {templateColumnColor} from "../../../main/resources/js/awe/services/criterion";
import {calendarColumnTemplate, timeColumnTemplate} from "../../../main/resources/js/awe/services/dateTime";
import {templateNumericColumn} from "../../../main/resources/js/awe/services/numeric";
import {templateSelectorColumn} from "../../../main/resources/js/awe/services/selector";
import {
  passwordColumnTemplate,
  textareaColumnTemplate,
  textColumnTemplate,
  textViewColumnTemplate
} from "../../../main/resources/js/awe/services/text";
import {uploaderColumnTemplate} from "../../../main/resources/js/awe/services/uploader";
import {
  cleanupHeavyComponentTest,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";

const resources = path.join(__dirname, "../../../main/resources");
const angularTemplates = path.join(resources, "templates/angular");
const readTemplate = name => fs.readFileSync(path.join(angularTemplates, name), "utf8");
const readIndex = () => fs.readFileSync(path.join(resources, "index.html"), "utf8");
const hook = testId => `[data-testid='${testId}']`;
const vocabulary = () => Object.keys(TestIds).map(name => TestIds[name]);

/**
 * Template files are served to the client as they are, so they cannot use the vocabulary constant:
 * every literal hook they render must belong to the vocabulary.
 */
function listHtml(directory) {
  return fs.readdirSync(directory, {withFileTypes: true}).flatMap(entry => {
    const entryPath = path.join(directory, entry.name);
    return entry.isDirectory() ? listHtml(entryPath) : entry.name.endsWith(".html") ? [entryPath] : [];
  });
}

describe("vocabulary used by the markup", () => {
  it("only renders hooks of the vocabulary in the template files and the index page", () => {
    const rendered = new Set();
    [...listHtml(angularTemplates), path.join(resources, "index.html")].forEach(file => {
      const markup = fs.readFileSync(file, "utf8");
      Array.from(markup.matchAll(/data-testid="([^"{}]+)"/g)).forEach(match => rendered.add(match[1]));
    });

    expect(rendered.size).toBeGreaterThan(0);
    rendered.forEach(value => expect(vocabulary()).toContain(value));
  });
});

describe("grid test ids", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  /**
   * Compile a ui-grid template fragment. The ui-grid directives that need the grid controller are removed
   * because only the markup of AWE is under test.
   */
  function compileFragment(html, scopeValues = {}) {
    const cleaned = html.replace(/ ui-grid-row="row"| ui-grid-header-cell col="col"/g, "");
    const scope = Object.assign(refs.$rootScope.$new(), scopeValues);
    const element = refs.$compile($("<div></div>").html(cleaned))(scope);
    scope.$digest();
    return element;
  }

  describe("templates", () => {
    const inlineGrid = {name: "inline grid template", template: GRID_TEMPLATE};
    const gridFile = {name: "grid.html", template: readTemplate("grid.html")};
    const treeGridFile = {name: "treegrid.html", template: readTemplate("treegrid.html")};

    it.each([inlineGrid, gridFile, treeGridFile])("$name marks the grid root next to the instance attributes", ({template}) => {
      const root = $("<div></div>").html(template).children().first();

      expect(root.attr("data-testid")).toBe("grid");
      expect(root.hasClass("grid")).toBe(true);
    });

    it.each([inlineGrid, gridFile, treeGridFile])("$name marks the row actions and the loader", ({template}) => {
      const root = $("<div></div>").html(template);

      expect(root.find(`button.grid-row-save${hook(TestIds.gridRowSave)}[id]`).length).toBe(1);
      expect(root.find(`button.grid-row-cancel${hook(TestIds.gridRowCancel)}[id]`).length).toBe(1);
      expect(root.find(`awe-loader.grid-loader${hook(TestIds.gridLoader)}`).length).toBe(1);
    });

    it.each([inlineGrid, gridFile])("$name marks the pagination controls", ({template}) => {
      const root = $("<div></div>").html(template);

      expect(root.find(`.pagination-content${hook(TestIds.gridPagination)}`).length).toBe(1);
      expect(root.find(`a${hook(TestIds.gridPagePrevious)}`).length).toBe(1);
      expect(root.find(`a${hook(TestIds.gridPageNext)}`).length).toBe(1);
      expect(root.find(`input.pagination-goto${hook(TestIds.gridGotoPage)}`).length).toBe(1);
      expect(root.find(`select.grid-pager${hook(TestIds.gridPageSize)}`).length).toBe(1);
    });

    it("keeps the inline grid template and grid.html in sync", () => {
      const normalize = html => $("<div></div>").html(html).find("[data-testid]").map((i, node) => node.getAttribute("data-testid")).get();

      expect(normalize(GRID_TEMPLATE)).toEqual(normalize(readTemplate("grid.html")));
    });

    it.each([
      ["grid/cell.html", ".ui-grid-cell-contents"],
      ["grid/cellCheckbox.html", ".ui-grid-checkbox"],
      ["grid/cellRowNumber.html", ".ui-grid-rownumber"],
      ["grid/cellTreeIcons.html", ".ui-grid-tree-base-row-header-buttons"]
    ])("%s marks the element that carries the column-id as a cell", (file, selector) => {
      const cell = $("<div></div>").html(readTemplate(file)).children().first();

      expect(cell.is(selector)).toBe(true);
      expect(cell.attr("column-id")).toBe("{{col.name}}");
      expect(cell.attr("data-testid")).toBe("grid-cell");
    });

    it("marks the header cell that carries the column-id", () => {
      const cell = $("<div></div>").html(readTemplate("grid/headerCell.html")).find("[ui-grid-header-cell]");

      expect(cell.attr("column-id")).toBe("{{col.name}}");
      expect(cell.attr("data-testid")).toBe("grid-header-cell");
    });

    it("marks the select all and row selection checkboxes on the clickable label", () => {
      const header = $("<div></div>").html(readTemplate("grid/headerCheckbox.html"));
      const row = $("<div></div>").html(readTemplate("grid/cellCheckbox.html"));

      expect(header.find(`label.checkbox${hook(TestIds.gridHeaderCheckbox)}`).length).toBe(1);
      expect(row.find(`label.checkbox${hook(TestIds.gridRowCheckbox)}`).length).toBe(1);
    });

    it("marks the tree expand and collapse icons", () => {
      const row = $("<div></div>").html(readTemplate("grid/cellTreeIcons.html"));
      const header = $("<div></div>").html(readTemplate("grid/headerTreeIcons.html"));

      expect(row.find(`i.tree-icon${hook(TestIds.treeIcon)}`).length).toBe(1);
      expect(header.find(`i${hook(TestIds.treeHeaderIcon)}`).length).toBe(1);
    });
  });

  describe("rendered", () => {
    const rows = [
      {uid: "u1", isSelected: true, entity: {id: "r1"}},
      {uid: "u2", isSelected: false, entity: {id: "r2"}}
    ];
    const viewportScope = () => ({
      rowContainer: {renderedRows: rows},
      colContainer: {getViewportStyle: () => ({}), name: "body"},
      grid: {appScope: {component: {constants: {ROW_IDENTIFIER: "id", ROW_CLASS_FIELD: "style"}, viewportClick: jest.fn()}}}
    });

    it("exposes the viewport and which container it belongs to", () => {
      const element = compileFragment(readTemplate("grid/viewport.html"), viewportScope());

      const viewport = element.find(hook(TestIds.gridViewport));
      expect(viewport.length).toBe(1);
      expect(viewport.hasClass("ui-grid-viewport")).toBe(true);
      expect(viewport.attr("data-container")).toBe("body");
    });

    it("exposes the rows with their selected state next to the row-id", () => {
      const element = compileFragment(readTemplate("grid/viewport.html"), viewportScope());

      const rendered = element.find(hook(TestIds.gridRow));
      expect(rendered.length).toBe(2);
      expect(rendered.eq(0).attr("row-id")).toBe("r1");
      expect(rendered.eq(0).attr("data-selected")).toBe("true");
      expect(rendered.eq(1).attr("row-id")).toBe("r2");
      expect(rendered.eq(1).attr("data-selected")).toBe("false");
    });

    it("follows the selection of a row without class names", () => {
      const scope = viewportScope();
      const element = compileFragment(readTemplate("grid/viewport.html"), scope);

      rows[1].isSelected = true;
      element.scope().$digest();

      expect(element.find(`${hook(TestIds.gridRow)}[data-selected='true']`).length).toBe(2);
      rows[1].isSelected = false;
    });

    it("exposes the header cell of every rendered column", () => {
      const element = compileFragment(readTemplate("grid/headerCell.html"), {
        colContainer: {
          headerCellWrapperStyle: () => ({}),
          renderedColumns: [{uid: "c1", name: "name", colDef: {}}, {uid: "c2", name: "age", colDef: {hidden: true}}]
        }
      });

      const cells = element.find(hook(TestIds.gridHeaderCell));
      expect(cells.map((i, node) => node.getAttribute("column-id")).get()).toEqual(["name", "age"]);
    });

    it("exposes the selected state of the select all and row checkboxes", () => {
      const header = compileFragment(readTemplate("grid/headerCheckbox.html"), {
        grid: {selection: {selectAll: true}, appScope: {component: {selectAllClick: jest.fn()}}}
      });
      const checkbox = compileFragment(readTemplate("grid/cellCheckbox.html"), {row: {isSelected: false}, col: {name: "selection"}});

      expect(header.find(hook(TestIds.gridHeaderCheckbox)).attr("data-selected")).toBe("true");
      expect(checkbox.find(hook(TestIds.gridRowCheckbox)).attr("data-selected")).toBe("false");
    });

    it.each([
      [{$$expanded: true, $$isLoading: false}, "true", "false"],
      [{$$expanded: false, $$isLoading: false}, "false", "false"],
      [{$$expanded: false, $$isLoading: true}, "false", "true"],
      [{}, "false", "false"]
    ])("exposes the expanded and loading state of the tree icon of %j", (entity, expanded, loading) => {
      const element = compileFragment(readTemplate("grid/cellTreeIcons.html"), {
        row: {entity, treeLevel: 0},
        col: {name: "name"},
        grid: {options: {treeIndent: 10}, appScope: {component: {treeButtonClick: jest.fn()}}}
      });

      const icon = element.find(hook(TestIds.treeIcon));
      expect(icon.attr("data-expanded")).toBe(expanded);
      expect(icon.attr("data-loading")).toBe(loading);
    });

    it("hooks the controls an editable cell renders", () => {
      const editors = [
        textColumnTemplate, passwordColumnTemplate, textareaColumnTemplate, templateNumericColumn, calendarColumnTemplate,
        timeColumnTemplate, templateColumnColor, templateSelectorColumn, templateColumnCheckbox, uploaderColumnTemplate
      ];

      editors.forEach(template => {
        expect($("<div></div>").html(template).find(hook(TestIds.criterionInput)).length).toBe(1);
      });
    });
  });
});

describe("column editor test ids", () => {
  const editors = [
    {name: "text", template: textColumnTemplate, control: "input[type='text']"},
    {name: "password", template: passwordColumnTemplate, control: "input[type='password']"},
    {name: "textarea", template: textareaColumnTemplate, control: "textarea"},
    {name: "numeric", template: templateNumericColumn, control: "input[ui-numeric]"},
    {name: "date", template: calendarColumnTemplate, control: "[ui-date] > input[type='text']"},
    {name: "time", template: timeColumnTemplate, control: "input[ui-time]"},
    {name: "color", template: templateColumnColor, control: ".colorpicker-element > input[type='text']"},
    {name: "select", template: templateSelectorColumn, control: "input[type='hidden'][ui-select2]"},
    {name: "checkbox", template: templateColumnCheckbox, control: "input[type='checkbox']"},
    {name: "uploader", template: uploaderColumnTemplate, control: "[ngf-select]"},
    {name: "text view", template: textViewColumnTemplate, control: "span.text-value"}
  ];

  it.each(editors)("marks the real control of the $name column like the criterion does", ({template, control}) => {
    const root = $("<div></div>").html(template);

    const hooked = root.find(hook(TestIds.criterionInput));
    expect(hooked.length).toBe(1);
    expect(hooked.is(control)).toBe(true);
  });

  it("marks the clear action of the uploader column", () => {
    const root = $("<div></div>").html(uploaderColumnTemplate);

    expect(root.find(`button${hook(TestIds.uploadClear)}`).length).toBe(1);
    expect(root.find(`.pfi-filename${hook(TestIds.uploadFilename)}`).length).toBe(1);
  });
});

describe("tab and wizard test ids", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  function compilePanel(tag, idAttribute, component) {
    const model = createPanelModel("2");
    model.selectedIndex = 1;
    stubPanelComponent(refs, model, createPanelController("panelId", component));
    const element = refs.$compile(`<${tag} ${idAttribute}='panelId'></${tag}>`)(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    return {element, model};
  }

  it("marks the tab list, headers, links and labels next to the tab ids", () => {
    const {element} = compilePanel("awe-input-tab", "input-tab-id", "tab");

    expect(element.attr("criterion-id")).toBe("panelId");
    expect(element.find(`ul.nav-tabs${hook(TestIds.tabList)}`).length).toBe(1);
    const tabs = element.find(`li${hook(TestIds.tab)}`);
    expect(tabs.map((i, node) => node.id).get()).toEqual(["tab-1", "tab-2", "tab-3"]);
    expect(tabs.find(`a${hook(TestIds.tabLink)}`).length).toBe(3);
    expect(tabs.find(`span${hook(TestIds.tabLabel)}`).length).toBe(3);
  });

  it("exposes the active tab and follows the selection without the active class", () => {
    const {element, model} = compilePanel("awe-input-tab", "input-tab-id", "tab");
    const states = () => element.find(hook(TestIds.tab)).map((i, node) => node.getAttribute("data-active")).get();

    expect(states()).toEqual(["false", "true", "false"]);

    model.selected = "3";
    element.scope().$digest();

    expect(states()).toEqual(["false", "false", "true"]);
  });

  it("exposes whether the tab list is disabled", () => {
    const {element} = compilePanel("awe-input-tab", "input-tab-id", "tab");
    const scope = element.isolateScope() || element.scope();
    const list = element.find(hook(TestIds.tabList));
    scope.isDisabled = () => false;
    scope.$digest();

    expect(list.attr("data-disabled")).toBe("false");

    scope.isDisabled = () => true;
    scope.$digest();

    expect(list.attr("data-disabled")).toBe("true");
  });

  it("marks the wizard steps with their active and completed state", () => {
    const {element, model} = compilePanel("awe-input-wizard", "input-wizard-id", "wizard");
    const state = attribute => element.find(hook(TestIds.wizardStep)).map((i, node) => node.getAttribute(attribute)).get();

    expect(element.attr("criterion-id")).toBe("panelId");
    expect(state("data-active")).toEqual(["false", "true", "false"]);
    expect(state("data-completed")).toEqual(["true", "false", "false"]);

    model.selectedIndex = 2;
    element.scope().$digest();

    expect(state("data-active")).toEqual(["false", "false", "true"]);
    expect(state("data-completed")).toEqual(["true", "true", "false"]);
  });

  it("marks the wizard steps of the shared wizard template", () => {
    const steps = $("<div></div>").html(readTemplate("input/wizard.html")).find(`li[ng-repeat]${hook(TestIds.wizardStep)}`);

    expect(steps.length).toBe(1);
    expect(steps.attr("ng-attr-data-active")).toBeDefined();
    expect(steps.attr("ng-attr-data-completed")).toBeDefined();
  });

  it.each([
    ["tabcontainer.html", TestIds.tabPane],
    ["wizardPanel.html", TestIds.wizardPane]
  ])("%s marks the pane that carries the id", (file, testId) => {
    const pane = $("<div></div>").html(readTemplate(file)).find(hook(testId));

    expect(pane.length).toBe(1);
    expect(pane.attr("ng-attr-id")).toBe("{{::controller.id}}");
  });

  it("exposes the state of the panes", () => {
    const pane = (file, active) => {
      const scope = Object.assign(refs.$rootScope.$new(), {isActive: () => active, controller: {id: "paneId", style: ""}});
      const markup = readTemplate(file).replace(' ng-transclude', '');
      const element = refs.$compile($("<div></div>").html(markup))(scope);
      scope.$digest();
      return element.find("[id='paneId']");
    };

    expect(pane("tabcontainer.html", true).attr("data-active")).toBe("true");
    expect(pane("tabcontainer.html", false).attr("data-active")).toBe("false");
    expect(pane("wizardPanel.html", true).attr("data-active")).toBe("true");
  });
});

describe("button, context menu, menu and info test ids", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it.each([
    ["button", templateButton],
    ["column button", templateColumnButton]
  ])("marks the %s element that carries the id", (name, template) => {
    const button = $("<div></div>").html(template).find("button");

    expect(button.attr("data-testid")).toBe("button");
    expect(button.attr("id")).toBeDefined();
  });

  it("renders the generic button hook next to the button id", () => {
    const model = {page: 1, records: 1, selected: null, total: 1, values: [{label: "BUTTON_ACCEPT", value: null}]};
    stubPanelComponent(refs, model, createPanelController("ButAccept", "button", {buttonType: "button", label: "BUTTON_ACCEPT"}));

    const element = refs.$compile("<awe-button button-id='ButAccept'></awe-button>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    const button = element.find("button");
    expect(button.attr("id")).toBe("ButAccept");
    expect(button.attr("data-testid")).toBe("button");
  });

  describe("context menu", () => {
    it("marks the menu", () => {
      const root = $("<div></div>").html(readTemplate("contextMenu.html"));

      expect(root.find(`ul.context-menu${hook(TestIds.contextMenu)}`).length).toBe(1);
    });

    function compileOption(disabled) {
      const controller = {
        id: "ContextOption", component: "contextOption", contextMenu: [{id: "Child"}], dependencies: [], visible: true,
        label: "OPTION", actions: []
      };
      const leaf = {id: "Child", component: "contextOption", contextMenu: [], dependencies: [], visible: true, actions: []};
      stubPanelComponent(refs, {values: []}, controller);
      refs.$control.getAddressController.mockImplementation(address => address.component === "ContextOption" ? controller : leaf);
      jest.spyOn(refs.$storage, "get").mockImplementation(key => key === "actions-running" ? disabled : {base: {}});

      const element = refs.$compile("<awe-context-option option-id='ContextOption' option='{id: \"ContextOption\"}'/>")(refs.$rootScope.$new());
      refs.$rootScope.$digest();
      return element;
    }

    it("marks the option, its link and the submenu next to the option-id", () => {
      const option = compileOption(false);

      expect(option.attr("option-id")).toBe("ContextOption");
      expect(option.attr("data-testid")).toBe("context-menu-option");
      expect(option.find(`a${hook(TestIds.contextMenuLink)}[name='ContextOption']`).length).toBe(1);
      expect(option.find(`ul.context-submenu${hook(TestIds.contextSubmenu)}`).length).toBe(1);
    });

    it.each([[false, "false"], [true, "true"]])("exposes the disabled state of the link (%s)", (disabled, expected) => {
      const option = compileOption(disabled);

      expect(option.find(hook(TestIds.contextMenuLink)).attr("data-disabled")).toBe(expected);
    });
  });

  describe("menu", () => {
    const twoLevels = [
      {id: "parent", name: "parent", label: "Parent", visible: true, options: [{id: "child", name: "child", label: "Child", visible: true, actions: []}]},
      {id: "leaf", name: "leaf", label: "Leaf", visible: true, actions: []}
    ];

    function compileMenu(options = twoLevels) {
      const controller = {id: "menuId", style: "vertical", options};
      jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
      jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "getAddressModel").mockReturnValue({values: []});
      jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
      jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
      refs.$utilities.timeout.cancel = jest.fn();
      jest.spyOn(refs.$utilities, "publish").mockImplementation(jest.fn());
      const element = refs.$compile("<awe-menu menu-id='menuId'></awe-menu>")(refs.$rootScope.$new());
      refs.$rootScope.$digest();
      return element;
    }

    const option = (element, name) => element.find(`${hook(TestIds.menuOption)}:has(> a[name='${name}'])`);

    it("marks the menu, its options and their links next to the option name", () => {
      const element = compileMenu();

      expect(element.is(`ul.awe-menu${hook(TestIds.menu)}`) || element.find(`ul.awe-menu${hook(TestIds.menu)}`).length === 1).toBe(true);
      expect(element.find(hook(TestIds.menuOption)).length).toBe(3);
      expect(element.find(`a${hook(TestIds.menuLink)}`).map((i, node) => node.getAttribute("name")).get()).toEqual(["parent", "child", "leaf"]);
    });

    it("distinguishes the first level dropdown from the nested submenus", () => {
      const element = compileMenu();

      const dropdown = element.find(hook(TestIds.menuDropdown));
      expect(dropdown.length).toBe(1);
      expect(dropdown.hasClass("mm-dropdown-first")).toBe(true);
      expect(element.find(hook(TestIds.menuSubmenu)).length).toBe(0);
    });

    it("renders the nested submenu of a three level menu apart from the first level dropdown", () => {
      const element = compileMenu([
        {
          id: "top", name: "top", label: "Top", visible: true, options: [
            {id: "middle", name: "middle", label: "Middle", visible: true, options: [{id: "bottom", name: "bottom", label: "Bottom", visible: true, actions: []}]},
            {id: "sibling", name: "sibling", label: "Sibling", visible: true, actions: []}
          ]
        }
      ]);

      const dropdowns = element.find(hook(TestIds.menuDropdown));
      const submenus = element.find(hook(TestIds.menuSubmenu));
      expect(dropdowns.length).toBe(1);
      expect(dropdowns.prev("a").attr("name")).toBe("top");
      expect(dropdowns.hasClass("mm-dropdown-first")).toBe(true);
      expect(submenus.length).toBe(1);
      expect(submenus.prev("a").attr("name")).toBe("middle");
      expect(submenus.hasClass("mm-dropdown-target")).toBe(true);
      expect(submenus.closest(hook(TestIds.menuDropdown)).length).toBe(1);
      expect(submenus.find(`a${hook(TestIds.menuLink)}`).map((i, node) => node.getAttribute("name")).get()).toEqual(["bottom"]);
    });

    describe("state guards", () => {
      const optionScope = () => option(compileMenu(), "leaf").isolateScope();

      it("reports an option as not active and not open when the selected option is unset", () => {
        const scope = optionScope();
        scope.selectedOption = undefined;

        expect(scope.isActive()).toBe(false);
        expect(scope.isOpen()).toBe(false);
      });

      it("reports an option as not open when the selected option has no opened branches", () => {
        const scope = optionScope();
        scope.selectedOption = {name: "leaf"};

        expect(scope.isOpen()).toBe(false);
        expect(scope.isActive()).toBe(true);
      });

      it.each([["", ""], [undefined, undefined], ["", undefined]])("never reports an unnamed option as active (option %j, selected %j)", (optionName, selectedName) => {
        const scope = optionScope();
        scope.optionName = optionName;
        scope.selectedOption = {name: selectedName, opened: {}};

        expect(scope.isActive()).toBe(false);
      });
    });

    it("exposes the active option and the opened branches", () => {
      const element = compileMenu();
      const scope = option(element, "parent").scope();
      expect(option(element, "parent").attr("data-active")).toBe("false");
      expect(option(element, "parent").attr("data-open")).toBe("false");

      scope.selectedOption.name = "leaf";
      element.find("a[name='parent']").click();
      scope.$digest();

      expect(option(element, "leaf").attr("data-active")).toBe("true");
      expect(option(element, "parent").attr("data-active")).toBe("false");
      expect(option(element, "parent").attr("data-open")).toBe("true");
      expect(element.find(hook(TestIds.menuDropdown)).attr("data-open")).toBe("true");
    });
  });

  describe("info", () => {
    const infoController = extra => ({
      id: "InfoId", component: "info-dropdown", contextMenu: [], dependencies: [], visible: true, title: "TITLE", children: 0, ...extra
    });

    it("marks the info dropdown, its toggle and its menu next to the info-dropdown-id", () => {
      stubPanelComponent(refs, {values: []}, infoController({children: 2}));

      const element = refs.$compile("<awe-info-dropdown info-dropdown-id='InfoId'></awe-info-dropdown>")(refs.$rootScope.$new());
      refs.$rootScope.$digest();

      expect(element.attr("info-dropdown-id")).toBe("InfoId");
      expect(element.attr("data-testid")).toBe("info-dropdown");
      expect(element.find(`> a${hook(TestIds.infoDropdownToggle)}`).length).toBe(1);
      expect(element.find(`> ul${hook(TestIds.infoDropdownMenu)}`).length).toBe(1);
    });

    it("marks the info button and its link", () => {
      stubPanelComponent(refs, {values: []}, infoController({component: "info-button"}));

      const element = refs.$compile("<awe-info-button info-button-id='InfoId'></awe-info-button>")(refs.$rootScope.$new());
      refs.$rootScope.$digest();

      expect(element.attr("data-testid")).toBe("info-button");
      expect(element.find(`a.info-button${hook(TestIds.infoButtonLink)}`).length).toBe(1);
    });
  });
});

describe("dialog, confirm and message test ids", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it("marks the modal dialog and its close button next to the dialog-id", () => {
    stubPanelComponent(refs, {values: []}, createPanelController("Dlg", "dialog", {label: "DIALOG_TITLE"}));

    const element = refs.$compile("<awe-dialog dialog-id='Dlg'></awe-dialog>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    const dialog = element.find(hook(TestIds.dialog));
    expect(element.attr("dialog-id")).toBe("Dlg");
    expect(dialog.length).toBe(1);
    expect(dialog.hasClass("modal")).toBe(true);
    expect(dialog.attr("data-testid-owner")).toBe("Dlg");
    expect(dialog.find(`button.close${hook(TestIds.dialogClose)}`).length).toBe(1);
  });

  it("marks the confirm dialog, accept and cancel buttons next to their ids", () => {
    const root = $("<div></div>").html(readTemplate("confirm.html"));

    expect(root.find(`.modal${hook(TestIds.confirmDialog)}`).length).toBe(1);
    expect(root.find(`button#confirm-accept${hook(TestIds.confirmAccept)}`).length).toBe(1);
    expect(root.find(`button#confirm-cancel${hook(TestIds.confirmCancel)}`).length).toBe(1);
  });

  it("marks the alerts, their type, text and close button", () => {
    const zone = $("<div></div>").html(readIndex().match(/<div class="alert-zone"[\s\S]*?<awe-confirm/)[0].replace(/<awe-confirm$/, ""));
    const alert = zone.find("[uib-alert]");

    expect(alert.attr("data-testid")).toBe("alert");
    expect(alert.attr("ng-attr-data-type")).toBe("{{alert.type}}");
    expect(alert.attr("template-url")).toBe("awe/template/alert.html");
    expect(alert.find(hook(TestIds.alertTitle)).length).toBe(1);
    expect(alert.find(hook(TestIds.alertMessage)).length).toBe(1);
  });

  it("renders an alert with its type and a hooked close button through the uib-alert directive", () => {
    const zone = $("<div></div>").html(readIndex().match(/<div class="alert-zone"[\s\S]*?<awe-confirm/)[0]
      .replace(/<awe-confirm$/, "").replace(/ ng-controller="[^"]*"/, ""));
    const scope = refs.$rootScope.$new();
    scope.$message = {alerts: [{type: "danger", title: "TITLE", msg: "MESSAGE"}], closeAlert: jest.fn()};
    const element = refs.$compile(zone)(scope);
    scope.$digest();

    const alert = element.find(hook(TestIds.alert));
    expect(alert.length).toBe(1);
    expect(alert.attr("data-type")).toBe("danger");
    expect(alert.hasClass("alert-danger")).toBe(true);
    alert.find(`button.close${hook(TestIds.alertClose)}`).click();
    expect(scope.$message.closeAlert).toHaveBeenCalledWith(0);
  });
});

describe("loader test ids", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    inject(["ServerData", "$templateCache", (serverData, $templateCache) => {
      $templateCache.put("loader/spinner", "<div></div>");
      jest.spyOn(serverData, "preloadAngularTemplate").mockImplementation((template, fn) => fn && fn("<div></div>"));
    }]);
  });

  afterEach(cleanupHeavyComponentTest);

  const compileLoader = html => {
    const element = refs.$compile(html)(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    return element;
  };

  it("marks every loader with the generic hook by default", () => {
    const element = compileLoader("<awe-loader class='loader' icon-loader='spinner'></awe-loader>");

    expect(element.attr("data-testid")).toBe("loader");
    expect(element.hasClass("loader")).toBe(true);
  });

  it("keeps a more specific hook set by the owner of the loader", () => {
    const element = compileLoader("<awe-loader class='loader grid-loader' data-testid='grid-loader' icon-loader='spinner'></awe-loader>");

    expect(element.attr("data-testid")).toBe("grid-loader");
  });

  it("marks the loader of the pivot table like the grid loader", () => {
    expect($("<div></div>").html(readTemplate("pivotTable.html")).find(`awe-loader${hook(TestIds.gridLoader)}`).length).toBe(1);
  });
});
