package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Matrix of the test screens: base, multiselect, editable and multioperation grids, and the tree grids. Every
 * test opens the matrix screen, so each one can run on its own.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class MatrixTestsIT extends AbstractSessionTests {

  MatrixTestsIT() {
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
   * Open the matrix test screen: every test of the class starts from it
   */
  private void openMatrixScreen() {
    // Go to screen
    gotoScreen("test", "matrix", "matrix-test");

    // Wait for button
    waitForButton("ButPrn");
  }

  /**
   * Grid test: base grid
   */
  @Test
  void t041_gridTestBase() {
    // Title
    setTestTitle("Grid test: base");

    // Go to screen
    openMatrixScreen();

    // Click row contents
    clickRowContents("GrdSta", "awedb1");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    clickTab("TabSelMat", "ENUM_MATRIX_MULTISELECT");

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

    // Go to screen
    openMatrixScreen();

    // Click on tab
    clickTab("TabSelMat", "ENUM_MATRIX_STATIC");

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

    // Click row contents
    clickRowContents("GrdSta", "awedb2");
  }

  /**
   * Grid test: multiselect grid
   */
  @Test
  void t051_gridTestMultiselect() {
    // Title
    setTestTitle("Grid test: Multiselect");

    // Go to screen
    openMatrixScreen();

    // Click on tab
    clickTab("TabSelMat", "ENUM_MATRIX_MULTISELECT");

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

    // Go to screen
    openMatrixScreen();

    // Click on tab
    clickTab("TabSelMat", "ENUM_MATRIX_EDITABLE");

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

    // Go to screen
    openMatrixScreen();

    // Click on tab
    clickTab("TabSelMat", "ENUM_MATRIX_MULTIOPTION");

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

    // Go to screen
    openMatrixScreen();

    // Manage grid
    manageTreeGrid("ENUM_MATRIX_TREEGRID", "TreGrd");
  }

  /**
   * Grid test: Editable tree grid
   */
  @Test
  void t082_gridTestTreeEditable() {
    // Title
    setTestTitle("Grid test: Editable tree grid");

    // Go to screen
    openMatrixScreen();

    // Manage grid
    manageTreeGrid("ENUM_MATRIX_TREEGRID_EDITABLE", "TreGrdEdi");

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

    // Go to screen
    openMatrixScreen();

    // Manage loading tree grid
    manageLoadingTreeGrid("ENUM_MATRIX_TREEGRID_LOADNODE", "TreGrdLoa");
  }

  /**
   * Grid test: Loading tree grid
   */
  @Test
  void t084_gridTestEditableLoadingTree() {
    // Title
    setTestTitle("Grid test: Loading editable tree grid");

    // Go to screen
    openMatrixScreen();

    // Manage loading tree grid
    manageLoadingTreeGrid("ENUM_MATRIX_TREEGRID_EDITABLE_LOADNODE", "TreGrdLoaEdi");

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

    // Click on button
    clickTreeButton(gridId, "ProAdministrator-ModBase");

    // Click on button
    clickTreeButton(gridId, "ProAdministrator-ModBase");

    // Click on button
    clickTreeButton(gridId, "ProGeneral");

    // Check visible
    checkTreeIconVisible(gridId, "ProGeneral-ModBase");
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
