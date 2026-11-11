const test = require('node:test');
const assert = require('node:assert/strict');
const {resolveValues, toScene} = require('../playgroundModel');
const {criteriaText, criteriaNumbers, criteriaSelects, criteriaCheckboxRadio, criteriaUpload} = require('../specs');

const sceneOf = (spec, values) => toScene(spec, resolveValues(spec, values));

test('the number format of a numeric criterion reaches the client parsed: it reads an object, not the text of the XML', () => {
  const {attributes} = sceneOf(criteriaNumbers, {component: 'numeric', slider: true}).components.demo;

  assert.deepEqual(attributes.numberFormat, {min: 0, max: 100, step: 5});
});

test('a suggest searches the options of its target action, the same list a select shows', () => {
  const scene = sceneOf(criteriaSelects, {component: 'suggest'});

  assert.equal(scene.components.demo.attributes.targetAction, 'CountryList');
  assert.ok(scene.options.CountryList.length > 1);
  assert.equal('options' in sceneOf(criteriaSelects, {component: 'select'}), false);
});

test('the radio playground is a group: three radios of the same group, one of them chosen', () => {
  const scene = sceneOf(criteriaCheckboxRadio, {component: 'radio'});
  const radios = scene.nodes.map(node => scene.components[node.id]);

  assert.equal(radios.length, 3);
  assert.ok(radios.every(radio => radio.attributes.group === 'plan'));
  assert.deepEqual(radios.map(radio => Boolean(radio.model.values[0].selected)), [true, false, false]);
  // The client treats radios as a group only when the attributes say `component: 'radio'` as well as the group
  assert.ok(radios.every(radio => radio.attributes.component === 'radio'));
});

test('the uploader playground simulates its upload and starts without a file', () => {
  const scene = sceneOf(criteriaUpload, {});

  assert.equal(scene.uploads, true);
  assert.equal(scene.components.demo.model, undefined);
});

test('a filtered calendar allows only Mondays and Thursdays of the coming weeks', () => {
  const values = sceneOf(criteriaNumbers, {component: 'filtered-calendar'}).components.demo.model.values;

  assert.ok(values.length >= 8);
  for (const {value} of values) {
    const [day, month, year] = value.split('/').map(Number);
    assert.ok([1, 4].includes(new Date(year, month - 1, day).getDay()), value);
  }
});

test('the required switch sends the rule with the criterion, but only for the kinds that validate', () => {
  const required = sceneOf(criteriaText, {component: 'text', required: true}).components.demo;

  assert.equal(required.attributes.required, true);
  assert.deepEqual(required.validationRules, {required: true});
  assert.equal(sceneOf(criteriaText, {component: 'text'}).components.demo.validationRules, undefined);
  // A hidden control keeps its value: a read-only kind does not get the rule of a switch left on
  assert.equal(sceneOf(criteriaText, {component: 'text-view', required: true}).components.demo.validationRules, undefined);
});

test('the text playground shows its text-view content and the color its chosen value', () => {
  const view = sceneOf(criteriaText, {component: 'text-view', content: 'Shipped'}).components.demo.model.values[0];
  const color = sceneOf(criteriaText, {component: 'color'}).components.demo.model.values[0];

  assert.equal(view.value, 'Shipped');
  assert.equal(color.selected, true);
  assert.equal(sceneOf(criteriaText, {component: 'text'}).components.demo.model, undefined);
});

test('the switch is a checkbox drawn with the switch style, and the playground offers it only for the checkbox', () => {
  const scene = sceneOf(criteriaCheckboxRadio, {component: 'checkbox', asSwitch: true}).components.demo;

  assert.equal(scene.attributes.style, 'switch');
  assert.equal(sceneOf(criteriaCheckboxRadio, {component: 'checkbox'}).components.demo.attributes.style, undefined);
});
