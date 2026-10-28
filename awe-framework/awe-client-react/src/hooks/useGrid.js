import React, { useCallback, useMemo } from "react";
import { Column } from "primereact/column";
import { Columns } from "../utilities/structure";
import ColumnRowEditor from "../columns/ColumnRowEditor";
import { getHelpTooltipNode, getIconNode, translateLabel } from "../utilities";
import { useContextMenu } from "./useContextMenu";
import { getGridIdentifier, getRowNumberWidthStyle, getWidthStyle, OperationIcon, OperationIconName, SELECTION_COLUMN_WIDTH, EDITOR_COLUMN_WIDTH } from "../utilities/grid";
import { isEmpty } from "../utilities/general";
import { useDispatch, useSelector } from "react-redux";
import { addActionsTop as addActionsTopAction } from "../redux/actions/actions";
import { updateModelWithDependencies as updateThunk } from "../redux/thunks/components";
import { updateAttributes as updateAttributesAction } from "../redux/actions/components";
import { useTranslation } from "react-i18next";
import AweButton from "../components/AweButton";
import { AweGridRowDetailToggle } from "../components/AweGridRowDetail";
import useComponent from "./useComponent";
import { useComponentState } from "./useComponentState";
import {
  bodyCellPassThrough,
  headerCellPassThrough,
  selectionColumnPassThrough
} from "../utilities/testPassThrough";
import { TestAttributes, TestIds, testHook } from "../utilities/testIds";
import { applyColumnLayout, buildHeader } from "../utilities/gridLayout";

// Model of a grid whose data has not arrived yet
const EMPTY_MODEL = Object.freeze({ values: [] });

/**
 * Hook providing shared grid helpers previously in AweGridCommons class.
 */
export function useGrid(id) {
  // Initialize as component
  const { address } = useComponent(id);

  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { attributes = {}, model: stateModel, specificAttributes = {} } = useComponentState(id);
  const model = stateModel ?? EMPTY_MODEL;
  // The model only exists once the data of the component has arrived
  const modelLoaded = stateModel !== undefined;
  const settings = useSelector(state => state.settings || {});

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

  const { onContextMenu, contextMenuTemplate } = useContextMenu({
    address, contextMenu: attributes.contextMenu, openWithoutVisibleOptions: true
  });

  const rowNumberColumnTemplate = (place, rowSpan, first, rows, rowNumbers, frozen = false) => {
    let rowNumberStyle = { fontWeight: "bold", textAlign: "center", ...getRowNumberWidthStyle(first, rows) };
    if (rowNumbers) {
      return <Column key={`row-number-${place}`} field={`row-number-${place}`} header={"#"} rowSpan={rowSpan} frozen={frozen}
        body={(_data, options) => options.rowIndex + 1}
        headerClassName={"p-row-number-header"} headerStyle={rowNumberStyle}
        bodyClassName={"p-row-number-cell"} bodyStyle={rowNumberStyle}
        footer={null}
        footerClassName={"p-row-number-footer"}
        footerStyle={rowNumberStyle}
      />;
    }
    return null;
  };

  const multiselectColumnTemplate = (place, rowSpan, multiselect, frozen = false) => (
    multiselect ? <Column
      key={`multiselect-${place}`}
      field={`multiselect-${place}`}
      selectionMode="multiple"
      rowSpan={rowSpan}
      frozen={frozen}
      pt={selectionColumnPassThrough()}
      footer={null}
      style={{ textAlign: "center", ...getWidthStyle(null, null, SELECTION_COLUMN_WIDTH) }}
    /> : null
  );

  // Leading column with the grip that drags each row (grids with move-rows only). Only the grip starts the drag
  const moveRowsColumnTemplate = (place, rowSpan, moveRows, onRowDragStart, onRowDragEnd, frozen = false) => (
    moveRows ? <Column
      key={`move-rows-${place}`}
      field={`move-rows-${place}`}
      rowSpan={rowSpan}
      frozen={frozen}
      body={rowData => <span className="grid-row-grip" role="img" draggable="true"
        title={t("GRID_ROW_DRAG")} aria-label={t("GRID_ROW_DRAG")}
        onDragStart={event => onRowDragStart?.(event, rowData.id)} onDragEnd={onRowDragEnd}>
        {getIconNode("pi-bars", { "aria-hidden": "true" })}
      </span>}
      footer={null}
      style={{ textAlign: "center", ...getWidthStyle(null, null, SELECTION_COLUMN_WIDTH) }}
    /> : null
  );

  // Leading column with the button that shows or hides the detail of each row (grids with row detail only)
  const rowDetailColumnTemplate = (place, rowSpan, rowDetail, expandedRows, onToggleRowDetail, frozen = false) => (
    rowDetail ? <Column
      key={`row-detail-${place}`}
      field={`row-detail-${place}`}
      rowSpan={rowSpan}
      frozen={frozen}
      body={rowData => <AweGridRowDetailToggle gridId={address?.component} rowId={rowData.id}
        expanded={expandedRows?.[rowData.id] !== undefined} onToggle={onToggleRowDetail} />}
      footer={null}
      style={{ textAlign: "center", ...getWidthStyle(null, null, SELECTION_COLUMN_WIDTH) }}
    /> : null
  );

  const operationColumnTemplate = (place, rowSpan, multioperation) => (
    multioperation ? <Column key={`operation-${place}`} field={`operation-${place}`} rowSpan={rowSpan}
      headerStyle={getWidthStyle(null, null, '32px')}
      bodyStyle={getWidthStyle(null, null, '32px')}
      style={{ textAlign: "center" }}
      footer={null}
      pt={bodyCellPassThrough("RowIco")}
      body={rowData => getIconNode(OperationIcon[rowData?.$row?.operation], {role: rowData?.$row?.operation,
        ...testHook(TestIds.columnIcon, { attributes: { [TestAttributes.icon]: OperationIconName[rowData?.$row?.operation] } })})} /> : null
  );

  // The header of a column is cut by its width: it shows its full text as tooltip
  const headerLabel = (text) => text ? <span className="p-column-title-text" title={text}>{text}</span> : text;

  // Header of a column: its label and, if it has help or help image, the help icon and tooltip
  const columnHeader = (name, label, help, helpImage) => {
    const text = translateLabel(label, t);
    if (!help && !helpImage) {
      return headerLabel(text);
    }
    const target = `help-target-${address.component}-${name}`;
    return <>
      {getHelpTooltipNode(help, helpImage, t, `.${target}`)}
      <span className={target} style={{ cursor: "pointer" }}>
        {getIconNode("pi-question-circle", { role: "icon", className: "help-icon" })}{headerLabel(text)}
      </span>
    </>;
  };

  const columnTemplate = (col, rowSpan, enableFilters, frozen = false) => {
    const { name, sortField, label, charlength, width, sortable, help, helpImage, align } = col;
    // A column with an alignment aligns its header like its cells (AWE 5 grid, #885); without one it stays centred
    return <Column key={name} field={sortField || name} header={columnHeader(name, label, help, helpImage)}
      alignHeader={align || undefined}
      style={{ textAlign: align || "center", ...getWidthStyle(charlength, width) }}
      pt={headerCellPassThrough(name)}
      sortable={sortable} rowSpan={rowSpan} filter={enableFilters} frozen={frozen && !!col.frozen} />;
  };

  const headerColumnTemplate = (col) => {
    const { startColumnName, label, numberOfColumns } = col;
    return <Column
      key={startColumnName}
      field={startColumnName}
      header={headerLabel(translateLabel(label, t))}
      style={{ textAlign: "center" }}
      colSpan={numberOfColumns}
      pt={{ headerCell: { ...headerCellPassThrough(startColumnName).headerCell, ...testHook(TestIds.gridGroupHeader) } }}
    />;
  };

  // The row edit column is frozen at the right: its buttons stay in view when a wide grid scrolls horizontally. It is as
  // narrow as its edit button; the save and cancel buttons of an edited row make it grow, at the right end only
  const EDITOR_COLUMN = {frozen: true, alignFrozen: "right", headerClassName: "grid-row-editor-column",
    bodyClassName: "grid-row-editor-column", footerClassName: "grid-row-editor-column"};
  const editorColumnTemplate = (place, forBody, rowSpan, editable, multioperation) => {
    if (editable || multioperation) {
      if (forBody) {
        return <Column
          key={`${place}-editor`}
          field={`${place}-editor`}
          {...EDITOR_COLUMN}
          rowSpan={rowSpan}
          body={(data, row) => <ColumnRowEditor
            key={`${row.rowIndex}-editor`}
            row={row}
            rowData={data}
            editRow={editRow}
            saveRow={saveRow}
            cancelRow={cancelRow} />}
          footer={null}
          style={getWidthStyle(null, null, EDITOR_COLUMN_WIDTH)}
          bodyStyle={{ textAlign: "center", ...getWidthStyle(null, null, EDITOR_COLUMN_WIDTH) }} />;
      } else {
        return <Column key={`${place}-editor`} field={`${place}-editor`} {...EDITOR_COLUMN} style={getWidthStyle(null, null, EDITOR_COLUMN_WIDTH)}
          rowSpan={rowSpan} />;
      }
    }
    return null;
  };

  // Columns to show: the ones of the screen in the order of the layout of the user, without the hidden ones
  const { columnModel: allColumns, headerModel: allHeaders, columnLayout } = attributes;
  const visibleColumns = useMemo(
    () => applyColumnLayout(allColumns, allHeaders, columnLayout),
    [allColumns, allHeaders, columnLayout]
  );

  // The headers are computed from the membership of the visible columns, not from their position
  const getHeader = () => buildHeader(visibleColumns, allHeaders, allColumns);

  const footerColumnTemplate = (column, frozen = false) => {
    const { footer = {} } = model;
    const { charlength, width, align } = column;
    return <Column key={`${column.name}-footer`} field={`${column.name}-footer`} footerClassName={"p-column-footer"} frozen={frozen && !!column.frozen}
      footerStyle={{ textAlign: align, ...getWidthStyle(charlength, width) }}
      footer={(footer[column.name] ?? {}).label ?? ""} />;
  };

  const cellTemplate = (rowData, column, hasFrozen = false) => {
    const gridId = getGridIdentifier(attributes);
    const definition = attributes.columnModel?.find(c => c.name === column);
    // A grid with frozen columns keeps its single-line rows, like in AngularJS
    const lines = hasFrozen ? { wrap: false, maxLines: null } : {};
    return Columns({
      ...definition,
      ...lines,
      address: { ...address, column, row: rowData[gridId] }
    }, rowData[column], rowData.$attrs?.[column], (rowData.$row || {}).editing, rowData);
  };

  const buttonsTemplate = () => {
    const { buttonModel = [] } = attributes;
    return buttonModel.map((button, index) => React.createElement(AweButton, {
      ...button, key: button.id || `component-${index}`
    }));
  };

  const preColumnTemplates = (place, rowSpan, specific) => {
    // specific includes first, rows, rowNumbers, multiselect flags, the grips to move rows and the row detail with its open rows
    return [
      moveRowsColumnTemplate(place, rowSpan, specific?.moveRows, specific?.onRowDragStart, specific?.onRowDragEnd, specific?.frozen),
      rowDetailColumnTemplate(place, rowSpan, specific?.rowDetail, specific?.expandedRows, specific?.onToggleRowDetail, specific?.frozen),
      multiselectColumnTemplate(place, rowSpan, specific?.multiselect, specific?.frozen),
      rowNumberColumnTemplate(place, rowSpan, specific?.first, specific?.rows, specific?.rowNumbers, specific?.frozen)
    ].filter(e => !isEmpty(e));
  };

  const postColumnTemplates = (place, rowSpan, forBody = false, editable, multioperation) => {
    // The filler goes before the row actions: the edit column, frozen at the right, is the last one, and nothing is
    // left at its right when the grid is scrolled to its end
    return [
      <Column key={`filler-${place}`} field={`filler-${place}`} rowSpan={rowSpan} />,
      operationColumnTemplate(place, rowSpan, multioperation),
      editorColumnTemplate(place, forBody, rowSpan, editable, multioperation)
    ].filter(e => !isEmpty(e));
  };

  return useMemo(() => ({
    t,
    address,
    attributes,
    model,
    modelLoaded,
    specificAttributes,
    settings,
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
    rowDetailColumnTemplate,
    operationColumnTemplate,
    columnTemplate,
    headerColumnTemplate,
    editorColumnTemplate,
    visibleColumns,
    getHeader,
    footerColumnTemplate,
    cellTemplate,
    buttonsTemplate,
    preColumnTemplates,
    postColumnTemplates
  }), [t,
    address,
    attributes,
    model,
    modelLoaded,
    specificAttributes,
    settings,
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
    rowDetailColumnTemplate,
    operationColumnTemplate,
    columnTemplate,
    headerColumnTemplate,
    editorColumnTemplate,
    visibleColumns,
    getHeader,
    footerColumnTemplate,
    cellTemplate,
    buttonsTemplate,
    preColumnTemplates,
    postColumnTemplates,
    ]);
}
