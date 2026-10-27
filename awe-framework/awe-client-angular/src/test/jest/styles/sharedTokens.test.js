import fs from "fs";
import path from "path";
import {compileStyles, ROOT} from "./support/compileStyles";
import {parseRules, tokenNames} from "./support/tokenRules";

const SHARED_TOKENS = path.join(ROOT, "..", "awe-design-tokens", "tokens.css");
const BRIDGE = path.join(ROOT, "src", "main", "resources", "less", "awe", "tokens.less");
const WEBSITE = path.join(ROOT, "..", "..", "website");
const PROTOTYPE_TOKENS = path.join(WEBSITE, "static", "design-system", "prototype", "tokens.css");
const TOKEN_REFERENCE = path.join(WEBSITE, "docs", "design-system", "tokens-reference.md");

// What the prototype has and the engines do not take from it: the Inter font face (the engines load Inter with the
// shell), the colour-mode rules (the engines set the mode on <html> with the theme attributes) and the stand-in of a
// customer logo
const isPrototypeOnly = rule => rule.selector.startsWith(".light") || rule.selector.startsWith(".dark")
  || Object.keys(rule.declarations).some(name => name === "color-scheme" || name.startsWith("--awe-customer-logo-"));

const read = file => fs.readFileSync(file, "utf8");

// The names of the AngularJS tokens (#770), which the engine bridged to its classic themes until T24 of #885
const FORMER_BRIDGE_NAMES = ["--awe-color-theme", "--awe-color-text", "--awe-color-primary", "--awe-color-on-primary",
  "--awe-color-secondary", "--awe-color-on-secondary", "--awe-color-background", "--awe-color-surface",
  "--awe-color-border", "--awe-color-shadow", "--awe-color-menu", "--awe-color-menu-accent", "--awe-color-menu-text",
  "--awe-color-navbar", "--awe-color-navbar-accent", "--awe-color-navbar-text", "--awe-color-navbar-dropdown",
  "--awe-color-panel", "--awe-color-panel-header", "--awe-color-panel-border", "--awe-color-panel-text",
  "--awe-color-grid-header", "--awe-color-grid-border", "--awe-color-skeleton", "--awe-color-skeleton-highlight",
  "--awe-color-disabled", "--awe-color-disabled-border", "--awe-color-disabled-text", "--awe-color-required",
  "--awe-color-loading-bar", "--awe-color-danger", "--awe-color-warning", "--awe-color-success", "--awe-color-info",
  "--awe-font-family-base", "--awe-font-family-monospace", "--awe-font-size-base", "--awe-font-size-small",
  "--awe-line-height-base", "--awe-font-weight-normal", "--awe-font-weight-bold", "--awe-space-2xs", "--awe-space-xs",
  "--awe-space-sm", "--awe-space-md", "--awe-space-lg", "--awe-space-xl", "--awe-radius", "--awe-radius-sm",
  "--awe-radius-lg", "--awe-radius-full", "--awe-shadow-sm", "--awe-shadow-md", "--awe-shadow-lg",
  "--awe-size-header-height", "--awe-size-menu-width"];

// The token names the LESS of the engine declares
const bridgeNames = () => [...read(BRIDGE).matchAll(/^\s*(--awe-[a-z0-9-]+)\s*:/gm)].map(match => match[1]);

describe("awe-framework/awe-client-angular/src/test/jest/styles/sharedTokens", () => {
  let css;
  let shared;

  beforeAll(async () => {
    css = await compileStyles();
    shared = read(SHARED_TOKENS);
  });

  describe("the AWE 5 token file (#885)", () => {
    it("is shipped whole in the stylesheet of the engine", () => {
      expect(css).toContain(shared.trim());
    });

    it("holds every token of the prototype, with the same selectors and values", () => {
      const prototype = parseRules(read(PROTOTYPE_TOKENS)).filter(rule => !isPrototypeOnly(rule));
      expect(parseRules(shared)).toEqual(prototype);
    });

    it("declares only custom properties of the --awe- prefix", () => {
      const others = parseRules(shared).flatMap(rule => Object.keys(rule.declarations)).filter(name => !name.startsWith("--awe-"));
      expect(others).toEqual([]);
    });

    it("documents every token in the token reference", () => {
      const reference = read(TOKEN_REFERENCE);
      // The primitive palettes are documented as one row per palette, one column per shade
      const documented = name => {
        const palette = /^--awe-palette-([a-z]+)-(\d+)$/.exec(name);
        return palette ? reference.includes(`| \`${palette[1]}\` |`) && reference.includes(`| ${palette[2]} |`)
          : reference.includes(`\`${name}\``);
      };
      expect(tokenNames(parseRules(shared)).filter(name => !documented(name))).toEqual([]);
    });
  });

  describe("without a bridge (#885 T24: the engine reads the AWE 5 values)", () => {
    it("declares no token in the LESS of the engine", () => {
      expect(bridgeNames()).toEqual([]);
    });

    it("keeps the 56 names of the former bridge in the AWE 5 token file", () => {
      const names = tokenNames(parseRules(shared));
      expect(FORMER_BRIDGE_NAMES).toHaveLength(56);
      expect(FORMER_BRIDGE_NAMES.filter(name => !names.includes(name))).toEqual([]);
    });
  });
});
