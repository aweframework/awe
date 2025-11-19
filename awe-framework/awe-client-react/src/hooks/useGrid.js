import React, {useCallback, useMemo, useRef} from "react";
import {Column} from "primereact/column";
import {Columns} from "../utilities/structure";
import ColumnRowEditor from "../columns/ColumnRowEditor";
import {getIconCode, translateLabel} from "../utilities";
import {calculateFooterValue, getGridIdentifier, getWidthStyle, OperationIcon} from "../utilities/grid";
import {ContextMenu} from "primereact/contextmenu";
import {isEmpty} from "../utilities/general";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop as addActionsTopAction} from "../redux/actions/actions";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import {updateAttributes as updateAttributesAction} from "../redux/actions/components";
import {useTranslation} from "react-i18next";
import AweButton from "../components/AweButton";
import useComponent from "./useComponent";

function mapContextMenu(contextMenu, props) {
  const {t, addActionsTop, address, components} = props;
  return (contextMenu || [])
    .map(option => components[option.id].attributes)
    .map(option => ({
      label: translateLabel(option.label, t),
      icon: getIconCode(option.icon, "p-menuitem-icon"),
      disabled: option.disabled,
      visible: option.visible,
      separator: option.separator,
      command: () => addActionsTop((option.actions || []).map(action => ({...action, address}))),
      ...(mapContextMenu(option.contextMenu, props).length > 0 ? {items: mapContextMenu(option.contextMenu, props)} : {}),
    }));
}

/**
 * Hook providing shared grid helpers previously in AweGridCommons class.
 */
export function useGrid(id) {
  // Initialize as component
  const { address } = useComponent(id);

  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { attributes = {}, model = { values: [] }, specificAttributes = {}, settings = {} } = useSelector(state => ({
    attributes: state.components[id]?.attributes || {},
    model: state.components[id]?.model || { values: [] },
    specificAttributes: state.components[id]?.specificAttributes || {},
    settings: state.settings || {}
  }));
  const components = useSelector(state => state.components);
  const cmRef = useRef(null);

  const addActionsTop = useCallback((actions) => dispatch(addActionsTopAction(actions)), [dispatch]);
  const updateModelWithDependencies = useCallback((addr, payload) => dispatch(updateThunk(addr, payload)), [dispatch]);
  const updateAttributes = useCallback((addr, payload) => dispatch(updateAttributesAction(addr, payload)), [dispatch]);

  const validateRow = useCallback((postActions) => {
    const { validateOnSave = true } = attributes;
    const { values = [] } = model;
    const editingRow = values.find(row => row.$row?.editing);
    const gridId = getGridIdentifier(attributes);
    let actions = [];
    if (validateOnSave && editingRow) {
      actions = [{ type: "validate-row", address: { ...address, row: editingRow[gridId] } }];
    }
    addActionsTop([...actions, ...postActions]);
  }, [addActionsTop, address, attributes, model]);

  const editRow = useCallback((row) => {
    validateRow([{ type: "edit-row", address, parameters: { row } }]);
  }, [validateRow, address]);

  const saveRow = useCallback(() => {
    validateRow([{ type: "save-row", address }]);
  }, [validateRow, address]);

  const cancelRow = useCallback(() => {
    validateRow([{ type: "cancel-row", address }]);
  }, [validateRow, address]);

  const filterRow = useCallback((action) => {
    const { loadAll } = attributes;
    const { values = [] } = model;
    const editingRow = values.find(row => row.$row?.editing);
    const cancelRowAction = editingRow ? [{ type: "cancel-row", address }] : [];
    if (!loadAll) {
      validateRow([...cancelRowAction, action, { type: "filter", address }]);
    } else {
      validateRow([...cancelRowAction, action]);
    }
  }, [validateRow, address, attributes, model]);

  const onSelect = useCallback((event) => {
    const gridId = getGridIdentifier(attributes);
    const values = [event.value].flat().filter(i => !isEmpty(i)).map(i => i[gridId]);
    validateRow([{ type: "select-row", address, parameters: { values } }]);
  }, [validateRow, address, attributes]);

  const onContextMenu = useCallback((data) => {
    cmRef.current?.show(data.originalEvent);
  }, []);

  const contextMenuTemplate = useCallback(() => {
    const { contextMenu = [] } = attributes;
    let contextMenuMapped = mapContextMenu(contextMenu, { t, addActionsTop, address, components });
    if (contextMenuMapped.length > 0) {
      return <ContextMenu model={contextMenuMapped} ref={cmRef} breakpoint="767px" />;
    }
    return null;
  }, [attributes, t, addActionsTop, address, components]);

  const rowNumberColumnTemplate = useCallback((place, rowSpan, first, rows, rowNumbers) => {
    let maxWidth = String((first || 0) + (rows || 0)).length - 2;
    let rowNumberStyle = { fontWeight: "bold", textAlign: "center", ...getWidthStyle(maxWidth) };
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
  }, []);

  const multiselectColumnTemplate = useCallback((place, rowSpan, multiselect) => (
    multiselect ? <Column
      key={`multiselect-${place}`}
      field={`multiselect-${place}`}
      selectionMode="multiple"
      rowSpan={rowSpan}
      footer={null}
      style={{ textAlign: "center", ...getWidthStyle(null, null, '40px') }}
    /> : null
  ), []);

  const operationColumnTemplate = useCallback((place, rowSpan, multioperation) => (
    multioperation ? <Column key={`operation-${place}`} field={`operation-${place}`} rowSpan={rowSpan}
                             headerStyle={getWidthStyle(null, null, '32px')}
                             bodyStyle={getWidthStyle(null, null, '32px')}
                             style={{ textAlign: "center" }}
                             footer={null}
                             body={rowData => <i role={rowData?.$row?.operation}
                                                 className={OperationIcon[rowData?.$row?.operation]} />}/> : null
  ), []);

  const columnTemplate = useCallback((col, rowSpan, enableFilters) => {
    const { name, sortField, label, charlength, width, sortable } = col;
    return <Column key={name} field={sortField || name} header={translateLabel(label, t)}
                   style={{ textAlign: "center", ...getWidthStyle(charlength, width) }}
                   sortable={sortable} rowSpan={rowSpan} filter={enableFilters}/>;
  }, [t]);

  const headerColumnTemplate = useCallback((col) => {
    const { startColumnName, label, numberOfColumns } = col;
    return <Column
      key={startColumnName}
      field={startColumnName}
      header={translateLabel(label, t)}
      style={{ textAlign: "center" }}
      colSpan={numberOfColumns}
    />;
  }, [t]);

  const editorColumnTemplate = useCallback((place, forBody, rowSpan, editable, multioperation) => {
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
            editRow={editRow}
            saveRow={saveRow}
            cancelRow={cancelRow}/>}
          footer={null}
          style={getWidthStyle(null, 50)}
          bodyStyle={{ textAlign: "center", ...getWidthStyle(null, 50) }} />;
      } else {
        return <Column key={`${place}-editor`} field={`${place}-editor`} style={getWidthStyle(null, 50)} rowSpan={rowSpan}/>;
      }
    }
    return null;
  }, [cancelRow, editRow, saveRow]);

  const getHeader = useCallback(() => {
    const { headerModel = [], columnModel = [] } = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    let headerColumns = [];
    let groupedColumns = [];
    let index = 0;
    headerModel.forEach(header => {
      let headerStartIndex = visibleColumns.findIndex(column => column.name === header.startColumnName);
      headerColumns = [...headerColumns, ...visibleColumns.slice(index, headerStartIndex)];
      headerColumns.push(header);
      let headerSubColumns = visibleColumns.slice(headerStartIndex, headerStartIndex + header.numberOfColumns);
      groupedColumns = [...groupedColumns, ...headerSubColumns];
      index = headerStartIndex + header.numberOfColumns;
    });
    return {
      columns: [...headerColumns, ...visibleColumns.slice(index)],
      grouped: groupedColumns
    };
  }, [attributes]);

  const footerColumnTemplate = useCallback((column) => {
    const { values = [] } = model;
    const { charlength, width, align } = column;
    return <Column key={`${column.name}-footer`} field={`${column.name}-footer`} footerClassName={"p-column-footer"}
                   footerStyle={{ textAlign: align, ...getWidthStyle(charlength, width) }}
                   footer={calculateFooterValue(column, values)?.label}/>;
  }, [model]);

  const cellTemplate = useCallback((rowData, column) => {
    const gridId = getGridIdentifier(attributes);
    return Columns({
      ...attributes.columnModel.find(c => c.name === column),
      address: { ...address, column, row: rowData[gridId] }
    }, rowData[column], rowData.$attrs?.[column], (rowData.$row || {}).editing);
  }, [attributes, updateModelWithDependencies, updateAttributes, addActionsTop, address, t, settings, components]);

  const buttonsTemplate = useCallback(() => {
    const { buttonModel = [] } = attributes;
    return buttonModel.map((button, index) => React.createElement(AweButton, {
      ...button, key: button.id || `component-${index}`
    }));
  }, [attributes]);

  const preColumnTemplates = useCallback((place, rowSpan, specific) => {
    // specific includes first, rows, rowNumbers, multiselect flags
    return [
      multiselectColumnTemplate(place, rowSpan, specific?.multiselect),
      rowNumberColumnTemplate(place, rowSpan, specific?.first, specific?.rows, specific?.rowNumbers)
    ].filter(e => !isEmpty(e));
  }, [multiselectColumnTemplate, rowNumberColumnTemplate]);

  const postColumnTemplates = useCallback((place, rowSpan, forBody = false, editable, multioperation) => {
    return [
      operationColumnTemplate(place, rowSpan, multioperation),
      editorColumnTemplate(place, forBody, rowSpan, editable, multioperation),
      <Column key={`filler-${place}`} field={`filler-${place}`} rowSpan={rowSpan}/>
    ].filter(e => !isEmpty(e));
  }, [operationColumnTemplate, editorColumnTemplate]);

  return useMemo( () => ({
    t,
    address,
    attributes,
    model,
    specificAttributes,
    settings,
    components,
    addActionsTop,
    updateModelWithDependencies,
    updateAttributes,
    editRow,
    saveRow,
    cancelRow,
    filterRow,
    onSelect,
    onContextMenu,
    contextMenuTemplate,
    rowNumberColumnTemplate,
    multiselectColumnTemplate,
    operationColumnTemplate,
    columnTemplate,
    headerColumnTemplate,
    editorColumnTemplate,
    getHeader,
    footerColumnTemplate,
    cellTemplate,
    buttonsTemplate,
    preColumnTemplates,
    postColumnTemplates,
    cmRef
  }), [t,
    address,
    attributes,
    model,
    specificAttributes,
    settings,
    components,
    addActionsTop,
    updateModelWithDependencies,
    updateAttributes,
    editRow,
    saveRow,
    cancelRow,
    filterRow,
    onSelect,
    onContextMenu,
    contextMenuTemplate,
    rowNumberColumnTemplate,
    multiselectColumnTemplate,
    operationColumnTemplate,
    columnTemplate,
    headerColumnTemplate,
    editorColumnTemplate,
    getHeader,
    footerColumnTemplate,
    cellTemplate,
    buttonsTemplate,
    preColumnTemplates,
    postColumnTemplates,
    cmRef]);
}
