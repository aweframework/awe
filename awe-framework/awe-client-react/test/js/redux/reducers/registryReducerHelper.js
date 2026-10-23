import { components } from '../../../../src/redux/reducers/componentsReducer';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';
import { mergeComponentState } from '../../../../src/utilities/mergeUtils';

/**
 * Runs the components reducer the way the application does: every component of the given state is registered as the
 * frozen base and the reducer stores only deltas. It returns the merged components (base + deltas), so a test can give
 * and expect whole components without knowing how they are split between the registry and Redux
 *
 * @param {Object} state Whole components, indexed by identifier
 * @param {Object} action Reducer action
 * @returns {Object} Merged components after the action
 */
export const reduceOnRegistry = (state = {}, action) => {
  ComponentRegistry.clearAll();
  Object.entries(state).forEach(([id, component]) => ComponentRegistry.register(id, _.cloneDeep(component)));
  const deltas = components({}, action);
  const ids = new Set([...ComponentRegistry.getAllIds(), ...Object.keys(deltas)]);
  return [...ids].reduce((merged, id) => ({
    ...merged,
    [id]: mergeComponentState(ComponentRegistry.get(id), deltas[id])
  }), {});
};
