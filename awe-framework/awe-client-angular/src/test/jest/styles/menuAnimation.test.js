const fs = require("fs");
const path = require("path");

const DIRECTIVES = path.resolve(__dirname, "../../../main/resources/less/awe/directives");
const read = file => fs.readFileSync(path.join(DIRECTIVES, file), "utf8").replace(/\/\/.*$/gm, "");

// The horizontal menu bar of the AWE 5 shell (#885): it shows and hides smoothly and its icons sit on the centre line
describe("Horizontal menu (#885)", () => {
  const horizontal = read("menu-horizontal.less");
  const block = selector => {
    const start = horizontal.indexOf(selector);
    return start < 0 ? "" : horizontal.slice(start, horizontal.indexOf("}", start));
  };

  it("keeps the bar a flex row while it shows or hides, so the options do not reflow", () => {
    const animating = block("&.ng-hide-add,\n    &.ng-hide-remove {");
    expect(animating).toMatch(/display: flex !important;/);
    expect(animating).not.toMatch(/display: ?block/);
  });

  it("animates the height, the padding and the opacity with the motion tokens", () => {
    const animating = block("&.ng-hide-add,\n    &.ng-hide-remove {");
    expect(animating).toMatch(/transition: height var\(--awe-motion-duration-slow\) var\(--awe-motion-easing-standard\),/);
    expect(animating).toMatch(/padding-top var\(--awe-motion-duration-slow\) var\(--awe-motion-easing-standard\),/);
    expect(animating).toMatch(/padding-bottom var\(--awe-motion-duration-slow\) var\(--awe-motion-easing-standard\),/);
    expect(animating).toMatch(/opacity var\(--awe-motion-duration-slow\) var\(--awe-motion-easing-standard\);/);
  });

  it("collapses the padding with the height, so the bar ends at zero without a jump", () => {
    const hidden = block("&.ng-hide-add.ng-hide-add-active,\n    &.ng-hide-remove {");
    expect(hidden).toMatch(/height: 0;/);
    expect(hidden).toMatch(/padding-top: 0;/);
    expect(hidden).toMatch(/padding-bottom: 0;/);
  });

  it("does not animate for users who prefer reduced motion", () => {
    expect(horizontal).toMatch(/@media \(prefers-reduced-motion: reduce\) \{\s*#main-menu\.horizontal ul\.navigation\.ng-hide-add,\s*#main-menu\.horizontal ul\.navigation\.ng-hide-remove \{\s*transition: none;/);
  });

  it("centres the SVG of an option icon in its box", () => {
    const menu = read("menu.less");
    expect(menu).toMatch(/#main-menu ul\.navigation \.menu-icon \{[^}]*display: inline-flex;[^}]*align-items: center;[^}]*justify-content: center;/);
    expect(menu).toMatch(/#main-menu ul\.navigation \.menu-icon > svg \{[^}]*display: block;[^}]*width: 100%;[^}]*height: 100%;/);
  });

  it("keeps the bar low: options as high as a small control and a thin bar padding", () => {
    expect(horizontal).toMatch(/@main-menu-height: calc\(var\(--awe-size-control-sm\) \+ 2 \* var\(--awe-space-1\)\);/);
    expect(horizontal).toMatch(/#main-menu\.horizontal ul\.navigation > li > a \{[^}]*min-height: var\(--awe-size-control-sm\);[^}]*padding: 0 var\(--awe-space-3\);/);
    expect(horizontal).toMatch(/#main-menu\.horizontal ul\.navigation \{[^}]*padding: var\(--awe-space-1\) calc\(var\(--awe-page-padding\) \+ var\(--awe-size-control-sm\) \+ var\(--awe-space-2\)\) var\(--awe-space-1\) var\(--awe-page-padding\);/);
    const shown = block("&.ng-hide-add,\n    &.ng-hide-remove.ng-hide-remove-active {");
    expect(shown).toMatch(/padding-top: var\(--awe-space-1\);/);
    expect(shown).toMatch(/padding-bottom: var\(--awe-space-1\);/);
    const search = fs.readFileSync(path.join(DIRECTIVES, "menu-search.less"), "utf8");
    expect(search).toMatch(/\.awe-menu-search-horizontal \{[^}]*top: var\(--awe-space-1\);/);
    expect(search).toMatch(/\.awe-menu-search-horizontal \.awe-menu-search-toggle \{[^}]*min-height: var\(--awe-size-control-sm\);/);
  });
});
