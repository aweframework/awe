import "../components/lightComponentTestUtils";

import fs from "fs";
import path from "path";
import {getIcon, ICON_SPRITE, iconMarkup, iconTemplate} from "../../../main/resources/js/awe/services/icon";
import {getIconTemplate} from "../../../main/resources/js/awe/services/component";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {textInputTemplate} from "../../../main/resources/js/awe/services/text";
import {uploaderColumnTemplate} from "../../../main/resources/js/awe/services/uploader";

const RESOURCES = path.join(__dirname, "..", "..", "..", "main", "resources");
const ANGULAR_TEMPLATES = path.join(RESOURCES, "templates", "angular");
const readTemplate = name => fs.readFileSync(path.join(ANGULAR_TEMPLATES, name), "utf8");

const listFiles = (folder, extension) => fs.readdirSync(folder, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(folder, entry.name);
  if (entry.isDirectory()) {
    return listFiles(file, extension);
  }
  return entry.name.endsWith(extension) ? [file] : [];
});

const lucide = (name, classes = "") => ({family: "lucide", name, classes, href: `${ICON_SPRITE}#${name}`});
const svgOf = (element, index = 0) => element.find("svg").eq(index);
const spriteOf = svg => svg.find("use").attr("href");
// Classes of an svg, without the ones AngularJS adds (ng-scope...)
const classesOf = svg => svg.attr("class").split(/\s+/).filter(name => name && !name.startsWith("ng-")).join(" ");

describe("awe-framework/awe-client-angular/src/test/jest/services/icon", () => {
  let $compile, $rootScope, $filter, $utilities;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$compile", "$rootScope", "$filter", "$httpBackend", "AweUtilities",
      (_$compile_, _$rootScope_, _$filter_, $httpBackend, _AweUtilities_) => {
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $filter = _$filter_;
      $utilities = _AweUtilities_;
    }]);
  });

  const render = (template, values = {}) => {
    const scope = Object.assign($rootScope.$new(), values);
    const element = $compile(angular.element("<div></div>").html(template))(scope);
    scope.$digest();
    return {element, scope};
  };

  describe("the icon of the screen XML or the data", () => {
    it("is the Lucide equivalent of a Font Awesome name, with or without prefix", () => {
      expect(getIcon("plus")).toEqual(lucide("plus"));
      expect(getIcon("fa-plus")).toEqual(lucide("plus"));
      expect(getIcon("fa:question-circle")).toEqual(lucide("circle-question-mark"));
    });

    it("is the Lucide equivalent of a Google Material Icons name", () => {
      expect(getIcon("mdi:dark_mode")).toEqual(lucide("moon"));
    });

    it("is any icon of the Lucide set with the lucide: family", () => {
      expect(getIcon("lucide:house")).toEqual(lucide("house"));
      expect(getIcon("lucide:trash-2")).toEqual(lucide("trash"));
    });

    it("keeps the extra classes and turns the Font Awesome modifiers into the AWE icon ones", () => {
      expect(getIcon("level-up icon-rotate-90")).toEqual(lucide("corner-right-up", "icon-rotate-90"));
      expect(getIcon("fa-refresh fa-spin")).toEqual(lucide("refresh-cw", "awe-icon-spin"));
      expect(getIcon("spinner fa-pulse fa-fw")).toEqual(lucide("loader-circle", "awe-icon-spin"));
      expect(getIcon("search fa-lg text-success")).toEqual(lucide("search", "awe-icon-lg text-success"));
    });

    it("keeps its current font for a name without Lucide equivalence", () => {
      expect(getIcon("rebel")).toEqual({family: "fa", name: "rebel"});
      expect(getIcon("youtube-play fa-fw")).toEqual({family: "fa", name: "youtube-play fa-fw"});
      expect(getIcon("GENERAL_STATUS_1")).toEqual({family: "fa", name: "GENERAL_STATUS_1"});
      expect(getIcon("mdi:not_an_icon")).toEqual({family: "mdi", name: "not_an_icon"});
    });

    it("is no icon without value or for an unknown Lucide name", () => {
      const warn = jest.spyOn(console, "warn").mockImplementation(() => null);
      expect(getIcon("lucide:not-an-icon")).toEqual({family: null, name: null});
      expect(getIcon(undefined)).toEqual({family: null, name: null});
      expect(getIcon("")).toEqual({family: null, name: null});
      warn.mockRestore();
    });

    it("is what the utilities and the aweIcon filter give to the templates", () => {
      expect($utilities.extractIcon("mdi:dark_mode")).toEqual(lucide("moon"));
      expect($utilities.extractIcon("rebel")).toEqual({family: "fa", name: "rebel"});
      expect($filter("aweIcon")("fa-plus icon-expand")).toEqual(lucide("plus", "icon-expand"));
    });
  });

  describe("the markup of an icon", () => {
    it("is an svg of the sprite, decorative, for a Lucide icon", () => {
      expect(iconMarkup("calendar")).toBe(`<svg class="awe-icon" aria-hidden="true" focusable="false"><use href="${ICON_SPRITE}#calendar"></use></svg>`);
      expect(iconMarkup("refresh fa-spin", 'ng-if="saving"'))
        .toBe(`<svg class="awe-icon awe-icon-spin" aria-hidden="true" focusable="false" ng-if="saving"><use href="${ICON_SPRITE}#refresh-cw"></use></svg>`);
    });

    it("keeps the element that holds the icon, with its classes and attributes, around the svg", () => {
      expect(iconTemplate("question-circle", {classes: "help-target", attributes: 'ng-if="::controller.help"'}))
        .toBe(`<i class="help-target" ng-if="::controller.help">${iconMarkup("question-circle")}</i>`);
      expect(iconTemplate("calendar")).toBe(`<i>${iconMarkup("calendar")}</i>`);
      expect(iconTemplate("bars", {tag: "span", classes: "grid-column-handle"}))
        .toBe(`<span class="grid-column-handle">${iconMarkup("bars")}</span>`);
    });

    it("is the current markup of its font for an icon without Lucide equivalence", () => {
      expect(iconTemplate("rebel", {classes: "help-target"})).toBe('<i class="help-target fa fa-rebel"></i>');
      expect(iconMarkup("rebel")).toBe('<i class="fa fa-rebel"></i>');
    });

    it("takes the sprite from the context path of the application (the base of the page)", () => {
      const base = document.createElement("base");
      base.setAttribute("href", "http://localhost/my-app/");
      document.head.appendChild(base);
      try {
        const {element} = render(iconTemplate("calendar"));
        const href = spriteOf(svgOf(element));
        expect(href).toBe("icons/lucide-sprite.svg#calendar");
        expect(new URL(href, document.baseURI).href).toBe("http://localhost/my-app/icons/lucide-sprite.svg#calendar");
      } finally {
        document.head.removeChild(base);
      }
    });
  });

  describe("the icon of a component (getIconTemplate)", () => {
    const renderIcon = (icon, style = "button-icon", tag = "i") =>
      render(getIconTemplate(style, tag), {getIcon: () => $utilities.extractIcon(icon)}).element;

    it("renders a Lucide svg inside the element of the icon, which keeps the classes of the component", () => {
      const element = renderIcon("search", "form-icon criterion-icon-lg");
      const holder = element.children();
      expect(holder.length).toBe(1);
      expect(holder.is("i.form-icon.criterion-icon-lg")).toBe(true);
      expect(holder.hasClass("fa")).toBe(false);
      const svg = svgOf(holder);
      expect(svg.attr("class")).toBe("awe-icon");
      expect(svg.attr("aria-hidden")).toBe("true");
      expect(svg.attr("focusable")).toBe("false");
      expect(spriteOf(svg)).toBe(`${ICON_SPRITE}#search`);
    });

    it("renders the lucide: family and the Material names with Lucide equivalence", () => {
      expect(spriteOf(svgOf(renderIcon("lucide:house")))).toBe(`${ICON_SPRITE}#house`);
      expect(spriteOf(svgOf(renderIcon("mdi:dark_mode")))).toBe(`${ICON_SPRITE}#moon`);
    });

    it("puts the extra classes of the icon on the svg", () => {
      const svg = svgOf(renderIcon("level-up icon-rotate-90"));
      expect(svg.attr("class")).toBe("awe-icon icon-rotate-90");
      expect(spriteOf(svg)).toBe(`${ICON_SPRITE}#corner-right-up`);
    });

    it("keeps the current markup for the icons without Lucide equivalence", () => {
      const fontAwesome = renderIcon("rebel");
      expect(fontAwesome.find("svg").length).toBe(0);
      expect(fontAwesome.children().is("i.button-icon.fa.fa-rebel")).toBe(true);

      const material = renderIcon("mdi:not_an_icon", "nav-icon", "span");
      expect(material.find("svg").length).toBe(0);
      expect(material.children().is("span.nav-icon.material-icons")).toBe(true);
      expect(material.children().text()).toBe("not_an_icon");
    });

    it("renders nothing without icon", () => {
      expect(renderIcon(undefined).children().length).toBe(0);
    });
  });

  describe("the icon column of a grid (icon of the data)", () => {
    const renderColumn = value => render(readTemplate("column/icon.html"), {component: {model: {values: [value]}}});
    const hook = element => element.find("[data-testid='column-icon']");

    it("shows the Lucide icon of the value and keeps the hooks of the browser tests", () => {
      const {element} = renderColumn({icon: "fa-plus", style: "text-success", label: "Added"});
      const icon = hook(element);
      expect(icon.is("span.text-success")).toBe(true);
      expect(icon.attr("data-icon")).toBe("fa-plus");
      expect(icon.hasClass("fa")).toBe(false);
      expect(icon.hasClass("fa-plus")).toBe(false);
      expect(spriteOf(svgOf(icon))).toBe(`${ICON_SPRITE}#plus`);
    });

    it("keeps the font of a value without Lucide equivalence", () => {
      const {element} = renderColumn({icon: "GENERAL_STATUS_1", style: "", label: "Ok"});
      const icon = hook(element);
      expect(icon.is("span.fa.GENERAL_STATUS_1")).toBe(true);
      expect(icon.attr("data-icon")).toBe("GENERAL_STATUS_1");
      expect(icon.find("svg").length).toBe(0);
    });

    it("follows the value of the row", () => {
      const {element, scope} = renderColumn({icon: "fa-plus", style: "", label: ""});
      scope.component.model.values[0] = {icon: "fa-trash", style: "text-danger", label: ""};
      scope.$digest();
      const icon = hook(element);
      expect(icon.attr("data-icon")).toBe("fa-trash");
      expect(icon.find("svg").length).toBe(1);
      expect(spriteOf(svgOf(icon))).toBe(`${ICON_SPRITE}#trash`);
    });
  });

  describe("the icons of the tree grid and the loader (icon values with the fa- prefix)", () => {
    it("shows the expand, collapse and loading icons of a tree row", () => {
      const {element, scope} = render(readTemplate("grid/cellTreeIcons.html"),
        {row: {entity: {$$treeIcon: "fa-plus-square icon-expand"}, treeLevel: 0}, col: {name: "Tree"}, grid: {options: {treeIndent: 10}}});
      const icon = element.find("[data-testid='tree-icon']");
      expect(icon.hasClass("tree-icon")).toBe(true);
      expect(icon.hasClass("fa")).toBe(false);
      expect(classesOf(svgOf(icon))).toBe("awe-icon icon-expand");
      expect(spriteOf(svgOf(icon))).toBe(`${ICON_SPRITE}#square-plus`);

      scope.row.entity.$$treeIcon = "fa-refresh fa-spin";
      scope.$digest();
      expect(classesOf(svgOf(icon))).toBe("awe-icon awe-icon-spin");
      expect(spriteOf(svgOf(icon))).toBe(`${ICON_SPRITE}#refresh-cw`);
    });

    it("shows the icon of the icon loader", () => {
      const {element} = render(readTemplate("loader/icon.html"), {loaderIcon: "fa-spinner fa-spin"});
      expect(classesOf(svgOf(element))).toBe("awe-icon awe-icon-spin");
      expect(spriteOf(svgOf(element))).toBe(`${ICON_SPRITE}#loader-circle`);
    });
  });

  describe("the icons written in the AngularJS client", () => {
    // Font Awesome markup that stays: the fallback branches of the icon helpers and of the templates of the icons of
    // the data, the resize handles (a styled empty element, not an icon), the icons of the plugins (the time picker
    // writes the class names in its own markup, the markdown editor takes the name of the font) and the icon class of
    // the text views, which their templates do not use
    const ALLOWED = {
      "js/awe/services/component.js": 2,
      "js/awe/services/icon.js": 2,
      "js/awe/services/text.js": 1,
      "js/awe/directives/plugins/uiTime.js": 2,
      "js/awe/directives/input/markdownEditor.js": 1,
      "js/awe/directives/resizable.js": 1,
      "templates/angular/resizable.html": 1,
      "templates/angular/column/icon.html": 1,
      "templates/angular/grid/cellTreeIcons.html": 1,
      "templates/angular/grid/headerTreeIcons.html": 1,
      "templates/angular/loader/icon.html": 1
    };
    // Stale templates that no directive loads: the grid (the grid directive has its own template) and the dialog column
    const UNUSED = ["templates/angular/grid.html", "templates/angular/column/dialog.html"];

    it("go through the icon helpers", () => {
      const files = [...listFiles(path.join(RESOURCES, "js", "awe"), ".js"), ...listFiles(ANGULAR_TEMPLATES, ".html")]
        .map(file => path.relative(RESOURCES, file).split(path.sep).join("/"))
        .filter(file => !UNUSED.includes(file));
      const fontMarkup = files.map(file => {
        const source = fs.readFileSync(path.join(RESOURCES, file), "utf8");
        return [file, (source.match(/["' ]fa["' ]|["']fa fa-/g) || []).length];
      }).filter(([file, count]) => count > (ALLOWED[file] || 0));
      expect(fontMarkup).toEqual([]);
    });

    it("render the Lucide icons of the sprite", () => {
      const {element} = render(readTemplate("help.html"), {});
      expect(spriteOf(svgOf(element))).toBe(`${ICON_SPRITE}#info`);

      const text = angular.element("<div></div>").html(textInputTemplate);
      expect(text.find(`i.help-target > svg.awe-icon > use[href='${ICON_SPRITE}#circle-question-mark']`).length).toBe(1);
    });

    it("switch between the icons of a state, the spinning one included", () => {
      const {element, scope} = render(uploaderColumnTemplate, {component: {deleting: false, controller: {}, model: {values: []}}});
      const clear = () => element.find("[data-testid='upload-clear'] svg");
      expect(clear().length).toBe(1);
      expect(spriteOf(clear())).toBe(`${ICON_SPRITE}#x`);

      scope.component.deleting = true;
      scope.$digest();
      expect(clear().length).toBe(1);
      expect(classesOf(clear())).toBe("awe-icon awe-icon-spin");
      expect(spriteOf(clear())).toBe(`${ICON_SPRITE}#refresh-cw`);
    });
  });
});
