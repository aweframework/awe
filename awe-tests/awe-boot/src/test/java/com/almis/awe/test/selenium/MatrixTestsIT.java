package com.almis.awe.test.selenium;

import com.almis.awe.testing.annotations.Quarantine;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * Matrix of the test screens, one flow per feature: selection and context menu, multi-operation, editing, tree,
 * paging, cell rendering, themes, dialogs, side panel and popover, deferred load, toolbar and maximize, row detail,
 * cards and row grouping. Every test opens its screen, so each one can run on its own. The rules that the Jest tests
 * of the client prove (sparkline, wrapping, empty state, window and tab maximize, row detail template, card formats,
 * group subtotals and sort by aggregate) are not checked again here.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class MatrixTestsIT extends AbstractSessionTests {

  private static final String BUTTON_DEFERRED_DIALOG = "ButDefDialog";
  private static final String DEFERRED_SELECT = "DefSelect";
  private static final String BUTTON_DEFERRED_SAVE = "ButDefSave";
  private static final String MATRIX_PAGER_OPTION = "matrix-test-pager";
  private static final String ROW_DETAIL_COMPONENTS_GRID = "GrdRowDetailCmp";
  private static final String ROW_DETAIL_NESTED_GRID = "GrdRowDetailCmpCus";
  private static final String ROW_DETAIL_INCLUDE_GRID = "GrdRowDetailCmpInc";
  private static final String ROW_DETAIL_INCLUDED_GRID = "GrdRowDetailCmpIncCus";
  private static final String MOVE_ROWS_GRID = "GrdMoveRows";
  private static final String MOVE_ROWS_COLUMN = "label";
  private static final String LAYOUT_GRID = "GrdLayoutTst";
  private static final String CELLS_GRID = "GrdCellTst";
  private static final String GROUPS_GRID = "GrdGrpTst";
  private static final String STEP_ONE = "Step one";
  private static final String STEP_TWO = "Step two";
  private static final String STEP_THREE = "Step three";
  private static final String STEP_FOUR = "Step four";
  private static final String STEP_FIVE = "Step five";

  MatrixTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Open the matrix test screen
   */
  private void openMatrixScreen() {
    // Go to screen
    gotoScreen("test", "matrix", "matrix-test");

    // Wait for button
    waitForButton("ButPrn");
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
   * Selection and context menu: the multiselect grid keeps several rows selected, a row selected in the base grid hides
   * the multiselect grid, and the context menu of the base grid shows the options of the row it opens on
   */
  @Test
  void t010_gridSelectionAndContextMenu() {
    // Title
    setTestTitle("Grid: selection and context menu");

    // Go to screen
    openMatrixScreen();

    // Multiselect grid: several rows are selected
    clickTab("TabSelMat", "ENUM_MATRIX_MULTISELECT");
    clickRowContents("GrdMus", "AWE DB 2");
    clickRowContents("GrdMus", "AWE DB 1");
    clickRowContents("GrdMus", "AWE DB 3");
    checkRowContentsGrid("GrdMus", "awedb2");

    // A row selected in the base grid hides the multiselect grid
    clickTab("TabSelMat", "ENUM_MATRIX_STATIC");
    clickRowContents("GrdSta", "awedb1");
    waitForButton("ButPrn");
    clickTab("TabSelMat", "ENUM_MATRIX_MULTISELECT");
    checkGridPresent("GrdMus");
    checkGridNotVisible("GrdMus");

    // The context menu of the base grid shows the options of its row, and closes
    clickTab("TabSelMat", "ENUM_MATRIX_STATIC");
    contextMenuRowContents("GrdSta", "awedb1");
    waitForContextButton("CtxGrdStaAdd");
    closeContextMenu();
    checkContextMenuNotVisible();
    contextMenuRowContents("GrdSta", "awedb2");
    waitForContextButton("CtxGrdStaDel");
    closeContextMenu();
    checkContextMenuNotVisible();

    // The grid keeps working after the menu
    clickGridViewport("GrdSta");
    clickRowContents("GrdSta", "awedb2");
  }

  /**
   * Move rows: move the selected row of an editable grid to the top, up, down and to the bottom, and drag the rows by
   * their grip (grids with move-rows)
   */
  @Test
  void t015_gridMoveRows() {
    // Title
    setTestTitle("Grid: move rows");

    // Go to screen
    openMatrixOption("matrix-test-move-rows");

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
   * Multi-operation grid: a new row, an edited row and a row deleted from the context menu that a row button opens show
   * the icon of their operation
   */
  @Test
  void t020_gridMultiOperation() {
    // Title
    setTestTitle("Grid: multi-operation");

    // Go to screen
    openMatrixScreen();
    clickTab("TabSelMat", "ENUM_MATRIX_MULTIOPTION");

    // New row
    clickButton("ButGrdMuoAdd");
    checkGridIconVisible("GrdMuo", "RowIco", "plus");
    saveRow("GrdMuo");
    checkGridIconVisible("GrdMuo", "RowIco", "plus");

    // Edited row
    editRow("GrdMuo", "1", "Des2");
    writeText("GrdMuo", "Des2", "asdasda");
    saveRow("GrdMuo");
    checkGridIconVisible("GrdMuo", "RowIco", "edit");

    // A left click on the button of a row selects the row and opens the menu of the grid under it, with the options
    // that depend on the selected row, and the option runs its action over that row
    clickGridCellButton("GrdMuo", "3", "CtxBtn");
    waitForContextButton("CtxGrdMuoAddSel");
    clickContextButton("CtxGrdMuoDel");
    checkGridIconVisible("GrdMuo", "RowIco", "trash");
  }

  /**
   * Radio columns: a radio column and a button-radio column show the options of their list in every cell, one choice
   * per cell, and the server receives the choice of each row
   */
  @Test
  void t025_gridRadioColumnsChooseOneOptionPerCell() {
    // Title
    setTestTitle("Grid radio columns: one choice per cell");

    // Go to screen
    openMatrixOption("matrix-test-radio");

    // The query chooses the option B in one row and nothing in the others. A grid without the grid-no-header class
    // shows its header
    checkRowContentsGrid("GrdRadioTst", "adminflare");
    checkGridHeaderVisible("GrdRadioTst", "name");
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
   * Editable grid: dates picked and typed in the inline editor are saved, and a row added from the context menu is saved
   */
  @Test
  void t030_gridEditable() {
    // Title
    setTestTitle("Grid: editable");

    // Go to screen
    openMatrixScreen();
    clickTab("TabSelMat", "ENUM_MATRIX_EDITABLE");

    // Edit row: editRow waits for the row of the grid of the tab to be shown with the grid loader hidden, so the click
    // never reaches the grid before its data. Pick a date in the datepicker
    editRow("GrdEdi", "adminflare");
    clickDate("GrdEdi", "FilDat");
    clickEnabledDatepickerDay();
    String date = getText("GrdEdi", "FilDat");
    saveRow("GrdEdi");
    checkCellContents("GrdEdi", "2", "Dat", date);

    // Type a date in another row
    editRow("GrdEdi", "asphalt");
    editRow("GrdEdi", "clean");
    selectDate("GrdEdi", "Dat", "23/10/1978");
    date = getText("GrdEdi", "Dat");
    saveRow("GrdEdi");
    checkCellContents("GrdEdi", "2", "FilDat", date);

    // Add a row from the context menu
    contextMenuRowContents("GrdEdi", "asphalt");
    clickContextButton("CtxGrdEdiAddSel", "CtxGrdEdiAddUpp");
    saveRow("GrdEdi");
    checkRowContentsGrid("GrdEdi", "-123.456,10 $");
    checkCellContents("GrdEdi", "1", "Txt", "adminflare");
    checkCellContents("GrdEdi", "2", "Txt", "asphalt");
  }

  /**
   * Paginator: the range of rows shown and the total, and a grid with pagination disabled and without buttons has no
   * paginator bar under its rows (#908). The wide pager of the AngularJS client has no hook for its page links, so the
   * change of page is checked in the React client only
   */
  @Test
  void t035_gridPaginator() {
    // Title
    setTestTitle("Grid paginator: visible range and total, and no paginator when disabled");

    // Go to screen
    openMatrixOption(MATRIX_PAGER_OPTION);

    // First page: five rows of eighteen
    checkRowContentsGrid("GrdPagerTst", "adminflare");
    checkGridPaginationInfo("View 1 - 5 of 18");

    // The grid has rows, so it does not show its empty state
    checkGridEmptyStateNotVisible();

    // The paged grid keeps its paginator, the grid with pagination disabled has none
    checkRowContentsGrid("GrdNoPagerTst", "awedb1");
    checkGridPaginationPresent("GrdPagerTst");
    checkGridPaginationNotPresent("GrdNoPagerTst");
  }

  /**
   * Tree grids: the editable tree and the editable loading tree (which loads its branches when they open) open and close
   * their branches, add a child and delete a branch from the context menu. The read-only tree and loading tree run the
   * same component with the same branch steps, so they are not opened again
   */
  @Test
  void t040_treeGrids() {
    // Title
    setTestTitle("Tree grids: branches, add a child and delete a branch");

    // Go to screen
    openMatrixScreen();

    // Editable tree grid: add a child, move the edition from its parent to it and save it, then delete a branch
    manageTreeGrid("ENUM_MATRIX_TREEGRID_EDITABLE", "TreGrdEdi");
    contextMenu("TreGrdEdi", "ProGeneral-ModBase", "TreGrdEdi_Nam");
    clickContextButton("CtxTreGrdEdiAddSel", "CtxTreGrdEdiAddChl");
    checkTreeRowVisible("TreGrdEdi", "new-row-0");
    editRow("TreGrdEdi", "ProGeneral-ModBase", "TreGrdEdi_Nam");
    editRow("TreGrdEdi", "new-row-0", "TreGrdEdi_Nam");
    saveRow("TreGrdEdi");
    clickTreeButton("TreGrdEdi", "ProOperator");
    contextMenu("TreGrdEdi", "ProOperator", "TreGrdEdi_Nam");
    clickContextButton("CtxTreGrdEdiDel");
    checkTreeRowNotVisible("TreGrdEdi", "ProOperator");

    // Editable loading tree grid: the branches load when they open; add a child, then delete a branch
    manageLoadingTreeGrid("ENUM_MATRIX_TREEGRID_EDITABLE_LOADNODE", "TreGrdLoaEdi");
    contextMenu("TreGrdLoaEdi", "ProGeneral-ModBase", "TreGrdLoaEdi_Nam");
    clickContextButton("CtxTreGrdLoaEdiAddSel", "CtxTreGrdLoaEdiAddChl");
    checkTreeRowVisible("TreGrdLoaEdi", "new-row-0");
    waitForLoadingGrid();
    saveRow("TreGrdLoaEdi");
    clickTreeButton("TreGrdLoaEdi", "ProOperator");
    contextMenu("TreGrdLoaEdi", "ProOperator", "TreGrdLoaEdi_Nam");
    clickContextButton("CtxTreGrdLoaEdiDel");
    checkTreeRowDeleted("TreGrdLoaEdi", "ProOperator");
  }

  /**
   * Cell rendering: a column with secondary-field shows a second line, a column with status-field a dot of the tone, a
   * column with chips each item as a chip up to max-chips and a "+n" chip; the values are text, never HTML, and every
   * row grows to fit its cells
   */
  @Test
  void t050_gridCellRendering() {
    // Title
    setTestTitle("Grid cells: secondary line, status, chips, text and row height");

    // Go to screen
    openMatrixOption("matrix-test-cells");
    checkRowContentsGrid(CELLS_GRID, "Javier García");

    // The secondary value under the main one, and only the main line without it
    checkGridCellSecondaryLine(CELLS_GRID, "1", "user", "Javier García", "jgarcia · j.garcia@almis.example");
    checkGridCellSecondaryLine(CELLS_GRID, "2", "user", "Laura Pérez", "lperez · l.perez@almis.example");
    checkGridCellSecondaryLine(CELLS_GRID, "4", "user", "Pablo Díaz", null);

    // Every tone, and the unknown one as neutral
    checkGridCellStatus(CELLS_GRID, "1", "status", "Locked", "danger");
    checkGridCellStatus(CELLS_GRID, "2", "status", "Active", "success");
    checkGridCellStatus(CELLS_GRID, "3", "status", "Pending activation", "info");
    checkGridCellStatus(CELLS_GRID, "4", "status", "Inactive", "neutral");
    checkGridCellStatus(CELLS_GRID, "5", "status", "Under review", "neutral");
    checkGridCellStatus(CELLS_GRID, "6", "status", "Expiring", "warning");

    // Two chips and "+2" with the hidden ones, the empty item dropped, as many chips as the maximum without "+n"
    checkGridCellChips(CELLS_GRID, "1", "teams", List.of("Sales", "Finance"), List.of("Logistics", "Support"));
    checkGridCellChips(CELLS_GRID, "2", "teams", List.of("A", "B"), List.of());
    checkGridCellChips(CELLS_GRID, "5", "teams", List.of("Finance", "Support"), List.of());
    checkGridCellChips(CELLS_GRID, "4", "teams", List.of("Sales"), List.of());

    // The HTML of the value shows literally
    checkGridCellChips(CELLS_GRID, "3", "teams", List.of("<b>bold</b>"), List.of());
    checkGridCellsHaveNoActiveContent(CELLS_GRID);

    // Each row fits its cells (the secondary line and the chips that wrap) and the last one is not cut
    checkGridRowFitsItsCells(CELLS_GRID, "1", "user");
    checkGridRowFitsItsCells(CELLS_GRID, "1", "teams");
    checkGridRowFitsItsCells(CELLS_GRID, "4", "user");
    checkGridRowNotCut(CELLS_GRID, "6", "user");
  }

  /**
   * Theme classes: the grid without header hides it and keeps the rows, and the cells take the colour scale class that
   * the query assigns
   */
  @Test
  void t055_gridThemeClasses() {
    // Title
    setTestTitle("Grid theme: no header and colour scales");

    // The grid with the grid-no-header class keeps its rows but not its header
    openMatrixOption("matrix-test-no-header");
    checkRowContentsGrid("GrdNoHeaderTst", "adminflare");
    checkGridHeaderNotVisible("GrdNoHeaderTst", "name");
    checkGridFirstRowAtTop("GrdNoHeaderTst", "12", "name");

    // Every scale paints its cells with the class of the query
    openMatrixOption("matrix-test-scale");
    checkRowContentsGrid("GrdScaleTst", "adminflare");
    checkCellHasClass("GrdScaleTst", "12", "seq3", "scale-seq-3");
    checkCellHasClass("GrdScaleTst", "12", "pos5", "scale-pos-5");
    checkCellHasClass("GrdScaleTst", "12", "neg1", "scale-neg-1");
  }

  /**
   * Dialog: the size classes give the dialog its width
   */
  @Test
  void t060_dialogSizeClasses() {
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
   * Side panel (#843): a panel opens against its edge as wide as its maximum width, keeps the focus inside while it is
   * shown, closes with Esc, its buttons or its close button and gives the focus back to the button that opened it
   */
  @Test
  @Quarantine(issue = "#976", reason = "After an Esc close the opening button can stay busy and the panel does not open again")
  void t061_sidePanelEdgeWidthFocusAndClose() {
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
   * Popover (#843): a popover opens next to its anchor (below it or at its right), keeps the focus inside while it is
   * shown, closes with Esc, its close button, its buttons or a click on its anchor (which does not open it again) and
   * gives the focus back to the button that opened it. The grid of the deferred popover is loaded once it opens, its
   * Columns work inside it, and its Cancel button closes it and leaves the result as it was
   */
  @Test
  void t062_popoverNextToItsAnchorFocusAndClose() {
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
   * Check how many times the queries of some components of the deferred load screen have run
   *
   * @param componentCalls Pairs of component whose query is counted and times it has run
   */
  private void checkDeferredCalls(String... componentCalls) {
    clickButton("ButDefCalls");
    for (int i = 0; i < componentCalls.length; i += 2) {
      checkCellContents("GrdDefCalls", componentCalls[i], "calls", componentCalls[i + 1]);
    }
  }

  /**
   * Deferred load: the queries of a deferred dialog and of a deferred tab page do not run with the screen, they run the
   * first time the dialog opens or the page is selected (and only then), and the required criteria of a deferred dialog
   * never opened do not block the validation. A dialog which is not deferred shows its rows when it opens
   */
  @Test
  void t070_deferredLoad() {
    // Title
    setTestTitle("Deferred dialog and tab load on their first opening");

    // Go to screen
    openMatrixOption("matrix-test-deferred");

    // The screen load ran the queries of what is not deferred, and none of the deferred dialog and page
    checkDeferredCalls("EagSelect", "1", "TabFirstSelect", "1", DEFERRED_SELECT, "0", "DefRows", "0", "DefText", "0",
      "TabSecondSelect", "0", "TabSecondRows", "0");
    waitForLoadingBar();
    waitForButtonClickability(BUTTON_DEFERRED_SAVE);

    // The required criteria of the dialog never opened are ignored: the save goes through without opening it
    clickButton(BUTTON_DEFERRED_SAVE);
    checkCriterionContents("DefSaved", "Saved");

    // The dialog which is not deferred was loaded with the screen
    clickButton("ButEagDialog");
    checkRowContentsGrid("GrdEagRows", "E3");
    clickButton("ButEagClose");
    checkDialogClosed("DlgEager");

    // The first opening loads the data of the deferred dialog. The select has no selection from the server: it shows
    // its first option
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkRowContentsGrid("GrdDefRows", "3");
    checkCriterionContents("DefText", "Loaded");
    checkSelectContents(DEFERRED_SELECT, "One");
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");
    checkDeferredCalls(DEFERRED_SELECT, "1", "DefRows", "1", "DefText", "1");

    // The next openings do not load it again, and a deferred dialog does not make the other ones wait
    clickButton(BUTTON_DEFERRED_DIALOG);
    checkRowContentsGrid("GrdDefRows", "3");
    clickButton("ButDefClose");
    checkDialogClosed("DlgDeferred");
    checkDeferredCalls(DEFERRED_SELECT, "1", "DefRows", "1", "TabSecondSelect", "0");

    // The first selection of the deferred page loads its data, going back to it does not load it again
    clickTab("TabDeferred", "ENUM_DEFERRED_SECOND");
    checkRowContentsGrid("GrdTabRows", "Two");
    clickTab("TabDeferred", "ENUM_DEFERRED_FIRST");
    checkDeferredCalls("TabSecondSelect", "1", "TabSecondRows", "1");
    clickTab("TabDeferred", "ENUM_DEFERRED_SECOND");
    checkRowContentsGrid("GrdTabRows", "Two");
    clickTab("TabDeferred", "ENUM_DEFERRED_FIRST");
    checkDeferredCalls("TabSecondSelect", "1", "TabSecondRows", "1");
  }

  /**
   * Grid toolbar: the Columns open as a popover where Cancel undoes the changes and the close button keeps them; the
   * user hides and reorders columns, group headers stay aligned and columns never leave their group, the layout is
   * saved for the user and the screen comes back with it, Reset goes back to the definition of the screen, and Maximize
   * fills the window
   */
  @Test
  void t080_gridToolbarColumnsAndMaximize() {
    // Title
    setTestTitle("Grid toolbar: columns popover, saved layout and maximize");

    // Go to screen, with no layout saved by a previous run: the definition of the screen
    openMatrixOption("matrix-test-layout");
    openGridColumns(LAYOUT_GRID);
    clickGridColumnsReset(LAYOUT_GRID);
    closeGridColumns(LAYOUT_GRID);
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c01", "c02", "c03", "c04", "c05", "c06", "c07");
    checkGridGroupHeaderOver(LAYOUT_GRID, "Group A", "c03", "c06");

    // The Columns open as a popover (the default) and hiding a column updates the grid while it is open. Cancel shows the
    // column again and closes the popover
    openGridColumns(LAYOUT_GRID);
    checkGridColumnsPanel(LAYOUT_GRID, "popover");
    hideGridColumn(LAYOUT_GRID, "c01");
    checkGridColumnNotShown(LAYOUT_GRID, "c01");
    clickGridColumnsCancel(LAYOUT_GRID);
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c01", "c02", "c03");

    // The close button keeps the change
    openGridColumns(LAYOUT_GRID);
    hideGridColumn(LAYOUT_GRID, "c01");
    closeGridColumns(LAYOUT_GRID);
    checkGridColumnNotShown(LAYOUT_GRID, "c01");

    // The dialog lists the 26 columns that the screen shows (one hidden now), and not the key that it hides
    openGridColumns(LAYOUT_GRID);
    checkGridColumnsCounter(LAYOUT_GRID, 25, 26);
    showGridColumn(LAYOUT_GRID, "c01");
    checkGridColumnsCounter(LAYOUT_GRID, 26, 26);
    checkGridColumnsListed(LAYOUT_GRID, "c01");
    checkGridColumnsNotListed(LAYOUT_GRID, "key");

    // Hiding the first column of a group, and then one inside it, keeps the header over the columns that are left
    hideGridColumn(LAYOUT_GRID, "c03");
    hideGridColumn(LAYOUT_GRID, "c05");
    checkGridColumnsCounter(LAYOUT_GRID, 24, 26);
    closeGridColumns(LAYOUT_GRID);
    checkGridColumnNotShown(LAYOUT_GRID, "c03");
    checkGridColumnNotShown(LAYOUT_GRID, "c05");
    checkGridGroupHeaderOver(LAYOUT_GRID, "Group A", "c04", "c06");

    // A group without visible columns has no header
    openGridColumns(LAYOUT_GRID);
    hideGridColumn(LAYOUT_GRID, "c04");
    hideGridColumn(LAYOUT_GRID, "c06");
    closeGridColumns(LAYOUT_GRID);
    checkGridGroupHeaderNotShown(LAYOUT_GRID, "Group A");
    openGridColumns(LAYOUT_GRID);
    showGridColumn(LAYOUT_GRID, "c04");
    closeGridColumns(LAYOUT_GRID);
    checkGridGroupHeaderOver(LAYOUT_GRID, "Group A", "c04", "c04");

    // Columns move among their own siblings only: the ones of a group cannot leave it
    openGridColumns(LAYOUT_GRID);
    checkGridColumnCannotMoveUp(LAYOUT_GRID, "c03");
    checkGridColumnCannotMoveDown(LAYOUT_GRID, "c06");
    moveGridColumnDown(LAYOUT_GRID, "c01");
    checkGridColumnCannotMoveUp(LAYOUT_GRID, "c02");
    closeGridColumns(LAYOUT_GRID);
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c02", "c01", "c04");

    // Save as default: the screen comes back with the layout of the user
    openGridColumns(LAYOUT_GRID);
    // The step waits until the server stores the layout and Save closes the Columns, so the screen is opened again only
    // after that. It is silent
    clickGridColumnsSave(LAYOUT_GRID);
    checkMessageMissing("success");
    openMatrixOption(MATRIX_PAGER_OPTION);
    openMatrixOption("matrix-test-layout");
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c02", "c01", "c04", "c07");
    checkGridColumnNotShown(LAYOUT_GRID, "c03");
    checkGridColumnNotShown(LAYOUT_GRID, "c06");
    checkGridGroupHeaderOver(LAYOUT_GRID, "Group A", "c04", "c04");

    // Show the hidden columns again, one by one
    openGridColumns(LAYOUT_GRID);
    checkGridColumnsCounter(LAYOUT_GRID, 23, 26);
    showGridColumn(LAYOUT_GRID, "c03");
    showGridColumn(LAYOUT_GRID, "c05");
    showGridColumn(LAYOUT_GRID, "c06");
    checkGridColumnsCounter(LAYOUT_GRID, 26, 26);
    closeGridColumns(LAYOUT_GRID);
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c02", "c01", "c03", "c04", "c05", "c06");
    checkGridGroupHeaderOver(LAYOUT_GRID, "Group A", "c03", "c06");

    // Reset goes back to the definition of the screen and removes the layout that was saved
    openGridColumns(LAYOUT_GRID);
    clickGridColumnsReset(LAYOUT_GRID);
    closeGridColumns(LAYOUT_GRID);
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c01", "c02", "c03");
    openMatrixOption(MATRIX_PAGER_OPTION);
    openMatrixOption("matrix-test-layout");
    checkGridColumnsLeftToRight(LAYOUT_GRID, "c01", "c02", "c03", "c04", "c05", "c06", "c07");
    checkGridGroupHeaderOver(LAYOUT_GRID, "Group A", "c03", "c06");

    // Maximize fills the content area of the screen (the header stays visible) and restores the grid
    maximizeGrid(LAYOUT_GRID);
    checkGridMaximized(LAYOUT_GRID);
    restoreGrid(LAYOUT_GRID);
    checkGridNotMaximized(LAYOUT_GRID);
  }

  /**
   * Row detail with components (#884): the detail of a country shows a nested grid with the customers of the country,
   * read from a hidden column of the parent row. Several rows keep their own nested grid, which sorts on its own, shows
   * its styled cells and runs its buttons. A reload of the parent closes every detail and removes the nested grids. The
   * second grid brings its nested grid with an include
   */
  @Test
  void t090_gridRowDetailNestedGrid() {
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
   * Card (#899, #836): a metric card filled from a query shows its value and its trend delta, the chart and the grid of
   * other cards are drawn in their body with their data, and the reload button refreshes the cards filled from the
   * server: the metric keeps its value and the status card shows another chip on every reload
   */
  @Test
  void t100_cardDataFromTheServer() {
    // Title
    setTestTitle("Card: data from the server, body and reload");

    // Go to screen
    gotoScreen("test", "layout", "card-test");

    // Metric filled from a query: the number of themes, rising (good news)
    checkCardValue("CrdTstThemes", "18");
    checkCardDelta("CrdTstThemes", "+12.5%", "up", "success");

    // A chart and a grid in the body of their cards
    checkCardBodyComponent("CrdTstChart", "ChrCrdTst");
    checkChartVisible("ChrCrdTst");
    checkCardBodyComponent("CrdTstGrid", "GrdCrdTst");
    checkRowContentsGrid("GrdCrdTst", "awedb2");

    // The status card shows a chip, and every reload changes it
    checkCardChipChangesOnClick("CrdTstStatus", "ButCrdReload");
    checkCardChipChangesOnClick("CrdTstStatus", "ButCrdReload");

    // The reload keeps the value and the delta of the card filled from a query
    checkCardValue("CrdTstThemes", "18");
    checkCardDelta("CrdTstThemes", "+12.5%", "up", "success");
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

    // Collapse the roots
    clickTreeButton(gridId, "ProAdministrator");
    clickTreeButton(gridId, "ProGeneral");
    clickTreeButton(gridId, "ProOperator");

    // Check not visible
    checkTreeIconNotVisible(gridId, "ProAdministrator-ModBase");

    // Open a root, close and open a branch
    clickTreeButton(gridId, "ProAdministrator");
    clickTreeButton(gridId, "ProAdministrator-ModBase");
    clickTreeButton(gridId, "ProAdministrator-ModBase");

    // Open another root
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

    // Open the roots and some branches
    clickTreeButton(gridId, "ProAdministrator");
    clickTreeButton(gridId, "ProGeneral");
    clickTreeButton(gridId, "ProOperator");
    clickTreeButton(gridId, "ProAdministrator-ModBase");
    clickTreeButton(gridId, "ProGeneral-ModBase");

    // Check visible
    checkTreeRowVisible(gridId, "ProOperator-ModBase");
  }

  /**
   * Row grouping (#833): a grid grouped by region, country and city shows a group row per group of each level with its
   * value and its number of rows, ordered by value; collapsed groups hide the groups under them. A group collapses and
   * expands with its toggle, and every group with Collapse all and Expand all, and the Group by panel changes the
   * grouping levels or removes them, all without calling the server
   */
  @Test
  void t110_gridRowGrouping() {
    // Title
    setTestTitle("Grid row grouping: levels, expand and collapse, group by");

    // Go to screen: every group starts expanded
    openMatrixOption("matrix-test-groups");
    checkGridGroupLabel(GROUPS_GRID, "America", "America", 13);
    checkGridGroupExpanded(GROUPS_GRID, "America");
    checkGridGroupLabel(GROUPS_GRID, "America/Canada", "Canada", 4);
    checkGridGroupLabel(GROUPS_GRID, "America/Canada/Toronto", "Toronto", 2);
    watchServerCalls();

    // A group hides its groups, rows and subtotal, and shows them again
    toggleGridGroup(GROUPS_GRID, "America");
    checkGridGroupCollapsed(GROUPS_GRID, "America");
    checkGridGroupExpanded(GROUPS_GRID, "Europe");
    toggleGridGroup(GROUPS_GRID, "America");
    checkGridGroupExpanded(GROUPS_GRID, "America");

    // The regions, by value
    collapseAllGridGroups(GROUPS_GRID);
    checkGridGroupCollapsed(GROUPS_GRID, "America");
    checkGridGroupCollapsed(GROUPS_GRID, "Europe");
    checkGridGroupRowCount(GROUPS_GRID, 1, 2);
    checkGridGroupRowCount(GROUPS_GRID, 2, 0);
    checkGridGroupOrder(GROUPS_GRID, 1, "America", "Europe");
    checkGridGroupLabel(GROUPS_GRID, "Europe", "Europe", 11);

    // The countries of a region and the cities of a country
    toggleGridGroup(GROUPS_GRID, "Europe");
    checkGridGroupOrder(GROUPS_GRID, 2, "Europe/France", "Europe/Spain");
    checkGridGroupLabel(GROUPS_GRID, "Europe/France", "France", 5);
    checkGridGroupLabel(GROUPS_GRID, "Europe/Spain", "Spain", 6);
    checkGridGroupRowCount(GROUPS_GRID, 3, 0);
    toggleGridGroup(GROUPS_GRID, "Europe/Spain");
    checkGridGroupOrder(GROUPS_GRID, 3, "Europe/Spain/Barcelona", "Europe/Spain/Madrid");
    checkGridGroupLabel(GROUPS_GRID, "Europe/Spain/Madrid", "Madrid", 3);

    // After Collapse all, the groups shown by expanding their parent are collapsed too
    checkGridGroupCollapsed(GROUPS_GRID, "Europe/Spain/Madrid");
    toggleGridGroup(GROUPS_GRID, "Europe/Spain/Madrid");
    checkGridGroupExpanded(GROUPS_GRID, "Europe/Spain/Madrid");

    // Expand all opens every group of every level
    expandAllGridGroups(GROUPS_GRID);
    checkGridGroupExpanded(GROUPS_GRID, "America");
    checkGridGroupExpanded(GROUPS_GRID, "America/Canada");
    checkGridGroupExpanded(GROUPS_GRID, "America/Canada/Toronto");

    // Group by one level: the countries
    groupGridBy(GROUPS_GRID, "country");
    collapseAllGridGroups(GROUPS_GRID);
    checkGridGroupOrder(GROUPS_GRID, 1, "Canada", "France", "Mexico", "Spain", "USA");
    checkGridGroupRowCount(GROUPS_GRID, 2, 0);
    checkGridGroupLabel(GROUPS_GRID, "Spain", "Spain", 6);
    checkGridGroupCell(GROUPS_GRID, "Spain", "sales", "500");

    // No grouping: the data rows
    groupGridBy(GROUPS_GRID);
    checkGridNotGrouped(GROUPS_GRID);
    checkRowContentsGrid(GROUPS_GRID, "Store 1");

    // Two levels again, in the order they are chosen
    groupGridBy(GROUPS_GRID, "city", "region");
    collapseAllGridGroups(GROUPS_GRID);
    checkGridGroupRowCount(GROUPS_GRID, 1, 10);
    toggleGridGroup(GROUPS_GRID, "Barcelona");
    checkGridGroupOrder(GROUPS_GRID, 2, "Barcelona/Europe");
    checkNoServerCalls();
  }
}
