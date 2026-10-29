/**
 * Initials of a name, as the avatar of the prototype: the first letter of its first and its last word, leaving out the
 * words that do not start with a letter ("Pat de la Vega" is "PV", "Manager (test)" is "M"). Same rules as getInitials of
 * the React engine (AweAvatar).
 * @param {string} name Name
 * @returns {string} Initials, in upper case
 */
export function getInitials(name) {
  const words = String(name || "").split(/\s+/).filter(word => /^\p{L}/u.test(word));
  const initials = words.length > 1 ? [words[0], words[words.length - 1]] : words;
  return initials.map(word => word.charAt(0)).join("").toUpperCase();
}

// Tones of a status cell (any other tone is shown as neutral)
export const STATUS_TONES = ["success", "warning", "danger", "info", "neutral"];

// Tones of an avatar (any other tone is shown as neutral)
export const AVATAR_TONES = ["primary", ...STATUS_TONES];

/**
 * Tone of a value: one of the given tones (trimmed, in lower case), neutral for any other value
 *
 * @param {*} value Tone value
 * @param {String[]} tones Known tones
 * @returns {String} Tone
 */
export const toTone = (value, tones) => {
  const tone = value == null ? "" : String(value).trim().toLowerCase();
  return tones.includes(tone) ? tone : "neutral";
};
