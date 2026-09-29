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
export function toColor(value) {
  return value ? "#" + fromColor(value) : "";
}

/**
 * Strip hexadecimal character from color
 * @param value Value
 * @returns {string} Color without hexadecimal character
 * @memberOf Colors
 */
export function fromColor(value) {
  return (value || "").replaceAll("#", "");
}