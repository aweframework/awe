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
  public static final String GRID_PAGE_SIZE = "grid-page-size";
  public static final String GRID_LOADER = "grid-loader";
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

  // Context menu
  public static final String CONTEXT_MENU = "context-menu";
  public static final String CONTEXT_MENU_OPTION = "context-menu-option";
  public static final String CONTEXT_MENU_LINK = "context-menu-link";

  // Application menu and info dropdowns
  public static final String MENU = "menu";
  public static final String MENU_OPTION = "menu-option";
  public static final String MENU_LINK = "menu-link";
  public static final String MENU_DROPDOWN = "menu-dropdown";
  public static final String MENU_SUBMENU = "menu-submenu";
  // AngularJS only: toggle of an info dropdown
  public static final String INFO_DROPDOWN_TOGGLE = "info-dropdown-toggle";
  // Info dropdown (the element that carries the id) and info button
  public static final String INFO_DROPDOWN = "info-dropdown";
  public static final String INFO_BUTTON = "info-button";
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
  // AngularJS only: popover of a message or of the help (the React client has no popover)
  public static final String POPOVER = "popover";
  public static final String HELP_POPOVER = "help-popover";

  // Modal dialogs (owner: dialog id)
  public static final String DIALOG = "dialog";

  // Log viewer: the element that holds the text of the log
  public static final String LOG_VIEWER = "log-viewer";

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
