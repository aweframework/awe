import _ from 'lodash';

// Sections of a component that are merged key by key (the rest of the component comes from the base)
const MERGED_SECTIONS = [
    'attributes', 'model', 'validationRules', 'storedModel', 'defaultModel', 'storedAttributes',
    'storedValidationRules', 'specificAttributes'
];

/**
 * Copies a component one level deep: a new component whose sections are new objects too, so a caller can replace
 * or assign the sections of the result. Everything below the sections (rows, columns, ...) is shared, never cloned
 *
 * @param {Object} component - Component to copy
 * @returns {Object} Component with new sections
 */
const copySections = (component) => {
    const copy = { ...component };
    MERGED_SECTIONS.forEach(section => {
        if (copy[section] && typeof copy[section] === 'object' && !Array.isArray(copy[section])) {
            copy[section] = { ...copy[section] };
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
export function mergeComponentState(base, deltas = {}) {
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
export function calculateDeltas(base, current) {
    const deltas = {};

    const addDeltaIfChanged = (key, value) => {
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
function diffObject(base, current) {
    const diff = {};
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
