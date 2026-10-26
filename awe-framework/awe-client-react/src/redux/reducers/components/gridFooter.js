import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import {calculateFooterValue} from "../../../utilities/grid";
import {getComponentId} from "../../../utilities/components";
import {hasFooter} from './stateHelpers';

/**
 * Update grid footer values based on current grid data.
 * @param {Object} state Current state
 * @param {Object} address Grid address
 * @param {Object} settings Settings
 * @returns {Object} Updated state
 */
export function updateGridFooter(state, address, settings) {
  const componentId = getComponentId(address);
  let component;

  const base = ComponentRegistry.get(componentId);
  if (base) {
    component = mergeComponentState(base, state[componentId]);
    const updatedFull = generateGridFooter(component);
    return {
      ...state,
      [componentId]: calculateDeltas(base, updatedFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  component = state[componentId];
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
export function generateGridFooter(component) {
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
export function generateFooter(component) {
  const { columnModel = [] } = component?.attributes || {};
  return columnModel.reduce((prev, column) => ({ ...prev, [column.name]: calculateFooterValue(column, component.model?.values || []) }), {});
}

/**
 * Update footer values for all components that support footers.
 * @param {Object} data Components map
 * @returns {Object} Components map with updated footers
 */
export function updateComponentFooters(data) {
  return Object.entries(data).reduce((prev, [componentId, component]) => ({ ...prev, [componentId]: hasFooter(component) ? generateGridFooter(component) : component }), {});
}
