import models from "../components/fixtures/chrTstEchartsModels.json";
import {
  PALETTE,
  buildEChartsOption,
  findDrilldownId,
  hasAweKeys,
  stripAwe
} from "../../../src/utilities/echartsOption";

const clone = (value) => JSON.parse(JSON.stringify(value));
const upper = (text) => `T(${text})`;

const day = (day) => new Date(2024, 0, day).getTime();
const values = [
  {dates: day(1), serie1: 10.12345, serie2: 5, serie3: 1, names: "Chrome", subserie1: 3, Ord: 2, serie1_1: 7, serie1_2: 100},
  {dates: day(2), serie1: 20, serie2: 6, serie3: 2, names: "Firefox", subserie1: 4, Ord: 3, serie1_1: 8, serie1_2: 300},
  {dates: day(3), serie1: null, serie2: 7, serie3: 3, names: "Edge", subserie1: 5, Ord: 4, serie1_1: 9, serie1_2: 200}
];

const locale = {
  decimalPoint: ".",
  thousandsSep: ",",
  noData: "No data to display",
  drillUpText: "Back to {series.name}"
};

const context = (overrides = {}) => ({t: (text) => text, locale, width: 800, height: 400, ...overrides});
const build = (name, data = values, overrides = {}) => buildEChartsOption(clone(models[name]), data, context(overrides));
const graphicTexts = (option) => (option.graphic || []).map(element => element.style?.text);

const collectAweKeys = (node, path = "") => {
  if (Array.isArray(node)) {
    return node.flatMap((item, index) => collectAweKeys(item, `${path}/${index}`));
  }
  if (node && typeof node === "object") {
    return Object.entries(node).flatMap(([key, value]) =>
      key === "awe" ? [`${path}/awe`] : collectAweKeys(value, `${path}/${key}`));
  }
  return [];
};

describe('awe-react-client/test/js/utilities/echartsOptionTest.jsx', () => {

  describe('stripAwe', () => {
    it('should remove the awe key at any depth without changing the input', () => {
      const input = {awe: {a: 1}, series: [{awe: {b: 2}, name: "s", data: [1, 2]}], nested: {awe: 1, keep: true}};
      const fn = () => 1;
      input.formatter = fn;

      const output = stripAwe(input);

      expect(output).toEqual({series: [{name: "s", data: [1, 2]}], nested: {keep: true}, formatter: fn});
      expect(input.awe).toEqual({a: 1});
      expect(hasAweKeys(input)).toBe(true);
      expect(hasAweKeys(output)).toBe(false);
    });
  });

  describe('model', () => {
    it('should keep an option free of awe hints for every chart', () => {
      Object.keys(models).forEach(name => {
        expect(collectAweKeys(build(name))).toEqual([]);
      });
    });

    it('should render an empty chart with the no data message when the server sent no echartsModel', () => {
      const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

      const option = buildEChartsOption(undefined, values, context());

      expect(option.series).toEqual([]);
      expect(graphicTexts(option)).toEqual(["No data to display"]);
      expect(warn).toHaveBeenCalledWith(expect.stringContaining("echartsModel"));
      warn.mockRestore();
    });

    it('should use a transparent background', () => {
      expect(build("ChrLinTst").backgroundColor).toBe("rgba(0, 0, 0, 0)");
    });

    it('should show the no data message when the series have no points', () => {
      const option = build("ChrLinTst", []);

      expect(graphicTexts(option)).toEqual(["No data to display"]);
      expect(option.graphic[0]).toMatchObject({type: "text", left: "center", top: "middle"});
    });

    it('should not show the no data message when there are points', () => {
      expect(graphicTexts(build("ChrLinTst"))).toEqual([]);
    });
  });

  describe('data binding', () => {
    it('should bind the x and y values of a cartesian series', () => {
      const option = build("ChrLinTst");

      expect(option.series[0].data).toEqual([[day(1), 10.12345], [day(2), 20], [day(3), null]]);
      expect(option.series[1].data).toEqual([[day(1), 5], [day(2), 6], [day(3), 7]]);
    });

    it('should swap the pairs of an inverted chart', () => {
      const option = build("ChrBarHorTst");

      expect(option.series[0].data.map(point => point.value)).toEqual([[10.12345, day(1)], [20, day(2)], [null, day(3)]]);
      expect(option.xAxis[0].type).toBe("value");
      expect(option.yAxis[0].type).toBe("time");
    });

    it('should bind pie points by name and value', () => {
      const option = build("ChrPieTst");

      expect(option.series[0].data).toEqual([
        {name: "Chrome", value: 10.12345},
        {name: "Firefox", value: 20},
        {name: "Edge", value: null}
      ]);
    });

    it('should add the z value of a bubble as a third coordinate', () => {
      const option = build("ChrBubTst");

      expect(option.series[0].data).toEqual([[2, 7, 100], [3, 8, 300], [4, 9, 200]]);
    });

    it('should not spread a numeric z value (it is a single coordinate)', () => {
      const option = build("ChrScaTst");

      expect(option.series[0].data[0]).toHaveLength(3);
      expect(option.series[0].data[0][2]).toBe(100);
    });

    it('should size the bubbles from their z value', () => {
      const {symbolSize} = build("ChrBubTst").series[0];

      expect(symbolSize([2, 7, 100])).toBe(10);
      expect(symbolSize([3, 8, 300])).toBe(50);
      expect(symbolSize([4, 9, 200])).toBe(30);
    });

    it('should size every bubble the same when they have the same z value', () => {
      const same = values.map(row => ({...row, serie1_2: 5}));

      const {symbolSize} = build("ChrBubTst", same).series[0];

      expect(symbolSize([1, 1, 5])).toBe(30);
    });

    it('should normalize percent stacked series', () => {
      const model = clone(models.ChrBarTst);
      model.awe.stacking = "percent";
      model.series.forEach(serie => {
        serie.awe.stackPercent = true;
      });

      const option = buildEChartsOption(model, values, context());
      const byId = Object.fromEntries(option.series.map(serie => [serie.id, serie.data]));

      // first row: 10.12345 + 5 + 1
      expect(byId["serie2-1"][0][1]).toBeCloseTo(10.12345 / 16.12345 * 100, 5);
      expect(byId["serie2-2"][0][1]).toBeCloseTo(5 / 16.12345 * 100, 5);
      expect(byId["serie2-3"][0][1]).toBeCloseTo(1 / 16.12345 * 100, 5);
    });
  });

  describe('drilldown', () => {
    it('should find the series that opens from a series', () => {
      expect(findDrilldownId(models.ChrPieTst, "serie1")).toBe("serie1_1");
      expect(findDrilldownId(models.ChrPieTst, "other")).toBeNull();
      expect(findDrilldownId(models.ChrLinTst, "serie-1")).toBeNull();
      expect(findDrilldownId(undefined, "serie1")).toBeNull();
    });

    it('should replace the series with the drilldown one and show a back control', () => {
      const onBack = jest.fn();

      const option = build("ChrPieTst", values, {drill: {from: "serie1", to: "serie1_1"}, onBack});

      expect(option.series).toHaveLength(1);
      expect(option.series[0].id).toBe("serie1_1");
      expect(option.series[0].name).toBe("SubThemes");
      expect(option.series[0].data).toEqual([
        {name: "Chrome", value: 3},
        {name: "Firefox", value: 4},
        {name: "Edge", value: 5}
      ]);
      expect(graphicTexts(option)).toEqual(["Back to Themes"]);
      option.graphic[0].onclick();
      expect(onBack).toHaveBeenCalledTimes(1);
    });

    it('should ignore a drill that does not exist', () => {
      const option = build("ChrPieTst", values, {drill: {from: "serie1", to: "missing"}});

      expect(option.series[0].id).toBe("serie1");
      expect(graphicTexts(option)).toEqual([]);
    });
  });

  describe('fixes from the comparison with Highcharts', () => {
    it('should let a value axis that holds only lines or points fit its data', () => {
      const mixed = build("ChrLinTst");
      expect(mixed.yAxis[0].scale).toBeUndefined();
      expect(mixed.yAxis[1].scale).toBe(true);
      expect(build("ChrStockTst").yAxis[0].scale).toBe(true);
      expect(build("ChrScaTst").yAxis[0].scale).toBe(true);
    });

    it('should keep zero in value axes that hold areas or bars', () => {
      expect(build("ChrAreTst").yAxis[0].scale).toBeUndefined();
      expect(build("ChrBarTst").yAxis[0].scale).toBeUndefined();
    });

    it('should apply the scale to the horizontal value axis of an inverted chart only when needed', () => {
      expect(build("ChrBarHorTst").xAxis[0].scale).toBeUndefined();
    });

    it('should hide overlapping labels of time axes and ignore their interval', () => {
      const option = build("ChrBarHorTst");

      expect(option.yAxis[0].axisLabel.hideOverlap).toBe(true);
      expect(option.yAxis[0].interval).toBeUndefined();
      expect(option.xAxis[0].interval).toBe(1);
      expect(build("ChrLinTst").xAxis[0].axisLabel.hideOverlap).toBe(true);
    });

    it('should hide overlapping labels of value axes, which a fixed interval can crowd', () => {
      expect(build("ChrBarHorTst").xAxis[0].axisLabel.hideOverlap).toBe(true);
      expect(build("ChrLinTst").yAxis[0].axisLabel.hideOverlap).toBe(true);
    });

    it('should shrink the pie radius and lower its center when there is a title', () => {
      const pie = build("ChrPieTst").series[0];
      const donut = build("ChrDonutTst").series[0];

      expect(pie.radius).toBe("60%");
      expect(pie.center).toEqual(["50%", "54%"]);
      expect(donut.radius).toEqual(["24%", "60%"]);
    });

    it('should keep the center of a semicircle', () => {
      expect(build("ChrSemiCircleTst").series[0].center).toEqual(["50%", "75%"]);
    });

    it('should not move the center of a pie without title', () => {
      const model = clone(models.ChrPieTst);
      delete model.title;

      const option = buildEChartsOption(model, values, context());

      expect(option.series[0].center).toBeUndefined();
    });

    it('should turn the vertical offset of a middle title into a numeric top', () => {
      const option = build("ChrSemiCircleTst", values, {height: 400});

      expect(option.title.top).toBe(238);
    });

    it('should keep the title top when it has no offset', () => {
      expect(build("ChrLinTst").title.top).toBeUndefined();
    });

    it('should hide the symbols of long lines', () => {
      const many = Array.from({length: 41}, (_, index) => ({dates: day(1) + index, serie1: index, serie2: index}));

      expect(build("ChrStockTst", many).series[0].showSymbol).toBe(false);
      expect(build("ChrStockTst").series[0].showSymbol).toBeUndefined();
    });

    it('should add a grid with room for the title, the legend and the slider', () => {
      const plain = build("ChrLinTst").grid;
      const stock = build("ChrStockTst").grid;
      const withLegend = build("ChrBarHorTst");

      expect(plain.top).toBeGreaterThan(60);
      expect(plain.outerBoundsMode).toBe("same");
      // slider dataZoom and legend at the bottom
      expect(stock.bottom).toBeGreaterThan(plain.bottom);
      // the legend of the horizontal bars is on the right, but it is hidden
      expect(withLegend.grid.right).toBe(plain.right);
    });

    it('should leave room for a visible vertical legend', () => {
      const model = clone(models.ChrBarHorTst);
      model.legend.show = true;

      const option = buildEChartsOption(model, values, context());

      expect(option.grid.right).toBeGreaterThan(build("ChrBarHorTst").grid.right);
    });

    it('should leave room for a visible legend at the bottom', () => {
      const model = clone(models.ChrLinTst);
      model.legend.show = true;

      const option = buildEChartsOption(model, values, context());

      expect(option.grid.bottom).toBeGreaterThan(build("ChrLinTst").grid.bottom);
    });

    it('should draw the first series of a stack on top, as Highcharts does', () => {
      const option = build("ChrBarTst");

      expect(option.series.map(serie => serie.id)).toEqual(["serie2-3", "serie2-2", "serie2-1"]);
      expect(option.legend.data).toEqual(["SCREEN_TEXT_CHART_SERIE_1", "SCREEN_TEXT_CHART_SERIE_2",
        "SCREEN_TEXT_CHART_SERIE_3"]);
    });

    it('should keep the order of the series that are not stacked', () => {
      const option = build("ChrLinTst");

      expect(option.series.map(serie => serie.id)).toEqual(["serie-1", "serie-2"]);
    });
  });

  describe('formatters', () => {
    it('should format the labels of an axis with the named formatter', () => {
      const {formatter} = build("ChrLinTst").yAxis[0].axisLabel;

      expect(formatter(2500000)).toBe("2.5M");
      expect(formatter(12)).toBe("12");
    });

    it('should format the labels of an axis with its label format', () => {
      const {formatter} = build("ChrBarHorTst").xAxis[0].axisLabel;

      expect(formatter(21)).toBe("21 ºC");
    });

    it('should format the date labels of a time axis', () => {
      const {formatter} = build("ChrAreTst").xAxis[0].axisLabel;

      expect(formatter).toEqual({day: "{yyyy}-{MM}-{dd}"});
    });

    it('should format the data labels of an inverted chart from the y coordinate', () => {
      const {formatter} = build("ChrBarHorTst").series[0].label;

      expect(formatter({value: [3.14159, day(1)], seriesName: "Serie 1"})).toBe("3.142");
    });

    it('should format the data labels of a pie with the name and the percentage', () => {
      const {formatter} = build("ChrPieTst").series[0].label;

      expect(formatter({name: "Chrome", value: 10, percent: 37.456})).toBe("Chrome: 37.5 %");
    });

    it('should format the data labels of a bubble', () => {
      const {formatter} = build("ChrBubTst").series[0].label;

      expect(formatter({value: [2, 7.456, 100]})).toBe("7.46");
    });

    it('should format the tooltip values with decimals, prefix and suffix', () => {
      const {valueFormatter} = build("ChrLinTst").tooltip;

      expect(valueFormatter(1.5)).toBe("1.500 ºC");
    });

    it('should format the date of the tooltip', () => {
      const {formatter} = build("ChrBarHorTst").tooltip;

      const html = formatter({seriesName: "Serie 1", marker: "<i></i>", value: [3.14159, day(5)]});

      expect(html).toContain("2024-01-05");
      expect(html).toContain("Serie 1");
      expect(html).toContain("3.142 ºC");
    });

    it('should format each point of a shared tooltip with the point format', () => {
      const model = clone(models.ChrLinTst);
      model.tooltip.awe.pointFormat = "{series.name}: <b>{point.y:.1f}</b>";

      const {formatter} = buildEChartsOption(model, values, context()).tooltip;
      const html = formatter([
        {seriesName: "Serie 1", marker: "<i>1</i>", value: [day(2), 20]},
        {seriesName: "Serie 2", marker: "<i>2</i>", value: [day(2), 6.25]}
      ]);

      expect(html).toContain("Serie 1: <b>20.0</b>");
      expect(html).toContain("Serie 2: <b>6.3</b>");
      expect(html).toContain("<i>2</i>");
    });

    it('should escape the names shown in the tooltip', () => {
      const {formatter} = build("ChrBarHorTst").tooltip;

      const html = formatter({seriesName: "<img src=x>", marker: "", value: [1, day(1)]});

      expect(html).not.toContain("<img");
    });

    it('should show the name and the value of a pie in the tooltip', () => {
      const model = clone(models.ChrPieTst);
      model.tooltip.awe.pointFormat = "{point.name}: {point.y}";

      const {formatter} = buildEChartsOption(model, values, context()).tooltip;

      expect(formatter({seriesName: "Themes", name: "Chrome", marker: "", value: 4, percent: 50}))
        .toContain("Chrome: 4");
    });

    it('should not replace the default tooltip when there is nothing to format', () => {
      const model = clone(models.ChrLinTst);
      delete model.tooltip.awe;

      expect(buildEChartsOption(model, values, context()).tooltip.formatter).toBeUndefined();
      expect(buildEChartsOption(model, values, context()).tooltip.valueFormatter).toBeUndefined();
    });

    it('should use the decimal point of the locale', () => {
      const {valueFormatter} = build("ChrLinTst", values, {locale: {...locale, decimalPoint: ",", thousandsSep: "."}})
        .tooltip;

      expect(valueFormatter(1234.5)).toBe("1.234,500 ºC");
    });
  });

  describe('i18n', () => {
    it('should translate titles, axis names and series names', () => {
      const option = build("ChrLinTst", values, {t: upper});

      expect(option.title.text).toBe("T(SCREEN_TEXT_CHART_TITLE_1)");
      expect(option.title.subtext).toBe("T(Subtitulo) T(grafico) T(1)");
      expect(option.xAxis[0].name).toBe("T(Fechassss)");
      expect(option.yAxis[0].name).toBe("T(Temperaturas) T((ºC))");
      expect(option.series[0].name).toBe("T(Serie) T(1)");
    });

    it('should translate the name of the drilldown series and the back text', () => {
      const option = build("ChrPieTst", values, {t: upper, drill: {from: "serie1", to: "serie1_1"}});

      expect(option.series[0].name).toBe("T(SubThemes)");
      expect(graphicTexts(option)).toEqual(["Back to T(Themes)"]);
    });

    it('should translate and show the title of a visible legend', () => {
      const model = clone(models.ChrLinTst);
      model.legend.show = true;

      const option = buildEChartsOption(model, values, context({t: upper}));

      expect(graphicTexts(option)).toEqual(["T(Leyenda)"]);
    });

    it('should leave the legend of a pie to its slices', () => {
      expect(build("ChrPieTst").legend.data).toBeUndefined();
    });

    it('should not show the title of a hidden legend', () => {
      expect(graphicTexts(build("ChrLinTst"))).toEqual([]);
    });
  });

  describe('palette', () => {
    it('should use the Highcharts palette', () => {
      expect(build("ChrLinTst").color).toEqual(PALETTE);
      expect(PALETTE[0]).toBe("#2caffe");
      expect(PALETTE).toHaveLength(10);
    });

    it('should give each series its color by its position in the model, before stacks are reversed', () => {
      const option = build("ChrBarTst");
      const colors = Object.fromEntries(option.series.map(serie => [serie.id, serie.itemStyle.color]));

      expect(colors).toEqual({"serie2-1": PALETTE[0], "serie2-2": PALETTE[1], "serie2-3": PALETTE[2]});
      expect(option.series[0].id).toBe("serie2-3");
    });

    it('should keep the color of the series and skip the pies', () => {
      expect(build("ChrLinTst").series[0].itemStyle.color).toBe("#A8E0A6");
      expect(build("ChrPieTst").series[0].itemStyle).toBeUndefined();
    });

    it('should advance the palette only with the series that have no color of their own', () => {
      const mixed = build("ChrLinTst");

      // the first series has its own color, so the second one takes the first color of the palette
      expect(mixed.series[1].itemStyle.color).toBe(PALETTE[0]);

      const model = clone(models.ChrBarTst);
      model.series[1].itemStyle = {color: "#123456"};
      const option = buildEChartsOption(model, values, context());
      const colors = Object.fromEntries(option.series.map(serie => [serie.id, serie.itemStyle.color]));
      expect(colors).toEqual({"serie2-1": PALETTE[0], "serie2-2": "#123456", "serie2-3": PALETTE[1]});
    });
  });

  describe('category labels', () => {
    const categories = (names, overrides = {}, mutate = () => {}) => {
      const model = clone(models.ChrBarTst);
      model.xAxis[0].type = "category";
      mutate(model);
      const rows = names.map((name, index) => ({dates: name, serie1: index, serie2: 1, serie3: 2}));
      return buildEChartsOption(model, rows, context(overrides));
    };

    it('should show every category with a small font when they fit', () => {
      const option = categories(["Ene", "Feb", "Mar"]);

      expect(option.xAxis[0].axisLabel).toMatchObject({interval: 0, fontSize: 11});
      expect(option.xAxis[0].axisLabel.rotate).toBeUndefined();
    });

    it('should rotate the labels 45 degrees when they do not fit and make room below', () => {
      const names = Array.from({length: 12}, (_, index) => `Category number ${index}`);
      const fits = categories(["a", "b"]);

      const option = categories(names, {width: 600});

      expect(option.xAxis[0].axisLabel.rotate).toBe(45);
      expect(option.xAxis[0].axisLabel.interval).toBe(0);
      expect(option.grid.bottom).toBe(fits.grid.bottom + 70);
    });

    it('should decide the rotation again when the chart is resized', () => {
      const names = ["Alpha", "Bravo", "Charlie", "Delta"];

      expect(categories(names, {width: 800}).xAxis[0].axisLabel.rotate).toBeUndefined();
      expect(categories(names, {width: 100}).xAxis[0].axisLabel.rotate).toBe(45);
    });

    it('should not rotate the labels before the chart is measured', () => {
      expect(categories(["Alpha", "Bravo", "Charlie"], {width: 0}).xAxis[0].axisLabel.rotate).toBeUndefined();
    });

    it('should leave room proportional to the longest label, up to a limit', () => {
      const base = categories(["a"]).grid.bottom;

      expect(categories(["abcdefghij", "klmnopqrst"], {width: 60}).grid.bottom).toBe(base + Math.round(10 * 4.7));
    });

    it('should leave the labels alone beyond 60 categories', () => {
      const names = Array.from({length: 61}, (_, index) => `C${index}`);

      expect(categories(names).xAxis[0].axisLabel).toBeUndefined();
    });

    it('should estimate 7 pixels per character plus 6 to decide the rotation', () => {
      // "2026": 4 * 7 + 6 = 34 pixels
      const names = ["2026", "2027", "2028", "2029"];

      expect(categories(names, {width: 4 * 35}).xAxis[0].axisLabel.rotate).toBeUndefined();
      expect(categories(names, {width: 4 * 33}).xAxis[0].axisLabel.rotate).toBe(45);
    });

    it('should not touch the categories of an inverted chart', () => {
      const option = categories(["Alpha", "Bravo"], {width: 100}, model => {
        model.awe.inverted = true;
        model.yAxis = model.xAxis;
        model.xAxis = [{type: "value", awe: {axis: "y"}}];
        model.series.forEach(serie => {
          serie.awe.xValue = "dates";
        });
      });

      expect(option.yAxis[0].axisLabel).toBeUndefined();
    });

    it('should respect the label rotation and interval of the model', () => {
      const option = categories(["Alpha", "Bravo"], {width: 20}, model => {
        model.xAxis[0].axisLabel = {rotate: -30, interval: 2};
      });

      expect(option.xAxis[0].axisLabel).toMatchObject({rotate: -30, interval: 2, fontSize: 11});
    });
  });

  describe('bar data labels', () => {
    const negative = values.map((row, index) => ({...row, serie1: index === 1 ? -4 : row.serie1}));

    it('should place them outside the end of each bar, away from zero', () => {
      const vertical = clone(models.ChrBarTst);
      vertical.series[0].label = {show: true};
      vertical.series.splice(1);
      delete vertical.series[0].stack;

      const option = buildEChartsOption(vertical, negative, context());

      expect(option.series[0].data.map(point => point.label.position)).toEqual(["top", "bottom", "top"]);
      expect(option.series[0].label).toMatchObject({show: true, fontWeight: "bold", fontSize: 11});
    });

    it('should place them to the right of the bars of an inverted chart', () => {
      const option = build("ChrBarHorTst");

      expect(option.series[0].data.map(point => point.label.position)).toEqual(["right", "right", "right"]);
    });

    it('should keep the labels inside the bars of a pyramid, in white', () => {
      const option = build("ChrBarHorTst", negative);

      expect(option.series[0].label).toMatchObject({position: "inside", color: "#ffffff", fontWeight: "bold", fontSize: 11});
      expect(option.series[0].data).toEqual([[10.12345, day(1)], [-4, day(2)], [null, day(3)]]);
      expect(option.xAxis[0].boundaryGap).toBeUndefined();
    });

    it('should not take a vertical chart with negative values for a pyramid', () => {
      const vertical = clone(models.ChrBarTst);
      vertical.series.splice(1);
      vertical.series[0].label = {show: true};
      delete vertical.series[0].stack;

      const option = buildEChartsOption(vertical, negative, context());

      expect(option.series[0].label.position).toBeUndefined();
      expect(option.series[0].data[1].label.position).toBe("bottom");
    });

    it('should keep the labels of stacked bars inside', () => {
      const model = clone(models.ChrBarTst);
      model.series.forEach(serie => {
        serie.label = {show: true};
      });

      const option = buildEChartsOption(model, values, context());

      expect(option.series[0].data[0].label.position).toBe("inside");
    });

    it('should leave room in the value axis for the labels', () => {
      // no gap before zero when there are no negative bars: the axis would start below zero
      expect(build("ChrBarHorTst").xAxis[0].boundaryGap).toEqual([0, "18%"]);
      const vertical = clone(models.ChrBarTst);
      vertical.series.splice(1);
      vertical.series[0].label = {show: true};
      delete vertical.series[0].stack;
      expect(buildEChartsOption(vertical, values, context()).yAxis[0].boundaryGap).toEqual([0, "12%"]);
    });

    it('should leave room on both sides when a vertical chart has negative bars', () => {
      const vertical = clone(models.ChrBarTst);
      vertical.series.splice(1);
      vertical.series[0].label = {show: true};
      delete vertical.series[0].stack;

      expect(buildEChartsOption(vertical, negative, context()).yAxis[0].boundaryGap).toEqual(["12%", "12%"]);
    });

    it('should leave the room only in the value axis of the bars, not in a numeric x axis', () => {
      const vertical = clone(models.ChrBarTst);
      vertical.xAxis[0].type = "value";
      vertical.series.splice(1);
      vertical.series[0].label = {show: true};
      delete vertical.series[0].stack;

      const option = buildEChartsOption(vertical, values, context());

      expect(option.xAxis[0].boundaryGap).toBeUndefined();
      expect(option.yAxis[0].boundaryGap).toEqual([0, "12%"]);
    });

    it('should not change the axis of bars without labels', () => {
      expect(build("ChrBarTst").yAxis[0].boundaryGap).toBeUndefined();
    });

    it('should keep the points of a series without labels as plain pairs', () => {
      expect(build("ChrLinTst").series[0].data[0]).toEqual([day(1), 10.12345]);
    });
  });
});
