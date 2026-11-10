/**
 * Playground of the AWE tree grid: a grid with the attribute `treegrid`. Every attribute is one the XSD declares for
 * `grid` and `column` and the React client reads. The rows are a fixture (data/departments.js) with `load-all`, so the
 * branches open and close on the client; without it a branch is asked to the server when it is opened.
 * A playground spec is described in playgroundModel.js.
 */
const {departments} = require('./data/departments');
const {columnsClient, columnsXml} = require('./data/dataModel');

const COLUMNS = [
  {name: 'name', label: 'Department', component: 'text', charlength: '24'},
  {name: 'owner', label: 'Head', component: 'text', charlength: '14'},
  {name: 'people', label: 'People', component: 'text', charlength: '8', align: 'right'},
  {name: 'budget', label: 'Budget (kEUR)', component: 'progress', charlength: '14', align: 'right', 'bar-max': '5500', 'bar-leaf-only': 'true'},
];

const ICONS = {
  plus: {expand: 'plus-square', collapse: 'minus-square'},
  chevron: {expand: 'chevron-right', collapse: 'chevron-down'},
};

const treeGrid = {
  name: 'tree-grid',
  element: 'TreeGrid',
  tag: 'grid',
  id: 'demo',
  reference: {element: 'grid', page: 'screen'},
  api: '/docs/api/grids',
  stage: 'wide',
  layout: 'stacked',
  height: '30rem',
  controls: [
    {
      id: 'initialLevel', type: 'segmented', label: 'Levels shown at start', xml: 'initial-level', client: '', default: '', initial: '2',
      options: [
        {value: '1', label: 'Roots only'},
        {value: '2', label: 'Two levels'},
        {value: '3', label: 'All levels'},
      ],
    },
    {
      id: 'icons', type: 'segmented', label: 'Toggle icons', client: '', writes: 'icon-expand and icon-collapse', default: 'plus', initial: 'plus',
      options: [
        {value: 'plus', label: 'Plus and minus'},
        {value: 'chevron', label: 'Chevrons'},
      ],
    },
    {id: 'multiselect', type: 'switch', label: 'Multiple selection', xml: 'multiselect', default: false},
  ],
  xmlAttributes: values => [
    ['treegrid', 'true'],
    ['tree-id', 'id'],
    ['tree-parent', 'parent'],
    ['tree-leaf', 'isLeaf'],
    ['expand-column', 'name'],
    ...(values.icons === 'chevron' ? [['icon-expand', ICONS.chevron.expand], ['icon-collapse', ICONS.chevron.collapse]] : []),
    ['initial-load', 'query'],
    ['target-action', 'Departments'],
    ['load-all', 'true'],
  ],
  children: columnsXml(COLUMNS),
  scene: values => ({
    attributes: {
      treegrid: true,
      loadAll: true,
      treeId: 'id',
      treeParent: 'parent',
      treeLeaf: 'isLeaf',
      expandColumn: 'name',
      initialLevel: Number(values.initialLevel),
      ...(values.icons === 'chevron' && {iconExpand: ICONS.chevron.expand, iconCollapse: ICONS.chevron.collapse}),
      columnModel: columnsClient(COLUMNS),
      headerModel: [],
      buttonModel: [],
    },
    model: {values: departments, records: departments.length},
  }),
  events: [
    {name: 'select-row', description: 'A row is selected: dependencies and actions that listen to the grid run.'},
  ],
  actions: [
    {name: 'tree-branch', description: 'Opens or closes a branch. Without load-all, opening a branch asks the server for its rows.'},
    {name: 'select-row', description: 'Selects the rows whose identifiers are in the value.'},
    {name: 'filter', description: 'Reloads the tree from its query.'},
  ],
};

/**
 * Props of an AweLive example with the departments tree grid.
 * @param {Object} [attributes] Client attributes of the grid (camelCase), over the ones of a tree grid
 */
const treeGridExample = (attributes = {}) => ({
  components: {
    grid: {
      attributes: {
        treegrid: true, loadAll: true, treeId: 'id', treeParent: 'parent', treeLeaf: 'isLeaf', expandColumn: 'name', initialLevel: 2,
        columnModel: columnsClient(COLUMNS), headerModel: [], buttonModel: [], ...attributes,
      },
      model: {values: departments, records: departments.length},
    },
  },
  nodes: [{elementType: 'TreeGrid', id: 'grid'}],
});

module.exports = {treeGrid, treeGridExample};
