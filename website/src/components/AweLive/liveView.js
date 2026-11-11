// View of the store of a live example. Pure on purpose, so that node:test can run it.

/**
 * Every example gets a view of its own. The client keeps one registry of components for the page and finds the members
 * of a radio group, the target of an upload or the components a screen clears by their view: with a shared view, two
 * examples with the same group (or the same ids) would reach into each other.
 * @param {string} [view] View the example names (default `live`)
 * @param {number} instance Number of the example on the page
 * @returns {string} View of the store of the example
 */
function liveView(view, instance) {
  return `${view ?? 'live'}-${instance}`;
}

module.exports = {liveView};
