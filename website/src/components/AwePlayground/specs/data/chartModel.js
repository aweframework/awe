/**
 * The `echartsModel` of a chart as the server sends it, built from a plain description. The server builds it from the
 * `chart`, `chart-serie`, `x-axis`, `y-axis`, `chart-legend` and `chart-tooltip` elements (EChartsModelBuilder in awe-model);
 * the playground has the same elements in its XML, so the stage gets the model those elements would produce. The model
 * carries no data: the client reads the points from the rows of the component, guided by the `xValue` and `yValue` hints.
 * Free of React so that node:test can run it.
 *
 * @param {Object} description
 * @param {string} description.type AWE chart type: line, spline, area, areaspline, column, bar, pie, donut or semicircle
 * @param {string} description.category Field of the rows that names each point (the month, the product line)
 * @param {Array<{id: string, label: string, field: string}>} description.series Series: the field of the rows each one reads
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
const SMOOTH = new Set(['spline', 'areaspline']);
const AREAS = new Set(['area', 'areaspline']);

function axisOf(role, type, label, inverse) {
  return {
    ...(label && {name: label, nameLocation: 'middle', nameGap: 30}),
    type,
    ...(inverse && {inverse: true}),
    awe: {axis: role},
  };
}

function serieOf(description, serie, pie) {
  const {type, category, stacking, dataLabels} = description;
  const stacked = stacking && !pie;
  const echartsType = pie ? 'pie' : (type === 'column' || type === 'bar' ? 'bar' : 'line');
  return {
    id: serie.id,
    name: serie.label,
    type: echartsType,
    ...(SMOOTH.has(type) && {smooth: true}),
    ...(AREAS.has(type) && {areaStyle: {}}),
    ...(stacked && {stack: 'stack'}),
    ...(dataLabels && {label: {show: true}}),
    ...(type === 'donut' && {radius: ['50%', '75%']}),
    ...(type === 'semicircle' && {radius: ['50%', '75%'], startAngle: 180, endAngle: 360, center: ['50%', '75%']}),
    awe: {
      type: pie ? 'pie' : type,
      xValue: category,
      yValue: serie.field,
      ...(stacking === 'percent' && !pie && {stackPercent: true}),
    },
  };
}

function buildEchartsModel(description) {
  const {type, series, stacking, legend, sharedTooltip, title, subtitle, xLabel, yLabel} = description;
  const pie = PIES.has(type);
  const inverted = type === 'bar';
  // A pie draws one series
  const used = pie ? series.slice(0, 1) : series;
  const model = {
    ...((title || subtitle) && {title: {...(title && {text: title}), ...(subtitle && {subtext: subtitle}), left: 'center'}}),
    legend: {show: legend ?? pie, left: 'center', top: 'bottom'},
    tooltip: {show: true, trigger: sharedTooltip && !pie ? 'axis' : 'item'},
    ...(!pie && {
      xAxis: [inverted ? axisOf('y', 'value', yLabel) : axisOf('x', 'category', xLabel)],
      yAxis: [inverted ? axisOf('x', 'category', xLabel, true) : axisOf('y', 'value', yLabel)],
    }),
    series: used.map(serie => serieOf(description, serie, pie)),
    awe: {chartType: type, ...(inverted && {inverted: true}), ...(stacking && !pie && {stacking})},
  };
  return model;
}

module.exports = {buildEchartsModel};
