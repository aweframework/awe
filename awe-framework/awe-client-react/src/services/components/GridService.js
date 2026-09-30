import {RowPositionType} from "../../utilities/grid";
import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {
  addColumnsGridAction,
  addRowGridAction,
  cancelRowGridAction,
  changeColumnLabelGridAction,
  changeFilterGridAction,
  changePageGridAction,
  changeSortGridAction,
  checkOneSelectedGridAction,
  checkRecordsGeneratedGridAction,
  checkRecordsSavedGridAction,
  checkSomeSelectedGridAction,
  copyRowGridAction, copySelectedRowsToClipboardGridAction,
  deleteRowGridAction,
  editRowGridAction,
  hideColumnsGridAction,
  replaceColumnsGridAction,
  saveRowGridAction,
  selectAllRowsGridAction,
  selectFirstRowGridAction,
  selectLastRowGridAction,
  selectRowGridAction,
  showColumnsGridAction, toggleBranchGridAction,
  toggleColumnVisibilityGridAction,
  unselectAllRowsGridAction,
  updateCellGridAction,
  updateRowGridAction, validateCurrentRowGridAction, verifyRowValidationGridAction
} from "../../redux/thunks/grid";

const { BEFORE, AFTER, FIRST, LAST, CHILD } = RowPositionType;

const useGridService = () => {

  const dispatch = useDispatch();
  const { t} = useTranslation();

  /**
   * Launch when a column visibility toggles
   * @param {Action} action Action received
   */
  const onGridColumnVisibilityToggle = (action) => dispatch(toggleColumnVisibilityGridAction(action));

  /**
   * Launch when row is selected
   * @param {Action} action Action received
   */
  const onGridSelectRow = (action) => dispatch(selectRowGridAction(action));

  /**
   * Launch when the first row is selected
   * @param {Action} action Action received
   */
  const onGridSelectFirstRow = (action) =>  dispatch(selectFirstRowGridAction(action));

  /**
   * Launch when the last row is selected
   * @param {Action} action Action received
   */
  const onGridSelectLastRow = (action) => dispatch(selectLastRowGridAction(action));

  /**
   * Launch when all rows are selected
   * @param {Action} action Action received
   */
  const onGridSelectAllRows = (action) => dispatch(selectAllRowsGridAction(action));

  /**
   * Launch when all rows are unselected
   * @param {Action} action Action received
   */
  const onGridUnselectAllRows = (action) => dispatch(unselectAllRowsGridAction(action));

  /**
   * Launch to check if one row is selected
   * @param {Action} action Action received
   */
  const onGridCheckOneSelected = (action) => dispatch(checkOneSelectedGridAction(action, t));

  /**
   * Launch to check if there are some rows selected
   * @param {Action} action Action received
   */
  const onGridCheckSomeSelected = (action) => dispatch(checkSomeSelectedGridAction(action, t));

  /**
   * Launch to check if all records are stored
   * @param {Action} action Action received
   */
  const onGridCheckRecordsSaved = (action) => dispatch(checkRecordsSavedGridAction(action, t));

  /**
   * Launch to check if there are new records generated
   * @param {Action} action Action received
   */
  const onGridCheckRecordsGenerated = (action) => dispatch(checkRecordsGeneratedGridAction(action, t));

  /**
   * Launch if one row has been deleted
   * @param {Action} action Action received
   */
  const onGridDeleteRow = (action) => dispatch(deleteRowGridAction(action));

  /**
   * Launch when adding a row
   * @param {Action} action Action received
   */
  const onGridAddRow = (action) => dispatch(addRowGridAction(action, CHILD));

  /**
   * Launch when adding a row on the first line
   * @param {Action} action Action received
   */
  const onGridAddRowFirst = (action) => dispatch(addRowGridAction(action, FIRST));

  /**
   * Launch when adding a row on the last line
   * @param {Action} action Action received
   */
  const onGridAddRowLast = (action) => dispatch(addRowGridAction(action, LAST));

  /**
   * Launch when adding a row after the selected row
   * @param {Action} action Action received
   */
  const onGridAddRowAfter = (action) => dispatch(addRowGridAction(action, AFTER));

  /**
   * Launch when adding a row before the selected row
   * @param {Action} action Action received
   */
  const onGridAddRowBefore = (action) => dispatch(addRowGridAction(action, BEFORE));

  /**
   * Launch when updating a row
   * @param {Action} action Action received
   */
  const onGridUpdateRow = (action) => dispatch(updateRowGridAction(action));

  /**
   * Launch when copying a row
   * @param {Action} action Action received
   */
  const onGridCopyRow = (action) => dispatch(copyRowGridAction(action, CHILD));

  /**
   * Launch when copying a row on the first line
   * @param {Action} action Action received
   */
  const onGridCopyRowFirst = (action) => dispatch(copyRowGridAction(action, FIRST));

  /**
   * Launch when copying a row on the last line
   * @param {Action} action Action received
   */
  const onGridCopyRowLast = (action) => dispatch(copyRowGridAction(action, LAST));

  /**
   * Launch when copying a row after the selected row
   * @param {Action} action Action received
   */
  const onGridCopyRowAfter = (action) => dispatch(copyRowGridAction(action, AFTER));

  /**
   * Launch when copying a row before the selected row
   * @param {Action} action Action received
   */
  const onGridCopyRowBefore = (action) => dispatch(copyRowGridAction(action, BEFORE));

  /**
   * Launch when adding some columns
   * @param {Action} action Action received
   */
  const onGridAddColumns = (action) => dispatch(addColumnsGridAction(action));

  /**
   * Launch when replacing some columns
   * @param {Action} action Action received
   */
  const onGridReplaceColumns = (action) => dispatch(replaceColumnsGridAction(action));

  /**
   * Launch when updating a cell
   * @param {Action} action Action received
   */
  const onGridUpdateCell = (action) => dispatch(updateCellGridAction(action));

  /**
   * Launch when showing some columns
   * @param {Action} action Action received
   */
  const onGridShowColumns = (action) => dispatch(showColumnsGridAction(action));

  /**
   * Launch when hiding some columns
   * @param {Action} action Action received
   */
  const onGridHideColumns = (action) => dispatch(hideColumnsGridAction(action));

  /**
   * Launch when changing a column label
   * @param {Action} action Action received
   */
  const onGridChangeColumnLabel = (action) => dispatch(changeColumnLabelGridAction(action));

  /**
   * Launch when editing a row
   * @param {Action} action Action received
   */
  const onGridEditRow = (action) => dispatch(editRowGridAction(action));

  /**
   * Launch when storing a row
   * @param {Action} action Action received
   */
  const onGridSaveRow = (action) => dispatch(saveRowGridAction(action));

  /**
   * Launch when canceling a row
   * @param {Action} action Action received
   */
  const onGridCancelRow = (action) => dispatch(cancelRowGridAction(action));

  /**
   * Launch when changing the page
   * @param {Action} action Action received
   */
  const onGridChangePage = (action) => dispatch(changePageGridAction(action));

  /**
   * Launch when changing the sort criteria
   * @param {Action} action Action received
   */
  const onGridChangeSort = (action) => dispatch(changeSortGridAction(action));

  /**
   * Launch when changing the filter criteria
   * @param {Action} action Action received
   */
  const onGridChangeFilter = (action) => dispatch(changeFilterGridAction(action));

  /**
   * Copy selected rows to the clipboard in csv format
   * @param {Action} action Action received
   */
  const copySelectedRowsToClipboard = (action) => dispatch(copySelectedRowsToClipboardGridAction(action, t));

  /**
   * Validate the form
   * @param {Action} action Action received
   */
  const validateCurrentRow = (action) => dispatch(validateCurrentRowGridAction(action));

  /**
   * Verify a row validation
   * @param {Action} action Action received
   */
  const verifyRowValidation = (action) => dispatch(verifyRowValidationGridAction(action));

  /**
   * Launch when toggling a tree branch
   * @param {Action} action Action received
   */
  const onBranchToggle = (action) => dispatch(toggleBranchGridAction(action));

  const getActions =  () => ({
    "toggle-columns-visibility": onGridColumnVisibilityToggle,
    "select-row": onGridSelectRow,
    "select-first-row": onGridSelectFirstRow,
    "select-last-row": onGridSelectLastRow,
    "select-all-rows": onGridSelectAllRows,
    "unselect-all-rows": onGridUnselectAllRows,
    "check-one-selected": onGridCheckOneSelected,
    "check-some-selected": onGridCheckSomeSelected,
    "check-records-generated": onGridCheckRecordsGenerated,
    "check-records-saved": onGridCheckRecordsSaved,
    "delete-row": onGridDeleteRow,
    "add-row": onGridAddRow,
    "add-row-top": onGridAddRowFirst,
    "add-row-bottom": onGridAddRowLast,
    "add-row-down": onGridAddRowAfter,
    "add-row-up": onGridAddRowBefore,
    "update-row": onGridUpdateRow,
    "copy-row": onGridCopyRow,
    "copy-row-top": onGridCopyRowFirst,
    "copy-row-bottom": onGridCopyRowLast,
    "copy-row-down": onGridCopyRowAfter,
    "copy-row-up": onGridCopyRowBefore,
    "add-columns": onGridAddColumns,
    "replace-columns": onGridReplaceColumns,
    "update-cell": onGridUpdateCell,
    "show-columns": onGridShowColumns,
    "hide-columns": onGridHideColumns,
    "change-column-label": onGridChangeColumnLabel,
    "save-row": onGridSaveRow,
    "cancel-row": onGridCancelRow,
    "edit-row": onGridEditRow,
    "change-page": onGridChangePage,
    "change-sort": onGridChangeSort,
    "change-filter": onGridChangeFilter,
    "copy-selected-rows-clipboard": copySelectedRowsToClipboard,
    "validate-row": validateCurrentRow,
    "verify-row-validation": verifyRowValidation,
    "tree-branch": onBranchToggle
  });

  return {
    getActions
  };
};

export default useGridService;
