import _ from 'lodash';
import {warnMalformedComponent} from '../../utilities/components';
import type {ComponentState} from '../../utilities/mergeUtils';

/**
 * Freezes an object and everything below it, so a nested mutation of a base throws instead of leaking into every
 * merged copy that shares it
 *
 * @param {Object} value - Cloned value (owned by the registry)
 * @returns {Object} The same value, frozen
 */
const deepFreeze = <T>(value: T): T => {
    if (value && typeof value === 'object' && !Object.isFrozen(value)) {
        Object.freeze(value);
        Object.values(value).forEach(deepFreeze);
    }
    return value;
};

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
    baseComponents: Record<string, ComponentState> = {};
    // Índice por vista para limpieza rápida
    viewIndex: Record<string, Set<string>> = {};

    /**
     * Registra un componente base
     * @param {string} componentId - ID único del componente
     * @param {Object} baseState - Estado base inmutable
     */
    register(componentId: string, baseState: ComponentState): void {
        if (!baseState?.address) {
            warnMalformedComponent({...baseState, uid: baseState?.uid ?? componentId}, {
                origin: 'registry',
                operation: 'registerComponent',
                componentKey: componentId,
                extra: { registryComponentId: componentId }
            });
        }

        this.baseComponents[componentId] = deepFreeze(_.cloneDeep({...baseState}));

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
     * Remove a registered component (and its view index entry)
     * @param {string} componentId - Component id
     */
    unregister(componentId: string): void {
        const view = this.resolveView(this.baseComponents[componentId]);
        delete this.baseComponents[componentId];
        // A component without a view is looked up as "null", as it always was
        this.viewIndex[view as string]?.delete(componentId);
    }

    /**
     * Obtiene el estado base de un componente
     * @param {string} componentId - ID del componente
     * @returns {Object|null} Estado base o null si no existe
     */
    get(componentId: string): ComponentState | null {
        return this.baseComponents[componentId] || null;
    }

    /**
     * Obtiene todos los IDs de componentes registrados
     * @returns {string[]} Array de component IDs
     */
    getAllIds(): string[] {
        return Object.keys(this.baseComponents);
    }

    /**
     * Limpia componentes de una vista específica
     * @param {string} view - Nombre de la vista
     */
    clear(view: string): string[] {
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

    resolveView(component: ComponentState = {}): string | null {
        return component?.address?.view ?? component?.context?.view ?? null;
    }

    /**
     * Limpia todos los componentes
     */
    clearAll(): void {
        this.baseComponents = {};
        this.viewIndex = {};
    }

    /**
     * Obtiene estadísticas del registry (útil para debugging)
     * @returns {Object} Estadísticas del registry
     */
    getStats(): { totalComponents: number; views: number; componentsByView: Record<string, number> } {
        return {
            totalComponents: Object.keys(this.baseComponents).length,
            views: Object.keys(this.viewIndex).length,
            componentsByView: Object.entries(this.viewIndex).reduce((acc, [view, ids]) => {
                acc[view] = ids.size;
                return acc;
            }, {} as Record<string, number>)
        };
    }
}

// Exportar singleton
export default new ComponentRegistry();
