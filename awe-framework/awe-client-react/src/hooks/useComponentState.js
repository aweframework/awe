import { useMemo } from 'react';
import { shallowEqual, useSelector } from 'react-redux';
import { makeGetComponent, getAllComponents, makeGetMultipleComponents, getContextMenuAttributes } from '../redux/selectors/componentSelectors';

/**
 * Hook para acceder al estado de UN componente específico
 * Reemplaza: useSelector(state => state.components[id])
 * 
 * @param {string} componentId - ID del componente
 * @returns {Object} Estado mergeado del componente
 */
export function useComponentState(componentId) {
    const getComponent = useMemo(() => makeGetComponent(), []);
    return useSelector(state => getComponent(state, componentId));
}

/**
 * Hook para acceder a TODOS los componentes
 * Usado por useGrid y casos similares
 * Reemplaza: useSelector(state => state.components)
 * 
 * @returns {Object} Objeto con todos los componentes mergeados
 */
export function useAllComponents() {
    return useSelector(getAllComponents);
}

/**
 * Hook para acceder a múltiples componentes específicos
 * Más eficiente que getAllComponents cuando solo necesitas algunos
 * 
 * @param {string[]} componentIds - Array de IDs de componentes
 * @returns {Object} Objeto con los componentes solicitados
 */
export function useMultipleComponents(componentIds) {
    const selector = useMemo(
        () => makeGetMultipleComponents(componentIds),
        [componentIds.join(',')]
    );
    return useSelector(selector, shallowEqual);
}

/**
 * Hook to access the attributes of the options of a context menu (and of their nested options), by option identifier.
 * It only renders again when one of those options changes, not when any other component does
 *
 * @param {object[]} contextMenu - Context menu options ({id})
 * @returns {Object} Attributes by option identifier
 */
export function useContextMenuAttributes(contextMenu) {
    return useSelector(state => getContextMenuAttributes(state, contextMenu), shallowEqual);
}
