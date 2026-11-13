/**
 * Playground of the AWE button. Every attribute is one the XSD declares (`button` element) and the React client
 * reads; `disabled` is not an attribute of the XML: a button is disabled by a dependency, so the control writes one.
 * A playground spec is described in playgroundModel.js; the extra fields here feed the sections under the playground.
 */
const button = {
  name: 'button',
  element: 'Button',
  tag: 'button',
  id: 'demo',
  reference: {element: 'button', page: 'screen'},
  api: '/docs/api/button',
  controls: [
    {id: 'label', type: 'text', label: 'Label', xml: 'label', default: '', initial: 'Save changes'},
    {
      id: 'buttonType', type: 'segmented', label: 'Button type', xml: 'button-type', default: 'button', initial: 'submit',
      options: [
        {value: 'button', label: 'Normal'},
        {value: 'submit', label: 'Submit'},
        {value: 'reset', label: 'Reset'},
      ],
    },
    {
      id: 'look', type: 'select', label: 'Look', xml: 'style', default: '', initial: '',
      options: [
        {value: '', label: 'Default'},
        {value: 'btn-primary', label: 'Primary'},
        {value: 'btn-default', label: 'Secondary'},
        {value: 'btn-danger', label: 'Danger'},
        {value: 'btn-success', label: 'Success'},
        {value: 'btn-warning', label: 'Warning'},
        {value: 'btn-dark', label: 'Dark'},
        {value: 'btn-ghost', label: 'Ghost'},
        {value: 'btn-tonal', label: 'Tonal'},
      ],
    },
    {
      id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', default: '', initial: 'save',
      options: ['', 'save', 'search', 'trash', 'plus', 'download', 'send', 'check'],
    },
    {
      id: 'size', type: 'segmented', label: 'Size', xml: 'size', default: '',
      options: [
        {value: '', label: 'Default'},
        {value: 'sm', label: 'Small'},
        {value: 'lg', label: 'Large'},
      ],
    },
    {id: 'iconOnly', type: 'switch', label: 'Icon only', xml: 'style', token: 'no-label', client: 'style', default: false},
    {id: 'help', type: 'text', label: 'Help tooltip', xml: 'help', default: ''},
    {id: 'disabled', type: 'switch', label: 'Disabled by a dependency', client: 'disabled', default: false},
  ],
  children: [
    {
      when: 'disabled',
      tag: 'dependency',
      attrs: {'target-type': 'disable', initial: 'true'},
      children: [{tag: 'dependency-element', attrs: {id: 'Role', condition: 'eq', value: 'viewer'}}],
    },
  ],
  events: [{name: 'click', description: 'Launched when the user clicks the button; it then runs its button actions.'}],
  actions: [
    {name: 'filter', description: 'Reloads a grid or a chart with the current criteria.'},
    {name: 'confirm', description: 'Asks the user to confirm before the following actions run.'},
    {name: 'server', description: 'Calls a server action (maintain, query, service).'},
    {name: 'dialog', description: 'Opens a dialog.'},
    {name: 'restore', description: 'What a reset button launches when it has no actions: restores the criteria.'},
  ],
};

module.exports = {button};
