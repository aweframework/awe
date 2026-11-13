import fs from "fs";
import path from "path";
import {TAILWIND_DIR} from "./support/compileTailwind";

// The compat files of the families that slice 5 sits next to: they were written in the same stylesheet, in the vendored order
const NEIGHBOURS = ["buttons.css", "button-groups.css", "input-groups.css", "input-groups-pixel-admin.css", "forms.css", "forms-pixel-admin.css", "panels.css", "utilities.css", "spacing.css"];
// The vendored build compiles the Bootstrap files in this order (legacy.less): the animations, the dropdowns, the navs, the navbar, then the close
// button and the modals, the tooltip and the popovers
const ORDER = ["component-animations.css", "dropdowns.css", "navs.css", "navbar.css", "modals.css", "tooltip.css", "popovers.css"];
const BOOTSTRAP_ORDER = ["component-animations", "dropdowns", "navs", "navbar", "close", "modals", "tooltip", "popovers"];

/**
 * Problems of legacy.less against the vendored order: every name has to be imported (as the vendored file or as its leftovers) and in sequence
 * @param {string} bootstrap Text of legacy.less
 * @param {string[]} names File names without extension, in the vendored order
 * @return {string[]} Problems, empty when right
 */
const bootstrapOrderProblems = (bootstrap, names) => {
  const position = name => Math.max(bootstrap.indexOf(`@import "${name}.less";`), bootstrap.indexOf(`@import "${name}-leftovers.less";`));
  const missing = names.filter(name => position(name) < 0).map(name => `${name} is not imported`);
  const found = names.filter(name => position(name) >= 0);
  const wrong = found.filter((name, index) => index > 0 && position(name) < position(found[index - 1])).map(name => `${name} comes out of order`);
  return [...missing, ...wrong];
};

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatSlice5Order", () => {
  const imports = () => [...fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8").matchAll(/^@import\s+"\.\/([\w.-]+\.css)";/gm)].map(match => match[1]);

  it("imports the files of the families in the order the vendored build compiles them: animations, dropdowns, navs, navbar, modals, tooltip, popovers", () => {
    expect(imports().filter(file => ORDER.includes(file))).toEqual(ORDER);
  });

  it("imports them after the files of the earlier families, which have to be imported (the vendored build compiles them before the components of slice 5)", () => {
    NEIGHBOURS.forEach(file => expect({file, position: imports().indexOf(file)}).not.toEqual({file, position: -1}));
    NEIGHBOURS.forEach(file => expect(imports().indexOf(ORDER[0])).toBeGreaterThan(imports().indexOf(file)));
  });

  it("is the order that legacy.less has: the eight files, close with the modals, each as the vendored file or as its leftovers", () => {
    const bootstrap = fs.readFileSync(path.join(TAILWIND_DIR, "..", "less", "legacy", "legacy.less"), "utf8");
    expect(bootstrapOrderProblems(bootstrap, BOOTSTRAP_ORDER)).toEqual([]);
  });

  describe("the check of legacy.less", () => {
    const line = name => `@import "${name}.less";\n`;

    it("accepts the vendored files and the leftovers, in sequence", () => {
      const mixed = BOOTSTRAP_ORDER.map((name, index) => line(index % 2 ? `${name}-leftovers` : name)).join("");
      expect(bootstrapOrderProblems(mixed, BOOTSTRAP_ORDER)).toEqual([]);
    });

    it("fails when none of the names is imported, or one is missing, or one is out of order", () => {
      expect(bootstrapOrderProblems("@import \"variables.less\";\n", BOOTSTRAP_ORDER)).toHaveLength(BOOTSTRAP_ORDER.length);
      expect(bootstrapOrderProblems(BOOTSTRAP_ORDER.filter(name => name !== "close").map(line).join(""), BOOTSTRAP_ORDER)).toEqual(["close is not imported"]);
      expect(bootstrapOrderProblems(["dropdowns", "component-animations", ...BOOTSTRAP_ORDER.slice(2)].map(line).join(""), BOOTSTRAP_ORDER)).toEqual(["dropdowns comes out of order"]);
    });
  });
});
