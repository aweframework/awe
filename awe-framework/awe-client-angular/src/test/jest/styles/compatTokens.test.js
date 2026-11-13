import fs from "fs";
import path from "path";
import {TAILWIND_DIR} from "./support/compileTailwind";

// The compat layer follows the AWE 5 design system (skills/awe-ui): it reads semantic or component --awe-* tokens, with no colour
// literal, no bare px size, no primitive (--awe-palette-*) and no mode or density selector
const COMPAT_DIR = path.join(TAILWIND_DIR, "compat");
const FILES = fs.readdirSync(COMPAT_DIR).filter(name => name.endsWith(".css") && name !== "index.css").sort();
const NAMED_COLORS = ["white", "black", "red", "green", "blue", "yellow", "orange", "gray", "grey", "silver", "navy", "maroon", "purple", "teal", "aqua", "lime"];

// A named colour is a whole word (white-space is not one)
const COLOR_LITERAL = new RegExp(`#[0-9a-fA-F]{3,8}\\b|\\b(?:rgb|rgba|hsl|hsla|hwb|lab|lch|oklab|oklch)\\(|(?<![\\w-])(?:${NAMED_COLORS.join("|")})(?![\\w-])`);
// A size in px (0px is not one) in a declaration or in an arbitrary Tailwind value; the width of a media query cannot read a custom property
const PX_SIZE = /(?<![\w.-])-?(?:[1-9]\d*|\d*\.\d+)px\b/;
const PALETTE = /--awe-palette-/;
const MODE_SELECTOR = /\.dark\b|\.light\b|\[data-awe-(?:density|primary|surface)|data-awe-/;

/**
 * The lines of a compat file that carry style: no comment, no @import, no media query prelude
 * @param {string} name File
 * @return {{line: number, text: string}[]} Lines
 */
const styleLines = name => fs.readFileSync(path.join(COMPAT_DIR, name), "utf8").replace(/\/\*[\s\S]*?\*\//g, match => match.replace(/[^\n]/g, ""))
  .split("\n").map((text, index) => ({line: index + 1, text: text.trim()}))
  .filter(({text}) => text && !text.startsWith("@import") && !text.startsWith("@media"));

const violations = (pattern, only = () => true) => FILES.flatMap(name => styleLines(name).filter(({text}) => only(text) && pattern.test(text)).map(({line, text}) => `${name}:${line} ${text}`));

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatTokens", () => {
  it("reads at least the compat files of every family", () => {
    expect(FILES.length).toBeGreaterThan(15);
  });

  it("writes no colour literal (#hex, rgb(), hsl(), named colour): colours are --awe-* semantic tokens", () => {
    expect(violations(COLOR_LITERAL)).toEqual([]);
  });

  it("writes no bare px size: sizes are --awe-* tokens (the breakpoints of the media queries are the only px)", () => {
    expect(violations(PX_SIZE)).toEqual([]);
  });

  it("reads no primitive of the palette (--awe-palette-*)", () => {
    expect(violations(PALETTE)).toEqual([]);
  });

  it("has no mode or density selector: light and dark live in the token file, density in the size tokens", () => {
    expect(violations(MODE_SELECTOR, text => !text.includes(":") || /[{,]$/.test(text))).toEqual([]);
  });

  it("detects what it forbids", () => {
    expect(COLOR_LITERAL.test("color: #337ab7;")).toBe(true);
    expect(COLOR_LITERAL.test("background: rgba(0, 0, 0, 0.5);")).toBe(true);
    expect(COLOR_LITERAL.test("color: white;")).toBe(true);
    expect(COLOR_LITERAL.test("color: var(--awe-color-text); border-color: transparent;")).toBe(false);
    expect(COLOR_LITERAL.test("white-space: nowrap;")).toBe(false);
    expect(PX_SIZE.test("margin: 0 5px;")).toBe(true);
    expect(PX_SIZE.test("@apply tw:w-[742px];")).toBe(true);
    expect(PX_SIZE.test("margin: 0; padding: var(--awe-space-1);")).toBe(false);
    expect(PX_SIZE.test("width: 0px;")).toBe(false);
  });
});
