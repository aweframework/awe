import {TestIds} from "../../data/testIds";
import {iconMarkup} from "../icon";
import _ from "lodash";

/**
 * Row detail of a grid: a toggle per row that shows, under the row and across the whole grid, the text of the
 * `<row-detail>` element where each `{name}` placeholder is replaced with the field `name` of the row (any field of the
 * datalist, also the ones of hidden columns).
 *
 * The text is plain text (it is never parsed as HTML). What is not a valid placeholder is kept as text.
 *
 * The template is split once by its closing braces: in each piece the last opening brace starts the placeholder, so its
 * time is linear. It does not use regular expressions on the template, only on single names.
 *
 * With components (`rowDetail.component`) each open row gets its own instance of the grid of the detail, as the React
 * client does: a clone of its controller and model stored with its own identifier, loaded once with the fields of its
 * parent row and removed with all its state when the row closes, the parent gets new data or the parent is destroyed.
 */

const FIELD_NAME = /^[\w.$-]+$/;

// Name of the leading column with the toggles
export const ROW_DETAIL_COLUMN = "rowDetailToggle";

// Toggle of a row: a native button, so it is reached with Tab and activated with Enter and Space. Its click does not
// reach the cell, so ui-grid does not select the row
const TOGGLE_CELL_TEMPLATE = `<div class="no-animate ui-grid-cell-contents ui-grid-row-detail text-center" column-id="{{col.name}}" data-testid="${TestIds.gridCell}">
  <button type="button" class="btn btn-link grid-row-detail-toggle" data-testid="${TestIds.gridRowDetailToggle}"
          ng-attr-row-id="{{row.entity[grid.appScope.component.constants.ROW_IDENTIFIER]}}"
          ng-attr-aria-expanded="{{grid.appScope.component.isRowDetailOpen(row)}}"
          ng-attr-aria-controls="{{grid.appScope.component.getRowDetailId(row)}}"
          ng-attr-aria-label="{{(grid.appScope.component.isRowDetailOpen(row) ? 'SCREEN_TEXT_GRID_ROW_DETAIL_HIDE' : 'SCREEN_TEXT_GRID_ROW_DETAIL_SHOW') | translateMultiple}}"
          ng-click="grid.appScope.component.toggleRowDetail(row, $event)">
    <i aria-hidden="true">${iconMarkup("chevron-down", 'ng-if="grid.appScope.component.isRowDetailOpen(row)"')}${iconMarkup("chevron-right", 'ng-if="!grid.appScope.component.isRowDetailOpen(row)"')}</i>
  </button>
</div>`;

/**
 * Text of a field of a row: the label of a cell object, else its value. Missing values are an empty text
 * @param {object} row Row data
 * @param {string} name Field name
 * @returns {string} Field text
 */
function fieldText(row, name) {
  if (row === null || row === undefined || !Object.hasOwn(row, name)) {
    return "";
  }
  const field = row[name];
  const value = field !== null && typeof field === "object" ? field.label ?? field.value : field;
  return value === null || value === undefined ? "" : String(value);
}

/**
 * Render the row detail template of a grid with the data of a row
 * @param {string} template Template with `{name}` placeholders
 * @param {object} row Row data
 * @returns {string} Plain text of the detail
 */
export function renderRowDetail(template, row) {
  const pieces = String(template ?? "").split("}");
  // The text after the last closing brace has no placeholder
  const tail = pieces.pop();
  return pieces.map(piece => {
    const open = piece.lastIndexOf("{");
    const name = open < 0 ? null : piece.substring(open + 1);
    return name !== null && FIELD_NAME.test(name) ? piece.substring(0, open) + fieldText(row, name) : `${piece}}`;
  }).join("") + tail;
}

/**
 * Identifier of the detail of a row, referenced by the "aria-controls" of its toggle
 * @param {string} gridId Grid identifier
 * @param {string|number} rowId Row identifier
 * @returns {string} Detail element id
 */
export function getRowDetailId(gridId, rowId) {
  return `${gridId}-row-detail-${rowId}`;
}

/**
 * Identifier of the instance of a row detail component for a row of its grid (e.g. GrdOrders-7). The buttons of the
 * instance get their own one from their identifier the same way
 * @param {string} componentId Identifier of the component in the row detail (or of one of its buttons)
 * @param {string|number} rowId Identifier of the row of the parent grid
 * @returns {string} Instance identifier
 */
export function getRowDetailInstanceId(componentId, rowId) {
  return `${componentId}-${rowId}`;
}

/**
 * Single values of a row of the parent grid sent with the requests of a row detail instance: every field of the row
 * (also the ones of hidden columns) as `<parentGridId>.row.<column>`, cell objects as their value. The selection and
 * the internal fields of the row ($$hashKey...) are not columns
 * @param {string} parentGridId Identifier of the parent grid
 * @param {object} row Row data
 * @returns {object} Request parameters
 */
export function getRowDetailParameters(parentGridId, row) {
  const parameters = {};
  for (const [name, cell] of Object.entries(row ?? {})) {
    if (name === "selected" || name.startsWith("$")) {
      continue;
    }
    const isCell = cell !== null && typeof cell === "object" && !Array.isArray(cell);
    parameters[`${parentGridId}.row.${name}`] = isCell ? cell.value : cell;
  }
  return parameters;
}

/**
 * Data of a row detail instance under the keys of its template (GrdOrders, GrdOrders.data, GrdOrders-id...), as the
 * server knows the grid by the identifier of the template. Its column values keep their keys
 * @param {object} data Data of the instance (getData)
 * @param {string} instanceId Instance identifier
 * @param {string} templateId Template identifier
 * @returns {object} Data with the keys of the template
 */
export function getRowDetailInstanceData(data, instanceId, templateId) {
  return Object.entries(data || {}).reduce((renamed, [key, value]) => {
    const own = key === instanceId || key.startsWith(`${instanceId}.`) || key.startsWith(`${instanceId}-`);
    return {...renamed, [own ? templateId + key.substring(instanceId.length) : key]: value};
  }, {});
}

/**
 * Instances of the row detail component of a grid, one per open row. Each one is stored as the screen stores its
 * components (controller, model and api by view and identifier) with its buttons, and keeps in its controller
 * (rowDetailInstance) the values of its parent row, so its requests send them and the screen leaves it out of its
 * form values
 * @param {object} component Grid component (with controller.rowDetail.component)
 * @param {object} Storage Storage service
 * @param {function} storeComponent Stores a component in a view (ServerData.storeComponent)
 * @returns {{get: function, remove: function, removeAll: function}} Instances by row
 */
export function initRowDetailInstances(component, Storage, storeComponent) {
  const template = component.controller.rowDetail.component;
  const instances = new Map();
  const viewOf = kind => (Storage.get(kind) || {})[component.address.view];

  /**
   * Store the instance of a row: a clone of the template with its own identifier and the ones of its buttons. It loads
   * only when it is shown (autoload off)
   * @param {object} entity Row data
   * @returns {object} Instance
   */
  const create = entity => {
    const rowId = entity[component.constants.ROW_IDENTIFIER];
    const id = getRowDetailInstanceId(template.id, rowId);
    const rowDetailInstance = {
      instance: id,
      template: template.id,
      parameters: getRowDetailParameters(component.address.component, entity)
    };
    const buttons = (template.controller.buttonModel || []).map(button => ({
      ..._.cloneDeep(button),
      id: getRowDetailInstanceId(button.id, rowId),
      rowDetailInstance
    }));
    const controller = {
      ..._.cloneDeep(template.controller),
      id,
      autoload: false,
      buttonModel: buttons,
      rowDetailInstance
    };
    if (controller.emptyAction) {
      controller.emptyAction = getRowDetailInstanceId(controller.emptyAction, rowId);
    }
    const option = ((Storage.get("screen") || {})[component.address.view] || {}).option;
    const entries = [
      // The template has no data: the instance starts on the first page, as its first load asks for it
      {id, controller, model: {page: 1, ..._.cloneDeep(template.model)}},
      ...buttons.map(button => ({id: button.id, controller: _.cloneDeep(button), model: {}}))
    ];
    entries.forEach(entry => storeComponent(entry, viewOf("model"), viewOf("controller"), viewOf("api"), option));
    return {
      id,
      buttons: buttons.map(button => button.id),
      entries: entries.map(entry => ({id: entry.id, controller: viewOf("controller")[entry.id]})),
      loaded: false
    };
  };

  /**
   * Remove the instance of a row with all its state. Only its own entries are removed: a new screen in the same view
   * may already use the identifiers
   * @param {string} rowId Row identifier
   */
  const remove = rowId => {
    const instance = instances.get(rowId);
    instances.delete(rowId);
    const controllers = viewOf("controller");
    (instance?.entries || []).filter(entry => controllers && controllers[entry.id] === entry.controller).forEach(entry => {
      ["controller", "model", "api"].forEach(kind => delete viewOf(kind)?.[entry.id]);
    });
  };

  return {
    /**
     * Instance of a row, created the first time
     * @param {object} entity Row data
     * @returns {object} Instance
     */
    get: entity => {
      const rowId = String(entity[component.constants.ROW_IDENTIFIER]);
      if (!instances.has(rowId)) {
        instances.set(rowId, create(entity));
      }
      return instances.get(rowId);
    },
    remove,
    removeAll: () => [...instances.keys()].forEach(remove)
  };
}

/**
 * Leading column of a grid with row detail: the toggle of each row
 * @param {object} component Grid component
 * @returns {object} Column model
 */
export function getRowDetailColumn(component) {
  return {
    width: 30,
    minWidth: 30,
    label: "",
    displayName: "",
    name: ROW_DETAIL_COLUMN,
    frozen: component.hasFrozen,
    headerCellTemplate: "grid/header",
    cellTemplate: TOGGLE_CELL_TEMPLATE,
    enableColumnResizing: false,
    enableColumnMoving: false,
    enableColumnMenu: false,
    enableFiltering: false,
    sortable: false
  };
}

/**
 * Add the row detail methods to a grid component. The rows whose detail is shown are kept by row identifier, so a row
 * keeps it across the client-side pages and sorting of the grid, until its data is reloaded
 * @param {object} component Grid component (with controller.rowDetail)
 * @param {function} onChange Called after the detail of a row is shown or hidden (the height of the rows changes)
 * @param {object} instances Instances of the row detail component (initRowDetailInstances), only with components
 */
export function initRowDetail(component, onChange, instances = null) {
  const openRows = new Set();
  const rowId = row => String(row.entity[component.constants.ROW_IDENTIFIER]);

  component.rowDetail = component.controller.rowDetail;

  /**
   * The detail of the row is shown
   * @param {object} row ui-grid row
   * @returns {boolean} Detail shown
   */
  component.isRowDetailOpen = row => openRows.has(rowId(row));

  /**
   * Identifier of the detail of the row
   * @param {object} row ui-grid row
   * @returns {string} Detail element id
   */
  component.getRowDetailId = row => getRowDetailId(component.id, rowId(row));

  /**
   * Text of the detail of the row
   * @param {object} row ui-grid row
   * @returns {string} Plain text of the detail
   */
  component.getRowDetailText = row => renderRowDetail(component.rowDetail.template, row.entity);

  /**
   * Show or hide the detail of the row, without selecting it
   * @param {object} row ui-grid row
   * @param {Event} event Click event
   */
  component.toggleRowDetail = function (row, event) {
    if (event) {
      event.stopPropagation();
    }
    const id = rowId(row);
    if (openRows.has(id)) {
      openRows.delete(id);
      instances?.remove(id);
    } else {
      openRows.add(id);
      instances?.get(row.entity);
    }
    onChange();
  };

  /**
   * Hide the details of every row (the data of the grid is reloaded)
   */
  component.closeRowDetails = function () {
    openRows.clear();
    instances?.removeAll();
  };

  if (instances) {
    /**
     * Instance of the row detail component of an open row
     * @param {object} row ui-grid row
     * @returns {object} Instance ({id, buttons, loaded})
     */
    component.getRowDetailInstance = row => instances.get(row.entity);

    // The instances go with the grid
    component.scope.$on("$destroy", () => instances.removeAll());
  }
}
