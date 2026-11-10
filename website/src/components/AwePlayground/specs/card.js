/**
 * Playground of the AWE card (a metric tile). Every attribute is one the XSD declares for `card` and the React client
 * reads from its attributes (a real screen can also fill them with the first row of a query). The body switch adds a
 * button as a child: the children of a card go below its value.
 * A playground spec is described in playgroundModel.js.
 */
const TONES = [
  {value: '', label: 'Default'},
  {value: 'success', label: 'Success'},
  {value: 'warning', label: 'Warning'},
  {value: 'danger', label: 'Danger'},
  {value: 'info', label: 'Info'},
  {value: 'neutral', label: 'Neutral'},
];

const card = {
  name: 'card',
  element: 'Card',
  tag: 'card',
  id: 'demo',
  reference: {element: 'card', page: 'screen'},
  api: '/docs/api/card',
  stage: 'wide',
  controls: [
    {id: 'title', type: 'text', label: 'Title', xml: 'title', default: '', initial: 'Active customers'},
    {id: 'subtitle', type: 'text', label: 'Subtitle', xml: 'subtitle', default: ''},
    {id: 'value', type: 'text', label: 'Value', xml: 'value', default: '', initial: '1284'},
    {id: 'unit', type: 'text', label: 'Unit', xml: 'unit', default: ''},
    {
      id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', default: '', initial: 'lucide:users',
      options: ['', 'lucide:users', 'lucide:trending-up', 'lucide:euro', 'lucide:shopping-cart'],
    },
    {id: 'iconTone', type: 'select', label: 'Icon tone', xml: 'icon-tone', default: '', options: TONES},
    {id: 'delta', type: 'text', label: 'Change', xml: 'delta', default: '', initial: '4.2'},
    {
      id: 'trend', type: 'segmented', label: 'Trend', xml: 'trend', default: '',
      options: [
        {value: '', label: 'Automatic'},
        {value: 'up', label: 'Up'},
        {value: 'down', label: 'Down'},
        {value: 'flat', label: 'Flat'},
      ],
    },
    {id: 'caption', type: 'text', label: 'Caption', xml: 'caption', default: '', initial: 'vs. last month'},
    {id: 'chip', type: 'text', label: 'Status chip', xml: 'chip', default: ''},
    {id: 'chipTone', type: 'select', label: 'Chip tone', xml: 'chip-tone', default: '', options: TONES},
    {id: 'target', type: 'text', label: 'Target', xml: 'target', default: ''},
    {
      id: 'progressStyle', type: 'segmented', label: 'Progress style', xml: 'progress-style', default: '',
      options: [
        {value: '', label: 'Bar'},
        {value: 'gauge', label: 'Gauge'},
      ],
    },
    {
      id: 'sparkline', type: 'segmented', label: 'Sparkline', xml: 'sparkline', default: '',
      options: [
        {value: '', label: 'None'},
        {value: '[8,12,10,16,14,22]', label: 'Rising'},
        {value: '[22,18,19,12,14,8]', label: 'Falling'},
      ],
    },
    {
      id: 'sparklineType', type: 'segmented', label: 'Sparkline type', xml: 'sparkline-type', default: '',
      options: [
        {value: '', label: 'Area'},
        {value: 'line', label: 'Line'},
        {value: 'bar', label: 'Bars'},
      ],
    },
    {id: 'description', type: 'text', label: 'Description', xml: 'description', default: ''},
    {id: 'footer', type: 'text', label: 'Footer', xml: 'footer', default: ''},
    {id: 'body', type: 'switch', label: 'Button in the body', writes: 'button', client: '', default: false},
  ],
  children: [
    {when: 'body', tag: 'button', attrs: {id: 'Details', label: 'View details', style: 'btn-ghost'}},
  ],
  scene: values => ({
    node: values.body ? {elementList: [{elementType: 'Button', id: 'Details'}]} : {},
    components: values.body ? {Details: {attributes: {label: 'View details', style: 'btn-ghost'}}} : {},
  }),
  events: [],
  actions: [
    {name: 'filter', description: 'Reloads the card with the first row of its query.'},
    {name: 'reload', description: 'Reloads the card, as any component with initial-load.'},
  ],
};

module.exports = {card};
