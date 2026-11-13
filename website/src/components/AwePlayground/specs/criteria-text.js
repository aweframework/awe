/**
 * Playground of the text criteria: text, password, textarea, markdown-editor, text-view and color. Every attribute is
 * one the XSD declares for `criteria` and the React client reads. A playground spec is described in playgroundModel.js.
 */
const shared = require('./criteriaShared');

const KINDS = [
  ['text', 'Text'], ['password', 'Password'], ['textarea', 'Text area'], ['markdown-editor', 'Markdown editor'],
  ['text-view', 'Text view'], ['color', 'Color'],
];
const LABELED = ['text', 'password', 'textarea', 'text-view', 'color'];
const PLACEHOLDER = ['text', 'password', 'textarea', 'markdown-editor', 'text-view', 'color'];
const WITH_ICON = ['text', 'password', 'text-view', 'color'];
const SIZED = ['text', 'password', 'textarea', 'text-view', 'color'];
const EDITABLE = ['text', 'password', 'textarea', 'markdown-editor', 'color'];
const COLOR = '#4f46e5';

const criteriaText = {
  name: 'criteriaText',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.componentControl(KINDS, 'text'),
    shared.labelControl(LABELED, 'Field'),
    shared.placeholderControl(PLACEHOLDER),
    shared.iconControl(WITH_ICON),
    shared.sizeControl(SIZED),
    {id: 'rows', type: 'text', label: 'Rows', xml: 'area-rows', default: '', initial: '4', when: {id: 'component', in: ['textarea']}},
    {id: 'unit', type: 'text', label: 'Unit', xml: 'unit', default: '', initial: '', when: {id: 'component', in: ['text', 'text-view']}},
    {
      id: 'content', type: 'text', label: 'Text to show', xml: 'value', default: '', initial: 'Order shipped',
      when: {id: 'component', in: ['text-view']},
    },
    shared.requiredControl(EDITABLE),
    shared.readonlyControl(EDITABLE),
  ],
  xmlAttributes: values => (values.component === 'color' ? [['value', COLOR]] : []),
  scene: values => ({
    node: {component: values.component},
    model: values.component === 'color' ? {values: [{value: COLOR, label: COLOR, selected: true}]}
      : values.component === 'text-view' ? {values: [{value: values.content, label: values.content, selected: true}]}
        : undefined,
    ...shared.requiredScene(values, EDITABLE),
  }),
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteriaText};
