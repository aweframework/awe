package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Advanced search of a window (#790) on a list of customers: the essential criteria first, then the advanced block, shown
 * inline below its Advanced filters row or in a side panel (a tab each), the counter of the advanced criteria in use, the
 * filters in effect with their chips, the search of each example in its own grid, a window without advanced block, and the
 * open state of the inline block, which is remembered for the user, and the column autofilter of the inline grid (#912).
 * Every test opens its screen with the inline block closed, so each one can run on its own.
 *
 * <p>The inline window keeps saved searches (saved-searches="true"): the test user saves, applies, renames and deletes its
 * own searches and, as it may publish (it reaches the profiles option), publishes one for the Test profile. A user of that
 * profile, which the class creates (Test reaches the test menu but not the tools, so it may not publish), applies it but
 * cannot change it. A saved search can open the screen (#915), and a search of the inline grid can open it filtered and
 * sorted (#945): the last tests make one the default search of the user, so the earlier tests start from the values of the
 * screen. Every test removes what it saves, and what an earlier run left, so the class can run again.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("RegressionWebsocketPrintIT")
class AdvancedSearchTestsIT extends AbstractSavedSearchTests {

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
  private static final String INLINE_STATUS_COLUMN = "InlSta";
  private static final String PANEL_NAME_COLUMN = "PnlNam";
  private static final String PANEL_COUNTRY_COLUMN = "PnlCtr";
  private static final String SPAIN = "Spain";
  private static final String FRANCE = "France";
  private static final String RETAIL = "Retail";
  private static final String YES = "Yes";
  // First page of the customers of the default country (Spain) in the inline grid
  private static final String[] SPANISH_CUSTOMERS = {"Atlas Logistics", "Granite Textiles", "Atlas Foods", "Granite Systems",
    "Boreal Retail", "Harbor Media", "Boreal Motors", "Harbor Logistics", "Cobalt Textiles", "Iris Labs"};
  private static final String SAVED_SEARCHES = "Saved searches";
  private static final String OWN_SEARCH = "Active retail";
  private static final String RENAMED_SEARCH = "Active retail customers";
  private static final String SHARED_SEARCH = "Priority customers";
  private static final String DEFAULT_SEARCH = "French customers";
  // First page of the customers of France in the inline grid
  private static final String[] FRENCH_CUSTOMERS = {"Fjord Energy", "Lumen Foods", "Fjord Textiles", "Lumen Labs", "Atlas Labs",
    "Granite Pharma", "Atlas Retail", "Granite Media", "Boreal Energy", "Harbor Foods"};
  private static final String GRID_DEFAULT_SEARCH = "Inactive customers by segment";
  // First page of every customer in the inline grid, as it loads without a search
  private static final String[] ALL_CUSTOMERS = {"Atlas Logistics", "Boreal Foods", "Cobalt Labs", "Delta Retail",
    "Ember Motors", "Fjord Energy", "Granite Textiles", "Harbor Systems", "Iris Pharma", "Juniper Media"};
  // Inactive customers of the default country in the inline grid, by segment (Online, then Retail)
  private static final String[] SPANISH_INACTIVE_CUSTOMERS_BY_SEGMENT = {"Harbor Media", "Delta Media", "Harbor Logistics",
    "Delta Logistics"};
  // First page of every inactive customer in the inline grid, by segment (six Online, then Public sector)
  private static final String[] INACTIVE_CUSTOMERS_BY_SEGMENT = {"Delta Energy", "Harbor Media", "Lumen Retail", "Delta Media",
    "Harbor Retail", "Lumen Systems", "Delta Motors", "Harbor Pharma", "Lumen Labs", "Delta Pharma"};
  // Profile that the shared search is published for (the name of its option) and its user, which the class creates
  private static final String PROFILE_ACRONYM = "TST";
  private static final String PROFILE_NAME = "Test";
  private static final String PROFILE_USER = "savsch";
  private static final String PROFILE_USER_PASSWORD = "savsch";
  private static final String PROFILE_USER_NAME = "Saved";
  private static final String PROFILE_USER_SHOWN = "Saved";

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
   * keeps it as it is (expanded advanced criteria, values). The inline block starts as the user left it (#790)
   */
  private void showAdvancedSearchScreen() {
    gotoScreen("test", "matrix", "matrix-test-side-panel");
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
   * The advanced block of the inline window (#790, first on the users screen, now deprecated): an advanced filter in effect
   * stays in view as a marked chip while the block is hidden, and removing that chip clears it without opening the block
   */
  @Test
  void t070_hiddenAdvancedFilterIsClearedFromItsMarkedChip() {
    setTestTitle("Advanced search: a hidden advanced filter is cleared from its marked chip");
    openAdvancedSearchScreen();
    checkCriterionNotVisible(SEGMENT);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);

    toggleAdvancedFilters(INLINE_WINDOW);
    selectContain(SEGMENT, RETAIL);
    checkAdvancedFiltersCount(INLINE_WINDOW, 1);
    checkFilterChip(INLINE_WINDOW, SEGMENT, false);

    // Hidden, it stays in view as a marked chip, which clears it
    toggleAdvancedFilters(INLINE_WINDOW);
    checkCriterionNotVisible(SEGMENT);
    checkFilterChip(INLINE_WINDOW, SEGMENT, true);
    removeFilterChip(INLINE_WINDOW, SEGMENT);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    checkNoFilterChips(INLINE_WINDOW);
    checkCriterionNotVisible(SEGMENT);
  }

  /**
   * The inline block is remembered for the user (#790): it is open when the screen is opened again after leaving it open,
   * and closed after closing it
   */
  @Test
  void t080_inlineBlockIsRememberedWhenTheScreenIsOpenedAgain() {
    setTestTitle("Advanced search: the inline block is opened again as the user left it");
    openAdvancedSearchScreen();
    checkCriterionNotVisible(COUNTRY);

    // Left open
    toggleAdvancedFilters(INLINE_WINDOW);
    checkAdvancedFiltersOpen(INLINE_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersOpen(INLINE_WINDOW);
    checkCriterionVisible(COUNTRY);
    checkCriterionBelowAdvancedFilters(INLINE_WINDOW, COUNTRY);

    // Left closed
    toggleAdvancedFilters(INLINE_WINDOW);
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    checkCriterionNotVisible(COUNTRY);
  }

  /**
   * The inline block is remembered across sessions (#790): left open, it is open after logging out and in again. It is
   * closed at the end, so the user is left as it was
   */
  @Test
  void t085_inlineBlockIsRememberedAfterLoggingOutAndIn() {
    setTestTitle("Advanced search: the inline block is remembered after logging out and in again");
    openAdvancedSearchScreen();
    toggleAdvancedFilters(INLINE_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersOpen(INLINE_WINDOW);

    // A new session
    ensureLoggedOut();
    checkLogin("test", "test", "Manager");
    ensureModule("Test", "test");
    showAdvancedSearchScreen();
    checkAdvancedFiltersOpen(INLINE_WINDOW);
    checkCriterionVisible(COUNTRY);

    // Back as it was
    toggleAdvancedFilters(INLINE_WINDOW);
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    showAdvancedSearchScreen();
    checkAdvancedFiltersClosed(INLINE_WINDOW);
  }

  /**
   * The side panel is not remembered (#790): it is closed when the screen is opened again, and the inline block of the
   * other window keeps its own state
   */
  @Test
  void t090_sidePanelIsNotRemembered() {
    setTestTitle("Advanced search: the side panel always starts closed");
    openAdvancedSearchScreen();
    clickTab("AdvSchTab", "Advanced filters in a side panel");
    toggleAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelOpen(PANEL_WINDOW);
    cancelAdvancedFilters(PANEL_WINDOW);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);

    showAdvancedSearchScreen();
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    clickTab("AdvSchTab", "Advanced filters in a side panel");
    checkCriterionVisible(PANEL_NAME);
    checkCriterionNotVisible(PANEL_COUNTRY);
    checkAdvancedFiltersPanelClosed(PANEL_WINDOW);
  }

  /**
   * Delete the saved searches of the inline window that an earlier run left, and close the menu
   *
   * @param names Names of the searches
   */
  private void deleteLeftSavedSearches(String... names) {
    for (String name : names) {
      if (hasSavedSearch(INLINE_WINDOW, name)) {
        closeSavedSearches(INLINE_WINDOW);
        deleteSavedSearch(INLINE_WINDOW, name);
      }
    }
    closeSavedSearches(INLINE_WINDOW);
  }

  /**
   * Own searches (#790): the values of the search criteria, advanced ones included, are saved with a name, which the
   * button shows while they do not change. Applying the search brings them back and launches the search, and the search
   * can be renamed and deleted
   */
  @Test
  void t100_ownSearchIsSavedAppliedRenamedAndDeleted() {
    setTestTitle("Saved searches: save, apply, rename and delete an own search");
    openAdvancedSearchScreen();
    deleteLeftSavedSearches(OWN_SEARCH, RENAMED_SEARCH);
    checkSavedSearchesText(INLINE_WINDOW, SAVED_SEARCHES);

    // Active retail customers of the default country
    selectContain(STATUS, "Active");
    toggleAdvancedFilters(INLINE_WINDOW);
    selectContain(SEGMENT, RETAIL);
    saveCurrentSearch(INLINE_WINDOW, OWN_SEARCH);
    checkSavedSearchesText(INLINE_WINDOW, OWN_SEARCH);
    checkSavedSearchListed(INLINE_WINDOW, OWN_SEARCH, true);
    closeSavedSearches(INLINE_WINDOW);

    // Cleared, the button names no search and the grid lists every customer of the default country
    clearAllFilters(INLINE_WINDOW);
    checkNoFilterChips(INLINE_WINDOW);
    checkSavedSearchesText(INLINE_WINDOW, SAVED_SEARCHES);
    toggleAdvancedFilters(INLINE_WINDOW);
    clickButton("ButAdvInlSch");
    checkNoFilterChips(INLINE_WINDOW);

    // Applied: its values and the search are back, the advanced ones hidden
    applySavedSearch(INLINE_WINDOW, OWN_SEARCH);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Atlas Logistics", "Iris Labs", "Ember Labs");
    checkGridColumnOrder(INLINE_GRID, INLINE_SEGMENT_COLUMN, RETAIL, RETAIL, RETAIL);
    checkFilterChip(INLINE_WINDOW, STATUS, false);
    checkFilterChip(INLINE_WINDOW, SEGMENT, true);
    checkAdvancedFiltersCount(INLINE_WINDOW, 1);
    checkSavedSearchesText(INLINE_WINDOW, OWN_SEARCH);

    // Renamed, the button follows it
    renameSavedSearch(INLINE_WINDOW, OWN_SEARCH, RENAMED_SEARCH);
    checkSavedSearchesText(INLINE_WINDOW, RENAMED_SEARCH);
    checkSavedSearchListed(INLINE_WINDOW, RENAMED_SEARCH, true);
    checkSavedSearchNotListed(INLINE_WINDOW, OWN_SEARCH);
    closeSavedSearches(INLINE_WINDOW);

    // Deleted
    deleteSavedSearch(INLINE_WINDOW, RENAMED_SEARCH);
    checkSavedSearchesText(INLINE_WINDOW, SAVED_SEARCHES);
    checkSavedSearchNotListed(INLINE_WINDOW, RENAMED_SEARCH);
    closeSavedSearches(INLINE_WINDOW);
    clearAllFilters(INLINE_WINDOW);
  }

  /**
   * A window opts in to saved searches (#790): the side-panel window, which does not, has no Saved searches button
   */
  @Test
  void t110_windowWithoutSavedSearchesHasNoSavedSearchesButton() {
    setTestTitle("Saved searches: a window that does not opt in has no Saved searches button");
    openAdvancedSearchScreen();
    clickTab("AdvSchTab", "Advanced filters in a side panel");
    checkCriterionVisible(PANEL_NAME);
    checkNoSavedSearches(PANEL_WINDOW);
  }

  /**
   * The test user may publish (#790): it reaches the profiles option, so the Save search dialog offers the profiles, and it
   * publishes a search for the Test profile, which it lists as shared and may change
   */
  @Test
  void t120_publisherPublishesASearchForAProfile() {
    setTestTitle("Saved searches: publish a search for a profile");
    openAdvancedSearchScreen();
    deleteLeftSavedSearches(SHARED_SEARCH);
    checkSaveSearchCanPublish(INLINE_WINDOW, true);
    publishSharedSearch();
    checkSavedSearchListed(INLINE_WINDOW, SHARED_SEARCH, false);
    closeSavedSearches(INLINE_WINDOW);
    checkSavedSearchCanBeChanged(INLINE_WINDOW, SHARED_SEARCH, true);
    clearAllFilters(INLINE_WINDOW);
  }

  /**
   * A user of the profile (#790) finds the published search shared with its profile and applies it, but it cannot rename
   * or delete it, and it may not publish. The test user creates that user and publishes the search first, unless they are
   * there (a rerun comes after the test that deletes them)
   */
  @Test
  void t130_userOfTheProfileAppliesTheSharedSearch() {
    setTestTitle("Saved searches: a user of the profile applies the shared search but cannot change it");
    createProfileUser();
    openAdvancedSearchScreen();
    if (!hasSavedSearch(INLINE_WINDOW, SHARED_SEARCH)) {
      closeSavedSearches(INLINE_WINDOW);
      publishSharedSearch();
      clearAllFilters(INLINE_WINDOW);
    }
    closeSavedSearches(INLINE_WINDOW);

    // The user of the profile, in the Test module
    ensureLoggedIn(PROFILE_USER, PROFILE_USER_PASSWORD, PROFILE_USER_SHOWN);
    ensureModule("Test", "test");
    openAdvancedSearchScreen();
    checkSavedSearchListed(INLINE_WINDOW, SHARED_SEARCH, false);
    closeSavedSearches(INLINE_WINDOW);

    applySavedSearch(INLINE_WINDOW, SHARED_SEARCH);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Atlas Logistics", "Boreal Retail", "Cobalt Textiles", "Ember Labs");
    checkFilterChip(INLINE_WINDOW, PRIORITY, true);
    checkSavedSearchesText(INLINE_WINDOW, SHARED_SEARCH);

    checkSavedSearchCanBeChanged(INLINE_WINDOW, SHARED_SEARCH, false);
    checkSaveSearchCanPublish(INLINE_WINDOW, false);
    clearAllFilters(INLINE_WINDOW);
  }

  /**
   * Publish the shared search for the Test profile: the priority customers of the default country. The advanced block is
   * left open
   */
  private void publishSharedSearch() {
    toggleAdvancedFilters(INLINE_WINDOW);
    clickCheckbox(PRIORITY);
    saveCurrentSearch(INLINE_WINDOW, SHARED_SEARCH, PROFILE_NAME);
  }

  /**
   * The publisher deletes the published search (#790), and the user of the profile is deleted
   */
  @Test
  void t140_publisherDeletesThePublishedSearch() {
    setTestTitle("Saved searches: the publisher deletes the published search");
    openAdvancedSearchScreen();
    if (hasSavedSearch(INLINE_WINDOW, SHARED_SEARCH)) {
      closeSavedSearches(INLINE_WINDOW);
      deleteSavedSearch(INLINE_WINDOW, SHARED_SEARCH);
    }
    checkSavedSearchNotListed(INLINE_WINDOW, SHARED_SEARCH);
    closeSavedSearches(INLINE_WINDOW);

    // The user of the profile, if it is still there
    verifyUserDeleted(PROFILE_USER);
  }

  /**
   * Column autofilter (#912): after a search of the customers of the default country, the funnel of the status column
   * lists its values from the server (codes shown by their labels). Unchecking Active and accepting keeps the inactive
   * customers, fills the funnel and shows the chip of the filter; removing the chip brings every customer back
   */
  @Test
  void t150_columnAutofilterFiltersTheGridAndItsChipRemovesIt() {
    setTestTitle("Column autofilter: filter the grid from the values of a column and remove the filter from its chip");
    openAdvancedSearchScreen();
    clickButton("ButAdvInlSch");
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, SPANISH_CUSTOMERS);
    checkGridPaginationInfo("View 1 - 10 of 20");
    checkNoGridColumnFilterChips(INLINE_GRID);

    // Only the inactive customers
    openGridColumnFilter(INLINE_GRID, INLINE_STATUS_COLUMN);
    checkGridColumnFilterValue(INLINE_GRID, "1", false);
    acceptGridColumnFilter(INLINE_GRID, INLINE_STATUS_COLUMN);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, "Harbor Media", "Harbor Logistics", "Delta Media", "Delta Logistics");
    checkGridPaginationInfo("View 1 - 4 of 4");
    checkGridColumnFiltered(INLINE_GRID, INLINE_STATUS_COLUMN, true);
    checkGridColumnFilterChip(INLINE_GRID, INLINE_STATUS_COLUMN, "Status: Inactive");

    // The chip removes the filter
    removeGridColumnFilterChip(INLINE_GRID, INLINE_STATUS_COLUMN);
    checkNoGridColumnFilterChips(INLINE_GRID);
    checkGridColumnFiltered(INLINE_GRID, INLINE_STATUS_COLUMN, false);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, SPANISH_CUSTOMERS);
    checkGridPaginationInfo("View 1 - 10 of 20");
  }

  /**
   * Default search (#915): a search saved with its layout and Open with this search opens the screen. The grid loads once,
   * already filtered by its values, the advanced criterion of its layout is in the main row with the block closed, and a
   * chip names the search. Clear all brings the values of the screen back and hides the chip. Deleting the search leaves the
   * screen as it was for the other tests
   */
  @Test
  void t160_defaultSearchOpensTheScreenWithItsValuesAndLayout() {
    setTestTitle("Saved searches: the default search opens the screen with its values and its layout");
    openAdvancedSearchScreen();
    deleteLeftSavedSearches(DEFAULT_SEARCH);

    // Customers of France, saved with the layout as the search that opens the screen
    toggleAdvancedFilters(INLINE_WINDOW);
    selectContain(COUNTRY, FRANCE);
    closeAdvancedFilters(INLINE_WINDOW);
    saveCurrentSearchAsDefault(INLINE_WINDOW, DEFAULT_SEARCH);
    closeSavedSearches(INLINE_WINDOW);

    // Opened again: filtered on load, without a search, and the country with the essential criteria
    showAdvancedSearchScreen();
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, FRENCH_CUSTOMERS);
    checkAdvancedFiltersClosed(INLINE_WINDOW);
    checkSelectContents(COUNTRY, FRANCE);
    checkCriterionAboveAdvancedFilters(INLINE_WINDOW, COUNTRY);
    checkFilterChip(INLINE_WINDOW, COUNTRY, false);
    checkAdvancedFiltersCount(INLINE_WINDOW, 0);
    checkDefaultSearchApplied(INLINE_WINDOW, DEFAULT_SEARCH);

    // Clear all: the values of the screen, without the chip
    clearAllFilters(INLINE_WINDOW);
    checkSelectContents(COUNTRY, SPAIN);
    checkNoFilterChips(INLINE_WINDOW);
    checkNoDefaultSearchApplied(INLINE_WINDOW);

    deleteSavedSearch(INLINE_WINDOW, DEFAULT_SEARCH);
    checkSavedSearchNotListed(INLINE_WINDOW, DEFAULT_SEARCH);
    closeSavedSearches(INLINE_WINDOW);
  }

  /**
   * Default search of a grid (#945): a search of the inline grid, with a column filter and a sort, saved with Open with this
   * search opens the screen with the grid already filtered and sorted, its column filter chip and a chip that names the
   * search. Clear all of the column filters brings the grid of the screen back and hides the chip. Deleting the search
   * leaves the screen as it was for the other tests
   */
  @Test
  void t170_gridDefaultSearchOpensTheGridFilteredAndSorted() {
    setTestTitle("Saved searches: the default search of a grid opens it filtered and sorted");
    openAdvancedSearchScreen();
    if (hasSavedSearch(INLINE_GRID, GRID_DEFAULT_SEARCH)) {
      closeSavedSearches(INLINE_GRID);
      deleteSavedSearch(INLINE_GRID, GRID_DEFAULT_SEARCH);
      showAdvancedSearchScreen();
    }
    closeSavedSearches(INLINE_GRID);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, ALL_CUSTOMERS);
    checkGridPaginationInfo("View 1 - 10 of 120");

    // The inactive customers by segment, saved as the search that opens the grid (the reloads of the grid send the country)
    openGridColumnFilter(INLINE_GRID, INLINE_STATUS_COLUMN);
    checkGridColumnFilterValue(INLINE_GRID, "1", false);
    acceptGridColumnFilter(INLINE_GRID, INLINE_STATUS_COLUMN);
    checkGridPaginationInfo("View 1 - 4 of 4");
    sortGrid(INLINE_GRID, INLINE_SEGMENT_COLUMN);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, SPANISH_INACTIVE_CUSTOMERS_BY_SEGMENT);
    saveCurrentGridSearchAsDefault(INLINE_GRID, GRID_DEFAULT_SEARCH);
    closeSavedSearches(INLINE_GRID);

    // Opened again: filtered and sorted on load (without the criteria, as the first load), with the chips of the column
    // filter and of the search
    showAdvancedSearchScreen();
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, INACTIVE_CUSTOMERS_BY_SEGMENT);
    checkGridPaginationInfo("View 1 - 10 of 30");
    checkGridColumnFiltered(INLINE_GRID, INLINE_STATUS_COLUMN, true);
    checkGridColumnFilterChip(INLINE_GRID, INLINE_STATUS_COLUMN, "Status: Inactive");
    checkDefaultSearchApplied(INLINE_GRID, GRID_DEFAULT_SEARCH);

    // Clear all: the grid of the screen, without its column filter and sort, and without the chip
    clearAllGridColumnFilters(INLINE_GRID);
    checkNoGridColumnFilterChips(INLINE_GRID);
    checkGridColumnOrder(INLINE_GRID, INLINE_NAME_COLUMN, SPANISH_CUSTOMERS);
    checkGridPaginationInfo("View 1 - 10 of 20");
    checkNoDefaultSearchApplied(INLINE_GRID);

    deleteSavedSearch(INLINE_GRID, GRID_DEFAULT_SEARCH);
    checkSavedSearchNotListed(INLINE_GRID, GRID_DEFAULT_SEARCH);
    closeSavedSearches(INLINE_GRID);
  }

  /**
   * Create the user of the Test profile from the side panel of the users screen, unless an earlier run left it
   */
  private void createProfileUser() {
    createUser(PROFILE_USER, PROFILE_USER_NAME, "savsch@almis.com", PROFILE_ACRONYM, PROFILE_USER_PASSWORD);
  }
}
