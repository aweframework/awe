import _ from 'lodash';
import type {ComponentAddress} from '../contract';

/**
 * Part of the state of a component: a plain object by key
 */
type StatePart = Record<string, unknown>;

/**
 * State of a component in the ComponentRegistry (base) or in Redux (deltas). Every part is optional because a delta
 * only has the parts that changed
 */
export interface ComponentState {
  address?: ComponentAddress;
  context?: {view?: string; source?: string[]};
  attributes?: StatePart;
  model?: StatePart;
  validationRules?: StatePart;
  storedModel?: StatePart;
  defaultModel?: StatePart;
  storedAttributes?: StatePart;
  storedValidationRules?: StatePart;
  specificAttributes?: StatePart;
  [key: string]: unknown;
}

type StatePartKey = "model" | "storedModel" | "defaultModel" | "storedAttributes" | "storedValidationRules" | "specificAttributes";

// Sections of a component that are merged key by key (the rest of the component comes from the base)
const MERGED_SECTIONS = [
    'attributes', 'model', 'validationRules', 'storedModel', 'defaultModel', 'storedAttributes',
    'storedValidationRules', 'specificAttributes'
] as const;

/**
 * Copies a component one level deep: a new component whose sections are new objects too, so a caller can replace
 * or assign the sections of the result. Everything below the sections (rows, columns, ...) is shared, never cloned
 *
 * @param {Object} component - Component to copy
 * @returns {Object} Component with new sections
 */
const copySections = (component: ComponentState): ComponentState => {
    const copy: ComponentState = { ...component };
    MERGED_SECTIONS.forEach(section => {
        const value = copy[section];
        if (value && typeof value === 'object' && !Array.isArray(value)) {
            copy[section] = { ...value };
        }
    });
    return copy;
};

/**
 * Merges the base state with the deltas, deltas always win.
 * The result is a new component but it is never a deep clone: whatever lives below the sections (model values,
 * column model, ...) is shared with the base and the deltas, so callers must replace it, not mutate it
 *
 * @param {Object} base - Base state (ComponentRegistry)
 * @param {Object} deltas - Dynamic changes (Redux)
 * @returns {Object} Merged state
 */
export function mergeComponentState(base?: ComponentState | null, deltas: ComponentState | null = {}): ComponentState {
    // Without base there are only deltas
    if (!base || Object.keys(base).length === 0) {
        return copySections(deltas || {});
    }

    // Without deltas there is only the base
    if (!deltas || Object.keys(deltas).length === 0) {
        return copySections(base);
    }

    return {
        ...base,
        attributes: { ...base.attributes, ...(deltas.attributes || {}) },
        model: { ...base.model, ...(deltas.model || {}) },
        validationRules: { ...base.validationRules, ...(deltas.validationRules || {}) },
        storedModel: { ...base.storedModel, ...(deltas.storedModel || {}) },
        defaultModel: { ...base.defaultModel, ...(deltas.defaultModel || {}) },
        storedAttributes: { ...base.storedAttributes, ...(deltas.storedAttributes || {}) },
        storedValidationRules: { ...base.storedValidationRules, ...(deltas.storedValidationRules || {}) },
        specificAttributes: { ...base.specificAttributes, ...(deltas.specificAttributes || {}) }
    };
}

/**
 * Calcula los deltas comparando estado actual con base
 * Solo retorna las propiedades que difieren
 * 
 * @param {Object} base - Estado base
 * @param {Object} current - Estado actual
 * @returns {Object} Solo los cambios
 */
export function calculateDeltas(base: ComponentState | null | undefined, current: ComponentState): ComponentState {
    const deltas: ComponentState = {};

    const addDeltaIfChanged = (key: StatePartKey, value?: StatePart): void => {
        if (value && !_.isEqual(base?.[key], value)) {
            deltas[key] = value;
        }
    };

    if (current.attributes && !_.isEqual(base?.attributes, current.attributes)) {
        const attributesDelta = diffObject(base?.attributes, current.attributes);
        if (!_.isEqual(base?.attributes?.columnModel, current.attributes.columnModel)) {
            attributesDelta.columnModel = current.attributes.columnModel;
        }
        deltas.attributes = attributesDelta;
    }

    addDeltaIfChanged('model', current.model);

    if (current.validationRules && !_.isEqual(base?.validationRules, current.validationRules)) {
        deltas.validationRules = diffObject(base?.validationRules, current.validationRules);
    }

    addDeltaIfChanged('storedModel', current.storedModel);
    addDeltaIfChanged('defaultModel', current.defaultModel);
    addDeltaIfChanged('storedAttributes', current.storedAttributes);
    addDeltaIfChanged('storedValidationRules', current.storedValidationRules);
    addDeltaIfChanged('specificAttributes', current.specificAttributes);

    return deltas;
}

/**
 * Calcula la diferencia entre dos objetos
 * Solo retorna las propiedades que son diferentes
 * 
 * @param {Object} base - Objeto base
 * @param {Object} current - Objeto actual
 * @returns {Object} Solo las diferencias
 */
function diffObject(base?: StatePart, current?: StatePart): StatePart {
    const diff: StatePart = {};
    const baseObj = base || {};
    const currentObj = current || {};

    // Cambios y adiciones
    Object.keys(currentObj).forEach(key => {
        if (!_.isEqual(baseObj[key], currentObj[key])) {
            diff[key] = currentObj[key];
        }
    });

    // Eliminaciones (marcar como undefined para que el merge las sobrescriba)
    Object.keys(baseObj).forEach(key => {
        if (!(key in currentObj)) {
            diff[key] = undefined;
        }
    });

    return diff;
}
