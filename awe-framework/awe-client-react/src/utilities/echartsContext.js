/**
 * Helpers shared by the modules that build the ECharts option: the `awe` hints of the model and the description of a
 * point in the terms of the Highcharts format language.
 */

/**
 * Key of the objects of the model that holds the AWE client hints
 * @type {string}
 */
export const AWE = "awe";

export const hintsOf = (node) => node?.[AWE] || {};
export const withoutAwe = (node) => Object.fromEntries(Object.entries(node || {}).filter(([key]) => key !== AWE));
export const isPie = (serie) => serie.type === "pie";
export const asArray = (value) => Array.isArray(value) ? value : [];

/**
 * What the formats and the bars know about each series that was built
 * @type {WeakMap<object, SeriesInfo>}
 */
export const SERIES_INFO = new WeakMap();

/**
 * What the formats know about a series, besides what ECharts gives to the formatters
 * @typedef {object} SeriesInfo
 * @property {object} [userOptions] Options of the series as they were written in the screen, which the formats read
 * as `series.userOptions`
 * @property {boolean} [silent] The series does not react to the mouse, so it is not shown in the tooltip
 * @property {string} [color] Color of the series
 * @property {string} [valueSuffix] Suffix of the values of the series in the tooltip
 * @property {number|string} [borderRadius] Radius of the corners of its bars, in pixels or as a percentage
 */

/**
 * Describe a point that ECharts gives to a formatter in the terms of the Highcharts format language
 * @param {object} params Formatter parameters of ECharts
 * @param {boolean} inverted The chart is inverted, so the coordinates are [y, x]
 * @param {SeriesInfo} [info] What the formats know about the series of the point
 * @returns {object} Context of the format template
 */
export function pointContext(params, inverted, info = {}) {
  const coordinates = Array.isArray(params.value) ? params.value : null;
  const [xIndex, yIndex] = inverted ? [1, 0] : [0, 1];
  const x = coordinates?.[xIndex];
  const y = coordinates ? coordinates[yIndex] : params.value;
  const z = coordinates ? coordinates[2] : undefined;
  const key = params.name ?? x;
  return {
    series: {name: params.seriesName, color: info.color, userOptions: info.userOptions},
    point: {name: params.name, x, y, z, key, percentage: params.percent, color: params.color},
    name: params.name, key, x, y, z, value: y, percentage: params.percent
  };
}
