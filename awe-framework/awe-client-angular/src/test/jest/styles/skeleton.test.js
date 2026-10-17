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
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[^@]*\.skeleton-bar\s*\{[^}]*animation:\s*awe-skeleton-appear/);
    expect(css).not.toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[^@]*\.skeleton-bar\s*\{[^}]*awe-skeleton-shimmer/);
  });

  it("styles the grid and screen skeleton containers", () => {
    expect(css).toMatch(/\.grid-skeleton\s*\{/);
    expect(css).toMatch(/\.screen-skeleton\s*\{/);
  });

  it("delays the skeleton visuals 150 ms and keeps the containers, the test hooks, in place", () => {
    expect(css).toMatch(/@keyframes awe-skeleton-appear\s*\{[^}]*\{[^}]*opacity:\s*1/);
    expect(css).toMatch(/\.skeleton-bar\s*\{[^}]*opacity:\s*0[^}]*awe-skeleton-appear\s+0s\s+linear\s+150ms\s+forwards/);
    expect(css).toMatch(/@keyframes awe-skeleton-panel/);
    expect(css).toMatch(/\.grid-skeleton \.grid-skeleton-body\s*\{[^}]*background-color:\s*transparent[^}]*awe-skeleton-panel\s+0s\s+linear\s+150ms\s+forwards/);
    expect(css).toMatch(/\.screen-skeleton\s*\{[^}]*background-color:\s*transparent[^}]*awe-skeleton-backdrop\s+0s\s+linear\s+150ms\s+forwards/);
    expect(css).not.toMatch(/\.(grid-skeleton|screen-skeleton|skeleton-loader)\s*\{[^}]*opacity:\s*0/);
  });

  it("hides the content of the view that is loading after the delay, so no previous screen paints over the skeleton", () => {
    expect(css).toMatch(/@keyframes awe-skeleton-hide\s*\{[^}]*\{[^}]*visibility:\s*hidden/);
    expect(css).toMatch(/\[ui-view\]\[aria-busy="true"\]\s*\{[^}]*awe-skeleton-hide\s+0s\s+linear\s+150ms\s+forwards/);
    expect(css).not.toMatch(/(^|[\s,}])\[aria-busy="true"\]\s*\{[^}]*awe-skeleton-hide/);
  });

  it("masks the label of a criterion and a button while their skeleton is pending, after the delay", () => {
    expect(css).toMatch(/@keyframes awe-skeleton-mask\s*\{[^}]*color:\s*transparent[^}]*var\(--awe-color-skeleton\)/);
    expect(css).toMatch(/\.criterion\.skeleton-pending[^{]*\{[^}]*awe-skeleton-mask\s+0s\s+linear\s+150ms\s+forwards/);
    expect(css).toMatch(/\.skeleton-pending[^{]*button[^{]*\{[^}]*pointer-events:\s*none[^}]*awe-skeleton-mask/);
  });

  it("shapes the chart hints and the checkbox and radio boxes", () => {
    expect(css).toMatch(/\.skeleton-chart-pie[^{]*\{[^}]*border-radius:\s*50%/);
    expect(css).toMatch(/\.skeleton-chart-trend[^{]*\{[^}]*clip-path/);
    expect(css).toMatch(/\.skeleton-chart-bars/);
    expect(css).toMatch(/\.skeleton-check\.skeleton-radio\s*\{[^}]*border-radius:\s*50%/);
  });

  it("dims the rows of a grid that reloads, with no pointer events, and adds no new token", () => {
    expect(css).toMatch(/\.grid\.grid-reloading \.grid-container\s*\{[^}]*opacity:[^}]*pointer-events:\s*none/);
    expect(css).toMatch(/\.loader\.grid-reload\s*\{[^}]*background-color:\s*transparent/);
    expect(Object.keys(tokens).filter(name => name.startsWith("--awe-color-skeleton")).sort())
      .toEqual(["--awe-color-skeleton", "--awe-color-skeleton-highlight"]);
  });
});
