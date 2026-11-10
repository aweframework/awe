/**
 * Playground of the AWE wizard (wizard and wizard-panel). Every attribute is one the XSD declares for `wizard` and the
 * React client reads. The steps come from an enum or a query on the server (initial-load and target-action); the stage
 * fills them with a static model whose values are the ids of the panels. The buttons below it carry the `prev-step` and
 * `next-step` actions: the examples run them for real.
 * A playground spec is described in playgroundModel.js.
 */
const STEPS = [
  {id: 'account', label: 'Account', text: 'Choose the name and the email of the new account.'},
  {id: 'plan', label: 'Plan', text: 'Pick the plan: monthly or yearly, with or without support.'},
  {id: 'payment', label: 'Payment', text: 'Enter the payment method and accept the conditions.'},
  {id: 'confirm', label: 'Confirm', text: 'Check the summary and create the account.'},
];

const stepsOf = values => STEPS.slice(0, Number(values.steps));

const wizard = {
  name: 'wizard',
  element: 'Wizard',
  tag: 'wizard',
  id: 'demo',
  reference: {element: 'wizard', page: 'screen'},
  api: '/docs/api/wizard-and-wizard-panel',
  stage: 'wide',
  controls: [
    {
      id: 'steps', type: 'segmented', label: 'Steps', client: '', writes: 'wizard-panel', default: '', initial: '3',
      options: ['2', '3', '4'],
    },
    {
      id: 'orientation', type: 'segmented', label: 'Orientation', xml: 'orientation', default: 'horizontal',
      options: [
        {value: 'horizontal', label: 'Horizontal'},
        {value: 'vertical', label: 'Vertical'},
      ],
    },
  ],
  xmlAttributes: () => [['initial-load', 'enum'], ['target-action', 'SignupSteps']],
  children: STEPS.map((step, index) => ({
    // The first two steps are always there; the others while the number of steps reaches them
    ...(index >= 2 && {when: {id: 'steps', in: ['2', '3', '4'].filter(count => Number(count) > index)}}),
    tag: 'wizard-panel',
    attrs: {id: step.id},
    children: [{tag: 'tag', attrs: {type: 'p', label: step.text}}],
  })),
  siblings: [
    {
      tag: 'button',
      attrs: {id: 'Back', label: 'Back', style: 'btn-ghost'},
      children: [{tag: 'button-action', attrs: {type: 'prev-step', target: 'demo'}}],
    },
    {
      tag: 'button',
      attrs: {id: 'Next', label: 'Next', style: 'btn-primary'},
      children: [{tag: 'button-action', attrs: {type: 'next-step', target: 'demo'}}],
    },
  ],
  scene: values => ({
    model: {values: stepsOf(values).map((step, index) => ({value: step.id, label: step.label, selected: index === 0}))},
    node: {
      elementList: stepsOf(values).map(step => ({
        elementType: 'WizardPanel',
        id: step.id,
        elementList: [{elementType: 'Tag', type: 'p', label: step.text}],
      })),
    },
    components: {
      Back: {attributes: {label: 'Back', style: 'btn-ghost'}, actions: [{type: 'prev-step', target: 'demo'}]},
      Next: {attributes: {label: 'Next', style: 'btn-primary'}, actions: [{type: 'next-step', target: 'demo'}]},
    },
    nodes: [{elementType: 'Tag', type: 'div', style: 'awe-live-actions', elementList: [
      {elementType: 'Button', id: 'Back'}, {elementType: 'Button', id: 'Next'},
    ]}],
  }),
  events: [],
  actions: [
    {name: 'next-step', description: 'Moves to the next step of the wizard named in target.'},
    {name: 'prev-step', description: 'Moves to the previous step.'},
    {name: 'first-step', description: 'Moves to the first step.'},
    {name: 'last-step', description: 'Moves to the last step.'},
    {name: 'nth-step', description: 'Moves to the step whose value (the id of its panel) is in the value parameter of the action.'},
  ],
};

module.exports = {wizard};
