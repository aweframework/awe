import {getComponentData, getComponentId} from "../../utilities/components";

/**
 * Get all form values
 * @param {object} state Full state
 * @param {function} t Translate function
 * @param {boolean} forPrinting For printing
 * @return {object} Form values
 * @memberOf Components
 */
export const getFormValues = (state, forPrinting = false, t = (o) => o) => {
  const {components = {}, settings = {}} = state;
  return Object.values(components)
    .filter(component => !("row" in component.address || "column" in component.address))
    .reduce((result, component) => {
      let values = getComponentData(component, {components, settings, t}, forPrinting);
      checkDuplicates(getComponentId(component.address), result, values);
      return {
        ...result,
        ...values
      };
    }, {
      ...(settings.token ? {[settings.tokenKey]: settings.token} : {})
    });
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