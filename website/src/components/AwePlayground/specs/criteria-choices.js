/**
 * Playground of the choice criteria: select, select-multiple, suggest, suggest-multiple, picklist, radio, button-radio,
 * checkbox and button-checkbox. Every attribute is one the XSD declares for `criteria` and the React client reads.
 * The options come from a query or an enum on the server: the stage fills them with a static model, and the searches of
 * a suggest are answered from the same list (the `options` of the scene), so the XML names what a real screen would use.
 * A playground spec is described in playgroundModel.js.
 */
const shared = require('./criteriaShared');

const KINDS = [
  ['select', 'Select'], ['select-multiple', 'Select multiple'], ['suggest', 'Suggest'], ['suggest-multiple', 'Suggest multiple'],
  ['picklist', 'Picklist'], ['radio', 'Radio'], ['button-radio', 'Button radio'], ['checkbox', 'Checkbox'],
  ['button-checkbox', 'Button checkbox'],
];
const ALL = KINDS.map(([value]) => value);
const SEARCH = ['suggest', 'suggest-multiple'];
const FIELDS = ['select', 'select-multiple', ...SEARCH];
const VALIDATED = ALL.filter(kind => !['picklist', 'radio'].includes(kind));

const SELECTED = ['es', 'fr'];
const PLANS = [
  {value: 'day', label: 'Day', selected: true},
  {value: 'week', label: 'Week'},
  {value: 'month', label: 'Month'},
];
const DAYS = [
  {value: 'mon', label: 'Mon', selected: true},
  {value: 'tue', label: 'Tue'},
  {value: 'wed', label: 'Wed'},
  {value: 'thu', label: 'Thu'},
];
const TEAM = [
  {value: 'ana', label: 'Ana Ruiz', description: 'Design', selected: true},
  {value: 'ben', label: 'Ben Okafor', description: 'Engineering'},
  {value: 'clara', label: 'Clara Weiss', description: 'Product', selected: true},
  {value: 'diego', label: 'Diego Vega', description: 'Support'},
  {value: 'eva', label: 'Eva Lind', description: 'Engineering'},
];
// The two other radios of the group of the radio playground
const OTHER_RADIOS = [['demo2', 'business', 'Business'], ['demo3', 'enterprise', 'Enterprise']];

const MODELS = {
  select: {values: shared.COUNTRIES},
  'select-multiple': {values: shared.COUNTRIES.map(item => ({...item, selected: SELECTED.includes(item.value)}))},
  suggest: {values: []},
  'suggest-multiple': {values: []},
  picklist: {values: TEAM},
  radio: {values: [{value: 'basic', selected: true}]},
  'button-radio': {values: PLANS},
  checkbox: {values: [{value: '1', label: 'Yes', selected: false}]},
  'button-checkbox': {values: DAYS},
};

// Query or enum a real screen names to load the options
const SERVER_OPTIONS = {
  select: [['initial-load', 'enum'], ['target-action', 'CountryList']],
  'select-multiple': [['initial-load', 'enum'], ['target-action', 'CountryList']],
  suggest: [['initial-load', 'query'], ['target-action', 'CountryList']],
  'suggest-multiple': [['initial-load', 'query'], ['target-action', 'CountryList']],
  picklist: [['initial-load', 'enum'], ['target-action', 'TeamMembers']],
  radio: [['group', 'plan'], ['value', 'basic'], ['checked', 'true'], ['style', 'no-label']],
  'button-radio': [['initial-load', 'enum'], ['target-action', 'Plans']],
  'button-checkbox': [['initial-load', 'enum'], ['target-action', 'WeekDays']],
};

const criteriaChoices = {
  name: 'criteriaChoices',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.componentControl(KINDS, 'select'),
    shared.labelControl(ALL.filter(kind => kind !== 'button-checkbox'), 'Field'),
    shared.placeholderControl(FIELDS),
    shared.iconControl(FIELDS),
    shared.sizeControl(ALL.filter(kind => kind !== 'picklist')),
    {id: 'optional', type: 'switch', label: 'Clear button', xml: 'optional', default: false, when: {id: 'component', in: ['select', 'select-multiple']}},
    {
      id: 'display', type: 'segmented', label: 'Selected values', xml: 'display', default: '', when: {id: 'component', in: ['select-multiple']},
      options: [{value: '', label: 'One line'}, {value: 'chip', label: 'Chips'}],
    },
    {
      id: 'asSwitch', type: 'switch', label: 'Show as a switch', xml: 'style', token: 'switch', client: 'style', default: false,
      when: {id: 'component', in: ['checkbox']},
    },
    shared.requiredControl(VALIDATED),
    shared.readonlyControl(VALIDATED),
  ],
  xmlAttributes: values => SERVER_OPTIONS[values.component] ?? [],
  siblings: OTHER_RADIOS.map(([id, value, label]) => ({
    when: {id: 'component', in: ['radio']},
    tag: 'criteria',
    attrs: {id, component: 'radio', label, group: 'plan', value, style: 'no-label'},
  })),
  scene: values => {
    const radio = values.component === 'radio';
    return {
      node: {component: values.component},
      model: MODELS[values.component],
      ...shared.requiredScene(values, VALIDATED),
      attributes: {
        ...shared.requiredScene(values, VALIDATED).attributes,
        ...(SEARCH.includes(values.component) && {targetAction: 'CountryList'}),
        ...(radio && {group: 'plan', style: 'no-label'}),
      },
      ...(SEARCH.includes(values.component) && {options: {CountryList: shared.COUNTRIES}}),
      ...(radio && {
        components: Object.fromEntries(OTHER_RADIOS.map(([id, value, label]) => [id, {
          attributes: {label, group: 'plan', style: 'no-label'}, model: {values: [{value}]},
        }])),
        nodes: OTHER_RADIOS.map(([id]) => ({elementType: 'Criteria', component: 'radio', id})),
      }),
    };
  },
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteriaChoices};
