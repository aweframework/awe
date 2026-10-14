/**
 * Sparkline geometry: a pure function from a series of numbers and a few options to the shapes of a small SVG.
 *
 * Framework-agnostic. The AngularJS client keeps an identical copy (js/awe/services/chart/sparkline.js, checked by
 * sparklineParity.jest.spec.js there), so both engines draw the same SVG for the same cell.
 */

export const SPARKLINE_WIDTH = 100;
export const SPARKLINE_HEIGHT = 24;
export const SPARKLINE_TYPES = ["line", "bar", "area"];

const PADDING_X = 3;
const PADDING_Y = 3;
const MAX_BAR_WIDTH = 12;
const BAR_FILL_RATIO = 0.7;
const DEFAULT_COLOR = "#3b82f6";
const MAX_COLOR = "#2e9e5b";
const MIN_COLOR = "#d64550";
const LAST_BAR_OUTLINE = "#111827";
const ZERO_LINE_COLOR = "#9ca3af";
const PLAIN_COLOR = /^(#[0-9a-fA-F]{3,8}|[a-zA-Z]+|(rgb|hsl)a?\([0-9.,%\s]+\))$/;

/**
 * Rounds a coordinate to two decimals so the same series always gives the same text
 * @param {number} value
 * @return {number}
 */
const round = value => Math.round(value * 100) / 100 || 0;

/**
 * Reads a CSS colour, ignoring anything that is not a plain colour
 * @param {*} color
 * @param {string} fallback
 * @return {string}
 */
const readColor = (color, fallback) => typeof color === "string" && PLAIN_COLOR.test(color.trim()) ? color.trim() : fallback;

/**
 * Reads a boolean option that may arrive as the text of an attribute
 * @param {*} value
 * @return {boolean}
 */
const readFlag = value => value === true || value === "true";

/**
 * Reads the series of a cell: a JSON array of numbers, as text or already parsed
 * @param {*} value Cell value
 * @return {number[]|null} The numbers, or null when the value is not a non-empty list of numbers
 */
export function parseSparklineSeries(value) {
  let list = value;
  if (typeof value === "string") {
    try {
      list = JSON.parse(value);
    } catch {
      return null;
    }
  }
  if (!Array.isArray(list) || list.length === 0) {
    return null;
  }
  const numbers = list.map(item => {
    if (typeof item === "number") {
      return item;
    }
    return typeof item === "string" && item.trim() !== "" ? Number(item) : NaN;
  });
  return numbers.every(Number.isFinite) ? numbers : null;
}

/**
 * Splits the points of a line in runs of the same sign, cutting the segments that cross zero at the zero line
 * @param {number[]} values
 * @param {number[]} xs
 * @param {Function} yOf
 * @param {boolean} split Split by sign
 * @return {Array<{negative: boolean, points: number[][]}>}
 */
function lineRuns(values, xs, yOf, split) {
  const runs = [];
  let current = null;
  values.forEach((value, index) => {
    const negative = split && value < 0;
    const point = [xs[index], yOf(value)];
    if (!current) {
      current = {negative, points: [point]};
      runs.push(current);
    } else if (current.negative === negative) {
      current.points.push(point);
    } else {
      const previous = values[index - 1];
      const ratio = previous / (previous - value);
      const cross = [xs[index - 1] + ratio * (xs[index] - xs[index - 1]), yOf(0)];
      current.points.push(cross);
      current = {negative, points: [cross, point]};
      runs.push(current);
    }
  });
  return runs.filter(run => run.points.length > 1);
}

const pathOf = points => points.map(([x, y], index) => `${index === 0 ? "M" : "L"}${round(x)} ${round(y)}`).join(" ");

/**
 * Builds the SVG of a sparkline
 * @param {*} value Cell value (a JSON array of numbers, as text or already parsed)
 * @param {Object} [options] Drawing options
 * @param {string} [options.type] line (default), bar or area
 * @param {string} [options.color] Colour of the series
 * @param {string} [options.negativeColor] Colour of the negative values
 * @param {boolean|string} [options.lastPoint] Highlight the last point
 * @param {boolean|string} [options.minMax] Mark the lowest and highest points
 * @return {{type: string, width: number, height: number, viewBox: string, label: string, shapes: Object[]}|null} The model of the
 * SVG, or null when the value is not a valid series (the cell stays empty). Every shape is {tag, attrs}; the hit areas
 * that show the tooltip also carry the title with the value of their point.
 */
export function buildSparkline(value, options = {}) {
  const values = parseSparklineSeries(value);
  if (!values) {
    return null;
  }
  const type = SPARKLINE_TYPES.includes(options.type) ? options.type : "line";
  const color = readColor(options.color, DEFAULT_COLOR);
  const negativeColor = options.negativeColor ? readColor(options.negativeColor, null) : null;
  const lastPoint = readFlag(options.lastPoint);
  const minMax = readFlag(options.minMax);

  const count = values.length;
  const bars = type === "bar";
  const minimum = Math.min(...values);
  const maximum = Math.max(...values);
  const low = bars ? Math.min(minimum, 0) : minimum;
  const high = bars ? Math.max(maximum, 0) : maximum;
  const span = high - low;
  const plotWidth = SPARKLINE_WIDTH - 2 * PADDING_X;
  const plotHeight = SPARKLINE_HEIGHT - 2 * PADDING_Y;
  const yOf = item => span === 0 ? SPARKLINE_HEIGHT / 2 : PADDING_Y + (high - item) / span * plotHeight;
  const baseline = yOf(Math.min(Math.max(0, low), high));
  const colorOf = item => item < 0 && negativeColor ? negativeColor : color;
  const slot = plotWidth / count;
  const xs = values.map((_, index) => count === 1 ? SPARKLINE_WIDTH / 2 : PADDING_X + index * plotWidth / (count - 1));
  const shapes = [];

  if (minimum < 0) {
    shapes.push({
      tag: "line",
      attrs: {
        x1: PADDING_X, y1: round(baseline), x2: SPARKLINE_WIDTH - PADDING_X, y2: round(baseline),
        stroke: ZERO_LINE_COLOR, "stroke-width": 0.5
      }
    });
  }

  if (bars) {
    const width = Math.min(slot * BAR_FILL_RATIO, MAX_BAR_WIDTH);
    values.forEach((item, index) => {
      const height = Math.max(Math.abs(yOf(item) - baseline), 1);
      const attrs = {
        x: round(PADDING_X + index * slot + (slot - width) / 2),
        y: round(item < 0 ? baseline : baseline - height),
        width: round(width),
        height: round(height),
        fill: colorOf(item)
      };
      if (lastPoint && index === count - 1 && count > 1) {
        attrs.stroke = LAST_BAR_OUTLINE;
        attrs["stroke-width"] = 1;
      }
      shapes.push({tag: "rect", attrs});
    });
  } else if (count === 1) {
    shapes.push({tag: "circle", attrs: {cx: xs[0], cy: round(yOf(values[0])), r: 2, fill: colorOf(values[0])}});
  } else {
    const runs = lineRuns(values, xs, yOf, Boolean(negativeColor));
    if (type === "area") {
      runs.forEach(run => {
        const first = run.points[0];
        const last = run.points[run.points.length - 1];
        shapes.push({
          tag: "path",
          attrs: {
            d: `M${round(first[0])} ${round(baseline)} ${pathOf(run.points).replace("M", "L")} L${round(last[0])} ${round(baseline)} Z`,
            fill: run.negative ? negativeColor : color,
            "fill-opacity": 0.25,
            stroke: "none"
          }
        });
      });
    }
    runs.forEach(run => shapes.push({
      tag: "path",
      attrs: {
        d: pathOf(run.points),
        fill: "none",
        stroke: run.negative ? negativeColor : color,
        "stroke-width": 1.5,
        "stroke-linecap": "round",
        "stroke-linejoin": "round"
      }
    }));
  }

  // The markers of bars sit on the centre of their bar, the ones of lines on their point
  const markerX = index => bars ? PADDING_X + index * slot + slot / 2 : xs[index];
  const dot = (index, fill, radius) => shapes.push({
    tag: "circle",
    attrs: {cx: round(markerX(index)), cy: round(yOf(values[index])), r: radius, fill, stroke: "#ffffff", "stroke-width": 0.75}
  });
  if (minMax && count > 1 && minimum !== maximum) {
    dot(values.indexOf(minimum), MIN_COLOR, 2);
    dot(values.indexOf(maximum), MAX_COLOR, 2);
  }
  if (lastPoint && count > 1 && !bars) {
    dot(count - 1, colorOf(values[count - 1]), 2.5);
  }

  values.forEach((item, index) => {
    const left = round(index * SPARKLINE_WIDTH / count);
    const right = round((index + 1) * SPARKLINE_WIDTH / count);
    shapes.push({
      tag: "rect",
      attrs: {x: left, y: 0, width: round(right - left), height: SPARKLINE_HEIGHT, fill: "transparent"},
      title: String(item)
    });
  });

  return {
    type,
    width: SPARKLINE_WIDTH,
    height: SPARKLINE_HEIGHT,
    viewBox: `0 0 ${SPARKLINE_WIDTH} ${SPARKLINE_HEIGHT}`,
    label: values.join(", "),
    shapes
  };
}
