import fs from "fs";
import path from "path";
import {vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";
import {compileTailwind, getRules, normalizeLength, TAILWIND_DIR} from "./support/compileTailwind";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

const GRID_CLASS = /^\.col-(xs|sm|md|lg)-(?:(?:offset|push|pull)-)?\d+$/;
const GRID_PROPERTIES = ["position", "min-height", "padding-left", "padding-right", "float", "width", "left", "right", "margin-left", "margin-right"];
const BREAKPOINTS = {xs: "", sm: "(min-width: 768px)", md: "(min-width: 992px)", lg: "(min-width: 1200px)"};

/**
 * Merge the declarations of every rule with a selector in a media query (the cascade of the rules that do not conflict)
 */
const mergeRules = (rules, selector, media, important = false) => rules
  .filter(rule => rule.selector === selector && rule.media === media)
  .reduce((merged, rule) => {
    Object.keys(rule.declarations).filter(property => rule.declarations[property].includes("!important") === important)
      .forEach(property => {
        merged[property] = normalizeLength(rule.declarations[property].replace(" !important", ""));
      });
    return merged;
  }, {});

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatGrid", () => {
  let vendored;
  let compat;

  beforeAll(async () => {
    vendored = await vendoredBeforeRemoval();
    compat = getRules(await compileTailwind());
  });

  it("emits the generated grid in the compat layer, in sync with the generator", () => {
    const {generateGrid} = require(path.join(TAILWIND_DIR, "tools", "generate-grid.js"));
    expect(fs.readFileSync(path.join(TAILWIND_DIR, "compat", "grid.generated.css"), "utf8")).toBe(generateGrid());
  });

  it("compares with the frozen vendored Bootstrap grid (support/vendored-removed.baseline.json)", () => {
    expect(vendored.some(rule => rule.selector === ".col-sm-6" && rule.media === BREAKPOINTS.sm)).toBe(true);
  });

  describe("columns, offsets, pushes and pulls (4 breakpoints x 12 columns)", () => {
    const selectors = () => [...new Set(vendored.filter(rule => GRID_CLASS.test(rule.selector)).map(rule => rule.selector))];

    it("covers exactly the grid classes of the vendored Bootstrap (no class missing, none extra)", () => {
      const compatSelectors = [...new Set(compat.filter(rule => GRID_CLASS.test(rule.selector)).map(rule => rule.selector))];
      expect(selectors()).toHaveLength(4 * (12 + 13 * 3));
      expect(compatSelectors.sort()).toEqual(selectors().sort());
    });

    it.each(Object.keys(BREAKPOINTS))("gives every %s column the same declarations as the vendored Bootstrap", breakpoint => {
      const media = BREAKPOINTS[breakpoint];
      const different = selectors().filter(selector => selector.startsWith(`.col-${breakpoint}-`)).filter(selector => {
        const expected = mergeRules(vendored, selector, media);
        const actual = mergeRules(compat, selector, media);
        return GRID_PROPERTIES.some(property => expected[property] !== actual[property]);
      });
      expect(different).toEqual([]);
    });

    it("emits the float, width, offset, push and pull of sm, md and lg only inside their min-width media query", () => {
      const layout = rule => ["float", "width", "left", "right", "margin-left"].some(property => property in rule.declarations);
      compat.filter(rule => /^\.col-(sm|md|lg)-/.test(rule.selector) && layout(rule)).forEach(rule => {
        const breakpoint = rule.selector.slice(5, 7);
        expect({selector: rule.selector, media: rule.media}).toEqual({selector: rule.selector, media: BREAKPOINTS[breakpoint]});
      });
    });
  });

  describe("AWE offset-right (less/awe/offset.less, not Bootstrap)", () => {
    const OFFSET_RIGHT = /^\.col-(xs|sm|md|lg)-offset-right-\d+$/;
    const selectors = () => [...new Set(vendored.filter(rule => OFFSET_RIGHT.test(rule.selector)).map(rule => rule.selector))];

    it("covers exactly the offset-right classes of the compiled AWE LESS (4 breakpoints x 13 values)", () => {
      const compatSelectors = [...new Set(compat.filter(rule => OFFSET_RIGHT.test(rule.selector)).map(rule => rule.selector))];
      expect(selectors()).toHaveLength(4 * 13);
      expect(compatSelectors.sort()).toEqual(selectors().sort());
    });

    it.each(Object.keys(BREAKPOINTS))("gives every %s offset-right the margin-right of the compiled AWE rule, in the same media query", breakpoint => {
      const media = BREAKPOINTS[breakpoint];
      const names = selectors().filter(selector => selector.startsWith(`.col-${breakpoint}-`));
      expect(names).toHaveLength(13);
      names.forEach(selector => {
        expect(vendored.some(rule => rule.selector === selector && rule.media === media)).toBe(true);
        expect({selector, ...mergeRules(compat, selector, media)}).toEqual({selector, ...mergeRules(vendored, selector, media)});
        expect(compat.filter(rule => rule.selector === selector).map(rule => rule.media)).toEqual([media]);
      });
    });
  });

  describe("container and row", () => {
    it("reimplements the row with the grid gutter of AWE (11px each side) and the clearfix", () => {
      expect(mergeRules(compat, ".row", "")).toMatchObject(mergeRules(vendored, ".row", ""));
      expect(compat.some(rule => /^\.row::?after$/.test(rule.selector) && rule.declarations.clear === "both")).toBe(true);
    });

    it("reimplements the container widths of AWE (742, 962 and 1162px) with the grid container tokens", () => {
      const tokens = fs.readFileSync(path.join(TAILWIND_DIR, "..", "..", "..", "..", "..", "awe-design-tokens", "tokens.css"), "utf8");
      const widths = {[BREAKPOINTS.sm]: ["sm", "742px"], [BREAKPOINTS.md]: ["md", "962px"], [BREAKPOINTS.lg]: ["lg", "1162px"]};
      Object.entries(widths).forEach(([media, [size, width]]) => {
        expect(mergeRules(compat, ".container", media).width).toBe(`var(--awe-grid-container-${size})`);
        expect(mergeRules(vendored, ".container", media).width).toBe(width);
        expect(tokens).toContain(`--awe-grid-container-${size}: ${width};`);
      });
      const base = mergeRules(vendored, ".container", "");
      expect(mergeRules(compat, ".container", "")).toMatchObject(base);
      expect(mergeRules(compat, ".container-fluid", "")).toMatchObject(mergeRules(vendored, ".container-fluid", ""));
    });
  });
});
