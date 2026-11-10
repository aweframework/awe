/**
 * Playground of the AWE window. Every attribute is one the XSD declares for `window` and the React client reads. The
 * client draws the window from the properties of its node in the screen structure (not from the state of a component), so
 * the scene gives them in the node. `maximize` is not offered: a maximized window is placed over the viewport (or over the
 * center area of the template), and the page of the docs scrolls, which leaves the window behind. The advanced search and
 * the saved searches need a block of criteria and the server of the screen; they are described in the documentation of the window.
 * A playground spec is described in playgroundModel.js.
 */
const BODY = [
  {elementType: 'Criteria', component: 'text', id: 'Name'},
  {elementType: 'Criteria', component: 'text', id: 'Email'},
];

const FOOTER = {
  elementType: 'Tag', type: 'div', style: 'panel-footer',
  elementList: [{elementType: 'Button', id: 'Save'}],
};

const windowSpec = {
  name: 'window',
  // The window is not a component of the store: the stage renders it from the properties of its node
  element: 'Window',
  tag: 'window',
  id: 'demo',
  reference: {element: 'window', page: 'screen'},
  api: '/docs/api/window',
  stage: 'wide',
  controls: [
    {id: 'label', type: 'text', label: 'Title', xml: 'label', client: '', default: '', initial: 'Customer data'},
    {
      id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', client: '', default: '', initial: 'lucide:user',
      options: ['', 'lucide:user', 'lucide:filter', 'lucide:list', 'lucide:settings'],
    },
    {id: 'help', type: 'text', label: 'Help text', xml: 'help', client: '', default: ''},
    {id: 'footer', type: 'switch', label: 'Footer with a button', writes: 'tag', client: '', default: true},
  ],
  children: [
    {
      tag: 'tag',
      attrs: {type: 'div', style: 'panel-body'},
      children: [
        {tag: 'criteria', attrs: {id: 'Name', component: 'text', label: 'Name'}},
        {tag: 'criteria', attrs: {id: 'Email', component: 'text', label: 'Email'}},
      ],
    },
    {
      when: 'footer',
      tag: 'tag',
      attrs: {type: 'div', style: 'panel-footer'},
      children: [{tag: 'button', attrs: {id: 'Save', label: 'Save', style: 'btn-primary'}}],
    },
  ],
  scene: values => ({
    node: {
      label: values.label,
      ...(values.icon && {icon: values.icon}),
      ...(values.help && {help: values.help}),
      elementList: [
        {elementType: 'Tag', type: 'div', style: 'panel-body', elementList: BODY},
        ...(values.footer ? [FOOTER] : []),
      ],
    },
    components: {
      Name: {attributes: {label: 'Name'}},
      Email: {attributes: {label: 'Email'}},
      Save: {attributes: {label: 'Save', style: 'btn-primary'}},
    },
  }),
  events: [],
  actions: [],
};

module.exports = {window: windowSpec};
