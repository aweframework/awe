import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// Row grouping of the AngularJS grids (#833): group rows (surface band, semibold label, muted count, indented by level),
// subtotal rows, the toggle (focus ring, chevron turned while expanded, no motion when reduced) and the Group by panel,
// with the AWE 5 tokens only, as the prototype (components.html#c-833)
const FILE = path.join(LESS_DIR, "awe", "directives", "grid-groups.less");
const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;
const REDUCED_MOTION = "@media (prefers-reduced-motion: reduce)";

describe("AngularJS grid row grouping styles (#833)", () => {
  let rules;

  beforeAll(async () => {
    rules = parseRules(await compileStyles()).map(rule => {
      const match = rule.selector.match(MEDIA);
      return {media: match ? match[1].trim() : "", selectors: splitSelectors(match ? match[2] : rule.selector), declarations: rule.declarations};
    });
  });

  const style = (selector, media = "") => rules
    .filter(rule => rule.media === media && rule.selectors.includes(selector))
    .reduce((declarations, rule) => ({...declarations, ...rule.declarations}), {});

  it("is imported by the directives and uses tokens only", () => {
    const directives = fs.readFileSync(path.join(LESS_DIR, "awe", "directives", "directives.less"), "utf8");
    expect(directives).toMatch(/@import "\.\/grid-groups";/);
    const source = fs.readFileSync(FILE, "utf8").replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/.*$/gm, "");
    expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
    expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
    expect(source).not.toMatch(/(?<![\w-])(white|black)(?![\w-])/);
    expect(source).not.toMatch(/\.dark\b|data-awe-density/);
    expect(source).not.toMatch(/--awe-palette-/);
    expect(source).not.toMatch(/(?<![\w-])\d+px/);
  });

  it("keeps the grouping controls at the start of the toolbar: a spacer pushes Columns and Maximize to its end", () => {
    expect(style(".grid-toolbar .grid-toolbar-spacer")).toMatchObject({"flex": "1 1 0", "min-width": "0"});
  });

  it("draws the group rows on the subtle surface with a semibold label and a muted count", () => {
    expect(style(".grid .ui-grid-row.grid-group-row")).toMatchObject({"background-color": "var(--awe-color-surface-subtle)", "cursor": "pointer"});
    expect(style(".grid .grid-group-row .grid-group-label")).toMatchObject({
      "font-weight": "var(--awe-font-weight-semibold)",
      "color": "var(--awe-color-text-emphasis)"
    });
    expect(style(".grid .grid-group-row .grid-group-count")).toMatchObject({"color": "var(--awe-color-text-muted)"});
  });

  it("draws the subtotal rows semibold with a muted label", () => {
    expect(style(".grid .ui-grid-row.grid-subtotal-row")).toMatchObject({
      "font-weight": "var(--awe-font-weight-semibold)",
      "color": "var(--awe-color-text-emphasis)"
    });
    expect(style(".grid .grid-subtotal-row .grid-subtotal-label")).toMatchObject({
      "font-weight": "var(--awe-font-weight-medium)",
      "color": "var(--awe-color-text-muted)"
    });
  });

  it("lets the label of a group run over the empty cells at its right", () => {
    expect(style(".grid .ui-grid-row .ui-grid-cell.grid-group-label-cell")).toMatchObject({"overflow": "visible", "position": "relative", "z-index": "1"});
    expect(style(".grid .ui-grid-cell.grid-group-label-cell .ui-grid-cell-contents")).toMatchObject({"overflow": "visible", "white-space": "nowrap"});
  });

  it("indents the label of each level with the spacing tokens", () => {
    expect(style(".grid .grid-group-level-1 .grid-group-title")["padding-left"]).toBe("calc(var(--awe-space-5) * 0)");
    expect(style(".grid .grid-group-level-3 .grid-group-title")["padding-left"]).toBe("calc(var(--awe-space-5) * 2)");
    expect(style(".grid .grid-subtotal-level-2 .grid-group-title")["padding-left"])
      .toBe("calc(var(--awe-space-5) * 1 + var(--awe-size-icon) + var(--awe-space-1-5) * 2)");
  });

  it("gives the toggle a focus ring and turns its chevron while the group is expanded, without motion when reduced", () => {
    expect(style(".grid-group-toggle")).toMatchObject({
      "width": "calc(var(--awe-size-icon) + var(--awe-space-1-5))",
      "height": "calc(var(--awe-size-icon) + var(--awe-space-1-5))",
      "color": "var(--awe-color-text-muted)",
      "border-radius": "var(--awe-radius-sm)"
    });
    expect(style(".grid-group-toggle:focus-visible")).toMatchObject({"outline": "var(--awe-focus-ring)"});
    expect(style(".grid-group-toggle:hover")).toMatchObject({"background": "var(--awe-color-surface-hover)"});
    expect(style(".grid-group-toggle .awe-icon")["transition"]).toContain("var(--awe-motion-duration-base)");
    expect(style(".grid-group-toggle[aria-expanded=\"true\"] .awe-icon")).toMatchObject({"transform": "rotate(90deg)"});
    expect(style(".grid-group-toggle .awe-icon", REDUCED_MOTION)).toMatchObject({"transition": "none"});
  });

  it("shows the Group by panel as an overlay", () => {
    expect(style(".grid-group-by-popover")).toMatchObject({
      "position": "fixed",
      "z-index": "var(--awe-z-modal)",
      "background": "var(--awe-color-overlay)",
      "border-radius": "var(--awe-radius-overlay)",
      "box-shadow": "var(--awe-shadow-overlay)"
    });
    expect(style(".grid-group-by-popover .grid-group-by-checkbox:focus-visible")).toMatchObject({"outline": "var(--awe-focus-ring)"});
    expect(style("awe-grid-group-by.grid-group-by")).toMatchObject({"display": "contents"});
  });
});
