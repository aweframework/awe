/**
 * Playground of the AWE side panel. Every attribute is one the XSD declares for `side-panel` and the React client
 * reads. A side panel is opened by a button with a `dialog` action, so the stage has that button (the XML shows it too)
 * and the examples run the dialog actions for real. The panel is drawn on document.body, over the page: it follows the
 * colour mode of the site, not the one of the preview.
 * `on-close` is not an attribute of the client: with `reject` the client reads `acceptOnClose: false`.
 * A playground spec is described in playgroundModel.js.
 */
const FIELDS = [
  {elementType: 'Criteria', component: 'text', id: 'Name'},
  {elementType: 'Criteria', component: 'textarea', id: 'Notes'},
];

const FOOTER = {
  elementType: 'Tag', type: 'div', style: 'modal-footer',
  elementList: [{elementType: 'Button', id: 'Cancel'}, {elementType: 'Button', id: 'Save'}],
};

const sidePanel = {
  name: 'side-panel',
  element: 'SidePanel',
  tag: 'side-panel',
  id: 'demo',
  reference: {element: 'side-panel', page: 'screen'},
  api: '/docs/api/side-panel',
  controls: [
    {id: 'label', type: 'text', label: 'Title', xml: 'label', default: '', initial: 'Edit customer'},
    {
      id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', default: '', initial: '',
      options: ['', 'lucide:user', 'lucide:settings', 'lucide:filter', 'lucide:bell'],
    },
    {
      id: 'position', type: 'segmented', label: 'Edge', xml: 'position', default: 'right',
      options: [
        {value: 'right', label: 'Right'},
        {value: 'left', label: 'Left'},
      ],
    },
    {
      id: 'maxWidth', type: 'segmented', label: 'Maximum width', xml: 'max-width', default: '',
      options: [
        {value: '', label: 'Default'},
        {value: '20rem', label: 'Narrow'},
        {value: '30rem', label: 'Medium'},
        {value: '50vw', label: 'Half the screen'},
      ],
    },
    {
      id: 'onClose', type: 'segmented', label: 'When it is dismissed', xml: 'on-close', client: '', default: 'accept',
      options: [
        {value: 'accept', label: 'Accept'},
        {value: 'reject', label: 'Reject'},
      ],
    },
    {id: 'footer', type: 'switch', label: 'Footer with buttons', writes: 'tag', client: '', default: true},
  ],
  children: [
    {
      tag: 'tag',
      attrs: {type: 'div', style: 'panel-body'},
      children: [
        {tag: 'criteria', attrs: {id: 'Name', component: 'text', label: 'Name'}},
        {tag: 'criteria', attrs: {id: 'Notes', component: 'textarea', label: 'Notes'}},
      ],
    },
    {
      when: 'footer',
      tag: 'tag',
      attrs: {type: 'div', style: 'modal-footer'},
      children: [
        {tag: 'button', attrs: {id: 'Cancel', label: 'Cancel'}, children: [{tag: 'button-action', attrs: {type: 'close-cancel', target: 'demo'}}]},
        {tag: 'button', attrs: {id: 'Save', label: 'Save', style: 'btn-primary'}, children: [{tag: 'button-action', attrs: {type: 'close', target: 'demo'}}]},
      ],
    },
  ],
  siblings: [{
    tag: 'button',
    attrs: {id: 'OpenPanel', label: 'Open the side panel'},
    children: [{tag: 'button-action', attrs: {type: 'dialog', target: 'demo'}}],
  }],
  scene: values => ({
    attributes: values.onClose === 'reject' ? {acceptOnClose: false} : {},
    node: {
      elementList: [
        {elementType: 'Tag', type: 'div', style: 'panel-body', elementList: FIELDS},
        ...(values.footer ? [FOOTER] : []),
      ],
    },
    components: {
      Name: {attributes: {label: 'Name'}},
      Notes: {attributes: {label: 'Notes'}},
      Cancel: {attributes: {label: 'Cancel'}, actions: [{type: 'close-cancel', target: 'demo'}]},
      Save: {attributes: {label: 'Save', style: 'btn-primary'}, actions: [{type: 'close', target: 'demo'}]},
      OpenPanel: {attributes: {label: 'Open the side panel'}, actions: [{type: 'dialog', target: 'demo'}]},
    },
    nodes: [{elementType: 'Button', id: 'OpenPanel'}],
  }),
  events: [
    {name: 'open', description: 'Stored in the side panel when it is opened.'},
    {name: 'close', description: 'Stored in the side panel when it is closed.'},
  ],
  actions: [
    {name: 'dialog', description: 'Opens the side panel named in target.'},
    {name: 'close', description: 'Closes the side panel and goes on with the actions that were waiting.'},
    {name: 'close-cancel', description: 'Closes the side panel and cancels the actions that were waiting.'},
  ],
};

module.exports = {sidePanel};
