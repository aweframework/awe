// The chart texts of the React client (number separators, months, no data, drilldown back control)
import chartsEN from "awe-client-react/i18n/charts/charts-en.json";
import chartsES from "awe-client-react/i18n/charts/charts-es.json";
import chartsFR from "awe-client-react/i18n/charts/charts-fr.json";

// Basque is an AngularJS language only
const chartsEU = {
  noData: "Ez daude daturik erakusteko",
  decimalPoint: ",",
  thousandsSep: ".",
  drillUpText: "{series.name} -ra itzuli",
  months: ["Urtarrila", "Otsaila", "Martxoa", "Apirila", "Maiatza", "Ekaina", "Uztaila", "Abuztua", "Iraila", "Urria",
    "Azaroa", "Abendua"],
  shortMonths: ["Urt", "Ots", "Mar", "Api", "Mai", "Eka", "Uzt", "Abu", "Ira", "Urr", "Aza", "Abe"],
  weekdays: ["Igandea", "Astelehena", "Asteartea", "Asteazkena", "Osteguna", "Ostirala", "Larunbata"]
};

// Message shown instead of the series when they have more points than the `limitPointsSerie` setting
const TOO_MUCH_DATA = {
  en: "Too much data to display",
  es: "Demasiados datos a mostrar",
  eu: "Erakusteko datu gehiegi",
  fr: "Trop de données pour montrer"
};

const TEXTS = {en: chartsEN, es: chartsES, eu: chartsEU, fr: chartsFR};
const DEFAULT_LANGUAGE = "en";

/**
 * Retrieve the chart texts of a language
 * @param {string} language Language of the application (es-ES, en-GB, eu-ES, fr-FR)
 * @returns {object} Chart texts: noData, tooMuchData, decimalPoint, thousandsSep, drillUpText, months...
 */
export function getChartTexts(language) {
  const code = String(language || "").substring(0, 2).toLowerCase();
  const name = TEXTS[code] ? code : DEFAULT_LANGUAGE;
  return {...TEXTS[name], tooMuchData: TOO_MUCH_DATA[name]};
}
