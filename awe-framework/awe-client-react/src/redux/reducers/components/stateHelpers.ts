import _ from 'lodash';
import type {ComponentAddress} from '../../../contract';
import ComponentRegistry from '../../registry/ComponentRegistry';
import {calculateDeltas, mergeComponentState} from '../../../utilities/mergeUtils';
import type {ComponentState} from '../../../utilities/mergeUtils';
import {ComponentAddressType, getAddressType, getComponentId} from "../../../utilities/components";
import {getGridIdentifier} from "../../../utilities/grid";
import type {CellOption, GridRow} from "../../../utilities/grid";
import {isEmpty} from "../../../utilities/general";
import type {NumberFormat} from "../../../utilities/numbers";
import type {Settings} from '../../actions/settings';
import type {ComponentsState} from '../../types';

const { ADDRESS_CELL, ADDRESS_COLUMN, ADDRESS_COMPONENT } = ComponentAddressType;

/**
 * ============================================================================
 * TYPES
 * ============================================================================
 * The state keeps the parts of a component (attributes, model, ...) as plain objects. The reducers read them
 * through these types: the grids have a column model, a model with rows and cells that keep their own attributes.
 */

/** Part of the state of a component: a plain object by key */
type StatePart = Record<string, unknown>;

/** Settings of the application the reducers may receive (the thunks add them to the actions) */
export type ReducerSettings = Partial<Settings>;

/** The attributes of a cell, kept in the `$attrs` of its row by column */
export interface CellAttributes {
  error?: unknown;
  validationRules?: StatePart;
  [key: string]: unknown;
}

/** A row of the model of a grid */
export interface StateRow extends GridRow {
  $attrs?: Record<string, CellAttributes>;
}

/** A column of the column model of a grid */
export interface StateColumn {
  name: string;
  component?: string;
  model?: {values?: CellOption[]};
  validationRules?: StatePart;
  numberFormat?: NumberFormat;
  summaryType?: string;
  [key: string]: unknown;
}

/** The attributes of a component, with the ones the reducers read */
export interface StateAttributes {
  columnModel?: StateColumn[];
  showTotals?: boolean;
  group?: string;
  component?: string;
  error?: unknown;
  treegrid?: boolean;
  treeId?: string;
  [key: string]: unknown;
}

/** The model of a component */
export interface StateModel {
  values?: StateRow[];
  selected?: unknown[];
  footer?: StatePart;
  modelVersion?: number;
  changed?: boolean;
  storedRows?: Record<string, StateRow>;
  [key: string]: unknown;
}

/** A component (base, delta or merged) as the reducers read it: its parts have their own types */
export interface StateComponent extends ComponentState {
  attributes?: StateAttributes;
  model?: StateModel;
  storedModel?: StateModel;
  defaultModel?: StateModel;
  storedAttributes?: StateAttributes;
}

/** An action as the thunks dispatch it: the settings of the application are added to it */
export type WithSettings<A> = A & {settings?: Settings};

/** Handlers by action type: each one gets the action of its type, narrowed from the union of the actions it handles */
export type ActionHandlers<A extends {type: string}> = {
  [T in A['type']]: (state: ComponentsState, action: WithSettings<Extract<A, {type: T}>>) => ComponentsState;
};

/** The component of an address with the components it points to: what a grid row needs to be updated */
export interface GridRowContext {
  componentId: string;
  base: ComponentState;
  component: StateComponent;
  model: StateModel;
  attributes: StateAttributes;
  values: StateRow[];
  rowIndex: number;
  rowData: StateRow;
}

/**
 * The no-action of the default arguments of the handlers: it has no address, so the state is returned as it is
 * (it is the default `{}` of the JavaScript version, which matches nothing)
 */
export const NO_ACTION = {} as {address: ComponentAddress};

/** The state of a component as the reducers read it (the state keeps its parts as plain objects) */
export function asStateComponent(component: ComponentState): StateComponent {
  return component as StateComponent;
}

/** Merge the base of a component with its delta and read the result as the reducers do */
export function mergeStateComponent(base: ComponentState | null | undefined, delta?: ComponentState | null): StateComponent {
  return asStateComponent(mergeComponentState(base, delta));
}

/**
 * Identifier of a component from its address. An address that does not point to a component has no identifier: it is
 * read as the reducers have always read it (the registry and the state look for the key "null")
 * @param address Address of the component
 * @returns Component identifier
 */
export function resolveComponentId(address: ComponentAddress | null | undefined): string {
  return getComponentId(address) as string;
}

/**
 * Identifier of the component of an address: its `component`, as the reducers read it even if the address has none
 * @param address Address of the component
 * @returns Component identifier
 */
export function addressComponentId(address: ComponentAddress): string {
  return address.component as string;
}

/**
 * Name of the column of an address: its `column`, as the reducers read it even if the address has none
 * @param address Address of the column or the cell
 * @returns Column name
 */
export function addressColumnName(address: ComponentAddress): string {
  return address.column as string;
}

/**
 * ============================================================================
 * IMMUTABLE STATE UPDATE UTILITIES
 * ============================================================================
 * These helper functions provide a cleaner, more readable way to update
 * nested state properties immutably, reducing code duplication.
 */

/**
 * Update a component entry by merging new data (registry-aware).
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} updates Partial data to merge into the component
 * @returns {Object} Updated state
 */
export function updateComponentInState(state: ComponentsState, componentId: string, updates: StatePart): ComponentsState {
  const base = ComponentRegistry.get(componentId);
  if (base) {
    const replaceObjectKeys = new Set(["validationRules", "storedValidationRules"]);
    // Get current merged state (base + existing delta)
    const currentFull = mergeComponentState(base, state[componentId]);
    // Apply updates to the full state (arrays replace instead of merging)
    const updatedFull = _.mergeWith({}, currentFull, updates, (objValue: unknown, srcValue: unknown, key: string) => {
      if (Array.isArray(srcValue)) {
        return srcValue;
      }
      if (replaceObjectKeys.has(key)) {
        return srcValue;
      }
      return undefined;
    });
    // Calculate new delta against base
    const newDelta = calculateDeltas(base, updatedFull);
    return {
      ...state,
      [componentId]: newDelta
    };
  }

  // Component not registered: operate directly on the Redux entry
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      ...updates
    }
  };
}

/**
 * Merge updates into a component property bag (e.g. attributes, validationRules).
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {string} propertyName Target property name
 * @param {Object} updates Partial updates for the property
 * @returns {Object} Updated state
 */
export function updateComponentProperty(
  state: ComponentsState,
  componentId: string,
  propertyName: 'attributes' | 'validationRules',
  updates: StatePart
): ComponentsState {
  const base = ComponentRegistry.get(componentId);
  if (base) {
    // Get current merged state
    const currentFull = mergeComponentState(base, state[componentId]);
    // Apply property update
    currentFull[propertyName] = mergeComponentPropertyValue(propertyName, currentFull?.[propertyName], updates);
    // Calculate new delta
    const newDelta = calculateDeltas(base, currentFull);
    return {
      ...state,
      [componentId]: newDelta
    };
  }

  // Component not registered: operate directly on the Redux entry
  if (!state[componentId]) return state;
  return {
    ...state,
    [componentId]: {
      ...state[componentId],
      [propertyName]: mergeComponentPropertyValue(propertyName, state[componentId][propertyName], updates)
    }
  };
}

/**
 * Merge updates for a component property while preserving nested numberFormat defaults.
 * @param {string} propertyName Target property name
 * @param {Object} currentValue Current property value
 * @param {Object} updates Partial updates
 * @returns {Object} Merged property value
 */
export function mergeComponentPropertyValue(propertyName: string, currentValue: StatePart = {}, updates: StatePart = {}): StatePart {
  if (propertyName !== 'attributes') {
    return {
      ...currentValue,
      ...updates
    };
  }

  const hasNumberFormatUpdate = Object.prototype.hasOwnProperty.call(updates, 'numberFormat')
    && typeof updates.numberFormat === 'object'
    && updates.numberFormat !== null;

  if (!hasNumberFormatUpdate) {
    return {
      ...currentValue,
      ...updates
    };
  }

  return {
    ...currentValue,
    ...updates,
    numberFormat: {
      ...(currentValue?.numberFormat || {}),
      // The check above proved it is an object
      ...(updates.numberFormat as StatePart)
    }
  };
}

/**
 * Update component attributes.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} attributeUpdates Attribute changes
 * @returns {Object} Updated state
 */
export function updateComponentAttributes(state: ComponentsState, componentId: string, attributeUpdates: StatePart): ComponentsState {
  return updateComponentProperty(state, componentId, 'attributes', attributeUpdates);
}

/**
 * Update component validation rules.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} validationUpdates Validation rule changes
 * @returns {Object} Updated state
 */
export function updateComponentValidation(state: ComponentsState, componentId: string, validationUpdates: StatePart): ComponentsState {
  return updateComponentProperty(state, componentId, 'validationRules', validationUpdates);
}

/**
 * Get the merged component (base + delta).
 * @param {string} componentId Component identifier
 * @param {Object} state Current state
 * @returns {Object|undefined} Merged component, or undefined when the component is neither registered nor in the state
 */
export function getMergedComponent(componentId: string, state: ComponentsState): StateComponent | undefined {
  const base = ComponentRegistry.get(componentId);
  const delta = state[componentId];
  if (!base && !delta) return undefined;
  return mergeStateComponent(base, delta);
}

/**
 * ============================================================================
 * COMPONENT TYPE CHECKS
 * ============================================================================
 */

/**
 * Check whether a component is a grid.
 * @param {Object} component Component to inspect
 * @returns {boolean} True if the component has a column model
 */
export function isGridComponent(component?: StateComponent | null): boolean {
  return !isEmpty(component) && "columnModel" in (component?.attributes || {});
}

/**
 * Check whether a grid component has a footer.
 * @param {Object} component Component to inspect
 * @returns {boolean} True when grid totals footer is enabled
 */
export function hasFooter(component?: StateComponent | null): boolean | undefined {
  return isGridComponent(component) && (component?.attributes || {}).showTotals;
}

/**
 * Check whether a component belongs to a radio group.
 * @param {Object} component Component to inspect
 * @returns {boolean} True if it is a grouped radio component
 */
export function isGroup(component?: StateComponent | null): boolean {
  const attributes = component?.attributes || {};
  return "group" in attributes && (attributes.component || "").includes("radio");
}

/** Handler of an update at one address scope: it gets the address and the data of the action */
type AddressFunction<D> = (state: ComponentsState, address: ComponentAddress, data: D, settings?: ReducerSettings) => ComponentsState;

/**
 * Route a model/action update to the proper handler by address type.
 * @param {Function} cellFunction Handler for cell-level updates
 * @param {Function} columnFunction Handler for column-level updates
 * @param {Function} componentFunction Handler for component-level updates
 * @param {Object} state Current state
 * @param {Object} action Action payload with address/data
 * @param {Object} settings Settings
 * @returns {*} Handler result or unchanged state
 */
export function launchAddressFunction<D>(
  cellFunction: AddressFunction<D>,
  columnFunction: AddressFunction<D>,
  componentFunction: AddressFunction<D>,
  state: ComponentsState,
  action: {address: ComponentAddress; data?: D},
  settings?: ReducerSettings
): ComponentsState {
  // The actions may come without data: each handler reads it as it always did
  const data = action.data as D;
  switch (getAddressType(action.address)) {
    case ADDRESS_CELL:
      return cellFunction(state, action.address, data, settings);
    case ADDRESS_COLUMN:
      return columnFunction(state, action.address, data, settings);
    case ADDRESS_COMPONENT:
      return componentFunction(state, action.address, data, settings);
    default:
      return state;
  }
}

/**
 * ============================================================================
 * GRID ROW HELPERS
 * ============================================================================
 */


/**
 * Resolve grid row context from an address.
 * @param {Object} state Current state
 * @param {Object} address Address with component/row/column
 * @returns {Object|null} Grid row context or null when not found
 */
export function getGridRowContext(state: ComponentsState = {}, address: ComponentAddress = {}): GridRowContext | null {
  const componentId = addressComponentId(address);
  const base = ComponentRegistry.get(componentId);

  if (!base) return null;

  const component = mergeStateComponent(base, state[componentId]);

  if (!component) return null;
  const { model, attributes } = component;
  if (!model || !attributes) return null;

  // A grid has values: it fails here, as it always did, if it has none
  const values = model.values as StateRow[];
  const gridId = getGridIdentifier(attributes);
  const rowIndex = values.findIndex((row) => String(row[gridId]) === String(address.row));
  if (rowIndex < 0) return null;

  const rowData = values[rowIndex] || {};

  return {
    componentId,
    base,
    component,
    model,
    attributes,
    values,
    rowIndex,
    rowData
  };
}

/**
 * Update grid row values using a resolved context.
 * @param {Object} state Current state
 * @param {Object} context Grid row context from getGridRowContext
 * @param {Array} nextValues Updated grid values array
 * @returns {Object} Updated state
 */
export function updateGridRowValues(state: ComponentsState, context: GridRowContext, nextValues: StateRow[]): ComponentsState {
  const { componentId, base, component } = context;

  const currentFull: StateComponent = {
    ...component,
    model: {
      ...component?.model,
      values: nextValues
    }
  };

  return {
    ...state,
    [componentId]: calculateDeltas(base, currentFull)
  };
}

/**
 * Build a component model update at component scope.
 * @param {Object} state Current state
 * @param {Object} address Component address
 * @param {Object} model Model updates
 * @returns {Object} Component update map
 */
export function getModelUpdate(state: ComponentsState, address: ComponentAddress, model: StatePart): ComponentsState {
  const componentId = resolveComponentId(address);

  const base = ComponentRegistry.get(componentId);
  if (base) {
    // Get current merged state
    const currentFull = mergeStateComponent(base, state[componentId]);
    // Apply model update
    currentFull.model = {
      ...currentFull?.model,
      ...model,
      changed: true
    };
    // Reset error on update
    currentFull.attributes = {
      ...currentFull?.attributes,
      error: null
    };

    // Calculate new delta
    const newDelta = calculateDeltas(base, currentFull);
    return {
      [componentId]: newDelta
    };
  }

  // Component not registered: operate directly on the Redux entry
  return {
    [componentId]: {
      ...state[componentId],
      model: {
        ...state[componentId]?.model,
        ...model,
        changed: true
      },
      attributes: {
        ...state[componentId]?.attributes,
        error: null
      }
    }
  };
}

/**
 * Build the model snapshot update for a component.
 * @param {Object} state Current state
 * @param {string} componentId Component identifier
 * @param {Object} settings Settings
 * @param {boolean} initial True when the snapshot is the initial model, so it is also the default one (restore)
 * @param {Object} defaultModel Default model to store with the initial one (the snapshot itself when it is not given)
 * @returns {Object} State diff for the snapshot
 */
export function getKeepModelComponent(
  state: ComponentsState,
  componentId: string,
  settings?: ReducerSettings,
  initial = false,
  defaultModel: StateModel | null = null
): ComponentsState {
  const base = ComponentRegistry.get(componentId);
  if (base) {
    const currentFull = mergeStateComponent(base, state[componentId]);
    currentFull.storedModel = {
      ...currentFull?.model,
      values: (currentFull.model?.values || []).map(value => ({ ...value }))
    };
    if (initial) {
      const source = defaultModel || currentFull.storedModel;
      currentFull.defaultModel = { ...source, values: (source.values || []).map(value => ({ ...value })) };
    }
    return {
      [componentId]: calculateDeltas(base, currentFull)
    };
  }

  // Component not registered: operate directly on the Redux entry
  if (!state[componentId]) return {};
  const current = asStateComponent(state[componentId]);
  const storedModel = {
    ...current.model,
    values: current.model!.values!.map(value => ({ ...value }))
  };
  return {
    [componentId]: {
      ...state[componentId],
      storedModel,
      ...(initial ? { defaultModel: { ...(defaultModel || storedModel), values: ((defaultModel || storedModel).values || []).map(value => ({ ...value })) } } : {})
    }
  };
}
