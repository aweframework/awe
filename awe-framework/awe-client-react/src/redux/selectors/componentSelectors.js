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
 * Selector optimizado para obtener múltiples componentes específicos
 * Útil cuando necesitas varios componentes pero no todos
 * 
 * @param {string[]} componentIds - Array de IDs de componentes
 * @returns {Function} Selector memoizado
 */
export const makeGetMultipleComponents = (componentIds) => createSelector(
    [getAllComponentDeltas, getSettings],
    (deltas, settings) => {
        if (!settings.useComponentRegistry) {
            return componentIds.reduce((acc, id) => {
                acc[id] = deltas[id];
                return acc;
            }, {});
        }

        return componentIds.reduce((acc, componentId) => {
            const base = ComponentRegistry.get(componentId) || {};
            const delta = deltas[componentId] || {};
            acc[componentId] = mergeComponentState(base, delta);
            return acc;
        }, {});
    }
);
