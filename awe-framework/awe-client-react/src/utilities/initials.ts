/**
 * Initials of a name, as the avatar of the prototype: the first letter of its first and its last word, leaving out the
 * words that do not start with a letter ("Pat de la Vega" is "PV", "Manager (test)" is "M")
 * @param {string} name Name
 * @returns {string} Initials, in upper case
 */
export function getInitials(name?: unknown): string {
  const words = String(name || "").split(/\s+/).filter(word => /^\p{L}/u.test(word));
  const initials = words.length > 1 ? [words[0], words[words.length - 1]] : words;
  return initials.map(word => word.charAt(0)).join("").toUpperCase();
}
