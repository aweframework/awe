/**
 * Size of the image of a chart
 */
export interface ChartSize {
  width: number;
  height: number;
}

/**
 * Chart that can draw its image
 */
export interface ChartImage {
  getImage: (size: ChartSize) => string;
}

/**
 * Components of the screen, as far as the print orientation reads them
 */
export type OrientationComponents = Record<string, {
  address?: {component?: string};
  model?: {values?: {value?: string; selected?: boolean}[]};
} | undefined>;

/**
 * Charts that are currently rendered, by component identifier.
 * The form values are collected from the store, which cannot reach the chart instances, so the
 * charts expose themselves here to be able to send their image when the screen is printed. A registered chart is an
 * object with a `getImage({width, height})` method that returns the SVG image of the chart.
 * @type {Map<string, ChartImage>}
 */
const charts = new Map<string, ChartImage>();

/**
 * Size of the image that a chart sends for each page orientation
 * @type {{PORTRAIT: {width: number, height: number}, LANDSCAPE: {width: number, height: number}}}
 */
export const CHART_SIZE: Record<"PORTRAIT" | "LANDSCAPE", ChartSize> = {
  PORTRAIT: {width: 796, height: 540},
  LANDSCAPE: {width: 1167, height: 360}
};

/**
 * Register a rendered chart
 * @param {string} id Chart identifier
 * @param {{getImage: function({width: number, height: number}): string}} chart Chart that can draw its image
 */
export function registerChart(id: string, chart: ChartImage): void {
  charts.set(id, chart);
}

/**
 * Forget a chart that is not rendered anymore
 * @param {string} id Chart identifier
 * @param {object} chart Chart instance that was registered
 */
export function unregisterChart(id: string, chart: ChartImage): void {
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
export function getChartImage(id: string, orientation: string): string | undefined {
  const chart = charts.get(id);
  if (!chart || typeof chart.getImage !== "function") {
    return undefined;
  }
  try {
    return chart.getImage({...((CHART_SIZE as Record<string, ChartSize>)[orientation] || CHART_SIZE.PORTRAIT)});
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
export function getPrintOrientation(components: OrientationComponents | null = {}): string {
  const orientation = Object.values(components || {})
    .find(component => component?.address?.component === "reportOrientation");
  const selected = (orientation?.model?.values || []).find(value => value.selected);
  return selected?.value || "PORTRAIT";
}
