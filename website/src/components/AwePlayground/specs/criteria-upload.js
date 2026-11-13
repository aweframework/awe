/**
 * Playground of the uploader criterion. Every attribute is one the XSD declares for `criteria` and the React client
 * reads. The upload is simulated on the stage (`uploads`): the file is never sent anywhere. A playground spec is
 * described in playgroundModel.js.
 */
const shared = require('./criteriaShared');


const criteriaUpload = {
  name: 'criteriaUpload',
  element: 'Criteria',
  tag: 'criteria',
  id: 'demo',
  reference: {element: 'criteria', page: 'screen'},
  api: '/docs/api/criteria',
  stage: 'wide',
  controls: [
    shared.labelControl(undefined, 'Attachment'),
    shared.placeholderControl(),
    shared.sizeControl(),
    {id: 'dropZone', type: 'switch', label: 'Drop zone', xml: 'drop-zone', default: false},
    {
      id: 'capture', type: 'segmented', label: 'Camera', xml: 'capture', default: '',
      options: [{value: '', label: 'Off'}, {value: 'user', label: 'Front'}, {value: 'environment', label: 'Rear'}],
    },
    shared.requiredControl(),
    shared.readonlyControl(),
  ],
  xmlAttributes: () => [['component', 'uploader'], ['destination', 'attachments']],
  scene: values => ({
    node: {component: 'uploader'},
    attributes: {destination: 'attachments', ...shared.requiredScene(values).attributes},
    validationRules: shared.requiredScene(values).validationRules,
    uploads: true,
  }),
  events: shared.EVENTS,
  actions: shared.ACTIONS,
};

module.exports = {criteriaUpload};
