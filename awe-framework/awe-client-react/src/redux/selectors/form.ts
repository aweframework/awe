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
 * Get all form values
 * @param state Full state
 * @param t Translate function
 * @param forPrinting For printing
 * @param address Address of the component that sends the values: a grid row detail instance adds its own
 * @return {object} Form values
 * @memberOf Components
 */
export const getFormValues = (
  state: RootState,
  forPrinting = false,
  t: (label: string) => string = (o) => o,
  address: ComponentAddress | null = null
): Record<string, unknown> => {
  const components = getAllComponents(state);
  const { settings = {} as SettingsState } = state;
  const formValues = Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "selector",
      operation: forPrinting ? "collectFormValuesForPrinting" : "collectFormValues",
      componentKey
    }))
    .reduce((result: Record<string, unknown>, [, component]) => {
      const values = getComponentData(component, { components, settings, t }, forPrinting);
      // The components that reach this point have a valid address: the others were filtered out above
      checkDuplicates(getComponentId(component.address) as string, result, values);
      return {
        ...result,
        ...values
      };
    }, {
      ...(settings.token ? { [settings.tokenKey]: settings.token } : {})
    });
  return {
    ...formValues,
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
