/**
 * Playground of the AWE widgets that show content: the `widget` element with the type `carousel` (images, videos and PDF
 * documents one after another) or `help-viewer` (the help page of the application). Every attribute is one the XSD declares for
 * `widget`. A carousel takes its slides from a query and a help viewer asks the server for the HTML of its help: the stage
 * fills the slides with a static model of three images and answers the request of the help with a static page (see
 * AweLive/simulateFiles.js). The file and PDF viewers are in their own page.
 * A playground spec is described in playgroundModel.js.
 */
const SLIDES = [
  {type: 'image', url: '/live/media/photo-mountains.svg', title: 'Mountains', description: 'Snow at dusk'},
  {type: 'image', url: '/live/media/photo-waves.svg', title: 'Waves', description: 'Calm sea'},
  {type: 'image', url: '/live/media/photo-forest.svg', title: 'Forest', description: 'Pine trees'},
];

const widget = {
  name: 'widget',
  element: 'Widget',
  tag: 'widget',
  id: 'demo',
  reference: {element: 'widget', page: 'screen'},
  api: '/docs/api/widget',
  stage: 'wide',
  controls: [
    {
      id: 'type', type: 'segmented', label: 'Widget type', xml: 'type', client: '', default: '', initial: 'carousel',
      options: [
        {value: 'carousel', label: 'Carousel'},
        {value: 'help-viewer', label: 'Help viewer'},
      ],
    },
  ],
  xmlAttributes: values => (values.type === 'carousel' ? [['initial-load', 'query'], ['target-action', 'GalleryQuery']] : []),
  scene: values => ({
    node: {type: values.type},
    ...(values.type === 'carousel' && {model: {values: SLIDES}}),
    ...(values.type === 'help-viewer' && {files: {help: '/live/help-en.html'}}),
  }),
  events: [],
  actions: [],
};

module.exports = {widget};
