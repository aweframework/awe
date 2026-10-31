/**
 * Color utility functions
 * @category Utilities
 * @namespace Colors
 */

/**
 * Return value as hexadecimal color
 * @param value Value
 * @returns {string} Color
 * @memberOf Colors
 */
export function toColor(value?: string | null): string {
  return value ? "#" + fromColor(value) : "";
}

/**
 * Strip hexadecimal character from color
 * @param value Value
 * @returns {string} Color without hexadecimal character
 * @memberOf Colors
 */
export function fromColor(value?: string | null): string {
  return (value || "").replace(/#/g, "");
}

/**
 * Palettes of the AWE 5 design tokens (awe-design-tokens/tokens.css, --awe-palette-<name>-500): the primary and the
 * neutral palettes of the themes. Same list as the AngularJS engine (services/grid/swatch)
 * @memberOf Colors
 */
export const SWATCH_PALETTES: string[] = ["red", "orange", "amber", "yellow", "lime", "green", "emerald", "teal", "cyan",
  "sky", "blue", "indigo", "violet", "purple", "fuchsia", "pink", "rose", "slate", "gray", "zinc", "neutral", "stone"];

const HEX = /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i;

/**
 * Colour of a swatch (#793): a known palette name (trimmed, any case) as its 500 shade of the design tokens, or a #RGB /
 * #RRGGBB hex as it is. Any other value gives no colour, so only these values ever reach the style of the swatch
 * @param value Palette name or hex
 * @returns {string} CSS colour, empty when the value is not a known palette or a hex
 * @memberOf Colors
 */
export function getSwatchColor(value?: unknown): string {
  if (typeof value !== "string") {
    return "";
  }
  const color = value.trim();
  if (HEX.test(color)) {
    return color;
  }
  const palette = color.toLowerCase();
  return SWATCH_PALETTES.includes(palette) ? `var(--awe-palette-${palette}-500)` : "";
}
