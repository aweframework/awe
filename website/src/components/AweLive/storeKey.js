const { stableKey } = require('./stableKey');

/**
 * Key of the store of a live example: it is rebuilt when the key changes. Only what feeds the state counts (the
 * components, the nodes that are rendered from them, the view, the settings, the simulated files, the predefined messages and the
 * answers of the server); the colour
 * scheme and the frame of the example do not touch the store.
 * Pure on purpose, so that node:test can run it.
 *
 * @param {{components?: Object, nodes?: Array, view?: string, settings?: Object, files?: Object, messages?: Object, answers?: Object}} props Props of AweLive
 * @returns {string} Stable key
 */
function storeKey({ components, nodes, view, settings, files, messages, answers }) {
  return stableKey({ components, nodes, view, settings, files, messages, answers });
}

module.exports = { storeKey };
