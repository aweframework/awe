const test = require('node:test');
const assert = require('node:assert/strict');
const {chart, chartExample} = require('../specs/chart');
const {initialValues, resolveValues, toXml} = require('../playgroundModel');

const TYPES = chart.controls.find(control => control.id === 'type').options.map(option => option.value);
const NEW_TYPES = ['scatter', 'bubble', 'arearange', 'areasplinerange', 'radar', 'heatmap', 'gauge', 'funnel'];
const valuesOf = type => resolveValues(chart, {...initialValues(chart), type});

test('the chart playground offers scatter, bubble, range, radar, heatmap, gauge and funnel', () => {
  assert.deepEqual(NEW_TYPES.filter(type => !TYPES.includes(type)), []);
});

test('every chart type reads fields the sample rows have, in the model and in the XML it writes', () => {
  for (const type of TYPES) {
    const {attributes, model} = chart.scene(valuesOf(type));
    const rows = model.values;
    const fields = new Set(Object.keys(rows[0]));
    assert.ok(rows.length > 0, type);
    for (const serie of attributes.echartsModel.series) {
      const {xValue, yValue, zValue} = serie.awe;
      assert.deepEqual([xValue, yValue, zValue].filter(field => field && !fields.has(field)), [], `${type}: ${serie.id}`);
    }
    const xml = toXml(chart, valuesOf(type));
    const written = Array.from(xml.matchAll(/ ([xyz]-value)="([^"]+)"/g), ([, attribute, field]) => [attribute, field]);
    for (const [attribute, field] of written) {
      assert.ok(fields.has(field), `${type}: ${attribute}`);
    }
    // The XML a user copies draws the same chart: it binds every field the model reads
    const bound = new Set(written.map(([, field]) => field));
    for (const serie of attributes.echartsModel.series) {
      const {xValue, yValue, zValue} = serie.awe;
      assert.deepEqual([xValue, yValue, zValue].filter(field => field && !bound.has(field)), [], `${type} XML: ${serie.id}`);
    }
  }
});

test('the controls that do not apply to a type are ignored: a funnel has no stacking, no axes and no shared tooltip', () => {
  const values = {...valuesOf('funnel'), stacking: 'percent', sharedTooltip: true, legend: true};
  const {attributes} = chart.scene(values);
  const xml = toXml(chart, values);

  assert.equal('stack' in attributes.echartsModel.series[0], false);
  assert.equal(attributes.echartsModel.tooltip.trigger, 'item');
  assert.equal(attributes.echartsModel.legend.show, false);
  assert.equal(/x-axis|chart-tooltip|chart-legend|stacking/.test(xml), false);
});

test('an example of a new type draws its own rows', () => {
  const heatmap = chartExample({type: 'heatmap'});

  assert.equal(heatmap.components.chart.model.values.length, 30);
  assert.equal(heatmap.components.chart.attributes.echartsModel.series[0].awe.zValue, 'orders');
});
