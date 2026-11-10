/**
 * Playground of the AWE file viewers: the `widget` element with the type `file-viewer` (the text of a file) or `pdf-viewer`
 * (a PDF document). Every attribute is one the XSD declares for `widget` and `widget-parameter`. A viewer asks the server for its
 * file; the stage answers that request with a small static file of the site (see AweLive/simulateFiles.js), so the viewer
 * is the real one and only the server is missing.
 * A playground spec is described in playgroundModel.js.
 */
const FILES = {
  'file-viewer': {target: 'ServerLog', url: '/live/app-server.log'},
  'pdf-viewer': {target: 'Manual', url: '/live/awe-sample.pdf'},
};

const fileViewer = {
  name: 'file-viewer',
  element: 'Widget',
  tag: 'widget',
  id: 'demo',
  reference: {element: 'widget', page: 'screen'},
  api: '/docs/api/widget',
  stage: 'wide',
  layout: 'stacked',
  height: '24rem',
  controls: [
    {
      id: 'type', type: 'segmented', label: 'Widget type', xml: 'type', client: '', default: '', initial: 'file-viewer',
      options: [
        {value: 'file-viewer', label: 'File viewer'},
        {value: 'pdf-viewer', label: 'PDF viewer'},
      ],
    },
    {
      id: 'stickBottom', type: 'switch', label: 'Follow the end of the file', client: '', writes: 'widget-parameter', default: false, initial: true,
      when: {id: 'type', in: ['file-viewer']},
    },
  ],
  xmlAttributes: values => [
    ...(values.type === 'file-viewer' ? [['server-action', 'file'], ['autoload', 'true']] : []),
    ['target-action', FILES[values.type].target],
    ['style', 'expand'],
  ],
  children: [{
    when: {id: 'type', in: ['file-viewer']},
    tag: 'widget-parameter',
    attrs: values => ({type: 'boolean', name: 'stickBottom', value: String(values.stickBottom)}),
  }],
  scene: values => ({
    node: {type: values.type},
    attributes: {
      style: 'expand',
      targetAction: FILES[values.type].target,
      ...(values.type === 'file-viewer' && {autoload: true, serverAction: 'file', parameters: {stickBottom: values.stickBottom}}),
    },
    files: {[FILES[values.type].target]: FILES[values.type].url},
  }),
  events: [],
  actions: [],
};

/**
 * Props of an AweLive example with a file viewer widget.
 * @param {'file-viewer'|'pdf-viewer'} type Widget type
 * @param {Object} [attributes] Client attributes of the widget (camelCase), over the ones of the viewer
 */
const fileViewerExample = (type, attributes = {}) => ({
  components: {
    viewer: {
      attributes: {
        style: 'expand',
        targetAction: FILES[type].target,
        ...(type === 'file-viewer' && {autoload: true, serverAction: 'file'}),
        ...attributes,
      },
    },
  },
  nodes: [{elementType: 'Widget', id: 'viewer', type}],
  files: {[FILES[type].target]: FILES[type].url},
});

module.exports = {fileViewer, fileViewerExample};
