const test = require('node:test');
const assert = require('node:assert/strict');
const {
  resolveValues,
  toXml,
  toClientAttributes,
  toScene,
  visibleControls,
} = require('../playgroundModel');

// A playground whose first control picks the kind of component, like the criteria one
const kinds = {
  tag: 'criteria',
  element: 'Criteria',
  id: 'demo',
  controls: [
    {id: 'component', type: 'select', xml: 'component', client: '', default: '', initial: 'text', options: ['text', 'numeric', 'textarea']},
    {id: 'label', type: 'text', xml: 'label', default: '', initial: 'Name'},
    {id: 'rows', type: 'text', xml: 'area-rows', default: '', initial: '4', when: {id: 'component', in: ['textarea']}},
    {id: 'slider', type: 'switch', xml: 'show-slider', default: false, when: {id: 'component', in: ['numeric']}},
  ],
  xmlAttributes: values => (values.component === 'numeric' ? [['number-format', '0,0']] : []),
  scene: values => ({
    node: {component: values.component},
    model: {values: [{value: 'a', label: 'A', selected: true}]},
    validationRules: {required: values.label === 'Name'},
    components: {other: {attributes: {label: 'Other'}}},
    nodes: [{elementType: 'Tag', type: 'p', label: 'after'}],
  }),
};

test('a control with a condition is only used while its condition holds', () => {
  const text = resolveValues(kinds, {});
  const area = resolveValues(kinds, {component: 'textarea'});

  assert.deepEqual(visibleControls(kinds, text).map(control => control.id), ['component', 'label']);
  assert.deepEqual(visibleControls(kinds, area).map(control => control.id), ['component', 'label', 'rows']);
  assert.equal(toXml(kinds, text), '<criteria id="demo" component="text" label="Name" />');
  assert.equal(toXml(kinds, area), '<criteria id="demo" component="textarea" label="Name" area-rows="4" />');
});

test('the value of a control that does not apply is kept but neither written nor sent to the client', () => {
  const values = resolveValues(kinds, {component: 'text', rows: '9', slider: true});

  assert.equal(values.rows, '9');
  assert.equal(toXml(kinds, values).includes('area-rows'), false);
  assert.deepEqual(toClientAttributes(kinds, values), {label: 'Name'});
});

test('xmlAttributes adds fixed attributes after the ones of the controls', () => {
  const xml = toXml(kinds, resolveValues(kinds, {component: 'numeric', label: 'N', slider: true}));

  assert.equal(xml, [
    '<criteria id="demo"',
    '          component="numeric"',
    '          label="N"',
    '          show-slider="true"',
    '          number-format="0,0" />',
  ].join('\n'));
});

test('toScene gives the root description, its extra components and the nodes to render', () => {
  const {components, nodes} = toScene(kinds, resolveValues(kinds, {component: 'numeric'}));

  assert.deepEqual(components.demo, {
    attributes: {label: 'Name'},
    model: {values: [{value: 'a', label: 'A', selected: true}]},
    validationRules: {required: true},
  });
  assert.deepEqual(components.other, {attributes: {label: 'Other'}});
  assert.deepEqual(nodes, [
    {elementType: 'Criteria', id: 'demo', component: 'numeric'},
    {elementType: 'Tag', type: 'p', label: 'after'},
  ]);
});

test('toScene of a spec without scene is just the attributes of its component', () => {
  const plain = {tag: 'card', element: 'Card', id: 'c', controls: [{id: 'title', type: 'text', xml: 'title', default: '', initial: 'T'}]};
  const {components, nodes} = toScene(plain, resolveValues(plain, {}));

  assert.deepEqual(components, {c: {attributes: {title: 'T'}}});
  assert.deepEqual(nodes, [{elementType: 'Card', id: 'c'}]);
});

test('a long element writes one attribute per line, aligned under the first one', () => {
  const long = {
    tag: 'criteria',
    id: 'customer',
    controls: [
      {id: 'component', type: 'text', xml: 'component', default: '', initial: 'select'},
      {id: 'label', type: 'text', xml: 'label', default: '', initial: 'Customer of the month'},
      {id: 'placeholder', type: 'text', xml: 'placeholder', default: '', initial: 'Choose one customer from the list'},
    ],
  };

  assert.equal(toXml(long, resolveValues(long, {})), [
    '<criteria id="customer"',
    '          component="select"',
    '          label="Customer of the month"',
    '          placeholder="Choose one customer from the list" />',
  ].join('\n'));
});

test('a long element with children closes its open tag on the last attribute and indents inside', () => {
  const long = {
    tag: 'card',
    id: 'revenue',
    controls: [
      {id: 'title', type: 'text', xml: 'title', default: '', initial: 'Monthly recurring revenue'},
      {id: 'subtitle', type: 'text', xml: 'subtitle', default: '', initial: 'All the plans of the year'},
      {id: 'value', type: 'text', xml: 'value', default: '', initial: '84200'},
    ],
    children: [{tag: 'tag', attrs: {type: 'div'}}],
  };

  assert.equal(toXml(long, resolveValues(long, {})), [
    '<card id="revenue"',
    '      title="Monthly recurring revenue"',
    '      subtitle="All the plans of the year"',
    '      value="84200">',
    '  <tag type="div" />',
    '</card>',
  ].join('\n'));
});

test('a nested long element wraps relative to its own indentation', () => {
  const nested = {
    tag: 'dialog',
    id: 'd',
    controls: [],
    children: [{tag: 'button', attrs: {id: 'closeDialogButton', label: 'Close the dialog window', icon: 'x', style: 'btn-primary'}}],
  };

  assert.equal(toXml(nested, {}), [
    '<dialog id="d">',
    '  <button id="closeDialogButton"',
    '          label="Close the dialog window"',
    '          icon="x"',
    '          style="btn-primary" />',
    '</dialog>',
  ].join('\n'));
});

test('a child can depend on the value of a control, like a control does', () => {
  const tabs = {
    tag: 'tab',
    id: 't',
    controls: [{id: 'count', type: 'segmented', default: '', initial: '2', options: ['2', '3']}],
    children: [
      {tag: 'tabcontainer', attrs: {id: 'a'}},
      {tag: 'tabcontainer', attrs: {id: 'b'}},
      {when: {id: 'count', in: ['3']}, tag: 'tabcontainer', attrs: {id: 'c'}},
    ],
  };

  assert.equal(toXml(tabs, resolveValues(tabs, {})), '<tab id="t">\n  <tabcontainer id="a" />\n  <tabcontainer id="b" />\n</tab>');
  assert.equal(toXml(tabs, resolveValues(tabs, {count: '3'})).includes('<tabcontainer id="c" />'), true);
});

test('siblings are written after the element, at the same level', () => {
  const dialog = {
    tag: 'dialog',
    id: 'd',
    controls: [],
    siblings: [{tag: 'button', attrs: {id: 'open'}, children: [{tag: 'button-action', attrs: {type: 'dialog', target: 'd'}}]}],
  };

  assert.equal(toXml(dialog, {}), [
    '<dialog id="d" />',
    '<button id="open">',
    '  <button-action type="dialog" target="d" />',
    '</button>',
  ].join('\n'));
});

test('toScene passes the specific attributes of the root, the simulated files and the settings of the scene', () => {
  const spec = {
    tag: 'grid', element: 'Grid', id: 'demo', controls: [],
    scene: () => ({
      specificAttributes: {sort: [{id: 'name', direction: 'asc'}]},
      files: {Log: '/log.txt'},
      settings: {mapTileUrl: 'x'},
    }),
  };
  const scene = toScene(spec, {});

  assert.deepEqual(scene.components.demo.specificAttributes, {sort: [{id: 'name', direction: 'asc'}]});
  assert.deepEqual(scene.files, {Log: '/log.txt'});
  assert.deepEqual(scene.settings, {mapTileUrl: 'x'});
});

test('toScene leaves the specific attributes, files and settings out when the scene has none', () => {
  const scene = toScene({tag: 'a', element: 'A', id: 'demo', controls: []}, {});

  assert.equal('specificAttributes' in scene.components.demo, false);
  assert.equal('files' in scene, false);
  assert.equal('settings' in scene, false);
});

test('the attributes of a child can be a function of the values', () => {
  const spec = {
    tag: 'chart', id: 'demo',
    controls: [{id: 'kind', type: 'select', default: 'line', options: ['line', 'bar']}],
    children: [{tag: 'chart-serie', attrs: values => ({id: 's', type: values.kind})}],
  };

  assert.equal(toXml(spec, {kind: 'bar'}), ['<chart id="demo">', '  <chart-serie id="s" type="bar" />', '</chart>'].join('\n'));
});

test('a scene hands the options of the simulated searches and the upload simulation to the stage', () => {
  const spec = {
    name: 'demo', tag: 'criteria', id: 'demo', element: 'Criteria', controls: [],
    scene: () => ({options: {CountryList: [{value: 'es', label: 'Spain'}]}, uploads: true}),
  };
  const scene = toScene(spec, {});

  assert.deepEqual(scene.options, {CountryList: [{value: 'es', label: 'Spain'}]});
  assert.equal(scene.uploads, true);
  assert.equal('options' in toScene({...spec, scene: () => ({})}, {}), false);
});

test('toScene passes the predefined messages and the answers of the server a scene describes, and only then', () => {
  const messages = {Remove: {title: 'Remove?', message: 'Sure?'}};
  const answers = {Save: [{type: 'message', parameters: {type: 'ok', title: 'Saved', message: 'Done'}}]};
  const spec = {...kinds, scene: () => ({messages, answers})};
  const scene = toScene(spec, resolveValues(spec, {}));

  assert.deepEqual(scene.messages, messages);
  assert.deepEqual(scene.answers, answers);
  const plain = toScene(kinds, resolveValues(kinds, {}));
  assert.equal('messages' in plain, false);
  assert.equal('answers' in plain, false);
});

test('toScene gives the root component the actions a scene describes (a button that opens a menu)', () => {
  const actions = [{type: 'show-context-menu'}];
  const spec = {...kinds, scene: () => ({actions})};

  assert.deepEqual(toScene(spec, resolveValues(spec, {})).components.demo.actions, actions);
  assert.equal('actions' in toScene(kinds, resolveValues(kinds, {})).components.demo, false);
});
