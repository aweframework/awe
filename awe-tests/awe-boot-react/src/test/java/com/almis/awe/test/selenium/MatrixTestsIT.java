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
    checkLogoutWithConfirmation();
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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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

    // Go to screen
    openMatrixScreen();

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
   * Open a screen of the matrix menu by the name of its option
   *
   * @param option Menu option
   */
  private void openMatrixOption(String option) {
    gotoScreen("test", "matrix", option);
  }

  /**
   * Paginator test: the range of rows shown and the total
   */
  @Test
  void t091_gridPaginatorShowsRangeAndTotal() {
    // Title
    setTestTitle("Grid paginator: visible range and total");

    // Go to screen
    openMatrixOption("matrix-test-pager");

    // First page: five rows of eighteen
    checkRowContentsGrid("GrdPagerTst", "adminflare");
    checkGridPaginationInfo("View 1 - 5 of 18");

    // The grid has rows, so it does not show its empty state
    checkGridEmptyStateNotVisible();

    // Second page
    clickGridNextPage();
    checkGridPaginationInfo("View 6 - 10 of 18");
  }

  /**
   * Radio columns test: a radio column and a button-radio column show the options of their list in every cell
   */
  @Test
  void t092_gridRadioColumnsChooseOneOptionPerCell() {
    // Title
    setTestTitle("Grid radio columns: one choice per cell");

    // Go to screen
    openMatrixOption("matrix-test-radio");

    // The query chooses the option B in one row and nothing in the others
    checkRowContentsGrid("GrdRadioTst", "adminflare");
    checkGridRadioChoice("GrdRadioTst", "12", "choice", "B");
    checkGridRadioNoChoice("GrdRadioTst", "3", "choice");
    checkGridRadioNoChoice("GrdRadioTst", "3", "mode");

    // Choosing another option of the list replaces the previous choice of the same cell
    selectGridRadioOption("GrdRadioTst", "12", "choice", "A");
    checkGridRadioChoice("GrdRadioTst", "12", "choice", "A");

    // The rows are independent: a choice in another row leaves the first row as it was
    selectGridRadioOption("GrdRadioTst", "3", "choice", "C");
    checkGridRadioChoice("GrdRadioTst", "3", "choice", "C");
    checkGridRadioChoice("GrdRadioTst", "12", "choice", "A");

    // The arrow keys move the choice between the options of the cell
    moveGridRadioChoicePrevious("GrdRadioTst", "3", "choice");
    checkGridRadioChoice("GrdRadioTst", "3", "choice", "B");
    moveGridRadioChoiceNext("GrdRadioTst", "3", "choice");
    checkGridRadioChoice("GrdRadioTst", "3", "choice", "C");
    checkGridRadioChoice("GrdRadioTst", "12", "choice", "A");

    // The button-radio column is another choice of the row, with the same behaviour
    selectGridRadioOption("GrdRadioTst", "3", "mode", "2");
    checkGridRadioChoice("GrdRadioTst", "3", "mode", "2");
    selectGridRadioOption("GrdRadioTst", "3", "mode", "3");
    checkGridRadioChoice("GrdRadioTst", "3", "mode", "3");
    moveGridRadioChoicePrevious("GrdRadioTst", "3", "mode");
    checkGridRadioChoice("GrdRadioTst", "3", "mode", "2");
    moveGridRadioChoiceNext("GrdRadioTst", "3", "mode");
    checkGridRadioChoice("GrdRadioTst", "3", "mode", "3");
    checkGridRadioNoChoice("GrdRadioTst", "12", "mode");
    checkGridRadioChoice("GrdRadioTst", "3", "choice", "C");

    // The server receives the chosen value of each column in every row, and nothing for the rows without choice
    clickButton("ButRadioSend");
    checkMessageText("success", "12:A/-;3:C/3");
  }

  /**
   * Theme class test: the grid without header hides it and keeps the rows, a normal grid shows it
   */
  @Test
  void t093_gridThemeNoHeader() {
    // Title
    setTestTitle("Grid theme: no header");

    // The grid with the grid-no-header class keeps its rows but not its header
    openMatrixOption("matrix-test-no-header");
    checkRowContentsGrid("GrdNoHeaderTst", "adminflare");
    checkGridHeaderNotVisible("GrdNoHeaderTst", "name");
    checkGridFirstRowAtTop("GrdNoHeaderTst", "12", "name");

    // A grid without the class shows it
    openMatrixOption("matrix-test-radio");
    checkRowContentsGrid("GrdRadioTst", "adminflare");
    checkGridHeaderVisible("GrdRadioTst", "name");
  }

  /**
   * Theme class test: the cells take the colour scale class that the query assigns
   */
  @Test
  void t094_gridThemeColourScales() {
    // Title
    setTestTitle("Grid theme: colour scales");

    // Go to screen
    openMatrixOption("matrix-test-scale");

    // Every scale paints its cells with the class of the query
    checkRowContentsGrid("GrdScaleTst", "adminflare");
    checkCellHasClass("GrdScaleTst", "12", "seq3", "scale-seq-3");
    checkCellHasClass("GrdScaleTst", "12", "pos5", "scale-pos-5");
    checkCellHasClass("GrdScaleTst", "12", "neg1", "scale-neg-1");
  }

  /**
   * Empty state test: a grid without rows shows its configured empty state, and its action runs the button
   */
  @Test
  void t095_gridEmptyState() {
    // Title
    setTestTitle("Grid empty state");

    // Go to screen
    openMatrixOption("matrix-test-empty");

    // The configured empty state is shown
    checkGridEmptyState("No themes found", "Clear the search to see the themes again");

    // Its action runs the actions of the button of the grid
    clickGridEmptyAction();
    checkCriterionContents("EmptySearch", "cleared");
  }

  /**
   * Dialog test: the size classes give the dialog its width
   */
  @Test
  void t096_dialogSizeClasses() {
    // Title
    setTestTitle("Dialog size classes");

    // Go to screen
    openMatrixOption("matrix-test-dialog-size");

    // Small dialog
    clickButton("ButDlgSmall");
    checkDialogWidth("DlgSmall", 300);
    clickButton("ButDlgSmallClose");
    checkDialogClosed("DlgSmall");

    // Large dialog
    clickButton("ButDlgLarge");
    checkDialogWidth("DlgLarge", 900);
    clickButton("ButDlgLargeClose");
    checkDialogClosed("DlgLarge");
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
