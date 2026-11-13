import {resolveIcon} from "awe-client-react/utilities/iconResolver";

// The Lucide sprite of the build (#885), relative to the base of the page (<base href> is the context path of the
// application), like the other static files of the client
export const ICON_SPRITE = "icons/lucide-sprite.svg";

// Font Awesome modifier classes written next to an icon -> classes of the AWE icon (fa-fw is dropped: the svg already
// has a fixed width)
const MODIFIERS = {"fa-spin": "awe-icon-spin", "fa-pulse": "awe-icon-spin", "fa-lg": "awe-icon-lg", "fa-fw": ""};
const NO_ICON = Object.freeze({family: null, name: null});

// Resolved icons by value: the templates ask for the icon on every digest
const icons = new Map();

const lucideClasses = classes => classes.split(/\s+/)
  .map(name => (name in MODIFIERS ? MODIFIERS[name] : name))
  .filter(Boolean)
  .join(" ");

// The family and name of an icon in its current font, as the templates have always received them: the family before
// a colon ("mdi:name"), Font Awesome by default, and the name with the extra classes after it
const currentIcon = icon => {
  const values = icon.split(":");
  return values.length === 2 ? {family: values[0], name: values[1]} : {family: "fa", name: values[0]};
};

const resolve = icon => {
  const resolved = resolveIcon(icon);
  if (!resolved) {
    return NO_ICON;
  }
  if (resolved.family === "lucide") {
    return {family: "lucide", name: resolved.name, classes: lucideClasses(resolved.classes), href: `${ICON_SPRITE}#${resolved.name}`};
  }
  return currentIcon(icon);
};

/**
 * Icon of the screen XML or the data for the templates
 * @param {string} icon Icon value ("name", "fa-<name>", "fa:name", "mdi:name", "lucide:name") and its extra classes
 * @returns {{family: string, name: string, classes?: string, href?: string}} The Lucide icon ({family: "lucide"}, its
 * extra classes and the sprite reference), or the family ("fa", "mdi"...) and name of the icon in its current font;
 * family and name null without icon or for an unknown Lucide name
 */
export function getIcon(icon) {
  if (!icon) {
    return NO_ICON;
  }
  if (!icons.has(icon)) {
    icons.set(icon, Object.freeze(resolve(String(icon))));
  }
  return icons.get(icon);
}

const attributesOf = (classes, attributes) => [classes ? `class="${classes}"` : "", attributes].filter(Boolean)
  .map(attribute => ` ${attribute}`).join("");

/**
 * Markup of an icon written in the client: the decorative svg of the sprite for a Lucide icon, the element of its
 * current font otherwise
 * @param {string} icon Icon value ("calendar", "refresh fa-spin"...)
 * @param {string} [attributes] Extra attributes of the svg (ng-if...)
 * @returns {string} Markup
 */
export function iconMarkup(icon, attributes = "") {
  const resolved = getIcon(icon);
  if (resolved.family !== "lucide") {
    return `<i${attributesOf(`fa fa-${resolved.name}`, attributes)}></i>`;
  }
  return `<svg${attributesOf(["awe-icon", resolved.classes].filter(Boolean).join(" "), `aria-hidden="true" focusable="false"${attributes ? " " + attributes : ""}`)}>`
    + `<use href="${resolved.href}"></use></svg>`;
}

/**
 * Markup of an icon written in the client, in the element that holds it: the element keeps its classes and attributes
 * (layout, behaviour and test hooks) and holds the svg of a Lucide icon; for an icon without Lucide equivalence it is
 * the current markup of its font
 * @param {string} icon Icon value ("calendar", "refresh fa-spin"...)
 * @param {{tag?: string, classes?: string, attributes?: string}} [options] Element, its classes and its attributes
 * @returns {string} Markup
 */
export function iconTemplate(icon, {tag = "i", classes = "", attributes = ""} = {}) {
  const resolved = getIcon(icon);
  if (resolved.family !== "lucide") {
    return `<${tag}${attributesOf([classes, `fa fa-${resolved.name}`].filter(Boolean).join(" "), attributes)}></${tag}>`;
  }
  return `<${tag}${attributesOf(classes, attributes)}>${iconMarkup(icon)}</${tag}>`;
}
