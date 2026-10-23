import { createSelector } from '@reduxjs/toolkit';
import ComponentRegistry from '../registry/ComponentRegistry';
import { mergeComponentState } from '../../utilities/mergeUtils';

// Input selectors must return the same reference for the same state: a new empty object would break the memoization
const EMPTY = {};

/**
 * Selector base: obtiene deltas de un componente específico
 */
const getComponentDeltas = (state, componentId) =>
    state.components[componentId] || EMPTY;

/**
 * Selector base: obtiene todos los deltas
 */
const getAllComponentDeltas = (state) => state.components;

/**
 * Factory de selector para un componente individual
 * Crea un selector memoizado que mergea base + deltas
 */
export const makeGetComponent = () => createSelector(
    [getComponentDeltas, (_, componentId) => componentId],
    (deltas, componentId) => {
        // Mergear base + deltas
        const base = ComponentRegistry.get(componentId) || {};
        return mergeComponentState(base, deltas);
    }
);

/**
 * Selector para obtener TODOS los componentes mergeados
 * Usado por useGrid y otros casos que necesitan acceso masivo
 */
export const getAllComponents = createSelector(
    [getAllComponentDeltas],
    (deltas) => {
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
        [getAllComponentDeltas],
        (deltas) => componentIds.reduce((acc, id) => {
            acc[id] = merge(id, deltas[id]);
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
    (contextMenu || []).forEach(({ id }) => {
        const component = mergeContextMenuComponent(id, deltas[id]);
        if (component?.attributes && !(id in collected)) {
            collected[id] = component.attributes;
            getContextMenuAttributes(state, component.attributes.contextMenu, collected);
        }
    });
    return collected;
};
