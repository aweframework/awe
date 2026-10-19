import fs from "fs";
import path from "path";
import "../../../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../../../main/resources/js/awe/data/options";
import {TestAttributes, TestIds} from "../../../../../main/resources/js/awe/data/testIds";

const TEMPLATE = fs.readFileSync(path.join(__dirname, "..", "..", "..", "..", "..", "main", "resources", "templates",
  "angular", "chart.html"), "utf8");

const LINE_MODEL = {
  title: {text: "TITLE"},
  legend: {show: true},
  xAxis: [{type: "value", awe: {axis: "x"}}],
  yAxis: [{type: "value", awe: {axis: "y"}}],
  dataZoom: [{type: "inside", xAxisIndex: 0}],
  series: [{id: "serie1", name: "Serie 1", type: "line", awe: {type: "line", xValue: "x", yValue: "y"}}]
};

// Inverted chart (horizontal bars): the AWE x axis is the vertical one, the points are drawn as [y, x]
const INVERTED_MODEL = {
  xAxis: [{type: "value", awe: {axis: "y"}}],
  yAxis: [{type: "value", awe: {axis: "x"}}],
  dataZoom: [{type: "inside", yAxisIndex: 0}],
  series: [{id: "serie1", name: "Serie 1", type: "bar", awe: {type: "bar", xValue: "x", yValue: "y"}}],
  awe: {inverted: true}
};

const PIE_MODEL = {
  series: [{id: "themes", name: "Themes", type: "pie", awe: {type: "pie", xValue: "name", yValue: "amount", drilldown: "detail"}}],
  awe: {
    chartType: "pie",
    drilldown: {series: [{id: "detail", name: "Detail", type: "pie", awe: {type: "pie", xValue: "name", yValue: "detail"}}]}
  }
};

const ROWS = [{x: 1, y: 10}, {x: 2, y: 20}, {x: 3, y: 30}, {x: 4, y: 40}, {x: 5, y: 50}];
const PIE_ROWS = [{name: "North", amount: 10, detail: 2}, {name: "South", amount: 15, detail: 3}];
const flush = () => new Promise(resolve => setTimeout(resolve, 50));
const copy = (value) => JSON.parse(JSON.stringify(value));

describe("Chart (ECharts)", () => {
  let $compile;
  let $rootScope;
  let $templateCache;
  let serverData;
  let settings;
  let control;
  let chartEvents;
  let controller;
  let currentSettings;
  let rows;

  beforeEach(() => {
    // jsdom has no canvas, which ECharts uses to measure the texts
    jest.spyOn(window.HTMLCanvasElement.prototype, "getContext").mockImplementation(() => ({
      font: "",
      measureText: (text) => ({width: String(text).length * 7})
    }));
    controller = {echartsModel: copy(LINE_MODEL)};
    rows = copy(ROWS);
    currentSettings = {language: "en-GB", chartOptions: {limitPointsSerie: 1000000}};
    chartEvents = {mapActions: jest.fn()};
    control = {
      getAddressApi: jest.fn(() => ({})),
      getAddressModel: jest.fn(() => ({selected: {}})),
      restoreInitialModelAttribute: jest.fn()
    };
    function ComponentMock(scope, id) {
      const component = {
        address: {view: "base", component: id}, attributeMethods: {}, controller, id, listeners: {},
        model: {selected: {}, values: rows, zoom: {}}, scope,
        asComponent: jest.fn(() => true), getMax: jest.fn(() => 6), showContextMenu: jest.fn(),
        hideContextMenu: jest.fn(), storeEvent: jest.fn()
      };
      scope.component = component;
      scope.controller = controller;
      return component;
    }

    angular.mock.module("aweApplication", {ChartEvents: chartEvents, Component: ComponentMock, Control: control});
    inject(["$compile", "$httpBackend", "$injector", "$rootScope", "$templateCache",
      (_$compile_, $httpBackend, $injector, _$rootScope_, _$templateCache_) => {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $templateCache = _$templateCache_;
        serverData = $injector.get("ServerData");
        settings = $injector.get("AweSettings");
        jest.spyOn(settings, "get").mockImplementation(key => currentSettings[key]);
        jest.spyOn($injector.get("AweUtilities"), "isVisible").mockReturnValue(true);
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
      }]);
    document.body.style.backgroundColor = "";
  });

  // jsdom has no layout: the chart element measures what the test says
  let chartSize;
  const measured = (node, side) => node.classList?.contains("chart-container") ? chartSize[side] : 0;
  beforeEach(() => {
    chartSize = {width: 600, height: 300};
    jest.spyOn(window.HTMLElement.prototype, "clientWidth", "get").mockImplementation(function () {
      return measured(this, "width");
    });
    jest.spyOn(window.HTMLElement.prototype, "clientHeight", "get").mockImplementation(function () {
      return measured(this, "height");
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  /**
   * Compile the chart directive (template and ECharts plugin) as a screen does
   */
  function compileChart({model, values} = {}) {
    if (model) {
      controller.echartsModel = copy(model);
    }
    if (values) {
      rows = values;
    }
    $templateCache.put(serverData.getAngularTemplateUrl("chart"), TEMPLATE);
    const element = $compile('<awe-chart chart-id="chr"></awe-chart>')($rootScope.$new());
    document.body.appendChild(element[0]);
    $rootScope.$digest();
    const scope = element.isolateScope();
    const component = scope.component;
    return {element, component, chart: component.chartInstance, scope};
  }

  const seriesData = (chart, index = 0) => chart.getOption().series[index].data;
  const texts = (chart) => (chart.getOption().graphic || []).flatMap(graphic => graphic.elements || [])
    .map(element => element.style?.text);

  it("draws the server echartsModel with the component values as an SVG chart", () => {
    const {element, chart} = compileChart();

    expect(element[0].querySelector(".chart-container svg")).not.toBeNull();
    expect(chart.getOption().title[0].text).toBe("TITLE");
    expect(seriesData(chart)).toEqual([[1, 10], [2, 20], [3, 30], [4, 40], [5, 50]]);
  });

  it("exposes the browser test hooks: chart-id, data-testid, data-rendered and data-zoomed", async () => {
    const {element, chart, component} = compileChart();
    const root = element[0];

    expect(root.getAttribute("chart-id")).toBe("chr");
    expect(root.getAttribute(TestAttributes.testId)).toBe(TestIds.chart);
    expect(root.getAttribute(TestAttributes.zoomed)).toBe("false");

    await flush();
    $rootScope.$digest();
    expect(root.getAttribute(TestAttributes.rendered)).toBe("true");

    chart.dispatchAction({type: "dataZoom", dataZoomIndex: 0, startValue: 2, endValue: 4});
    $rootScope.$digest();
    expect(component.isZoomed()).toBe(true);
    expect(root.getAttribute(TestAttributes.zoomed)).toBe("true");
  });

  it("stores the clicked point and raises the click event", () => {
    const {chart, component} = compileChart();

    chart.trigger("click", {componentType: "series", seriesId: "serie1", value: [3, 30]});

    expect(component.model.selected).toEqual({x: 3, y: 30});
    expect(component.getAttribute("x")).toBe(3);
    expect(component.getAttribute("y")).toBe(30);
    expect(component.storeEvent).toHaveBeenCalledWith("click");
  });

  it("stores the double clicked point and raises the double-click event", () => {
    const {chart, component} = compileChart();

    chart.trigger("dblclick", {componentType: "series", seriesId: "serie1", value: [4, 40]});

    expect(component.model.selected).toEqual({x: 4, y: 40});
    expect(component.storeEvent).toHaveBeenCalledWith("double-click");
  });

  it("stores the point of the context menu, raises the context-menu event and cancels the browser menu", () => {
    const {chart, component} = compileChart({model: PIE_MODEL, values: copy(PIE_ROWS)});
    const nativeEvent = {preventDefault: jest.fn()};

    chart.trigger("contextmenu", {componentType: "series", seriesId: "themes", name: "South", value: 15,
      event: {event: nativeEvent}});

    expect(component.model.selected).toEqual({x: "South", y: 15});
    expect(component.storeEvent).toHaveBeenCalledWith("context-menu");
    expect(nativeEvent.preventDefault).toHaveBeenCalled();
  });

  it("opens the drilldown series on click and goes back with the back control", () => {
    const {chart, component} = compileChart({model: PIE_MODEL, values: copy(PIE_ROWS)});

    chart.trigger("click", {componentType: "series", seriesId: "themes", name: "North", value: 10});

    expect(component.storeEvent).toHaveBeenCalledWith("click");
    expect(component.chartInstance.getOption().series[0].id).toBe("detail");
    expect(seriesData(component.chartInstance)).toEqual([{name: "North", value: 2}, {name: "South", value: 3}]);
    expect(texts(component.chartInstance)).toContain("Back to Themes");

    component.drillUp();

    expect(component.chartInstance.getOption().series[0].id).toBe("themes");
    expect(texts(component.chartInstance)).not.toContain("Back to Themes");
  });

  it("stores the zoom of the user in xMin, xMax, yMin and yMax and raises the zoom event", () => {
    const {chart, component} = compileChart();

    chart.dispatchAction({type: "dataZoom", dataZoomIndex: 0, startValue: 2, endValue: 4});

    expect(component.storeEvent).toHaveBeenCalledWith("zoom");
    expect(component.getAttribute("xMin")).toBe(2);
    expect(component.getAttribute("xMax")).toBe(4);
    expect(component.getAttribute("yMin")).toBeNull();
    expect(component.getAttribute("yMax")).toBeNull();
  });

  it("zooms to the chart-options of a dependency (JSON text) without raising the zoom event", () => {
    const {component} = compileChart();

    component.checkParametersChanged({chartOptions: JSON.stringify({zoom: {x: {min: 2, max: 3}}})});

    const dataZoom = component.chartInstance.getOption().dataZoom[0];
    expect([dataZoom.startValue, dataZoom.endValue]).toEqual([2, 3]);
    expect(component.getAttribute("xMin")).toBe(2);
    expect(component.getAttribute("xMax")).toBe(3);
    expect(component.storeEvent).not.toHaveBeenCalled();

    component.checkParametersChanged({chartOptions: {zoom: {x: {min: null, max: null}}}});

    const reset = component.chartInstance.getOption().dataZoom[0];
    expect([reset.start, reset.end]).toEqual([0, 100]);
    expect(component.isZoomed()).toBe(false);
  });

  it("reads the AWE x and y of the points and the zoom of an inverted chart", () => {
    const {chart, component} = compileChart({model: INVERTED_MODEL});

    chart.trigger("click", {componentType: "series", seriesId: "serie1", value: [30, 3]});
    chart.dispatchAction({type: "dataZoom", dataZoomIndex: 0, startValue: 2, endValue: 4});

    expect(component.model.selected).toEqual({x: 3, y: 30});
    expect(component.getAttribute("xMin")).toBe(2);
    expect(component.getAttribute("xMax")).toBe(4);
    expect(component.getAttribute("yMin")).toBeNull();
    expect(component.getAttribute("yMax")).toBeNull();
  });

  it("zooms the vertical axis of an inverted chart to the chart-options x range", () => {
    const {component} = compileChart({model: INVERTED_MODEL});

    component.checkParametersChanged({chartOptions: {zoom: {x: {min: 2, max: 3}}}});

    const dataZoom = component.chartInstance.getOption().dataZoom[0];
    expect([dataZoom.startValue, dataZoom.endValue]).toEqual([2, 3]);
    expect(component.getAttribute("xMin")).toBe(2);
    expect(component.getAttribute("xMax")).toBe(3);
  });

  it("keeps the zoom when the chart is drawn again", () => {
    const {chart, component} = compileChart();
    chart.dispatchAction({type: "dataZoom", dataZoomIndex: 0, startValue: 2, endValue: 4});

    component.onModelChanged({values: copy(ROWS)});

    const dataZoom = chart.getOption().dataZoom[0];
    expect([dataZoom.startValue, dataZoom.endValue]).toEqual([2, 4]);
  });

  it("draws the chart again when the model values change", () => {
    const {chart, component} = compileChart();

    component.onModelChanged({values: [{x: 7, y: 70}]});

    expect(component.model.values).toEqual([{x: 7, y: 70}]);
    expect(seriesData(chart)).toEqual([[7, 70]]);
  });

  it("shows the too much data message instead of the series over the limitPointsSerie setting", () => {
    currentSettings.chartOptions = {limitPointsSerie: 3};
    const {chart} = compileChart();

    expect(seriesData(chart)).toEqual([]);
    expect(texts(chart)).toContain("Too much data to display");
  });

  it("adds the points of add-points and removes the oldest ones over the chart max", () => {
    const {chart, component} = compileChart();

    component.addPoints([{x: 6, y: 60}, {x: 7, y: 70}]);

    expect(component.model.values.map(row => row.x)).toEqual([2, 3, 4, 5, 6, 7]);
    expect(seriesData(chart).map(point => point[0])).toEqual([2, 3, 4, 5, 6, 7]);

    component.addPoints(null);
    expect(component.model.values).toHaveLength(6);
  });

  it("adds the series of add-chart-series with their points", () => {
    const {chart, component} = compileChart();

    component.addSeries([{id: "serie2", name: "Serie 2", type: "column", xValue: "x", yValue: "z",
      data: [[1, 5], [2, 6], [3, 7], [4, 8], [5, 9]]}]);

    const series = chart.getOption().series;
    expect(series.map(serie => [serie.id, serie.type])).toEqual([["serie1", "line"], ["serie2", "bar"]]);
    expect(series[1].data).toEqual([[1, 5], [2, 6], [3, 7], [4, 8], [5, 9]]);
  });

  it("adds and replaces the series that a Java service builds with their points only (no xValue nor yValue)", () => {
    const {chart, component} = compileChart();
    const serie = (id) => ({id, name: id, echarts: {id, type: "line", awe: {type: "line"}}, data: [[1, 3], [2, 4]]});

    component.addSeries([serie("donald")]);

    expect(chart.getOption().series.map(drawn => drawn.name)).toEqual(["Serie 1", "donald"]);
    // The chart had 5 rows: the rows without a point of the new series are gaps
    expect(chart.getOption().series[1].data).toEqual([[1, 3], [2, 4], [null, null], [null, null], [null, null]]);

    component.replaceSeries([serie("test")]);

    expect(chart.getOption().series.map(drawn => drawn.name)).toEqual(["test"]);
    expect(chart.getOption().series[0].data).toEqual([[1, 3], [2, 4]]);
  });

  it("uses the ECharts translation that the server sends with the series of a chart action", () => {
    const {chart, component} = compileChart();

    component.addSeries([{id: "serie2", name: "Serie 2", type: "spline", xValue: "x", yValue: "z", data: [[1, 5]],
      echarts: {id: "serie2", name: "Serie 2", type: "line", smooth: true, awe: {type: "spline", xValue: "x", yValue: "z"}}}]);

    expect(chart.getOption().series[1].smooth).toBe(true);
  });

  it("replaces the series and the values of the chart with replace-chart-series", () => {
    const {chart, component} = compileChart();

    component.replaceSeries([{id: "other", name: "Other", type: "line", xValue: "a", yValue: "b", data: [[1, 2], [3, 4]]}]);

    expect(chart.getOption().series.map(serie => serie.id)).toEqual(["other"]);
    expect(component.model.values).toEqual([{a: 1, b: 2}, {a: 3, b: 4}]);
    expect(seriesData(chart)).toEqual([[1, 2], [3, 4]]);
  });

  it("removes the series of remove-chart-series by identifier", () => {
    const {chart, component} = compileChart();
    component.addSeries([{id: "serie2", name: "Serie 2", type: "line", xValue: "x", yValue: "z", data: []}]);

    component.removeSeries([{id: "serie1"}, {id: "missing"}]);

    expect(chart.getOption().series.map(serie => serie.id)).toEqual(["serie2"]);
  });

  it("removes the points of the chart on reset", () => {
    const {chart, component} = compileChart();

    component.resetChart();

    expect(component.model.values).toEqual([]);
    expect(seriesData(chart)).toEqual([]);
    expect(texts(chart)).toContain("No data to display");
  });

  it("restores the initial values through the Control service on restore", () => {
    const {component} = compileChart();

    component.restoreChart();

    expect(control.restoreInitialModelAttribute).toHaveBeenCalledWith(component.address, "values");
  });

  it("resizes and draws the chart again only when the size of its element changes", () => {
    const {chart, scope} = compileChart();
    const resize = jest.spyOn(chart, "resize");

    scope.$broadcast("resize");
    expect(resize).not.toHaveBeenCalled();

    chartSize = {width: 900, height: 400};
    scope.$broadcast("resize-action");
    expect(resize).toHaveBeenCalledTimes(1);
  });

  it("creates the chart again with the texts of the new language when the language changes", () => {
    const {chart, component, scope} = compileChart({values: []});
    expect(texts(chart)).toContain("No data to display");

    currentSettings.language = "es-ES";
    scope.$broadcast("languageChanged");

    expect(chart.isDisposed()).toBe(true);
    expect(component.chartInstance).not.toBe(chart);
    expect(texts(component.chartInstance)).toContain("No hay datos a mostrar");
  });

  it("draws in dark mode when the application theme is dark", () => {
    document.body.style.backgroundColor = "rgb(20, 20, 20)";
    const {chart} = compileChart({values: []});

    const noData = chart.getOption().graphic[0].elements[0];
    expect(noData.style.fill).toBe("#dddddd");
  });

  it("disposes the chart when the component is destroyed", () => {
    const {chart, scope} = compileChart();

    scope.$destroy();

    expect(chart.isDisposed()).toBe(true);
  });

  it("returns the selected point as the chart data for the server", () => {
    const api = {};
    control.getAddressApi.mockReturnValue(api);
    control.getAddressModel.mockReturnValue({selected: {x: 21, y: 34}});
    compileChart();

    expect(api.getData()).toEqual({"chr.x": 21, "chr.y": 34});

    control.getAddressModel.mockReturnValue({selected: {x: "North"}});
    expect(api.getData()).toEqual({"chr.x": "North"});
  });

  it.each([
    ["LANDSCAPE", 1167, 360],
    ["PORTRAIT", 796, 540]
  ])("prints the chart as an SVG image of the %s page size", (orientation, width, height) => {
    const api = {};
    control.getAddressApi.mockReturnValue(api);
    compileChart();

    const {chr} = api.getPrintData(orientation);

    expect(chr.image).toMatch(/^<svg/);
    expect(chr.image).toContain(`width="${width}"`);
    expect(chr.image).toContain(`height="${height}"`);
    expect(chr.image).toContain("TITLE");
  });

  it("hides and shows the linked series with the series they are linked to", () => {
    const model = copy(LINE_MODEL);
    model.series.push({id: "linked", name: "Linked", type: "line", awe: {type: "line", xValue: "x", yValue: "y", linkedTo: "serie1"}});
    const {chart} = compileChart({model});
    const dispatch = jest.spyOn(chart, "dispatchAction");

    chart.trigger("legendselectchanged", {name: "Serie 1", selected: {"Serie 1": false}});

    expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({type: "legendUnSelect", name: "Linked"}));
  });
});
