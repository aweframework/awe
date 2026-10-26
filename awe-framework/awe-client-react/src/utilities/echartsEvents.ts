/**
 * Helpers for the events and the zoom of the charts drawn with Apache ECharts: they translate what ECharts reports to
 * the attributes of an AWE chart (x, y, xMin, xMax, yMin, yMax) and back.
 */
import _ from "lodash";

/**
 * Axis of an AWE chart
 */
type Axis = "x" | "y";

/**
 * Data zoom component of a chart, as far as the helpers read it
 */
interface DataZoomComponent {
  xAxisIndex?: number | number[] | null;
  yAxisIndex?: number | number[] | null;
  start?: number;
  end?: number;
  startValue?: unknown;
  endValue?: unknown;
}

/**
 * ECharts instance, as far as the helpers read it
 */
export interface EventChart {
  getOption?: () => {dataZoom?: DataZoomComponent | DataZoomComponent[]} | undefined;
}

/**
 * Range that each axis shows, null when the whole axis is shown
 */
export interface ZoomRange {
  xMin: unknown;
  xMax: unknown;
  yMin: unknown;
  yMax: unknown;
}

/**
 * Zoom of the chart options: the range of each axis
 */
export type ChartZoom = Partial<Record<Axis, {min?: number | null; max?: number | null}>>;

/**
 * Action of ECharts that zooms a data zoom component
 */
export interface DataZoomAction {
  type: "dataZoom";
  dataZoomIndex: number;
  start?: number;
  end?: number;
  startValue?: number | null;
  endValue?: number | null;
}

const FULL_RANGE = {start: 0, end: 100};
const AXES: Record<Axis, "xAxisIndex" | "yAxisIndex"> = {x: "xAxisIndex", y: "yAxisIndex"};
// The AWE x axis is the vertical one in an inverted chart
const INVERTED_AXES: Record<Axis, "xAxisIndex" | "yAxisIndex"> = {x: "yAxisIndex", y: "xAxisIndex"};
const axesOf = (inverted: boolean) => inverted ? INVERTED_AXES : AXES;
const EMPTY_ZOOM: ZoomRange = {xMin: null, xMax: null, yMin: null, yMax: null};

/**
 * Read the AWE point (x, y) of an ECharts mouse event
 * @param {object} params ECharts event parameters
 * @param {boolean} [inverted] The chart is inverted: its points are drawn as [y, x]
 * @returns {{x: *, y: *}} Coordinates of the point, the name and the value when it has no coordinates (pie)
 */
export function pointOf(params?: {name?: unknown; value?: unknown} | null, inverted = false): {x: unknown; y: unknown} {
  const {value, name} = params ?? {};
  if (Array.isArray(value)) {
    const [x, y] = inverted ? [value[1], value[0]] : value;
    return {x: x ?? null, y: y ?? null};
  }
  return {x: name ?? null, y: value ?? null};
}

/**
 * Retrieve the data zoom components of a chart
 * @param {object} chart ECharts instance
 * @returns {object[]} Data zoom components, empty when the chart cannot tell
 */
function dataZoomOf(chart?: EventChart | null): DataZoomComponent[] {
  try {
    return _.castArray(chart?.getOption?.()?.dataZoom ?? []);
  } catch {
    // A chart that is being disposed cannot tell its zoom
    return [];
  }
}

/**
 * Check if a data zoom component zooms an axis
 * @param {object} zoom Data zoom component
 * @param {string} axis AWE axis (x or y)
 * @param {boolean} inverted The chart is inverted
 * @returns {boolean} The component zooms the axis
 */
function zoomsAxis(zoom: DataZoomComponent, axis: Axis, inverted: boolean): boolean {
  return !_.isNil(zoom[axesOf(inverted)[axis]]);
}

/**
 * Read the zoom of a chart as the chart attributes of the AWE chart
 * @param {object} chart ECharts instance
 * @param {boolean} [inverted] The chart is inverted
 * @returns {{xMin: *, xMax: *, yMin: *, yMax: *}} Range of each axis, null when the whole axis is shown
 */
export function zoomOf(chart?: EventChart | null, inverted = false): ZoomRange {
  const zoom = dataZoomOf(chart);
  const result: Record<string, unknown> = {...EMPTY_ZOOM};
  (Object.keys(AXES) as Axis[]).forEach(axis => {
    const component = zoom.find(item => zoomsAxis(item, axis, inverted));
    const zoomed = component && !((component.start as number) <= FULL_RANGE.start && (component.end as number) >= FULL_RANGE.end);
    if (zoomed) {
      result[`${axis}Min`] = component.startValue ?? null;
      result[`${axis}Max`] = component.endValue ?? null;
    }
  });
  return result as unknown as ZoomRange;
}

/**
 * Read the chart options that a dependency sends to a chart (an object or its JSON text)
 * @param {object|string} options Chart options
 * @returns {object} Chart options, empty when they are not an object
 */
export function parseChartOptions(options?: unknown): Record<string, unknown> {
  if (_.isString(options)) {
    try {
      return parseChartOptions(JSON.parse(options));
    } catch (error) {
      console.error("[ERROR] The chart options are not valid JSON", error);
      return {};
    }
  }
  return _.isPlainObject(options) ? options as Record<string, unknown> : {};
}

/**
 * Build the ECharts actions that zoom a chart to the range of the chart options
 * @param {object} chart ECharts instance
 * @param {object} zoom Zoom of the chart options: {x: {min, max}, y: {min, max}}
 * @param {boolean} [inverted] The chart is inverted
 * @returns {object[]} Data zoom actions, one for each data zoom component of an axis of the zoom
 */
export function zoomRequests(chart?: EventChart | null, zoom?: ChartZoom | null, inverted = false): DataZoomAction[] {
  const requests: DataZoomAction[] = [];
  dataZoomOf(chart).forEach((component, dataZoomIndex) => {
    (Object.keys(AXES) as Axis[]).filter(axis => zoom?.[axis] && zoomsAxis(component, axis, inverted)).forEach(axis => {
      const {min = null, max = null} = zoom?.[axis] as {min?: number | null; max?: number | null};
      const range = min === null && max === null ? FULL_RANGE : {startValue: min, endValue: max};
      requests.push({type: "dataZoom", dataZoomIndex, ...range});
    });
  });
  return requests;
}
