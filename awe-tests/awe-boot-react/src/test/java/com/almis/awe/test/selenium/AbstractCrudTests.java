package com.almis.awe.test.selenium;

/**
 * Steps that the tests of the administration screens (a list and a side panel) share. Each one opens its own screen, so a
 * test does not depend on the screen that the previous test left open, and every class creates, updates and deletes
 * records with names of its own (no name, acronym or alias of a record of one class contains the one of a record of
 * another class), so the classes do not share rows. The grids are searched by contained text, so a test that updates a
 * record finds it by its original name and the test that deletes it uses the same text. A class that needs a profile or
 * a user creates its own with {@link #createProfile(String, String)} or
 * {@link #createUser(String, String, String, String, String)}, and deletes it after the records that use it.
 *
 * <p>The steps of a class are ordered (create, update, delete), but a test that fails is rerun after the whole run, when the
 * later steps have already deleted what the earlier ones made. So every step makes sure of what it needs before it uses it:
 * the helpers that create a record do nothing when it is already there, and the delete steps do nothing when it is gone.
 * Each step still runs all its own checks.</p>
 */
abstract class AbstractCrudTests extends AbstractSessionTests {

  static final String TOOLS = "tools";
  static final String SITES = "sites";
  static final String MODULES = "modules";
  static final String PROFILES = "profiles";
  static final String DATABASES = "databases";
  static final String THEMES = "themes";
  static final String USERS = "users";

  private static final String PROFILE_GRID = "GrdProAdm";
  private static final String PROFILE_SEARCH = "CrtProTxt";
  private static final String USER_GRID = "GrdUsrAdm";
  private static final String USER_SEARCH = "CrtUsrTxt";

  protected AbstractCrudTests() {
    this(Session.LOGGED_IN);
  }

  /**
   * @param classSession State that the tests of the class start from (a class that is not a CRUD test and needs a record
   *                     of its own, as a user)
   */
  protected AbstractCrudTests(Session classSession) {
    super(classSession);
  }

  /**
   * Open a screen of the tools menu as new, even when it is the one that the previous test left open. The client does not
   * load again a screen that is already open, so its criteria and its selected row would still be there when the test starts:
   * the steps go through another screen of the menu first.
   *
   * @param options Menu options
   */
  void openScreen(String... options) {
    gotoScreen(TOOLS, SITES.equals(options[options.length - 1]) ? MODULES : SITES);
    gotoScreen(options);
  }

  /**
   * Open the profiles screen and search a profile by its acronym
   *
   * @param acronym Profile acronym
   */
  private void searchProfile(String acronym) {
    openScreen(TOOLS, PROFILES);
    searchGrid(PROFILE_SEARCH, acronym);
  }

  /**
   * Create an active profile from the side panel of the profiles screen, for a test that needs one
   *
   * @param acronym Profile acronym
   * @param name    Profile name
   */
  void createProfile(String acronym, String name) {
    // A rerun finds the profile that the failed attempt saved
    searchProfile(acronym);
    if (hasRowContents(acronym)) {
      return;
    }

    // Open the side panel of a new profile, which starts active
    clickButton("ButNew");
    writeText("Acr", acronym);
    writeText("Nam", name);
    suggest("IdeThm", "sunset", "sunset");
    suggest("ScrIni", "Modules", "Modules");

    // Store and confirm
    clickButtonAndConfirm("ButPnlNew");

    // Check that the profile is there
    searchProfile(acronym);
    checkRowContentsGrid(PROFILE_GRID, acronym);
  }

  /**
   * Delete a profile that a test created, if it is still there, from the row menu of the profiles screen
   *
   * @param acronym Profile acronym
   */
  void verifyProfileDeleted(String acronym) {
    searchProfile(acronym);
    if (hasRowContents(acronym)) {
      clickGridRowButton(PROFILE_GRID, acronym, "MorBtn");
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
    }

    // Verify deleted
    checkRowGone(PROFILE_GRID, acronym);
  }

  /**
   * Open the users screen and search a user by its login
   *
   * @param login User login
   */
  private void searchUser(String login) {
    openScreen(TOOLS, USERS);
    searchGrid(USER_SEARCH, login);
  }

  /**
   * Create an active user from the side panel of the users screen, for a test that needs one (as a user of a profile)
   *
   * @param login    User login
   * @param name     Full name
   * @param email    Email
   * @param profile  Profile acronym
   * @param password Password
   */
  void createUser(String login, String name, String email, String profile, String password) {
    // A rerun finds the user that the failed attempt saved
    searchUser(login);
    if (!hasRowContents(login)) {
      // Active is a switch, on for a new user
      clickButton("ButNew");
      writeText("Nam", name);
      writeText("Eml", email);
      writeText("Usr", login);
      suggest("Pro", profile, profile);
      checkCheckboxRadio(true, "Sta");
      writeText("Pas", password);
      writeText("RetPas", password);
      clickButtonAndConfirm("ButPnlNew");
    }

    // Check that the user is there
    searchUser(login);
    checkRowContentsGrid(USER_GRID, login);
  }

  /**
   * Delete a user that a test created, if it is still there, from the row menu of the users screen
   *
   * @param login User login
   */
  void verifyUserDeleted(String login) {
    searchUser(login);
    if (hasRowContents(login)) {
      clickGridRowButton(USER_GRID, login, "MorBtn");
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
    }

    // Verify deleted
    checkRowGone(USER_GRID, login);
  }
}
