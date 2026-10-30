const fs = require("fs");
const path = require("path");
const postcss = require("postcss");

const ROOT = path.resolve(__dirname, "../../../../..");
const TAILWIND_DIR = path.join(ROOT, "src", "main", "resources", "tailwind");
const ENTRY = path.join(TAILWIND_DIR, "awe-tailwind.css");

/**
 * Compile the Tailwind entry point with the same PostCSS configuration as the webpack build
 * @param {string} [extra] Extra CSS appended to the entry (for example an `@source inline(...)` to force a utility)
 * @return {Promise<string>} Compiled CSS
 */
const compileTailwind = (extra = "") => {
  const config = require(path.join(ROOT, "postcss.config.js"));
  const source = `${fs.readFileSync(ENTRY, "utf8")}\n${extra}`;
  return postcss(config.plugins).process(source, {from: ENTRY}).then(output => output.css);
};

/**
 * Flatten a stylesheet into rules: one entry per selector with its media query (normalized) and its declarations
 * @param {string} css Stylesheet
 * @return {Array<{selector: string, media: string, declarations: Object<string, string>}>} Rules in source order
 */
const getRules = css => {
  const rules = [];
  const normalizeMedia = params => params.replace(/\(width\s*>=\s*(\d+)px\)/, "(min-width: $1px)").replace(/\s+/g, " ").trim();
  const walk = (node, media) => {
    node.each(child => {
      if (child.type === "rule") {
        const declarations = {};
        const shadowed = {};
        child.each(declaration => {
          if (declaration.type === "decl") {
            // A later normal declaration replaces the value in `declarations` (the model every frozen rule was hashed with), but it does not
            // undo an earlier !important one in the same rule (`float: left !important; float: left`): that one is kept in `shadowed`
            if (!declaration.important && declarations[declaration.prop] && declarations[declaration.prop].endsWith(" !important")) {
              shadowed[declaration.prop] = declarations[declaration.prop];
            }
            declarations[declaration.prop] = `${declaration.value}${declaration.important ? " !important" : ""}`;
          }
        });
        const extra = Object.keys(shadowed).length ? {shadowed} : {};
        child.selectors.forEach(selector => rules.push({selector: selector.trim(), media, declarations, ...extra}));
      } else if (child.type === "atrule" && child.name === "media") {
        walk(child, normalizeMedia(child.params));
      } else if (child.type === "atrule" && child.name === "layer") {
        walk(child, media);
      }
    });
  };
  walk(postcss.parse(css), "");
  return rules;
};

/**
 * The rules with the !important declarations that a later normal one of the same rule replaced (see getRules) put back: the cascade applies them
 * @param {Array} rules Flattened rules
 * @return {Array} Rules whose declarations are the ones the cascade resolves
 */
const withShadowed = rules => rules.map(({shadowed, ...rule}) => (shadowed ? {...rule, declarations: {...rule.declarations, ...shadowed}} : rule));

/**
 * Evaluate the length of a declaration value as a percentage (`25%`, `calc(3 / 12 * 100%)`, `100%`) or a pixel value
 * @param {string} value Declaration value
 * @return {string} Normalized value: percentages with 4 decimals, anything else as is
 */
const normalizeLength = value => {
  const calc = /^calc\(\s*(-?\d+(?:\.\d+)?)\s*\/\s*(\d+)\s*\*\s*100%\s*\)$/.exec(value);
  if (calc) {
    return `${(Number(calc[1]) / Number(calc[2]) * 100).toFixed(4)}%`;
  }
  const negative = /^calc\(\s*(\d+(?:\.\d+)?)px\s*\*\s*-1\s*\)$/.exec(value);
  if (negative) {
    return `-${negative[1]}px`;
  }
  const percentage = /^(-?\d+(?:\.\d+)?)%$/.exec(value);
  if (percentage && Number(percentage[1]) === 0) {
    return "0";
  }
  if (percentage) {
    return `${Number(percentage[1]).toFixed(4)}%`;
  }
  return value;
};

/**
 * Selectors of the rules that are not inside a cascade layer (directly or through an @media/@supports): an unlayered rule
 * beats every layered one, so the Tailwind output must have none or it would override the Bootstrap LESS rules
 * @param {string} css Stylesheet
 * @return {string[]} Selectors outside any @layer block
 */
const getUnlayeredSelectors = css => {
  const found = [];
  const walk = node => node.each(child => {
    if (child.type === "rule") {
      found.push(child.selector);
    } else if (child.type === "atrule" && child.name !== "layer" && child.nodes) {
      walk(child);
    }
  });
  walk(postcss.parse(css));
  return found;
};

module.exports = {compileTailwind, getRules, getUnlayeredSelectors, normalizeLength, withShadowed, ROOT, TAILWIND_DIR, ENTRY};
