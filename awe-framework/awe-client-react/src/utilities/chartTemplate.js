/**
 * Parser and evaluator of the Highcharts 11 template language, which the charts use for labels, tooltips and axes.
 *
 * ```
 * Template    := (text | Expression | Condition)*
 * Expression  := "{" Body [":" specification] "}"
 * Condition   := "{#if " Body "}" Template ["{else}" Template] "{/if}"
 * Body        := Argument+            the first one is a helper when there is more than one: multiply value 0.001
 * Argument    := number | path | "(" Body ")"
 * ```
 *
 * The helpers are `gt`, `lt`, `ge`, `le`, `eq`, `ne`, `multiply`, `divide`, `add` and `subtract`. A path reads the
 * context (`point.y`, `series.userOptions.fullname`). What is not a valid tag is kept as text.
 *
 * The parser reads the template once, from left to right, and looks for each closing brace after the opening one
 * only, so its time is linear. It does not use regular expressions on the template, only on single words.
 */

const NUMBER = /^-?\d+(?:\.\d+)?$/;
const PATH = /^[\w.$-]+$/;
const IF_PREFIX = "#if ";
const MAX_DEPTH = 20;

const toNumber = (value) => value === null || value === undefined || value === "" ? Number.NaN : Number(value);
const same = (left, right) => left === right || (left != null && right != null && String(left) === String(right));

/**
 * Helpers of the language
 * @type {Object<string, function(...*): *>}
 */
const HELPERS = {
  gt: (left, right) => toNumber(left) > toNumber(right),
  lt: (left, right) => toNumber(left) < toNumber(right),
  ge: (left, right) => toNumber(left) >= toNumber(right),
  le: (left, right) => toNumber(left) <= toNumber(right),
  eq: same,
  ne: (left, right) => !same(left, right),
  multiply: (left, right) => toNumber(left) * toNumber(right),
  divide: (left, right) => toNumber(left) / toNumber(right),
  add: (left, right) => toNumber(left) + toNumber(right),
  subtract: (left, right) => toNumber(left) - toNumber(right)
};

/**
 * Resolve a dotted path in a context
 * @param {object} context Context
 * @param {string} path Path (point.y)
 * @returns {*} Value, undefined when the path does not exist
 */
export function resolvePath(context, path) {
  return String(path).split(".").reduce((value, key) => value?.[key], context);
}

/**
 * Split the body of a tag into words and parentheses
 * @param {string} text Body
 * @returns {string[]} Tokens
 */
function tokenize(text) {
  const tokens = [];
  let word = "";
  const flush = () => {
    if (word) {
      tokens.push(word);
      word = "";
    }
  };
  for (const character of text) {
    if (character === "(" || character === ")") {
      flush();
      tokens.push(character);
    } else if (character.trim() === "") {
      flush();
    } else {
      word += character;
    }
  }
  flush();
  return tokens;
}

/**
 * Parse a word as a number or a path
 * @param {string} token Word
 * @returns {object|null} Expression, null when the word is neither
 */
function parseAtom(token) {
  if (NUMBER.test(token)) {
    return {type: "number", value: Number(token)};
  }
  return PATH.test(token) ? {type: "path", path: token} : null;
}

/**
 * Parse an argument: a number, a path, or a body between parentheses
 * @param {{tokens: string[], position: number}} cursor Tokens, and the position of the next one
 * @param {number} depth Depth of the parentheses
 * @returns {object|null} Expression, null when the argument is not valid
 */
function parseArgument(cursor, depth) {
  const token = cursor.tokens[cursor.position];
  cursor.position += 1;
  if (token === undefined || token === ")") {
    return null;
  }
  if (token !== "(") {
    return parseAtom(token);
  }
  const inner = parseSequence(cursor, depth + 1);
  if (!inner || cursor.tokens[cursor.position] !== ")") {
    return null;
  }
  cursor.position += 1;
  return inner;
}

/**
 * Parse a sequence of arguments: one argument, or a helper followed by its arguments
 * @param {{tokens: string[], position: number}} cursor Tokens, and the position of the next one
 * @param {number} depth Depth of the parentheses
 * @returns {object|null} Expression, null when the sequence is not valid
 */
function parseSequence(cursor, depth) {
  if (depth > MAX_DEPTH) {
    return null;
  }
  const items = [];
  while (cursor.position < cursor.tokens.length && cursor.tokens[cursor.position] !== ")") {
    const item = parseArgument(cursor, depth);
    if (!item) {
      return null;
    }
    items.push(item);
  }
  if (items.length <= 1) {
    return items[0] ?? null;
  }
  const [helper, ...args] = items;
  // Words that do not start with a helper are text between braces, not a call
  return helper.type === "path" && Object.hasOwn(HELPERS, helper.path) ? {type: "call", helper: helper.path, args} : null;
}

/**
 * Parse the tokens of an expression body
 * @param {string[]} tokens Tokens
 * @returns {object|null} Expression, null when the body is not valid
 */
function parseTokens(tokens) {
  const cursor = {tokens, position: 0};
  const expression = parseSequence(cursor, 0);
  return cursor.position === tokens.length ? expression : null;
}

/**
 * Parse the body of an expression or of a condition
 * @param {string} body Body
 * @returns {object|null} Expression, null when the body is not valid
 */
function parseBody(body) {
  return parseTokens(tokenize(body));
}

/**
 * Parse the content of a tag, between the braces
 * @param {string} content Content
 * @returns {object|null} Tag, null when the content is not a tag of the language
 */
function parseTag(content) {
  const text = content.trim();
  if (text === "else" || text === "/if") {
    return {kind: text === "else" ? "else" : "end"};
  }
  if (text.startsWith(IF_PREFIX)) {
    const condition = parseBody(text.substring(IF_PREFIX.length));
    return condition ? {kind: "if", condition} : null;
  }
  // The specification is everything after the first colon (a date pattern may have more: %H:%M)
  const colon = content.indexOf(":");
  const expression = parseBody(colon < 0 ? content : content.substring(0, colon));
  const spec = colon < 0 ? undefined : content.substring(colon + 1);
  return expression ? {kind: "expression", expression, spec} : null;
}

/**
 * Add the text before a tag to the node that is open
 * @param {object} state State of the parser
 * @param {number} end Position where the text ends
 */
function flushText(state, end) {
  if (end > state.textStart) {
    state.target.push({type: "text", value: state.text.substring(state.textStart, end)});
  }
}

/**
 * Check that a tag fits where it appears: an else needs an open condition without else, an end needs an open condition
 * @param {object|null} tag Tag
 * @param {{inElse: boolean}|undefined} frame Condition that is open
 * @returns {boolean} The tag is part of the template
 */
function isAccepted(tag, frame) {
  if (!tag) {
    return false;
  }
  if (tag.kind === "else") {
    return Boolean(frame) && !frame.inElse;
  }
  return tag.kind !== "end" || Boolean(frame);
}

/**
 * Add a tag to the template that is being parsed
 * @param {object} state State of the parser
 * @param {object} tag Tag
 * @param {number} start Position of the opening brace of the tag
 */
function applyTag(state, tag, start) {
  flushText(state, start);
  const frame = state.open.at(-1);
  switch (tag.kind) {
    case "expression":
      state.target.push({type: "expression", expression: tag.expression, spec: tag.spec});
      break;
    case "if": {
      const node = {type: "if", condition: tag.condition, whenTrue: [], whenFalse: []};
      state.target.push(node);
      state.open.push({node, parent: state.target, inElse: false});
      state.target = node.whenTrue;
      break;
    }
    case "else":
      frame.inElse = true;
      state.target = frame.node.whenFalse;
      break;
    default:
      state.target = state.open.pop().parent;
  }
}

/**
 * Parse a template into text, expression and condition nodes
 * @param {string} template Template
 * @returns {object[]} Nodes
 */
export function parseTemplate(template) {
  const text = template === null || template === undefined ? "" : String(template);
  const state = {text, root: [], open: [], textStart: 0};
  state.target = state.root;
  let position = 0;

  while (position < text.length) {
    const start = text.indexOf("{", position);
    const end = start < 0 ? -1 : text.indexOf("}", start);
    if (end < 0) {
      break;
    }
    // The last opening brace before the closing one is the start of the tag
    const innerStart = text.lastIndexOf("{", end);
    if (innerStart > start) {
      position = innerStart;
      continue;
    }
    const tag = parseTag(text.substring(start + 1, end));
    if (!isAccepted(tag, state.open.at(-1))) {
      position = start + 1;
      continue;
    }
    applyTag(state, tag, start);
    position = end + 1;
    state.textStart = position;
  }
  flushText(state, text.length);
  return state.root;
}

/**
 * Evaluate an expression
 * @param {object} expression Expression of a parsed template
 * @param {object} context Values that the paths refer to
 * @returns {*} Value, undefined when a path or a helper does not exist
 */
export function evaluate(expression, context) {
  if (expression.type === "number") {
    return expression.value;
  }
  if (expression.type === "path") {
    return resolvePath(context, expression.path);
  }
  return HELPERS[expression.helper](...expression.args.map(argument => evaluate(argument, context)));
}
