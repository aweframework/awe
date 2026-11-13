// What the menu does for the levels of the window breadcrumb (#938): launch an option, open at a group
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";

describe("menu of the breadcrumb levels", () => {
  let refs;
  let breadcrumb;
  let actionController;

  const options = [
    {id: "root", name: "root", label: "Root", visible: true, options: [
      {id: "inner", name: "inner", label: "Inner", visible: true, options: [
        {id: "leaf", name: "leaf", label: "Leaf", visible: true, actions: [{type: "screen", target: "leaf"}]}
      ]}
    ]},
    {id: "other", name: "other", label: "Other", visible: true, options: [
      {id: "child", name: "child", label: "Child", visible: true, actions: []}
    ]}
  ];

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["Breadcrumb", "ActionController", (_Breadcrumb_, _ActionController_) => {
      breadcrumb = _Breadcrumb_;
      actionController = _ActionController_;
    }]);
  });

  afterEach(cleanupHeavyComponentTest);

  function compileMenu(style = "vertical") {
    const controller = {id: "menuId", style, options};
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
    return {element, scope: element.find("ul.awe-menu").scope()};
  }

  const opened = scope => Object.keys(scope.selectedOption.opened).sort();

  it("opens the vertical menu at a group, with the groups above it", () => {
    const {scope} = compileMenu();

    expect(breadcrumb.isLink({name: "inner", group: true})).toBe(true);
    breadcrumb.follow({name: "inner", group: true});

    expect(opened(scope)).toEqual(["inner", "root"]);
    // Only the first level group is the opened one of the menu: the same value the option click writes
    expect(scope.selectedOption.opened).toEqual({root: true, inner: false});
  });

  it("opens the collapsed rail at the group, so the group shows", () => {
    angular.element(document.body).addClass("mmc");
    const {scope} = compileMenu();
    expect(scope.status.minimized).toBe(true);

    breadcrumb.follow({name: "other", group: true});

    expect(scope.status.minimized).toBe(false);
    expect(angular.element(document.body).hasClass("mmc")).toBe(false);
    expect(opened(scope)).toEqual(["other"]);
  });

  it("opens the drawer of a narrow viewport at the group", () => {
    angular.element(document.body).addClass("mmc");
    const {scope} = compileMenu();
    scope.status.resolution = "mobile";

    breadcrumb.follow({name: "other", group: true});

    expect(scope.status.minimized).toBe(false);
    expect(angular.element(document.body).hasClass("mme")).toBe(true);
    expect(opened(scope)).toEqual(["other"]);
  });

  it("does not open at a group that is not in the menu", () => {
    const {scope} = compileMenu();

    expect(breadcrumb.isLink({name: "missing", group: true})).toBe(false);
    breadcrumb.follow({name: "missing", group: true});

    expect(opened(scope)).toEqual([]);
  });

  it("cannot open the horizontal menu at a group", () => {
    compileMenu("horizontal");

    expect(breadcrumb.isLink({name: "inner", group: true})).toBe(false);
  });

  it("launches an option of the menu as a menu option does, and closes the drawer", () => {
    const close = jest.spyOn(actionController, "closeAllActions").mockImplementation(jest.fn());
    const add = jest.spyOn(actionController, "addActionList").mockImplementation(jest.fn());
    const {scope} = compileMenu("horizontal");
    const clicked = jest.fn();
    scope.$on("optionClicked", clicked);

    expect(breadcrumb.isLink({name: "leaf", group: false})).toBe(true);
    breadcrumb.follow({name: "leaf", group: false});

    expect(close).toHaveBeenCalled();
    expect(add).toHaveBeenCalledWith([{type: "screen", target: "leaf", origin: "menu"}], true, {});
    expect(clicked).toHaveBeenCalled();
  });

  it("detaches from the breadcrumb when it is destroyed", () => {
    const {scope} = compileMenu();
    scope.$destroy();

    expect(breadcrumb.isLink({name: "inner", group: true})).toBe(false);
  });
});
