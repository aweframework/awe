import fs from "fs";
import path from "path";

// The design system rule (website/docs/design-system/overview.md): tokens only, no sizes outside the scales. The width
// of a border is --awe-border-width (1px) or --awe-border-width-strong (2px), so a theme can thicken or thin it. A
// literal 1px or 2px border is invisible to the tokens and makes the reference of the design system a lie.
// Only the sources of this engine are read: less/libs, less/legacy and the vendored file managers keep their literals.
const LESS_DIR = path.join(__dirname, "..", "..", "..", "main", "resources", "less", "awe");

// The declarations whose value is a border width: border, border-top|right|bottom|left, border-width, the logical sides.
// border-radius, border-color, border-style, border-collapse and border-image are not a width and stay out.
const WIDTH_PROPERTY = /^(border|border-(?:top|right|bottom|left|width|block|block-start|block-end|inline|inline-start|inline-end))\s*:/;
// A 1px or 2px literal, not the "1px" inside "11px" or "7px". The token reads var(--awe-border-width), which never
// matches.
const HAIRLINE = /(?<![\w.-])(?:1|2)px(?![\w-])/;

/**
 * Every LESS file of the engine, sorted
 * @param {string} dir Directory
 * @return {string[]} Absolute paths
 */
const lessFiles = dir => fs.readdirSync(dir, {withFileTypes: true})
  .flatMap(entry => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return lessFiles(full);
    }
    return entry.name.endsWith(".less") ? [full] : [];
  })
  .sort();

/**
 * The lines of a file that carry style: comments blanked, but their line numbers kept
 * @param {string} file Absolute path
 * @return {{line: number, text: string}[]} Lines
 */
const styleLines = file => fs.readFileSync(file, "utf8")
  .replace(/\/\*[\s\S]*?\*\//g, match => match.replace(/[^\n]/g, ""))
  .replace(/^\s*\/\/.*$/gm, "")
  .split("\n")
  .map((text, index) => ({line: index + 1, text: text.trim()}))
  .filter(({text}) => text.length > 0);

const FILES = lessFiles(LESS_DIR);
const relative = file => path.relative(path.join(__dirname, "..", "..", "..", ".."), file).replace(/\\/g, "/");

/**
 * Whether a line is a border width declaration that writes a literal instead of the token
 * @param {string} text Trimmed line
 * @return {boolean} True when the line is a violation
 */
const violates = text => {
  const separator = text.indexOf(":");
  return separator >= 0
    && WIDTH_PROPERTY.test(text.slice(0, separator + 1))
    && HAIRLINE.test(text.slice(separator + 1));
};

const violations = () => FILES.flatMap(file => styleLines(file)
  .filter(({text}) => violates(text))
  .map(({line, text}) => `${relative(file)}:${line} ${text}`));

describe("awe-framework/awe-client-angular/src/test/jest/styles/borderWidths", () => {
  it("reads the LESS files of the engine", () => {
    expect(FILES.length).toBeGreaterThan(15);
  });

  it("writes no literal 1px or 2px border width: borders read --awe-border-width or --awe-border-width-strong", () => {
    expect(violations()).toEqual([]);
  });

  it("reads the tokens in the components, not only in the token file", () => {
    const readers = FILES.filter(file => /var\(--awe-border-width(?:-strong)?\)/.test(fs.readFileSync(file, "utf8")));
    expect(readers.length).toBeGreaterThan(3);
  });

  it("detects what it forbids", () => {
    expect(violates("border: 1px solid @color;")).toBe(true);
    expect(violates("border-bottom: 2px solid transparent;")).toBe(true);
    expect(violates("border: 1px dashed var(--awe-color-field-border);")).toBe(true);
    expect(violates("border-width: 1px 0 0 0;")).toBe(true);
    expect(violates("border: var(--awe-border-width) solid var(--awe-color-border);")).toBe(false);
    expect(violates("border-bottom: var(--awe-border-width-strong) solid var(--awe-color-primary);")).toBe(false);
    expect(violates("border: 7px solid transparent;")).toBe(false);
    expect(violates("border-radius: 1px;")).toBe(false);
    expect(violates("box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);")).toBe(false);
  });
});
