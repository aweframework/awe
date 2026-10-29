import "./gridTestUtils";
import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/services/grid/cells.js (secondary line, status and chips of #832)', function () {
  let GridCommons;
  let $compile;
  let $rootScope;
  let $translate;
  const WRAP_TEMPLATE = "<div class=\"no-animate ui-grid-cell-contents cell-wrap {{col.grid.appScope.component.getCellStyle(COL_FIELD)}}\"" +
    " column-id=\"{{col.name}}\" data-testid=\"grid-cell\" ng-bind-html=\"col.grid.appScope.component.getCellValue(COL_FIELD)\"></div>";

  const getComponent = (columnModel) => ({
    constants: {ROW_IDENTIFIER: "id", CELL_VALUE: "value", CELL_LABEL: "label", CELL_TITLE: "title", CELL_STYLE: "style"},
    controller: {columnModel, headerModel: [], rowHeight: "30"},
    enableSorting: true,
    scope: {$on: () => null, charSize: 7, gridOptions: {}},
    listeners: {},
    api: {},
    model: {values: []},
    address: {view: "viewId", component: "componentId"},
    attributeMethods: {}
  });

  // Fix the first column of a grid with the given columns
  const fix = (columnModel) => {
    const component = getComponent(columnModel);
    new GridCommons(component).init();
    const column = component.controller.columnModel[0];
    component.fixColumn(column);
    return {component, column};
  };

  // Render the cell of a column for a row, as ui-grid does (COL_FIELD is the value of the cell in the row)
  const render = (columnModel, entity) => {
    const {component, column} = fix(columnModel);
    const template = column.cellTemplate
      .replace(/COL_FIELD/g, "row.entity[col.colDef.field]")
      .replace(/title="TOOLTIP"/g, "")
      .replace(/CUSTOM_FILTERS/g, "");
    const scope = $rootScope.$new();
    scope.row = {entity};
    scope.col = {name: column.name, colDef: column, grid: {appScope: {component}}};
    const element = $compile(template)(scope);
    scope.$digest();
    return {element: element[0], column, component};
  };

  beforeEach(function () {
    angular.mock.module('aweApplication');
    inject(["$injector", function ($injector) {
      GridCommons = $injector.get('GridCommons');
      $compile = $injector.get('$compile');
      $rootScope = $injector.get('$rootScope');
      $translate = $injector.get('$translate');
      $injector.get('$httpBackend').when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
  });

  describe("secondary line", () => {
    it("shows the secondary field under the main value", () => {
      const {element} = render([{id: "name", secondaryField: "email"}], {name: "Ann", email: "ann@almis.com"});

      const lines = element.querySelector(".awe-cell-lines");
      expect(lines.tagName).toBe("DIV");
      expect(lines.children.length).toBe(2);
      expect(lines.children[0].tagName).toBe("SPAN");
      expect(lines.children[0].classList.contains("awe-cell-main")).toBe(true);
      expect(lines.children[0].textContent).toBe("Ann");
      expect(lines.children[1].className).toContain("awe-cell-secondary");
      expect(lines.children[1].tagName).toBe("SPAN");
      expect(lines.children[1].textContent).toBe("ann@almis.com");
    });

    it("shows only the main line when the secondary value is empty or missing", () => {
      expect(render([{id: "name", secondaryField: "email"}], {name: "Ann", email: ""}).element.querySelector(".awe-cell-secondary")).toBeNull();
      const {element} = render([{id: "name", secondaryField: "email"}], {name: "Ann"});
      expect(element.querySelector(".awe-cell-secondary")).toBeNull();
      expect(element.querySelector(".awe-cell-main").textContent).toBe("Ann");
    });

    it("reads the label of a secondary cell given as an object", () => {
      const {element} = render([{id: "name", secondaryField: "email"}], {name: {value: "1", label: "Ann"}, email: {value: "x", label: "ann@almis.com"}});

      expect(element.querySelector(".awe-cell-main").textContent).toBe("Ann");
      expect(element.querySelector(".awe-cell-secondary").textContent).toBe("ann@almis.com");
    });

    it("lets the rows of the grid grow to fit the two lines", () => {
      const {component, column} = fix([{id: "name", secondaryField: "email"}]);

      expect(component.wrapsText).toBe(true);
      expect(column.cellTemplate).not.toContain("cell-wrap");
    });

    it("combines with a wrapping column with a maximum of lines", () => {
      const {column} = fix([{id: "name", secondaryField: "email", maxLines: 2}]);

      expect(column.cellTemplate).toContain("cell-wrap cell-clamp");
      expect(column.cellTemplate).toContain("--cell-max-lines: 2");
      expect(column.cellTemplate).toContain("awe-cell-lines");
    });
  });

  describe("status", () => {
    it.each(["success", "warning", "danger", "info", "neutral"])("shows the main value as a %s status", (tone) => {
      const {element} = render([{id: "state", statusField: "tone"}], {state: "Active", tone});

      const status = element.querySelector(".awe-cell-status");
      expect(status.tagName).toBe("SPAN");
      expect(status.classList.contains("awe-cell-status-" + tone)).toBe(true);
      expect(status.getAttribute("data-tone")).toBe(tone);
      expect(status.textContent).toBe("Active");
      expect(status.getAttribute("title")).toBe("Active");
    });

    it.each([["SUCCESS ", "success"], [" Danger", "danger"]])("normalises the tone %p to %s", (tone, expected) => {
      const {element} = render([{id: "state", statusField: "tone"}], {state: "Active", tone});

      expect(element.querySelector(".awe-cell-status").getAttribute("data-tone")).toBe(expected);
    });

    it.each(["unknown", "", null, undefined])("falls back to neutral with the tone %p", (tone) => {
      const {element} = render([{id: "state", statusField: "tone"}], {state: "Active", tone});

      const status = element.querySelector(".awe-cell-status");
      expect(status.className).toContain("awe-cell-status-neutral");
      expect(status.getAttribute("data-tone")).toBe("neutral");
    });

    it("reads the value of a tone given as a cell object", () => {
      const {element} = render([{id: "state", statusField: "tone"}], {state: "Active", tone: {value: "danger", label: "Danger"}});

      expect(element.querySelector(".awe-cell-status").getAttribute("data-tone")).toBe("danger");
    });

    it("combines with a secondary line", () => {
      const {element} = render([{id: "state", statusField: "tone", secondaryField: "since"}], {state: "Active", tone: "success", since: "2026"});

      const main = element.querySelector(".awe-cell-lines > .awe-cell-main");
      expect(main.querySelector(".awe-cell-status.awe-cell-status-success").textContent).toBe("Active");
      expect(element.querySelector(".awe-cell-lines > .awe-cell-secondary").textContent).toBe("2026");
    });
  });

  describe("chips", () => {
    const chipsOf = (element) => Array.from(element.querySelectorAll(".awe-cell-chips > .awe-cell-chip")).map(chip => chip.textContent);

    it("splits the main value by commas, trimming and dropping the empty items", () => {
      const {element} = render([{id: "tags", chips: true}], {tags: " red, green ,, blue , "});

      expect(element.querySelector(".awe-cell-chips").tagName).toBe("SPAN");
      expect(chipsOf(element)).toEqual(["red", "green", "blue"]);
      expect(element.querySelector(".awe-cell-chip-more")).toBeNull();
    });

    it("splits by the separator of the column", () => {
      const {element} = render([{id: "tags", chips: true, chipSeparator: "|"}], {tags: "a, b|c"});

      expect(chipsOf(element)).toEqual(["a, b", "c"]);
    });

    it("shows no chips for an empty value", () => {
      const {element} = render([{id: "tags", chips: true}], {tags: ""});

      expect(chipsOf(element)).toEqual([]);
    });

    it("collapses the chips over the maximum into a +n chip that lists the hidden ones", () => {
      const {element} = render([{id: "tags", chips: true, maxChips: 2}], {tags: "a,b,c,d"});

      expect(chipsOf(element)).toEqual(["a", "b", "+2"]);
      const more = element.querySelector(".awe-cell-chip.awe-cell-chip-more");
      expect(more.getAttribute("title")).toBe("c, d");
      expect(more.getAttribute("aria-label")).toBe("2 more: c, d");
    });

    it("shows no +n chip when the items fit in the maximum", () => {
      const {element} = render([{id: "tags", chips: true, maxChips: "3"}], {tags: "a,b,c"});

      expect(chipsOf(element)).toEqual(["a", "b", "c"]);
    });

    it("ignores the status of a chips column", () => {
      const {element} = render([{id: "tags", chips: true, statusField: "tone"}], {tags: "a,b", tone: "danger"});

      expect(element.querySelector(".awe-cell-status")).toBeNull();
      expect(chipsOf(element)).toEqual(["a", "b"]);
    });

    it("lets the rows of the grid grow to fit the chips", () => {
      expect(fix([{id: "tags", chips: true}]).component.wrapsText).toBe(true);
    });
  });

  describe("avatar (#793)", () => {
    const avatarOf = (element) => element.querySelector(".awe-cell-entity > .awe-cell-avatar");

    it("shows the initials of the avatar field before the main value, with the tone of the row", () => {
      const {element} = render([{id: "name", avatarField: "ini", avatarToneField: "ton"}], {name: "Carmen Fernández", ini: "Carmen Fernández", ton: "info"});

      const entity = element.querySelector(".awe-cell-entity");
      expect(entity.tagName).toBe("DIV");
      expect(entity.children.length).toBe(2);
      const avatar = avatarOf(element);
      expect(entity.children[0]).toBe(avatar);
      expect(avatar.tagName).toBe("SPAN");
      expect(avatar.textContent).toBe("CF");
      expect(avatar.classList.contains("awe-cell-avatar-info")).toBe(true);
      expect(avatar.getAttribute("data-tone")).toBe("info");
      expect(avatar.getAttribute("aria-hidden")).toBe("true");
      expect(entity.children[1].className).toContain("awe-cell-entity-text");
      expect(entity.children[1].querySelector(".awe-cell-main").textContent).toBe("Carmen Fernández");
    });

    it.each([
      ["two words", "Carmen Fernández", "CF"],
      ["one word", "admin", "A"],
      ["particles", "Pat de la Vega", "PV"],
      ["leading words that do not start with a letter", "123 ann (test) lee", "AL"],
      ["words that do not start with a letter at the end", "Manager (test)", "M"],
      ["lower case and accents", "álvaro ruiz", "ÁR"],
      ["initials", "jg", "J"],
      ["spaces around", "  Diego   Moreno  ", "DM"]
    ])("builds the initials of %s", (_name, text, initials) => {
      const {element} = render([{id: "name", avatarField: "ini"}], {name: "x", ini: text});

      expect(avatarOf(element).textContent).toBe(initials);
    });

    it("reads the label of an avatar cell given as an object", () => {
      const {element} = render([{id: "name", avatarField: "ini"}], {name: "x", ini: {value: "1", label: "Diego Moreno"}});

      expect(avatarOf(element).textContent).toBe("DM");
    });

    it.each(["primary", "info", "success", "warning", "danger", "neutral"])("shows the %s tone", (tone) => {
      const {element} = render([{id: "name", avatarField: "ini", avatarToneField: "ton"}], {name: "Ann", ini: "Ann", ton: tone});

      expect(avatarOf(element).className).toBe("awe-cell-avatar awe-cell-avatar-" + tone);
    });

    it.each(["purple", "", null, undefined, " Danger "])("normalises or falls back to neutral with the tone %p", (tone) => {
      const {element} = render([{id: "name", avatarField: "ini", avatarToneField: "ton"}], {name: "Ann", ini: "Ann", ton: tone});

      expect(avatarOf(element).getAttribute("data-tone")).toBe(tone === " Danger " ? "danger" : "neutral");
    });

    it("is neutral without a tone field", () => {
      const {element} = render([{id: "name", avatarField: "ini"}], {name: "Ann", ini: "Ann"});

      expect(avatarOf(element).getAttribute("data-tone")).toBe("neutral");
    });

    it.each([["empty", ""], ["missing", undefined], ["without letters", "(1)"]])("shows no avatar, only an aligned space, when the value is %s", (_name, ini) => {
      const {element} = render([{id: "name", avatarField: "ini"}], {name: "Ann", ini});

      expect(element.querySelector(".awe-cell-avatar")).toBeNull();
      const spacer = element.querySelector(".awe-cell-entity > .awe-cell-avatar-spacer");
      expect(spacer.textContent).toBe("");
      expect(spacer.getAttribute("aria-hidden")).toBe("true");
      expect(element.querySelector(".awe-cell-entity-text .awe-cell-main").textContent).toBe("Ann");
    });

    it("combines with a secondary line and a status", () => {
      const {element} = render([{id: "name", avatarField: "ini", avatarToneField: "ton", secondaryField: "login", statusField: "state"}],
        {name: "Ann Lee", ini: "Ann Lee", ton: "warning", login: "alee", state: "success"});

      expect(avatarOf(element).textContent).toBe("AL");
      const text = element.querySelector(".awe-cell-entity > .awe-cell-entity-text");
      expect(text.querySelector(".awe-cell-lines > .awe-cell-main > .awe-cell-status.awe-cell-status-success").textContent).toBe("Ann Lee");
      expect(text.querySelector(".awe-cell-lines > .awe-cell-secondary").textContent).toBe("alee");
    });

    it("lets the rows of the grid grow to fit the avatar", () => {
      const {component, column} = fix([{id: "name", avatarField: "ini"}]);

      expect(component.wrapsText).toBe(true);
      expect(column.cellTemplate).toContain("cell-rich");
    });

    it("never renders the HTML of the avatar or the main value", () => {
      const {element} = render([{id: "name", avatarField: "ini"}], {name: "<b>x</b>", ini: "<i>y</i> z"});

      expect(element.querySelector("b, i")).toBeNull();
      expect(element.querySelector(".awe-cell-main").textContent).toBe("<b>x</b>");
      expect(avatarOf(element).textContent).toBe("Z");
    });

    it("keeps sorting and filtering by the main value", () => {
      const {column} = fix([{id: "name", sortable: true, avatarField: "ini"}]);

      expect(column.field).toBe("name");
      expect(column.sortField).toBe("name");
    });

    it("is ignored in a column with a component", () => {
      const {column, component} = fix([{id: "name", component: "text", avatarField: "ini", avatarToneField: "ton"}]);

      expect(column.cellTemplate).toContain("<awe-column-text");
      expect(column.cellTemplate).not.toContain("awe-cell-");
      expect(component.wrapsText).toBe(false);
    });

    it("ignores a tone field without an avatar field", () => {
      expect(fix([{id: "name", avatarToneField: "ton"}]).column.cellTemplate).toBe("grid/cell");
    });
  });

  describe("icon (#793)", () => {
    const iconOf = (element) => element.querySelector(".awe-cell-icon");

    it("shows the icon of the row inside the status, in place of its dot", () => {
      const {element} = render([{id: "twoFa", statusField: "ton", iconField: "icn"}], {twoFa: "On", ton: "neutral", icn: "lucide:shield-check"});

      const status = element.querySelector(".awe-cell-status");
      expect(status.className).toContain("awe-cell-status-neutral");
      expect(status.classList.contains("awe-cell-status-plain")).toBe(true);
      expect(status.getAttribute("title")).toBe("On");
      expect(status.textContent).toBe("On");
      expect(status.firstElementChild).toBe(iconOf(element));
      expect(iconOf(element).getAttribute("aria-hidden")).toBe("true");
      expect(iconOf(element).querySelector("svg.awe-icon use").getAttribute("href")).toBe("icons/lucide-sprite.svg#shield-check");
    });

    it.each([["empty", ""], ["missing", undefined], ["null", null]])("shows a plain status without icon when the icon of the row is %s", (_name, icn) => {
      const {element} = render([{id: "twoFa", statusField: "ton", iconField: "icn"}], {twoFa: "Off", ton: "neutral", icn});

      const status = element.querySelector(".awe-cell-status");
      expect(status.classList.contains("awe-cell-status-plain")).toBe(true);
      expect(status.textContent).toBe("Off");
      expect(iconOf(element)).toBeNull();
    });

    it("shows the icon before the text of a column without status", () => {
      const {element} = render([{id: "twoFa", iconField: "icn"}], {twoFa: "On", icn: "lucide:shield-check"});

      const text = element.querySelector(".awe-cell-icon-text");
      expect(text.getAttribute("title")).toBe("On");
      expect(text.textContent).toBe("On");
      expect(text.firstElementChild).toBe(iconOf(element));
    });

    it("reads the value of an icon cell given as an object and resolves the icon names of the screens", () => {
      const {element} = render([{id: "twoFa", iconField: "icn"}], {twoFa: "On", icn: {value: "fa-check", label: "x"}});

      expect(iconOf(element).querySelector("use").getAttribute("href")).toBe("icons/lucide-sprite.svg#check");
    });

    it("combines with a secondary line", () => {
      const {element} = render([{id: "twoFa", statusField: "ton", iconField: "icn", secondaryField: "since"}],
        {twoFa: "On", ton: "success", icn: "lucide:shield-check", since: "2026"});

      expect(element.querySelector(".awe-cell-lines > .awe-cell-main > .awe-cell-status-plain .awe-cell-icon")).not.toBeNull();
      expect(element.querySelector(".awe-cell-lines > .awe-cell-secondary").textContent).toBe("2026");
    });

    it("never renders the HTML of the value", () => {
      const {element} = render([{id: "twoFa", statusField: "ton", iconField: "icn"}], {twoFa: "<b>x</b>", icn: "lucide:shield-check"});

      expect(element.querySelector("b")).toBeNull();
      expect(element.querySelector(".awe-cell-status").textContent.trim()).toBe("<b>x</b>");
    });

    it("keeps a single line and sorting and filtering by the main value", () => {
      const {column, component} = fix([{id: "twoFa", sortable: true, statusField: "ton", iconField: "icn"}]);

      expect(column.cellTemplate).not.toContain("cell-rich");
      expect(component.wrapsText).toBe(false);
      expect(column.field).toBe("twoFa");
      expect(column.sortField).toBe("twoFa");
    });

    it("is ignored in a column with a component and in a chips column", () => {
      expect(fix([{id: "twoFa", component: "text", iconField: "icn"}]).column.cellTemplate).not.toContain("awe-cell-");
      expect(fix([{id: "tags", chips: true, iconField: "icn"}]).column.cellTemplate).not.toContain("awe-cell-icon");
    });

    it("keeps the status with its dot in a column without icon field", () => {
      const {element} = render([{id: "state", statusField: "tone"}], {state: "Active", tone: "success", icn: "lucide:shield-check"});

      const status = element.querySelector(".awe-cell-status");
      expect(status.classList.contains("awe-cell-status-plain")).toBe(false);
      expect(status.children.length).toBe(0);
      expect(status.textContent).toBe("Active");
    });
  });

  describe("values as text", () => {
    it("never renders the HTML of the values", () => {
      const {element} = render([{id: "name", secondaryField: "email", statusField: "tone"}], {name: "<b>x</b>", email: "<i>y</i>", tone: "info"});

      expect(element.querySelector("b")).toBeNull();
      expect(element.querySelector("i")).toBeNull();
      expect(element.querySelector(".awe-cell-status").textContent).toBe("<b>x</b>");
      expect(element.querySelector(".awe-cell-status").getAttribute("title")).toBe("<b>x</b>");
      expect(element.querySelector(".awe-cell-secondary").textContent).toBe("<i>y</i>");
    });

    it("never renders the HTML of the chips", () => {
      const {element} = render([{id: "tags", chips: true}], {tags: "<b>x</b>,y"});

      expect(element.querySelector("b")).toBeNull();
      expect(element.querySelector(".awe-cell-chip").textContent).toBe("<b>x</b>");
    });
  });

  describe("sorting and filtering", () => {
    it("keeps sorting and filtering by the main value", () => {
      const {column} = fix([{id: "name", sortable: true, secondaryField: "email", statusField: "tone"}]);

      expect(column.field).toBe("name");
      expect(column.sortField).toBe("name");
      expect(column.enableSorting).toBe(true);
      expect(column.enableFiltering).not.toBe(false);
    });
  });

  describe("unchanged columns", () => {
    it("ignores the three features in a column with a component", () => {
      const {column, component} = fix([{id: "name", component: "text", secondaryField: "email", statusField: "tone", chips: true}]);

      expect(column.cellTemplate).toContain("<awe-column-text");
      expect(column.cellTemplate).not.toContain("awe-cell-");
      expect(component.wrapsText).toBe(false);
    });

    it("keeps the default cell of a column without the attributes", () => {
      const {column, component} = fix([{id: "name"}]);

      expect(column.cellTemplate).toBe("grid/cell");
      expect(component.wrapsText).toBe(false);
    });

    it("keeps the wrapping cell of #825 for a wrapping column without the attributes", () => {
      expect(fix([{id: "name", wrap: true}]).column.cellTemplate).toBe(WRAP_TEMPLATE);
    });
  });

  describe("export column information", () => {
    const visibleColumns = (columns) => {
      const component = getComponent([]);
      new GridCommons(component).init();
      component.addColumns(columns.map(column => ({charlength: 20, printable: true, hidden: false, ...column})));
      return component.getVisibleColumns();
    };

    it("sends the secondary field, its export flag and its translated label", () => {
      jest.spyOn($translate, "instant").mockImplementation(key => "t:" + key);
      const [column] = visibleColumns([{id: "name", label: "PARAMETER_NAME", secondaryField: "email", exportSecondary: true, secondaryLabel: "PARAMETER_EMAIL"}]);

      expect(column.secondaryField).toBe("email");
      expect(column.exportSecondary).toBe(true);
      expect(column.secondaryLabel).toBe("t:PARAMETER_EMAIL");
      expect(column.label).toBe("t:PARAMETER_NAME");
    });

    it("sends a null secondary label when the column declares none", () => {
      const [column] = visibleColumns([{id: "name", secondaryField: "email"}]);

      expect(column.secondaryField).toBe("email");
      expect(column.exportSecondary).toBe(false);
      expect(column.secondaryLabel).toBeNull();
    });

    it("sends no secondary information for a column without a secondary field", () => {
      const [column] = visibleColumns([{id: "name", label: "PARAMETER_NAME"}]);

      expect(column).not.toHaveProperty("secondaryField");
      expect(column).not.toHaveProperty("exportSecondary");
      expect(column).not.toHaveProperty("secondaryLabel");
    });
  });
});
