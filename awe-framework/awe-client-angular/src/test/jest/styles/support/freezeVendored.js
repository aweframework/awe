/**
 * Generates the frozen vendored rules of support/vendored-removed.baseline.json: the compiled vendored rules that a removal took out of
 * the LESS build, so the compat equivalence tests keep their proof after the vendored source is gone. The first removal (grid, panels,
 * utilities, responsive utilities, step 1) is the top-level `removed` list; later removals (step 2 and up) are appended under `later`.
 * Both are written by this script:
 *
 *   npm run compat:freeze-vendored -- <less folder before> <less folder after> --step 2 --files buttons.less,button-groups.less,forms.less,input-groups.less
 *
 * `--unserved` marks a step that dropped families with no compat CSS (nothing writes their classes): the compat tests skip its rules.
 *
 * <before> and <after> are the `less` folders (src/main/resources/less) of the build before and after the removal, for example
 * `git archive <commit before> awe-framework/awe-client-angular/src/main/resources/less | tar -x -C /tmp/before`. The rules of the
 * removal are the ones the build before has and the build after lacks (the rules that stay, like the leftovers files, are not
 * frozen: they are still in the build). Each frozen rule records the Bootstrap file it came from (found by importing that file as a
 * reference, which keeps its rules out of the output) and where it goes back (the position in the build after).
 *
 * It is run once per removal, from the tree before it, and never touched by a normal change.
 */
const fs = require("fs");
const {Buffer} = require("buffer");
const path = require("path");
const {compileStyles} = require("./compileStyles");
const {getRules} = require("./compileTailwind");
const {BASELINE, diffRemoved, digestOf, hashOf} = require("./vendoredBeforeRemoval");

// The byte counts of the baseline are the LF ones, as the CI builds them: a Windows checkout (core.autocrlf) has CRLF sources and the preserved comments carry them into the CSS
const lfBytes = css => Buffer.byteLength(css.replace(/\r\n/g, "\n"));

const compileTree = async lessDir => {
  const entry = path.join(lessDir, "awe.less");
  return compileStyles(fs.readFileSync(entry, "utf8"), {filename: entry});
};

/**
 * Which file produces each rule: compile the tree before with that file imported as a reference (its rules are not written)
 * @return {Promise<Map<string, string>>} File name by rule identity (hashOf)
 */
const sourcesOf = async (before, beforeRules, files) => {
  const sources = new Map();
  for (const file of files) {
    // A sibling of the folder (the same depth): awe.less imports the shared design tokens with a relative path
    const copy = fs.mkdtempSync(path.join(path.dirname(before), "freeze-vendored-"));
    try {
      fs.cpSync(before, copy, {recursive: true});
      const entry = path.join(copy, "legacy", "legacy.less");
      const source = fs.readFileSync(entry, "utf8");
      if (!source.includes(`@import "${file}";`)) {
        throw new Error(`legacy.less does not import ${file}`);
      }
      fs.writeFileSync(entry, source.replace(`@import "${file}";`, `@import (reference) "${file}";`));
      const without = new Set(getRules(await compileTree(copy)).map(hashOf));
      beforeRules.filter(rule => !without.has(hashOf(rule))).forEach(rule => sources.has(hashOf(rule)) || sources.set(hashOf(rule), file));
    } finally {
      fs.rmSync(copy, {recursive: true, force: true});
    }
  }
  return sources;
};

const option = (args, name) => (args.includes(name) ? args[args.indexOf(name) + 1] : undefined);

const main = async () => {
  const args = process.argv.slice(2);
  const [before, after] = args;
  const step = Number(option(args, "--step"));
  const files = (option(args, "--files") || "").split(",").filter(Boolean);
  if (!before || !after || !(step >= 1) || files.length === 0) {
    throw new Error("Usage: freezeVendored.js <less folder before> <less folder after> --step <1 or more> --files <a.less,b.less>");
  }
  const beforeCss = await compileTree(before);
  const afterCss = await compileTree(after);
  const beforeRules = getRules(beforeCss);
  const sources = await sourcesOf(before, beforeRules, files);
  const removed = diffRemoved(beforeRules, getRules(afterCss)).map(rule => ({...rule, source: sources.get(hashOf(rule)) || null}));
  const unsourced = removed.filter(rule => !rule.source);
  if (unsourced.length) {
    throw new Error(`${unsourced.length} rules vanished that none of ${files.join(", ")} produces, for example ${unsourced[0].selector}: a rule set they define is called or extended and lost`);
  }
  // The rules of the files that stay in the build (the leftovers): the cascade guard decided they stay, so a test keeps them there
  const afterHashes = new Set(getRules(afterCss).map(hashOf));
  const kept = [...new Set(beforeRules.filter(rule => sources.has(hashOf(rule)) && afterHashes.has(hashOf(rule))).map(hashOf))];
  const document = JSON.parse(fs.readFileSync(BASELINE, "utf8"));
  const later = (document.later || []).filter(entry => entry.header.step !== step);
  if (step === 1) {
    // The first removal is the top-level list (it has no header, kept list nor digest)
    document.removed = removed;
    document.lessBytesBefore = lfBytes(beforeCss);
    document.lessBytesAfter = lfBytes(afterCss);
  } else {
    later.push({header: {step, files, ...(args.includes("--unserved") ? {unserved: true} : {}), lessBytesBefore: lfBytes(beforeCss), lessBytesAfter: lfBytes(afterCss), kept, digest: digestOf(beforeRules)}, removed});
  }
  later.sort((a, b) => a.header.step - b.header.step);
  // The first removal is written one rule per line; each later one has a header line and one rule per line
  const lines = document.removed.map(rule => `    ${JSON.stringify(rule)}`).join(",\n");
  const head = JSON.stringify({...document, removed: undefined, later: undefined}, null, 2).replace(/\n}$/, "");
  const laterText = later.map(entry => `    {\n      "header": ${JSON.stringify(entry.header)},\n      "removed": [\n${entry.removed.map(rule => `        ${JSON.stringify(rule)}`).join(",\n")}\n      ]\n    }`).join(",\n");
  fs.writeFileSync(BASELINE, `${head},\n  "removed": [\n${lines}\n  ],\n  "later": [\n${laterText}\n  ]\n}\n`);
  console.log(`${removed.length} rules frozen (step ${step}) into ${path.relative(process.cwd(), BASELINE)} (LESS build ${lfBytes(beforeCss)} -> ${lfBytes(afterCss)} bytes)`);
};

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
