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

/** A value of a saved search: the value of a criterion and its label (the text a suggest or a select shows) */
export interface SavedOption {
  value?: unknown;
  label?: unknown;
}

/** Values of a saved search by criterion id */
export type SavedValues = Record<string, SavedOption[]>;

/** Comparable selection of saved values */
const savedKey = (options: SavedOption[]): string =>
  selectionKey({values: options.map(option => ({...option, selected: true}))});

/** A criterion whose values are its options (a select or a checkbox): a saved search selects them */
const hasOptions = (component: StateComponent): boolean => {
  const {component: type} = (component.attributes || {}) as {component?: string};
  return type === ComponentType.COMPONENT_SELECT || type === ComponentType.COMPONENT_SELECT_MULTIPLE ||
    type === ComponentType.COMPONENT_CHECKBOX;
};

/**
 * Values of the criteria of a window to save as a search (saved searches, #790): the selected values of each criterion,
 * with their labels (an empty list when it has none)
 * @param {object} components Components of the state, by id
 * @param {string[]} ids Search criteria of the window
 * @return {object} Values by criterion id
 */
export function getSearchValues(components: Record<string, StateComponent | undefined>, ids: string[]): SavedValues {
  return Object.fromEntries(ids
    .filter(id => components[id]?.model)
    .map(id => [id, getSelectedOptions(components[id]!.model as {values?: FilterOption[]})
      .map(({value, label}) => isEmpty(label) ? {value} : {value, label})]));
}

/**
 * Read the values of a saved search (the JSON the server keeps). Values that cannot be read are no values.
 * @param {string} json Values of the search
 * @return {object} Values by criterion id
 */
export function parseSearchValues(json: unknown): SavedValues {
  try {
    const values = typeof json === "string" ? JSON.parse(json) : json;
    if (!values || typeof values !== "object" || Array.isArray(values)) {
      return {};
    }
    return Object.fromEntries(Object.entries(values as Record<string, unknown>)
      .filter(([, options]) => Array.isArray(options)) as [string, SavedOption[]][]);
  } catch {
    return {};
  }
}

/**
 * Model values of a criterion with the values of a saved search: a select or a checkbox selects its options (a saved
 * value that is not among them is added with its label); any other criterion takes the saved values with their labels
 * @param {object} component Criterion of the state
 * @param {object[]} options Saved values of the criterion
 * @return {object[]} Model values
 */
export function getSavedModelValues(component: StateComponent, options: SavedOption[]): FilterOption[] {
  const selected = options.map(option => ({...option, selected: true}));
  if (!hasOptions(component)) {
    return selected;
  }
  const keys = options.map(option => String(option.value));
  const current = ((component.model as {values?: FilterOption[]})?.values || [])
    .map(option => ({...option, selected: keys.includes(String(option.value))}));
  const missing = selected.filter(option => !current.some(value => String(value.value) === String(option.value)));
  return [...current, ...missing];
}

/**
 * Check whether the criteria of a window keep the values of a saved search: the ones it carries have its values and
 * the others their defaults
 * @param {object} components Criteria of the state, by id
 * @param {string[]} ids Search criteria of the window
 * @param {object} values Values of the saved search
 * @return {boolean} The search is still in effect
 */
export function isSavedSearchInEffect(components: Record<string, StateComponent | undefined>, ids: string[],
                                      values: SavedValues): boolean {
  return ids.every(id => {
    const component = components[id];
    if (!component?.model) {
      return true;
    }
    const expected = id in values ? savedKey(values[id]) :
      selectionKey(getRestoredModel(component, false) as {values?: FilterOption[]});
    return selectionKey(component.model as {values?: FilterOption[]}) === expected;
  });
}
