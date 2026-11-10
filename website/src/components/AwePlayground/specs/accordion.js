/**
 * Playground of the AWE accordion. Every attribute is one the XSD declares for `accordion` and the React client reads;
 * `autocollapse` is a boolean for the client (false lets several items stay open).
 * A playground spec is described in playgroundModel.js.
 */
const ITEMS = [
  {id: 'shipping', label: 'Shipping', text: 'Address, carrier and delivery window of the order.'},
  {id: 'payment', label: 'Payment', text: 'Payment method, instalments and the invoice data.'},
  {id: 'notes', label: 'Notes', text: 'Free text for the warehouse and for the customer service.'},
];

const accordion = {
  name: 'accordion',
  element: 'Accordion',
  tag: 'accordion',
  id: 'demo',
  reference: {element: 'accordion', page: 'screen'},
  api: '/docs/api/accordion',
  stage: 'wide',
  controls: [
    {
      id: 'autocollapse', type: 'segmented', label: 'Open items', xml: 'autocollapse', default: 'true',
      options: [
        {value: 'true', label: 'One at a time'},
        {value: 'false', label: 'Several'},
      ],
    },
  ],
  children: ITEMS.map(item => ({
    tag: 'accordion-item',
    attrs: {id: item.id, label: item.label},
    children: [{tag: 'tag', attrs: {type: 'p', label: item.text}}],
  })),
  scene: values => ({
    attributes: {autocollapse: values.autocollapse !== 'false'},
    node: {
      elementList: ITEMS.map(item => ({
        elementType: 'AccordionItem',
        id: item.id,
        label: item.label,
        elementList: [{elementType: 'Tag', type: 'p', label: item.text}],
      })),
    },
  }),
  events: [],
  actions: [],
};

module.exports = {accordion};
