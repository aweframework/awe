import { createContext, useContext } from "react";

/**
 * View mode of the dialog or side panel the components are rendered in: the components it makes read-only (the
 * criteria, by component type) while the panel is shown by an action the server sent with viewMode, null otherwise.
 * The panel provides them: this module is used by the criteria, so it cannot import them back
 */
export const ViewModeContext = createContext(null);

/**
 * Whether a dialog or side panel is shown in view mode: the action that opened it has viewMode. The action is cleared
 * when the panel closes, so the view mode belongs to the opening, not to the panel
 * @param {Object} attributes Dialog or side panel attributes
 * @returns {boolean} The panel is shown in view mode
 */
export const isViewMode = (attributes) => !!attributes?.isShowing && attributes?.action?.viewMode === true;

/**
 * Components the view mode of the panel the component is rendered in makes read-only
 * @returns {Object|null} Read-only components by type, null when the panel is not in view mode
 */
export const useViewMode = () => useContext(ViewModeContext);

/**
 * A criterion of a panel in view mode is read-only, whatever its own readonly attribute says: no dependency can make
 * it editable while the panel is in view mode
 * @param {Object} component Component state
 * @param {Object|null} readOnlyComponents Components the view mode makes read-only, by type
 * @returns {Object} Component state, read-only when the view mode makes it read-only
 */
export function applyViewMode(component, readOnlyComponents) {
  const attributes = component?.attributes;
  if (!readOnlyComponents || !attributes || attributes.readonly === true || !(attributes.component in readOnlyComponents)) {
    return component;
  }
  return { ...component, attributes: { ...attributes, readonly: true } };
}
