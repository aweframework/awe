import { getComponentData, getComponentId, isTopLevelFormComponent } from "../../utilities/components";
import type { ComponentAddress } from "../../contract";
import type { ComponentState } from "../../utilities/mergeUtils";
import type { RootState, SettingsState } from "../types";
import { getAllComponents } from "./componentSelectors";

/**
 * Values of the grid row detail instance an address belongs to (the instance, one of its cells or one of its buttons):
 * the data of the instance and the values of its parent row. Other addresses have none
 * @param {object} components All components
 * @param {object} address Address of the component that sends a request
 * @param {object} props Properties to get the component data
 * @param {boolean} forPrinting For printing
 * @return {object} Values of the row detail instance
 */
function getRowDetailInstanceValues(
  components: Record<string, ComponentState>,
  address: ComponentAddress | null,
  props: object,
  forPrinting: boolean
): Record<string, unknown> {
  const { rowDetailInstance } = (components[address?.component as string] || {}) as { rowDetailInstance?: { instance: string; parameters?: Record<string, unknown> } };
  const instance = rowDetailInstance ? components[rowDetailInstance.instance] : null;
  if (!instance) {
    return {};
  }
  return {
    ...getComponentData(instance, props, forPrinting),
    ...(instance.rowDetailInstance as { parameters?: Record<string, unknown> }).parameters
  };
}

/**
 * Address of the grid cell that calls an action, in the parameters of the grid: the address of the cell, with its
 * column, replaces the one of the selected rows, as the caller row is the one the action acts on. An address that is
 * not the cell of a grid changes nothing
 * @param {object} components All components
 * @param {object} address Address of the component that sends a request
 * @return {object} Parameter with the address of the cell, or nothing
 */
function getCallerCellAddress(
  components: Record<string, ComponentState>,
  address: ComponentAddress | null
): Record<string, unknown> {
  const { view, component, row, column } = (address || {}) as ComponentAddress & { row?: unknown; column?: unknown };
  const isCell = !!component && row !== undefined && row !== null && column !== undefined && column !== null;
  const attributes = (components[component as string] || {}).attributes as { id?: string; component?: string } | undefined;
  if (!isCell || attributes?.component !== "grid") {
    return {};
  }
  return { [`${attributes.id || component}.selectedRowAddress`]: { view, component, row, column } };
}

/**
 * Get all form values
 * @param state Full state
 * @param t Translate function
 * @param forPrinting For printing
 * @param address Address of the component that sends the values: a grid row detail instance adds its own
 * @param view Only the components of this view send their values (all the views when it is not given)
 * @return {object} Form values
 * @memberOf Components
 */
export const getFormValues = (
  state: RootState,
  forPrinting = false,
  t: (label: string) => string = (o) => o,
  address: ComponentAddress | null = null,
  view?: string
): Record<string, unknown> => {
  const components = getAllComponents(state);
  const { settings = {} as SettingsState } = state;
  const formValues = Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "selector",
      operation: forPrinting ? "collectFormValuesForPrinting" : "collectFormValues",
      componentKey
    }))
    .filter(([, component]) => !view || component.address?.view === view)
    .reduce((result: Record<string, unknown>, [, component]) => {
      const values = getComponentData(component, { components, settings, t }, forPrinting);
      // The components that reach this point have a valid address: the others were filtered out above
      checkDuplicates(getComponentId(component.address) as string, result, values);
      // The values join the same object: joining a copy per component would be quadratic
      return Object.assign(result, values);
    }, {
      ...(settings.token ? { [settings.tokenKey]: settings.token } : {})
    });
  return {
    ...formValues,
    ...getCallerCellAddress(components, address),
    ...getRowDetailInstanceValues(components, address, { components, settings, t }, forPrinting)
  };
};

/**
 * Check duplicates
 * @param {string} componentId Component id
 * @param {object} result Result
 * @param {object} values New values
 * @memberOf Components
 */
function checkDuplicates(componentId: string, result: Record<string, unknown>, values: unknown): void {
  if (componentId in result) {
    console.warn(`[WARNING] Overwriting '${componentId}' duplicated parameter`, {
      'old': result[componentId],
      'new': values
    });
  }
}
