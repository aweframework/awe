/**
 * Sample organisation of the tree grid examples: invented departments with a head count and a yearly budget. A tree grid
 * reads the hierarchy from two fields of each row: its identifier and the identifier of its parent (empty for the roots),
 * plus a flag that tells whether the row is a leaf.
 */
const NODES = [
  // [id, parent, name, owner, people, budget]
  [1, '', 'Engineering', 'Lucia Ferrer', 64, 5200],
  [2, 1, 'Platform', 'Jonas Keller', 24, 2100],
  [3, 2, 'Infrastructure', 'Tiago Matos', 12, 1100],
  [4, 2, 'Developer tools', 'Marta Ruiz', 12, 1000],
  [5, 1, 'Product', 'Claire Dubois', 40, 3100],
  [6, 5, 'Web', 'Ingrid Berg', 22, 1700],
  [7, 5, 'Mobile', 'Paolo Rossi', 18, 1400],
  [8, '', 'Operations', 'Oliver Grant', 38, 2400],
  [9, 8, 'Customer support', 'Elena Soler', 26, 1300],
  [10, 8, 'Logistics', 'Daan Visser', 12, 1100],
  [11, '', 'Commercial', 'Pablo Navarro', 45, 3800],
  [12, 11, 'Sales', 'Ines Costa', 29, 2300],
  [13, 12, 'Enterprise accounts', 'Hugo Martin', 17, 1500],
  [14, 12, 'Partners', 'Giulia Conti', 12, 800],
  [15, 11, 'Marketing', 'Amelia Hart', 16, 1500],
];

const parents = new Set(NODES.map(node => node[1]));

/** Rows of the tree grid: `id`, `parent` and `isLeaf` build the tree, `name` is the column that holds the toggles. */
const departments = NODES.map(([id, parent, name, owner, people, budget]) => ({
  id, parent, name, owner, people, budget, isLeaf: !parents.has(id),
}));

module.exports = {departments};
