import {translateLabel} from "./index";
import {hintsOf, isPie, withoutAwe} from "./echartsContext";

/**
 * Position of the series that a series is linked to (`linkedTo`: `:previous` or the id of an earlier series)
 * @param {object[]} modelSeries Series of the model
 * @param {number} index Position of the series
 * @returns {number} Position of the linked series, -1 when the series is not linked to an earlier one
 */
function linkedIndex(modelSeries, index) {
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
export function seriesNames(modelSeries, t) {
  const names = [];
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
 * @returns {object} Legend for ECharts
 */
function buildLegend(legend, modelSeries, cartesian, t) {
  const built = withoutAwe(legend);
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
  built.data = [...new Set(listed.map(({name}) => name))];
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
export function linkedNames(modelSeries, name, t) {
  const names = seriesNames(modelSeries, t);
  return names.filter((own, index) => {
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
 * @returns {object|object[]} Legend for ECharts, or the visible one and the hidden one
 */
export function buildLegends(legend, modelSeries, cartesian, t) {
  const built = buildLegend(legend, modelSeries, cartesian, t);
  if (!cartesian || built.show === false) {
    return built;
  }
  const names = seriesNames(modelSeries, t);
  const linked = names.filter((name, index) => name && linkedIndex(modelSeries, index) >= 0 && !modelSeries[index].silent);
  return linked.length > 0 ? [built, {show: false, data: linked}] : built;
}
