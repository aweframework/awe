// Quick filters of a grid shown as a menu button (display="dropdown") and the date range quick filter (display="date-range")
import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const hook = id => `[data-testid='${id}']`;

const ORIGINS = [
  {value: "", label: "All"},
  {value: "manual", label: "Manual", count: 3},
  {value: "file", label: "File", count: 1, tone: "danger"}
];

const WINDOWS = [
  {value: "", label: "Any date"},
  {value: "24", label: "Last 24 h"},
  {value: "168", label: "Last 7 days"}
];

describe("grid quick filter menu", () => {
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

  const mount = (display = "dropdown", values = ORIGINS, label = "Origin") => {
    $control.setAddressController({view: "base", component: "CrtFlt"}, {
      id: "CrtFlt", component: "quick-filter", initialLoad: "query", serverAction: "data", targetAction: "FltLst",
      visible: true, contextMenu: [], dependencies: [], display, label
    });
    $control.setAddressModel({view: "base", component: "CrtFlt"}, {
      selected: "", defaultValues: [""], values: values.map(value => ({...value})), page: 1, records: values.length, total: 1
    });
    model = $control.getAddressModel({view: "base", component: "CrtFlt"});
    jest.spyOn($actionController, "addActionList").mockImplementation(() => undefined);
    const element = $compile("<div><awe-grid-quick-filter quick-filter-id='CrtFlt' grid-id='GrdExe'></awe-grid-quick-filter></div>")($rootScope);
    $(document.body).append(element);
    $rootScope.$digest();
    return element;
  };

  afterEach(() => $(document.body).empty());

  const api = () => $control.getAddressApi({view: "base", component: "CrtFlt"});
  const toggle = element => element.find(hook("grid-quick-filter-toggle"));
  const items = element => element.find(`${hook("grid-quick-filter-menu")} [role^=menuitem]`);
  const text = node => node.textContent.replace(/\s+/g, " ").trim();
  const click = node => {
    $(node).click();
    $rootScope.$digest();
  };
  const keyDown = (node, key) => {
    $(node).trigger($.Event("keydown", {key}));
    $rootScope.$digest();
  };
  const type = (node, value) => {
    $(node).val(value).trigger("change");
    $rootScope.$digest();
  };
  const filters = () => $actionController.addActionList.mock.calls.filter(([list]) => list[0].type === "filter");

  it("shows a dropdown quick filter as a menu button «Label: option» whose menu lists the options and their counts", () => {
    const element = mount();

    expect(element.find("[role=radiogroup]").length).toBe(0);
    expect(text(toggle(element)[0])).toBe("Origin: All");
    expect(toggle(element).attr("aria-haspopup")).toBe("menu");
    expect(toggle(element).attr("aria-expanded")).toBe("false");

    click(toggle(element));

    expect(toggle(element).attr("aria-expanded")).toBe("true");
    expect(items(element).map((index, item) => text(item)).get()).toEqual(["All", "Manual 3", "File 1"]);
    expect(items(element).map((index, item) => item.getAttribute("role")).get()).toEqual(["menuitemradio", "menuitemradio", "menuitemradio"]);
    expect(items(element).map((index, item) => item.getAttribute("aria-checked")).get()).toEqual(["true", "false", "false"]);
    expect(items(element).eq(2).find(".grid-quick-filter-count").attr("data-tone")).toBe("danger");
  });

  it("filters the grid with the option chosen in the menu, closes it and shows the option on the button", () => {
    const element = mount();

    click(toggle(element));
    click(items(element)[1]);

    expect(model.selected).toBe("manual");
    expect(api().getData()).toEqual({CrtFlt: "manual"});
    expect(filters().map(([actions]) => actions)).toEqual([[{type: "filter", target: "GrdExe"}]]);
    expect(toggle(element).attr("aria-expanded")).toBe("false");
    expect(text(toggle(element)[0])).toBe("Origin: Manual");
  });

  it("opens with the arrow keys on the chosen option, moves with them and closes with Escape on the button", () => {
    const element = mount();

    keyDown(toggle(element), "ArrowDown");
    expect(toggle(element).attr("aria-expanded")).toBe("true");
    expect(document.activeElement).toBe(items(element)[0]);

    keyDown(items(element)[0], "ArrowDown");
    expect(document.activeElement).toBe(items(element)[1]);
    keyDown(items(element)[1], "ArrowUp");
    keyDown(items(element)[0], "ArrowUp");
    expect(document.activeElement).toBe(items(element)[2]);

    keyDown(items(element)[2], "Escape");
    expect(toggle(element).attr("aria-expanded")).toBe("false");
    expect(document.activeElement).toBe(toggle(element)[0]);
  });

  it("sends the preset of a date range quick filter as its value and an empty range", () => {
    const element = mount("date-range", WINDOWS, "Started");

    expect(text(toggle(element)[0])).toBe("Started: Any date");
    click(toggle(element));
    expect(items(element).map((index, item) => text(item)).get()).toEqual(["Any date", "Last 24 h", "Last 7 days", "DATE_RANGE_CUSTOM"]);
    click(items(element)[1]);

    expect(api().getData()).toEqual({CrtFlt: "24", "CrtFlt.from": null, "CrtFlt.to": null});
    expect(text(toggle(element)[0])).toBe("Started: Last 24 h");
    expect(filters().length).toBe(1);
  });

  it("applies a custom range: sends it as <id>.from and <id>.to with no preset, shows it and filters the grid", () => {
    const element = mount("date-range", WINDOWS, "Started");

    click(toggle(element));
    click(element.find(hook("grid-quick-filter-custom")));
    type(element.find(hook("date-range-from")), "01/11/2026");
    type(element.find(hook("date-range-to")), "02/11/2026");
    click(element.find(hook("grid-quick-filter-range-apply")));

    expect(api().getData()).toEqual({CrtFlt: null, "CrtFlt.from": "01/11/2026", "CrtFlt.to": "02/11/2026"});
    expect(text(toggle(element)[0])).toBe("Started: 01/11/2026 – 02/11/2026");
    expect(filters().length).toBe(1);

    // Clear goes back to any date
    click(toggle(element));
    expect(element.find(hook("grid-quick-filter-custom")).attr("aria-checked")).toBe("true");
    click(element.find(hook("grid-quick-filter-custom")));
    click(element.find(hook("grid-quick-filter-range-clear")));
    expect(api().getData()).toEqual({CrtFlt: "", "CrtFlt.from": null, "CrtFlt.to": null});
    expect(text(toggle(element)[0])).toBe("Started: Any date");
    expect(filters().length).toBe(2);
  });

  it("does not apply a custom range whose end is before its start", () => {
    const element = mount("date-range", WINDOWS, "Started");

    click(toggle(element));
    click(element.find(hook("grid-quick-filter-custom")));
    type(element.find(hook("date-range-from")), "05/11/2026");
    type(element.find(hook("date-range-to")), "02/11/2026");

    expect(element.find(hook("grid-quick-filter-range-apply")).prop("disabled")).toBe(true);
    expect(element.find(hook("date-range-to")).attr("aria-invalid")).toBe("true");
  });
});
