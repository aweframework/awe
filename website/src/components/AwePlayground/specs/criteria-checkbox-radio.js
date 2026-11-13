/**
 * Playground of the checkbox and radio criteria: radio, button-radio, checkbox (also drawn as a switch) and
 * button-checkbox. Every attribute is one the XSD declares for `criteria` and the React client reads. The options of
 * the button kinds come from an enum on the server: the stage fills them with a static model. A playground spec is
 * described in playgroundModel.js.
 */
const shared = require('./criteriaShared');

const KINDS = [
  ['radio', 'Radio'], ['button-radio', 'Button radio'], ['checkbox', 'Checkbox'], ['button-checkbox', 'Button checkbox'],
];
const ALL = KINDS.map(([value]) => value);
const VALIDATED = ALL.filter(kind => kind !== 'radio');

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
// The two other radios of the group of the radio playground
const OTHER_RADIOS = [['demo2', 'business', 'Business'], ['demo3', 'enterprise', 'Enterprise']];

const MODELS = {
  radio: {values: [{value: 'basic', selected: true}]},
  'button-radio': {values: PLANS},
  checkbox: {values: [{value: '1', label: 'Yes', selected: false}]},
  'button-checkbox': {values: DAYS},
};

// Enum a real screen names to load the options, or the group of a radio
const SERVER_OPTIONS = {
  radio: [['group', 'plan'], ['value', 'basic'], ['checked', 'true'], ['style', 'no-label']],
  'button-radio': [['initial-load', 'enum'], ['target-action', 'Plans']],
  'button-checkbox': [['initial-load', 'enum'], ['target-action', 'WeekDays']],
};

const criteriaCheckboxRadio = {
  name: 'criteriaCheckboxRadio',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.componentControl(KINDS, 'radio'),
    shared.labelControl(ALL.filter(kind => kind !== 'button-checkbox'), 'Field'),
    shared.sizeControl(ALL),
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
        ...(radio && {component: 'radio', group: 'plan', style: 'no-label'}),
      },
      ...(radio && {
        components: Object.fromEntries(OTHER_RADIOS.map(([id, value, label]) => [id, {
          attributes: {component: 'radio', label, group: 'plan', style: 'no-label'}, model: {values: [{value}]},
        }])),
        nodes: OTHER_RADIOS.map(([id]) => ({elementType: 'Criteria', component: 'radio', id})),
      }),
    };
  },
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteriaCheckboxRadio};
