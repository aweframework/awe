/**
 * AWE 5 theme appearance on the <html> element.
 *
 * The server resolves the appearance of the theme into the settings themePrimary, themeSurface, themeMode, density and
 * themePalette. It is rendered as the design system prototype does: the class `light` or `dark` (none for the system
 * mode), data-awe-primary, data-awe-surface (absent for the auto surface) and data-awe-density. A primary or a surface
 * given as a hex is rendered as `custom`, and the palette generated from it as the --awe-primary-* or --awe-surface-*
 * inline custom properties of <html>.
 *
 * The AngularJS engine imports this module too, so both engines render the same attributes.
 */
export const THEME_APPEARANCE_FIELDS = ["themePrimary", "themeSurface", "themeMode", "density", "themePalette"];

const MODES = ["light", "dark"];
const HEX = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;
const PALETTE_PROPERTY = /^--awe-(primary|surface)-[a-z0-9-]+$/;

const paletteAttribute = (value) => HEX.test(value) ? "custom" : value;

function setAttribute(root, name, value) {
  if (value) {
    root.setAttribute(name, value);
  } else {
    root.removeAttribute(name);
  }
}

function applyPalette(root, palette) {
  // The generated properties of the previous appearance are removed first, as the prototype does
  Array.from(root.style)
    .filter((name) => PALETTE_PROPERTY.test(name))
    .forEach((name) => root.style.removeProperty(name));
  Object.entries(palette || {})
    .filter(([name]) => PALETTE_PROPERTY.test(name))
    .forEach(([name, value]) => root.style.setProperty(name, value));
  if (!root.getAttribute("style")) {
    root.removeAttribute("style");
  }
}

/**
 * Render the theme appearance of the settings on <html>
 * @param {object} settings Settings (themePrimary, themeSurface, themeMode, density, themePalette)
 * @param {HTMLElement} root Element to render on (<html>)
 */
export function applyThemeAppearance(settings = {}, root = document.documentElement) {
  const {themePrimary, themeSurface, themeMode, density, themePalette} = settings;
  MODES.forEach((mode) => root.classList.toggle(mode, mode === themeMode));
  setAttribute(root, "data-awe-primary", themePrimary && paletteAttribute(themePrimary));
  setAttribute(root, "data-awe-surface", themeSurface && themeSurface !== "auto" && paletteAttribute(themeSurface));
  setAttribute(root, "data-awe-density", density);
  applyPalette(root, themePalette);
}

/**
 * Retrieve the appearance fields that the settings carry
 * @param {object} settings Settings
 * @returns {object} Appearance fields
 */
export function pickThemeAppearance(settings = {}) {
  return Object.fromEntries(THEME_APPEARANCE_FIELDS
    .filter((field) => settings[field] !== undefined)
    .map((field) => [field, settings[field]]));
}

/**
 * Call back when any of the mode classes of an element is added or removed
 * @param {HTMLElement} element Observed element
 * @param {string[]} classes Mode classes
 * @param {function} onChange Callback
 * @returns {function} Stop observing
 */
export function observeModeClasses(element, classes, onChange) {
  if (typeof MutationObserver === "undefined") {
    return () => undefined;
  }
  const modeClasses = () => classes.filter((cssClass) => element.classList.contains(cssClass)).join(" ");
  let previous = modeClasses();
  const observer = new MutationObserver(() => {
    const current = modeClasses();
    if (current !== previous) {
      previous = current;
      onChange();
    }
  });
  observer.observe(element, {attributes: true, attributeFilter: ["class"]});
  return () => observer.disconnect();
}
