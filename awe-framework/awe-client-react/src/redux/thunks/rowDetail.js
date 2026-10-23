import _ from "lodash";
import { getComponent } from "../../utilities";
import { getRowDetailInstanceId, getRowDetailParameters } from "../../utilities/gridRowDetail";
import { addActionsTop } from "../actions/actions";
import { registerComponents, unregisterComponents } from "../actions/components";
import { getAllComponents } from "../selectors/componentSelectors";
import { buildComponent } from "./screen";

/**
 * Components of the row detail instances of a grid (the instances and their buttons)
 * @param {object} components All components
 * @param {object} parentAddress Address of the parent grid
 * @returns {object[]} Components of its instances
 */
function getRowDetailComponents(components, parentAddress) {
  return Object.values(components).filter(({ rowDetailInstance }) =>
    rowDetailInstance?.parent?.view === parentAddress.view && rowDetailInstance?.parent?.component === parentAddress.component);
}

/**
 * Build the components of the instance of a row detail component for a row of the parent grid: a clone of the
 * template with its own id (and its buttons with their own ones), built as the screen builds its components. It is not
 * loaded by itself (the caller loads it once), and it keeps the values of the parent row it sends with its requests
 * @param {object} parent Parent grid
 * @param {object} template Row detail component ({id, controller, model})
 * @param {object} row Row of the parent grid
 * @param {object} settings Application settings
 * @returns {object} Components by id
 */
function buildRowDetailInstance(parent, template, row, settings) {
  const { view } = parent.address;
  const instance = getRowDetailInstanceId(template.id, row.id);
  const source = [...(parent.context?.source || []), instance];
  const rowDetailInstance = {
    parent: { view, component: parent.address.component },
    row: row.id,
    instance,
    parameters: getRowDetailParameters(parent.attributes?.id || parent.address.component, row)
  };
  const controller = _.cloneDeep(template.controller);
  const buttons = (controller.buttonModel || []).map(button => ({
    id: getRowDetailInstanceId(button.id, row.id),
    controller: button
  }));
  // The empty state action names a button of the grid: in an instance it is the button of the same instance
  const emptyAction = buttons.find(({ controller: button }) => button.id === controller.emptyAction)?.id ?? controller.emptyAction;
  const instanceController = {
    ...controller,
    autoload: false,
    ...(controller.emptyAction ? { emptyAction } : {}),
    buttonModel: buttons.map(({ id, controller: button }) => ({ ...button, id }))
  };
  const instanceModel = { ...(_.cloneDeep(template.model) || {}), values: _.cloneDeep(template.model?.values) || [] };

  return {
    [instance]: {
      ...buildComponent(view, { id: instance, controller: instanceController, model: instanceModel }, settings, source),
      rowDetailInstance
    },
    ...buttons.reduce((list, { id, controller: button }) => ({
      ...list,
      [id]: {
        ...buildComponent(view, { id, controller: button, model: { values: [] } }, settings, [...source, id]),
        rowDetailInstance
      }
    }), {})
  };
}

/**
 * Keep the row detail instances of a grid as its open rows: the rows just opened get a new instance, loaded once with
 * filter, and the instances of the rows no longer open (closed, reloaded or with the grid gone) are removed with all
 * their state. The instances are read from the state, so calling it again with the same rows changes nothing
 * @param {object} parentAddress Address of the parent grid
 * @param {object[]} openRows Rows of the parent grid whose detail is shown
 * @returns {function} Thunk
 */
export function syncRowDetailInstances(parentAddress, openRows = []) {
  return (dispatch, getState) => {
    const { settings } = getState();
    const components = getAllComponents(getState());
    const parent = getComponent(components, parentAddress);
    const template = parent?.attributes?.rowDetail?.component;
    const rows = template ? openRows : [];
    const wanted = new Set(rows.map(row => getRowDetailInstanceId(template.id, row.id)));
    const existing = getRowDetailComponents(components, parentAddress);
    const registered = new Set(existing.map(({ rowDetailInstance }) => rowDetailInstance.instance));

    const stale = existing
      .filter(({ rowDetailInstance }) => !wanted.has(rowDetailInstance.instance))
      .map(({ address }) => address.component);
    if (stale.length > 0) {
      dispatch({ ...unregisterComponents(stale), settings });
    }

    const opened = rows.filter(row => !registered.has(getRowDetailInstanceId(template.id, row.id)));
    if (opened.length > 0) {
      dispatch({
        ...registerComponents(opened.reduce((list, row) => ({ ...list, ...buildRowDetailInstance(parent, template, row, settings) }), {})),
        settings
      });
      dispatch(addActionsTop(opened.map(row => ({
        type: "filter",
        address: { view: parentAddress.view, component: getRowDetailInstanceId(template.id, row.id) }
      }))));
    }
  };
}
