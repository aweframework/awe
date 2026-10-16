import fs from "fs";
import path from "path";

const COMPONENTS = path.join(__dirname, "../../../src/components");
const read = (file) => fs.readFileSync(path.join(COMPONENTS, file), "utf8");

describe("awe-react-client/test/js/styles/skeletonDelayTest.js", () => {
  const less = read("AweSkeleton.less");

  it("fades the visuals in after 150 ms, so the skeleton hook is in the page from the start", () => {
    expect(less).toMatch(/@skeleton-delay:\s*150ms;/);
    expect(less).toMatch(/@keyframes awe-skeleton-appear\s*\{\s*to\s*\{\s*opacity:\s*1;/);
    expect(less).toMatch(/\.skeleton-delayed\(\)\s*\{\s*opacity:\s*0;\s*animation:\s*awe-skeleton-appear 0s linear @skeleton-delay forwards;/);
  });

  it.each([
    ".grid-skeleton .p-skeleton",
    ".screen-skeleton-contents",
    ".awe-loader-skeleton .p-skeleton",
    ".awe-skeleton-criterion .p-skeleton",
    ".awe-skeleton-button .p-skeleton",
    ".awe-skeleton-chart .p-skeleton"
  ])("delays the visuals of %s", (selector) => {
    const group = less.match(/((?:^[.\w -]+,\n)*[^{\n]+)\{\s*\.skeleton-delayed\(\);\s*\}/m)[1];

    expect(group.split(",").map(item => item.trim())).toContain(selector);
  });

  it("dims the rows of a grid that reloads, without making them clickable, and shows the progress cursor", () => {
    expect(less).toMatch(/\.grid-container\.grid-reloading\s*\{[^]*?> \.grid-contents\s*\{[^}]*opacity:\s*0\.5;[^}]*pointer-events:\s*none/);
    expect(less).toMatch(/\.grid-container\.grid-reloading\s*\{\s*cursor:\s*progress/);
  });

  it("is loaded by every component that paints a skeleton", () => {
    ["AweSkeleton.jsx", "AweGridSkeleton.jsx", "AweScreenSkeleton.jsx", "AweLoader.jsx", "AweGridContainer.jsx"]
      .forEach(file => expect(read(file)).toContain('import "./AweSkeleton.less";'));
  });
});
