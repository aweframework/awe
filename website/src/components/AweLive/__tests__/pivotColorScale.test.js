const test = require('node:test');
const assert = require('node:assert/strict');
const {scaleStep, tokenColorScaleGenerator, withPivotColorScale} = require('../pivotColorScale');

test('the lowest value takes the first step and the highest the fifth', () => {
  assert.equal(scaleStep(0, 0, 100), 1);
  assert.equal(scaleStep(100, 0, 100), 5);
});

test('the values between are spread over the five steps in order', () => {
  const steps = [0, 19, 20, 39, 40, 59, 60, 79, 80, 99].map(value => scaleStep(value, 0, 100));
  assert.deepEqual(steps, [1, 1, 2, 2, 3, 3, 4, 4, 5, 5]);
});

test('a scale without range (one value, all equal) stays on the first step', () => {
  assert.equal(scaleStep(7, 7, 7), 1);
});

test('a value outside the range is held to the first or the fifth step', () => {
  assert.equal(scaleStep(-5, 0, 10), 1);
  assert.equal(scaleStep(50, 0, 10), 5);
});

test('the generator paints the cell with the scale token of its step and the text token of the same step', () => {
  const colorOf = tokenColorScaleGenerator([10, 20, 30]);
  assert.deepEqual(colorOf(10), {backgroundColor: 'var(--awe-color-scale-1)', color: 'var(--awe-color-on-scale-1)'});
  assert.deepEqual(colorOf(30), {backgroundColor: 'var(--awe-color-scale-5)', color: 'var(--awe-color-on-scale-5)'});
});

test('the generator only uses tokens, never a colour value', () => {
  const colorOf = tokenColorScaleGenerator([0, 100]);
  for (const value of [0, 25, 50, 75, 100]) {
    for (const css of Object.values(colorOf(value))) {
      assert.match(css, /^var\(--awe-color-(on-)?scale-[1-5]\)$/);
    }
  }
});

test('only the pivot tables of the nodes receive the generator, in a copy of the components', () => {
  const generator = () => ({});
  const components = {
    pivot: {attributes: {rows: 'Region'}},
    other: {attributes: {label: 'x'}},
  };
  const nodes = [{elementType: 'PivotTable', id: 'pivot'}, {elementType: 'Button', id: 'other'}];
  const result = withPivotColorScale(components, nodes, generator);
  assert.equal(result.pivot.attributes.tableColorScaleGenerator, generator);
  assert.equal(result.pivot.attributes.rows, 'Region');
  assert.equal(result.other, components.other);
  assert.equal(components.pivot.attributes.tableColorScaleGenerator, undefined);
});

test('without nodes, or for a node without description, nothing changes', () => {
  const components = {a: {attributes: {}}};
  assert.equal(withPivotColorScale(components, undefined, () => ({})), components);
  assert.deepEqual(withPivotColorScale(components, [{elementType: 'PivotTable', id: 'missing'}], () => ({})), components);
});

test('a pivot table nested in a container receives the generator too', () => {
  const generator = () => ({});
  const components = {inner: {attributes: {rows: 'Region'}}, tab: {attributes: {}}};
  const nodes = [{elementType: 'Tab', id: 'tab', elementList: [
    {elementType: 'TabContainer', id: 'one', elementList: [{elementType: 'PivotTable', id: 'inner'}]},
  ]}];
  const result = withPivotColorScale(components, nodes, generator);

  assert.equal(result.inner.attributes.tableColorScaleGenerator, generator);
  assert.equal(result.tab, components.tab);
});
