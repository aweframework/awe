import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";
import Highcharts from "highcharts/highstock";

describe('awe-framework/awe-client-angular/src/test/js/services/chart/chart.js', function () {
  let $injector, Chart, $rootScope, $compile, $httpBackend, Utilities, Control, Storage;
  function buildDirectiveScope(chartOptions) {
    let scope = $rootScope.$new();
    scope.chartOptions = chartOptions;
    scope.component = {
      initialized: false,
      model: {values: []},
      initializeModel: jest.fn().mockName('initializeModel'),
      translateLabels: jest.fn().mockName('translateLabels'),
      onZoom: jest.fn().mockName('onZoom'),
      onRedraw: jest.fn().mockName('onRedraw'),
      onClick: jest.fn().mockName('onClick')
    };
    return scope;
  }

  function buildDirectiveScopeWithWrapper(chartOptions) {
    let scope = buildDirectiveScope(chartOptions);
    scope.component.chart = {
      container: {
        bind: jest.fn().mockName('bind')
      },
      scope: scope,
      id: 'ChartWrapper'
    };
    return scope;
  }

  function buildChartOptions(theme) {
    return {
      chart: {
        type: 'line'
      },
      theme: theme,
      title: {
        text: 'TITLE'
      },
      xAxis: [{}],
      yAxis: [{}],
      series: [{
        data: [1, 2, 3]
      }]
    };
  }

  function buildChartScope(id, chartModel, values) {
    let scope = $rootScope.$new();
    scope.view = 'testView';
    scope.context = {};
    Control.setAddressController({view: 'testView', component: id}, chartModel || {});
    Control.setAddressModel({view: 'testView', component: id}, _.merge({selected: null, values: []}, values || {}));
    return scope;
  }

  function setElementSize(element, width, height) {
    Object.defineProperty(element[0], 'clientWidth', {
      configurable: true,
      get: function () {
        return width;
      }
    });
    Object.defineProperty(element[0], 'clientHeight', {
      configurable: true,
      get: function () {
        return height;
      }
    });
  }

  function configureChartComponent(chart, id, chartModel, model) {
    chart.address = {view: 'testView', component: id};
    chart.controller = {chartModel: chartModel || {}};
    chart.model = _.merge({selected: null, values: []}, model || {});
    chart.listeners = {};
    chart.element = {bind: jest.fn().mockName('bind')};
    Control.setAddressApi(chart.address, {});
    chart.api = Control.getAddressApi(chart.address);
  }

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", "$httpBackend", function (__$injector__, _$httpBackend_) {
      $injector = __$injector__;
      $httpBackend = _$httpBackend_;
      $rootScope = $injector.get('$rootScope');
      $compile = $injector.get('$compile');
      Chart = $injector.get('Chart');
      Utilities = $injector.get('AweUtilities');
      Control = $injector.get('Control');
      Storage = $injector.get('Storage');

      $httpBackend.when('POST', 'settings').respond(DefaultSettings);
    }]);

    Storage.init();
    Storage.put('model', {});
    Storage.put('controller', {});
    Storage.put('api', {});

    jest.setTimeout(10000);
  });

  afterEach(function () {  });

  it('should reinitialize dark themed charts from a clean merge target', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-blue'));
    let mergeTargets = [];
    let merge = _.merge;
    let chartSpy = jest.fn().mockName('chart').mockReturnValue({
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    });

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockImplementation(chartSpy);
    jest.spyOn(_, 'merge').mockImplementation(function (target) {
      mergeTargets.push(_.cloneDeep(target));
      return merge.apply(_, arguments);
    });

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    mergeTargets = [];
    scope.chartOptions = buildChartOptions('dark-blue');
    scope.$digest();

    expect(mergeTargets[0]).toEqual({});
  });

  it('should not mutate the original chart options when applying a theme', function () {
    let chartOptions = buildChartOptions('dark-unica');
    let scope = buildDirectiveScope(chartOptions);

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue({
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    });

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    expect(chartOptions.chart.backgroundColor).toBeUndefined();
    expect(chartOptions.xAxis[0].gridLineColor).toBeUndefined();
    expect(chartOptions.yAxis[0].gridLineColor).toBeUndefined();
  });

  it('should not mutate the original chart options when initializing series data', function () {
    let chartOptions = buildChartOptions('dark-unica');
    let scope = buildDirectiveScope(chartOptions);

    scope.component.initializeModel.mockImplementation(function (options) {
      _.each(options.series, function (serie) {
        serie.data = [];
      });
    });

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue({
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    });

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    expect(chartOptions.series[0].data).toEqual([1, 2, 3]);
  });

  it('should destroy initialized charts when the directive scope is destroyed', function () {
    let chart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chart);

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    scope.$destroy();

    expect(chart.destroy).toHaveBeenCalledTimes(1);
  });

  it('should not fail destroying charts before initialization completes', function () {
    let scope = buildDirectiveScope(undefined);

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue({
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    });

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);

    expect(function () {
      scope.$destroy();
    }).not.toThrow();
  });

  it('should keep the chart wrapper separate from the Highcharts instance', function () {
    let wrapper = {
      container: {
        bind: jest.fn().mockName('bind')
      },
      id: 'ChartWrapper'
    };
    let chartInstance = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };
    let scope = buildDirectiveScopeWithWrapper(buildChartOptions('dark-unica'));

    scope.component.chart = wrapper;

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chartInstance);

    expect(function () {
      $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
      scope.$digest();
    }).not.toThrow();
    expect(scope.component.chart).toBe(wrapper);
    expect(scope.component.chartInstance).toBe(chartInstance);
  });

  it('should destroy an existing chart before reinitializing in the same scope', function () {
    let existingChart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };
    let replacementChart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValueOnce(existingChart).mockReturnValueOnce(replacementChart);

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    scope.chartOptions = buildChartOptions('dark-unica');
    scope.$digest();

    expect(existingChart.destroy).toHaveBeenCalledTimes(1);
    expect(Highcharts.chart.mock.calls.length).toBe(2);
    expect(existingChart.destroy.mock.invocationCallOrder[0])
      .toBeLessThan(Highcharts.chart.mock.invocationCallOrder[1]);
    expect(scope.component.chartInstance).toBe(replacementChart);
  });

  it('should reinitialize safely when there is no existing chart instance', function () {
    let replacementChart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };
    let scope = buildDirectiveScope(buildChartOptions('dark-blue'));

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(replacementChart);

    expect(function () {
      $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
      scope.$digest();
    }).not.toThrow();
    expect(Highcharts.chart).toHaveBeenCalledTimes(1);
    expect(scope.component.chartInstance).toBe(replacementChart);
  });

  it('should normalize svg url references after creating the chart', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));
    let svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    let rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    let path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    let group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    let chartInstance = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy'),
      renderer: {
        box: svg,
        url: 'http://localhost:8080/screen/private/home/chart-series-test'
      }
    };

    rect.setAttribute('fill', 'url(http://localhost:8080/screen/private/home/chart-series-test#highcharts-1)');
    path.setAttribute('clip-path', 'url(http://localhost:8080/screen/private/home/chart-series-test#highcharts-2)');
    group.setAttribute('stroke', '');
    svg.appendChild(rect);
    svg.appendChild(path);
    svg.appendChild(group);

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chartInstance);

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    expect(chartInstance.renderer.url).toBe('');
    expect(rect.getAttribute('fill')).toBe('url(#highcharts-1)');
    expect(path.getAttribute('clip-path')).toBe('url(#highcharts-2)');
    expect(group.getAttribute('stroke')).toBe('');
  });

  it('should fallback to lodash cloneDeep when structuredClone is unavailable', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));
    let originalStructuredClone = globalThis.structuredClone;

    globalThis.structuredClone = undefined;

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(_, 'cloneDeep');
    jest.spyOn(Highcharts, 'chart').mockReturnValue({
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    });

    try {
      $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
      scope.$digest();
    } finally {
      globalThis.structuredClone = originalStructuredClone;
    };

    expect(_.cloneDeep).toHaveBeenCalled();
  });

  it('should create stock charts when stockChart flag is enabled', function () {
    let chartOptions = buildChartOptions('dark-unica');
    chartOptions.stockChart = true;
    let scope = buildDirectiveScope(chartOptions);
    let stockChart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'stockChart').mockReturnValue(stockChart);
    jest.spyOn(Highcharts, 'chart');

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();

    expect(Highcharts.stockChart).toHaveBeenCalledTimes(1);
    expect(Highcharts.chart).not.toHaveBeenCalled();
    expect(scope.component.chartInstance).toBe(stockChart);
  });

  it('should update global language options when language changes', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));
    let chart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy')
    };

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chart);
    jest.spyOn(Highcharts, 'setOptions');

    $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    scope.$digest();
    scope.$broadcast('languageChanged');

    expect(chart.destroy).toHaveBeenCalled();
    expect(Highcharts.setOptions.mock.calls.length).toBeGreaterThan(1);
    expect(Highcharts.chart.mock.calls.length).toBe(2);
  });

  it('should reflow chart on resize when dimensions change', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));
    let chart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy'),
      reflow: jest.fn().mockName('reflow')
    };

    jest.spyOn(Utilities, 'isVisible').mockReturnValueOnce(true).mockReturnValueOnce(true, true);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chart);

    let element = $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    setElementSize(element, 300, 200);
    scope.$digest();

    setElementSize(element, 450, 200);
    scope.$broadcast('resize');

    expect(chart.reflow).toHaveBeenCalledTimes(1);
  });

  it('should not reflow chart on resize when dimensions stay equal', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));
    let chart = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy'),
      reflow: jest.fn().mockName('reflow')
    };

    jest.spyOn(Utilities, 'isVisible').mockReturnValueOnce(true).mockReturnValueOnce(true, true);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chart);

    let element = $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
    setElementSize(element, 300, 200);
    scope.$digest();

    scope.$broadcast('resize-action');

    expect(chart.reflow).not.toHaveBeenCalled();
  });

  it('should ignore svg normalization when chart container is missing', function () {
    let scope = buildDirectiveScope(buildChartOptions('dark-unica'));
    let chartInstance = {
      redraw: jest.fn().mockName('redraw'),
      destroy: jest.fn().mockName('destroy'),
      renderer: null,
      container: null
    };

    jest.spyOn(Utilities, 'isVisible').mockReturnValue(false);
    jest.spyOn(Highcharts, 'chart').mockReturnValue(chartInstance);

    expect(function () {
      $compile('<div ui-chart="chartOptions" ng-model="component.model.values"></div>')(scope);
      scope.$digest();
    }).not.toThrow();
  });

  it('should initialize aweChart directive and expose controller chart model', function () {
    let scope = $rootScope.$new();
    let directive = $injector.get('aweChartDirective')[0];
    let element = angular.element('<div></div>');

    scope.chartId = 'ChartDirective';
    jest.spyOn(Chart.prototype, 'init').mockImplementation(function () {
      this.component.controller = {chartModel: {title: {text: 'From controller'}}};
      return true;
    });
    directive.link(scope, element);

    expect(directive.templateUrl()).toContain('template/angular/chart');
    expect(scope.chartOptions).toEqual({title: {text: 'From controller'}});
  });

  it('should expose selected point data through chart api', function () {
    let scope = buildChartScope('ChartApiData', {
      chartModel: {}
    }, {
      selected: null,
      values: []
    });
    let chart = new Chart(scope, 'ChartApiData', {bind:() => null});

    configureChartComponent(chart, 'ChartApiData');
    jest.spyOn(chart, 'asComponent').mockReturnValue(true);

    chart.asChart();
    chart.model.selected = {x: 10, y: 20};
    Control.getAddressModel(chart.address).selected = {x: 10, y: 20};

    expect(Control.getAddressApi(chart.address).getData()).toEqual({
      'ChartApiData.x': 10,
      'ChartApiData.y': 20
    });
  });

  it('should expose chart svg through print api using portrait defaults', function () {
    let scope = buildChartScope('ChartPrint', {
      chartModel: {}
    }, {
      selected: null,
      values: []
    });
    let chart = new Chart(scope, 'ChartPrint', {bind:() => null});
    let getSvg = jest.fn().mockName('getSVG').mockReturnValue('<svg></svg>');

    configureChartComponent(chart, 'ChartPrint');
    jest.spyOn(chart, 'asComponent').mockReturnValue(true);

    chart.asChart();
    chart.chartInstance = {getSVG: getSvg};

    expect(Control.getAddressApi(chart.address).getPrintData('PORTRAIT')).toEqual({
      ChartPrint: {
        image: '<svg></svg>'
      }
    });
    expect(getSvg).toHaveBeenCalledWith({chart: {width:796, height:540}});
  });

  it('should update zoom only when values change and reset with zoomOut on null extremes', function () {
    let scope = buildChartScope('ChartZoom', {
      chartModel: {}
    }, {
      selected: null,
      values: []
    });
    let chart = new Chart(scope, 'ChartZoom', {bind:() => null});
    let xSetExtremes = jest.fn().mockName('setExtremes');
    let ySetExtremes = jest.fn().mockName('setExtremes');
    let zoomOut = jest.fn().mockName('zoomOut');

    configureChartComponent(chart, 'ChartZoom');
    jest.spyOn(chart, 'asComponent').mockReturnValue(true);

    chart.asChart();
    chart.model.zoom = {
      x: {min: null, max: null},
      y: {min: 2, max: 4}
    };
    chart.chartInstance = {
      xAxis: [{setExtremes: xSetExtremes}],
      yAxis: [{setExtremes: ySetExtremes}],
      zoomOut: zoomOut
    };

    chart.changeZoom({
      x: {min: 1, max: 5},
      y: {min: null, max: null}
    });

    expect(xSetExtremes).toHaveBeenCalledWith(1, 5);
    expect(ySetExtremes).toHaveBeenCalledWith(null, null);
    expect(zoomOut).toHaveBeenCalled();
  });

  it('should translate chart labels and assign formatter functions', function () {
    let scope = buildChartScope('ChartTranslate', {
      chartModel: {}
    }, {
      selected: null,
      values: []
    });
    let chart = new Chart(scope, 'ChartTranslate', {bind:() => null});
    let highchartOptions = {
      title: {text: 'TITLE'},
      subtitle: {text: 'SUBTITLE'},
      legend: {title: {text: 'LEGEND'}},
      xAxis: [{title: {text: 'X'}, labels: {formatter: 'formatCurrencyMagnitude'}}],
      yAxis: [{title: {text: 'Y'}, labels: {formatter: 'formatCurrencyMagnitude'}}],
      series: [{name: 'SERIE'}],
      drilldown: {series: [{name: 'DRILL'}]}
    };

    configureChartComponent(chart, 'ChartTranslate');
    jest.spyOn(chart, 'asComponent').mockReturnValue(true);

    chart.asChart();
    chart.translateLabels(highchartOptions);

    expect(highchartOptions.title.text).toBe('TITLE');
    expect(typeof highchartOptions.xAxis[0].labels.formatter).toBe('function');
    expect(typeof highchartOptions.yAxis[0].labels.formatter).toBe('function');
  });

  it('should update chart series and show no-data message when series exceed limit', function () {
    let scope = buildChartScope('ChartUpdateModel', {
      chartModel: {
        series: [{id: 'serie1', xValue: 'x', yValue: 'y'}],
        plotOptions: {}
      }
    }, {
      selected: null,
      values: []
    });
    let chart = new Chart(scope, 'ChartUpdateModel', {bind:() => null});
    let chartSerie = {setData: jest.fn().mockName('setData')};
    let chartInstance = {
      get: jest.fn().mockName('get').mockReturnValue(chartSerie),
      redraw: jest.fn().mockName('redraw'),
      hasData: jest.fn().mockName('hasData').mockReturnValue(false),
      hideNoData: jest.fn().mockName('hideNoData'),
      showNoData: jest.fn().mockName('showNoData'),
      options: {lang: {tooMuchData: 'tooMuchData'}}
    };

    configureChartComponent(chart, 'ChartUpdateModel', {
      series: [{id: 'serie1', xValue: 'x', yValue: 'y'}],
      plotOptions: {}
    });
    jest.spyOn(chart, 'asComponent').mockReturnValue(true);
    chart.asChart();
    chart.initialized = true;
    chart.chartInstance = chartInstance;
    jest.spyOn(chart, 'resetChart').mockImplementation(function () {});
    jest.spyOn($injector.get('AweSettings'), 'get').mockImplementation(function (key) {
      if (key === 'chartOptions') {
        return {limitPointsSerie: 1};
      }
      return DefaultSettings[key];
    });

    chart.updateModel([{x: 1, y: 2}, {x: 2, y: 3}]);

    expect(chartSerie.setData).toHaveBeenCalled();
    expect(chart.resetChart).toHaveBeenCalled();
    expect(chartInstance.showNoData).toHaveBeenCalledWith('tooMuchData');
  });

  it('should check if parameters have changed', function () {
    // Mock
    let $scope = $rootScope.$new();
    let chart = new Chart($scope, "lala", {bind:() => null});
    chart.controller = {chartModel: {}};
    chart.model = {};
    chart.listeners = {};
    chart.chart.element = {bind: jest.fn().mockName('bind')};
    jest.spyOn(chart, "asComponent").mockReturnValue(true);
    chart.asChart();
    jest.spyOn(chart, "changeZoom").mockReturnValue(null);

    // Launch
    chart.checkParametersChanged ({chartOptions: {}});
    expect(chart.changeZoom).not.toHaveBeenCalled();

    chart.checkParametersChanged ({chartOptions: {zoom: 10}});
    expect(chart.changeZoom).toHaveBeenCalledWith(10);
  });

  it('should add points', function () {
    // Mock
    let $scope = $rootScope.$new();
    let chart = new Chart($scope, "lala", {bind:() => null});
    chart.controller = {chartModel: {series: [{id: "serie1"}, {id: "serie2", zValue: 12}, {id: "serie3"}]}};
    chart.model = {values: []};
    chart.listeners = {};
    chart.chart.element = {bind: jest.fn().mockName('bind')};
    const redrawSpy = jest.fn().mockName("redraw");
    const addPointSpy = jest.fn().mockName("addPoint");
    chart.chartInstance = {
      redraw: redrawSpy,
      get: () => ({ addPoint: addPointSpy})
    };

    jest.spyOn(chart, "asComponent").mockReturnValue(true);
    chart.asChart();
    jest.spyOn(chart, "changeZoom").mockReturnValue(null);

    // Launch
    chart.addPoints([{}, {}]);

    // Expect redraw has been called
    expect(redrawSpy).toHaveBeenCalled();
    expect(addPointSpy).toHaveBeenCalled();
  });

  it('should launch onRedraw event', function () {
    // Mock
    let $scope = $rootScope.$new();
    let chart = new Chart($scope, "lala", {bind:() => null});
    chart.controller = {chartModel: {}};
    chart.model = {};
    chart.listeners = {};
    chart.chart.element = {bind: jest.fn().mockName('bind')};
    const point = {
      graphic: $(document.createElement("div"))
    };
    chart.series = [
      {id: "serie1", data: [point, point, point]},
      {id: "serie2", zValue: 12, data: [point, point, point]},
      {id: "serie3", data: [point, point, point]}];
    jest.spyOn(chart, "asComponent").mockReturnValue(true);
    chart.asChart();
    jest.spyOn(chart, "changeZoom").mockReturnValue(null);
    jest.spyOn(Utilities, "timeout").mockImplementation(fn => fn());
    jest.spyOn(Control, "publishModelChanged").mockReturnValue(null);
    jest.spyOn(Utilities, "stopPropagation");
    const contextMenuShowSpy = jest.fn().mockName("show");
    $scope.contextMenu = {show: contextMenuShowSpy};

    // Launch
    chart.onRedraw();

    // Trigger events
    let mousedownEvent = jQuery.Event( "mousedown" );
    mousedownEvent.which = 3;
    let clickEvent = jQuery.Event( "click" );
    clickEvent.which = 3;
    point.graphic.trigger("dblclick");
    point.graphic.trigger(mousedownEvent);
    point.graphic.trigger("contextmenu");
    point.graphic.trigger(clickEvent);
    point.graphic.trigger("mouseup");

    expect(Utilities.stopPropagation).toHaveBeenCalled();
    expect(contextMenuShowSpy).toHaveBeenCalled();

  });
});
