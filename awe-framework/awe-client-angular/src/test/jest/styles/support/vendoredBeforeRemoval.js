const fs = require("fs");
const path = require("path");
const {compileStyles, LESS_DIR} = require("./compileStyles");
const {getRules} = require("./compileTailwind");

const BASELINE = path.join(__dirname, "vendored-removed.baseline.json");

/** Short identity of a flattened rule: selector, media query and declarations */
const hashOf = rule => require("crypto").createHash("sha1")
  .update(`${rule.selector}|${rule.media}|${JSON.stringify(rule.declarations)}`).digest("hex").slice(0, 10);

/**
 * Compare a build with the vendored rules before a removal and list the rules that the removal took out.
 * The build after the removal must hold the same rules but those (nothing may be added or change; order and duplicates may differ)
 * @param {Array} before Flattened rules (see getRules) before the removal
 * @param {Array} after Flattened rules after the removal
 * @return {Array<{at: number, after: string|null, selector: string, media: string, declarations: object}>} Removed rules in
 * source order: `at` is the position in the build after the removal where it goes back and `after` the identity of the rule right before it
 */
const diffRemoved = (before, after) => {
  // Where each rule is in the build after: a rule can be there more than once, and the one that matters is the next after the previous match
  const places = new Map();
  after.forEach((rule, index) => {
    const hash = hashOf(rule);
    places.set(hash, [...(places.get(hash) || []), index]);
  });
  const known = new Set(before.map(hashOf));
  const added = after.filter(rule => !known.has(hashOf(rule)));
  if (added.length) {
    throw new Error(`The build after the removal is not the build before it minus rules: ${added.length} rules were added or changed, for example ${added[0].selector}`);
  }
  let anchor = -1;
  return before.reduce((removed, rule) => {
    const hash = hashOf(rule);
    if (places.has(hash)) {
      // The rule stays: the next one after the previous match, or the first one when it moved (a mixin call instead of an extend moves it next to its component)
      const indexes = places.get(hash);
      anchor = indexes.find(index => index > anchor) ?? indexes[0];
    } else {
      removed.push({at: anchor + 1, after: anchor >= 0 ? hashOf(after[anchor]) : null, selector: rule.selector, media: rule.media, declarations: rule.declarations, ...(rule.shadowed ? {shadowed: rule.shadowed} : {})});
    }
    return removed;
  }, []);
};

/**
 * Put the frozen rules back where they were in a build, so the compat equivalence tests keep comparing with the vendored rule
 * after its LESS source is gone
 * @param {Array} current Flattened rules of the current build
 * @param {Array} frozen Frozen rules (diffRemoved format)
 * @return {Array} Flattened rules of the build before the removal
 */
const restoreStep = (current, frozen) => {
  const hashes = current.map(hashOf);
  const present = new Set(hashes);
  const overlap = frozen.filter(rule => present.has(hashOf(rule)));
  if (overlap.length) {
    // A frozen rule is a rule the build lacks: all of them there means the removal is not applied to this build (the caller has to
    // skip that step), some of them there and some not means the baseline is for another build
    throw new Error(`${overlap.length} of ${frozen.length} frozen rules are already in the build, for example ${overlap[0].selector}`);
  }
  const removed = frozen;
  const placed = removed.map((rule, order) => {
    let at = rule.at;
    if (rule.after !== null && hashes[at - 1] !== rule.after) {
      // The rules before the removed ones moved: the closest identical anchor wins
      const anchors = hashes.map((hash, index) => (hash === rule.after ? index + 1 : -1)).filter(index => index >= 0);
      at = anchors.length ? anchors.reduce((best, index) => (Math.abs(index - rule.at) < Math.abs(best - rule.at) ? index : best)) : rule.at;
    }
    return {at: Math.min(at, current.length), order, rule: {selector: rule.selector, media: rule.media, declarations: rule.declarations, ...(rule.shadowed ? {shadowed: rule.shadowed} : {})}};
  }).sort((a, b) => a.at - b.at || a.order - b.order);
  const merged = [];
  let next = 0;
  current.forEach((rule, index) => {
    while (next < placed.length && placed[next].at <= index) {
      merged.push(placed[next++].rule);
    }
    merged.push(rule);
  });
  placed.slice(next).forEach(entry => merged.push(entry.rule));
  return merged;
};

/**
 * Put the frozen rules back into a build, removal by removal from the last to the first: the position of the rules of a removal is
 * relative to the build right after it, which lacks the rules of the removals that came later
 * @param {Array} current Flattened rules of the current build
 * @param {Array} frozen Frozen rules, each with its `step` (the first removal has none)
 * @param {Array<number>} [steps] The removals to put back (default: every one in `frozen`)
 * @return {Array} Flattened rules of the build before those removals; a removal whose rules are already there (all or some) is refused
 */
const restoreRemoved = (current, frozen, steps = [...new Set(frozen.map(rule => rule.step || 1))]) => [...steps].sort((a, b) => b - a)
  .reduce((build, step) => restoreStep(build, frozen.filter(rule => (rule.step || 1) === step)), current);

/**
 * @return {object} The frozen baseline: `removed` holds the rules of every removal (each with its `step`), `served` the ones that the compat layer serves
 * (every step but the `unserved` ones), `steps` says which files each step took out
 */
const readBaseline = () => {
  const document = JSON.parse(fs.readFileSync(BASELINE, "utf8"));
  const later = document.later || [];
  const removed = [...document.removed.map(rule => ({step: 1, ...rule})), ...later.flatMap(entry => entry.removed.map(rule => ({step: entry.header.step, ...rule})))];
  // A step marked `unserved` dropped families that nothing writes, with no compat CSS: its rules are frozen to keep the build digest honest but nothing serves them
  const unserved = new Set(later.filter(entry => entry.header.unserved).map(entry => entry.header.step));
  return {
    ...document,
    steps: [{step: 1, files: ["grid.less", "panels.less", "utilities.less", "responsive-utilities.less"], lessBytesBefore: document.lessBytesBefore, lessBytesAfter: document.lessBytesAfter},
      ...later.map(entry => entry.header)],
    removed,
    served: removed.filter(rule => !unserved.has(rule.step))
  };
};

/**
 * Flattened rules of the vendored build as it was before the families of the baseline were removed from the LESS sources:
 * the current LESS build plus the frozen rules of the removed families
 * @return {Promise<Array>} Rules in source order
 */
const vendoredBeforeRemoval = async () => {
  const baseline = readBaseline();
  // A removal is applied to this build when its vendored files are gone from the LESS sources; the one that is not applied yet has its rules in the build
  const applied = baseline.steps.filter(entry => entry.files.every(file => !fs.existsSync(path.join(LESS_DIR, "legacy", file)))).map(entry => entry.step);
  return restoreRemoved(getRules(await compileStyles()), baseline.removed, applied);
};

/**
 * Selectors that a rule of the current LESS build styles besides the Bootstrap rules of the baseline (the frozen ones and the kept leftovers):
 * pixel-admin and AWE rules (the AWE 5 design system among them). Being unlayered they win over the compat copy of the selector whatever the
 * order, so the order of the compat rules does not decide the cascade for them
 * @param {Array} current Flattened rules of the current build
 * @return {Set<string>} Selectors (pseudo-elements normalized)
 */
const overriddenSelectors = current => {
  const baseline = readBaseline();
  const bootstrap = new Set([...baseline.removed.map(hashOf), ...baseline.steps.flatMap(entry => entry.kept || [])]);
  return new Set(current.filter(rule => !bootstrap.has(hashOf(rule))).map(rule => rule.selector.replace(/::(before|after)$/, ":$1")));
};

/**
 * Identity of an ordered list of rules: changes when a rule is added, removed, changed or moved
 * @param {Array} rules Flattened rules
 * @return {string} SHA-1 of the rule identities in order
 */
const digestOf = rules => require("crypto").createHash("sha1").update(rules.map(hashOf).join(",")).digest("hex");

module.exports = {overriddenSelectors, BASELINE, diffRemoved, restoreRemoved, readBaseline, vendoredBeforeRemoval, hashOf, digestOf};
