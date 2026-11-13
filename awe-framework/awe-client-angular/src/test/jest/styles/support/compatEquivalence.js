const postcss = require("postcss");
const {normalizeLength} = require("./compileTailwind");
const {overlaps} = require("./importantGuard");
const {specificity} = require("./cascadeGuard");

const IMPORTANT = " !important";

/** Selector without the `::` of a pseudo-element: lightningcss and LESS write `:before` and `::before` */
const normalizeSelector = selector => selector.replace(/::(before|after)$/, ":$1");

/** Classes, ids and the `[data-toggle="buttons"]` marker that a selector names (classes without the dot, the others as written) */
const namesOf = selector => (selector.match(/\.-?[_a-zA-Z][\w-]*|#-?[_a-zA-Z][\w-]*|\[data-toggle="buttons"]/g) || [])
  .map(name => (name.startsWith(".") ? name.slice(1) : name));

/**
 * The Tailwind build (lightningcss) writes a color-mix() declaration as a plain fallback plus the same declaration in a nested
 * `@supports (color: color-mix(...))`. A browser with color-mix applies the nested one: model it by hoisting it over the fallback
 * @param {string} css Compiled Tailwind CSS
 * @return {string} CSS where the supported declarations replace their fallbacks
 */
const hoistSupported = css => {
  const root = postcss.parse(css);
  root.walkAtRules("supports", at => {
    if (at.parent.type === "rule") {
      at.walkDecls(declaration => {
        at.parent.walkDecls(declaration.prop, previous => previous !== declaration && previous.parent === at.parent && previous.remove());
        at.parent.append(declaration.clone());
      });
      at.remove();
    }
  });
  return root.toString();
};

/**
 * Effective declarations of the rules of a selector in a media query, as the cascade resolves them within one origin:
 * an `!important` value beats a normal one, otherwise the last one wins. --tw-* are Tailwind internals, not styling
 * @param {Array} rules Flattened rules
 * @param {string} selector Selector (pseudo-elements normalized)
 * @param {string} media Media query of the rules
 * @param {Function} [normalize] Brings a value to a comparable form
 * @return {Object<string, string>} Value by property, with the ` !important` suffix when it has one
 */
const effective = (rules, selector, media, normalize = normalizeLength) => Object.fromEntries(Object.entries(rules
  .filter(rule => normalizeSelector(rule.selector) === selector && rule.media === media)
  .reduce((merged, rule) => {
    Object.entries(rule.declarations).filter(([property]) => !property.startsWith("--tw-")).forEach(([property, value]) => {
      const important = value.endsWith(IMPORTANT);
      if (!(merged[property] && merged[property].important && !important)) {
        merged[property] = {important, value: normalize(value.replace(IMPORTANT, ""))};
      }
    });
    return merged;
  }, {})).map(([property, {important, value}]) => [property, important ? `${value}${IMPORTANT}` : value]));

/**
 * What the compat layer keeps of a declaration block since it reads the AWE 5 tokens: the property and whether it is !important, not the value
 * (the values follow the tokens, so they may differ from the compiled vendored ones)
 * @param {Object<string, string>} declarations Value by property, with the ` !important` suffix when it has one
 * @return {Object<string, boolean>} Importance by property
 */
const shape = declarations => Object.fromEntries(Object.entries(declarations).map(([property, value]) => [property, value.endsWith(IMPORTANT)]));

const mediasOf = (rules, selector) => [...new Set(rules.filter(rule => normalizeSelector(rule.selector) === selector).map(rule => rule.media))].sort();

/**
 * Where the cascade puts two rules of the family that can both match one element: same media query, same specificity, an
 * overlapping property and another value, so only the order in the stylesheet decides. Returns, for each such pair, which
 * selector wins in a stylesheet (the one with the last declaration of the property); a family keeps the vendored cascade when this
 * is the same for the vendored rules and for the compat ones
 * @param {Array} rules Flattened rules of a stylesheet, in order
 * @param {string[]} selectors Selectors of the family
 * @param {Function} normalize Brings a value to a comparable form
 * @param {Function} [meets] Whether one element can match both selectors (all pairs compete by default)
 * @return {Object<string, string>} Winner by `media|first|second|property` (first and second in alphabetical order)
 */
const orderWinners = (rules, selectors, normalize, meets = () => true) => {
  const own = new Set(selectors);
  const indexed = rules.map((rule, index) => ({...rule, selector: normalizeSelector(rule.selector), index}))
    .filter(rule => own.has(rule.selector));
  const winners = {};
  const lastOf = (rule, property) => Math.max(...indexed.filter(other => other.selector === rule.selector && other.media === rule.media
    && property in other.declarations && !other.declarations[property].endsWith(IMPORTANT)).map(other => other.index));
  // A property that any rule of the selector sets with !important is decided by the importance, not by the order
  const important = (rule, property) => indexed.some(other => other.selector === rule.selector && other.media === rule.media
    && property in other.declarations && other.declarations[property].endsWith(IMPORTANT));
  indexed.forEach(first => indexed.filter(second => second.media === first.media && second.selector > first.selector
    && specificity(second.selector) === specificity(first.selector) && meets(first.selector, second.selector)).forEach(second => {
    Object.entries(first.declarations).filter(([property]) => !important(first, property)).forEach(([property, value]) => {
      Object.entries(second.declarations).filter(([other, otherValue]) => !important(second, other) && overlaps(property, other)
        && normalize(value) !== normalize(otherValue)).forEach(([other]) => {
        const key = `${first.media}|${first.selector}|${second.selector}|${property}/${other}`;
        winners[key] = lastOf(first, property) > lastOf(second, other) ? first.selector : second.selector;
      });
    });
  }));
  return winners;
};

/**
 * The winners of the pairs that both builds see as competing. The compat values are --awe-* tokens, so two rules whose compiled values differed
 * can read the same token (no conflict any more) and the other way round: only the pairs both builds report are comparable, and each of them
 * has to be won by the same selector
 * @param {Object<string, string>} actual Winners in the compat layer (orderWinners)
 * @param {Object<string, string>} expected Winners in the vendored build
 * @return {{actual: Object<string, string>, expected: Object<string, string>}} The common pairs, to compare with toEqual
 */
const commonWinners = (actual, expected) => {
  const keys = Object.keys(actual).filter(key => key in expected);
  return {actual: Object.fromEntries(keys.map(key => [key, actual[key]])), expected: Object.fromEntries(keys.map(key => [key, expected[key]]))};
};

module.exports = {commonWinners, shape, IMPORTANT, normalizeSelector, namesOf, hoistSupported, effective, mediasOf, orderWinners};
