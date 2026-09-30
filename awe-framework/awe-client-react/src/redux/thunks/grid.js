import {
  generateMessageAction,
  getActionAddress,
  getComponent,
  getGridAndValues,
  translateLabel
} from "../../utilities";
import { acceptAction, addActionsTop, rejectAction } from "../actions/actions";
import { updateAttributes, updateSpecificAttributes } from "../actions/components";
import { updateModelWithDependencies } from "./components";
import { compareEqualValues } from "../../utilities/general";
import { extractCellValue, getGridIdentifier, getRow, OperationType, RowPositionType } from "../../utilities/grid";
import { validateRow } from "./validate";
import { getAllComponents } from "../selectors/componentSelectors";
const { BEFORE, AFTER, FIRST, LAST, CHILD } = RowPositionType;
const { INSERT, UPDATE, DELETE } = OperationType;
let addedRows = 0;
let copiedRows = 0;

const addRowToValues = (rowId, identifier, values, position, newRow) => {
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

export const toggleColumnVisibilityGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { columns = [], show } = action.parameters;
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

export const selectRowGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { values = [] } = action.parameters;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      selected: values
    }));
  };
};

export const selectFirstRowGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row, index) => ({ ...row, selected: index === 0 }))
    }));
  };
};

export const selectLastRowGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row, index) => ({ ...row, selected: index === values.length - 1 }))
    }));
  };
};

export const selectAllRowsGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row) => ({ ...row, selected: true }))
    }));
  };
};

export const unselectAllRowsGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;

    dispatch(acceptAction(action));

    dispatch(updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row) => ({ ...row, selected: false }))
    }));
  };
};

export const checkOneSelectedGridAction = (action, t) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;

    if (values.filter((row) => row.selected).length === 1) {
      dispatch(acceptAction(action));
    } else {
      dispatch(rejectAction(action));
      dispatch(addActionsTop([generateMessageAction("warning", translateLabel('GRID_CHECK_ONE_SELECTED_TITLE', t), translateLabel('GRID_CHECK_ONE_SELECTED_MESSAGE', t))]));
    }
  };
};

export const checkSomeSelectedGridAction = (action, t) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;

    if (values.filter((row) => row.selected).length > 0) {
      dispatch(acceptAction(action));
    } else {
      dispatch(rejectAction(action));
      dispatch(addActionsTop([generateMessageAction("warning", translateLabel('GRID_CHECK_SOME_SELECTED_TITLE', t), translateLabel('GRID_CHECK_SOME_SELECTED_MESSAGE', t))]));
    }
  };
};

export const checkRecordsSavedGridAction = (action, t) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
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

export const checkRecordsGeneratedGridAction = (action, t) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
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

export const deleteRowGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { multioperation } = component.attributes || {};
    const { values } = component.model;
    const { rowId } = action.parameters;
    const gridId = getGridIdentifier(component.attributes);
    const selectedRow = values.find((row) => (rowId ? String(row[gridId]) === String(rowId) : row.selected));

    dispatch(acceptAction(action));

    let deleteRow = true;
    if (multioperation) {
      const { operation } = selectedRow.$row || {};
      if (operation !== INSERT) {
        deleteRow = false;
        dispatch(updateModelWithDependencies(address, {
          values: values.map((item) => ({
            ...item,
            $row: {
              ...(item.$row || {}),
              operation: compareEqualValues(item[gridId], selectedRow[gridId]) ? DELETE : item?.$row?.operation
            }
          })),
          event: "after-delete-row"
        }));
      }
    }

    if (deleteRow) {
      let filteredValues = values.filter((item) => !compareEqualValues(item[gridId], selectedRow[gridId]));
      dispatch(updateModelWithDependencies(address, { values: filteredValues, records: filteredValues.length }));
    }
  };
};

/**
 * Verify if there is a row being edited and if it has passed validation
 * @param dispatch Dispatch function
 * @param getState Get state function
 * @param action Action
 * @returns {boolean} No row is being edited or has passed validation
 */
function checkEditingRow(dispatch, getState, action) {
  const components = getAllComponents(getState());
  const address = getActionAddress(action);
  const component = getComponent(components, address);
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
    dispatch(validateRow({ ...address, row: editingRowId }));

    // Verificar si la validación fue exitosa
    const validatedRow = getComponent(getAllComponents(getState()), address).model.values.find(row => row.$row?.editing);

    if (errorsInValidatedRow(validatedRow)) {
      // Si hay errores de validación, rechazar la acción
      dispatch(rejectAction(action));
      return false;
    }
  }

  return true;
}

function errorsInValidatedRow(validatedRow) {
  const cellErrors = Object.values(validatedRow.$attrs || {}).filter(cell => cell?.error);
  return cellErrors.length > 0;
}

export const addRowGridAction = (action, position) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { editable, multioperation, treegrid, treeParent, columnModel = [] } = component.attributes || {};
    const { values } = component.model;

    // Verify if editing row
    if (!checkEditingRow(dispatch, getState, action)) return;

    const { rowId, row = {} } = action.parameters;
    const gridId = getGridIdentifier(component.attributes);
    const selectedRow = values.find((item) => (rowId ? String(item[gridId]) === String(rowId) : item.selected)) || {};
    const parentId = position !== CHILD ? selectedRow[treeParent] || "" : selectedRow[gridId] || "";
    const rowDefaultValues = columnModel.reduce((prev, current) => ({
      ...prev,
      [current.name]: current.value || null
    }), {});

    dispatch(updateModelWithDependencies(address, { event: "add-row" }));
    dispatch(acceptAction(action));

    addedRows++;

    const newRow = {
      [gridId]: `new-row-${addedRows}`,
      ...rowDefaultValues,
      ...(treegrid ? { [treeParent]: parentId } : {}),
      ...row,
      $row: {
        ...row?.$row,
        ...(multioperation ? { operation: INSERT } : {}),
        ...(editable || multioperation ? { editing: false } : {})
      }
    };

    dispatch(updateModelWithDependencies(address, {
      records: values.length + 1,
      values: addRowToValues(selectedRow[gridId], gridId, values, position, {
        ...newRow,
        $row: {
          ...newRow.$row,
          ...((editable || multioperation) ? { editing: true } : {}),
          newRow
        }
      }),
      event: "after-add-row"
    }));
    addedRows++;
  };
};

export const updateRowGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;
    const { rowId, row } = action.parameters;
    const gridId = getGridIdentifier(component.attributes);
    const updatedValues = values.map((item) =>
      String(item[gridId]) === String(rowId) ? { ...item, ...row } : item
    );

    dispatch(acceptAction(action));
    dispatch(updateModelWithDependencies(address, { values: updatedValues }));
  };
};

export const copyRowGridAction = (action, position) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;
    const row = values.find(item => item.selected);
    copiedRows++;
    dispatch(addRowGridAction({
      ...action,
      parameters: { ...action.parameters, row: { ...row, id: `copied-row-${copiedRows}` } }
    }, position));
  };
};

export const addColumnsGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { columns } = action.parameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateAttributes(address, { columnModel: [...(component.attributes?.columnModel || []), ...columns] }));
  };
};

export const replaceColumnsGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { columns } = action.parameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateAttributes(address, { columnModel: [...columns] }));
  };
};

export const updateCellGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { data } = action.parameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update selected rows
    dispatch(updateModelWithDependencies(address, { values: data }));
  };
};

export const showColumnsGridAction = (action) => {
  return (dispatch, getState) => {
    dispatch(toggleColumnVisibilityGridAction({ ...action, parameters: { ...action.parameters, show: true } }));
  };
};

export const hideColumnsGridAction = (action) => {
  return (dispatch, getState) => {
    dispatch(toggleColumnVisibilityGridAction({ ...action, parameters: { ...action.parameters, show: false } }));
  };
};

export const changeColumnLabelGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { column, label } = action.parameters;
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

export const editRowGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;
    const { row } = action.parameters;
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

export const saveRowGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const components = getAllComponents(getState());
    const { component, values } = getGridAndValues(components, address);
    const editingRow = values.find(row => row.$row?.editing) || {};
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
    const { values: newValues } = getGridAndValues(updatedComponents, address);
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

export const cancelRowGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const components = getAllComponents(getState());
    const { component, values } = getGridAndValues(components, address);
    const editingRow = values.find(row => row.$row?.editing)?.$row?.editingRow || {};
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
    }
  };
};


export const changePageGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { page, first, rows, max } = action.parameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update page
    dispatch(updateSpecificAttributes(address, { page, first, rows, max }));
  };
};

export const changeSortGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const {sort} = action.parameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update sort
    dispatch(updateSpecificAttributes(address, {sort}));
  };
};

export const changeFilterGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { filters } = action.parameters;

    // Accept action
    dispatch(acceptAction(action));

    // Update filters
    dispatch(updateSpecificAttributes(address, { filters }));
  };
};

export const copySelectedRowsToClipboardGridAction = (action, t) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { values } = component.model;
    const { columnModel } = component.attributes;

    // Accept the action
    dispatch(acceptAction(action));

    let clipboardHeaders = columnModel.map(column => translateLabel(column.label, t)).join("\t") + "\n";

    let clipboardData = values
      .filter(row => row.selected)
      .map(row => columnModel
        .map(column => extractCellValue(row[column.name] || ""))
        .join("\t")
      )
      .join("\n");

    // Get selected lines values and store them into the clipboard
    navigator.clipboard.writeText(clipboardHeaders + clipboardData)
      .catch((reason) => console.error("Error copying the selected rows to the clipboard:", reason));
  };
};

export const validateCurrentRowGridAction = (action) => {
  return (dispatch, getState) => {
    // Validate all selected components
    const address = getActionAddress(action);
    dispatch(validateRow(address));

    // Accept the action
    dispatch(acceptAction(action));
  };
};

export const verifyRowValidationGridAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, { component: address.component, view: address.view });
    // Check if validation has been successful
    if (errorsInValidatedRow(getRow(component, address.row))) {
      // If there are errors, reject action
      dispatch(rejectAction(action));
    } else {
      // If is ok, accept the action
      dispatch(acceptAction(action));
    }
  };
};

export const toggleBranchGridAction = (action) => {
  return (dispatch, getState) => {
    const address = getActionAddress(action);
    const { rows = [] } = action.parameters?.datalist || {};
    const components = getAllComponents(getState());
    const { component, values } = getGridAndValues(components, address);
    const { attributes } = component;
    const { loadAll } = attributes;
    const { row } = action.parameters;
    const gridId = getGridIdentifier(component.attributes);

    // Accept action
    dispatch(acceptAction(action));

    // If not loadAll, add the loaded rows
    let treeValues = values;
    if (!loadAll && rows.length > 0) {
      let rowIndex = treeValues.findIndex(item => String(item[gridId]) === String(row));
      treeValues = [
        ...treeValues.slice(0, rowIndex),
        ...rows,
        ...treeValues.slice(rowIndex, treeValues.length)
      ];
    }

    // Update selected rows
    dispatch(updateModelWithDependencies(address, {
      event: "toggle-row",
      values: treeValues
        .map(item => String(item[gridId]) === String(row) ?
          ({ ...item, $row: { ...item.$row, expanded: !item.$row?.expanded, loaded: true } }) : ({ ...item }))
    }));
  };
};
