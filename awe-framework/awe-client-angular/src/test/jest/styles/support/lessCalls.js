/** Names a LESS source extends (`&:extend(.name)`) */
const extendsOf = source => [...source.matchAll(/:extend\(\s*\.([\w-]+)/g)].map(match => match[1]);

/**
 * Names a LESS source calls as a mixin or rule set: `.name;`, `.name();`, optionally `!important`. A call is a statement: it starts a
 * line or follows `{`, `;` or `}` on the same line, and ends with `;` or with the `}` that closes its block
 */
const callsOf = source => [...source.matchAll(/(?:^|[{;}])\s*\.([\w-]+)\s*(?:\(\s*\))?\s*(?:!important)?\s*(?=[;}])/gm)].map(match => match[1]);

/**
 * Rule sets that a LESS source defines on their own: `.name {` alone or in a selector list (`.a, .b {`), not a mixin (`.name() {`),
 * a compound selector (`.a .b`), nor one with a pseudo-class (`.a:hover`)
 * @param {string} source LESS source
 * @return {Set<string>} Class names, without the dot
 */
const ruleSetsOf = source => new Set([...source.replace(/\/\*[\s\S]*?\*\/|\/\/.*$/gm, "").matchAll(/(?:^|[;}])\s*([^{};@]+?)\s*\{/g)]
  .flatMap(match => match[1].split(",").map(selector => selector.trim()))
  .filter(selector => /^\.[\w-]+$/.test(selector)).map(selector => selector.slice(1)));

/**
 * Extends of a rule set that a removal took out and that no leftover keeps: Less drops them without a warning
 * @param {string} source LESS source
 * @param {Set<string>} removed Names of the removed rule sets
 * @param {Set<string>} kept Names of the rule sets that the leftovers define (see ruleSetsOf)
 * @return {Array<string>} Names extended
 */
const unkeptExtends = (source, removed, kept) => extendsOf(source).filter(name => removed.has(name) && !kept.has(name));

module.exports = {extendsOf, callsOf, ruleSetsOf, unkeptExtends};
