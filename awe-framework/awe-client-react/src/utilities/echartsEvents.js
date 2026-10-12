/**
 * Helpers for the events and the zoom of the charts drawn with Apache ECharts: they translate what ECharts reports to
 * the attributes of an AWE chart (x, y, xMin, xMax, yMin, yMax) and back.
 */
import _ from "lodash";

const FULL_RANGE = {start: 0, end: 100};
const AXES = {x: "xAxisIndex", y: "yAxisIndex"};
const EMPTY_ZOOM = {xMin: null, xMax: null, yMin: null, yMax: null};

/**
 * Read the AWE point (x, y) of an ECharts mouse event
 * @param {object} params ECharts event parameters
 * @returns {{x: *, y: *}} Coordinates of the point, the name and the value when it has no coordinates (pie)
 */
export function pointOf(params) {
  const {value, name} = params ?? {};
  if (Array.isArray(value)) {
    return {x: value[0] ?? null, y: value[1] ?? null};
  }
  return {x: name ?? null, y: value ?? null};
}

/**
 * Retrieve the data zoom components of a chart
 * @param {object} chart ECharts instance
 * @returns {object[]} Data zoom components, empty when the chart cannot tell
 */
function dataZoomOf(chart) {
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
 * @returns {boolean} The component zooms the axis
 */
function zoomsAxis(zoom, axis) {
  return !_.isNil(zoom[AXES[axis]]);
}

/**
 * Read the zoom of a chart as the chart attributes of the AWE chart
 * @param {object} chart ECharts instance
 * @returns {{xMin: *, xMax: *, yMin: *, yMax: *}} Range of each axis, null when the whole axis is shown
 */
export function zoomOf(chart) {
  const zoom = dataZoomOf(chart);
  const result = {...EMPTY_ZOOM};
  Object.keys(AXES).forEach(axis => {
    const component = zoom.find(item => zoomsAxis(item, axis));
    const zoomed = component && !(component.start <= FULL_RANGE.start && component.end >= FULL_RANGE.end);
    if (zoomed) {
      result[`${axis}Min`] = component.startValue ?? null;
      result[`${axis}Max`] = component.endValue ?? null;
    }
  });
  return result;
}

/**
 * Read the chart options that a dependency sends to a chart (an object or its JSON text)
 * @param {object|string} options Chart options
 * @returns {object} Chart options, empty when they are not an object
 */
export function parseChartOptions(options) {
  if (_.isString(options)) {
    try {
      return parseChartOptions(JSON.parse(options));
    } catch (error) {
      console.error("[ERROR] The chart options are not valid JSON", error);
      return {};
    }
  }
  return _.isPlainObject(options) ? options : {};
}

/**
 * Build the ECharts actions that zoom a chart to the range of the chart options
 * @param {object} chart ECharts instance
 * @param {object} zoom Zoom of the chart options: {x: {min, max}, y: {min, max}}
 * @returns {object[]} Data zoom actions, one for each data zoom component of an axis of the zoom
 */
export function zoomRequests(chart, zoom) {
  const requests = [];
  dataZoomOf(chart).forEach((component, dataZoomIndex) => {
    Object.keys(AXES).filter(axis => zoom?.[axis] && zoomsAxis(component, axis)).forEach(axis => {
      const {min = null, max = null} = zoom[axis];
      const range = min === null && max === null ? FULL_RANGE : {startValue: min, endValue: max};
      requests.push({type: "dataZoom", dataZoomIndex, ...range});
    });
  });
  return requests;
}
