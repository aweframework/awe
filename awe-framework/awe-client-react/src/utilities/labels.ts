/**
 * Translate label
 * @param label Label to translate
 * @param t Translate function
 * @param args Arguments to replace in the translated string
 * @return Label translated
 */
export function translateLabel(
  label: unknown = "",
  t: (key: string) => string = (l) => l,
  ...args: Array<string | number>
): string {
  let translated = (String(label) || "")
    .split(" ") // Split from spaces
    .filter(w => w !== "") // Remove blank words
    .map(w => t(w)) // Translate
    .join(" "); // Join with spaces

  // Replace placeholders {0}, {1}, etc. with args
  args.forEach((arg, index) => {
    translated = translated.replace(new RegExp(`\\{${index}\\}`, 'g'), String(arg));
  });

  return translated;
}
