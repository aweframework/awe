import _ from 'lodash';
import {warnMalformedComponent} from '../../utilities/components';

/**
 * ComponentRegistry - Singleton para almacenar estado base de componentes
 * 
 * Este registry almacena el estado inmutable de componentes que raramente cambia:
 * - address
 * - attributes (base)
 * - validationRules (base)
 * - dependencies
 * - actions
 * 
 * El estado dinámico (model, cambios en attributes) se guarda en Redux como deltas.
 */
class ComponentRegistry {
    constructor() {
        this.baseComponents = {};
        this.viewIndex = {}; // Índice por vista para limpieza rápida
    }

    /**
     * Registra un componente base
     * @param {string} componentId - ID único del componente
     * @param {Object} baseState - Estado base inmutable
     */
    register(componentId, baseState) {
        if (!baseState?.address) {
            warnMalformedComponent({...baseState, uid: baseState?.uid ?? componentId}, {
                origin: 'registry',
                operation: 'registerComponent',
                componentKey: componentId,
                extra: { registryComponentId: componentId }
            });
        }

        this.baseComponents[componentId] = Object.freeze(_.cloneDeep({...baseState}));

        // Indexar por vista para limpieza eficiente
        const view = this.resolveView(baseState);
        if (view) {
            if (!this.viewIndex[view]) {
                this.viewIndex[view] = new Set();
            }
            this.viewIndex[view].add(componentId);
        }
    }

    /**
     * Obtiene el estado base de un componente
     * @param {string} componentId - ID del componente
     * @returns {Object|null} Estado base o null si no existe
     */
    get(componentId) {
        return this.baseComponents[componentId] || null;
    }

    /**
     * Obtiene todos los IDs de componentes registrados
     * @returns {string[]} Array de component IDs
     */
    getAllIds() {
        return Object.keys(this.baseComponents);
    }

    /**
     * Limpia componentes de una vista específica
     * @param {string} view - Nombre de la vista
     */
    clear(view) {
        const indexedComponentIds = this.viewIndex[view] || new Set();
        const orphanIds = Object.entries(this.baseComponents)
            .filter(([id, component]) => !indexedComponentIds.has(id) && this.resolveView(component) === view)
            .map(([id]) => id);
        const ids = [...new Set([...Array.from(indexedComponentIds), ...orphanIds])];

        ids.forEach(id => {
            delete this.baseComponents[id];
        });

        delete this.viewIndex[view];
        return ids;
    }

    resolveView(component = {}) {
        return component?.address?.view ?? component?.context?.view ?? null;
    }

    /**
     * Limpia todos los componentes
     */
    clearAll() {
        this.baseComponents = {};
        this.viewIndex = {};
    }

    /**
     * Obtiene estadísticas del registry (útil para debugging)
     * @returns {Object} Estadísticas del registry
     */
    getStats() {
        return {
            totalComponents: Object.keys(this.baseComponents).length,
            views: Object.keys(this.viewIndex).length,
            componentsByView: Object.entries(this.viewIndex).reduce((acc, [view, ids]) => {
                acc[view] = ids.size;
                return acc;
            }, {})
        };
    }
}

// Exportar singleton
export default new ComponentRegistry();
