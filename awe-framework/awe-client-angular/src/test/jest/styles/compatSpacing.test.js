import fs from "fs";
import path from "path";
import postcss from "postcss";
import {LESS_DIR} from "./support/compileStyles";
import {vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";
import {importantConflicts} from "./support/importantGuard";
import {usedXmlClasses} from "./support/xmlScreens";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

const sides = base => ["", "-hr", "-vr", "-t", "-b"].map(side => `${base}${side}`);

// pixel-admin utils.less (spacing, borders, alignment, type) and the AWE rules next to them (less/awe: heights, side paddings)
const SPACING = [
  ...sides("no-padding"), "no-padding-left", "no-padding-right", "padding-sm", "padding-sm-hr", "padding-sm-vr", "padding-xs-hr", "padding-xs-vr",
  ...sides("no-margin"), ...sides("no-border"), "no-border-l", "no-border-r", "no-border-radius", "no-bg",
  "valign-top", "valign-middle", "valign-bottom", "height-sm", "height-md", "height-lg", "height-200"
];
const TYPE = [
  "text-xxs", "text-xs", "text-sm", "text-bg", "text-lg", "text-xlg", "text-slg", "text-slim", "text-normal", "text-semibold", "text-bold",
  "text-underline", "text-default", "text-light-gray"
];
const CLASSES = [...SPACING, ...TYPE];
// Same family, deliberately not here: see the compat guide. Panels own panel-padding*, the contextual colors go with the feedback slice
const LEFT_OUT = [
  "panel-padding", "panel-padding-h", "panel-padding-r", "tab-content-padding", "form-group-margin", "no-grid-gutter-h", "panel-wide", "rounded",
  "text-success", "text-danger", "text-warning", "text-info", "text-muted", "text-primary"
];
const FAMILY = /^(no-(padding|margin|border|bg)|padding-(sm|xs)|text-(xxs|xs|sm|bg|lg|xlg|slg|slim|normal|semibold|bold|underline|default|light-gray|success|danger|warning|info|muted|primary)|valign-|height-(sm|md|lg|\d+)|panel-padding[\w-]*|panel-wide|tab-content-padding|form-group-margin|no-grid-gutter-h|rounded)/;
// AWE form rule that merely mentions .no-margin: it stays in LESS
const CONTEXT_RULES = [".criterion.no-margin .form-group"];
// Tailwind writes spacing as calc(5px * N) (the theme unit) and the weights as theme tokens: the compiled vendored rule has the values
const TOKEN_VALUES = {"var(--awe-font-weight-bold)": "700", "var(--awe-font-weight-normal)": "400"};
const SPACING_UNIT = 5;

const selectorsOf = name => [...new Set(globalThis.vendoredRules.map(rule => rule.selector))]
  .filter(selector => new RegExp(`\\.${name}(?![\\w-])`).test(selector) && !CONTEXT_RULES.includes(selector));

const normalize = value => normalizeLength(TOKEN_VALUES[value] || value
  .replace(/calc\((?:var\(--spacing\)|5px)\s*\*\s*(-?\d+(?:\.\d+)?)\)/g, (match, units) => `${Number(units) * SPACING_UNIT}px`)
  .replace(/\b0px\b/g, "0"));

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

/** Property and importance of a declaration block: the values follow the AWE 5 tokens, so they may differ from the compiled vendored ones */
const shape = declarations => Object.fromEntries(Object.entries(declarations).map(([property, value]) => [property, value.endsWith(" !important")]));

/**
 * Effective declarations of the rules of a selector in a media query, as the cascade resolves them within one origin:
 * an `!important` value beats a normal one, otherwise the last one wins
 */
const effective = (rules, selector, media) => Object.fromEntries(Object.entries(rules
  .filter(rule => rule.selector === selector && rule.media === media)
  .reduce((merged, rule) => {
    // --tw-* are Tailwind internals (the variable behind a utility), not styling
    Object.entries(rule.declarations).filter(([property]) => !property.startsWith("--tw-")).forEach(([property, value]) => {
      const important = value.endsWith(" !important");
      if (!(merged[property] && merged[property].important && !important)) {
        merged[property] = {important, value: normalize(value.replace(" !important", ""))};
      }
    });
    return merged;
  }, {})).map(([property, {important, value}]) => [property, important ? `${value} !important` : value]));

const mediasOf = (rules, selector) => [...new Set(rules.filter(rule => rule.selector === selector).map(rule => rule.media))].sort();

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatSpacing", () => {
  let vendored;
  let compat;

  beforeAll(async () => {
    vendored = await vendoredBeforeRemoval();
    globalThis.vendoredRules = vendored;
    compat = getRules(hoistSupported(await compileTailwind()));
  });

  afterAll(() => {
    delete globalThis.vendoredRules;
  });

  it("covers every spacing, border, alignment and type class of the vendored CSS, but the ones left out on purpose", () => {
    const declared = [...new Set(vendored.flatMap(rule => (rule.selector.match(/\.[\w-]+/g) || []).map(name => name.slice(1))))]
      .filter(name => FAMILY.test(name) && !CONTEXT_RULES.some(context => context.includes(`.${name}`) && name !== "no-margin"));
    expect(declared.filter(name => !CLASSES.includes(name) && !LEFT_OUT.includes(name))).toEqual([]);
    expect(CLASSES.filter(name => !declared.includes(name))).toEqual([]);
  });

  it("keeps the vendored pixel-admin rules active until a later slice removes them", () => {
    expect(vendored.some(rule => rule.selector === ".no-padding")).toBe(true);
  });

  describe.each(CLASSES)("%s", name => {
    it("has the same selectors and effective declarations as the compiled vendored CSS in every media query", () => {
      const selectors = selectorsOf(name);
      expect(selectors.length).toBeGreaterThan(0);
      selectors.forEach(selector => {
        expect({selector, medias: mediasOf(compat, selector)}).toEqual({selector, medias: mediasOf(vendored, selector)});
        mediasOf(vendored, selector).forEach(media => {
          expect({selector, media, declarations: shape(effective(compat, selector, media))})
            .toEqual({selector, media, declarations: shape(effective(vendored, selector, media))});
        });
      });
    });

    it("emits no selector besides the vendored ones", () => {
      const extra = compat.filter(rule => new RegExp(`\\.${name}(?![\\w-])`).test(rule.selector)).map(rule => rule.selector)
        .filter(selector => !selectorsOf(name).includes(selector));
      expect(extra).toEqual([]);
    });
  });

  it("serves every class of the family that the XML screens use", () => {
    const used = [...usedXmlClasses().keys()].filter(token => CLASSES.includes(token));
    expect(used).toEqual(expect.arrayContaining(["no-padding", "no-padding-hr", "padding-sm", "no-margin", "text-xs", "text-bold", "text-light-gray"]));
    expect(used.filter(token => mediasOf(compat, selectorsOf(token)[0]).length === 0)).toEqual([]);
    expect([...usedXmlClasses().keys()].filter(token => FAMILY.test(token) && !CLASSES.includes(token) && !LEFT_OUT.includes(token))).toEqual([]);
  });

  it("does not change a computed value: the weights it takes from tokens are the compiled ones", () => {
    // The tokens come from the shared AWE 5 token file (#885)
    const tokens = fs.readFileSync(path.join(LESS_DIR, "..", "..", "..", "..", "..", "awe-design-tokens", "tokens.css"), "utf8");
    expect(tokens).toContain("--awe-font-weight-bold: 700;");
    expect(tokens).toContain("--awe-font-weight-normal: 400;");
  });

  describe("!important and the cascade layers", () => {
    it("keeps !important exactly where the vendored rule has it, on the compat declarations", () => {
      const importance = (rules, selector, media) => Object.fromEntries(Object.entries(effective(rules, selector, media))
        .map(([property, value]) => [property, value.endsWith(" !important")]));
      const compared = CLASSES.flatMap(name => selectorsOf(name).flatMap(selector => mediasOf(compat, selector).map(media => ({selector, media}))));
      expect(compared.length).toBeGreaterThan(CLASSES.length - 1);
      compared.forEach(({selector, media}) => {
        expect({selector, media, importance: importance(compat, selector, media)})
          .toEqual({selector, media, importance: importance(vendored, selector, media)});
      });
      // the weights are the only !important type classes, every spacing, border, alignment and height class is
      const important = CLASSES.filter(name => compared.some(({selector, media}) => selectorsOf(name).includes(selector)
        && Object.values(importance(compat, selector, media)).includes(true)));
      expect(important).toEqual([...SPACING, "text-slim", "text-normal", "text-semibold", "text-bold"]);
    });

    // Elements that carry one of these classes and a class with an unlayered !important rule on an overlapping property, checked by hand.
    // Both sides are !important, so the layered compat rule wins; each pair resolves to the same computed value as the vendored cascade
    const REVIEWED = [
      // border: none !important already computes a 0 width (style none), as no-border does
      "no-border .panel-transparent border [panel-transparent]"
    ];

    it("has no element in the framework that carries one of these classes and a class whose unlayered !important rule sets another value", () => {
      const {important, candidates, conflicts} = importantConflicts(vendored, compat, CLASSES, name => selectorsOf(name), normalize);
      const label = conflict => `${conflict.utility} ${conflict.rule} ${conflict.property} [${conflict.tokens}]`;
      expect(important.length).toBeGreaterThan(30);
      expect(candidates.length).toBeGreaterThan(5);
      expect(conflicts.map(label).filter(conflict => !REVIEWED.includes(conflict))).toEqual([]);
      expect(REVIEWED.filter(reviewed => !conflicts.map(label).includes(reviewed))).toEqual([]);
    });
  });

  it("writes the compat spacing utilities in their own file, imported by the compat index", () => {
    expect(fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8")).toContain("./spacing.css");
  });
});
