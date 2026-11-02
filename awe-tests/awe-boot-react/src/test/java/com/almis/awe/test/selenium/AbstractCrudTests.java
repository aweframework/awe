package com.almis.awe.test.selenium;

/**
 * Steps that the tests of the CRUD screens (sites, modules, profiles, databases, themes and users) share. Each one opens its
 * own screen, so a test does not depend on the screen that the previous test left open, and every CRUD test class creates, updates
 * and deletes records with names of its own (no name, acronym or alias of a record of one class contains the one of a record
 * of another class), so the classes do not share rows. The grids are searched by contained text, so a test that updates a
 * record finds it by its original name and the test that deletes it uses the same text. A class that needs a site or a
 * profile creates its own with {@link #createSite(String)} or {@link #createProfile(String, String)}, and deletes it after the
 * records that use it.
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

  static final String CONFIRM_BUTTON = "ButCnf";
  static final String RESET_BUTTON = "ButRst";

  private static final String SITE_GRID = "GrdSitAdm";
  private static final String SITE_SEARCH = "CrtSitTxt";
  private static final String SITE_MODULES_GRID = "SitModDbsLst";
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
    // Go to screen: the administration screens (a list with a side panel) have no criteria to reset, so the search of the
    // grid lists the records
    openScreen(options);
    if (isSitesScreen(options)) {
      searchGrid(SITE_SEARCH, search);
    } else {
      clickButton(RESET_BUTTON);
      searchAndWait();
    }

    // Check that the row is there
    checkRowContents(search);
  }

  /**
   * Tell whether the options of a test open the sites screen (site-admin), which has a search instead of a criteria panel
   *
   * @param options Menu options of the screen
   * @return The options open the sites screen
   */
  private boolean isSitesScreen(String... options) {
    return options.length > 0 && SITES.equals(options[options.length - 1]);
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
   * Open the sites screen and search a site by its name
   *
   * @param name Site name
   */
  private void searchSite(String name) {
    openScreen(TOOLS, SITES);
    searchGrid(SITE_SEARCH, name);
  }

  /**
   * Create a site that is active and has the Base module with the first database, for a test that needs one: the site and
   * its module rows are created from the side panel of the sites screen (site-admin), with one save
   *
   * @param name Site name
   */
  void createSite(String name) {
    // A rerun finds the site that the failed attempt saved
    searchSite(name);
    if (hasRowContents(name)) {
      return;
    }

    // Open the side panel of a new site, which starts active
    clickButton("ButNew");
    writeText("Nam", name);
    writeText("OrdCrt", "3");

    // The modules of the site, edited inside the panel
    clickButton("ButSitModAdd");
    // In React the select of a grid cell shows its search only when it is open: choose the option by its text
    selectContain(SITE_MODULES_GRID, "IdeMod", "Base");
    selectContain(SITE_MODULES_GRID, "IdeDbs", "awedb");
    writeText(SITE_MODULES_GRID, "SitModOrd", "3");
    saveRow(SITE_MODULES_GRID);

    // Store and confirm
    clickButtonAndConfirm("ButPnlNew");

    // Check that the site is there
    searchSite(name);
    checkRowContentsGrid(SITE_GRID, name);
  }

  /**
   * Delete a site that a test created, if it is still there, from the row menu of the sites screen
   *
   * @param name Site name
   */
  void verifySiteDeleted(String name) {
    searchSite(name);
    if (hasRowContents(name)) {
      clickGridRowButton(SITE_GRID, name, "MorBtn");
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
    }

    // Verify deleted
    checkRowGone(SITE_GRID, name);
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
