// View of a card (#899): the client contract shared with the React engine (AweCard). The fields come from the first
// row of the model over the attributes of the screen XML; the directive only renders the result
import {buildSparkline, sparklineMarkup} from "./chart/sparklineRenderer";

export const CARD_TONES = ["success", "warning", "danger", "info", "neutral"];
export const CARD_TRENDS = ["up", "down", "flat"];
// Fields of the card (row or attribute)
export const CARD_FIELDS = ["title", "label", "subtitle", "icon", "iconTone", "value", "unit", "numberDecimals", "delta",
  "deltaUnit", "trend", "goodTrend", "tone", "caption", "target", "description", "footer", "visible",
  "sparkline", "sparklineType", "progressStyle", "chip", "chipTone", "pills", "strip", "stack", "tiles", "tilesLabel",
  "details", "detailsLabel"];

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

const pillText = value => (isPresent(value) ? String(value) : null);

/**
 * Pills of the card (#993): the facts shown under its heading, each one a label, an optional icon and an optional code
 * (a name the application runs, shown in the monospace font). The same rule as the React engine (utilities/card.js)
 * @param {*} value Field value: a JSON array (text or parsed) of {label, icon?, code?}
 * @returns {{label: string|null, icon: string|null, code: string|null}[]} The pills with a label or a code, in order;
 * an empty list for anything else
 */
export function getCardPills(value) {
  let items = value;
  if (typeof value === "string") {
    try {
      items = JSON.parse(value);
    } catch {
      return [];
    }
  }
  if (!Array.isArray(items)) {
    return [];
  }
  return items
    .filter(item => item !== null && typeof item === "object" && !Array.isArray(item))
    .map(item => ({label: pillText(item.label), icon: pillText(item.icon), code: pillText(item.code)}))
    .filter(item => item.label !== null || item.code !== null);
}

/**
 * Parsed JSON array of a field: the array itself, the array of a JSON text, null for anything else
 * @param {*} value Field value
 * @returns {Array|null} The array
 */
const jsonArray = (value) => {
  let items = value;
  if (typeof value === "string") {
    if (!value.trim().startsWith("[")) {
      return null;
    }
    try {
      items = JSON.parse(value);
    } catch {
      return null;
    }
  }
  return Array.isArray(items) ? items : null;
};

/**
 * Segments of a rich text field (#993, T37): a JSON array (text or parsed) of {text, strong?}, the strong ones drawn
 * bold. The same rule as the React engine (utilities/card.js)
 * @param {*} value Field value
 * @returns {{text: string, strong: boolean}[]|null} The segments with a text, in order; null when the value is not a
 * list of segments (a plain text stays as it is)
 */
export function getCardSegments(value) {
  const items = jsonArray(value);
  if (!items) {
    return null;
  }
  const segments = items
    .filter(item => item !== null && typeof item === "object" && !Array.isArray(item) && isPresent(item.text))
    .map(item => ({text: String(item.text), strong: item.strong === true || item.strong === "true"}));
  return segments.length > 0 ? segments : null;
}

/**
 * Tiles of the card (#993, T37): small boxes in a grid, each one a label on top, a bold value and an optional muted
 * hint. The same rule as the React engine (utilities/card.js)
 * @param {*} value Field value: a JSON array (text or parsed) of {label, value, hint?}
 * @returns {{label: string, value: string|null, hint: string|null}[]} The tiles with a label, in order; an empty list
 * for anything else
 */
export function getCardTiles(value) {
  return (jsonArray(value) || [])
    .filter(item => item !== null && typeof item === "object" && !Array.isArray(item))
    .map(item => ({label: pillText(item.label), value: pillText(item.value), hint: pillText(item.hint)}))
    .filter(item => item.label !== null);
}

/**
 * Stacked bar of the card (#993): the parts of a whole (the executions by status), each one a segment as wide as its
 * share, in the colour of its tone. The same rule as the React engine (utilities/card.js)
 * @param {*} value Field value: a JSON array (text or parsed) of {tone, label, value}
 * @returns {{tone: string, label: string|null, value: number}[]} The segments with a positive value, in order (an
 * unknown tone is neutral); an empty list for anything else
 */
export function getCardStack(value) {
  let items = value;
  if (typeof value === "string") {
    try {
      items = JSON.parse(value);
    } catch {
      return [];
    }
  }
  if (!Array.isArray(items)) {
    return [];
  }
  return items
    .filter(item => item !== null && typeof item === "object" && !Array.isArray(item))
    .map(item => ({
      tone: CARD_TONES.includes(item.tone) ? item.tone : DEFAULT_CHIP_TONE,
      label: pillText(item.label),
      value: toCardNumber(item.value)
    }))
    .filter(item => item.value !== null && item.value > 0);
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

// Stacked bar: its segments, each one with its text (count and label), and the text of the whole bar; null without segments
const getStack = (value, translate) => {
  const segments = getCardStack(value).map(segment => ({
    ...segment, text: `${segment.value} ${segment.label === null ? "" : translate(segment.label)}`.trim()
  }));
  return segments.length > 0 ? {segments, label: segments.map(segment => segment.text).join(", ")} : null;
};

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
 * Text of a field of the card, or null when it is missing
 * @param {*} value Field value
 * @returns {string|null} Text
 */
const optionalText = value => (isPresent(value) ? String(value) : null);

/**
 * Value of a field when it is one of the allowed ones, or its default
 * @param {*} value Field value
 * @param {string[]} allowed Allowed values
 * @param {string} fallback Default value
 * @returns {string} Value
 */
const allowedOr = (value, allowed, fallback) => (allowed.includes(value) ? value : fallback);

/**
 * Decimals of the card value: a non negative integer, or undefined to format it as it comes
 * @param {*} field Field value
 * @returns {number|undefined} Decimals
 */
function getCardDecimals(field) {
  const decimals = toCardNumber(field);
  return Number.isInteger(decimals) && decimals >= 0 ? decimals : undefined;
}

/**
 * Value view of the card: its formatted number (or its text when it is not a number) and its unit
 * @param {*} rawValue Value field
 * @param {number|null} value Value as number
 * @param {string|null} unit Unit
 * @param {string} language User language
 * @param {number} [decimals] Decimals
 * @returns {Object|null} Value view, null without value
 */
function getValueView(rawValue, value, unit, language, decimals) {
  if (!isPresent(rawValue)) {
    return null;
  }
  return {number: value === null ? String(rawValue) : formatCardNumber(value, language, decimals), unit};
}

/**
 * Title of the card: its segments joined, its title or its label
 * @param {Array|null} titleSegments Title segments
 * @param {function} field Field getter
 * @param {function} text Translated text getter
 * @returns {string|null} Title
 */
function getCardTitle(titleSegments, field, text) {
  const title = isPresent(field("title")) ? text("title") : text("label");
  return titleSegments ? titleSegments.map(segment => segment.text).join("") : title;
}

/**
 * View of the card
 * @param {Object} attributes Attributes of the card (controller)
 * @param {Object} [row] First row of the model values
 * @param {{language?: string, translate: function}} options User language and translation of the texts (locale key or
 * plain text)
 * @returns {Object} View: texts, status chip, pills, value, delta and its placement, sparkline, status strip, stacked bar
 * (its segments and its text), progress and its style, and visibility (null for the missing parts, no pills when there
 * are none)
 */
export function buildCard(attributes = {}, row = null, {language, translate}) {
  const field = name => getCardField(name, attributes, row);
  const text = name => (isPresent(field(name)) ? translate(String(field(name))) : null);
  const decimals = getCardDecimals(field("numberDecimals"));

  const rawValue = field("value");
  const value = toCardNumber(rawValue);
  const unit = optionalText(field("unit"));
  const icon = optionalText(field("icon"));
  const delta = getDelta(field, translate, language);
  const valueView = getValueView(rawValue, value, unit, language, decimals);

  const sparklineType = allowedOr(field("sparklineType"), CARD_SPARKLINE_TYPES, DEFAULT_SPARKLINE_TYPE);
  const progressStyle = allowedOr(field("progressStyle"), CARD_PROGRESS_STYLES, DEFAULT_PROGRESS_STYLE);

  const titleSegments = getCardSegments(field("title"));
  const title = getCardTitle(titleSegments, field, text);

  return {
    title,
    titleSegments,
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
    pills: getCardPills(field("pills")).map(pill => ({...pill, label: pill.label === null ? null : translate(pill.label)})),
    strip: isPresent(field("strip")) ? field("strip") : null,
    stack: getStack(field("stack"), translate),
    tiles: getCardTiles(field("tiles")),
    tilesLabel: text("tilesLabel"),
    details: text("details"),
    detailsLabel: text("detailsLabel"),
    visible: field("visible") !== false && field("visible") !== "false"
  };
}
