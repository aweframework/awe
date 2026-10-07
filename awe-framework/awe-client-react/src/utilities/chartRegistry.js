/**
 * Charts that are currently rendered, by component identifier.
 * The form values are collected from the store, which cannot reach the chart instances, so the
 * charts expose themselves here to be able to send their image when the screen is printed. A registered chart is an
 * object with a `getImage({width, height})` method that returns the SVG image of the chart.
 * @type {Map<string, object>}
 */
const charts = new Map();

/**
 * Size of the image that a chart sends for each page orientation
 * @type {{PORTRAIT: {width: number, height: number}, LANDSCAPE: {width: number, height: number}}}
 */
export const CHART_SIZE = {
  PORTRAIT: {width: 796, height: 540},
  LANDSCAPE: {width: 1167, height: 360}
};

/**
 * Register a rendered chart
 * @param {string} id Chart identifier
 * @param {{getImage: function({width: number, height: number}): string}} chart Chart that can draw its image
 */
export function registerChart(id, chart) {
  charts.set(id, chart);
}

/**
 * Forget a chart that is not rendered anymore
 * @param {string} id Chart identifier
 * @param {object} chart Chart instance that was registered
 */
export function unregisterChart(id, chart) {
  // A chart that has already been replaced by a new instance must stay registered
  if (charts.get(id) === chart) {
    charts.delete(id);
  }
}

/**
 * Retrieve the SVG image of a rendered chart
 * @param {string} id Chart identifier
 * @param {string} orientation Page orientation (PORTRAIT or LANDSCAPE)
 * @returns {string|undefined} SVG image, or undefined when the chart is not rendered
 */
export function getChartImage(id, orientation) {
  const chart = charts.get(id);
  if (!chart || typeof chart.getImage !== "function") {
    return undefined;
  }
  try {
    return chart.getImage({...(CHART_SIZE[orientation] || CHART_SIZE.PORTRAIT)});
  } catch (error) {
    console.warn(`[WARNING] Chart '${id}' image couldn't be generated`, error);
    return undefined;
  }
}

/**
 * Retrieve the page orientation selected to print
 * @param {object} components Components of the screen
 * @returns {string} Page orientation (PORTRAIT by default)
 */
export function getPrintOrientation(components = {}) {
  const orientation = Object.values(components || {})
    .find(component => component?.address?.component === "reportOrientation");
  const selected = (orientation?.model?.values || []).find(value => value.selected);
  return selected?.value || "PORTRAIT";
}
