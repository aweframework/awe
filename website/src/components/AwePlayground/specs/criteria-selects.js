/**
 * Playground of the select criteria: select, select-multiple, suggest, suggest-multiple and picklist. Every attribute
 * is one the XSD declares for `criteria` and the React client reads.
 * The options come from a query or an enum on the server: the stage fills them with a static model, and the searches of
 * a suggest are answered from the same list (the `options` of the scene), so the XML names what a real screen would use.
 * A playground spec is described in playgroundModel.js.
 */
const shared = require('./criteriaShared');

const KINDS = [
  ['select', 'Select'], ['select-multiple', 'Select multiple'], ['suggest', 'Suggest'], ['suggest-multiple', 'Suggest multiple'],
  ['picklist', 'Picklist'],
];
const ALL = KINDS.map(([value]) => value);
const SEARCH = ['suggest', 'suggest-multiple'];
const FIELDS = ['select', 'select-multiple', ...SEARCH];
const VALIDATED = ALL.filter(kind => kind !== 'picklist');

const SELECTED = ['es', 'fr'];
const TEAM = [
  {value: 'ana', label: 'Ana Ruiz', description: 'Design', selected: true},
  {value: 'ben', label: 'Ben Okafor', description: 'Engineering'},
  {value: 'clara', label: 'Clara Weiss', description: 'Product', selected: true},
  {value: 'diego', label: 'Diego Vega', description: 'Support'},
  {value: 'eva', label: 'Eva Lind', description: 'Engineering'},
];

const MODELS = {
  select: {values: shared.COUNTRIES},
  'select-multiple': {values: shared.COUNTRIES.map(item => ({...item, selected: SELECTED.includes(item.value)}))},
  suggest: {values: []},
  'suggest-multiple': {values: []},
  picklist: {values: TEAM},
};

// Query or enum a real screen names to load the options
const SERVER_OPTIONS = {
  select: [['initial-load', 'enum'], ['target-action', 'CountryList']],
  'select-multiple': [['initial-load', 'enum'], ['target-action', 'CountryList']],
  suggest: [['initial-load', 'query'], ['target-action', 'CountryList']],
  'suggest-multiple': [['initial-load', 'query'], ['target-action', 'CountryList']],
  picklist: [['initial-load', 'enum'], ['target-action', 'TeamMembers']],
};

const criteriaSelects = {
  name: 'criteriaSelects',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.componentControl(KINDS, 'select'),
    shared.labelControl(ALL, 'Field'),
    shared.placeholderControl(FIELDS),
    shared.iconControl(FIELDS),
    shared.sizeControl(FIELDS),
    {id: 'optional', type: 'switch', label: 'Clear button', xml: 'optional', default: false, when: {id: 'component', in: ['select', 'select-multiple']}},
    {
      id: 'display', type: 'segmented', label: 'Selected values', xml: 'display', default: '', when: {id: 'component', in: ['select-multiple']},
      options: [{value: '', label: 'One line'}, {value: 'chip', label: 'Chips'}],
    },
    shared.requiredControl(VALIDATED),
    shared.readonlyControl(VALIDATED),
  ],
  xmlAttributes: values => SERVER_OPTIONS[values.component] ?? [],
  scene: values => ({
    node: {component: values.component},
    model: MODELS[values.component],
    ...shared.requiredScene(values, VALIDATED),
    attributes: {
      ...shared.requiredScene(values, VALIDATED).attributes,
      ...(SEARCH.includes(values.component) && {targetAction: 'CountryList'}),
    },
    ...(SEARCH.includes(values.component) && {options: {CountryList: shared.COUNTRIES}}),
  }),
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteriaSelects};
