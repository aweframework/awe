package com.almis.awe.testing.selenium;

/**
 * Fixed vocabulary of the {@code data-testid} values rendered by the AWE clients.
 *
 * <p>A value names the PART of a component, never an instance. The instance is identified with the attributes AWE
 * already renders ({@code criterion-id}, {@code grid-id}, {@code row-id}, {@code column-id}...) or, for the DOM that a
 * library appends outside the component (select dropdown, datepicker popup), with the owner attribute
 * {@link TestAttributes#OWNER}.</p>
 *
 * <p>Each constant mirrors the entry of the JavaScript constant {@code TestIds} (awe-client-angular,
 * {@code js/awe/data/testIds.js}) whose name is the camel case of the constant name (for instance
 * {@code SELECT_DROPDOWN} is {@code selectDropdown}). Only the values that the Java Selenium layer uses are declared;
 * {@code TestIdsVocabularyTest} fails if one of them drifts from the JavaScript vocabulary.</p>
 */
public final class TestIds {

  // Real control of every criterion (input, textarea, hidden input, checkbox...)
  public static final String CRITERION_INPUT = "criterion-input";

  // Selectors: container, chosen value, search input, multiple choice (and its close link), dropdown and its options
  public static final String SELECT = "select";
  public static final String SELECT_VALUE = "select-value";
  public static final String SELECT_SEARCH = "select-search";
  public static final String SELECT_CHOICE = "select-choice";
  public static final String SELECT_CHOICE_CLOSE = "select-choice-close";
  public static final String SELECT_DROPDOWN = "select-dropdown";
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
  public static final String GRID_CELL = "grid-cell";
  public static final String GRID_ROW_SAVE = "grid-row-save";
  public static final String GRID_LOADER = "grid-loader";
  public static final String TREE_ICON = "tree-icon";

  // Tabs and the "more" menu that holds the tabs that do not fit
  public static final String TAB_LIST = "tab-list";
  public static final String TAB = "tab";
  public static final String TAB_LABEL = "tab-label";
  public static final String TABDROP_TOGGLE = "tabdrop-toggle";
  public static final String TABDROP_MENU = "tabdrop-menu";

  // Context menu
  public static final String CONTEXT_MENU = "context-menu";
  public static final String CONTEXT_MENU_OPTION = "context-menu-option";
  public static final String CONTEXT_MENU_LINK = "context-menu-link";

  // Application menu and info dropdowns
  public static final String MENU_LINK = "menu-link";
  public static final String MENU_DROPDOWN = "menu-dropdown";
  public static final String MENU_SUBMENU = "menu-submenu";
  public static final String INFO_DROPDOWN_TOGGLE = "info-dropdown-toggle";

  // Messages
  public static final String ALERT = "alert";
  public static final String ALERT_CLOSE = "alert-close";
  public static final String POPOVER = "popover";

  // Loaders
  public static final String LOADER = "loader";
  public static final String LOADING_BAR = "loading-bar";

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
