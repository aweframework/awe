/**
 * Playground of the AWE grid. Every attribute is one the XSD declares for `grid` and `column` and the React client
 * reads. The rows are a fixture (data/customers.js): the XML says `load-all="true"`, so the grid selects, pages, sorts
 * and edits on the client, which is exactly what the stage runs. With a real query the server sends the same rows.
 * A playground spec is described in playgroundModel.js.
 */
const {customers} = require('./data/customers');
const {columnsClient, columnsXml, sumOf} = require('./data/dataModel');

// Columns, written once as XML attributes. A cell with an avatar, a second line or a status is read only: an editable
// grid gives its text columns component="text", the one that has an editor, and loses those extras
const columnsFor = values => [
  {
    name: 'name', label: 'Customer', sortable: 'true', charlength: '20',
    ...(values.editable ? {component: 'text'} : {'avatar-field': 'initials', 'secondary-field': 'email'}),
  },
  {
    name: 'plan', label: 'Plan', sortable: 'true', charlength: '8',
    ...(values.editable ? {component: 'text'} : {'status-field': 'planTone'}),
  },
  {name: 'country', label: 'Country', sortable: 'true', charlength: '10', ...(values.editable && {component: 'text'})},
  {
    name: 'mrr', label: 'MRR (EUR)', component: values.editable ? 'numeric' : 'progress', sortable: 'true',
    charlength: '11', align: 'right', ...(!values.editable && {'bar-max': '9500'}), 'summary-type': 'sum',
  },
  {name: 'trend', label: 'Last 6 months', component: 'sparkline', charlength: '11', 'sparkline-type': 'line', 'sparkline-last-point': 'true'},
];

const grid = {
  name: 'grid',
  element: 'Grid',
  tag: 'grid',
  id: 'demo',
  reference: {element: 'grid', page: 'screen'},
  api: '/docs/api/grids',
  stage: 'wide',
  layout: 'stacked',
  height: '44rem',
  controls: [
    {id: 'rows', type: 'segmented', label: 'Rows per page', xml: 'max', client: '', default: '', initial: '8', options: ['5', '8', '12']},
    {id: 'multiselect', type: 'switch', label: 'Multiple selection', xml: 'multiselect', default: false},
    {id: 'rowNumbers', type: 'switch', label: 'Row numbers', xml: 'row-numbers', default: false},
    {id: 'enableFilters', type: 'switch', label: 'Column filters', xml: 'enable-filters', default: false},
    {id: 'showTotals', type: 'switch', label: 'Totals row', xml: 'show-totals', default: false},
    {id: 'editable', type: 'switch', label: 'Editable rows', xml: 'editable', default: false},
    {
      id: 'groupBy', type: 'segmented', label: 'Group by', xml: 'group-by', default: '',
      when: {id: 'editable', in: [false]},
      options: [
        {value: '', label: 'None'},
        {value: 'plan', label: 'Plan'},
        {value: 'country', label: 'Country'},
      ],
    },
  ],
  xmlAttributes: values => [
    ['initial-load', 'query'],
    ['target-action', 'Customers'],
    ['load-all', 'true'],
    ...(values.editable ? [['validate-on-save', 'false']] : []),
  ],
  children: columnsXml(columnsFor({})).map((child, index) => ({...child, attrs: values => columnsFor(values)[index]})),
  scene: values => ({
    attributes: {
      max: Number(values.rows),
      loadAll: true,
      columnModel: columnsClient(columnsFor(values)),
      headerModel: [],
      buttonModel: [],
      ...(values.editable && {validateOnSave: false}),
    },
    model: {
      values: customers,
      records: customers.length,
      footer: {mrr: {label: `${sumOf(customers, 'mrr').toLocaleString('en-US')}`}},
    },
  }),
  events: [
    {name: 'select-row', description: 'A row is selected: dependencies and actions that listen to the grid run.'},
  ],
  actions: [
    {name: 'select-row', description: 'Selects the rows whose identifiers are in the value.'},
    {name: 'select-all-rows', description: 'Selects every row; unselect-all-rows clears the selection.'},
    {name: 'add-row', description: 'Adds a row (add-row-top and add-row-bottom choose where).'},
    {name: 'delete-row', description: 'Deletes the selected rows.'},
    {name: 'filter', description: 'Reloads the grid from its query, with the criteria of the screen.'},
  ],
};

/**
 * Props of an AweLive example with the customers grid: `<AweLive height="28rem" {...gridExample({multiselect: true})} />`.
 * @param {Object} [attributes] Client attributes of the grid (camelCase), over the loaded-all columns of the playground
 * @param {Object} [model] Model of the grid, over the customers
 */
const gridExample = (attributes = {}, model = {}) => ({
  components: {
    grid: {
      attributes: {max: 8, loadAll: true, columnModel: columnsClient(columnsFor({editable: attributes.editable})), headerModel: [], buttonModel: [], ...attributes},
      model: {values: customers, records: customers.length, ...model},
    },
  },
  nodes: [{elementType: 'Grid', id: 'grid'}],
});

module.exports = {grid, gridExample};
