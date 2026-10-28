import {translateLabel} from "./labels";
import {ModelNode, ModelSeries, hintsOf, isPie, withoutAwe} from "./echartsContext";

/**
 * Legend that was built for ECharts
 */
export interface BuiltLegend {
  [key: string]: unknown;
  show?: boolean;
  data?: string[];
}

type Translator = (key: string) => string;

const isNode = (value: unknown): value is ModelNode => Boolean(value) && typeof value === "object";

/**
 * Position of the series that a series is linked to (`linkedTo`: `:previous` or the id of an earlier series)
 * @param {object[]} modelSeries Series of the model
 * @param {number} index Position of the series
 * @returns {number} Position of the linked series, -1 when the series is not linked to an earlier one
 */
function linkedIndex(modelSeries: ModelSeries[], index: number): number {
  const {linkedTo} = hintsOf(modelSeries[index]);
  if (!linkedTo) {
    return -1;
  }
  const target = linkedTo === ":previous" ? index - 1 : modelSeries.findIndex(serie => serie.id === linkedTo);
  return target >= 0 && target < index ? target : -1;
}

/**
 * Names of the series that the chart shows. A series that is linked to another one and does not react to the mouse (it
 * is not in the tooltip, like the hidden helper series of a pyramid) takes the name of the other, so that ECharts toggles
 * both from the same legend entry. A linked series that shows in the tooltip keeps its own name, which the tooltip and
 * the formats need; it is left out of the legend but is not toggled with the other
 * @param {object[]} modelSeries Series of the model
 * @param {function} t Translator
 * @returns {Array<string|undefined>} Name of each series
 */
export function seriesNames(modelSeries: ModelSeries[], t: Translator): Array<string | undefined> {
  const names: Array<string | undefined> = [];
  modelSeries.forEach((serie, index) => {
    const linked = linkedIndex(modelSeries, index);
    names.push(linked >= 0 && serie.silent ? names[linked] : serie.name && translateLabel(serie.name, t));
  });
  return names;
}

/**
 * Build the legend. The series order was changed for the stacks, so the legend keeps the order of the model, or the
 * one of `legendIndex`; the series that are not in the legend (`showInLegend`, or linked to another) are left out;
 * the legend of a pie lists its slices
 * @param {object} legend Legend of the model
 * @param {object[]} modelSeries Series of the model
 * @param {boolean} cartesian The chart has axes
 * @param {function} t Translator
 * @param {object} defaults Properties that the model does not set
 * @returns {object} Legend for ECharts
 */
function buildLegend(legend: ModelNode, modelSeries: ModelSeries[], cartesian: boolean, t: Translator, defaults: ModelNode): BuiltLegend {
  const built: BuiltLegend = {...defaults, ...withoutAwe(legend)};
  if (isNode(defaults.textStyle) && isNode(built.textStyle)) {
    built.textStyle = {...defaults.textStyle, ...built.textStyle};
  }
  if (!cartesian || modelSeries.length === 0 || modelSeries.some(isPie)) {
    return built;
  }
  const names = seriesNames(modelSeries, t);
  const listed = modelSeries
    .map((serie, index) => ({index, name: names[index], order: hintsOf(serie).legendIndex ?? index}))
    .filter(({index}) => hintsOf(modelSeries[index]).showInLegend !== false && linkedIndex(modelSeries, index) < 0)
    .sort((left, right) => left.order - right.order || left.index - right.index);
  if (!listed.every(({name}) => name)) {
    return built;
  }
  built.data = [...new Set(listed.map(({name}) => name as string))];
  if (built.data.length === 0) {
    built.show = false;
  }
  return built;
}

/**
 * Names of the series that are linked to a series and show in the tooltip, so that they keep their own name and do not
 * toggle with it by name
 * @param {object[]} modelSeries Series of the model
 * @param {string} name Name of the series that is not the linked one
 * @param {function} t Translator
 * @returns {string[]} Names of the series that are linked to the one with that name
 */
export function linkedNames(modelSeries: ModelSeries[], name: string, t: Translator): string[] {
  const names = seriesNames(modelSeries, t);
  return (names as string[]).filter((own, index) => {
    const target = linkedIndex(modelSeries, index);
    return target >= 0 && !modelSeries[index].silent && names[target] === name;
  });
}

/**
 * Build the legend, with a hidden second one for the linked series that show in the tooltip: the chart toggles them with
 * the series they are linked to (ECharts only toggles the names that a legend lists)
 * @param {object} legend Legend of the model
 * @param {object[]} modelSeries Series of the model
 * @param {boolean} cartesian The chart has axes
 * @param {function} t Translator
 * @param {object} [defaults] Properties of the legend that the model does not set (the sizes of a small chart)
 * @returns {object|object[]} Legend for ECharts, or the visible one and the hidden one
 */
export function buildLegends(legend: ModelNode, modelSeries: ModelSeries[], cartesian: boolean, t: Translator, defaults: ModelNode = {}): BuiltLegend | BuiltLegend[] {
  const built = buildLegend(legend, modelSeries, cartesian, t, defaults);
  if (!cartesian || built.show === false) {
    return built;
  }
  const names = seriesNames(modelSeries, t);
  const linked = (names as string[]).filter((name, index) => name && linkedIndex(modelSeries, index) >= 0 && !modelSeries[index].silent);
  return linked.length > 0 ? [built, {show: false, data: linked}] : built;
}
