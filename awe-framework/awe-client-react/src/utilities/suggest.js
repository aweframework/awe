/**
 * Get the label of a suggestion as text.
 * The server answers the labels of a suggest over a numeric column (an identifier, a counter...) as numbers, and
 * PrimeReact AutoComplete requires the field it shows to be a string (it lowercases it on blur)
 * @param {object} item Suggestion ({value, label})
 * @returns {string} Label as text (the value if the suggestion has no label)
 */
export const getSuggestionLabel = item => {
  const label = item?.label ?? item?.value;
  return label === undefined || label === null ? "" : String(label);
};

/**
 * Convert the labels of the suggestions to text. The values are kept untouched
 * @param {object[]} items Suggestions
 * @returns {object[]} Suggestions with a text label
 */
export const withTextLabels = items => (items || []).map(item => ({...item, label: getSuggestionLabel(item)}));

/**
 * Convert the label of the value of an autocomplete (a suggestion, a list of suggestions or the typed text) to text
 * @param {object|object[]|string} value Value of the autocomplete
 * @returns {object|object[]|string} Value with a text label
 */
export const withTextLabel = value => {
  if (Array.isArray(value)) {
    return withTextLabels(value);
  }
  return value && typeof value === "object" && "label" in value ? {...value, label: getSuggestionLabel(value)} : value;
};
