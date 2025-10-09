import {getComponentData} from "../../utilities/components";
import {isEmpty} from "../../utilities/general";

/**
 * Get all form values
 * @param {object} state Full state
 * @param {Function<"translation", undefined>} t Translate function
 * @param {boolean} forPrinting For printing
 * @return {object} Form values
 * @memberOf Components
 */
export const getModelValidation = (state) => {
  const {components = {}, settings = {}} = state;
  const componentsToCheck = Object.values(components)
    .filter(component => !("row" in component.address || "column" in component.address));

  const isEmptyModel = componentsToCheck
    .filter(component => component.attributes.checkEmpty)
    .reduce((result, component) => {
      const values = getComponentData(component, {components, settings}, false);
      return result && isEmpty(values[component.attributes.id]);
    }, true);

  const isUpdatedModel = componentsToCheck
    .reduce((result, component) => {
      const values = getComponentData(component, {components, settings}, false);
      const storedValues = getComponentData(component, {components, settings}, false, "storedModel");
      return result || !_.isEqual(values[component.attributes.id], storedValues[component.attributes.id]);
    }, false);

  const isUnchangedModel = !isUpdatedModel;

  return {
    isEmptyModel,
    isUpdatedModel,
    isUnchangedModel
  };
};