/**
 * Playground of the AWE info (`info` element): a button of the header that shows a value and, when it has content, opens
 * a dropdown with it. Every attribute is one the XSD declares for `info` and the React client reads; a real screen can
 * also fill the label from a query (the `value` and `property` attributes) or from the session. The dropdown follows
 * the colour mode of the site, not the one of the preview (it is drawn over the page).
 * A playground spec is described in playgroundModel.js.
 */
const info = {
  name: 'info',
  element: 'Info',
  tag: 'info',
  id: 'demo',
  reference: {element: 'info', page: 'screen'},
  api: '/docs/api/info',
  controls: [
    {id: 'label', type: 'text', label: 'Label', xml: 'label', default: '', initial: 'Notifications'},
    {
      id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', default: '', initial: 'lucide:bell',
      options: ['', 'lucide:bell', 'lucide:mail', 'lucide:circle-help', 'lucide:user'],
    },
    {id: 'unit', type: 'text', label: 'Badge', xml: 'unit', default: '', initial: '3'},
    {id: 'content', type: 'switch', label: 'Dropdown with content', writes: 'tag', client: '', default: true},
  ],
  children: [{
    when: 'content',
    tag: 'tag',
    attrs: {type: 'p', label: 'You have 3 unread notifications.'},
  }],
  scene: values => ({
    node: {elementList: values.content ? [{elementType: 'Tag', type: 'p', label: 'You have 3 unread notifications.'}] : []},
  }),
  events: [{name: 'click', description: 'Stored in the info when the user clicks it.'}],
  actions: [],
};

module.exports = {info};
