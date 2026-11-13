/**
 * Items of the menu breadcrumb of a live example: the window template draws the path of the current menu option, which
 * the menu of the application registers. The docs have no menu, so an example lists the labels of the path it shows.
 * Pure on purpose, so that node:test can run it.
 *
 * @param {string[]} [labels] Labels of the path, from the first level of the menu down to the screen
 * @returns {{label: string}[]} Items the breadcrumb of the template draws; blank and non-text labels are left out
 */
function breadcrumbItems(labels) {
  if (!Array.isArray(labels)) {
    return [];
  }
  return labels
    .filter(label => typeof label === 'string' && label.trim() !== '')
    .map(label => ({ label: label.trim() }));
}

/**
 * Clears the breadcrumb of the menu registry, but only when it still holds the items this example registered: the
 * registry is one for the whole page, so an example that unmounts after another one registered its own must not wipe it.
 * @param {{getBreadcrumbs: function(): {items: Array}, clear: function(): void}} registry Menu registry
 * @param {Array} items The items this example registered
 */
function releaseBreadcrumbs(registry, items) {
  if (registry.getBreadcrumbs().items === items) {
    registry.clear();
  }
}

module.exports = { breadcrumbItems, releaseBreadcrumbs };
