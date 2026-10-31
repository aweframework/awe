// Palettes of the AWE 5 design tokens (awe-design-tokens/tokens.css, --awe-palette-<name>-500): the primary and the
// neutral palettes of the themes. Same list as the React engine (utilities/color)
export const SWATCH_PALETTES = ["red", "orange", "amber", "yellow", "lime", "green", "emerald", "teal", "cyan", "sky",
  "blue", "indigo", "violet", "purple", "fuchsia", "pink", "rose", "slate", "gray", "zinc", "neutral", "stone"];

const HEX = /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i;

/**
 * Colour of a swatch (#793): a known palette name (trimmed, any case) as its 500 shade of the design tokens, or a #RGB /
 * #RRGGBB hex as it is. Any other value gives no colour, so only these values ever reach the style of the swatch.
 *
 * @param {*} value Palette name or hex
 * @returns {String} CSS colour, empty when the value is not a known palette or a hex
 */
export const getSwatchColor = (value) => {
  if (typeof value !== "string") {
    return "";
  }
  const color = value.trim();
  if (HEX.test(color)) {
    return color;
  }
  const palette = color.toLowerCase();
  return SWATCH_PALETTES.includes(palette) ? `var(--awe-palette-${palette}-500)` : "";
};
