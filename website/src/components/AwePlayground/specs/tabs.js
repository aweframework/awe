/**
 * Playground of the AWE tab (tab and tabcontainer). Every attribute is one the XSD declares for `tab` and the React
 * client reads. The list of tabs comes from an enum or a query on the server (initial-load and target-action); the
 * stage fills it with a static model whose values are the ids of the tabcontainers.
 * A playground spec is described in playgroundModel.js.
 */
const PAGES = [
  {id: 'general', label: 'General', text: 'Name, tax id and the main contact of the customer.'},
  {id: 'billing', label: 'Billing', text: 'Payment method, invoicing address and the invoices of the year.'},
  {id: 'history', label: 'History', text: 'Orders, incidents and every change made to the customer.'},
];

const tabs = {
  name: 'tabs',
  element: 'Tab',
  tag: 'tab',
  id: 'demo',
  reference: {element: 'tab', page: 'screen'},
  api: '/docs/api/tab-and-tabcontainer',
  stage: 'wide',
  controls: [
    {
      id: 'orientation', type: 'segmented', label: 'Orientation', xml: 'orientation', default: 'horizontal',
      options: [
        {value: 'horizontal', label: 'Horizontal'},
        {value: 'vertical', label: 'Vertical'},
      ],
    },
  ],
  xmlAttributes: () => [['initial-load', 'enum'], ['target-action', 'CustomerTabs']],
  children: PAGES.map(page => ({
    tag: 'tabcontainer',
    attrs: {id: page.id},
    children: [{tag: 'tag', attrs: {type: 'p', label: page.text}}],
  })),
  scene: () => ({
    model: {values: PAGES.map((page, index) => ({value: page.id, label: page.label, selected: index === 0}))},
    node: {
      elementList: PAGES.map(page => ({
        elementType: 'TabContainer',
        id: page.id,
        elementList: [{elementType: 'Tag', type: 'p', label: page.text}],
      })),
    },
  }),
  events: [],
  actions: [],
};

module.exports = {tabs};
