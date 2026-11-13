package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Advanced search of a window (#790) on a list of customers: the essential criteria first, then the advanced block, shown
 * inline below its Advanced filters row or in a side panel (a tab each), the counter of the advanced criteria in use, the
 * filters in effect with their chips, the search of the side panel in its own grid, and the open state of the inline block,
 * which is remembered for the user. One flow per window (the inline block, the side panel) and one for the open state that
 * is remembered: every test opens its screen with the inline block closed, so each one can run on its own.
 *
 * <p>Proven in a lower layer, not here: the customers each search returns ({@code AdvancedSearchCustomersTest}), a window
 * without advanced block or without saved searches (Jest {@code windowAdvancedSearch.test.js},
 * {@code windowSavedSearches.test.js}), and the saved searches of the inline window: the server rules in
 * {@code SavedSearchMaintainTest}, the client rules in {@code windowSavedSearches.test.js}. The saved searches flow runs in
 * the React application only ({@code awe-boot-react} AdvancedSearchTestsIT).</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("RegressionWebsocketPrintIT")
class AdvancedSearchTestsIT extends AbstractSessionTests {

  private static final String INLINE_WINDOW = "WinAdvInl";
  private static final String PANEL_WINDOW = "WinAdvPnl";
  private static final String PANEL_GRID = "GrdAdvPnl";
  private static final String NAME = "AdvInlNam";
  private static final String STATUS = "AdvInlSta";
  private static final String COUNTRY = "AdvInlCtr";
  private static final String SEGMENT = "AdvInlSeg";
  private static final String AMOUNT = "AdvInlAmt";
  private static final String CREATED = "AdvInlDat";
  private static final String PRIORITY = "AdvInlPri";
  private static final String PANEL_NAME = "AdvPnlNam";
  private static final String PANEL_COUNTRY = "AdvPnlCtr";
  private static final String PANEL_PRIORITY = "AdvPnlPri";
  private static final String PANEL_NAME_COLUMN = "PnlNam";
  private static final String PANEL_COUNTRY_COLUMN = "PnlCtr";
  private static final String SPAIN = "Spain";
  private static final String FRANCE = "France";
  private static final String RETAIL = "Retail";

  AdvancedSearchTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Open the advanced search test screen from another one, so it is rendered again: choosing the screen that is shown
   * keeps it as it is (expanded advanced criteria, values). The other screen is the buttons test screen, an option of the
   * first level of the test menu without a grid. The inline block starts as the user left it (#790)
   */
  private void showAdvancedSearchScreen() {
    gotoScreen("test", "button-test");
    gotoScreen("test", "matrix-test-advanced-search");
    checkCriterionVisible(NAME);
  }

  /**
   * Open the advanced search test screen with the inline block closed, whatever an earlier test or run left
   */
  private void openAdvancedSearchScreen() {
    showAdvancedSearchScreen();
    closeAdvancedFilters(INLINE_WINDOW);
  }

  /**
   * Inline window: the essential criteria come first, then the Advanced filters row, and the toggle opens the advanced
   * block below the row, which does not move. The badge counts the advanced criteria whose value differs from their
   * default (the country with a default value counts only when it changes, a flag counts on its own), and the chips show
   * every filter in effect, the hidden advanced ones marked. Removing a chip restores the criterion to its default, also a
   * marked one, without opening the block, and Clear all restores them all. A dependency of an advanced criterion fires
   * while it is hidden, and the validation of a hidden advanced criterion that fails reveals it
   */
  @Test
  void t010_inlineBlockTogglesCountsChipsDependenciesAndValidation() {
    setTestTitle("Advanced search: the inline block, its badge and chips, a hidden dependency and a hidden validation");
    openAdvancedSearchScreen();

    // Collapsed: only the essential criteria, above the row, and the default country is not a filter in use
    checkCriterionVisible(STATUS);
    checkCriterionAboveAdvancedFilters(INLINE_WINDOW, NAME);
    checkCriterionAboveAdvancedFilters(INLINE_WINDOW, STATUS);
    checkCriterionNotVisible(COUNTRY);
    checkCriterionNotVisible(SEGMENT);
    checkCriterionNotVisible(AMOUNT);
    checkCriterionNotVisible(CREATED);
    checkCriterionNotVisible(PRIORITY);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    checkNoFilterChips(INLINE_WINDOW);

    // Expanded below the row, which stays where it was
    double toggleTop = getAdvancedFiltersToggleTop(INLINE_WINDOW);
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionVisible(COUNTRY);
    checkCriterionVisible(SEGMENT);
    checkCriterionVisible(AMOUNT);
    checkCriterionVisible(CREATED);
    checkCriterionVisible(PRIORITY);
    checkAdvancedFiltersToggleTop(INLINE_WINDOW, toggleTop);
    checkCriterionBelowAdvancedFilters(INLINE_WINDOW, COUNTRY);
    checkCriterionBelowAdvancedFilters(INLINE_WINDOW, CREATED);
    checkCriterionBelowAdvancedFilters(INLINE_WINDOW, PRIORITY);
    checkCriterionAboveAdvancedFilters(INLINE_WINDOW, NAME);

    // Collapsed again, from the same place
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionNotVisible(COUNTRY);
    checkCriterionNotVisible(PRIORITY);
    checkCriterionVisible(NAME);
    checkAdvancedFiltersToggleTop(INLINE_WINDOW, toggleTop);

    // The default value of a select is not a filter
    toggleAdvancedFilters(INLINE_WINDOW);
    checkSelectContents(COUNTRY, SPAIN);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    checkNoFilterChips(INLINE_WINDOW);

    // A segment, a flag and a changed country
    selectContain(SEGMENT, RETAIL);
    checkAdvancedFiltersCount(INLINE_WINDOW, 1);
    clickCheckbox(PRIORITY);
    checkAdvancedFiltersCount(INLINE_WINDOW, 2);
    selectContain(COUNTRY, FRANCE);
    checkAdvancedFiltersCount(INLINE_WINDOW, 3);
    checkFilterChip(INLINE_WINDOW, SEGMENT, false);
    checkFilterChip(INLINE_WINDOW, PRIORITY, false);
    checkFilterChip(INLINE_WINDOW, COUNTRY, false);

    // Removing a chip restores its default
    removeFilterChip(INLINE_WINDOW, COUNTRY);
    checkSelectContents(COUNTRY, SPAIN);
    checkNoFilterChip(INLINE_WINDOW, COUNTRY);
    checkAdvancedFiltersCount(INLINE_WINDOW, 2);

    // An essential criterion is a filter in effect too, but it is not counted
    writeText(NAME, "Atlas");
    checkFilterChip(INLINE_WINDOW, NAME, false);
    checkAdvancedFiltersCount(INLINE_WINDOW, 2);

    // Hidden advanced filters in effect stay in view, marked, and a marked chip clears its filter without opening the block
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionNotVisible(SEGMENT);
    checkFilterChip(INLINE_WINDOW, SEGMENT, true);
    checkFilterChip(INLINE_WINDOW, PRIORITY, true);
    checkFilterChip(INLINE_WINDOW, NAME, false);
    removeFilterChip(INLINE_WINDOW, SEGMENT);
    checkNoFilterChip(INLINE_WINDOW, SEGMENT);
    checkAdvancedFiltersCount(INLINE_WINDOW, 1);
    checkCriterionNotVisible(SEGMENT);

    // Clear all restores every criterion of the window
    clearAllFilters(INLINE_WINDOW);
    checkNoFilterChips(INLINE_WINDOW);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    toggleAdvancedFilters(INLINE_WINDOW);
    checkSelectContents(COUNTRY, SPAIN);
    checkCheckboxRadio(false, PRIORITY);
    checkCriterionContents(NAME, "");

    // A dependency fires while its criterion is hidden: inactive customers are never priority, so choosing the Inactive
    // status clears the hidden priority flag and its chip
    clickCheckbox(PRIORITY);
    toggleAdvancedFilters(INLINE_WINDOW);
    checkFilterChip(INLINE_WINDOW, PRIORITY, true);
    checkAdvancedFiltersCount(INLINE_WINDOW, 1);
    selectContain(STATUS, "Inactive");
    checkFilterChip(INLINE_WINDOW, STATUS, false);
    checkNoFilterChip(INLINE_WINDOW, PRIORITY);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCheckboxRadio(false, PRIORITY);
    clearAllFilters(INLINE_WINDOW);
    checkNoFilterChips(INLINE_WINDOW);

    // A validation error reveals the hidden advanced criterion
    writeText(AMOUNT, "2000000");
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionNotVisible(AMOUNT);
    clickButton("ButAdvInlSch");
    checkCriterionVisible(AMOUNT);
    checkValidationErrorVisible();
  }

  /**
   * Side-panel window: Advanced filters opens the advanced criteria in a side panel. Cancel gives them back the values they had
   * when it opened, Reset to defaults restores their defaults, and Show results closes it and fills its own grid. The side
   * panel is not remembered (#790): it is closed when the screen is opened again, and the inline block of the other window
   * keeps its own state
   */
  @Test
  void t020_sidePanelCancelResetShowResultsAndIsNotRemembered() {
    setTestTitle("Advanced search: side panel with Cancel, Reset to defaults and Show results, closed when opened again");
    openAdvancedSearchScreen();
    clickTab("AdvSchTab", "ENUM_ADV_SCH_SIDE_PANEL");
    checkCriterionVisible(PANEL_NAME);
    checkCriterionNotVisible(PANEL_COUNTRY);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);

    // Cancel
    toggleAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelOpen(PANEL_WINDOW);
    selectContain(PANEL_COUNTRY, FRANCE);
    checkFilterChip(PANEL_WINDOW, PANEL_COUNTRY, false);
    cancelAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);
    checkAdvancedFiltersCount(PANEL_WINDOW, 0);
    checkNoFilterChips(PANEL_WINDOW);

    // Reset to defaults
    toggleAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelOpen(PANEL_WINDOW);
    selectContain(PANEL_COUNTRY, FRANCE);
    checkFilterChip(PANEL_WINDOW, PANEL_COUNTRY, false);
    resetAdvancedFilters(PANEL_WINDOW);
    checkNoFilterChips(PANEL_WINDOW);
    checkAdvancedFiltersPanelOpen(PANEL_WINDOW);

    // Show results: priority customers of France in the grid of the window
    selectContain(PANEL_COUNTRY, FRANCE);
    clickCheckbox(PANEL_PRIORITY);
    applyAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);
    checkGridColumnOrder(PANEL_GRID, PANEL_NAME_COLUMN, "Fjord Energy", "Granite Pharma", "Iris Motors", "Juniper Systems");
    checkGridColumnOrder(PANEL_GRID, PANEL_COUNTRY_COLUMN, FRANCE, FRANCE, FRANCE, FRANCE);
    checkAdvancedFiltersCount(PANEL_WINDOW, 2);
    checkFilterChip(PANEL_WINDOW, PANEL_COUNTRY, true);
    checkFilterChip(PANEL_WINDOW, PANEL_PRIORITY, true);

    // Opened and cancelled, the panel is closed when the screen is opened again
    toggleAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelOpen(PANEL_WINDOW);
    cancelAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    clickTab("AdvSchTab", "ENUM_ADV_SCH_SIDE_PANEL");
    checkCriterionVisible(PANEL_NAME);
    checkCriterionNotVisible(PANEL_COUNTRY);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);
  }

  /**
   * The inline block is remembered for the user (#790): left open, it is open when the screen is opened again and after
   * logging out and in again; left closed, it is closed when the screen is opened again, so the user is left as it was
   */
  @Test
  void t030_inlineBlockIsRememberedWhenTheScreenIsOpenedAgainAndAfterLoggingOutAndIn() {
    setTestTitle("Advanced search: the inline block is opened again as the user left it, also after logging out and in");
    openAdvancedSearchScreen();
    checkCriterionNotVisible(COUNTRY);

    // Left open
    toggleAdvancedFilters(INLINE_WINDOW);
    checkAdvancedFiltersOpen(INLINE_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersOpen(INLINE_WINDOW);
    checkCriterionVisible(COUNTRY);
    checkCriterionBelowAdvancedFilters(INLINE_WINDOW, COUNTRY);

    // A new session
    ensureLoggedOut();
    checkLogin("test", "test", "Manager (test)");
    ensureModule("Test", "test");
    showAdvancedSearchScreen();
    checkAdvancedFiltersOpen(INLINE_WINDOW);
    checkCriterionVisible(COUNTRY);

    // Left closed
    toggleAdvancedFilters(INLINE_WINDOW);
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    checkCriterionNotVisible(COUNTRY);
  }
}
