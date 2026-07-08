/**
 * Menu search helpers.
 *
 * Pure, framework-agnostic logic for the menu command-palette search: it walks
 * the (potentially deep) menu option tree, collects the launchable leaf options
 * with their translated breadcrumb path, and matches them against a query.
 *
 * Translation and allowance are injected as dependencies (translate, isAllowed)
 * so this module stays free of Angular and trivially unit-testable.
 */

/**
 * Normalize a text for case- and accent-insensitive matching.
 * @param {string} text Text to normalize
 * @returns {string} Lowercased, accent-stripped text
 */
export function normalizeText(text) {
  return String(text == null ? "" : text)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "");
}

/**
 * Resolve the translated label of an option (falling back to its name).
 * @param {object} option Menu option
 * @param {function} translate Translation function (key -> visible text)
 * @returns {string} Translated label
 */
function optionLabel(option, translate) {
  return translate(option.label != null ? option.label : (option.name || ""));
}

/**
 * Retrieve the allowed, non-separator children of an option.
 * @param {object} option Menu option
 * @param {function} isAllowed Allowance predicate
 * @returns {Array} Allowed children
 */
function allowedChildren(option, isAllowed) {
  return (option.options || []).filter(child => !child.separator && isAllowed(child));
}

/**
 * Flatten a menu option tree into its launchable leaf options.
 *
 * A leaf is any allowed, non-separator option that has no allowed children
 * (mirroring the menu's own "has visible children" branch/leaf decision).
 * Container options are not returned themselves, but their translated label
 * becomes part of the breadcrumb path of every descendant leaf.
 *
 * @param {Array} options Option list (tree root or subtree)
 * @param {object} deps Dependencies
 * @param {function} deps.translate Translation function (key -> visible text)
 * @param {function} [deps.isAllowed] Allowance predicate (defaults to allow all)
 * @returns {Array<{option: object, path: string[]}>} Leaf options with breadcrumb paths
 */
export function flattenMenuOptions(options, {translate, isAllowed = () => true} = {}) {
  const result = [];

  const walk = (list, parentPath) => {
    (list || []).forEach(option => {
      if (option.separator || !isAllowed(option)) {
        return;
      }
      const path = parentPath.concat(optionLabel(option, translate));
      const children = allowedChildren(option, isAllowed);
      if (children.length > 0) {
        walk(option.options, path);
      } else {
        result.push({option, path});
      }
    });
  };

  walk(options, []);
  return result;
}

/**
 * Search the menu option tree for leaf options matching a query.
 *
 * Matching is case- and accent-insensitive, token-based (every whitespace
 * separated token must appear), and runs against both the translated
 * breadcrumb path and the option name. A blank query returns no results.
 *
 * @param {Array} options Option list (tree root)
 * @param {string} query Search query
 * @param {object} deps Dependencies
 * @param {function} deps.translate Translation function (key -> visible text)
 * @param {function} [deps.isAllowed] Allowance predicate
 * @returns {Array<{option: object, path: string[]}>} Matching leaf options with breadcrumb paths
 */
export function searchOptions(options, query, {translate, isAllowed = () => true} = {}) {
  const normalizedQuery = normalizeText(query).trim();
  if (!normalizedQuery) {
    return [];
  }

  const tokens = normalizedQuery.split(/\s+/);
  return flattenMenuOptions(options, {translate, isAllowed}).filter(({option, path}) => {
    const haystack = normalizeText(path.join(" ") + " " + (option.name || ""));
    return tokens.every(token => haystack.includes(token));
  });
}
