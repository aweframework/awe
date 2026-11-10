const test = require('node:test');
const assert = require('node:assert/strict');
const {
  initialValues,
  resolveValues,
  buildAttributes,
  toXml,
  toClientAttributes,
  toComponents,
  escapeXml,
  optionValues,
} = require('../playgroundModel');

const spec = {
  tag: 'button',
  id: 'save',
  controls: [
    {id: 'label', type: 'text', xml: 'label', default: '', initial: 'Save'},
    {id: 'type', type: 'segmented', xml: 'button-type', client: 'buttonType', default: 'button', options: ['button', 'submit', 'reset']},
    {id: 'icon', type: 'icon', xml: 'icon', default: '', options: ['', 'save', 'search']},
    {id: 'look', type: 'select', xml: 'style', default: '', options: ['', 'btn-primary', 'btn-danger']},
    {id: 'iconOnly', type: 'switch', xml: 'style', token: 'no-label', default: false},
    {id: 'disabled', type: 'switch', client: 'disabled', default: false},
  ],
  children: [
    {
      when: 'disabled',
      tag: 'dependency',
      attrs: {'target-type': 'disable', initial: 'true'},
      children: [{tag: 'dependency-element', attrs: {id: 'Role', condition: 'eq', value: 'viewer'}}],
    },
  ],
};

test('initial values come from initial, then default', () => {
  assert.deepEqual(initialValues(spec), {
    label: 'Save', type: 'button', icon: '', look: '', iconOnly: false, disabled: false,
  });
});

test('resolveValues completes missing ids and drops unknown ones', () => {
  const values = resolveValues(spec, {type: 'submit', ghost: 1});

  assert.equal(values.type, 'submit');
  assert.equal(values.label, 'Save');
  assert.equal('ghost' in values, false);
});

test('resolveValues replaces a value outside the options or of the wrong kind by the initial one', () => {
  const values = resolveValues(spec, {type: 'nope', iconOnly: 'yes', label: 5});

  assert.equal(values.type, 'button');
  assert.equal(values.iconOnly, false);
  assert.equal(values.label, 'Save');
});

test('attributes omit values equal to their default and keep the order of the spec', () => {
  const values = resolveValues(spec, {type: 'submit', icon: 'save'});

  assert.deepEqual(buildAttributes(spec, values), [
    ['id', 'save'], ['label', 'Save'], ['button-type', 'submit'], ['icon', 'save'],
  ]);
});

test('the attribute order does not depend on the order the values were changed', () => {
  const a = toXml(spec, resolveValues(spec, {icon: 'save', type: 'reset'}));
  const b = toXml(spec, resolveValues(spec, {type: 'reset', icon: 'save'}));

  assert.equal(a, b);
});

test('controls that share an attribute join their tokens in spec order', () => {
  const values = resolveValues(spec, {look: 'btn-danger', iconOnly: true});

  assert.deepEqual(buildAttributes(spec, values).find(([name]) => name === 'style'), ['style', 'btn-danger no-label']);
});

test('a token control alone gives just its token', () => {
  const values = resolveValues(spec, {iconOnly: true});

  assert.deepEqual(buildAttributes(spec, values).find(([name]) => name === 'style'), ['style', 'no-label']);
});

test('a childless element is self-closing', () => {
  assert.equal(toXml(spec, resolveValues(spec, {})), '<button id="save" label="Save" />');
});

test('children are written with two-space indentation when their condition holds', () => {
  const xml = toXml(spec, resolveValues(spec, {disabled: true}));

  assert.equal(xml, [
    '<button id="save" label="Save">',
    '  <dependency target-type="disable" initial="true">',
    '    <dependency-element id="Role" condition="eq" value="viewer" />',
    '  </dependency>',
    '</button>',
  ].join('\n'));
});

test('attribute values are escaped', () => {
  assert.equal(escapeXml(`a & b < c > "d"`), 'a &amp; b &lt; c &gt; &quot;d&quot;');
  const xml = toXml(spec, resolveValues(spec, {label: 'Tom & "Jerry" <3'}));

  assert.equal(xml, '<button id="save" label="Tom &amp; &quot;Jerry&quot; &lt;3" />');
});

test('client attributes use the client names, skip empty values and keep booleans', () => {
  const values = resolveValues(spec, {type: 'submit', icon: 'save', look: 'btn-primary', disabled: true});

  assert.deepEqual(toClientAttributes(spec, values), {
    label: 'Save', buttonType: 'submit', icon: 'save', style: 'btn-primary', disabled: true,
  });
});

test('client attributes join the tokens of a shared attribute', () => {
  const values = resolveValues(spec, {look: 'btn-primary', iconOnly: true});

  assert.equal(toClientAttributes(spec, values).style, 'btn-primary no-label');
});

test('toComponents describes the component under its id for AweLive, with the defaults the client applies', () => {
  const components = toComponents(spec, resolveValues(spec, {}));

  assert.deepEqual(components, {save: {attributes: {label: 'Save', buttonType: 'button'}}});
});

test('a spec without controls still writes its id', () => {
  assert.equal(toXml({tag: 'tag', id: 'x', controls: []}, {}), '<tag id="x" />');
});

test('options can be {value, label} objects: the value is what is validated and written', () => {
  const objectSpec = {
    tag: 'button',
    id: 'b',
    controls: [{id: 'size', type: 'segmented', xml: 'size', default: '', options: [{value: '', label: 'Default'}, {value: 'lg', label: 'Large'}]}],
  };

  assert.equal(toXml(objectSpec, resolveValues(objectSpec, {size: 'lg'})), '<button id="b" size="lg" />');
  assert.equal(resolveValues(objectSpec, {size: 'Large'}).size, '');
  assert.deepEqual(optionValues(objectSpec.controls[0]), ['', 'lg']);
});

test('a child that names a hidden control is not written, whatever value that control kept', () => {
  const hidden = {
    tag: 'a',
    id: 'a',
    controls: [
      {id: 'mode', type: 'select', default: 'x', options: ['x', 'y']},
      {id: 'extra', type: 'switch', default: false, when: {id: 'mode', in: ['x']}},
    ],
    children: [{when: 'extra', tag: 'extra'}],
  };

  assert.match(toXml(hidden, {mode: 'x', extra: true}), /<extra \/>/);
  assert.doesNotMatch(toXml(hidden, {mode: 'y', extra: true}), /extra/);
});
