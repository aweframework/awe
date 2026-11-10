/**
 * Playground of the AWE avatar: the user image of the header, which shows an image, the initials of its label or an icon,
 * next to the label, and opens a dropdown when it has content. Every attribute is one the XSD declares for `avatar` and the
 * React client reads; a real screen also gives it the name and the description of the user from a query. The image is a small
 * static portrait of the site. The dropdown follows the colour mode of the site, not the one of the preview.
 * A playground spec is described in playgroundModel.js.
 */
const ITEMS = [['Profile', 'Profile', 'user'], ['SignOut', 'Sign out', 'sign-out']];

const avatar = {
  name: 'avatar',
  element: 'Avatar',
  tag: 'avatar',
  id: 'demo',
  reference: {element: 'avatar', page: 'screen'},
  api: '/docs/api/info',
  controls: [
    {id: 'label', type: 'text', label: 'Label', xml: 'label', default: '', initial: 'Ada Lovelace'},
    {
      id: 'image', type: 'segmented', label: 'Image', xml: 'image', default: '',
      options: [
        {value: '', label: 'Initials'},
        {value: '/live/media/avatar.svg', label: 'Portrait'},
      ],
    },
    {
      id: 'size', type: 'segmented', label: 'Size', xml: 'size', default: '',
      options: [
        {value: '', label: 'Default'},
        {value: 'sm', label: 'Small'},
        {value: 'lg', label: 'Large'},
      ],
    },
    {id: 'unit', type: 'text', label: 'Badge', xml: 'unit', default: ''},
    {id: 'showLabel', type: 'switch', label: 'Show the label', xml: 'show-label', default: true},
    {id: 'menu', type: 'switch', label: 'Dropdown menu', writes: 'info-button', client: '', default: true},
  ],
  children: ITEMS.map(([id, label, icon]) => ({
    when: 'menu',
    tag: 'info-button',
    attrs: {id, label, icon},
  })),
  scene: values => ({
    node: {elementList: values.menu ? ITEMS.map(([id]) => ({elementType: 'InfoButton', id})) : []},
    components: Object.fromEntries(ITEMS.map(([id, label, icon]) => [id, {attributes: {label, icon}}])),
  }),
  events: [{name: 'click', description: 'Stored in the avatar when the user clicks it.'}],
  actions: [],
};

module.exports = {avatar};
