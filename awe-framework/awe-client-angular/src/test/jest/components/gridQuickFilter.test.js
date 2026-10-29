// Quick filters of a grid: a segmented control in the grid toolbar whose value is sent with the grid and filters it
import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const hook = id => `[data-testid='${id}']`;

const OPTIONS = [
  {value: "", label: "All", count: 128},
  {value: "LCK", label: "Locked", count: 3, tone: "danger"},
  {value: "NO2FA", label: "Without 2FA", count: 41},
  {value: "INA", label: "Inactive", count: 12}
];

describe("grid quick filter", () => {
  let $rootScope, $compile, $control, $storage, $actionController;
  let model;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage", "ActionController",
      (_$rootScope_, _$compile_, $httpBackend, _Control_, _Storage_, _ActionController_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $control = _Control_;
        $storage = _Storage_;
        $actionController = _ActionController_;
        $rootScope.view = "base";
        $rootScope.context = "screen";
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
        $httpBackend.when("POST", "./settings").respond(DefaultSettings);
        $storage.put("controller", {base: {}});
        $storage.put("model", {base: {}});
        $storage.put("api", {base: {}});
        $storage.put("status", {base: "loaded"});
        $storage.put("screen", {base: {}});
      }]);
  });

  const mount = (selected = "", values = OPTIONS, controller = {}) => {
    $control.setAddressController({view: "base", component: "CrtUsrSta"}, {
      id: "CrtUsrSta", component: "quick-filter", initialLoad: "query", serverAction: "data", targetAction: "UsrStaCnt",
      visible: true, contextMenu: [], dependencies: [], ...controller
    });
    $control.setAddressModel({view: "base", component: "CrtUsrSta"}, {
      selected, defaultValues: [selected], values: values.map(value => ({...value})), page: 1, records: values.length, total: 1
    });
    model = $control.getAddressModel({view: "base", component: "CrtUsrSta"});
    jest.spyOn($actionController, "addActionList").mockImplementation(() => undefined);
    const element = $compile("<div><awe-grid-quick-filter quick-filter-id='CrtUsrSta' grid-id='GrdUsr'></awe-grid-quick-filter></div>")($rootScope);
    $(document.body).append(element);
    $rootScope.$digest();
    return element;
  };

  afterEach(() => $(document.body).empty());

  const options = element => element.find(hook("grid-quick-filter-option"));
  const radios = element => element.find("input[type=radio]");
  const checked = element => radios(element).map((index, radio) => radio.checked).get();
  const actionTypes = () => $actionController.addActionList.mock.calls.flatMap(([actions]) => actions.map(action => action.type));

  it("renders a radio group with one segment per option, its label and its count", () => {
    const element = mount();

    const group = element.find(hook("grid-quick-filter"));
    expect(group.attr("role")).toBe("radiogroup");
    expect(group.attr("aria-label")).toBe("SCREEN_TEXT_FILTERS");
    expect(group.attr("data-testid-owner")).toBe("GrdUsr");
    expect(options(element).map((index, option) => option.textContent.replace(/\s+/g, " ").trim()).get())
      .toEqual(["All 128", "Locked 3", "Without 2FA 41", "Inactive 12"]);
    expect(options(element).eq(1).find(".grid-quick-filter-count").attr("data-tone")).toBe("danger");
    expect(options(element).eq(0).find(".grid-quick-filter-count").attr("data-tone")).toBeUndefined();
    expect(options(element).map((index, option) => option.getAttribute("option-id")).get()).toEqual(["", "LCK", "NO2FA", "INA"]);
    expect(new Set(radios(element).map((index, radio) => radio.name).get()).size).toBe(1);
  });

  it("shows the option of its value as chosen, and the first one when no option has it", () => {
    expect(checked(mount("LCK"))).toEqual([false, true, false, false]);
    $(document.body).empty();
    expect(checked(mount(["NO2FA"]))).toEqual([false, false, true, false]);
    $(document.body).empty();
    expect(checked(mount("UNKNOWN"))).toEqual([true, false, false, false]);
    $(document.body).empty();
    expect(checked(mount(null, [{value: null, label: "All", count: 5}, {value: "LCK", label: "Locked", count: 1}]))).toEqual([true, false]);
  });

  it("hides the count of an option without it", () => {
    const element = mount("", [{value: "", label: "All"}, {value: "LCK", label: "Locked", count: 0}]);

    expect(options(element).eq(0).find(".grid-quick-filter-count").length).toBe(0);
    expect(options(element).eq(1).find(".grid-quick-filter-count").text()).toBe("0");
  });

  it("sends its value as a criterion and filters the grid when another option is chosen", () => {
    const element = mount();
    const api = $control.getAddressApi({view: "base", component: "CrtUsrSta"});

    radios(element).eq(1).click();
    $rootScope.$digest();

    expect(model.selected).toBe("LCK");
    expect(api.getData()).toEqual({CrtUsrSta: "LCK"});
    expect(checked(element)).toEqual([false, true, false, false]);
    const [actions] = $actionController.addActionList.mock.calls.find(([list]) => list[0].type === "filter");
    expect(actions).toEqual([{type: "filter", target: "GrdUsr"}]);
  });

  it("does not filter the grid again when the chosen option is clicked", () => {
    const element = mount("LCK");

    radios(element).eq(1).click();
    $rootScope.$digest();

    expect(actionTypes()).not.toContain("filter");
  });

  it("moves the choice with the arrow keys and filters the grid", () => {
    const element = mount();
    const keyDown = (index, key) => {
      const event = $.Event("keydown", {key});
      radios(element).eq(index).trigger(event);
      $rootScope.$digest();
      return event;
    };

    expect(keyDown(0, "ArrowRight").isDefaultPrevented()).toBe(true);
    expect(model.selected).toBe("LCK");
    expect(document.activeElement).toBe(radios(element)[1]);

    keyDown(1, "ArrowLeft");
    expect(model.selected).toBe("");

    keyDown(0, "ArrowUp");
    expect(model.selected).toBe("INA");
    expect(actionTypes().filter(type => type === "filter").length).toBe(3);
  });

  it("reloads its options when its grid is filtered, and not for other components", () => {
    mount("LCK");
    const reloads = () => $actionController.addActionList.mock.calls.map(([actions]) => actions[0])
      .filter(action => action.attr && action.attr("callbackTarget")?.component === "CrtUsrSta");
    const filter = component => ({
      attr: name => ({callbackTarget: {view: "base", component}, parameters: {}})[name],
      accept: jest.fn(),
      abort: jest.fn()
    });

    $rootScope.$broadcast("/action/filter", filter("GrdOther"));
    expect(reloads().length).toBe(0);

    $rootScope.$broadcast("/action/filter", filter("GrdUsr"));
    expect(reloads().length).toBe(1);
    expect(reloads()[0].attr("silent")).toBe(true);
    expect(reloads()[0].attr("async")).toBe(true);
    expect(reloads()[0].attr("parameters")).toEqual(expect.objectContaining({type: "data", targetAction: "UsrStaCnt"}));
  });

  it("shows the new options and counts when they reload, keeping the chosen one", () => {
    const element = mount("LCK");
    const api = $control.getAddressApi({view: "base", component: "CrtUsrSta"});

    api.updateModelValues({values: [{value: "", label: "All", count: 127}, {value: "LCK", label: "Locked", count: 2, tone: "danger"}]});
    $rootScope.$digest();

    expect(options(element).map((index, option) => option.textContent.replace(/\s+/g, " ").trim()).get()).toEqual(["All 127", "Locked 2"]);
    expect(checked(element)).toEqual([false, true]);
  });

  it("names the group with its label when it has one", () => {
    const element = mount("", OPTIONS, {label: "SCREEN_TEXT_USER"});

    expect(element.find(hook("grid-quick-filter")).attr("aria-label")).toBe("SCREEN_TEXT_USER");
  });
});
