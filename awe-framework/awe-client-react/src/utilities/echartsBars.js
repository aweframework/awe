import {SERIES_INFO} from "./echartsContext";

const DEFAULT_THICKNESS = 20;
const DEFAULT_GAPS = {barGap: 0.3, barCategoryGap: 0.2};
const MARGIN = 16;
const pointOf = (data) => Array.isArray(data) ? data : data?.value;

/**
 * Last value that a bar series gives to an option (ECharts takes the gaps and the widths from the series that set them)
 * @param {object[]} bars Bar series
 * @param {string} key Option
 * @returns {*} Value, undefined when no series sets it
 */
const optionOf = (bars, key) => bars.map(serie => serie[key]).findLast(value => value !== undefined);

/**
 * Read a size that is in pixels or a percentage (`30%`)
 * @param {*} value Size
 * @returns {{pixels: number, fraction: number}|null} Pixels, or fraction of the reference size; null when not a size
 */
function parseSize(value) {
  if (Number.isFinite(value)) {
    return {pixels: value, fraction: 0};
  }
  const percent = typeof value === "string" && value.trim().endsWith("%") ? Number.parseFloat(value) : Number.NaN;
  return Number.isFinite(percent) ? {pixels: 0, fraction: percent / 100} : null;
}

/**
 * Gap of a bar series option, in pixels and as a fraction of the thickness of the bars
 * @param {object[]} bars Bar series
 * @param {string} key barGap or barCategoryGap
 * @returns {{pixels: number, fraction: number}} Gap
 */
const gapOf = (bars, key) => parseSize(optionOf(bars, key)) ?? {pixels: 0, fraction: DEFAULT_GAPS[key]};

/**
 * Estimate the thickness of the bars, which a percentage radius is relative to. An explicit `barWidth` (pixels, or a
 * percentage of the room of the category) is used as it is, and `barMaxWidth` caps it. Otherwise ECharts divides the
 * room of each category between the bars side by side (one for each stack, and for each series that is not stacked) and
 * the gaps between them and between categories, which are pixels or percentages of the thickness
 * @param {object[]} bars Bar series
 * @param {{inverted: boolean, width: number, height: number}} environment Environment
 * @returns {number} Thickness in pixels
 */
function estimateThickness(bars, {inverted, width, height}) {
  const extent = (inverted ? height : width) - 2 * MARGIN;
  const categories = new Set(bars.flatMap(serie => serie.data.map(data => pointOf(data)?.[inverted ? 1 : 0])));
  const usable = extent > 0;
  if (!usable || categories.size === 0) {
    return DEFAULT_THICKNESS;
  }
  const slot = extent / categories.size;
  const groups = new Set(bars.map(serie => serie.stack ?? serie)).size;
  const gap = gapOf(bars, "barGap");
  const categoryGap = gapOf(bars, "barCategoryGap");
  const explicit = parseSize(optionOf(bars, "barWidth"));
  const room = slot - categoryGap.pixels - (groups - 1) * gap.pixels;
  const computed = room / (groups + (groups - 1) * gap.fraction + categoryGap.fraction);
  const thickness = explicit ? explicit.pixels + explicit.fraction * slot : computed;
  const maximum = parseSize(optionOf(bars, "barMaxWidth"));
  return Math.max(1, maximum ? Math.min(thickness, maximum.pixels + maximum.fraction * slot) : thickness);
}

/**
 * Convert the radius of a series to pixels: a number is in pixels, a percentage is relative to the bar thickness
 * @param {number|string} radius Radius
 * @param {number} thickness Thickness of the bars
 * @returns {number} Radius in pixels, which is at most half the thickness
 */
function toPixels(radius, thickness) {
  const text = String(radius).trim();
  const amount = Number.parseFloat(text);
  const pixels = text.endsWith("%") ? amount / 100 * thickness : amount;
  return Number.isFinite(pixels) ? Math.round(Math.min(Math.max(pixels, 0), thickness / 2)) : 0;
}

/**
 * Corners to round in the end of a bar that is away from zero
 * @param {number} radius Radius in pixels
 * @param {boolean} negative The bar is negative
 * @param {boolean} inverted The bars are horizontal
 * @returns {number[]} Radius of the corners: top left, top right, bottom right, bottom left
 */
function corners(radius, negative, inverted) {
  if (inverted) {
    return negative ? [radius, 0, 0, radius] : [0, radius, radius, 0];
  }
  return negative ? [0, 0, radius, radius] : [radius, radius, 0, 0];
}

/**
 * Amount of a point, which is the first coordinate when the bars are horizontal
 * @param {Array} pair Point [x, y], or [y, x] when the chart is inverted
 * @param {boolean} inverted The chart is inverted
 * @returns {number|undefined} Amount
 */
const amountOf = (pair, inverted) => pair?.[inverted ? 0 : 1];

/**
 * Identify the stack that a point belongs to: the stack, the category and the side of zero. A bar that is not stacked
 * is a stack of its own
 * @param {object[]} bars Bar series
 * @param {object} serie Series of the point
 * @param {Array} pair Point
 * @param {boolean} inverted The chart is inverted
 * @returns {string} Identity of the stack
 */
function stackKey(bars, serie, pair, inverted) {
  return `${serie.stack ?? bars.indexOf(serie)}|${pair?.[inverted ? 1 : 0]}|${amountOf(pair, inverted) < 0}`;
}

/**
 * Find, for each stack, the series that ends it
 * @param {object[]} bars Bar series in the order of ECharts, which draws the last one of a stack outermost
 * @param {boolean} inverted The chart is inverted
 * @returns {Map<string, object>} Outermost series by stack
 */
function outermostSeries(bars, inverted) {
  const outermost = new Map();
  bars.forEach(serie => serie.data.forEach(data => {
    const pair = pointOf(data);
    const amount = amountOf(pair, inverted);
    if (Number.isFinite(amount) && amount !== 0) {
      outermost.set(stackKey(bars, serie, pair, inverted), serie);
    }
  }));
  return outermost;
}

/**
 * Give a point the radius of its bar when it ends a stack
 * @param {object|Array} data Point of the series
 * @param {object} serie Series of the point
 * @param {number} radius Radius in pixels
 * @param {{bars: object[], outermost: Map<string, object>, inverted: boolean}} stacks Bars and the end of each stack
 * @returns {object|Array} Point, with its radius when it ends a stack
 */
function roundPoint(data, serie, radius, {bars, outermost, inverted}) {
  const pair = pointOf(data);
  const amount = amountOf(pair, inverted);
  if (!Number.isFinite(amount) || amount === 0 || outermost.get(stackKey(bars, serie, pair, inverted)) !== serie) {
    return data;
  }
  const point = Array.isArray(data) ? {value: data} : data;
  return {...point, itemStyle: {...point.itemStyle, borderRadius: corners(radius, amount < 0, inverted)}};
}

/**
 * Round the corners of the bars like Highcharts does: only the end of the bar that is away from zero, and only in the
 * outermost series of each stack. ECharts rounds a series as a whole, so the radius goes in each point, once the order
 * of the series is final (the stacks were reversed). A percentage radius is approximated from the estimated thickness
 * of the bars.
 * @param {object[]} series Built series, in the order of ECharts
 * @param {{inverted: boolean, width: number, height: number}} environment Environment
 */
export function roundBars(series, environment) {
  const bars = series.filter(serie => serie.type === "bar");
  if (!bars.some(serie => SERIES_INFO.get(serie)?.borderRadius !== undefined)) {
    return;
  }
  const thickness = estimateThickness(bars, environment);
  const stacks = {bars, outermost: outermostSeries(bars, environment.inverted), inverted: environment.inverted};
  bars.forEach(serie => {
    const radius = toPixels(SERIES_INFO.get(serie)?.borderRadius ?? 0, thickness);
    if (radius > 0) {
      serie.data = serie.data.map(data => roundPoint(data, serie, radius, stacks));
    }
  });
}
