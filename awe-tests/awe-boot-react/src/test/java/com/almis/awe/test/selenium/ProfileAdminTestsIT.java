package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Profiles screen (profile-admin, #793): the side panel creates an active and an inactive profile (Active is a switch),
 * the quick filters count the profiles of the search (All, Active, Inactive) and filter the grid, the search finds them
 * by name, acronym or theme, the pencil and the row menu of a row edit its profile in the side panel (whose header shows
 * the profile: name, acronym · theme and state), and the selection bar and the row menu delete them. The class creates
 * its own two profiles and deletes them at the end.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ProfileAdminTestsIT extends AbstractCrudTests {

  private static final String GRID = "GrdProAdm";
  private static final String QUICK_FILTER = "CrtProSta";
  private static final String SEARCH = "CrtProTxt";
  private static final String PANEL = "PnlPro";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String MORE_BUTTON = "MorBtn";
  // Words of the profiles of the class only: the search by them lists just these two
  private static final String PREFIX = "Paneltest";
  private static final String ACTIVE_ACRONYM = "PNA";
  private static final String ACTIVE_PROFILE = PREFIX + " active";
  private static final String INACTIVE_ACRONYM = "PNI";
  private static final String INACTIVE_PROFILE = PREFIX + " inactive";
  private static final String INACTIVE_PROFILE_CHANGED = PREFIX + " reactivated";

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
    checkLogoutWithConfirmation();
  }

  /**
   * Open the profiles screen and search the profiles of the class
   */
  private void searchProfilesOfTheClass() {
    openScreen(TOOLS, PROFILES);
    searchGrid(SEARCH, PREFIX);
  }

  /**
   * Tell whether the profiles screen lists a profile of the class (a rerun finds what a failed attempt saved or deleted)
   *
   * @param name Profile name
   * @return The grid shows the profile
   */
  private boolean listed(String name) {
    searchProfilesOfTheClass();
    return hasRowContents(name);
  }

  /**
   * Create a profile from the side panel of the profiles screen
   *
   * @param acronym Acronym
   * @param name    Name
   * @param theme   Theme name
   * @param active  Leave the Active switch on
   */
  private void createFromThePanel(String acronym, String name, String theme, boolean active) {
    openScreen(TOOLS, PROFILES);
    clickButton("ButNew");
    // No selected profile: the header shows the label of the panel. Active is a switch, on for a new profile
    checkSidePanelTitle(PANEL, "Profile");
    checkCheckboxRadio(true, "Act");
    writeText("Acr", acronym);
    writeText("Nam", name);
    suggest("IdeThm", theme, theme);
    suggest("ScrIni", "Modules", "Modules");
    if (!active) {
      clickCheckbox("Act");
      checkCheckboxRadio(false, "Act");
    }
    clickButtonAndConfirm("ButPnlNew");
  }

  /**
   * Create the active profile from the side panel, unless it is there (a rerun)
   */
  @Test
  void t001_newActiveProfileFromThePanel() {
    setTestTitle("Add an active profile from the side panel");

    if (!listed(ACTIVE_PROFILE)) {
      createFromThePanel(ACTIVE_ACRONYM, ACTIVE_PROFILE, "sunset", true);
    }

    searchProfilesOfTheClass();
    checkRowContentsGrid(GRID, ACTIVE_PROFILE);
  }

  /**
   * Create the inactive profile from the side panel, unless it is there (a rerun)
   */
  @Test
  void t002_newInactiveProfileFromThePanel() {
    setTestTitle("Add an inactive profile from the side panel");

    if (!listed(INACTIVE_PROFILE)) {
      createFromThePanel(INACTIVE_ACRONYM, INACTIVE_PROFILE, "sky", false);
    }

    searchProfilesOfTheClass();
    checkRowContentsGrid(GRID, INACTIVE_PROFILE);
  }

  /**
   * The counts of the quick filters follow the search: the profiles of the class
   */
  @Test
  void t003_quickFilterCounts() {
    setTestTitle("Quick filter counts of the profiles of the search");

    searchProfilesOfTheClass();

    checkGridQuickFilterOption(QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 1");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
  }

  /**
   * Each quick filter shows the profiles of its option, and All shows them all again
   */
  @Test
  void t004_quickFilterChoices() {
    setTestTitle("Quick filters filter the grid");

    searchProfilesOfTheClass();
    checkRowContentsGrid(GRID, ACTIVE_PROFILE, INACTIVE_PROFILE);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, ACTIVE_PROFILE);
    checkRowContentsGrid(GRID, INACTIVE_PROFILE);

    clickGridQuickFilterOption(QUICK_FILTER, "active");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "active");
    checkRowGone(GRID, INACTIVE_PROFILE);
    checkRowContentsGrid(GRID, ACTIVE_PROFILE);

    // The counts stay the ones of the search, whatever the option chosen
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, ACTIVE_PROFILE, INACTIVE_PROFILE);
  }

  /**
   * The search finds the profiles by acronym (typing, with no Enter) and by theme, ignoring case, the counts follow it,
   * and its clear button shows every profile again
   */
  @Test
  void t005_searchByAcronymOrTheme() {
    setTestTitle("Search profiles by acronym or theme");

    searchProfilesOfTheClass();
    checkRowContentsGrid(GRID, ACTIVE_PROFILE, INACTIVE_PROFILE);

    typeGridSearch(SEARCH, INACTIVE_ACRONYM.toLowerCase());
    checkRowGone(GRID, ACTIVE_PROFILE);
    checkRowContentsGrid(GRID, INACTIVE_PROFILE);
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 0");

    searchGrid(SEARCH, "SKY");
    checkRowGone(GRID, ACTIVE_PROFILE);
    checkRowContentsGrid(GRID, INACTIVE_PROFILE);

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, ACTIVE_PROFILE, INACTIVE_PROFILE);
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the profile of its row, its header shows the
   * profile (name, acronym · theme and its state), the Active switch activates it and saving it updates the row. Help is
   * an icon-only button named by its label
   */
  @Test
  void t006_editProfileFromThePencil() {
    setTestTitle("Edit a profile from the pencil of its row");

    if (!listed(INACTIVE_PROFILE_CHANGED)) {
      searchProfilesOfTheClass();
      checkButtonIconOnly("ButHlp", "Help");

      clickGridRowButton(GRID, INACTIVE_PROFILE, EDIT_BUTTON);

      checkCriterionContents("Acr", INACTIVE_ACRONYM);
      checkCriterionContents("Nam", INACTIVE_PROFILE);
      checkSuggestContents("IdeThm", "sky");
      checkSidePanelTitle(PANEL, INACTIVE_PROFILE);
      checkSidePanelSubtitle(PANEL, INACTIVE_ACRONYM + " · sky");
      checkSidePanelTag(PANEL, "Inactive");

      writeText("Nam", INACTIVE_PROFILE_CHANGED);
      checkCheckboxRadio(false, "Act");
      clickCheckbox("Act");
      checkCheckboxRadio(true, "Act");
      clickButtonAndConfirm("ButPnlUpd");
    }

    searchProfilesOfTheClass();
    checkRowContentsGrid(GRID, INACTIVE_PROFILE_CHANGED, "Active");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 2");
  }

  /**
   * The ellipsis of a row opens the row menu of its profile, whose Edit opens the side panel filled with it
   */
  @Test
  void t007_editProfileFromTheRowMenuButton() {
    setTestTitle("Edit a profile from the row menu of its ellipsis");

    searchProfilesOfTheClass();
    clickGridRowButton(GRID, INACTIVE_PROFILE_CHANGED, MORE_BUTTON);
    clickContextButton("CtxButUpd");

    checkCriterionContents("Acr", INACTIVE_ACRONYM);
    checkCriterionContents("Nam", INACTIVE_PROFILE_CHANGED);
    checkCheckboxRadio(true, "Act");
    checkSidePanelTitle(PANEL, INACTIVE_PROFILE_CHANGED);
    checkSidePanelTag(PANEL, "Active");
    clickButton("ButPnlCnl");
  }

  /**
   * Delete a profile from the row menu (the ellipsis selects only its row) and, once it is gone and nothing is selected,
   * the other one from the selection bar
   */
  @Test
  void t008_deleteProfiles() {
    setTestTitle("Delete the profiles from the row menu and the selection bar");

    if (listed(INACTIVE_PROFILE_CHANGED)) {
      clickGridRowButton(GRID, INACTIVE_PROFILE_CHANGED, MORE_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, INACTIVE_PROFILE_CHANGED);
    }

    if (listed(ACTIVE_PROFILE)) {
      toggleRowContents(GRID, ACTIVE_PROFILE);
      checkGridSelectionBar(GRID, "1 selected");
      clickGridSelectionButton(GRID, "ButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, ACTIVE_PROFILE);
    }

    searchProfilesOfTheClass();
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 0");
  }
}
