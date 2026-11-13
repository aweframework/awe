import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";

const rect = {left: 200, right: 260, top: 100, bottom: 130, width: 60, height: 30};
const view = "report";

/**
 * show-context-menu dispatched through the real action pipeline (ActionController -> /action/show-context-menu
 * listener defined for ClientActions.component), not by calling the component method: the action has to be accepted
 * by the pipeline, so the queue continues, whether a menu opens or there is nothing to open.
 */
describe("show-context-menu through the action controller", () => {
  let $actionController, $control, $rootScope, $timeout, $httpBackend, Button, Column;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$injector", ($injector) => {
      $actionController = $injector.get("ActionController");
      $control = $injector.get("Control");
      $rootScope = $injector.get("$rootScope");
      $timeout = $injector.get("$timeout");
      $httpBackend = $injector.get("$httpBackend");
      Button = $injector.get("Button");
      Column = $injector.get("Column");
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    jest.spyOn($control, "checkOnlyComponent").mockReturnValue(true);
    jest.spyOn($control, "setAddressApi").mockReturnValue(null);
    jest.spyOn($control, "getAddressModel").mockImplementation(() => ({selected: null, values: []}));
    jest.spyOn(window.Element.prototype, "getBoundingClientRect").mockReturnValue(rect);
  });

  afterEach(() => {
    $("body").empty();
    $actionController.deleteStack();
    jest.restoreAllMocks();
  });

  const menuFor = (scope) => {
    const menu = {
      hasVisibleOptions: jest.fn(() => true),
      showUnder: jest.fn(),
      show: jest.fn()
    };
    scope.contextMenu = menu;
    return menu;
  };

  // Launches the action like a button does and runs the queue until it is empty
  const launch = (address, owner) => {
    let finished = false;
    $actionController.addActionList([{type: "show-context-menu", address}], false, {address: owner || address})
      .then(() => { finished = true; });
    $timeout.flush();
    $rootScope.$digest();
    return () => finished;
  };

  const createButton = (id, controller = {}) => {
    const element = $("<div><button></button></div>");
    $("body").append(element);
    const scope = $rootScope.$new().$new();
    scope.view = view;
    scope.context = "";
    jest.spyOn($control, "getAddressController").mockReturnValue(controller);
    const component = new Button(scope, id, element);
    component.asButton();
    return {component, scope, element};
  };

  describe("on a plain button", () => {
    it("is accepted and the queue continues when the menu opens", () => {
      const {scope, element} = createButton("but");
      const menu = menuFor(scope);

      const finished = launch({view, component: "but"});

      expect(menu.showUnder).toHaveBeenCalledTimes(1);
      expect(menu.showUnder.mock.calls[0][0].is(element.find("button"))).toBe(true);
      expect(finished()).toBe(true);
      expect($actionController.actionStackList[$actionController.currentStack]).toEqual([]);
    });

    it("is accepted and the queue continues when there is nothing to open", () => {
      const {scope} = createButton("but");
      const menu = menuFor(scope);
      menu.hasVisibleOptions.mockReturnValue(false);

      const finished = launch({view, component: "but"});

      expect(menu.showUnder).not.toHaveBeenCalled();
      expect(finished()).toBe(true);
      expect($actionController.actionStackList[$actionController.currentStack]).toEqual([]);
    });

    it("is accepted when the button declares no menu at all", () => {
      createButton("but");

      const finished = launch({view, component: "but"});

      expect(finished()).toBe(true);
    });
  });

  describe("on a grid column button", () => {
    const address = {view, component: "grid", row: "2", column: "btn"};

    const createColumnButton = (grid) => {
      const element = $("<div><button></button></div>");
      $("body").append(element);
      const scope = $rootScope.$new().$new();
      scope.view = view;
      scope.context = "";
      jest.spyOn($control, "getAddressController").mockReturnValue({});
      const attrs = {cellAddress: JSON.stringify(address), $observe: jest.fn(() => jest.fn())};
      const column = new Column(attrs);
      const component = new Button(scope, column.id, element);
      component.address = {...address};
      component.col = {grid: {appScope: {component: grid}}};
      component.element = element;
      column.init(component).asButton();
      return {component, element};
    };

    const gridFixture = () => ({
      model: {selected: [], values: [{id: 1}, {id: 2}]},
      scope: {onSaveRow: jest.fn()},
      checkInitialized: jest.fn(() => false),
      getModel: jest.fn(() => ({values: [], selected: null})),
      getController: jest.fn(() => ({dependencies: [], align: "left"})),
      updateCellModel: jest.fn(),
      showRowContextMenu: jest.fn()
    });

    it("is accepted and the queue continues when the grid menu opens under the button", () => {
      const grid = gridFixture();
      const {element} = createColumnButton(grid);

      const finished = launch(address);

      expect(grid.showRowContextMenu).toHaveBeenCalledTimes(1);
      const [row, anchor] = grid.showRowContextMenu.mock.calls[0];
      expect(row).toBe("2");
      expect(anchor.is(element.find("button"))).toBe(true);
      expect(finished()).toBe(true);
    });

    it("is accepted and the queue continues when the grid has nothing to open", () => {
      const grid = gridFixture();
      delete grid.showRowContextMenu;
      createColumnButton(grid);

      const finished = launch(address);

      expect(finished()).toBe(true);
      expect($actionController.actionStackList[$actionController.currentStack]).toEqual([]);
    });
  });
});
