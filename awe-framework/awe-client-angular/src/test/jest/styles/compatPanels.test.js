import fs from "fs";
import path from "path";
import {vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";
import {importantConflicts} from "./support/importantGuard";
import {sourceFiles, usedXmlClasses} from "./support/xmlScreens";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

// Bootstrap 3 panels.less, the pixel-admin panel overrides and layout helpers (utils.less) and the AWE rules on the same selectors
const CLASSES = [
  "panel", "panel-body", "panel-heading", "panel-title", "panel-footer", "panel-title-icon", "panel-transparent",
  "panel-default", "panel-info", "panel-body-colorful", "panel-group", "panel-collapse",
  "panel-padding", "panel-padding-h", "panel-padding-r", "panel-wide", "tab-content-padding", "form-group-margin", "no-grid-gutter-h", "rounded"
];
// A vendored rule is in this family when every class of its selector is one of CLASSES. The classes below keep their rules in
// LESS for now: the selector is left out as a whole, so a rule never half moves
const LEFT_OUT = [
  {names: ["panel-primary", "panel-success", "panel-warning", "panel-danger", "panel-dark", "panel-heading-text", "panel-heading-controls"], reason: "contextual panel colors: used nowhere in the framework"},
  {names: ["panel-group-info", "panel-group-success", "panel-group-warning", "panel-group-danger", "panel-group-dark", "accordion-toggle", "collapsed"], reason: "colored accordions: with the accordion directive"},
  {names: ["panel-awe", "panel-content", "panel-sm", "panel-md", "panel-lg", "panel-invalid", "maximized", "maximizing", "maximize-button", "maximize-content", "expand", "expand-maximize", "expandible-horizontal", "expandible-vertical", "resizeAnimation", "invisible", "wizard-panel", "wizard-panel-sm", "wizard-panel-md", "wizard-panel-lg", "wizard-content", "wizard-wrapper", "tab-pane", "button-text", "awe-icon", "tab-container", "totp-form"], reason: "AWE panel, window, wizard and accordion directives: behavior of the component, not Bootstrap or pixel-admin"},
  {match: name => /^theme-/.test(name) || ["awe-theme", "base-theme"].includes(name), reason: "selectors that depend on a theme class of the body: themed"},
  {names: ["table", "table-bordered", "table-responsive", "list-group", "list-group-item", "badge", "nav-tabs", "active", "dropdown", "dropdown-menu", "dropdown-toggle", "alert-page", "btn", "btn-group", "pull-right", "small"], reason: "rules of another family that merely sit inside a panel: tables, list groups, tabs, dropdowns, badges, alerts, buttons"}
];
const isLeftOut = name => LEFT_OUT.some(entry => (entry.names ? entry.names.includes(name) : entry.match(name)));

const classesOf = selector => (selector.match(/\.-?[_a-zA-Z][\w-]*/g) || []).map(name => name.slice(1));
const normalizeSelector = selector => selector.replace(/::(before|after)$/, ":$1");
const familyUse = selector => classesOf(selector).some(name => CLASSES.includes(name));
const inFamily = selector => familyUse(selector) && classesOf(selector).every(name => CLASSES.includes(name));

const SPACING_UNIT = 5;
// calc(<length> +/- <unitless number>) is invalid (a length cannot be added to a number): the declaration is invalid at computed-value
// time and the property computes to its initial value, 0 for the border radii. Bootstrap's panel-heading and panel-footer corners
// (`calc(var(--border-radius, 6px) - 1)`) are such a case, so they compute to 0 today
const INVALID_CALC = /^calc\(\s*(?:var\([^)]*\)|-?\d+(?:\.\d+)?px)\s*[-+]\s*\d+(?:\.\d+)?\s*\)$/;
const invalidAtComputedValue = value => (INVALID_CALC.test(value) ? "0" : value);
// Tailwind writes spacing as calc(5px * N) (the theme unit): the compiled vendored rule has the pixels
const normalize = value => normalizeLength(invalidAtComputedValue(value)
  .replace(/calc\((?:var\(--spacing\)|5px)\s*\*\s*(-?\d+(?:\.\d+)?)\)/g, (match, units) => `${Number(units) * SPACING_UNIT}px`)
  .replace(/\b0px\b/g, "0"));

/** Property and importance of a declaration block: the values follow the AWE 5 tokens, so they may differ from the compiled vendored ones */
const shape = declarations => Object.fromEntries(Object.entries(declarations).map(([property, value]) => [property, value.endsWith(" !important")]));

/**
 * Effective declarations of the rules of a selector in a media query, as the cascade resolves them within one origin:
 * an `!important` value beats a normal one, otherwise the last one wins
 */
const effective = (rules, selector, media) => Object.fromEntries(Object.entries(rules
  .filter(rule => normalizeSelector(rule.selector) === selector && rule.media === media)
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

const mediasOf = (rules, selector) => [...new Set(rules.filter(rule => normalizeSelector(rule.selector) === selector).map(rule => rule.media))].sort();

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatPanels", () => {
  let vendored;
  let compat;
  let selectorsOf;

  beforeAll(async () => {
    vendored = await vendoredBeforeRemoval();
    compat = getRules(await compileTailwind());
    const family = [...new Set(vendored.map(rule => normalizeSelector(rule.selector)))].filter(inFamily);
    selectorsOf = name => family.filter(selector => classesOf(selector).includes(name));
  });

  it("compares with the frozen vendored Bootstrap panel rules (support/vendored-removed.baseline.json)", () => {
    expect(vendored.some(rule => rule.selector === ".panel-body")).toBe(true);
  });

  it("accounts for every vendored selector that mentions a panel class: served here or left out for a stated reason", () => {
    const outside = [...new Set(vendored.filter(rule => familyUse(rule.selector)).flatMap(rule => classesOf(rule.selector)))]
      .filter(name => !CLASSES.includes(name));
    expect(outside.filter(name => !isLeftOut(name))).toEqual([]);
    const named = LEFT_OUT.flatMap(entry => entry.names || []);
    const seen = new Set([...outside, ...[...sourceUsedTokens()]]);
    expect(named.filter(name => !seen.has(name))).toEqual([]);
  });

  it("serves every class of the family and nothing else besides the vendored selectors", () => {
    expect(CLASSES.filter(name => selectorsOf(name).length === 0)).toEqual([]);
    const served = new Set(CLASSES.flatMap(selectorsOf));
    expect(compat.map(rule => normalizeSelector(rule.selector)).filter(familyUse).filter(selector => !served.has(selector))).toEqual([]);
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
  });

  // Static `panel...` class names that the templates, the directives and the XML screens write; the dynamic ones (panel-sm|md|lg) are AWE
  const sourceUsedTokens = () => new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/(?<![\w-])(panel(?:-[a-z]+)*)(?![\w-])/g)]
    .map(match => match[1])));

  it("serves every panel class that the templates, the directives and the XML screens use, but the ones left out", () => {
    const used = [...sourceUsedTokens()].filter(name => name !== "panel-");
    expect(used).toEqual(expect.arrayContaining(["panel", "panel-body", "panel-heading", "panel-footer", "panel-title-icon", "panel-padding", "panel-transparent",
      "panel-info", "panel-body-colorful", "panel-group", "panel-collapse"]));
    // The identity header of the AWE side panel (panel-identity-*) and its left variant are AWE LESS component classes, not Bootstrap panel classes;
    // panel-right is the Lucide icon of the button that opens the advanced search of a window in a side panel (#790)
    const AWE_SIDE_PANEL = ["panel-identity", "panel-identity-avatar", "panel-identity-text", "panel-identity-title", "panel-identity-subtitle", "panel-identity-tags", "panel-left",
      "panel-right"];
    expect(used.filter(name => !CLASSES.includes(name) && !isLeftOut(name) && !AWE_SIDE_PANEL.includes(name))).toEqual([]);
    const xml = [...usedXmlClasses().keys()].filter(name => CLASSES.includes(name));
    expect(xml).toEqual(expect.arrayContaining(["panel-body", "panel-footer", "panel-padding", "panel-transparent", "panel-info", "panel-body-colorful"]));
  });

  describe("corners of the heading and the footer", () => {
    const CORNERS = {
      ".panel-heading": ["border-top-right-radius", "border-top-left-radius"],
      ".panel-footer": ["border-bottom-right-radius", "border-bottom-left-radius"]
    };

    it("pins the vendored corners as invalid calc() values, which compute to 0", () => {
      Object.entries(CORNERS).forEach(([selector, properties]) => {
        const raw = vendored.filter(rule => rule.selector === selector && rule.media === "");
        properties.forEach(property => {
          expect(raw.some(rule => INVALID_CALC.test(rule.declarations[property] || ""))).toBe(true);
          expect(effective(vendored, selector, "")[property]).toBe("0");
        });
      });
    });

    it("writes radius 0 explicitly on those corners instead of the invalid calc(): no visual change", () => {
      Object.entries(CORNERS).forEach(([selector, properties]) => {
        const raw = compat.filter(rule => rule.selector === selector && rule.media === "");
        properties.forEach(property => {
          expect(raw.map(rule => rule.declarations[property]).filter(Boolean)).toEqual(["0"]);
        });
      });
    });

    it("has no invalid calc() (a length plus or minus a unitless number) in any declaration", () => {
      const invalid = compat.flatMap(rule => Object.entries(rule.declarations)
        .filter(([, value]) => INVALID_CALC.test(value.replace(" !important", ""))).map(([property]) => `${rule.selector} ${property}`));
      expect(invalid).toEqual([]);
    });
  });

  describe(".panel-body padding", () => {
    // The vendored CSS has two unlayered .panel-body rules: Bootstrap padding 15px, then pixel-admin padding 20px (the later wins)
    it("computes to 20px, in the vendored cascade and in the compat layer (tw:p-4 on the 5px unit)", () => {
      const paddings = vendored.filter(rule => rule.selector === ".panel-body" && rule.media === "" && rule.declarations.padding)
        .map(rule => rule.declarations.padding);
      expect(paddings).toEqual(["15px", "20px"]);
      expect(effective(vendored, ".panel-body", "").padding).toBe("20px");
      expect(effective(compat, ".panel-body", "").padding).toBe("20px");
    });

    // Specificity of a selector made of classes, attributes and pseudo-classes: [ids, classes, types]
    const specificity = selector => [(selector.match(/#[\w-]+/g) || []).length,
      (selector.match(/\.[\w-]+|\[[^\]]+]|(?<!:):(?!:)[\w-]+/g) || []).length, 0];
    const higher = (left, right) => left[0] - right[0] || left[1] - right[1];

    it("is overridden by every AWE size rule that stays in LESS (panel-sm|md|lg, wizard-panel): more specific than the compat rule", () => {
      const compatSpecificity = specificity(".panel-body");
      const own = vendored.filter(rule => /(^|[\s>+~])\.panel-body$/.test(rule.selector) && rule.selector !== ".panel-body" && rule.declarations.padding);
      expect(own.map(rule => `${rule.selector} ${rule.declarations.padding}`).sort()).toEqual([
        // the AWE 5 design system (#885) writes the sizes of the window and of the dialog panels with tokens
        ".panel.panel-awe .panel-body var(--awe-content-padding)", ".panel.panel-lg .panel-body var(--awe-space-6)", ".panel.panel-sm .panel-body var(--awe-space-3)",
        ".totp-form .panel > .panel-body var(--awe-space-3)",
        ".wizard-panel.panel-lg .panel-body var(--awe-space-6)", ".wizard-panel.panel-sm .panel-body var(--awe-space-3)",
        ".wizard-panel.wizard-panel-lg .panel-body 20px", ".wizard-panel.wizard-panel-md .panel-body 15px", ".wizard-panel.wizard-panel-sm .panel-body 10px"
      ].sort());
      own.forEach(rule => expect(higher(specificity(rule.selector), compatSpecificity)).toBeGreaterThan(0));
    });
  });

  describe("!important and the cascade layers", () => {
    it("keeps !important exactly where the vendored rule has it", () => {
      const importance = (rules, selector, media) => Object.fromEntries(Object.entries(effective(rules, selector, media))
        .map(([property, value]) => [property, value.endsWith(" !important")]));
      const compared = CLASSES.flatMap(selectorsOf).flatMap(selector => mediasOf(vendored, selector).map(media => ({selector, media})));
      compared.forEach(({selector, media}) => {
        expect({selector, media, importance: importance(compat, selector, media)})
          .toEqual({selector, media, importance: importance(vendored, selector, media)});
      });
      const important = [...new Set(compared.filter(({selector, media}) => Object.values(importance(vendored, selector, media)).includes(true))
        .map(({selector}) => selector))];
      expect(important).toEqual(expect.arrayContaining([".panel-padding", ".panel-wide", ".panel-transparent", ".rounded", ".panel-info"]));
    });

    // Elements that carry one of these classes and a class with an unlayered !important rule on an overlapping property, checked by hand.
    // Both sides are !important, so the layered compat rule wins; each pair resolves to the same computed value as the vendored cascade
    const REVIEWED = [
      // border: none !important already computes a 0 width (style none), as no-border does; written on the panel and on its footer
      "panel-transparent .no-border border-width [no-border]"
    ];

    it("has no element in the framework that carries one of these classes and a class whose unlayered !important rule sets another value", () => {
      const {important, candidates, conflicts} = importantConflicts(vendored, compat, CLASSES, selectorsOf, normalize);
      const label = conflict => `${conflict.utility} ${conflict.rule} ${conflict.property} [${conflict.tokens}]`;
      expect(important.length).toBeGreaterThan(8);
      expect(candidates.length).toBeGreaterThan(0);
      expect(conflicts.map(label).filter(conflict => !REVIEWED.includes(conflict))).toEqual([]);
      expect(REVIEWED.filter(reviewed => !conflicts.map(label).includes(reviewed))).toEqual([]);
    });
  });

  describe("import order", () => {
    const index = () => fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8");

    it("imports panels.css before spacing.css: panel-padding no-padding-t is in screens and no-padding-t must keep winning", () => {
      expect(index()).toContain("./panels.css");
      expect(index().indexOf("./panels.css")).toBeLessThan(index().indexOf("./spacing.css"));
    });

    it("emits .panel-padding before .no-padding-t, the order that gives the later !important rule the padding-top", () => {
      const position = selector => compat.findIndex(rule => rule.selector === selector);
      expect(position(".panel-padding")).toBeGreaterThan(-1);
      expect(position(".panel-padding")).toBeLessThan(position(".no-padding-t"));
    });
  });
});
