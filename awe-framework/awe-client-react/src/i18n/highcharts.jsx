import "./highcharts/highcharts-lang.jsx";

/**
 * Retrieve highcharts locales
 * @param language Language to retrieve
 * @returns {*}
 */
export function getHighchartsLocale(language) {
  if (language in global.HighchartsLocale) {
    return global.HighchartsLocale[language];
  } else {
    return global.HighchartsLocale['en'];
  }
}
