import {parseChartOptions, pointOf, zoomOf, zoomRequests} from "../../../src/utilities/echartsEvents";
import {getEChartsSerie, mergeSeriesPoints} from "../../../src/utilities/echartsSeries";

describe('awe-react-client/test/js/utilities/echartsEventsTest.jsx', () => {
  const none = {xMin: null, xMax: null, yMin: null, yMax: null};

  describe('pointOf', () => {
    it('should read the coordinates of a point', () => {
      expect(pointOf({value: [10, 20]})).toEqual({x: 10, y: 20});
    });

    it('should read the name and the value of a point without coordinates', () => {
      expect(pointOf({name: "Chrome", value: 3})).toEqual({x: "Chrome", y: 3});
    });

    it('should read the AWE coordinates of a point of an inverted chart, drawn as [y, x]', () => {
      expect(pointOf({value: [20, 10]}, true)).toEqual({x: 10, y: 20});
      expect(pointOf({name: "Chrome", value: 3}, true)).toEqual({x: "Chrome", y: 3});
    });

    it('should read an empty point', () => {
      expect(pointOf({})).toEqual({x: null, y: null});
      expect(pointOf(undefined)).toEqual({x: null, y: null});
    });
  });

  describe('zoomOf', () => {
    it('should read the range of the x and y axes', () => {
      const chart = {getOption: () => ({dataZoom: [
        {xAxisIndex: [0], start: 10, end: 50, startValue: 3, endValue: 7},
        {yAxisIndex: [0], start: 0, end: 30, startValue: 1, endValue: 2}
      ]})};

      expect(zoomOf(chart)).toEqual({xMin: 3, xMax: 7, yMin: 1, yMax: 2});
    });

    it('should read the AWE x axis from the vertical axis of an inverted chart', () => {
      const chart = {getOption: () => ({dataZoom: [
        {yAxisIndex: [0], start: 10, end: 50, startValue: 3, endValue: 7},
        {xAxisIndex: [0], start: 0, end: 30, startValue: 1, endValue: 2}
      ]})};

      expect(zoomOf(chart, true)).toEqual({xMin: 3, xMax: 7, yMin: 1, yMax: 2});
    });

    it('should read an empty range when everything is shown', () => {
      const chart = {getOption: () => ({dataZoom: [{xAxisIndex: [0], start: 0, end: 100, startValue: 1, endValue: 9}]})};

      expect(zoomOf(chart)).toEqual(none);
    });

    it('should read an empty range when the chart has no limits, no zoom or cannot tell', () => {
      const limits = {getOption: () => ({dataZoom: [{xAxisIndex: [0], start: 5, end: 60}]})};
      const broken = {getOption: () => {
        throw new Error("disposed");
      }};

      expect(zoomOf(limits)).toEqual(none);
      expect(zoomOf({getOption: () => ({})})).toEqual(none);
      expect(zoomOf(broken)).toEqual(none);
      expect(zoomOf(null)).toEqual(none);
    });
  });

  describe('parseChartOptions', () => {
    it('should accept an object and a JSON text', () => {
      expect(parseChartOptions({zoom: {}})).toEqual({zoom: {}});
      expect(parseChartOptions('{"zoom": {"x": {"min": 1}}}')).toEqual({zoom: {x: {min: 1}}});
    });

    it('should reject what is not an object, with a message', () => {
      const error = jest.spyOn(console, "error").mockImplementation(() => {});

      expect(parseChartOptions("not json")).toEqual({});
      expect(error).toHaveBeenCalledWith(expect.stringContaining("chart options"), expect.anything());
      expect(parseChartOptions(null)).toEqual({});
      expect(parseChartOptions(5)).toEqual({});
      error.mockRestore();
    });
  });

  describe('zoomRequests', () => {
    const chart = {getOption: () => ({dataZoom: [
      {xAxisIndex: [0]},
      {xAxisIndex: [0]},
      {yAxisIndex: [0]},
      {}
    ]})};

    it('should ask a range for every data zoom of the axis', () => {
      expect(zoomRequests(chart, {x: {min: 1, max: 2}})).toEqual([
        {type: "dataZoom", dataZoomIndex: 0, startValue: 1, endValue: 2},
        {type: "dataZoom", dataZoomIndex: 1, startValue: 1, endValue: 2}
      ]);
      expect(zoomRequests(chart, {y: {min: 5, max: 6}})).toEqual([
        {type: "dataZoom", dataZoomIndex: 2, startValue: 5, endValue: 6}
      ]);
    });

    it('should zoom the vertical axis for the AWE x axis of an inverted chart', () => {
      expect(zoomRequests(chart, {x: {min: 1, max: 2}}, true)).toEqual([
        {type: "dataZoom", dataZoomIndex: 2, startValue: 1, endValue: 2}
      ]);
      expect(zoomRequests(chart, {y: {min: 5, max: 6}}, true)).toEqual([
        {type: "dataZoom", dataZoomIndex: 0, startValue: 5, endValue: 6},
        {type: "dataZoom", dataZoomIndex: 1, startValue: 5, endValue: 6}
      ]);
    });

    it('should ask the whole range when the range is empty', () => {
      const whole = {type: "dataZoom", dataZoomIndex: 0, start: 0, end: 100};

      expect(zoomRequests(chart, {x: {min: null, max: null}})[0]).toEqual(whole);
      expect(zoomRequests(chart, {x: {}})[0]).toEqual(whole);
    });

    it('should ask nothing for an unknown axis or a chart without zoom', () => {
      expect(zoomRequests(chart, {z: {min: 1, max: 2}})).toEqual([]);
      expect(zoomRequests({getOption: () => ({})}, {x: {min: 1, max: 2}})).toEqual([]);
    });
  });

  describe('series of the chart actions', () => {
    it('should use the ECharts translation that the server sends', () => {
      const echarts = {id: "s1", type: "line", smooth: true, awe: {xValue: "x", yValue: "y"}};

      expect(getEChartsSerie({id: "s1", type: "spline", echarts}, false)).toBe(echarts);
    });

    it('should build the series from its Highcharts fields when the translation is missing', () => {
      expect(getEChartsSerie({id: "s1", name: "Serie", type: "column", color: "#ff0000", xValue: "x", yValue: "y",
        zValue: "z"}, false)).toEqual({
        id: "s1", name: "Serie", type: "bar", itemStyle: {color: "#ff0000"},
        awe: {type: "column", xValue: "x", yValue: "y", zValue: "z"}
      });
      expect(getEChartsSerie({id: "s2", label: "Label", type: "areaspline", xValue: "x", yValue: "y"}, false))
        .toEqual({id: "s2", name: "Label", type: "line", awe: {type: "areaspline", xValue: "x", yValue: "y"}});
    });

    it('should swap the axes of the series of an inverted chart', () => {
      const echarts = {id: "s1", type: "bar", xAxisIndex: 1, awe: {}};

      expect(getEChartsSerie({echarts}, true)).toEqual({id: "s1", type: "bar", yAxisIndex: 1, awe: {}});
    });

    it('should bind a series that only brings its points (no xValue nor yValue) to keys of its own', () => {
      const serie = {id: "donald", name: "donald", echarts: {id: "donald", type: "line", awe: {type: "line"}},
        data: [["Jan", 3], ["Feb", 5]]};

      expect(getEChartsSerie(serie, false)).toEqual({
        id: "donald", name: "donald", type: "line", awe: {type: "line", xValue: "donald#x", yValue: "donald#y"}
      });
      expect(mergeSeriesPoints([{other: 1}], [serie])).toEqual([
        {other: 1, "donald#x": "Jan", "donald#y": 3}, {"donald#x": "Feb", "donald#y": 5}
      ]);
    });

    it('should name a series after its name when the translation has no name', () => {
      expect(getEChartsSerie({id: "s1", echarts: {id: "s1", type: "line", awe: {xValue: "x", yValue: "y"}}}, false).name)
        .toBeUndefined();
      expect(getEChartsSerie({id: "s1", name: "Named", echarts: {id: "s1", type: "line", awe: {xValue: "x", yValue: "y"}}},
        false).name).toBe("Named");
    });

    it('should merge the points of the series into the values, row by row', () => {
      const values = [{x: 1, y: 10}];
      const series = [
        {xValue: "x", yValue: "z", data: [[1, 5], [2, 6]]},
        {echarts: {awe: {xValue: "x", yValue: "w", zValue: "v"}}, data: [[1, 7, 8]]},
        {data: [[9, 9]]}
      ];

      expect(mergeSeriesPoints(values, series)).toEqual([{x: 1, y: 10, z: 5, w: 7, v: 8}, {x: 2, z: 6}]);
      expect(values).toEqual([{x: 1, y: 10}]);
    });
  });
});
