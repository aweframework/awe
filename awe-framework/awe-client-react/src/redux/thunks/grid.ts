import type { ComponentAddress } from "../../contract";
import {
  generateMessageAction,
  getActionAddress,
  getComponent,
  getGridAndValues,
  translateLabel
} from "../../utilities";
import { acceptAction, addActionsTop, rejectAction } from "../actions/actions";
import { updateAttributes, updateSpecificAttributes } from "../actions/components";
import { raiseEventOnDependencies, updateModelWithDependencies } from "./components";
import { compareEqualValues } from "../../utilities/general";
import {
  extractCellValue,
  getGridIdentifier,
  getRow,
  moveRowInValues,
  OperationType,
  RowPositionType
} from "../../utilities/grid";
import type { GridAttributes, GridColumn, GridRow, RowPosition } from "../../utilities/grid";
import type { ComponentState } from "../../utilities/mergeUtils";
import { validateRow } from "./validate";
import { getAllComponents } from "../selectors/componentSelectors";
import type { AppDispatch, AppThunk, RootState } from "../types";
import type { ClientAction, Translate } from "./types";
const { BEFORE, AFTER, FIRST, LAST, CHILD } = RowPositionType;
const { INSERT, UPDATE, DELETE } = OperationType;
let copiedRows = 0;

const NEW_ROW_PREFIX = "new-row-";

/** Position a row is added to: next to another row, at the ends of the grid or as the child of a branch */
type AddRowPosition = RowPosition | typeof CHILD;

/** What a row is going through: the edition, the operation and the state of its branch */
interface RowState {
  editing?: boolean;
  operation?: string;
  expanded?: boolean;
  loaded?: boolean;
  editingRow?: Row | null;
  newRow?: Row;
}

/** A row of a grid as these thunks change it: the cells by column and what the row is going through */
interface Row extends GridRow {
  selected?: unknown;
  $row?: RowState;
  $attrs?: Record<string, { error?: unknown } | undefined>;
}

/** A column of the column model: the columns the server sends can be hidden and have a default value */
type Column = GridColumn & { hidden?: boolean; value?: unknown };

/** Attributes of a grid that these thunks read */
type Attributes = Partial<GridAttributes> & {
  columnModel?: Column[];
  loadAll?: boolean;
  loading?: boolean;
  multiselect?: boolean;
  pendingCellUpdates?: PendingCellUpdate[];
  treeParent?: string;
  validateOnSave?: boolean;
};

/** A grid in the state: an action is launched on a grid that exists, as it always did reading it fails otherwise */
type GridState = ComponentState & { attributes: Attributes; model: { values: Row[] } };

/** A change of a cell pushed while the grid was loading, to put back once the rows arrive */
interface PendingCellUpdate {
  row?: string;
  column?: string;
  data: unknown;
}

/** Parameters of the actions that show or hide columns */
interface ColumnsParameters {
  columns?: string[];
  show?: boolean;
}

/** Parameters of the actions that select the rows of a grid */
interface SelectRowParameters {
  values?: unknown[];
}

/** Parameters of the actions that work on a row: the identifier of the row and its data */
interface RowParameters {
  rowId?: string | number | null;
  row?: Row;
  selectedRow?: string | number;
}

/** Parameters of the actions that add or replace the columns of a grid: the action always carries them */
interface NewColumnsParameters {
  columns: Column[];
}

/** Parameters of the actions that change a cell with the data pushed by the server */
interface CellParameters {
  data?: unknown;
}

/** Parameters of the action that changes the label of a column */
interface ColumnLabelParameters {
  column?: string;
  label?: string;
}

/** Parameters of the action that edits a row: its identifier */
interface EditRowParameters {
  row?: string | number;
}

/** Parameters of the action that changes the page of a grid */
interface PageParameters {
  page?: number;
  first?: number;
  rows?: number;
  max?: number;
}

/** Parameters of the action that opens or closes a branch of a tree grid */
interface ToggleBranchParameters {
  datalist?: { rows?: Row[] };
  row?: string | number;
  expanded?: boolean;
}

/**
 * The grid an action works on
 * @param components Components of the state
 * @param address Address of the grid
 * @returns Grid
 */
const getGrid = (components: Record<string, ComponentState>, address: ComponentAddress): GridState =>
  getComponent(components, address) as GridState;

/**
 * The grid and its rows, as the actions that edit rows read them
 * @param components Components of the state
 * @param address Address of the grid
 * @returns Grid and its rows
 */
const getGridAndRows = (
  components: Record<string, ComponentState>,
  address: ComponentAddress
): { component: GridState; values: Row[] } =>
  getGridAndValues(components, address) as { component: GridState; values: Row[] };

/**
 * Identifier of the next new row of a grid: the AngularJS client numbers them from zero in every grid
 * @param {Array} values Rows of the grid
 * @param {string} gridId Row identifier field of the grid
 * @return {string} Identifier of the new row
 */
const getNewRowId = (values: Row[], gridId: string): string => {
  const numbers = values
    .map(row => String(row[gridId]))
    .filter(id => id.startsWith(NEW_ROW_PREFIX))
    .map(id => Number(id.substring(NEW_ROW_PREFIX.length)))
    .filter(number => Number.isInteger(number));
  return `${NEW_ROW_PREFIX}${numbers.length > 0 ? Math.max(...numbers) + 1 : 0}`;
};

const addRowToValues = (
  rowId: unknown,
  identifier: string,
  values: Row[],
  position: AddRowPosition,
  newRow: Row
): Row[] => {
  let rowIndex = values.findIndex((row) => String(row[identifier]) === String(rowId));
  rowIndex = rowIndex < 0 ? values.length : rowIndex;

  switch (position) {
    case FIRST:
      rowIndex = 0;
      break;
    case AFTER:
    case CHILD:
      rowIndex++;
      break;
    case LAST:
      rowIndex = values.length;
      break;
    case BEFORE:
    default:
      break;
  }

  return [
    ...values.slice(0, rowIndex)
      .map((row) => ({ ...row, selected: false, $row: { ...(row?.$row || {}), editing: false } })),
    { ...newRow, selected: true },
    ...values.slice(rowIndex, values.length)
      .map((row) => ({ ...row, selected: false, $row: { ...(row?.$row || {}), editing: false } }))
  ];
};

export const toggleColumnVisibilityGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { columns = [], show } = action.parameters as ColumnsParameters;
    const { columnModel = [] } = component.attributes || {};

    // Accept action
    dispatch(acceptAction(action));

    // Update attributes
    dispatch(updateAttributes(address, {
      columnModel: columnModel.map(col => ({
        ...col,
        hidden: columns.includes(col.name) ? !show : col.hidden
      }))
    }));
  };
};

/**
 * Rows to select in a grid. The row being edited is the selected one (the AngularJS client edits the row the user selects):
 * while it is edited, the selection stays on it, as a click on another row (or two quick clicks that unselect it) would
 * leave a row being edited that is not selected, and the criteria that depend on the selected row would ignore it
 * @param {function} getState Get state function
 * @param {object} address Address of the grid
 * @param {Array} values Identifiers of the rows to select
 * @returns {Array} Identifiers of the rows to select
 */
const getRowsToSelect = (
  getState: () => RootState,
  address: ComponentAddress,
  values: unknown[]
): unknown[] => {
  const component = getGrid(getAllComponents(getState()), address);
  const editingRow = !component?.attributes?.multiselect
    ? component?.model?.values?.find(row => row.$row?.editing) : undefined;
  return editingRow ? [editingRow[getGridIdentifier(component.attributes)]] : values;
};

const selectRowsUpdate = (address: ComponentAddress, selected: unknown[]): AppThunk => updateModelWithDependencies(address, {
  event: "select-row",
  selected
});

/**
 * Select rows of a grid (see getRowsToSelect)
 * @param {function} dispatch Dispatch function
 * @param {function} getState Get state function
 * @param {object} address Address of the grid
 * @param {Array} values Identifiers of the rows to select
 */
export const selectRows = (
  dispatch: AppDispatch,
  getState: () => RootState,
  address: ComponentAddress,
  values: unknown[]
): void => {
  dispatch(selectRowsUpdate(address, getRowsToSelect(getState, address, values)));
};

export const selectRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { values = [] } = action.parameters as SelectRowParameters;

    // Read the state before accepting the action: accepting it runs the queue, which can change the row being edited
    const selected = getRowsToSelect(getState, address, values);

    dispatch(acceptAction(action));

    dispatch(selectRowsUpdate(address, selected));
  };
};

export const selectFirstRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row, index) => ({ ...row, selected: index === 0 }))
    }));
  };
};

export const selectLastRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row, index) => ({ ...row, selected: index === values.length - 1 }))
    }));
  };
};

export const selectAllRowsGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row) => ({ ...row, selected: true }))
    }));
  };
};

export const unselectAllRowsGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row) => ({ ...row, selected: false }))
    }));
  };
};

/**
 * Raise the notification event of a grid and, when asked, unselect all its rows, like the AngularJS client does when
 * it runs its after-* actions. A grid that is not on screen or has no rows is only notified.
 * @param {function} dispatch Dispatch function
 * @param {function} getState Get state function
 * @param {Object} address Address of the grid
 * @param {string} event Name of the notification, which is also the event raised
 * @param {boolean} unselect Whether the rows of the grid are unselected after the notification
 */
const notifyAfterRow = (
  dispatch: AppDispatch,
  getState: () => RootState,
  address: ComponentAddress,
  event: string,
  unselect: boolean
): void => {
  dispatch(updateModelWithDependencies(address, { event }));

  const values = getGrid(getAllComponents(getState()), address)?.model?.values;
  if (unselect && Array.isArray(values)) {
    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row) => ({ ...row, selected: false }))
    }));
  }
};

/**
 * Notification launched after a row was added, deleted, saved or cancelled. It accepts the action and notifies the
 * grid. After saving or cancelling a row the selection is cleared too.
 * @param {string} event Name of the notification, which is also the event raised
 * @param {boolean} unselect Whether the rows of the grid are unselected after the notification
 * @return {function(Action): function} Thunk factory for the action
 */
const afterRowGridAction = (event: string, unselect: boolean) => (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    dispatch(acceptAction(action));
    notifyAfterRow(dispatch, getState, getActionAddress(action), event, unselect);
  };
};

export const afterAddRowGridAction = afterRowGridAction("after-add-row", false);
export const afterDeleteRowGridAction = afterRowGridAction("after-delete-row", false);
export const afterSaveRowGridAction = afterRowGridAction("after-save-row", true);
export const afterCancelRowGridAction = afterRowGridAction("after-cancel-row", true);

export const checkOneSelectedGridAction = (action: ClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    // Like AngularJS, the check event is raised whether the check passes or not
    dispatch(raiseEventOnDependencies(address, "check-one-selected"));
    const components = getAllComponents(getState());
    const component = getGrid(components, address);
    const { values } = component.model;

    if (values.filter((row) => row.selected).length === 1) {
      dispatch(acceptAction(action));
    } else {
      dispatch(rejectAction(action));
      dispatch(addActionsTop([generateMessageAction("warning", translateLabel('GRID_CHECK_ONE_SELECTED_TITLE', t), translateLabel('GRID_CHECK_ONE_SELECTED_MESSAGE', t))]));
    }
  };
};

export const checkSomeSelectedGridAction = (action: ClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    // Like AngularJS, the check event is raised whether the check passes or not
    dispatch(raiseEventOnDependencies(address, "check-some-selected"));
    const components = getAllComponents(getState());
    const component = getGrid(components, address);
    const { values } = component.model;

    if (values.filter((row) => row.selected).length > 0) {
      dispatch(acceptAction(action));
    } else {
      dispatch(rejectAction(action));
      dispatch(addActionsTop([generateMessageAction("warning", translateLabel('GRID_CHECK_SOME_SELECTED_TITLE', t), translateLabel('GRID_CHECK_SOME_SELECTED_MESSAGE', t))]));
    }
  };
};

export const checkRecordsSavedGridAction = (action: ClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    // Like AngularJS, the check event is raised whether the check passes or not
    dispatch(raiseEventOnDependencies(address, "check-records-saved"));
    const components = getAllComponents(getState());
    const component = getGrid(components, address);
    const { values } = component.model;

    if (values.filter(row => row.$row?.editing).length === 0) {
      // Accept action
      dispatch(acceptAction(action));
    } else {
      // Reject action
      dispatch(rejectAction(action));

      // Send message
      dispatch(addActionsTop([generateMessageAction("warning", translateLabel('GRID_CHECK_ALL_SAVED_TITLE', t), translateLabel('GRID_CHECK_ALL_SAVED_MESSAGE', t))]));
    }
  };
};

export const checkRecordsGeneratedGridAction = (action: ClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    // Like AngularJS, the check event is raised whether the check passes or not
    dispatch(raiseEventOnDependencies(address, "check-records-generated"));
    const components = getAllComponents(getState());
    const component = getGrid(components, address);
    const { values } = component.model;

    if (values.filter(row => row.$row?.operation).length > 0) {
      // Accept action
      dispatch(acceptAction(action));
    } else {
      // Reject action
      dispatch(rejectAction(action));

      // Send message
      dispatch(addActionsTop([generateMessageAction("warning", translateLabel('GRID_CHECK_RECORDS_GENERATED_TITLE', t), translateLabel('GRID_CHECK_RECORDS_GENERATED_MESSAGE', t))]));
    }
  };
};

export const deleteRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { multioperation } = component.attributes || {};
    const { values } = component.model;
    const { rowId } = action.parameters as RowParameters;
    const gridId = getGridIdentifier(component.attributes);
    // The row to delete exists: reading it fails otherwise, as it always did
    const selectedRow = values.find((row) => (rowId ? String(row[gridId]) === String(rowId) : row.selected)) as Row;

    dispatch(acceptAction(action));

    let newValues: Row[];
    if (multioperation && (selectedRow.$row || {}).operation !== INSERT) {
      newValues = values.map((item) => ({
        ...item,
        $row: {
          ...(item.$row || {}),
          operation: compareEqualValues(item[gridId], selectedRow[gridId]) ? DELETE : item?.$row?.operation
        }
      }));
    } else {
      newValues = values.filter((item) => !compareEqualValues(item[gridId], selectedRow[gridId]));
    }

    // Like AngularJS, no row stays selected and the after-delete-row notification is raised
    dispatch(updateModelWithDependencies(address, {
      event: "delete-row",
      values: newValues.map((item) => ({ ...item, selected: false })),
      ...(newValues.length === values.length ? {} : { records: newValues.length })
    }));
    notifyAfterRow(dispatch, getState, address, "after-delete-row", false);
  };
};

/**
 * Verify if there is a row being edited and if it has passed validation
 * @param dispatch Dispatch function
 * @param getState Get state function
 * @param action Action
 * @returns {boolean} No row is being edited or has passed validation
 */
function checkEditingRow(dispatch: AppDispatch, getState: () => RootState, action: ClientAction): boolean {
  const components = getAllComponents(getState());
  const address = getActionAddress(action);
  const component = getGrid(components, address);
  const { validateOnSave = true } = component.attributes;
  const { values } = component.model;

  // Verificar si hay una fila en edición antes de proceder
  const editingRow = values.find(row => row.$row?.editing);

  if (validateOnSave && editingRow) {
    // Si hay una fila en edición, validarla primero
    const gridId = getGridIdentifier(component.attributes);
    const editingRowId = editingRow[gridId];

    // Usar la validación similar a validateRow del useGrid.js
    // Esto debería invocar la validación de la fila actual
    // The identifier of a row is a text or a number, and the validation compares it as a text
    dispatch(validateRow({ ...address, row: editingRowId as string }));

    // Verificar si la validación fue exitosa
    const validatedRow = getGrid(getAllComponents(getState()), address).model.values.find(row => row.$row?.editing);

    if (errorsInValidatedRow(validatedRow)) {
      // Si hay errores de validación, rechazar la acción
      dispatch(rejectAction(action));
      return false;
    }
  }

  return true;
}

function errorsInValidatedRow(validatedRow: Row | undefined): boolean {
  const cellErrors = Object.values(validatedRow?.$attrs || {}).filter(cell => cell?.error);
  return cellErrors.length > 0;
}

export const addRowGridAction = (action: ClientAction, position: AddRowPosition): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { editable, multioperation, treegrid, treeParent, columnModel = [] } = component.attributes || {};
    const { values } = component.model;

    // Verify if editing row
    if (!checkEditingRow(dispatch, getState, action)) return;

    const { rowId, row = {} } = action.parameters as RowParameters;
    const gridId = getGridIdentifier(component.attributes);
    const selectedRow: Row = values.find((item) => (rowId ? String(item[gridId]) === String(rowId) : item.selected)) || {};
    // Only tree grids have a parent field: a grid without it reads the cell "undefined", as it always did
    const parentField = treeParent as string;
    const parentId = position !== CHILD ? selectedRow[parentField] || "" : selectedRow[gridId] || "";
    const rowDefaultValues = columnModel.reduce<Record<string, unknown>>((prev, current) => ({
      ...prev,
      [current.name]: current.value || null
    }), {});

    dispatch(updateModelWithDependencies(address, { event: "add-row" }));
    dispatch(acceptAction(action));

    const newRow: Row = {
      [gridId]: getNewRowId(values, gridId),
      ...rowDefaultValues,
      ...(treegrid ? { [parentField]: parentId } : {}),
      ...row,
      $row: {
        ...row?.$row,
        ...(multioperation ? { operation: INSERT } : {}),
        ...(editable || multioperation ? { editing: false } : {})
      }
    };

    // A branch that gets a child shows it, like the AngularJS client does
    const branchValues: Row[] = treegrid && position === CHILD
      ? values.map(item => String(item[gridId]) === String(selectedRow[gridId])
        ? { ...item, $row: { ...(item.$row || {}), expanded: true, loaded: true } } : item)
      : values;

    dispatch(updateModelWithDependencies(address, {
      records: values.length + 1,
      values: addRowToValues(selectedRow[gridId], gridId, branchValues, position, {
        ...newRow,
        $row: {
          ...newRow.$row,
          ...((editable || multioperation) ? { editing: true } : {}),
          newRow
        }
      }),
      event: "after-add-row"
    }));
  };
};

/**
 * Move the selected row (or the one of the selectedRow parameter) of a grid to the top, one position up or down, or
 * to the bottom. The rows of the model keep the new order, so it is the order the grid sends to the server when it
 * is saved
 * @param {object} action Action received
 * @param {string} position Where to move the row: FIRST, BEFORE, AFTER or LAST
 */
export const moveRowGridAction = (action: ClientAction, position: RowPosition): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    // Tree grids keep their rows under their parents: the move does not apply, as in the AngularJS engine
    if (component.attributes?.treegrid) {
      dispatch(acceptAction(action));
      return;
    }
    const { values } = component.model;
    const gridId = getGridIdentifier(component.attributes);
    const { selectedRow, rowId = selectedRow } = (action.parameters || {}) as RowParameters;
    // The identifier of a row is a text or a number
    const movedRowId = rowId ?? values.find(item => item.selected)?.[gridId] as string | number | undefined;
    const movedValues = moveRowInValues(values, gridId, movedRowId, position);

    dispatch(acceptAction(action));
    if (movedValues !== values) {
      dispatch(updateModelWithDependencies(address, { values: movedValues }));
    }
  };
};

export const updateRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;
    const { rowId, row } = action.parameters as RowParameters;
    const gridId = getGridIdentifier(component.attributes);
    const updatedValues = values.map((item) =>
      String(item[gridId]) === String(rowId) ? { ...item, ...row } : item
    );

    dispatch(acceptAction(action));
    dispatch(updateModelWithDependencies(address, { values: updatedValues }));
  };
};

export const copyRowGridAction = (action: ClientAction, position: AddRowPosition): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;
    const row = values.find(item => item.selected);
    copiedRows++;
    dispatch(addRowGridAction({
      ...action,
      parameters: { ...action.parameters, row: { ...row, id: `copied-row-${copiedRows}` } }
    }, position));
  };
};

export const addColumnsGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { columns } = action.parameters as unknown as NewColumnsParameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateAttributes(address, { columnModel: [...(component.attributes?.columnModel || []), ...columns] }));
  };
};

export const replaceColumnsGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { columns } = action.parameters as unknown as NewColumnsParameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateAttributes(address, { columnModel: [...columns] }));
  };
};

export const updateCellGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { data } = action.parameters as CellParameters;

    // Accept action
    dispatch(acceptAction(action));

    // A load of the grid which is still pending was requested before this pushed change, so its rows come from a
    // query that may not have seen it (or may not even have the row yet): remember the cell to put it back
    // once the rows arrive (see fillAction)
    const gridAddress = { view: address.view, component: address.component };
    const component = getGrid(getAllComponents(getState()), gridAddress);
    if (component?.attributes?.loading) {
      dispatch(updateAttributes(gridAddress, {
        pendingCellUpdates: [
          ...(component.attributes.pendingCellUpdates || []),
          { row: address.row, column: address.column, data }
        ]
      }));
    }

    // Update selected rows
    dispatch(updateModelWithDependencies(address, { values: data }));
  };
};

export const showColumnsGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    dispatch(toggleColumnVisibilityGridAction({ ...action, parameters: { ...action.parameters, show: true } }));
  };
};

export const hideColumnsGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    dispatch(toggleColumnVisibilityGridAction({ ...action, parameters: { ...action.parameters, show: false } }));
  };
};

export const changeColumnLabelGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { column, label } = action.parameters as ColumnLabelParameters;
    const { columnModel = [] } = component.attributes || {};

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateAttributes(address, {
      columnModel: columnModel.map(col => ({
        ...col,
        label: col.name === column ? label : col.label
      }))
    }));
  };
};

export const editRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;
    const { row } = action.parameters as EditRowParameters;
    const gridId = getGridIdentifier(component.attributes);

    // Verify if editing row
    if (!checkEditingRow(dispatch, getState, action)) return;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateModelWithDependencies(address, {
      event: "edit-row",
      values: values.map(item => ({
        ...item,
        selected: String(item[gridId]) === String(row),
        $row: {
          ...(item.$row || {}),
          editing: String(item[gridId]) === String(row),
          ...(String(item[gridId]) === String(row) ? { editingRow: item.$row?.editingRow || { ...item } } : {})
        }
      }))
    }));
  };
};

export const saveRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    // Like AngularJS, before-save-row is raised before the row is validated and saved
    dispatch(raiseEventOnDependencies(address, "before-save-row"));
    const components = getAllComponents(getState());
    const { component, values } = getGridAndRows(components, address);
    const editingRow: Row = values.find(row => row.$row?.editing) || {};
    const gridId = getGridIdentifier(component.attributes);

    // Verify if editing row
    if (!checkEditingRow(dispatch, getState, action)) return;

    // Accept action
    dispatch(acceptAction(action));

    // Update save row event
    dispatch(updateModelWithDependencies(address, {
      event: "save-row",
      values: values.map(item => ({
        ...item,
        $row: {
          ...(item.$row || {}),
          operation: String(item[gridId]) === String(editingRow[gridId]) ? item?.$row?.operation || UPDATE : item?.$row?.operation
        }
      }))
    }));

    // After save row event
    const updatedComponents = getAllComponents(getState());
    const { values: newValues } = getGridAndRows(updatedComponents, address);
    dispatch(updateModelWithDependencies(address, {
      event: "after-save-row",
      values: newValues.map(item => ({
        ...item,
        selected: false,
        $row: {
          ...(item.$row || {}),
          editing: false,
          editingRow: null,
        }
      }))
    }));
  };
};

export const cancelRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    // Like AngularJS, before-cancel-row is raised before the row is restored
    dispatch(raiseEventOnDependencies(address, "before-cancel-row"));
    const components = getAllComponents(getState());
    const { component, values } = getGridAndRows(components, address);
    const editingRow: Row = values.find(row => row.$row?.editing)?.$row?.editingRow || {};
    const gridId = getGridIdentifier(component.attributes);

    // Verify if editing row
    if (!checkEditingRow(dispatch, getState, action)) return;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    if (editingRow) {
      dispatch(updateModelWithDependencies(address, {
        event: "cancel-row",
        values: values.map(item => String(item[gridId]) === String(editingRow[gridId]) ? ({
          ...editingRow,
          selected: false
        }) : ({ ...item }))
      }));

      // After cancel row, as the AngularJS client launches it: event and no row selected
      notifyAfterRow(dispatch, getState, address, "after-cancel-row", true);
    }
  };
};


export const changePageGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { page, first, rows, max } = action.parameters as PageParameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update page
    dispatch(updateSpecificAttributes(address, { page, first, rows, max }));
  };
};

export const changeSortGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { sort } = action.parameters as { sort?: unknown };

    // Accept action
    dispatch(acceptAction(action));

    // Update sort
    dispatch(updateSpecificAttributes(address, {sort}));
  };
};

export const changeFilterGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { filters } = action.parameters as { filters?: unknown };

    // Accept action
    dispatch(acceptAction(action));

    // Update filters
    dispatch(updateSpecificAttributes(address, { filters }));
  };
};

export const copySelectedRowsToClipboardGridAction = (action: ClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, address);
    const { values } = component.model;
    // The grid has its columns: reading them fails otherwise, as it always did
    const columnModel = component.attributes.columnModel as Column[];

    // Accept the action
    dispatch(acceptAction(action));

    const clipboardHeaders = columnModel.map(column => translateLabel(column.label, t)).join("\t") + "\n";

    const clipboardData = values
      .filter(row => row.selected)
      .map(row => columnModel
        .map(column => extractCellValue(row[column.name] || ""))
        .join("\t")
      )
      .join("\n");

    // Get selected lines values and store them into the clipboard
    navigator.clipboard.writeText(clipboardHeaders + clipboardData)
      // eslint-disable-next-line no-console
      .catch((reason) => console.error("Error copying the selected rows to the clipboard:", reason));
  };
};

export const validateCurrentRowGridAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    // Validate all selected components
    const address = getActionAddress(action);
    dispatch(validateRow(address));

    // Accept the action
    dispatch(acceptAction(action));
  };
};

export const verifyRowValidationGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getGrid(components, { component: address.component, view: address.view });
    // Check if validation has been successful
    // The row of the address is a text, as the rows are searched
    if (errorsInValidatedRow(getRow(component, address.row as string))) {
      // If there are errors, reject action
      dispatch(rejectAction(action));
    } else {
      // If is ok, accept the action
      dispatch(acceptAction(action));
    }
  };
};

export const toggleBranchGridAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const parameters = action.parameters as ToggleBranchParameters;
    const { rows = [] } = parameters?.datalist || {};
    const components = getAllComponents(getState());
    const { component, values } = getGridAndRows(components, address);
    const { attributes } = component;
    const { loadAll } = attributes;
    const { row, expanded } = parameters;
    const gridId = getGridIdentifier(component.attributes);

    // Accept action
    dispatch(acceptAction(action));

    // If not loadAll, add the loaded rows
    let treeValues = values;
    if (!loadAll && rows.length > 0) {
      const rowIndex = treeValues.findIndex(item => String(item[gridId]) === String(row));
      treeValues = [
        ...treeValues.slice(0, rowIndex),
        ...rows,
        ...treeValues.slice(rowIndex, treeValues.length)
      ];
    }

    // The branch is loaded when the server answered it (even empty), with loadAll, or when its children are already in
    // the grid; a node opened by its row and collapsed locally keeps asking the server for its branch when reopened
    const { treeParent = "parent" } = attributes;
    const serverAnswer = "datalist" in parameters;
    const branchLoaded = serverAnswer || treeValues.some(item => String(item[treeParent]) === String(row));

    // Update selected rows
    dispatch(updateModelWithDependencies(address, {
      event: "toggle-row",
      values: treeValues
        .map(item => String(item[gridId]) === String(row) ?
          ({ ...item, $row: { ...item.$row, expanded: typeof expanded === "boolean" ? !expanded : !item.$row?.expanded,
            loaded: !!(loadAll || item.$row?.loaded || branchLoaded) } }) : ({ ...item }))
    }));
  };
};
