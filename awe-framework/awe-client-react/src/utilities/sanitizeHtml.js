import DOMPurify from "dompurify";
import parse from "html-react-parser";

/**
 * Tags that a value coming from data may keep. They are the inline and basic block tags that the transforms of the
 * server generate (TEXT_HTML, MARKDOWN_HTML) and that a label may use to format a text. Nothing that loads resources
 * (img, iframe, object, svg, link, style) or that runs code (script) is in the list.
 * @type {string[]}
 */
export const ALLOWED_TAGS = [
  "a", "abbr", "b", "blockquote", "br", "code", "del", "em", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "i", "ins",
  "li", "mark", "ol", "p", "pre", "s", "small", "span", "strong", "sub", "sup", "u", "ul"
];

/**
 * Attributes that a value coming from data may keep. There are no event handlers, no inline styles and no data
 * attributes. The urls of `href` are filtered by DOMPurify, which drops `javascript:` and other script schemes
 * @type {string[]}
 */
export const ALLOWED_ATTRIBUTES = ["class", "href", "target", "title"];

const SANITIZE_OPTIONS = {
  ALLOWED_TAGS,
  ALLOWED_ATTR: ALLOWED_ATTRIBUTES,
  ALLOW_DATA_ATTR: false,
  ALLOW_ARIA_ATTR: false
};

// A dedicated instance, so that the hook below does not change the DOMPurify that other code of the page may use, and
// loading this module again does not add the hook twice to a shared one
const purifier = DOMPurify(window);

// A link that opens a new window must not give access to this one
purifier.addHook("afterSanitizeAttributes", (node) => {
  if (node.hasAttribute?.("target")) {
    node.setAttribute("rel", "noopener noreferrer");
  }
});

/**
 * Sanitize the html of a value with a strict allow-list of tags and attributes
 * @param {*} value Value that may contain html
 * @returns {string} Html that is safe to put in the DOM
 */
export function sanitizeHtml(value) {
  return purifier.sanitize(String(value ?? ""), SANITIZE_OPTIONS);
}

/**
 * Sanitize the html of a value and convert it to React nodes. Every place of the client that renders data as html must
 * use this function instead of parsing the value directly
 * @param {*} value Value that may contain html
 * @returns {React.ReactNode} React nodes
 */
export function renderSafeHtml(value) {
  return parse(sanitizeHtml(value));
}
