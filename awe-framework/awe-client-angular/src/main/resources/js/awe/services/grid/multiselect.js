/**
 * Checkbox multiselect of a grid (multiselect="true"), shared by the flat grid and the tree grid (#791): a leading
 * column with a checkbox per row and the select-all checkbox in its header. A row of a tree is selected on its own:
 * selecting a parent does not select its children
 */

// Name of the leading column with the checkboxes
export const MULTISELECT_COLUMN = "chkBox";

/**
 * Column with the checkbox of each row and the select-all checkbox in its header
 * @param {object} component Grid component
 * @returns {object} ui-grid column definition
 */
export function getMultiselectColumn(component) {
  return {
    width: 26,
    label: "",
    displayName: "",
    name: MULTISELECT_COLUMN,
    frozen: component.hasFrozen,
    headerCellTemplate: "grid/headerCheckbox",
    cellTemplate: "grid/cellCheckbox",
    enableColumnResizing: false,
    enableColumnMoving: false,
    enableColumnMenu: false,
    enableFiltering: false,
    sortable: false
  };
}

/**
 * Click on the select-all checkbox: it selects every row, or none when it is unchecked
 * @param {object} api ui-grid api of the grid (none while the grid is not rendered)
 * @param {Event} event Click event
 */
export function selectAllClick(api, event) {
  if (api) {
    if (!api.selection.getSelectAllState()) {
      api.selection.clearSelectedRows(event);
    } else {
      api.selection.selectAllRows(event);
    }
  }
}
