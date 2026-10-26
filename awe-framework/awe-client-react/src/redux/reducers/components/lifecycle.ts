import {
  CLEAR_ALL_COMPONENTS,
  CLEAR_COMPONENTS,
  REGISTER_COMPONENTS,
  UNREGISTER_COMPONENTS,
  UPDATE_COMPONENT,
  UPDATE_MULTIPLE_COMPONENTS,
  UPDATE_VIEW_COMPONENTS
} from '../../actions/components';
import type {
  clearAllComponents,
  clearComponents as clearComponentsCreator,
  registerComponents,
  unregisterComponents as unregisterComponentsCreator,
  updateComponent,
  updateMultipleComponents,
  updateViewComponents
} from '../../actions/components';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas} from '../../../utilities/mergeUtils';
import {ComponentAddressType, getAddressType, getComponentId, warnMalformedComponent} from "../../../utilities/components";
import type {ComponentsState} from '../../types';
import {updateComponentFooters} from './gridFooter';
import {updateComponentData} from './model';
import type {ActionHandlers, ReducerSettings} from './stateHelpers';

/** Actions the lifecycle handlers take */
export type LifecycleAction = ReturnType<
  typeof clearComponentsCreator | typeof clearAllComponents | typeof updateViewComponents |
  typeof registerComponents | typeof unregisterComponentsCreator | typeof updateComponent |
  typeof updateMultipleComponents>;

/**
 * Remove all components for a given view.
 * @param {Object} state Current state
 * @param {string} view View identifier
 * @returns {Object} Updated state
 */
export function clearComponents(state: ComponentsState, view: string): ComponentsState {
  const clearedIds = new Set(ComponentRegistry.clear(view));
  return Object.fromEntries(Object.entries(state)
    .filter(([id, component]) => !clearedIds.has(id) && (component?.address?.view ?? component?.context?.view) !== view));
}

/**
 * Remove some components from the state and the registry.
 * @param {Object} state Current state
 * @param {string[]} ids Component identifiers
 * @returns {Object} Updated state
 */
export function unregisterComponents(state: ComponentsState, ids: string[] = []): ComponentsState {
  const removed = new Set(ids);
  removed.forEach(id => ComponentRegistry.unregister(id));
  return Object.fromEntries(Object.entries(state).filter(([id]) => !removed.has(id)));
}

/**
 * Add new components to the state: their base goes to the registry and their deltas to the state.
 * @param {Object} clearedState State the components are added to
 * @param {Object} data Components by id
 * @param {Object} settings Settings
 * @param {string} operation Operation name for the diagnostics
 * @param {string} view Target view for the diagnostics
 * @returns {Object} Updated state
 */
export function addComponents(
  clearedState: ComponentsState,
  data: ComponentsState,
  settings: ReducerSettings | undefined,
  operation: string,
  view: string | null
): ComponentsState {
  const updatedData = updateComponentFooters(data);

  Object.entries(updatedData || {}).forEach(([id, component]) => {
    if (getAddressType(component?.address) === ComponentAddressType.ADDRESS_INVALID) {
      warnMalformedComponent({...component, uid: component?.uid ?? id}, {
        origin: 'reducer',
        operation,
        componentKey: id,
        extra: {
          registryComponentId: id,
          targetView: view
        }
      });
    }
  });

  // Registrar componentes en el Registry y calcular deltas para Redux
  const deltas = Object.entries(updatedData).reduce<ComponentsState>((acc, [id, component]) => {
    // Registrar en Registry (estado base)
    ComponentRegistry.register(id, component);

    // Calcular deltas (lo que realmente guardamos en Redux)
    const base = ComponentRegistry.get(id);
    acc[id] = calculateDeltas(base, component);

    return acc;
  }, {});

  return {
    ...clearedState,
    ...deltas
  };
}

/**
 * Lifecycle action handlers.
 * Each handler is a pure function that takes (state, action) and returns new state.
 */
export const lifecycleHandlers: ActionHandlers<LifecycleAction> = {
  [CLEAR_COMPONENTS]: (state, action) => clearComponents(state, action.view),

  [CLEAR_ALL_COMPONENTS]: () => {
    ComponentRegistry.clearAll();
    return {};
  },

  [UPDATE_VIEW_COMPONENTS]: (state, action) => {
    const clearedState = action.view === "base" ? {} : clearComponents(state, action.view);
    return addComponents(clearedState, action.data, action.settings, 'updateViewComponents', action.view);
  },

  [REGISTER_COMPONENTS]: (state, action) => addComponents(state, action.data, action.settings, 'registerComponents', null),

  [UNREGISTER_COMPONENTS]: (state, action) => unregisterComponents(state, action.ids),

  [UPDATE_COMPONENT]: (state, action) =>
    updateComponentData(state, getComponentId(action.address), action.data),

  [UPDATE_MULTIPLE_COMPONENTS]: (state, action) =>
    action.componentList.reduce(
      (newState, _action) => updateComponentData(newState, getComponentId(_action.address), _action),
      state
    ),
};
