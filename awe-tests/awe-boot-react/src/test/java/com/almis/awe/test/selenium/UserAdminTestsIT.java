package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Users screen (user-admin, #793), one flow per step of the screen: the side panel creates an active and an inactive user
 * (Active is a switch), a quick filter and the search of its grid toolbar filter the grid, the pencil and the row menu of a
 * row edit its user in the side panel (whose header shows the user: name, login · profile and state), the selection bar
 * resets the 2FA of the selected users and the row menu and the selection bar delete them. The class creates its own
 * profile and its two users with it, so searching by the profile gives known rows; it deletes them at the end.
 *
 * <p>Proven in a lower layer, not here: the counts of the quick filters, that they follow the search and stay whatever the
 * option chosen, the rows of each option (Locked included: no screen of the application locks a user any more) and the
 * search by full name, login, email or profile ignoring case (Spring {@code UserStateQuickFilterTest},
 * {@code UserListQueryTest}); how the quick filter renders its options with their counts and the chosen one (Jest
 * {@code AweGridQuickFilterTest}); the maintains the panel sends and the unlock ({@code UserAdminScreenTest}).</p>
 *
 * <p>This is the only browser flow of the side-panel admin pattern (React only, the AWE 5 engine); the modules screen,
 * whose panel has tabs with nested grids, is proven by ModuleAdminScreenTest and ModuleListQueryTest.</p>
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
  private static final String INACTIVE_USER = "qfilter inactive";
  private static final String INACTIVE_NAME = "Quick Inactive";
  private static final String UPDATED_EMAIL = "qfilter.updated@almis.com";
  private static final String PASSWORD = "1234";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String MORE_BUTTON = "MorBtn";
  private static final String PANEL = "PnlUsr";

  /**
   * Open the users screen and search the users of the profile of the class by its name
   */
  private void searchUsersOfTheProfile() {
    openScreen(TOOLS, USERS);
    searchGrid(SEARCH, PROFILE);
  }

  /**
   * Create a user of the profile of the class from the side panel of the users screen, which is open
   *
   * @param login  Login
   * @param name   Full name
   * @param email  Email
   * @param active Leave the Active switch on
   */
  private void createFromThePanel(String login, String name, String email, boolean active) {
    clickButton("ButNew");
    writeText("Nam", name);
    writeText("Eml", email);
    writeText("Usr", login);
    suggest("Pro", PROFILE_ACRONYM, PROFILE_ACRONYM);
    // No selected user: the header shows the label of the panel. Active is a switch, on for a new user
    checkSidePanelTitle(PANEL, "User");
    checkCheckboxRadio(true, "Sta");
    if (!active) {
      clickCheckbox("Sta");
      checkCheckboxRadio(false, "Sta");
    }
    writeText("Pas", PASSWORD);
    writeText("RetPas", PASSWORD);
    clickButtonAndConfirm("ButPnlNew");
  }

  /**
   * Create the profile of the class, and an active and an inactive user with the side panel, unless they are there (a
   * rerun finds what a failed attempt saved)
   */
  @Test
  void t010_newActiveAndInactiveUsersFromThePanel() {
    setTestTitle("Add the profile, an active and an inactive user from the side panel");

    createProfile(PROFILE_ACRONYM, PROFILE);

    searchUsersOfTheProfile();
    if (!hasRowContents(ACTIVE_USER)) {
      createFromThePanel(ACTIVE_USER, "Quick Active", "qfilter.active@almis.com", true);
    }
    if (!hasRowContents(INACTIVE_USER)) {
      createFromThePanel(INACTIVE_USER, INACTIVE_NAME, "qfilter.inactive@almis.com", false);
    }

    searchGrid(SEARCH, PROFILE);
    checkRowContentsGrid(GRID, ACTIVE_USER, "Active");
    checkRowContentsGrid(GRID, INACTIVE_USER, "Inactive");
  }

  /**
   * A quick filter shows the users of its option, and All shows them all again; the search finds the users while typing
   * (no Enter), and its clear button shows every user again
   */
  @Test
  void t020_quickFilterAndSearchFilterTheGrid() {
    setTestTitle("Filter the users with a quick filter and the search");

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, ACTIVE_USER);
    checkRowContentsGrid(GRID, INACTIVE_USER);

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);

    typeGridSearch(SEARCH, "qfilter.active@");
    checkRowGone(GRID, INACTIVE_USER);
    checkRowContentsGrid(GRID, ACTIVE_USER);

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the user of its row, its header shows the user
   * (full name, login · profile and its state), the Active switch activates it and saving it updates the row. The ellipsis
   * of a row opens the row menu of its user, whose Edit opens the side panel filled with the saved values. Help is an
   * icon-only button named by its label
   */
  @Test
  void t030_editUserFromThePencilAndTheRowMenu() {
    setTestTitle("Edit a user from the pencil of its row and from its row menu");

    searchUsersOfTheProfile();
    checkButtonIconOnly("ButHlp", "Help");

    // The pencil
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

    // The row menu
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
   * it; Reset 2FA runs from the bar on the selected user (the only test of that rule). A click on a row toggles it, and the
   * grid keeps its selection when it is reloaded
   */
  @Test
  void t040_resetTwoFactorFromTheSelectionBar() {
    setTestTitle("Reset 2FA of the selected users from the selection bar");

    searchUsersOfTheProfile();
    checkRowContentsGrid(GRID, ACTIVE_USER, INACTIVE_USER);
    checkGridSelectionBarNotVisible(GRID);

    toggleRowContents(GRID, ACTIVE_USER);
    checkGridSelectionBar(GRID, "1 selected");
    toggleRowContents(GRID, INACTIVE_USER);
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
   * Delete a user from the row menu (the ellipsis selects only its row) and, once it is gone and nothing is selected, the
   * other one from the selection bar; then the profile of the class, once its users are deleted. A rerun finds what is
   * already deleted
   */
  @Test
  void t050_deleteUsersAndTheirProfile() {
    setTestTitle("Delete the users from the row menu and the selection bar, and their profile");

    searchUsersOfTheProfile();
    if (hasRowContents(INACTIVE_USER)) {
      clickGridRowButton(GRID, INACTIVE_USER, MORE_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, INACTIVE_USER);
    }

    if (hasRowContents(ACTIVE_USER)) {
      toggleRowContents(GRID, ACTIVE_USER);
      checkGridSelectionBar(GRID, "1 selected");
      clickGridSelectionButton(GRID, "ButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, ACTIVE_USER);
    }

    searchGrid(SEARCH, PROFILE);
    checkRowGone(GRID, INACTIVE_USER);
    checkRowGone(GRID, ACTIVE_USER);

    verifyProfileDeleted(PROFILE_ACRONYM);
  }
}
