/**
 * Playground of the number and date criteria: numeric, date, date-range, time and filtered-calendar. Every attribute
 * is one the XSD declares for `criteria` and the React client reads. A playground spec is described in playgroundModel.js.
 */
const shared = require('./criteriaShared');

const KINDS = [
  ['numeric', 'Numeric'], ['date', 'Date'], ['date-range', 'Date range'], ['time', 'Time'],
  ['filtered-calendar', 'Filtered calendar'],
];
const ALL = KINDS.map(([value]) => value);
// The XML writes the format as text; the server sends it to the client already parsed
const NUMBER_FORMAT = '{min: 0, max: 100, step: 5}';
const NUMBER_FORMAT_VALUE = {min: 0, max: 100, step: 5};

const pad = number => String(number).padStart(2, '0');

/** Dates the server allows in a filtered calendar: every Monday and Thursday of the next five weeks, as dd/mm/yyyy. */
function allowedDates(today = new Date()) {
  const dates = [];
  for (let offset = 1; offset <= 35; offset++) {
    const day = new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset);
    if ([1, 4].includes(day.getDay())) {
      dates.push(`${pad(day.getDate())}/${pad(day.getMonth() + 1)}/${day.getFullYear()}`);
    }
  }
  return dates;
}

const criteriaNumbers = {
  name: 'criteriaNumbers',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.componentControl(KINDS, 'numeric'),
    shared.labelControl(ALL, 'Field'),
    shared.placeholderControl(ALL),
    shared.iconControl(['numeric', 'date', 'time', 'filtered-calendar']),
    shared.sizeControl(ALL),
    {id: 'unit', type: 'text', label: 'Unit', xml: 'unit', default: '', initial: 'EUR', when: {id: 'component', in: ['numeric']}},
    {id: 'slider', type: 'switch', label: 'Slider', xml: 'show-slider', default: false, when: {id: 'component', in: ['numeric']}},
    {
      id: 'presets', type: 'text', label: 'Presets', xml: 'presets', default: '', initial: '1M,3M,YTD,ALL',
      when: {id: 'component', in: ['date-range']},
    },
    {
      id: 'timeFormat', type: 'select', label: 'Time format', xml: 'time-format', default: 'HH:mm:ss', initial: 'HH:mm',
      options: ['HH:mm', 'HH:mm:ss', 'hh:mm a', 'hh:mm:ss a'], when: {id: 'component', in: ['time']},
    },
    shared.requiredControl(ALL),
    shared.readonlyControl(ALL),
  ],
  xmlAttributes: values => [
    ...(values.component === 'numeric' && values.slider ? [['number-format', NUMBER_FORMAT]] : []),
    ...(values.component === 'filtered-calendar' ? [['initial-load', 'query'], ['target-action', 'AvailableDates']] : []),
  ],
  scene: values => ({
    node: {component: values.component},
    model: values.component === 'filtered-calendar'
      ? {values: allowedDates().map(value => ({value, label: value}))} : undefined,
    attributes: {
      ...shared.requiredScene(values, ALL).attributes,
      ...(values.component === 'numeric' && values.slider && {numberFormat: NUMBER_FORMAT_VALUE}),
    },
    validationRules: shared.requiredScene(values, ALL).validationRules,
  }),
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteriaNumbers, allowedDates};
