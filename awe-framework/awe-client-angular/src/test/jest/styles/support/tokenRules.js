/**
 * Parse the rules of a plain CSS stylesheet (no nesting besides at-rules such as @media)
 * @param {string} css Stylesheet
 * @return {{selector: string, declarations: Object<string, string>}[]} Rules in source order; the selector of a rule
 * inside an at-rule is prefixed with the at-rule ("@media (...) :root")
 */
const parseRules = css => {
  const source = css.replace(/\/\*[\s\S]*?\*\//g, "");
  const rules = [];
  const parents = [];
  let start = 0;
  for (let index = 0; index < source.length; index++) {
    const char = source[index];
    if (char === "{") {
      parents.push(source.slice(start, index).trim().replace(/\s+/g, " "));
      start = index + 1;
    } else if (char === "}") {
      const body = source.slice(start, index);
      const selector = parents.pop();
      if (selector !== undefined && !selector.startsWith("@")) {
        rules.push({selector: [...parents, selector].join(" "), declarations: parseDeclarations(body)});
      }
      start = index + 1;
    }
  }
  return rules;
};

/**
 * Parse the declarations of a rule body, keeping the value of each one as written (whitespace collapsed)
 * @param {string} body Declarations
 * @return {Object<string, string>} Values by property name
 */
const parseDeclarations = body => body.split(";").reduce((declarations, declaration) => {
  const separator = declaration.indexOf(":");
  if (separator < 0) {
    return declarations;
  }
  const name = declaration.slice(0, separator).trim();
  return {...declarations, [name]: declaration.slice(separator + 1).trim().replace(/\s+/g, " ")};
}, {});

/**
 * Names of the --awe-* tokens declared by the rules
 * @param {{declarations: Object<string, string>}[]} rules Parsed rules
 * @return {string[]} Sorted unique names
 */
const tokenNames = rules => [...new Set(rules.flatMap(rule => Object.keys(rule.declarations)))]
  .filter(name => name.startsWith("--awe-"))
  .sort();

/**
 * Split a selector list at its top-level commas (not the ones inside :has(), :not()...)
 * @param {string} list Selector list
 * @return {string[]} Selectors
 */
const splitSelectors = list => {
  const selectors = [];
  let depth = 0;
  let start = 0;
  [...list].forEach((char, index) => {
    if (char === "(") {
      depth++;
    } else if (char === ")") {
      depth--;
    } else if (char === "," && depth === 0) {
      selectors.push(list.slice(start, index).trim());
      start = index + 1;
    }
  });
  selectors.push(list.slice(start).trim());
  return selectors;
};

module.exports = {parseRules, splitSelectors, tokenNames};
