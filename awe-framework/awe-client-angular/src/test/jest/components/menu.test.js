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
});
