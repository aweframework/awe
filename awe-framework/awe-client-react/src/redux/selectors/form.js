import { getComponentData, getComponentId, isTopLevelFormComponent } from "../../utilities/components";
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
function getRowDetailInstanceValues(components, address, props, forPrinting) {
  const { rowDetailInstance } = components[address?.component] || {};
  const instance = rowDetailInstance ? components[rowDetailInstance.instance] : null;
  if (!instance) {
    return {};
  }
  return {
    ...getComponentData(instance, props, forPrinting),
    ...instance.rowDetailInstance.parameters
  };
}

/**
 * Get all form values
 * @param {object} state Full state
 * @param {function} t Translate function
 * @param {boolean} forPrinting For printing
 * @param {object} address Address of the component that sends the values: a grid row detail instance adds its own
 * @return {object} Form values
 * @memberOf Components
 */
export const getFormValues = (state, forPrinting = false, t = (o) => o, address = null) => {
  const components = getAllComponents(state);
  const { settings = {} } = state;
  const formValues = Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "selector",
      operation: forPrinting ? "collectFormValuesForPrinting" : "collectFormValues",
      componentKey
    }))
    .reduce((result, [, component]) => {
      let values = getComponentData(component, { components, settings, t }, forPrinting);
      checkDuplicates(getComponentId(component.address), result, values);
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
function checkDuplicates(componentId, result, values) {
  if (componentId in result) {
    console.warn(`[WARNING] Overwriting '${componentId}' duplicated parameter`, {
      'old': result[componentId],
      'new': values
    });
  }
}
