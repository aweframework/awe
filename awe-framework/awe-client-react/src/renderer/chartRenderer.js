import {buildEChartsOption} from "../utilities/echartsOption";
import {renderSvg} from "../utilities/echartsSetup";
import chartsES from "../i18n/charts/charts-es.json";
import chartsFR from "../i18n/charts/charts-fr.json";
import chartsEN from "../i18n/charts/charts-en.json";
import datesES from "../i18n/dates/dates-es.json";
import datesFR from "../i18n/dates/dates-fr.json";

/**
 * Server render of the AWE charts: draws the `echartsModel` of a chart, with its data, to an SVG image using the
 * server-side rendering of Apache ECharts (`ssr: true`, `renderToSVGString()`), without a browser or a DOM.
 *
 * The chart option is built with the same code that draws the chart in the React client (`buildEChartsOption`), so the
 * image of the server is the chart of the screen.
 */

/**
 * Size of the image when the request does not give it (the default size of the Highcharts export server)
 * @type {{width: number, height: number}}
 */
export const DEFAULT_SIZE = Object.freeze({width: 600, height: 400});

/**
 * Texts of the charts by language (the same ones the React client registers in i18n.js)
 * @type {Object<string, object>}
 */
const LOCALES = {
  ES: {...datesES, ...chartsES},
  FR: {...datesFR, ...chartsFR},
  EN: {...chartsEN}
};

/**
 * Texts of the charts of a language, English when the language has none
 * @param {string} [language] Language (es-ES, fr, en-GB...)
 * @returns {object} Chart locale
 */
function localeOf(language) {
  return LOCALES[String(language || "").substring(0, 2).toUpperCase()] || LOCALES.EN;
}

/**
 * Render request of a chart
 * @typedef {object} ChartRenderRequest
 * @property {object} infile `echartsModel` of the chart
 * @property {object[]} [values] Rows of the chart. A series of another datasource carries its rows in `awe.values`
 * @property {number} [width] Width of the image
 * @property {number} [height] Height of the image
 * @property {string} [language] Language of the texts (es-ES, fr-FR, en-GB...)
 * @property {Object<string, string>} [translations] Translations of the locale keys of the chart
 */

/**
 * Render a chart to an SVG image
 * @param {ChartRenderRequest} request Render request
 * @returns {string} SVG image
 */
export function renderChart({infile, values = [], width, height, language, translations = {}}) {
  if (!infile || typeof infile !== "object" || Array.isArray(infile)) {
    throw new Error("The request has no chart (infile)");
  }
  const size = {width: width ?? DEFAULT_SIZE.width, height: height ?? DEFAULT_SIZE.height};
  const t = (key) => translations[key] ?? key;
  const option = buildEChartsOption(infile, values, {...size, t, locale: localeOf(language), print: true});
  return renderSvg(option, size, language);
}
