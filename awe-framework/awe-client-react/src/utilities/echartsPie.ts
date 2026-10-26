import {BuiltSeries, DataPoint, Size} from "./echartsContext";

/**
 * Pie series that was built for ECharts, with its data and its label
 */
export type PieSeries = BuiltSeries;

const PIE_LABEL = {charWidth: 7, lineLength: 20, margin: 16, minRadius: 30};
const DEFAULT_RADIUS = "75%";

type Radius = Size | undefined;

/**
 * Texts of the labels of a pie, as they are drawn
 * @param {object} built Pie series for ECharts, with its data and its label
 * @returns {string[]} Texts
 */
function labelTexts(built: PieSeries): string[] {
  const points = built.data as DataPoint[];
  const total = points.reduce((sum, point) => sum + (Number.isFinite(point.value) ? point.value as number : 0), 0);
  const format = built.label?.formatter;
  return points.map(point => {
    const percent = total && Number.isFinite(point.value) ? point.value as number / total * 100 : 0;
    return String((typeof format === "function" ? (format as (params: object) => unknown)({name: point.name, value: point.value, percent}) : point.name) ?? "");
  });
}

/**
 * Convert a radius to pixels
 * @param {number|string|undefined} radius Radius in pixels or as a percentage of the base
 * @param {number} base Pixels that 100% is
 * @returns {number} Pixels, NaN when the radius is not a size
 */
function toPixels(radius: Radius, base: number): number {
  if (Number.isFinite(radius)) {
    return radius as number;
  }
  return typeof radius === "string" && radius.endsWith("%") ? Number.parseFloat(radius) / 100 * base : Number.NaN;
}

/**
 * Highcharts shrinks a pie until its outer labels fit; ECharts truncates the labels. Cap the outer radius in pixels by
 * the room that is left at the sides of the pie for the longest label (there is always a minimum), and shrink the inner
 * radius of a donut in the same proportion. A wide panel keeps its radius
 * @param {object} built Pie series for ECharts, which is changed
 * @param {{width: number, height: number}} environment Size of the chart in pixels, 0 when it is not known
 */
export function fitPieLabels(built: PieSeries, {width, height}: {width: number; height: number}): void {
  const sized = width > 0 && height > 0;
  if (!sized || !built.label?.show || built.label.position === "inside") {
    return;
  }
  const longest = Math.max(0, ...labelTexts(built).map(text => text.length));
  const room = width / 2 - longest * PIE_LABEL.charWidth - PIE_LABEL.lineLength - PIE_LABEL.margin;
  const limit = Math.round(Math.max(PIE_LABEL.minRadius, room));
  const base = Math.min(width, height) / 2;
  const donut = Array.isArray(built.radius);
  const [inner, outer] = donut ? built.radius as Size[] : [undefined, built.radius as Radius ?? DEFAULT_RADIUS];
  const outerPixels = toPixels(outer, base);
  const overflows = outerPixels > limit;
  if (!overflows) {
    return;
  }
  const innerPixels = toPixels(inner, base);
  built.radius = donut ? [Number.isFinite(innerPixels) ? Math.round(innerPixels * limit / outerPixels) : inner as Size, limit] : limit;
}
