import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {getOsDarkVariables, getRuleVariables, getThemeNames, getThemeSnapshot} from "./support/themeVariables";

const BASELINE = JSON.parse(fs.readFileSync(path.join(__dirname, "support", "themes.baseline.json"), "utf8"));
const DOCUMENTATION = path.join(__dirname, "../../../../../../website/docs/guides/design-tokens-guide.md");

const TOKEN_PREFIX = "--awe-";
const TOKEN_SELECTOR = ":root, .base-theme";
const OS_DARK_THEME_RULE = /prefers-color-scheme:\s*dark\)\s*\{\s*\.theme-[a-z-]+:not\(\.light\):not\(\.dark\)/;
const TOKEN_GROUPS = ["color", "font-size", "font-family", "line-height", "font-weight", "space", "radius", "shadow", "size"];

describe("awe-framework/awe-client-angular/src/test/jest/styles/designTokens", () => {
  let css;
  let tokens;

  beforeAll(async () => {
    css = await compileStyles();
    tokens = getRuleVariables(css, TOKEN_SELECTOR) || {};
  });

  describe("theme variables (what the themes override)", () => {
    it("keeps the 19 theme names", () => {
      expect(getThemeNames(css).sort()).toEqual(Object.keys(BASELINE).sort());
      expect(Object.keys(BASELINE)).toHaveLength(19);
    });

    it("keeps the variables of every theme, light and dark, exactly as before the tokens", () => {
      expect(getThemeSnapshot(css)).toEqual(BASELINE);
    });

    it.each(Object.keys(BASELINE))("applies the dark variables of %s when the OS prefers dark and the user did not choose", theme => {
      expect(getOsDarkVariables(css, theme)).toEqual(BASELINE[theme].dark);
    });

    it("does not apply the OS preference when the user chose the light mode or the dark mode", () => {
      expect(css).toMatch(OS_DARK_THEME_RULE);
      expect(css).not.toMatch(/prefers-color-scheme: dark\)\s*\{\s*\.theme-[a-z-]+\s*\{/);
      expect(css).not.toMatch(/prefers-color-scheme: dark\)\s*\{\s*\.theme-[a-z-]+\.dark/);
    });
  });

  describe("token layer (what the components and Tailwind read)", () => {
    it("emits every token on the base theme, so each theme and each mode resolves its own values", () => {
      const names = Object.keys(tokens).filter(name => name.startsWith(TOKEN_PREFIX));
      expect(names.length).toBeGreaterThanOrEqual(50);
      TOKEN_GROUPS.forEach(group => {
        expect(names.some(name => name.startsWith(`${TOKEN_PREFIX}${group}-`))).toBe(true);
      });
    });

    it("follows the theme variable that a theme, a database theme or a product overrides", () => {
      expect(tokens["--awe-color-primary"]).toMatch(/var\(--primary-color/);
      expect(tokens["--awe-color-text"]).toMatch(/var\(--text-color/);
      expect(tokens["--awe-color-panel"]).toMatch(/var\(--panel-background-color/);
      expect(tokens["--awe-color-border"]).toMatch(/var\(--border-color/);
      expect(tokens["--awe-radius"]).toMatch(/var\(--border-radius/);
      expect(tokens["--awe-font-size-base"]).toMatch(/var\(--primary-font-size/);
    });

    it("derives the radius, shadow and spacing scales from the base tokens", () => {
      expect(tokens["--awe-radius-sm"]).toMatch(/var\(--awe-radius\)/);
      expect(tokens["--awe-shadow-md"]).toMatch(/var\(--awe-color-shadow\)/);
      expect(tokens["--awe-space-md"]).toBe("15px");
    });

    it("has a fallback on the document root for the elements outside the themed body", () => {
      expect(getRuleVariables(css, TOKEN_SELECTOR)).not.toBeNull();
    });

    it("documents every token in the design tokens guide", () => {
      const guide = fs.readFileSync(DOCUMENTATION, "utf8");
      const undocumented = Object.keys(tokens)
        .filter(name => name.startsWith(TOKEN_PREFIX))
        .filter(name => !guide.includes(`\`${name}\``));
      expect(undocumented).toEqual([]);
    });
  });

  describe("compatibility with the products", () => {
    it("keeps compiling with the LESS variables of a product and uses them", async () => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe.less"), "utf8");
      const product = await compileStyles(`${source}\n@screen-small: 800px;\n@border-color: #123456;\n`);
      expect(product).toMatch(/#123456/);
    });

    it("lets a product switch the OS preference off", async () => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe.less"), "utf8");
      const product = await compileStyles(`${source}\n@awe-follow-os-color-scheme: false;\n`);
      expect(product).not.toMatch(OS_DARK_THEME_RULE);
      expect(getThemeSnapshot(product)).toEqual(BASELINE);
    });
  });
});
