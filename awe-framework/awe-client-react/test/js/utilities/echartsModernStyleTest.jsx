import {buildEChartsOption, PALETTE} from "../../../src/utilities/echartsOption";
import {renderSvg} from "../../../src/utilities/echartsSetup";
import {SCALE_TOKENS, SERIES_TOKENS, CHART_TOKENS, resolveChartColors, sameColors} from "../../../src/utilities/echartsTheme";

const SERIES = Array.from({length: 8}, (_, index) => `rgb(${index + 10}, 20, 30)`);
const SCALE = ["rgb(250, 250, 250)", "rgb(200, 200, 250)", "rgb(150, 150, 250)", "rgb(100, 100, 250)", "rgb(50, 50, 250)"];
const COLORS = {
  text: "rgb(1, 1, 1)", title: "rgb(2, 2, 2)", muted: "rgb(3, 3, 3)", axis: "rgb(4, 4, 4)", split: "rgb(5, 5, 5)",
  tooltipBackground: "rgb(6, 6, 6)", tooltipBorder: "rgb(7, 7, 7)", tooltipText: "rgb(8, 8, 8)", surface: "rgb(9, 9, 9)",
  series: SERIES, scale: SCALE
};
const locale = {decimalPoint: ".", thousandsSep: ",", noData: "No data", drillUpText: "Back"};
const rows = [{x: "A", y: 10, z: 4, y2: 3}, {x: "B", y: 20, z: 6, y2: 8}, {x: "C", y: 30, z: 8, y2: 2}];
const build = (model, data = rows, overrides = {}) => buildEChartsOption(JSON.parse(JSON.stringify(model)), data,
  {t: (text) => text, locale, width: 800, height: 400, colors: COLORS, ...overrides});
const serieOf = (type, extra = {}, hints = {}) => ({
  id: type, name: type, type, ...extra, awe: {type, xValue: "x", yValue: "y", ...hints}
});
const cartesian = (series, extra = {}) => ({
  xAxis: [{type: "category", awe: {axis: "x"}}], yAxis: [{type: "value", awe: {axis: "y"}}], series, ...extra
});
const valueOf = (point) => Array.isArray(point) ? point : point.value;

describe('awe-react-client/test/js/utilities/echartsModernStyleTest.jsx', () => {

  describe('palette', () => {
    it('should resolve the series and the scale tokens next to the colors of the chrome', () => {
      const element = document.createElement("div");
      document.body.appendChild(element);
      const byToken = {
        ...Object.fromEntries(Object.values(CHART_TOKENS).map(token => [token, "rgb(1, 1, 1)"])),
        ...Object.fromEntries(SERIES_TOKENS.map((token, index) => [token, SERIES[index]])),
        ...Object.fromEntries(SCALE_TOKENS.map((token, index) => [token, SCALE[index]]))
      };
      const computed = jest.spyOn(window, "getComputedStyle").mockImplementation((probe) => probe.tagName !== "SPAN"
        ? {color: "", getPropertyValue: () => "light-dark(a, b)"}
        : {color: byToken[probe.style.color.replace(/^var\((.*)\)$/, "$1")] ?? ""});

      const colors = resolveChartColors(element);
      computed.mockRestore();
      element.remove();

      expect(colors.series).toEqual(SERIES);
      expect(colors.scale).toEqual(SCALE);
      expect(sameColors(colors, {...colors, series: [...SERIES.slice(1), "rgb(0, 0, 0)"]})).toBe(false);
    });

    it('should leave the palette out when a page does not define the tokens', () => {
      const element = document.createElement("div");
      document.body.appendChild(element);
      const computed = jest.spyOn(window, "getComputedStyle").mockImplementation((probe) => probe.tagName !== "SPAN"
        ? {color: "", getPropertyValue: (token) => token === "--awe-color-text" ? "x" : ""}
        : {color: "rgb(1, 1, 1)"});

      const colors = resolveChartColors(element);
      computed.mockRestore();
      element.remove();

      expect(colors).not.toHaveProperty("series");
      expect(colors).not.toHaveProperty("scale");
    });

    it('should use the series tokens as the palette of the chart', () => {
      const option = build(cartesian([serieOf("line"), serieOf("line", {id: "second"})]));

      expect(option.color).toEqual(SERIES);
      expect(option.series.map(serie => serie.itemStyle.color)).toEqual([SERIES[0], SERIES[1]]);
    });

    it('should keep the colors of the model over the tokens', () => {
      const option = build({...cartesian([serieOf("line", {itemStyle: {color: "#123456"}})]), color: ["#aaaaaa"]});

      expect(option.color).toEqual(["#aaaaaa"]);
      expect(option.series[0].itemStyle.color).toBe("#123456");
    });

    it('should fall back to the last-resort palette without resolved colors', () => {
      expect(build(cartesian([serieOf("line")]), rows, {colors: null}).color).toEqual(PALETTE);
      expect(build(cartesian([serieOf("line")]), rows, {colors: {text: "rgb(1, 1, 1)"}}).color).toEqual(PALETTE);
    });
  });

  describe('areas', () => {
    const area = (extra = {}) => serieOf("line", {areaStyle: {}, ...extra});
    const alphaOf = (stop) => Number(/,\s*([\d.]+)\)$/.exec(stop.color)?.[1] ?? 1);

    it('should fade the area from the color of the series to transparent at the axis', () => {
      const {areaStyle} = build(cartesian([area()])).series[0];
      const [top, bottom] = areaStyle.color.colorStops;

      expect(areaStyle.color).toMatchObject({type: "linear", x: 0, y: 0, x2: 0, y2: 1});
      expect(top.color).toMatch(/^rgba\(10, ?20, ?30/);
      expect(bottom.color).toMatch(/^rgba\(10, ?20, ?30/);
      expect(alphaOf(top)).toBeGreaterThan(0);
      expect(alphaOf(bottom)).toBe(0);
    });

    it('should derive the gradient from the color that the series sets', () => {
      const {areaStyle} = build(cartesian([area({itemStyle: {color: "#ff0000"}})])).series[0];

      expect(areaStyle.color.colorStops[0].color).toMatch(/^rgba\(255, ?0, ?0/);
    });

    it('should keep the area color of the model', () => {
      const {areaStyle} = build(cartesian([area({areaStyle: {color: "#00ff00"}})])).series[0];

      expect(areaStyle.color).toBe("#00ff00");
    });

    it('should fill the band of a range evenly, so its low edge shows, and keep its hidden base invisible', () => {
      const band = serieOf("line", {areaStyle: {}}, {type: "arearange", range: true, zValue: "z"});
      const [base, visible] = build(cartesian([band])).series;

      // A gradient would fade the band out before its low edge
      expect(visible.areaStyle.color).toMatch(/^rgba\(10, ?20, ?30/);
      expect(base.areaStyle).toBeUndefined();
      expect(base.lineStyle).toEqual({width: 0, opacity: 0});
      expect(base.silent).toBe(true);
    });

    it('should draw a flat translucent area when the color cannot be read', () => {
      const {areaStyle} = build(cartesian([area({itemStyle: {color: "not-a-color"}})])).series[0];

      expect(areaStyle.color).toBeUndefined();
      expect(areaStyle.opacity).toBeLessThan(0.5);
    });
  });

  describe('bars', () => {
    const bars = (extra = {}, hints = {}) => serieOf("bar", extra, hints);
    const radiusOf = (serie, index = 0) => serie.data[index].itemStyle?.borderRadius;

    it('should round the value end of the bars and make them narrower', () => {
      const [serie] = build(cartesian([bars()])).series;

      expect(radiusOf(serie)).toEqual([4, 4, 0, 0]);
      expect(serie.barMaxWidth).toBeDefined();
      expect(serie.barCategoryGap).toBeDefined();
    });

    it('should round the end away from zero of a negative bar', () => {
      const [serie] = build(cartesian([bars()]), [{x: "A", y: -5}]).series;

      expect(radiusOf(serie)).toEqual([0, 0, 4, 4]);
    });

    it('should round only the outermost series of a stack', () => {
      const option = build(cartesian([bars({id: "low", stack: "s"}), bars({id: "high", stack: "s"}, {yValue: "y2"})]));
      const byId = Object.fromEntries(option.series.map(serie => [serie.id, serie]));

      // Like Highcharts, the first series of a stack is the one on top
      expect(radiusOf(byId.low)).toEqual([4, 4, 0, 0]);
      expect(radiusOf(byId.high)).toBeUndefined();
    });

    it('should leave the bars square when the model sets a radius of zero', () => {
      const [serie] = build(cartesian([bars({itemStyle: {borderRadius: 0}})])).series;

      expect(serie.data.every(point => Array.isArray(point))).toBe(true);
    });

    it('should keep the radius, the gap and the maximum width of the model', () => {
      const [serie] = build(cartesian([bars({barMaxWidth: 10, barCategoryGap: "5%"}, {borderRadius: 2})])).series;

      expect(radiusOf(serie)).toEqual([2, 2, 0, 0]);
      expect(serie.barMaxWidth).toBe(10);
      expect(serie.barCategoryGap).toBe("5%");
    });
  });

  describe('lines and grid', () => {
    it('should draw lines of width 2 whose points show on hover only', () => {
      const [serie] = build(cartesian([serieOf("line")])).series;

      expect(serie.lineStyle.width).toBe(2);
      expect(serie.showSymbol).toBe(false);
    });

    it('should keep the markers that the model asks for', () => {
      const [serie] = build(cartesian([serieOf("line", {showSymbol: true, lineStyle: {width: 5}})])).series;

      expect(serie.showSymbol).toBe(true);
      expect(serie.lineStyle.width).toBe(5);
    });

    it('should draw dashed grid lines and no line on the value axis', () => {
      const option = build(cartesian([serieOf("line")]));

      expect(option.yAxis[0].splitLine.lineStyle).toMatchObject({type: "dashed", color: COLORS.split});
      expect(option.yAxis[0].axisLine.show).toBe(false);
      // The category axis keeps its line, which is the baseline of the bars
      expect(option.xAxis[0].axisLine.show).toBeUndefined();
      expect(option.xAxis[0].axisLine.lineStyle.color).toBe(COLORS.axis);
    });

    it('should keep the grid options of the model', () => {
      const model = cartesian([serieOf("line")]);
      model.yAxis[0].splitLine = {show: false};
      model.yAxis[0].axisLine = {show: true};

      const [axis] = build(model).yAxis;

      expect(axis.splitLine.show).toBe(false);
      expect(axis.splitLine.lineStyle.type).toBeUndefined();
      expect(axis.axisLine.show).toBe(true);
    });
  });

  describe('tooltip and legend', () => {
    it('should round the tooltip and give it the shadow of the design system through its tokens', () => {
      const {tooltip} = build(cartesian([serieOf("line")], {tooltip: {trigger: "axis"}}));

      expect(tooltip.extraCssText).toContain("var(--awe-radius-");
      expect(tooltip.extraCssText).toContain("var(--awe-shadow-overlay)");
      expect(tooltip.extraCssText).not.toMatch(/#|rgb/);
    });

    it('should point at the axis with a vertical line, only when the tooltip follows the axis', () => {
      expect(build(cartesian([serieOf("line")], {tooltip: {trigger: "axis"}})).tooltip.axisPointer.type).toBe("line");
      expect(build(cartesian([serieOf("line")], {tooltip: {trigger: "item"}})).tooltip.axisPointer).toBeUndefined();
      expect(build(cartesian([serieOf("line")], {tooltip: {trigger: "axis", axisPointer: {type: "shadow"}}}))
        .tooltip.axisPointer.type).toBe("shadow");
    });

    it('should draw round legend marks unless the model chooses another icon', () => {
      const model = cartesian([serieOf("line")], {legend: {show: true}});
      const {legend} = build(model);

      expect(legend.icon).toBe("circle");
      expect(legend.itemWidth).toBe(legend.itemHeight);
      expect(build({...model, legend: {show: true, icon: "rect"}}).legend.icon).toBe("rect");
    });
  });

  describe('pie and donut', () => {
    const pie = (extra = {}) => ({
      series: [{id: "pie", name: "Share", type: "pie", awe: {type: "pie", xValue: "x", yValue: "y"}, ...extra}]
    });
    const totalOf = (option) => option.graphic?.find(element => element.type === "text" && element.silent);

    it('should leave a gap of the color of the surface between the slices, with soft corners', () => {
      const [serie] = build(pie()).series;

      expect(serie.itemStyle).toMatchObject({borderColor: COLORS.surface});
      expect(serie.itemStyle.borderWidth).toBeGreaterThan(0);
      expect(serie.itemStyle.borderRadius).toBeGreaterThan(0);
    });

    it('should keep the slice style of the model', () => {
      const [serie] = build(pie({itemStyle: {borderColor: "#fff", borderWidth: 0, borderRadius: 0}})).series;

      expect(serie.itemStyle).toMatchObject({borderColor: "#fff", borderWidth: 0, borderRadius: 0});
    });

    it('should show the total of the values in the centre of a donut', () => {
      const option = build(pie({radius: ["50%", "75%"]}));

      expect(totalOf(option).style.text).toBe("60");
      expect(totalOf(option)).toMatchObject({x: 400, y: 200});
      expect(totalOf(option).style.fill).toBe(COLORS.text);
    });

    it('should format the total like the values of the tooltip and follow the centre of the pie', () => {
      const model = {...pie({radius: ["50%", "75%"], center: ["25%", "40%"]}), tooltip: {awe: {numberDecimals: 1, suffix: " u"}}};
      const big = rows.map(row => ({...row, y: row.y * 1000}));

      const option = build(model, big);
      const [x, y] = option.series[0].center;

      expect(totalOf(option).style.text).toBe("60,000.0 u");
      expect(totalOf(option)).toMatchObject({x, y});
      expect(x).not.toBe(400);
    });

    it('should not show a total in an empty donut, only the no data text', () => {
      const option = build(pie({radius: ["50%", "75%"]}), []);

      expect(option.graphic.map(element => element.style.text)).toEqual(["No data"]);
    });

    it('should not show a total in a plain pie, a semicircle or a hole without room', () => {
      expect(totalOf(build(pie()))).toBeUndefined();
      expect(totalOf(build(pie({radius: ["50%", "75%"], startAngle: 180, endAngle: 360})))).toBeUndefined();
      expect(totalOf(build(pie({radius: ["50%", "75%"]}), rows, {width: 100, height: 100}))).toBeUndefined();
    });

    it('should keep fitting the labels of a donut inside the chart', () => {
      const [serie] = build(pie({radius: ["50%", "75%"], label: {show: true}}), [{x: "A very long name for a slice", y: 1}],
        {width: 300, height: 300}).series;

      expect(serie.radius[1]).toBeLessThan(150);
      expect(serie.radius[0]).toBeLessThan(serie.radius[1]);
    });
  });

  describe('radar, gauge, heatmap and funnel', () => {
    it('should fill the polygon of a radar with its series color', () => {
      const model = {radar: {}, series: [serieOf("radar")]};

      expect(build(model).series[0].areaStyle.color).toMatch(/^rgba\(10, ?20, ?30/);
      expect(build({...model, series: [serieOf("radar", {areaStyle: {color: "#00f"}})]}).series[0].areaStyle.color).toBe("#00f");
    });

    it('should draw the progress of a gauge with its series color and a track of the axis color', () => {
      const [serie] = build({series: [serieOf("gauge")]}, [{y: 70, z: 100}]).series;

      expect(serie.progress).toMatchObject({show: true, itemStyle: {color: SERIES[0]}});
      expect(serie.axisLine.lineStyle.color).toEqual([[1, COLORS.axis]]);
    });

    it('should lower a gauge under the title and put its name and value in the opening of the arc', () => {
      const [serie] = build({series: [serieOf("gauge")]}, [{y: 70, z: 100}]).series;
      const below = (offset) => parseFloat(offset[1]);

      expect(parseFloat(serie.center[1])).toBeGreaterThan(50);
      expect(below(serie.title.offsetCenter)).toBeGreaterThan(below(serie.detail.offsetCenter));

      const own = build({series: [serieOf("gauge", {center: ["50%", "50%"], title: {offsetCenter: [0, "10%"]}})]},
        [{y: 70, z: 100}]).series[0];
      expect(own.center).toEqual(["50%", "50%"]);
      expect(own.title.offsetCenter).toEqual([0, "10%"]);
    });

    it('should color the heatmap from the scale tokens, unless the model sets the colors', () => {
      const model = {
        visualMap: {type: "continuous"},
        series: [serieOf("heatmap", {}, {zValue: "z"})]
      };

      expect(build(model).visualMap.inRange.color).toEqual(SCALE);
      expect(build({...model, visualMap: {type: "continuous", inRange: {color: ["#000", "#fff"]}}}).visualMap.inRange.color)
        .toEqual(["#000", "#fff"]);
      expect(build(model).series[0].itemStyle.borderColor).toBe(COLORS.surface);
    });

    it('should leave the visual map of another chart without the heatmap scale', () => {
      const model = {...cartesian([serieOf("scatter")]), visualMap: {type: "continuous"}};

      expect(build(model).visualMap.inRange).toBeUndefined();
    });

    it('should separate the steps of a funnel with the color of the surface', () => {
      const [serie] = build({series: [serieOf("funnel")]}).series;

      expect(serie.itemStyle.borderColor).toBe(COLORS.surface);
    });
  });

  describe('static image', () => {
    it('should draw a gradient area and a round legend as SVG 1.1 without a DOM', () => {
      const area = serieOf("line", {areaStyle: {}});
      const option = buildEChartsOption(cartesian([area], {legend: {show: true}}), rows,
        {t: (text) => text, locale, width: 600, height: 300, print: true});

      const svg = renderSvg(option, {width: 600, height: 300});

      expect(svg).toContain("<linearGradient");
      expect(svg).not.toContain("<style");
      expect(svg).not.toContain("feDropShadow");
    });
  });
});
