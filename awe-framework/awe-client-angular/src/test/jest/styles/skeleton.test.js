import {compileStyles} from "./support/compileStyles";
import {getRuleVariables} from "./support/themeVariables";

const TOKEN_SELECTOR = ":root, .base-theme";

describe("awe-framework/awe-client-angular/src/test/jest/styles/skeleton", () => {
  let css;
  let tokens;

  beforeAll(async () => {
    css = await compileStyles();
    tokens = getRuleVariables(css, TOKEN_SELECTOR) || {};
  });

  it("derives the skeleton colors from the grid colors of the active theme and mode", () => {
    expect(tokens["--awe-color-skeleton"]).toMatch(/var\(--grid-border-color/);
    expect(tokens["--awe-color-skeleton-highlight"]).toMatch(/var\(--grid-header-color/);
  });

  it("paints the skeleton bars with the tokens and a shimmer animation", () => {
    expect(css).toMatch(/@keyframes awe-skeleton-shimmer/);
    expect(css).toMatch(/\.skeleton-bar\s*\{[^}]*var\(--awe-color-skeleton\)[^}]*var\(--awe-color-skeleton-highlight\)/);
    expect(css).toMatch(/\.skeleton-bar\s*\{[^}]*animation:\s*awe-skeleton-shimmer/);
  });

  it("disables the shimmer when the user prefers reduced motion", () => {
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[^@]*\.skeleton-bar\s*\{[^}]*animation:\s*none/);
  });

  it("styles the grid and screen skeleton containers", () => {
    expect(css).toMatch(/\.grid-skeleton\s*\{/);
    expect(css).toMatch(/\.screen-skeleton\s*\{/);
  });
});
