/**
 * Suggestion of a suggest: the server answers a value and a label, whose types depend on the column of the query
 */
export interface Suggestion {
  value?: unknown;
  label?: unknown;
  [key: string]: unknown;
}

/**
 * Suggestion whose label is text (the original label, whatever its type, is replaced)
 */
export type TextSuggestion<T extends Suggestion = Suggestion> = Omit<T, "label"> & {label: string};

/**
 * Result of converting one suggestion: only a suggestion with a label gets a text label, any other is kept unchanged
 */
export type WithTextLabel<T extends Suggestion> = T extends {label: unknown} ? TextSuggestion<T> : T;

/**
 * Get the label of a suggestion as text.
 * The server answers the labels of a suggest over a numeric column (an identifier, a counter...) as numbers, and
 * PrimeReact AutoComplete requires the field it shows to be a string (it lowercases it on blur)
 * @param {object} item Suggestion ({value, label})
 * @returns {string} Label as text (the value if the suggestion has no label)
 */
export const getSuggestionLabel = (item?: Suggestion | null): string => {
  const label = item?.label ?? item?.value;
  return label === undefined || label === null ? "" : String(label);
};

/**
 * Convert the labels of the suggestions to text. The values are kept untouched
 * @param {object[]} items Suggestions
 * @returns {object[]} Suggestions with a text label
 */
export const withTextLabels = <T extends Suggestion>(items?: T[] | null): TextSuggestion<T>[] =>
  (items || []).map(item => ({...item, label: getSuggestionLabel(item)}));

/**
 * Convert the label of the value of an autocomplete (a suggestion, a list of suggestions or the typed text) to text
 * @param {object|object[]|string} value Value of the autocomplete
 * @returns {object|object[]|string} Value with a text label
 */
export function withTextLabel<T extends Suggestion>(value: T[]): TextSuggestion<T>[];
export function withTextLabel<T extends Suggestion>(value: T): WithTextLabel<T>;
export function withTextLabel<T extends string | null | undefined>(value: T): T;
export function withTextLabel(value: Suggestion | Suggestion[] | string | null | undefined) {
  if (Array.isArray(value)) {
    return withTextLabels(value);
  }
  return value && typeof value === "object" && "label" in value ? {...value, label: getSuggestionLabel(value)} : value;
}

/**
 * Group of options of a suggest: the locale key of the group, its header (translated) and its options
 */
export interface SuggestionGroup<T extends Suggestion = Suggestion> {
  group: string;
  label: string;
  items: T[];
}

/**
 * Group the suggestions by their group (a locale key), in the order of the first suggestion of each group. A suggestion
 * without a group in a grouped list goes to a group without header
 * @param {object[]} items Suggestions
 * @param {function} translate Translation of the locale key of a group
 * @returns {object[]|null} Groups of suggestions, or null when no suggestion has a group (the list is shown as always)
 */
export const groupSuggestions = <T extends Suggestion>(
  items: T[],
  translate: (key: string) => string
): SuggestionGroup<T>[] | null => {
  if (!items.some(item => item.group)) {
    return null;
  }
  const groups = new Map<string, SuggestionGroup<T>>();
  items.forEach(item => {
    const group = item.group ? String(item.group) : "";
    const current = groups.get(group) ?? {group, label: group ? translate(group) : "", items: []};
    current.items.push(item);
    groups.set(group, current);
  });
  return [...groups.values()];
};

// Height (px) of an option of a long suggest list: the virtual scroller gives every option this fixed height, so it is
// the height of an option of the AWE theme (padding + line height) in the density of <html> (#885)
const VIRTUAL_ITEM_SIZE: Record<string, number> = {comfortable: 38, compact: 32};

/**
 * Height of an option of a long (virtual) suggest list in the current density
 * @returns Option height in px
 */
export const getVirtualItemSize = (): number => VIRTUAL_ITEM_SIZE[document.documentElement.dataset.aweDensity ?? ""]
  ?? VIRTUAL_ITEM_SIZE.comfortable;
