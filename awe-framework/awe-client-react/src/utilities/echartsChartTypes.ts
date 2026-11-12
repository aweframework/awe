/**
 * Chart types that are not drawn from a row per point: radar, heatmap, gauge, funnel and the range band. Each helper
 * turns the rows of the component into what ECharts needs for the type (indicators, categories, visual map limits,
 * the stacked series of a band).
 */
import {
  BuiltSeries,
  DataPoint,
  ModelNode,
  ModelSeries,
  Point,
  SERIES_INFO,
  asArray,
  hintsOf
} from "./echartsContext";
import {ChartLocale} from "./chartFormat";
import {TooltipHints, createValueText} from "./echartsTooltip";

type Row = Record<string, unknown>;

/**
 * What a type needs to bind a series
 */
export interface TypeBinding {
  /** Translated name of the series */
  name?: string;
  /** Indicators of the radar chart: the distinct x values of all the radar series */
  indicators: unknown[];
  /** Whether the x and y axes are category axes (the default), which take the coordinates of a heatmap as text */
  categoryAxes?: {x: boolean; y: boolean};
}

const HIDDEN_LINE = {width: 0, opacity: 0};
const RANGE_SEPARATOR = " – ";
/** Room below the plot for a horizontal visual map, in pixels */
export const VISUAL_MAP_ROOM = 56;

const isType = (type: string) => (serie: {type?: string}): boolean => serie.type === type;
export const isRadar = isType("radar");
export const isHeatmap = isType("heatmap");
export const isGauge = isType("gauge");
export const isRange = (serie: ModelSeries): boolean => serie.type === "line" && Boolean(hintsOf(serie).range);

/**
 * Rows that a series reads: its own rows (`awe.values`) or the values of the component
 * @param {object} serie Series of the model
 * @param {object[]} values Component values
 * @returns {object[]} Rows
 */
export function rowsOf(serie: ModelSeries, values: Row[]): Row[] {
  return asArray<Row>(hintsOf(serie).values ?? values);
}

/**
 * Distinct values of a list, in order
 * @param {Array} list Values
 * @returns {Array} Values without repetition, without nulls
 */
const distinct = (list: unknown[]): unknown[] => [...new Set(list.filter(item => item !== null && item !== undefined))];

/**
 * Indicators of the radar chart: the distinct x values of its series, in the order of the rows
 * @param {object[]} modelSeries Series of the model
 * @param {object[]} values Component values
 * @returns {Array} Indicator names
 */
export function radarIndicators(modelSeries: ModelSeries[], values: Row[]): unknown[] {
  return distinct(modelSeries.filter(isRadar)
    .flatMap(serie => rowsOf(serie, values).map(row => row[hintsOf(serie).xValue as string])));
}

/**
 * Bind the rows to the data of a series that is not drawn from a row per point
 * @param {object} serie Series of the model
 * @param {object[]} values Component values
 * @param {{name: string, indicators: Array}} binding Name of the series and indicators of the radar
 * @returns {Array|undefined} Data, undefined when the type of the series is bound row by row
 */
export function bindTypedData(serie: ModelSeries, values: Row[], binding: TypeBinding): Array<Point | DataPoint> | undefined {
  const {xValue, yValue, zValue} = hintsOf(serie);
  const rows = rowsOf(serie, values);
  if (isRadar(serie)) {
    // One data item with a value for each indicator, so that a series is one polygon
    const byIndicator = new Map(rows.map(row => [row[xValue as string], row[yValue as string]]));
    return [{name: binding.name, value: binding.indicators.map(indicator => byIndicator.get(indicator) ?? null)}];
  }
  if (isGauge(serie)) {
    return rows.length === 0 ? [] : [{name: binding.name, value: rows[0][yValue as string] ?? null}];
  }
  if (isHeatmap(serie)) {
    // A number in a category axis is a position, so the categories are text; any other axis keeps its values
    const {x: textX = true, y: textY = true} = binding.categoryAxes ?? {};
    const coordinate = (value: unknown, text: boolean) => {
      if (value === null || value === undefined) {
        return null;
      }
      return text ? String(value as string | number) : value;
    };
    return rows.map(row => [coordinate(row[xValue as string], textX), coordinate(row[yValue as string], textY),
      row[zValue as string] ?? null]);
  }
  return undefined;
}

/**
 * Complete a built gauge: the first z value is its maximum, unless the model sets it
 * @param {object} built Series for ECharts, which is changed
 * @param {object} serie Series of the model
 * @param {object[]} values Component values
 */
export function completeGauge(built: BuiltSeries, serie: ModelSeries, values: Row[]): void {
  const zValue = hintsOf(serie).zValue;
  const max = zValue ? rowsOf(serie, values)[0]?.[zValue] : undefined;
  if (built.max === undefined && Number.isFinite(max)) {
    built.max = max;
  }
}

/**
 * Indicators of the radar: the ones that the model gives, or the ones of the data
 * @param {object|object[]} radar Radar of the model
 * @param {Array} indicators Indicator names of the data
 * @returns {object|object[]} Radar for ECharts, without hints
 */
export function completeRadar(radar: ModelNode | ModelNode[], indicators: unknown[]): ModelNode | ModelNode[] {
  const complete = (node: ModelNode): ModelNode =>
    node.indicator === undefined ? {...node, indicator: indicators.map(name => ({name}))} : node;
  return Array.isArray(radar) ? radar.map(complete) : complete(radar);
}

/**
 * Categories of an axis of a heatmap: the distinct values that its series put in that coordinate
 * @param {object[]} users Built series that use the axis
 * @param {number} index Coordinate of the points: 0 for the x axis, 1 for the y axis
 * @returns {string[]|undefined} Categories, undefined when no heatmap uses the axis
 */
export function heatmapCategories(users: BuiltSeries[], index: number): unknown[] | undefined {
  const maps = users.filter(isHeatmap);
  return maps.length === 0 ? undefined : distinct(maps.flatMap(serie => serie.data.map(point => (point as Point)[index])));
}

/**
 * Complete the visual map of a heatmap: its limits are the ones of the z values, unless the model sets them
 * @param {object|object[]} visualMap Visual map of the model
 * @param {object[]} series Built series
 * @returns {object|object[]} Visual map for ECharts
 */
export function completeVisualMap(visualMap: ModelNode | ModelNode[], series: BuiltSeries[]): ModelNode | ModelNode[] {
  const amounts = series.filter(isHeatmap).flatMap(serie => serie.data.map(point => (point as Point)[2]))
    .filter((amount): amount is number => Number.isFinite(amount));
  const [min, max] = amounts.length > 0 ? [Math.min(...amounts), Math.max(...amounts)] : [0, 1];
  const complete = (node: ModelNode): ModelNode => ({min, max, ...node});
  return Array.isArray(visualMap) ? visualMap.map(complete) : complete(visualMap);
}

/**
 * Check if a visual map is horizontal, which is drawn at the bottom of the chart and needs room there
 * @param {object|object[]} visualMap Visual map of the model
 * @returns {boolean} The visual map is horizontal
 */
export function hasHorizontalVisualMap(visualMap: unknown): boolean {
  return asArray<ModelNode>(Array.isArray(visualMap) ? visualMap : [visualMap])
    .some(node => node?.orient === "horizontal" && node.show !== false);
}

/**
 * A range band is drawn with two stacked lines: a hidden one that goes along the low values, and the visible one that
 * has the height of the band. The visible one is the series of the chart (name, legend, color), and its points keep the
 * real values in `range` for the tooltip and the events. The hidden one is silent, so the tooltip and the mouse
 * ignore it, and it takes no name of the model, so the legend does not list it
 * @param {object[]} series Built series, in the order of ECharts
 * @param {{inverted: boolean, tooltipHints: object, locale: object}} environment Environment
 * @returns {object[]} Series with the hidden base before each band
 */
export function withRangeBases(series: BuiltSeries[], environment: {inverted: boolean; tooltipHints: TooltipHints; locale?: ChartLocale}): BuiltSeries[] {
  const [xIndex, yIndex] = environment.inverted ? [1, 0] : [0, 1];
  return series.flatMap((serie, index) => {
    const info = SERIES_INFO.get(serie);
    if (!info?.range) {
      return [serie];
    }
    const stack = `\u0000range:${index}`;
    const place = (x: unknown, amount: unknown): Point => environment.inverted ? [amount, x] : [x, amount];
    const points = serie.data as Point[];
    const base: BuiltSeries = {
      type: "line", stack, silent: true, symbol: "none", legendHoverLink: false, lineStyle: HIDDEN_LINE,
      // An axis tooltip lists every series of the axis unless the series says it has no tooltip
      tooltip: {show: false},
      data: points.map(point => place(point[xIndex], point[yIndex] ?? null))
    };
    SERIES_INFO.set(base, {silent: true});
    const band = serie as BuiltSeries;
    band.stack = stack;
    band.data = points.map(point => {
      const [low, high] = [point[yIndex], point[2]];
      const height = Number.isFinite(low) && Number.isFinite(high) ? (high as number) - (low as number) : null;
      return {value: place(point[xIndex], height), range: [low ?? null, high ?? null]} as DataPoint;
    });
    const write = createValueText(environment.tooltipHints, environment.locale ?? {}, info.valueSuffix).write;
    band.tooltip = {
      ...band.tooltip,
      valueFormatter: (_value: unknown, dataIndex: number) => {
        const [low, high] = (band.data[dataIndex] as {range?: unknown[]}).range ?? [];
        return `${write(low)}${RANGE_SEPARATOR}${write(high)}`;
      }
    };
    return [base, band];
  });
}
