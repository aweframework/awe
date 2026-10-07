import {evaluate, parseTemplate} from "./chartTemplate";

export {parseTemplate, resolvePath} from "./chartTemplate";

/**
 * Formatting of chart texts.
 *
 * The server cannot send functions, so the chart sends Highcharts format templates (for example
 * `<b>{point.name}</b>: {point.percentage:.1f} %`, or `{#if (gt y 0)}{y:,.0f}{else}{(multiply y -1):,.0f}{/if}`) and
 * the name of the formatters it needs. This module interprets them in the client: the template is parsed into nodes
 * by `chartTemplate` (text, expressions and conditions) and then rendered against a context, with the number and
 * date specifications of Highcharts.
 */

/**
 * Locale used to format numbers and dates
 * @typedef {object} ChartLocale
 * @property {string} [decimalPoint] Decimal point
 * @property {string} [thousandsSep] Thousands separator
 * @property {string[]} [months] Month names
 * @property {string[]} [shortMonths] Short month names
 * @property {string[]} [weekdays] Weekday names, starting on Sunday
 */

const DEFAULT_MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September",
  "October", "November", "December"];
const DEFAULT_SHORT_MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const DEFAULT_WEEKDAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

/**
 * Magnitudes of the currency formatter, from the biggest to the smallest
 * @type {{exp: number, symbol: string}[]}
 */
const MAGNITUDES = [{exp: 6, symbol: "M"}, {exp: 3, symbol: "K"}, {exp: 0, symbol: ""}];

const NUMBER_SPEC = /^(,)?\.(\d+)f$/;
const MAX_PRECISION = 12;

/**
 * Named formatters that the server asks for by name
 * @type {Object<string, function(number): string>}
 */
export const FORMATTERS = {
  /**
   * Format a value with the symbol of its magnitude (K, M)
   * @param {number} value Value
   * @returns {string} Formatted value
   */
  formatCurrencyMagnitude: (value) => {
    if (value === null || value === undefined || !Number.isFinite(Number(value))) {
      return "";
    }
    const number = Number(value);
    const round = (exp) => Math.round(number * 100 / Math.pow(10, exp)) / 100;
    // The magnitude is chosen after rounding, so 999999 is 1M and not 1000K
    const index = MAGNITUDES.findIndex(({exp}) => Math.abs(number) >= Math.pow(10, exp));
    let magnitude = index >= 0 ? MAGNITUDES[index] : MAGNITUDES.at(-1);
    if (index > 0 && Math.abs(round(magnitude.exp)) >= 1000) {
      magnitude = MAGNITUDES[index - 1];
    }
    return (magnitude.exp > 0 ? round(magnitude.exp) : number) + magnitude.symbol;
  }
};

/**
 * Separate the digits of an integer in groups of three
 * @param {string} digits Digits
 * @param {string} separator Thousands separator
 * @returns {string} Grouped digits
 */
function groupThousands(digits, separator) {
  const groups = [];
  for (let end = digits.length; end > 0; end -= 3) {
    groups.unshift(digits.substring(Math.max(end - 3, 0), end));
  }
  return groups.join(separator);
}

/**
 * Format a number
 * @param {number|string} value Value
 * @param {number} [decimals] Number of decimals, the value is kept when it is not defined
 * @param {ChartLocale} [locale] Decimal point and thousands separator
 * @returns {string} Formatted number, empty when the value is not a number
 */
export function formatNumber(value, decimals, locale = {}) {
  const number = value === null || value === undefined || value === "" ? Number.NaN : Number(value);
  if (!Number.isFinite(number)) {
    return "";
  }
  const text = decimals === undefined ? String(number) : number.toFixed(decimals);
  const [integer, fraction] = text.split(".");
  const sign = integer.startsWith("-") ? "-" : "";
  const digits = sign ? integer.slice(1) : integer;
  const grouped = locale.thousandsSep ? groupThousands(digits, locale.thousandsSep) : digits;
  return sign + grouped + (fraction === undefined ? "" : (locale.decimalPoint || ".") + fraction);
}

const pad = (value, length = 2) => String(value).padStart(length, "0");

const DATE_CODES = {
  Y: (date) => String(date.getFullYear()),
  y: (date) => pad(date.getFullYear() % 100),
  m: (date) => pad(date.getMonth() + 1),
  d: (date) => pad(date.getDate()),
  e: (date) => String(date.getDate()),
  H: (date) => pad(date.getHours()),
  M: (date) => pad(date.getMinutes()),
  S: (date) => pad(date.getSeconds()),
  L: (date) => pad(date.getMilliseconds(), 3),
  b: (date, locale) => (locale.shortMonths || DEFAULT_SHORT_MONTHS)[date.getMonth()],
  B: (date, locale) => (locale.months || DEFAULT_MONTHS)[date.getMonth()],
  A: (date, locale) => (locale.weekdays || DEFAULT_WEEKDAYS)[date.getDay()],
  a: (date, locale) => (locale.weekdays || DEFAULT_WEEKDAYS)[date.getDay()].substring(0, 3)
};

/**
 * Format a date with a Highcharts date pattern (%Y-%m-%d), in local time like the ECharts time axes
 * @param {number|string|Date} value Timestamp
 * @param {string} pattern Pattern with the codes %Y %y %m %d %e %H %M %S %L %b %B %a %A
 * @param {ChartLocale} [locale] Month and weekday names
 * @returns {string} Formatted date, empty when the date is not valid
 */
export function formatDate(value, pattern, locale = {}) {
  if (value === null || value === undefined || value === "") {
    return "";
  }
  const date = new Date(value instanceof Date ? value : Number(value));
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return String(pattern ?? "").replace(/%(.)/g, (match, code) => {
    if (code === "%") {
      return "%";
    }
    return DATE_CODES[code] ? DATE_CODES[code](date, locale) : match;
  });
}

const ECHARTS_TIME_CODES = {
  Y: "{yyyy}", y: "{yy}", m: "{MM}", d: "{dd}", e: "{d}", H: "{HH}", M: "{mm}", S: "{ss}", L: "{SSS}",
  b: "{MMM}", B: "{MMMM}", a: "{ee}", A: "{eeee}"
};

/**
 * Translate a Highcharts date pattern into the template of the ECharts time axis labels
 * @param {string} pattern Highcharts pattern (%Y-%m-%d)
 * @returns {string} ECharts template ({yyyy}-{MM}-{dd})
 */
export function toEChartsTimeFormat(pattern) {
  return String(pattern ?? "").replace(/%(.)/g, (match, code) => ECHARTS_TIME_CODES[code] || match);
}

/**
 * Render the value of an expression
 * @param {*} value Value
 * @param {string} [spec] Format specification (.2f, ,.2f, %Y-%m-%d)
 * @param {ChartLocale} locale Locale
 * @returns {string} Text
 */
function formatValue(value, spec, locale) {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return "";
  }
  if (spec?.startsWith("%")) {
    return formatDate(value, spec, locale);
  }
  const numberSpec = spec ? NUMBER_SPEC.exec(spec) : null;
  if (numberSpec) {
    return formatNumber(value, Number(numberSpec[2]), {
      decimalPoint: locale.decimalPoint,
      thousandsSep: numberSpec[1] ? locale.thousandsSep : undefined
    });
  }
  return String(value);
}

/**
 * Escape the characters of a text that have a meaning in html
 * @param {*} text Text
 * @returns {string} Escaped text
 */
export function escapeHtml(text) {
  return String(text ?? "").replace(/[&<>"']/g, character =>
    ({"&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"}[character]));
}

/**
 * Remove the tags of a text, from each "<" to the next ">" (an unclosed "<" is kept as text)
 * @param {string} text Text
 * @returns {string} Text without tags
 */
function removeTags(text) {
  let result = "";
  let position = 0;
  while (position < text.length) {
    const open = text.indexOf("<", position);
    const close = open < 0 ? -1 : text.indexOf(">", open);
    if (close < 0) {
      break;
    }
    result += text.substring(position, open);
    position = close + 1;
  }
  return result + text.substring(position);
}

/**
 * Remove the html tags of a text, line breaks become spaces
 * @param {string} text Text
 * @returns {string} Plain text
 */
export function stripHtml(text) {
  return removeTags(String(text ?? "").replace(/<br\s*\/?>/gi, " "))
    .replaceAll("&nbsp;", " ")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Evaluate an expression. The result of a calculation may have floating point noise (0.1 + 0.2), which is rounded; a
 * value that the context gives (a timestamp in milliseconds) stays exact
 * @param {object} expression Expression of a parsed template
 * @param {object} context Values that the expression refers to
 * @returns {*} Value
 */
function calculated(expression, context) {
  const value = evaluate(expression, context);
  const rounded = expression.type === "call" && typeof value === "number" && Number.isFinite(value);
  return rounded ? Number(value.toPrecision(MAX_PRECISION)) : value;
}

/**
 * Render the nodes of a parsed template
 * @param {object[]} nodes Nodes
 * @param {object} context Values that the expressions refer to
 * @param {{locale: ChartLocale, escapeValues: boolean}} options Locale and escaping of the values
 * @returns {string} Text
 */
function renderNodes(nodes, context, options) {
  return nodes.map(node => {
    if (node.type === "text") {
      return node.value;
    }
    if (node.type === "if") {
      return renderNodes(evaluate(node.condition, context) ? node.whenTrue : node.whenFalse, context, options);
    }
    const text = formatValue(calculated(node.expression, context), node.spec, options.locale);
    return options.escapeValues ? escapeHtml(text) : text;
  }).join("");
}

/**
 * Format a Highcharts template with a context
 * @param {string} template Template ({point.name}: {point.y:.2f}, {#if (gt y 0)}up{else}down{/if})
 * @param {object} context Values that the expressions refer to
 * @param {{locale: ChartLocale, stripHtml: boolean, escapeValues: boolean}} [options] Locale to format numbers and
 * dates, whether the html of the template has to be removed (labels of ECharts do not render html), and whether the
 * values that replace the expressions have to be escaped (the template is html, the values are not)
 * @returns {string} Formatted text
 */
export function formatTemplate(template, context, options = {}) {
  const text = renderNodes(parseTemplate(template), context, {locale: options.locale || {}, escapeValues: options.escapeValues});
  return options.stripHtml ? stripHtml(text) : text;
}
