/**
 * Client actions of a grid: the actions the grid service runs on a grid of the screen (its selection, its rows, its
 * columns, its page, sort and filters, the edition of a row and the branches of a tree grid). They are immediate, so
 * they are silent by default: running them never disables the buttons of the screen. The work that goes to the server
 * is a server action of its own (a filter, a load, a branch of a tree grid that was never loaded), which disables them
 * unless it is marked silent.
 */
export const GRID_ACTIONS: readonly string[] = [
  "toggle-columns-visibility", "select-row", "select-first-row", "select-last-row", "select-all-rows",
  "unselect-all-rows", "check-one-selected", "check-some-selected", "check-records-generated",
  "check-records-saved", "delete-row", "add-row", "add-row-top", "add-row-bottom", "add-row-down", "add-row-up",
  "update-row", "copy-row", "copy-row-top", "copy-row-bottom", "copy-row-down", "copy-row-up", "copy-rows", "add-columns",
  "move-row-top", "move-row-up", "move-row-down", "move-row-bottom", "replace-columns", "update-cell", "show-columns",
  "hide-columns", "change-column-label", "save-row", "cancel-row", "after-add-row", "after-delete-row", "after-save-row",
  "after-cancel-row", "edit-row", "change-page", "change-sort", "change-filter", "change-column-filters",
  "copy-selected-rows-clipboard", "validate-row", "verify-row-validation", "tree-branch"
];

const GRID_ACTION_SET = new Set(GRID_ACTIONS);

/**
 * Check if an action type is a client action of a grid
 * @param {string} type Action type
 * @return {boolean} The action is a client action of a grid
 */
export const isGridAction = (type: unknown): boolean => GRID_ACTION_SET.has(type as string);
