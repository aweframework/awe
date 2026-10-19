/**
 * Helpers for the chart actions that change the series of a chart drawn with Apache ECharts (add-chart-series,
 * replace-chart-series): they translate the series that the server sends to the series of the `echartsModel` and merge
 * their points into the values of the chart.
 */

const ECHARTS_TYPES = {column: "bar", bar: "bar", pie: "pie", scatter: "scatter", bubble: "scatter"};

/**
 * Retrieve the row keys that a series of a chart action reads: its xValue, yValue and zValue. A series built in Java with
 * its points only (ChartSerie with data, no xValue nor yValue) gets keys of its own, made of its identifier
 * @param {object} serie Series of the action
 * @returns {{xValue: string, yValue: string, zValue: string}|null} Row keys, null when the series cannot be bound
 */
function bindingOf(serie) {
  const hints = serie.echarts?.awe || {};
  const xValue = serie.xValue ?? hints.xValue;
  const yValue = serie.yValue ?? hints.yValue;
  const zValue = serie.zValue ?? hints.zValue;
  if (xValue && yValue) {
    return {xValue, yValue, zValue};
  }
  const points = Array.isArray(serie.data) ? serie.data : [];
  if (!serie.id || points.length === 0) {
    return null;
  }
  const withZ = points.some(point => Array.isArray(point) && point.length > 2);
  return {xValue: `${serie.id}#x`, yValue: `${serie.id}#y`, zValue: withZ ? `${serie.id}#z` : undefined};
}

/**
 * Give an ECharts series the name and the row keys of the series of the action when it misses them
 * @param {object} serie Series of the action
 * @param {object} translated ECharts series
 * @returns {object} ECharts series, the same object when nothing is missing
 */
function withBinding(serie, translated) {
  const hints = translated.awe || {};
  const binding = bindingOf(serie);
  const unbound = binding && !(hints.xValue && hints.yValue);
  const unnamed = translated.name == null && (serie.name ?? serie.label) != null;
  if (!unbound && !unnamed) {
    return translated;
  }
  const {xValue, yValue, zValue} = binding || {};
  return {
    ...translated,
    ...(unnamed ? {name: serie.name ?? serie.label} : {}),
    ...(unbound ? {awe: {...hints, xValue, yValue, ...(zValue ? {zValue} : {})}} : {})
  };
}

/**
 * Retrieve the ECharts series of a series that a chart action sends. The server sends the translation in the
 * "echarts" property; the series is built from its Highcharts fields when it is missing
 * @param {object} serie Series of the action
 * @param {boolean} inverted The chart is inverted, so its axes are swapped
 * @returns {object} ECharts series
 */
export function getEChartsSerie(serie, inverted) {
  const translated = withBinding(serie, serie.echarts || {
    id: serie.id,
    name: serie.name ?? serie.label,
    type: ECHARTS_TYPES[serie.type] || "line",
    ...(serie.color ? {itemStyle: {color: serie.color}} : {}),
    awe: {
      type: serie.type,
      xValue: serie.xValue,
      yValue: serie.yValue,
      ...(serie.zValue ? {zValue: serie.zValue} : {})
    }
  });
  if (!inverted) {
    return translated;
  }
  const {xAxisIndex, yAxisIndex, ...rest} = translated;
  return {
    ...rest,
    ...(yAxisIndex === undefined ? {} : {xAxisIndex: yAxisIndex}),
    ...(xAxisIndex === undefined ? {} : {yAxisIndex: xAxisIndex})
  };
}

/**
 * Merge the points of the series of an action into the values of the chart, row by row
 * @param {object[]} values Current values
 * @param {object[]} series Series of the action
 * @returns {object[]} New values
 */
export function mergeSeriesPoints(values, series) {
  const rows = [...values];
  series.forEach(serie => {
    const binding = bindingOf(serie);
    if (!binding) {
      return;
    }
    const {xValue, yValue, zValue} = binding;
    (serie.data || []).forEach(([x, y, z], index) => {
      rows[index] = {...rows[index], [xValue]: x, [yValue]: y, ...(zValue ? {[zValue]: z} : {})};
    });
  });
  return rows;
}
