package com.almis.awe.test.selenium;

import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class PrintTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   */
  @Test
  void t000_loginTest() {
    checkLogin("test", "test", "#ButUsrAct span.info-text", "Manager (test)");
  }

  /**
   * Log out from the application
   *
   * @
   */
  @Test
  void t999_logoutTest() {
    checkLogout(".slogan", "Almis Web Engine");
  }

  /**
   * Select test module on select criterion
   *
   * @ Error on test
   */
  @Test
  void t001_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectModule("Test");

    // Wait for text
    waitForText("mm-text", "Tests");

    // Check text
    checkVisible("[translate-multiple='MENU_TEST'");
  }

  /**
   * Print user list
   *
   * @ Error on test
   */
  @Test
  void t010_printUserList() {
    // Title
    setTestTitle("Print user list");

    // Print screen
    verifyPrintScreen(false, "tools", "users");
  }

  /**
   * Print profile list
   *
   * @ Error on test
   */
  @Test
  void t020_printProfilesList() {
    // Title
    setTestTitle("Print profiles list");

    // Print screen
    verifyPrintScreen(false, "tools", "profiles");
  }

  /**
   * Print matrix selected tab
   *
   * @ Error on test
   */
  @Test
  void t030_printMatrixSelectedTab() {
    // Title
    setTestTitle("Print matrix selected tab");

    // Print screen
    verifyPrintScreen(false, "test", "matrix", "matrix-test");
  }

  /**
   * Print matrix all tabs
   *
   * @ Error on test
   */
  @Test
  void t040_printMatrixAllTabs() {
    // Title
    setTestTitle("Print all matrix tabs");

    // Print screen
    verifyPrintScreen(true, "test", "matrix", "matrix-test");
  }

  /**
   * Print chart screen
   *
   * @ Error on test
   */
  @Test
  void t050_printChartScreen() {
    // Title
    setTestTitle("Print chart screen");

    // Print screen
    verifyPrintScreen(false, "test", "chart", "chart-test");
  }

  /**
   * Print chart and grid screen
   *
   * @ Error on test
   */
  @Test
  void t060_printChartAndGrid() {
    // Title
    setTestTitle("Print chart and grid");

    // Print screen
    verifyPrintScreen(false, "test", "chart", "grid-and-chart");

    // Check for pager values selector
    Select select = new Select(getDriver().findElement(By.cssSelector(".grid-pager")));
    WebElement option = select.getFirstSelectedOption();
    assertEquals("25", option.getText());
  }

  /**
   * Print chart and grid screen
   *
   * @ Error on test
   */
  @Test
  void t070_printLayout2() {
    // Title
    setTestTitle("Print layout 2");

    // Print screen
    verifyPrintScreen(false, "test", "layout", "layout2-test");
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

    // Wait for button
    waitForButton("ButPrn");

    // Wait for button (again)
    waitForButton("ButPrn");

    // Wait 1 second
    pause(1000);

    // Click print button
    clickButton("ButPrn");

    // Wait 1 second
    pause(1000);

    // Wait for button
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

    // Wait modal backdrop to disappear
    checkNotVisible(".modal-backdrop");
  }
}
