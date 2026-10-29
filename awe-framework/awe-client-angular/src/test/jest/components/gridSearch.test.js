// Search of a grid: a text field in the grid toolbar whose value is sent with the grid and filters it as the user types
import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const hook = id => `[data-testid='${id}']`;

describe("grid search", () => {
  let $rootScope, $compile, $control, $storage, $actionController, $timeout;
  let model;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", "$timeout", "Control", "Storage", "ActionController",
      (_$rootScope_, _$compile_, $httpBackend, _$timeout_, _Control_, _Storage_, _ActionController_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $timeout = _$timeout_;
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

  const mount = (selected = "", controller = {}) => {
    $control.setAddressController({view: "base", component: "CrtUsrTxt"}, {
      id: "CrtUsrTxt", component: "grid-search", placeholder: "SCREEN_TEXT_USER_SEARCH", visible: true, contextMenu: [],
      dependencies: [], ...controller
    });
    $control.setAddressModel({view: "base", component: "CrtUsrTxt"}, {
      selected, defaultValues: [selected], values: [], page: 1, records: 0, total: 1
    });
    model = $control.getAddressModel({view: "base", component: "CrtUsrTxt"});
    jest.spyOn($actionController, "addActionList").mockImplementation(() => undefined);
    const element = $compile("<div><awe-grid-search search-id='CrtUsrTxt' grid-id='GrdUsr'></awe-grid-search></div>")($rootScope);
    $(document.body).append(element);
    $rootScope.$digest();
    return element;
  };

  afterEach(() => $(document.body).empty());

  const input = element => element.find(`${hook("grid-search")} input`);
  const clearButton = element => element.find(hook("grid-search-clear"));
  const filters = () => $actionController.addActionList.mock.calls.filter(([actions]) => actions[0].type === "filter")
    .map(([actions]) => actions);
  const type = (element, text) => {
    input(element).val(text).trigger("input");
    $rootScope.$digest();
  };
  const keyDown = (element, key) => {
    const event = $.Event("keydown", {key});
    input(element).trigger(event);
    $rootScope.$digest();
    return event;
  };

  it("renders a search field with the search icon, its placeholder and its accessible name", () => {
    const element = mount();

    const search = element.find(hook("grid-search"));
    expect(search.attr("role")).toBe("search");
    expect(search.attr("data-testid-owner")).toBe("GrdUsr");
    expect(search.attr("criterion-id")).toBe("CrtUsrTxt");
    expect(input(element).attr("type")).toBe("search");
    expect(input(element).attr("placeholder")).toBe("SCREEN_TEXT_USER_SEARCH");
    expect(input(element).attr("aria-label")).toBe("BUTTON_SEARCH");
    expect(input(element).attr("data-testid")).toBe("criterion-input");
    expect(search.find(".grid-search-icon svg use").attr("href")).toContain("#search");
    expect(clearButton(element).length).toBe(0);
  });

  it("names the field with its label and shows its value", () => {
    const element = mount("adm", {label: "SCREEN_TEXT_USER_SEARCH_LABEL"});

    expect(input(element).attr("aria-label")).toBe("SCREEN_TEXT_USER_SEARCH_LABEL");
    expect(input(element).val()).toBe("adm");
    expect(clearButton(element).attr("aria-label")).toBe("BUTTON_CLEAR");
  });

  it("sends its value as a criterion and filters the grid 300 ms after the user stops typing", () => {
    const element = mount();
    const api = $control.getAddressApi({view: "base", component: "CrtUsrTxt"});

    type(element, "ad");
    $timeout.flush(200);
    type(element, "adm");
    $timeout.flush(299);
    expect(filters()).toEqual([]);

    $timeout.flush(1);
    expect(api.getData()).toEqual({CrtUsrTxt: "adm"});
    expect(filters()).toEqual([[{type: "filter", target: "GrdUsr"}]]);
    expect($actionController.addActionList.mock.calls[0][2]).toEqual(expect.objectContaining({
      address: {view: "base", component: "CrtUsrTxt"}
    }));
  });

  it("filters the grid at once with Enter, without waiting nor filtering twice", () => {
    const element = mount();

    type(element, "adm");
    const event = keyDown(element, "Enter");

    expect(event.isDefaultPrevented()).toBe(true);
    expect(filters().length).toBe(1);
    $timeout.flush(300);
    keyDown(element, "Enter");
    expect(filters().length).toBe(1);
  });

  it("empties the field and filters the grid with the clear button, keeping the focus in the field", () => {
    const element = mount("adm");

    clearButton(element).click();
    $rootScope.$digest();

    expect(model.selected).toBe("");
    expect(input(element).val()).toBe("");
    expect(filters()).toEqual([[{type: "filter", target: "GrdUsr"}]]);
    expect(document.activeElement).toBe(input(element)[0]);
    expect(clearButton(element).length).toBe(0);
  });

  it("empties the field and filters the grid with Escape, and does nothing when it is empty", () => {
    const element = mount("adm");

    expect(keyDown(element, "Escape").isPropagationStopped()).toBe(true);
    expect(model.selected).toBe("");
    expect(filters().length).toBe(1);

    expect(keyDown(element, "Escape").isPropagationStopped()).toBe(false);
    expect(filters().length).toBe(1);
  });

  it("takes the text of a filter of the grid from elsewhere as the filtered one", () => {
    const element = mount("adm");
    const filterOf = component => ({
      attr: name => ({callbackTarget: {view: "base", component}, parameters: {}})[name],
      accept: jest.fn(),
      abort: jest.fn()
    });

    // A reset of the screen empties the field and filters the grid: typing the old text back filters it again
    model.selected = "";
    $rootScope.$broadcast("/action/filter", filterOf("GrdOther"));
    $rootScope.$broadcast("/action/filter", filterOf("GrdUsr"));
    $rootScope.$digest();
    expect(input(element).val()).toBe("");
    type(element, "adm");
    $timeout.flush(300);

    expect(filters()).toEqual([[{type: "filter", target: "GrdUsr"}]]);
  });

  it("does not filter when the text is typed back to the filtered one, nor when it is read only", () => {
    const element = mount("adm");

    type(element, "admi");
    type(element, "adm");
    $timeout.flush(300);
    expect(filters()).toEqual([]);

    $(document.body).empty();
    const readonly = mount("", {readonly: true});
    expect(input(readonly).prop("disabled")).toBe(true);
  });
});
