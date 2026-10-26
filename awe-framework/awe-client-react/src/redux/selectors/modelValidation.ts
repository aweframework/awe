import _ from "lodash";
import { getComponentData } from "../../utilities/components";
import { isTopLevelFormComponent } from "../../utilities/components";
import { isEmpty } from "../../utilities/general";
import type { ComponentState } from "../../utilities/mergeUtils";
import type { RootState, SettingsState } from "../types";
import { getAllComponents } from "./componentSelectors";

// Components that reach the checks have attributes: reading them fails otherwise, as it always did
const attributesOf = (component: ComponentState) => component.attributes as { id: string; checkEmpty?: boolean };

/**
 * Check whether the model is empty, updated or unchanged
 * @param state Full state
 * @return Flags of the model
 * @memberOf Components
 */
export const getModelValidation = (state: RootState): { isEmptyModel: boolean; isUpdatedModel: boolean; isUnchangedModel: boolean } => {
  const components = getAllComponents(state);
  const { settings = {} as SettingsState } = state;
  const componentsToCheck = Object.entries(components)
    .filter(([componentKey, component]) => isTopLevelFormComponent(component, {
      origin: "selector",
      operation: "getModelValidation",
      componentKey
    }));

  const isEmptyModel = componentsToCheck
    .filter(([, component]) => attributesOf(component).checkEmpty)
    .reduce((result, [, component]) => {
      const values = getComponentData(component, { components, settings }, false) as Record<string, unknown>;
      return result && isEmpty(values[attributesOf(component).id]);
    }, true);

  const isUpdatedModel = componentsToCheck
    .reduce((result, [, component]) => {
      const values = getComponentData(component, { components, settings }, false) as Record<string, unknown>;
      const storedValues = getComponentData(component, { components, settings }, false, "storedModel") as Record<string, unknown>;
      return result || !_.isEqual(values[attributesOf(component).id], storedValues[attributesOf(component).id]);
    }, false);

  const isUnchangedModel = !isUpdatedModel;

  return {
    isEmptyModel,
    isUpdatedModel,
    isUnchangedModel
  };
};
