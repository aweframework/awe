// Type-level assertions for the grid thunks, checked by `npm run typecheck` (not run by Jest)
import type {AppDispatch, AppThunk, RootState} from "../../../../src/redux/types";
import type {ClientAction} from "../../../../src/redux/thunks/types";
import {
  addColumnsGridAction,
  addRowGridAction,
  afterAddRowGridAction,
  afterCancelRowGridAction,
  afterDeleteRowGridAction,
  afterSaveRowGridAction,
  cancelRowGridAction,
  changeColumnLabelGridAction,
  changeFilterGridAction,
  changePageGridAction,
  changeSortGridAction,
  checkOneSelectedGridAction,
  checkRecordsGeneratedGridAction,
  checkRecordsSavedGridAction,
  checkSomeSelectedGridAction,
  copyRowGridAction,
  copySelectedRowsToClipboardGridAction,
  deleteRowGridAction,
  editRowGridAction,
  hideColumnsGridAction,
  moveRowGridAction,
  replaceColumnsGridAction,
  saveRowGridAction,
  selectAllRowsGridAction,
  selectFirstRowGridAction,
  selectLastRowGridAction,
  selectRowGridAction,
  selectRows,
  showColumnsGridAction,
  toggleBranchGridAction,
  toggleColumnVisibilityGridAction,
  unselectAllRowsGridAction,
  updateCellGridAction,
  updateRowGridAction,
  validateCurrentRowGridAction,
  verifyRowValidationGridAction
} from "../../../../src/redux/thunks/grid";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

const address = {view: "report", component: "Grid"};
const action: ClientAction = {type: "select-row", address};
const t = (label: string): string => label;

// Every grid thunk works on an action and returns a thunk of the store
assertType<Equal<ReturnType<typeof addColumnsGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof addRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof afterAddRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof afterCancelRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof afterDeleteRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof afterSaveRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof cancelRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changeColumnLabelGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changeFilterGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changePageGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changeSortGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkOneSelectedGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkRecordsGeneratedGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkRecordsSavedGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkSomeSelectedGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof copyRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof copySelectedRowsToClipboardGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof deleteRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof editRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof hideColumnsGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof moveRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof replaceColumnsGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof saveRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof selectAllRowsGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof selectFirstRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof selectLastRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof selectRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof showColumnsGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof toggleBranchGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof toggleColumnVisibilityGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof unselectAllRowsGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateCellGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof validateCurrentRowGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof verifyRowValidationGridAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof selectRows>, void>>();

// Actions that only need the action
addColumnsGridAction(action);
afterAddRowGridAction(action);
afterCancelRowGridAction(action);
afterDeleteRowGridAction(action);
afterSaveRowGridAction(action);
cancelRowGridAction(action);
changeColumnLabelGridAction(action);
changeFilterGridAction(action);
changePageGridAction(action);
changeSortGridAction(action);
deleteRowGridAction(action);
editRowGridAction(action);
hideColumnsGridAction(action);
replaceColumnsGridAction(action);
saveRowGridAction(action);
selectAllRowsGridAction(action);
selectFirstRowGridAction(action);
selectLastRowGridAction(action);
selectRowGridAction(action);
showColumnsGridAction(action);
toggleBranchGridAction(action);
toggleColumnVisibilityGridAction(action);
unselectAllRowsGridAction(action);
updateCellGridAction(action);
updateRowGridAction(action);
validateCurrentRowGridAction(action);
verifyRowValidationGridAction(action);
checkOneSelectedGridAction(action, t);
checkRecordsGeneratedGridAction(action, t);
checkRecordsSavedGridAction(action, t);
checkSomeSelectedGridAction(action, t);
copySelectedRowsToClipboardGridAction(action, t);
// Actions with parameters
selectRowGridAction({...action, parameters: {values: ["1"]}});
deleteRowGridAction({...action, parameters: {rowId: 3}});
changePageGridAction({...action, parameters: {page: 2, first: 10, rows: 10, max: 50}});

// Positions: a row is added next to another, before or after, first, last or as its child; it only moves in a line
addRowGridAction(action, "CHILD");
addRowGridAction(action, "FIRST");
copyRowGridAction(action, "AFTER");
moveRowGridAction(action, "LAST");
// @ts-expect-error a row is not added to a position that does not exist
addRowGridAction(action, "MIDDLE");
// @ts-expect-error a row does not move to be the child of another
moveRowGridAction(action, "CHILD");
// @ts-expect-error the position is needed
copyRowGridAction(action);

// Select rows: the store functions, the address of the grid and the identifiers of the rows
const dispatch = (() => undefined) as unknown as AppDispatch;
const getState = (() => undefined) as unknown as () => RootState;
selectRows(dispatch, getState, address, ["1", 2]);
// @ts-expect-error the address is an object
selectRows(dispatch, getState, "Grid", ["1"]);
// @ts-expect-error the rows are a list
selectRows(dispatch, getState, address, "1");
// @ts-expect-error the rows to select are needed
selectRows(dispatch, getState, address);

// @ts-expect-error the action is an object
selectRowGridAction("select-row");
// @ts-expect-error the action needs its type
editRowGridAction({address});
// @ts-expect-error the address of an action is an object
saveRowGridAction({type: "save-row", address: "Grid"});
// @ts-expect-error the messages are translated: the translator is needed
checkOneSelectedGridAction(action);
// @ts-expect-error the translator is a function
copySelectedRowsToClipboardGridAction(action, "en");
