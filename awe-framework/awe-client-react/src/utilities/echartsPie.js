const PIE_LABEL = {charWidth: 7, lineLength: 20, margin: 16, minRadius: 30};
const DEFAULT_RADIUS = "75%";

/**
 * Texts of the labels of a pie, as they are drawn
 * @param {object} built Pie series for ECharts, with its data and its label
 * @returns {string[]} Texts
 */
function labelTexts(built) {
  const total = built.data.reduce((sum, point) => sum + (Number.isFinite(point.value) ? point.value : 0), 0);
  const format = built.label?.formatter;
  return built.data.map(point => {
    const percent = total && Number.isFinite(point.value) ? point.value / total * 100 : 0;
    return String((typeof format === "function" ? format({name: point.name, value: point.value, percent}) : point.name) ?? "");
  });
}

/**
 * Convert a radius to pixels
 * @param {number|string|undefined} radius Radius in pixels or as a percentage of the base
 * @param {number} base Pixels that 100% is
 * @returns {number} Pixels, NaN when the radius is not a size
 */
function toPixels(radius, base) {
  if (Number.isFinite(radius)) {
    return radius;
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
export function fitPieLabels(built, {width, height}) {
  const sized = width > 0 && height > 0;
  if (!sized || !built.label?.show || built.label.position === "inside") {
    return;
  }
  const longest = Math.max(0, ...labelTexts(built).map(text => text.length));
  const room = width / 2 - longest * PIE_LABEL.charWidth - PIE_LABEL.lineLength - PIE_LABEL.margin;
  const limit = Math.round(Math.max(PIE_LABEL.minRadius, room));
  const base = Math.min(width, height) / 2;
  const donut = Array.isArray(built.radius);
  const [inner, outer] = donut ? built.radius : [undefined, built.radius ?? DEFAULT_RADIUS];
  const outerPixels = toPixels(outer, base);
  const overflows = outerPixels > limit;
  if (!overflows) {
    return;
  }
  const innerPixels = toPixels(inner, base);
  built.radius = donut ? [Number.isFinite(innerPixels) ? Math.round(innerPixels * limit / outerPixels) : inner, limit] : limit;
}
