import "../../../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../../../main/resources/js/awe/data/options";

describe("ChartEvents", () => {
  let ChartEvents;
  let $rootScope;
  let component;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$httpBackend", "$injector", "$rootScope", ($httpBackend, $injector, _$rootScope_) => {
      $rootScope = _$rootScope_;
      ChartEvents = $injector.get("ChartEvents");
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
    const scope = $rootScope.$new();
    component = {
      address: {view: "base", component: "chr"}, listeners: {}, scope,
      chart: {element: $("<div></div>")},
      addPoints: jest.fn(), addSeries: jest.fn(), removeSeries: jest.fn(), replaceSeries: jest.fn(),
      resetChart: jest.fn(), restoreChart: jest.fn(), showContextMenu: jest.fn(), hideContextMenu: jest.fn()
    };
    scope.component = component;
    ChartEvents.mapActions(component);
  });

  /**
   * Broadcast a client action as the action controller does
   * @param {string} type Action type
   * @param {object} parameters Action parameters
   * @param {object} target Address of the action
   * @returns {object} Action
   */
  function launch(type, parameters, target = {view: "base", component: "chr", row: "1"}) {
    const attributes = {callbackTarget: target, parameters};
    // The chart is not registered in Control, so the garbage collector of actions aborts it after the chart applies it
    const action = {attr: (key) => attributes[key], accept: jest.fn(), abort: jest.fn()};
    $rootScope.$broadcast(`/action/${type}`, action);
    return action;
  }

  it.each([
    ["add-points", {data: {rows: [{x: 1}]}}, "addPoints", [{x: 1}]],
    ["add-chart-series", {series: [{id: "a"}]}, "addSeries", [{id: "a"}]],
    ["remove-chart-series", {series: [{id: "a"}]}, "removeSeries", [{id: "a"}]],
    ["replace-chart-series", {series: [{id: "a"}]}, "replaceSeries", [{id: "a"}]]
  ])("applies %s to the chart of the action view and component", (type, parameters, method, argument) => {
    const action = launch(type, parameters);

    expect(component[method]).toHaveBeenCalledWith(argument);
    expect(action.accept).toHaveBeenCalled();
  });

  it.each([
    ["reset", "resetChart"],
    ["restore", "restoreChart"]
  ])("applies %s to the chart", (type, method) => {
    launch(type, {});

    expect(component[method]).toHaveBeenCalled();
  });

  it("ignores the series actions of another chart", () => {
    const action = launch("remove-chart-series", {series: [{id: "a"}]}, {view: "base", component: "other"});

    expect(component.removeSeries).not.toHaveBeenCalled();
    expect(action.accept).not.toHaveBeenCalled();
  });

  it("resets and restores the chart with the scope of its view", () => {
    $rootScope.$broadcast("reset-scope", "other");
    $rootScope.$broadcast("reset-scope", "base");
    $rootScope.$broadcast("restore-scope", "base");

    expect(component.resetChart).toHaveBeenCalledTimes(1);
    expect(component.restoreChart).toHaveBeenCalledTimes(1);
  });

  it("shows the context menu on right click and hides it on left click", () => {
    component.chart.element.trigger($.Event("mousedown", {which: 3}));
    component.chart.element.trigger($.Event("mousedown", {which: 1}));

    expect(component.showContextMenu).toHaveBeenCalledTimes(1);
    expect(component.hideContextMenu).toHaveBeenCalledTimes(1);
  });
});
