/**
 * Resolve the pivot locale of an AWE language. The pivot language bundles register the primary language subtag
 * as their key, while the AWE language is a full tag, and pivotUI fails when the locale it receives is not registered
 * @param {String} language AWE language
 * @param {Object} locales Registered pivot locales ($.pivotUtilities.locales)
 * @return {String} Registered pivot locale
 */
export function resolvePivotLocale(language, locales) {
  const primary = language ? language.split("-")[0] : null;
  return [language, primary, "en"].find((locale) => locale && locale in locales) || Object.keys(locales)[0] || "en";
}
