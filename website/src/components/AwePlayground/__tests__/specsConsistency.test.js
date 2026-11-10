const test = require('node:test');
const assert = require('node:assert/strict');
const {resolveValues, toScene, toXml} = require('../playgroundModel');
const {chart, grid} = require('../specs');

const echartsOf = values => toScene(chart, resolveValues(chart, values)).components.demo.attributes.echartsModel;

test('the chart XML and stage ignore what the type of chart does not offer, even when a control kept its value', () => {
  const pie = {type: 'pie', legend: true, sharedTooltip: true, stacking: 'normal'};
  const xml = toXml(chart, resolveValues(chart, pie));

  assert.doesNotMatch(xml, /chart-legend|chart-tooltip|stacking/);
  assert.equal(echartsOf(pie).tooltip.trigger, 'item');

  const line = echartsOf({type: 'line', stacking: 'normal'});
  assert.equal('stack' in line.series[0], false);
  assert.equal('stacking' in line.awe, false);
});

test('a stackable chart keeps its stacking and a cartesian one its legend and shared tooltip', () => {
  const column = {type: 'column', stacking: 'normal', legend: true, sharedTooltip: true};
  const xml = toXml(chart, resolveValues(chart, column));

  assert.match(xml, /chart-legend/);
  assert.match(xml, /chart-tooltip/);
  assert.equal(echartsOf(column).series[0].stack, 'stack');
});

const columnOf = (values, name) => toScene(grid, resolveValues(grid, values)).components.demo.attributes.columnModel
  .find(column => column.name === name);

test('an editable grid gives its text columns an editor and drops the read-only extras in the XML and on the stage', () => {
  const editable = resolveValues(grid, {editable: true});
  const xml = toXml(grid, editable);

  assert.doesNotMatch(xml, /avatar-field|status-field|group-by/);
  assert.match(xml, /name="name"[^>]*component="text"/s);
  assert.equal(columnOf({editable: true}, 'name').component, 'text');
  assert.equal(columnOf({editable: false}, 'name').component, undefined);
});

test('group-by is written only for a grid that is not editable', () => {
  assert.match(toXml(grid, resolveValues(grid, {groupBy: 'plan'})), /group-by="plan"/);
  assert.doesNotMatch(toXml(grid, resolveValues(grid, {groupBy: 'plan', editable: true})), /group-by/);
});
