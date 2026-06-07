import "../../../../../main/resources/js/awe/app";
import "../../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../../main/resources/webpack/locals-es-ES.config";
import {DefaultSettings} from "../../../../../main/resources/js/awe/data/options";
import Highcharts from "highcharts/highstock";

describe("Chart", () => {
  let Chart;
  let $compile;
  let $httpBackend;
  let utilities;
  let chartEvents;
  let control;
  let settings;
  let $rootScope;

  beforeEach(() => {
    chartEvents = {mapActions: jest.fn()};
    control = {getAddressApi: jest.fn(() => ({})), getAddressModel: jest.fn(() => ({selected: {x: 1, y: 2}})), restoreInitialModelAttribute: jest.fn()};
    function ComponentMock(scope, id) {
      return {address: {view: "base", component: id}, asComponent: jest.fn(() => true), attributeMethods: {}, getMax: jest.fn(() => 3), id, listeners: {}, model: {selected: {}, values: [], zoom: {}}, scope, showContextMenu: jest.fn(), storeEvent: jest.fn()};
    }

    angular.mock.module("aweApplication", {ChartEvents: chartEvents, Component: ComponentMock, Control: control});
    inject(["$compile", "$httpBackend", "$injector", "$rootScope", (_$compile_, _$httpBackend_, $injector, _$rootScope_) => {
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $rootScope = _$rootScope_;
      Chart = $injector.get("Chart");
      utilities = $injector.get("AweUtilities");
      settings = $injector.get("AweSettings");
      jest.spyOn(utilities, "defineModelChangeListeners").mockImplementation(() => null);
      jest.spyOn(utilities, "timeout").mockImplementation(fn => fn());
      utilities.timeout.cancel = jest.fn();
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
  });

  function buildDirectiveScope(chartOptions) {
    const scope = $rootScope.$new();
    scope.chartOptions = chartOptions;
    scope.component = {
      chart: {container: {bind: jest.fn()}, id: "ChartWrapper"},
      initialized: false,
      initializeModel: jest.fn(),
      model: {values: []},
      onClick: jest.fn(),
      onRedraw: jest.fn(),
      onZoom: jest.fn(),
      translateLabels: jest.fn()
    };
    return scope;
  }

  function buildChartOptions(theme) {
    return {
      chart: {type: "line"},
      series: [{data: [1, 2, 3]}],
      theme,
      title: {text: "TITLE"},
      xAxis: [{}],
      yAxis: [{}]
    };
  }

  function setElementSize(element, width, height) {
    Object.defineProperty(element[0], "clientWidth", {configurable: true, get: () => width});
    Object.defineProperty(element[0], "clientHeight", {configurable: true, get: () => height});
  }

  function initializedChart(chartModel = {}) {
    const component = new Chart($rootScope.$new(), "chartId", {bind: jest.fn()});
    component.controller = {chartModel};
    component.asChart();
    component.chartInstance = {get: jest.fn(() => ({addPoint: jest.fn(), setData: jest.fn()})), redraw: jest.fn(), series: [], xAxis: [{setExtremes: jest.fn()}], yAxis: [{setExtremes: jest.fn()}], zoomOut: jest.fn()};
    return component;
  }

  it("changes zoom only when controller parameters include chartOptions.zoom", () => {
    const component = initializedChart({});
    jest.spyOn(component, "changeZoom");

    component.checkParametersChanged({chartOptions: {}});
    component.checkParametersChanged({chartOptions: {zoom: {x: {min: 1, max: 2}}}});

    expect(component.changeZoom).toHaveBeenCalledTimes(1);
    expect(component.changeZoom).toHaveBeenCalledWith({x: {min: 1, max: 2}});
  });

  it("adds points to each addressed chart series and redraws once", () => {
    const serie1 = {addPoint: jest.fn()};
    const serie2 = {addPoint: jest.fn()};
    const component = initializedChart({series: [{id: "serie1", xValue: "x", yValue: "y"}, {id: "serie2", xValue: "x", yValue: "y", zValue: "z"}]});
    component.chartInstance.get.mockImplementation(id => id === "serie1" ? serie1 : serie2);

    component.addPoints([{x: 10, y: 20, z: 30}]);

    expect(component.model.values).toEqual([{x: 10, y: 20, z: 30}]);
    expect(serie1.addPoint).toHaveBeenCalledWith([10, 20], false, false);
    expect(serie2.addPoint).toHaveBeenCalledWith([10, 20, 30], false, false);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(1);
  });

  it("updates axis extremes and resets zoom when requested extremes are null", () => {
    const xAxis = {setExtremes: jest.fn()};
    const yAxis = {setExtremes: jest.fn()};
    const component = initializedChart({});
    component.model.zoom = {x: {min: 1, max: 5}, y: {min: 2, max: 4}};
    component.chartInstance.xAxis = [xAxis];
    component.chartInstance.yAxis = [yAxis];

    component.changeZoom({x: {min: 3, max: 8}, y: {min: null, max: null}});

    expect(xAxis.setExtremes).toHaveBeenCalledWith(3, 8);
    expect(yAxis.setExtremes).toHaveBeenCalledWith(null, null);
    expect(component.chartInstance.zoomOut).toHaveBeenCalledTimes(1);
    expect(component.model.zoom).toEqual({x: {min: 3, max: 8}, y: {min: null, max: null}});
  });

  it("replaces, adds, removes, and resets series before redrawing the chart", () => {
    const component = initializedChart({series: [{id: "serie1"}, {name: "serie2"}]});
    const oldSeries = [{remove: jest.fn()}, {remove: jest.fn()}];
    const serie1 = {remove: jest.fn(), setData: jest.fn()};
    const serie2 = {setData: jest.fn()};
    component.chartInstance.series = oldSeries;
    component.chartInstance.addSeries = jest.fn();
    component.chartInstance.get.mockImplementation(id => ({serie1, serie2}[id] || null));

    component.addSeries([{id: "new1"}, {id: "new2"}]);
    component.replaceSeries([{id: "replacement"}]);
    component.removeSeries([{id: "serie1"}, {id: "missing"}]);
    component.resetChart();

    expect(component.chartInstance.addSeries).toHaveBeenCalledWith({id: "new1"}, false, true);
    expect(component.chartInstance.addSeries).toHaveBeenCalledWith({id: "new2"}, false, true);
    expect(oldSeries[1].remove).toHaveBeenCalledWith(false);
    expect(oldSeries[0].remove).toHaveBeenCalledWith(false);
    expect(component.chartInstance.addSeries).toHaveBeenCalledWith({id: "replacement"}, false, true);
    expect(serie1.remove).toHaveBeenCalledWith(false);
    expect(serie1.setData).toHaveBeenCalledWith([], false, true);
    expect(serie2.setData).toHaveBeenCalledWith([], false, true);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(4);
  });

  it("initializes series and drilldown data from model rows without mutating unrelated fields", () => {
    const chartModel = {
      series: [{id: "serie1", xValue: "name", yValue: "amount"}],
      drilldown: {series: [{id: "drill1", xValue: "name", yValue: "detail", drilldown: "children"}]},
      plotOptions: {pie: {allowPointSelect: true}}
    };
    const component = initializedChart(chartModel);
    component.model.values = [
      {name: "North", amount: 10, detail: 2, ignored: "metadata"},
      {name: "South", amount: 15, detail: 3}
    ];

    component.initializeModel(chartModel);

    expect(chartModel.series[0].data).toEqual([["North", 10], ["South", 15]]);
    expect(chartModel.drilldown.series[0].data).toEqual([
      {name: "North", y: 2, drilldown: "children"},
      {name: "South", y: 3, drilldown: "children"}
    ]);
    expect(chartModel.plotOptions.pie.allowPointSelect).toBe(false);
    expect(component.model.selected).toEqual({});
    expect(component.model.zoom).toEqual({});
  });

  it("updates initialized chart series from new model values and redraws when under the configured limit", () => {
    const chartModel = {series: [{id: "serie1", xValue: "x", yValue: "y"}], plotOptions: {}};
    const chartSerie = {setData: jest.fn()};
    const component = initializedChart(chartModel);
    component.initialized = true;
    component.chartInstance.get.mockReturnValue(chartSerie);
    component.chartInstance.hasData = jest.fn(() => true);
    component.chartInstance.hideNoData = jest.fn();
    component.chartInstance.showNoData = jest.fn();
    component.chartInstance.options = {lang: {tooMuchData: "Too much data"}};
    jest.spyOn(settings, "get").mockImplementation(key => key === "chartOptions" ? {limitPointsSerie: 5} : undefined);

    component.updateModel([{x: 1, y: 2}, {x: 3, y: 4}]);

    expect(component.model.values).toEqual([{x: 1, y: 2}, {x: 3, y: 4}]);
    expect(chartSerie.setData).toHaveBeenCalledWith([[1, 2], [3, 4]], false, true);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(1);
    expect(component.chartInstance.showNoData).not.toHaveBeenCalled();
  });

  it("stores click, double-click, context-menu, and zoom selections as chart model events", () => {
    const component = initializedChart({});
    const contextEvent = {type: "contextmenu"};
    const point = {x: "A", y: 9};

    component.onClick.call(point);
    component.onDblClick({}, {x: "B", y: 12});
    component.onContextMenu(contextEvent, {x: "C", y: 15});
    component.onZoom({xAxis: [{min: 1, max: 7}], yAxis: [{min: 2, max: 8}]});

    expect(component.storeEvent).toHaveBeenCalledWith("click");
    expect(component.storeEvent).toHaveBeenCalledWith("double-click");
    expect(component.storeEvent).toHaveBeenCalledWith("context-menu");
    expect(component.showContextMenu).toHaveBeenCalledWith(contextEvent);
    expect(component.model.selected).toEqual({x: "C", y: 15});
    expect(component.model.zoom).toEqual({y: {min: 2, max: 8}, x: {min: 1, max: 7}});
    expect(component.storeEvent).toHaveBeenCalledWith("zoom");
  });

  it("exposes selected point and printable SVG data through the chart address api", () => {
    const api = {};
    const component = initializedChart({});
    component.chartInstance.getSVG = jest.fn(() => "<svg></svg>");
    control.getAddressApi.mockReturnValue(api);
    control.getAddressModel.mockReturnValue({selected: {x: 21, y: 34}});

    component.asChart();

    expect(api.getData()).toEqual({"chartId.x": 21, "chartId.y": 34});
    expect(api.getPrintData("LANDSCAPE")).toEqual({chartId: {image: "<svg></svg>"}});
    expect(component.chartInstance.getSVG).toHaveBeenCalledWith({chart: {width: 1167, height: 360}});
  });

  it("creates stock charts and keeps the wrapper separate from the Highcharts instance", () => {
    const chartOptions = buildChartOptions("dark-unica");
    const chartInstance = {destroy: jest.fn(), redraw: jest.fn()};
    const scope = buildDirectiveScope({...chartOptions, stockChart: true});

    jest.spyOn(utilities, "isVisible").mockReturnValue(false);
    jest.spyOn(Highcharts, "stockChart").mockReturnValue(chartInstance);
    jest.spyOn(Highcharts, "chart");

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    expect(Highcharts.stockChart).toHaveBeenCalledTimes(1);
    expect(Highcharts.chart).not.toHaveBeenCalled();
    expect(scope.component.chart.id).toBe("ChartWrapper");
    expect(scope.component.chartInstance).toBe(chartInstance);
    expect(scope.component.initializeModel).toHaveBeenCalledWith(expect.objectContaining({stockChart: true}), expect.any(Object));
  });

  it("normalizes SVG URL references and clears renderer URL after chart creation", () => {
    const scope = buildDirectiveScope(buildChartOptions("dark-blue"));
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    const chartInstance = {destroy: jest.fn(), redraw: jest.fn(), renderer: {box: svg, url: "http://localhost/chart"}};

    rect.setAttribute("fill", "url(http://localhost/chart#highcharts-1)");
    path.setAttribute("clip-path", "url(http://localhost/chart#highcharts-2)");
    svg.appendChild(rect);
    svg.appendChild(path);
    jest.spyOn(utilities, "isVisible").mockReturnValue(false);
    jest.spyOn(Highcharts, "chart").mockReturnValue(chartInstance);

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    expect(chartInstance.renderer.url).toBe("");
    expect(rect.getAttribute("fill")).toBe("url(#highcharts-1)");
    expect(path.getAttribute("clip-path")).toBe("url(#highcharts-2)");
  });

  it("destroys the existing chart before reinitializing changed chart options", () => {
    const scope = buildDirectiveScope(buildChartOptions("dark-unica"));
    const existingChart = {destroy: jest.fn(), redraw: jest.fn()};
    const replacementChart = {destroy: jest.fn(), redraw: jest.fn()};
    let chartBuilds = 0;

    jest.spyOn(utilities, "isVisible").mockReturnValue(false);
    jest.spyOn(Highcharts, "chart").mockImplementation(() => chartBuilds++ === 0 ? existingChart : replacementChart);

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();
    scope.chartOptions = {...buildChartOptions("dark-blue"), title: {text: "UPDATED"}};
    scope.$digest();

    expect(existingChart.destroy).toHaveBeenCalledTimes(1);
    expect(Highcharts.chart).toHaveBeenCalledTimes(2);
    expect(scope.component.chartInstance).toBe(replacementChart);
  });

  it("updates language options by rebuilding the chart when language changes", () => {
    const scope = buildDirectiveScope(buildChartOptions("dark-unica"));
    const chartInstance = {destroy: jest.fn(), redraw: jest.fn()};

    jest.spyOn(utilities, "isVisible").mockReturnValue(false);
    jest.spyOn(Highcharts, "chart").mockReturnValue(chartInstance);
    jest.spyOn(Highcharts, "setOptions").mockImplementation(() => null);
    settings.update({language: "en-GB"});

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();
    scope.$broadcast("languageChanged");

    expect(chartInstance.destroy).toHaveBeenCalledTimes(1);
    expect(Highcharts.setOptions).toHaveBeenCalledWith({lang: global.HighchartsLocale["en-GB"]});
    expect(Highcharts.chart).toHaveBeenCalledTimes(2);
  });

  it("reflows only when resize events change visible chart dimensions", () => {
    const scope = buildDirectiveScope(buildChartOptions("dark-unica"));
    const chartInstance = {destroy: jest.fn(), redraw: jest.fn(), reflow: jest.fn()};

    jest.spyOn(utilities, "isVisible").mockReturnValue(true);
    jest.spyOn(Highcharts, "chart").mockReturnValue(chartInstance);

    const element = $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    setElementSize(element, 300, 200);
    scope.$digest();
    scope.$broadcast("resize-action");
    setElementSize(element, 450, 200);
    scope.$broadcast("resize");

    expect(chartInstance.reflow).toHaveBeenCalledTimes(1);
  });

  it("restores chart values through the Control service boundary", () => {
    const component = initializedChart({});

    component.restoreChart();

    expect(control.restoreInitialModelAttribute).toHaveBeenCalledWith(component.address, "values");
  });

  it("exposes selected and zoom attributes through chart attribute methods", () => {
    const component = initializedChart({});
    component.model.selected = {x: "North", y: 42};
    component.model.zoom = {x: {min: 1, max: 10}, y: {min: 2, max: 20}};

    expect(component.getAttribute("x")).toBe("North");
    expect(component.getAttribute("y")).toBe(42);
    expect(component.getAttribute("xMin")).toBe(1);
    expect(component.getAttribute("xMax")).toBe(10);
    expect(component.getAttribute("yMin")).toBe(2);
    expect(component.getAttribute("yMax")).toBe(20);
  });

  it("ignores add-points requests without point payloads", () => {
    const component = initializedChart({series: [{id: "serie1", xValue: "x", yValue: "y"}]});

    component.addPoints(null);

    expect(component.model.values).toEqual([]);
    expect(component.chartInstance.get).not.toHaveBeenCalled();
    expect(component.chartInstance.redraw).not.toHaveBeenCalled();
  });

  it("updates the model through the model-change listener path", () => {
    const chartSerie = {setData: jest.fn()};
    const component = initializedChart({series: [{id: "serie1", xValue: "x", yValue: "y"}], plotOptions: {}});
    component.initialized = true;
    component.chartInstance.get.mockReturnValue(chartSerie);
    component.chartInstance.hasData = jest.fn(() => true);
    component.chartInstance.hideNoData = jest.fn();
    component.chartInstance.showNoData = jest.fn();
    jest.spyOn(settings, "get").mockImplementation(key => key === "chartOptions" ? {limitPointsSerie: 10} : undefined);

    component.onModelChanged({values: [{x: 5, y: 8}]});

    expect(component.model.values).toEqual([{x: 5, y: 8}]);
    expect(chartSerie.setData).toHaveBeenCalledWith([[5, 8]], false, true);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(1);
  });

  it("does not update axis extremes when requested zoom matches the current model", () => {
    const component = initializedChart({});
    component.model.zoom = {x: {min: 1, max: 5}, y: {min: 2, max: 4}};

    component.changeZoom({x: {min: 1, max: 5}, y: {min: 2, max: 4}});

    expect(component.chartInstance.xAxis[0].setExtremes).not.toHaveBeenCalled();
    expect(component.chartInstance.yAxis[0].setExtremes).not.toHaveBeenCalled();
    expect(component.chartInstance.zoomOut).not.toHaveBeenCalled();
  });

  it("stores model values without redrawing before the chart is initialized", () => {
    const component = initializedChart({series: [{id: "serie1", xValue: "x", yValue: "y"}]});
    component.initialized = false;

    component.updateModel([{x: 1, y: 2}]);

    expect(component.model.values).toEqual([{x: 1, y: 2}]);
    expect(component.chartInstance.redraw).not.toHaveBeenCalled();
  });

  it("shows the configured no-data message when model updates exceed the series limit", () => {
    const component = initializedChart({series: [{id: "serie1", xValue: "x", yValue: "y"}], plotOptions: {}});
    component.initialized = true;
    component.chartInstance.get.mockReturnValue({setData: jest.fn()});
    component.chartInstance.hasData = jest.fn(() => false);
    component.chartInstance.hideNoData = jest.fn();
    component.chartInstance.showNoData = jest.fn();
    component.chartInstance.options = {lang: {tooMuchData: "Too much data"}};
    jest.spyOn(component, "resetChart");
    jest.spyOn(settings, "get").mockImplementation(key => key === "chartOptions" ? {limitPointsSerie: 1} : undefined);

    component.updateModel([{x: 1, y: 2}, {x: 3, y: 4}]);

    expect(component.resetChart).toHaveBeenCalled();
    expect(component.chartInstance.hideNoData).toHaveBeenCalled();
    expect(component.chartInstance.showNoData).toHaveBeenCalledWith("Too much data");
  });

  it("adds standalone series and redraws the chart once", () => {
    const component = initializedChart({series: []});
    component.chartInstance.addSeries = jest.fn();

    component.addSeries([{id: "north"}, {id: "south"}]);

    expect(component.chartInstance.addSeries).toHaveBeenCalledWith({id: "north"}, false, true);
    expect(component.chartInstance.addSeries).toHaveBeenCalledWith({id: "south"}, false, true);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(1);
  });

  it("replaces existing chart series before adding replacements", () => {
    const component = initializedChart({series: []});
    const first = {remove: jest.fn()};
    const second = {remove: jest.fn()};
    component.chartInstance.series = [first, second];
    component.chartInstance.addSeries = jest.fn();

    component.replaceSeries([{id: "replacement"}]);

    expect(second.remove).toHaveBeenCalledWith(false);
    expect(first.remove).toHaveBeenCalledWith(false);
    expect(component.chartInstance.addSeries).toHaveBeenCalledWith({id: "replacement"}, false, true);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(1);
  });

  it("removes only series found by identifier and redraws once", () => {
    const removable = {remove: jest.fn()};
    const component = initializedChart({series: []});
    component.chartInstance.get.mockImplementation(id => id === "north" ? removable : null);

    component.removeSeries([{id: "north"}, {id: "missing"}]);

    expect(removable.remove).toHaveBeenCalledWith(false);
    expect(component.chartInstance.redraw).toHaveBeenCalledTimes(1);
  });

  it("omits selected data fields that are not present in the selected chart model", () => {
    const api = {};
    const component = initializedChart({});
    control.getAddressApi.mockReturnValue(api);
    control.getAddressModel.mockReturnValue({selected: {x: "North"}});

    component.asChart();

    expect(api.getData()).toEqual({"chartId.x": "North"});
  });
});
