import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import fs from "fs";
import path from "path";
import {templateButton, templateColumnButton} from "../../../main/resources/js/awe/services/button";
import {textInputTemplate, textViewInputTemplate, passwordInputTemplate, textareaInputTemplate} from "../../../main/resources/js/awe/services/text";
import {templateNumeric} from "../../../main/resources/js/awe/services/numeric";
import {calendarInputTemplate, timeInputTemplate} from "../../../main/resources/js/awe/services/dateTime";
import {dateRangeInputTemplate} from "../../../main/resources/js/awe/services/dateRange";
import {templateSelector} from "../../../main/resources/js/awe/services/selector";
import {uploaderInputTemplate} from "../../../main/resources/js/awe/services/uploader";
import {templateInputColor} from "../../../main/resources/js/awe/services/criterion";
import {
  templateButtonCheckbox, templateButtonRadio, templateInputCheckbox, templateRadio
} from "../../../main/resources/js/awe/services/checkboxRadio";

const resources = path.join(__dirname, "..", "..", "..", "main", "resources");
const tag = "<awe-context-menu open-on-right-click ng-cloak></awe-context-menu>";

describe("context menu opened with the right click", () => {
  let $compile, $rootScope, Utilities, optionControllers;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    optionControllers = {};
    inject(["$compile", "$rootScope", "$httpBackend", "AweUtilities", "Control",
      (_$compile_, _$rootScope_, $httpBackend, _Utilities_, Control) => {
      // The options are components: they take their controller (visibility included) from the declared data
      jest.spyOn(Control, "checkComponent").mockReturnValue(true);
      jest.spyOn(Control, "getAddressModel").mockReturnValue({selected: null, values: []});
      jest.spyOn(Control, "getAddressController").mockImplementation(address => optionControllers[address.component]);
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $rootScope.status = {loading: false};
      Utilities = _Utilities_;
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
    jest.spyOn(Utilities, "timeout").mockImplementation(fn => fn());
  });

  afterEach(() => {
    $("body").empty();
    $("head style[data-context-menu-test]").remove();
    jest.restoreAllMocks();
  });

  const option = {id: "Option", component: "contextOption", contextMenu: [], dependencies: [], visible: true, actions: []};

  function declareMenu(scope, menu) {
    scope.contextMenuData = menu;
    menu.forEach(declared => {
      optionControllers[declared.id] = {...declared};
    });
  }

  function compileHost(attributes = "open-on-right-click", menu = [option]) {
    const scope = $rootScope.$new();
    declareMenu(scope, menu);
    const host = $compile(`<div class="component-root"><awe-context-menu ${attributes} ng-cloak></awe-context-menu></div>`)(scope);
    $("body").append(host);
    scope.$digest();
    return {host, scope, menu: host.find("ul.context-menu")};
  }

  function rightClick(target, options = {}) {
    const event = $.Event("contextmenu", {clientX: 120, clientY: 80, ...options});
    $(target).trigger(event);
    return event;
  }

  it("shows the menu and cancels the browser menu when the component root gets a right click", () => {
    const {host, menu} = compileHost();
    expect(menu.hasClass("ng-hide")).toBe(true);

    const event = rightClick(host);

    expect(event.isDefaultPrevented()).toBe(true);
    expect(event.isPropagationStopped()).toBe(true);
    expect(menu.hasClass("ng-hide")).toBe(false);
  });

  it("opens the menu from a right click on a descendant of the component", () => {
    const {host, menu} = compileHost();
    host.append("<span class='inner'></span>");

    const event = rightClick(host.find(".inner"));

    expect(event.isDefaultPrevented()).toBe(true);
    expect(menu.hasClass("ng-hide")).toBe(false);
  });

  it("keeps the browser menu when the component declares no context menu", () => {
    const {host} = compileHost("open-on-right-click", []);

    const event = rightClick(host);

    expect(event.isDefaultPrevented()).toBe(false);
    expect(host.find("ul.context-menu").length).toBe(0);
  });

  it("does not listen to the right click without the open-on-right-click attribute", () => {
    const {host, menu} = compileHost("");

    const event = rightClick(host);

    expect(event.isDefaultPrevented()).toBe(false);
    expect(menu.hasClass("ng-hide")).toBe(true);
  });

  it("does not reopen the menu on a right click inside the menu itself", () => {
    const {host, menu} = compileHost();
    rightClick(host);
    menu.append("<li class='probe'></li>");

    const event = rightClick(menu.find(".probe"));

    expect(event.isDefaultPrevented()).toBe(false);
  });

  it("hides the menu when the mask is clicked", () => {
    const {host, scope, menu} = compileHost();
    rightClick(host);
    expect(menu.hasClass("ng-hide")).toBe(false);

    $("body > .component-mask").trigger("click");
    scope.$digest();

    expect(menu.hasClass("ng-hide")).toBe(true);
    expect($("body > .component-mask").length).toBe(0);
  });

  it("hides the menu when the hideContextMenu event is broadcast", () => {
    const {host, scope, menu} = compileHost();
    rightClick(host);

    scope.$broadcast("hideContextMenu");
    scope.$digest();

    expect(menu.hasClass("ng-hide")).toBe(true);
  });

  it("never writes an inline position on the component root", () => {
    const {host} = compileHost();
    expect(host[0].style.position).toBe("");
  });

  describe("menu placement", () => {
    const menuIn = host => host.find("ul.context-menu");

    it("renders the menu as a child of the body while it is open, fixed at the pointer", () => {
      const {host, menu} = compileHost();
      expect(menu.parent()[0]).toBe(host[0]);

      rightClick(host, {clientX: 120, clientY: 180});

      expect(menu.parent()[0]).toBe(document.body);
      expect(menu[0].style.position).toBe("fixed");
      expect(menu[0].style.top).toBe("164px");
      expect(menu[0].style.left).toBe("104px");
    });

    it("keeps the same position whatever the ancestors of the component are", () => {
      const scope = $rootScope.$new();
      declareMenu(scope, [option]);
      const wrapper = $("<div style='position: relative; transform: translateX(10px)'></div>");
      const host = $compile(`<div class="component-root"><awe-context-menu open-on-right-click ng-cloak></awe-context-menu></div>`)(scope);
      wrapper.append(host);
      $("body").append(wrapper);
      scope.$digest();
      const menu = host.find("ul.context-menu");

      rightClick(host, {clientX: 120, clientY: 180});

      expect(menu.parent()[0]).toBe(document.body);
      expect(menu[0].style.top).toBe("164px");
      expect(menu[0].style.left).toBe("104px");
    });

    it("puts the menu back where it was, with no position left, when it hides", () => {
      const {host, scope, menu} = compileHost();
      host.prepend("<span class='before'></span>");
      host.append("<span class='after'></span>");
      const index = menu.index();
      rightClick(host);

      scope.contextMenu.hide();

      expect(menu.parent()[0]).toBe(host[0]);
      expect(menu.index()).toBe(index);
      expect(menu[0].style.position + menu[0].style.top + menu[0].style.left).toBe("");
      expect(host.contents().filter((i, node) => node.nodeType === 8 && node.nodeValue === "context-menu").length).toBe(0);
      expect($("body > .component-mask").length).toBe(0);
    });

    it("can be opened again after hiding it", () => {
      const {host, scope, menu} = compileHost();
      rightClick(host);
      scope.contextMenu.hide();

      rightClick(host, {clientX: 300, clientY: 300});

      expect(menu.parent()[0]).toBe(document.body);
      expect(menu[0].style.left).toBe("284px");
      expect(menuIn(host).length).toBe(0);
      expect(scope.contextMenu.isVisible()).toBe(true);
    });

    it("leaves no second mask nor placeholder when it is shown again before hiding", () => {
      const {host, scope, menu} = compileHost();
      rightClick(host, {clientX: 120, clientY: 80});
      rightClick(host, {clientX: 300, clientY: 300});

      expect($("body > .component-mask").length).toBe(1);
      expect(menu[0].style.left).toBe("284px");

      scope.contextMenu.hide();

      expect($("body > .component-mask").length).toBe(0);
      expect(menu.parent()[0]).toBe(host[0]);
      expect(host.contents().filter((i, node) => node.nodeType === 8 && node.nodeValue === "context-menu").length).toBe(0);
    });

    it("removes the mask and the menu when the scope is destroyed while it is open", () => {
      const {host, scope, menu} = compileHost();
      rightClick(host);

      scope.$destroy();

      expect($("body > .component-mask").length).toBe(0);
      expect(menu.parent().length).toBe(0);
      expect($("body > ul.context-menu").length).toBe(0);
    });

    it("never changes the style of any ancestor and renders the menu and the mask at body level", () => {
      const scope = $rootScope.$new();
      declareMenu(scope, [option]);
      const wrapper = $("<div class='toolbar' style='position: absolute; z-index: 5'></div>");
      const host = $compile(`<div class="component-root"><awe-context-menu open-on-right-click ng-cloak></awe-context-menu></div>`)(scope);
      wrapper.append(host);
      $("body").append(wrapper);
      scope.$digest();
      const menu = host.find("ul.context-menu");

      rightClick(host);

      expect(wrapper.attr("style")).toBe("position: absolute; z-index: 5");
      expect(host[0].getAttribute("style")).toBeNull();
      expect(menu.parent()[0]).toBe(document.body);
      expect($("body > .component-mask").parent()[0]).toBe(document.body);
    });

    it("keeps the options working at body level", () => {
      const {host, scope, menu} = compileHost();
      rightClick(host);
      scope.$digest();

      expect(menu.find("awe-context-option, li.context-option").length).toBeGreaterThan(0);
      expect(menu.hasClass("ng-hide")).toBe(false);
    });
  });

  describe("grid-like show and menu lookup", () => {
    it("opens the menu on show() even when every option is hidden, and shows options that resolve afterwards", () => {
      const {scope, menu} = compileHost("open-on-right-click", [{...option, id: "Late", visible: false}]);
      expect(scope.contextMenu.hasVisibleOptions()).toBe(false);

      scope.contextMenu.show($.Event("contextmenu", {clientX: 10, clientY: 10}));

      expect(scope.contextMenu.isVisible()).toBe(true);
      expect($("body > .component-mask").length).toBe(1);
      expect(menu.parent()[0]).toBe(document.body);

      optionControllers.Late.visible = true;
      scope.$digest();
      expect(scope.contextMenu.hasVisibleOptions()).toBe(true);
      expect(menu.find("li.context-option").not(".ng-hide").length).toBe(1);
    });

    it("does not remember an empty lookup: a menu rendered after the first lookup is found", () => {
      inject(["ContextMenu", ContextMenu => {
        const parent = $("<div><span class='anchor'></span></div>");
        const menu = new ContextMenu($rootScope.$new(), parent.find(".anchor"), []);
        expect(menu.getMenuElement().length).toBe(0);

        parent.append("<ul class='context-menu'><li class='context-option'></li></ul>");

        expect(menu.getMenuElement().length).toBe(1);
        expect(menu.hasVisibleOptions()).toBe(true);
      }]);
    });

    it("re-resolves the menu when the remembered node left its place for good", () => {
      inject(["ContextMenu", ContextMenu => {
        const parent = $("<div><span class='anchor'></span><ul class='context-menu' id='old'></ul></div>");
        const menu = new ContextMenu($rootScope.$new(), parent.find(".anchor"), []);
        expect(menu.getMenuElement().attr("id")).toBe("old");

        parent.find("#old").remove();
        parent.append("<ul class='context-menu' id='fresh'></ul>");

        expect(menu.getMenuElement().attr("id")).toBe("fresh");
      }]);
    });

    it("hides without removing the linked menu when the component DOM is gone, and shows it again later", () => {
      const {host, scope, menu} = compileHost();
      menu.data("probe", "linked");
      rightClick(host);
      // The placeholder is gone with the component DOM while the scope stays alive
      host.empty();

      scope.contextMenu.hide();

      expect($("body > ul.context-menu").length).toBe(0);
      expect($("body > .component-mask").length).toBe(0);
      expect(menu.data("probe")).toBe("linked");
      expect(menu.parent().length).toBe(0);

      scope.contextMenu.show($.Event("contextmenu", {clientX: 50, clientY: 50}));

      expect(scope.contextMenu.isVisible()).toBe(true);
      expect(menu.parent()[0]).toBe(document.body);
      expect(menu[0].style.left).toBe("34px");
    });
  });

  describe("viewport clamping", () => {
    beforeEach(() => {
      window.innerWidth = 1000;
      window.innerHeight = 800;
    });

    function showAt(clientX, clientY, size) {
      const {host, menu} = compileHost();
      menu[0].getBoundingClientRect = function () {
        const left = parseFloat(this.style.left) || 0;
        const top = parseFloat(this.style.top) || 0;
        return {left, top, width: size.width, height: size.height, right: left + size.width, bottom: top + size.height};
      };
      rightClick(host, {clientX, clientY});
      return menu[0];
    }

    it("keeps the menu where it opens when it fits", () => {
      const menu = showAt(300, 200, {width: 150, height: 100});
      expect(menu.style.left).toBe("284px");
      expect(menu.style.top).toBe("184px");
    });

    it("moves the menu left when it would overflow the right edge", () => {
      const menu = showAt(950, 200, {width: 200, height: 100});
      expect(parseFloat(menu.style.left) + 200).toBeLessThanOrEqual(1000);
      expect(menu.style.top).toBe("184px");
    });

    it("moves the menu up when it would overflow the bottom edge", () => {
      const menu = showAt(300, 780, {width: 150, height: 300});
      expect(parseFloat(menu.style.top) + 300).toBeLessThanOrEqual(800);
      expect(menu.style.left).toBe("284px");
    });

    it("never moves the menu past the top left corner of the viewport", () => {
      // Larger than the viewport: the overflow is bigger than the room left above and before it
      const menu = showAt(100, 100, {width: 1200, height: 1000});
      expect(parseFloat(menu.style.left)).toBe(0);
      expect(parseFloat(menu.style.top)).toBe(0);
    });
  });

  describe("hidden options", () => {
    const hiddenOption = {...option, id: "Hidden", visible: false};
    const separator = {id: "Sep", component: "contextOption", contextMenu: [], dependencies: [], visible: true, separator: true, actions: []};

    const compileWithVisibility = (menu) => compileHost("open-on-right-click", menu);

    it("keeps the browser menu when every option is hidden", () => {
      const {host, menu} = compileWithVisibility([hiddenOption]);

      const event = rightClick(host);

      expect(event.isDefaultPrevented()).toBe(false);
      expect(menu.hasClass("ng-hide")).toBe(true);
    });

    it("keeps the browser menu when only separators remain visible", () => {
      const {host, menu} = compileWithVisibility([separator, hiddenOption]);

      const event = rightClick(host);

      expect(event.isDefaultPrevented()).toBe(false);
      expect(menu.hasClass("ng-hide")).toBe(true);
    });

    it("opens the menu when at least one option is visible", () => {
      const {host, menu} = compileWithVisibility([hiddenOption, option]);

      const event = rightClick(host);

      expect(event.isDefaultPrevented()).toBe(true);
      expect(menu.hasClass("ng-hide")).toBe(false);
    });
  });

  describe("nested components", () => {
    it("opens only the menu of the closest component", () => {
      const outerScope = $rootScope.$new();
      declareMenu(outerScope, [option]);
      const innerScope = $rootScope.$new();
      declareMenu(innerScope, [option]);
      const host = $compile(`<div class="tab"><awe-context-menu open-on-right-click ng-cloak></awe-context-menu></div>`)(outerScope);
      const inner = $compile(`<div class="inner"><awe-context-menu open-on-right-click ng-cloak></awe-context-menu></div>`)(innerScope);
      host.append(inner);
      $("body").append(host);
      outerScope.$digest();
      innerScope.$digest();
      const innerMenu = inner.find("ul.context-menu");
      const outerMenu = host.children("ul.context-menu");
      expect(innerMenu.length).toBe(1);
      expect(outerMenu.length).toBe(1);

      rightClick(inner);

      expect(innerMenu.hasClass("ng-hide")).toBe(false);
      expect(outerMenu.hasClass("ng-hide")).toBe(true);
    });

    it("does not open the menu of a tab when a grid inside it handles the right click", () => {
      const scope = $rootScope.$new();
      declareMenu(scope, [option]);
      scope.gridRightClick = jest.fn();
      const host = $compile(`<div class="tab"><awe-context-menu open-on-right-click ng-cloak></awe-context-menu>
        <div class="grid-row" ng-right-click="gridRightClick($event)"></div></div>`)(scope);
      $("body").append(host);
      scope.$digest();
      const menu = host.find("ul.context-menu");
      expect(menu.length).toBe(1);
      jest.spyOn(Utilities, "isVisible").mockReturnValue(true);

      rightClick(host.find(".grid-row"));

      expect(scope.gridRightClick).toHaveBeenCalledTimes(1);
      expect(menu.hasClass("ng-hide")).toBe(true);
    });
  });

  it("stops listening when the scope is destroyed", () => {
    const {host, scope} = compileHost();
    scope.$destroy();

    const event = rightClick(host);

    expect(event.isDefaultPrevented()).toBe(false);
  });

  describe("component templates", () => {
    const opened = {
      button: templateButton,
      text: textInputTemplate,
      textView: textViewInputTemplate,
      password: passwordInputTemplate,
      textarea: textareaInputTemplate,
      numeric: templateNumeric,
      calendar: calendarInputTemplate,
      time: timeInputTemplate,
      dateRange: dateRangeInputTemplate,
      selector: templateSelector,
      uploader: uploaderInputTemplate,
      color: templateInputColor,
      buttonCheckbox: templateButtonCheckbox,
      buttonRadio: templateButtonRadio,
      checkbox: templateInputCheckbox,
      radio: templateRadio
    };

    Object.entries(opened).forEach(([name, template]) => {
      it(`embeds the menu that opens on right click in the ${name} template`, () => {
        expect(template).toContain(tag);
        expect(template).not.toContain("<awe-context-menu ng-cloak>");
      });
    });

    [
      ["js/awe/directives/input/tab.js", "tab"],
      ["js/awe/directives/input/wizard.js", "wizard"],
      ["templates/angular/input/wizard.html", "wizard html"],
      ["templates/angular/input/checkbox.html", "checkbox html"],
      ["templates/angular/input/radio.html", "radio html"]
    ].forEach(([file, name]) => {
      it(`embeds the menu that opens on right click in the ${name} source`, () => {
        const source = fs.readFileSync(path.join(resources, file), "utf8");
        expect(source).toContain(tag);
        expect(source).not.toContain("<awe-context-menu ng-cloak>");
      });
    });

    it("leaves the grid, tree grid, pivot table and chart menus on their own triggers", () => {
      ["js/awe/directives/grid.js", "templates/angular/grid.html", "templates/angular/treegrid.html",
        "templates/angular/pivotTable.html", "templates/angular/chart.html"].forEach(file => {
        const source = fs.readFileSync(path.join(resources, file), "utf8");
        expect(source).toContain("<awe-context-menu ng-cloak></awe-context-menu>");
        expect(source).not.toContain("open-on-right-click");
      });
    });

    it("leaves the column templates without a context menu", () => {
      expect(templateColumnButton).not.toContain("awe-context-menu");
    });
  });
});
