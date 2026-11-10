/**
 * Playground of the AWE messages: the `message` element, a predefined message that a `confirm` action shows in a modal
 * confirm dialog before the actions that follow it go on. Every attribute is one the XSD declares for `message`. The
 * message is not a visual element, so the stage has the button that raises it (the XML shows it too). The messages the
 * server sends (toasts) are in the recipes of the page. The dialog is drawn over the page: it follows the colour mode of
 * the site, not the one of the preview.
 * A playground spec is described in playgroundModel.js.
 */
const message = {
  name: 'message',
  // A message has no element of its own: the stage renders an empty tag next to the button
  element: 'Tag',
  tag: 'message',
  id: 'DeleteMsg',
  reference: {element: 'message', page: 'screen'},
  api: '/docs/api/messages',
  controls: [
    {id: 'title', type: 'text', label: 'Title', xml: 'title', client: '', default: '', initial: 'Delete the customer?'},
    {id: 'message', type: 'text', label: 'Message', xml: 'message', client: '', default: '', initial: 'The customer and its orders will be deleted.'},
  ],
  siblings: [{
    tag: 'button',
    attrs: {id: 'AskDelete', label: 'Delete customer', icon: 'lucide:trash-2', style: 'btn-danger'},
    children: [{tag: 'button-action', attrs: {type: 'confirm', target: 'DeleteMsg'}}],
  }],
  scene: values => ({
    node: {elementList: []},
    messages: {DeleteMsg: {title: values.title, message: values.message}},
    components: {
      AskDelete: {
        attributes: {label: 'Delete customer', icon: 'lucide:trash-2', style: 'btn-danger'},
        actions: [{type: 'confirm', target: 'DeleteMsg', parameters: {target: 'DeleteMsg'}}],
      },
    },
    nodes: [{elementType: 'Button', id: 'AskDelete'}],
  }),
  events: [],
  actions: [
    {name: 'confirm', description: 'Shows the message named in target in a confirm dialog. Accept goes on with the actions that follow, Cancel stops them.'},
  ],
};

module.exports = {message};
