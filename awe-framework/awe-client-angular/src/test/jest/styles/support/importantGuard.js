const fs = require("fs");
const {sourceFiles} = require("./xmlScreens");

const IMPORTANT = " !important";

const normalizeSelector = selector => selector.replace(/::(before|after)$/, ":$1");
const classOf = selector => (selector.match(/\.-?[_a-zA-Z][\w-]*/g) || []).map(name => name.slice(1));
const subject = selector => selector.split(/[\s>+~]+/).pop();

const SIDES = ["top", "right", "bottom", "left"];
const CORNERS = ["top-left", "top-right", "bottom-right", "bottom-left"];
const BORDER_PARTS = ["width", "style", "color"];

/**
 * The longhand properties that a declaration sets, following the CSS shorthand expansion:
 * `padding|margin` to their sides, `border` to every `border-{side}-{width|style|color}`, `border-{side}` to its three,
 * `border-{width|style|color}` to their four sides and `border-radius` to its four corners
 * @param {string} property CSS property
 * @return {string[]} Longhands (the property itself when it is one, or unknown)
 */
const longhands = property => {
  const side = property.match(/^border-(top|right|bottom|left)$/);
  const part = property.match(/^border-(width|style|color)$/);
  if (/^(padding|margin)$/.test(property)) {
    return SIDES.map(name => `${property}-${name}`);
  }
  if (property === "border") {
    return SIDES.flatMap(name => BORDER_PARTS.map(kind => `border-${name}-${kind}`));
  }
  if (side) {
    return BORDER_PARTS.map(kind => `border-${side[1]}-${kind}`);
  }
  if (part) {
    return SIDES.map(name => `border-${name}-${part[1]}`);
  }
  return property === "border-radius" ? CORNERS.map(corner => `border-${corner}-radius`) : [property];
};

/**
 * Whether one declaration can override the other: they set at least one longhand in common (the same property, a shorthand and
 * one of its longhands, two shorthands that share a longhand), while the sides, corners and parts of a border are independent
 * @param {string} first CSS property
 * @param {string} second CSS property
 * @return {boolean} True when they overlap
 */
const overlaps = (first, second) => {
  const covered = longhands(second);
  return longhands(first).some(property => covered.includes(property));
};

/**
 * Classes written in the same string as each class of the list, in every template, directive and XML screen
 * @param {string[]} classes Class names
 * @return {Object<string, Set<string>>} Neighbour class names by class
 */
const neighbours = classes => sourceFiles().reduce((found, file) => {
  [...fs.readFileSync(file, "utf8").matchAll(/["'`]([^"'`\n]*)["'`]/g)].forEach(match => {
    const words = match[1].split(/\s+/).filter(Boolean);
    words.filter(word => classes.includes(word)).forEach(word => words.forEach(other => (found[word] = found[word] || new Set()).add(other)));
  });
  return found;
}, {});

/**
 * For !important the cascade layer order inverts: a layered !important beats every unlayered !important, whatever its
 * specificity. So the !important declarations of the compat classes are not inert next to the vendored LESS: they win over an
 * unlayered `.foo { padding: 3px !important }` on an element that carries both classes. This finds the vendored rules that could
 * clash (another class, !important, an overlapping property with another value) and which of them share a string with the class
 * @param {Array} vendored Rules of the compiled vendored LESS
 * @param {Array} compat Rules of the compiled Tailwind output
 * @param {string[]} classes Class names of the compat family
 * @param {Function} selectorsOf Selectors that a class name has
 * @param {Function} [normalize] Brings a value of either side to a comparable form (identity by default)
 * @return {{important: Array, candidates: Array, conflicts: Array}} Declarations with !important, possible clashes and real ones
 */
const importantConflicts = (vendored, compat, classes, selectorsOf, normalize = value => value) => {
  const own = new Set(classes.flatMap(selectorsOf));
  const important = compat.filter(rule => own.has(normalizeSelector(rule.selector)))
    .flatMap(rule => Object.entries(rule.declarations).filter(([, value]) => value.endsWith(IMPORTANT))
      .map(([property, value]) => ({selector: rule.selector, property, value: normalize(value.replace(IMPORTANT, ""))})));
  const candidates = [];
  vendored.filter(rule => !/::?(before|after)/.test(rule.selector) && !/-(add|remove)(-active)?$/.test(subject(rule.selector)))
    .forEach(rule => {
      const tokens = classOf(subject(rule.selector)).filter(token => !classes.includes(token));
      if (tokens.length === 0) {
        return;
      }
      important.forEach(utility => {
        Object.entries(rule.declarations).filter(([property, value]) => value.endsWith(IMPORTANT) && overlaps(property, utility.property)
          && normalize(value.replace(IMPORTANT, "")) !== utility.value)
          .forEach(([property]) => candidates.push({utility: classOf(utility.selector)[0], rule: rule.selector, property, tokens}));
      });
    });
  const found = neighbours(classes);
  const conflicts = candidates.filter(candidate => candidate.tokens.some(token => found[candidate.utility] && found[candidate.utility].has(token)));
  return {important, candidates, conflicts};
};

module.exports = {importantConflicts, overlaps};
