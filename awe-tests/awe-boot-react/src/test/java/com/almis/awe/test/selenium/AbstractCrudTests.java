package com.almis.awe.test.selenium;

/**
 * Steps that the tests of the CRUD screens (sites, modules, profiles, databases and users) share. Each one opens its own
 * screen, so a test does not depend on the screen that the previous test left open, and every CRUD test class creates, updates
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
  static final String USERS = "users";

  static final String CONFIRM_BUTTON = "ButCnf";
  static final String RESET_BUTTON = "ButRst";

  protected AbstractCrudTests() {
    super(Session.LOGGED_IN);
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
    selectContain("SitModDbsLst", "IdeMod", "Base");

    // Suggest on column selector
    selectContain("SitModDbsLst", "IdeDbs", "awedb");

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
   * Create an active profile, for a test that needs one
   *
   * @param acronym Profile acronym
   * @param name    Profile name
   */
  void createProfile(String acronym, String name) {
    // A rerun finds the profile that the failed attempt saved
    if (exists(acronym, TOOLS, PROFILES)) {
      return;
    }

    // Go for new screen
    addNew(TOOLS, PROFILES);

    // Insert text
    writeText("Acr", acronym);

    // Insert text
    writeText("Nam", name);

    // Select on selector
    selectContain("Act", "Yes");

    // Suggest on  selector
    suggest("IdeThm", "sunse", "sunse");

    // Suggest on  selector
    suggest("ScrIni", "Modules", "Modules");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);
  }

  /**
   * Delete a profile that a test created
   *
   * @param acronym Profile acronym
   */
  void verifyProfileDeleted(String acronym) {
    // Delete the profile, if it is still there
    verifyRecordDeleted("CrtPro", acronym, TOOLS, PROFILES);
  }
}
