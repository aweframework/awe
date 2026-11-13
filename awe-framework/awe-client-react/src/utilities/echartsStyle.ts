/**
 * Modern default style of the charts (#959): what the chart draws when the model does not say otherwise. Every default is
 * given to the properties that the option does not have, so the colors and the options of the model (series colors,
 * `areaStyle`, `borderRadius`, `barMaxWidth`, markers...) always win. The colors come from the series palette, which are
 * the design tokens, or from the colors of the chrome.
 */
// zrender is the drawing library of ECharts: this module only needs its color parser, and it must not pull echarts/core
// (a heavy package, loaded on demand) into the main bundle
import {modifyAlpha} from "zrender/lib/tool/color";
import {formatNumber} from "./chartFormat";
import type {ChartLocale} from "./chartFormat";
import {BuiltSeries, DataPoint, ModelNode, Size} from "./echartsContext";
import type {TooltipHints} from "./echartsTooltip";
import {createValueText} from "./echartsTooltip";

type Node = Record<string, unknown>;

/** Colors of the chrome that the style of the series needs */
export interface StyleColors {
  surface?: string;
  axis?: string;
  muted?: string;
  text?: string;
}

const LINE_WIDTH = 2;
const AREA_ALPHA = {top: 0.32, bottom: 0};
const RADAR_ALPHA = 0.2;
// A range band is filled evenly: a gradient would fade it out before its low edge
const BAND_ALPHA = 0.24;
const FLAT_AREA_OPACITY = 0.2;
const SLICE_GAP = 2;
const SLICE_RADIUS = 6;
const LEGEND_MARK = 10;
const BAR_CATEGORY_GAP = "40%";
const BAR_MAX_WIDTH = 56;
/** Radius of the value end of the bars, in pixels */
export const BAR_RADIUS = 4;
const TOOLTIP_CSS = "border-radius:var(--awe-radius-overlay);box-shadow:var(--awe-shadow-overlay);";
const DONUT_FONT = 22;
const DONUT_MIN_HOLE = 36;
// A gauge goes a bit lower, out of the way of the title, with its name and value in the opening of the arc
const GAUGE_LAYOUT = {center: ["50%", "58%"], title: {offsetCenter: [0, "72%"]}, detail: {offsetCenter: [0, "42%"]}};

const isNode = (value: unknown): value is Node => Boolean(value) && typeof value === "object" && !Array.isArray(value);

/**
 * Add the properties that a node does not have, at any depth. The node is not changed
 * @param {object} node Node, may be missing
 * @param {object} defaults Properties to add
 * @returns {object} New node
 */
export function withDefaults(node: unknown, defaults: Node): Node {
  const result: Node = isNode(node) ? {...node} : {};
  Object.entries(defaults).forEach(([key, value]) => {
    if (result[key] === undefined) {
      result[key] = value;
    } else if (isNode(value) && isNode(result[key])) {
      result[key] = withDefaults(result[key], value);
    }
  });
  return result;
}

/**
 * Apply a function to an element or to each element of a list
 * @param {object|object[]|undefined} value Element or list
 * @param {function} apply Function
 * @returns {object|object[]|undefined} Result
 */
export function each(value: unknown, apply: (node: Node) => Node): unknown {
  if (Array.isArray(value)) {
    return value.map(item => isNode(item) ? apply(item) : item);
  }
  return isNode(value) ? apply(value) : value;
}

/**
 * Make a color transparent
 * @param {string} color Color (hex, rgb, named...)
 * @param {number} alpha Opacity, from 0 to 1
 * @returns {string|undefined} Color with the opacity, undefined when the color cannot be read
 */
export function withAlpha(color: unknown, alpha: number): string | undefined {
  return typeof color === "string" ? modifyAlpha(color, alpha) || undefined : undefined;
}

/**
 * Vertical gradient of an area: the color of the series, partly transparent at the top, and transparent at the axis
 * @param {string} color Color of the series
 * @returns {object|undefined} Linear gradient for ECharts, undefined when the color cannot be read
 */
export function areaGradient(color: unknown): Node | undefined {
  const top = withAlpha(color, AREA_ALPHA.top);
  const bottom = withAlpha(color, AREA_ALPHA.bottom);
  return top && bottom
    ? {type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{offset: 0, color: top}, {offset: 1, color: bottom}]}
    : undefined;
}

/**
 * Style of a built series that does not need the colors of the chrome: lines of width 2 without points until the mouse is
 * over them, gradient areas, evenly filled range bands, bars with rounded ends, soft radar polygons and the gauge
 * progress and layout. The series is changed
 * @param {object} built Series for ECharts
 * @param {object} model Series of the model, with the options that win
 * @param {*} color Color of the series
 * @param {{markers: boolean, maxPoints: number}} line The series asks for markers, and the points above which they hide
 */
export function styleSeries(built: BuiltSeries, model: ModelNode, color: unknown, line: {markers: boolean; maxPoints: number}): void {
  switch (built.type) {
    case "line":
      styleLine(built, model, color, line);
      break;
    case "radar":
      if (built.areaStyle === undefined && withAlpha(color, RADAR_ALPHA)) {
        built.areaStyle = {color: withAlpha(color, RADAR_ALPHA)};
      }
      break;
    case "gauge":
      styleGauge(built, color);
      break;
    case "pie":
      built.itemStyle = withDefaults(built.itemStyle, {borderRadius: SLICE_RADIUS});
      break;
    default:
  }
}

/**
 * Style of a line: width 2, no points when it has more than the maximum or does not ask for them, and the fill of its
 * area. The series is changed
 * @param {object} built Series for ECharts
 * @param {object} model Series of the model
 * @param {*} color Color of the series
 * @param {{markers: boolean, maxPoints: number}} line The series asks for markers, and the points above which they hide
 */
function styleLine(built: BuiltSeries, model: ModelNode, color: unknown, line: {markers: boolean; maxPoints: number}): void {
  built.lineStyle = {width: LINE_WIDTH, ...built.lineStyle};
  if (built.showSymbol === undefined && (!line.markers || built.data.length > line.maxPoints)) {
    built.showSymbol = false;
  }
  if (built.areaStyle) {
    built.areaStyle = lineArea(built.areaStyle, model, color);
  }
}

/**
 * Fill of the area of a line: a range band is evenly filled, any other area has a gradient, and an area whose color
 * cannot be read keeps a flat opacity. A color set by the model wins
 * @param {object} areaStyle Area style of the built series
 * @param {object} model Series of the model
 * @param {*} color Color of the series
 * @returns {object} Area style
 */
function lineArea(areaStyle: NonNullable<BuiltSeries["areaStyle"]>, model: ModelNode, color: unknown): NonNullable<BuiltSeries["areaStyle"]> {
  if (areaStyle.color !== undefined) {
    return areaStyle;
  }
  const band = Boolean((model.awe as {range?: boolean} | undefined)?.range);
  const fill = band ? withAlpha(color, BAND_ALPHA) : areaGradient(color);
  return fill ? {...areaStyle, color: fill} : {opacity: FLAT_AREA_OPACITY, ...areaStyle};
}

/**
 * Style of a gauge: the progress in the color of the series, and the layout of its center, title and detail. The series
 * is changed
 * @param {object} built Series for ECharts
 * @param {*} color Color of the series
 */
function styleGauge(built: BuiltSeries, color: unknown): void {
  if (typeof color === "string") {
    built.progress = withDefaults(built.progress, {show: true, itemStyle: {color}});
  }
  built.center ??= [...GAUGE_LAYOUT.center];
  built.title = withDefaults(built.title, GAUGE_LAYOUT.title);
  built.detail = withDefaults(built.detail, GAUGE_LAYOUT.detail);
}

/**
 * Narrower bars: the gap between categories and the maximum width of the bars. ECharts reads them from any series of the
 * chart, so they are only given when no series of the model sets them. The series are changed
 * @param {object[]} series Built series
 */
export function styleBars(series: BuiltSeries[]): void {
  const bars = series.filter(serie => serie.type === "bar");
  const missing = (key: string) => bars.every(serie => serie[key] === undefined);
  const gap = missing("barCategoryGap");
  const width = missing("barMaxWidth");
  bars.forEach(serie => {
    if (gap) {
      serie.barCategoryGap = BAR_CATEGORY_GAP;
    }
    if (width) {
      serie.barMaxWidth = BAR_MAX_WIDTH;
    }
  });
}

/**
 * Style of the series that needs the colors of the chrome: the gap between the slices of a pie and the steps of a funnel,
 * the cells of a heatmap, and the track and the texts of a gauge
 * @param {object} serie Series of the option
 * @param {object} colors Colors of the chrome
 * @returns {object} Series with the style
 */
export function colorSeries(serie: Node, colors: StyleColors): Node {
  const border = colors.surface ? {borderColor: colors.surface, borderWidth: SLICE_GAP} : {};
  switch (serie.type) {
    case "pie":
    case "funnel":
      return withDefaults(serie, {itemStyle: border});
    case "heatmap":
      return withDefaults(serie, {itemStyle: border});
    case "gauge":
      return withDefaults(serie, {
        axisLine: {lineStyle: {color: colors.axis ? [[1, colors.axis]] : undefined}},
        axisLabel: {color: colors.muted}, title: {color: colors.muted}, detail: {color: colors.text}
      });
    default:
      return serie;
  }
}

/**
 * Style of the chrome that does not need colors: dashed grid without axis line on the value axes, a vertical pointer and
 * rounded tooltip with the shadow of the design system, and round legend marks
 * @param {object} option ECharts option, which is not changed
 * @returns {object} Option with the style
 */
export function applyModernStyle<T extends object>(option: T): T {
  const source = option as Node;
  const result: Node = {...source};
  const set = (key: string, apply: (node: Node) => Node) => {
    if (source[key] !== undefined) {
      result[key] = each(source[key], apply);
    }
  };
  const hidden = (part: unknown) => isNode(part) && part.show === false;
  const valueAxis = (axis: Node) => axis.type === "value" || axis.type === "log"
    ? withDefaults(axis, {
      axisLine: {show: false}, ...hidden(axis.splitLine) ? {} : {splitLine: {lineStyle: {type: "dashed"}}}
    })
    : axis;
  set("xAxis", valueAxis);
  set("yAxis", valueAxis);
  set("tooltip", tooltip => withDefaults(tooltip, {
    extraCssText: TOOLTIP_CSS,
    ...tooltip.trigger === "axis" ? {axisPointer: {type: "line"}} : {}
  }));
  set("legend", legend => legend.show === false ? legend : withDefaults(legend, {icon: "circle", itemWidth: LEGEND_MARK, itemHeight: LEGEND_MARK}));
  return result as T;
}

/**
 * Centre of a pie in pixels
 * @param {Array} center Center of the series: pixels, percentages or `middle`
 * @param {{width: number, height: number}} size Size of the chart
 * @returns {number[]|null} X and y in pixels, null when it cannot be told
 */
function centerOf(center: unknown, {width, height}: {width: number; height: number}): number[] | null {
  const [x, y] = Array.isArray(center) ? center : ["50%", "50%"];
  const toPixels = (value: unknown, extent: number): number => {
    if (Number.isFinite(value)) {
      return value as number;
    }
    if (value === "middle" || value === "center") {
      return extent / 2;
    }
    return typeof value === "string" && value.endsWith("%") ? Number.parseFloat(value) / 100 * extent : Number.NaN;
  };
  const point = [toPixels(x, width), toPixels(y, height)];
  return point.every(Number.isFinite) ? point : null;
}

/**
 * Size in pixels of the hole of a donut
 * @param {Array} radius Inner and outer radius of the series
 * @param {{width: number, height: number}} size Size of the chart
 * @returns {number} Inner radius in pixels, NaN when it is not a size
 */
function holeOf(radius: Size[], {width, height}: {width: number; height: number}): number {
  const inner = radius[0];
  if (Number.isFinite(inner)) {
    return inner as number;
  }
  return typeof inner === "string" && inner.endsWith("%") ? Number.parseFloat(inner) / 100 * Math.min(width, height) / 2 : Number.NaN;
}

/**
 * Text that shows the total of the values in the centre of a donut. It is not drawn for a pie, a semicircle, a chart with
 * more than one donut, or a hole without room for the text. The total is formatted like the values of the tooltip
 * @param {object[]} series Built series
 * @param {{width: number, height: number, locale: object, tooltipHints: object}} environment Size, locale and number format
 * @returns {object|null} Graphic text, null when the chart has no donut that fits the total
 */
export function donutTotal(series: BuiltSeries[], environment: {width: number; height: number; locale?: ChartLocale; tooltipHints: TooltipHints}): ModelNode | null {
  const donuts = series.filter(serie => serie.type === "pie" && Array.isArray(serie.radius));
  if (donuts.length !== 1 || series.some(serie => serie.type === "pie" && !donuts.includes(serie))) {
    return null;
  }
  const [donut] = donuts;
  const partial = donut.endAngle !== undefined && donut.endAngle !== "auto" &&
    Math.abs(Number(donut.endAngle) - Number(donut.startAngle ?? 90)) !== 360;
  if (partial) {
    return null;
  }
  const hole = holeOf(donut.radius as Size[], environment);
  const center = centerOf(donut.center, environment);
  if (!center || !(environment.width > 0 && environment.height > 0) || Number.isNaN(hole) || hole < DONUT_MIN_HOLE) {
    return null;
  }
  const amounts = (donut.data as Array<DataPoint | unknown[]>).map(point => Array.isArray(point) ? point[1] : (point as DataPoint).value);
  const total = amounts.filter((amount): amount is number => Number.isFinite(amount)).reduce((sum, amount) => sum + amount, 0);
  const locale = environment.locale ?? {};
  const {numberDecimals} = environment.tooltipHints;
  const formatted = createValueText(environment.tooltipHints, locale);
  const text = formatted.formatted ? formatted.write(total) : formatNumber(total, numberDecimals, locale);
  return {
    type: "text", x: center[0], y: center[1], silent: true, z: 10,
    style: {text, fontSize: DONUT_FONT, fontWeight: "bold", textAlign: "center", textVerticalAlign: "middle"}
  };
}
