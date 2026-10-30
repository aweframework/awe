package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Advanced search of a window (#790) on a list of customers: the essential criteria first, then the advanced block, shown
 * inline below its Advanced filters row or in a side panel (a tab each), the counter of the advanced criteria in use, the
 * filters in effect with their chips, the search of each example in its own grid, and a window without advanced block.
 * Every test opens its screen, so each one can run on its own.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class AdvancedSearchTestsIT extends AbstractSessionTests {

  private static final String INLINE_WINDOW = "WinAdvInl";
  private static final String PANEL_WINDOW = "WinAdvPnl";
  private static final String INLINE_GRID = "GrdAdvInl";
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
  private static final String INLINE_NAME_COLUMN = "InlNam";
  private static final String INLINE_COUNTRY_COLUMN = "InlCtr";
  private static final String INLINE_SEGMENT_COLUMN = "InlSeg";
  private static final String INLINE_PRIORITY_COLUMN = "InlPri";
  private static final String PANEL_NAME_COLUMN = "PnlNam";
  private static final String PANEL_COUNTRY_COLUMN = "PnlCtr";
  private static final String SPAIN = "Spain";
  private static final String FRANCE = "France";
  private static final String RETAIL = "Retail";
  private static final String YES = "Yes";

  AdvancedSearchTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    // The application asks for a confirmation before logging out
    checkLogoutWithConfirmation();
  }

  /**
   * Open the advanced search test screen from another one, so it is rendered again: choosing the screen that is shown
   * keeps it as it is (expanded advanced criteria, values)
   */
  private void openAdvancedSearchScreen() {
    gotoScreen("test", "matrix", "matrix-test-side-panel");
    gotoScreen("test", "matrix-test-advanced-search");
    checkCriterionVisible(NAME);
  }

  /**
   * Inline window: the essential criteria come first, then the Advanced filters row, and the toggle opens the advanced
   * block below the row, which does not move
   */
  @Test
  void t010_inlineToggleShowsAndHidesTheAdvancedCriteria() {
    setTestTitle("Advanced search: the inline toggle shows and hides the advanced criteria below its row");
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
  }

  /**
   * The badge counts the advanced criteria whose value differs from their default (the country with a default value counts
   * only when it changes, a flag counts on its own), and the chips show every filter in effect, the hidden advanced ones
   * marked. Removing a chip restores the criterion to its default, and Clear all restores them all
   */
  @Test
  void t020_badgeAndChipsFollowTheCriteriaThatDifferFromTheirDefault() {
    setTestTitle("Advanced search: badge, chips, remove a chip and clear all");
    openAdvancedSearchScreen();
    toggleAdvancedFilters(INLINE_WINDOW);

    // The default value of a select is not a filter
    checkSelectContents(COUNTRY, SPAIN);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    checkNoFilterChips(INLINE_WINDOW);

    // A segment, a flag and a changed country
    selectContain(SEGMENT, "Retail");
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

    // Hidden advanced filters in effect stay in view, marked
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionNotVisible(SEGMENT);
    checkFilterChip(INLINE_WINDOW, SEGMENT, true);
    checkFilterChip(INLINE_WINDOW, PRIORITY, true);
    checkFilterChip(INLINE_WINDOW, NAME, false);

    // Clear all restores every criterion of the window
    clearAllFilters(INLINE_WINDOW);
    checkNoFilterChips(INLINE_WINDOW);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    toggleAdvancedFilters(INLINE_WINDOW);
    checkSelectContents(COUNTRY, SPAIN);
    checkCheckboxRadio(false, PRIORITY);
    checkCriterionContents(NAME, "");
  }

  /**
   * A dependency of an advanced criterion fires while it is hidden: inactive customers are never priority, so choosing the
   * Inactive status clears the hidden priority flag and its chip
   */
  @Test
  void t030_dependencyOfAHiddenAdvancedCriterionStillFires() {
    setTestTitle("Advanced search: the dependency of a hidden advanced criterion fires");
    openAdvancedSearchScreen();
    toggleAdvancedFilters(INLINE_WINDOW);
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
  }

  /**
   * The validation of a hidden advanced criterion that fails reveals it, so its error can be seen
   */
  @Test
  void t040_validationRevealsAHiddenAdvancedCriterion() {
    setTestTitle("Advanced search: a validation error reveals the hidden advanced criterion");
    openAdvancedSearchScreen();
    toggleAdvancedFilters(INLINE_WINDOW);
    writeText(AMOUNT, "2000000");
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionNotVisible(AMOUNT);

    clickButton("ButAdvInlSch");
    checkCriterionVisible(AMOUNT);
    checkValidationErrorVisible();
  }

  /**
   * The search of the inline window fills its grid with the customers that pass every filter, the default country
   * included, also while the advanced block is hidden, and a column per criterion shows why each one is there
   */
  @Test
  void t045_inlineSearchFiltersItsGridByTheAdvancedCriteria() {
    setTestTitle("Advanced search: the inline search filters its grid by the advanced criteria");
    openAdvancedSearchScreen();
    toggleAdvancedFilters(INLINE_WINDOW);

    // Retail customers of the default country, searched with the advanced block hidden
    selectContain(SEGMENT, RETAIL);
    toggleAdvancedFilters(INLINE_WINDOW);
    checkFilterChip(INLINE_WINDOW, SEGMENT, true);
    clickButton("ButAdvInlSch");
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Atlas Logistics", "Harbor Logistics", "Iris Labs", "Delta Logistics",
      "Ember Labs");
    checkGridColumnOrder(INLINE_GRID, INLINE_COUNTRY_COLUMN, SPAIN, SPAIN, SPAIN, SPAIN, SPAIN);
    checkGridColumnOrder(INLINE_GRID, INLINE_SEGMENT_COLUMN, RETAIL, RETAIL, RETAIL, RETAIL, RETAIL);

    // An essential criterion narrows them down
    selectContain(STATUS, "Active");
    clickButton("ButAdvInlSch");
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Atlas Logistics", "Iris Labs", "Ember Labs");

    // Priority customers of the default country
    clearAllFilters(INLINE_WINDOW);
    checkNoFilterChips(INLINE_WINDOW);
    toggleAdvancedFilters(INLINE_WINDOW);
    clickCheckbox(PRIORITY);
    clickButton("ButAdvInlSch");
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Atlas Logistics", "Boreal Retail", "Cobalt Textiles", "Ember Labs");
    checkGridColumnOrder(INLINE_GRID, INLINE_COUNTRY_COLUMN, SPAIN, SPAIN, SPAIN, SPAIN);
    checkGridColumnOrder(INLINE_GRID, INLINE_PRIORITY_COLUMN, YES, YES, YES, YES);

    // A minimum amount instead of the flag
    clickCheckbox(PRIORITY);
    writeText(AMOUNT, "8000");
    clickButton("ButAdvInlSch");
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Ember Labs", "Krypton Pharma", "Ember Retail", "Krypton Media");
  }

  /**
   * Side-panel window: Advanced filters opens the advanced criteria in a side panel. Cancel gives them back the values they had
   * when it opened, Reset to defaults restores their defaults, and Show results closes it and fills its own grid
   */
  @Test
  void t050_sidePanelCancelResetAndShowResults() {
    setTestTitle("Advanced search: side panel with Cancel, Reset to defaults and Show results");
    openAdvancedSearchScreen();
    clickTab("AdvSchTab", "Advanced filters in a side panel");
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
  }

  /**
   * A window without advanced block (the criteria test screen) shows no toggle and no filters in effect, even with a
   * value that differs from its default
   */
  @Test
  void t060_windowWithoutAdvancedCriteriaIsUnchanged() {
    setTestTitle("Advanced search: a window without advanced criteria is unchanged");
    gotoScreen("test", "criteria", "criteria-test");

    checkNoAdvancedFilters("Criteria");
    writeText("Txt", "Ana");
    checkNoAdvancedFilters("Criteria");
  }

  /**
   * The users screen (#790 pilot): theme, initial screen and blocked are the advanced block of its criteria window, below
   * the user, profile and active criteria
   */
  @Test
  void t070_usersScreenShowsItsAdvancedCriteriaOnDemand() {
    setTestTitle("Advanced search: the users screen shows its advanced criteria on demand");
    gotoScreen("test", "matrix", "matrix-test-side-panel");
    gotoScreen("tools", "users");
    checkCriterionVisible("CrtUsr");
    checkCriterionVisible("CrtPro");
    checkCriterionVisible("CrtSta");
    checkCriterionNotVisible("CrtThm");
    checkCriterionNotVisible("CrtScr");
    checkCriterionNotVisible("CrtBlk");
    checkAdvancedFiltersCount("WinUsrCrt", 0);
    checkCriterionAboveAdvancedFilters("WinUsrCrt", "CrtUsr");

    double toggleTop = getAdvancedFiltersToggleTop("WinUsrCrt");
    toggleAdvancedFilters("WinUsrCrt");
    checkCriterionVisible("CrtThm");
    checkCriterionVisible("CrtScr");
    checkCriterionVisible("CrtBlk");
    checkAdvancedFiltersToggleTop("WinUsrCrt", toggleTop);
    checkCriterionBelowAdvancedFilters("WinUsrCrt", "CrtThm");
    selectContain("CrtBlk", "No");
    checkAdvancedFiltersCount("WinUsrCrt", 1);
    checkFilterChip("WinUsrCrt", "CrtBlk", false);

    // Hidden, it stays in view as a marked chip, which clears it
    toggleAdvancedFilters("WinUsrCrt");
    checkCriterionNotVisible("CrtBlk");
    checkFilterChip("WinUsrCrt", "CrtBlk", true);
    removeFilterChip("WinUsrCrt", "CrtBlk");
    checkAdvancedFiltersCount("WinUsrCrt", 0);
    checkNoFilterChips("WinUsrCrt");
  }
}
