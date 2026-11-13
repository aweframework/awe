import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {buildSparkline, sparklineMarkup} from "../../../main/resources/js/awe/services/chart/sparklineRenderer";

describe("awe-framework/awe-client-angular/src/test/jest/components/sparklineColumn.test.js", function () {
  let $rootScope, $compile, $control, gridComponent, cellModel, cellController;
  const address = {component: "grid", view: "report", row: "1", column: "trend"};

  /**
   * Parse the markup of the renderer as the browser does, to compare it with what the cell drew
   * @param {string} markup SVG markup
   * @return {string} Serialized SVG
   */
  function parsed(markup) {
    const holder = document.createElement("div");
    holder.innerHTML = markup;
    return holder.firstChild.outerHTML;
  }

  /**
   * Compile a sparkline cell
   * @param {string} cellAddress Address of the cell
   * @return {Object} Compiled element
   */
  function compileCell(cellAddress = address) {
    const parent = angular.element("<div id=\"scope-grid\"></div>");
    angular.element(document.body).append(parent);
    parent.data("$scope", Object.assign($rootScope.$new(), {component: gridComponent}));
    const scope = Object.assign($rootScope.$new(), {view: "report", context: "screen", cellAddress: JSON.stringify(cellAddress)});
    const element = $compile("<awe-column-sparkline cell-address='{{cellAddress}}'></awe-column-sparkline>")(scope);
    $rootScope.$digest();
    element.testScope = scope;
    return element;
  }

  beforeEach(function () {
    angular.mock.module("aweApplication");

    inject(["$rootScope", "$compile", "$httpBackend", "Control", function (_$rootScope_, _$compile_, _$httpBackend_, _Control_) {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $control = _Control_;
      _$httpBackend_.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);

    cellModel = {selected: "[3, 5, -1, 8]", values: [{value: "[3, 5, -1, 8]", label: "[3, 5, -1, 8]"}]};
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
  });

  it("draws the series of the cell as the shared SVG, with the tooltip of every point", function () {
    const element = compileCell();

    const svg = element.find("svg.awe-sparkline");
    expect(svg).toHaveLength(1);
    expect(svg[0].getAttribute("viewBox")).toBe("0 0 100 24");
    expect(svg.attr("aria-label")).toBe("3, 5, -1, 8");
    expect(svg.find("title").toArray().map(title => title.textContent)).toEqual(["3", "5", "-1", "8"]);
    expect(svg[0].outerHTML).toBe(parsed(sparklineMarkup(buildSparkline("[3, 5, -1, 8]"))));
  });

  it("draws an already parsed series sent as the value of the cell", function () {
    cellModel = {selected: [3, 5, -1], values: []};
    const element = compileCell();

    expect(element.find("svg.awe-sparkline title")).toHaveLength(3);
  });

  it("applies the options of the column", function () {
    Object.assign(cellController, {
      sparklineType: "bar", sparklineColor: "#112233", sparklineNegativeColor: "#aa0000", sparklineLastPoint: true, sparklineMinMax: true
    });
    const element = compileCell();

    const options = {type: "bar", color: "#112233", negativeColor: "#aa0000", lastPoint: true, minMax: true};
    expect(element.find("svg")[0].outerHTML).toBe(parsed(sparklineMarkup(buildSparkline("[3, 5, -1, 8]", options))));
    expect(element.find("svg rect[fill='#aa0000']")).toHaveLength(1);
  });

  it("leaves the cell empty when the value is not a series", function () {
    [null, "", "oops", "[]", "[1, \"a\"]"].forEach(value => {
      cellModel = {selected: value, values: [{value, label: value}]};
      const element = compileCell();
      expect(element.find("svg")).toHaveLength(0);
      expect(element.hasClass("column-sparkline")).toBe(true);
    });
  });

  it("draws a single value as one dot", function () {
    cellModel = {selected: "[7]", values: [{value: "[7]", label: "[7]"}]};
    const element = compileCell();

    expect(element.find("svg circle")).toHaveLength(1);
  });

  it("is not focusable, so the keyboard navigation of the grid is untouched", function () {
    const element = compileCell();

    expect(element.find("svg").attr("focusable")).toBe("false");
    expect(element.find("[tabindex]")).toHaveLength(0);
    expect(element.attr("tabindex")).toBeUndefined();
  });

  it("draws the new series when the grid reuses the cell for another row", function () {
    const element = compileCell();
    expect(element.find("svg title")).toHaveLength(4);

    cellModel = {selected: "[1, 2]", values: [{value: "[1, 2]", label: "[1, 2]"}]};
    // The grid changes the row of the interpolated attribute, which notifies the observers of the cell
    element.testScope.cellAddress = JSON.stringify({...address, row: "2"});
    $rootScope.$digest();

    expect(element.find("svg title")).toHaveLength(2);
  });

  it("draws a page of 100 rows with 12 points each", function () {
    const series = JSON.stringify(Array.from({length: 12}, (_, index) => Math.round(Math.sin(index) * 50)));
    cellModel = {selected: series, values: [{value: series, label: series}]};
    const elements = Array.from({length: 100}, (_, row) => compileCell({...address, row: String(row)}));

    expect(elements.every(element => element.find("svg.awe-sparkline").length === 1)).toBe(true);
  });
});
