import fs from "fs";
import path from "path";
import {fromColor, getSwatchColor, SWATCH_PALETTES, toColor} from "../../../src/utilities/color";

describe("awe-react-client/test/js/utilities/colorTest.ts", () => {
  it.each([
    ["a bare hexadecimal value", "ff0000", "#ff0000"],
    ["a value that already has the hash", "#00ff00", "#00ff00"],
    ["an empty value", "", ""],
    ["null", null, ""],
    ["undefined", undefined, ""]
  ])("converts %s to a color", (_name, value, expected) => {
    expect(toColor(value)).toBe(expected);
  });

  it.each([
    ["a color with hash", "#ff0000", "ff0000"],
    ["a bare value", "ff0000", "ff0000"],
    ["several hashes", "#ff#00", "ff00"],
    ["an empty value", "", ""],
    ["null", null, ""],
    ["undefined", undefined, ""]
  ])("strips the hash from %s", (_name, value, expected) => {
    expect(fromColor(value)).toBe(expected);
  });
});

// Colour swatch of a grid cell and of the identity header of a side panel (#793): only a known AWE 5 palette or a hex
// reaches the style of the circle (same rules as the AngularJS engine)
describe("awe-react-client/test/js/utilities/colorTest.ts swatch (#793)", () => {
  const TOKENS = fs.readFileSync(path.resolve(__dirname, "../../../../awe-design-tokens/tokens.css"), "utf8");

  it.each(SWATCH_PALETTES)("draws the %s palette with its 500 shade of the design tokens", palette => {
    expect(getSwatchColor(palette)).toBe(`var(--awe-palette-${palette}-500)`);
    expect(TOKENS).toContain(`--awe-palette-${palette}-500:`);
  });

  it("knows every primary and neutral palette of the themes", () => {
    expect(SWATCH_PALETTES).toEqual(["red", "orange", "amber", "yellow", "lime", "green", "emerald", "teal", "cyan", "sky",
      "blue", "indigo", "violet", "purple", "fuchsia", "pink", "rose", "slate", "gray", "zinc", "neutral", "stone"]);
  });

  it.each([[" Emerald ", "var(--awe-palette-emerald-500)"], ["SKY", "var(--awe-palette-sky-500)"]])(
    "trims the palette %p and ignores its case", (value, color) => {
      expect(getSwatchColor(value)).toBe(color);
    });

  it.each([["#10B981", "#10B981"], ["#abc", "#abc"], [" #0f0f0F ", "#0f0f0F"]])("draws the hex %p as it is, trimmed", (value, color) => {
    expect(getSwatchColor(value)).toBe(color);
  });

  it.each([
    ["empty", ""], ["null", null], ["undefined", undefined], ["an unknown palette", "custom"], ["auto", "auto"],
    ["a hex without #", "10B981"], ["a hex of 4 digits", "#abcd"], ["a hex of 8 digits", "#10B98180"], ["a non hex digit", "#GGGGGG"],
    ["a CSS injection", "red;background:url(x)"], ["a CSS function", "rgb(1,2,3)"], ["a variable", "var(--x)"],
    ["a hex with an injection", "#fff;color:red"], ["a hex with a new line", "#fff\nx"], ["a number", 12], ["an object", {value: "red"}]
  ])("draws nothing for %s", (_name, value) => {
    expect(getSwatchColor(value)).toBe("");
  });
});
