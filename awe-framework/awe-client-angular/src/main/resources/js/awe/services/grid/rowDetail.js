import {TestIds} from "../../data/testIds";

/**
 * Row detail of a grid: a toggle per row that shows, under the row and across the whole grid, the text of the
 * `<row-detail>` element where each `{name}` placeholder is replaced with the field `name` of the row (any field of the
 * datalist, also the ones of hidden columns).
 *
 * The text is plain text (it is never parsed as HTML). What is not a valid placeholder is kept as text.
 *
 * The template is split once by its closing braces: in each piece the last opening brace starts the placeholder, so its
 * time is linear. It does not use regular expressions on the template, only on single names.
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
    <i class="fa" ng-class="grid.appScope.component.isRowDetailOpen(row) ? 'fa-chevron-down' : 'fa-chevron-right'" aria-hidden="true"></i>
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
 */
export function initRowDetail(component, onChange) {
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
    } else {
      openRows.add(id);
    }
    onChange();
  };

  /**
   * Hide the details of every row (the data of the grid is reloaded)
   */
  component.closeRowDetails = function () {
    openRows.clear();
  };
}
