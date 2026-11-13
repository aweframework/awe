const fs = require("fs");
const path = require("path");

const LESS = path.resolve(__dirname, "../../../main/resources/less/awe/directives/menu-horizontal.less");

// The menu toggle draws a Lucide SVG (#885): the pixel-admin offsets were made for a 60px-high font icon and push the
// 16px SVG out of the top of the button, so the AWE rule centres it in the button in every menu layout
describe("Menu toggle icon (#885)", () => {
  const less = fs.readFileSync(LESS, "utf8").replace(/\/\/.*$/gm, "");

  it("centres the SVG icon of the toggle in the button, over the pixel-admin offsets of every layout", () => {
    const rule = less.match(/([^{}]*svg\.navbar-icon[^{}]*)\{([^}]*)\}/);
    expect(rule).not.toBeNull();
    const [, selector, body] = rule;
    expect(selector).toContain("#main-navbar #main-menu-toggle svg.navbar-icon");
    expect(selector).toContain(".main-menu-right #main-navbar #main-menu-toggle svg.navbar-icon");
    expect(selector).toContain("#main-navbar.horizontal #main-menu-toggle svg.navbar-icon");
    expect(selector).toContain(".mmc #main-navbar.horizontal #main-menu-toggle svg.navbar-icon");
    expect(body).toMatch(/inset: 0;/);
    expect(body).toMatch(/margin: auto;/);
    expect(body).toMatch(/width: var\(--awe-size-icon-lg\);/);
    expect(body).toMatch(/height: var\(--awe-size-icon-lg\);/);
  });

  it("gives the collapse toggle of the narrow navbar the height of the header, with its SVG centred", () => {
    const home = fs.readFileSync(path.resolve(__dirname, "../../../main/resources/less/awe/pages/home.less"), "utf8");
    const rule = home.match(/#main-navbar \.navbar-toggle \{([^}]*)\}/);
    expect(rule).not.toBeNull();
    expect(rule[1]).toMatch(/height: var\(--awe-header-height\);/);
    expect(rule[1]).toMatch(/display: flex;/);
    expect(rule[1]).toMatch(/align-items: center;/);
    expect(rule[1]).toMatch(/justify-content: center;/);
  });
});
