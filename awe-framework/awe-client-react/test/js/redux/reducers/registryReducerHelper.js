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
  const deltas = reduceDeltasOnRegistry(state, action, {});
  const ids = new Set([...ComponentRegistry.getAllIds(), ...Object.keys(deltas)]);
  return [...ids].reduce((merged, id) => ({
    ...merged,
    [id]: mergeComponentState(ComponentRegistry.get(id), deltas[id])
  }), {});
};

/**
 * Registers every component of the given state as the frozen base and runs the reducer over the given deltas,
 * returning the raw reducer result. Use it to check the identity of the state a reducer returns (a no-op action must
 * return the very same deltas), which the merged result of reduceOnRegistry cannot show
 *
 * @param {Object} state Whole components, indexed by identifier
 * @param {Object} action Reducer action
 * @param {Object} deltas Redux deltas the reducer starts from
 * @returns {Object} Deltas after the action
 */
export const reduceDeltasOnRegistry = (state = {}, action, deltas = {}) => {
  ComponentRegistry.clearAll();
  Object.entries(state).forEach(([id, component]) => ComponentRegistry.register(id, _.cloneDeep(component)));
  return components(deltas, action);
};
