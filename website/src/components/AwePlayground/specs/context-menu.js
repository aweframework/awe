/**
 * Playground of the AWE context menu: the `context-button` and `context-separator` elements a button declares. The menu
 * opens with a right click on the button (or with the menu key of the keyboard), or with a left click when the button has a
 * `show-context-menu` action. Every attribute is one the XSD declares and the React client reads. Each option writes the
 * name of the option in a read-only criterion with a `value` action, so the reader sees what the menu did; the actions
 * of an option are the ones of any button.
 * A playground spec is described in playgroundModel.js.
 */
const ICONS = {Edit: 'lucide:pencil', Duplicate: 'lucide:copy', Export: 'lucide:download', Delete: 'lucide:trash-2'};
const EXPORTS = [['ExportCsv', 'CSV'], ['ExportPdf', 'PDF']];

// What an option does: it writes its label in the criterion that shows the last option chosen
const writes = label => ({type: 'value', target: 'LastAction', value: label});

const optionAttrs = (id, label, values) => ({id, label, ...(values.icons && ICONS[id] && {icon: ICONS[id]})});
const optionChild = (id, label) => ({
  tag: 'context-button',
  attrs: values => optionAttrs(id, label, values),
  children: [{tag: 'button-action', attrs: writes(label)}],
});

const contextMenu = {
  name: 'context-menu',
  element: 'Button',
  tag: 'button',
  id: 'demo',
  reference: {element: 'context-button', page: 'screen'},
  api: '/docs/api/context-menu',
  controls: [
    {id: 'label', type: 'text', label: 'Button label', xml: 'label', default: '', initial: 'Customer actions'},
    {
      id: 'opens', type: 'segmented', label: 'Opens with', client: '', default: 'right',
      options: [
        {value: 'right', label: 'Right click'},
        {value: 'click', label: 'Left click'},
      ],
    },
    {id: 'icons', type: 'switch', label: 'Icons', client: '', writes: 'context-button', default: true},
    {id: 'submenu', type: 'switch', label: 'Export submenu', client: '', writes: 'context-button', default: false},
    {id: 'separator', type: 'switch', label: 'Separator before Delete', client: '', writes: 'context-separator', default: true},
  ],
  children: [
    {when: {id: 'opens', in: ['click']}, tag: 'button-action', attrs: {type: 'show-context-menu'}},
    optionChild('Edit', 'Edit'),
    optionChild('Duplicate', 'Duplicate'),
    {
      when: 'submenu',
      tag: 'context-button',
      attrs: values => optionAttrs('Export', 'Export', values),
      children: EXPORTS.map(([id, label]) => optionChild(id, label)),
    },
    {when: 'separator', tag: 'context-separator', attrs: {}},
    optionChild('Delete', 'Delete'),
  ],
  siblings: [{tag: 'criteria', attrs: {id: 'LastAction', component: 'text', label: 'Last option chosen', readonly: 'true'}}],
  scene: values => {
    const option = (id, label, extra = {}) => ({
      [id]: {attributes: {...optionAttrs(id, label, values), ...extra, ...(!extra.contextMenu && {actions: [writes(label)]})}},
    });
    const menu = [
      {id: 'Edit'}, {id: 'Duplicate'},
      ...(values.submenu ? [{id: 'Export'}] : []),
      ...(values.separator ? [{id: 'Sep'}] : []),
      {id: 'Delete'},
    ];
    return {
      attributes: {contextMenu: menu},
      components: {
        ...option('Edit', 'Edit'),
        ...option('Duplicate', 'Duplicate'),
        ...(values.submenu ? option('Export', 'Export', {contextMenu: EXPORTS.map(([id]) => ({id}))}) : {}),
        ...(values.submenu ? Object.assign({}, ...EXPORTS.map(([id, label]) => option(id, label))) : {}),
        ...(values.separator ? {Sep: {attributes: {separator: true}}} : {}),
        ...option('Delete', 'Delete'),
        LastAction: {attributes: {label: 'Last option chosen', readonly: true}},
      },
      nodes: [{elementType: 'Criteria', component: 'text', id: 'LastAction'}],
      ...(values.opens === 'click' && {actions: [{type: 'show-context-menu'}]}),
    };
  },
  events: [{name: 'click', description: 'Stored in the option of the menu when the user chooses it.'}],
  actions: [
    {name: 'show-context-menu', description: 'Opens the context menu under the button that launches it, with a left click.'},
  ],
};

module.exports = {contextMenu};
