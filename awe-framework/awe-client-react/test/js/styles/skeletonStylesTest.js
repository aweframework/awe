import fs from "fs";
import path from "path";

const SRC = path.join(__dirname, "../../../src");
const read = (...parts) => fs.readFileSync(path.join(SRC, ...parts), "utf8");
const PRIMEREACT = path.join(__dirname, "../../../node_modules/primereact");
const {buildThemeFrom, readPrimeReact, TOKENS_FILE} = require("../../../../awe-design-tokens/primereact/primereact-theme.cjs");

describe("awe-react-client/test/js/styles/skeletonStylesTest.js", () => {
  it("takes the skeleton tokens from the AWE 5 token file shared with the AngularJS client", () => {
    const tokens = fs.readFileSync(TOKENS_FILE, "utf8");

    expect(tokens).toMatch(/--awe-color-skeleton:\s*light-dark\(/);
    expect(tokens).toMatch(/--awe-color-skeleton-highlight:\s*light-dark\(/);
    expect(read("main.css")).not.toMatch(/--awe-color-skeleton(-highlight)?\s*:/);
  });

  it("disables the skeleton shimmer when the user prefers reduced motion", () => {
    const main = read("main.css");

    expect(main).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[\s\S]*?\.p-skeleton::after\s*\{[^}]*animation:\s*none/);
  });

  it("styles the PrimeReact skeleton with the shared tokens", () => {
    const css = buildThemeFrom(readPrimeReact(PRIMEREACT).theme, PRIMEREACT);

    expect(css).toMatch(/\.p-skeleton\s*\{[^}]*background-color:\s*var\(--awe-color-skeleton\)/);
    expect(css).toMatch(/\.p-skeleton:after\s*\{[^}]*linear-gradient\(90deg,\s*transparent,\s*var\(--awe-color-skeleton-highlight\),\s*transparent\)/);
  });
});
