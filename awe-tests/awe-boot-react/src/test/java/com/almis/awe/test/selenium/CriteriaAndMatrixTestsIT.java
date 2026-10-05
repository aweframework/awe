package com.almis.awe.test.selenium;

import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class CriteriaAndMatrixTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   */
  @Test
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
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
    checkSuggestContents("Sug", "test (Manager)");

    // Click button
    clickButton("ButRst");

    // Check selector
    checkSuggestContents("Sug", "test (Manager)");
  }

  /**
   * Test criteria: text criteria
   */
  @Test
  void t011_criteriaTestText() {
    // Title
    setTestTitle("Test criteria: Text criteria");

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

    // Click an option of the button radio (this screen groups them in one criterion)
    clickCheckboxOption("RadBoxButGrp", "Radio5");
  }

  /**
   * Test criteria: Dependencies
   */
  @Test
  void t018_criteriaTestDependencies() {

    // Title
    setTestTitle("Test criteria: Dependencies");

    // Write text
    writeText("Txt", "radios");

    // Click checkbox
    clickCheckboxOption("ChkBoxButGrp", "ChkBoxVa21");

    // Wait for value
    checkCriterionContents("Tar", "checkbox on");

    // Click checkbox
    clickCheckboxOption("ChkBoxButGrp", "ChkBoxVa21");

    // Wait for value
    checkCriterionContents("Tar", "checkbox off");

    // Click button
    clickButton("ButCnf");

    // Wait for error message
    checkValidationErrorVisible();

    // Check suggest value
    checkSuggestContents("SugRea", "test (Manager)");

    // Click button
    clickButton("ButRst");

    // Check value
    checkCriterionContents("Num", "-123.456,10 EUR");

    // Check value
    checkCriterionContents("NumReq", "-123.456,10 EUR");

    // Check checked (the button groups of this screen have no option checked by default)
    checkCheckboxRadio(true, "ChkBoxVa5", "RadBox1");

    // Check not checked
    checkCheckboxRadio(false, "ChkBoxVa1", "ChkBoxVa2", "RadBox4");
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
   * Grid test: base grid
   */
  @Test
  void t041_gridTestBase() {
    // Title
    setTestTitle("Grid test: base");

    // Go to screen
    gotoScreen("test", "matrix", "matrix-test");

    // Wait for button
    waitForButton("ButPrn");

    // Click row contents
    clickRowContents("GrdSta", "awedb1");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    clickTab("TabSelMat", "Multiselect");

    // Verify that button is present
    checkGridPresent("GrdMus");

    // Verify that button is not visible
    checkGridNotVisible("GrdMus");
  }

  /**
   * Grid test: base grid context menu
   */
  @Test
  void t042_gridTestBaseContextMenu() {
    // Title
    setTestTitle("Grid test: base grid with context menu");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    clickTab("TabSelMat", "Static");

    // Context menu on grid
    contextMenuRowContents("GrdSta", "awedb1");

    // Wait for button
    waitForContextButton("CtxGrdStaAdd");

    // Click on component mask
    closeContextMenu();

    // Wait for context menu to hide
    checkContextMenuNotVisible();

    // Context menu on grid
    contextMenuRowContents("GrdSta", "awedb2");

    // Wait for button
    waitForContextButton("CtxGrdStaDel");

    // Click on component mask
    closeContextMenu();

    // Wait for context menu to hide
    checkContextMenuNotVisible();

    // Click on viewport
    clickGridViewport("GrdSta");

    // Click row contents: the row the context menu selected is selected again, so the click unselects it
    toggleRowContents("GrdSta", "awedb2");
  }

  /**
   * Grid test: multiselect grid
   */
  @Test
  void t051_gridTestMultiselect() {
    // Title
    setTestTitle("Grid test: Multiselect");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    clickTab("TabSelMat", "Multiselect");

    // Click on grid
    clickRowContents("GrdMus", "AWE DB 2");

    // Click on grid
    clickRowContents("GrdMus", "AWE DB 1");

    // Click on grid
    clickRowContents("GrdMus", "AWE DB 3");

    // Check row contents
    checkRowContentsGrid("GrdMus", "awedb2");
  }

  /**
   * Grid test: Editable grid
   */
  @Test
  void t061_gridTestEditable() {
    // Title
    setTestTitle("Grid test: Editable");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    clickTab("TabSelMat", "Editable");

    // Pause for 5 seconds
    pause(5000);

    // Edit row
    editRow("GrdEdi", "adminflare");

    // Click on date
    clickDate("GrdEdi", "FilDat");

    // Click on selector
    clickEnabledDatepickerDay();

    // Get selector text
    String date = getText("GrdEdi", "FilDat");

    // Save row
    saveRow("GrdEdi");

    // Check date on second row
    checkCellContents("GrdEdi", "2", "Dat", date);

    // Edit row
    editRow("GrdEdi", "asphalt");

    // Edit row
    editRow("GrdEdi", "clean");

    // Click on date
    selectDate("GrdEdi", "Dat", "23/10/1978");

    // Get date
    date = getText("GrdEdi", "Dat");

    // Save row
    saveRow("GrdEdi");

    // Check date on second row
    checkCellContents("GrdEdi", "2", "FilDat", date);

    // Context menu
    contextMenuRowContents("GrdEdi", "asphalt");

    // Wait for context button
    clickContextButton("CtxGrdEdiAddSel", "CtxGrdEdiAddUpp");

    // Save row
    saveRow("GrdEdi");

    // Click on grid
    checkRowContentsGrid("GrdEdi", "-123.456,10 $");

    // Check cell contents
    checkCellContents("GrdEdi", "1", "Txt", "adminflare");

    // Check cell contents
    checkCellContents("GrdEdi", "2", "Txt", "asphalt");
  }

  /**
   * Grid test: Multioperation grid
   */
  @Test
  void t071_gridTestMultiOperation() {
    // Title
    setTestTitle("Grid test: Multioperation");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    clickTab("TabSelMat", "Multioption");

    // Click on button
    clickButton("ButGrdMuoAdd");

    // Wait for visible
    checkGridIconVisible("GrdMuo", "RowIco", "plus");

    // Save row
    saveRow("GrdMuo");

    // Check icon
    checkGridIconVisible("GrdMuo", "RowIco", "plus");

    // Edit row
    editRow("GrdMuo", "1", "Des2");

    // Write on text
    writeText("GrdMuo", "Des2", "asdasda");

    // Save row
    saveRow("GrdMuo");

    // Check icon
    checkGridIconVisible("GrdMuo", "RowIco", "edit");

    // Context menu on grid
    contextMenu("GrdMuo", "3", "Des2");

    // Click on context button
    clickContextButton("CtxGrdMuoDel");

    // Check icon
    checkGridIconVisible("GrdMuo", "RowIco", "trash");
  }

  /**
   * Grid test: Tree grid
   */
  @Test
  void t081_gridTestTree() {
    // Title
    setTestTitle("Grid test: Tree grid");

    // Manage grid
    manageTreeGrid("Tree", "TreGrd");
  }

  /**
   * Grid test: Editable tree grid
   */
  @Test
  void t082_gridTestTreeEditable() {
    // Title
    setTestTitle("Grid test: Editable tree grid");

    // Manage grid
    manageTreeGrid("Editable tree", "TreGrdEdi");

    // Context menu
    contextMenu("TreGrdEdi", "ProGeneral-ModBase", "TreGrdEdi_Nam");

    // Select context menu option
    clickContextButton("CtxTreGrdEdiAddSel", "CtxTreGrdEdiAddChl");

    // Pause
    pause(500);

    // Click on a cell
    editRow("TreGrdEdi", "ProGeneral-ModBase", "TreGrdEdi_Nam");

    // Click on a cell
    editRow("TreGrdEdi", "new-row-0", "TreGrdEdi_Nam");

    // Click on a cell
    editRow("TreGrdEdi", "ProGeneral-ModBase", "TreGrdEdi_Nam");

    // Click on a cell
    editRow("TreGrdEdi", "new-row-0", "TreGrdEdi_Nam");

    // Save row
    saveRow("TreGrdEdi");

    // Click on button
    clickTreeButton("TreGrdEdi", "ProOperator");

    // Context menu
    contextMenu("TreGrdEdi", "ProOperator", "TreGrdEdi_Nam");

    // Select context menu option
    clickContextButton("CtxTreGrdEdiDel");

    // Check not visible
    checkTreeRowNotVisible("TreGrdEdi", "ProOperator");
  }

  /**
   * Grid test: Loading tree grid
   */
  @Test
  void t083_gridTestLoadingTree() {
    // Title
    setTestTitle("Grid test: Loading tree grid");

    // Manage loading tree grid
    manageLoadingTreeGrid("Loading tree", "TreGrdLoa");
  }

  /**
   * Grid test: Loading tree grid
   */
  @Test
  void t084_gridTestEditableLoadingTree() {
    // Title
    setTestTitle("Grid test: Loading editable tree grid");

    // Manage loading tree grid
    manageLoadingTreeGrid("Editable multioperation tree", "TreGrdLoaEdi");

    // Context menu
    contextMenu("TreGrdLoaEdi", "ProGeneral-ModBase", "TreGrdLoaEdi_Nam");

    // Select context menu option
    clickContextButton("CtxTreGrdLoaEdiAddSel", "CtxTreGrdLoaEdiAddChl");

    // Check new row visible
    checkTreeRowVisible("TreGrdLoaEdi", "new-row-0");

    // Pause
    pause(250);

    // Save row
    saveRow("TreGrdLoaEdi");

    // Click on button
    clickTreeButton("TreGrdLoaEdi", "ProOperator");

    // Context menu
    contextMenu("TreGrdLoaEdi", "ProOperator", "TreGrdLoaEdi_Nam");

    // Select context menu option
    clickContextButton("CtxTreGrdLoaEdiDel");

    // Check visible
    checkTreeRowDeleted("TreGrdLoaEdi", "ProOperator");
  }

  /**
   * Open and close some tree grid leafs
   *
   * @param gridTab Tab where the grid is
   * @param gridId  Grid identifier
   */
  private void manageTreeGrid(String gridTab, String gridId) {
    // Click on tab
    clickTab("TabSelMat", gridTab);

    // Click on button
    clickTreeButton(gridId, "ProAdministrator");

    // Click on button
    clickTreeButton(gridId, "ProGeneral");

    // Click on button
    clickTreeButton(gridId, "ProOperator");

    // Check not visible
    checkTreeIconNotVisible(gridId, "ProAdministrator-ModBase");

    // Click on button
    clickTreeButton(gridId, "ProAdministrator");

    // Check not visible (the node is collapsed)
    checkTreeRowNotVisible(gridId, "ProAdministrator-ModBase");

    // Click on button
    clickTreeButton(gridId, "ProGeneral");

    // Check not visible (the node is collapsed)
    checkTreeRowNotVisible(gridId, "ProGeneral-ModBase");

    // Click on button
    clickTreeButton(gridId, "ProGeneral");

    // Check visible
    checkTreeRowVisible(gridId, "ProGeneral-ModBase");
  }

  /**
   * Open and close some tree grid leafs on a loading treegrid
   *
   * @param gridTab Tab where the grid is
   * @param gridId  Grid identifier
   */
  private void manageLoadingTreeGrid(String gridTab, String gridId) {
    // Click on tab
    clickTab("TabSelMat", gridTab);

    // Click on button
    clickTreeButton(gridId, "ProAdministrator");

    // Click on button
    clickTreeButton(gridId, "ProGeneral");

    // Click on button
    clickTreeButton(gridId, "ProOperator");

    // Click on button
    clickTreeButton(gridId, "ProAdministrator-ModBase");

    // Click on button
    clickTreeButton(gridId, "ProGeneral-ModBase");

    // Check visible
    checkTreeRowVisible(gridId, "ProOperator-ModBase");
  }
}
