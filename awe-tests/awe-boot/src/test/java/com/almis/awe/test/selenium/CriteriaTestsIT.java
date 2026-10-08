package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Criteria of the test screens: every criterion type, their dependencies and the reset of the criteria.
 * Every test opens its own screen, so each one can run on its own. The login and the module selection are checked
 * here once; the rest of the tests start from the state that the setup leaves.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class CriteriaTestsIT extends AbstractSessionTests {

  CriteriaTestsIT() {
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
   * Select test module on select criterion
   */
  @Test
  @StartsFrom(Session.LOGGED_IN)
  void t002_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectModule("Test");

    // Check the menu of the module
    checkMenuOption("test", "Tests");
  }

  /**
   * Test criteria: initialization
   */
  @Test
  void t010_criteriaTest() {
    // Title
    setTestTitle("Test criteria: Initialization");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButRst");

    // Wait for value
    checkCriterionContents("Tar", "checkbox off");

    // Check selector
    checkSelectContents("Sug", "test (Manager)");

    // Click button
    clickButton("ButRst");

    // Check selector
    checkSelectContents("Sug", "");
  }

  /**
   * Test criteria: text criteria
   */
  @Test
  void t011_criteriaTestText() {
    // Title
    setTestTitle("Test criteria: Text criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text on criterion
    writeText("Txt", "Texto Normal");

    // Check text on criterion
    checkCriterionLabel("Unt", "Texto Normal");

    // Write text on criterion
    writeText("TxtReq", "Text Required");

  }

  /**
   * Test criteria: numeric criteria
   */
  @Test
  void t012_criteriaTestNumeric() {
    // Title
    setTestTitle("Test criteria: Numeric criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text on numeric
    writeText("Num", "10000");

    // Check text on criterion
    checkCriterionLabel("Unt", "Numeric");

    // Write text on numeric
    writeText("NumReq", "-20000");

  }

  /**
   * Test criteria: date and time criteria
   */
  @Test
  void t013_criteriaTestDate() {
    // Title
    setTestTitle("Test criteria: Date and time criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Select a date
    selectDate("Cal", "23/10/1978");

    // Select a date
    selectDate("CalReq", "23/10/1978");

    // Write hour
    writeText("Tim", "12:23:41");

    // Write hour
    writeText("TimReq", "12:23:41");

    // Click on date
    clickDate("FilCal");

    // Click on selector
    clickEnabledDatepickerDay();

    // Click on date
    clickDate("FilCalReq");

    // Click on selector
    clickEnabledDatepickerDay();

    // Check date contents
    checkCriterionContents("Cal", "23/10/1978");

    // Check time contents
    checkCriterionContents("TimReq", "12:23:41");
  }

  /**
   * Test criteria: Suggest and select criteria
   */
  @Test
  void t014_criteriaTestSuggestSelect() {
    // Title
    setTestTitle("Test criteria: Suggest and select criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Select on selector
    suggest("Sug", "test", "test");

    // Wait for loader
    waitForLoadingBar();

    // Select on selector
    selectContain("SelDep", "Yes");

    // Wait for loader
    waitForLoadingBar();

    // Pause
    pause(250);

    // Select on selector
    selectContain("SelDepDep", "Yes");

    // Select on selector
    suggest("SugReq", "a", "a");

    // Select on selector
    selectContain("Sel", "No");

    // Select on selector
    suggest("SugNum", "1", "1");

    // Select on selector
    selectContain("Sel", "Yes");

    // Select on selector
    selectContain("SelReq", "Administrator");

    // Check select
    checkSelectContents("SelReq", "Administrator");
  }

  /**
   * Test criteria: textarea criteria
   */
  @Test
  void t015_criteriaTestTextarea() {
    // Title
    setTestTitle("Test criteria: Textarea criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text
    writeText("Tar", "Area de Texto");

    // Write text
    writeText("TarReq", "Area de Texto");

    // Check text
    checkCriterionContents("Tar", "Area de Texto");

    // Check text
    checkCriterionContents("TarReq", "Area de Texto");
  }

  /**
   * Test criteria: suggest multiple criteria
   */
  @Test
  void t016_criteriaTestSelectSuggestMultiple() {

    // Title
    setTestTitle("Test criteria: Select and suggest multiple");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Select on selector
    suggestMultiple("SelMul", "test@", "test@");

    // Verify text
    checkMultipleSelectorContents("SelMul", "test (test@test.com)");

    // Select on selector
    suggestMultiple("SelMulReq", "e", "e");

    // Verify text
    checkMultipleSelectorContents("SelMulReq", "General");

    // Select on selector
    suggestMultiple("SugMul", "test", "test");

    // Verify text
    checkMultipleSelectorContents("SugMul", "test (test@test.com)");

    // Select on selector
    suggestMultiple("SugMulReq", "test", "test");

    // Verify text
    checkMultipleSelectorContents("SugMulReq", "test (test@test.com)");
  }

  /**
   * Test criteria: Checkbox and radio
   */
  @Test
  void t017_criteriaTestCheckboxRadio() {

    // Title
    setTestTitle("Test criteria: Checkbox and radio");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Click checkbox
    clickCheckbox("ChkBoxVa1");

    // Check text on criterion
    checkCriterionLabel("Unt", "Inf");

    // Click checkbox
    clickCheckbox("ChkBoxVa2");

    // Check text on criterion
    checkCriterionUnit("Unt", "Inf");

    // Click checkbox
    clickCheckbox("ChkBoxVa5");

    // Click checkbox
    clickCheckbox("RadBox4");

    // Click checkbox
    clickCheckbox("RadBox1");

    // Check text on criterion
    checkCriterionUnit("Unt", "EUR");

    // Click checkbox
    clickCheckbox("RadBox3");

    // Check text on criterion
    checkCriterionUnit("Unt", "USD");

    // Click checkbox
    clickCheckbox("RadBox4");

    // Click checkbox
    clickCheckbox("RadBox25");
  }

  /**
   * Test criteria: Dependencies
   */
  @Test
  void t018_criteriaTestDependencies() {

    // Title
    setTestTitle("Test criteria: Dependencies");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text
    writeText("Txt", "radios");

    // Click checkbox
    clickCheckbox("ChkBoxVa21");

    // Wait for value
    checkCriterionContents("Tar", "checkbox on");

    // Click checkbox
    clickCheckbox("ChkBoxVa21");

    // Wait for value
    checkCriterionContents("Tar", "checkbox off");

    // Click button
    clickButton("ButCnf");

    // Wait for error message
    checkValidationErrorVisible();

    // Check suggest value
    checkSelectContents("SugRea", "test (Manager)");

    // Click button
    clickButton("ButRst");

    // Check value
    checkCriterionContents("Num", "-123.456,10 EUR");

    // Check value
    checkCriterionContents("NumReq", "-123.456,10 EUR");

    // Check checked
    checkCheckboxRadio(true, "ChkBoxVa5", "RadBox1", "ChkBoxVa22", "ChkBoxVa24", "RadBox22");

    // Check not checked
    checkCheckboxRadio(false, "ChkBoxVa1", "ChkBoxVa2", "RadBox4", "RadBox25");
  }

  /**
   * Criteria reset test
   */
  @Test
  void t020_criteriaReset() {
    // Title
    setTestTitle("Criteria reset");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-reset");

    // Wait for button
    waitForButton("ButRst");

    // Write text
    writeText("CrtTst", "test");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");

    // Click button
    clickButton("ButRstTar");

    // Wait for button
    waitForButton("ButRst");

    // Check text
    checkCriterionContents("CrtTst", "1");

    // Click button
    clickButton("ButRst");

    // Wait for button
    waitForButton("ButTxt");

    // Check text
    checkCriterionContents("CrtTst", "xml");

    // Click button
    clickButton("ButTxt");

    // Wait for button
    waitForButton("ButRstSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "otra cosa");

    // Click button
    clickButton("ButRstSpe");

    // Wait for button
    waitForButton("ButTxt");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");

    // Click button
    clickButton("ButTxt");

    // Wait for button
    waitForButton("ButRstTarSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "otra cosa");

    // Click button
    clickButton("ButRstTarSpe");

    // Wait for button
    waitForButton("ButRst");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");

    // Click button
    clickButton("ButRst");

    // Wait for button
    waitForButton("ButTxt");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "");

    // Check text
    checkCriterionContents("CrtTstHid", "RstTst");

    // Click button
    clickButton("ButTxt");

    // Wait for button
    waitForButton("ButRstSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "otra cosa");

    // Click button
    clickButton("ButRstSpe");

    // Wait for button
    waitForButton("ButRstSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");
  }
}
