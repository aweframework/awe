import { fetchAction, fetchFile, getActionAddress, getComponent, getRestUrl } from "../../utilities";
import { acceptAction, addActionsTop } from "../actions/actions";
import { updateAttributes } from "../actions/components";
import { getFormValues } from "../selectors/form";
import { getAllComponents } from "../selectors/componentSelectors";

let downloadFormIdentifier = 0;

export const serverAction = (action, forPrinting = false, t = (o) => o) => {
  return async (dispatch, getState) => {
    const { settings } = getState();
    const { parameters, target, address } = action;
    const { component } = address || {};
    const { serverAction, targetAction } = parameters;
    // The target of the action loads until its response (fill, end-load...) arrives, as in the AngularJS client
    const targetAddress = target ? { ...address, component: target } : null;
    if (targetAddress) {
      dispatch(updateAttributes(targetAddress, { loading: true }));
    }

    try {
      const response = await fetchAction(
        serverAction,
        targetAction,
        // Form values win over the action parameters (as in the AngularJS client): the action always carries
        // null target, value and label, which would otherwise wipe a component or column with that name. The
        // components of a grid row detail instance also send the values of their instance
        { ...parameters, ...getFormValues(getState(), forPrinting, t, address) },
        settings.token,
        null
      );

      // Añadir nuevas acciones a la pila
      dispatch(addActionsTop(
        response.map(a => ({
          ...a,
          address: { ...address, component: target || component, ...(a.address || {}) }
        }))
      ));

      // Aceptar la acción original
      dispatch(acceptAction(action));
      return true;
    } catch (error) {
      console.error("Error in server action call:", error);

      // No response will end the load of the target
      if (targetAddress) {
        dispatch(updateAttributes(targetAddress, { loading: false }));
      }

      // The action can ask to set some attributes on its component when the call fails (i.e. to try again)
      if (action.errorAttributes) {
        dispatch(updateAttributes(address, action.errorAttributes));
      }
      return false;
    }
  };
};

export function serverDownloadAction(action) {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { settings } = getState();
    const address = getActionAddress(action);
    let component = getComponent(components, address);
    const { specificAttributes } = component;

    let parameters = {
      ...action.parameters,
      ...getFormValues(getState(), false, undefined, address),
      ...(specificAttributes || {}),
    };

    let targetAction = parameters[settings.targetActionKey];
    fetchFile(
      getRestUrl("file", "download", "maintain", targetAction),
      { ...parameters, d: downloadFormIdentifier++ },
      settings.token
    ).then(() => dispatch(acceptAction(action)))
      .catch((reason) => console.error("Error downloading file:", reason));
  };
}