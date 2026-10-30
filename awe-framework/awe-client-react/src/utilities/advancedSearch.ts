/**
 * Advanced search (#790): the filters in effect in a window. A filter is in effect when the value of its criterion
 * differs from its default, the value the "restore" action brings it back to.
 */
import {getRestoredModel} from "../redux/reducers/components/model";
import type {StateComponent} from "../redux/reducers/components/stateHelpers";
import {ComponentType, isEmpty} from "./general";
import {translateLabel} from "./labels";

type Translate = (key: string) => string;

interface FilterOption {
  value?: unknown;
  label?: unknown;
  selected?: boolean;
}

/** Selected values of a model, without the empty ones (an emptied text keeps an empty value) */
const getSelectedOptions = (model?: {values?: FilterOption[]}): FilterOption[] =>
  (model?.values || []).filter(option => option?.selected && !isEmpty(option.value));

/** Comparable selection of a model: the order of a multiple selection does not matter */
const selectionKey = (model?: {values?: FilterOption[]}): string =>
  JSON.stringify(getSelectedOptions(model).map(option => String(option.value)).sort((a, b) => a.localeCompare(b)));

/**
 * Check whether the value of a criterion differs from its default
 * @param {object} component Criterion of the state
 * @return {boolean} The criterion is a filter in effect
 */
export function isFilterInEffect(component?: StateComponent | null): boolean {
  if (!component?.model) {
    return false;
  }
  return selectionKey(component.model as {values?: FilterOption[]}) !==
    selectionKey(getRestoredModel(component, false) as {values?: FilterOption[]});
}

/**
 * Label of a criterion as a filter: its label, or its placeholder or its id when it has none
 * @param {object} component Criterion of the state
 * @param {function} t Translate function
 * @return {string} Label
 */
export function getFilterLabel(component: StateComponent, t: Translate): string {
  const {label, placeholder} = (component.attributes || {}) as {label?: string; placeholder?: string};
  return translateLabel(label || placeholder || component.address?.component || "", t);
}

/**
 * Parts of the chip of a filter in effect: "Label: value". A checkbox is just its label (null value), a selector shows
 * the label of its option, a date range its two dates and a multiple criterion its first value and the number of the
 * others (more: " +2")
 * @param {object} component Criterion of the state
 * @param {function} t Translate function
 * @return {{label: string, value: string|null, more: string}} Chip parts
 */
export function getFilterChip(component: StateComponent, t: Translate): {label: string; value: string | null; more: string} {
  const label = getFilterLabel(component, t);
  const {component: type} = (component.attributes || {}) as {component?: string};
  if (type === ComponentType.COMPONENT_CHECKBOX) {
    return {label, value: null, more: ""};
  }
  const texts = getSelectedOptions(component.model as {values?: FilterOption[]})
    .map(option => translateLabel(String(option.label ?? option.value), t));
  if (type === ComponentType.COMPONENT_DATE_RANGE) {
    return {label, value: texts.join(" - "), more: ""};
  }
  const [first = "", ...others] = texts;
  return {label, value: first, more: others.length ? ` +${others.length}` : ""};
}
