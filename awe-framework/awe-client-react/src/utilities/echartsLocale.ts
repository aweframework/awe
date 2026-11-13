const LOCALE_NAMES = ["EN", "ES", "FR"];

/**
 * Name of the ECharts locale of a language
 * @param {string} language Language (es-ES, fr, en-GB...)
 * @returns {string} ES, FR or EN
 */
export function getEChartsLocale(language?: string | null): "ES" | "FR" | "EN" {
  const name = String(language || "").substring(0, 2).toUpperCase();
  return LOCALE_NAMES.includes(name) ? name as "ES" | "FR" : "EN";
}
