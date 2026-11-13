/**
 * Serialization of a value that does not depend on the order of the keys, and ignores functions and undefined values
 * (as JSON does). Two descriptions with the same content give the same key even when they are different objects, which
 * is what lets a store be rebuilt only when the content of a live example really changes.
 * Pure on purpose, so that node:test can run it.
 *
 * @param {*} value JSON-like value
 * @returns {string} Stable key
 */
function stableKey(value) {
  return JSON.stringify(value, (key, item) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      return Object.fromEntries(Object.keys(item).sort().map(name => [name, item[name]]));
    }
    return item;
  });
}

module.exports = { stableKey };
