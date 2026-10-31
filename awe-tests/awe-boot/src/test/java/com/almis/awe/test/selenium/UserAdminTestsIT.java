package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Users screen (user-admin, #793): the search of its grid toolbar finds users by full name, login, email or profile, the
 * quick filters count the users of the search (All, Locked, Without 2FA, Inactive) and filter the grid, the pencil and the
 * row menu of a row edit its user in the side panel (whose header shows the user: name, login · profile and state), the
 * selection bar resets the 2FA of the selected users and the row menu deletes them. Active is a switch. The class creates
 * its own profile (with the profiles screen) and two users with it from the side panel, an active one and an inactive
 * one, so searching by the profile gives known counts; it deletes them at the end. No screen of the menu locks a user
 * (the side panel only unlocks), so the locked state (its count, its filter and the unlock) is covered by the Spring
 * tests (UserStateQuickFilterTest, UserListQueryTest and UserAdminScreenTest).
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class UserAdminTestsIT extends AbstractCrudTests {

  private static final String GRID = "GrdUsrAdm";
  private static final String QUICK_FILTER = "CrtUsrSta";
  private static final String SEARCH = "CrtUsrTxt";
  private static final String PROFILE_ACRONYM = "PQF";
  private static final String PROFILE = "Quick filter accounts";
  private static final String ACTIVE_USER = "qfilter active";
  private static final String ACTIVE_NAME = "Quick Active";
  private static final String INACTIVE_USER = "qfilter inactive";
  private static final String INACTIVE_NAME = "Quick Inactive";
  private static final String UPDATED_EMAIL = "qfilter.updated@almis.com";
  private static final String PASSWORD = "1234";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String PANEL = "PnlUsr";

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogout();
  }

  /**
   * Open the users screen and search the users of the profile of the class by its name
   */
  private void searchUsersOfTheProfile() {
    openScreen(TOOLS, USERS);
    searchGrid(SEARCH, PROFILE);
  }

  /**
   * Tell whether the users screen lists a user of the class (a rerun finds what a failed attempt saved or deleted)
   *
   * @param login User login
   * @return The grid shows the user
   */
  private boolean listed(String login) {
    return listedInSearch(USERS, SEARCH, login);
  }

  /**
   * Create the profile of the class and the active user with the side panel of the users screen, unless they are there (a
   * rerun)
   */
  @Test
  void t001_newProfileAndActiveUserFromThePanel() {
    setTestTitle("Add the profile and an active user from the side panel");

    createProfile(PROFILE_ACRONYM, PROFILE);

    if (!listed(ACTIVE_USER)) {
      clickButton("ButNew");
      writeText("Nam", ACTIVE_NAME);
      writeText("Eml", "qfilter.active@almis.com");
      writeText("Usr", ACTIVE_USER);
      suggest("Pro", PROFILE_ACRONYM, PROFILE_ACRONYM);
      // Active is a switch, on for a new user
      checkCheckboxRadio(true, "Sta");
      writeText("Pas", PASSWORD);
      writeText("RetPas", PASSWORD);
      clickButtonAndConfirm("ButPnlNew");
    }

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, ACTIVE_USER);
  }

  /**
   * Create the inactive user with the side panel of the users screen, unless it is there (a rerun)
   */
  @Test
  void t002_newInactiveUserFromThePanel() {
    setTestTitle("Add an inactive user from the side panel");

    if (!listed(INACTIVE_USER)) {
      clickButton("ButNew");
      writeText("Nam", INACTIVE_NAME);
      writeText("Eml", "qfilter.inactive@almis.com");
      writeText("Usr", INACTIVE_USER);
      suggest("Pro", PROFILE_ACRONYM, PROFILE_ACRONYM);
      // No selected user: the header shows the label of the panel. Active is a switch, on for a new user
      checkSidePanelTitle(PANEL, "User");
      checkCheckboxRadio(true, "Sta");
      clickCheckbox("Sta");
      checkCheckboxRadio(false, "Sta");
      writeText("Pas", PASSWORD);
      writeText("RetPas", PASSWORD);
      clickButtonAndConfirm("ButPnlNew");
    }

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, INACTIVE_USER);
  }

  /**
   * The counts of the quick filters follow the search: the users of the profile
   */
  @Test
  void t003_quickFilterCounts() {
    setTestTitle("Quick filter counts of the users of the search");

    searchUsersOfTheProfile();

    checkGridQuickFilterOption(QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(QUICK_FILTER, "locked", "Locked 0");
    checkGridQuickFilterOption(QUICK_FILTER, "no2fa", "Without 2FA 2");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
  }

  /**
   * Each quick filter shows the users of its option, and All shows them all again
   */
  @Test
  void t004_quickFilterChoices() {
    setTestTitle("Quick filters filter the grid");

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, ACTIVE_USER);
    checkRowContentsGrid(GRID, INACTIVE_USER);

    // No user of the class has 2FA
    clickGridQuickFilterOption(QUICK_FILTER, "no2fa");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "no2fa");
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);

    // The counts stay the ones of the search, whatever the option chosen
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);
  }

  /**
   * The search finds the users by email (typing, with no Enter), full name and login, ignoring case, the counts follow
   * it, and its clear button shows every user again
   */
  @Test
  void t005_searchByEmailNameOrLogin() {
    setTestTitle("Search users by email, full name or login");

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);

    typeGridSearch(SEARCH, "qfilter.active@");
    checkRowGone(GRID, INACTIVE_USER);
    checkRowContentsGrid(GRID, ACTIVE_USER);
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 0");

    searchGrid(SEARCH, INACTIVE_NAME.toUpperCase());
    checkRowGone(GRID, ACTIVE_USER);
    checkRowContentsGrid(GRID, INACTIVE_USER);

    searchGrid(SEARCH, ACTIVE_USER);
    checkRowGone(GRID, INACTIVE_USER);
    checkRowContentsGrid(GRID, ACTIVE_USER);

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the user of its row, its header shows the user
   * (full name, login · profile and its state), the Active switch activates it and saving it updates the row. Help is an
   * icon-only button named by its label
   */
  @Test
  void t006_editUserFromThePencil() {
    setTestTitle("Edit a user from the pencil of its row");

    searchUsersOfTheProfile();
    checkButtonIconOnly("ButHlp", "Help");

    clickGridRowButton(GRID, INACTIVE_USER, EDIT_BUTTON);

    checkCriterionContents("Nam", INACTIVE_NAME);
    checkCriterionContents("Usr", INACTIVE_USER);
    checkSuggestContents("Pro", PROFILE_ACRONYM);
    checkSidePanelTitle(PANEL, INACTIVE_NAME);
    checkSidePanelSubtitle(PANEL, INACTIVE_USER + " · " + PROFILE);
    checkSidePanelTag(PANEL, "Inactive");

    writeText("Eml", UPDATED_EMAIL);
    checkCheckboxRadio(false, "Sta");
    clickCheckbox("Sta");
    checkCheckboxRadio(true, "Sta");
    clickButtonAndConfirm("ButPnlUpd");

    checkRowContentsGrid(GRID, UPDATED_EMAIL, "Active");
  }

  /**
   * The ellipsis of a row opens the row menu of its user, whose Edit opens the side panel filled with it
   */
  @Test
  void t007_editUserFromTheRowMenuButton() {
    setTestTitle("Edit a user from the row menu of its ellipsis");

    searchUsersOfTheProfile();
    clickGridRowButton(GRID, INACTIVE_USER, MORE_BUTTON);
    clickContextButton("CtxButUpd");

    checkCriterionContents("Nam", INACTIVE_NAME);
    checkCriterionContents("Eml", UPDATED_EMAIL);
    checkCheckboxRadio(true, "Sta");
    checkSidePanelTitle(PANEL, INACTIVE_NAME);
    checkSidePanelTag(PANEL, "Active");
    clickButton("ButPnlCnl");
  }

  /**
   * Selecting users shows the selection bar with their number and the selection actions, and unselecting them all hides
   * it; Reset 2FA runs from the bar on the selected user. The grid keeps its selection when it is searched again or
   * reloaded, so the user of the row menu is still selected (only it: a row button selects only its row), and a click on
   * a row toggles it
   */
  @Test
  void t008_resetTwoFactorFromTheSelectionBar() {
    setTestTitle("Reset 2FA of the selected users from the selection bar");

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");

    toggleRowContents(GRID, ACTIVE_USER);
    checkGridSelectionBar(GRID, "2 selected");
    toggleRowContents(GRID, ACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");
    toggleRowContents(GRID, INACTIVE_USER);
    checkGridSelectionBarNotVisible(GRID);

    toggleRowContents(GRID, INACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");
    clickGridSelectionButton(GRID, "ButRst2fa");
    acceptConfirm();
    checkAndCloseMessage("success");

    // The grid reloads after the action and keeps the selected user
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");
  }

  /**
   * Delete the users of the class from the row menu, if they are still there
   */
  @Test
  void t009_deleteUsers() {
    setTestTitle("Delete the users");

    verifyDeletedFromRowMenu(USERS, GRID, SEARCH, ACTIVE_USER);
    verifyDeletedFromRowMenu(USERS, GRID, SEARCH, INACTIVE_USER);
  }

  /**
   * Delete the profile of the class, once its users are deleted
   */
  @Test
  void t010_deleteProfile() {
    setTestTitle("Delete the profile");

    verifyProfileDeleted(PROFILE);
  }
}
