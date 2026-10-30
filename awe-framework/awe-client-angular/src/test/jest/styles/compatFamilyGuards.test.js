import {importantHazards, importOrderProblems} from "./support/compatFamily";
import {mediaMeet} from "./support/cascadeGuard";

const rule = (selector, declarations, media = "") => ({selector, media, declarations});

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatFamilyGuards (the guards of the family harness, on crafted fixtures)", () => {
  describe("importOrderProblems", () => {
    it("accepts a file between its neighbours", () => {
      expect(importOrderProblems(["a.css", "me.css", "z.css"], "me.css", ["a.css"], ["z.css"])).toEqual([]);
    });

    it("fails when a listed predecessor or successor is not imported at all (a missing file never passes vacuously)", () => {
      expect(importOrderProblems(["me.css", "z.css"], "me.css", ["a.css"], ["z.css"])).toEqual(["a.css is not imported"]);
      expect(importOrderProblems(["a.css", "me.css"], "me.css", ["a.css"], ["z.css"])).toEqual(["z.css is not imported"]);
    });

    it("fails when the file is not imported, or comes on the wrong side", () => {
      expect(importOrderProblems(["a.css"], "me.css", [], [])).toEqual(["me.css is not imported"]);
      expect(importOrderProblems(["me.css", "a.css"], "me.css", ["a.css"], [])).toEqual(["me.css comes before a.css"]);
      expect(importOrderProblems(["z.css", "me.css"], "me.css", [], ["z.css"])).toEqual(["me.css comes after z.css"]);
    });
  });

  describe("importantHazards", () => {
    const served = [rule(".navbar-text", {float: "left !important"}, "print")];

    it("sees a served !important in a media query meet an unlayered !important rule with no media query (it applies too)", () => {
      const other = rule(".navbar-text.x", {float: "right !important"});
      expect(importantHazards([...served, other], served, new Set())).toEqual([".navbar-text { float } over .navbar-text.x"]);
    });

    it("sees it meet a rule of an overlapping media query, and not one of a disjoint query", () => {
      const wide = [rule(".a", {margin: "1px !important"}, "(min-width: 768px)")];
      expect(importantHazards([...wide, rule(".a.b", {margin: "2px !important"}, "(min-width: 992px)")], wide, new Set())).toHaveLength(1);
      expect(importantHazards([...wide, rule(".a.b", {margin: "2px !important"}, "(max-width: 767px)")], wide, new Set())).toEqual([]);
    });

    it("ignores the same value, a rule that is layered too, and a normal declaration", () => {
      expect(importantHazards([...served, rule(".navbar-text", {float: "left !important"})], served, new Set())).toEqual([]);
      expect(importantHazards([...served, rule(".navbar-text.x", {float: "right !important"})], served, new Set([".navbar-text.x"]))).toEqual([]);
      expect(importantHazards([...served, rule(".navbar-text.x", {float: "right"})], served, new Set())).toEqual([]);
    });
  });

  describe("mediaMeet with a list of queries", () => {
    it.each([
      ["print, (min-width: 768px)", "(max-width: 767px)", true],
      ["(min-width: 768px), (min-width: 1000px)", "(max-width: 767px)", false],
      ["(max-width: 500px), (min-width: 992px)", "(min-width: 1200px)", true],
      ["(max-width: 500px), (min-width: 992px)", "(min-width: 600px) and (max-width: 900px)", false],
      ["(min-width: 768px)", "(max-width: 400px), (min-width: 800px)", true],
      ["(max-width: 767px)", "(min-width: 768px), (min-width: 1000px)", false]
    ])("%s and %s: %s", (first, second, expected) => {
      expect(mediaMeet(first, second)).toBe(expected);
    });
  });
});

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatFamilyGuards (the exemption of the earlier family tests)", () => {
  const {fileSelectors, laterSelectors, LATER_FILES} = require("./support/compatFile");
  const {namesOf} = require("./support/compatEquivalence");
  const families = Object.values(require("./support/compatFamilies"));

  it("exempts only selectors that a family of slice 5 or 6 serves: every class of each is one of the classes of the family whose file writes it", () => {
    expect(families.map(family => family.file).sort()).toEqual([...LATER_FILES].sort());
    const stray = families.flatMap(family => [...fileSelectors(family.file)].filter(selector => !namesOf(selector).every(name => [...family.group, ...family.context].includes(name))
      || !namesOf(selector).some(family.use)).map(selector => `${family.file}: ${selector}`));
    expect(stray).toEqual([]);
    expect(laterSelectors().size).toBe(new Set(families.flatMap(family => [...fileSelectors(family.file)])).size);
  });

  it("never serves a left-out selector of a family: its examples are written by no compat file, or only by the file its entry names (servedIn)", () => {
    const written = file => fileSelectors(file);
    const files = [...LATER_FILES, "buttons.css", "button-groups.css", "input-groups.css", "input-groups-pixel-admin.css", "forms.css", "forms-pixel-admin.css", "panels.css"];
    const wrong = families.flatMap(family => family.leftOut.filter(entry => entry.examples).flatMap(entry => entry.examples.flatMap(example => {
      const where = files.filter(file => written(file).has(example));
      const expected = entry.servedIn ? [entry.servedIn] : [];
      return where.length === 0 || where.join() === expected.join() ? [] : [`${family.file} left out ${example}: written by [${where}], expected [${expected}]`];
    })));
    expect(wrong).toEqual([]);
  });
});
