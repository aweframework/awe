import {parseChartOptions, pointOf, zoomOf, zoomRequests} from "../../../src/utilities/echartsEvents";

describe('awe-react-client/test/js/utilities/echartsEventsTest.jsx', () => {
  const none = {xMin: null, xMax: null, yMin: null, yMax: null};

  describe('pointOf', () => {
    it('should read the coordinates of a point', () => {
      expect(pointOf({value: [10, 20]})).toEqual({x: 10, y: 20});
    });

    it('should read the name and the value of a point without coordinates', () => {
      expect(pointOf({name: "Chrome", value: 3})).toEqual({x: "Chrome", y: 3});
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
});
