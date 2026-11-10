/**
 * Playground of the AWE tag: the basic building block of a screen. Every attribute is one the XSD declares for `tag` and the
 * React client reads (`type`, `label`, `style`, `icon`); the client reads them from the node of the structure. The two
 * section title styles are the ones of the design system and need a heading type.
 * A playground spec is described in playgroundModel.js.
 */
const tag = {
  name: 'tag',
  element: 'Tag',
  tag: 'tag',
  id: 'demo',
  reference: {element: 'tag', page: 'screen'},
  api: '/docs/api/tags',
  stage: 'wide',
  controls: [
    {
      id: 'type', type: 'segmented', label: 'HTML element', xml: 'type', client: '', default: '', initial: 'h3',
      options: [
        {value: 'h3', label: 'h3'},
        {value: 'p', label: 'p'},
        {value: 'div', label: 'div'},
        {value: 'span', label: 'span'},
      ],
    },
    {id: 'label', type: 'text', label: 'Label', xml: 'label', client: '', default: '', initial: 'Customer identity'},
    {
      id: 'look', type: 'select', label: 'Style', xml: 'style', client: '', default: '', initial: 'awe-section-title',
      options: [
        {value: '', label: 'None'},
        {value: 'awe-section-title', label: 'Section title'},
        {value: 'awe-form-section-title', label: 'Form section title'},
      ],
    },
    {
      id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', client: '', default: '', initial: '',
      options: ['', 'lucide:user', 'lucide:key-round', 'lucide:mail', 'lucide:settings'],
    },
  ],
  scene: values => ({
    node: {
      id: 'demo',
      label: values.label,
      ...(values.type && {type: values.type}),
      ...(values.look && {style: values.look}),
      ...(values.icon && {icon: values.icon}),
    },
  }),
  events: [],
  actions: [],
};

module.exports = {tag};
