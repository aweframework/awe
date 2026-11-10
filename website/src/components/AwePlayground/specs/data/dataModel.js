/**
 * Helpers of the data playgrounds (grids, charts...): a column is written once, as the XML attributes, and the client
 * model the stage needs is derived from it. Free of React so that node:test can run it.
 */
const camelCase = name => name.replace(/-([a-z])/g, (match, letter) => letter.toUpperCase());

// Attributes that the client reads as numbers; the rest of the numeric-looking texts (a label "2024") stay text
const NUMERIC = new Set(['charlength', 'barMin', 'barMax', 'maxLines', 'max', 'zoom', 'latitude', 'longitude']);

/**
 * Client attributes of an XML element: camelCase names, true and false as booleans and the numeric ones as numbers.
 * @param {Object<string, string>} attrs XML attributes
 * @returns {Object} Attributes as the client reads them
 */
function attrsToClient(attrs) {
  return Object.fromEntries(Object.entries(attrs).map(([name, value]) => {
    const key = camelCase(name);
    if (value === 'true' || value === 'false') return [key, value === 'true'];
    return [key, NUMERIC.has(key) && value.trim() !== '' && !Number.isNaN(Number(value)) ? Number(value) : value];
  }));
}

/** `column` children for the XML of a grid. */
const columnsXml = columns => columns.map(column => ({tag: 'column', attrs: {...column}}));

/** Column model (`columnModel` attribute) of the client for the same columns. */
const columnsClient = columns => columns.map(attrsToClient);

/** Sum of a numeric field of the rows. */
const sumOf = (rows, field) => rows.reduce((total, row) => total + (typeof row[field] === 'number' ? row[field] : 0), 0);

module.exports = {attrsToClient, columnsXml, columnsClient, sumOf};
