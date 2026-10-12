import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweChart from "../../../src/components/AweChart";
import {updateAttributes, updateModel} from "../../../src/redux/actions/components";
import {getChartImage} from "../../../src/utilities/chartRegistry";
import {echarts, isDarkTheme, renderSvg} from "../../../src/utilities/echartsSetup";
import models from "./fixtures/chrTstEchartsModels.json";
import advancedModels from "./fixtures/advancedEchartsModels.json";
import i18n from "../../../src/i18n/i18n";
import {updateAttributesWithDependencies} from "../../../src/redux/thunks/components";

// The component is tested against a fake ECharts that records what it is given: the options are built by the real
// code, and the real library is exercised (SVG output, no warnings) by echartsSvgTest
jest.mock("../../../src/utilities/echartsSetup", () => ({
  echarts: {init: jest.fn()},
  getEChartsLocale: jest.requireActual("../../../src/utilities/echartsSetup").getEChartsLocale,
  isDarkTheme: jest.fn(() => false),
  renderSvg: jest.fn(() => "<svg>print</svg>")
}));

// The events of the chart go through the thunk that writes the attributes and checks the dependencies: it is spied
jest.mock("../../../src/redux/thunks/components", () => {
  const actual = jest.requireActual("../../../src/redux/thunks/components");
  return {...actual, updateAttributesWithDependencies: jest.fn(actual.updateAttributesWithDependencies)};
});

const clone = (value) => JSON.parse(JSON.stringify(value));
const day = (value) => new Date(2024, 0, value).getTime();
const values = [
  {dates: day(1), serie1: 10, serie2: 5, names: "Chrome", subserie1: 3},
  {dates: day(2), serie1: 20, serie2: 6, names: "Firefox", subserie1: 4}
];

describe('awe-react-client/test/js/components/AweChartTest.jsx', () => {
  let instances;
  let observers;

  const fakeChart = () => {
    const handlers = {};
    const chart = {
      handlers,
      setOption: jest.fn(),
      resize: jest.fn(),
      dispatchAction: jest.fn(),
      getOption: jest.fn(() => ({})),
      dispose: jest.fn(),
      on: jest.fn((event, handler) => {
        handlers[event] = handler;
      })
    };
    instances.push(chart);
    return chart;
  };
  const lastChart = () => instances[instances.length - 1];
  const lastOption = (chart = lastChart()) => chart.setOption.mock.calls[chart.setOption.mock.calls.length - 1][0];
  const texts = (option) => (option.graphic || []).map(element => element.style.text);

  const stateOf = (echartsModel, rows = values, extra = {}) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      chart: {
        address: {component: "chart", view: "report"},
        model: {values: rows},
        attributes: {visible: true, label: "chart", ...(echartsModel ? {echartsModel} : {}), ...extra}
      }
    }
  });

  const renderChart = (echartsModel, rows, extra) => renderWithProviders(
    <div style={{width: "1000px", height: "600px"}}><AweChart id="chart"/></div>,
    {preloadedState: stateOf(echartsModel, rows, extra)});

  beforeEach(() => {
    instances = [];
    observers = [];
    updateAttributesWithDependencies.mockClear();
    echarts.init.mockReset();
    echarts.init.mockImplementation(fakeChart);
    isDarkTheme.mockReturnValue(false);
    renderSvg.mockClear();
    global.ResizeObserver = class {
      constructor(callback) {
        this.callback = callback;
        this.observe = jest.fn();
        this.disconnect = jest.fn();
        observers.push(this);
      }
    };
    Object.defineProperty(HTMLElement.prototype, "clientWidth", {configurable: true, get: () => 900});
    Object.defineProperty(HTMLElement.prototype, "clientHeight", {configurable: true, get: () => 500});
  });

  afterEach(() => {
    delete global.ResizeObserver;
    delete HTMLElement.prototype.clientWidth;
    delete HTMLElement.prototype.clientHeight;
    jest.useRealTimers();
  });

  describe('creation', () => {
    it('should create an SVG chart in its container, with the plain theme', () => {
      renderChart(models.ChrLinTst);

      expect(echarts.init).toHaveBeenCalledTimes(1);
      const [element, theme, options] = echarts.init.mock.calls[0];
      expect(element.parentElement.id).toBe("chart");
      expect(theme).toBeNull();
      expect(options).toEqual({renderer: "svg", locale: "EN"});
    });

    it('should use the dark theme when the application theme is dark', () => {
      isDarkTheme.mockReturnValue(true);

      renderChart(models.ChrLinTst);

      expect(echarts.init.mock.calls[0][1]).toBe("dark");
      expect(lastOption().backgroundColor).toBe("rgba(0, 0, 0, 0)");
    });

    it('should expose the test hook with the chart identifier, rendered once ECharts finishes', () => {
      renderChart(models.ChrLinTst);

      const hook = document.querySelector("[data-testid='chart']");
      expect(hook.getAttribute("chart-id")).toBe("chart");
      expect(hook.getAttribute("data-rendered")).toBe("false");

      act(() => lastChart().handlers.finished());

      expect(hook.getAttribute("data-rendered")).toBe("true");
    });

    it('should also mark the chart as rendered when ECharts reports a render', () => {
      renderChart(models.ChrLinTst);

      act(() => lastChart().handlers.rendered());

      expect(document.querySelector("[data-testid='chart']").getAttribute("data-rendered")).toBe("true");
    });

    it('should hide the chart when it is not visible', () => {
      renderChart(models.ChrLinTst, values, {visible: false});

      expect(document.querySelector("div#chart").classList.contains("hidden")).toBe(true);
    });

    it('should not crash the screen when ECharts cannot create the chart', () => {
      const error = jest.spyOn(console, "error").mockImplementation(() => {});
      echarts.init.mockImplementation(() => {
        throw new Error("no svg");
      });

      renderChart(models.ChrLinTst);

      expect(document.querySelector("div#chart")).not.toBeNull();
      expect(error).toHaveBeenCalled();
      error.mockRestore();
    });

    it('should not crash the screen when ECharts cannot draw the option', () => {
      const error = jest.spyOn(console, "error").mockImplementation(() => {});
      echarts.init.mockImplementation(() => {
        const chart = fakeChart();
        chart.setOption.mockImplementation(() => {
          throw new Error("bad option");
        });
        return chart;
      });

      renderChart(models.ChrLinTst);

      expect(document.querySelector("div#chart")).not.toBeNull();
      expect(error).toHaveBeenCalled();
      error.mockRestore();
    });
  });

  describe('option', () => {
    it('should give ECharts the option built from the model and the values', () => {
      renderChart(models.ChrLinTst);

      const option = lastOption();
      expect(lastChart().setOption.mock.calls[0][1]).toEqual({notMerge: true});
      expect(option.series[0].data).toEqual([[day(1), 10], [day(2), 20]]);
      expect(option.series[1].data).toEqual([[day(1), 5], [day(2), 6]]);
      expect(JSON.stringify(option)).not.toContain("\"awe\"");
    });

    it('should translate the texts of the chart', () => {
      renderChart(models.ChrLinTst);

      expect(lastOption().title.text).toBe(i18n.t("SCREEN_TEXT_CHART_TITLE_1"));
    });

    it('should draw an empty chart with the no data message when the server sent no echartsModel', () => {
      const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

      renderChart(undefined);

      expect(lastOption().series).toEqual([]);
      expect(texts(lastOption())).toEqual([expect.any(String)]);
      expect(warn).toHaveBeenCalledWith(expect.stringContaining("echartsModel"));
      warn.mockRestore();
    });

    it('should show the no data message when there are no values', () => {
      renderChart(models.ChrLinTst, []);

      expect(texts(lastOption())).toHaveLength(1);
      expect(texts(lastOption())[0]).toBeTruthy();
    });

    it('should draw again when the values change', () => {
      const {store} = renderChart(models.ChrLinTst);
      const calls = lastChart().setOption.mock.calls.length;

      act(() => {
        store.dispatch(updateModel({component: "chart", view: "report"}, {values: [values[0]]}));
      });

      expect(lastChart().setOption.mock.calls.length).toBeGreaterThan(calls);
      expect(lastOption().series[0].data).toEqual([[day(1), 10]]);
    });

    it('should draw again when the server replaces the echarts model', () => {
      const {store} = renderChart(models.ChrLinTst);

      act(() => {
        store.dispatch(updateAttributes({component: "chart", view: "report"}, {echartsModel: models.ChrStockTst}));
      });

      expect(lastOption().series).toHaveLength(1);
      expect(lastOption().dataZoom).toHaveLength(2);
    });
  });

  describe('lifecycle', () => {
    it('should dispose the chart and forget its image when it is unmounted', () => {
      const {unmount} = renderChart(models.ChrLinTst);
      const chart = lastChart();
      expect(getChartImage("chart", "PORTRAIT")).toBe("<svg>print</svg>");

      unmount();

      expect(chart.dispose).toHaveBeenCalledTimes(1);
      expect(getChartImage("chart", "PORTRAIT")).toBeUndefined();
    });

    it('should create the chart again when the language changes', async () => {
      renderChart(models.ChrLinTst);
      const first = lastChart();

      await act(async () => {
        await i18n.changeLanguage("es-ES");
      });

      expect(first.dispose).toHaveBeenCalled();
      expect(echarts.init).toHaveBeenCalledTimes(2);
      expect(echarts.init.mock.calls[1][2].locale).toBe("ES");
      await act(async () => {
        await i18n.changeLanguage("en-GB");
      });
    });
  });

  describe('print', () => {
    it('should draw the image of the page size apart, in light colors, from the same model', () => {
      isDarkTheme.mockReturnValue(true);
      renderChart(models.ChrLinTst);

      expect(getChartImage("chart", "PORTRAIT")).toBe("<svg>print</svg>");
      expect(getChartImage("chart", "LANDSCAPE")).toBe("<svg>print</svg>");

      const [portrait, landscape] = renderSvg.mock.calls;
      expect(portrait[1]).toEqual({width: 796, height: 540});
      expect(landscape[1]).toEqual({width: 1167, height: 360});
      expect(portrait[0].series[0].data).toEqual([[day(1), 10], [day(2), 20]]);
      expect(portrait[2]).toBe(i18n.language);
    });

    it('should print the latest values', () => {
      const {store} = renderChart(models.ChrLinTst);

      act(() => {
        store.dispatch(updateModel({component: "chart", view: "report"}, {values: [values[1]]}));
      });
      getChartImage("chart", "PORTRAIT");

      expect(renderSvg.mock.calls[0][0].series[0].data).toEqual([[day(2), 20]]);
    });
  });

  describe('resize', () => {
    it('should observe the container and resize the chart, debounced', () => {
      jest.useFakeTimers();
      renderChart(models.ChrLinTst);
      const chart = lastChart();
      expect(observers).toHaveLength(1);
      expect(observers[0].observe).toHaveBeenCalledWith(document.querySelector("div#chart"));
      chart.resize.mockClear();

      act(() => {
        observers[0].callback();
        observers[0].callback();
        jest.advanceTimersByTime(60);
      });

      expect(chart.resize).toHaveBeenCalledTimes(1);
    });

    it('should build the option with the size of the container', () => {
      const model = clone(models.ChrSemiCircleTst);

      renderChart(model);

      // 500 / 2 + 50 - 12
      expect(lastOption().title.top).toBe(288);
    });

    it('should place a semicircle again with the new size of the container', () => {
      jest.useFakeTimers();
      renderChart(clone(models.ChrSemiCircleTst));
      // Plot area of 900x500: margins of 16 and the legend below (34)
      expect(lastOption().series[0].center).toEqual([450, 16 + 0.75 * (500 - 16 - 50)]);

      Object.defineProperty(HTMLElement.prototype, "clientWidth", {configurable: true, get: () => 600});
      Object.defineProperty(HTMLElement.prototype, "clientHeight", {configurable: true, get: () => 400});
      act(() => {
        observers[0].callback();
        jest.advanceTimersByTime(60);
      });

      expect(lastOption().series[0].center).toEqual([300, 16 + 0.75 * (400 - 16 - 50)]);
    });

    it('should stop observing when it is unmounted', () => {
      const {unmount} = renderChart(models.ChrLinTst);

      unmount();

      expect(observers[0].disconnect).toHaveBeenCalled();
    });

    it('should work without ResizeObserver', () => {
      delete global.ResizeObserver;

      renderChart(models.ChrLinTst);

      expect(echarts.init).toHaveBeenCalled();
    });
  });

  describe('linked series', () => {
    const linkedModel = () => {
      const model = clone(advancedModels.AdvPyramid);
      delete model.series[3].silent;
      return model;
    };

    it('should hide and show a linked series together with the series it is linked to', () => {
      renderChart(linkedModel(), []);
      const {handlers, dispatchAction} = lastChart();

      act(() => handlers.legendselectchanged({name: "max", selected: {max: false, Hombres: true}}));
      act(() => handlers.legendselectchanged({name: "max", selected: {max: true, Hombres: true}}));

      expect(dispatchAction).toHaveBeenNthCalledWith(1, {type: "legendUnSelect", name: "min"});
      expect(dispatchAction).toHaveBeenNthCalledWith(2, {type: "legendSelect", name: "min"});
    });

    it('should not dispatch anything for a series that nothing is linked to', () => {
      renderChart(linkedModel(), []);
      const {handlers, dispatchAction} = lastChart();

      act(() => handlers.legendselectchanged({name: "Hombres", selected: {Hombres: false}}));
      act(() => handlers.legendselectchanged({name: "min", selected: {min: false}}));

      expect(dispatchAction).not.toHaveBeenCalled();
    });
  });

  describe('drilldown', () => {
    const pieValues = [
      {names: "Chrome", serie1: 10, subserie1: 3},
      {names: "Firefox", serie1: 20, subserie1: 4}
    ];

    it('should replace a series that has a drilldown when it is clicked, and restore it with the back control', () => {
      renderChart(models.ChrPieTst, pieValues);
      expect(lastOption().series[0].id).toBe("serie1");

      act(() => lastChart().handlers.click({seriesId: "serie1", name: "Chrome"}));

      expect(lastOption().series[0].id).toBe("serie1_1");
      expect(lastOption().series[0].data[0]).toEqual({name: "Chrome", value: 3});
      expect(texts(lastOption())[0]).toContain(i18n.t("Themes"));

      act(() => lastOption().graphic[0].onclick());

      expect(lastOption().series[0].id).toBe("serie1");
      expect(lastOption().graphic).toBeUndefined();
    });

    it('should ignore the click on a series without drilldown', () => {
      renderChart(models.ChrLinTst);
      const calls = lastChart().setOption.mock.calls.length;

      act(() => lastChart().handlers.click({seriesId: "serie-1"}));

      expect(lastChart().setOption.mock.calls.length).toBe(calls);
    });

    it('should close the drilldown when the server sends another chart', () => {
      const {store} = renderChart(models.ChrPieTst, pieValues);
      act(() => lastChart().handlers.click({seriesId: "serie1"}));
      expect(lastOption().series[0].id).toBe("serie1_1");

      act(() => {
        store.dispatch(updateAttributes({component: "chart", view: "report"}, {echartsModel: {...clone(models.ChrPieTst), title: {text: "Other"}}}));
      });

      expect(lastOption().series[0].id).toBe("serie1");
    });

    it('should keep the drilldown and the drawing when an event only writes the chart attributes', () => {
      renderChart(models.ChrPieTst, pieValues);
      act(() => lastChart().handlers.click({seriesId: "serie1"}));
      const calls = lastChart().setOption.mock.calls.length;

      act(() => lastChart().handlers.contextmenu({name: "Chrome", value: 3}));

      expect(lastOption().series[0].id).toBe("serie1_1");
      expect(lastChart().setOption.mock.calls.length).toBe(calls);
    });
  });

  describe('events and attributes', () => {
    const address = {component: "chart", view: "report"};
    const attributesOf = (store) => store.getState().components.chart.attributes;
    const emitted = () => updateAttributesWithDependencies.mock.calls;

    it('should emit click with the point as the x and y attributes', () => {
      const {store} = renderChart(models.ChrLinTst);

      act(() => lastChart().handlers.click({seriesId: "serie-1", value: [day(2), 20]}));

      expect(emitted()).toEqual([[address, {x: day(2), y: 20, event: "click"}]]);
      expect(attributesOf(store)).toMatchObject({x: day(2), y: 20});
    });

    it('should use the name and the value of a point without coordinates (pie)', () => {
      const {store} = renderChart(models.ChrPieTst, [{names: "Chrome", serie1: 3}]);

      act(() => lastChart().handlers.click({seriesId: "none", name: "Chrome", value: 3}));

      expect(attributesOf(store)).toMatchObject({x: "Chrome", y: 3});
    });

    it('should emit double-click and context-menu with the point', () => {
      renderChart(models.ChrLinTst);

      act(() => lastChart().handlers.dblclick({value: [day(1), 10]}));
      act(() => lastChart().handlers.contextmenu({value: [day(2), 20]}));

      expect(emitted()).toEqual([
        [address, {x: day(1), y: 10, event: "double-click"}],
        [address, {x: day(2), y: 20, event: "context-menu"}]
      ]);
    });

    it('should emit zoom with the range of the zoomed axes, and keep the y range empty', () => {
      const {store} = renderChart(models.ChrLinTst);
      lastChart().getOption.mockReturnValue({dataZoom: [{xAxisIndex: [0], start: 20, end: 60, startValue: day(2), endValue: day(5)}]});

      act(() => lastChart().handlers.datazoom({}));

      expect(emitted()).toEqual([[address, {xMin: day(2), xMax: day(5), yMin: null, yMax: null, event: "zoom"}]]);
      expect(attributesOf(store)).toMatchObject({xMin: day(2), xMax: day(5), yMin: null, yMax: null});
    });

    it('should tell through its test hook whether it is zoomed', () => {
      renderChart(models.ChrLinTst);
      const hook = () => document.querySelector("[data-testid='chart']").getAttribute("data-zoomed");
      expect(hook()).toBe("false");

      lastChart().getOption.mockReturnValue({dataZoom: [{xAxisIndex: [0], start: 20, end: 60, startValue: day(2), endValue: day(5)}]});
      act(() => lastChart().handlers.datazoom({}));

      expect(hook()).toBe("true");
    });

    it('should emit zoom with an empty range when the zoom is reset', () => {
      renderChart(models.ChrLinTst);
      lastChart().getOption.mockReturnValue({dataZoom: [{xAxisIndex: [0], start: 0, end: 100, startValue: day(1), endValue: day(9)}]});

      act(() => lastChart().handlers.datazoom({}));

      expect(emitted()[0][1]).toMatchObject({xMin: null, xMax: null, event: "zoom"});
    });

    it('should not fail when the chart cannot tell its zoom', () => {
      renderChart(models.ChrLinTst);
      lastChart().getOption.mockImplementation(() => {
        throw new Error("disposed");
      });

      act(() => lastChart().handlers.datazoom({}));

      expect(emitted()[0][1]).toMatchObject({xMin: null, xMax: null, event: "zoom"});
    });
  });

  describe('chart options and linked zoom', () => {
    const setOptions = (store, chartOptions) => act(() => {
      store.dispatch(updateAttributes({component: "chart", view: "report"}, {chartOptions}));
    });
    const zoomOption = () => ({dataZoom: [
      {xAxisIndex: [0], start: 0, end: 100},
      {xAxisIndex: [0], start: 0, end: 100}
    ]});

    it('should zoom every x data zoom to the range of the chart options', () => {
      const {store} = renderChart(models.ChrStockTst);
      lastChart().getOption.mockReturnValue(zoomOption());

      setOptions(store, {zoom: {x: {min: day(2), max: day(5)}}});

      expect(lastChart().dispatchAction).toHaveBeenCalledWith({type: "dataZoom", dataZoomIndex: 0, startValue: day(2), endValue: day(5)});
      expect(lastChart().dispatchAction).toHaveBeenCalledWith({type: "dataZoom", dataZoomIndex: 1, startValue: day(2), endValue: day(5)});
      expect(store.getState().components.chart.attributes).toMatchObject({xMin: day(2), xMax: day(5)});
    });

    it('should accept the chart options as a JSON text', () => {
      const {store} = renderChart(models.ChrStockTst);
      lastChart().getOption.mockReturnValue(zoomOption());

      setOptions(store, `{"zoom": {"x": {"min": 10, "max": 20}}}`);

      expect(lastChart().dispatchAction).toHaveBeenCalledWith(expect.objectContaining({startValue: 10, endValue: 20}));
    });

    it('should show the whole range when the zoom of the options is empty', () => {
      const {store} = renderChart(models.ChrStockTst, values, {xMin: day(2), xMax: day(5)});
      lastChart().getOption.mockReturnValue(zoomOption());

      setOptions(store, {zoom: {x: {min: null, max: null}}});

      expect(lastChart().dispatchAction).toHaveBeenCalledWith({type: "dataZoom", dataZoomIndex: 0, start: 0, end: 100});
      expect(store.getState().components.chart.attributes).toMatchObject({xMin: null, xMax: null});
    });

    it('should not zoom again to the range it already has', () => {
      const {store} = renderChart(models.ChrStockTst, values, {xMin: day(2), xMax: day(5)});
      lastChart().getOption.mockReturnValue(zoomOption());

      setOptions(store, {zoom: {x: {min: day(2), max: day(5)}}});

      expect(lastChart().dispatchAction).not.toHaveBeenCalled();
    });

    it('should not emit the zoom event that its own zoom raises (no loop between linked charts)', () => {
      const {store} = renderChart(models.ChrStockTst);
      const chart = lastChart();
      chart.getOption.mockReturnValue(zoomOption());
      chart.dispatchAction.mockImplementation(() => chart.handlers.datazoom({}));

      setOptions(store, {zoom: {x: {min: day(2), max: day(5)}}});

      const events = updateAttributesWithDependencies.mock.calls.filter(([, data]) => data.event);
      expect(events).toEqual([]);
      expect(store.getState().components.chart.attributes).toMatchObject({xMin: day(2), xMax: day(5)});
    });

    it('should ignore the options that have no zoom, a text that is not JSON and an unknown axis', () => {
      const {store} = renderChart(models.ChrStockTst);
      lastChart().getOption.mockReturnValue(zoomOption());

      setOptions(store, {title: {text: "none"}});
      setOptions(store, "not json at all");
      setOptions(store, {zoom: {y: {min: 1, max: 2}}});
      setOptions(store, null);

      expect(lastChart().dispatchAction).not.toHaveBeenCalled();
    });

    it('should zoom the chart that is linked to another one when the first one is zoomed', () => {
      // Two charts: the zoom event of the first one becomes the chart options of the second one (the dependency)
      const first = renderChart(models.ChrLinTst);
      const firstChart = lastChart();
      const second = renderChart(models.ChrStockTst);
      const secondChart = lastChart();
      secondChart.getOption.mockReturnValue(zoomOption());
      firstChart.getOption.mockReturnValue({dataZoom: [{xAxisIndex: [0], start: 10, end: 50, startValue: day(3), endValue: day(4)}]});

      act(() => firstChart.handlers.datazoom({}));
      const {xMin, xMax} = first.store.getState().components.chart.attributes;
      setOptions(second.store, {zoom: {x: {min: xMin, max: xMax}}});

      expect(secondChart.dispatchAction).toHaveBeenCalledWith(expect.objectContaining({startValue: day(3), endValue: day(4)}));
      expect(firstChart.dispatchAction).not.toHaveBeenCalled();
    });
  });
});
