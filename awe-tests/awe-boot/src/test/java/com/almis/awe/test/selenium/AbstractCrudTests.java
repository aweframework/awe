package com.almis.awe.test.selenium;

/**
 * Steps that the tests of the CRUD screens (sites and modules) and of the administration screens (users, profiles, themes
 * and databases: a list and a side panel) share. Each one opens its own screen, so a test does not depend on the screen
 * that the previous test left open, and every class creates, updates and deletes records with names of its own (no name,
 * acronym or alias of a record of one class contains the one of a record of another class), so the classes do not share
 * rows. The grids are searched by contained text, so a test that updates a record finds it by its original name and the
 * test that deletes it uses the same text. A class that needs a site or a profile creates its own with
 * {@link #createSite(String)} or {@link #createProfile(String, String)}, and deletes it after the records that use it.
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
  static final String USERS = "users";

  static final String CONFIRM_BUTTON = "ButCnf";
  static final String RESET_BUTTON = "ButRst";
  static final String MORE_BUTTON = "MorBtn";
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
   * Go to a screen to add a new option
   *
   * @param options Menu options
   */
  void addNew(String... options) {
    // Go to screen
    openScreen(options);

    // Click on new button
    clickButton("ButNew", true);

    // Wait for button
    waitForButton(CONFIRM_BUTTON);
  }

  /**
   * Go to a screen to update an option
   *
   * @param suggest Criterion to search the option
   * @param search  Search text
   * @param options Menu options
   */
  void update(String suggest, String search, String... options) {
    // Go to screen
    openScreen(options);

    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(suggest, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Click on button
    clickButton("ButUpd", true);

    // Wait for button
    waitForButton(CONFIRM_BUTTON);

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Search an option on its screen and open it to view it
   *
   * @param suggest Criterion to search the option
   * @param search  Search text
   */
  void verifyView(String suggest, String search) {
    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(suggest, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Click on button
    clickButton("ButViw", true);

    // Wait for button
    waitForButton("ButBck");
  }

  /**
   * Delete from a screen
   *
   * @param criterion Criterion to search
   * @param search    Search text
   * @param options   Screen options
   */
  void delete(String criterion, String search, String... options) {
    // Go to screen
    openScreen(options);

    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(criterion, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Store and confirm
    clickButtonAndConfirm("ButDel");
  }

  /**
   * Tell whether a record is in the list of a screen. A step that creates or deletes a record asks it first, because a step
   * that is run again after a failed attempt finds the record that the attempt saved (or deleted): it must not add it twice, and
   * it must not look for the one that is gone. The step still runs all its checks.
   *
   * @param search  Text of the record in the list
   * @param options Screen options
   * @return The list shows the record
   */
  boolean exists(String search, String... options) {
    // Go to screen
    openScreen(options);

    // List everything
    clickButton(RESET_BUTTON);
    searchAndWait();

    return hasRowContents(search);
  }

  /**
   * Check that a record is in the list of a screen
   *
   * @param search  Text of the record in the list
   * @param options Screen options
   */
  void verifyListed(String search, String... options) {
    // Go to screen
    openScreen(options);

    // List everything
    clickButton(RESET_BUTTON);
    searchAndWait();

    // Check that the row is there
    checkRowContents(search);
  }

  /**
   * Delete a record when it is there, and verify that it is not. A rerun of a delete step finds the record already deleted
   * when the failed attempt deleted it and failed afterwards
   *
   * @param criterion Criterion to search
   * @param search    Search text
   * @param options   Screen options
   */
  void verifyRecordDeleted(String criterion, String search, String... options) {
    if (exists(search, options)) {
      delete(criterion, search, options);
    }

    // Verify deleted
    verifyDeleted(search);
  }

  /**
   * Verify deleted
   *
   * @param search Search text
   */
  void verifyDeleted(String search) {
    // Wait for button
    clickButton(RESET_BUTTON);

    // Search on grid
    searchAndWait();

    // Check that the row is not there
    checkRowNotContains(search);
  }

  /**
   * Create a site that is active and has the Base module with the first database, for a test that needs one
   *
   * @param name Site name
   */
  void createSite(String name) {
    // A rerun finds the site that the failed attempt saved
    if (exists(name, TOOLS, SITES)) {
      return;
    }

    // Go for new screen
    addNew(TOOLS, SITES);

    // Write on criterion
    writeText("Nam", name);

    // Select on selector
    selectLast("Act");

    // Write on criterion
    writeText("Ord", "3");

    // Click on button
    clickButton("ButGrdAdd");

    // Suggest on column selector
    suggest("SitModDbsLst", "IdeMod", "Base", "Base");

    // Suggest on column selector
    suggest("SitModDbsLst", "IdeDbs", "awedb", "awedb");

    // Write on criterion
    writeText("SitModDbsLst", "Order", "3");

    // Save line
    saveRow();

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);
  }

  /**
   * Delete a site that a test created
   *
   * @param name Site name
   */
  void verifySiteDeleted(String name) {
    // Delete the site, if it is still there
    verifyRecordDeleted("CrtSit", name, TOOLS, SITES);
  }

  /**
   * Tell whether the search of an administration screen (a list and a side panel) lists a record. A step that creates or
   * deletes a record asks it first, because a step that is run again after a failed attempt finds the record that the
   * attempt saved (or deleted)
   *
   * @param option Menu option of the screen
   * @param search Search of the grid
   * @param text   Text of the record, searched in the grid
   * @return The grid shows the record
   */
  boolean listedInSearch(String option, String search, String text) {
    openScreen(TOOLS, option);
    searchGrid(search, text);
    return hasRowContents(text);
  }

  /**
   * Delete a record of an administration screen from the row menu of its row (the ellipsis selects only its row), when
   * its search lists it, and verify that it is gone
   *
   * @param option Menu option of the screen
   * @param grid   Grid of the screen
   * @param search Search of the grid
   * @param text   Text of the record, searched in the grid
   */
  void verifyDeletedFromRowMenu(String option, String grid, String search, String text) {
    if (listedInSearch(option, search, text)) {
      clickGridRowButton(grid, text, MORE_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
    }

    // Verify deleted
    searchGrid(search, text);
    checkRowGone(grid, text);
  }

  /**
   * Create an active profile from the side panel of the profiles screen, for a test that needs one
   *
   * @param acronym Profile acronym
   * @param name    Profile name
   */
  void createProfile(String acronym, String name) {
    // A rerun finds the profile that the failed attempt saved
    if (!listedInSearch(PROFILES, PROFILE_SEARCH, name)) {
      // Active is a switch, on for a new profile
      clickButton("ButNew");
      writeText("Acr", acronym);
      writeText("Nam", name);
      suggest("IdeThm", "sunse", "sunse");
      suggest("ScrIni", "Modules", "Modules");
      clickButtonAndConfirm("ButPnlNew");
    }

    // Verify listed
    searchGrid(PROFILE_SEARCH, name);
    checkRowContentsGrid(PROFILE_GRID, name);
  }

  /**
   * Delete a profile that a test created, if it is still there
   *
   * @param name Profile name
   */
  void verifyProfileDeleted(String name) {
    verifyDeletedFromRowMenu(PROFILES, PROFILE_GRID, PROFILE_SEARCH, name);
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
    if (!listedInSearch(USERS, USER_SEARCH, login)) {
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

    // Verify listed
    searchGrid(USER_SEARCH, login);
    checkRowContentsGrid(USER_GRID, login);
  }

  /**
   * Delete a user that a test created, if it is still there
   *
   * @param login User login
   */
  void verifyUserDeleted(String login) {
    verifyDeletedFromRowMenu(USERS, USER_GRID, USER_SEARCH, login);
  }
}
