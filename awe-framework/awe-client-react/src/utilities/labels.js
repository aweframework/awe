/**
 * Translate label
 * @param label Label to translate
 * @param {function} t translate function
 * @param {...any} args Arguments to replace in the translated string
 * @return {string} Label translated
 */
export function translateLabel(label = "", t = (l) => l, ...args) {
  let translated = (String(label) || "")
    .split(" ") // Split from spaces
    .filter(w => w !== "") // Remove blank words
    .map(w => t(w)) // Translate
    .join(" "); // Join with spaces

  // Replace placeholders {0}, {1}, etc. with args
  args.forEach((arg, index) => {
    translated = translated.replace(new RegExp(`\\{${index}\\}`, 'g'), arg);
  });

  return translated;
}
