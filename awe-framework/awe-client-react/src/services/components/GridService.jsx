import {generateMessageAction, getActionAddress, getComponent} from "../../utilities";
import {extractCellValue, getGridIdentifier, getRow, OperationType, RowPositionType} from "../../utilities/grid";

const {BEFORE, AFTER, FIRST, LAST, CHILD} = RowPositionType;
const {INSERT, UPDATE, DELETE} = OperationType;

function addRowToValues(rowId, identifier, values, position, newRow) {
  let rowIndex = values.findIndex(row => String(row[identifier]) === String(rowId));
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
      .map(row => ({...row, selected: false, $row: {...row?.$row || {}, editing: false}})),
    {...newRow, selected: true},
    ...values.slice(rowIndex, values.length)
      .map(row => ({...row, selected: false, $row: {...row?.$row || {}, editing: false}}))
  ];
}

/**
 * Grid service
 * @category Services
 * @subcategory Grid
 */
class GridService {
  addedRows = 0;
  copiedRows = 0;

  getActions() {
    return {
      // Grid
      "toggle-columns-visibility": this.onGridColumnVisibilityToggle.bind(this),
      "select-row": this.onGridSelectRow.bind(this),
      "select-first-row": this.onGridSelectFirstRow.bind(this),
      "select-last-row": this.onGridSelectLastRow.bind(this),
      "select-all-rows": this.onGridSelectAllRows.bind(this),
      "unselect-all-rows": this.onGridUnselectAllRows.bind(this),
      "check-one-selected": this.onGridCheckOneSelected.bind(this),
      "check-some-selected": this.onGridCheckSomeSelected.bind(this),
      "check-records-generated": this.onGridCheckRecordsGenerated.bind(this),
      "check-records-saved": this.onGridCheckRecordsSaved.bind(this),
      "delete-row": this.onGridDeleteRow.bind(this),
      "add-row": this.onGridAddRow.bind(this),
      "add-row-top": this.onGridAddRowFirst.bind(this),
      "add-row-bottom": this.onGridAddRowLast.bind(this),
      "add-row-down": this.onGridAddRowAfter.bind(this),
      "add-row-up": this.onGridAddRowBefore.bind(this),
      "update-row": this.onGridUpdateRow.bind(this),
      "copy-row": this.onGridCopyRow.bind(this),
      "copy-row-top": this.onGridCopyRowFirst.bind(this),
      "copy-row-bottom": this.onGridCopyRowLast.bind(this),
      "copy-row-down": this.onGridCopyRowAfter.bind(this),
      "copy-row-up": this.onGridCopyRowBefore.bind(this),
      "add-columns": this.onGridAddColumns.bind(this),
      "replace-columns": this.onGridReplaceColumns.bind(this),
      "update-cell": this.onGridUpdateCell.bind(this),
      "show-columns": this.onGridShowColumns.bind(this),
      "hide-columns": this.onGridHideColumns.bind(this),
      "change-column-label": this.onGridChangeColumnLabel.bind(this),
      "save-row": this.onGridSaveRow.bind(this),
      "cancel-row": this.onGridCancelRow.bind(this),
      "edit-row": this.onGridEditRow.bind(this),
      "change-page": this.onGridChangePage.bind(this),
      "change-sort": this.onGridChangeSort.bind(this),
      "change-filter": this.onGridChangeFilter.bind(this),
      "copy-selected-rows-clipboard": this.copySelectedRowsToClipboard.bind(this),
      "validate-row": this.validateRow.bind(this),
      "verify-row-validation": this.verifyRowValidation.bind(this),
      "tree-branch": this.onBranchToggle.bind(this)
    };
  }

  onGridSelectRow(action, props) {
    const address = getActionAddress(action);
    const {values = []} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "select-row",
      selected: values
    });
  }

  onGridSelectFirstRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row, index) => ({...row, selected: (index === 0)}))
    });
  }

  onGridSelectLastRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map((row, index) => ({
        ...row, selected: (index === values.length - 1)
      }))
    });
  }

  onGridSelectAllRows(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map(row => ({
        ...row,
        selected: true
      }))
    });
  }

  onGridUnselectAllRows(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "select-row",
      values: values.map(row => ({
        ...row,
        selected: false
      }))
    });
  }

  onGridCheckOneSelected(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {t} = props;
    const {values} = component.model;

    if (values.filter(row => row.selected).length === 1) {
      // Accept action
      props.acceptAction(action);
    } else {
      // Reject action
      props.rejectAction(action);

      // Send message
      props.addActionsTop([generateMessageAction("warning", t('GRID_CHECK_ONE_SELECTED_TITLE'), t('GRID_CHECK_ONE_SELECTED_MESSAGE'))]);
    }
  }

  onGridCheckSomeSelected(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {t} = props;
    const {values} = component.model;

    if (values.filter(row => row.selected).length > 0) {
      // Accept action
      props.acceptAction(action);
    } else {
      // Reject action
      props.rejectAction(action);

      // Send message
      props.addActionsTop([generateMessageAction("warning", t('GRID_CHECK_SOME_SELECTED_TITLE'), t('GRID_CHECK_SOME_SELECTED_MESSAGE'))]);
    }
  }

  /**
   * Remove row UNLESS it is a multioperation grid, in which case row will be marked as deletable
   * @param action
   * @param props
   */
  onGridDeleteRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {multioperation} = component.attributes || {};
    const {values} = component.model;
    const {rowId} = action.parameters;
    const gridId = getGridIdentifier(component.attributes);
    const selectedRow = values.find(row => rowId ? String(row[gridId]) === String(rowId) : row.selected);

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    let deleteRow = true;
    if (multioperation) {
      const {operation} = (selectedRow.$row || {});
      // If row is new, delete it
      if (operation !== INSERT) {
        deleteRow = false;
        props.updateModelWithDependencies(address, {
          values: values.map(item => ({
            ...item,
            $row: {
              ...(item.$row || {}),
              operation: String(item[gridId]) === String(selectedRow[gridId]) ? DELETE : (item.$row || {}).operation
            }
          })),
          event: "after-delete-row"
        });
      }
    }

    if (deleteRow) {
      let filteredValues = values.filter(item => String(item[gridId]) !== String(selectedRow[gridId]));
      props.updateModelWithDependencies(address, {values: filteredValues, records: filteredValues.length});
    }
  }

  addRow(action, props, position) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {editable, multioperation, treegrid, treeParent, columnModel = []} = component.attributes || {};
    const {values} = component.model;
    const {rowId, row} = action.parameters;
    const gridId = getGridIdentifier(component.attributes);
    const selectedRow = values.find(item => rowId ? String(item[gridId]) === String(rowId) : item.selected) || {};
    const parentId = position !== CHILD ? selectedRow[treeParent] || "" : selectedRow[gridId] || "";
    const rowDefaultValues = columnModel.reduce((prev, current) => ({...prev, [current.name]: current.value || null}), {});

    // Set add-row event
    props.updateModelWithDependencies(address, {
      event: "add-row"
    });

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    const editingRow = {
      [gridId]: `new-row-${++this.addedRows}`,
      ...rowDefaultValues,
      ...(treegrid ? {[treeParent]: parentId} : {}),
      ...(row || {}),
      $row: {
        ...(row?.$row || {}),
        ...(multioperation ? {operation: INSERT} : {}),
        ...(editable || multioperation ? {editing: false} : {})
      }
    };
    props.updateModelWithDependencies(address, {
      records: values.length + 1,
      values: addRowToValues(selectedRow[gridId], gridId, values, position, {
        ...editingRow,
        $row: {
          ...editingRow.$row,
          ...(editable || multioperation ? {editing: true} : {}),
          editingRow
        }
      }),
      event: "after-add-row"
    });
  }

  copyRow(action, props, position) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;
    const row = values.find(item => item.selected);
    this.addRow({
      ...action,
      parameters: {...action.parameters, row: {...row, id: `copied-row-${++this.copiedRows}`}}
    }, props, position);
  }

  onGridAddRow(action, props) {
    this.addRow(action, props, CHILD);
  }

  onGridAddRowFirst(action, props) {
    this.addRow(action, props, FIRST);
  }

  onGridAddRowLast(action, props) {
    this.addRow(action, props, LAST);
  }

  onGridAddRowAfter(action, props) {
    this.addRow(action, props, AFTER);
  }

  onGridAddRowBefore(action, props) {
    this.addRow(action, props, BEFORE);
  }

  onGridUpdateRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {multioperation} = component.attributes || {};
    const {values} = component.model;
    const {row} = action.parameters;
    const gridId = getGridIdentifier(component.attributes);

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      values: values.map(item => ({
        ...item,
        ...(String(item[gridId]) === String(row[gridId]) ? row : {}),
        $row: {
          ...((row || {}).$row || {}),
          ...(multioperation ? {operation: UPDATE} : {})
        }
      })),
      event: "after-edit-row"
    });
  }

  onGridCopyRow(action, props) {
    this.copyRow(action, props, CHILD);
  }

  onGridCopyRowFirst(action, props) {
    this.copyRow(action, props, FIRST);
  }

  onGridCopyRowLast(action, props) {
    this.copyRow(action, props, LAST);
  }

  onGridCopyRowAfter(action, props) {
    this.copyRow(action, props, AFTER);
  }

  onGridCopyRowBefore(action, props) {
    this.copyRow(action, props, BEFORE);
  }

  onGridAddColumns(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {columns} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateAttributes(address, {columnModel: [...component.attributes.columnModel, ...columns]});
  }

  onGridReplaceColumns(action, props) {
    const address = getActionAddress(action);
    const {columns} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateAttributes(address, {columnModel: [...columns]});
  }

  onGridUpdateCell(action, props) {
    const address = getActionAddress(action);
    const {data} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {values: data});
  }

  onGridShowColumns(action, props) {
    this.onGridColumnVisibilityToggle({...action, parameters: {...action.parameters, show: true}}, props);
  }

  onGridHideColumns(action, props) {
    this.onGridColumnVisibilityToggle({...action, parameters: {...action.parameters, show: false}}, props);
  }

  onGridColumnVisibilityToggle(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {columns = [], show} = action.parameters;
    const {columnModel} = component.attributes;

    // Accept action
    props.acceptAction(action);

    // Update attributes
    props.updateAttributes(address, {
      columnModel: columnModel.map(col => ({
        ...col,
        hidden: columns.includes(col.name) ? !show : col.hidden
      }))
    });
  }

  onGridChangeColumnLabel(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {column, label} = action.parameters;
    const {columnModel} = component.attributes;

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateAttributes(address, {
      columnModel: columnModel.map(col => ({
        ...col,
        label: col.name === column ? label : col.label
      }))
    });
  }

  onGridEditRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;
    const {row} = action.parameters;
    const gridId = getGridIdentifier(component.attributes);

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "edit-row",
      values: values.map(item => ({
        ...item,
        $row: {
          ...(item.$row || {}),
          editing: String(item[gridId]) === String(row),
          ...(String(item[gridId]) === String(row) ? {editingRow: item.$row?.editingRow || {...item}} : {})
        }
      }))
    });
  }

  onGridSaveRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;
    const editingRow = values.find(row => row.$row?.editing) || {};
    const gridId = getGridIdentifier(component.attributes);

    // Accept action
    props.acceptAction(action);

    // Update save row event
    props.updateModelWithDependencies(address, {
      event: "save-row",
      values: values.map(item => ({
        ...item,
        $row: {
          ...(item.$row || {}),
          operation: String(item[gridId]) === String(editingRow[gridId]) ? (item.$row || {}).operation || UPDATE : (item.$row || {}).operation
        }
      }))
    });

    // Remove save row event
    props.afterSaveRow(address, {});
  }

  onGridCancelRow(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {values} = component.model;
    const editingRow = values.find(row => row.$row?.editing)?.$row?.editingRow || {};
    const gridId = getGridIdentifier(component.attributes);

    // Accept action
    props.acceptAction(action);

    // Update selected rows
    if (editingRow) {
      props.updateModelWithDependencies(address, {
        event: "cancel-row",
        values: values.map(item => String(item[gridId]) === String(editingRow[gridId]) ? ({
          ...editingRow,
          selected: false
        }) : ({...item}))
      });
    }
  }

  onGridChangePage(action, props) {
    const address = getActionAddress(action);
    const {page, first, rows, max} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update page
    props.updateSpecificAttributes(address, {page, first, rows, max});
  }

  onGridChangeSort(action, props) {
    const address = getActionAddress(action);
    const {sort} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update sort
    props.updateSpecificAttributes(address, {sort});
  }

  onGridChangeFilter(action, props) {
    const address = getActionAddress(action);
    const {filters} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Update filters
    props.updateSpecificAttributes(address, {filters});
  }

  onGridCheckRecordsSaved(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {t} = props;
    const {values} = component.model;

    if (values.filter(row => row.$row?.editing).length === 0) {
      // Accept action
      props.acceptAction(action);
    } else {
      // Reject action
      props.rejectAction(action);

      // Send message
      props.addActionsTop([generateMessageAction("warning", t('GRID_CHECK_ALL_SAVED_TITLE'), t('GRID_CHECK_ALL_SAVED_MESSAGE'))]);
    }
  }

  onGridCheckRecordsGenerated(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {t} = props;
    const {values} = component.model;

    if (values.filter(row => row.$row?.operation).length > 0) {
      // Accept action
      props.acceptAction(action);
    } else {
      // Reject action
      props.rejectAction(action);

      // Send message
      props.addActionsTop([generateMessageAction("warning", t('GRID_CHECK_RECORDS_GENERATED_TITLE'), t('GRID_CHECK_RECORDS_GENERATED_MESSAGE'))]);
    }
  }

  /**
   * Copy selected rows to the clipboard in csv format
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  copySelectedRowsToClipboard(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {t} = props;
    const {values} = component.model;
    const {columnModel} = component.attributes;

    // Accept the action
    props.acceptAction(action);

    let clipboardHeaders = columnModel.map(column => t(column.label)).join("\t") + "\n";

    let clipboardData = values
      .filter(row => row.selected)
      .map(row => columnModel
        .map(column => extractCellValue(row[column.name] || ""))
        .join("\t")
      )
      .join("\n");

    // Get selected lines values and store them into the clipboard
    navigator.clipboard.writeText(clipboardHeaders + clipboardData);
  }

  /**
   * Validate the form
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  validateRow(action, props) {
    // Validate all selected components
    const address = getActionAddress(action);
    props.validateRow(address);

    // Accept the action
    props.acceptAction(action);
  }

  /**
   * Verify a row validation
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  verifyRowValidation(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, {component: address.component, view: address.view});
    // Check if validation has been successful
    if (Object.values(getRow(component, address.row)).filter(cell => cell?.error).length) {
      // If there are errors, reject action
      props.rejectAction(action);
    } else {
      // If is ok, accept the action
      props.acceptAction(action);
    }
  }

  onBranchToggle(action, props) {
    const address = getActionAddress(action);
    const {rows = []} = action.parameters?.datalist || {};
    const component = getComponent(props.components, address);
    const {attributes, model} = component;
    const {values} = model;
    const {loadAll} = attributes;
    const {row} = action.parameters;
    const gridId= getGridIdentifier(component.attributes);

    // Accept action
    props.acceptAction(action);

    // If not loadAll, add the loaded rows
    let treeValues = values;
    if (!loadAll && rows.length > 0) {
      let rowIndex = treeValues.findIndex(item => String(item[gridId]) === String(row));
      treeValues = [
        ...treeValues.slice(0, rowIndex),
        ...rows,
        ...treeValues.slice(rowIndex, treeValues.length)
      ]
    }

    // Update selected rows
    props.updateModelWithDependencies(address, {
      event: "toggle-row",
      values: treeValues
        .map(item => String(item[gridId]) === String(row) ?
          ({...item, $row: {...item.$row, expanded: !item.$row?.expanded, loaded: true}}) : ({...item}))
    });
  }
}

export default GridService;
