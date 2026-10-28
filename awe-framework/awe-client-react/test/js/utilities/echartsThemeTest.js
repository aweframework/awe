import models from "../components/fixtures/chrTstEchartsModels.json";
import {buildEChartsOption} from "../../../src/utilities/echartsOption";
import {renderSvg} from "../../../src/utilities/echartsSetup";
import {
  CHART_TOKENS,
  applyChartColors,
  observeColorMode,
  resolveChartColors,
  sameColors
} from "../../../src/utilities/echartsTheme";

const COLORS = {
  text: "rgb(1, 1, 1)", title: "rgb(2, 2, 2)", muted: "rgb(3, 3, 3)", axis: "rgb(4, 4, 4)", split: "rgb(5, 5, 5)",
  tooltipBackground: "rgb(6, 6, 6)", tooltipBorder: "rgb(7, 7, 7)", tooltipText: "rgb(8, 8, 8)", surface: "rgb(9, 9, 9)"
};
const clone = (value) => JSON.parse(JSON.stringify(value));
const locale = {decimalPoint: ".", thousandsSep: ",", noData: "No data to display", drillUpText: "Back"};
const values = Array.from({length: 4}, (_, index) => ({
  dates: new Date(2024, 0, index + 1).getTime(), serie1: 10 + index, serie2: 5 + index, serie3: 1 + index,
  names: `Name ${index}`, subserie1: index, Ord: index, serie1_1: index, serie1_2: index
}));
const build = (name, colors = COLORS, size = {width: 800, height: 400}) =>
  buildEChartsOption(clone(models[name]), values, {t: (text) => text, locale, ...size, colors});

describe('awe-react-client/test/js/utilities/echartsThemeTest.js', () => {

  describe('resolveChartColors', () => {
    let computed;
    let parents;

    beforeEach(() => {
      parents = [];
      // jsdom does not resolve custom properties: the computed color of a probe is the one of its token
      const byToken = Object.fromEntries(Object.entries(CHART_TOKENS).map(([role, token]) => [`var(${token})`, COLORS[role]]));
      // Every token is defined on the element that holds the chart; the probes are spans
      computed = jest.spyOn(window, "getComputedStyle").mockImplementation((probe) => {
        if (probe.tagName !== "SPAN") {
          return {color: "", getPropertyValue: () => "light-dark(a, b)"};
        }
        parents.push(probe.parentElement);
        return {color: byToken[probe.style.color] ?? ""};
      });
    });

    afterEach(() => {
      computed.mockRestore();
    });

    it('should map every role to a semantic token, never to a primitive', () => {
      Object.values(CHART_TOKENS).forEach(token => {
        expect(token).toMatch(/^--awe-color-/);
        expect(token).not.toMatch(/palette|surface-\d/);
      });
    });

    it('should resolve each token through the computed color of a probe inside the element', () => {
      const element = document.createElement("div");
      document.body.appendChild(element);

      const colors = resolveChartColors(element);

      expect(colors).toEqual(COLORS);
      expect(parents).toHaveLength(Object.keys(CHART_TOKENS).length);
      expect(parents.every(parent => parent === element)).toBe(true);
      // The probe does not stay in the page
      expect(element.childElementCount).toBe(0);
      element.remove();
    });

    it('should leave out the roles whose token is not defined, which keep the ECharts default', () => {
      // An undefined token makes the probe inherit the text color, so it must not be read
      const undefinedTokens = ["--awe-color-chart-axis", "--awe-color-overlay"];
      computed.mockImplementation((element) => element.tagName === "SPAN"
        ? {color: "rgb(1, 1, 1)"}
        : {color: "", getPropertyValue: (token) => undefinedTokens.includes(token) ? "" : "light-dark(a, b)"});

      const colors = resolveChartColors(document.body);

      expect(colors.text).toBe("rgb(1, 1, 1)");
      expect(colors).not.toHaveProperty("axis");
      expect(colors).not.toHaveProperty("tooltipBackground");
      expect(colors.tooltipText).toBe("rgb(1, 1, 1)");
    });

    it('should resolve nothing without a DOM element or without a text color', () => {
      expect(resolveChartColors(null)).toBeNull();
      computed.mockImplementation(() => ({color: "rgb(1, 1, 1)", getPropertyValue: () => ""}));
      expect(resolveChartColors(document.body)).toBeNull();
    });
  });

  describe('sameColors', () => {
    it('should compare the resolved colors by value', () => {
      expect(sameColors({...COLORS}, COLORS)).toBe(true);
      expect(sameColors({...COLORS, text: "rgb(0, 0, 0)"}, COLORS)).toBe(false);
      expect(sameColors(null, null)).toBe(true);
      expect(sameColors(null, COLORS)).toBe(false);
    });
  });

  describe('applyChartColors', () => {
    it('should return the option as it is without colors', () => {
      const option = {series: []};

      expect(applyChartColors(option, null)).toBe(option);
    });

    it('should color the title, the legend and the tooltip', () => {
      const option = applyChartColors({
        title: {text: "T", subtext: "S"}, legend: {show: true}, tooltip: {trigger: "item"}, series: []
      }, COLORS);

      expect(option.title.textStyle.color).toBe(COLORS.title);
      expect(option.title.subtextStyle.color).toBe(COLORS.muted);
      expect(option.legend.textStyle.color).toBe(COLORS.text);
      expect(option.legend.pageTextStyle.color).toBe(COLORS.text);
      expect(option.legend.pageIconColor).toBe(COLORS.text);
      expect(option.legend.pageIconInactiveColor).toBe(COLORS.muted);
      expect(option.tooltip).toEqual({
        trigger: "item", backgroundColor: COLORS.tooltipBackground, borderColor: COLORS.tooltipBorder,
        textStyle: {color: COLORS.tooltipText}
      });
    });

    it('should color every legend and every title of a list', () => {
      const option = applyChartColors({title: [{text: "A"}, {text: "B"}], legend: [{}, {show: false}], series: []}, COLORS);

      expect(option.title.map(title => title.textStyle.color)).toEqual([COLORS.title, COLORS.title]);
      expect(option.legend.map(legend => legend.textStyle.color)).toEqual([COLORS.text, COLORS.text]);
    });

    it('should color the lines, the labels and the names of the axes', () => {
      const option = applyChartColors({
        xAxis: [{type: "category", name: "X"}], yAxis: [{type: "value"}, {type: "value"}], series: []
      }, COLORS);

      [option.xAxis[0], option.yAxis[0], option.yAxis[1]].forEach(axis => {
        expect(axis.axisLine.lineStyle.color).toBe(COLORS.axis);
        expect(axis.axisTick.lineStyle.color).toBe(COLORS.axis);
        expect(axis.axisLabel.color).toBe(COLORS.muted);
        expect(axis.nameTextStyle.color).toBe(COLORS.text);
        expect(axis.splitLine.lineStyle.color).toBe(COLORS.split);
      });
    });

    it('should color the texts of the zoom', () => {
      const option = applyChartColors({dataZoom: [{type: "slider"}, {type: "inside"}], series: []}, COLORS);

      expect(option.dataZoom.map(zoom => zoom.textStyle.color)).toEqual([COLORS.muted, COLORS.muted]);
    });

    it('should color the texts drawn on the chart, without a color of their own', () => {
      const option = applyChartColors({
        series: [], graphic: [{type: "text", style: {text: "No data"}}, {type: "text", style: {text: "Own", fill: "red"}}]
      }, COLORS);

      expect(option.graphic[0].style.fill).toBe(COLORS.text);
      expect(option.graphic[1].style.fill).toBe("red");
    });

    it('should color the outside labels of the series without an outline, and leave the inside ones', () => {
      const option = applyChartColors({
        series: [
          {type: "pie", label: {show: true}},
          {type: "bar", label: {show: true, position: "inside"}},
          {type: "bar", label: {show: true}, data: [{value: [1, 2], label: {position: "inside"}}]},
          {type: "bar", label: {show: true, position: "top"}},
          {type: "line", label: {show: false}},
          {type: "line"}
        ]
      }, COLORS);

      expect(option.series[0].label).toEqual({show: true, color: COLORS.text, textBorderWidth: 0});
      expect(option.series[1].label).toEqual({show: true, position: "inside"});
      expect(option.series[2].label.color).toBeUndefined();
      expect(option.series[3].label.color).toBe(COLORS.text);
      expect(option.series[4].label).toEqual({show: false});
      expect(option.series[5].label).toBeUndefined();
    });

    it('should color the labels of a pie that draws them outside by default (without a label in the series)', () => {
      const option = applyChartColors({series: [{type: "pie"}, {type: "pie", label: {position: "inside"}}]}, COLORS);

      expect(option.series[0].label).toEqual({color: COLORS.text, textBorderWidth: 0});
      expect(option.series[1].label).toEqual({position: "inside"});
    });

    it('should not override a color that the model sets', () => {
      const option = applyChartColors({
        title: {text: "T", textStyle: {color: "red", fontSize: 20}},
        legend: {textStyle: {color: "blue"}},
        tooltip: {backgroundColor: "green"},
        xAxis: [{axisLabel: {color: "pink", rotate: 45}, splitLine: {show: true}}],
        series: [{type: "pie", label: {show: true, color: "orange", textBorderWidth: 2}}]
      }, COLORS);

      expect(option.title.textStyle).toEqual({color: "red", fontSize: 20});
      expect(option.legend.textStyle.color).toBe("blue");
      expect(option.tooltip.backgroundColor).toBe("green");
      expect(option.xAxis[0].axisLabel).toEqual({color: "pink", rotate: 45});
      expect(option.xAxis[0].splitLine).toEqual({show: true, lineStyle: {color: COLORS.split}});
      expect(option.series[0].label).toEqual({show: true, color: "orange", textBorderWidth: 2});
    });

    it('should not change the option it receives', () => {
      const input = {title: {text: "T"}, xAxis: [{type: "category"}], series: [{type: "pie", label: {show: true}}]};
      const copy = clone(input);

      applyChartColors(input, COLORS);

      expect(input).toEqual(copy);
    });
  });

  describe('buildEChartsOption', () => {
    it('should color the chrome of a cartesian chart from the colors of the context', () => {
      const option = build("ChrBarTst");

      expect(option.title.textStyle.color).toBe(COLORS.title);
      expect(option.xAxis[0].axisLabel.color).toBe(COLORS.muted);
      expect(option.yAxis[0].splitLine.lineStyle.color).toBe(COLORS.split);
      expect(option.tooltip.backgroundColor).toBe(COLORS.tooltipBackground);
    });

    it('should color the message of a chart without data and the legend title, without literals', () => {
      const empty = buildEChartsOption({}, [], {t: (text) => text, locale, width: 800, height: 400, colors: COLORS});

      expect(empty.graphic[0].style.fill).toBe(COLORS.text);
      expect(JSON.stringify(build("ChrPieTst"))).not.toMatch(/dddddd|555555/);
    });

    it('should build the same option without colors, with no color of the chrome', () => {
      const option = build("ChrBarTst", null);

      expect(option.title.textStyle).toBeUndefined();
      expect(option.xAxis[0].axisLabel.color).toBeUndefined();
    });

    it('should draw the colors in the image of the chart', () => {
      jest.spyOn(HTMLCanvasElement.prototype, "getContext").mockImplementation(() => ({
        font: "", measureText: (text) => ({width: String(text).length * 7})
      }));

      const svg = renderSvg(build("ChrPieTst"), {width: 800, height: 400}, "en");
      HTMLCanvasElement.prototype.getContext.mockRestore();

      expect(svg).toContain(COLORS.title);
      expect(svg).toContain(COLORS.text);
    });
  });

  describe('observeColorMode', () => {
    const flush = () => new Promise(resolve => setTimeout(resolve, 0));
    let stop;

    afterEach(() => {
      stop?.();
      document.documentElement.className = "";
      document.body.className = "";
      document.documentElement.removeAttribute("data-awe-surface");
      delete window.matchMedia;
    });

    it('should call back when the mode class of the page changes', async () => {
      const callback = jest.fn();
      stop = observeColorMode(callback);

      document.documentElement.classList.add("dark");
      await flush();

      expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should call back when the body class, the surface or the palette change', async () => {
      const callback = jest.fn();
      stop = observeColorMode(callback);

      document.body.classList.add("dark");
      await flush();
      document.documentElement.setAttribute("data-awe-surface", "slate");
      await flush();
      document.documentElement.style.setProperty("--awe-palette-primary-500", "red");
      await flush();

      expect(callback).toHaveBeenCalledTimes(3);
    });

    it('should call back when the preference of the system changes', () => {
      const listeners = [];
      window.matchMedia = jest.fn(() => ({
        addEventListener: (event, listener) => listeners.push([event, listener]),
        removeEventListener: jest.fn()
      }));
      const callback = jest.fn();
      stop = observeColorMode(callback);

      expect(window.matchMedia).toHaveBeenCalledWith("(prefers-color-scheme: dark)");
      listeners.forEach(([event, listener]) => {
        expect(event).toBe("change");
        listener();
      });

      expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should stop observing', async () => {
      const removeEventListener = jest.fn();
      window.matchMedia = jest.fn(() => ({addEventListener: jest.fn(), removeEventListener}));
      const callback = jest.fn();

      observeColorMode(callback)();
      document.documentElement.classList.add("dark");
      await flush();

      expect(callback).not.toHaveBeenCalled();
      expect(removeEventListener).toHaveBeenCalledWith("change", expect.any(Function));
    });
  });
});
