import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR, ROOT} from "./support/compileStyles";
import {parseRules} from "./support/tokenRules";

const SHARED_TOKENS = path.join(ROOT, "..", "awe-design-tokens", "tokens.css");

// The theme variables of the classic themes and of /css/themeVariables.css, replaced by the AWE 5 tokens (#885)
const LEGACY_VARIABLES = ["theme-color", "text-color", "primary-color", "secondary-color", "primary-text-color",
  "secondary-text-color", "primary-background-color", "secondary-background-color", "primary-menu-color",
  "secondary-menu-color", "menu-text-color", "menu-color", "primary-navbar-color", "secondary-navbar-color",
  "navbar-text-color", "navbar-dropdown-color", "primary-font-size", "secondary-font-size", "disabled-color",
  "disabled-border-color", "disabled-text-color", "header-height", "panel-background-color", "panel-header-color",
  "panel-border-color", "panel-text-color", "grid-header-color", "grid-border-color", "danger-color", "warning-color",
  "success-color", "info-color", "border-color", "border-radius", "loading-bar-color", "required-color", "shadow-color",
  "text-color-secondary"];

// What a LESS variable of the engine (read by the Bootstrap 3 / pixel-admin rules and by the products) resolves to
const LESS_VARIABLES = {
  "@primary-color": "var(--awe-color-primary)",
  "@primary-text-color": "var(--awe-color-on-primary)",
  "@text-color": "var(--awe-color-text)",
  "@primary-background-color": "var(--awe-color-background)",
  "@secondary-background-color": "var(--awe-color-surface)",
  "@panel-background-color": "var(--awe-color-panel)",
  "@primary-menu-color": "var(--awe-color-menu)",
  "@primary-navbar-color": "var(--awe-color-navbar)",
  "@border-color": "var(--awe-color-border)",
  "@border-radius": "var(--awe-radius)",
  "@primary-font-size": "var(--awe-font-size-base)",
  "@main-navbar-height": "var(--awe-size-header-height)",
  "@danger-color": "var(--awe-color-danger)"
};

const read = file => fs.readFileSync(file, "utf8");

describe("awe-framework/awe-client-angular/src/test/jest/styles/designTokens", () => {
  let css;
  let engineCss;

  beforeAll(async () => {
    css = await compileStyles();
    // The stylesheet of the engine without the shared token file
    engineCss = css.replace(read(SHARED_TOKENS).trim(), "");
  });

  describe("the AWE 5 tokens are the only theme (#885)", () => {
    it("declares the --awe-* tokens only in the shared token file (no bridge of the engine)", () => {
      expect(engineCss.match(/--awe-[a-z0-9-]+\s*:/g)).toBeNull();
    });

    it("has no classic theme rule: the classic names are presets resolved by the server", () => {
      expect(engineCss.match(/\.theme-[a-z-]+/g)).toBeNull();
    });

    it("neither declares nor reads the theme variables of the classic themes", () => {
      const legacy = LEGACY_VARIABLES.filter(name => new RegExp(`--${name}\\s*[:,)]`).test(engineCss));
      expect(legacy).toEqual([]);
    });

    it("takes the colour mode of the tokens from <html>: the system one unless the user chose light or dark", () => {
      const rules = parseRules(engineCss);
      const colorScheme = selector => rules.find(rule => rule.selector === selector)?.declarations["color-scheme"];
      expect(colorScheme(":root")).toBe("light dark");
      expect(colorScheme(":root.light")).toBe("light");
      expect(colorScheme(":root.dark")).toBe("dark");
    });

    it("does not follow the operating system through theme media rules any more", () => {
      expect(engineCss).not.toMatch(/prefers-color-scheme:\s*dark\)\s*\{\s*\.theme-/);
    });
  });

  describe("compatibility with the products", () => {
    const compileWith = extra => compileStyles(`${read(path.join(LESS_DIR, "awe.less"))}\n${extra}\n`);

    it.each(Object.entries(LESS_VARIABLES))("resolves the LESS variable %s to the token %s", async (name, token) => {
      const product = await compileWith(`.product-check { color: ${name}; }`);
      expect(product).toMatch(new RegExp(`\\.product-check\\s*\\{\\s*color:\\s*${token.replace(/[()]/g, "\\$&")};`));
    });

    it("keeps compiling with the LESS variables of a product and uses them", async () => {
      const product = await compileWith("@screen-small: 800px;\n@border-color: #123456;");
      expect(product).toMatch(/#123456/);
    });

    it("keeps compiling when a product still sets the removed OS preference switch", async () => {
      const product = await compileWith("@awe-follow-os-color-scheme: false;");
      expect(product).toContain(read(SHARED_TOKENS).trim());
    });
  });
});
