// View of a card (#899): the client contract shared with the React engine (AweCard). The fields come from the first
// row of the model over the attributes of the screen XML; the directive only renders the result
import {buildSparkline, sparklineMarkup} from "./chart/sparklineRenderer";

export const CARD_TONES = ["success", "warning", "danger", "info", "neutral"];
export const CARD_TRENDS = ["up", "down", "flat"];
// Fields of the card (row or attribute)
export const CARD_FIELDS = ["title", "label", "subtitle", "icon", "iconTone", "value", "unit", "numberDecimals", "delta",
  "deltaUnit", "trend", "goodTrend", "tone", "caption", "target", "description", "footer", "visible",
  "sparkline", "sparklineType", "progressStyle", "chip", "chipTone"];

const DEFAULT_DELTA_UNIT = "%";
const DEFAULT_ICON_TONE = "primary";
const DEFAULT_CHIP_TONE = "neutral";
const CARD_SPARKLINE_TYPES = ["line", "area", "bar"];
const DEFAULT_SPARKLINE_TYPE = "area";
const CARD_PROGRESS_STYLES = ["bar", "gauge"];
const DEFAULT_PROGRESS_STYLE = "bar";
const DEFAULT_MAX_DECIMALS = 2;
const MINUS = "\u2212";
const ARROWS = {up: "\u2197", down: "\u2198", flat: "\u2192"};
const TREND_LABELS = {up: "SCREEN_TEXT_CARD_TREND_UP", down: "SCREEN_TEXT_CARD_TREND_DOWN", flat: "SCREEN_TEXT_CARD_TREND_FLAT"};
const TARGET_OF = "SCREEN_TEXT_CARD_TARGET_OF";

const isPresent = value => value !== undefined && value !== null && value !== "";

/**
 * Field of the card: the one of the row when it is present (not null nor empty), else the attribute
 * @param {string} name Field name
 * @param {Object} attributes Attributes of the card (controller)
 * @param {Object} [row] First row of the model values
 * @returns {*} Field value
 */
export function getCardField(name, attributes = {}, row = null) {
  return row && isPresent(row[name]) ? row[name] : attributes[name];
}

/**
 * Number of a field: a finite number or a numeric string
 * @param {*} value Field value
 * @returns {number|null} The number, null if it is not numeric
 */
export function toCardNumber(value) {
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
 * Format a number with the user language: exactly the given decimals, else up to 2
 * @param {number} number Number
 * @param {string} [language] User language
 * @param {number} [decimals] Decimals
 * @returns {string} Formatted number
 */
export function formatCardNumber(number, language, decimals) {
  const fixed = Number.isInteger(decimals) && decimals >= 0;
  const options = fixed ? {minimumFractionDigits: decimals, maximumFractionDigits: decimals} : {maximumFractionDigits: DEFAULT_MAX_DECIMALS};
  try {
    return new Intl.NumberFormat(language || undefined, options).format(number);
  } catch {
    // Unknown language tag: the default locale of the browser
    return new Intl.NumberFormat(undefined, options).format(number);
  }
}

/**
 * Trend of the card: the explicit one when it is valid, else from the sign of the delta
 * @param {string} trend Explicit trend
 * @param {number|null} delta Numeric delta
 * @returns {string|null} up, down or flat; null without trend nor numeric delta
 */
export function getCardTrend(trend, delta) {
  if (CARD_TRENDS.includes(trend)) {
    return trend;
  }
  if (delta === null || delta === undefined) {
    return null;
  }
  if (delta < 0) {
    return "down";
  }
  return delta > 0 ? "up" : "flat";
}

/**
 * Tone of the delta: the tone field wins; else flat is neutral, the good trend (up by default) success, else danger.
 * The sign of the value never matters
 * @param {{tone?: string, trend: string, goodTrend?: string}} options Tone, trend and good trend
 * @returns {string} Tone
 */
export function getCardTone({tone, trend, goodTrend}) {
  if (CARD_TONES.includes(tone)) {
    return tone;
  }
  if (trend === "flat") {
    return "neutral";
  }
  const good = ["up", "down"].includes(goodTrend) ? goodTrend : "up";
  return trend === good ? "success" : "danger";
}

const withUnit = (text, unit) => (unit === "%" ? `${text}%` : `${text} ${unit}`);

// The delta keeps up to 2 decimals: number-decimals only applies to the value and the target
const getDelta = (field, translate, language) => {
  const delta = toCardNumber(field("delta"));
  if (delta === null) {
    return null;
  }
  const explicit = CARD_TRENDS.includes(field("trend"));
  const trend = getCardTrend(field("trend"), delta);
  let sign = "";
  if (!explicit && delta > 0) {
    sign = "+";
  } else if (!explicit && delta < 0) {
    sign = MINUS;
  }
  const unit = isPresent(field("deltaUnit")) ? String(field("deltaUnit")) : DEFAULT_DELTA_UNIT;
  const tone = getCardTone({tone: field("tone"), trend, goodTrend: field("goodTrend")});
  return {
    trend,
    tone,
    arrow: ARROWS[trend],
    text: withUnit(sign + formatCardNumber(Math.abs(delta), language), unit),
    label: translate(TREND_LABELS[trend])
  };
};

// The gauge shows the percent in the middle of its arc, so its text below only tells the target
const getProgress = (value, target, unit, translate, language, decimals, gauge) => {
  if (value === null || target === null || target === 0) {
    return null;
  }
  const percent = Math.round(value / target * 100);
  const targetText = `${translate(TARGET_OF)} ${formatCardNumber(target, language, decimals)}` +
    (isPresent(unit) ? ` ${unit}` : "");
  return {
    percent,
    width: Math.min(100, Math.max(0, percent)),
    text: gauge ? targetText : `${percent} % ${targetText}`
  };
};

// Status chip at the top right, before the icon or the delta (#836): its text tells the status, the dot and the colour
// only repeat it. No text, no chip; an unknown tone is neutral
const getChip = (text, tone) => (text === null ? null : {text, tone: CARD_TONES.includes(tone) ? tone : DEFAULT_CHIP_TONE});

/**
 * Writes the sparkline of a card: the SVG of the grid sparkline column without its fixed size, stretched to the width
 * of the card (the strokes keep their width through the CSS)
 * @param {Object|null} model Model built by buildSparkline
 * @returns {string} The SVG markup, or an empty string when there is nothing to draw
 */
export function cardSparklineMarkup(model) {
  if (!model) {
    return "";
  }
  return sparklineMarkup(model)
    .replace(` width="${model.width}" height="${model.height}" `, ' preserveAspectRatio="none" ');
}

/**
 * View of the card
 * @param {Object} attributes Attributes of the card (controller)
 * @param {Object} [row] First row of the model values
 * @param {{language?: string, translate: function}} options User language and translation of the texts (locale key or
 * plain text)
 * @returns {Object} View: texts, status chip, value, delta and its placement, sparkline, progress and its style, and visibility (null
 * for the missing parts)
 */
export function buildCard(attributes = {}, row = null, {language, translate}) {
  const field = name => getCardField(name, attributes, row);
  const text = name => (isPresent(field(name)) ? translate(String(field(name))) : null);
  const decimalsField = toCardNumber(field("numberDecimals"));
  const decimals = Number.isInteger(decimalsField) && decimalsField >= 0 ? decimalsField : undefined;

  const rawValue = field("value");
  const value = toCardNumber(rawValue);
  const unit = isPresent(field("unit")) ? String(field("unit")) : null;
  const icon = isPresent(field("icon")) ? String(field("icon")) : null;
  const delta = getDelta(field, translate, language);

  let valueView = null;
  if (isPresent(rawValue)) {
    valueView = {number: value === null ? String(rawValue) : formatCardNumber(value, language, decimals), unit};
  }

  const sparklineType = CARD_SPARKLINE_TYPES.includes(field("sparklineType")) ? field("sparklineType") : DEFAULT_SPARKLINE_TYPE;
  const progressStyle = CARD_PROGRESS_STYLES.includes(field("progressStyle")) ? field("progressStyle") : DEFAULT_PROGRESS_STYLE;

  return {
    title: isPresent(field("title")) ? text("title") : text("label"),
    subtitle: text("subtitle"),
    chip: getChip(text("chip"), field("chipTone")),
    icon,
    iconTone: isPresent(field("iconTone")) ? String(field("iconTone")) : DEFAULT_ICON_TONE,
    value: valueView,
    delta,
    deltaInHeader: delta !== null && icon === null,
    deltaInFooter: delta !== null && icon !== null,
    caption: text("caption"),
    sparkline: buildSparkline(field("sparkline"), {type: sparklineType, color: "currentColor"}),
    progress: getProgress(value, toCardNumber(field("target")), unit, translate, language, decimals,
      progressStyle === "gauge"),
    progressStyle,
    description: text("description"),
    footer: text("footer"),
    visible: field("visible") !== false && field("visible") !== "false"
  };
}
