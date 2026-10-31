import {Buffer} from "buffer";
import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {compileTailwind, getRules} from "./support/compileTailwind";
import {callsOf, extendsOf, ruleSetsOf, unkeptExtends} from "./support/lessCalls";
import {digestOf, hashOf, readBaseline, restoreRemoved} from "./support/vendoredBeforeRemoval";
import {sourceFiles, usedXmlClasses, walk} from "./support/xmlScreens";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

const BOOTSTRAP = path.join(LESS_DIR, "legacy");
const REMOVED_FILES = ["grid.less", "panels.less", "utilities.less", "responsive-utilities.less"];
// Selectors the compat layer does not repeat, on purpose: the Bootstrap affix plugin class, which no AWE screen, template or directive uses
const NOT_SERVED = [".affix"];
const REVIEWED_EXTEND = "btn-lg";
// What stays of the second removal (buttons, button groups, forms and input groups): the rules that the cascade guard found must stay unlayered
const LEFTOVERS = ["forms-leftovers.less", "buttons-leftovers.less", "button-groups-leftovers.less", "input-groups-leftovers.less"];
// What stays of the third removal (component animations, dropdowns, navs, navbar, close button, modals, tooltip, popovers), in the order legacy.less imports it
const LEFTOVERS3 = ["component-animations-leftovers.less", "dropdowns-leftovers.less", "navs-leftovers.less", "navbar-leftovers.less", "close-leftovers.less", "modals-leftovers.less",
  "tooltip-leftovers.less", "popovers-leftovers.less"];

// What stays of the fourth removal (tables, breadcrumbs, pagination, labels, badges, alerts, wells; progress-bars.less leaves nothing), in the order legacy.less imports it
const LEFTOVERS4 = ["tables-leftovers.less", "breadcrumbs-leftovers.less", "pagination-leftovers.less", "labels-leftovers.less", "badges-leftovers.less", "alerts-leftovers.less", "wells-leftovers.less"];
const STEP4_FILES = ["tables.less", "labels.less", "badges.less", "wells.less", "alerts.less", "breadcrumbs.less", "progress-bars.less", "pagination.less"];

// What stayed of the fifth removal (pager, media, jumbotron, carousel, glyphicons, and the .pre-scrollable of code.less) was only the element rules of code.less (code-leftovers.less), which
// the seventh removal took over. That step drops its families with no compat CSS, because nothing writes their classes (unusedBootstrapFamilies.test.js): it is `unserved` in the baseline
const LEFTOVERS5 = [];
const STEP5_FILES = ["pager.less", "media.less", "jumbotron.less", "carousel.less", "glyphicons.less", "code.less"];

// What stays of the sixth removal (list groups, thumbnails, responsive embeds, served by the compat layer since something still writes them), in the order legacy.less imports it

const LEFTOVERS6 = ["thumbnails-leftovers.less", "list-group-leftovers.less", "responsive-embed-leftovers.less"];
const STEP6_FILES = ["list-group.less", "thumbnails.less", "responsive-embed.less"];

// What stays of the seventh removal (the base styles: normalize, print, scaffolding, type and the element rules of code), in the order legacy.less imports it
const LEFTOVERS7 = ["normalize-leftovers.less", "print-leftovers.less", "scaffolding-leftovers.less", "type-leftovers.less", "pre-leftovers.less"];
const STEP7_FILES = ["normalize.less", "print.less", "scaffolding.less", "type.less", "code-leftovers.less"];

// The byte counts of the baseline are the LF ones, as the CI builds them: a Windows checkout (core.autocrlf) has CRLF sources and the preserved comments carry them into the CSS
const lfBytes = text => Buffer.byteLength(text.replace(/\r\n/g, "\n"));

// The LESS that the merge of develop into the final #755 branch brought in (card, grid groups, quick filters, selection bar,
// sign in, busy buttons...): compiled size of the awe.less build at the merge minus the one of the branch before it. The
// baseline sizes are from before it, so the "smaller than before the removal" checks measure the build without that growth.
// The sync of the map component (#882: leaflet.css and less/awe/directives/map.less) added 15365 bytes more (compiled awe.less on LF: 892109 before the sync, 907474 after), 83225 + 15365
// The advanced search of windows (#790: less/awe/directives/window-advanced.less) added 5762 bytes more (907474 before, 913236 after), 98590 + 5762
// The saved searches of windows (#790: less/awe/directives/window-saved-searches.less) added 7319 bytes more (913236 before, 920555 after), 104352 + 7319
const DEVELOP_LESS_BYTES = 111671;
const ownBytes = text => lfBytes(text) - DEVELOP_LESS_BYTES;

const normalizeSelector = selector => selector.replace(/::(before|after)$/, ":$1");
const classesOf = css => new Set([...css.replace(/\/\*[\s\S]*?\*\//g, "").matchAll(/\.(-?[_a-zA-Z][\w-]*)/g)].map(match => match[1]));

describe("awe-framework/awe-client-angular/src/test/jest/styles/vendoredRemoval", () => {
  let css;
  let current;
  let compat;
  let baseline;

  beforeAll(async () => {
    css = await compileStyles();
    current = getRules(css);
    compat = getRules(await compileTailwind());
    baseline = readBaseline();
  });

  // The same rule as vendoredBeforeRemoval: a removal is applied to the LESS sources when every vendored file of its header is gone
  const appliedAfter = first => baseline.steps.filter(entry => entry.step > first && entry.files.every(file => !fs.existsSync(path.join(BOOTSTRAP, file)))).map(entry => entry.step);
  const frozenOf = steps => baseline.removed.filter(rule => steps.includes(rule.step));

  describe("the vendored Bootstrap grid, panels, utilities and responsive utilities", () => {
    it("are not in the LESS sources any more, nor imported by legacy.less", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      REMOVED_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
    });

    it("have none of their compiled rules in the vendored build", () => {
      const present = new Set(current.map(hashOf));
      const first = baseline.removed.filter(rule => rule.step === 1);
      expect(first.length).toBeGreaterThan(300);
      expect(first.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
    });

    it("compile to a smaller LESS build than before the removal", () => {
      const bytes = ownBytes(css);
      expect(bytes).toBeLessThan(baseline.lessBytesBefore);
      expect(baseline.lessBytesBefore - bytes).toBeGreaterThan(10000);
    });
  });

  describe("the vendored Bootstrap buttons, button groups, forms and input groups (step 2)", () => {
    const STEP2_FILES = ["buttons.less", "button-groups.less", "forms.less", "input-groups.less"];

    it("are not in the LESS sources any more, and legacy.less imports their leftovers files where they were, in the same order", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      STEP2_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
      const positions = LEFTOVERS.map(file => entry.indexOf(`@import "${file}";`));
      expect(positions.every(position => position > 0)).toBe(true);
      // As the vendored imports were: forms, buttons, then the dropdowns, the button groups and the input groups
      expect(positions[0]).toBeLessThan(positions[1]);
      expect(positions[1]).toBeLessThan(entry.indexOf('@import "dropdowns-leftovers.less";'));
      expect(entry.indexOf('@import "dropdowns-leftovers.less";')).toBeLessThan(positions[2]);
      expect(positions[2]).toBeLessThan(positions[3]);
      expect(positions[3]).toBeLessThan(entry.indexOf('@import "navs-leftovers.less";'));
    });

    it("have none of their frozen compiled rules in the vendored build, and the leftovers files print the rest", () => {
      const present = new Set(current.map(hashOf));
      const second = baseline.removed.filter(rule => rule.step === 2);
      expect(second.length).toBeGreaterThan(100);
      expect(second.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
      LEFTOVERS.forEach(file => expect(fs.readFileSync(path.join(BOOTSTRAP, file), "utf8").length).toBeGreaterThan(500));
      // What the leftovers keep unlayered is what the cascade guard found: the frozen rules plus the leftovers are the four files
      // One sample selector per kind of rule that stays: the disabled button colors (buttons), a pixel-admin state (buttons), a rule of a
      // size of the input groups, a global element rule that the compat layer leaves out (forms), and the base rules of the controls and the buttons
      const keptSelectors = [".btn-default.disabled", ".btn:active:focus", ".input-group-lg > .input-group-btn > .btn", "fieldset", ".form-control", ".btn"];
      keptSelectors.forEach(selector => expect({selector, kept: current.some(rule => rule.selector === selector)}).toEqual({selector, kept: true}));
    });

    it("keep in the build, unlayered, every rule that the cascade guard found would lose to a rule that stays (the leftovers)", () => {
      const present = new Set(current.map(hashOf));
      const {kept} = baseline.steps.find(entry => entry.step === 2);
      expect(kept.length).toBeGreaterThan(150);
      expect(kept.filter(hash => !present.has(hash))).toEqual([]);
    });

    it("print every kept rule from a leftovers file", async () => {
      // Compile with the leftovers imported as a reference (their rules are not written): the rules that vanish are the ones they print.
      // The copy sits beside the LESS folder: awe.less imports the shared design tokens with a relative path
      const copy = fs.mkdtempSync(path.join(path.dirname(LESS_DIR), "leftovers-"));
      try {
        fs.cpSync(LESS_DIR, copy, {recursive: true});
        const entry = path.join(copy, "legacy", "legacy.less");
        const source = LEFTOVERS.reduce((text, file) => text.replace(`@import "${file}";`, `@import (reference) "${file}";`), fs.readFileSync(entry, "utf8"));
        expect(source.match(/@import \(reference\) "[\w-]+-leftovers\.less"/g)).toHaveLength(LEFTOVERS.length);
        fs.writeFileSync(entry, source);
        const awe = path.join(copy, "awe.less");
        const without = new Set(getRules(await compileStyles(fs.readFileSync(awe, "utf8"), {filename: awe})).map(hashOf));
        const {kept} = baseline.steps.find(step => step.step === 2);
        expect(kept.filter(hash => without.has(hash))).toEqual([]);
      } finally {
        fs.rmSync(copy, {recursive: true, force: true});
      }
    });

    it("compile to a smaller LESS build than before the removal", () => {
      const bytes = ownBytes(css);
      const step = baseline.steps.find(entry => entry.step === 2);
      expect(bytes).toBeLessThan(step.lessBytesBefore);
      expect(step.lessBytesBefore - bytes).toBeGreaterThan(5000);
    });
  });

  describe("the vendored Bootstrap component animations, dropdowns, navs, navbar, close button, modals, tooltip and popovers (step 3)", () => {
    const STEP3_FILES = ["component-animations.less", "dropdowns.less", "navs.less", "navbar.less", "close.less", "modals.less", "tooltip.less", "popovers.less"];
    const header = () => baseline.steps.find(entry => entry.step === 3);

    it("are not in the LESS sources any more, and legacy.less imports their leftovers files where they were, in the same order", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      STEP3_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
      const positions = LEFTOVERS3.map(file => entry.indexOf(`@import "${file}";`));
      expect(positions.every(position => position > 0)).toBe(true);
      // As the vendored imports were: the animations, the dropdowns, (the button group and input group leftovers), the navs, the navbar, ...the close button, the modals, the tooltip and the popovers
      expect([...positions].sort((a, b) => a - b)).toEqual(positions);
      expect(positions[1]).toBeLessThan(entry.indexOf('@import "button-groups-leftovers.less";'));
      expect(entry.indexOf('@import "input-groups-leftovers.less";')).toBeLessThan(positions[2]);
      expect(positions[3]).toBeLessThan(entry.indexOf('@import "breadcrumbs-leftovers.less";'));
      expect(entry.indexOf('@import "wells-leftovers.less";')).toBeLessThan(positions[4]);
      expect(positions[7]).toBeLessThan(entry.indexOf("// Utility classes"));
    });

    it("have none of their frozen compiled rules in the vendored build, and the leftovers files print the rest", () => {
      const present = new Set(current.map(hashOf));
      const third = baseline.removed.filter(rule => rule.step === 3);
      expect(third.length).toBeGreaterThan(30);
      expect(third.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
      LEFTOVERS3.forEach(file => expect(fs.readFileSync(path.join(BOOTSTRAP, file), "utf8").length).toBeGreaterThan(150));
      // One sample selector per kind of rule that stays: the dropdown menu and its open state, the tabs, the navbar links, the close button, the modal title, the fade and tooltip states and the popover arrow
      const keptSelectors = [".dropdown-menu", ".open > .dropdown-menu", ".nav-tabs > li > a", ".navbar-nav", ".navbar-default .navbar-nav > .active > a", ".close", ".modal-title", ".fade.in", ".tooltip.in", ".popover.top > .arrow"];
      keptSelectors.forEach(selector => expect({selector, kept: current.some(rule => rule.selector === selector)}).toEqual({selector, kept: true}));
    });

    it("print every kept rule from a leftovers file", async () => {
      // Compile with the leftovers imported as a reference (their rules are not written): the rules that vanish are the ones they print.
      // The copy sits beside the LESS folder: awe.less imports the shared design tokens with a relative path
      const copy = fs.mkdtempSync(path.join(path.dirname(LESS_DIR), "leftovers3-"));
      try {
        fs.cpSync(LESS_DIR, copy, {recursive: true});
        const entry = path.join(copy, "legacy", "legacy.less");
        const source = LEFTOVERS3.reduce((text, file) => text.replace(`@import "${file}";`, `@import (reference) "${file}";`), fs.readFileSync(entry, "utf8"));
        expect(source.match(/@import \(reference\) "[\w-]+-leftovers\.less"/g).filter(line => LEFTOVERS3.some(file => line.includes(`"${file}"`)))).toHaveLength(LEFTOVERS3.length);
        fs.writeFileSync(entry, source);
        const awe = path.join(copy, "awe.less");
        const without = new Set(getRules(await compileStyles(fs.readFileSync(awe, "utf8"), {filename: awe})).map(hashOf));
        expect(header().kept.filter(hash => without.has(hash))).toEqual([]);
      } finally {
        fs.rmSync(copy, {recursive: true, force: true});
      }
    });

    it("compile to a smaller LESS build than before the removal", () => {
      const bytes = ownBytes(css);
      expect(bytes).toBeLessThan(header().lessBytesBefore);
      expect(header().lessBytesBefore - bytes).toBeGreaterThan(3000);
    });
  });

  describe("the vendored Bootstrap tables, labels, badges, wells, alerts, breadcrumbs, progress bars and pagination (step 4)", () => {
    const header = () => baseline.steps.find(entry => entry.step === 4);

    it("are not in the LESS sources any more, and legacy.less imports their leftovers files where they were, in the same order (progress bars leave nothing)", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      STEP4_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
      expect(entry).not.toContain("progress-bars");
      const positions = LEFTOVERS4.map(file => entry.indexOf(`@import "${file}";`));
      expect(positions.every(position => position > 0)).toBe(true);
      // As the vendored imports were: the tables (before the forms), then the breadcrumbs, the pagination, the labels, the badges, the alerts and the wells
      expect(positions[0]).toBeLessThan(entry.indexOf('@import "forms-leftovers.less";'));
      expect(entry.indexOf('@import "navbar-leftovers.less";')).toBeLessThan(positions[1]);
      expect([...positions.slice(1, 6)].sort((a, b) => a - b)).toEqual(positions.slice(1, 6));
      expect(positions[5]).toBeLessThan(entry.indexOf('@import "list-group-leftovers.less";'));
      expect(entry.indexOf('@import "panels-leftovers.less";')).toBeLessThan(positions[6]);
      expect(positions[6]).toBeLessThan(entry.indexOf('@import "close-leftovers.less";'));
    });

    it("have none of their frozen compiled rules in the vendored build, and the leftovers files print the rest", () => {
      const present = new Set(current.map(hashOf));
      const fourth = baseline.removed.filter(rule => rule.step === 4);
      expect(fourth.length).toBeGreaterThan(30);
      expect(fourth.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
      LEFTOVERS4.forEach(file => expect(fs.readFileSync(path.join(BOOTSTRAP, file), "utf8").length).toBeGreaterThan(300));
      // One sample selector per kind of rule that stays: the element rules, the cells of the table, the responsive table, the pagination links, the label link states, the badge, the alert heading and the well
      const keptSelectors = ["table", "th", ".table > thead > tr > th", ".table-hover > tbody > tr:hover", ".table-responsive", ".pagination > li > a", ".label-primary[href]:hover", ".badge", ".alert h4", ".breadcrumb > li", ".well"];
      keptSelectors.forEach(selector => expect({selector, kept: current.some(rule => rule.selector === selector)}).toEqual({selector, kept: true}));
    });

    it("print every kept rule from a leftovers file", async () => {
      // Compile with the leftovers imported as a reference (their rules are not written): the rules that vanish are the ones they print.
      // The copy sits beside the LESS folder: awe.less imports the shared design tokens with a relative path
      const copy = fs.mkdtempSync(path.join(path.dirname(LESS_DIR), "leftovers4-"));
      try {
        fs.cpSync(LESS_DIR, copy, {recursive: true});
        const entry = path.join(copy, "legacy", "legacy.less");
        const source = LEFTOVERS4.reduce((text, file) => text.replace(`@import "${file}";`, `@import (reference) "${file}";`), fs.readFileSync(entry, "utf8"));
        expect(source.match(/@import \(reference\) "[\w-]+-leftovers\.less"/g).filter(line => LEFTOVERS4.some(file => line.includes(`"${file}"`)))).toHaveLength(LEFTOVERS4.length);
        fs.writeFileSync(entry, source);
        const awe = path.join(copy, "awe.less");
        const without = new Set(getRules(await compileStyles(fs.readFileSync(awe, "utf8"), {filename: awe})).map(hashOf));
        expect(header().kept.filter(hash => without.has(hash))).toEqual([]);
      } finally {
        fs.rmSync(copy, {recursive: true, force: true});
      }
    });

    it("compile to a smaller LESS build than before the removal", () => {
      const bytes = ownBytes(css);
      expect(bytes).toBeLessThan(header().lessBytesBefore);
      expect(header().lessBytesBefore - bytes).toBeGreaterThan(3000);
    });

    it("keeps the rule that pixel-admin repeats (.list-group-item > .badge + .badge) in the leftovers, so the build is the vendored one minus the frozen rules", () => {
      expect(fs.readFileSync(path.join(BOOTSTRAP, "badges-leftovers.less"), "utf8")).toContain(".list-group-item > .badge + .badge {");
      expect(current.filter(rule => rule.selector === ".list-group-item > .badge + .badge")).toHaveLength(2);
    });
  });

  describe("the vendored Bootstrap pager, media, jumbotron, carousel, glyphicons and code helpers, dropped with no compat CSS (step 5)", () => {
    const header = () => baseline.steps.find(entry => entry.step === 5);

    it("are not in the LESS sources any more, nor imported by legacy.less", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      STEP5_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
      // theme.less was never imported (the optional Bootstrap theme): nothing referenced it
      expect(fs.existsSync(path.join(BOOTSTRAP, "theme.less"))).toBe(false);
      // What something writes was served by the compat layer in step 6: the notifier list group, the file manager embed and its (third party) thumbnails
      LEFTOVERS6.forEach(file => expect(entry).toContain(`@import "${file}";`));
    });

    it("leave no icon font behind: the glyphicons-halflings files of the client and of awe-tools are gone, and nothing names them", () => {
      expect(fs.existsSync(path.join(BOOTSTRAP, "fonts"))).toBe(false);
      expect(fs.existsSync(path.join(BOOTSTRAP, "..", "..", "..", "..", "..", "..", "..", "awe-modules", "awe-tools", "src", "main", "resources", "fonts"))).toBe(false);
      expect(css).not.toMatch(/glyphicon|halflings/i);
    });

    it("have none of their frozen compiled rules in the vendored build, and the block of code that the seventh removal keeps is still printed", () => {
      const present = new Set(current.map(hashOf));
      const fifth = baseline.removed.filter(rule => rule.step === 5);
      expect(fifth.length).toBeGreaterThan(100);
      expect(fifth.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
      ["pre"].forEach(selector => expect({selector, kept: current.some(rule => rule.selector === selector)}).toEqual({selector, kept: true}));
      expect(current.some(rule => rule.selector === ".pre-scrollable")).toBe(false);
    });

    it("compile to a smaller LESS build than before the removal", () => {
      // The build before this removal is the one of the base styles removal too, when it is applied: its size is the one frozen for the last removal that is applied
      const later = appliedAfter(5);
      if (later.length === 0) {
        const bytes = ownBytes(css);
        expect(bytes).toBeLessThan(header().lessBytesBefore);
        expect(header().lessBytesBefore - bytes).toBeGreaterThan(8000);
      } else {
        expect(header().lessBytesAfter).toBeLessThan(header().lessBytesBefore);
        expect(header().lessBytesBefore - header().lessBytesAfter).toBeGreaterThan(8000);
      }
    });
  });

  describe("the vendored Bootstrap list groups, thumbnails and responsive embeds, served by the compat layer (step 6)", () => {
    const header = () => baseline.steps.find(entry => entry.step === 6);

    it("are not in the LESS sources any more, and legacy.less imports their leftovers files where they were, in the same order", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      STEP6_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
      const positions = LEFTOVERS6.map(file => entry.indexOf(`@import "${file}";`));
      expect(positions.every(position => position > 0)).toBe(true);
      // As the vendored imports were: the thumbnails after the badges, then the alerts, the list group, the panels, the responsive embed and the wells
      expect(entry.indexOf('@import "badges-leftovers.less";')).toBeLessThan(positions[0]);
      expect(positions[0]).toBeLessThan(entry.indexOf('@import "alerts-leftovers.less";'));
      expect(entry.indexOf('@import "alerts-leftovers.less";')).toBeLessThan(positions[1]);
      expect(positions[1]).toBeLessThan(entry.indexOf('@import "panels-leftovers.less";'));
      expect(entry.indexOf('@import "panels-leftovers.less";')).toBeLessThan(positions[2]);
      expect(positions[2]).toBeLessThan(entry.indexOf('@import "wells-leftovers.less";'));
    });

    it("have none of their frozen compiled rules in the vendored build, and the leftovers files print the rest", () => {
      const present = new Set(current.map(hashOf));
      const sixth = baseline.removed.filter(rule => rule.step === 6);
      expect(sixth.map(rule => rule.selector)).toEqual([".thumbnail .caption", ".list-group-item:first-child"]);
      expect(sixth.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
      LEFTOVERS6.forEach(file => expect(fs.readFileSync(path.join(BOOTSTRAP, file), "utf8").length).toBeGreaterThan(300));
      // One sample selector per kind of rule that stays: the list group, the item, its link states, the contextual links, the thumbnail and its images, the embed and its items
      const keptSelectors = [".list-group", ".list-group-item", "a.list-group-item:hover", ".list-group-item.active", "a.list-group-item-success.active", ".thumbnail", ".thumbnail a > img", "a.thumbnail.active", ".embed-responsive", ".embed-responsive iframe"];
      keptSelectors.forEach(selector => expect({selector, kept: current.some(rule => rule.selector === selector)}).toEqual({selector, kept: true}));
    });

    it("print every kept rule from a leftovers file", async () => {
      const copy = fs.mkdtempSync(path.join(path.dirname(LESS_DIR), "leftovers6-"));
      try {
        fs.cpSync(LESS_DIR, copy, {recursive: true});
        const entry = path.join(copy, "legacy", "legacy.less");
        const source = LEFTOVERS6.reduce((text, file) => text.replace(`@import "${file}";`, `@import (reference) "${file}";`), fs.readFileSync(entry, "utf8"));
        expect(source.match(/@import \(reference\) "[\w-]+-leftovers\.less"/g).filter(line => LEFTOVERS6.some(file => line.includes(`"${file}"`)))).toHaveLength(LEFTOVERS6.length);
        fs.writeFileSync(entry, source);
        const awe = path.join(copy, "awe.less");
        const without = new Set(getRules(await compileStyles(fs.readFileSync(awe, "utf8"), {filename: awe})).map(hashOf));
        // The two rules that .img-responsive prints for the thumbnail images stay where they were (the extend is in the leftovers): the same rules, the same place
        expect(header().kept.filter(hash => without.has(hash))).toEqual([]);
      } finally {
        fs.rmSync(copy, {recursive: true, force: true});
      }
    });

    it("compile to a smaller LESS build than before the removal", () => {
      const later = appliedAfter(6);
      if (later.length === 0) {
        const bytes = ownBytes(css);
        expect(bytes).toBeLessThan(header().lessBytesBefore);
        expect(header().lessBytesBefore - bytes).toBeGreaterThan(100);
      } else {
        expect(header().lessBytesAfter).toBeLessThan(header().lessBytesBefore);
      }
    });

    it("keeps the extend of .img-responsive for the thumbnail images, so the rule that img-responsive prints for them is where it was", () => {
      expect(extendsOf(fs.readFileSync(path.join(BOOTSTRAP, "thumbnails-leftovers.less"), "utf8"))).toContain("img-responsive");
      expect(current.filter(rule => /^\.thumbnail (a )?> img$|^\.thumbnail > img$/.test(rule.selector) && rule.declarations["max-width"] === "100%")).toHaveLength(2);
    });
  });

  describe("the vendored Bootstrap base styles, served by the compat layer (step 7: normalize, print, scaffolding, type and the element rules of code)", () => {
    const header = () => baseline.steps.find(entry => entry.step === 7);

    it("are not in the LESS sources any more, and legacy.less imports their leftovers files where they were, in the same order", () => {
      const entry = fs.readFileSync(path.join(BOOTSTRAP, "legacy.less"), "utf8");
      STEP7_FILES.forEach(file => {
        expect({file, exists: fs.existsSync(path.join(BOOTSTRAP, file))}).toEqual({file, exists: false});
        expect(entry).not.toContain(`"${file}"`);
      });
      const positions = LEFTOVERS7.map(file => entry.indexOf(`@import "${file}";`));
      expect(positions.every(position => position > 0)).toBe(true);
      // As the vendored imports were: normalize and print first, then scaffolding, type and the element rules of code, before the grid columns
      positions.slice(1).forEach((position, index) => expect(position).toBeGreaterThan(positions[index]));
      expect(entry.indexOf('@import "variables.less";')).toBeLessThan(positions[0]);
      expect(entry.indexOf('@import "mixins.less";')).toBeLessThan(positions[0]);
      expect(positions[4]).toBeLessThan(entry.indexOf('@import "grid-columns.less";'));
    });

    it("have none of their frozen compiled rules in the vendored build, and the leftovers files print the rest", () => {
      const present = new Set(current.map(hashOf));
      const seventh = baseline.removed.filter(rule => rule.step === 7);
      expect(seventh.length).toBeGreaterThan(200);
      expect(seventh.filter(rule => present.has(hashOf(rule))).map(rule => rule.selector)).toEqual([]);
      LEFTOVERS7.forEach(file => expect(fs.readFileSync(path.join(BOOTSTRAP, file), "utf8").length).toBeGreaterThan(300));
      // One sample selector per kind of rule that stays: the root and the display roles (normalize), the print black on white and the link addresses (print),
      // the link states and the images (scaffolding), the contextual text and the blockquote (type) and the block of code
      const keptSelectors = ["html", "[hidden]", "input[type=\"search\"]", "a:hover", ".img-responsive", ".img-thumbnail", ".text-success", "a.text-primary:hover", ".dl-horizontal dd:before", "blockquote", "blockquote.pull-right", "pre"];
      keptSelectors.forEach(selector => expect({selector, kept: current.some(rule => rule.selector === selector)}).toEqual({selector, kept: true}));
      // What the compat layer serves and the build no longer prints: the headings, the body, the paragraphs, the lists and the code
      const frozenSelectors = ["h1", "body", "p", "ul", ".lead", ".sr-only", ".img-rounded", "code", "kbd", "samp", "pre code"];
      frozenSelectors.forEach(selector => expect({selector, frozen: seventh.some(rule => rule.selector === selector)}).toEqual({selector, frozen: true}));
    });

    it("print every kept rule from a leftovers file", async () => {
      const copy = fs.mkdtempSync(path.join(path.dirname(LESS_DIR), "leftovers7-"));
      try {
        fs.cpSync(LESS_DIR, copy, {recursive: true});
        const entry = path.join(copy, "legacy", "legacy.less");
        const source = LEFTOVERS7.reduce((text, file) => text.replace(`@import "${file}";`, `@import (reference) "${file}";`), fs.readFileSync(entry, "utf8"));
        expect(source.match(/@import \(reference\) "[\w-]+-leftovers\.less"/g).filter(line => LEFTOVERS7.some(file => line.includes(`"${file}"`)))).toHaveLength(LEFTOVERS7.length);
        fs.writeFileSync(entry, source);
        const awe = path.join(copy, "awe.less");
        const without = new Set(getRules(await compileStyles(fs.readFileSync(awe, "utf8"), {filename: awe})).map(hashOf));
        expect(header().kept.filter(hash => without.has(hash))).toEqual([]);
      } finally {
        fs.rmSync(copy, {recursive: true, force: true});
      }
    });

    it("compile to a smaller LESS build than before the removal", () => {
      const bytes = ownBytes(css);
      expect(bytes).toBeLessThan(header().lessBytesBefore);
      expect(header().lessBytesBefore - bytes).toBeGreaterThan(5000);
    });

    it("keeps the rule sets that the AWE icon styles call (.text-success, .text-info, .text-warning, .text-danger) and the one that the thumbnails extend (.img-responsive)", () => {
      const icons = fs.readFileSync(path.join(LESS_DIR, "awe", "icon.less"), "utf8");
      ["text-success", "text-info", "text-warning", "text-danger"].forEach(name => {
        expect(icons).toContain(`.${name};`);
        expect(callsOf(fs.readFileSync(path.join(BOOTSTRAP, "type-leftovers.less"), "utf8"))).not.toContain(name);
        expect({name, kept: current.some(rule => rule.selector === `.${name}`)}).toEqual({name, kept: true});
      });
      expect(extendsOf(fs.readFileSync(path.join(BOOTSTRAP, "thumbnails-leftovers.less"), "utf8"))).toContain("img-responsive");
      expect(current.some(rule => rule.selector === ".img-responsive")).toBe(true);
    });
  });

  describe("the remaining LESS", () => {
    // Found by the pixel diff: an empty column (a spacer) lost its 1px height to `body * { min-height: 0 }`, because an unlayered
    // rule beats a layered compat rule whatever its specificity, and the controls beside it moved
    it("keeps the base rule of the grid columns, which has to win over `body * { min-height: 0 }`", () => {
      const base = current.filter(rule => /^\.col-(xs|sm|md|lg)-\d+$/.test(rule.selector) && rule.media === "");
      expect(base).toHaveLength(48);
      base.forEach(rule => expect(rule.declarations).toMatchObject({position: "relative", "min-height": "1px"}));
    });

    // Less drops without a warning what extends or calls a rule set that no longer exists: a component that extended the removed
    // `.clearfix`, or called `.pull-right`, would silently lose its declarations
    it("does not extend or call a rule set that the removal took out", () => {
      // The pixel-admin right-to-left sources are not imported by any entry, so what they call is never compiled
      const lessFiles = walk(LESS_DIR).filter(file => file.endsWith(".less"));
      const allSources = lessFiles.map(file => fs.readFileSync(file, "utf8")).join("\n");
      expect(allSources).not.toMatch(/@import[^;]*["']([^"']*\/)?rtl\//);
      const sources = lessFiles.filter(file => !file.includes(`${path.sep}rtl${path.sep}`)).map(file => fs.readFileSync(file, "utf8").replace(/\/\*[\s\S]*?\*\/|\/\/.*$/gm, ""));
      const ruleSet = name => current.some(rule => rule.selector.replace(/::?(before|after)$/, "") === `.${name}`);
      const mixin = name => sources.some(source => new RegExp(`^\\s*\\.${name}\\s*\\(`, "m").test(source));
      // The bare rule sets that were removed, and every class of the compound selectors of the second removal: a rule set such as .form-inline
      // prints only compound selectors (.form-inline .form-group), but navbar.less still calls it
      const bare = baseline.removed.map(rule => rule.selector).filter(selector => /^\.[\w-]+(?::{1,2}(?:before|after))?$/.test(selector)).map(selector => selector.slice(1).replace(/:.*$/, ""));
      const classes = baseline.removed.filter(rule => rule.step >= 2).flatMap(rule => (rule.selector.match(/\.-?[_a-zA-Z][\w-]*/g) || []).map(name => name.slice(1)));
      const removedSets = [...new Set([...bare, ...classes])];
      expect(removedSets.length).toBeGreaterThan(100);
      const matches = find => sources.flatMap(find).filter(name => removedSets.includes(name));
      // An extend needs the rule set itself (a mixin of the same name does not count); a call also accepts a mixin
      // A call that resolves to a rule set of the same name that is still there (pixel-admin's .btn-lg, .form-inline) silently loses what
      // the removed Bootstrap rule set added: for the second removal the call has to find a mixin that the leftovers keep
      const secondRemoval = new Set(classes);
      const definesMixin = name => sources.some(source => new RegExp(`^\\s*\\.${name}\\s*\\([^)]*\\)\\s*\\{`, "m").test(source));
      // Likewise an extend of a rule set of the second removal needs a leftover that keeps that rule set (a rule set of the same name that
      // is still there, pixel-admin's, does not carry what the Bootstrap one added)
      const leftoverSets = new Set([...LEFTOVERS, ...LEFTOVERS3, ...LEFTOVERS4, ...LEFTOVERS5, ...LEFTOVERS6, ...LEFTOVERS7].flatMap(file => [...ruleSetsOf(fs.readFileSync(path.join(BOOTSTRAP, file), "utf8"))]));
      // Reviewed: button-groups-leftovers.less extends .btn-lg on purpose, to add the group size to the pixel-admin .btn-lg rule that prints
      // it (the Bootstrap one is in the leftovers verbatim); the check keeps the reason true: the rule is still compiled and still extended
      expect(ruleSet(REVIEWED_EXTEND)).toBe(true);
      expect(extendsOf(fs.readFileSync(path.join(BOOTSTRAP, "button-groups-leftovers.less"), "utf8"))).toContain(REVIEWED_EXTEND);
      const broken = [
        ...matches(extendsOf).filter(name => name !== REVIEWED_EXTEND && (secondRemoval.has(name) ? !leftoverSets.has(name) : !ruleSet(name))),
        ...matches(callsOf).filter(name => (secondRemoval.has(name) ? !definesMixin(name) : !ruleSet(name) && !mixin(name)))
      ];
      expect([...new Set(broken)]).toEqual([]);
    });
  });

  describe("the guard on extends and mixin calls", () => {
    it("sees a call at the start of a line", () => {
      expect(callsOf("  .pull-right();\n  .hidden;\n  .show !important;")).toEqual(["pull-right", "hidden", "show"]);
    });

    it("sees a call after an opening brace or a semicolon on the same line, with or without a closing semicolon", () => {
      expect(callsOf(".navbar-left { .pull-left(); }")).toEqual(["pull-left"]);
      expect(callsOf(".a { color: red; .pull-right(); }")).toEqual(["pull-right"]);
      expect(callsOf(".a { .clearfix(); .hidden }")).toEqual(["clearfix", "hidden"]);
    });

    it("does not take a declaration, a selector or a mixin with arguments for a call", () => {
      expect(callsOf(".a { margin: 0; }\n.b, .c { color: red; }\n.d { .border-top-radius(calc(2px - 1)); }")).toEqual([]);
    });

    it("sees the bare rule sets a source defines, not its mixins, its compound selectors or its pseudo-classes", () => {
      expect([...ruleSetsOf(".btn-lg() { a: b; }\n.form-inline, .navbar-form { a: b; }\n.x .y { a: b; }\n.z:hover { a: b; }\n// .c { a: b; }")].sort())
        .toEqual(["form-inline", "navbar-form"]);
    });

    it("flags an extend of a rule set that the removal took out, unless a leftover keeps that rule set", () => {
      const removed = new Set(["form-inline", "btn-group"]);
      const kept = ruleSetsOf(".btn-group { display: inline-block; }\n.btn-lg() { a: b; }");
      expect(unkeptExtends(".nav { &:extend(.form-inline all); }", removed, kept)).toEqual(["form-inline"]);
      expect(unkeptExtends(".nav { &:extend(.btn-group); }", removed, kept)).toEqual([]);
      expect(unkeptExtends(".nav { &:extend(.clearfix all); }", removed, kept)).toEqual([]);
      // A mixin of the same name is not a rule set: an extend does not find it
      expect(unkeptExtends(".nav { &:extend(.btn-lg); }", new Set(["btn-lg"]), kept)).toEqual(["btn-lg"]);
    });

    it("sees an extend wherever it is", () => {
      expect(extendsOf(".nav { &:extend(.clearfix all); }\n.a:extend(.pull-right) {}")).toEqual(["clearfix", "pull-right"]);
    });
  });

  describe("what the removed rules provided is served by the compat layer", () => {
    it("has a compat rule with the same selector and media query for every removed rule, but the ones left out on purpose", () => {
      const served = new Set(compat.map(rule => `${normalizeSelector(rule.selector)}|${rule.media}`));
      const unserved = baseline.served.filter(rule => !served.has(`${normalizeSelector(rule.selector)}|${rule.media}`)).map(rule => rule.selector);
      expect([...new Set(unserved)].sort()).toEqual(NOT_SERVED);
    });

    it("serves every class of the removed rules that the templates, the directives and the XML screens write", () => {
      const compatClasses = classesOf(JSON.stringify(compat.map(rule => rule.selector)).replace(/"/g, " "));
      const currentClasses = classesOf(css);
      const written = new Set([...sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/[A-Za-z][\w-]*/g)].map(match => match[0])), ...usedXmlClasses().keys()]);
      const removedClasses = [...new Set(baseline.served.flatMap(rule => (rule.selector.match(/\.-?[_a-zA-Z][\w-]*/g) || []).map(name => name.slice(1))))];
      // Exactly the classes of the frozen rules: a smaller number means a removed family lost its proof
      expect(removedClasses).toHaveLength(375);
      expect(removedClasses.filter(name => written.has(name) && !compatClasses.has(name) && !currentClasses.has(name))).toEqual([]);
    });

    it("leaves out only classes that nothing writes", () => {
      const written = new Set(sourceFiles().flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/[A-Za-z][\w-]*/g)].map(match => match[0])));
      expect(NOT_SERVED.map(selector => selector.slice(1)).filter(name => written.has(name))).toEqual([]);
    });
  });

  describe("the frozen rules of the buttons, button groups, forms and input groups (step 2)", () => {
    const STEP2_FILES = ["buttons.less", "button-groups.less", "forms.less", "input-groups.less"];
    const second = () => baseline.removed.filter(rule => rule.step === 2);

    // The snapshot is proven against the vendored build while the vendored files are still there: the removal that follows can then be
    // judged against it
    it("freezes rules that all come from the four files, each recorded with its source", () => {
      expect(baseline.steps.map(step => step.step)).toEqual([1, 2, 3, 4, 5, 6, 7]);
      expect(baseline.steps[1].files).toEqual(STEP2_FILES);
      expect(second().length).toBeGreaterThan(100);
      expect(second().filter(rule => !STEP2_FILES.includes(rule.source))).toEqual([]);
      expect(new Set(second().map(rule => rule.source))).toEqual(new Set(STEP2_FILES));
    });

    it("covers the buttons, the groups, the controls, the choices, the validation states and the input groups", () => {
      const selectors = new Set(second().map(rule => rule.selector));
      [".btn-success", ".btn-link", ".btn-group", ".btn-group > .btn", ".btn-toolbar", ".form-group", ".help-block", ".has-error .help-block", ".input-group", ".input-group-addon"]
        .forEach(selector => expect({selector, frozen: selectors.has(selector)}).toEqual({selector, frozen: true}));
    });

    it("puts every frozen rule back where it was: restoring them into a build that lacks them gives the vendored build again, order and content", () => {
      const frozen = new Set(second().map(hashOf));
      // A frozen rule has no copy among the rules that stay (its identity is absent from the build after the removal)
      const lacking = current.filter(rule => !frozen.has(hashOf(rule)));
      // The build before this removal still had the rules that a later removal took out (the step 3 and 4 files), when that removal is applied: they go back first
      const later = frozenOf(appliedAfter(2));
      const restored = restoreRemoved(lacking, [...second(), ...later], [2, ...appliedAfter(2)]);
      expect(restored).toHaveLength(lacking.length + second().length + later.length);
      // The header holds the digest of the ordered rules of the vendored build before the removal
      // The digests of steps 2 to 6 were refrozen after the develop sync of 2026-10-30: develop's AWE tokens and rules change the whole-build hash, no vendored rule changed; refrozen once more when pixel-admin moved to less/legacy: the unused `.ie-background-none` mixin prints its image path, 4 lines of the build; refrozen after the #882 sync (2026-10-30): the whole-build hash now includes leaflet.css and the map LESS, no vendored rule changed; refrozen after the #790 merge (2026-10-31): the whole-build hash now includes the window advanced search LESS, and once more for its saved searches LESS, no vendored rule changed
      const {digest} = baseline.steps.find(entry => entry.step === 2);
      expect(digest).toMatch(/^[0-9a-f]{40}$/);
      expect(digestOf(restored)).toBe(digest);
    });

    it("keeps in the build every rule that the baseline says the cascade guard kept (the leftovers)", () => {
      const present = new Set(current.map(hashOf));
      const {kept} = baseline.steps.find(entry => entry.step === 2);
      expect(kept).toHaveLength(406);
      expect(new Set(kept).size).toBe(kept.length);
      expect(kept.filter(hash => !present.has(hash))).toEqual([]);
    });
  });

  describe("the frozen rules of the component animations, dropdowns, navs, navbar, close button, modals, tooltip and popovers (step 3)", () => {
    const STEP3_FILES = ["component-animations.less", "dropdowns.less", "navs.less", "navbar.less", "close.less", "modals.less", "tooltip.less", "popovers.less"];
    const third = () => baseline.removed.filter(rule => rule.step === 3);
    const header = () => baseline.steps.find(entry => entry.step === 3);
    // While the vendored files are in the LESS sources the current build is the vendored build; once they are gone the frozen rules go back into it
    // The same rule as vendoredBeforeRemoval: the step is applied when every file the header lists is gone (close.less too, though none of its
    // rules left the build: all four stay in close-leftovers.less)
    const removed = () => header().files.every(file => !fs.existsSync(path.join(BOOTSTRAP, file)));

    it("freezes rules that all come from the eight files, each recorded with its source", () => {
      expect(header().files).toEqual(STEP3_FILES);
      expect(third().length).toBeGreaterThan(30);
      expect(third().filter(rule => !STEP3_FILES.includes(rule.source))).toEqual([]);
      expect(new Set(third().map(rule => rule.source))).toEqual(new Set(["component-animations.less", "dropdowns.less", "navs.less", "navbar.less", "modals.less", "tooltip.less", "popovers.less"]));
    });

    it("lists close.less among its files although none of its rules was frozen: they all stay, and the switch cannot skip a half applied step", () => {
      expect(header().files).toContain("close.less");
      expect(third().filter(rule => rule.source === "close.less")).toEqual([]);
      expect(fs.readFileSync(path.join(BOOTSTRAP, "close-leftovers.less"), "utf8")).toContain(".close {");
      // Either every vendored file of the step is gone and none of its frozen rules is in the build, or all of them are there
      const present = new Set(current.map(hashOf));
      const frozenPresent = third().filter(rule => present.has(hashOf(rule))).length;
      expect(removed() ? frozenPresent === 0 : frozenPresent === third().length).toBe(true);
      expect(header().files.filter(file => fs.existsSync(path.join(BOOTSTRAP, file))).length === 0 || !removed()).toBe(true);
    });

    it("covers the fade and collapse, the dropdown menu, the tabs, the navbar, the modal, the tooltip and the popover", () => {
      const selectors = new Set(third().map(rule => rule.selector));
      [".fade", ".collapsing", ".dropup", ".nav-justified", ".navbar-collapse.in", ".modal-footer:before", ".modal-dialog", ".tooltip-inner", ".popover > .arrow:after"]
        .forEach(selector => expect({selector, frozen: selectors.has(selector)}).toEqual({selector, frozen: true}));
    });

    it("is the vendored build before the removal: the digest of its ordered rules is the one frozen with the rules", () => {
      // The build before this removal still had the rules that the fourth removal took out, when it is applied: they go back first
      const later = appliedAfter(3);
      const before = removed() ? restoreRemoved(current, [...third(), ...frozenOf(later)], [3, ...later]) : current;
      expect(header().digest).toMatch(/^[0-9a-f]{40}$/);
      expect(digestOf(before)).toBe(header().digest);
      // The size of the LESS build is the one frozen for the last removal that is applied, and the fourth removal starts where this one ends
      expect(header().lessBytesAfter).toBe(baseline.steps.find(entry => entry.step === 4).lessBytesBefore);
      if (later.length === 0) {
        expect(lfBytes(css)).toBe(removed() ? header().lessBytesAfter : header().lessBytesBefore);
      }
    });

    it("keeps in the build every rule that the baseline says the cascade guard kept (the leftovers)", () => {
      const present = new Set(current.map(hashOf));
      const {kept} = header();
      expect(kept).toHaveLength(294);
      expect(new Set(kept).size).toBe(kept.length);
      expect(kept.filter(hash => !present.has(hash))).toEqual([]);
    });
  });

  describe("the frozen rules of the tables, labels, badges, wells, alerts, breadcrumbs, progress bars and pagination (step 4)", () => {
    const fourth = () => baseline.removed.filter(rule => rule.step === 4);
    const header = () => baseline.steps.find(entry => entry.step === 4);
    // While the vendored files are in the LESS sources the current build is the vendored build; once they are gone the frozen rules go back into it
    const removed = () => header().files.every(file => !fs.existsSync(path.join(BOOTSTRAP, file)));

    // Badges, wells and pagination freeze nothing: every one of their compiled rules stays in the leftovers (the cascade guard found a rule that stays beats each)
    it("freezes rules that all come from the eight files, each recorded with its source", () => {
      expect(header().files).toEqual(STEP4_FILES);
      expect(fourth().length).toBeGreaterThan(30);
      expect(fourth().filter(rule => !STEP4_FILES.includes(rule.source))).toEqual([]);
      expect(new Set(fourth().map(rule => rule.source))).toEqual(new Set(["tables.less", "labels.less", "alerts.less", "breadcrumbs.less", "progress-bars.less"]));
    });

    it("covers the table, the labels, the alerts, the breadcrumb, the progress bars with their stripes, and the responsive table", () => {
      const selectors = new Set(fourth().map(rule => rule.selector));
      [".table", ".table-bordered", ".label-primary", ".alert", ".alert-success", ".breadcrumb", ".progress", ".progress-bar.active", ".progress-striped .progress-bar-info", "a.label:hover"]
        .forEach(selector => expect({selector, frozen: selectors.has(selector)}).toEqual({selector, frozen: true}));
      expect(fourth().some(rule => rule.selector === ".table-responsive" && rule.media === "screen and (max-width: 767px)")).toBe(true);
    });

    it("is the vendored build before the removal: the digest of its ordered rules is the one frozen with the rules", () => {
      // The build before this removal still had the rules that the fifth removal took out, when it is applied: they go back first
      const later = appliedAfter(4);
      const before = removed() ? restoreRemoved(current, [...fourth(), ...frozenOf(later)], [4, ...later]) : current;
      expect(header().digest).toMatch(/^[0-9a-f]{40}$/);
      expect(digestOf(before)).toBe(header().digest);
      // The size of the LESS build is the one frozen for the last removal that is applied, and the fifth removal starts where this one ends
      expect(header().lessBytesAfter).toBe(baseline.steps.find(entry => entry.step === 5).lessBytesBefore);
      if (later.length === 0) {
        expect(lfBytes(css)).toBe(removed() ? header().lessBytesAfter : header().lessBytesBefore);
      }
    });

    it("keeps in the build every rule that the baseline says the cascade guard kept (the leftovers)", () => {
      const present = new Set(current.map(hashOf));
      const {kept} = header();
      expect(kept).toHaveLength(221);
      expect(new Set(kept).size).toBe(kept.length);
      expect(kept.filter(hash => !present.has(hash))).toEqual([]);
    });
  });

  describe("the frozen rules of the pager, media, jumbotron, carousel, glyphicons and code helpers (step 5, dropped with no compat CSS)", () => {
    const fifth = () => baseline.removed.filter(rule => rule.step === 5);
    const header = () => baseline.steps.find(entry => entry.step === 5);
    const removed = () => header().files.every(file => !fs.existsSync(path.join(BOOTSTRAP, file)));

    it("freezes rules that all come from the six files, each recorded with its source, and marks the step as unserved", () => {
      expect(header().files).toEqual(STEP5_FILES);
      expect(header().unserved).toBe(true);
      expect(new Set(fifth().map(rule => rule.source))).toEqual(new Set(STEP5_FILES));
      expect(baseline.served.some(rule => rule.step === 5)).toBe(false);
    });

    it("covers the carousel, the jumbotron, the media object, the pager, the icon font and the scrollable pre", () => {
      const selectors = new Set(fifth().map(rule => rule.selector));
      [".carousel", ".carousel-inner > .item", ".carousel-control", ".jumbotron", ".media", ".media-body", ".pager", ".pager li > a", ".glyphicon", ".glyphicon-star:before", ".pre-scrollable"]
        .forEach(selector => expect({selector, frozen: selectors.has(selector.replace(/:before$/, "::before")) || selectors.has(selector)}).toEqual({selector, frozen: true}));
    });

    it("is the vendored build before the removal: the digest of its ordered rules is the one frozen with the rules", () => {
      // The build before this removal still had the rules that the sixth removal took out, when it is applied: they go back first
      const later = appliedAfter(5);
      const before = removed() ? restoreRemoved(current, [...fifth(), ...frozenOf(later)], [5, ...later]) : current;
      expect(header().digest).toMatch(/^[0-9a-f]{40}$/);
      expect(digestOf(before)).toBe(header().digest);
      expect(header().lessBytesAfter).toBe(baseline.steps.find(entry => entry.step === 6).lessBytesBefore);
      if (later.length === 0) {
        expect(lfBytes(css)).toBe(removed() ? header().lessBytesAfter : header().lessBytesBefore);
      }
    });

    it("kept the element rules of code.less, which the seventh removal took over: what is not in the build any more is frozen by that removal", () => {
      const present = new Set(current.map(hashOf));
      const {kept} = header();
      expect(kept).toHaveLength(9);
      expect(new Set(kept).size).toBe(kept.length);
      const frozenLater = new Set(frozenOf(appliedAfter(5)).map(hashOf));
      expect(kept.filter(hash => !present.has(hash) && !frozenLater.has(hash))).toEqual([]);
    });
  });

  describe("the frozen rules of the list groups, thumbnails and responsive embeds (step 6)", () => {
    const sixth = () => baseline.removed.filter(rule => rule.step === 6);
    const header = () => baseline.steps.find(entry => entry.step === 6);
    const removed = () => header().files.every(file => !fs.existsSync(path.join(BOOTSTRAP, file)));

    it("freezes rules that all come from the three files, each recorded with its source, and is served by the compat layer", () => {
      expect(header().files).toEqual(STEP6_FILES);
      expect(header().unserved).toBeUndefined();
      expect(sixth().filter(rule => !STEP6_FILES.includes(rule.source))).toEqual([]);
      expect(baseline.served.filter(rule => rule.step === 6)).toHaveLength(sixth().length);
    });

    it("covers the first list group item and the thumbnail caption (the list group itself stays: the `ul, ol` rules of type.less would win over a compat rule on a `ul.list-group`; the rest of the three families stays in the leftovers: the cascade guard found a rule that stays beneath each)", () => {
      const selectors = new Set(sixth().map(rule => rule.selector));
      [".list-group-item:first-child", ".thumbnail .caption"]
        .forEach(selector => expect({selector, frozen: selectors.has(selector)}).toEqual({selector, frozen: true}));
    });

    it("is the vendored build before the removal: the digest of its ordered rules is the one frozen with the rules", () => {
      // The build before this removal still had the rules that the seventh removal took out, when it is applied: they go back first
      const later = appliedAfter(6);
      const before = removed() ? restoreRemoved(current, [...sixth(), ...frozenOf(later)], [6, ...later]) : current;
      expect(header().digest).toMatch(/^[0-9a-f]{40}$/);
      expect(digestOf(before)).toBe(header().digest);
      expect(header().lessBytesAfter).toBe(baseline.steps.find(entry => entry.step === 7).lessBytesBefore);
      if (later.length === 0) {
        expect(lfBytes(css)).toBe(removed() ? header().lessBytesAfter : header().lessBytesBefore);
      }
    });

    it("keeps in the build every rule that the baseline says the cascade guard kept (the leftovers)", () => {
      const present = new Set(current.map(hashOf));
      const {kept} = header();
      expect(kept).toHaveLength(81);
      expect(new Set(kept).size).toBe(kept.length);
      expect(kept.filter(hash => !present.has(hash))).toEqual([]);
    });
  });

  describe("the frozen rules (support/vendored-removed.baseline.json)", () => {
    it("put back the removed rules into the current build, so the compat equivalence tests keep their proof", () => {
      const first = baseline.removed.filter(rule => rule.step === 1);
      const restored = restoreRemoved(current, first);
      expect(restored).toHaveLength(current.length + first.length);
      expect(restored.some(rule => rule.selector === ".col-sm-6" && rule.media === "(min-width: 768px)")).toBe(true);
      expect(restored.some(rule => rule.selector === ".pull-right")).toBe(true);
      expect(restored.some(rule => rule.selector === ".panel-body")).toBe(true);
      // A removal that is already fully there is refused (nothing to put back), and so is a build with only some of its rules
      expect(() => restoreRemoved(restored, first)).toThrow(/already in the build/);
      const partial = restored.filter(rule => hashOf(rule) !== hashOf(first[0]));
      expect(() => restoreRemoved(partial, first)).toThrow(/already in the build/);
    });

    it("say how they were generated and how to generate them again", () => {
      expect(baseline.note).toContain("compat:freeze-vendored");
      expect(JSON.parse(fs.readFileSync(path.join(__dirname, "..", "..", "..", "..", "package.json"), "utf8")).scripts["compat:freeze-vendored"])
        .toContain("freezeVendored.js");
    });
  });
});
