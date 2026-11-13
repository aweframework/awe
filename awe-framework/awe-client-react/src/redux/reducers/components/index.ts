import type {ComponentsState} from '../../types';
import {attributeHandlers} from './attributes';
import type {AttributeAction} from './attributes';
import {gridModelHandlers} from './gridModel';
import type {GridModelAction} from './gridModel';
import {lifecycleHandlers} from './lifecycle';
import type {LifecycleAction} from './lifecycle';
import {modelHandlers} from './model';
import type {ModelAction} from './model';
import type {WithSettings} from './stateHelpers';
import {validationRuleHandlers} from './validationRules';
import type {ValidationRuleAction} from './validationRules';

export const InitialState: ComponentsState = {};

/** Actions the components reducer handles */
export type ComponentsAction = LifecycleAction | AttributeAction | ValidationRuleAction | ModelAction | GridModelAction;

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
// The default `{}` is the no-action of the JavaScript version: it matches no handler, so the state is returned as it is
export function components(
  state: ComponentsState = InitialState,
  action: WithSettings<ComponentsAction> = {} as ComponentsAction
): ComponentsState {
  // Each handler takes the action of its own type: the type of the action picks the handler, so they always match
  const handler = actionHandlers[action.type] as ((state: ComponentsState, action: WithSettings<ComponentsAction>) => ComponentsState) | undefined;
  return handler ? handler(state, action) : state;
}
