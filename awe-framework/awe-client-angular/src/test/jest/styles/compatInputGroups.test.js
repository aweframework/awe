import fs from "fs";
import path from "path";
import {overriddenSelectors, vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileStyles} from "./support/compileStyles";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";
import {expectCompatTokens} from "./support/compatFamily";
import {commonWinners, shape, effective, hoistSupported, mediasOf, namesOf, normalizeSelector, orderWinners} from "./support/compatEquivalence";
import {importantConflicts} from "./support/importantGuard";
import {sourceFiles} from "./support/xmlScreens";
import {fileRules, laterSelectors} from "./support/compatFile";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

// Bootstrap 3 input-groups.less and the pixel-admin / AWE rules on the same selectors, plus the selectors of the forms and the button groups that
// mention an input group (.has-* .input-group-addon, .form-inline .input-group, .btn-toolbar .input-group), which the earlier slices left for this one
const GROUP = ["input-group", "input-group-addon", "input-group-btn", "input-group-lg", "input-group-sm"];
// Classes of the other families that these selectors pair with: a rule is served when every class of its selector is in CLASSES
const CONTEXT = ["form-control", "form-inline", "btn", "btn-group", "dropdown-toggle", "btn-toolbar", "input-sm", "input-lg", "has-success", "has-warning", "has-error"];
const CLASSES = [...GROUP, ...CONTEXT];

// A rule with a class outside CLASSES keeps its whole selector in LESS (a rule never half moves). `examples` are vendored selectors of the entry:
// each must still exist in the LESS build and must not be served, so a rule that a later slice moves cannot be accepted silently by a generic name
const LEFT_OUT = [
  {names: ["navbar-form"], examples: [".navbar-form .input-group", ".navbar-form .input-group > .form-control"],
    reason: "navbar forms belong to the navbar family, which has its own slice (the same rules as .form-inline, scoped to the navbar)"},
  {names: ["btn-outline", "btn-flat", "btn-primary"], examples: [".input-group-btn > .btn.btn-outline", ".input-group-btn > .btn.btn-outline.btn-flat.btn-primary:focus"],
    reason: "the pixel-admin outline and flat button modifiers inside an input group button, which stay in LESS with the button modifiers"},
  {names: ["awe-icon", "grid", "ui-grid-cell", "edition", "signin-form", "change-password-form", "totp-form"], examples: [".input-group-addon .awe-icon", ".grid .ui-grid-cell .edition .input-group-addon", ".signin-form .form-group.w-icon .input-group-lg > input"],
    reason: "rules of the AWE 5 design system (#885) for the addon icons, the grid edition and the sign-in screens: they are written in the AWE LESS"},
  {names: ["dark", "no-background"], examples: [".has-error.dark .input-group-addon", ".input-group-addon.no-background"],
    reason: "the themed dark validation addon and the no-background addon, which carries an !important background that a layered copy would turn into a conflict"},
  {names: ["fa"], examples: [".input-group-addon > .fa"], reason: "the icon library class: the addon icon size stays with the icon styles"},
  {names: ["panel-heading-controls", "panel-dark"], examples: [".panel-heading-controls .input-group", ".panel-dark .panel-heading-controls .input-group .form-control"],
    reason: "input groups placed in a panel heading (size and border colors): they belong to the panel family"},
  {names: ["criterion"], examples: [".criterion .input-group", ".criterion .input-group-addon"],
    reason: "AWE criterion layout (full width group, addon width and sizes): scoped to the criterion directive, which owns its sizes"},
  {names: ["select2-container", "select2-choice", "select2-choices", "select2-search-choice", "select2-search-field"],
    examples: [".form-group .input-group-sm .select2-container .select2-choice", ".form-group .input-group-sm .select2-container .select2-choices .select2-search-choice"],
    reason: "the select2 plugin renders its own control inside an input group: its heights and paddings stay with the plugin styles"},
  {names: ["form-group", "w-icon"], examples: [".form-group.w-icon .input-group > input"],
    reason: "the AWE form-group with an icon sets an !important left padding on the input of a group: it stays with the criterion directives"},
  {names: ["colorpicker-element", "transparent", "disabled", "date", "bootstrap-timepicker"],
    examples: [".colorpicker-element .input-group-addon i", ".colorpicker-element .input-group-addon.disabled", ".date .input-group-addon", ".bootstrap-timepicker .input-group-addon"],
    reason: "the color, date and time picker plugins restyle the addon they are attached to: they stay with the plugin styles"},
  {names: ["pivotTable", "input-group-md"], examples: [".pivotTable .input-group-sm select", ".pivotTable .input-group-md select", ".pivotTable .input-group-lg select"],
    reason: "the pivot table plugin sizes the selects inside its own input groups (it also has an input-group-md that Bootstrap does not define)"}
];
const isLeftOut = name => LEFT_OUT.some(entry => (entry.names ? entry.names.includes(name) : entry.match(name)));
const hasDeclarations = rule => Object.keys(rule.declarations).length > 0;
// Any class that starts with input-group counts (also one that Bootstrap does not define, such as input-group-md), so a rule on it cannot be served unaccounted
const familyUse = selector => namesOf(selector).some(name => name.startsWith("input-group"));
const inFamily = selector => familyUse(selector) && namesOf(selector).every(name => CLASSES.includes(name));

// Tailwind's output has the same values, written as lightningcss prints them
const normalize = value => normalizeLength(value.replace(/\b0px\b/g, "0")).replace(/\s*,\s*/g, ", ").replace(/(?<![\d])0\.(\d)/g, ".$1");

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatInputGroups", () => {
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

  it("compares with the vendored Bootstrap input group rules, still in the LESS build", () => {
    expect(vendored.some(rule => rule.selector === ".input-group-addon:first-child")).toBe(true);
  });

  it("accounts for every vendored selector that mentions an input group class: served here or left out for a stated reason", () => {
    const outside = [...new Set(vendored.filter(rule => familyUse(rule.selector)).flatMap(rule => namesOf(rule.selector)))].filter(name => !CLASSES.includes(name));
    expect(outside.filter(name => !isLeftOut(name))).toEqual([]);
    const named = LEFT_OUT.flatMap(entry => entry.names || []);
    expect(named.filter(name => !outside.includes(name))).toEqual([]);
    LEFT_OUT.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
  });

  it("keeps in LESS the example selectors of every left-out entry: they exist in the vendored build and none is served", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    const examples = LEFT_OUT.flatMap(entry => entry.examples || []);
    expect(LEFT_OUT.filter(entry => !entry.examples || entry.examples.length === 0)).toEqual([]);
    expect(examples.filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    // (slice 5) the navbar form selectors that this family left for the navbar moved to navbar.css, with its own test
    expect(compat.filter(rule => examples.includes(normalizeSelector(rule.selector)) && !laterSelectors().has(normalizeSelector(rule.selector)))).toEqual([]);
  });

  it("serves every class of the family and nothing else that mentions an input group", () => {
    expect(CLASSES.filter(name => selectorsOf(name).length === 0)).toEqual([]);
    const served = new Set(family);
    expect(compat.map(rule => normalizeSelector(rule.selector)).filter(familyUse).filter(selector => !served.has(selector) && !laterSelectors().has(selector))).toEqual([]);
  });

  describe.each(GROUP)("%s", name => {
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

  describe("selectors written twice (the Bootstrap copy and the pixel-admin copy)", () => {
    // The vendored build compiles the pixel-admin form rules from this one on (the first rule of forms-pixel-admin.css)
    const pixelStart = rules => rules.findIndex(rule => rule.selector === ".form-inline .form-control" && rule.declarations["margin-bottom"] === "10px");
    const copies = () => {
      const boundary = pixelStart(vendored);
      const own = new Set(family);
      const rules = vendored.map((rule, index) => ({...rule, selector: normalizeSelector(rule.selector), index})).filter(rule => own.has(rule.selector) && hasDeclarations(rule));
      // The addon rules of the AWE 5 design system (#885, field tokens) come after the pixel-admin ones: they are written in the AWE LESS
      const awe5 = rule => /--awe-(?:color-field|field|size-control)/.test(JSON.stringify(rule.declarations));
      return {boundary, bootstrap: rules.filter(rule => rule.index < boundary), pixel: rules.filter(rule => rule.index >= boundary && !awe5(rule))};
    };
    // The values follow the AWE 5 tokens: a rule is its selector, its media query and the properties it sets
    const key = rule => JSON.stringify([rule.selector, rule.media, Object.keys(rule.declarations).sort()]);
    const fileKeys = file => fileRules(file).map(key);

    it("has selectors with both copies, so the check below is not empty", () => {
      const {bootstrap, pixel} = copies();
      const twice = [...new Set(pixel.map(rule => rule.selector))].filter(selector => bootstrap.some(rule => rule.selector === selector));
      expect(twice).toEqual(expect.arrayContaining([".has-error .input-group-addon", ".input-group-btn > .btn"]));
    });

    it("serves each copy in its own file: Bootstrap in input-groups.css, pixel-admin in input-groups-pixel-admin.css, and in neither file the other copy", () => {
      const {bootstrap, pixel} = copies();
      const bootstrapKeys = fileKeys("input-groups.css");
      const pixelKeys = fileKeys("input-groups-pixel-admin.css");
      expect(bootstrap.map(key).filter(rule => !bootstrapKeys.includes(rule))).toEqual([]);
      expect(pixel.map(key).filter(rule => !pixelKeys.includes(rule))).toEqual([]);
      expect(pixel.map(key).filter(rule => bootstrapKeys.includes(rule) && !bootstrap.map(key).includes(rule))).toEqual([]);
      expect(bootstrap.map(key).filter(rule => pixelKeys.includes(rule) && !pixel.map(key).includes(rule))).toEqual([]);
      expect(bootstrapKeys.length).toBe(bootstrap.length);
      expect(pixelKeys.length).toBe(pixel.length);
    });

    it("keeps the vendored order of the copies inside each file", () => {
      const {bootstrap, pixel} = copies();
      expect(fileKeys("input-groups.css")).toEqual(bootstrap.map(key));
      expect(fileKeys("input-groups-pixel-admin.css")).toEqual(pixel.map(key));
    });
  });

  describe("themed values", () => {
    it("reads the --awe-* tokens that LESS compiles, with no fallback and no theme variable of the classic themes", () => {
      expectCompatTokens({compat, family, file: ["input-groups.css", "input-groups-pixel-admin.css"]});
    });
  });

  describe("source use", () => {
    it("serves every input group class that the templates, the directives and the XML screens write, but the ones left out", () => {
      const used = new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/(?<![\w-])(input-group(?:-[a-z]+)*)(?![\w-])/g)].map(match => match[1])));
      expect([...used]).toEqual(expect.arrayContaining(["input-group", "input-group-addon"]));
      expect([...used].filter(name => !CLASSES.includes(name) && !isLeftOut(name))).toEqual([]);
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
    it("serves no !important: the ones of the family (.no-background, the criterion padding) are left in LESS", () => {
      const own = new Set(family);
      expect(vendored.filter(rule => own.has(normalizeSelector(rule.selector)) && Object.values(rule.declarations).some(value => value.endsWith(" !important")))).toEqual([]);
      expect(compat.filter(rule => own.has(normalizeSelector(rule.selector)) && Object.values(rule.declarations).some(value => value.endsWith(" !important")))).toEqual([]);
    });

    it("has no !important to meet an unlayered one on an element: nothing served here carries it", () => {
      const {important, conflicts} = importantConflicts(vendored, compat, GROUP, selectorsOf, normalize);
      expect(important).toEqual([]);
      expect(conflicts).toEqual([]);
    });
  });

  describe("import order", () => {
    const index = () => fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8");
    const imports = () => [...index().matchAll(/^@import\s+"\.\/([\w.-]+\.css)";/gm)].map(match => match[1]);

    it("imports input-groups.css (Bootstrap) after the buttons and the button groups and before forms-pixel-admin.css, as the vendored build orders them", () => {
      expect(imports()).toContain("input-groups.css");
      ["buttons.css", "button-groups.css"].forEach(file => expect(imports().indexOf("input-groups.css")).toBeGreaterThan(imports().indexOf(file)));
      expect(imports().indexOf("forms-pixel-admin.css")).toBeGreaterThan(imports().indexOf("input-groups.css"));
    });

    it("imports input-groups-pixel-admin.css after forms-pixel-admin.css: the vendored build compiles the pixel-admin input group rules after the pixel-admin form rules", () => {
      expect(imports()).toContain("input-groups-pixel-admin.css");
      expect(imports().indexOf("input-groups-pixel-admin.css")).toBeGreaterThan(imports().indexOf("forms-pixel-admin.css"));
    });

    it("lets the same rule win as in the vendored build where an input group rule and a single-class rule of another compat family have the same specificity", () => {
      const familySet = new Set(family);
      const together = new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/["'`]([^"'`\n]*)["'`]/g)]
        .map(match => match[1].split(/\s+/).filter(Boolean)).filter(words => words.some(word => GROUP.includes(word))).flat()));
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
