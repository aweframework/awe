const fs = require("fs");
const path = require("path");

// Inter (#885): the AngularJS engine loads the same face as the React engine, from the shared design tokens folder
const MODULE = path.resolve(__dirname, "../../../..");
const LESS = path.join(MODULE, "src/main/resources/less");
const read = file => fs.readFileSync(file, "utf8");

describe("Inter font (#885)", () => {
  it("loads the shared Inter face before the AWE styles", () => {
    const entry = read(path.join(MODULE, "src/main/resources/webpack/awe.config.js"));
    const font = entry.indexOf('import "../../../../../awe-design-tokens/fonts/inter.css";');
    expect(font).toBeGreaterThanOrEqual(0);
    expect(font).toBeLessThan(entry.indexOf('import "./../less/awe";'));
  });

  it("lets the stylesheet rule of the bundle read the shared font folder", () => {
    expect(read(path.join(MODULE, "webpack.config.js")))
      .toMatch(/test : \/\\\.css\$\/, include : \[ styleDir, fontsDir \]/);
  });

  it("names the base font family token in every Bootstrap font family", () => {
    expect(read(path.join(LESS, "awe/variables.less")))
      .toMatch(/^@font-family-sans-serif: var\(--awe-font-family-base\);$/m);
  });

  it("sets the base size and line height of the type scale on the body", () => {
    const body = read(path.join(LESS, "awe/base.less")).match(/\nbody \{[^}]*\}/);
    expect(body).not.toBeNull();
    expect(body[0]).toContain("font-size: var(--awe-font-size-base);");
    expect(body[0]).toContain("line-height: var(--awe-line-height-base);");
  });

  it("publishes the SIL Open Font License next to the bundled fonts", () => {
    const pom = read(path.join(MODULE, "pom.xml"));
    expect(pom).toMatch(/<directory>\$\{project\.basedir\}\/\.\.\/awe-design-tokens\/fonts<\/directory>\s*<targetPath>\$\{project\.build\.directory\}\/classes\/static\/fonts<\/targetPath>\s*<includes>\s*<include>LICENSE-inter-OFL\.txt<\/include>/);
  });
});
