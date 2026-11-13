import fs from "fs";
import path from "path";
import {readBaseline, vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";
import {importantConflicts} from "./support/importantGuard";
import {usedXmlClasses} from "./support/xmlScreens";
import {fileSelectors, laterSelectors} from "./support/compatFile";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

const BREAKPOINTS = ["(max-width: 767px)", "(min-width: 768px) and (max-width: 991px)", "(min-width: 992px) and (max-width: 1199px)", "(min-width: 1200px)"];
const DISPLAYS = ["", "-block", "-inline", "-inline-block"];
const SIZES = ["xs", "sm", "md", "lg"];
const TEXT = ["left", "right", "center", "justify", "nowrap", "lowercase", "uppercase", "capitalize"].map(name => `text-${name}`);

// Bootstrap 3 utilities.less and responsive-utilities.less (without .affix: nothing uses it) and the text classes of type.less
const CLASSES = [
  "clearfix", "center-block", "pull-right", "pull-left", "hide", "show", "invisible", "text-hide", "hidden", ...TEXT,
  ...SIZES.flatMap(size => ["visible", "hidden"].flatMap(kind => (kind === "visible" ? DISPLAYS : [""]).map(display => `${kind}-${size}${display}`))),
  ...["visible-print", "visible-print-block", "visible-print-inline", "visible-print-inline-block", "hidden-print"]
];
const VARIANTS = ["", "table", "tr", "th", "td"];
const PSEUDO = ["", ":before", ":after"];
const IMPORTANT_PROPERTIES = ["float", "display", "visibility"];

const normalizeSelector = selector => selector.replace(/::(before|after)$/, ":$1");

/** Property and importance of a declaration block: the values follow the AWE 5 tokens, so they may differ from the compiled vendored ones */
const shape = declarations => Object.fromEntries(Object.entries(declarations).map(([property, value]) => [property, value.endsWith(" !important")]));

/**
 * Effective declarations of the rules of a selector in a media query, as the cascade resolves them within one origin:
 * an `!important` value beats a normal one, otherwise the last one wins
 */
const effective = (rules, selector, media) => Object.fromEntries(Object.entries(rules
  .filter(rule => normalizeSelector(rule.selector) === selector && rule.media === media)
  .reduce((merged, rule) => {
    Object.entries(rule.declarations).forEach(([property, value]) => {
      const important = value.endsWith(" !important");
      if (!(merged[property] && merged[property].important && !important)) {
        merged[property] = {important, value: normalizeLength(value.replace(" !important", ""))};
      }
    });
    return merged;
  }, {})).map(([property, {important, value}]) => [property, important ? `${value} !important` : value]));

const mediasOf = (rules, selector) => [...new Set(rules.filter(rule => normalizeSelector(rule.selector) === selector).map(rule => rule.media))].sort();

const variantsOf = name => VARIANTS.flatMap(element => PSEUDO.map(pseudo => `${element}.${name}${pseudo}`));

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatUtilities", () => {
  let vendored;
  let compat;

  beforeAll(async () => {
    vendored = await vendoredBeforeRemoval();
    compat = getRules(await compileTailwind());
  });

  it("covers every class of the Bootstrap utilities, responsive utilities and text classes, but .affix", () => {
    // The classes of the frozen rules that came from utilities.less and responsive-utilities.less (the source recorded when the
    // baseline was generated), not a list written here
    const declared = readBaseline().removed.filter(rule => ["utilities.less", "responsive-utilities.less"].includes(rule.source))
      .flatMap(rule => (rule.selector.match(/\.[\w-]+/g) || []).map(name => name.slice(1)))
      .filter(name => name !== "affix");
    expect([...new Set(declared)].sort()).toEqual(CLASSES.filter(name => !TEXT.includes(name)).sort());
  });

  it("compares with the frozen vendored Bootstrap utilities (support/vendored-removed.baseline.json)", () => {
    expect(vendored.some(rule => rule.selector === ".pull-right")).toBe(true);
  });

  describe.each(CLASSES)("%s", name => {
    it("has the same declarations as the compiled vendored CSS in every media query", () => {
      const selectors = variantsOf(name).filter(selector => mediasOf(vendored, selector).length > 0);
      expect(selectors.length).toBeGreaterThan(0);
      selectors.forEach(selector => {
        expect({selector, medias: mediasOf(compat, selector)}).toEqual({selector, medias: mediasOf(vendored, selector)});
        mediasOf(vendored, selector).forEach(media => {
          expect({selector, media, declarations: shape(effective(compat, selector, media))})
            .toEqual({selector, media, declarations: shape(effective(vendored, selector, media))});
        });
      });
    });

    it("emits no selector besides the Bootstrap ones", () => {
      // (slice 5) the dropdown, navbar and modal selectors that carry the class are served by those families, with their own test; (slice 8) so are the blockquote ones (type.css)
      const extra = compat.filter(rule => new RegExp(`\\.${name}(?![\\w-])`).test(rule.selector))
        .map(rule => normalizeSelector(rule.selector)).filter(selector => !variantsOf(name).includes(selector) && !laterSelectors().has(selector) && !fileSelectors("type.css").has(selector));
      expect(extra).toEqual([]);
    });
  });

  it("scopes the responsive classes to the Bootstrap breakpoints (xs below 768px, sm, md and lg in their range)", () => {
    SIZES.forEach((size, index) => {
      expect(mediasOf(compat, `.hidden-${size}`)).toEqual([BREAKPOINTS[index]]);
    });
  });

  it("serves every utility class that the XML screens use", () => {
    const used = [...usedXmlClasses().keys()].filter(token => CLASSES.includes(token));
    expect(used).toEqual(expect.arrayContaining(["pull-right", "text-right", "text-center", "hidden"]));
    expect(used.filter(token => !variantsOf(token).some(selector => mediasOf(compat, selector).length > 0))).toEqual([]);
  });

  describe("!important and the cascade layers", () => {
    // For !important the layer order inverts: a layered !important beats every unlayered !important, whatever its specificity.
    // So the compat float, display and visibility declarations are not inert next to the vendored LESS: they would win over an
    // unlayered `.foo { display: block !important }` on an element that also carries the utility class
    const UTILITY_SELECTORS = new Set(CLASSES.flatMap(variantsOf));
    const isUtility = rule => UTILITY_SELECTORS.has(normalizeSelector(rule.selector));
    const utilityImportant = () => compat.filter(isUtility)
      .flatMap(rule => Object.entries(rule.declarations).filter(([, value]) => value.endsWith(" !important"))
        .map(([property, value]) => ({selector: rule.selector, property, value: value.replace(" !important", "")})));

    it("sets !important only on float, display and visibility; the text classes, invisible and center-block are plain, so inert", () => {
      const plain = compat.filter(isUtility).flatMap(rule => Object.entries(rule.declarations)
        .filter(([, value]) => !value.endsWith(" !important")).map(([property]) => property));
      expect(utilityImportant().every(declaration => IMPORTANT_PROPERTIES.includes(declaration.property))).toBe(true);
      expect(utilityImportant().length).toBeGreaterThan(40);
      expect(plain).not.toContain("float");
    });

    it("has no element in the framework that carries a utility class and a class whose unlayered !important rule sets another value", () => {
      const {candidates, conflicts} = importantConflicts(vendored, compat, CLASSES, variantsOf);
      expect(candidates.length).toBeGreaterThan(20);
      expect(conflicts).toEqual([]);
    });
  });

  it("writes the compat utilities in their own file, imported by the compat index", () => {
    expect(fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8")).toContain("./utilities.css");
  });
});
