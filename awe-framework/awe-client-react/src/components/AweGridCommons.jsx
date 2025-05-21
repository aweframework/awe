import React from "react";
import {Column} from "primereact/column";
import {AweComponent} from "./AweComponent";
import {Columns} from "../utilities/structure";
import ColumnRowEditor from "../columns/ColumnRowEditor";
import {bindMethods, getIconCode, translateLabel} from "../utilities";
import {calculateFooterValue, getGridIdentifier, getWidthStyle, OperationIcon} from "../utilities/grid";
import "./AweGridCommons.less";
import AweButton from "./AweButton";
import {ContextMenu} from "primereact/contextmenu";
import {isEmpty} from "../utilities/general";

/**
 * Map contextMenu
 * @param {object[]} contextMenu Context menu buttons
 * @param {object} props Properties
 * @return {object[]} Context menu in primereact format
 */
function mapContextMenu(contextMenu, props) {
  const {t, addActionsTop, address, components} = props;
  return (contextMenu || [])
    .map(option => components[option.id].attributes)
    .map(option => ({
      label: t(option.label),
      icon: getIconCode(option.icon, "p-menuitem-icon"),
      disabled: option.disabled,
      visible: option.visible,
      separator: option.separator,
      command: () => addActionsTop((option.actions || []).map(action => ({...action, address}))),
      ...(mapContextMenu(option.contextMenu, props).length > 0 ? {items: mapContextMenu(option.contextMenu, props)} : {}),
    }));
}

/**
 * AWE Grid component
 * @extends AweComponent
 * @category Components
 * @subcategory Grid
 */
export class AweGridCommons extends AweComponent {

  /**
   * Create a grid
   * @param {object} props Grid properties
   */
  constructor(props) {
    super(props);

    bindMethods(this, ["onSelect", "onContextMenu", "editRow", "saveRow", "cancelRow", "filterRow",
      "getHeader", "columnTemplate", "headerColumnTemplate", "editorColumnTemplate", "cellTemplate",
      "multiselectColumnTemplate", "rowNumberColumnTemplate", "footerColumnTemplate", "preColumnTemplates",
      "postColumnTemplates", "contextMenuTemplate"
    ]);
  }

  editRow(row) {
    const {address, addActionsTop} = this.props;
    addActionsTop([{type: "edit-row", address, parameters:{row}}]);
  }

  saveRow() {
    const {address, addActionsTop, attributes, model} = this.props;
    const {validateOnSave = true} = attributes;
    const {values} = model;
    const editingRow = values.find(row => row.$row?.editing);
    const gridId = getGridIdentifier(attributes);
    let actions = [];
    if (validateOnSave) {
      actions = [{type: "validate-row", address: {...address, row: editingRow[gridId]}}];
    }
    addActionsTop([...actions, {type: "save-row", address}]);
  }

  cancelRow() {
    const {address, addActionsTop} = this.props;
    addActionsTop([{type: "cancel-row", address}]);
  }

  filterRow(action) {
    const {address, addActionsTop, attributes, model} = this.props;
    const {loadAll} = attributes;
    const {values} = model;
    const editingRow = values.find(row => row.$row?.editing);
    const cancelRowAction = editingRow ? [{type:"cancel-row", address}] : [];
    if (!loadAll) {
      // Filter grid
      addActionsTop([...cancelRowAction, action, {type: "filter", address}]);
    } else {
      addActionsTop([...cancelRowAction, action]);
    }
  }

  onSelect(event) {
    const {address, addActionsTop, attributes} = this.props;
    const gridId = getGridIdentifier(attributes);
    const values = [event.value].flat().filter(i => !isEmpty(i)).map(i => i[gridId]);
    addActionsTop([{type: "select-row", address, parameters:{values}}]);
  }

  onContextMenu(data) {
    if (this.cm != null) {
      this.cm.show(data.originalEvent);
    }
  }

  contextMenuTemplate() {
    const {t, addActionsTop, address, components, attributes} = this.props;
    const {contextMenu = []} = attributes;
    let contextMenuMapped = mapContextMenu(contextMenu, {t, addActionsTop, address, components});
    if (contextMenuMapped.length > 0) {
      return <ContextMenu model={contextMenuMapped} ref={(el) => this.cm = el} breakpoint="767px" />;
    }
    return null;
  }

  preColumnTemplates(place, rowSpan) {
    return [this.multiselectColumnTemplate(place, rowSpan), this.rowNumberColumnTemplate(place, rowSpan)].filter(e => !isEmpty(e));
  }

  postColumnTemplates(place, rowSpan, forBody = false) {
    return [
      this.operationColumnTemplate(place, rowSpan),
      this.editorColumnTemplate(place, forBody, rowSpan),
      <Column key={`filler-${place}`} field={`filler-${place}`} rowSpan={rowSpan}/>
    ].filter(e => !isEmpty(e));
  }

  rowNumberColumnTemplate(place, rowSpan) {
    const {attributes, specificAttributes} = this.props;
    const {rowNumbers} = attributes;
    const {first, rows} = specificAttributes;
    let maxWidth = String(first + rows).length - 2;
    let rowNumberStyle = {fontWeight: "bold", textAlign: "center", ...getWidthStyle(maxWidth)};
    if (rowNumbers) {
      return <Column key={`row-number-${place}`} field={`row-number-${place}`} header={"#"} rowSpan={rowSpan}
                     body={(_data, options) => options.rowIndex + 1}
                     headerClassName={"p-row-number-header"} headerStyle={rowNumberStyle}
                     bodyClassName={"p-row-number-cell"} bodyStyle={rowNumberStyle}
                     footer={null}
                     footerClassName={"p-row-number-footer"}
                     footerStyle={rowNumberStyle}
      />;
    }
    return null;
  }

  multiselectColumnTemplate(place, rowSpan) {
    const {attributes} = this.props;
    const {multiselect} = attributes;
    return multiselect ? <Column
      key={`multiselect-${place}`}
      field={`multiselect-${place}`}
      selectionMode="multiple"
      rowSpan={rowSpan}
      footer={null}
      style={{textAlign: "center", ...getWidthStyle(null, null, '40px')}}
    /> : null;
  }

  operationColumnTemplate(place, rowSpan) {
    const {attributes} = this.props;
    const {multioperation} = attributes;
    return multioperation ? <Column key={`operation-${place}`} field={`operation-${place}`} rowSpan={rowSpan}
                                    headerStyle={getWidthStyle(null, null, '32px')}
                                    bodyStyle={getWidthStyle(null, null, '32px')}
                                    style={{textAlign: "center"}}
                                    footer={null}
                                    body={rowData => <i role={rowData?.$row?.operation}
                                                        className={OperationIcon[rowData?.$row?.operation]}/>}/> : null;
  }

  columnTemplate(col, rowSpan) {
    const {t, attributes} = this.props;
    const {enableFilters = false} = attributes;
    const {name, sortField, label, charlength, width, sortable} = col;

    return <Column key={name} field={sortField || name} header={translateLabel(label, t)}
                   style={{textAlign: "center", ...getWidthStyle(charlength, width)}}
                   sortable={sortable} rowSpan={rowSpan} filter={enableFilters}/>;
  }

  headerColumnTemplate(col) {
    const {t} = this.props;
    const {startColumnName, label, numberOfColumns} = col;
    return <Column
      key={startColumnName}
      field={startColumnName}
      header={translateLabel(label, t)}
      style={{textAlign: "center"}}
      colSpan={numberOfColumns}
    />;
  }

  editorColumnTemplate(place, forBody, rowSpan) {
    const {attributes} = this.props;
    const {editable, multioperation} = attributes;
    if (editable || multioperation) {
      if (forBody) {
        return <Column
          key={`${place}-editor`}
          field={`${place}-editor`}
          rowSpan={rowSpan}
          body={(data, row) => <ColumnRowEditor
            key={`${row.rowIndex}-editor`}
            row={row}
            rowData={data}
            editRow={this.editRow}
            saveRow={this.saveRow}
            cancelRow={this.cancelRow}/>}
          footer={null}
          style={getWidthStyle(null, 50)}
          bodyStyle={{textAlign: "center", ...getWidthStyle(null, 50)}}/>;
      } else {
        return <Column key={`${place}-editor`} field={`${place}-editor`} style={getWidthStyle(null, 50)} rowSpan={rowSpan}/>;
      }
    } else {
      return null;
    }
  }

  getHeader() {
    const {attributes} = this.props;
    const {headerModel, columnModel} = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    let headerColumns = [];
    let groupedColumns = [];
    let index = 0;
    headerModel.forEach(header => {
      let headerStartIndex = visibleColumns.findIndex(column => column.name === header.startColumnName);
      headerColumns = headerColumns.concat(visibleColumns.slice(index, headerStartIndex));
      headerColumns.push(header);
      let headerSubColumns = visibleColumns.slice(headerStartIndex, headerStartIndex + header.numberOfColumns);
      groupedColumns = groupedColumns.concat(headerSubColumns);
      index = headerStartIndex + header.numberOfColumns;
    });
    return {
      columns: headerColumns.concat(visibleColumns.slice(index, visibleColumns.length)),
      grouped: groupedColumns
    };
  }

  footerColumnTemplate(column) {
    const {model} = this.props;
    const {values} = model;
    const {charlength, width, align} = column;
    return <Column key={`${column.name}-footer`} field={`${column.name}-footer`} footerClassName={"p-column-footer"}
                   footerStyle={{textAlign: align, ...getWidthStyle(charlength, width)}}
                   footer={calculateFooterValue(column, values)?.label}/>;
  }

  cellTemplate(rowData, column) {
    const {t, settings, attributes, updateModelWithDependencies, updateAttributes, addActionsTop, address, components} = this.props;
    const gridId = getGridIdentifier(attributes);
    return Columns({
          ...attributes.columnModel.find(c => c.name === column),
          updateModelWithDependencies,
          updateAttributes,
          addActionsTop,
          address: {...address, column, row: rowData[gridId]},
          t, settings, components
        }, rowData[column], (rowData.$row || {}).editing);
  }

  buttonsTemplate() {
    const {attributes} = this.props;
    const {buttonModel} = attributes;
    return buttonModel.map((button, index) => React.createElement(AweButton, {
      ...button, key: button.id || `component-${index}`
    }));
  }
}
