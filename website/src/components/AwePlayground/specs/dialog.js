/**
 * Playground of the AWE dialog. Every attribute is one the XSD declares for `dialog` and the React client reads. A
 * dialog is opened by a button with a `dialog` action, so the stage has that button (the XML shows it too) and the
 * examples run the dialog actions for real. The dialog is drawn on document.body, over the page: it follows the colour
 * mode of the site, not the one of the preview.
 * A playground spec is described in playgroundModel.js.
 */
const FOOTER = {
  elementType: 'Tag', type: 'div', style: 'modal-footer', elementList: [{elementType: 'Button', id: 'Close'}],
};

const dialog = {
  name: 'dialog',
  element: 'Dialog',
  tag: 'dialog',
  id: 'demo',
  reference: {element: 'dialog', page: 'screen'},
  api: '/docs/api/dialog',
  controls: [
    {id: 'label', type: 'text', label: 'Title', xml: 'label', default: '', initial: 'Edit customer'},
    {
      id: 'size', type: 'segmented', label: 'Width', xml: 'style', default: '',
      options: [
        {value: '', label: 'Default'},
        {value: 'modal-sm', label: 'Small'},
        {value: 'modal-md', label: 'Medium'},
        {value: 'modal-lg', label: 'Large'},
      ],
    },
    {id: 'footer', type: 'switch', label: 'Footer with a button', writes: 'tag', client: '', default: true},
  ],
  children: [
    {tag: 'tag', attrs: {type: 'div', style: 'modal-body', label: 'Update the data of the customer and save the changes.'}},
    {
      when: 'footer',
      tag: 'tag',
      attrs: {type: 'div', style: 'modal-footer'},
      children: [{
        tag: 'button',
        attrs: {id: 'Close', label: 'Close', style: 'btn-primary'},
        children: [{tag: 'button-action', attrs: {type: 'close', target: 'demo'}}],
      }],
    },
  ],
  siblings: [{
    tag: 'button',
    attrs: {id: 'Open', label: 'Open the dialog'},
    children: [{tag: 'button-action', attrs: {type: 'dialog', target: 'demo'}}],
  }],
  scene: values => ({
    node: {
      elementList: [
        {elementType: 'Tag', type: 'div', style: 'modal-body', label: 'Update the data of the customer and save the changes.'},
        ...(values.footer ? [FOOTER] : []),
      ],
    },
    components: {
      Close: {attributes: {label: 'Close', style: 'btn-primary'}, actions: [{type: 'close', target: 'demo'}]},
      Open: {attributes: {label: 'Open the dialog'}, actions: [{type: 'dialog', target: 'demo'}]},
    },
    nodes: [{elementType: 'Button', id: 'Open'}],
  }),
  events: [{name: 'open', description: 'Stored in the dialog when it is opened.'}, {name: 'close', description: 'Stored in the dialog when it is closed.'}],
  actions: [
    {name: 'dialog', description: 'Opens the dialog named in target.'},
    {name: 'close', description: 'Closes the dialog and goes on with the actions that were waiting.'},
    {name: 'close-cancel', description: 'Closes the dialog and cancels the actions that were waiting.'},
  ],
};

module.exports = {dialog};
