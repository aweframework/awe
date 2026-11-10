/**
 * Mark the actions of a menu option as coming from the menu: they do not send the values of the screen the menu
 * leaves, and the screen opens fresh. Every way of launching an option (menu, search, breadcrumb) uses it
 * @param {object[]} actions Actions of the option
 * @returns {object[]} Actions coming from the menu
 */
export const stampMenuOrigin = (actions) => actions.map(action => ({...action, origin: "menu"}));

/**
 * Retrieve the names of the options from the first level down to the given option
 * @param {object[]} optionList Option list
 * @param {string} name Option name
 * @returns {string[]|null} Option names of the path, or null when the option is not in the list
 */
export function findOptionPath(optionList, name) {
  for (const option of optionList || []) {
    if (option.name === name) {
      return [option.name];
    }
    const path = findOptionPath(option.options, name);
    if (path) {
      return [option.name, ...path];
    }
  }
  return null;
}
