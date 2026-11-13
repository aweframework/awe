import {buildEChartsOption, hasAweKeys} from "../../../src/utilities/echartsOption";
import {VISUAL_MAP_ROOM} from "../../../src/utilities/echartsChartTypes";
import {renderSvg} from "../../../src/utilities/echartsSetup";
import {getEChartsSerie} from "../../../src/utilities/echartsSeries";
import {pointOf} from "../../../src/utilities/echartsEvents";
import {applyChartColors} from "../../../src/utilities/echartsTheme";

const clone = (value) => JSON.parse(JSON.stringify(value));
const context = (overrides = {}) => ({t: (text) => text, locale: {}, width: 800, height: 400, ...overrides});
const build = (model, rows, overrides = {}) => buildEChartsOption(clone(model), rows, context(overrides));

const radarModel = {
  radar: {},
  legend: {show: true, top: "bottom"},
  tooltip: {trigger: "item"},
  series: [
    {id: "alpha", type: "radar", name: "Alpha", awe: {type: "radar", xValue: "skill", yValue: "alpha"}},
    {id: "beta", type: "radar", name: "Beta", awe: {type: "radar", xValue: "skill", yValue: "beta"}}
  ],
  awe: {chartType: "radar"}
};
const radarRows = [
  {skill: "Speed", alpha: 3, beta: 5}, {skill: "Power", alpha: 4, beta: 2}, {skill: "Skill", alpha: 5, beta: 4}
];

const heatmapModel = {
  tooltip: {position: "top"},
  xAxis: [{type: "category", awe: {axis: "x"}}],
  yAxis: [{type: "category", awe: {axis: "y"}}],
  visualMap: {type: "continuous", calculable: true, orient: "horizontal", left: "center", bottom: 0},
  series: [{id: "heat", type: "heatmap", name: "Visits", awe: {type: "heatmap", xValue: "day", yValue: "hour", zValue: "visits"}}],
  awe: {chartType: "heatmap"}
};
const heatmapRows = [
  {day: "Mon", hour: "10h", visits: 4}, {day: "Mon", hour: "12h", visits: 9},
  {day: "Tue", hour: "10h", visits: 1}, {day: "Tue", hour: "12h", visits: 7}
];

const gaugeModel = {
  series: [{id: "speed", type: "gauge", name: "Speed", awe: {type: "gauge", yValue: "value", zValue: "target"}}],
  awe: {chartType: "gauge"}
};
const gaugeRows = [{value: 72, target: 200}, {value: 10, target: 50}];

const funnelModel = {
  legend: {show: true},
  series: [{id: "steps", type: "funnel", name: "Sales", awe: {type: "funnel", xValue: "step", yValue: "count"}}],
  awe: {chartType: "funnel"}
};
const funnelRows = [{step: "Visits", count: 100}, {step: "Carts", count: 40}, {step: "Orders", count: 10}];

const rangeModel = {
  legend: {show: true},
  tooltip: {trigger: "axis"},
  xAxis: [{type: "category", awe: {axis: "x"}}],
  yAxis: [{type: "value", awe: {axis: "y"}}],
  series: [{
    id: "band", type: "line", name: "Temperature", areaStyle: {},
    awe: {type: "arearange", xValue: "day", yValue: "low", zValue: "high", range: true}
  }],
  awe: {chartType: "arearange"}
};
const rangeRows = [{day: "Mon", low: 10, high: 20}, {day: "Tue", low: 12, high: 25}, {day: "Wed", low: 8, high: 15}];

const models = {
  radar: [radarModel, radarRows], heatmap: [heatmapModel, heatmapRows], gauge: [gaugeModel, gaugeRows],
  funnel: [funnelModel, funnelRows], range: [rangeModel, rangeRows]
};

describe('awe-react-client/test/js/utilities/echartsChartTypesTest.jsx', () => {

  describe('radar', () => {
    it('should take the indicators from the distinct x values and draw one polygon for each series', () => {
      const option = build(radarModel, radarRows);

      expect(option.radar.indicator).toEqual([{name: "Speed"}, {name: "Power"}, {name: "Skill"}]);
      expect(option.series).toHaveLength(2);
      expect(option.series[0].data).toEqual([{name: "Alpha", value: [3, 4, 5]}]);
      expect(option.series[1].data).toEqual([{name: "Beta", value: [5, 2, 4]}]);
    });

    it('should not draw axes or a grid', () => {
      const option = build(radarModel, radarRows);

      expect(option.xAxis).toBeUndefined();
      expect(option.yAxis).toBeUndefined();
      expect(option.grid).toBeUndefined();
    });

    it('should keep the indicators that the model defines', () => {
      const model = {...radarModel, radar: {indicator: [{name: "Speed", max: 10}]}};

      expect(build(model, radarRows).radar.indicator).toEqual([{name: "Speed", max: 10}]);
    });
  });

  describe('heatmap', () => {
    it('should fill the categories of the axes and bind the points as [x, y, z]', () => {
      const option = build(heatmapModel, heatmapRows);

      expect(option.xAxis[0].data).toEqual(["Mon", "Tue"]);
      expect(option.yAxis[0].data).toEqual(["10h", "12h"]);
      expect(option.series[0].data).toEqual([["Mon", "10h", 4], ["Mon", "12h", 9], ["Tue", "10h", 1], ["Tue", "12h", 7]]);
    });

    it('should take the limits of the visual map from the z values unless the model sets them', () => {
      expect(build(heatmapModel, heatmapRows).visualMap).toMatchObject({min: 1, max: 9, calculable: true});

      const fixed = {...heatmapModel, visualMap: {...heatmapModel.visualMap, min: 0, max: 100}};
      expect(build(fixed, heatmapRows).visualMap).toMatchObject({min: 0, max: 100});
    });

    it('should leave room below the plot for a horizontal visual map', () => {
      const option = build(heatmapModel, heatmapRows);
      const plain = build({...heatmapModel, visualMap: undefined}, heatmapRows);

      expect(option.grid.bottom).toBeGreaterThan(plain.grid.bottom);
    });

    it('should add the room of the visual map to the room of rotated category labels, not overlap them', () => {
      const rows = ["Monday morning shift", "Tuesday morning shift", "Wednesday morning shift", "Thursday morning shift"]
        .map((day, index) => ({day, hour: "10h", visits: index + 1}));
      const option = build(heatmapModel, rows, {width: 320});
      const plain = build({...heatmapModel, visualMap: undefined}, rows, {width: 320});

      expect(option.xAxis[0].axisLabel.rotate).toBeDefined();
      expect(option.grid.bottom - plain.grid.bottom).toBe(VISUAL_MAP_ROOM);
    });
  });

  describe('heatmap in a mixed chart', () => {
    it('should keep numbers on the axes that are not category axes and strings on the category ones', () => {
      const rows = [{day: "Mon", hour: 10, visits: 4}, {day: "Tue", hour: 12, visits: 9}];
      const mixed = {...heatmapModel, yAxis: [{type: "value", awe: {axis: "y"}}]};

      const option = build(mixed, rows);

      expect(option.series[0].data).toEqual([["Mon", 10, 4], ["Tue", 12, 9]]);
      expect(option.yAxis[0].data).toBeUndefined();
    });

    it('should turn numbers into categories when both axes are category axes', () => {
      const rows = [{day: 1, hour: 10, visits: 4}];

      expect(build(heatmapModel, rows).series[0].data).toEqual([["1", "10", 4]]);
    });
  });

  describe('gauge', () => {
    it('should show the first value, and take the maximum from the first z value', () => {
      const [serie] = build(gaugeModel, gaugeRows).series;

      expect(serie.data).toEqual([{name: "Speed", value: 72}]);
      expect(serie.max).toBe(200);
    });

    it('should keep the maximum that the model defines', () => {
      const model = {...gaugeModel, series: [{...gaugeModel.series[0], max: 300}]};

      expect(build(model, gaugeRows).series[0].max).toBe(300);
    });
  });

  describe('funnel', () => {
    it('should bind the steps as name and value, coloured by step like a pie', () => {
      const [serie] = build(funnelModel, funnelRows).series;

      expect(serie.data).toEqual([{name: "Visits", value: 100}, {name: "Carts", value: 40}, {name: "Orders", value: 10}]);
      expect(serie.itemStyle).toBeUndefined();
    });

    it('should let the legend list the steps', () => {
      expect(build(funnelModel, funnelRows).legend.data).toBeUndefined();
    });
  });

  describe('range', () => {
    it('should draw the band with a hidden base series and a visible one with the real values', () => {
      const option = build(rangeModel, rangeRows);
      const [base, band] = option.series;

      expect(option.series).toHaveLength(2);
      expect(base.data).toEqual([["Mon", 10], ["Tue", 12], ["Wed", 8]]);
      expect(base.silent).toBe(true);
      expect(base.areaStyle).toBeUndefined();
      expect(base.stack).toBeDefined();
      expect(band.stack).toBe(base.stack);
      expect(band.name).toBe("Temperature");
      expect(band.data.map(point => point.value)).toEqual([["Mon", 10], ["Tue", 13], ["Wed", 7]]);
      expect(band.data.map(point => point.range)).toEqual([[10, 20], [12, 25], [8, 15]]);
    });

    it('should keep the hidden base out of the tooltip of the axis', () => {
      const option = build(rangeModel, rangeRows);
      const [base, band] = option.series;

      expect(base.tooltip).toEqual({show: false});
      expect(band.tooltip.show).not.toBe(false);
    });

    it('should list only the visible series, with low and high, in a formatted axis tooltip', () => {
      const model = {...rangeModel, tooltip: {trigger: "axis", awe: {headerFormat: "{point.key}<br/>"}}};
      const option = build(model, rangeRows);
      const params = [
        {seriesIndex: 0, seriesName: "", value: ["Mon", 10], data: ["Mon", 10], marker: ""},
        {seriesIndex: 1, seriesName: "Temperature", value: ["Mon", 10], data: {value: ["Mon", 10], range: [10, 20]}, marker: ""}
      ];

      const text = option.tooltip.formatter(params);

      expect(text).toContain("Temperature: 10 – 20");
      expect(text.match(/: /g)).toHaveLength(1);
    });

    it('should place the base and the band with swapped coordinates in an inverted chart', () => {
      const inverted = {
        ...rangeModel, awe: {...rangeModel.awe, inverted: true},
        xAxis: [{type: "value", awe: {axis: "y"}}], yAxis: [{type: "category", awe: {axis: "x"}}]
      };

      const [base, band] = build(inverted, rangeRows).series;

      expect(base.data).toEqual([[10, "Mon"], [12, "Tue"], [8, "Wed"]]);
      expect(band.data.map(point => point.value)).toEqual([[10, "Mon"], [13, "Tue"], [7, "Wed"]]);
      expect(band.data.map(point => point.range)).toEqual([[10, 20], [12, 25], [8, 15]]);
    });

    it('should list only the visible series in the legend', () => {
      expect(build(rangeModel, rangeRows).legend.data).toEqual(["Temperature"]);
    });

    it('should show low and high in the tooltip', () => {
      const [, band] = build(rangeModel, rangeRows).series;

      expect(band.tooltip.valueFormatter(10, 0)).toBe("10 – 20");
    });

    it('should describe the point with low as y and high as z in the formats of the tooltip', () => {
      const model = {...rangeModel, tooltip: {trigger: "axis", awe: {pointFormat: "{point.y}-{point.z}"}}};
      const option = build(model, rangeRows);
      const params = [{
        seriesIndex: 1, seriesName: "Temperature", value: ["Mon", 10], data: {value: ["Mon", 10], range: [10, 20]}
      }];

      expect(option.tooltip.formatter(params)).toBe("10-20");
    });

    it('should read the low value as the y of a click', () => {
      const {data} = build(rangeModel, rangeRows).series[1];

      expect(pointOf({value: data[1].value, data: data[1]})).toEqual({x: "Tue", y: 12});
    });
  });

  describe('events and actions', () => {
    it('should report the name of the series when a radar polygon is clicked', () => {
      expect(pointOf({componentSubType: "radar", name: "Alpha", value: [3, 4, 5]})).toEqual({x: "Alpha", y: null});
    });

    it.each([["radar", "radar"], ["heatmap", "heatmap"], ["gauge", "gauge"], ["funnel", "funnel"]])(
      'should translate a %s series of a chart action to an ECharts %s series', (type, expected) => {
        expect(getEChartsSerie({id: "s", type, xValue: "x", yValue: "y"}, false).type).toBe(expected);
      });

    it('should translate a range series of a chart action to a line with a band', () => {
      const serie = getEChartsSerie({id: "s", type: "arearange", xValue: "x", yValue: "low", zValue: "high"}, false);

      expect(serie).toMatchObject({type: "line", areaStyle: {}, awe: {range: true, yValue: "low", zValue: "high"}});
    });
  });

  describe('colours of the chrome', () => {
    it('should colour the texts of the visual map and the axes of a radar', () => {
      const colors = {text: "rgb(1, 1, 1)", muted: "rgb(2, 2, 2)", axis: "rgb(3, 3, 3)", split: "rgb(4, 4, 4)"};
      const option = applyChartColors({visualMap: {type: "continuous"}, radar: {indicator: []}, series: []}, colors);

      expect(option.visualMap.textStyle.color).toBe("rgb(1, 1, 1)");
      expect(option.radar.axisName.color).toBe("rgb(1, 1, 1)");
      expect(option.radar.splitLine.lineStyle.color).toBe("rgb(4, 4, 4)");
    });
  });

  describe('option', () => {
    it.each(Object.keys(models))('should not leave client hints in the %s option', (name) => {
      const [model, rows] = models[name];

      expect(hasAweKeys(model)).toBe(true);
      expect(hasAweKeys(build(model, rows))).toBe(false);
    });
  });

  describe('real ECharts', () => {
    let warn;
    let error;

    beforeAll(() => {
      // jsdom has no canvas, which ECharts uses to measure the texts
      jest.spyOn(HTMLCanvasElement.prototype, "getContext").mockImplementation(() => ({
        font: "", measureText: (text) => ({width: String(text).length * 7})
      }));
    });

    afterAll(() => {
      HTMLCanvasElement.prototype.getContext.mockRestore();
    });

    beforeEach(() => {
      warn = jest.spyOn(console, "warn").mockImplementation(() => {});
      error = jest.spyOn(console, "error").mockImplementation(() => {});
    });

    afterEach(() => {
      warn.mockRestore();
      error.mockRestore();
    });

    it.each(Object.keys(models))('should register what the %s chart needs and draw it without warnings', (name) => {
      const [model, rows] = models[name];

      const svg = renderSvg(build(model, rows), {width: 800, height: 400}, "en");

      expect(svg).toContain("<path");
      expect(warn).not.toHaveBeenCalled();
      expect(error).not.toHaveBeenCalled();
    });
  });
});
