import models from "../components/fixtures/advancedEchartsModels.json";
import {PALETTE, buildEChartsOption} from "../../../src/utilities/echartsOption";
import {renderSvg} from "../../../src/utilities/echartsSetup";
import {linkedNames} from "../../../src/utilities/echartsLegend";

const clone = (value) => JSON.parse(JSON.stringify(value));

const locale = {
  decimalPoint: ",",
  thousandsSep: ".",
  months: ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
    "noviembre", "diciembre"],
  noData: "No hay datos"
};

const context = (overrides = {}) => ({t: (text) => text, locale, width: 900, height: 500, ...overrides});
const build = (name, rows, overrides = {}, tweak = () => {}) => {
  const model = clone(models[name]);
  tweak(model);
  return buildEChartsOption(model, rows, context(overrides));
};

const pyramidRows = [
  {age: "25-29", men: -2500, women: 2200, dummyMax: 4000, dummyMin: -4000},
  {age: "30-34", men: -3200.4, women: 2800, dummyMax: 4000, dummyMin: -4000},
  {age: "35-39", men: -3900, women: 3300.6, dummyMax: 4000, dummyMin: -4000}
];

// The position of a series in the option is not the one of the model: the stacks are reversed
const itemOf = (option, id, name, value) => ({
  seriesIndex: option.series.findIndex(serie => serie.id === id),
  seriesName: option.series.find(serie => serie.id === id).name,
  name, value, marker: "<i></i>"
});

const allRows = Array.from({length: 6}, (_, index) => ({
  asset: `Asset ${index}`, amount: 100 + index * 10, date: new Date(2024, index, 1).getTime(), nav: 10 + index,
  risk: index + 1, return: 2 * index + 1, weight: 10 * (index + 1), manager: `Manager ${index}`, equity: 5 + index,
  fixed: 7 + index, money: 3 + index, age: `${20 + index * 5}-${24 + index * 5}`, men: -(2000 + index * 100),
  women: 1800 + index * 120, dummyMax: 4000, dummyMin: -4000, month: new Date(2024, index, 1).getTime(),
  staff: 100 + index * 7, year: `${2020 + index}`, company: 1000 + index * 50, employee: 800 + index * 40,
  category: `Category ${index}`, people: 10 + index
}));

describe('awe-react-client/test/js/utilities/echartsAdvancedOptionsTest.jsx', () => {

  describe('real ECharts', () => {
    beforeAll(() => {
      // jsdom has no canvas, which ECharts uses to measure the texts
      jest.spyOn(HTMLCanvasElement.prototype, "getContext").mockImplementation(() => ({
        font: "",
        measureText: (text) => ({width: String(text).length * 7})
      }));
    });

    afterAll(() => {
      HTMLCanvasElement.prototype.getContext.mockRestore();
    });

    it.each(Object.keys(models))('should draw %s without warnings', (name) => {
      const warn = jest.spyOn(console, "warn").mockImplementation(() => {});
      const error = jest.spyOn(console, "error").mockImplementation(() => {});

      const svg = renderSvg(build(name, allRows), {width: 900, height: 500}, "es-ES");

      expect(svg).toContain("<svg");
      expect(svg.length).toBeGreaterThan(500);
      expect(warn).not.toHaveBeenCalled();
      expect(error).not.toHaveBeenCalled();
      warn.mockRestore();
      error.mockRestore();
    });
  });

  describe('pyramid: HTML tooltip', () => {
    it('should build the tooltip of a point from the header, point and footer formats', () => {
      const option = build("AdvPyramid", pyramidRows);

      const html = option.tooltip.formatter(itemOf(option, "men", "30-34", [-3200.4, "30-34"]));

      expect(html).toBe("<table><tr><th colspan='2'>30-34</th></tr><tr><td>Salario bruto medio hombres: </td>" +
        "<td><b>3.200€</b></td></tr></table>");
    });

    it('should read the positive side of the pyramid with the same format', () => {
      const option = build("AdvPyramid", pyramidRows);

      const html = option.tooltip.formatter(itemOf(option, "women", "30-34", [2800, "30-34"]));

      expect(html).toContain("Salario bruto medio mujeres: </td><td><b>2.800€</b>");
    });

    it('should show the header once and one line per visible series of a shared tooltip', () => {
      const model = clone(models.AdvPyramid);
      model.tooltip.trigger = "axis";
      const option = buildEChartsOption(model, pyramidRows, context());

      const html = option.tooltip.formatter([
        itemOf(option, "men", "30-34", [-3200.4, "30-34"]),
        itemOf(option, "women", "30-34", [2800, "30-34"]),
        itemOf(option, "dummyMax", "30-34", [4000, "30-34"])
      ]);

      expect(html.match(/<th colspan='2'>30-34<\/th>/g)).toHaveLength(1);
      expect(html.match(/<tr><td>/g)).toHaveLength(2);
      expect(html).toContain("Salario bruto medio hombres");
      expect(html).toContain("Salario bruto medio mujeres");
      expect(html).not.toContain("max");
      expect(html.endsWith("</table>")).toBe(true);
    });

    it('should not add the marker of ECharts inside an html table', () => {
      const option = build("AdvPyramid", pyramidRows);

      expect(option.tooltip.formatter(itemOf(option, "men", "30-34", [-3200.4, "30-34"]))).not.toContain("<i>");
    });

    it('should escape the values that replace the expressions of an html format', () => {
      const option = build("AdvPyramid", pyramidRows);

      const html = option.tooltip.formatter(itemOf(option, "men", "<img src=x>", [-1, "<img src=x>"]));

      expect(html).not.toContain("<img");
      expect(html).toContain("&lt;img src=x&gt;");
    });

    it('should keep the basic html of a tooltip that is not html, like Highcharts does, and escape the values', () => {
      const model = clone(models.AdvPyramid);
      model.tooltip.awe = {
        headerFormat: "<i>{point.key}</i>", pointFormat: "<b>{point.y}</b><br>{series.name}", footerFormat: "<span style=\"color:red\">end</span>"
      };
      const option = buildEChartsOption(model, pyramidRows, context());

      const text = option.tooltip.formatter(itemOf(option, "men", "30-34", [-3200.4, "30-34"]));

      expect(text).toBe("<i>30-34</i><br/><i></i><b>-3200.4</b><br>Hombres<br/><span style=\"color:red\">end</span>");
      expect(option.tooltip.formatter(itemOf(option, "men", "<img src=x>", [-1, "<img src=x>"])))
        .not.toContain("<img");
    });

    it('should only format the pieces that the tooltip defines', () => {
      const model = clone(models.AdvPyramid);
      model.tooltip.awe = {useHTML: true, pointFormat: "<i>{point.y}</i>"};
      const option = buildEChartsOption(model, pyramidRows, context());

      const html = option.tooltip.formatter(itemOf(option, "men", "30-34", [-3200.4, "30-34"]));

      expect(html).toContain("<i>-3200.4</i>");
      expect(html).toContain("30-34");
    });

    it('should keep the font size of the tooltip text', () => {
      const model = clone(models.AdvPyramid);
      model.tooltip.textStyle = {fontSize: 12};

      expect(buildEChartsOption(model, pyramidRows, context()).tooltip.textStyle).toEqual({fontSize: 12});
    });
  });

  describe('tooltip of the series', () => {
    const areaRows = [{month: new Date(2024, 0, 1).getTime(), staff: 12}, {month: new Date(2024, 1, 1).getTime(), staff: 15}];

    it('should write the value of a series with its own suffix instead of the one of the tooltip', () => {
      const model = clone(models.AdvArea);
      model.series[0].awe.valueSuffix = " empleados";
      const option = buildEChartsOption(model, areaRows, context());

      expect(option.series[0].tooltip.valueFormatter(12)).toBe("12 empleados");
      expect(option.tooltip.valueFormatter(12)).toBe("12 personas");
    });

    it('should use the suffix of the series in the default line of the tooltip', () => {
      const model = clone(models.AdvArea);
      model.series[0].awe.valueSuffix = " empleados";
      const option = buildEChartsOption(model, areaRows, context());

      const html = option.tooltip.formatter({seriesIndex: 0, seriesName: "Plantilla", marker: "", value: [areaRows[1].month, 15]});

      expect(html).toContain("febrero 2024");
      expect(html).toContain("Plantilla: 15 empleados");
    });

    it('should show the default header of the html tooltip when only the point is formatted', () => {
      const model = clone(models.AdvArea);
      model.tooltip.awe = {useHTML: true, pointFormat: "<b>{point.y}</b>", dateFormat: "%B %Y"};
      const option = buildEChartsOption(model, areaRows, context());

      const html = option.tooltip.formatter({seriesIndex: 0, seriesName: "Plantilla", value: [areaRows[1].month, 15]});

      expect(html).toBe("<span style=\"font-size:0.8em\">febrero 2024</span><br/><b>15</b>");
    });
  });

  describe('rounded bars', () => {
    const columnRows = [
      {year: "2020", company: 1000, employee: 800},
      {year: "2021", company: 1100, employee: 900},
      {year: "2022", company: null, employee: 950}
    ];
    const pointsOf = (serie) => serie.data.map(point => ({
      value: Array.isArray(point) ? point : point.value, radius: Array.isArray(point) ? undefined : point.itemStyle?.borderRadius
    }));
    const byId = (option, id) => option.series.find(serie => serie.id === id);

    it('should round only the outermost series of a stack, after the stacks are reversed', () => {
      const option = build("AdvColumns", columnRows);

      // Highcharts draws the first series on top, so ECharts gets it last
      expect(option.series.map(serie => serie.id)).toEqual(["employee", "company"]);
      expect(pointsOf(byId(option, "company"))[0].radius).toEqual([expect.any(Number), expect.any(Number), 0, 0]);
      expect(pointsOf(byId(option, "employee"))[0].radius).toBeUndefined();
      expect(pointsOf(byId(option, "employee"))[1].radius).toBeUndefined();
    });

    it('should round the next series when the outermost has no value in a category', () => {
      const option = build("AdvColumns", columnRows);

      expect(pointsOf(byId(option, "company"))[2].radius).toBeUndefined();
      expect(pointsOf(byId(option, "employee"))[2].radius).toEqual([expect.any(Number), expect.any(Number), 0, 0]);
    });

    it('should approximate a percentage radius in pixels from the width of the bars', () => {
      const narrow = pointsOf(byId(build("AdvColumns", columnRows, {width: 300}), "company"))[0].radius[0];
      const wide = pointsOf(byId(build("AdvColumns", columnRows, {width: 900}), "company"))[0].radius[0];

      expect(narrow).toBeGreaterThan(0);
      expect(wide).toBeGreaterThan(narrow);
      // 30% of a bar that takes about a third of a 900 pixels wide chart (three categories)
      expect(wide).toBeLessThanOrEqual(0.3 * 300);
    });

    it('should take the radius percentage from an explicit bar width', () => {
      const option = build("AdvColumns", columnRows, {width: 900}, (model) => {
        model.series.forEach(serie => {
          serie.barWidth = 40;
        });
      });

      expect(pointsOf(byId(option, "company"))[0].radius[0]).toBe(12);
    });

    it('should cap the bar width with the maximum width of the bars', () => {
      const option = build("AdvColumns", columnRows, {width: 900}, (model) => {
        model.series.forEach(serie => {
          serie.barMaxWidth = 20;
        });
      });

      expect(pointsOf(byId(option, "company"))[0].radius[0]).toBe(6);
    });

    it('should read the gaps between categories from any series, as pixels or percentages', () => {
      const radius = (tweak) => pointsOf(byId(build("AdvColumns", columnRows, {width: 900}, tweak), "company"))[0]
        .radius[0];

      // 868 pixels for three categories, minus a gap of 20 pixels, makes bars of about 269 pixels
      expect(radius((model) => {
        delete model.series[0].barCategoryGap;
        delete model.series[1].barCategoryGap;
        model.series[1].barCategoryGap = 20;
      })).toBe(81);
      // The default gap is a percentage of the bar, and a larger one makes narrower bars
      expect(radius((model) => {
        model.series[0].barCategoryGap = "100%";
        model.series[1].barCategoryGap = "100%";
      })).toBe(43);
    });

    it('should round the end away from zero of each side of a pyramid', () => {
      const option = build("AdvPyramid", pyramidRows);

      expect(pointsOf(byId(option, "men"))[0].radius).toEqual([20, 0, 0, 20]);
      expect(pointsOf(byId(option, "women"))[0].radius).toEqual([0, 20, 20, 0]);
    });

    it('should keep the data labels of the points that are rounded', () => {
      const option = build("AdvColumns", columnRows, {}, (model) => {
        model.series[0].label = {show: true};
      });

      const [first] = byId(option, "company").data;

      expect(first.itemStyle.borderRadius).toBeDefined();
      expect(first.label.position).toBe("inside");
    });

    it('should leave unrounded bars as they are', () => {
      const model = clone(models.AdvColumns);
      model.series.forEach(serie => delete serie.awe.borderRadius);
      const option = buildEChartsOption(model, columnRows, context());

      expect(option.series[0].data).toEqual([["2020", 800], ["2021", 900], ["2022", 950]]);
    });

    it('should round every bar of a series that is not stacked', () => {
      const model = clone(models.AdvColumns);
      model.series.forEach(serie => delete serie.stack);
      model.series[0].awe.borderRadius = 4;
      model.series[1].awe.borderRadius = 4;
      const option = buildEChartsOption(model, columnRows, context());

      expect(pointsOf(byId(option, "company"))[0].radius).toEqual([4, 4, 0, 0]);
      expect(pointsOf(byId(option, "employee"))[0].radius).toEqual([4, 4, 0, 0]);
    });

    it('should round the bottom of the negative bars of a vertical chart', () => {
      const model = clone(models.AdvColumns);
      model.series.forEach(serie => {
        serie.awe.borderRadius = 5;
      });
      const option = buildEChartsOption(model, [{year: "2020", company: -10, employee: -4}], context());

      expect(pointsOf(byId(option, "company"))[0].radius).toEqual([0, 0, 5, 5]);
    });
  });

  describe('legend', () => {
    it('should order the legend by legendIndex', () => {
      const option = build("AdvColumns", []);

      expect(option.legend.data).toEqual(["Empleado", "Empresa"]);
    });

    it('should leave out the series that are hidden or linked to another one', () => {
      const option = build("AdvPyramid", pyramidRows);

      expect(option.legend.data).toEqual(["Hombres", "Mujeres"]);
    });

    it('should give a linked series the name of the series it is linked to, so that they toggle together', () => {
      const option = build("AdvPyramid", pyramidRows);

      expect(option.series.find(serie => serie.id === "dummyMin").name).toBe("max");
      expect(option.series.find(serie => serie.id === "dummyMax").name).toBe("max");
    });

    it('should keep its own name in a linked series that shows in the tooltip, and leave it out of the legend', () => {
      const model = clone(models.AdvPyramid);
      delete model.series[3].silent;
      const option = buildEChartsOption(model, pyramidRows, context());

      expect(option.series.find(serie => serie.id === "dummyMin").name).toBe("min");
      expect(option.legend[0].data).toEqual(["Hombres", "Mujeres"]);
    });

    it('should toggle a linked series that shows in the tooltip through a hidden legend', () => {
      const model = clone(models.AdvPyramid);
      delete model.series[3].silent;
      const option = buildEChartsOption(model, pyramidRows, context());

      expect(option.legend).toHaveLength(2);
      expect(option.legend[0].data).toEqual(["Hombres", "Mujeres"]);
      expect(option.legend[1]).toEqual({show: false, data: ["min"]});
      expect(linkedNames(model.series, "max", (text) => text)).toEqual(["min"]);
      expect(linkedNames(model.series, "Hombres", (text) => text)).toEqual([]);
    });

    it('should not need a hidden legend when the linked series are silent or the legend is off', () => {
      const model = clone(models.AdvPyramid);
      expect(Array.isArray(buildEChartsOption(model, pyramidRows, context()).legend)).toBe(false);
      delete model.series[3].silent;
      model.legend.show = false;
      expect(Array.isArray(buildEChartsOption(model, pyramidRows, context()).legend)).toBe(false);
    });

    it('should link a series to another one by its id', () => {
      const model = clone(models.AdvPyramid);
      model.series[3].awe.linkedTo = "men";
      const option = buildEChartsOption(model, pyramidRows, context());

      expect(option.series.find(serie => serie.id === "dummyMin").name).toBe("Hombres");
    });

    it('should hide the legend when no series is listed', () => {
      const model = clone(models.AdvColumns);
      model.series.forEach(serie => {
        serie.awe.showInLegend = false;
      });

      expect(buildEChartsOption(model, [], context()).legend.show).toBe(false);
    });

    it('should let a pie list its slices', () => {
      expect(build("AdvPie3d", []).legend.data).toBeUndefined();
    });

    it('should translate the names of the legend', () => {
      const option = build("AdvColumns", [], {t: (text) => `T(${text})`});

      expect(option.legend.data).toEqual(["T(Empleado)", "T(Empresa)"]);
    });
  });

  describe('pie labels', () => {
    const pieRows = Array.from({length: 4}, (_, index) => ({asset: `Asset ${index}`, amount: 100 + index * 50}));

    it('should keep the radius of the chart in a wide panel', () => {
      const option = build("AdvPieLabels", pieRows, {width: 900, height: 500});

      expect(option.series[0].radius).toBeUndefined();
    });

    it('should shrink the pie in a narrow panel so that the outer labels fit', () => {
      const option = build("AdvPieLabels", pieRows, {width: 190, height: 400});

      expect(option.series[0].radius).toBe(30);
    });

    it('should shrink the pie to the room left by the labels in a medium panel', () => {
      const medium = build("AdvPieLabels", pieRows, {width: 400, height: 400}).series[0].radius;

      expect(medium).toBeGreaterThan(30);
      expect(medium).toBeLessThan(0.6 * 200);
    });

    it('should shrink the inner radius of a donut with the outer one', () => {
      const option = build("AdvPieLabels", pieRows, {width: 190, height: 400}, (model) => {
        model.series[0].radius = ["40%", "75%"];
      });
      const [inner, outer] = option.series[0].radius;

      expect(outer).toBe(30);
      expect(inner).toBeGreaterThan(0);
      expect(inner).toBeLessThan(outer);
    });

    it('should not shrink a pie whose labels are inside, or whose size is unknown', () => {
      const inside = build("AdvPieLabels", pieRows, {width: 190, height: 400}, (model) => {
        model.series[0].label.position = "inside";
      });

      expect(inside.series[0].radius).toBeUndefined();
      expect(build("AdvPieLabels", pieRows, {width: 0, height: 0}).series[0].radius).toBeUndefined();
    });
  });

  describe('series colors', () => {
    it('should use the colors of the chart as the palette of the series and of the points', () => {
      const option = build("AdvPie3d", [{category: "A", people: 1}, {category: "B", people: 2}]);

      expect(option.color).toEqual(models.AdvPie3d.color);
      expect(option.series[0].colorBy).toBe("data");
      expect(option.series[0].selectedMode).toBe("single");
    });

    it('should draw the series with the colors of the chart', () => {
      const model = clone(models.AdvStacked);
      model.color = ["#111111", "#222222", "#333333"];
      const option = buildEChartsOption(model, [], context());

      expect(option.color).toEqual(model.color);
      expect(option.series.map(serie => serie.itemStyle.color)).toEqual(["#333333", "#222222", "#111111"]);
    });

    it('should keep the default palette when the chart has no colors', () => {
      expect(build("AdvStacked", []).color).toEqual(PALETTE);
    });

    it('should let the points of a bar series take the palette when it colors by point', () => {
      const model = clone(models.AdvStacked);
      model.series[0].colorBy = "data";
      const option = buildEChartsOption(model, [], context());

      expect(option.series.find(serie => serie.id === model.series[0].id).itemStyle?.color).toBeUndefined();
    });

    it('should give the marker fill to the symbols and keep the color of the series for the line', () => {
      const [serie] = build("AdvArea", [{month: 1, staff: 2}]).series;

      expect(serie.lineStyle.color).toBe("#8cac41");
      expect(serie.itemStyle.color).toBe("#FFFFFF");
      expect(serie.symbol).toBe("circle");
      // The fill of the area is a gradient, which keeps its own colors
      expect(serie.areaStyle.color.type).toBe("linear");
    });

    it('should keep the palette color for the line and the area when the series has no color of its own', () => {
      const option = build("AdvArea", [{month: 1, staff: 2}], {}, (model) => {
        delete model.series[0].itemStyle;
        model.series[0].areaStyle = {origin: "start"};
      });
      const [serie] = option.series;

      expect(serie.lineStyle.color).toBe(PALETTE[0]);
      expect(serie.areaStyle.color).toBe(PALETTE[0]);
      expect(serie.itemStyle.color).toBe("#FFFFFF");
    });

    it('should resolve the palette color before the marker fill, also when the points take the palette', () => {
      const option = build("AdvArea", [{month: 1, staff: 2}], {}, (model) => {
        delete model.series[0].itemStyle;
        model.series[0].colorBy = "data";
      });
      const [serie] = option.series;

      expect(serie.lineStyle.color).toBe(PALETTE[0]);
      expect(serie.lineStyle.color).not.toBeUndefined();
      expect(serie.itemStyle.color).toBe("#FFFFFF");
    });

    it('should let the palette advance for a series whose markers have a fill', () => {
      const option = build("AdvArea", [{month: 1, staff: 2}], {}, (model) => {
        delete model.series[0].itemStyle;
        model.series.push({...clone(model.series[0]), id: "second", awe: {...model.series[0].awe, markerFill: undefined}});
      });

      expect(option.series[1].itemStyle.color).toBe(PALETTE[1]);
    });
  });

  describe('areas and bubbles', () => {
    it('should fit the value axis to the data of an area that fills from the bottom of the axis', () => {
      const option = build("AdvArea", [{month: 1, staff: 2}]);

      expect(option.series[0].areaStyle.origin).toBe("start");
      expect(option.yAxis[0].scale).toBe(true);
    });

    it('should keep zero in the axis of an area that fills from zero', () => {
      const model = clone(models.AdvArea);
      delete model.series[0].areaStyle.origin;

      expect(buildEChartsOption(model, [{month: 1, staff: 2}], context()).yAxis[0].scale).toBeUndefined();
    });

    it('should size the bubbles in percentages of the smaller dimension of the chart', () => {
      const rows = [{risk: 1, return: 2, weight: 5}, {risk: 3, return: 4, weight: 15}];
      const option = build("AdvBubble", rows, {width: 800, height: 400}, (model) => {
        model.series[0].awe.minSize = "5%";
        model.series[0].awe.maxSize = "20%";
      });

      expect(option.series[0].symbolSize([1, 2, 5])).toBe(20);
      expect(option.series[0].symbolSize([3, 4, 15])).toBe(80);
    });

    it('should use the default sizes when the sizes of the bubbles are not valid', () => {
      const rows = [{risk: 1, return: 2, weight: 5}, {risk: 3, return: 4, weight: 15}];
      const option = build("AdvBubble", rows, {width: 800, height: 400}, (model) => {
        model.series[0].awe.minSize = "abc";
        model.series[0].awe.maxSize = {};
      });

      expect(option.series[0].symbolSize([1, 2, 5])).toBe(10);
      expect(option.series[0].symbolSize([3, 4, 15])).toBe(50);
    });

    it('should size the bubbles between the minimum and maximum size of the chart', () => {
      const rows = [{risk: 1, return: 2, weight: 5}, {risk: 2, return: 3, weight: 10}, {risk: 3, return: 4, weight: 15}];
      const {symbolSize} = build("AdvBubble", rows).series[0];

      expect(symbolSize([1, 2, 5])).toBe(10);
      expect(symbolSize([2, 3, 10])).toBe(25);
      expect(symbolSize([3, 4, 15])).toBe(40);
    });
  });

  describe('pyramid: formats of labels and axes', () => {
    it('should format the data labels with the condition of the template', () => {
      const option = build("AdvPyramid", pyramidRows);

      const label = (id) => option.series.find(serie => serie.id === id).label.formatter;

      expect(label("men")({value: [-3200.4, "30-34"]})).toBe("3.200€");
      expect(label("women")({value: [2800, "30-34"]})).toBe("2.800€");
    });

    it('should format the labels of the value axis in thousands on both sides of zero', () => {
      const {formatter} = build("AdvPyramid", pyramidRows).xAxis[0].axisLabel;

      expect(formatter(-4000)).toBe("4k");
      expect(formatter(2500)).toBe("2.5k");
    });

    it('should hide the grid lines of the axis', () => {
      expect(build("AdvPyramid", pyramidRows).xAxis[0].splitLine).toEqual({show: false});
    });

    it('should expose the custom keys of a series to the formats', () => {
      const model = clone(models.AdvPyramid);
      model.series[0].awe.labelFormat = "{series.userOptions.fullname}: {point.y}";
      const option = buildEChartsOption(model, pyramidRows, context());

      expect(option.series.find(serie => serie.id === "men").label.formatter({value: [-3200.4, "30-34"]}))
        .toBe("Salario bruto medio hombres: -3200.4");
    });

    it('should keep the hints out of the option', () => {
      const option = build("AdvPyramid", pyramidRows);

      expect(JSON.stringify(option, (key, value) => typeof value === "function" ? "fn" : value)).not.toContain("\"awe\"");
    });
  });
});
