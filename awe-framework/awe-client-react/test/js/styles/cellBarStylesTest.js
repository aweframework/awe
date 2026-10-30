import fs from "fs";
import path from "path";

const less = fs.readFileSync(path.join(__dirname, "../../../src/columns/Columns.less"), "utf8");

// Body of the rule whose selector is exactly the given one (null when there is none)
const ruleBody = (selector) => {
  const start = less.indexOf(`
${selector} {`);
  return start < 0 ? null : less.slice(start, less.indexOf("}", start));
};

// Every rule of the data bar block
const dataBarLess = () => less.slice(less.indexOf(".awe-data-bar {"), less.indexOf("// Image column"));

describe("awe-react-client/test/js/styles/cellBarStylesTest.js", () => {
  it("does not keep the old cell bar rules", () => {
    expect(less).not.toContain("awe-cell-bar");
  });

  it("spans the cell padding with a relative wrapper, as the data bar of the design system", () => {
    const body = ruleBody(".awe-data-bar");
    expect(body).toMatch(/position:\s*relative;/);
    expect(body).toMatch(/padding:\s*var\(--awe-space-0-5\) var\(--awe-space-2\);/);
    expect(body).toMatch(/margin:\s*calc\(var\(--awe-space-0-5\) \* -1\) calc\(var\(--awe-space-2\) \* -1\);/);
  });

  it("fills a single bar with the data bar colours and the medium radius", () => {
    expect(ruleBody(".awe-data-bar-fill")).toMatch(/background:\s*var\(--awe-color-data-bar\);/);
    expect(ruleBody(".awe-data-bar-fill")).toMatch(/border-radius:\s*var\(--awe-radius-md\);/);
    expect(ruleBody(".awe-data-bar-fill.awe-data-bar-negative"))
      .toMatch(/background:\s*var\(--awe-color-data-bar-negative\);/);
  });

  it("splits two thin rounded lanes with a small gap and a neutral second fill", () => {
    expect(ruleBody(".awe-data-bar-lanes")).toMatch(/gap:\s*var\(--awe-space-1\);/);
    expect(ruleBody(".awe-data-bar-two .awe-data-bar-fill")).toMatch(/border-radius:\s*var\(--awe-radius-full\);/);
    expect(ruleBody(".awe-data-bar-lane-second .awe-data-bar-fill"))
      .toMatch(/background:\s*var\(--awe-color-neutral-subtle\);/);
    expect(ruleBody(".awe-data-bar-lane-second .awe-data-bar-fill.awe-data-bar-negative"))
      .toMatch(/background:\s*var\(--awe-color-data-bar-negative\);/);
  });

  it("squares the end on the zero of a signed scale and rounds only the outer end", () => {
    expect(ruleBody(".awe-data-bar .awe-data-bar-signed"))
      .toMatch(/border-radius:\s*0 var\(--awe-radius-md\) var\(--awe-radius-md\) 0;/);
    expect(ruleBody(".awe-data-bar .awe-data-bar-signed.awe-data-bar-negative"))
      .toMatch(/border-radius:\s*var\(--awe-radius-md\) 0 0 var\(--awe-radius-md\);/);
    expect(ruleBody(".awe-data-bar-two .awe-data-bar-signed"))
      .toMatch(/border-radius:\s*0 var\(--awe-radius-full\) var\(--awe-radius-full\) 0;/);
    expect(ruleBody(".awe-data-bar-two .awe-data-bar-signed.awe-data-bar-negative"))
      .toMatch(/border-radius:\s*var\(--awe-radius-full\) 0 0 var\(--awe-radius-full\);/);
  });

  it("defines no custom property of its own (tokens live in the token file)", () => {
    expect(less).not.toMatch(/--_/);
  });

  it("rounds only the outer ends of a stack", () => {
    expect(ruleBody(".awe-data-bar-segment:first-child")).toMatch(/border-radius:\s*var\(--awe-radius-md\) 0 0 var\(--awe-radius-md\);/);
    expect(ruleBody(".awe-data-bar-segment:last-child")).toMatch(/border-radius:\s*0 var\(--awe-radius-md\) var\(--awe-radius-md\) 0;/);
    expect(ruleBody(".awe-data-bar-segment:only-child")).toMatch(/border-radius:\s*var\(--awe-radius-md\);/);
  });

  it("draws the stacked segments with the data scale", () => {
    [2, 3, 4].forEach((tone, index) => {
      expect(ruleBody(`.awe-data-bar-segment-${index}`))
        .toMatch(new RegExp(`background:\\s*var\\(--awe-color-scale-${tone}\\);`));
    });
  });

  it("draws the zero axis with the chart axis colour over the bars", () => {
    expect(ruleBody(".awe-data-bar-axis")).toMatch(/border-left:\s*thin solid var\(--awe-color-chart-axis\);/);
  });

  it("shows the number with tabular figures, right aligned, and the danger text for a negative one", () => {
    const body = ruleBody(".awe-data-bar-value");
    expect(body).toMatch(/text-align:\s*right;/);
    expect(body).toMatch(/font-variant-numeric:\s*var\(--awe-font-variant-numeric\);/);
    expect(ruleBody(".awe-data-bar-value-negative")).toMatch(/color:\s*var\(--awe-color-danger-text\);/);
  });

  it("uses tokens only: no colour or size literals and no theme or density selectors", () => {
    const block = dataBarLess();
    expect(block).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(block).not.toMatch(/\d+px/);
    expect(block).not.toMatch(/rgba?\(/);
    expect(block).not.toMatch(/\.dark|density/);
  });
});
