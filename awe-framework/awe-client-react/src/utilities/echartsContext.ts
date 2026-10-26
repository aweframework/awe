/**
 * Helpers shared by the modules that build the ECharts option: the `awe` hints of the model and the description of a
 * point in the terms of the Highcharts format language.
 */

/**
 * Size (pixels or a percentage such as `30%`) or any other value that the model gives to an option
 */
export type Size = number | string;

/**
 * Client hints that the server writes in the `awe` key of the objects of the model. Each module reads the hints of
 * the object it builds (series, axes, tooltip, legend, title, model)
 */
export interface AweHints {
  [key: string]: unknown;
  /** Row keys of the series */
  xValue?: string;
  yValue?: string;
  zValue?: string;
  /** Rows of the series, when it does not read the values of the component */
  values?: Array<Record<string, unknown>>;
  /** Type of the series in the screen (bubble...), or of the axis (x, y) in `axis` */
  type?: string;
  axis?: string;
  userOptions?: object;
  valueSuffix?: string;
  borderRadius?: Size;
  labelFormat?: string;
  markerFill?: string;
  minSize?: Size;
  maxSize?: Size;
  linkedTo?: string;
  showInLegend?: boolean;
  legendIndex?: number;
  stackPercent?: boolean;
  /** Identifier of the series that a series opens, or the drilldown series of the model */
  drilldown?: string | {series?: ModelSeries[]};
  inverted?: boolean;
  /** Formatting of the labels of an axis */
  formatter?: string;
  dateTimeLabelFormats?: Record<string, string>;
  /** Formats of the tooltip */
  numberDecimals?: number;
  prefix?: string;
  suffix?: string;
  pointFormat?: string;
  headerFormat?: string;
  footerFormat?: string;
  dateFormat?: string;
  useHTML?: boolean;
  /** Offset of the title, and text of the title of the legend */
  offsetY?: number;
  title?: string;
}

/**
 * Object of the model that the server sends: an ECharts option without data and without functions, which may carry
 * client hints
 */
export interface ModelNode {
  [key: string]: unknown;
  awe?: AweHints;
}

/**
 * Label of a series
 */
export interface SeriesLabel extends ModelNode {
  show?: boolean;
  position?: string;
  color?: string;
  formatter?: unknown;
}

/**
 * Style of a series
 */
export interface SeriesStyle extends ModelNode {
  color?: unknown;
  origin?: string;
  borderRadius?: unknown;
}

/**
 * Series of the model
 */
export interface ModelSeries extends ModelNode {
  id?: string;
  name?: string;
  type?: string;
  silent?: boolean;
  stack?: string;
  colorBy?: string;
  label?: SeriesLabel;
  itemStyle?: SeriesStyle;
  tooltip?: ModelNode;
  showSymbol?: boolean;
  showEmptyCircle?: boolean;
  center?: unknown[];
  radius?: Size | Size[];
}

/**
 * Point of a series: the coordinates [x, y, z], or [y, x, z] when the chart is inverted
 */
export type Point = unknown[];

/**
 * Point of a series that has its own name, value or style
 */
export interface DataPoint {
  name?: unknown;
  value?: unknown;
  itemStyle?: SeriesStyle;
  label?: SeriesLabel;
}

/**
 * Series that was built for ECharts, with its data and what the modules add to it
 */
export interface BuiltSeries {
  [key: string]: unknown;
  type?: string;
  name?: string;
  stack?: string;
  data: Array<Point | DataPoint>;
  label?: SeriesLabel;
  itemStyle?: SeriesStyle;
  lineStyle?: ModelNode;
  areaStyle?: SeriesStyle;
  radius?: Size | Size[];
  center?: unknown[];
  showSymbol?: boolean;
  showEmptyCircle?: boolean;
  symbolSize?: (point: Point) => number;
  tooltip?: ModelNode;
}

/**
 * Key of the objects of the model that holds the AWE client hints
 * @type {string}
 */
export const AWE = "awe" as const;

export const hintsOf = (node?: ModelNode | null): AweHints => node?.[AWE] || {};
export const withoutAwe = (node?: ModelNode | null): Record<string, unknown> =>
  Object.fromEntries(Object.entries(node || {}).filter(([key]) => key !== AWE));
export const isPie = (serie: {type?: string}): boolean => serie.type === "pie";
export const asArray = <T = unknown>(value: unknown): T[] => Array.isArray(value) ? value : [];

/**
 * What the formats and the bars know about each series that was built
 * @type {WeakMap<object, SeriesInfo>}
 */
export const SERIES_INFO = new WeakMap<object, SeriesInfo>();

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
export interface SeriesInfo {
  userOptions?: object;
  silent?: boolean;
  color?: unknown;
  valueSuffix?: string;
  borderRadius?: Size;
}

/**
 * What ECharts gives to a formatter about a point
 */
export interface PointParams {
  seriesName?: string;
  seriesIndex?: number;
  name?: unknown;
  value?: unknown;
  percent?: number;
  color?: unknown;
  marker?: string;
  axisValueLabel?: string;
}

/**
 * Description of a point in the terms of the Highcharts format language
 */
export interface PointContext {
  series: {name?: string; color?: unknown; userOptions?: object};
  point: {name: unknown; x: unknown; y: unknown; z: unknown; key: unknown; percentage?: number; color: unknown};
  name: unknown;
  key: unknown;
  x: unknown;
  y: unknown;
  z: unknown;
  value: unknown;
  percentage?: number;
}

/**
 * Describe a point that ECharts gives to a formatter in the terms of the Highcharts format language
 * @param {object} params Formatter parameters of ECharts
 * @param {boolean} inverted The chart is inverted, so the coordinates are [y, x]
 * @param {SeriesInfo} [info] What the formats know about the series of the point
 * @returns {object} Context of the format template
 */
export function pointContext(params: PointParams, inverted: boolean, info: SeriesInfo = {}): PointContext {
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
