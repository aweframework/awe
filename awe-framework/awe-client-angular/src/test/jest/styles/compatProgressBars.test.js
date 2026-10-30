import {compatFamilyCases} from "./support/compatFamily";
import {progressBars} from "./support/compatFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${progressBars.title}`, () => {
  it.each(compatFamilyCases(progressBars))("%s", (title, check) => check());
});

describe("awe-framework/awe-client-angular/src/test/jest/styles/compat progress bars (motion)", () => {
  const fs = require("fs");
  const path = require("path");
  const {TAILWIND_DIR} = require("./support/compileTailwind");
  const css = fs.readFileSync(path.join(TAILWIND_DIR, "compat", "progress-bars.css"), "utf8");

  it("keeps the striped keyframes that the active bar animates", () => {
    expect(css).toMatch(/@keyframes progress-bar-stripes\s*\{\s*from\s*\{[^}]*background-position/);
    expect(css).toMatch(/\.progress-bar\.active\s*\{[^}]*animation:\s*progress-bar-stripes/);
  });

  it("takes the duration of the stripes and of the width transition from the motion tokens, which are 0ms under prefers-reduced-motion", () => {
    const tokens = fs.readFileSync(path.join(TAILWIND_DIR, "..", "..", "..", "..", "..", "awe-design-tokens", "tokens.css"), "utf8");
    expect(tokens).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*:root\s*\{[^}]*--awe-motion-duration-slow: 0ms/);
    expect(css).toMatch(/animation:\s*progress-bar-stripes calc\(var\(--awe-motion-duration-slow\)/);
    expect(css).toMatch(/transition:\s*width calc\(var\(--awe-motion-duration-slow\)/);
    expect(css).not.toMatch(/animation:[^;]*\b\d+(\.\d+)?m?s\b/);
  });
});
