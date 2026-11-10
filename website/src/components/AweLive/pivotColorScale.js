// Colour scale of the pivot table heatmaps, drawn with the data visualisation tokens of the design system
// (--awe-color-scale-1..5 with their text --awe-color-on-scale-1..5), which keep the contrast in light and dark.
// Pure on purpose, so that node:test can run it.

const STEPS = 5;

/**
 * Step (1..5) of a value inside the range of its scale: the range is cut in five equal parts.
 * @param {number} value Value of the cell
 * @param {number} min Lowest value of the scale
 * @param {number} max Highest value of the scale
 * @returns {number} Step from 1 (lowest) to 5 (highest); 1 when the scale has no range
 */
function scaleStep(value, min, max) {
  if (!(max > min)) {
    return 1;
  }
  const position = Math.floor(((value - min) / (max - min)) * STEPS);
  return Math.min(STEPS - 1, Math.max(0, position)) + 1;
}

/**
 * Same contract as the default scale of react-pivottable (tableColorScaleGenerator): values in, a function that gives the
 * style of a cell out. The style names tokens, never a colour value.
 * @param {number[]} values Values of the scale
 * @returns {function(number): {backgroundColor: string, color: string}} Style of a cell
 */
function tokenColorScaleGenerator(values) {
  const min = Math.min(...values);
  const max = Math.max(...values);
  return value => {
    const step = scaleStep(value, min, max);
    return {backgroundColor: `var(--awe-color-scale-${step})`, color: `var(--awe-color-on-scale-${step})`};
  };
}

/**
 * The pivot tables among the nodes and, at any depth, among the children of a container (a card, a tab, a window...).
 * @param {Array<{elementType: string, id: string, elementList?: Array}>} [nodes] Rendered nodes
 * @returns {Array<{elementType: string, id: string}>} Pivot table nodes
 */
function pivotNodes(nodes) {
  return (nodes ?? []).flatMap(node => [
    ...(node.elementType === 'PivotTable' ? [node] : []),
    ...pivotNodes(node.elementList),
  ]);
}

/**
 * Gives a generator to the pivot tables of the described nodes, nested ones included. The client keeps it in the attributes of the component
 * (tableColorScaleGenerator) and hands it to react-pivottable; the description itself stays untouched.
 * @param {Object} components Description of the components by id
 * @param {Array<{elementType: string, id: string}>} [nodes] Rendered nodes
 * @param {Function} generator Colour scale generator
 * @returns {Object} Components, a copy when a pivot table was changed
 */
function withPivotColorScale(components, nodes, generator) {
  const pivots = pivotNodes(nodes).filter(node => components?.[node.id]);
  if (!pivots.length) {
    return components;
  }
  const result = {...components};
  for (const {id} of pivots) {
    result[id] = {...components[id], attributes: {...components[id].attributes, tableColorScaleGenerator: generator}};
  }
  return result;
}

module.exports = {scaleStep, tokenColorScaleGenerator, withPivotColorScale};
