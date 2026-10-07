import {escapeHtml, formatDate, formatNumber, formatTemplate} from "./chartFormat";
import {hintsOf, pointContext, withoutAwe} from "./echartsContext";

const NO_VALUE = "-";
const LINE_BREAK = "<br/>";

/**
 * Create the function that writes the value of a point in the tooltip, with the decimals, prefix and suffix of the
 * tooltip. The suffix of a series (`valueSuffix`) replaces the one of the tooltip, like in Highcharts
 * @param {object} hints Hints of the tooltip
 * @param {object} locale Locale
 * @param {string} [valueSuffix] Suffix of the series
 * @returns {{write: function(*): string, formatted: boolean}} Writer, and whether the tooltip asked for any format
 */
export function createValueText(hints, locale, valueSuffix) {
  const {numberDecimals, prefix = ""} = hints;
  const suffix = valueSuffix ?? hints.suffix ?? "";
  return {
    formatted: numberDecimals !== undefined || prefix !== "" || suffix !== "",
    write: (value) => {
      if (value === null || value === undefined || value === "") {
        return NO_VALUE;
      }
      const text = numberDecimals === undefined ? String(value) : formatNumber(value, numberDecimals, locale);
      return `${prefix}${text}${suffix}`;
    }
  };
}

/**
 * Create the function that renders a template of the tooltip. The tooltip of ECharts is html, and Highcharts draws basic
 * html (b, i, br, span) in its tooltips even without `useHTML`, so the tags of the template are kept and the values that
 * replace its expressions are escaped
 * @param {object} locale Locale
 * @returns {function(string, object): string} Renderer
 */
function createRenderer(locale) {
  return (template, context) => formatTemplate(template, context, {locale, escapeValues: true});
}

/**
 * Text of the header when the tooltip does not format it: the date, when the tooltip has a date format, or the axis
 * value
 * @param {object} hints Hints of the tooltip
 * @param {object} first First point, with its item and its context
 * @param {object} environment Environment (locale, timeX)
 * @returns {string} Text, empty when there is nothing to show
 */
function axisLabel(hints, first, environment) {
  if (!Array.isArray(first.item.value)) {
    return "";
  }
  if (hints.dateFormat && environment.timeX) {
    return formatDate(first.context.x, hints.dateFormat, environment.locale);
  }
  return first.item.axisValueLabel ?? first.item.name ?? "";
}

/**
 * Header of the tooltip, which shows once above the lines of the points
 * @param {object} hints Hints of the tooltip
 * @param {object[]} points Points, with their item and their context
 * @param {object} environment Environment (locale, timeX)
 * @param {function(string, object): string} render Renderer of templates
 * @returns {string} Header, empty when there is nothing to show
 */
function headerOf(hints, points, environment, render) {
  if (points.length === 0) {
    return "";
  }
  if (hints.headerFormat !== undefined) {
    return render(hints.headerFormat, points[0].context);
  }
  const label = axisLabel(hints, points[0], environment);
  if (!label) {
    return "";
  }
  return hints.useHTML ? `<span style="font-size:0.8em">${escapeHtml(label)}</span>${LINE_BREAK}` : escapeHtml(label);
}

/**
 * Line of a point of the tooltip: its format, or the name of the series and the value
 * @param {{item: object, context: object}} point Point, with its item and its context
 * @param {object} hints Hints of the tooltip
 * @param {object} environment Environment (locale, infoOf)
 * @param {function(string, object): string} render Renderer of templates
 * @returns {string} Line
 */
function lineOf({item, context}, hints, environment, render) {
  // An html format draws its own markers, and a marker inside a table would break it
  const marker = hints.useHTML ? "" : item.marker ?? "";
  if (hints.pointFormat !== undefined) {
    return `${marker}${render(hints.pointFormat, context)}`;
  }
  const valueText = createValueText(hints, environment.locale, environment.infoOf(item).valueSuffix).write;
  const line = `${item.marker ?? ""}${escapeHtml(item.seriesName ?? item.name)}: ${valueText(context.y)}`;
  return hints.useHTML ? line + LINE_BREAK : line;
}

/**
 * Build the formatter of the tooltip from its formats: a header once, a line for each visible point and a footer.
 * With `useHTML` the pieces are joined as they are (they are html blocks, such as table rows); otherwise each one goes in
 * its own line.
 * @param {object} hints Hints of the tooltip
 * @param {object} environment Environment (inverted, locale, timeX, infoOf)
 * @returns {function(object|object[]): string} Formatter
 */
function createFormatter(hints, environment) {
  const {inverted, infoOf} = environment;
  const render = createRenderer(environment.locale);
  return (params) => {
    const list = Array.isArray(params) ? params : [params];
    const points = list.map(item => ({item, context: pointContext(item, inverted, infoOf(item))}));
    const visible = points.filter(({item}) => !infoOf(item).silent);
    const footer = hints.footerFormat !== undefined && points.length > 0 ? render(hints.footerFormat, points[0].context) : "";
    const pieces = [
      headerOf(hints, points, environment, render),
      ...visible.map(point => lineOf(point, hints, environment, render)),
      footer
    ];
    return pieces.filter(piece => piece !== "").join(hints.useHTML ? "" : LINE_BREAK);
  };
}

/**
 * Build the tooltip: the functions that the server cannot send are created from its hints
 * @param {object} tooltip Tooltip of the model
 * @param {object} environment Environment (inverted, locale, timeX, infoOf)
 * @returns {object} Tooltip for ECharts
 */
export function buildTooltip(tooltip, environment) {
  const hints = hintsOf(tooltip);
  const built = {...withoutAwe(tooltip), confine: true};
  const valueText = createValueText(hints, environment.locale);
  const hasFormats = [hints.pointFormat, hints.headerFormat, hints.footerFormat].some(format => format !== undefined);

  if (valueText.formatted) {
    built.valueFormatter = valueText.write;
  }
  if (hasFormats || (hints.dateFormat && environment.timeX)) {
    built.formatter = createFormatter(hints, environment);
  }
  return built;
}
