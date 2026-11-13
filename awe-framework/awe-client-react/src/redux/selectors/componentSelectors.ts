import { createSelector } from '@reduxjs/toolkit';
import ComponentRegistry from '../registry/ComponentRegistry';
import { mergeComponentState } from '../../utilities/mergeUtils';
import type { ComponentState } from '../../utilities/mergeUtils';
import type { RootState } from '../types';

// Input selectors must return the same reference for the same state: a new empty object would break the memoization
const EMPTY: ComponentState = Object.freeze({});

/**
 * Selector base: obtiene deltas de un componente específico
 */
export const getComponentDeltas = (state: RootState, componentId: string): ComponentState =>
    state.components[componentId] || EMPTY;

/**
 * Selector base: obtiene todos los deltas
 */
const getAllComponentDeltas = (state: RootState) => state.components;

/** Merge of base + deltas of one component, memoized per component */
interface MergeCache {
    (componentId: string, delta: ComponentState | undefined): ComponentState;
    /** Forgets the components that are not in the list */
    retain: (componentIds: string[]) => void;
}

/**
 * Memoizes the merge of base + deltas of every component: while neither the base nor the delta of a component change
 * it returns the same object, so whoever uses it does not render again when other components change
 *
 * @returns (componentId, delta) => merged component
 */
const makeMergeCache = (): MergeCache => {
    const cache = new Map<string, { base: ComponentState | null; delta: ComponentState | undefined; value: ComponentState }>();
    const merge = ((componentId: string, delta: ComponentState | undefined): ComponentState => {
        // Compare the registered base itself: a component that is not registered has no base
        const base = ComponentRegistry.get(componentId);
        const cached = cache.get(componentId);
        if (cached && cached.base === base && cached.delta === delta) {
            return cached.value;
        }
        const value = mergeComponentState(base || {}, delta || EMPTY);
        cache.set(componentId, { base, delta, value });
        return value;
    }) as MergeCache;
    // Forgets the components that are gone, so the cache does not grow with every screen
    merge.retain = (componentIds) => {
        const keep = new Set(componentIds);
        cache.forEach((_entry, id) => keep.has(id) || cache.delete(id));
    };
    return merge;
};

const mergeComponent = makeMergeCache();

/**
 * Factory de selector para un componente individual
 * The merged component is the same object while its base and its delta do not change, whatever the other
 * components or the selector instance do
 */
export const makeGetComponent = () => (state: RootState, componentId: string): ComponentState =>
    mergeComponent(componentId, getComponentDeltas(state, componentId));

const mergeAllComponents = makeMergeCache();

/**
 * Selector para obtener TODOS los componentes mergeados
 * Usado por useGrid y otros casos que necesitan acceso masivo. After a change only the components whose base or
 * delta changed are merged again: the rest keep their reference
 */
export const getAllComponents = createSelector(
    [getAllComponentDeltas],
    (deltas): Record<string, ComponentState> => {
        // Obtener todos los IDs únicos (de Registry + Redux)
        const allIds = [...new Set([...ComponentRegistry.getAllIds(), ...Object.keys(deltas)])];
        mergeAllComponents.retain(allIds);

        return allIds.reduce((acc, componentId) => {
            acc[componentId] = mergeAllComponents(componentId, deltas[componentId]);
            return acc;
        }, {} as Record<string, ComponentState>);
    }
);

/**
 * Selector to get several specific components when not all of them are needed. The result is a new object when any
 * component changes, but every component keeps its reference while it does not change: use it with shallowEqual
 *
 * @param componentIds - Component identifiers
 * @returns Memoized selector
 */
export const makeGetMultipleComponents = (componentIds: string[]) => {
    const merge = makeMergeCache();
    return createSelector(
        [getAllComponentDeltas],
        (deltas): Record<string, ComponentState> => componentIds.reduce((acc, id) => {
            acc[id] = merge(id, deltas[id]);
            return acc;
        }, {} as Record<string, ComponentState>)
    );
};

const mergeContextMenuComponent = makeMergeCache();

/**
 * Gets the attributes of the options of a context menu and of their nested options, indexed by identifier.
 * Every attributes object keeps its reference while its component does not change: use it with shallowEqual so the
 * menu only renders again when one of its options changes
 *
 * @param state - Redux state
 * @param contextMenu - Context menu options ({id})
 * @param [collected] - Attributes collected so far
 * @returns Attributes by option identifier
 */
export const getContextMenuAttributes = (
    state: RootState,
    contextMenu: { id: string }[] | null | undefined,
    collected: Record<string, Record<string, unknown>> = {}
): Record<string, Record<string, unknown>> => {
    const deltas = getAllComponentDeltas(state);
    (contextMenu || []).forEach(({ id }) => {
        const component = mergeContextMenuComponent(id, deltas[id]);
        if (component?.attributes && !(id in collected)) {
            collected[id] = component.attributes;
            getContextMenuAttributes(state, component.attributes.contextMenu as { id: string }[] | undefined, collected);
        }
    });
    return collected;
};
