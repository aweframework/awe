describe('awe-framework/awe-client-angular/src/test/js/services/chart/chart.js', function () {
  let $injector, Chart, $rootScope, Utilities, Control;
  let originalTimeout;

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      Chart = $injector.get('Chart');
      Utilities = $injector.get('AweUtilities');
      Control = $injector.get('Control');
    }]);

    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function () {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  it('should check if parameters have changed', function () {
    // Mock
    let $scope = $rootScope.$new();
    let chart = new Chart($scope, "lala", {bind:() => null});
    chart.controller = {chartModel: {}};
    chart.model = {};
    chart.listeners = {};
    spyOn(chart, "asComponent").and.returnValue(true);
    chart.asChart();
    spyOn(chart, "changeZoom").and.returnValue(null);

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
    const redrawSpy = jasmine.createSpy("redraw");
    const addPointSpy = jasmine.createSpy("addPoint");
    chart.chart.redraw = redrawSpy;
    chart.chart.get = () => ({ addPoint: addPointSpy});

    spyOn(chart, "asComponent").and.returnValue(true);
    chart.asChart();
    spyOn(chart, "changeZoom").and.returnValue(null);

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
    const point = {
      graphic: $(document.createElement("div"))
    }
    chart.series = [
      {id: "serie1", data: [point, point, point]},
      {id: "serie2", zValue: 12, data: [point, point, point]},
      {id: "serie3", data: [point, point, point]}]
    spyOn(chart, "asComponent").and.returnValue(true);
    chart.asChart();
    spyOn(chart, "changeZoom").and.returnValue(null);
    spyOn(Utilities, "timeout").and.callFake(fn => fn());
    spyOn(Control, "publishModelChanged").and.returnValue(null);
    spyOn(Utilities, "stopPropagation");
    const contextMenuShowSpy = jasmine.createSpy("show");
    $scope.contextMenu = {show: contextMenuShowSpy}

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