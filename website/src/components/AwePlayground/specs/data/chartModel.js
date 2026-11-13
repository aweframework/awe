/**
 * The `echartsModel` of a chart as the server sends it, built from a plain description. The server builds it from the
 * `chart`, `chart-serie`, `x-axis`, `y-axis`, `chart-legend` and `chart-tooltip` elements (EChartsModelBuilder in awe-model);
 * the playground has the same elements in its XML, so the stage gets the model those elements would produce. The model
 * carries no data: the client reads the points from the rows of the component, guided by the `xValue` and `yValue` hints.
 * Free of React so that node:test can run it.
 *
 * @param {Object} description
 * @param {string} description.type AWE chart type: line, spline, area, areaspline, column, bar, pie, donut, semicircle, scatter,
 *   bubble, arearange, areasplinerange, radar, heatmap, gauge or funnel
 * @param {string} description.category Field of the rows that names each point (the month, the product line)
 * @param {Array<{id: string, label: string, field: string, z?: string}>} description.series Series: the fields of the rows each
 *   one reads (field is the y value, z the z value: the size of a bubble, the high of a range, the cell of a heatmap)
 * @param {string} [description.stacking] normal or percent
 * @param {boolean} [description.legend] Legend; hidden by default on a chart with axes, shown on a pie
 * @param {boolean} [description.dataLabels] Value on every point
 * @param {boolean} [description.sharedTooltip] One tooltip for every series, at the position of the mouse
 * @param {string} [description.title] Title
 * @param {string} [description.subtitle] Subtitle
 * @param {string} [description.xLabel] Name of the category axis
 * @param {string} [description.yLabel] Name of the value axis
 * @returns {Object} ECharts model
 */
const PIES = new Set(['pie', 'donut', 'semicircle']);
const SMOOTH = new Set(['spline', 'areaspline', 'areasplinerange']);
const AREAS = new Set(['area', 'areaspline', 'arearange', 'areasplinerange']);
const RANGES = new Set(['arearange', 'areasplinerange']);
const POINTS = new Set(['scatter', 'bubble']);
/** Types that are not drawn on axes: they have no axes, an item tooltip and no stacking */
const AXIS_FREE = new Set([...PIES, 'radar', 'gauge', 'funnel']);
const STACKABLE = new Set(['line', 'spline', 'area', 'areaspline', 'column', 'bar']);

function axisOf(role, type, label, inverse) {
  return {
    ...(label && {name: label, nameLocation: 'middle', nameGap: 30}),
    type,
    ...(inverse && {inverse: true}),
    awe: {axis: role},
  };
}

function echartsTypeOf(type) {
  if (PIES.has(type)) return 'pie';
  if (POINTS.has(type)) return 'scatter';
  if (type === 'column' || type === 'bar') return 'bar';
  if (['radar', 'heatmap', 'gauge', 'funnel'].includes(type)) return type;
  return 'line';
}

function serieOf(description, serie) {
  const {type, category, stacking, dataLabels} = description;
  const pie = PIES.has(type);
  const stacked = stacking && STACKABLE.has(type);
  return {
    id: serie.id,
    name: serie.label,
    type: echartsTypeOf(type),
    ...(SMOOTH.has(type) && {smooth: true}),
    ...(AREAS.has(type) && {areaStyle: {}}),
    ...(stacked && {stack: 'stack'}),
    ...(dataLabels && {label: {show: true}}),
    ...(type === 'donut' && {radius: ['50%', '75%']}),
    ...(type === 'semicircle' && {radius: ['50%', '75%'], startAngle: 180, endAngle: 360, center: ['50%', '75%']}),
    awe: {
      type: pie ? 'pie' : type,
      ...(RANGES.has(type) && {range: true}),
      ...(category && {xValue: category}),
      yValue: serie.field,
      ...(serie.z && {zValue: serie.z}),
      ...(stacking === 'percent' && stacked && {stackPercent: true}),
    },
  };
}

/** Axes of a chart that has them: both categories for a heatmap, both values for points, a category and a value otherwise */
function axesOf(description) {
  const {type, xLabel, yLabel} = description;
  if (type === 'heatmap') return {xAxis: [axisOf('x', 'category', xLabel)], yAxis: [axisOf('y', 'category', yLabel)]};
  if (POINTS.has(type)) return {xAxis: [axisOf('x', 'value', xLabel)], yAxis: [axisOf('y', 'value', yLabel)]};
  const inverted = type === 'bar';
  return {
    xAxis: [inverted ? axisOf('y', 'value', yLabel) : axisOf('x', 'category', xLabel)],
    yAxis: [inverted ? axisOf('x', 'category', xLabel, true) : axisOf('y', 'value', yLabel)],
  };
}

function buildEchartsModel(description) {
  const {type, series, stacking, legend, sharedTooltip, title, subtitle} = description;
  const pie = PIES.has(type);
  const axisFree = AXIS_FREE.has(type);
  const inverted = type === 'bar';
  const stackable = STACKABLE.has(type);
  // A pie, a gauge and a funnel draw one series
  const used = pie || type === 'gauge' || type === 'funnel' ? series.slice(0, 1) : series;
  return {
    ...((title || subtitle) && {title: {...(title && {text: title}), ...(subtitle && {subtext: subtitle}), left: 'center'}}),
    legend: {show: legend ?? pie, left: 'center', top: 'bottom'},
    tooltip: {show: true, trigger: sharedTooltip && !axisFree ? 'axis' : 'item'},
    ...(!axisFree && axesOf(description)),
    ...(type === 'heatmap' && {visualMap: {type: 'continuous', calculable: true, orient: 'horizontal', left: 'center', bottom: 0}}),
    ...(type === 'radar' && {radar: {}}),
    series: used.map(serie => serieOf(description, serie)),
    awe: {chartType: type, ...(inverted && {inverted: true}), ...(stacking && stackable && {stacking})},
  };
}

module.exports = {buildEchartsModel};
