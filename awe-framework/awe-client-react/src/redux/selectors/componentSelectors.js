import { createSelector } from 'reselect';
import ComponentRegistry from '../registry/ComponentRegistry';
import { mergeComponentState } from '../../utilities/mergeUtils';

/**
 * Selector base: obtiene deltas de un componente específico
 */
const getComponentDeltas = (state, componentId) =>
    state.components[componentId] || {};

/**
 * Selector base: obtiene todos los deltas
 */
const getAllComponentDeltas = (state) => state.components;

/**
 * Selector base: obtiene settings (para feature flag)
 */
const getSettings = (state) => state.settings || {};

/**
 * Factory de selector para un componente individual
 * Crea un selector memoizado que mergea base + deltas
 */
export const makeGetComponent = () => createSelector(
    [getComponentDeltas, getSettings, (_, componentId) => componentId],
    (deltas, settings, componentId) => {
        // Feature flag: modo legacy
        if (!settings.useComponentRegistry) {
            return deltas;
        }

        // Modo nuevo: mergear base + deltas
        const base = ComponentRegistry.get(componentId) || {};
        return mergeComponentState(base, deltas);
    }
);

/**
 * Selector para obtener TODOS los componentes mergeados
 * Usado por useGrid y otros casos que necesitan acceso masivo
 */
export const getAllComponents = createSelector(
    [getAllComponentDeltas, getSettings],
    (deltas, settings) => {
        // Feature flag: modo legacy
        if (!settings.useComponentRegistry) {
            return deltas;
        }

        // Obtener todos los IDs únicos (de Registry + Redux)
        const registryIds = ComponentRegistry.getAllIds();
        const deltaIds = Object.keys(deltas);
        const allIds = [...new Set([...registryIds, ...deltaIds])];

        // Mergear cada componente
        return allIds.reduce((acc, componentId) => {
            const base = ComponentRegistry.get(componentId) || {};
            const delta = deltas[componentId] || {};
            acc[componentId] = mergeComponentState(base, delta);
            return acc;
        }, {});
    }
);

/**
 * Memoizes the merge of base + deltas of every component: while neither the base nor the delta of a component change
 * it returns the same object, so whoever uses it does not render again when other components change
 *
 * @returns {Function} (componentId, delta) => merged component
 */
const makeMergeCache = () => {
    const cache = new Map();
    return (componentId, delta) => {
        // Compare the registered base itself: a component that is not registered has no base
        const base = ComponentRegistry.get(componentId);
        const cached = cache.get(componentId);
        if (cached && cached.base === base && cached.delta === delta) {
            return cached.value;
        }
        const value = mergeComponentState(base || {}, delta || {});
        cache.set(componentId, { base, delta, value });
        return value;
    };
};

/**
 * Selector to get several specific components when not all of them are needed. The result is a new object when any
 * component changes, but every component keeps its reference while it does not change: use it with shallowEqual
 *
 * @param {string[]} componentIds - Component identifiers
 * @returns {Function} Memoized selector
 */
export const makeGetMultipleComponents = (componentIds) => {
    const merge = makeMergeCache();
    return createSelector(
        [getAllComponentDeltas, getSettings],
        (deltas, settings) => componentIds.reduce((acc, id) => {
            acc[id] = settings.useComponentRegistry ? merge(id, deltas[id]) : deltas[id];
            return acc;
        }, {})
    );
};

const mergeContextMenuComponent = makeMergeCache();

/**
 * Gets the attributes of the options of a context menu and of their nested options, indexed by identifier.
 * Every attributes object keeps its reference while its component does not change: use it with shallowEqual so the
 * menu only renders again when one of its options changes
 *
 * @param {object} state - Redux state
 * @param {object[]} contextMenu - Context menu options ({id})
 * @param {object} [collected] - Attributes collected so far
 * @returns {Object} Attributes by option identifier
 */
export const getContextMenuAttributes = (state, contextMenu, collected = {}) => {
    const deltas = getAllComponentDeltas(state);
    const useRegistry = getSettings(state).useComponentRegistry;
    (contextMenu || []).forEach(({ id }) => {
        const component = useRegistry ? mergeContextMenuComponent(id, deltas[id]) : deltas[id];
        if (component?.attributes && !(id in collected)) {
            collected[id] = component.attributes;
            getContextMenuAttributes(state, component.attributes.contextMenu, collected);
        }
    });
    return collected;
};
