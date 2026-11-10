/**
 * Playground of the attributes every AWE criterion shares (label, placeholder, icon, size, validation, read only), on
 * three of the simplest inputs. The other types have their own playgrounds, one by family: criteria-text.js,
 * criteria-numbers.js, criteria-choices.js and criteria-upload.js. The options of a select come from a query or an
 * enum on the server; here the stage fills them with a static model, so the XML names the enum a real screen would use.
 * A playground spec is described in playgroundModel.js.
 */
const shared = require('./criteriaShared');

const KINDS = ['text', 'select', 'checkbox'];
const FIELDS = ['text', 'select'];

const criteria = {
  name: 'criteria',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.componentControl([['text', 'Text'], ['select', 'Select'], ['checkbox', 'Checkbox']], 'text'),
    shared.labelControl(KINDS, 'Customer'),
    shared.placeholderControl(FIELDS),
    shared.iconControl(FIELDS),
    shared.sizeControl(FIELDS),
    shared.requiredControl(KINDS),
    shared.readonlyControl(KINDS),
  ],
  xmlAttributes: values => (values.component === 'select'
    ? [['initial-load', 'enum'], ['target-action', 'CountryList']] : []),
  scene: values => ({
    node: {component: values.component},
    model: {
      select: {values: shared.COUNTRIES},
      checkbox: {values: [{value: '1', label: 'Yes', selected: false}]},
    }[values.component],
    ...shared.requiredScene(values, KINDS),
  }),
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteria};
