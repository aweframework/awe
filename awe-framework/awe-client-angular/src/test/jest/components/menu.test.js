// Source-traceable focused Jest parity for components/menu.js.
import {cleanupHeavyComponentTest, createAction, initHeavyComponentTest} from "./heavyComponentTestUtils";

describe("components/menu.js", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it("migrates horizontal/vertical minimized state and menu actions", () => {
    const controller = {
      id: "menuId",
      style: "horizontal",
      options: [
        {id: "option1", name: "option1", label: "Option 1", visible: true},
        {id: "option2", name: "option2", label: "Option 2", visible: false}
      ]
    };
    const action = createAction({options: [{id: "option3", name: "option3", label: "Option 3", visible: true}]});
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockReturnValue({values: []});
    jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    jest.spyOn(refs.$utilities, "publish").mockImplementation(jest.fn());

    angular.element(document.body).addClass("mmc");
    const element = refs.$compile("<awe-menu menu-id='menuId'></awe-menu>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    const scope = element.find("ul.awe-menu").scope();
    expect(scope.menuType).toBe("horizontal");
    expect(scope.visible).toBe(false);
    expect(scope.status.minimized).toBe(true);
    expect(scope.options).toEqual(controller.options);

    refs.$rootScope.$broadcast("/action/toggle-menu", action);
    expect(scope.visible).toBe(true);
    expect(angular.element(document.body).hasClass("mmc")).toBe(false);
    expect(refs.$utilities.publish).toHaveBeenCalledWith("resize-action");

    refs.$rootScope.$broadcast("/action/change-menu", action);
    expect(scope.options).toEqual(action.attr("parameters").options);
  });

  function compileMenu(controllerOverrides = {}) {
    const controller = {
      id: "menuId",
      style: "vertical",
      options: [
        {id: "option1", name: "option1", label: "Option 1", visible: true},
        {id: "option2", name: "option2", label: "Option 2", visible: true}
      ],
      ...controllerOverrides
    };
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
    return {controller, element, scope: element.find("ul.awe-menu").scope()};
  }

  it("initializes a visible vertical menu when the body is not minimized", () => {
    const {scope} = compileMenu({style: "vertical"});

    expect(scope.menuType).toBe("vertical");
    expect(scope.visible).toBe(true);
    expect(scope.status.minimized).toBe(false);
  });

  it("initializes a minimized vertical menu from the body class", () => {
    angular.element(document.body).addClass("mmc");

    const {scope} = compileMenu({style: "vertical"});

    expect(scope.visible).toBe(true);
    expect(scope.status.minimized).toBe(true);
  });

  it("renders only visible allowed menu options", () => {
    const {element} = compileMenu({options: [
      {id: "option1", name: "option1", label: "Option 1", visible: true},
      {id: "option2", name: "option2", label: "Option 2", visible: false}
    ]});

    expect(element.text()).toContain("Option 1");
    expect(element.text()).not.toContain("Option 2");
  });

  it("toggles a visible menu back to minimized state", () => {
    const {scope} = compileMenu({style: "vertical"});

    refs.$rootScope.$broadcast("/action/toggle-menu", createAction());

    expect(angular.element(document.body).hasClass("mmc")).toBe(true);
    expect(refs.$utilities.publish).toHaveBeenCalledWith("resize-action");
  });

  it("changes menu options from action parameters", () => {
    const {scope} = compileMenu();
    const action = createAction({options: [{id: "new", name: "new", label: "New", visible: true}]});

    refs.$rootScope.$broadcast("/action/change-menu", action);

    expect(scope.options).toEqual(action.attr("parameters").options);
  });

  function getService(name) {
    let service;
    inject([name, dependency => { service = dependency; }]);
    return service;
  }

  it("enables the option search by default and toggles the panel", () => {
    const {scope} = compileMenu({style: "vertical"});

    expect(scope.searchEnabled).toBe(true);
    expect(scope.search.open).toBe(false);

    scope.toggleSearch();
    expect(scope.search.open).toBe(true);

    scope.toggleSearch();
    expect(scope.search.open).toBe(false);
  });

  it("disables the option search when the menuSearchEnabled setting is false", () => {
    jest.spyOn(getService("AweSettings"), "get")
      .mockImplementation(key => (key === "menuSearchEnabled" ? false : undefined));

    const {scope} = compileMenu({style: "vertical"});

    expect(scope.searchEnabled).toBe(false);
  });

  it("filters nested options into flat breadcrumb results", () => {
    const {scope} = compileMenu({options: [
      {id: "sales", name: "sales", label: "Sales", visible: true, options: [
        {id: "invoices", name: "invoices", label: "Invoices", visible: true, options: [
          {id: "newInvoice", name: "newInvoice", label: "New invoice", visible: true, actions: [{type: "screen"}]}
        ]}
      ]}
    ]});

    scope.search.query = "invoice";
    scope.onSearchChange();

    expect(scope.search.results).toHaveLength(1);
    expect(scope.search.results[0].label).toBe("New invoice");
    expect(scope.search.results[0].breadcrumb).toBe("Sales › Invoices");
    expect(scope.search.active).toBe(0);
  });

  it("navigates results with the keyboard and launches the active one on Enter", () => {
    const actionController = getService("ActionController");
    jest.spyOn(actionController, "closeAllActions").mockImplementation(jest.fn());
    const addActionList = jest.spyOn(actionController, "addActionList").mockImplementation(jest.fn());
    const {scope} = compileMenu({options: [
      {id: "a", name: "a", label: "Alpha", visible: true, actions: [{type: "screen"}]},
      {id: "b", name: "b", label: "Alfa", visible: true, actions: [{type: "screen"}]}
    ]});

    scope.search.query = "a";
    scope.onSearchChange();
    expect(scope.search.results).toHaveLength(2);
    expect(scope.search.active).toBe(0);

    scope.onSearchKeydown({keyCode: 40, preventDefault: jest.fn()});
    expect(scope.search.active).toBe(1);
    scope.onSearchKeydown({keyCode: 40, preventDefault: jest.fn()});
    expect(scope.search.active).toBe(0);
    scope.onSearchKeydown({keyCode: 38, preventDefault: jest.fn()});
    expect(scope.search.active).toBe(1);

    scope.onSearchKeydown({keyCode: 13, preventDefault: jest.fn()});
    expect(addActionList).toHaveBeenCalled();
    expect(scope.search.open).toBe(false);
  });

  it("closes the search on Escape and resets its state", () => {
    const {scope} = compileMenu({style: "vertical"});

    scope.openSearch();
    scope.search.query = "something";
    scope.onSearchKeydown({keyCode: 27, preventDefault: jest.fn()});

    expect(scope.search.open).toBe(false);
    expect(scope.search.query).toBe("");
    expect(scope.search.results).toEqual([]);
  });

  it("ignores a selected result without actions but still closes the search", () => {
    const {scope} = compileMenu({style: "vertical"});

    scope.openSearch();
    scope.selectResult({option: {name: "no-actions"}});

    expect(scope.search.open).toBe(false);
  });

  it("closes the open search when clicking outside the panel", () => {
    const {scope} = compileMenu({style: "vertical"});

    scope.openSearch();
    expect(scope.search.open).toBe(true);

    document.dispatchEvent(new Event("click"));

    expect(scope.search.open).toBe(false);
  });
});
