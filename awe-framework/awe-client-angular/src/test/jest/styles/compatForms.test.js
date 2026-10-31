import fs from "fs";
import path from "path";
import {overriddenSelectors, vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileStyles} from "./support/compileStyles";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";
import {expectCompatTokens} from "./support/compatFamily";
import {commonWinners, shape, effective, hoistSupported, mediasOf, namesOf, normalizeSelector, orderWinners} from "./support/compatEquivalence";
import {importantConflicts} from "./support/importantGuard";
import {sourceFiles, usedXmlClasses} from "./support/xmlScreens";
import {fileSelectors, laterSelectors} from "./support/compatFile";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

// Bootstrap 3 forms.less and the pixel-admin / AWE rules on the same selectors (legacy/pixel-admin/forms.less, less/awe)
const CONTROLS = ["form-control", "form-control-static", "input-sm", "input-lg", "form-group-sm", "form-group-lg", "form-control-feedback"];
const LAYOUT = ["form-group", "control-label", "help-block", "form-horizontal", "form-inline", "has-feedback"];
// sr-only only appears as a modifier of the label in `.has-feedback label.sr-only ~ .form-control-feedback`
const CHOICES = ["checkbox", "radio", "checkbox-inline", "radio-inline"];
const VALIDATION = ["has-success", "has-warning", "has-error"];
// sr-only is a modifier of the label (`.has-feedback label.sr-only ~ .form-control-feedback`), disabled one of a choice (`.radio.disabled label`)
const STATES = ["sr-only", "disabled"];
const CLASSES = [...CONTROLS, ...LAYOUT, ...CHOICES, ...VALIDATION, ...STATES];
// A rule with a class outside CLASSES keeps its whole selector in LESS (a rule never half moves): each entry says why
// `examples` are vendored selectors of the entry: each must still exist in the LESS build and must not be served, so a rule of one of these
// components that a later slice moves into the family cannot be accepted silently by a generic name
const LEFT_OUT = [
  {names: ["focus"], examples: [".has-error .form-control.focus"],
    reason: "pixel-admin .has-* .form-control.focus: written with !important, like the :focus selectors of the states, so they stay in LESS"},
  {names: ["input-group", "input-group-btn", "input-group-addon", "input-group-lg", "input-group-sm"], examples: [".input-group .form-control", ".input-group-lg > .form-control", ".has-error .input-group-addon", ".form-inline .input-group"],
    servedBy: ["input-groups.css", "input-groups-pixel-admin.css"],
    reason: "input groups are their own family (slice 3c, compatInputGroups.test.js): these selectors are served by compat/input-groups.css and its pixel-admin file, not here"},
  {names: ["navbar-form", "#main-navbar"], examples: [".navbar-form .form-control", "#main-navbar form.navbar-form .form-control"],
    reason: "navbar forms belong to the navbar family, which has its own slice and its own themed rules"},
  {names: ["px", "lbl", "gt-ie8"], examples: [".form-group .checkbox > .lbl"],
    reason: "the pixel-admin custom checkbox and radio (px input + lbl label): their !important rules and pseudo-elements stay with the custom control"},
  {names: ["dark", "simple"], examples: [".has-error.dark .form-control", ".has-error.simple .help-block"],
    reason: "themed validation variants (.dark colors from the theme variables, .simple with an !important color): they stay in LESS"},
  {names: ["w-icon", "form-icon", "group-sm", "input", "focused"], examples: [".form-group.w-icon .form-icon", ".form-group.group-sm", ".form-group.w-icon .input > input"],
    reason: "AWE form-group modifiers (icon inside the control, compact group), with !important paddings: they stay with the criterion directives"},
  {names: ["form-bordered", "no-margin", "no-label", "no-label-xs", "no-label-sm", "no-label-md", "no-label-lg"], examples: [".form-bordered .form-group", ".no-label .control-label", ".criterion.no-margin .form-group"],
    reason: "pixel-admin form-bordered and the AWE no-margin and no-label* helpers: modifiers of a containing class, not of the form classes"},
  {names: ["criterion", "label-left", "criterion-icon-sm", "criterion-icon-lg"], examples: [".criterion .form-group", ".criterion.label-left .form-control"],
    reason: "AWE criterion layout (label left, icons): scoped to the criterion directive, which owns its sizes and !important"},
  {names: ["signin-form", "change-password-form", "totp-form", "required", "awe-icon", "error-container", "edition", "column-checkbox", "btn-group",
    "select2-arrow", "select2-search-choice-close", "select2-container-multi", "select2-default", "select2-container-active", "select2-dropdown-open", "select2-search-field",
    "select2-search-choice", "select2-container-disabled", "switch", "switch__track", "password-toggle", "signin-password-toggle"],
  examples: [".criterion .form-group:has(.required:not(.px)) > .control-label:after", ".signin-form .form-actions .sign-in-azure-btn-light", ".select2-container .select2-choice .select2-arrow"],
  reason: "rules of the AWE 5 design system (#885) for the sign-in screens, the required mark, the icons, the grid edition and the select2 states: they are written in the AWE LESS"},
  {names: ["window-saved-search-rename"], examples: [".window-saved-search-rename .form-control"],
    reason: "the inline rename field of the saved searches of a window (#790): an AWE component rule that sizes the input in its row"},
  {match: name => /^theme-/.test(name) || name.startsWith("#"), examples: [".has-error .select2-choice"],
    reason: "theme-scoped selectors (themed values) and id-scoped selectors (the ui-grid library ships its own Bootstrap subset)"},
  {names: ["grid", "ui-grid-cell", "ui-grid-cell-contents", "component", "column-radio", "ui-grid-checkbox", "ui-grid-header-checkbox", "column-input",
    "treegrid", "treegrid-sm", "treegrid-md", "treegrid-lg", "table", "table-bordered"],
  examples: [".grid .ui-grid-checkbox .checkbox", ".grid .ui-grid-header-checkbox .checkbox", ".grid .ui-grid-cell .ui-grid-cell-contents.component .column-radio label.radio",
    ".treegrid.treegrid-sm .table.table-bordered td.component .column-input label.checkbox"],
  reason: "the AWE grid and tree grid cells draw their own checkbox and radio columns (sizes grid-sm|md|lg): the rules belong to the grid component"},
  {names: ["btn", "column-button-radio"], examples: [".grid .ui-grid-cell .ui-grid-cell-contents.component .column-button-radio.has-error > label.btn"],
    reason: "a radio rendered as a button group inside a grid cell: it stays with the grid and the button family"},
  {names: ["panel-heading-controls", "panel-dark"], examples: [".panel-heading-controls > .form-control", ".panel-dark .panel-heading-controls > .form-control"],
    reason: "controls placed in a panel heading (margin, and an !important transparent border in the dark panel): they belong to the panel family"},
  {names: ["select2-container", "select2-choice", "select2-choices", "select2-chosen"], examples: [".has-error .select2-choice", ".select2-container.form-control", ".form-group.w-icon .select2-chosen"],
    reason: "the select2 plugin renders its own control: its validation colors and padding stay with the plugin styles"},
  {names: ["editableform", "editable"], examples: [".editableform .form-control", ".form-horizontal .editable"],
    reason: "the x-editable plugin form: it resizes the control and its inline layout stays with the plugin styles"},
  {names: ["pixel-file-input"], examples: [".criterion.has-error .pixel-file-input"],
    reason: "the pixel-admin file input has its own border, with an !important error color: it stays with the criterion"}
];
// Rules of forms.less (and normalize) that name no class: global element selectors, left in LESS (the Tailwind base layer already
// resets form elements, and a layered rule on a bare element would compete with it)
const ELEMENT_ONLY = ["fieldset", "legend", "label", "optgroup", "output", "select[multiple]", "select[size]", "input[type=\"search\"]", "input[type=\"file\"]",
  "input[type=\"range\"]", "input[type=\"radio\"]", "input[type=\"checkbox\"]", "input[type=\"file\"]:focus", "input[type=\"radio\"]:focus", "input[type=\"checkbox\"]:focus",
  "input[type=\"radio\"][disabled]", "input[type=\"checkbox\"][disabled]", "input[type=\"radio\"].disabled", "input[type=\"checkbox\"].disabled",
  "fieldset[disabled] input[type=\"radio\"]", "fieldset[disabled] input[type=\"checkbox\"]", "input[type=\"date\"]", "input[type=\"time\"]", "input[type=\"datetime-local\"]", "input[type=\"month\"]"];
// Selectors left out whole although every class is in the family: pixel-admin writes `border-color: ... !important` on them (the focus
// ring of a validation state), and the themed rules of the same states (.has-error.dark .form-control:focus) carry it too
const FOCUS_IMPORTANT = ["warning", "error", "success"].flatMap(state => [`.has-${state} .form-control:focus`]);

const isLeftOut = name => LEFT_OUT.some(entry => (entry.names ? entry.names.includes(name) : entry.match(name)));
// Selectors that input-groups.css writes (slice 3c, both files): another family serves them, with its own test
const inputGroupSelectors = new Set([...fileSelectors("input-groups.css"), ...fileSelectors("input-groups-pixel-admin.css")]);
const isInputGroup = selector => inputGroupSelectors.has(selector);
const hasDeclarations = rule => Object.keys(rule.declarations).length > 0;
const familyUse = selector => namesOf(selector).some(name => CLASSES.includes(name) && !STATES.includes(name));
const inFamily = selector => familyUse(selector) && namesOf(selector).every(name => CLASSES.includes(name)) && !FOCUS_IMPORTANT.includes(selector);

// Tailwind's output has the same values, written as lightningcss prints them
const normalize = value => normalizeLength(value.replace(/\b0px\b/g, "0")).replace(/\s*,\s*/g, ", ").replace(/(?<![\d])0\.(\d)/g, ".$1");

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatForms", () => {
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
    family = [...new Set(vendored.filter(hasDeclarations).map(rule => normalizeSelector(rule.selector)))].filter(inFamily);
    selectorsOf = name => family.filter(selector => namesOf(selector).includes(name));
  });

  it("compares with the vendored Bootstrap form rules, still in the LESS build", () => {
    expect(vendored.some(rule => rule.selector === ".form-control:focus")).toBe(true);
  });

  it("accounts for every vendored selector that mentions a form class: served here or left out for a stated reason", () => {
    const outside = [...new Set(vendored.filter(rule => familyUse(rule.selector)).flatMap(rule => namesOf(rule.selector)))]
      .filter(name => !CLASSES.includes(name));
    expect(outside.filter(name => !isLeftOut(name))).toEqual([]);
    const named = LEFT_OUT.flatMap(entry => entry.names || []);
    expect(named.filter(name => !outside.includes(name))).toEqual([]);
    LEFT_OUT.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
  });

  it("keeps in LESS the example selectors of every left-out entry: they exist in the vendored build and none is served", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    const examples = LEFT_OUT.flatMap(entry => entry.examples || []);
    const unserved = LEFT_OUT.filter(entry => !entry.servedBy).flatMap(entry => entry.examples || []);
    const servedBy = LEFT_OUT.filter(entry => entry.servedBy);
    servedBy.forEach(entry => entry.examples.forEach(example => expect({example, written: entry.servedBy.some(file => fileSelectors(file).has(example))}).toEqual({example, written: true})));
    expect(LEFT_OUT.filter(entry => !entry.examples || entry.examples.length === 0)).toEqual([]);
    expect(examples.filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    // (slice 5) the selectors that the navbar, modal and dropdown files serve moved there, with their own test
    expect(compat.filter(rule => unserved.includes(normalizeSelector(rule.selector)) && !laterSelectors().has(normalizeSelector(rule.selector)))).toEqual([]);
  });

  it("leaves out whole the global element selectors of forms.less, and serves none of them", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    expect(ELEMENT_ONLY.filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    // The normalize rules on the form elements (fieldset, legend, optgroup, the radio, checkbox and search inputs) are served by the base styles (normalize.css, slice 8), with their own test
    const normalizeRules = fileSelectors("normalize.css");
    expect(compat.filter(rule => ELEMENT_ONLY.includes(normalizeSelector(rule.selector)) && !normalizeRules.has(normalizeSelector(rule.selector)))).toEqual([]);
  });

  it("leaves out whole the focus selectors of the validation states, which carry an !important border-color, and serves none of them", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    expect(FOCUS_IMPORTANT.filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    expect(vendored.filter(rule => FOCUS_IMPORTANT.includes(rule.selector) && (rule.declarations["border-color"] || "").endsWith(" !important")).length).toBe(3);
    expect(compat.filter(rule => FOCUS_IMPORTANT.includes(normalizeSelector(rule.selector)))).toEqual([]);
  });

  it("serves every class of the family and nothing else besides the vendored selectors", () => {
    expect(CLASSES.filter(name => selectorsOf(name).length === 0)).toEqual([]);
    const served = new Set(CLASSES.flatMap(selectorsOf));
    expect(compat.map(rule => normalizeSelector(rule.selector)).filter(familyUse).filter(selector => !served.has(selector) && !isInputGroup(selector) && !laterSelectors().has(selector))).toEqual([]);
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

  describe(".form-inline .form-control width", () => {
    it("has no width: AWE comments out Bootstrap's `width: auto` in the .form-inline rule set (kept as a mixin in forms-leftovers.less), so the compiled rule and the compat rule leave the width to .form-control (100%)", () => {
      const source = fs.readFileSync(path.join(TAILWIND_DIR, "..", "less", "legacy", "forms-leftovers.less"), "utf8");
      expect(source).toMatch(/\.form-control\s*\{\s*display: inline-block;\s*\/\*width: auto;\*\//);
      [vendored, compat].forEach(rules => {
        const declarations = effective(rules, ".form-inline .form-control", "(min-width: 768px)", normalize);
        expect(declarations).toMatchObject({display: "inline-block", "vertical-align": "middle"});
        expect(declarations).not.toHaveProperty("width");
      });
    });
  });

  describe("themed values", () => {
    it("reads the --awe-* tokens that LESS compiles, with no fallback and no theme variable of the classic themes", () => {
      expectCompatTokens({compat, family, file: ["forms.css", "forms-pixel-admin.css"]});
    });
  });

  describe("source use", () => {
    const usedTokens = () => new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8")
      .matchAll(/(?<![\w-])(form-[a-z]+(?:-[a-z]+)*|control-label|help-block|has-feedback|input-sm|input-lg|sr-only)(?![\w-])/g)].map(match => match[1])));

    it("serves every form class that the templates, the directives and the XML screens write, but the ones left out", () => {
      const used = [...usedTokens()];
      expect(used).toEqual(expect.arrayContaining(["form-group", "form-control", "control-label", "has-feedback", "form-control-feedback", "sr-only"]));
      expect(used.filter(name => !CLASSES.includes(name) && !isLeftOut(name) && !["form-actions", "form-only"].includes(name))).toEqual([]);
      expect([...usedXmlClasses().keys()].filter(name => CLASSES.includes(name))).toEqual(expect.arrayContaining(["form-group"]));
    });

    it("leaves form-actions out: the vendored build only styles it inside the sign-in, change-password and TOTP screens (never on its own)", () => {
      const rules = vendored.filter(rule => /\.form-actions(?![\w-])/.test(rule.selector));
      expect(rules.length).toBeGreaterThan(0);
      // AWE 5 (#885) scopes it with the screen form (.signin-form, .signin-with, .totp-form...): no rule styles it unscoped
      expect(rules.filter(rule => /^\.form-actions(?![\w-])/.test(rule.selector))).toEqual([]);
    });
  });

  describe("cascade order", () => {
    it("lets the same rule win as in the vendored build wherever two rules of the family with the same specificity compete", () => {
      const winners = orderWinners(vendored, family.filter(selector => !overridden.has(selector)), normalize);
      expect(Object.keys(winners).length).toBeGreaterThan(5);
      const common = commonWinners(orderWinners(compat, family.filter(selector => !overridden.has(selector)), normalize), winners);
      expect(common.actual).toEqual(common.expected);
      expect(Object.keys(common.actual).length).toBeGreaterThan(0);
    });
  });

  describe("!important and the cascade layers", () => {
    it("serves no !important: the only ones of the family are the focus selectors and the screens' selectors, all left in LESS", () => {
      const own = new Set(family);
      expect(vendored.filter(rule => own.has(normalizeSelector(rule.selector)) && Object.values(rule.declarations).some(value => value.endsWith(" !important")))).toEqual([]);
      expect(compat.filter(rule => own.has(normalizeSelector(rule.selector)) && Object.values(rule.declarations).some(value => value.endsWith(" !important")))).toEqual([]);
    });

    it("has no !important to meet an unlayered one on an element: nothing served here carries it", () => {
      const {important, conflicts} = importantConflicts(vendored, compat, CLASSES, selectorsOf, normalize);
      expect(important).toEqual([]);
      expect(conflicts).toEqual([]);
    });
  });

  describe("import order", () => {
    const index = () => fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8");
    const imports = () => [...index().matchAll(/^@import\s+"\.\/([\w.-]+\.css)";/gm)].map(match => match[1]);

    it("imports forms.css (Bootstrap) after the grid and before the utilities, the panels, the spacing and the buttons, as the vendored build orders them", () => {
      expect(imports()).toContain("forms.css");
      expect(imports().indexOf("forms.css")).toBeGreaterThan(imports().indexOf("grid.generated.css"));
      ["utilities.css", "panels.css", "spacing.css", "buttons.css", "button-groups.css"].forEach(file => {
        expect(imports().indexOf(file)).toBeGreaterThan(imports().indexOf("forms.css"));
      });
      expect(imports().indexOf("grid.generated.css")).toBeGreaterThan(-1);
    });

    it("imports forms-pixel-admin.css after the buttons and the button groups: the vendored build compiles the pixel-admin form rules after them", () => {
      expect(imports()).toContain("forms-pixel-admin.css");
      ["buttons.css", "button-groups.css"].forEach(file => expect(imports().indexOf("forms-pixel-admin.css")).toBeGreaterThan(imports().indexOf(file)));
    });

    it("lets the same rule win as in the vendored build where a form rule and a single-class rule of another compat family have the same specificity", () => {
      const familySet = new Set(family);
      // Classes that the templates, directives and screens write in the same string as a form class: the only ones that can meet it on an element
      const together = new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/["'`]([^"'`\n]*)["'`]/g)]
        .map(match => match[1].split(/\s+/).filter(Boolean)).filter(words => words.some(word => CLASSES.includes(word))).flat()));
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
