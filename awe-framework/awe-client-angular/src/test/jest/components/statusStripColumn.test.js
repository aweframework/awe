import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe("awe-framework/awe-client-angular/src/test/jest/components/statusStripColumn.test.js", function () {
  let $rootScope, $compile, $control, gridComponent, cellModel, cellController;
  const address = {component: "grid", view: "report", row: "1", column: "history"};
  const ITEMS = [
    {tone: "success", label: "OK"},
    {tone: "warning", label: "Aviso"},
    {tone: "success", label: "OK"},
    {tone: "danger", label: "Error"}
  ];

  /**
   * Compile a status strip cell
   * @return {Object} Compiled element
   */
  function compileCell() {
    const parent = angular.element("<div id=\"scope-grid\"></div>");
    angular.element(document.body).append(parent);
    parent.data("$scope", Object.assign($rootScope.$new(), {component: gridComponent}));
    const scope = Object.assign($rootScope.$new(), {view: "report", context: "screen", cellAddress: JSON.stringify(address)});
    const element = $compile("<awe-column-status-strip cell-address='{{cellAddress}}'></awe-column-status-strip>")(scope);
    $rootScope.$digest();
    return element;
  }

  /**
   * Model of a cell as the grid builds it for a value
   * @param {*} value Cell value
   * @return {Object} Cell model
   */
  const modelOf = value => ({selected: value, values: [{value, label: value}]});

  const drawnItems = element => element.find(".awe-status-strip-item").toArray()
    .map(item => ({tone: item.getAttribute("data-tone"), tooltip: item.getAttribute("data-tooltip")}));

  /**
   * Hover a bar and let its tooltip show
   * @param {Object} element Compiled cell
   * @param {number} index Bar position
   * @return {string|undefined} Text of the visible tooltip
   */
  const hoverBar = (element, index) => {
    element.find(".awe-status-strip-item")[index].dispatchEvent(new MouseEvent("mouseover", {bubbles: true}));
    jest.runOnlyPendingTimers();
    return document.querySelector("body > .tooltip[role=tooltip] .tooltip-inner")?.textContent;
  };

  beforeEach(function () {
    angular.mock.module("aweApplication");

    inject(["$rootScope", "$compile", "$httpBackend", "Control", function (_$rootScope_, _$compile_, _$httpBackend_, _Control_) {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $control = _Control_;
      _$httpBackend_.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);

    cellModel = modelOf(JSON.stringify(ITEMS));
    cellController = {align: "left", visible: true, dependencies: []};
    gridComponent = {
      model: {selected: []},
      scope: {onSaveRow: jest.fn()},
      checkInitialized: jest.fn(() => true),
      getModel: jest.fn(() => cellModel),
      getController: jest.fn(() => cellController),
      updateCellModel: jest.fn()
    };
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    jest.spyOn($control, "getAddressModel").mockImplementation(() => cellModel);
    jest.spyOn($control, "getAddressController").mockImplementation(() => cellController);
    jest.spyOn($control, "setAddressApi").mockImplementation(() => undefined);
  });

  afterEach(function () {
    angular.element(document.body).empty();
    jest.useRealTimers();
  });

  it("draws one item per status with its tone and its label as the tooltip, and names the strip with the summary", function () {
    const element = compileCell();

    const strip = element.find(".awe-status-strip");
    expect(strip.attr("role")).toBe("img");
    expect(strip.attr("aria-label")).toBe("2 OK, 1 Aviso, 1 Error");
    expect(drawnItems(element)).toEqual(ITEMS.map(item => ({tone: item.tone, tooltip: item.label})));
    expect(strip.find("[title]")).toHaveLength(0);
  });

  it("shows the label and the title of the hovered bar in a tooltip, and the summary keeps the labels", function () {
    jest.useFakeTimers();
    cellModel = modelOf(JSON.stringify([
      {tone: "success", label: "OK", title: "#4807 · 17/11/2026 08:05 · 1 min 12 s"},
      {tone: "danger", label: "Error"}
    ]));
    const element = compileCell();

    expect(hoverBar(element, 0)).toBe("OK\n#4807 · 17/11/2026 08:05 · 1 min 12 s");
    expect(hoverBar(element, 1)).toBe("Error");
    expect(document.querySelectorAll("body > .tooltip[role=tooltip]")).toHaveLength(1);
    expect(element.find(".awe-status-strip").attr("aria-label")).toBe("1 OK, 1 Error");

    element.find(".awe-status-strip")[0].dispatchEvent(new MouseEvent("mouseleave"));
    expect(document.querySelector("body > .tooltip[role=tooltip]")).toBeNull();
  });

  it("draws an already parsed list sent as the value of the cell", function () {
    cellModel = modelOf(ITEMS);
    const element = compileCell();

    expect(drawnItems(element)).toHaveLength(4);
    expect(element.find(".awe-status-strip").attr("aria-label")).toBe("2 OK, 1 Aviso, 1 Error");
  });

  it("leaves the cell empty when the value is not a list of statuses", function () {
    [null, "", "oops", "[]", "[{\"tone\":\"success\"}]"].forEach(value => {
      cellModel = modelOf(value);
      const element = compileCell();
      expect(element.hasClass("column-status-strip")).toBe(true);
      expect(element.find(".awe-status-strip")).toHaveLength(0);
    });
  });
});
