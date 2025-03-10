import React, {Component} from "react";
import {Ripple} from "primereact/ripple";

class ColumnRowEditor extends Component {

  /**
   * Create a grid
   * @param {object} props Grid properties
   */
  constructor(props) {
    super(props);
  }

  render() {
    const {rowData, editRow, saveRow, cancelRow} = this.props;
    const {editing} = (rowData.$row || {});
    if (editing) {
      return <>
        <button type="button" role="save-edit-row" onClick={saveRow} className="p-row-editor-save p-link">
          <span className="p-row-editor-save-icon pi pi-fw pi-check p-clickable"/>
          <Ripple/>
        </button>
        <button type="button" role="cancel-edit-row" onClick={cancelRow} className="p-row-editor-cancel p-link">
          <span className="p-row-editor-cancel-icon pi pi-fw pi-times p-clickable"/>
          <Ripple/>
        </button>
      </>;
    } else {
      return <button type="button" role="edit-row" onClick={() => editRow(rowData.id)} className="p-row-editor-init p-link">
        <span className="p-row-editor-init-icon pi pi-fw pi-pencil p-clickable"/>
        <Ripple/>
      </button>;
    }
  }
}

export default ColumnRowEditor;
