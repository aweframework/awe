/**
 * Parse the custom property declarations of a CSS block body
 * @param {string} body Declarations
 * @return {Object<string, string>} Variables by name
 */
const parseDeclarations = body => body.split(";").reduce((variables, declaration) => {
  const separator = declaration.indexOf(":");
  const name = declaration.slice(0, separator).trim();
  return name.startsWith("--") ? {...variables, [name]: declaration.slice(separator + 1).trim()} : variables;
}, {});

/**
 * Collect the custom properties declared by the plain rules with exactly the given selector (the rules that only
 * declare other properties are ignored)
 * @param {string} css Compiled CSS
 * @param {string} selector Selector
 * @return {Object<string, string>|null} Variables or null when the rule does not exist
 */
const getRuleVariables = (css, selector) => {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\:]/g, "\\$&").replace(/,\s*/g, ",\\s*");
  const regex = new RegExp(`(?:^|[};/])\\s*${escaped}\\s*\\{([^{}]*)\\}`, "gm");
  let variables = null;
  let match;
  while ((match = regex.exec(css)) !== null) {
    const declared = parseDeclarations(match[1]);
    if (Object.keys(declared).length > 0) {
      variables = {...variables, ...declared};
    }
  }
  return variables;
};

/**
 * Collect the variables a theme declares for the OS dark preference
 * @param {string} css Compiled CSS
 * @param {string} theme Theme name
 * @return {Object<string, string>|null} Variables or null when there is no media rule for the theme
 */
const getOsDarkVariables = (css, theme) => {
  const selector = `.theme-${theme}:not(.light):not(.dark)`.replace(/[.*+?^${}()|[\]\\:]/g, "\\$&");
  const match = new RegExp(`@media \\(prefers-color-scheme:\\s*dark\\)\\s*\\{[^@]*?${selector}\\s*\\{([^{}]*)\\}`).exec(css);
  return match ? parseDeclarations(match[1]) : null;
};

/**
 * Names of the themes the stylesheet defines, in definition order
 * @param {string} css Compiled CSS
 * @return {string[]} Theme names
 */
const getThemeNames = css => {
  const names = [];
  const regex = /(?:^|[};/])\s*\.theme-([a-z-]+)\s*\{/gm;
  let match;
  while ((match = regex.exec(css)) !== null) {
    if (!names.includes(match[1])) {
      names.push(match[1]);
    }
  }
  return names;
};

/**
 * Snapshot of the variables of every theme (light and dark)
 * @param {string} css Compiled CSS
 * @return {Object} Variables by theme and mode
 */
const getThemeSnapshot = css => getThemeNames(css).reduce((snapshot, name) => ({
  ...snapshot,
  [name]: {light: getRuleVariables(css, `.theme-${name}`), dark: getRuleVariables(css, `.theme-${name}.dark`)}
}), {});

module.exports = {getRuleVariables, getOsDarkVariables, getThemeNames, getThemeSnapshot};
