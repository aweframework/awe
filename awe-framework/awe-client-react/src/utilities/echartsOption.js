import {translateLabel} from "./index";
import {FORMATTERS, formatDate, formatNumber, formatTemplate, toEChartsTimeFormat} from "./chartFormat";

/**
 * Builds the Apache ECharts option of a chart from the `echartsModel` that the server sends beside the Highcharts
 * `chartModel`, and from the values of the component.
 *
 * The model is an ECharts option without data and without functions. Any object of it may carry an `awe` key with
 * client hints (see `EChartsModelBuilder` in awe-model); the hints are read here and removed before the option is given
 * to ECharts. The functions that ECharts needs (formatters, symbol sizes) are created here from those hints.
 *
 * `awe.theme` (blue, gray, dark-unica...) is ignored on purpose: the React client has never applied the Highcharts
 * themes, the look comes from the application theme.
 */

const AWE = "awe";
const TRANSPARENT = "rgba(0, 0, 0, 0)";
const MAX_SYMBOL_POINTS = 40;
const PIE_RADIUS_FACTOR = 0.8;
const BUBBLE_SIZE = {min: 10, max: 50};
const TIME_LEVELS = ["year", "month", "day", "hour", "minute", "second", "millisecond"];
const NO_VALUE = "-";
const MAX_LABELLED_CATEGORIES = 60;
const CATEGORY_LABEL = {fontSize: 11, charWidth: 7, labelPadding: 6, rotatedCharHeight: 4.7, maxRoom: 70, rotation: 45};

/**
 * Default colors of the series (the Highcharts 11 palette), used when the XML does not give a color
 * @type {string[]}
 */
export const PALETTE = ["#2caffe", "#544fc5", "#00e272", "#fe6a35", "#6b8abc", "#d568fb", "#2ee0ca", "#fa4b42",
  "#feb56a", "#91e8e1"];

const LAYOUT = {
  margin: 16,
  title: 28,
  subtitle: 20,
  legend: 34,
  legendTitle: 18,
  verticalLegend: 120,
  slider: 50,
  titleOffset: 12
};

/**
 * Check if a node has an `awe` key at any depth
 * @param {*} node Node
 * @returns {boolean} The node has hints
 */
export function hasAweKeys(node) {
  if (Array.isArray(node)) {
    return node.some(hasAweKeys);
  }
  if (node && typeof node === "object") {
    return Object.entries(node).some(([key, value]) => key === AWE || hasAweKeys(value));
  }
  return false;
}

/**
 * Remove the `awe` client hints of a node, at any depth. Functions and primitives are kept and the input is not changed
 * @param {*} node Node
 * @returns {*} Copy without hints
 */
export function stripAwe(node) {
  if (Array.isArray(node)) {
    return node.map(stripAwe);
  }
  if (node && typeof node === "object" && Object.getPrototypeOf(node) === Object.prototype) {
    return Object.fromEntries(Object.entries(node)
      .filter(([key]) => key !== AWE)
      .map(([key, value]) => [key, stripAwe(value)]));
  }
  return node;
}

const hintsOf = (node) => node?.[AWE] || {};
const withoutAwe = (node) => Object.fromEntries(Object.entries(node || {}).filter(([key]) => key !== AWE));
const escapeHtml = (text) => String(text ?? "").replace(/[&<>"']/g, character =>
  ({"&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"}[character]));
const isPie = (serie) => serie.type === "pie";
const asArray = (value) => Array.isArray(value) ? value : [];

/**
 * Find the drilldown series that a series opens
 * @param {object} model ECharts model
 * @param {string} seriesId Identifier of the series that was clicked
 * @returns {string|null} Identifier of the drilldown series, null when the series has no drilldown
 */
export function findDrilldownId(model, seriesId) {
  const serie = asArray(model?.series).find(item => item.id === seriesId);
  const target = hintsOf(serie).drilldown;
  const exists = asArray(hintsOf(model).drilldown?.series).some(item => item.id === target);
  return target && exists ? target : null;
}

// ------------------------------------------------------------------------------------------------------------
// Point context, shared by labels and tooltips
// ------------------------------------------------------------------------------------------------------------

/**
 * Describe a point that ECharts gives to a formatter in the terms of the Highcharts format language
 * @param {object} params Formatter parameters of ECharts
 * @param {boolean} inverted The chart is inverted, so the coordinates are [y, x]
 * @returns {object} Context of the format template
 */
function pointContext(params, inverted) {
  const coordinates = Array.isArray(params.value) ? params.value : null;
  const [xIndex, yIndex] = inverted ? [1, 0] : [0, 1];
  const x = coordinates?.[xIndex];
  const y = coordinates ? coordinates[yIndex] : params.value;
  const z = coordinates ? coordinates[2] : undefined;
  return {
    series: {name: params.seriesName},
    point: {name: params.name, x, y, z, percentage: params.percent},
    name: params.name, x, y, z, value: y, percentage: params.percent
  };
}

// ------------------------------------------------------------------------------------------------------------
// Series
// ------------------------------------------------------------------------------------------------------------

/**
 * Bind the component values to the points of a series
 * @param {object} serie Series of the model
 * @param {object[]} values Component values
 * @param {boolean} inverted The chart is inverted
 * @returns {Array} Points
 */
function bindData(serie, values, inverted) {
  const {xValue, yValue, zValue} = hintsOf(serie);
  const hasZ = Boolean(zValue);
  const rows = asArray(values);
  if (isPie(serie)) {
    return rows.map(row => ({name: row[xValue], value: row[yValue] ?? null}));
  }
  return rows.map(row => {
    const x = row[xValue] ?? null;
    const y = row[yValue] ?? null;
    const point = inverted ? [y, x] : [x, y];
    return hasZ ? [...point, row[zValue] ?? null] : point;
  });
}

/**
 * Scale a percentage radius. The Highcharts size leaves room for the labels, the ECharts radius does not
 * @param {string|number|Array} radius Radius
 * @returns {string|number|Array} Scaled radius
 */
function scaleRadius(radius) {
  if (Array.isArray(radius)) {
    return radius.map(scaleRadius);
  }
  const percent = typeof radius === "string" && radius.endsWith("%") ? Number.parseFloat(radius) : Number.NaN;
  return Number.isFinite(percent) ? `${Math.round(percent * PIE_RADIUS_FACTOR * 100) / 100}%` : radius;
}

/**
 * Size of the bubbles, scaled between a minimum and a maximum from their z value
 * @param {Array} data Points with [x, y, z]
 * @returns {function(Array): number} Symbol size function
 */
function bubbleSize(data) {
  const zs = data.map(point => point[2]).filter(Number.isFinite);
  const min = Math.min(...zs);
  const max = Math.max(...zs);
  const middle = (BUBBLE_SIZE.min + BUBBLE_SIZE.max) / 2;
  return (point) => {
    const z = point?.[2];
    if (!Number.isFinite(z) || max === min) {
      return middle;
    }
    return BUBBLE_SIZE.min + (z - min) / (max - min) * (BUBBLE_SIZE.max - BUBBLE_SIZE.min);
  };
}

/**
 * Build a series of the option
 * @param {object} serie Series of the model
 * @param {object} environment Environment (values, inverted, t, locale, hasTitle)
 * @param {number} index Position of the series in the model, which gives its color
 * @returns {object} Series for ECharts
 */
function buildSeries(serie, environment, index) {
  const {values, inverted, t, locale, hasTitle} = environment;
  const hints = hintsOf(serie);
  const data = bindData(serie, values, inverted);
  const built = {...withoutAwe(serie), data};

  if (serie.name) {
    built.name = translateLabel(serie.name, t);
  }
  if (hints.labelFormat && serie.label) {
    built.label = {
      ...serie.label,
      formatter: (params) => formatTemplate(hints.labelFormat, pointContext(params, inverted), {locale, stripHtml: true})
    };
  }
  if (!isPie(serie)) {
    // The colors are assigned here because the stacks are reversed later, which would swap them
    built.itemStyle = {color: PALETTE[index % PALETTE.length], ...serie.itemStyle};
  }
  if (hints.type === "bubble") {
    built.symbolSize = bubbleSize(data);
    built.itemStyle = {opacity: 0.6, ...built.itemStyle};
  }
  if (serie.type === "bar" && serie.label?.show) {
    built.label = {fontWeight: "bold", fontSize: 11, ...built.label};
  }
  if (serie.type === "line" && data.length > MAX_SYMBOL_POINTS && serie.showSymbol === undefined) {
    built.showSymbol = false;
  }
  if (isPie(serie)) {
    if (serie.radius !== undefined) {
      built.radius = scaleRadius(serie.radius);
    }
    if (hasTitle && serie.center === undefined) {
      built.center = ["50%", "54%"];
    }
  }
  return built;
}

/**
 * Share of a value in the total of its stack
 * @param {number} amount Value
 * @param {number} total Total of the stack
 * @returns {number|null} Percentage, null when the value is not a number
 */
function toPercent(amount, total) {
  if (!Number.isFinite(amount)) {
    return null;
  }
  return total ? amount / total * 100 : 0;
}

/**
 * Turn percent stacked series into percentages of the total of their stack
 * @param {object[]} series Built series with their hints in the model order
 * @param {object[]} modelSeries Series of the model
 * @param {boolean} inverted The chart is inverted
 */
function normalizePercentStacks(series, modelSeries, inverted) {
  const xIndex = inverted ? 1 : 0;
  const yIndex = inverted ? 0 : 1;
  const totals = new Map();
  const percent = series.filter((serie, index) => hintsOf(modelSeries[index]).stackPercent);
  percent.forEach(serie => serie.data.forEach(point => {
    const key = `${serie.stack}|${point[xIndex]}`;
    totals.set(key, (totals.get(key) || 0) + (Number.isFinite(point[yIndex]) ? point[yIndex] : 0));
  }));
  percent.forEach(serie => {
    serie.data = serie.data.map(point => {
      const total = totals.get(`${serie.stack}|${point[xIndex]}`);
      const copy = [...point];
      copy[yIndex] = toPercent(point[yIndex], total);
      return copy;
    });
  });
}

/**
 * A horizontal chart whose bars lie on both sides of zero is a pyramid
 * @param {object[]} bars Built bar series
 * @returns {boolean} The bars are a pyramid
 */
function isPyramid(bars) {
  const amounts = bars.flatMap(serie => serie.data.map(point => point[0])).filter(Number.isFinite);
  return amounts.some(amount => amount < 0) && amounts.some(amount => amount > 0);
}

/**
 * Side of the end of a bar where its label goes, away from zero
 * @param {boolean} negative The bar is negative
 * @param {boolean} inverted The chart is inverted
 * @returns {string} Position of the label
 */
function barLabelPosition(negative, inverted) {
  const [positive, below] = inverted ? ["right", "left"] : ["top", "bottom"];
  return negative ? below : positive;
}

/**
 * The data labels of the bars go outside the end of the bar, away from zero, like in Highcharts (inside when the bars are
 * stacked or form a pyramid). ECharts positions the label of a series as a whole, so the position goes in each point
 * @param {object[]} series Built series
 * @param {boolean} inverted The chart is inverted
 */
function placeBarLabels(series, inverted) {
  const bars = series.filter(serie => serie.type === "bar" && serie.label?.show);
  const pyramid = inverted && isPyramid(bars);
  bars.forEach(serie => {
    if (pyramid) {
      serie.label = {...serie.label, position: "inside", color: "#ffffff"};
      return;
    }
    serie.data = serie.data.map(point => {
      const amount = point[inverted ? 0 : 1];
      const negative = Number.isFinite(amount) && amount < 0;
      return {value: point, label: {position: serie.stack ? "inside" : barLabelPosition(negative, inverted)}};
    });
  });
}

/**
 * Highcharts draws the first series of a stack on top (reversed stacks), ECharts at the bottom: reverse the order of the
 * series that share a stack, leaving the other series where they are
 * @param {object[]} series Series
 * @returns {object[]} Series in the order for ECharts
 */
function reverseStacks(series) {
  const groups = new Map();
  series.forEach((serie, index) => {
    if (serie.stack) {
      groups.set(serie.stack, [...(groups.get(serie.stack) || []), index]);
    }
  });
  const result = [...series];
  groups.forEach(indexes => {
    if (indexes.length > 1) {
      indexes.forEach((index, position) => {
        result[index] = series[indexes[indexes.length - 1 - position]];
      });
    }
  });
  return result;
}

/**
 * Resolve the drilldown of the chart
 * @param {object} model ECharts model
 * @param {{from: string, to: string}} [drill] Drill state
 * @returns {{from: string, serie: object}|null} Series that replaces the one drilled
 */
function resolveDrill(model, drill) {
  const serie = asArray(hintsOf(model).drilldown?.series).find(item => item.id === drill?.to);
  return serie ? {from: drill.from, serie} : null;
}

// ------------------------------------------------------------------------------------------------------------
// Axes
// ------------------------------------------------------------------------------------------------------------

/**
 * ECharts forces zero in value axes, Highcharts does not: let an axis that holds only lines or points fit its data
 * @param {object[]} users Built series that use the axis
 * @returns {boolean} The axis has to fit its data
 */
function fitsData(users) {
  return users.length > 0 && users.every(serie => (serie.type === "line" && !serie.areaStyle) ||
    serie.type === "scatter");
}

/**
 * Build the label formatter of an axis
 * @param {object} hints Hints of the axis
 * @param {object} environment Environment
 * @returns {function|object|undefined} Formatter
 */
function axisFormatter(hints, environment) {
  if (FORMATTERS[hints.formatter]) {
    return FORMATTERS[hints.formatter];
  }
  if (hints.labelFormat) {
    return (value) => formatTemplate(hints.labelFormat, {value}, {locale: environment.locale, stripHtml: true});
  }
  const levels = hints.dateTimeLabelFormats || {};
  const formats = TIME_LEVELS.filter(level => levels[level]).map(level => [level, toEChartsTimeFormat(levels[level])]);
  return formats.length > 0 ? Object.fromEntries(formats) : undefined;
}

const pointOf = (data) => Array.isArray(data) ? data : data?.value;

/**
 * Labels of a horizontal category axis: Highcharts shows every category and rotates the labels when they do not fit,
 * ECharts hides the ones that do not fit
 * @param {object} axis Axis of the model
 * @param {object[]} users Built series that use the axis
 * @param {object} environment Environment
 * @returns {{axisLabel: object, room: number}|null} Labels and the room that they need below the axis
 */
function categoryLabels(axis, users, environment) {
  const names = new Set(users.flatMap(serie => serie.data.map(data => pointOf(data)?.[0]))
    .filter(name => name !== null && name !== undefined).map(String));
  if (names.size === 0 || names.size > MAX_LABELLED_CATEGORIES) {
    return null;
  }
  const label = {interval: 0, fontSize: CATEGORY_LABEL.fontSize, ...axis.axisLabel};
  const longest = Math.max(...[...names].map(name => name.length));
  let room = 0;
  // The width is unknown until the chart is measured, so the labels are not rotated before that
  const crowded = environment.width > 0 && longest * CATEGORY_LABEL.charWidth + CATEGORY_LABEL.labelPadding > environment.width / names.size;
  if (label.rotate === undefined && crowded) {
    label.rotate = CATEGORY_LABEL.rotation;
    room = Math.min(CATEGORY_LABEL.maxRoom, Math.round(longest * CATEGORY_LABEL.rotatedCharHeight));
  }
  return {axisLabel: label, room};
}

/**
 * Room that the data labels of the bars need at the end of a value axis. There is room before zero only when
 * there are negative bars, otherwise the axis would start below zero
 * @param {object[]} users Built series that use the axis
 * @param {boolean} horizontal The axis is the horizontal one
 * @returns {Array|undefined} Boundary gap
 */
function barLabelGap(users, horizontal) {
  const labelled = users.filter(serie => serie.type === "bar" && serie.label?.show && !serie.stack &&
    serie.label.position !== "inside");
  if (labelled.length === 0) {
    return undefined;
  }
  const amountIndex = horizontal ? 0 : 1;
  const negative = labelled.some(serie => serie.data.some(data => pointOf(data)?.[amountIndex] < 0));
  const gap = horizontal ? "18%" : "12%";
  return [negative ? gap : 0, gap];
}

/**
 * Build the axes of one direction
 * @param {object[]} axes Axes of the model
 * @param {string} key xAxis or yAxis
 * @param {object[]} series Built series
 * @param {object} environment Environment
 * @param {{bottom: number}} layout Extra room that the axes need, which the method increases
 * @returns {object[]} Axes for ECharts
 */
function buildAxes(axes, key, series, environment, layout) {
  return axes.map((axis, index) => {
    const users = series.filter(serie => !isPie(serie) && (serie[`${key}Index`] ?? 0) === index);
    const built = withoutAwe(axis);
    if (axis.name) {
      built.name = translateLabel(axis.name, environment.t);
    }
    if (axis.type === "time") {
      // Highcharts tick intervals are in milliseconds, which makes thousands of ticks in ECharts
      delete built.interval;
    }
    const axisLabel = buildAxisLabel(axis, key, users, environment, layout);
    if (Object.keys(axisLabel).length > 0) {
      built.axisLabel = axisLabel;
    }
    if (axis.type === "value") {
      Object.assign(built, valueAxisOptions(axis, key, users, environment));
    }
    return built;
  });
}

/**
 * Build the labels of an axis
 * @param {object} axis Axis of the model
 * @param {string} key xAxis or yAxis
 * @param {object[]} users Built series that use the axis
 * @param {object} environment Environment
 * @param {{bottom: number}} layout Extra room that the axes need, which the method increases
 * @returns {object} Labels
 */
function buildAxisLabel(axis, key, users, environment, layout) {
  const axisLabel = {...axis.axisLabel};
  const formatter = axisFormatter(hintsOf(axis), environment);
  if (formatter) {
    axisLabel.formatter = formatter;
  }
  if (axis.type === "time" || axis.type === "value") {
    axisLabel.hideOverlap = true;
  }
  // The categories are on the horizontal axis (xAxis) unless the chart is inverted
  const categories = axis.type === "category" && key === "xAxis"
    ? categoryLabels({axisLabel}, users, environment) : null;
  if (categories) {
    Object.assign(axisLabel, categories.axisLabel);
    layout.bottom = Math.max(layout.bottom, categories.room);
  }
  return axisLabel;
}

/**
 * Options of a value axis: fit to the data, and room for the data labels of the bars
 * @param {object} axis Axis of the model
 * @param {string} key xAxis or yAxis
 * @param {object[]} users Built series that use the axis
 * @param {object} environment Environment
 * @returns {object} Options to add to the axis
 */
function valueAxisOptions(axis, key, users, environment) {
  const options = {};
  if (axis.scale === undefined && fitsData(users)) {
    options.scale = true;
  }
  // The bars grow along the vertical axis, or along the horizontal one when the chart is inverted
  const horizontal = key === "xAxis";
  const gap = axis.boundaryGap === undefined && horizontal === environment.inverted
    ? barLabelGap(users, horizontal) : undefined;
  if (gap) {
    options.boundaryGap = gap;
  }
  return options;
}

// ------------------------------------------------------------------------------------------------------------
// Tooltip
// ------------------------------------------------------------------------------------------------------------

/**
 * Build the tooltip: the functions that the server cannot send are created from its hints
 * @param {object} tooltip Tooltip of the model
 * @param {object} environment Environment (inverted, locale, timeX)
 * @returns {object} Tooltip for ECharts
 */
function buildTooltip(tooltip, environment) {
  const {inverted, locale, timeX} = environment;
  const hints = hintsOf(tooltip);
  const built = {...withoutAwe(tooltip), confine: true};
  const {numberDecimals, prefix = "", suffix = "", pointFormat, dateFormat} = hints;
  const hasValueFormat = numberDecimals !== undefined || prefix !== "" || suffix !== "";
  const valueText = (value) => {
    if (value === null || value === undefined || value === "") {
      return NO_VALUE;
    }
    const text = numberDecimals === undefined ? String(value) : formatNumber(value, numberDecimals, locale);
    return `${prefix}${text}${suffix}`;
  };

  if (hasValueFormat) {
    built.valueFormatter = valueText;
  }
  if (pointFormat || (dateFormat && timeX)) {
    built.formatter = (params) => {
      const list = Array.isArray(params) ? params : [params];
      const points = list.map(item => ({item, context: pointContext(item, inverted)}));
      const cartesian = Array.isArray(list[0]?.value);
      let header = "";
      if (cartesian) {
        header = dateFormat && timeX
          ? formatDate(points[0].context.x, dateFormat, locale)
          : list[0].axisValueLabel ?? list[0].name ?? "";
      }
      const rows = points.map(({item, context}) => pointFormat
        ? `${item.marker ?? ""}${formatTemplate(pointFormat, context, {locale})}`
        : `${item.marker ?? ""}${escapeHtml(item.seriesName ?? item.name)}: ${valueText(context.y)}`);
      return [header ? escapeHtml(header) : null, ...rows].filter(row => row !== null).join("<br/>");
    };
  }
  return built;
}

// ------------------------------------------------------------------------------------------------------------
// Layout
// ------------------------------------------------------------------------------------------------------------

/**
 * Where a legend is drawn
 * @param {object} legend Legend of the model
 * @returns {string} bottom, top, left, right or none
 */
function legendSide(legend) {
  if (!legend || legend.show === false) {
    return "none";
  }
  if (legend.orient === "vertical") {
    return legend.left === "left" ? "left" : "right";
  }
  return legend.top === "bottom" ? "bottom" : "top";
}

/**
 * Margins of the plot area. The server does not send the grid, but the title, the legend and the slider need room
 * @param {object} model ECharts model
 * @param {string} legendPlace Side of the legend
 * @param {boolean} hasLegendTitle The legend has a title
 * @param {number} extraBottom Room that the labels of the axes need below the plot
 * @returns {object} Grid
 */
function buildGrid(model, legendPlace, hasLegendTitle, extraBottom) {
  const margin = {top: LAYOUT.margin, right: LAYOUT.margin, bottom: LAYOUT.margin, left: LAYOUT.margin};
  if (model.title?.text) {
    margin.top += LAYOUT.title;
  }
  if (model.title?.subtext) {
    margin.top += LAYOUT.subtitle;
  }
  if (legendPlace === "top" || legendPlace === "bottom") {
    margin[legendPlace] += LAYOUT.legend + (hasLegendTitle ? LAYOUT.legendTitle : 0);
  } else if (legendPlace !== "none") {
    margin[legendPlace] += LAYOUT.verticalLegend;
  }
  if (asArray(model.dataZoom).some(zoom => zoom.type === "slider")) {
    margin.bottom += LAYOUT.slider;
  }
  margin.bottom += extraBottom;
  return {...margin, outerBoundsMode: "same"};
}

/**
 * Text drawn on the chart
 * @param {string} text Text
 * @param {object} position Position
 * @param {object} environment Environment
 * @param {object} [extra] Extra properties of the element
 * @returns {object} Graphic element
 */
function graphicText(text, position, environment, extra = {}) {
  return {
    type: "text",
    ...position,
    style: {text, fontSize: 14, fill: environment.dark ? "#dddddd" : "#555555"},
    silent: !extra.onclick,
    ...extra
  };
}

/**
 * Position of the title of the legend, next to the legend
 * @param {string} place Side of the legend
 * @param {number} height Height of the chart
 * @returns {object} Position
 */
function legendTitlePosition(place, height) {
  switch (place) {
    case "bottom":
      return {left: "center", bottom: LAYOUT.legend};
    case "top":
      return {right: LAYOUT.margin, top: LAYOUT.title + LAYOUT.margin};
    default:
      return {[place]: LAYOUT.margin, top: Math.max(height / 2 - 70, LAYOUT.margin)};
  }
}

// ------------------------------------------------------------------------------------------------------------
// Option
// ------------------------------------------------------------------------------------------------------------

/**
 * Build the title
 * @param {object} title Title of the model
 * @param {object} environment Environment
 * @returns {object} Title for ECharts
 */
function buildTitle(title, environment) {
  const built = withoutAwe(title);
  if (title.text) {
    built.text = translateLabel(title.text, environment.t);
  }
  if (title.subtext) {
    built.subtext = translateLabel(title.subtext, environment.t);
  }
  const offsetY = hintsOf(title).offsetY;
  if (title.top === "middle" && Number.isFinite(offsetY) && environment.height > 0) {
    built.top = environment.height / 2 + offsetY - LAYOUT.titleOffset;
  }
  return built;
}

/**
 * Build all the series of the chart, with their data, labels and colors
 * @param {object[]} modelSeries Series of the model
 * @param {object} environment Environment
 * @returns {object[]} Series for ECharts, in the order of the model
 */
function buildAllSeries(modelSeries, environment) {
  // Like Highcharts, only the series without a color of their own advance the palette
  let colored = 0;
  const built = modelSeries.map(serie => {
    const index = colored;
    if (!isPie(serie) && !serie.itemStyle?.color) {
      colored += 1;
    }
    return buildSeries(serie, environment, index);
  });
  normalizePercentStacks(built, modelSeries, environment.inverted);
  placeBarLabels(built, environment.inverted);
  return built;
}

/**
 * Build the legend. The series order was changed for the stacks, so the legend keeps the order of the model; the
 * legend of a pie lists its slices
 * @param {object} legend Legend of the model
 * @param {object[]} modelSeries Series of the model
 * @param {boolean} cartesian The chart has axes
 * @param {function} t Translator
 * @returns {object} Legend for ECharts
 */
function buildLegend(legend, modelSeries, cartesian, t) {
  const built = withoutAwe(legend);
  const names = modelSeries.map(serie => serie.name && translateLabel(serie.name, t));
  if (cartesian && !modelSeries.some(isPie) && names.length > 0 && names.every(Boolean)) {
    built.data = names;
  }
  return built;
}

/**
 * Build the control that goes back from a drilldown series
 * @param {object} model ECharts model
 * @param {{from: string}} drill Drill state
 * @param {object} environment Environment
 * @returns {object} Graphic element
 */
function drillBackControl(model, drill, environment) {
  const original = asArray(model.series).find(serie => serie.id === drill.from);
  const name = translateLabel(original?.name || "", environment.t);
  const text = (environment.locale?.drillUpText || "{series.name}").replace("{series.name}", name);
  return graphicText(text, {left: LAYOUT.margin, top: LAYOUT.margin}, environment,
    {onclick: () => environment.onBack?.(), cursor: "pointer"});
}

/**
 * Context to build the chart
 * @typedef {object} ChartContext
 * @property {function} t Translator
 * @property {object} locale Locale: decimalPoint, thousandsSep, noData and drillUpText
 * @property {number} width Width of the chart in pixels
 * @property {number} height Height of the chart in pixels
 * @property {{from: string, to: string}} [drill] Series that is replaced by a drilldown series
 * @property {function} [onBack] Called when the back control of the drilldown is clicked
 * @property {boolean} [dark] The chart is drawn in dark mode
 */

/**
 * Check if the server sent no model
 * @param {*} model Model
 * @returns {boolean} The model is missing or empty
 */
function isEmptyModel(model) {
  return !model || typeof model !== "object" || Object.keys(model).length === 0;
}

/**
 * Build the axes and the zoom of a chart
 * @param {object} model ECharts model
 * @param {object[]} series Built series
 * @param {object} environment Environment
 * @param {{bottom: number}} layout Extra room that the axes need, which is increased
 * @returns {object} xAxis, yAxis and dataZoom, when the model has them
 */
function buildAxesOptions(model, series, environment, layout) {
  const options = {};
  const xAxis = asArray(model.xAxis);
  const yAxis = asArray(model.yAxis);
  if (xAxis.length > 0) {
    options.xAxis = buildAxes(xAxis, "xAxis", series, environment, layout);
  }
  if (yAxis.length > 0) {
    options.yAxis = buildAxes(yAxis, "yAxis", series, environment, layout);
  }
  if (model.dataZoom) {
    options.dataZoom = stripAwe(model.dataZoom);
  }
  return options;
}

/**
 * Check if the horizontal axis (the AWE x axis) is a time axis
 * @param {object} model ECharts model
 * @returns {boolean} The x axis shows dates
 */
function hasTimeXAxis(model) {
  const axes = [...asArray(model.xAxis), ...asArray(model.yAxis)];
  return axes.find(axis => hintsOf(axis).axis === "x")?.type === "time";
}

/**
 * Build the ECharts option
 * @param {object} model `echartsModel` sent by the server
 * @param {object[]} values Values of the component
 * @param {ChartContext} context Context
 * @returns {object} ECharts option
 */
export function buildEChartsOption(model, values, context) {
  const environment = {inverted: false, hasTitle: false, ...context, values};
  const locale = environment.locale || {};
  const noData = () => graphicText(locale.noData, {left: "center", top: "middle"}, environment);

  if (isEmptyModel(model)) {
    console.warn("[WARNING] The chart has no echartsModel (the server could not translate it), it is shown empty");
    return {backgroundColor: TRANSPARENT, series: [], graphic: [noData()]};
  }

  environment.inverted = Boolean(hintsOf(model).inverted);
  environment.hasTitle = Boolean(model.title?.text);
  const {t, inverted} = environment;

  // Series (a drilldown series replaces the one that was drilled)
  const drill = resolveDrill(model, environment.drill);
  const modelSeries = asArray(model.series).map(serie => drill?.from === serie.id ? drill.serie : serie);
  const built = buildAllSeries(modelSeries, environment);
  const series = reverseStacks(built);
  const layout = {bottom: 0};
  const option = {
    backgroundColor: TRANSPARENT, color: PALETTE, aria: {enabled: true}, series,
    ...buildAxesOptions(model, series, environment, layout)
  };
  const cartesian = Boolean(option.xAxis || option.yAxis);
  const graphic = [];

  if (model.title) {
    option.title = buildTitle(model.title, environment);
  }
  if (model.tooltip) {
    option.tooltip = buildTooltip(model.tooltip, {inverted, locale, timeX: hasTimeXAxis(model)});
  }
  if (model.legend) {
    option.legend = buildLegend(model.legend, modelSeries, cartesian, t);
  }

  const legendPlace = legendSide(model.legend);
  const legendTitle = hintsOf(model.legend).title;
  if (legendTitle && legendPlace !== "none") {
    graphic.push(graphicText(translateLabel(legendTitle, t), legendTitlePosition(legendPlace, environment.height),
      environment));
  }
  if (cartesian) {
    option.grid = buildGrid(model, legendPlace, graphic.length > 0, layout.bottom);
  }
  if (drill) {
    graphic.push(drillBackControl(model, drill, environment));
  }
  if (built.every(serie => serie.data.length === 0)) {
    graphic.push(noData());
  }
  if (graphic.length > 0) {
    option.graphic = graphic;
  }
  return option;
}
