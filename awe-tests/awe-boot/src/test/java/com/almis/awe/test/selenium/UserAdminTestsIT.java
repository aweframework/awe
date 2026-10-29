package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Users screen (user-admin, #793): the search of its grid toolbar finds users by full name, login, email or profile, the
 * quick filters count the users of the search (All, Locked, Without 2FA, Inactive) and filter the grid, the pencil and the
 * row menu of a row edit its user in the side panel (whose header shows the user: name, login · profile and state) and the
 * selection bar resets the 2FA of the selected users. Active is a switch. The class creates its own
 * profile and two users with it, a locked one (with the Users screen, the only one that sets the lock) and an inactive
 * one (with the side panel), so searching by the profile gives known counts; it deletes them at the end.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class UserAdminTestsIT extends AbstractCrudTests {

  private static final String USER_ADMIN = "user-admin";
  private static final String GRID = "GrdUsrAdm";
  private static final String QUICK_FILTER = "CrtUsrSta";
  private static final String SEARCH = "CrtUsrTxt";
  private static final String PROFILE_ACRONYM = "PQF";
  private static final String PROFILE = "Quick filter accounts";
  private static final String LOCKED_USER = "qfilter locked";
  private static final String INACTIVE_USER = "qfilter inactive";
  private static final String INACTIVE_NAME = "Quick Inactive";
  private static final String UPDATED_EMAIL = "qfilter.updated@almis.com";
  private static final String PASSWORD = "1234";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String MORE_BUTTON = "MorBtn";
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
    openScreen(TOOLS, USER_ADMIN);
    searchGrid(SEARCH, PROFILE);
  }

  /**
   * Create the profile of the class and the locked user, unless they are there (a rerun)
   */
  @Test
  void t001_newLockedUser() {
    setTestTitle("Add the profile and a locked user");

    createProfile(PROFILE_ACRONYM, PROFILE);

    if (!exists(LOCKED_USER, TOOLS, USERS)) {
      addNew(TOOLS, USERS);
      writeText("Usr", LOCKED_USER);
      writeText("Nam", "Quick Locked");
      writeText("Eml", "qfilter.locked@almis.com");
      writeText("Pas", PASSWORD);
      writeText("RetPas", PASSWORD);
      suggest("Pro", PROFILE_ACRONYM, PROFILE_ACRONYM);
      selectContain("Sta", "Yes");
      selectContain("PwdLck", "Yes");
      clickButtonAndConfirm(CONFIRM_BUTTON);
    }

    verifyListed(LOCKED_USER, TOOLS, USERS);
  }

  /**
   * Create the inactive user with the side panel of the users screen, unless it is there (a rerun)
   */
  @Test
  void t002_newInactiveUserFromThePanel() {
    setTestTitle("Add an inactive user from the side panel");

    if (!exists(INACTIVE_USER, TOOLS, USERS)) {
      openScreen(TOOLS, USER_ADMIN);
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
    checkGridQuickFilterOption(QUICK_FILTER, "locked", "Locked 1");
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
    checkRowContentsGrid(GRID, LOCKED_USER, INACTIVE_USER);

    clickGridQuickFilterOption(QUICK_FILTER, "locked");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "locked");
    checkRowGone(GRID, INACTIVE_USER);
    checkRowContentsGrid(GRID, LOCKED_USER);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, LOCKED_USER);
    checkRowContentsGrid(GRID, INACTIVE_USER);

    // The counts stay the ones of the search, whatever the option chosen
    checkGridQuickFilterOption(QUICK_FILTER, "locked", "Locked 1");

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, LOCKED_USER, INACTIVE_USER);
  }

  /**
   * The search finds the users by email (typing, with no Enter), full name and login, ignoring case, the counts follow
   * it, and its clear button shows every user again
   */
  @Test
  void t005_searchByEmailNameOrLogin() {
    setTestTitle("Search users by email, full name or login");

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, LOCKED_USER, INACTIVE_USER);

    typeGridSearch(SEARCH, "qfilter.locked@");
    checkRowGone(GRID, INACTIVE_USER);
    checkRowContentsGrid(GRID, LOCKED_USER);
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 0");

    searchGrid(SEARCH, INACTIVE_NAME.toUpperCase());
    checkRowGone(GRID, LOCKED_USER);
    checkRowContentsGrid(GRID, INACTIVE_USER);

    searchGrid(SEARCH, LOCKED_USER);
    checkRowGone(GRID, INACTIVE_USER);
    checkRowContentsGrid(GRID, LOCKED_USER);

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, LOCKED_USER, INACTIVE_USER);
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
    checkRowContentsGrid(GRID, LOCKED_USER, INACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");

    toggleRowContents(GRID, LOCKED_USER);
    checkGridSelectionBar(GRID, "2 selected");
    toggleRowContents(GRID, LOCKED_USER);
    checkGridSelectionBar(GRID, "1 selected");
    toggleRowContents(GRID, INACTIVE_USER);
    checkGridSelectionBarNotVisible(GRID);

    toggleRowContents(GRID, INACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");
    clickGridSelectionButton(GRID, "ButRst2fa");
    acceptConfirm();
    checkAndCloseMessage("success");

    // The grid reloads after the action and keeps the selected user
    checkRowContentsGrid(GRID, LOCKED_USER, INACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");
  }

  /**
   * Delete the users of the class, if they are still there
   */
  @Test
  void t009_deleteUsers() {
    setTestTitle("Delete the users");

    verifyRecordDeleted("CrtUsr", LOCKED_USER, TOOLS, USERS);
    verifyRecordDeleted("CrtUsr", INACTIVE_USER, TOOLS, USERS);
  }

  /**
   * Delete the profile of the class, once its users are deleted
   */
  @Test
  void t010_deleteProfile() {
    setTestTitle("Delete the profile");

    verifyProfileDeleted(PROFILE_ACRONYM);
  }
}
