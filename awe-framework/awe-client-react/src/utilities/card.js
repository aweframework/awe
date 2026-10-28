/**
 * Rules of the card (#899): the shared client contract that the AngularJS client applies the same way, so both
 * engines show the same text, trend and tone for the same data.
 *
 * Framework-agnostic: pure functions from the data of the card to what it shows.
 */

export const CARD_TRENDS = ["up", "down", "flat"];
export const CARD_TONES = ["success", "warning", "danger", "info", "neutral"];
export const CARD_ARROWS = Object.freeze({up: "↗", down: "↘", flat: "→"});
export const CARD_TREND_LABELS = Object.freeze({
  up: "SCREEN_TEXT_CARD_TREND_UP",
  down: "SCREEN_TEXT_CARD_TREND_DOWN",
  flat: "SCREEN_TEXT_CARD_TREND_FLAT"
});
export const CARD_TARGET_OF = "SCREEN_TEXT_CARD_TARGET_OF";
export const CARD_SPARKLINE_TYPES = ["line", "area", "bar"];
export const CARD_GAUGE_ARC = "M 8 50 A 42 42 0 0 1 92 50";

const DEFAULT_DELTA_UNIT = "%";
const DEFAULT_CHIP_TONE = "neutral";
const DEFAULT_SPARKLINE_TYPE = "area";
const GAUGE = "gauge";
const BAR = "bar";
const DEFAULT_GOOD_TREND = "up";
const GOOD_TRENDS = ["up", "down"];
const MAX_DECIMALS = 2;
const MINUS_SIGN = "\u2212";

const isEmptyField = value => value === undefined || value === null || value === "";

/**
 * Field of the card: the one of the row (first row of the model values) when it has a value, else the attribute
 * @param {object} row First row of the model values
 * @param {object} attributes Attributes of the card
 * @param {string} name Field name
 * @return {*} Value of the field
 */
export function cardField(row, attributes, name) {
  const value = row?.[name];
  return isEmptyField(value) ? attributes?.[name] : value;
}

/**
 * Number of a field: a finite number or a numeric string
 * @param {*} value Field value
 * @return {number|null} The number, or null when the value is not a finite number
 */
export function cardNumber(value) {
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }
  if (typeof value === "string" && value.trim() !== "") {
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
  }
  return null;
}

/**
 * Format a number with the language of the user: exactly the decimals given, else up to 2
 * @param {number} number Number
 * @param {string} language Language of the user
 * @param {number|string} [decimals] Number of decimals (a whole number of zero or more)
 * @return {string} Formatted number
 */
export function formatCardNumber(number, language, decimals) {
  const fixed = cardNumber(decimals);
  const options = Number.isInteger(fixed) && fixed >= 0 ?
    {minimumFractionDigits: fixed, maximumFractionDigits: fixed} :
    {maximumFractionDigits: MAX_DECIMALS};
  try {
    return new Intl.NumberFormat(language || undefined, options).format(number);
  } catch {
    // Unknown language tag: the default language of the browser
    return new Intl.NumberFormat(undefined, options).format(number);
  }
}

/**
 * Visibility of the card: hidden only when the field is false (a boolean or its text)
 * @param {*} visible Field value
 * @return {boolean} The card is visible
 */
export function isCardVisible(visible) {
  return visible !== false && visible !== "false";
}

/**
 * Trend of the card: the explicit trend, else the sign of the delta
 * @param {string} trend Explicit trend
 * @param {number|null} delta Numeric delta
 * @return {string|null} up, down or flat; null without a numeric delta
 */
export function cardTrend(trend, delta) {
  if (delta === null || delta === undefined) {
    return null;
  }
  if (CARD_TRENDS.includes(trend)) {
    return trend;
  }
  if (delta < 0) {
    return "down";
  }
  return delta > 0 ? "up" : "flat";
}

/**
 * Text of the delta: unsigned with an explicit trend, else signed (minus sign U+2212); the percent glued, any other
 * unit after a space
 * @param {number} delta Numeric delta
 * @param {object} options Options
 * @param {string} options.language Language of the user
 * @param {boolean} [options.explicitTrend] The trend is explicit
 * @param {string} [options.unit] Unit of the delta (% by default)
 * @return {string} Text of the delta (up to 2 decimals: number-decimals only applies to the value and the target)
 */
export function cardDeltaText(delta, {language, explicitTrend = false, unit} = {}) {
  const number = formatCardNumber(Math.abs(delta), language);
  let sign = "";
  if (!explicitTrend && delta > 0) {
    sign = "+";
  } else if (!explicitTrend && delta < 0) {
    sign = MINUS_SIGN;
  }
  const deltaUnit = isEmptyField(unit) ? DEFAULT_DELTA_UNIT : String(unit);
  return deltaUnit === DEFAULT_DELTA_UNIT ? `${sign}${number}${deltaUnit}` : `${sign}${number} ${deltaUnit}`;
}

/**
 * Tone of the delta: the tone given, else flat is neutral and the good trend (up by default) is success
 * @param {string} tone Tone given
 * @param {string} trend Trend
 * @param {string} [goodTrend] Good trend
 * @return {string} success, warning, danger, info or neutral
 */
export function cardTone(tone, trend, goodTrend) {
  if (CARD_TONES.includes(tone)) {
    return tone;
  }
  if (trend === "flat") {
    return "neutral";
  }
  const good = GOOD_TRENDS.includes(goodTrend) ? goodTrend : DEFAULT_GOOD_TREND;
  return trend === good ? "success" : "danger";
}

/**
 * Tone of the status chip (#836)
 * @param {string} tone Tone given
 * @return {string} The tone given when it is known, neutral otherwise
 */
export function cardChipTone(tone) {
  return CARD_TONES.includes(tone) ? tone : DEFAULT_CHIP_TONE;
}

/**
 * Progress towards the target
 * @param {number|null} value Numeric value
 * @param {number|null} target Numeric target
 * @return {{percent: number, width: number}|null} Rounded percent and width of the bar (0..100); null without a
 * numeric value or a numeric non zero target
 */
export function cardProgress(value, target) {
  if (value === null || value === undefined || !target) {
    return null;
  }
  const percent = Math.round(value / target * 100);
  return {percent, width: Math.min(100, Math.max(0, percent))};
}

/**
 * Type of the sparkline of the card: line, area or bar
 * @param {string} type Type given
 * @return {string} The type given, an area by default
 */
export function cardSparklineType(type) {
  return CARD_SPARKLINE_TYPES.includes(type) ? type : DEFAULT_SPARKLINE_TYPE;
}

/**
 * Style of the progress of the card: a bar or a gauge
 * @param {string} style Style given
 * @return {string} gauge when given, a bar otherwise
 */
export function cardProgressStyle(style) {
  return style === GAUGE ? GAUGE : BAR;
}
