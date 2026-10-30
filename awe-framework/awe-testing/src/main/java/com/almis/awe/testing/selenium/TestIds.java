package com.almis.awe.testing.selenium;

/**
 * Fixed vocabulary of the {@code data-testid} values rendered by the AWE clients.
 *
 * <p>A value names the PART of a component, never an instance. The instance is identified with the attributes AWE
 * already renders ({@code criterion-id}, {@code grid-id}, {@code row-id}, {@code column-id}...) or, for the DOM that a
 * library appends outside the component (select dropdown, datepicker popup), with the owner attribute
 * {@link TestAttributes#OWNER}.</p>
 *
 * <p>Each constant mirrors the entry of the JavaScript constant {@code TestIds} whose name is the camel case of the
 * constant name (for instance {@code SELECT_DROPDOWN} is {@code selectDropdown}), in the AngularJS client
 * ({@code awe-client-angular}, {@code js/awe/data/testIds.js}), the React client ({@code awe-client-react},
 * {@code src/utilities/testIds.js}) or both. The two clients share the values of the concepts they have in common; the
 * constants marked "React only" or "AngularJS only" name parts that only one client renders. Only the values that the
 * Java Selenium layer (or the suites that extend it) uses are declared; {@code TestIdsVocabularyTest} fails if one of
 * them drifts from the JavaScript vocabularies.</p>
 */
public final class TestIds {

  // Real control of every criterion (input, textarea, hidden input, checkbox...)
  public static final String CRITERION_INPUT = "criterion-input";
  // React only: unit addon of a criterion (the text after the input)
  public static final String CRITERION_UNIT = "criterion-unit";
  // React only: validation error of a criterion (AngularJS shows it in the shared "error-container")
  public static final String CRITERION_ERROR = "criterion-error";

  // Selectors: container, chosen value, search input, multiple choice (and its close link), dropdown and its options
  public static final String SELECT = "select";
  public static final String SELECT_VALUE = "select-value";
  public static final String SELECT_SEARCH = "select-search";
  public static final String SELECT_CHOICE = "select-choice";
  public static final String SELECT_CHOICE_CLOSE = "select-choice-close";
  public static final String SELECT_DROPDOWN = "select-dropdown";
  /** React only: arrow that opens the panel of a select */
  public static final String SELECT_TRIGGER = "select-trigger";
  public static final String SELECT_OPTION = "select-option";

  // Date criteria: popup and its day, month and year cells
  public static final String DATEPICKER = "datepicker";
  public static final String DATEPICKER_DAY = "datepicker-day";
  public static final String DATEPICKER_MONTH = "datepicker-month";
  public static final String DATEPICKER_YEAR = "datepicker-year";

  // Grids and trees
  public static final String GRID = "grid";
  public static final String GRID_VIEWPORT = "grid-viewport";
  public static final String GRID_HEADER_CELL = "grid-header-cell";
  public static final String GRID_HEADER_CHECKBOX = "grid-header-checkbox";
  public static final String GRID_ROW = "grid-row";
  public static final String GRID_ROW_CHECKBOX = "grid-row-checkbox";
  public static final String GRID_CELL = "grid-cell";
  public static final String GRID_ROW_SAVE = "grid-row-save";
  // React only: button that starts the edition of a row
  public static final String GRID_ROW_EDIT = "grid-row-edit";
  // Button that opens and closes the detail of a row, and the detail area under the row
  public static final String GRID_ROW_DETAIL_TOGGLE = "grid-row-detail-toggle";
  public static final String GRID_ROW_DETAIL = "grid-row-detail";
  public static final String GRID_PAGE_SIZE = "grid-page-size";
  public static final String GRID_PAGE_NEXT = "grid-page-next";
  public static final String GRID_GOTO_PAGE = "grid-goto-page";
  public static final String GRID_PAGINATION = "grid-pagination";
  public static final String GRID_PAGINATION_INFO = "grid-pagination-info";
  public static final String GRID_EMPTY = "grid-empty";
  public static final String GRID_EMPTY_TITLE = "grid-empty-title";
  public static final String GRID_EMPTY_TEXT = "grid-empty-text";
  public static final String GRID_EMPTY_ACTION = "grid-empty-action";
  public static final String GRID_LOADER = "grid-loader";
  // Grid toolbar: the toolbar, its Columns and Maximize buttons and the Columns dialog with its parts
  public static final String GRID_TOOLBAR = "grid-toolbar";
  public static final String GRID_GROUP_HEADER = "grid-group-header";
  public static final String GRID_COLUMNS_BUTTON = "grid-columns-button";
  public static final String GRID_MAXIMIZE_BUTTON = "grid-maximize-button";
  // Quick filters of the grid toolbar (identified by "criterion-id") and each of their options ("option-id", its value)
  public static final String GRID_QUICK_FILTER = "grid-quick-filter";
  public static final String GRID_QUICK_FILTER_OPTION = "grid-quick-filter-option";
  // Search of the grid toolbar (identified by "criterion-id"; its text box is its criterion input) and its clear button
  public static final String GRID_SEARCH = "grid-search";
  public static final String GRID_SEARCH_CLEAR = "grid-search-clear";
  // Selection bar of the grid (identified by its owner, the grid; "data-count" is the number of selected rows) and its text
  public static final String GRID_SELECTION_BAR = "grid-selection-bar";
  public static final String GRID_SELECTION_COUNT = "grid-selection-count";
  // Windows: the window root (identified by its owner) and its Maximize button
  public static final String WINDOW = "window";
  public static final String WINDOW_MAXIMIZE_BUTTON = "window-maximize-button";
  // Advanced search of a window (#790; owner: window id): the toggle of the advanced criteria with its counter badge,
  // the filters in effect (row, chip, chip remove button, Clear all) and the side panel with its footer buttons
  public static final String WINDOW_ADVANCED_TOGGLE = "window-advanced-toggle";
  public static final String WINDOW_ADVANCED_COUNT = "window-advanced-count";
  public static final String WINDOW_FILTER_CHIPS = "window-filter-chips";
  public static final String WINDOW_FILTER_CHIP = "window-filter-chip";
  public static final String WINDOW_FILTER_CHIP_REMOVE = "window-filter-chip-remove";
  public static final String WINDOW_FILTER_CLEAR_ALL = "window-filter-clear-all";
  public static final String WINDOW_ADVANCED_PANEL = "window-advanced-panel";
  public static final String WINDOW_ADVANCED_RESET = "window-advanced-reset";
  public static final String WINDOW_ADVANCED_CANCEL = "window-advanced-cancel";
  public static final String WINDOW_ADVANCED_APPLY = "window-advanced-apply";
  // Tabs: the whole tab component (list and content, identified by its owner) and its Maximize button
  public static final String TABS = "tabs";
  public static final String TAB_MAXIMIZE_BUTTON = "tab-maximize-button";
  public static final String GRID_COLUMNS_DIALOG = "grid-columns-dialog";
  public static final String GRID_COLUMN_GROUP = "grid-column-group";
  public static final String GRID_COLUMN_ITEM = "grid-column-item";
  public static final String GRID_COLUMN_CHECKBOX = "grid-column-checkbox";
  public static final String GRID_COLUMN_HANDLE = "grid-column-handle";
  public static final String GRID_COLUMN_UP = "grid-column-up";
  public static final String GRID_COLUMN_DOWN = "grid-column-down";
  public static final String GRID_COLUMNS_COUNTER = "grid-columns-counter";
  public static final String GRID_COLUMNS_SHOW_ALL = "grid-columns-show-all";
  public static final String GRID_COLUMNS_RESET = "grid-columns-reset";
  public static final String GRID_COLUMNS_SAVE = "grid-columns-save";
  public static final String GRID_COLUMNS_CANCEL = "grid-columns-cancel";
  public static final String GRID_COLUMNS_CLOSE = "grid-columns-close";
  // Row grouping: group and subtotal rows (data-group-path, data-group-level), the toggle of a group, the Expand all
  // and Collapse all buttons, the Group by button and its panel (owner: the grid), its No grouping action and the
  // option of a column (data-column)
  public static final String GRID_GROUP_ROW = "grid-group-row";
  public static final String GRID_SUBTOTAL_ROW = "grid-subtotal-row";
  public static final String GRID_GROUP_TOGGLE = "grid-group-toggle";
  public static final String GRID_GROUP_EXPAND_ALL = "grid-group-expand-all";
  public static final String GRID_GROUP_COLLAPSE_ALL = "grid-group-collapse-all";
  public static final String GRID_GROUP_BY = "grid-group-by";
  public static final String GRID_GROUP_BY_PANEL = "grid-group-by-panel";
  public static final String GRID_GROUP_NONE = "grid-group-none";
  public static final String GRID_GROUP_OPTION = "grid-group-option";
  public static final String TREE_ICON = "tree-icon";
  public static final String COLUMN_ICON = "column-icon";

  // Tabs and the "more" menu that holds the tabs that do not fit
  public static final String TAB_LIST = "tab-list";
  public static final String TAB = "tab";
  public static final String TAB_LABEL = "tab-label";
  // AngularJS only: the React tab list has no "more" menu
  public static final String TABDROP_TOGGLE = "tabdrop-toggle";
  public static final String TABDROP_MENU = "tabdrop-menu";

  // Wizard steps
  public static final String WIZARD_STEP = "wizard-step";

  // Date range criterion: the start and the end inputs and a quick preset button (it carries "option-id", the preset code)
  public static final String DATE_RANGE_FROM = "date-range-from";
  public static final String DATE_RANGE_TO = "date-range-to";
  public static final String DATE_RANGE_PRESET = "date-range-preset";

  // Uploader criterion: name of the uploaded file, clear action, drop zone, image preview and camera button
  public static final String UPLOAD_FILENAME = "upload-filename";
  public static final String UPLOAD_CLEAR = "upload-clear";
  public static final String UPLOAD_DROP_ZONE = "upload-drop-zone";
  public static final String UPLOAD_PREVIEW = "upload-preview";
  public static final String UPLOAD_CAPTURE = "upload-capture";

  // Context menu
  public static final String CONTEXT_MENU = "context-menu";
  public static final String CONTEXT_MENU_OPTION = "context-menu-option";
  public static final String CONTEXT_MENU_LINK = "context-menu-link";

  // Application menu and info dropdowns
  public static final String MENU = "menu";
  public static final String MENU_OPTION = "menu-option";
  // Breadcrumb of the page (the path of menu options that leads to the screen)
  public static final String BREADCRUMB = "breadcrumb";
  public static final String MENU_LINK = "menu-link";
  public static final String MENU_DROPDOWN = "menu-dropdown";
  public static final String MENU_SUBMENU = "menu-submenu";
  // AngularJS only: toggle of an info dropdown
  public static final String INFO_DROPDOWN_TOGGLE = "info-dropdown-toggle";
  // Info dropdown (the element that carries the id) and info button
  public static final String INFO_DROPDOWN = "info-dropdown";
  public static final String INFO_BUTTON = "info-button";
  /** AngularJS only */
  public static final String INFO_BUTTON_LINK = "info-button-link";
  // React only: avatar of the logged user (it carries the id and the user name as title) and the name shown next to it
  public static final String AVATAR = "avatar";
  public static final String AVATAR_NAME = "avatar-name";
  // React only: the number of a wizard step
  public static final String WIZARD_STEP_NUMBER = "wizard-step-number";

  // Messages
  public static final String ALERT = "alert";
  public static final String ALERT_TITLE = "alert-title";
  public static final String ALERT_MESSAGE = "alert-message";
  public static final String ALERT_CLOSE = "alert-close";
  // Popover of a target message (owner: id of the component the message points at) with its title and content, and
  // popover containers anchored to a control (owner: popover id) with their close button (both clients)
  public static final String POPOVER = "popover";
  public static final String POPOVER_TITLE = "popover-title";
  public static final String POPOVER_CONTENT = "popover-content";
  public static final String POPOVER_CLOSE = "popover-close";
  // AngularJS only: popover of the help (the help of the React client is a tooltip)
  public static final String HELP_POPOVER = "help-popover";

  // Modal dialogs (owner: dialog id)
  public static final String DIALOG = "dialog";
  public static final String DIALOG_CLOSE = "dialog-close";
  // Side panels anchored to an edge of the screen and their close button (owner: side panel id), the title of their
  // header (the label of the panel, or the title of its identity header) and the subtitle and tags of the identity header
  public static final String SIDE_PANEL = "side-panel";
  public static final String SIDE_PANEL_CLOSE = "side-panel-close";
  public static final String SIDE_PANEL_TITLE = "side-panel-title";
  public static final String SIDE_PANEL_SUBTITLE = "side-panel-subtitle";
  public static final String SIDE_PANEL_TAG = "side-panel-tag";

  // Chart (the instance is identified by "chart-id"; "data-rendered" tells that the chart was drawn)
  public static final String CHART = "chart";

  // Cards (owner: card id) with their value, their trend delta ("data-trend" and "data-tone"), their target progress and
  // their status chip ("data-tone")
  public static final String CARD = "card";
  public static final String CARD_VALUE = "card-value";
  public static final String CARD_DELTA = "card-delta";
  public static final String CARD_CHIP = "card-chip";
  public static final String CARD_PROGRESS = "card-progress";
  public static final String CARD_SPARKLINE = "card-sparkline";

  // Map (the instance is identified by "map-id"; "data-rendered" tells that its markers were drawn), its
  // markers ("map-id" and "row-id") and its route ("map-id")
  public static final String MAP = "map";
  public static final String MAP_MARKER = "map-marker";
  public static final String MAP_ROUTE = "map-route";
  // React only: tag list (the instance is identified by "tag-list-id")
  public static final String TAG_LIST = "tag-list";

  // Log viewer: the element that holds the text of the log
  public static final String LOG_VIEWER = "log-viewer";

  // File viewer: the element that holds the text of the file
  public static final String FILE_VIEWER = "file-viewer";

  // React only: error panel shown by an error boundary when a part of the application failed to render (the
  // instance is identified by "data-scope"), with its title, the message of the error, the component stack and buttons
  public static final String ERROR_BOUNDARY = "error-boundary";
  public static final String ERROR_BOUNDARY_TITLE = "error-boundary-title";
  public static final String ERROR_BOUNDARY_MESSAGE = "error-boundary-message";
  public static final String ERROR_BOUNDARY_DETAILS = "error-boundary-details";
  public static final String ERROR_BOUNDARY_RETRY = "error-boundary-retry";
  public static final String ERROR_BOUNDARY_RELOAD = "error-boundary-reload";

  // Loaders
  public static final String LOADER = "loader";
  // AngularJS only: loading bar of the page
  public static final String LOADING_BAR = "loading-bar";
  // Spinner shown while a view is loading (the React client has no loading bar)
  public static final String LOADING_SPINNER = "loading-spinner";

  private TestIds() {
    // Constants only
  }

  /**
   * Css selector of the elements that carry a hook
   *
   * @param testId Vocabulary value
   * @return Css selector, for instance {@code [data-testid='grid-row']}
   */
  public static String css(String testId) {
    return "[" + TestAttributes.TEST_ID + "='" + testId + "']";
  }

  /**
   * Xpath condition of the elements that carry a hook
   *
   * @param testId Vocabulary value
   * @return Xpath condition, for instance {@code @data-testid='grid-row'}
   */
  public static String xpath(String testId) {
    return "@" + TestAttributes.TEST_ID + "='" + testId + "'";
  }
}
