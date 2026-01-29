import _ from 'lodash';

/**
 * Mergea el estado base con los deltas
 * Los deltas siempre tienen prioridad sobre el base
 * 
 * @param {Object} base - Estado base del ComponentRegistry
 * @param {Object} deltas - Cambios dinámicos de Redux
 * @returns {Object} Estado mergeado
 */
export function mergeComponentState(base, deltas) {
    // Si no hay base, retornar solo deltas
    if (!base || Object.keys(base).length === 0) {
        return _.cloneDeep(deltas || {});
    }

    // Si no hay deltas, retornar solo base
    if (!deltas || Object.keys(deltas).length === 0) {
        return _.cloneDeep(base);
    }

    return {
        ...base,
        attributes: { ...base.attributes, ...(deltas.attributes || {}) },
        model: { ...base.model, ...(deltas.model || {}) },
        validationRules: { ...base.validationRules, ...(deltas.validationRules || {}) },
        storedModel: { ...base.storedModel, ...(deltas.storedModel || {}) },
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
