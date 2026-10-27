import fs from "fs";
import path from "path";
import {compileStyles, ROOT} from "./support/compileStyles";

const ICON_STYLES = path.join(ROOT, "src", "main", "resources", "less", "awe", "awe-icon.less");

// Declarations of the rule with exactly this selector
const declarationsOf = (css, selector) => {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = new RegExp(`(?:^|\\})\\s*${escaped}\\s*\\{([^}]*)\\}`, "m").exec(css);
  return match ? Object.fromEntries(match[1].split(";").map(part => part.trim()).filter(Boolean)
    .map(part => [part.slice(0, part.indexOf(":")).trim(), part.slice(part.indexOf(":") + 1).trim()])) : null;
};

describe("awe-framework/awe-client-angular/src/test/jest/styles/aweIcon", () => {
  let css;

  beforeAll(async () => {
    css = await compileStyles();
  });

  it("draws the Lucide icons with the size tokens, the text colour and the stroke of the prototype", () => {
    expect(declarationsOf(css, ".awe-icon")).toEqual({
      "display": "inline-block",
      "width": "var(--awe-size-icon)",
      "height": "var(--awe-size-icon)",
      "flex": "none",
      "fill": "none",
      "stroke": "currentColor",
      "stroke-width": "1.75",
      "stroke-linecap": "round",
      "stroke-linejoin": "round",
      "vertical-align": "middle"
    });
    expect(declarationsOf(css, ".awe-icon-sm")).toEqual({"width": "var(--awe-size-icon-sm)", "height": "var(--awe-size-icon-sm)"});
    expect(declarationsOf(css, ".awe-icon-lg")).toEqual({"width": "var(--awe-size-icon-lg)", "height": "var(--awe-size-icon-lg)"});
  });

  it("spins like the Font Awesome icons, except when the user prefers reduced motion", () => {
    expect(declarationsOf(css, ".awe-icon-spin")).toEqual(declarationsOf(css, ".fa-spin"));
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.awe-icon-spin\s*\{\s*animation:\s*none;?\s*\}/);
  });

  it("keeps the size of the icons that their context shows larger than the text", () => {
    expect(css).toMatch(/\.grid-empty-icon > \.awe-icon,[^{]*\.modal-alert \.modal-header > i > \.awe-icon\s*\{\s*width: 1em;\s*height: 1em;?\s*\}/);
    expect(css).toMatch(/\.modal-alert \.modal-header \.fa,\s*\.modal-alert \.modal-header > i\s*\{\s*font-size: 90px/);
    expect(css).toMatch(/\.help-target\.fa,\s*\.help-target > \.awe-icon\s*\{\s*cursor: help;/);
  });

  it("uses no colour or size literal", () => {
    const source = fs.readFileSync(ICON_STYLES, "utf8").replace(/\/\/.*$/gm, "");
    expect(source.match(/#[0-9a-f]{3,8}\b|\b\d+(\.\d+)?(px|rem|ms|s)\b|rgba?\(/gi)).toBeNull();
  });
});
