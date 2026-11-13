const {hashOf} = require("./vendoredBeforeRemoval");
const {overlaps} = require("./importantGuard");

/**
 * Specificity of a selector as a comparable number (ids x 10000 + classes, attributes and pseudo-classes x 100 + elements).
 * Pseudo-elements count as elements; :not() counts its argument.
 */
const specificity = selector => {
  const flat = selector.replace(/:not\(([^)]*)\)/g, "$1").replace(/::?(before|after|first-line|first-letter)/g, " ()");
  const ids = (flat.match(/#[\w-]+/g) || []).length;
  const classes = (flat.match(/\.[\w-]+|\[[^\]]*\]|:(?!\()[\w-]+/g) || []).length;
  const elements = (flat.replace(/#[\w-]+|\.[\w-]+|\[[^\]]*\]|:[\w-]+/g, " ").match(/(^|[\s>+~])[a-zA-Z][\w-]*/g) || []).length + (flat.match(/\(\)/g) || []).length;
  return ids * 10000 + classes * 100 + elements;
};

/**
 * Classes of which an element carries at most one: the Bootstrap variants (colors, sizes) and the validation states are alternatives
 */
const EXCLUSIVE = [
  ["btn-default", "btn-primary", "btn-success", "btn-info", "btn-warning", "btn-danger", "btn-link"],
  ["btn-lg", "btn-sm", "btn-xs"], ["btn-group-lg", "btn-group-sm", "btn-group-xs"], ["input-sm", "input-lg"], ["form-group-sm", "form-group-lg"],
  ["input-group-sm", "input-group-md", "input-group-lg"], ["has-success", "has-warning", "has-error"], ["panel-default", "panel-primary", "panel-success", "panel-info", "panel-warning", "panel-danger"]
];

/** Compounds of a selector with the combinator that follows each one: `a > .b c` is [{text: "a", next: ">"}, {text: ".b", next: " "}, {text: "c"}] */
const compoundsOf = selector => selector.split(/(\s*[>+~]\s*|\s+)(?![^(]*\))/).reduce((list, part, index) => {
  if (index % 2 === 0) {
    list.push({text: part});
  } else {
    list[list.length - 1].next = part.trim() || " ";
  }
  return list;
}, []);
const tagOf = compound => (compound.match(/^[a-zA-Z][\w-]*/) || [null])[0];
const classesIn = compound => (compound.match(/\.[\w-]+/g) || []).map(name => name.slice(1));

/** Whether two compounds can be one element: they do not name two elements, nor two alternative classes of one group */
const compatibleCompounds = (first, second) => {
  const [tagA, tagB] = [tagOf(first), tagOf(second)];
  const [classesA, classesB] = [classesIn(first), classesIn(second)];
  return !((tagA && tagB && tagA !== tagB)
    || EXCLUSIVE.some(group => classesA.some(x => group.includes(x)) && classesB.some(y => group.includes(y))
      && !classesA.every(x => !group.includes(x) || classesB.includes(x)) && !classesB.every(y => !group.includes(y) || classesA.includes(y))));
};

/**
 * Whether two selectors can match one element. Only compounds that are certainly the same element for both selectors can exclude
 * a match: the subjects, and the compounds linked to the subject by a chain of `>` (or `+`) that both selectors share. Across a
 * descendant combinator the ancestors may be different elements (an element can sit inside a `.panel-info` that sits inside a
 * `.panel-primary`), so they never exclude.
 * @param {string} first Selector
 * @param {string} second Selector
 * @return {boolean} False when no element can match both
 */
const canMeet = (first, second) => {
  const [a, b] = [compoundsOf(first), compoundsOf(second)];
  let [x, y] = [a.length - 1, b.length - 1];
  while (x >= 0 && y >= 0) {
    if (!compatibleCompounds(a[x].text, b[y].text)) {
      return false;
    }
    const link = x > 0 && y > 0 && a[x - 1].next === b[y - 1].next && [">", "+"].includes(a[x - 1].next);
    x = link ? x - 1 : -1;
    y = link ? y - 1 : -1;
  }
  return true;
};

const subjectClasses = selector => new Set(selector.split(/\s*[>+~]\s*|\s+/).pop().match(/\.[\w-]+/g) || []);
const subjectTag = selector => tagOf(compoundsOf(selector).pop().text);

/**
 * Tags that Bootstrap writes a class on, for the classes the AWE sources never write (a product can): `<a class="navbar-brand">` meets the rules on `a`
 */
const HOSTS = {
  "navbar-brand": ["a"], "navbar-link": ["a"], "navbar-toggle": ["button"], "navbar-btn": ["button", "a"], "navbar-text": ["p"], "navbar-form": ["form"], "navbar-nav": ["ul"],
  "dropdown-header": ["li"], "dropdown-menu": ["ul"], "popover-title": ["h3"], caret: ["span"], close: ["button"], "nav-divider": ["li"], "nav-tabs": ["ul"], "nav-pills": ["ul"],
  // Bootstrap documents a list group on a list (`<ul class="list-group">`): the AWE sources write it on a div, but the `ul, ol` rules of type.less meet it
  "list-group": ["ul", "ol"]
};

let usage = null;
/**
 * What the templates, the directives and the XML screens write: the classes that appear together in one string (so one element can
 * carry them) and the tags a class is written on (`<label class="btn ...">`)
 * @return {{together: Map<string, Set<string>>, tags: Map<string, Set<string>>}} Neighbour classes and tags by class
 */
const sourceUsage = () => {
  if (!usage) {
    const fs = require("fs");
    const {sourceFiles} = require("./xmlScreens");
    const together = new Map();
    const tags = new Map();
    const add = (map, key, value) => map.set(key, (map.get(key) || new Set()).add(value));
    sourceFiles().forEach(file => {
      const text = fs.readFileSync(file, "utf8");
      [...text.matchAll(/["'`]([^"'`\n]*)["'`]/g)].forEach(match => {
        const words = match[1].split(/\s+/).filter(word => /^[\w-]+$/.test(word));
        words.forEach(word => words.forEach(other => add(together, word, other)));
      });
      [...text.matchAll(/<([a-z][a-z0-9-]*)\b[^<>]*?\bclass(?:Name)?\s*=\s*["']([^"']*)["']/g)]
        .forEach(match => match[2].split(/\s+/).filter(Boolean).forEach(name => add(tags, name, match[1])));
    });
    Object.entries(HOSTS).forEach(([name, hosts]) => hosts.forEach(host => add(tags, name, host)));
    usage = {together, tags};
  }
  return usage;
};
const namesIn = selector => [...subjectClasses(selector)].map(name => name.slice(1));

/**
 * Whether the subjects of two selectors can be one element. They share a class; or (when `broad`) a class of one is written with a
 * class of the other in one string of the sources (`form-control input-sm`); or one names no class (`label`, `textarea`) and the other has
 * a class that is written on that tag
 */
const mayMeet = (winner, other, broad, universal = false) => {
  if ([...winner.classes].some(name => other.classes.has(name))) {
    return true;
  }
  if (!broad) {
    return false;
  }
  const {together, tags} = sourceUsage();
  const [namesW, namesO] = [namesIn(winner.selector), namesIn(other.selector)];
  if (namesW.some(name => namesO.some(neighbour => (together.get(name) || new Set()).has(neighbour)))) {
    return true;
  }
  const [tagW, tagO] = [subjectTag(winner.selector), subjectTag(other.selector)];
  // (third removal) Variants of one component are written on one element (`navbar navbar-default navbar-right`, `nav nav-tabs`): classes that start with
  // the same word (`navbar-text` and `navbar-right`, `navbar` and `navbar-default`) can be on the same element even when no AWE source writes them together
  if (universal && namesW.some(first => namesO.some(second => first.split("-")[0] === second.split("-")[0]))) {
    return true;
  }
  // (third removal) A rule on every element (`body *`, `*`) or on an attribute alone has no class and no tag: it can meet any rule
  if (universal && ((namesO.length === 0 && !tagO) || (namesW.length === 0 && !tagW))) {
    return true;
  }
  return (namesO.length === 0 && tagO && namesW.some(name => (tags.get(name) || new Set()).has(tagO)))
    || (namesW.length === 0 && tagW && namesO.some(name => (tags.get(name) || new Set()).has(tagW)))
    || (namesW.length === 0 && namesO.length === 0 && (!tagW || !tagO || tagW === tagO));
};

/** Range of widths that a media query allows, as [min, max] in pixels (null when it has no width condition we understand) */
const widthRange = media => {
  const min = [...media.matchAll(/min-width:\s*(\d+(?:\.\d+)?)px/g)].map(match => Number(match[1]));
  const max = [...media.matchAll(/max-width:\s*(\d+(?:\.\d+)?)px/g)].map(match => Number(match[1]));
  return [min.length ? Math.max(...min) : 0, max.length ? Math.min(...max) : Infinity];
};

/**
 * Whether two media queries can apply at the same time: the empty one (no query) applies always, equal ones do, and two width ranges that do
 * not overlap (`(max-width: 767px)` and `(min-width: 768px)`) do not. Anything else may overlap
 * @param {string} first Media query of a rule ("" for none)
 * @param {string} second Media query of another rule
 * @return {boolean} False only when the two can never apply together
 */
const mediaMeet = (first, second) => {
  if (first === second || first === "" || second === "") {
    return true;
  }
  // A comma separates alternatives (`print, (min-width: 768px)`): the queries can apply together when any pair of alternatives can
  const alternatives = media => media.split(",").map(item => item.trim()).filter(Boolean);
  return alternatives(first).some(one => alternatives(second).some(other => {
    const [a, b] = [widthRange(one), widthRange(other)];
    return Math.max(a[0], b[0]) <= Math.min(a[1], b[1]);
  }));
};
const isPseudoElement = selector => /::?(before|after)/.test(selector);
const isImportant = value => value.includes("important");

/**
 * The compat layer is layered and the LESS that remains is not, and an unlayered rule beats a layered one whatever its
 * specificity. A rule that was removed from the LESS (so a compat rule serves it now) changes what an element renders when, before the
 * removal, it won over a rule that is still in the LESS: same property, a different value, same media query, winning by
 * specificity or by order, and a subject class in common (so both can match one element). For the rules of the later removals
 * (buttons, button groups, forms, input groups; then the dropdowns, navs, navbar, modals, tooltip, popovers and animations) the classes need not be the same: classes written together in the sources, and element
 * rules (`label`) on a tag that carries the class, count too (the pixel diff found `.input-sm` losing to `.form-control`, and `.btn`
 * losing to `label`).
 * @param {Array} rules Flattened rules of the vendored build before the removal, in order (see vendoredBeforeRemoval)
 * @param {Array} removed Frozen rules, the ones a compat rule serves now
 * @return {Array<{removed: string, kept: string, property: string, removedValue: string, keptValue: string, media: string}>} Pairs
 */
const cascadeCandidates = (rules, removed) => {
  const gone = new Set(removed.map(hashOf));
  const second = new Set(removed.filter(rule => rule.step >= 2).map(hashOf));
  // The third removal (dropdowns, navs, navbar, modals...) also meets across media queries (a rule inside `@media (min-width: 768px)` beats a base rule
  // of less specificity that stays) and against rules on every element (`body *`): the pixel diff of the first build found both
  const third = new Set(removed.filter(rule => rule.step >= 3).map(hashOf));
  const indexed = rules.map((rule, index) => ({...rule, index, specificity: specificity(rule.selector), classes: subjectClasses(rule.selector)}));
  const kept = indexed.filter(rule => !gone.has(hashOf(rule)));
  const found = new Map();
  indexed.filter(rule => gone.has(hashOf(rule))).forEach(winner => {
    Object.keys(winner.declarations).filter(property => !isImportant(winner.declarations[property])).forEach(property => {
      // The same property with another value; for the second removal also a shorthand against its longhand (`border-radius` and `border-top-right-radius`)
      kept.flatMap(other => Object.keys(other.declarations).filter(name => (name === property || (second.has(hashOf(winner)) && overlaps(property, name))) && !isImportant(other.declarations[name])
        && (name !== property || other.declarations[name] !== winner.declarations[property])).map(name => ({other, name}))).filter(({other}) => (other.media === winner.media || (third.has(hashOf(winner)) && mediaMeet(other.media, winner.media))) && isPseudoElement(other.selector) === isPseudoElement(winner.selector)
        && (winner.specificity > other.specificity || (winner.specificity === other.specificity && winner.index > other.index))
        && mayMeet(winner, other, second.has(hashOf(winner)), third.has(hashOf(winner))) && canMeet(winner.selector, other.selector))
        .forEach(({other, name}) => {
          const pair = {removed: winner.selector, kept: other.selector, property: name === property ? property : `${property}/${name}`, removedValue: winner.declarations[property], keptValue: other.declarations[name], media: winner.media, removedHash: hashOf(winner)};
          found.set(JSON.stringify(pair), pair);
        });
    });
  });
  return [...found.values()];
};

/**
 * Whether one element can match both selectors, judged as broadly as the guard does for the second removal: the subjects share a class or
 * are written together in the sources, and nothing in the selectors excludes it (see canMeet and mayMeet)
 */
const meetable = (first, second) => canMeet(first, second)
  && mayMeet({selector: first, classes: subjectClasses(first)}, {selector: second, classes: subjectClasses(second)}, true);

module.exports = {cascadeCandidates, specificity, canMeet, meetable, mediaMeet};
