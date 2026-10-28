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

/**
 * Attributes and validation rules of a criterion, as far as its required state is read
 */
interface CriterionRequirement {
  required?: boolean;
  label?: unknown;
  placeholder?: unknown;
  style?: string;
}

/**
 * Check if a criterion is required, by its attribute or by its validation rules
 * @param attributes Criterion attributes
 * @param validationRules Criterion validation rules
 * @return The criterion is required
 */
export function isCriterionRequired(attributes?: CriterionRequirement, validationRules?: { required?: boolean }): boolean {
  return !!(attributes?.required || validationRules?.required);
}

/**
 * Placeholder of a criterion. A required criterion without a visible label has no asterisk to show its state, so the
 * placeholder carries it (as the editors of the grid columns do).
 * @param attributes Criterion attributes
 * @param validationRules Criterion validation rules
 * @param t Translate function
 * @param withAsterisk Add the asterisk (false when the control draws its own, as the select placeholder does)
 * @return Placeholder translated
 */
export function criterionPlaceholder(
  attributes: CriterionRequirement | undefined,
  validationRules: { required?: boolean } | undefined,
  t: (key: string) => string,
  withAsterisk = true
): string {
  const placeholder = translateLabel(attributes?.placeholder, t);
  const hasLabel = !!attributes?.label && !attributes?.style?.includes("no-label");
  return withAsterisk && placeholder && !hasLabel && isCriterionRequired(attributes, validationRules)
    ? `${placeholder} *`
    : placeholder;
}

/**
 * Accessibility attribute of the focusable element of a required criterion
 * @param attributes Criterion attributes
 * @param validationRules Criterion validation rules
 * @return Props to spread on the element
 */
export function requiredAria(
  attributes?: CriterionRequirement,
  validationRules?: { required?: boolean }
): { "aria-required"?: "true" } {
  return isCriterionRequired(attributes, validationRules) ? { "aria-required": "true" } : {};
}
