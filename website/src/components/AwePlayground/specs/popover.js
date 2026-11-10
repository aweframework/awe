/**
 * Playground of the AWE popover. Every attribute is one the XSD declares for `popover` and the React client reads. A
 * popover is a dialog anchored to a control of the screen: the stage has that control, a button with a `dialog`
 * action that opens it (the XML shows it too). The popover is drawn on document.body, next to its anchor: it follows
 * the colour mode of the site, not the one of the preview.
 * A playground spec is described in playgroundModel.js.
 */
const FOOTER = {
  elementType: 'Tag', type: 'div', style: 'modal-footer',
  elementList: [{elementType: 'Button', id: 'Done'}],
};

const popover = {
  name: 'popover',
  element: 'Popover',
  tag: 'popover',
  id: 'demo',
  reference: {element: 'popover', page: 'screen'},
  api: '/docs/api/popover',
  stage: 'wide',
  controls: [
    {id: 'label', type: 'text', label: 'Title', xml: 'label', default: '', initial: 'Quick note'},
    {
      id: 'position', type: 'segmented', label: 'Side of the anchor', xml: 'position', default: 'bottom',
      options: [
        {value: 'bottom', label: 'Bottom'},
        {value: 'top', label: 'Top'},
        {value: 'left', label: 'Left'},
        {value: 'right', label: 'Right'},
      ],
    },
    {
      id: 'maxWidth', type: 'segmented', label: 'Maximum width', xml: 'max-width', default: '',
      options: [
        {value: '', label: 'Default'},
        {value: '14rem', label: 'Narrow'},
        {value: '30rem', label: 'Wide'},
      ],
    },
    {id: 'footer', type: 'switch', label: 'Footer with a button', writes: 'tag', client: '', default: false},
  ],
  xmlAttributes: () => [['anchor', 'OpenNote']],
  children: [
    {tag: 'criteria', attrs: {id: 'Note', component: 'textarea', label: 'Note'}},
    {
      when: 'footer',
      tag: 'tag',
      attrs: {type: 'div', style: 'modal-footer'},
      children: [{
        tag: 'button',
        attrs: {id: 'Done', label: 'Done', style: 'btn-primary'},
        children: [{tag: 'button-action', attrs: {type: 'close', target: 'demo'}}],
      }],
    },
  ],
  siblings: [{
    tag: 'button',
    attrs: {id: 'OpenNote', label: 'Add a note', icon: 'lucide:message-square'},
    children: [{tag: 'button-action', attrs: {type: 'dialog', target: 'demo'}}],
  }],
  scene: values => ({
    attributes: {anchor: 'OpenNote'},
    node: {
      elementList: [
        {elementType: 'Criteria', component: 'textarea', id: 'Note'},
        ...(values.footer ? [FOOTER] : []),
      ],
    },
    components: {
      Note: {attributes: {label: 'Note'}},
      Done: {attributes: {label: 'Done', style: 'btn-primary'}, actions: [{type: 'close', target: 'demo'}]},
      OpenNote: {attributes: {label: 'Add a note', icon: 'lucide:message-square'}, actions: [{type: 'dialog', target: 'demo'}]},
    },
    nodes: [{elementType: 'Button', id: 'OpenNote'}],
  }),
  events: [
    {name: 'open', description: 'Stored in the popover when it is opened.'},
    {name: 'close', description: 'Stored in the popover when it is closed.'},
  ],
  actions: [
    {name: 'dialog', description: 'Opens the popover named in target, next to its anchor.'},
    {name: 'close', description: 'Closes the popover and goes on with the actions that were waiting.'},
    {name: 'close-cancel', description: 'Closes the popover and cancels the actions that were waiting.'},
  ],
};

module.exports = {popover};
