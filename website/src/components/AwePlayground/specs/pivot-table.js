/**
 * Playground of the AWE pivot table. Every attribute is one the XSD declares for `pivot-table` and the React client
 * reads (react-pivottable draws it). The rows are a fixture (data/sales.js). The reader of a real screen can also drag the
 * attributes between the rows and the columns and pick the renderer and the aggregator in the table itself.
 * A playground spec is described in playgroundModel.js.
 */
const {sales} = require('./data/sales');

// The pivot table offers every field of the rows as an attribute: the identifier of the row is not one
const rows = sales.map(({id, ...row}) => row);

const NEEDS_FIELD = ['Custom Sum', 'Custom Average', 'Custom Maximum', 'Sum as Fraction of Total'];

const pivotTable = {
  name: 'pivot-table',
  element: 'PivotTable',
  tag: 'pivot-table',
  id: 'demo',
  reference: {element: 'pivot-table', page: 'screen'},
  api: '/docs/api/pivot-table',
  stage: 'wide',
  layout: 'stacked',
  height: '30rem',
  controls: [
    {
      id: 'rows', type: 'segmented', label: 'Rows', xml: 'rows', default: '', initial: 'Region',
      options: ['Region', 'Product'],
    },
    {
      id: 'cols', type: 'segmented', label: 'Columns', xml: 'cols', default: '', initial: 'Quarter',
      options: ['Quarter', 'Channel'],
    },
    {
      id: 'renderer', type: 'select', label: 'Renderer', xml: 'renderer', default: '', initial: 'Heatmap',
      options: [
        {value: 'Table', label: 'Table'},
        {value: 'Heatmap', label: 'Heatmap'},
        {value: 'Row Heatmap', label: 'Row heatmap'},
        {value: 'Col Heatmap', label: 'Column heatmap'},
        {value: 'Table Barchart', label: 'Table with bars'},
      ],
    },
    {
      id: 'aggregator', type: 'select', label: 'Aggregator', xml: 'aggregator', default: '', initial: 'Custom Sum',
      options: [
        {value: 'Count', label: 'Count'},
        {value: 'Custom Sum', label: 'Sum'},
        {value: 'Custom Average', label: 'Average'},
        {value: 'Custom Maximum', label: 'Maximum'},
        {value: 'Sum as Fraction of Total', label: 'Sum as fraction of total'},
      ],
    },
    {
      id: 'field', type: 'segmented', label: 'Field to aggregate', xml: 'aggregation-field', client: 'aggregationField', default: '', initial: 'Revenue',
      when: {id: 'aggregator', in: NEEDS_FIELD},
      options: ['Revenue', 'Units'],
    },
  ],
  xmlAttributes: () => [['decimal-numbers', '0'], ['initial-load', 'query'], ['target-action', 'Sales']],
  scene: () => ({
    attributes: {decimalNumbers: 0},
    model: {values: rows},
  }),
  events: [],
  actions: [
    {name: 'set-pivot-sorters', description: 'Sets the order of the values of an attribute (rows and columns).'},
    {name: 'set-pivot-group-rows', description: 'Changes the attributes of the rows; set-pivot-group-cols does it for the columns.'},
    {name: 'filter', description: 'Reloads the pivot table from its query, with the criteria of the screen.'},
  ],
};

/**
 * Props of an AweLive example with the sales pivot table.
 * @param {Object} [attributes] Client attributes of the pivot table (camelCase), over the whole numbers of the playground
 */
const pivotTableExample = (attributes = {}) => ({
  components: {pivot: {attributes: {rows: 'Region', cols: 'Quarter', decimalNumbers: 0, ...attributes}, model: {values: rows}}},
  nodes: [{elementType: 'PivotTable', id: 'pivot'}],
});

module.exports = {pivotTable, pivotTableExample};
