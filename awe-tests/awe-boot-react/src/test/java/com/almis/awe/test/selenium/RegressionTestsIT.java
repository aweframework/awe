package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.almis.awe.test.selenium.PaneBoxes.LEFT;
import static com.almis.awe.test.selenium.PaneBoxes.TOLERANCE;
import static com.almis.awe.test.selenium.PaneBoxes.WIDTH;
import static com.almis.awe.test.selenium.PaneBoxes.checkClose;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests of the test screens. Every test starts logged in with the Test module selected and opens its own screen,
 * so a failing test does not leave the next ones without a session or a screen. The wrong login test logs out and in again:
 * when it fails in between, the next test logs in again through the session setup.
 */
@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class RegressionTestsIT extends AbstractSessionTests {

  private static final String SETTINGS = "settings";
  private static final String SECURITY = "security";
  private static final String SECURITY_AUDIT = "security-audit";
  private static final String SECURITY_AUDIT_GRID = "GrdSecAud";
  private static final String USERS_GRID = "GrdUsrAdm";
  private static final String BASE_MODULE = "Base";

  RegressionTestsIT() {
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
    checkLogoutWithConfirmation();
  }

  /**
   * Select test module on select criterion
   */
  @Test
  @StartsFrom(Session.LOGGED_IN)
  void t001_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectModule("Test");

    // Check the menu of the module
    checkMenuOption("test", "Tests");
  }

  /**
   * Load suggest on grid: Test to check suggest initial load on grid (#30648)
   */
  @Test
  void t002_loadSuggestOnGrid() {
    // Title
    setTestTitle("Load suggest on grid: Test to check suggest initial load on grid (#30648)");

    // Go to matrix test
    gotoScreen("test", "matrix", "matrix-test");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    // The React client shows the translated label of the tab
    clickTab("TabSelMat", "Editable");

    // Check row contents
    checkRowContents("Prueba - adminflare");
  }

  /**
   * Test to check suggest criteria with 'strict' attribute set to false
   */
  @Test
  void t003_suggestStrict() {
    // Title
    setTestTitle("Suggest Strict: Test to check suggest criteria with 'strict' attribute set to false");

    // Go to screen (its user suggest is not strict, and the login of the class is one of its events)
    gotoScreen(SETTINGS, SECURITY, SECURITY_AUDIT);

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggestLast("CrtUsr", "te");

    // Search and wait
    searchAndWait();

    // Check grid values
    checkRowContents("test");

    // Check criterion value
    checkSuggestContents("CrtUsr", "te");
  }

  /**
   * Test for read dependency
   */
  @Test
  void t004_readDependency() {
    // Title
    setTestTitle("Test for read dependency");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("TxtReq", "aaa");

    // Write on criterion (the numeric input of the React client takes a typed "." as its decimal separator, so the
    // thousands separator is not typed: the client adds it)
    writeText("Unt", "325274,50");

    // Assert text
    checkCriterionContents("Unt", "325.274,50");
  }

  /**
   * Quote check on unit label
   */
  @Test
  void t005_quoteCheckUnitLabel() {
    // Title
    setTestTitle("Quote check on unit label");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Tar", "\"");

    // Click on checkbox
    clickCheckbox("RadBox3");

    // Wait for text
    checkCriterionUnit("Unt", "USD");
  }

  /**
   * Check filtered date dependency (#31141)
   */
  @Test
  void t006_checkFilteredDateDependency() {
    // Title
    setTestTitle("Check filtered date dependency (#31141)");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Txt", "edita");

    // Check text
    checkCriterionContents("Txt", "edita");

    // Click on date
    clickDate("FilCalRea");

    // Click on selector
    clickEnabledDatepickerDay();
  }

  /**
   * Keep criteria test
   */
  @Test
  void t007_keepCriteria() {
    // Title
    setTestTitle("Keep criteria test");

    // Go to screen (it keeps its criteria)
    gotoScreen("tools", "modules");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtMod", BASE_MODULE, BASE_MODULE);

    // Search and wait
    searchAndWait();

    // Check grid values
    clickRowContents(BASE_MODULE);

    // Click button
    clickButton("ButViw", true);

    // Click button
    clickButton("ButBck", true);

    // Wait for button
    waitForButton("ButRst");

    // Wait for button
    checkSuggestContents("CrtMod", BASE_MODULE);
  }

  /**
   * Delayed suggest
   */
  @Test
  void t008_delayedSuggest() {
    // Title
    setTestTitle("Delayed suggest");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Click button
    clickButton("ButRst");

    // Suggest delayed
    suggestReplacingSearch("Sug", "tee", "test", "test", 800);

    // Suggest delayed
    suggestMultipleReplacingSearch("SugMulReq", "tee", "test", "test", 800);

    // Check selector
    checkMultipleSelectorContents("SugMulReq", "test (test@test.com)");
  }

  /**
   * Wrong login
   */
  @Test
  void t009_wrongLogin() {
    // Title
    setTestTitle("Wrong login");

    // Do logout
    checkLogoutWithConfirmation();

    // Check wrong login
    checkLoginRejected("test", "lala", "warning", "Invalid credentials",
      "The credentials entered for the user -test- are not valid");

    // Check wrong login
    checkLoginRejected("tutu", "lala", "warning", "Wrong username", "Username -tutu- is wrong or inactive");

    // Do right login
    checkLogin("test", "test", "Manager");
  }

  /**
   * Escape closes a confirmation and cancels its action (#873)
   */
  @Test
  void t013_cancelConfirmWithEscape() {
    // Close the logout confirmation with the Escape key
    checkLogoutCancelledWithEscape();

    // The session is still alive: the logout can be asked and accepted again
    checkLogoutWithConfirmation();

    // Do right login
    checkLogin("test", "test", "Manager");
  }

  /**
   * Sort a grid using a component column
   */
  @Test
  void t010_sortComponentColumn() {
    // Title
    setTestTitle("Sort a grid using a component column");

    // Go to screen (its events grid has icon columns)
    gotoScreen(SETTINGS, SECURITY, SECURITY_AUDIT);

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtUsr", "test", "test");

    // Search and wait
    searchAndWait();

    // Sort by a component field
    sortGrid(SECURITY_AUDIT_GRID, "TypIco");

    // Expect not to have an error message
    checkMessageMissing("danger");

    // Sort by a component field
    sortGrid(SECURITY_AUDIT_GRID, "ResIco");

    // Expect not to have an error message
    checkMessageMissing("danger");
  }

  /**
   * Sort a grid using a component column
   */
  @Test
  void t011_suggestRepeatsValues() {
    // Title
    setTestTitle("Check that suggested values are not repeated");

    // Go to screen (its theme suggest searches the themes as the old themes screen did)
    gotoScreen("tools", "modules");

    // Wait for button
    clickButton("ButRst");

    // Click on suggest
    openSuggest("CrtThm");

    // Pause
    pause(1000);

    // Write text
    writeSuggestSearch("CrtThm", "a");

    // Pause
    pause(1000);

    // Write text
    writeSuggestSearch("CrtThm", "s");

    // Pause
    pause(1000);

    // Write text
    writeSuggestSearch("CrtThm", "p");

    // Pause
    pause(1000);

    // Check there's only one result
    checkSuggestResultCount(1);

    // Click selector
    selectResult("asp");
  }

  /**
   * Test for fill over select
   */
  @Test
  void t012_fillOverSelect() {
    // Title
    setTestTitle("Test filling a select with less values than usual");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Assert text
    checkSelectNumberOfResults("SelReq", 2);

    // Select result
    selectResult("Yes");

    // Select a date
    selectDate("CalReq", "23/10/1978");

    // Wait for loading bar
    waitForLoadingBar();

    // Write hour
    writeText("Tim", "12:23:41");

    // Write hour
    writeText("TimReq", "12:23:41");

    // Assert text
    checkSelectNumberOfResults("SelReq", 4);

    // Select result
    selectResult("General");

    // Write on criterion
    writeText("Txt", "sino");

    // Wait for loading bar
    waitForLoadingBar();

    // Assert text
    checkSelectNumberOfResults("SelReq", 2);

    // Select result
    selectResult("No");
  }

  /**
   * Test for fill over select
   */
  @Test
  void t020_checkDependenciesAfterRestore() {
    // Title
    setTestTitle("Check if dependencies are working after restore (issue #279)");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Txt", "4decimales");

    // Check that Num criterion contains JPY
    checkCriterionContents("Num", "JPY");

    // Wait for button
    waitForButton("ButRst");

    // Click on button
    clickButton("ButRst");

    // Check that Txt criterion has been restored
    checkCriterionContents("Txt", "test");

    // Check that Num criterion contains EUR
    checkCriterionContents("Num", "EUR");

    // Write on criterion
    writeText("Txt", "4decimales");

    // Check that Num criterion contains JPY
    checkCriterionContents("Num", "JPY");
  }

  /**
   * Test for select all rows of grid
   */
  @Test
  void t030_selectAllRowsOfGrid() {

    // Title
    setTestTitle("Test select all rows of multi select grid");

    // Go to screen
    gotoScreen("tools", "users");

    // Wait for button
    waitForButton("ButPrn");

    // Click to select all rows of grid
    selectAllRowsOfGrid(USERS_GRID);

    // Wait for button
    checkAllRowsSelected(USERS_GRID);
  }

  /**
   * Test for select all rows of grid
   */
  @Test
  void t040_suggestChangeValue() {
    // Title
    setTestTitle("Launch 2 selected actions into a suggest and check value has changed successfully");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test-left");

    // The screen loads criteria from queries that do not exist, and the client keeps its buttons blocked until the user
    // has closed the six error messages
    closeMessages("danger", 6);

    // Wait for button
    waitForButton("ButPrn");

    // Wait for button
    waitForButton("ButtonLoadSuggest");

    // Click on button
    clickButton("ButtonLoadSuggest", true);

    // Check suggest value
    checkSuggestContents("SugTst", "DjrRepPth");

    // Click on button: its query also returns an alias that is not a component of the screen, whose selection the
    // client ignores instead of taking the whole application down (#901)
    clickButton("ButtonLoadSuggest2", true);

    // Check suggest value
    checkSuggestContents("SugTst", "DjrHdgPag");

    // Click on button
    clickButton("ButtonResetSuggest", true);

    // Check suggest value
    checkSuggestContents("SugTst", "");
  }

  /**
   * Test update suggest multiple with dependency
   */
  @Test
  void t050_updateSuggestMultipleWithDependency() {
    // Title
    setTestTitle("Update suggest multiple with dependency");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Txt", "suggest-multiple");

    // Wait for button
    waitForButton("ButPrn");

    // Check suggest value
    // The database of the React test application has no user "pei", so the dependency selects only the user "test"
    checkMultipleSelectorContents("SugMul", "test (test@test.com)");
  }

  /**
   * Split layout test (#892): on the Layout 1 screen the splitter of the middle pane follows the pointer. The middle pane
   * grows from its left edge, which does not move, the pane after the splitter shrinks by the same amount, and the pane
   * before the middle one keeps its width
   */
  @Test
  void t060_layoutSplitterResizesItsNeighbours() {
    // Title
    setTestTitle("Layout 1: the splitter resizes the panes on its two sides");

    // Go to screen
    gotoScreen("test", "layout", "layout-test");

    // The middle pane is the resizable around the first inner window
    String middlePane = "ButPru";

    double[] middleBox = getResizableBounds(middlePane, "right");
    double[] beforeBox = getResizableOppositeNeighbourBounds(middlePane, "right");
    double[] afterBox = getResizableNeighbourBounds(middlePane, "right");

    // The pane after the splitter is narrow on this screen and may reach its minimum before the end of the drag: it gives
    // part or all of the drag, and the middle pane takes exactly that
    int drag = 50;
    dragResizable(middlePane, "right", drag);

    double[] middleDragged = getResizableBounds(middlePane, "right");
    double[] beforeDragged = getResizableOppositeNeighbourBounds(middlePane, "right");
    double[] afterDragged = getResizableNeighbourBounds(middlePane, "right");
    double moved = middleDragged[WIDTH] - middleBox[WIDTH];
    assertTrue(moved > TOLERANCE && moved <= drag + TOLERANCE, "The middle pane grew " + moved + "px for a drag of " + drag + "px");
    checkClose("left edge of the middle pane", middleDragged[LEFT], middleBox[LEFT]);
    checkClose("width of the pane before the middle one", beforeDragged[WIDTH], beforeBox[WIDTH]);
    checkClose("width of the pane after the splitter", afterDragged[WIDTH], afterBox[WIDTH] - moved);
  }
}
