package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matrix of the test screens: base, multiselect, editable and multioperation grids, and the tree grids. Every
 * test opens the matrix screen, so each one can run on its own.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("ApplicationIntegrationIT")
class MatrixTestsIT extends AbstractSessionTests {

  private static final String BUTTON_DEFERRED_DIALOG = "ButDefDialog";
  private static final String DEFERRED_SELECT = "DefSelect";
  private static final String BUTTON_DEFERRED_SAVE = "ButDefSave";
  private static final String MATRIX_PAGER_OPTION = "matrix-test-pager";
  private static final String ROW_DETAIL_GRID = "GrdRowDetailTst";
  private static final String ROW_DETAIL_COMPONENTS_GRID = "GrdRowDetailCmp";
  private static final String ROW_DETAIL_NESTED_GRID = "GrdRowDetailCmpCus";
  private static final String ROW_DETAIL_INCLUDE_GRID = "GrdRowDetailCmpInc";
  private static final String ROW_DETAIL_INCLUDED_GRID = "GrdRowDetailCmpIncCus";

  /**
   * Time that a page with wrapping columns may take to render, as a multiple of the time of the same page without wrapping.
   * The wrapping grid turns the virtualization of ui-grid off and renders all its rows, and still measured about 0.2 of the
   * time of the single-line grid in this harness (3 runs), so the 10% of the feature holds with room to spare
   */
  private static final double WRAP_RENDER_TIME_LIMIT = 1.10;
  private static final String MOVE_ROWS_GRID = "GrdMoveRows";
  private static final String MOVE_ROWS_COLUMN = "label";
  private static final String STEP_ONE = "Step one";
  private static final String STEP_TWO = "Step two";
  private static final String STEP_THREE = "Step three";
  private static final String STEP_FOUR = "Step four";
  private static final String STEP_FIVE = "Step five";

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
   * Grid test: a row button with the show-context-menu action opens the context menu of the grid for its row
   */
  @Test
  void t043_gridTestRowButtonContextMenu() {
    // Title
    setTestTitle("Grid test: row button that opens the context menu");

    // Go to screen
    openMatrixScreen();

    // Click on tab
    clickTab("TabSelMat", "ENUM_MATRIX_MULTIOPTION");

    // A left click on the button of a row selects the row and opens the menu of the grid under it
    clickGridCellButton("GrdMuo", "3", "CtxBtn");

    // The options that depend on the selected row are shown
    waitForContextButton("CtxGrdMuoAddSel");

    // The option runs its action over the selected row
    clickContextButton("CtxGrdMuoDel");
    checkGridIconVisible("GrdMuo", "RowIco", "trash");
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
   * Grid test: move the selected row of an editable grid to the top, up, down and to the bottom, and drag the rows by
   * their grip (grids with move-rows)
   */
  @Test
  void t062_gridTestMoveRows() {
    // Title
    setTestTitle("Grid test: move rows");

    // Go to screen
    gotoScreen("test", "matrix", "matrix-test-move-rows");

    // The rows come in the order of the query
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_ONE, STEP_TWO, STEP_THREE, STEP_FOUR, STEP_FIVE);

    // Move the third row to the top
    clickRowContents(MOVE_ROWS_GRID, STEP_THREE);
    clickButton("ButMoveRowTop");
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_THREE, STEP_ONE, STEP_TWO, STEP_FOUR, STEP_FIVE);

    // Move the fourth row one position up and down: the row stays selected
    clickRowContents(MOVE_ROWS_GRID, STEP_FOUR);
    clickButton("ButMoveRowUp");
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_THREE, STEP_ONE, STEP_FOUR, STEP_TWO, STEP_FIVE);
    clickButton("ButMoveRowDown");
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_THREE, STEP_ONE, STEP_TWO, STEP_FOUR, STEP_FIVE);

    // Move it to the bottom
    clickButton("ButMoveRowBottom");
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_THREE, STEP_ONE, STEP_TWO, STEP_FIVE, STEP_FOUR);

    // Every row shows the grip that drags it
    checkGridRowGripShown(MOVE_ROWS_GRID, 1);
    checkGridRowGripShown(MOVE_ROWS_GRID, 5);

    // Drag the first row below the third one
    dragGridRowAfter(MOVE_ROWS_GRID, 1, 3);
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_ONE, STEP_TWO, STEP_THREE, STEP_FIVE, STEP_FOUR);

    // Drag the last row above the second one
    dragGridRowBefore(MOVE_ROWS_GRID, 5, 2);
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_ONE, STEP_FOUR, STEP_TWO, STEP_THREE, STEP_FIVE);

    // Drag them back to the order of the query
    dragGridRowAfter(MOVE_ROWS_GRID, 2, 5);
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_ONE, STEP_TWO, STEP_THREE, STEP_FIVE, STEP_FOUR);
    dragGridRowAfter(MOVE_ROWS_GRID, 5, 3);
    checkGridColumnOrder(MOVE_ROWS_GRID, MOVE_ROWS_COLUMN, STEP_ONE, STEP_TWO, STEP_THREE, STEP_FOUR, STEP_FIVE);
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
   * Open a screen of the matrix menu by the name of its option
   *
   * @param option Menu option
   */
  private void openMatrixOption(String option) {
    gotoScreen("test", "matrix", option);
  }

  /**
   * Paginator test: the range of rows shown and the total. The wide pager of the AngularJS client has no hook for its
   * page links, so the change of page is checked in the React client only
   */
  @Test
  void t091_gridPaginatorShowsRangeAndTotal() {
    // Title
    setTestTitle("Grid paginator: visible range and total");

    // Go to screen
    openMatrixOption(MATRIX_PAGER_OPTION);

    // First page: five rows of eighteen
    checkRowContentsGrid("GrdPagerTst", "adminflare");
    checkGridPaginationInfo("View 1 - 5 of 18");

    // The grid has rows, so it does not show its empty state
    checkGridEmptyStateNotVisible();
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
   * Side panel test (#843): a panel opens against its edge as wide as its maximum width, keeps the focus inside while it
   * is shown, closes with Esc, its buttons or its close button and gives the focus back to the button that opened it
   */
  @Test
  void t117_sidePanelEdgeWidthFocusAndClose() {
    // Title
    setTestTitle("Side panel: edge, width, focus and close");

    // Go to screen
    openMatrixOption("matrix-test-side-panel");

    // Right panel: on the right edge, as wide as its maximum width, Tab goes round inside it
    clickButton("ButPnlRight");
    checkSidePanelOpen("PnlRight", "right", "30rem");
    checkSidePanelKeepsTheFocus("PnlRight", 8);

    // Esc closes it and the focus goes back to the button that opened it
    closeSidePanelWithEscape("PnlRight");
    checkButtonFocused("ButPnlRight");

    // Accept closes it and runs its actions
    clickButton("ButPnlRight");
    checkSidePanelOpen("PnlRight", "right", "30rem");
    clickButton("ButPnlRightAccept");
    checkSidePanelClosed("PnlRight");
    checkCriterionContents("CrtPnlResult", "accepted");

    // Left panel: on the left edge, it closes with the close button of its header
    clickButton("ButPnlLeft");
    checkSidePanelOpen("PnlLeft", "left", "20rem");
    closeSidePanel("PnlLeft");
    checkSidePanelClosed("PnlLeft");

    // A grid with columns-panel="side-panel" opens its Columns as a side panel
    openGridColumns("GrdPnlCols");
    checkGridColumnsPanel("GrdPnlCols", "side-panel");
    checkGridColumnsCounter("GrdPnlCols", 4, 4);
    closeGridColumns("GrdPnlCols");
  }

  /**
   * Popover test (#843): a popover opens next to its anchor (below it or at its right), keeps the focus inside while it
   * is shown, closes with Esc, its close button, its buttons or a click on its anchor (which does not open it again) and
   * gives the focus back to the button that opened it. The grid of the deferred popover is loaded once it opens, its
   * Columns work inside it, and its Cancel button closes it and leaves the result as it was
   */
  @Test
  void t118_popoverNextToItsAnchorFocusAndClose() {
    // Title
    setTestTitle("Popover: anchor, focus and close");

    // Go to screen
    openMatrixOption("matrix-test-popover");

    // Popover below its anchor with the rows of its grid, Tab goes round inside it (criteria, grid and buttons)
    clickButton("ButPopBottom");
    checkPopoverOpen("PopBottom", "ButPopBottom", "bottom");
    checkRowContentsGrid("GrdPopThemes", "adminflare");
    checkPopoverKeepsTheFocus("PopBottom", 15);

    // Esc closes it and the focus goes back to the button that opened it
    closePopoverWithEscape("PopBottom");
    checkButtonFocused("ButPopBottom");

    // The close button of its header closes it too
    clickButton("ButPopBottom");
    checkPopoverOpen("PopBottom", "ButPopBottom", "bottom");
    closePopover("PopBottom");
    checkPopoverClosed("PopBottom");
    checkButtonFocused("ButPopBottom");

    // The Columns of its grid open over it and hide and show a column, and it stays open (#843 L10). Cancel closes it
    // and leaves the result as it was
    clickButton("ButPopBottom");
    checkPopoverOpen("PopBottom", "ButPopBottom", "bottom");
    openGridColumns("GrdPopThemes");
    checkGridColumnsPanel("GrdPopThemes", "popover");
    hideGridColumn("GrdPopThemes", "name");
    checkGridColumnNotShown("GrdPopThemes", "name");
    showGridColumn("GrdPopThemes", "name");
    closeGridColumns("GrdPopThemes");
    checkPopoverOpen("PopBottom", "ButPopBottom", "bottom");
    clickButton("ButPopBottomCancel");
    checkPopoverClosed("PopBottom");
    checkButtonFocused("ButPopBottom");
    checkCriterionContents("CrtPopResult", "pending");

    // Accept closes it and runs its actions
    clickButton("ButPopBottom");
    checkPopoverOpen("PopBottom", "ButPopBottom", "bottom");
    clickButton("ButPopBottomAccept");
    checkPopoverClosed("PopBottom");
    checkCriterionContents("CrtPopResult", "accepted");

    // Popover at the right of its anchor: a click on the anchor closes it and does not open it again
    clickButton("ButPopRight");
    checkPopoverOpen("PopRight", "ButPopRight", "right");
    clickButton("ButPopRight");
    checkPopoverClosed("PopRight");

    // It opens again and its button closes it
    clickButton("ButPopRight");
    checkPopoverOpen("PopRight", "ButPopRight", "right");
    clickButton("ButPopRightClose");
    checkPopoverClosed("PopRight");
  }

  /**
   * Sparkline columns test: the series of every row is drawn as a small chart, whatever the type, and the series that
   * cannot be drawn leave the cell empty
   */
  @Test
  void t097_gridSparklineColumnsDrawTheSeriesOfEachRow() {
    // Title
    setTestTitle("Grid sparkline columns: the series of each row as a small chart");

    // Go to screen
    openMatrixOption("matrix-test-sparkline");

    // The same series is drawn as a line, as bars and as an area, with a tooltip value per point
    checkGridSparkline("GrdSparklineTst", "1", "line", "line", "1,2,3,5,8,13");
    checkGridSparkline("GrdSparklineTst", "1", "bar", "bar", "1,2,3,5,8,13");
    checkGridSparkline("GrdSparklineTst", "1", "area", "area", "1,2,3,5,8,13");

    // Negative values are drawn too, and a series with a single value is one point
    checkGridSparkline("GrdSparklineTst", "3", "bar", "bar", "3,5,-1,8,-4,2");
    checkGridSparkline("GrdSparklineTst", "4", "line", "line", "-3,-5,-1,-8");
    checkGridSparkline("GrdSparklineTst", "5", "line", "line", "7");
    checkGridSparkline("GrdSparklineTst", "5", "bar", "bar", "7");

    // The lowest, highest and last points are marked only in the column that asks for it
    checkGridSparklineMarkers("GrdSparklineTst", "3", "marks", 3);
    checkGridSparklineMarkers("GrdSparklineTst", "3", "line", 0);

    // An empty series and a value that is not a series leave the cell empty, with no error
    checkGridSparklineEmpty("GrdSparklineTst", "6", "line");
    checkGridSparklineEmpty("GrdSparklineTst", "7", "bar");

    // The chart does not take the keyboard focus, so the navigation of the grid is not affected
    checkGridSparklineNotFocusable("GrdSparklineTst", "3", "line");
  }

  /**
   * Check how many times the query of a component of the deferred load screen has run
   *
   * @param component Component whose query is counted
   * @param calls     Times it has run
   */
  private void checkDeferredCalls(String component, String calls) {
    clickButton("ButDefCalls");
    checkCellContents("GrdDefCalls", component, "calls", calls);
  }

  /**
   * Deferred dialog test: its queries do not run with the screen, they run the first time it is opened (and only then)
   */
  @Test
  void t110_deferredDialogLoadsOnFirstOpening() {
    // Title
    setTestTitle("Deferred dialog loads on its first opening");

    // Go to screen
    openMatrixOption("matrix-test-deferred");

    // The screen load ran the queries of what is not deferred, and none of the deferred dialog
    checkDeferredCalls("EagSelect", "1");
    checkDeferredCalls("TabFirstSelect", "1");
    checkDeferredCalls(DEFERRED_SELECT, "0");
    checkDeferredCalls("DefRows", "0");
    checkDeferredCalls("DefText", "0");

    // The first opening loads the data of the dialog
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkRowContentsGrid("GrdDefRows", "3");
    checkCriterionContents("DefText", "Loaded");
    // The select has no selection from the server: it shows its first option
    checkSelectContents(DEFERRED_SELECT, "One");
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");
    checkDeferredCalls(DEFERRED_SELECT, "1");
    checkDeferredCalls("DefRows", "1");
    checkDeferredCalls("DefText", "1");

    // The next openings do not load it again
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkRowContentsGrid("GrdDefRows", "3");
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");
    checkDeferredCalls(DEFERRED_SELECT, "1");
    checkDeferredCalls("DefRows", "1");

    // A deferred dialog does not make the other ones wait
    checkDeferredCalls("TabSecondSelect", "0");
  }

  /**
   * Control for the deferred dialog: a grid in a dialog which is not deferred shows its rows when the dialog opens
   */
  @Test
  void t109_dialogGridRendersWithoutDeferral() {
    // Title
    setTestTitle("Dialog grid renders without deferral");

    // Go to screen
    openMatrixOption("matrix-test-deferred");

    // The dialog was loaded with the screen
    clickButton("ButEagDialog");
    checkRowContentsGrid("GrdEagRows", "E3");
    clickButton("ButEagClose");
    checkDialogClosed("DlgEager");
  }

  /**
   * Deferred tab test: the queries of a deferred page do not run with the screen, they run the first time it is selected
   */
  @Test
  void t111_deferredTabLoadsOnFirstSelection() {
    // Title
    setTestTitle("Deferred tab loads on its first selection");

    // Go to screen
    openMatrixOption("matrix-test-deferred");

    // The first page was loaded with the screen, the deferred one was not
    checkDeferredCalls("TabFirstSelect", "1");
    checkDeferredCalls("TabSecondSelect", "0");
    checkDeferredCalls("TabSecondRows", "0");

    // The first selection of the deferred page loads its data
    clickTab("TabDeferred", "ENUM_DEFERRED_SECOND");
    checkRowContentsGrid("GrdTabRows", "Two");
    clickTab("TabDeferred", "ENUM_DEFERRED_FIRST");
    checkDeferredCalls("TabSecondSelect", "1");
    checkDeferredCalls("TabSecondRows", "1");

    // Going back to it does not load it again
    clickTab("TabDeferred", "ENUM_DEFERRED_SECOND");
    checkRowContentsGrid("GrdTabRows", "Two");
    clickTab("TabDeferred", "ENUM_DEFERRED_FIRST");
    checkDeferredCalls("TabSecondSelect", "1");
    checkDeferredCalls("TabSecondRows", "1");
  }

  /**
   * Deferred dialog test: a value set before the first opening is kept, the initial load does not replace it
   */
  @Test
  void t112_deferredDialogKeepsTheValueSetBeforeItsFirstOpening() {
    // Title
    setTestTitle("Deferred dialog keeps a value set before its first opening");

    // Go to screen
    openMatrixOption("matrix-test-deferred");

    // An action gives a value to a criterion of the dialog which was not opened yet
    clickButton("ButDefPreset");

    // The first opening loads the dialog, but the value of the action stays
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkRowContentsGrid("GrdDefRows", "3");
    checkCriterionContents("DefText", "Preset");
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");
  }

  /**
   * Deferred dialog test: the required criteria of a deferred dialog which was never opened do not block the validation
   */
  @Test
  void t113_deferredDialogNeverOpenedDoesNotBlockValidation() {
    // Title
    setTestTitle("Deferred dialog never opened does not block validation");

    // Go to the screen from another one, so it is loaded again (the previous test leaves the dialog loaded on it)
    openMatrixOption(MATRIX_PAGER_OPTION);
    openMatrixOption("matrix-test-deferred");
    checkDeferredCalls(DEFERRED_SELECT, "0");
    waitForLoadingBar();
    waitForButtonClickability(BUTTON_DEFERRED_SAVE);

    // Its required criteria are ignored: the save goes through without opening the dialog
    clickButton(BUTTON_DEFERRED_SAVE);
    checkCriterionContents("DefSaved", "Saved");
  }

  /**
   * Deferred dialog test: once the dialog was opened its required criteria validate as normal
   */
  @Test
  void t114_deferredDialogOpenedValidatesItsRequiredCriteria() {
    // Title
    setTestTitle("Deferred dialog opened validates its required criteria");

    // Go to screen
    openMatrixOption("matrix-test-deferred");

    // Open the dialog (its required criteria load), empty one and close it
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkRowContentsGrid("GrdDefRows", "3");
    writeText("DefText", "");
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");

    // Now the empty required criterion blocks the validation: wait for the validation error, then check nothing was saved
    clickButton(BUTTON_DEFERRED_SAVE);
    checkValidationErrorVisible();
    checkCriterionContents("DefSaved", "");
  }

  /**
   * Wrapping cells test: a column that wraps shows its whole text over several lines and the row grows to fit it; with
   * a maximum number of lines the text is cut at that line and its whole value shows on hover and on keyboard focus.
   * Selection, sorting and inline editing keep working with the taller rows
   */
  @Test
  void t098_gridWrappingCells() {
    // Title
    setTestTitle("Grid wrapping cells");

    // Go to screen
    openMatrixOption("matrix-test-wrap");
    checkRowContentsGrid("GrdWrapTst", "WR-005");

    // The column that wraps shows its whole text over several lines
    checkGridCellLinesAtLeast("GrdWrapTst", "5", "title", 2);
    checkGridCellTextCut("GrdWrapTst", "5", "title", false);

    // The column with a maximum shows three lines, and the whole value on hover and on keyboard focus
    String description = words(5 * 7, 6 + (5 % 6) * 14);
    checkGridCellLines("GrdWrapTst", "5", "description", 3);
    checkGridCellTextCut("GrdWrapTst", "5", "description", true);
    checkGridCellFullTextTooltip("GrdWrapTst", "5", "description", description);

    // The row grows to fit the text, and a row with short texts stays low
    checkGridRowFitsItsCells("GrdWrapTst", "5", "code");
    checkGridRowTaller("GrdWrapTst", "5", "18", "code");

    // Selection
    clickRowContents("GrdWrapTst", "WR-005");
    checkRowSelected("GrdWrapTst", "WR-005");

    // Inline editing of a row with taller cells
    editRow("GrdWrapTst", "WR-002");
    writeText("GrdWrapTst", "2", "code", "WR-X02");
    saveRow("GrdWrapTst");
    checkCellContents("GrdWrapTst", "2", "code", "WR-X02");
    checkGridRowFitsItsCells("GrdWrapTst", "2", "code");

    // Sorting
    sortGrid("GrdWrapTst", "code");
    sortGrid("GrdWrapTst", "code");
    checkGridCellLines("GrdWrapTst", "5", "description", 3);
    checkGridRowFitsItsCells("GrdWrapTst", "5", "code");
  }

  /**
   * Wrapping cells render time test: a page of 100 rows with wrapping columns renders in a time close to the one of the
   * same page without wrapping. The client-side sorts of each grid are timed in two rounds, one grid after the other and
   * the second round in the opposite order, so that neither grid has the advantage of the warm browser; the median of
   * all the samples of each grid is compared, so that one slow sample of a loaded machine does not decide the result
   */
  @Test
  void t099_gridWrappingRenderTime() {
    // Title
    setTestTitle("Grid wrapping render time");

    // Measure both grids
    List<Double> wrapping = new ArrayList<>();
    List<Double> singleLine = new ArrayList<>();
    for (int round = 0; round < 2; round++) {
      boolean wrappingFirst = round % 2 == 0;
      for (int turn = 0; turn < 2; turn++) {
        if (wrappingFirst == (turn == 0)) {
          wrapping.addAll(sortRenderTimes("matrix-test-wrap", "GrdWrapTst"));
        } else {
          singleLine.addAll(sortRenderTimes("matrix-test-nowrap", "GrdNoWrapTst"));
        }
      }
    }
    double wrappingMedian = median(wrapping);
    double singleLineMedian = median(singleLine);
    System.out.printf("GRID-WRAP-TIMING engine=%s rows=100 samples=%d: median wrapping=%.1fms singleLine=%.1fms ratio=%.3f; best wrapping=%.1fms singleLine=%.1fms%n",
      "angular", wrapping.size(), wrappingMedian, singleLineMedian, wrappingMedian / singleLineMedian, Collections.min(wrapping), Collections.min(singleLine));

    // Compare
    assertTrue(wrappingMedian <= singleLineMedian * WRAP_RENDER_TIME_LIMIT, "Rendering 100 rows with wrapping columns took " + wrappingMedian
      + " ms (median), more than " + Math.round(WRAP_RENDER_TIME_LIMIT * 100) + "% of the " + singleLineMedian + " ms of the same page without wrapping");
  }

  /**
   * Render times of the client-side sorts of a grid, once it is warm. A click on a header cycles the sort of the column
   * (ascending, descending, none) and the three renders cost differently, so each time is the sum of a whole cycle
   *
   * @param option Menu option of the screen
   * @param gridId Grid identifier
   * @return Times of the cycles, in milliseconds
   */
  private List<Double> sortRenderTimes(String option, String gridId) {
    openMatrixOption(option);
    checkRowContentsGrid(gridId, "WR-005");
    for (int i = 0; i < 6; i++) {
      measureGridSortRender(gridId, "code");
    }
    List<Double> times = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      double cycle = 0;
      for (int click = 0; click < 3; click++) {
        cycle += measureGridSortRender(gridId, "code");
      }
      times.add(cycle);
    }
    return times;
  }

  /**
   * Median of some times
   *
   * @param times Times
   * @return Median
   */
  private static double median(List<Double> times) {
    List<Double> sorted = new ArrayList<>(times);
    Collections.sort(sorted);
    return sorted.get(sorted.size() / 2);
  }

  /**
   * Text of the wrapping test screens: consecutive words of a list, as the test application generates them
   *
   * @param start Index of the first word
   * @param count Number of words
   * @return Text
   */
  private static String words(int start, int count) {
    String[] list = {"alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india", "juliet", "kilo", "lima"};
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < count; i++) {
      text.append(i == 0 ? "" : " ").append(list[(start + i) % list.length]);
    }
    return text.toString();
  }

  /**
   * Grid toolbar test: the user hides and reorders columns with the Columns dialog, group headers stay aligned and
   * columns never leave their group, the layout is saved for the user and the screen comes back with it, Reset goes back
   * to the definition of the screen, and Maximize fills the window
   */
  @Test
  void t115_gridToolbarColumnsAndMaximize() {
    // Title
    setTestTitle("Grid toolbar: columns dialog, saved layout and maximize");

    // Go to screen, with no layout saved by a previous run: the definition of the screen
    openMatrixOption("matrix-test-layout");
    openGridColumns("GrdLayoutTst");
    clickGridColumnsReset("GrdLayoutTst");
    closeGridColumns("GrdLayoutTst");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c01", "c02", "c03", "c04", "c05", "c06", "c07");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c03", "c06");

    // The dialog lists the 26 columns that the screen shows, and not the key that it hides
    openGridColumns("GrdLayoutTst");
    checkGridColumnsCounter("GrdLayoutTst", 26, 26);
    checkGridColumnsListed("GrdLayoutTst", "c01");
    checkGridColumnsNotListed("GrdLayoutTst", "key");

    // Hiding the first column of a group, and then one inside it, keeps the header over the columns that are left
    hideGridColumn("GrdLayoutTst", "c03");
    hideGridColumn("GrdLayoutTst", "c05");
    checkGridColumnsCounter("GrdLayoutTst", 24, 26);
    closeGridColumns("GrdLayoutTst");
    checkGridColumnNotShown("GrdLayoutTst", "c03");
    checkGridColumnNotShown("GrdLayoutTst", "c05");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c04", "c06");

    // A group without visible columns has no header
    openGridColumns("GrdLayoutTst");
    hideGridColumn("GrdLayoutTst", "c04");
    hideGridColumn("GrdLayoutTst", "c06");
    closeGridColumns("GrdLayoutTst");
    checkGridGroupHeaderNotShown("GrdLayoutTst", "Group A");
    openGridColumns("GrdLayoutTst");
    showGridColumn("GrdLayoutTst", "c04");
    closeGridColumns("GrdLayoutTst");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c04", "c04");

    // Columns move among their own siblings only: the ones of a group cannot leave it
    openGridColumns("GrdLayoutTst");
    checkGridColumnCannotMoveUp("GrdLayoutTst", "c03");
    checkGridColumnCannotMoveDown("GrdLayoutTst", "c06");
    moveGridColumnDown("GrdLayoutTst", "c01");
    checkGridColumnCannotMoveUp("GrdLayoutTst", "c02");
    closeGridColumns("GrdLayoutTst");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c02", "c01", "c04");

    // Save as default: the screen comes back with the layout of the user
    openGridColumns("GrdLayoutTst");
    // The step waits until the server stores the layout and Save closes the Columns, so the screen is opened again only
    // after that. It is silent
    clickGridColumnsSave("GrdLayoutTst");
    checkMessageMissing("success");
    openMatrixOption(MATRIX_PAGER_OPTION);
    openMatrixOption("matrix-test-layout");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c02", "c01", "c04", "c07");
    checkGridColumnNotShown("GrdLayoutTst", "c03");
    checkGridColumnNotShown("GrdLayoutTst", "c06");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c04", "c04");

    // Show the hidden columns again, one by one
    openGridColumns("GrdLayoutTst");
    checkGridColumnsCounter("GrdLayoutTst", 23, 26);
    showGridColumn("GrdLayoutTst", "c03");
    showGridColumn("GrdLayoutTst", "c05");
    showGridColumn("GrdLayoutTst", "c06");
    checkGridColumnsCounter("GrdLayoutTst", 26, 26);
    closeGridColumns("GrdLayoutTst");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c02", "c01", "c03", "c04", "c05", "c06");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c03", "c06");

    // Reset goes back to the definition of the screen and removes the layout that was saved
    openGridColumns("GrdLayoutTst");
    clickGridColumnsReset("GrdLayoutTst");
    closeGridColumns("GrdLayoutTst");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c01", "c02", "c03");
    openMatrixOption(MATRIX_PAGER_OPTION);
    openMatrixOption("matrix-test-layout");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c01", "c02", "c03", "c04", "c05", "c06", "c07");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c03", "c06");

    // Maximize fills the content area of the screen (the header stays visible) and restores the grid
    maximizeGrid("GrdLayoutTst");
    checkGridMaximized("GrdLayoutTst");
    restoreGrid("GrdLayoutTst");
    checkGridNotMaximized("GrdLayoutTst");
  }

  /**
   * Window maximize test: a window with a label and maximize="true" fills the content area of the screen (the header
   * of the application stays visible) and the Restore button brings it back
   */
  @Test
  void t117_windowMaximizeAndRestore() {
    // Title
    setTestTitle("Window: maximize and restore");

    // Go to screen
    gotoScreen("test", "screen-test");

    // Maximize fills the content area of the screen and restores the window
    String windowId = "WinGrd";
    maximizeWindow(windowId);
    checkWindowMaximized(windowId);
    restoreWindow(windowId);
    checkWindowNotMaximized(windowId);
  }

  /**
   * Tab maximize test: a tab with maximize="true" fills the content area of the screen (tab list and content, with the
   * header of the application visible) and the Restore button brings it back
   */
  @Test
  void t118_tabMaximizeAndRestore() {
    // Title
    setTestTitle("Tab: maximize and restore");

    // Go to screen
    gotoScreen("test", "chart", "queue-test");

    // Maximize fills the content area of the screen and restores the tab component
    String tabId = "QueTabSel";
    maximizeTab(tabId);
    checkTabMaximized(tabId);
    restoreTab(tabId);
    checkTabNotMaximized(tabId);
  }

  /**
   * Row detail test (#835): the toggle of a row opens its detail under the row with a visible and a hidden column of
   * the row, several rows stay open, the toggle does not select the row, the keyboard opens and closes it, and a reload
   * of the data (a sort on the server) closes every detail
   */
  @Test
  void t119_gridRowDetailOpensUnderItsRow() {
    // Title
    setTestTitle("Grid row detail: toggle, several rows, keyboard and reload");

    // Go to screen
    openMatrixOption("matrix-test-row-detail");
    checkRowContentsGrid(ROW_DETAIL_GRID, "adminflare");

    // The hidden column is not shown and no detail is open
    checkGridHeaderNotVisible(ROW_DETAIL_GRID, "code");
    checkNoGridRowDetailOpen(ROW_DETAIL_GRID);

    // The toggle opens the detail under its row with the name (visible column), the code and the active flag (hidden
    // columns), and keeps the line breaks of the template. The area has the style classes of the row detail. It does
    // not select the row
    clickGridRowDetailToggle(ROW_DETAIL_GRID, "12");
    checkRowDetailOpen("12", "Theme: adminflare\nCode: THM-12\nActive: Yes");
    checkGridRowDetailHasClass(ROW_DETAIL_GRID, "12", "grid-row-detail row-detail-card");
    // The style of the row detail reaches the page: the card shows its left accent border
    checkGridRowDetailComputedStyle(ROW_DETAIL_GRID, "12", "border-left-width", "4px");
    checkGridRowDetailUnderItsRow(ROW_DETAIL_GRID, "12");
    checkGridRowNotSelected(ROW_DETAIL_GRID, "12");

    // Several rows can be open at the same time
    clickGridRowDetailToggle(ROW_DETAIL_GRID, "15");
    checkRowDetailOpen("15", "Theme: asphalt\nCode: THM-15\nActive: Yes");
    checkRowDetailOpen("12", "Theme: adminflare\nCode: THM-12\nActive: Yes");

    // The toggle closes the detail of its row only
    clickGridRowDetailToggle(ROW_DETAIL_GRID, "15");
    checkRowDetailClosed("15");
    checkRowDetailOpen("12", "Theme: adminflare\nCode: THM-12\nActive: Yes");

    // The keyboard: Enter on the focused toggle opens the detail and Space closes it
    pressEnterOnGridRowDetailToggle(ROW_DETAIL_GRID, "10");
    checkRowDetailOpen("10", "Theme: clean\nCode: THM-10\nActive: Yes");
    pressSpaceOnGridRowDetailToggle(ROW_DETAIL_GRID, "10");
    checkRowDetailClosed("10");

    // A reload of the data (a sort on the server) closes every detail
    sortGrid(ROW_DETAIL_GRID, "name");
    checkRowContentsGrid(ROW_DETAIL_GRID, "adminflare");
    checkNoGridRowDetailOpen(ROW_DETAIL_GRID);
  }

  /**
   * Check that the detail of a row of the row detail test grid is shown with a text, and that its toggle says so, points
   * to it and offers to hide it
   *
   * @param rowId Row identifier
   * @param text  Text of the detail, with its line breaks
   */
  private void checkRowDetailOpen(String rowId, String text) {
    checkGridRowDetailOpen(ROW_DETAIL_GRID, rowId, text);
    checkGridRowDetailToggleLabel(ROW_DETAIL_GRID, rowId, "Hide row details");
  }

  /**
   * Check that the detail of a row of the row detail test grid is not shown, and that its toggle says so and offers to
   * show it
   *
   * @param rowId Row identifier
   */
  private void checkRowDetailClosed(String rowId) {
    checkGridRowDetailClosed(ROW_DETAIL_GRID, rowId);
    checkGridRowDetailToggleLabel(ROW_DETAIL_GRID, rowId, "Show row details");
  }

  /**
   * Row detail with components (#884): the detail of a country shows a nested grid with the customers of the country,
   * read from a hidden column of the parent row. Several rows keep their own nested grid, which sorts on its own, shows
   * its styled cells and runs its buttons. A reload of the parent closes every detail and removes the nested grids. The
   * second grid brings its nested grid with an include
   */
  @Test
  void t120_gridRowDetailNestedGrid() {
    // Title
    setTestTitle("Grid row detail with a nested grid: own rows, independent sort, buttons, styles, reload and include");

    // Go to screen
    openMatrixOption("matrix-test-row-detail-components");
    checkRowContentsGrid(ROW_DETAIL_COMPONENTS_GRID, "Spain");

    // The nested grid is not a screen component: there is none until a row opens
    checkGridHeaderNotVisible(ROW_DETAIL_COMPONENTS_GRID, "country");
    checkGridNotPresent(nestedGridId("1"));

    // Spain (1) opens its detail with a nested grid that shows only its two customers, in the order of the query
    clickGridRowDetailToggle(ROW_DETAIL_COMPONENTS_GRID, "1");
    checkGridRowDetailHasNestedGrid(ROW_DETAIL_COMPONENTS_GRID, "1", nestedGridId("1"));
    checkRowContentsGrid(nestedGridId("1"), "Customer2", "Customer3");
    checkGridColumnOrder(nestedGridId("1"), "customer", "Customer2", "Customer3");

    // The status cells keep the style that the query gives to them
    checkCellContents(nestedGridId("1"), "2", "status", "Regular");
    checkCellHasClass(nestedGridId("1"), "2", "status", "scale-seq-2");
    checkCellContents(nestedGridId("1"), "3", "status", "New");
    checkCellHasClass(nestedGridId("1"), "3", "status", "scale-pos-3");

    // The detail starts under its row, spans the grid and holds the whole nested grid, down to its last row
    checkRowDetailWithNestedGrid("1");

    // United Kingdom (6) opens its own nested grid with its only customer; the one of Spain keeps its rows
    clickGridRowDetailToggle(ROW_DETAIL_COMPONENTS_GRID, "6");
    checkGridRowDetailHasNestedGrid(ROW_DETAIL_COMPONENTS_GRID, "6", nestedGridId("6"));
    checkRowContentsGrid(nestedGridId("6"), "Customer1");
    checkGridColumnOrder(nestedGridId("6"), "customer", "Customer1");
    checkGridColumnOrder(nestedGridId("1"), "customer", "Customer2", "Customer3");
    checkRowDetailWithNestedGrid("6");

    // Sorting the nested grid of Spain on the server (descending, on the second click) keeps it on its own customers and
    // does not touch the nested grid of the United Kingdom
    sortGrid(nestedGridId("1"), "customer");
    sortGrid(nestedGridId("1"), "customer");
    checkGridColumnOrder(nestedGridId("1"), "customer", "Customer3", "Customer2");
    checkGridColumnOrder(nestedGridId("6"), "customer", "Customer1");

    // The button of a nested grid runs its actions
    clickButton("ButRowDetailCmpInfo-6");
    acceptConfirm();

    // A reload of the parent (a sort on the server) closes every detail and leaves no nested grid behind
    sortGrid(ROW_DETAIL_COMPONENTS_GRID, "name");
    checkRowContentsGrid(ROW_DETAIL_COMPONENTS_GRID, "Spain");
    checkNoGridRowDetailOpen(ROW_DETAIL_COMPONENTS_GRID);
    checkGridNotPresent(nestedGridId("1"));
    checkGridNotPresent(nestedGridId("6"));

    // The nested grid of an include works the same way
    clickGridRowDetailToggle(ROW_DETAIL_INCLUDE_GRID, "1");
    checkGridRowDetailHasNestedGrid(ROW_DETAIL_INCLUDE_GRID, "1", ROW_DETAIL_INCLUDED_GRID + "-1");
    checkRowContentsGrid(ROW_DETAIL_INCLUDED_GRID + "-1", "Customer2", "Customer3");
    checkGridColumnOrder(ROW_DETAIL_INCLUDED_GRID + "-1", "incCustomer", "Customer2", "Customer3");
  }

  /**
   * Grid Columns popover test: a grid without columns-panel opens its Columns in a popover anchored to the Columns
   * button, where the changes apply to the grid at once. Cancel undoes them and closes it, the close button (x) keeps
   * them
   */
  @Test
  void t123_gridColumnsPopoverCancelAndClose() {
    // Title
    setTestTitle("Grid toolbar: columns popover, cancel and close");

    // Go to screen
    openMatrixOption("matrix-test-layout");

    // The Columns open as a popover (the default) and hiding a column updates the grid while it is open
    openGridColumns("GrdLayoutTst");
    checkGridColumnsPanel("GrdLayoutTst", "popover");
    hideGridColumn("GrdLayoutTst", "c01");
    checkGridColumnNotShown("GrdLayoutTst", "c01");

    // Cancel shows the column again and closes the popover
    clickGridColumnsCancel("GrdLayoutTst");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c01", "c02", "c03");

    // The close button keeps the change
    openGridColumns("GrdLayoutTst");
    hideGridColumn("GrdLayoutTst", "c01");
    closeGridColumns("GrdLayoutTst");
    checkGridColumnNotShown("GrdLayoutTst", "c01");

    // Show the column again so that the grid is left as the screen defines it
    openGridColumns("GrdLayoutTst");
    showGridColumn("GrdLayoutTst", "c01");
    closeGridColumns("GrdLayoutTst");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c01", "c02", "c03", "c04", "c05", "c06", "c07");
  }

  /**
   * Identifier of the nested grid of the detail of a row of the row detail with components test grid
   *
   * @param rowId Row identifier
   * @return Grid identifier
   */
  private static String nestedGridId(String rowId) {
    return ROW_DETAIL_NESTED_GRID + "-" + rowId;
  }

  /**
   * Check that the detail of a row of the row detail with components test grid starts right under its row, spans the
   * visible part of the grid and holds the whole nested grid of the row
   *
   * @param rowId Row identifier
   */
  private void checkRowDetailWithNestedGrid(String rowId) {
    checkGridRowDetailUnderItsRow(ROW_DETAIL_COMPONENTS_GRID, rowId);
    checkGridRowDetailAcrossGrid(ROW_DETAIL_COMPONENTS_GRID, rowId);
    checkGridRowDetailHoldsNestedGrid(ROW_DETAIL_COMPONENTS_GRID, rowId, nestedGridId(rowId));
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

  /**
   * Card test (#899): the metric cards show their value with its unit, their trend delta with its direction and tone
   * (the tone follows the good trend of the card, so a falling error rate is good news), and their progress towards the
   * target. The chart and the grid of the other cards are drawn in their body. The reload button filters the card filled
   * from a query, which keeps its value and its delta
   */
  @Test
  void t121_cardMetricTrendProgressAndBody() {
    // Title
    setTestTitle("Card: metric, trend, progress and body");

    // Go to screen
    gotoScreen("test", "layout", "card-test");

    // Metric filled from a query: the number of themes, rising (good news)
    checkCardValue("CrdTstThemes", "18");
    checkCardDelta("CrdTstThemes", "+12.5%", "up", "success");

    // Static metric whose good trend is down: a falling error rate is good news, signed with U+2212
    checkCardValue("CrdTstErrors", "0.42 %");
    checkCardDelta("CrdTstErrors", "\u22120.18 pt", "down", "success");

    // Progress towards the target: 412,600 of 500,000
    checkCardValue("CrdTstTarget", "412,600 \u20ac");
    checkCardDelta("CrdTstTarget", "+6.1%", "up", "success");
    checkCardProgress("CrdTstTarget", 83);

    // A gauge tells its progress like the bar does, and a sparkline from a query field shows its value
    checkCardValue("CrdTstDisk", "720GB");
    checkCardProgress("CrdTstDisk", 72);
    checkCardValue("CrdTstOrders", "1,284");
    checkCardDelta("CrdTstOrders", "+4.2%", "up", "success");

    // A chart and a grid in the body of their cards
    checkCardBodyComponent("CrdTstChart", "ChrCrdTst");
    checkChartVisible("ChrCrdTst");
    checkCardBodyComponent("CrdTstGrid", "GrdCrdTst");
    checkRowContentsGrid("GrdCrdTst", "awedb2");

    // The reload button filters the card filled from a query, which keeps its value and its delta
    clickButton("ButCrdReload");
    checkCardValue("CrdTstThemes", "18");
    checkCardDelta("CrdTstThemes", "+12.5%", "up", "success");
  }

  /**
   * Card status chip (#836): a static chip at the top right of a card, and a status card filled from a service that
   * moves to the next status on every call, so each reload shows another chip (text and tone)
   */
  @Test
  void t122_cardStatusChip() {
    // Title
    setTestTitle("Card: status chip");

    // Go to screen
    gotoScreen("test", "layout", "card-test");

    // Static chip from the XML of the card
    checkCardChip("CrdTstTarget", "On track", "success");

    // The status card shows a chip, and every reload changes it
    checkCardChipChangesOnClick("CrdTstStatus", "ButCrdReload");
    checkCardChipChangesOnClick("CrdTstStatus", "ButCrdReload");
  }

  /**
   * Secondary line test: a column with secondary-field shows the login under the user, and a row without login shows only
   * the main line
   */
  @Test
  void t124_gridCellSecondaryLine() {
    // Title
    setTestTitle("Grid cells: secondary line");

    // Go to screen
    openMatrixOption("matrix-test-cells");
    checkRowContentsGrid("GrdCellTst", "Javier García");

    // The secondary value under the main one, and only the main line without it
    checkGridCellSecondaryLine("GrdCellTst", "1", "user", "Javier García", "jgarcia · j.garcia@almis.example");
    checkGridCellSecondaryLine("GrdCellTst", "2", "user", "Laura Pérez", "lperez · l.perez@almis.example");
    checkGridCellSecondaryLine("GrdCellTst", "4", "user", "Pablo Díaz", null);
  }

  /**
   * Status test: a column with status-field shows a dot of the tone and the text, and an unknown tone is neutral
   */
  @Test
  void t125_gridCellStatus() {
    // Title
    setTestTitle("Grid cells: status");

    // Go to screen
    openMatrixOption("matrix-test-cells");
    checkRowContentsGrid("GrdCellTst", "Javier García");

    // Every tone, and the unknown one as neutral
    checkGridCellStatus("GrdCellTst", "1", "status", "Locked", "danger");
    checkGridCellStatus("GrdCellTst", "2", "status", "Active", "success");
    checkGridCellStatus("GrdCellTst", "3", "status", "Pending activation", "info");
    checkGridCellStatus("GrdCellTst", "4", "status", "Inactive", "neutral");
    checkGridCellStatus("GrdCellTst", "5", "status", "Under review", "neutral");
    checkGridCellStatus("GrdCellTst", "6", "status", "Expiring", "warning");
  }

  /**
   * Chips test: a column with chips shows each item as a chip, up to max-chips and then a "+n" chip that lists the hidden
   * ones; empty items are dropped
   */
  @Test
  void t126_gridCellChips() {
    // Title
    setTestTitle("Grid cells: chips");

    // Go to screen
    openMatrixOption("matrix-test-cells");
    checkRowContentsGrid("GrdCellTst", "Javier García");

    // Two chips and "+2" with the hidden ones
    checkGridCellChips("GrdCellTst", "1", "teams", List.of("Sales", "Finance"), List.of("Logistics", "Support"));

    // The empty item is dropped
    checkGridCellChips("GrdCellTst", "2", "teams", List.of("A", "B"), List.of());

    // As many chips as the maximum, without "+n"
    checkGridCellChips("GrdCellTst", "5", "teams", List.of("Finance", "Support"), List.of());
    checkGridCellChips("GrdCellTst", "4", "teams", List.of("Sales"), List.of());
  }

  /**
   * Text test: the values of the cells are shown as text, never as HTML
   */
  @Test
  void t127_gridCellValuesAreText() {
    // Title
    setTestTitle("Grid cells: values are text");

    // Go to screen
    openMatrixOption("matrix-test-cells");
    checkRowContentsGrid("GrdCellTst", "Javier García");

    // The HTML of the value shows literally
    checkGridCellChips("GrdCellTst", "3", "teams", List.of("<b>bold</b>"), List.of());
    checkGridCellsHaveNoActiveContent("GrdCellTst");
  }

  /**
   * Row height test: every row grows to fit its secondary line, status and chips (the height of a row is set by its tallest
   * cell, so rows are not compared with each other)
   */
  @Test
  void t128_gridCellRowsGrowToFit() {
    // Title
    setTestTitle("Grid cells: rows grow to fit");

    // Go to screen
    openMatrixOption("matrix-test-cells");
    checkRowContentsGrid("GrdCellTst", "Javier García");

    // Each row fits its cells: the secondary line of the user and the chips that wrap
    checkGridRowFitsItsCells("GrdCellTst", "1", "user");
    checkGridRowFitsItsCells("GrdCellTst", "1", "teams");
    checkGridRowFitsItsCells("GrdCellTst", "4", "user");

    // The grid grows with its rows: the last one is not cut
    checkGridRowNotCut("GrdCellTst", "6", "user");
  }

  /**
   * Open the row grouping screen with its initial state (the client keeps a screen that is opened again from the menu,
   * with the grouping, expanded groups and sort that a previous test left)
   */
  private void openGroupingScreen() {
    openMatrixOption("matrix-test-cells");
    openMatrixOption("matrix-test-groups");
  }

  /**
   * Row grouping test: a grid grouped by region, country and city shows a group row per group of each level with its
   * value and its number of rows, ordered by value; collapsed groups hide the groups under them
   */
  @Test
  void t129_gridGroupsThreeLevels() {
    // Title
    setTestTitle("Grid row grouping: three levels");

    // Go to screen: every group starts expanded
    openGroupingScreen();
    checkGridGroupLabel("GrdGrpTst", "America", "America", 13);
    checkGridGroupExpanded("GrdGrpTst", "America");
    checkGridGroupLabel("GrdGrpTst", "America/Canada", "Canada", 4);
    checkGridGroupLabel("GrdGrpTst", "America/Canada/Toronto", "Toronto", 2);

    // The regions, by value
    collapseAllGridGroups("GrdGrpTst");
    checkGridGroupRowCount("GrdGrpTst", 1, 2);
    checkGridGroupRowCount("GrdGrpTst", 2, 0);
    checkGridGroupOrder("GrdGrpTst", 1, "America", "Europe");
    checkGridGroupLabel("GrdGrpTst", "Europe", "Europe", 11);

    // The countries of a region and the cities of a country
    toggleGridGroup("GrdGrpTst", "Europe");
    checkGridGroupOrder("GrdGrpTst", 2, "Europe/France", "Europe/Spain");
    checkGridGroupLabel("GrdGrpTst", "Europe/France", "France", 5);
    checkGridGroupLabel("GrdGrpTst", "Europe/Spain", "Spain", 6);
    checkGridGroupRowCount("GrdGrpTst", 3, 0);
    toggleGridGroup("GrdGrpTst", "Europe/Spain");
    checkGridGroupOrder("GrdGrpTst", 3, "Europe/Spain/Barcelona", "Europe/Spain/Madrid");
    checkGridGroupLabel("GrdGrpTst", "Europe/Spain/Madrid", "Madrid", 3);

    // After Collapse all, the groups shown by expanding their parent are collapsed too
    checkGridGroupCollapsed("GrdGrpTst", "Europe/Spain/Madrid");
    toggleGridGroup("GrdGrpTst", "Europe/Spain/Madrid");
    checkGridGroupExpanded("GrdGrpTst", "Europe/Spain/Madrid");
  }

  /**
   * Subtotals test: the aggregates of the groups of a level add up to their parent group, and the groups of the first
   * level to the grand total of the footer; each expanded group ends with its subtotal row
   */
  @Test
  void t130_gridGroupSubtotalsAddUp() {
    // Title
    setTestTitle("Grid row grouping: subtotals add up to the grand total");

    // Go to screen
    openGroupingScreen();
    checkGridGroupLabel("GrdGrpTst", "America", "America", 13);
    collapseAllGridGroups("GrdGrpTst");

    // Regions: 850 + 1.600 = 2.450 (the grand total), and the units
    checkGridGroupCell("GrdGrpTst", "Europe", "sales", "850");
    checkGridGroupCell("GrdGrpTst", "America", "sales", "1.600");
    checkGridFooterTotal("GrdGrpTst", "sales", "2.450");
    checkGridGroupCell("GrdGrpTst", "Europe", "units", "85");
    checkGridGroupCell("GrdGrpTst", "America", "units", "160");
    checkGridFooterTotal("GrdGrpTst", "units", "245");

    // Countries: 500 + 350 = 850 and 800 + 550 + 250 = 1.600
    toggleGridGroup("GrdGrpTst", "Europe");
    toggleGridGroup("GrdGrpTst", "America");
    checkGridGroupCell("GrdGrpTst", "Europe/Spain", "sales", "500");
    checkGridGroupCell("GrdGrpTst", "Europe/France", "sales", "350");
    checkGridGroupCell("GrdGrpTst", "America/Canada", "sales", "800");
    checkGridGroupCell("GrdGrpTst", "America/Mexico", "sales", "550");
    checkGridGroupCell("GrdGrpTst", "America/USA", "sales", "250");

    // Cities: 300 + 200 = 500, and the average price of the group
    toggleGridGroup("GrdGrpTst", "Europe/Spain");
    checkGridGroupCell("GrdGrpTst", "Europe/Spain/Madrid", "sales", "300");
    checkGridGroupCell("GrdGrpTst", "Europe/Spain/Barcelona", "sales", "200");
    checkGridGroupCell("GrdGrpTst", "Europe/Spain/Madrid", "price", "20");
    checkGridGroupCell("GrdGrpTst", "Europe/Spain", "price", "35");

    // The subtotal rows of the expanded groups repeat their aggregates
    checkGridSubtotalCell("GrdGrpTst", "Europe/Spain", "sales", "500");
    checkGridSubtotalCell("GrdGrpTst", "Europe/Spain", "region", "Subtotal Spain");
    checkGridSubtotalCell("GrdGrpTst", "Europe", "sales", "850");
  }

  /**
   * Expand and collapse test: a group collapses and expands with its toggle, and every group with Collapse all and
   * Expand all, without calling the server
   */
  @Test
  void t131_gridGroupExpandAndCollapse() {
    // Title
    setTestTitle("Grid row grouping: expand and collapse");

    // Go to screen
    openGroupingScreen();
    checkGridGroupExpanded("GrdGrpTst", "America");
    watchServerCalls();

    // A group hides its groups, rows and subtotal, and shows them again
    toggleGridGroup("GrdGrpTst", "America");
    checkGridGroupCollapsed("GrdGrpTst", "America");
    checkGridGroupExpanded("GrdGrpTst", "Europe");
    toggleGridGroup("GrdGrpTst", "America");
    checkGridGroupExpanded("GrdGrpTst", "America");

    // Every group of every level
    collapseAllGridGroups("GrdGrpTst");
    checkGridGroupCollapsed("GrdGrpTst", "America");
    checkGridGroupCollapsed("GrdGrpTst", "Europe");
    expandAllGridGroups("GrdGrpTst");
    checkGridGroupExpanded("GrdGrpTst", "America");
    checkGridGroupExpanded("GrdGrpTst", "America/Canada");
    checkGridGroupExpanded("GrdGrpTst", "America/Canada/Toronto");
    checkNoServerCalls();
  }

  /**
   * Group by test: the Group by panel changes the grouping levels, or removes them, without calling the server
   */
  @Test
  void t132_gridGroupBySelector() {
    // Title
    setTestTitle("Grid row grouping: group by selector");

    // Go to screen
    openGroupingScreen();
    checkGridGroupLabel("GrdGrpTst", "America", "America", 13);
    watchServerCalls();

    // One level: the countries
    groupGridBy("GrdGrpTst", "country");
    collapseAllGridGroups("GrdGrpTst");
    checkGridGroupOrder("GrdGrpTst", 1, "Canada", "France", "Mexico", "Spain", "USA");
    checkGridGroupRowCount("GrdGrpTst", 2, 0);
    checkGridGroupLabel("GrdGrpTst", "Spain", "Spain", 6);
    checkGridGroupCell("GrdGrpTst", "Spain", "sales", "500");

    // No grouping: the data rows
    groupGridBy("GrdGrpTst");
    checkGridNotGrouped("GrdGrpTst");
    checkRowContentsGrid("GrdGrpTst", "Store 1");

    // Two levels again, in the order they are chosen
    groupGridBy("GrdGrpTst", "city", "region");
    collapseAllGridGroups("GrdGrpTst");
    checkGridGroupRowCount("GrdGrpTst", 1, 10);
    toggleGridGroup("GrdGrpTst", "Barcelona");
    checkGridGroupOrder("GrdGrpTst", 2, "Barcelona/Europe");
    checkNoServerCalls();
  }

  /**
   * Sort test: sorting by a column with summary type orders the groups of every level by their aggregate
   */
  @Test
  void t133_gridGroupSortByAggregate() {
    // Title
    setTestTitle("Grid row grouping: sort the groups by their aggregate");

    // Go to screen
    openGroupingScreen();
    checkGridGroupLabel("GrdGrpTst", "America", "America", 13);
    collapseAllGridGroups("GrdGrpTst");
    checkGridGroupOrder("GrdGrpTst", 1, "America", "Europe");

    // By the sales of the groups, ascending: 850 before 1.600, and 250, 550, 800 in America
    sortGrid("GrdGrpTst", "sales");
    checkGridGroupOrder("GrdGrpTst", 1, "Europe", "America");
    toggleGridGroup("GrdGrpTst", "America");
    checkGridGroupOrder("GrdGrpTst", 2, "America/USA", "America/Mexico", "America/Canada");
  }

  /**
   * Pagination disabled test: a grid with pagination disabled and without buttons has no paginator bar under its rows
   * (#908)
   */
  @Test
  void t134_gridWithPaginationDisabledHasNoPaginator() {
    // Title
    setTestTitle("Grid with pagination disabled: no paginator");

    // Go to screen
    openMatrixOption(MATRIX_PAGER_OPTION);
    checkRowContentsGrid("GrdNoPagerTst", "awedb1");

    // The paged grid keeps its paginator, the grid with pagination disabled has none
    checkGridPaginationPresent("GrdPagerTst");
    checkGridPaginationNotPresent("GrdNoPagerTst");
  }
}
