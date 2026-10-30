/**
 * HTML sanitizer: a port of the one Bootstrap 3.4.1 applied to the templates of its popovers (AWE no longer ships
 * Bootstrap). It keeps the elements of an allow list, removes the others with their content, and keeps only the
 * attributes of the allow list (an URL attribute must also be safe).
 */

// Attributes which hold an URL
const URI_ATTRIBUTES = new Set(["background", "cite", "href", "itemtype", "longdesc", "poster", "src", "xlink:href"]);

const ARIA_ATTRIBUTE_PATTERN = /^aria-[\w-]*$/i;

/**
 * Allow list of elements and their attributes ("*" lists the attributes allowed in every element)
 */
export const DEFAULT_ALLOW_LIST = Object.freeze({
  "*": ["class", "dir", "id", "lang", "role", ARIA_ATTRIBUTE_PATTERN],
  a: ["target", "href", "title", "rel"],
  area: [],
  b: [],
  br: [],
  col: [],
  code: [],
  div: [],
  em: [],
  hr: [],
  h1: [],
  h2: [],
  h3: [],
  h4: [],
  h5: [],
  h6: [],
  i: [],
  img: ["src", "alt", "title", "width", "height"],
  li: [],
  ol: [],
  p: [],
  pre: [],
  s: [],
  small: [],
  span: [],
  sub: [],
  sup: [],
  strong: [],
  u: [],
  ul: []
});

// URLs which are safe: web, mail, ftp, telephone and file schemes, or relative
const SAFE_URL_PATTERN = /^(?:(?:https?|mailto|ftp|tel|file):|[^&:/?#]*(?:[/?#]|$))/i;

// Data URLs which are safe: only images, videos and audios
const DATA_URL_PATTERN = /^data:(?:image\/(?:bmp|gif|jpeg|jpg|png|tiff|webp)|video\/(?:mpeg|mp4|ogg|webm)|audio\/(?:mp3|oga|ogg|opus));base64,[a-z0-9+/]+=*$/i;

/**
 * Check if an attribute is allowed
 * @param {Attr} attribute Attribute
 * @param {Array<string|RegExp>} allowedList Allowed attribute names (or patterns)
 * @returns {boolean} The attribute is allowed
 */
const allowedAttribute = (attribute, allowedList) => {
  const name = attribute.nodeName.toLowerCase();

  if (allowedList.includes(name)) {
    if (URI_ATTRIBUTES.has(name)) {
      return SAFE_URL_PATTERN.test(attribute.nodeValue) || DATA_URL_PATTERN.test(attribute.nodeValue);
    }
    return true;
  }

  return allowedList.some(allowed => allowed instanceof RegExp && allowed.test(name));
};

/**
 * Sanitize an HTML text
 * @param {string} unsafeHtml HTML to sanitize
 * @param {object} [allowList] Allowed elements and attributes
 * @returns {string} Safe HTML
 */
export function sanitizeHtml(unsafeHtml, allowList = DEFAULT_ALLOW_LIST) {
  if (!unsafeHtml) {
    return unsafeHtml;
  }

  // The document is inert: nothing runs (scripts, image handlers) while the HTML is parsed
  const createdDocument = document.implementation.createHTMLDocument("sanitization");
  createdDocument.body.innerHTML = unsafeHtml;

  const allowedElements = Object.keys(allowList);
  Array.from(createdDocument.body.querySelectorAll("*")).forEach(element => {
    const name = element.nodeName.toLowerCase();

    if (!allowedElements.includes(name)) {
      element.remove();
      return;
    }

    const allowedAttributes = [].concat(allowList["*"] || [], allowList[name] || []);
    Array.from(element.attributes).forEach(attribute => {
      if (!allowedAttribute(attribute, allowedAttributes)) {
        element.removeAttribute(attribute.nodeName);
      }
    });
  });

  return createdDocument.body.innerHTML;
}
