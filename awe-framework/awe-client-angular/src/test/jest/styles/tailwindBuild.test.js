import fs from "fs";
import os from "os";
import path from "path";
import {compileStyles} from "./support/compileStyles";
import {parseRules, tokenNames} from "./support/tokenRules";
import {compileTailwind, getRules, getUnlayeredSelectors, TAILWIND_DIR} from "./support/compileTailwind";
import {fileSelectors} from "./support/compatFile";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

const THEME = fs.readFileSync(path.join(TAILWIND_DIR, "theme.css"), "utf8");
const SOURCES = fs.readFileSync(path.join(TAILWIND_DIR, "sources.css"), "utf8");

const sourcePaths = () => [...SOURCES.matchAll(/@source\s+"([^"]+)"/g)].map(match => match[1]);

describe("awe-framework/awe-client-angular/src/test/jest/styles/tailwindBuild", () => {
  let css;

  beforeAll(async () => {
    css = await compileTailwind();
  });

  describe("coexistence with the Bootstrap 3 LESS build", () => {
    it("declares the layers in order, so the unlayered Bootstrap and AWE LESS always win over Tailwind", () => {
      expect(css).toMatch(/@layer theme, base, components, utilities;/);
    });

    it("emits every compat and utility rule inside a cascade layer, none unlayered (an unlayered rule would beat Bootstrap)", async () => {
      const forced = await compileTailwind("@source inline(\"tw:flex tw:p-3 tw:sm:w-1/2 tw:bg-primary\");");
      expect(forced).toMatch(/\.col-sm-6/);
      expect(forced).toMatch(/\.tw\\:flex/);
      expect(getUnlayeredSelectors(forced)).toEqual([]);
    });

    it("emits the responsive tw: variants on the Bootstrap 3 breakpoints (sm 768px, md 992px, lg 1200px)", async () => {
      const forced = await compileTailwind("@source inline(\"tw:sm:w-1/2 tw:md:w-1/3 tw:lg:w-1/4\");");
      const mediaOf = selector => getRules(forced).find(rule => rule.selector === selector)?.media;
      expect(mediaOf(".tw\\:sm\\:w-1\\/2")).toBe("(min-width: 768px)");
      expect(mediaOf(".tw\\:md\\:w-1\\/3")).toBe("(min-width: 992px)");
      expect(mediaOf(".tw\\:lg\\:w-1\\/4")).toBe("(min-width: 1200px)");
    });

    it("detects an unlayered rule, also inside a media query", () => {
      expect(getUnlayeredSelectors("@layer components { .a { color: red } } .b { color: red } @media (min-width: 1px) { .c { color: red } }"))
        .toEqual([".b", ".c"]);
    });

    it("does not ship the preflight reset, so the Bootstrap base styles stay", async () => {
      expect(css).not.toMatch(/@layer base\s*\{/);
      // A compat rule may set the box model of its own class (the modal dialog), and the compat base styles (scaffolding.css, normalize.css: slice 8) write the
      // Bootstrap element rules on purpose; the Tailwind reset is the one on every element that no compat file writes
      const written = new Set(["scaffolding.css", "normalize.css"].flatMap(file => [...fileSelectors(file)]));
      expect(getRules(css).filter(rule => rule.declarations["box-sizing"] === "border-box" && !/\.[\w-]/.test(rule.selector) && !written.has(rule.selector))).toEqual([]);
      // The Tailwind heading reset sets the font size and weight of every heading to inherit; the Bootstrap headings (type.css) set theirs from the type scale
      expect(getRules(css).filter(rule => rule.selector === "h1" && (rule.declarations["font-size"] === "inherit" || rule.declarations["font-weight"] === "inherit"))).toEqual([]);
    });

    it("only emits utilities with the tw: prefix, so no class collides with Bootstrap (hidden, table, collapse...)", async () => {
      const forced = await compileTailwind("@source inline(\"tw:flex tw:hidden tw:table flex hidden table\");");
      expect(forced).toMatch(/\.tw\\:flex\s*\{/);
      expect(forced).toMatch(/\.tw\\:hidden\s*\{/);
      expect(forced).not.toMatch(/\.flex\s*\{/);
      // `.hidden` is also a Bootstrap 3 name that the compat layer writes explicitly: forcing the unprefixed Tailwind one adds nothing
      expect(forced.match(/\.hidden\s*\{/g)).toEqual(css.match(/\.hidden\s*\{/g));
      // Likewise `.table`, the Bootstrap 3 table that the compat layer writes (tables.css)
      expect(forced.match(/\.table\s*\{/g)).toEqual(css.match(/\.table\s*\{/g));
    });

    it("does not detect sources by itself: a class only written in a test (tw:mt-7, tw:mb-9 below) is not emitted", () => {
      expect(css).not.toContain("tw\\:mt-7");
      expect(css).not.toContain("tw\\:mb-9");
      expect(css).not.toContain("tw\\:mr-11");
    });

    it("emits no Tailwind default theme: only the AWE tokens are available as colors", async () => {
      const forced = await compileTailwind("@source inline(\"tw:bg-red-500 tw:text-blue-600 tw:bg-primary\");");
      expect(forced).not.toMatch(/red-500|blue-600/);
      expect(forced).toMatch(/\.tw\\:bg-primary\s*\{\s*background-color:\s*var\(--awe-color-primary\)/);
    });
  });

  describe("theme mapped to the --awe-* tokens", () => {
    it("maps only tokens that the design tokens declare, each to a theme variable that keeps the var(--awe-...) reference", async () => {
      // The token file grew to the AWE 5 system (#885): the theme maps the semantic subset the compat layer and the tw: utilities use
      const declared = new Set(tokenNames(parseRules(await compileStyles())));
      const mapped = [...THEME.matchAll(/:\s*var\((--awe-[\w-]+)\);/g)].map(match => match[1]);
      expect(mapped.length).toBeGreaterThanOrEqual(50);
      expect(mapped.filter(name => !declared.has(name))).toEqual([]);
    });

    it("uses @theme inline, so a utility reads the token at runtime and follows the theme and the dark mode", () => {
      expect(THEME).toMatch(/@theme inline\s*\{/);
    });

    it.each([
      ["tw:bg-primary", /background-color:\s*var\(--awe-color-primary\)/],
      ["tw:text-on-primary", /color:\s*var\(--awe-color-on-primary\)/],
      ["tw:bg-panel", /background-color:\s*var\(--awe-color-panel\)/],
      ["tw:border-border", /border-color:\s*var\(--awe-color-border\)/],
      ["tw:rounded-base", /border-radius:\s*var\(--awe-radius\)/],
      ["tw:font-base", /font-family:\s*var\(--awe-font-family-base\)/],
      ["tw:text-base", /font-size:\s*var\(--awe-font-size-base\)/],
      ["tw:p-md", /padding:\s*var\(--awe-space-md\)/],
      ["tw:h-header", /height:\s*var\(--awe-size-header-height\)/],
      ["tw:shadow-md", /--tw-shadow:\s*var\(--awe-shadow-md\)/]
    ])("resolves the utility %s through a token", async (utility, expected) => {
      expect(await compileTailwind(`@source inline("${utility}");`)).toMatch(expected);
    });
  });

  describe("content scanning", () => {
    it("scans the AngularJS templates, the directives and the XML screens of the framework and of the test applications", () => {
      const paths = sourcePaths().join("\n");
      expect(paths).toMatch(/\.\.\/templates/);
      expect(paths).toMatch(/\.\.\/js\/awe/);
      expect(paths).toMatch(/awe-generic-screens/);
      expect(paths).toMatch(/awe-modules\/\*\//);
      expect(paths).toMatch(/awe-tests\/awe-boot\//);
    });

    it("points every scanned path to an existing folder (a wildcard to at least one, as not every module has screens)", () => {
      const resolve = source => {
        const [head, ...tail] = source.split("*");
        const base = path.resolve(TAILWIND_DIR, head);
        if (!tail.length) {
          return [base];
        }
        return fs.readdirSync(base).map(entry => path.join(base, entry, ...tail)).filter(folder => fs.existsSync(folder));
      };
      sourcePaths().forEach(source => {
        const existing = resolve(source).filter(folder => fs.existsSync(folder));
        expect({source, found: existing.length > 0}).toEqual({source, found: true});
      });
    });

    it("emits a utility written in an XML screen, a template or a directive", async () => {
      const folder = fs.mkdtempSync(path.join(os.tmpdir(), "awe-tailwind-"));
      try {
        fs.writeFileSync(path.join(folder, "Screen.xml"), "<view><panel style=\"tw:mt-7 col-xs-12\"/></view>");
        fs.writeFileSync(path.join(folder, "template.html"), "<div class=\"tw:mb-9\"></div>");
        fs.writeFileSync(path.join(folder, "directive.js"), "element.addClass('tw:mr-11');");
        const forced = await compileTailwind(`@source "${folder}";`);
        ["mt-7", "mb-9", "mr-11"].forEach(utility => expect(forced).toContain(`.tw\\:${utility}`));
      } finally {
        fs.rmSync(folder, {recursive: true, force: true});
      }
    });
  });
});
