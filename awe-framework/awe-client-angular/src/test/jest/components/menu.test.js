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

  describe("flyout of the collapsed menu", () => {
    const groupOptions = [
      {id: "group", name: "group", label: "Group", visible: true, options: [
        {id: "child", name: "child", label: "Child", visible: true, actions: []}
      ]},
      {id: "other", name: "other", label: "Other", visible: true, options: [
        {id: "otherChild", name: "otherChild", label: "Other child", visible: true, actions: []}
      ]}
    ];
    const option = (element, name) => element.find(`a[name='${name}']`).parent();

    it("opens the options of a first level option as a flyout while the pointer is over it, as the prototype", () => {
      angular.element(document.body).addClass("mmc");
      const {element} = compileMenu({style: "vertical", options: groupOptions});

      option(element, "group").triggerHandler("mouseenter");
      refs.$rootScope.$digest();

      expect(option(element, "group").hasClass("mmc-dropdown-open")).toBe(true);
      expect(option(element, "group").find("ul").hasClass("mmc-dropdown-open-ul")).toBe(true);

      // Another option replaces the flyout
      option(element, "other").triggerHandler("mouseenter");
      refs.$rootScope.$digest();
      expect(option(element, "group").hasClass("mmc-dropdown-open")).toBe(false);
      expect(option(element, "other").hasClass("mmc-dropdown-open")).toBe(true);

      // Leaving the option closes it after a moment (the test runs the timeout at once)
      option(element, "other").triggerHandler("mouseleave");
      refs.$rootScope.$digest();
      expect(option(element, "other").hasClass("mmc-dropdown-open")).toBe(false);
      expect(refs.$utilities.timeout).toHaveBeenCalledWith(expect.any(Function), 220);
    });

    it("does not open the options under the pointer while the menu is expanded", () => {
      const {element} = compileMenu({style: "vertical", options: groupOptions});

      option(element, "group").triggerHandler("mouseenter");
      refs.$rootScope.$digest();

      expect(option(element, "group").hasClass("open")).toBe(false);
      expect(option(element, "group").hasClass("mmc-dropdown-open")).toBe(false);
    });
  });

  it("renders only visible allowed menu options", () => {
    const {element} = compileMenu({options: [
      {id: "option1", name: "option1", label: "Option 1", visible: true},
      {id: "option2", name: "option2", label: "Option 2", visible: false}
    ]});

    expect(element.text()).toContain("Option 1");
    expect(element.text()).not.toContain("Option 2");
  });

  it("shows the description of an option as the hint of its link", () => {
    const {element} = compileMenu({options: [
      {id: "described", name: "described", label: "Described", description: "Shown on hover", visible: true},
      {id: "plain", name: "plain", label: "Plain", visible: true}
    ]});

    expect(element.find("a[name='described']").attr("title")).toBe("Shown on hover");
    expect(element.find("a[name='plain']").attr("title")).toBeFalsy();
  });

  const translateWith = (dictionary) => jest.spyOn(getService("$translate"), "instant")
    .mockImplementation(key => dictionary[key] || key);

  it("shows the translated description as the hint of the option link", () => {
    translateWith({MENU_DESC: "Translated hint"});
    const {element} = compileMenu({options: [
      {id: "described", name: "described", label: "Described", description: "MENU_DESC", visible: true}
    ]});

    expect(element.find("a[name='described']").attr("title")).toBe("Translated hint");
  });

  it("matches the search against the translated description, not its key", () => {
    translateWith({MENU_DESC: "Quarterly figures"});
    const {scope} = compileMenu({options: [
      {id: "described", name: "described", label: "Described", description: "MENU_DESC", visible: true, actions: [{type: "screen"}]}
    ]});

    scope.search.query = "quarterly";
    scope.onSearchChange();
    expect(scope.search.results).toHaveLength(1);

    scope.search.query = "menu_desc";
    scope.onSearchChange();
    expect(scope.search.results).toHaveLength(0);
  });

  it("toggles a visible menu back to minimized state", () => {
    compileMenu({style: "vertical"});

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

  describe("Ctrl K shortcut", () => {
    const pressShortcut = (init) => {
      const event = new window.KeyboardEvent("keydown", {key: "k", code: "KeyK", bubbles: true, cancelable: true, ...init});
      document.dispatchEvent(event);
      return event;
    };

    afterEach(() => {
      angular.element(document.body).removeClass("mmc");
    });

    it("shows the invitation and the shortcut in the vertical search field", () => {
      const {element} = compileMenu({style: "vertical"});

      const toggle = element.find(".awe-menu-search-toggle");
      expect(toggle.attr("aria-keyshortcuts")).toBe("Control+K Meta+K");
      expect(toggle.attr("title")).toBe("MENU_SEARCH_PLACEHOLDER");
      expect(toggle.find(".awe-menu-search-toggle-label").text()).toBe("MENU_SEARCH_PLACEHOLDER");
      const kbd = toggle.find("kbd.awe-menu-search-kbd");
      expect(kbd.text()).toBe("Ctrl K");
      expect(kbd.attr("aria-hidden")).toBe("true");
    });

    it("keeps the icon toggle of the horizontal menu without label or shortcut hint", () => {
      const {element} = compileMenu({style: "horizontal"});

      const toggle = element.find(".awe-menu-search-toggle");
      expect(toggle.attr("aria-keyshortcuts")).toBe("Control+K Meta+K");
      expect(toggle.attr("title")).toBe("BUTTON_SEARCH");
      expect(toggle.find(".awe-menu-search-toggle-label").length).toBe(0);
      expect(toggle.find("kbd").length).toBe(0);
    });

    it.each([["Ctrl", {ctrlKey: true}], ["Meta", {metaKey: true}], ["Ctrl with caps lock", {ctrlKey: true, key: "K"}]])(
      "opens the search and focuses its input with %s + K", (_name, init) => {
        const {element, scope} = compileMenu({style: "vertical"});
        angular.element(document.body).append(element);

        const event = pressShortcut(init);
        refs.$rootScope.$digest();

        expect(event.defaultPrevented).toBe(true);
        expect(scope.search.open).toBe(true);
        expect(document.activeElement).toBe(element.find(".awe-menu-search-input")[0]);
        element.remove();
      });

    it("keeps the search open with its query when it is already open", () => {
      const {scope} = compileMenu({style: "vertical"});
      scope.openSearch();
      scope.search.query = "invoice";

      pressShortcut({ctrlKey: true});

      expect(scope.search.open).toBe(true);
      expect(scope.search.query).toBe("invoice");
    });

    it.each([["K alone", {}], ["Ctrl + Shift + K", {ctrlKey: true, shiftKey: true}], ["Alt + K", {altKey: true}],
      ["Ctrl + J", {ctrlKey: true, key: "j", code: "KeyJ"}]])("ignores %s and leaves the event untouched", (_name, init) => {
      const {scope} = compileMenu({style: "vertical"});

      const event = pressShortcut(init);

      expect(event.defaultPrevented).toBe(false);
      expect(scope.search.open).toBe(false);
    });

    it("ignores the shortcut when the vertical menu is collapsed", () => {
      angular.element(document.body).addClass("mmc");
      const {scope} = compileMenu({style: "vertical"});

      const event = pressShortcut({ctrlKey: true});

      expect(event.defaultPrevented).toBe(false);
      expect(scope.search.open).toBe(false);
    });

    it("ignores the shortcut when the option search is disabled", () => {
      jest.spyOn(getService("AweSettings"), "get")
        .mockImplementation(key => (key === "menuSearchEnabled" ? false : undefined));
      const {scope} = compileMenu({style: "vertical"});

      const event = pressShortcut({ctrlKey: true});

      expect(event.defaultPrevented).toBe(false);
      expect(scope.search.open).toBe(false);
    });

    it("stops listening to the shortcut once the menu is destroyed", () => {
      const {scope} = compileMenu({style: "vertical"});
      scope.$destroy();

      const event = pressShortcut({ctrlKey: true});

      expect(event.defaultPrevented).toBe(false);
      expect(scope.search.open).toBe(false);
    });
  });
});
