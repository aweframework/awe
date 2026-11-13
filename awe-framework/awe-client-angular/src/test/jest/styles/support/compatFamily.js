const fs = require("fs");
const path = require("path");
const {overriddenSelectors, vendoredBeforeRemoval} = require("./vendoredBeforeRemoval");
const {compileTailwind, getRules, normalizeLength, withShadowed, TAILWIND_DIR} = require("./compileTailwind");
const {compileStyles} = require("./compileStyles");
const {commonWinners, shape, effective, hoistSupported, mediasOf, namesOf, normalizeSelector, orderWinners} = require("./compatEquivalence");
const {sourceFiles} = require("./xmlScreens");
const {tokenNames, parseRules} = require("./tokenRules");
const {fileRules, fileSelectors} = require("./compatFile");
const {importantConflicts, overlaps} = require("./importantGuard");
const {meetable, mediaMeet} = require("./cascadeGuard");

// The family hook runs three full builds (vendored, Tailwind and the AWE LESS): on a busy CI runner they take longer than the per-test timeout
const BUILD_TIMEOUT = 180000;

const IMPORTANT = " !important";
const COMPAT_DIR = path.join(TAILWIND_DIR, "compat");

/**
 * Tailwind's output has the same values as the compiled vendored CSS, written as lightningcss prints them (`0px` as `0`, `0.5` as `.5`,
 * one space after each comma)
 */
const normalize = value => normalizeLength(value.replace(/\b0px\b/g, "0")).replace(/\s*,\s*/g, ", ").replace(/(?<![\d])0\.(\d)/g, ".$1");

/**
 * The served !important declarations that a rule left outside the compat layer could meet on an element: an overlapping property with another
 * !important value, a media query that can apply at the same time (a served `@media print` declaration meets the rules with no media query too)
 * and selectors that one element can match
 * @param {Array} vendored Flattened vendored rules (with the shadowed !important put back)
 * @param {Array} served Rules the compat file serves
 * @param {Set<string>} inCompat Normalized selectors that any compat file writes (those are layered too, so they are not competitors)
 * @return {string[]} One line per hazard
 */
const importantHazards = (vendored, served, inCompat) => {
  const own = new Set(served.map(rule => normalizeSelector(rule.selector)));
  const others = vendored.filter(rule => !own.has(normalizeSelector(rule.selector)) && !inCompat.has(normalizeSelector(rule.selector)));
  return served.flatMap(rule => Object.entries(rule.declarations).filter(([, value]) => value.endsWith(IMPORTANT)).flatMap(([property, value]) => others
    .filter(other => mediaMeet(other.media, rule.media)
      && Object.entries(other.declarations).some(([name, otherValue]) => otherValue.endsWith(IMPORTANT) && overlaps(property, name) && normalize(otherValue) !== normalize(value))
      && meetable(normalizeSelector(rule.selector), normalizeSelector(other.selector)))
    .map(other => `${rule.selector} { ${property} } over ${other.selector}`)));
};

/**
 * Problems of the position of a compat file in index.css: every listed neighbour has to be imported (an absent one would make the comparison pass
 * vacuously) and the file has to come after the `after` ones and before the `before` ones
 * @param {string[]} order Files in import order
 * @param {string} file The compat file
 * @param {string[]} after Files it is imported after
 * @param {string[]} before Files it is imported before
 * @return {string[]} Problems, empty when the order is right
 */
const importOrderProblems = (order, file, after, before) => [
  ...(order.includes(file) ? [] : [`${file} is not imported`]),
  ...[...after, ...before].filter(other => !order.includes(other)).map(other => `${other} is not imported`),
  ...after.filter(other => order.includes(other) && order.indexOf(file) < order.indexOf(other)).map(other => `${file} comes before ${other}`),
  ...before.filter(other => order.includes(other) && order.indexOf(other) < order.indexOf(file)).map(other => `${file} comes after ${other}`)
];

const hasDeclarations = rule => Object.keys(rule.declarations).length > 0;

/**
 * The checks every family of the compat layer has to pass against the vendored build: the selectors it serves are the vendored ones
 * with the same effective declarations in every media query, the cascade order among them is the vendored one, and every other
 * selector that mentions one of its classes is left out for a stated reason (with examples that must still exist in the LESS build)
 * @param {object} config Family: `title`, `file` (compat file), `use` (name => whether a class belongs to the family), `group` (classes the
 * family serves), `context` (classes of other families that sit in its selectors), `leftOut` (entries `{names|match, reason, examples}`),
 * `sample` (a vendored selector that proves the comparison is not empty), `after` and `before` (compat files that this one is imported after and before)
 * @return {Array<[string, Function]>} The checks as `[title, check]` pairs. The test file declares them with `it.each` (SonarJS S2187 only counts a file
 * as having tests when it calls `it` itself); the `beforeAll` that compiles the builds is registered in the `describe` that calls this function
 */
const compatFamilyCases = config => {
  const {file, use, group, context, leftOut, sample, after = [], before = [], markers = [], servedBy = [], excluded = [], importantReviewed = [], owned = [], selectors = [], hazardsReviewed = []} = config;
  // Element rules have no class to find them by: a family can also list the exact selectors of the vendored file it replaces (base styles: normalize, scaffolding, type, code, print)
  const listed = new Set(selectors.map(normalizeSelector));
  const ELEMENTS = "element rules";
  const hasElementRules = selectors.some(selector => !namesOf(normalizeSelector(selector)).some(name => group.includes(name)));
  const GROUPS = [...group, ...(hasElementRules ? [ELEMENTS] : [])];
  const CLASSES = [...group, ...context];
  const isLeftOut = name => leftOut.some(entry => (entry.names ? entry.names.includes(name) : entry.match(name)));
  const familyUse = selector => listed.has(normalizeSelector(selector)) || namesOf(selector).some(use);
  // A selector of the family that an earlier compat file already serves (the dropdown toggle inside a button group) stays there: it is written once
  const elsewhere = () => new Set(servedBy.flatMap(other => [...fileSelectors(other)]));
  // A family that lists its selectors serves exactly those: the other selectors that mention its classes (`.text-success a`) are rules of pixel-admin or of the AWE LESS that stay there
  const inFamily = selector => (listed.size ? listed.has(selector) : familyUse(selector) && namesOf(selector).every(name => CLASSES.includes(name)));
  const excludedSelectors = new Set(excluded.flatMap(entry => entry.selectors));
  // Rules of the AWE 5 design system (#885) on a selector of the family: they are written in the AWE LESS, unlayered, and win over the compat layer
  const ownedSelectors = new Set(owned.flatMap(entry => entry.selectors));

  // Each check of the family, as a `[title, check]` pair: the ones of a group of the original layout have its name as prefix
  const cases = [];
  const checksOf = group => (name, check) => cases.push([group ? `${group}: ${name}` : name, check]);
  const check = checksOf();

  let vendored;
  let compat;
  // What the browser applies: the compat layer under the rules that stay in the LESS build (unlayered, so they win over it)
  let view;
  // Selectors that unlayered pixel-admin or AWE rules style too: they win over the compat rule whatever the order
  let overridden;
  let selectorsOf;
  let family;

  beforeAll(async () => {
    vendored = withShadowed(await vendoredBeforeRemoval());
    compat = getRules(hoistSupported(await compileTailwind()));
    const current = getRules(await compileStyles());
    view = [...compat, ...current];
    overridden = overriddenSelectors(current);
    const taken = elsewhere();
    family = [...new Set(vendored.filter(hasDeclarations).map(rule => normalizeSelector(rule.selector)))].filter(selector => inFamily(selector) && !taken.has(selector) && !excludedSelectors.has(selector) && !ownedSelectors.has(selector));
    selectorsOf = name => family.filter(selector => (name === ELEMENTS ? !namesOf(selector).some(other => group.includes(other)) : namesOf(selector).includes(name)));
  }, BUILD_TIMEOUT);

  check("compares with the vendored rules, which are still in the build or frozen in the baseline", () => {
    expect(vendored.some(rule => normalizeSelector(rule.selector) === sample)).toBe(true);
  });

  check("accounts for every vendored selector that mentions a class of the family: served here or left out for a stated reason", () => {
    const outside = [...new Set(vendored.filter(rule => familyUse(rule.selector)).flatMap(rule => namesOf(rule.selector)))].filter(name => !CLASSES.includes(name));
    expect(outside.filter(name => !isLeftOut(name))).toEqual([]);
    const named = leftOut.flatMap(entry => entry.names || []);
    expect(named.filter(name => !outside.includes(name))).toEqual([]);
    leftOut.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
  });

  check("keeps in LESS the example selectors of every left-out entry: they exist in the vendored build and this file does not serve them", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    const examples = leftOut.flatMap(entry => entry.examples || []);
    expect(leftOut.filter(entry => entry.names && (!entry.examples || entry.examples.length === 0))).toEqual([]);
    expect(examples.filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    const written = fileSelectors(file);
    expect(examples.filter(selector => written.has(selector))).toEqual([]);
  });

  check("leaves to the AWE LESS the selectors that only the AWE 5 design system styles: they exist in the build and this file does not write them", () => {
    owned.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    const written = fileSelectors(file);
    expect([...ownedSelectors].filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    expect([...ownedSelectors].filter(selector => written.has(selector))).toEqual([]);
  });

  check("leaves to the compat files of other families the selectors that they serve: this file never writes one twice", () => {
    const taken = elsewhere();
    servedBy.forEach(other => expect(fileSelectors(other).size).toBeGreaterThan(0));
    expect(fileRules(file).map(rule => normalizeSelector(rule.selector)).filter(selector => taken.has(selector))).toEqual([]);
  });

  check("lists only selectors that exist in the vendored build, and each is served here or left to the AWE LESS", () => {
    const vendoredSelectors = new Set(vendored.map(rule => normalizeSelector(rule.selector)));
    expect([...listed].filter(selector => !vendoredSelectors.has(selector))).toEqual([]);
    expect([...listed].filter(selector => !family.includes(selector) && !ownedSelectors.has(selector) && !excludedSelectors.has(selector))).toEqual([]);
  });

  check("serves every class of the family, and this file holds exactly the selectors of the family", () => {
    expect(group.filter(name => selectorsOf(name).length === 0)).toEqual([]);
    const served = new Set(family);
    const written = fileRules(file).map(rule => normalizeSelector(rule.selector));
    expect(written.filter(selector => !served.has(selector))).toEqual([]);
    expect(family.filter(selector => !written.includes(selector))).toEqual([]);
  });

  describe.each(GROUPS)("%s", name => {
    check("has the same selectors and effective declarations as the compiled vendored CSS in every media query", () => {
      const selectors = selectorsOf(name);
      expect(selectors.length).toBeGreaterThan(0);
      selectors.forEach(selector => {
        expect({selector, medias: mediasOf(view, selector)}).toEqual({selector, medias: mediasOf(vendored, selector)});
        mediasOf(vendored, selector).forEach(media => {
          expect({selector, media, declarations: shape(effective(view, selector, media, normalize))})
            .toEqual({selector, media, declarations: shape(effective(vendored, selector, media, normalize))});
        });
      });
    });
  });

  {
    const check = checksOf("themed values");
    check("reads the --awe-* tokens that LESS compiles, with no fallback and no theme variable of the classic themes", () => {
      expectCompatTokens({compat, family, file});
    });
  }

  {
    const check = checksOf("source use");
    check("serves every class of the family that the templates, the directives and the XML screens write, but the ones left out", () => {
      const used = new Set(sourceFiles().flatMap(source => [...fs.readFileSync(source, "utf8").matchAll(/(?<![\w-])([a-z]+(?:-[a-z]+)*)(?![\w-])/g)].map(match => match[1])).filter(use));
      expect([...used].filter(name => !CLASSES.includes(name) && !isLeftOut(name) && !markers.includes(name))).toEqual([]);
    });

    check("lists as markers only classes that the sources write and no vendored rule styles", () => {
      const styled = new Set(vendored.flatMap(rule => namesOf(rule.selector)));
      expect(markers.filter(name => styled.has(name))).toEqual([]);
    });
  }

  {
    const check = checksOf("cascade order");
    // Two element selectors that no element can both match (`code` and `fieldset`) do not compete: only a family of listed selectors (the base styles) has such pairs
    const meetsOnly = listed.size ? meetable : undefined;

    check("lets the same rule win as in the vendored build wherever two rules of the family with the same specificity compete", () => {
      const ordered = family.filter(selector => !overridden.has(selector));
      const winners = orderWinners(vendored, ordered, normalize, meetsOnly);
      const common = commonWinners(orderWinners(compat, ordered, normalize, meetsOnly), winners);
      expect(common.actual).toEqual(common.expected);
    });
  }

  {
    const check = checksOf("!important and the cascade layers");
    /**
     * A layered !important beats every unlayered !important, whatever the specificity: a served rule that carries one would repaint an element
     * that a rule which stays in LESS paints with its own !important (a themed button color, a plugin). This finds the served !important
     * declarations that another rule outside the compat layer (same media, an overlapping property, another value) can meet
     */
    const hazards = rules => {
      const inCompat = new Set(fs.readdirSync(COMPAT_DIR).filter(name => name.endsWith(".css") && name !== "index.css").flatMap(name => [...fileSelectors(name)]));
      return importantHazards(vendored, rules, inCompat);
    };

    check("has no !important that would meet an unlayered one on an element, by the classes that the sources write together", () => {
      const written = fileSelectors(file);
      const {conflicts} = importantConflicts(vendored, compat.filter(rule => written.has(normalizeSelector(rule.selector))), GROUPS, selectorsOf, normalize);
      expect(conflicts.filter(conflict => !importantReviewed.some(entry => entry.rule === conflict.rule && entry.property === conflict.property))).toEqual([]);
    });

    check("reviews only conflicts that still exist, each with its reason", () => {
      const written = fileSelectors(file);
      const {conflicts} = importantConflicts(vendored, compat.filter(rule => written.has(normalizeSelector(rule.selector))), GROUPS, selectorsOf, normalize);
      importantReviewed.forEach(entry => {
        expect(entry.reason.length).toBeGreaterThan(20);
        expect(conflicts.some(conflict => conflict.rule === entry.rule && conflict.property === entry.property)).toBe(true);
      });
    });

    // The selector and media query of every rule that this file writes: a rule of the same selector in another media query (the print `*`) or of another source (a pixel-admin `body`) is not served here
    const written = () => new Set(fileRules(file).map(rule => `${normalizeSelector(rule.selector)}|${rule.media}`));
    const found = () => [...new Set(hazards(vendored.filter(rule => family.includes(normalizeSelector(rule.selector)) && hasDeclarations(rule) && written().has(`${normalizeSelector(rule.selector)}|${rule.media}`))))];

    check("serves no !important declaration that a rule left in LESS with its own !important could meet on an element, but the reviewed ones", () => {
      expect(found().filter(hazard => !hazardsReviewed.some(entry => entry.hazard === hazard))).toEqual([]);
    });

    check("reviews only hazards that still exist, each with its reason", () => {
      hazardsReviewed.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
      expect(hazardsReviewed.filter(entry => !found().includes(entry.hazard))).toEqual([]);
    });

    check("excludes only selectors that are such a hazard (or that write a url()), each for a stated reason, and keeps them out of this file", () => {
      const written = fileSelectors(file);
      excluded.forEach(entry => expect(entry.reason.length).toBeGreaterThan(20));
      const rules = vendored.filter(rule => excludedSelectors.has(normalizeSelector(rule.selector)));
      expect(new Set(rules.map(rule => normalizeSelector(rule.selector)))).toEqual(excludedSelectors);
      expect([...excludedSelectors].filter(selector => written.has(selector))).toEqual([]);
      // A url() is resolved relative to the file that writes it: moved to another folder it would no longer find its image
      const urls = new Set(excluded.filter(entry => entry.url).flatMap(entry => entry.selectors));
      urls.forEach(selector => expect({selector, url: rules.some(rule => normalizeSelector(rule.selector) === selector && Object.values(rule.declarations).some(value => value.includes("url(")))}).toEqual({selector, url: true}));
      // Each of the others is hazardous on its own: served, it would meet a rule that stays
      const own = rules.filter(rule => !urls.has(normalizeSelector(rule.selector)) && Object.values(rule.declarations).some(value => value.endsWith(IMPORTANT)));
      expect(new Set(own.map(rule => normalizeSelector(rule.selector)))).toEqual(new Set([...excludedSelectors].filter(selector => !urls.has(selector))));
      const hazardous = new Set(hazards(own).map(text => text.split(" {")[0]));
      expect([...excludedSelectors].filter(selector => !urls.has(selector) && !hazardous.has(selector))).toEqual([]);
    });

    check("writes no url() in this file: a relative path would not resolve from the compat folder", () => {
      expect(fs.readFileSync(path.join(COMPAT_DIR, file), "utf8")).not.toContain("url(");
    });

    check("pins the left-out selectors that carry !important: they exist with it and stay in LESS", () => {
      const written = fileSelectors(file);
      leftOut.filter(entry => entry.important).forEach(entry => {
        entry.examples.forEach(selector => {
          const important = vendored.some(rule => normalizeSelector(rule.selector) === selector && Object.values(rule.declarations).some(value => value.endsWith(" !important")));
          expect({selector, important, served: written.has(selector)}).toEqual({selector, important: true, served: false});
        });
      });
    });
  }

  {
    const check = checksOf("import order");
    const imports = () => [...fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8").matchAll(/^@import\s+"\.\/([\w.-]+\.css)";/gm)].map(match => match[1]);

    check(`imports ${file} where the vendored build compiles it: after ${after.join(", ") || "nothing"} and before ${before.join(", ") || "nothing"}`, () => {
      expect(importOrderProblems(imports(), file, after, before)).toEqual([]);
    });
  }

  return cases;
};

/**
 * States, in the test of a family, the selectors that its file header says stay in LESS: each exists in the vendored build and the compat file
 * does not write it, so the claim of the header is checked where it is made
 * @param {string} title Family title
 * @param {string} file Compat file
 * @param {string[]} selectors Selectors that stay in LESS
 */
const describeLessOnly = (title, file, selectors) => {
  describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${title} (what the header of ${file} says stays in LESS)`, () => {
    it("exists in the vendored build and is not written in the compat file", async () => {
      const vendored = new Set((await vendoredBeforeRemoval()).map(rule => normalizeSelector(rule.selector)));
      const written = fileSelectors(file);
      expect(selectors.filter(selector => !vendored.has(selector))).toEqual([]);
      expect(selectors.filter(selector => written.has(selector))).toEqual([]);
      const header = fs.readFileSync(path.join(COMPAT_DIR, file), "utf8").split("*/")[0];
      expect(header).toContain("Not here");
    });
  });
};


/**
 * The compat file reads the AWE 5 tokens (skills/awe-ui): every var() is a --awe-* token that the shared token file declares, with no fallback, no
 * variable of the classic themes and no primitive of the palette. The values may differ from the compiled vendored ones: they follow the tokens
 * @param {{compat: Array, family: string[], file: string|string[]}} context Rules of the compat build, selectors of the family and compat file(s)
 */
const expectCompatTokens = ({compat, family, file}) => {
  const declared = new Set(tokenNames(parseRules(fs.readFileSync(path.join(TAILWIND_DIR, "..", "..", "..", "..", "..", "awe-design-tokens", "tokens.css"), "utf8"))));
  const references = value => [...value.matchAll(/var\(--([\w-]+)\s*([,)])/g)].map(match => ({name: match[1], fallback: match[2] === ","}));
  const written = new Set([].concat(file).flatMap(name => [...fileSelectors(name)]));
  const values = compat.filter(rule => written.has(normalizeSelector(rule.selector)) && (!family || family.includes(normalizeSelector(rule.selector))))
    .flatMap(rule => Object.values(rule.declarations));
  const used = values.flatMap(references);
  expect(used.filter(reference => !reference.name.startsWith("awe-") || reference.fallback || reference.name.startsWith("awe-palette-"))).toEqual([]);
  expect([...new Set(used.map(reference => `--${reference.name}`))].filter(name => !declared.has(name))).toEqual([]);
};

module.exports = {expectCompatTokens, describeLessOnly, compatFamilyCases, normalize, hasDeclarations, importantHazards, importOrderProblems};
