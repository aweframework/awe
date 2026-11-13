import React from "react";
import {Ripple} from "primereact/ripple";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";
import {getIconNode} from "../utilities";

function ColumnRowEditor({rowData, editRow, saveRow, cancelRow}) {
  const { editing } = (rowData.$row || {});
  if (editing) {
    return <>
      <button type="button" role="save-edit-row" onClick={saveRow} className="p-row-editor-save p-link" {...testHook(TestIds.gridRowSave)}>
        {getIconNode("pi-check", {tag: "span", className: "p-row-editor-save-icon p-clickable"})}
        <Ripple/>
      </button>
      <button type="button" role="cancel-edit-row" onClick={cancelRow} className="p-row-editor-cancel p-link" {...testHook(TestIds.gridRowCancel)}>
        {getIconNode("pi-times", {tag: "span", className: "p-row-editor-cancel-icon p-clickable"})}
        <Ripple/>
      </button>
    </>;
  }
  return <button type="button" role="edit-row" onClick={() => editRow(rowData.id)} className="p-row-editor-init p-link" {...testHook(TestIds.gridRowEdit)}>
    {getIconNode("pi-pencil", {tag: "span", className: "p-row-editor-init-icon p-clickable"})}
    <Ripple/>
  </button>;
}

ColumnRowEditor.propTypes = {
  editRow: PropTypes.func.isRequired,
  saveRow: PropTypes.func.isRequired,
  cancelRow: PropTypes.func.isRequired,
  rowData: PropTypes.object.isRequired
};

export default ColumnRowEditor;
