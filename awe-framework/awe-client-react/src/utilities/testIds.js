/**
 * Fixed vocabulary of "data-testid" values for the React client.
 *
 * It uses the SAME values as the AngularJS client (awe-client-angular, js/awe/data/testIds.js) for the same concepts,
 * so a test or a Selenium instruction can locate a component part whatever the client renders. A Jest test fails if
 * a shared value drifts. The entries marked "React only" name parts that PrimeReact renders and AngularJS does not.
 *
 * A value names the PART of a component (never an instance). The instance is identified by the attributes AWE
 * renders ("criterion-id", "grid-id", "tree-grid-id", "row-id", "column-id", "option-id", "name"...) or, for DOM that
 * PrimeReact appends to the body (dropdown panels, calendars, dialogs...), by the owner attribute "data-testid-owner".
 */
export const TestIds = Object.freeze({
  // Real control of every criterion (input, textarea, checkbox, radio...)
  criterionInput: "criterion-input",
  // React only: unit addon of a criterion (the text after the input, such as "EUR")
  criterionUnit: "criterion-unit",
  // Selectors: container, chosen value, search input, multiple choice (and its close icon), overlay panel and options
  select: "select",
  selectValue: "select-value",
  selectSearch: "select-search",
  selectChoice: "select-choice",
  selectChoiceClose: "select-choice-close",
  selectDropdown: "select-dropdown",
  // React only: arrow that opens the overlay panel (the middle of a short select can be its clear icon)
  selectTrigger: "select-trigger",
  selectOption: "select-option",
  // Date criteria: calendar overlay and its day, month and year cells
  datepicker: "datepicker",
  datepickerDay: "datepicker-day",
  datepickerMonth: "datepicker-month",
  datepickerYear: "datepicker-year",
  // Uploader criterion: name of the uploaded file and clear action
  uploadFilename: "upload-filename",
  uploadClear: "upload-clear",
  // Grids and tree grids: the instance is identified by "grid-id" / "tree-grid-id", "row-id" and "column-id"
  grid: "grid",
  gridViewport: "grid-viewport",
  gridHeaderCell: "grid-header-cell",
  gridHeaderCheckbox: "grid-header-checkbox",
  gridRow: "grid-row",
  gridCell: "grid-cell",
  gridRowCheckbox: "grid-row-checkbox",
  // React only: button that starts the edition of a row
  gridRowEdit: "grid-row-edit",
  gridRowSave: "grid-row-save",
  gridRowCancel: "grid-row-cancel",
  gridPagination: "grid-pagination",
  gridPagePrevious: "grid-page-previous",
  gridPageNext: "grid-page-next",
  gridPageSize: "grid-page-size",
  gridLoader: "grid-loader",
  // Tree grids: expand/collapse icon of a row
  treeIcon: "tree-icon",
  // Icon column of a grid (the element that shows the icon; "data-icon" carries the icon of the cell value)
  columnIcon: "column-icon",
  // Tab criterion: the tab list, a tab header ("option-id") with its link and label, the content pane
  tabList: "tab-list",
  tab: "tab",
  tabLink: "tab-link",
  tabLabel: "tab-label",
  tabPane: "tab-pane",
  // Wizard criterion: a step (the button of the step header, it carries "option-id"), its number (React only) and a
  // content pane
  wizardStep: "wizard-step",
  wizardStepNumber: "wizard-step-number",
  wizardPane: "wizard-pane",
  // Buttons (the element that carries the button id)
  button: "button",
  // Context menu, its options (the element that carries "option-id"), their links and nested menus
  contextMenu: "context-menu",
  contextMenuOption: "context-menu-option",
  contextMenuLink: "context-menu-link",
  contextSubmenu: "context-submenu",
  // Application menu: the menu, an option (with "option-name"), its link (the element that carries "name"), the first
  // level dropdown and the nested submenus
  menu: "menu",
  menuOption: "menu-option",
  menuLink: "menu-link",
  menuDropdown: "menu-dropdown",
  menuSubmenu: "menu-submenu",
  // React only: avatar of the logged user in the header (the avatar itself and the user name that goes with it)
  avatar: "avatar",
  avatarName: "avatar-name",
  // React only: chart (the instance is identified by "chart-id"); "data-rendered" tells that the chart was drawn
  chart: "chart",
  // React only: tag list (the instance is identified by "tag-list-id")
  tagList: "tag-list",
  // Log viewer: the element that holds the text of the log
  logViewer: "log-viewer",
  // Info dropdowns (the element that carries the id), its toggle and menu, and info buttons
  infoDropdown: "info-dropdown",
  infoDropdownToggle: "info-dropdown-toggle",
  infoDropdownMenu: "info-dropdown-menu",
  infoButton: "info-button",
  // Messages: alerts (toasts) with their title, text and close icon
  alert: "alert",
  alertTitle: "alert-title",
  alertMessage: "alert-message",
  alertClose: "alert-close",
  // Modal dialogs and the confirm dialog with its buttons
  dialog: "dialog",
  dialogClose: "dialog-close",
  confirmDialog: "confirm-dialog",
  confirmAccept: "confirm-accept",
  confirmCancel: "confirm-cancel",
  // Loaders: component loaders ("loader"; grids use "grid-loader") and the spinner shown while a view is loading
  loader: "loader",
  loadingSpinner: "loading-spinner"
});

/**
 * Attribute names that carry the test hooks. State is exposed as data attributes ("true"/"false") so tests do not
 * depend on PrimeReact state classes.
 */
export const TestAttributes = Object.freeze({
  testId: "data-testid",
  owner: "data-testid-owner",
  selected: "data-selected",
  // React only: a grid row is being edited (it is independent from the selection: a multiselect grid can toggle the
  // selection of a row while the user double clicks it to edit it)
  editing: "data-editing",
  active: "data-active",
  disabled: "data-disabled",
  outsideMonth: "data-outside-month",
  open: "data-open",
  expanded: "data-expanded",
  loading: "data-loading",
  completed: "data-completed",
  // React only: the component drew its content (a chart)
  rendered: "data-rendered",
  // React only: number of a wizard step (the step shows an icon instead of its number when it has one)
  stepNumber: "data-step-number",
  type: "data-type",
  container: "data-container",
  // Icon shown by an icon column
  icon: "data-icon",
  // React only: value of a control whose text repeats it (the page size of a grid)
  value: "data-value"
});

/**
 * State options of {@link testHook}, mapped to the attribute that carries them
 */
const STATE_ATTRIBUTES = {
  selected: TestAttributes.selected,
  editing: TestAttributes.editing,
  active: TestAttributes.active,
  disabled: TestAttributes.disabled,
  outsideMonth: TestAttributes.outsideMonth,
  open: TestAttributes.open,
  expanded: TestAttributes.expanded,
  loading: TestAttributes.loading,
  completed: TestAttributes.completed,
  rendered: TestAttributes.rendered
};

/**
 * Build the attributes of a test hook, ready to spread on an element or to return from a PrimeReact "pt" function
 * @param {string} testId One of the {@link TestIds}
 * @param {object} [options] Hook options
 * @param {string} [options.owner] Id of the component that owns an overlay rendered outside of it
 * @param {boolean} [options.selected] Boolean states (selected, active, disabled, outsideMonth, open, expanded,
 * loading, completed, rendered, editing): rendered as "true"/"false" and skipped when undefined
 * @param {string} [options.type] Message type
 * @param {string} [options.container] Container of a grid viewport
 * @param {object} [options.attributes] Extra attributes (AWE identifiers such as "row-id")
 * @returns {object} Attributes
 */
export function testHook(testId, options = {}) {
  const { owner, type, container, attributes = {} } = options;
  const hook = { [TestAttributes.testId]: testId };
  if (owner !== undefined && owner !== null) {
    hook[TestAttributes.owner] = owner;
  }
  Object.entries(STATE_ATTRIBUTES).forEach(([option, attribute]) => {
    if (options[option] !== undefined && options[option] !== null) {
      hook[attribute] = String(!!options[option]);
    }
  });
  if (type !== undefined) {
    hook[TestAttributes.type] = type;
  }
  if (container !== undefined) {
    hook[TestAttributes.container] = container;
  }
  return { ...hook, ...attributes };
}
