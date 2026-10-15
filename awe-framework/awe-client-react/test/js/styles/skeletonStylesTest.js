import fs from "fs";
import path from "path";

const SRC = path.join(__dirname, "../../../src");
const read = (...parts) => fs.readFileSync(path.join(SRC, ...parts), "utf8");

describe("awe-react-client/test/js/styles/skeletonStylesTest.js", () => {
  it("declares the shared skeleton tokens next to the other AWE tokens", () => {
    const main = read("main.css");

    // Same colors as the AngularJS client: grid border and grid header
    expect(main).toMatch(/--awe-color-skeleton:\s*var\(--surface-c,\s*#dbdbdb\);/);
    expect(main).toMatch(/--awe-color-skeleton-highlight:\s*var\(--surface-b,\s*#ebebeb\);/);
  });

  it("disables the skeleton shimmer when the user prefers reduced motion", () => {
    const main = read("main.css");

    expect(main).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[\s\S]*?\.p-skeleton::after\s*\{[^}]*animation:\s*none/);
  });

  it.each(["sky", "sunset"])("styles the skeleton of the %s theme with the shared tokens", (theme) => {
    const css = read("static", "css", "themes", theme, "theme.css");

    expect(css).toMatch(/\.p-skeleton\s*\{[^}]*background-color:\s*var\(--awe-color-skeleton\)/);
    expect(css).toMatch(/\.p-skeleton::after\s*\{[^}]*linear-gradient\(90deg,\s*var\(--awe-color-skeleton\),\s*var\(--awe-color-skeleton-highlight\),\s*var\(--awe-color-skeleton\)\)/);
  });
});
