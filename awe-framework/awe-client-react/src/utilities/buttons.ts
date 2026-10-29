/**
 * Styles of the buttons shared with the AngularJS engine (#793): the btn-* styles of the screen XML and the icon-only
 * buttons (style="no-label")
 */

// PrimeReact variant of every btn-* style the AngularJS engine draws (directives/button.less): btn-primary is the filled
// primary button (no variant), the others a filled button of their colour, btn-ghost the button without background
const BUTTON_VARIANTS: Record<string, string> = {
  "btn-primary": "",
  "btn-danger": "p-button-danger",
  "btn-success": "p-button-success",
  "btn-warning": "p-button-warning",
  "btn-dark": "p-button-contrast",
  "btn-ghost": "p-button-text"
};

/**
 * Class names of a style (a space separated list)
 * @param style Style of the button
 * @returns Class names
 */
const classesOf = (style: unknown): string[] => typeof style === "string" ? style.split(/\s+/).filter(Boolean) : [];

/**
 * PrimeReact variant of a button: the one of the first btn-* style of the button, else the variant of its type
 * @param style Style of the button (style attribute of the screen XML)
 * @param fallback Variant without a btn-* style
 * @returns Variant class names ("" for the filled primary button)
 */
export function getButtonVariant(style: unknown, fallback: string): string {
  const name = classesOf(style).find(item => item in BUTTON_VARIANTS);
  return name === undefined ? fallback : BUTTON_VARIANTS[name];
}

/**
 * Check if a button shows only its icon (style no-label, as for criteria): its label is its tooltip and accessible name
 * @param style Style of the button
 * @returns The button is icon-only
 */
export function isIconOnly(style: unknown): boolean {
  return classesOf(style).includes("no-label");
}
