import {attributeHandlers} from './attributes';
import {gridModelHandlers} from './gridModel';
import {lifecycleHandlers} from './lifecycle';
import {modelHandlers} from './model';
import {validationRuleHandlers} from './validationRules';

export const InitialState = {};

/**
 * Action handlers map, composed from the per-concern handler maps.
 * Each action type must be handled by exactly one module: a repeated key would be silently overridden.
 */
export const actionHandlers = {
  ...lifecycleHandlers,
  ...attributeHandlers,
  ...validationRuleHandlers,
  ...modelHandlers,
  ...gridModelHandlers
};

/**
 * Components reducer.
 * @param {Object} state Current Redux state
 * @param {Object} action Redux action with type and payload
 * @returns {Object} New state
 */
export function components(state = InitialState, action = {}) {
  const handler = actionHandlers[action.type];
  return handler ? handler(state, action) : state;
}
