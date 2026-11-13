package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Print dialog of the test screens: every test starts logged in with the Test module selected (session setup of
 * {@link AbstractSessionTests}), opens its screen and generates its print. The class has no login, logout or module
 * selection test of its own: {@code IntegrationTestsIT.t000_loginTest} and {@code IntegrationTestsIT.t999_logoutTest} prove
 * the login and the logout, and {@code RegressionTestsIT.t001_selectTestModule} proves the module selection.
 * <p>
 * Only what the browser alone sees is printed here: one flow of the print dialog ({@code matrix-test}, all tabs) and the
 * chart screen, whose SVG the client draws. The files each print generates, for every screen and tab option, are proven by
 * {@code PrintScreenActionTest} (awe-boot), which replaced the print tests of the user list, profile list, matrix selected
 * tab, grid and chart and layout 2 screens.
 */
@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class PrintTestsIT extends AbstractSessionTests {

  PrintTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Print matrix all tabs
   */
  @Test
  void t040_printMatrixAllTabs() {
    // Title
    setTestTitle("Print all matrix tabs");

    // Print screen
    verifyPrintScreen(true, "test", "matrix", "matrix-test");
  }

  /**
   * Print chart screen. The grid of the grid and chart screen then shows the page size of its max attribute in its pager
   */
  @Test
  void t050_printChartScreen() {
    // Title
    setTestTitle("Print chart screen");

    // Print screen
    verifyPrintScreen(false, "test", "chart", "chart-test");

    // Check for pager values selector
    gotoScreen("test", "chart", "grid-and-chart");
    waitForLoadingBar();
    checkGridPageSize("25");
  }

  /**
   * Go to a screen and print the options
   *
   * @param allTabs     print all tabs
   * @param menuOptions Menu options
   */
  private void verifyPrintScreen(boolean allTabs, String... menuOptions) {
    // Go to matrix test
    gotoScreen(menuOptions);

    // Wait until the screen has loaded its data, so the print button is not clicked while the screen is still loading
    waitForLoadingBar();
    waitForButton("ButPrn");

    // Click print button
    clickButton("ButPrn");

    // Wait for the print dialog: its accept button is shown once the dialog is open
    waitForButton("ButDiaVal");

    // Select
    selectContain("ActPrn", "Generate");

    // Check all tabs if defined
    if (allTabs) {
      selectContain("TypPrn", "All tabs");
    } else {
      selectContain("TypPrn", "Selected tab");
    }

    // Click accept
    clickButton("ButDiaVal");

    // Accept message
    checkAndCloseMessage("success");

    // Wait for the print dialog to be closed
    checkDialogClosed("PrnOpt");
  }
}
