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
