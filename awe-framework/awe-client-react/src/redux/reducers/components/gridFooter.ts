import type {ComponentAddress} from '../../../contract';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas} from '../../../utilities/mergeUtils';
import {calculateFooterValue} from "../../../utilities/grid";
import type {ComponentsState} from '../../types';
import {asStateComponent, hasFooter, mergeStateComponent, resolveComponentId} from './stateHelpers';
import type {StateComponent} from './stateHelpers';

/**
 * Update grid footer values based on current grid data.
 * @param {Object} state Current state
 * @param {Object} address Grid address
 * @returns {Object} Updated state
 */
export function updateGridFooter(state: ComponentsState, address: ComponentAddress): ComponentsState {
  const componentId = resolveComponentId(address);
  let component: StateComponent;

  const base = ComponentRegistry.get(componentId);
  if (base) {
    component = mergeStateComponent(base, state[componentId]);
    const updatedFull = generateGridFooter(component);
    return {
      ...state,
      [componentId]: calculateDeltas(base, updatedFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  component = asStateComponent(state[componentId]);
  return {
    ...state,
    [componentId]: generateGridFooter(component)
  };
}

/**
 * Attach footer data to a grid component model.
 * @param {Object} component Grid component
 * @returns {Object} Updated component with footer model
 */
export function generateGridFooter(component: StateComponent): StateComponent {
  return {
    ...component,
    model: {
      ...component.model,
      footer: generateFooter(component)
    }
  };
}

/**
 * Compute footer values for all grid columns.
 * @param {Object} component Grid component
 * @returns {Object} Footer values by column name
 */
export function generateFooter(component: StateComponent): Record<string, unknown> {
  const { columnModel = [] } = component?.attributes || {};
  return columnModel.reduce<Record<string, unknown>>((prev, column) => ({ ...prev, [column.name]: calculateFooterValue(column, component.model?.values || []) }), {});
}

/**
 * Update footer values for all components that support footers.
 * @param {Object} data Components map
 * @returns {Object} Components map with updated footers
 */
export function updateComponentFooters(data: ComponentsState): ComponentsState {
  return Object.entries(data).reduce<ComponentsState>((prev, [componentId, component]) => ({ ...prev, [componentId]: hasFooter(asStateComponent(component)) ? generateGridFooter(asStateComponent(component)) : component }), {});
}
