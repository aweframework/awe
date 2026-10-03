import {
  CHART_SIZE,
  getChartImage,
  getPrintOrientation,
  registerChart,
  unregisterChart
} from "../../../src/utilities/chartRegistry";

describe('awe-react-client/test/js/utilities/chartRegistryTest.jsx', () => {
  const buildChart = () => ({getSVG: jest.fn(() => "<svg/>")});

  it('should return no image for a chart that is not registered', () => {
    expect(getChartImage("unknown", "PORTRAIT")).toBeUndefined();
  });

  it('should return the svg of a registered chart sized for a portrait page', () => {
    const chart = buildChart();
    registerChart("chartPortrait", chart);

    expect(getChartImage("chartPortrait", "PORTRAIT")).toBe("<svg/>");
    expect(chart.getSVG).toHaveBeenCalledWith({chart: CHART_SIZE.PORTRAIT});
    unregisterChart("chartPortrait", chart);
  });

  it('should size the svg for a landscape page', () => {
    const chart = buildChart();
    registerChart("chartLandscape", chart);

    getChartImage("chartLandscape", "LANDSCAPE");

    expect(chart.getSVG).toHaveBeenCalledWith({chart: CHART_SIZE.LANDSCAPE});
    unregisterChart("chartLandscape", chart);
  });

  it('should forget an unregistered chart', () => {
    const chart = buildChart();
    registerChart("chartRemoved", chart);
    unregisterChart("chartRemoved", chart);

    expect(getChartImage("chartRemoved", "PORTRAIT")).toBeUndefined();
  });

  it('should keep a chart that replaced the one that is being unregistered', () => {
    const oldChart = buildChart();
    const newChart = buildChart();
    registerChart("chartReplaced", oldChart);
    registerChart("chartReplaced", newChart);
    unregisterChart("chartReplaced", oldChart);

    expect(getChartImage("chartReplaced", "PORTRAIT")).toBe("<svg/>");
    expect(newChart.getSVG).toHaveBeenCalled();
    unregisterChart("chartReplaced", newChart);
  });

  it('should return no image when the chart cannot export an svg', () => {
    const chart = {getSVG: jest.fn(() => {
      throw new Error("not rendered");
    })};
    const warn = jest.spyOn(console, "warn").mockImplementation(() => {});
    registerChart("chartBroken", chart);

    expect(getChartImage("chartBroken", "PORTRAIT")).toBeUndefined();
    expect(warn).toHaveBeenCalled();
    warn.mockRestore();
    unregisterChart("chartBroken", chart);
  });

  it('should read the print orientation from the report orientation criterion', () => {
    const components = {
      reportOrientation: {
        address: {component: "reportOrientation", view: "report"},
        model: {values: [{value: "PORTRAIT", selected: false}, {value: "LANDSCAPE", selected: true}]}
      }
    };

    expect(getPrintOrientation(components)).toBe("LANDSCAPE");
  });

  it('should print in portrait when the orientation is not defined', () => {
    expect(getPrintOrientation({})).toBe("PORTRAIT");
    expect(getPrintOrientation(undefined)).toBe("PORTRAIT");
  });
});
