import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {ClientActions} from "../../../main/resources/js/awe/data/actions";
import "../../../main/resources/js/awe/app";

const rect = {left: 200, right: 260, top: 100, bottom: 130, width: 60, height: 30};

describe("show-context-menu client action", () => {
  let ContextMenu, Component, GridCommons, Utilities, $rootScope;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$injector", "$httpBackend", ($injector, $httpBackend) => {
      ContextMenu = $injector.get("ContextMenu");
      Component = $injector.get("Component");
      GridCommons = $injector.get("GridCommons");
      Utilities = $injector.get("AweUtilities");
      $rootScope = $injector.get("$rootScope");
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
    jest.spyOn(Utilities, "timeout").mockImplementation(fn => fn());
  });

  afterEach(() => {
    $("body").empty();
    jest.restoreAllMocks();
  });

  const anchorWithRect = () => {
    const anchor = $("<button></button>");
    anchor[0].getBoundingClientRect = () => rect;
    return anchor;
  };

  it("is registered as a component action handled by onShowContextMenu", () => {
    expect(ClientActions.component["show-context-menu"]).toEqual({method: "onShowContextMenu"});
  });

  describe("ContextMenu.showUnder", () => {
    const createMenu = () => {
      const host = $("<div><ul class='context-menu'></ul></div>");
      $("body").append(host);
      const menu = new ContextMenu($rootScope.$new(), host.find("ul"), []);
      menu.init();
      return {menu, element: host.find("ul")};
    };

    it("places the menu top left corner at the bottom left corner of the anchor", () => {
      const {menu, element} = createMenu();

      menu.showUnder(anchorWithRect());

      expect(menu.isVisible()).toBe(true);
      expect(element[0].style.position).toBe("fixed");
      expect(element[0].style.left).toBe("200px");
      expect(element[0].style.top).toBe("130px");
    });

    it.each([
      ["sits in the window header", 1024, true],
      ["would overflow the right edge of the viewport", 400, false]
    ])("aligns the right edge of the menu to the right edge of the anchor when the anchor %s",
      (_, viewportWidth, inHeader) => {
        // The menu is 250 px wide: left aligned it would start at 200 px, right aligned it ends at 260 px
        const {menu, element} = createMenu();
        element[0].getBoundingClientRect = () => {
          const left = parseFloat(element[0].style.left) || 0;
          return {left, right: left + 250, top: 130, bottom: 230, width: 250, height: 100};
        };
        Object.defineProperty(element[0], "offsetWidth", {configurable: true, value: 250});
        const anchor = anchorWithRect();
        if (inHeader) {
          $("<div class='window-buttons'></div>").append(anchor).appendTo("body");
        }
        const innerWidth = window.innerWidth;
        window.innerWidth = viewportWidth;
        try {
          menu.showUnder(anchor);
        } finally {
          window.innerWidth = innerWidth;
        }

        expect(element[0].style.left).toBe("10px");
        expect(element[0].style.top).toBe("130px");
      });

    it("can be closed through the mask like any other menu", () => {
      const {menu} = createMenu();
      menu.showUnder(anchorWithRect());

      $("body > .component-mask").trigger("click");

      expect(menu.isVisible()).toBe(false);
    });

    describe("from the keyboard", () => {
      const createMenuWithOptions = () => {
        const host = $("<div><ul class='context-menu'>" +
          "<li class='context-option'><a name='one' tabindex='-1'>One</a></li>" +
          "<li class='context-option ng-hide'><a name='hidden' tabindex='-1'>Hidden</a></li>" +
          "<li class='context-option divider'></li>" +
          "<li class='context-option'><a name='two' tabindex='-1'>Two</a></li>" +
          "</ul></div>");
        $("body").append(host);
        const menu = new ContextMenu($rootScope.$new(), host.find("ul"), []);
        menu.init();
        return {menu, element: host.find("ul")};
      };
      const key = (element, name) => element[0].dispatchEvent(new KeyboardEvent("keydown", {key: name, bubbles: true}));

      it("focuses the first visible option, moves with the arrows and activates the option with Enter", () => {
        const {menu, element} = createMenuWithOptions();
        const anchor = anchorWithRect().appendTo("body");
        const click = jest.fn();
        element.find("a[name=two]").on("click", click);

        menu.showUnder(anchor);
        expect(document.activeElement.getAttribute("name")).toBe("one");

        key(element, "ArrowDown");
        expect(document.activeElement.getAttribute("name")).toBe("two");
        key(element, "ArrowDown");
        expect(document.activeElement.getAttribute("name")).toBe("one");
        key(element, "ArrowUp");
        expect(document.activeElement.getAttribute("name")).toBe("two");

        key(element, "Enter");
        expect(click).toHaveBeenCalledTimes(1);
      });

      it("closes with Escape and gives the focus back to the button it hangs from", () => {
        const {menu, element} = createMenuWithOptions();
        const anchor = anchorWithRect().appendTo("body");

        menu.showUnder(anchor);
        key(element, "Escape");

        expect(menu.isVisible()).toBe(false);
        expect(document.activeElement).toBe(anchor[0]);
      });
    });

    it("does nothing when the anchor is empty", () => {
      const {menu} = createMenu();

      menu.showUnder($());

      expect(menu.isVisible()).toBe(false);
    });
  });

  describe("ContextMenu.hide when the scope is destroyed", () => {
    it("removes the menu from the page instead of leaving it detached", () => {
      const host = $("<div><ul class='context-menu'></ul></div>");
      $("body").append(host);
      const element = host.find("ul");
      const menu = new ContextMenu($rootScope.$new(), element, []);
      menu.init();
      menu.show({clientX: 10, clientY: 10, preventDefault: () => null});
      host.remove();
      const removeSpy = jest.spyOn(element, "remove");
      const detachSpy = jest.spyOn(element, "detach");

      menu.hide(true);

      expect(removeSpy).toHaveBeenCalled();
      expect(detachSpy).not.toHaveBeenCalled();
      expect($("body ul.context-menu").length).toBe(0);
    });
  });

  describe("Component.onShowContextMenu", () => {
    const createComponent = (contextMenu) => {
      const component = new Component($rootScope.$new(), "button1");
      component.scope.contextMenu = contextMenu;
      const anchor = anchorWithRect();
      component.getContextMenuAnchor = () => anchor;
      return {component, anchor};
    };

    it("opens its own menu under the component", () => {
      const contextMenu = {hasVisibleOptions: () => true, showUnder: jest.fn()};
      const {component, anchor} = createComponent(contextMenu);

      component.onShowContextMenu();

      expect(contextMenu.showUnder).toHaveBeenCalledWith(anchor);
    });

    it("finishes without opening anything when the component has no menu", () => {
      const {component} = createComponent(undefined);

      expect(() => component.onShowContextMenu()).not.toThrow();
    });

    it("finishes without opening anything when no option is visible", () => {
      const contextMenu = {hasVisibleOptions: () => false, showUnder: jest.fn()};
      const {component} = createComponent(contextMenu);

      component.onShowContextMenu();

      expect(contextMenu.showUnder).not.toHaveBeenCalled();
    });
  });

  describe("Grid showRowContextMenu", () => {
    const createGrid = () => {
      const calls = [];
      const component = {
        constants: {ROW_IDENTIFIER: "id"},
        scope: {$on: () => null, gridOptions: {}},
        listeners: {},
        api: {},
        controller: {columnModel: [], headerModel: [], contextMenu: [{id: "OptAdd"}]},
        model: {values: [{id: 1}, {id: 2}], selected: []},
        selectRow: jest.fn(row => calls.push(["select", row])),
        showContextMenuUnder: jest.fn(anchor => calls.push(["show", anchor]))
      };
      new GridCommons(component).init();
      return {component, calls};
    };

    it("selects the row of the button and then opens the grid menu under it", () => {
      const {component, calls} = createGrid();
      const anchor = anchorWithRect();

      component.showRowContextMenu("2", anchor);

      expect(calls).toEqual([["select", {id: 2}], ["show", anchor]]);
    });

    it("opens the menu even when no option is visible yet: the options depend on the selected row", () => {
      // showContextMenuUnder is not gated by visible options for grids (their dependencies resolve after the selection)
      const {component, calls} = createGrid();
      const anchor = anchorWithRect();
      component.scope.contextMenu = {hasVisibleOptions: () => false};

      component.showRowContextMenu("2", anchor);

      expect(calls).toEqual([["select", {id: 2}], ["show", anchor]]);
    });

    it("neither selects nor opens anything when the grid declares no options", () => {
      const {component, calls} = createGrid();
      component.controller.contextMenu = [];

      component.showRowContextMenu("2", anchorWithRect());

      expect(calls).toEqual([]);
    });

    it("finishes without selecting or opening anything when the row does not exist", () => {
      const {component, calls} = createGrid();

      component.showRowContextMenu("99", anchorWithRect());

      expect(calls).toEqual([]);
    });
  });
});
