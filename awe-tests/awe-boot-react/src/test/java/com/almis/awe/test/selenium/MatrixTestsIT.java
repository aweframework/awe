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
@Tag("CRUDCriteriaMatrixIT")
class MatrixTestsIT extends AbstractSessionTests {

  private static final String BUTTON_DEFERRED_DIALOG = "ButDefDialog";
  private static final String DEFERRED_SELECT = "DefSelect";
  private static final String BUTTON_DEFERRED_SAVE = "ButDefSave";
  private static final String MATRIX_PAGER_OPTION = "matrix-test-pager";

  /**
   * Time that a page with wrapping columns may take to render, as a multiple of the time of the same page without wrapping.
   * The React grid lays out every row, and the wrapped text of the taller rows costs about 20% more in this harness (median
   * of the medians 1.22, from 1.04 to 1.52 over 3 runs). The limit leaves room for that and for the noise of a shared
   * machine, and still fails for a render that does several times the work (a layout per cell measured on every render cost
   * a median of 1.37, up to 1.55)
   */
  private static final double WRAP_RENDER_TIME_LIMIT = 1.35;

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

    // Go to the screen from another one, so it is loaded again: the client does not reload the current screen from its
    // own menu option, and the previous test leaves a row of GrdSta selected, which hides GrdMus
    openMatrixOption(MATRIX_PAGER_OPTION);
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
   * Grid test: move the selected row of an editable grid to the top, up, down and to the bottom
   */
  @Test
  void t062_gridTestMoveRows() {
    // Title
    setTestTitle("Grid test: move rows");

    // Go to screen
    gotoScreen("test", "matrix", "matrix-test-move-rows");

    // The rows come in the order of the query
    checkGridColumnOrder("GrdMoveRows", "label", "Step one", "Step two", "Step three", "Step four", "Step five");

    // Move the third row to the top
    clickRowContents("GrdMoveRows", "Step three");
    clickButton("ButMoveRowTop");
    checkGridColumnOrder("GrdMoveRows", "label", "Step three", "Step one", "Step two", "Step four", "Step five");

    // Move the fourth row one position up and down: the row stays selected
    clickRowContents("GrdMoveRows", "Step four");
    clickButton("ButMoveRowUp");
    checkGridColumnOrder("GrdMoveRows", "label", "Step three", "Step one", "Step four", "Step two", "Step five");
    clickButton("ButMoveRowDown");
    checkGridColumnOrder("GrdMoveRows", "label", "Step three", "Step one", "Step two", "Step four", "Step five");

    // Move it to the bottom
    clickButton("ButMoveRowBottom");
    checkGridColumnOrder("GrdMoveRows", "label", "Step three", "Step one", "Step two", "Step five", "Step four");
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
    openMatrixOption(MATRIX_PAGER_OPTION);

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
    clickTab("TabDeferred", "Deferred");
    checkRowContentsGrid("GrdTabRows", "Two");
    clickTab("TabDeferred", "Loaded with the screen");
    checkDeferredCalls("TabSecondSelect", "1");
    checkDeferredCalls("TabSecondRows", "1");

    // Going back to it does not load it again
    clickTab("TabDeferred", "Deferred");
    checkRowContentsGrid("GrdTabRows", "Two");
    clickTab("TabDeferred", "Loaded with the screen");
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

    // Now the empty required criterion blocks the validation. The React client unmounts a closed dialog, so open it again
    // to wait for the validation error of its criterion, then check nothing was saved
    clickButton(BUTTON_DEFERRED_SAVE);
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkValidationErrorVisible();
    checkCriterionContents("DefSaved", "");

    // Close the dialog, so it does not cover the menu for the next tests
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");
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

    // The cell that the column width cuts shows its whole value as tooltip, and so does its header
    checkGridCellTitle("GrdWrapTst", "5", "notes", words(5 * 3, 8));

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
      "react", wrapping.size(), wrappingMedian, singleLineMedian, wrappingMedian / singleLineMedian, Collections.min(wrapping), Collections.min(singleLine));

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
    clickGridColumnsSave("GrdLayoutTst");
    // The layout is stored when the server answers: the screen is opened again only after that
    checkMessageTitle("success", "Operation successful");
    closeGridColumns("GrdLayoutTst");
    openMatrixOption(MATRIX_PAGER_OPTION);
    openMatrixOption("matrix-test-layout");
    checkGridColumnsLeftToRight("GrdLayoutTst", "c02", "c01", "c04", "c07");
    checkGridColumnNotShown("GrdLayoutTst", "c03");
    checkGridColumnNotShown("GrdLayoutTst", "c06");
    checkGridGroupHeaderOver("GrdLayoutTst", "Group A", "c04", "c04");

    // Show all
    openGridColumns("GrdLayoutTst");
    checkGridColumnsCounter("GrdLayoutTst", 23, 26);
    clickGridColumnsShowAll("GrdLayoutTst");
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
