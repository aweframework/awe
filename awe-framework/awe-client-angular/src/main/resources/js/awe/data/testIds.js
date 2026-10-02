/**
 * Fixed vocabulary of "data-testid" values.
 *
 * A value names the PART of a component (never an instance). The instance is identified by the attributes AWE already
 * renders ("criterion-id", "grid-id", "row-id", "column-id") or, for plugin DOM that lives outside the component
 * (select2 dropdown, datepicker popup), by the owner attribute "data-testid-owner".
 */
export const TestIds = Object.freeze({
  // Real control of every criterion (input, textarea, hidden input, text value...)
  criterionInput: "criterion-input",
  // Selectors (select2): container, chosen value, search input, multiple choice (and its close link), global dropdown and its options
  select: "select",
  selectValue: "select-value",
  selectSearch: "select-search",
  selectChoice: "select-choice",
  selectChoiceClose: "select-choice-close",
  selectDropdown: "select-dropdown",
  selectOption: "select-option",
  // Date criteria (bootstrap-datepicker): popup and its day, month and year cells
  datepicker: "datepicker",
  datepickerDay: "datepicker-day",
  datepickerMonth: "datepicker-month",
  datepickerYear: "datepicker-year",
  // Uploader criterion: name of the uploaded file and clear action
  uploadFilename: "upload-filename",
  uploadClear: "upload-clear",
  // Grids (ui-grid and tree grids): the instance is identified by "grid-id" / "tree-grid-id", "row-id" and "column-id".
  // A cell hook goes on the element that carries the "column-id", whatever the cell renders (value, editor, checkbox).
  grid: "grid",
  gridViewport: "grid-viewport",
  gridHeaderCell: "grid-header-cell",
  gridHeaderCheckbox: "grid-header-checkbox",
  gridRow: "grid-row",
  gridCell: "grid-cell",
  gridRowCheckbox: "grid-row-checkbox",
  gridRowSave: "grid-row-save",
  gridRowCancel: "grid-row-cancel",
  gridPagination: "grid-pagination",
  gridPagePrevious: "grid-page-previous",
  gridPageNext: "grid-page-next",
  gridGotoPage: "grid-goto-page",
  gridPageSize: "grid-page-size",
  gridLoader: "grid-loader",
  // Tree grids: expand/collapse icon of a row and the expand/collapse all icon of the header
  treeIcon: "tree-icon",
  treeHeaderIcon: "tree-header-icon",
  // Icon column of a grid (the element that shows the icon; "data-icon" carries the icon of the cell value)
  columnIcon: "column-icon",
  // Tab criterion (the tab list, a tab header with its link and label, the content pane) and the tabdrop "more" menu
  tabList: "tab-list",
  tab: "tab",
  tabLink: "tab-link",
  tabLabel: "tab-label",
  tabPane: "tab-pane",
  tabdrop: "tabdrop",
  tabdropToggle: "tabdrop-toggle",
  tabdropMenu: "tabdrop-menu",
  // Wizard criterion: a step header and a content pane
  wizardStep: "wizard-step",
  wizardPane: "wizard-pane",
  // Buttons (the element that carries the button id)
  button: "button",
  // Context menu, its options (the element that carries "option-id"), their links and nested menus
  contextMenu: "context-menu",
  contextMenuOption: "context-menu-option",
  contextMenuLink: "context-menu-link",
  contextSubmenu: "context-submenu",
  // Application menu: the menu, an option (li), its link (the element that carries "name"), the first level dropdown
  // and the nested submenus
  menu: "menu",
  menuOption: "menu-option",
  menuLink: "menu-link",
  menuDropdown: "menu-dropdown",
  menuSubmenu: "menu-submenu",
  // Info dropdowns (the element that carries "info-dropdown-id"), its toggle and menu, and info buttons
  infoDropdown: "info-dropdown",
  infoDropdownToggle: "info-dropdown-toggle",
  infoDropdownMenu: "info-dropdown-menu",
  infoButton: "info-button",
  infoButtonLink: "info-button-link",
  // Messages: alerts of the alert zone, the Bootstrap popover shown over a component and the help popover (always
  // rendered by the "awe-help" directive: it is displayed only while "data-open" is "true")
  alert: "alert",
  alertTitle: "alert-title",
  alertMessage: "alert-message",
  alertClose: "alert-close",
  popover: "popover",
  popoverTitle: "popover-title",
  popoverContent: "popover-content",
  helpPopover: "help-popover",
  // Log viewer: the element that holds the text of the log
  logViewer: "log-viewer",
  // Modal dialogs: the dialog (owner: dialog id) with its close button, and the confirm dialog with its buttons
  dialog: "dialog",
  dialogClose: "dialog-close",
  confirmDialog: "confirm-dialog",
  confirmAccept: "confirm-accept",
  confirmCancel: "confirm-cancel",
  // Loaders: component loaders ("loader"; grids use "grid-loader") and the global loading bar with its spinner
  loader: "loader",
  loadingBar: "loading-bar",
  loadingSpinner: "loading-spinner"
});

/**
 * Attribute names that carry the test hooks. State is exposed as data attributes ("true"/"false") so tests do not
 * depend on library state classes.
 */
export const TestAttributes = Object.freeze({
  testId: "data-testid",
  owner: "data-testid-owner",
  selected: "data-selected",
  active: "data-active",
  disabled: "data-disabled",
  outsideMonth: "data-outside-month",
  // Menu options and branches that are open, tree rows that are expanded or loading, finished wizard steps
  open: "data-open",
  expanded: "data-expanded",
  loading: "data-loading",
  completed: "data-completed",
  // Message type (success, info, warning, danger) and the grid container a viewport belongs to (body, left, right)
  type: "data-type",
  container: "data-container",
  // Icon shown by an icon column
  icon: "data-icon"
});
