const test = require('node:test');
const assert = require('node:assert/strict');
const {buildEchartsModel} = require('../specs/data/chartModel');

const SERIES = [
  {id: 'software', label: 'Software', field: 'software'},
  {id: 'services', label: 'Services', field: 'services'},
];
const base = {type: 'line', category: 'month', series: SERIES};

test('a line chart has a category axis, a value axis and one line by series with the fields it reads', () => {
  const model = buildEchartsModel(base);

  assert.deepEqual(model.xAxis.map(axis => [axis.type, axis.awe.axis]), [['category', 'x']]);
  assert.deepEqual(model.yAxis.map(axis => [axis.type, axis.awe.axis]), [['value', 'y']]);
  assert.deepEqual(model.series.map(serie => [serie.id, serie.name, serie.type]), [['software', 'Software', 'line'], ['services', 'Services', 'line']]);
  assert.deepEqual(model.series[0].awe, {type: 'line', xValue: 'month', yValue: 'software'});
  assert.equal(model.awe.chartType, 'line');
});

test('spline smooths the line, area fills it and areaspline does both', () => {
  const kinds = type => buildEchartsModel({...base, type}).series[0];

  assert.equal(kinds('spline').smooth, true);
  assert.equal('areaStyle' in kinds('spline'), false);
  assert.deepEqual(kinds('area').areaStyle, {});
  assert.equal('smooth' in kinds('area'), false);
  assert.ok(kinds('areaspline').smooth && kinds('areaspline').areaStyle);
});

test('column draws bars and bar draws them horizontally: the axes swap and the category goes top to bottom', () => {
  const column = buildEchartsModel({...base, type: 'column'});
  const bar = buildEchartsModel({...base, type: 'bar'});

  assert.equal(column.series[0].type, 'bar');
  assert.equal(column.awe.inverted, undefined);
  assert.equal(bar.series[0].type, 'bar');
  assert.equal(bar.awe.inverted, true);
  assert.equal(bar.xAxis[0].type, 'value');
  assert.equal(bar.yAxis[0].type, 'category');
  assert.equal(bar.yAxis[0].inverse, true);
});

test('stacking puts the stackable series in one stack and tells the client how', () => {
  const normal = buildEchartsModel({...base, type: 'column', stacking: 'normal'});
  const percent = buildEchartsModel({...base, type: 'column', stacking: 'percent'});

  assert.deepEqual(normal.series.map(serie => serie.stack), ['stack', 'stack']);
  assert.equal(normal.awe.stacking, 'normal');
  assert.equal(percent.awe.stacking, 'percent');
  assert.ok(percent.series.every(serie => serie.awe.stackPercent === true));
  assert.equal('stack' in buildEchartsModel(base).series[0], false);
});

test('a pie has no axes, shows one series and its legend, and a donut and a semicircle are rings', () => {
  const pie = buildEchartsModel({type: 'pie', category: 'line', series: SERIES});
  const donut = buildEchartsModel({type: 'donut', category: 'line', series: SERIES});
  const half = buildEchartsModel({type: 'semicircle', category: 'line', series: SERIES});

  assert.equal('xAxis' in pie, false);
  assert.equal(pie.series.length, 1);
  assert.equal(pie.series[0].type, 'pie');
  assert.deepEqual(pie.series[0].awe, {type: 'pie', xValue: 'line', yValue: 'software'});
  assert.equal(pie.legend.show, true);
  assert.equal(pie.series[0].radius, undefined);
  assert.deepEqual(donut.series[0].radius, ['50%', '75%']);
  assert.deepEqual([half.series[0].startAngle, half.series[0].endAngle, half.series[0].center], [180, 360, ['50%', '75%']]);
});

test('the legend is hidden on a chart with axes unless it is asked for', () => {
  assert.equal(buildEchartsModel(base).legend.show, false);
  assert.equal(buildEchartsModel({...base, legend: true}).legend.show, true);
  assert.equal(buildEchartsModel({...base, legend: false, type: 'pie'}).legend.show, false);
});

test('data labels show on every series', () => {
  assert.ok(buildEchartsModel({...base, dataLabels: true}).series.every(serie => serie.label.show === true));
  assert.equal('label' in buildEchartsModel(base).series[0], false);
});

test('the title and the subtitle are centred and only there when given', () => {
  assert.deepEqual(buildEchartsModel({...base, title: 'Revenue', subtitle: 'kEUR'}).title, {text: 'Revenue', subtext: 'kEUR', left: 'center'});
  assert.equal('title' in buildEchartsModel(base), false);
});

test('a shared tooltip follows the axis, any other shows the point under the mouse', () => {
  assert.equal(buildEchartsModel({...base, sharedTooltip: true}).tooltip.trigger, 'axis');
  assert.equal(buildEchartsModel(base).tooltip.trigger, 'item');
  assert.equal(buildEchartsModel({type: 'pie', series: SERIES, sharedTooltip: true}).tooltip.trigger, 'item');
});

test('the axis labels are named when given and the model does not share the series of the description', () => {
  const model = buildEchartsModel({...base, xLabel: 'Month', yLabel: 'Revenue (kEUR)'});
  model.series[0].name = 'changed';

  assert.equal(model.xAxis[0].name, 'Month');
  assert.equal(model.yAxis[0].name, 'Revenue (kEUR)');
  assert.equal(SERIES[0].label, 'Software');
});

test('a pie ignores stacking and draws only the first series', () => {
  const pie = buildEchartsModel({type: 'pie', category: 'line', series: SERIES, stacking: 'percent'});

  assert.equal(pie.series.length, 1);
  assert.equal('stack' in pie.series[0], false);
  assert.equal('stacking' in pie.awe, false);
});

const POINTS = [{id: 'deals', label: 'Deals', field: 'margin', z: 'seats'}];
const byPoint = {category: 'size', series: POINTS};

test('scatter and bubble draw points on two value axes, and the bubble reads its size from the z value', () => {
  const scatter = buildEchartsModel({...byPoint, type: 'scatter', xLabel: 'Deal size', yLabel: 'Margin'});
  const bubble = buildEchartsModel({...byPoint, type: 'bubble'});

  assert.deepEqual(scatter.xAxis.map(axis => [axis.type, axis.awe.axis, axis.name]), [['value', 'x', 'Deal size']]);
  assert.deepEqual(scatter.yAxis.map(axis => [axis.type, axis.awe.axis, axis.name]), [['value', 'y', 'Margin']]);
  assert.equal(scatter.series[0].type, 'scatter');
  assert.deepEqual(scatter.series[0].awe, {type: 'scatter', xValue: 'size', yValue: 'margin', zValue: 'seats'});
  assert.equal(bubble.series[0].type, 'scatter');
  assert.equal(bubble.series[0].awe.type, 'bubble');
  assert.equal(bubble.awe.chartType, 'bubble');
  assert.equal('smooth' in scatter.series[0], false);
  assert.equal('areaStyle' in scatter.series[0], false);
});

test('a range is a filled line with the range hint: low in the y value and high in the z value, on a category axis', () => {
  const range = buildEchartsModel({type: 'arearange', category: 'month', series: [{id: 'f', label: 'Forecast', field: 'low', z: 'high'}]});
  const smooth = buildEchartsModel({type: 'areasplinerange', category: 'month', series: [{id: 'f', label: 'Forecast', field: 'low', z: 'high'}]});

  assert.equal(range.xAxis[0].type, 'category');
  assert.equal(range.series[0].type, 'line');
  assert.deepEqual(range.series[0].areaStyle, {});
  assert.deepEqual(range.series[0].awe, {type: 'arearange', range: true, xValue: 'month', yValue: 'low', zValue: 'high'});
  assert.equal(smooth.series[0].smooth, true);
  assert.equal(smooth.series[0].awe.range, true);
});

test('radar, gauge and funnel have no axes, show the item tooltip and hide the legend; only the radar has the radar component', () => {
  for (const type of ['radar', 'gauge', 'funnel']) {
    const model = buildEchartsModel({type, category: 'step', series: SERIES, sharedTooltip: true, stacking: 'normal'});

    assert.equal('xAxis' in model, false, type);
    assert.equal('yAxis' in model, false, type);
    assert.equal(model.tooltip.trigger, 'item', type);
    assert.equal(model.legend.show, false, type);
    assert.equal(model.series[0].type, type);
    assert.equal(model.series[0].awe.type, type);
    assert.equal('stack' in model.series[0], false, type);
    assert.equal('stacking' in model.awe, false, type);
    assert.equal('radar' in model, type === 'radar', type);
  }
});

test('a radar draws one polygon for each series and keeps the legend an option, a gauge and a funnel read one series', () => {
  assert.equal(buildEchartsModel({type: 'radar', category: 'measure', series: SERIES}).series.length, 2);
  assert.equal(buildEchartsModel({type: 'radar', category: 'measure', series: SERIES, legend: true}).legend.show, true);
  assert.equal(buildEchartsModel({type: 'gauge', category: 'measure', series: SERIES}).series.length, 1);
  assert.equal(buildEchartsModel({type: 'funnel', category: 'step', series: SERIES}).series.length, 1);
});

test('a heatmap has category axes, a visual map for the client to fill, the z value and no stack', () => {
  const model = buildEchartsModel({type: 'heatmap', category: 'hour', series: [{id: 'orders', label: 'Orders', field: 'day', z: 'orders'}], stacking: 'normal'});

  assert.deepEqual(model.xAxis.map(axis => [axis.type, axis.awe.axis]), [['category', 'x']]);
  assert.deepEqual(model.yAxis.map(axis => [axis.type, axis.awe.axis]), [['category', 'y']]);
  assert.deepEqual(model.visualMap, {type: 'continuous', calculable: true, orient: 'horizontal', left: 'center', bottom: 0});
  assert.deepEqual(model.series[0].awe, {type: 'heatmap', xValue: 'hour', yValue: 'day', zValue: 'orders'});
  assert.equal('stack' in model.series[0], false);
  assert.equal('stacking' in model.awe, false);
  assert.equal('visualMap' in buildEchartsModel(base), false);
});

test('types that cannot be stacked ignore the stacking', () => {
  for (const type of ['scatter', 'bubble', 'arearange', 'areasplinerange']) {
    const model = buildEchartsModel({...byPoint, type, stacking: 'percent'});

    assert.equal('stack' in model.series[0], false, type);
    assert.equal('stackPercent' in model.series[0].awe, false, type);
    assert.equal('stacking' in model.awe, false, type);
  }
});
