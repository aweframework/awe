import { fetchAction, fetchFile, getActionAddress, getComponent, getRestUrl } from "../../utilities";
import { acceptAction, addActionsTop } from "../actions/actions";
import { getFormValues } from "../selectors/form";
import { getAllComponents } from "../selectors/componentSelectors";

let downloadFormIdentifier = 0;

export const serverAction = (action, forPrinting = false, t = (o) => o) => {
  return async (dispatch, getState) => {
    const { settings } = getState();
    const { parameters, target, address } = action;
    const { component } = address || {};
    const { serverAction, targetAction } = parameters;

    try {
      const response = await fetchAction(
        serverAction,
        targetAction,
        { ...getFormValues(getState(), forPrinting, t), ...parameters },
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
    } catch (error) {
      console.error("Error in server action call:", error);
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
      ...getFormValues(getState()),
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