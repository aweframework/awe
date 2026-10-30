import fs from "fs";
import path from "path";
import {overriddenSelectors, vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileStyles} from "./support/compileStyles";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";
import {expectCompatTokens} from "./support/compatFamily";
import {commonWinners, shape, effective, hoistSupported, mediasOf, namesOf, normalizeSelector, orderWinners} from "./support/compatEquivalence";
import {importantConflicts} from "./support/importantGuard";
import {specificity} from "./support/cascadeGuard";
import {sourceFiles, usedXmlClasses} from "./support/xmlScreens";
import {fileSelectors, laterSelectors} from "./support/compatFile";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

// Bootstrap 3 buttons.less and button-groups.less and the pixel-admin / AWE rules on the same selectors (base.less, print.less)
const VARIANTS = ["btn-default", "btn-primary", "btn-success", "btn-info", "btn-warning", "btn-danger", "btn-link"];
const SIZES = ["btn-lg", "btn-sm", "btn-xs"];
// active, disabled and focus are the state classes of a button: a rule is served when every class of its selector is in this list
const GROUPS = ["btn-group", "btn-group-lg", "btn-group-sm", "btn-group-xs", "btn-group-vertical", "btn-group-justified", "btn-toolbar", "dropdown-toggle", "[data-toggle=\"buttons\"]"];
const CLASSES = ["btn", ...VARIANTS, ...SIZES, "btn-block", ...GROUPS, "active", "disabled", "focus"];
const STATES = ["active", "disabled", "focus"];
// A rule with a class outside CLASSES keeps its whole selector in LESS (a rule never half moves): each entry says why
const LEFT_OUT = [
  {names: ["btn-flat", "btn-outline", "btn-rounded", "btn-labeled", "btn-label", "focused", "dark", "btn-facebook", "btn-dark", "navbar-btn", "button-icon", "button-text"],
    reason: "pixel-admin button modifiers (flat, outline, rounded, labeled): a later slice, they stay in LESS"},
  {match: name => /^theme-/.test(name) || ["awe-theme", "base-theme", "btn-awe"].includes(name), reason: "selectors that depend on a theme class of the body: themed (AWE button themes)"},
  {match: name => name.startsWith("#"), reason: "id-scoped selectors: the ui-grid library ships its own Bootstrap subset under #ui-grid-twbs"},
  {names: ["badge", "label", "caret", "fa", "fa-caret-down", "icon", "input-group", "input-group-btn", "input-group-lg", "input-group-sm", "navbar-default", "navbar-inverse",
    "modal-footer", "carousel-caption", "md-editor", "md-fullscreen-mode", "md-header", "note-editor", "note-toolbar", "panel-dark", "panel-footer", "panel-heading-controls",
    "pull-right", "pixel-file-input", "has-error", "column", "column-button-radio", "component", "criterion", "grid", "menu-screen-button", "window-buttons", "ui-grid-cell",
    "ui-grid-cell-contents", "panel", "panel-heading", "maximize-button", "dropdown", "nav", "nav-icon",
    "ui-grid-menu", "ui-grid-menu-inner", "ui-grid-menu-close-button", "ui-grid-pager-control", "ui-spinner-button", "ui-datepicker-buttonpane", "ui-priority-primary", "tabdrop",
    "note-color", "note-palette-title", "note-color-reset", "note-popover", "popover", "popover-content", "w-text",
    "validator", "panel-awe", "grid-columns-dialog", "grid-columns-actions", "modal", "grid-columns-side-panel", "pfi-actions", "pixel-file-input-sm", "pixel-file-input-lg",
    "modal-alert", "signin-form", "form-actions", "change-password-form", "totp-form", "btn-ghost", "no-label", "grid-selection-bar", "grid-selection-bar-actions", "awe-icon",
    "grid-columns-popover", "is-loading", "signin-busy", "sign-in-sso-btn"],
  reason: "rules of other components, plugins and pages that restyle a button inside them: they stay with their component"},
  {names: ["open", "dropup", "dropdown-menu"], reason: "dropdown states of a group (.open, .dropup) and the menu: with the dropdown slice"}
];
// Selectors left out whole although their classes are in the family: pixel-admin writes `background: ... !important` on them, and every AWE
// button carries a themed !important background of higher specificity (see "!important and the cascade layers")
const INTERACTIVE = ["btn", "btn-default", "btn-info", "btn-success", "btn-warning", "btn-danger"]
  .flatMap(name => [":hover", ":focus", ":active", ".active"].map(state => `.${name}${state}`));
const isLeftOut = name => LEFT_OUT.some(entry => (entry.names ? entry.names.includes(name) : entry.match(name)));

const familyUse = selector => namesOf(selector).some(name => CLASSES.includes(name) && !STATES.includes(name));
const inFamily = selector => familyUse(selector) && namesOf(selector).every(name => CLASSES.includes(name)) && !INTERACTIVE.includes(selector);

// Tailwind's output has the same values, written as lightningcss prints them
const normalize = value => normalizeLength(value.replace(/\b0px\b/g, "0"));

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatButtons", () => {
  let vendored;
  let compat;
  // What the browser applies: the compat layer under the rules that stay in the LESS build (unlayered, so they win over it)
  let view;
  // Selectors that unlayered pixel-admin or AWE rules style too: they win over the compat rule whatever the order
  let overridden;
  let selectorsOf;
  let family;

  beforeAll(async () => {
    vendored = await vendoredBeforeRemoval();
    compat = getRules(hoistSupported(await compileTailwind()));
    const current = getRules(await compileStyles());
    view = [...compat, ...current];
    overridden = overriddenSelectors(current);
    family = [...new Set(vendored.map(rule => normalizeSelector(rule.selector)))].filter(inFamily);
    selectorsOf = name => family.filter(selector => namesOf(selector).includes(name));
  });

  it("compares with the vendored Bootstrap button rules, still in the LESS build", () => {
    expect(vendored.some(rule => rule.selector === ".btn-primary:hover")).toBe(true);
  });

  it("accounts for every vendored selector that mentions a button class: served here or left out for a stated reason", () => {
    const outside = [...new Set(vendored.filter(rule => familyUse(rule.selector)).flatMap(rule => namesOf(rule.selector)))]
      .filter(name => !CLASSES.includes(name));
    expect(outside.filter(name => !isLeftOut(name))).toEqual([]);
    const named = LEFT_OUT.flatMap(entry => entry.names || []);
    expect(named.filter(name => !outside.includes(name))).toEqual([]);
    LEFT_OUT.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
  });

  it("leaves out whole the 24 hover, focus, active and .active selectors of .btn and the colors, and serves none of them", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    expect(INTERACTIVE.length).toBe(24);
    expect(INTERACTIVE.filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    expect(compat.filter(rule => INTERACTIVE.includes(normalizeSelector(rule.selector)))).toEqual([]);
  });

  it("serves every class of the family and nothing else besides the vendored selectors", () => {
    expect(CLASSES.filter(name => selectorsOf(name).length === 0)).toEqual([]);
    const served = new Set(CLASSES.flatMap(selectorsOf));
    expect(compat.map(rule => normalizeSelector(rule.selector)).filter(familyUse)
      // A selector that the input group files write is served by that family (slice 3c), with its own test
      // (slice 5) A selector that a navbar, modal or dropdown file writes (`.modal-footer .btn + .btn`) is served by that family, with its own test
      .filter(selector => !served.has(selector) && !fileSelectors("input-groups.css").has(selector) && !fileSelectors("input-groups-pixel-admin.css").has(selector) && !laterSelectors().has(selector))).toEqual([]);
  });

  describe.each(CLASSES)("%s", name => {
    it("has the same selectors and effective declarations as the compiled vendored CSS in every media query", () => {
      const selectors = selectorsOf(name);
      expect(selectors.length).toBeGreaterThan(0);
      selectors.forEach(selector => {
        expect({selector, medias: mediasOf(view, selector)}).toEqual({selector, medias: mediasOf(vendored, selector)});
        mediasOf(vendored, selector).forEach(media => {
          expect({selector, media, declarations: shape(effective(view, selector, media, normalize))})
            .toEqual({selector, media, declarations: shape(effective(vendored, selector, media, normalize))});
        });
      });
    });
  });

  describe("themed values", () => {
    it("reads the --awe-* tokens that LESS compiles, with no fallback and no theme variable of the classic themes", () => {
      expectCompatTokens({compat, family, file: ["buttons.css", "button-groups.css"]});
    });
  });

  describe("source use", () => {
    const usedTokens = () => new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/(?<![\w-])(btn(?:-[a-z]+)*|dropdown-toggle)(?![\w-])/g)]
      .map(match => match[1])));

    it("serves every button class that the templates, the directives and the XML screens write, but the ones left out", () => {
      const used = [...usedTokens()];
      expect(used).toEqual(expect.arrayContaining(["btn", "btn-default", "btn-primary", "btn-danger", "btn-link", "btn-group", "dropdown-toggle"]));
      expect(used.filter(name => !CLASSES.includes(name) && !isLeftOut(name) && !["btn-dark", "btn-update", "btn-text"].includes(name))).toEqual([]);
      expect([...usedXmlClasses().keys()].filter(name => CLASSES.includes(name))).toEqual(expect.arrayContaining(["btn", "btn-primary"]));
    });

    it("leaves btn-dark and btn-update out: the vendored build only styles btn-dark on the AWE button (btn-awe), and has no rule for btn-update", () => {
      const rules = vendored.filter(rule => /\.btn-(dark|update)(?![\w-])/.test(rule.selector));
      expect(rules.length).toBeGreaterThan(0);
      // AWE 5 (#885) paints the themes with the tokens: the rules are on the AWE button (.btn.btn-awe.btn-dark), with no theme class
      expect(rules.filter(rule => !/\.btn-awe(?![\w-])/.test(rule.selector))).toEqual([]);
      expect(rules.some(rule => /\.btn-update/.test(rule.selector))).toBe(false);
    });
  });

  describe("cascade order", () => {
    it("lets the same rule win as in the vendored build wherever two rules of the family with the same specificity compete", () => {
      const winners = orderWinners(vendored, family.filter(selector => !overridden.has(selector)), normalize);
      expect(Object.keys(winners).length).toBeGreaterThan(20);
      const common = commonWinners(orderWinners(compat, family.filter(selector => !overridden.has(selector)), normalize), winners);
      expect(common.actual).toEqual(common.expected);
      expect(Object.keys(common.actual).length).toBeGreaterThan(0);
    });
  });

  describe("!important and the cascade layers", () => {
    it("keeps !important exactly where the vendored rule has it", () => {
      const importance = (rules, selector, media) => Object.fromEntries(Object.entries(effective(rules, selector, media, normalize))
        .map(([property, value]) => [property, value.endsWith(" !important")]));
      // The selectors that only the AWE 5 button rules style (.btn.btn-default...) are written in the AWE LESS, not here
      const written = new Set(["buttons.css", "button-groups.css"].flatMap(name => [...fileSelectors(name)]));
      const compared = family.filter(selector => written.has(selector)).flatMap(selector => mediasOf(vendored, selector).map(media => ({selector, media})));
      compared.forEach(({selector, media}) => {
        expect({selector, media, importance: importance(compat, selector, media)})
          .toEqual({selector, media, importance: importance(vendored, selector, media)});
      });
      const important = [...new Set(compared.filter(({selector, media}) => Object.values(importance(vendored, selector, media)).includes(true))
        .map(({selector}) => selector))];
      expect(important).toEqual([".btn", ".btn-group"]);
    });

    // Elements that carry one of these classes and a class with an unlayered !important rule on an overlapping property, checked by hand
    const REVIEWED = [
      // outline: 0 !important (the sso logout screen button) and outline: none !important (.btn): both are outline-style none, no outline is drawn
      // (since #885 the sign-in, change-password and TOTP screens no longer set an !important outline on their buttons)
      "btn .sso-logout-screen .sso-logout-form .sso-logout-actions .sso-logout-btn outline [sso-logout-btn]"
    ];

    it("pins the reviewed outline pairs: 0 on the screens and none on .btn, both draw no outline", () => {
      const outlines = rules => rules.filter(rule => /^\.(page-signin|page-change-password|totp-screen|sso-logout-screen) .*-btn$/.test(rule.selector) && rule.declarations.outline)
        .map(rule => rule.declarations.outline);
      expect(outlines(vendored).length).toBeGreaterThan(0);
      expect([...new Set(outlines(vendored))]).toEqual(["0 !important"]);
      expect(effective(compat, ".btn", "", normalize).outline).toBe("none !important");
    });

    it("gives every AWE button btn and btn-awe plus a style class (services/button.js), so the themed !important of btn-awe can meet any left-out interactive rule", () => {
      const button = fs.readFileSync(path.join(TAILWIND_DIR, "..", "js", "awe", "services", "button.js"), "utf8");
      expect(button).toMatch(/\bbtn-awe\b/);
      expect(button).toMatch(/["']btn-["']\s*\+/);
    });

    it("has, for every left-out interactive selector, a themed rule of higher specificity with an !important background on the same state of a btn-awe button", () => {
      const themed = vendored.filter(rule => /\.btn-awe(?![\w-])/.test(rule.selector) && (rule.declarations.background || "").endsWith(" !important"));
      // The subject is exactly btn-awe, optionally the color of the selector, and the state: .awe-theme .btn-awe.btn-danger:hover
      const overrides = selector => {
        const match = selector.match(/^\.(btn[\w-]*?)((?::|\.)(?:hover|focus|active))$/);
        if (!match) {
          return false;
        }
        const [, color, state] = match;
        return themed.some(rule => {
          const subject = rule.selector.split(/[\s>+~]+/).pop();
          const rest = subject.replace(".btn-awe", "").replace(state, "");
          return subject.includes(".btn-awe") && subject.includes(state) && (rest === "" || rest === `.${color}`) && specificity(rule.selector) > specificity(selector);
        });
      };
      // The .active ones are no longer overridden since #885 removed the themes: they stay in LESS (left out whole, so nothing changes)
      expect(INTERACTIVE.filter(selector => !selector.endsWith(".active") && !overrides(selector))).toEqual([]);
    });

    it("has no element in the framework that carries a button class and a class whose unlayered !important rule sets another value", () => {
      const {important, candidates, conflicts} = importantConflicts(vendored, compat, CLASSES, selectorsOf, normalize);
      const label = conflict => `${conflict.utility} ${conflict.rule} ${conflict.property} [${conflict.tokens}]`;
      expect(important.map(declaration => `${declaration.selector} ${declaration.property}`)).toEqual([".btn outline", ".btn display", ".btn-group display"]);
      expect(candidates.length).toBeGreaterThan(0);
      expect(conflicts.map(label).filter(conflict => !REVIEWED.includes(conflict))).toEqual([]);
      expect(REVIEWED.filter(reviewed => !conflicts.map(label).includes(reviewed))).toEqual([]);
    });
  });

  describe("import order", () => {
    const index = () => fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8");
    const imports = () => [...index().matchAll(/^@import\s+"\.\/([\w.-]+\.css)";/gm)].map(match => match[1]);

    it("imports buttons.css and then button-groups.css in the compat index", () => {
      expect(imports()).toContain("buttons.css");
      expect(imports()).toContain("button-groups.css");
      expect(imports().indexOf("buttons.css")).toBeLessThan(imports().indexOf("button-groups.css"));
    });

    it("imports buttons.css after spacing.css (the order that later families keep; the cascade check below proves no winner depends on it today)", () => {
      expect(imports().indexOf("buttons.css")).toBeGreaterThan(imports().indexOf("spacing.css"));
      expect(imports().indexOf("spacing.css")).toBeGreaterThan(-1);
    });

    it("lets the same rule win as in the vendored build where a button rule and a single-class rule of another compat family have the same specificity", () => {
      const familySet = new Set(family);
      // Classes that the templates, directives and screens write in the same string as a button class (not a state such as `disabled`, which
      // inputs carry too): the only ones that can meet it on an element
      const together = new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/["'`]([^"'`\n]*)["'`]/g)]
        .map(match => match[1].split(/\s+/).filter(Boolean)).filter(words => words.some(word => CLASSES.includes(word) && !STATES.includes(word))).flat()));
      const others = [...new Set(compat.map(rule => normalizeSelector(rule.selector)))]
        .filter(selector => !familySet.has(selector) && /^\.[\w-]+$/.test(selector) && together.has(selector.slice(1)));
      const involved = winners => Object.fromEntries(Object.entries(winners).filter(([key]) => key.split("|").slice(1, 3).some(selector => familySet.has(selector))));
      const expected = involved(orderWinners(vendored, [...family, ...others].filter(selector => !overridden.has(selector)), normalize));
      expect(Object.keys(expected).length).toBeGreaterThan(0);
      const common = commonWinners(involved(orderWinners(compat, [...family, ...others].filter(selector => !overridden.has(selector)), normalize)), expected);
      expect(common.actual).toEqual(common.expected);
    });
  });
});
