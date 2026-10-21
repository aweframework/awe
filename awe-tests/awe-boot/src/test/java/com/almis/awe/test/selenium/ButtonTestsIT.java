package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Buttons of the test screens: their dependencies and their actions. Every test opens its own screen.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class ButtonTestsIT extends AbstractSessionTests {

  ButtonTestsIT() {
    super(Session.TEST_MODULE);
  }

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
   * Test buttons: dependencies
   */
  @Test
  void t030_buttonTest() {
    // Title
    setTestTitle("Button test: dependencies");

    // Go to screen
    gotoScreen("test", "button-test");

    // Wait for button
    waitForButton("ButTxt");

    // Wait for button
    waitForButton("ButIco");

    // Verify that button is not visible
    checkButtonNotVisible("ButCnfTs1");

    // Verify that button is not visible
    checkButtonNotVisible("ButCnfTs2");

    // Select on selector
    selectContain("ButSel", "No");

    // Wait for button
    waitForButton("ButTxt");

    // Wait for button
    waitForButton("ButIco");

    // Verify that button is disabled
    checkButtonDisabled("ButCnfTs1");

    // Wait for button
    waitForButton("ButCnfTs2");

    // Select on selector
    selectContain("ButSel", "Yes");

    // Wait for button
    waitForButton("ButTxt");

    // Wait for button
    waitForButton("ButIco");

    // Wait for button
    waitForButton("ButCnfTs1");

    // Verify that button is not visible
    checkButtonNotVisible("ButCnfTs2");
  }

  /**
   * Test buttons: actions
   */
  @Test
  void t031_buttonTestActions() {
    // Title
    setTestTitle("Button test: actions");

    // Go to screen
    gotoScreen("test", "button-test");

    // Write text
    writeText("ButVal", "");

    // Click on button
    clickButton("ButSetVa1");

    // Check text
    checkCriterionContents("ButVal", "Valor1");

    // Click on button
    clickButton("ButSetVa2");

    // Check text
    checkCriterionContents("ButVal", "Valor2");

    // Click on button
    clickButton("ButSetVa3");

    // Check text
    checkCriterionContents("ButVal", "Valor3");
  }

  /**
   * Context menu on a button and on a criterion: a right click opens the menu declared with context-button and its
   * option runs its actions
   */
  @Test
  void t033_contextMenuOnButtonAndCriterion() {
    // Title
    setTestTitle("Button test: context menu on a button and on a criterion");

    // Go to screen
    gotoScreen("test", "button-test");

    // Right click on the button and choose its option
    contextMenuOnButton("ButTxt");
    clickContextButton("CtxButSet");
    checkCriterionContents("ButCtxBut", "FromButton");

    // Right click on the criterion and choose its option
    contextMenuOnCriterion("ButCtxCri");
    clickContextButton("CtxCriSet");
    checkCriterionContents("ButCtxCri", "FromCriterion");
  }

  /**
   * Context menu on tabs and on a wizard: a right click opens the menu declared with context-button and its option runs
   * its actions
   */
  @Test
  void t034_contextMenuOnTabAndWizard() {
    // Title
    setTestTitle("Button test: context menu on tabs and on a wizard");

    // Go to screen
    gotoScreen("test", "reveal-invalid-test");

    // Right click on the tabs and choose their option
    contextMenuOnTab("RevTabs");
    clickContextButton("CtxTabSet");
    checkCriterionContents("RevTabFirst", "FromTabs");

    // Right click on the wizard and choose its option
    contextMenuOnWizard("RevWizard");
    clickContextButton("CtxWizardSet");
    checkCriterionContents("RevStepFirst", "FromWizard");
  }
}
