import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {DataTable} from "primereact/datatable";
import {Column} from "primereact/column";
import {ColumnGroup} from "primereact/columngroup";
import {Row} from "primereact/row";
import {arrangeFrozenColumns, getColumnWrap, getGridIdentifier, getWidthStyle, MoveRowActionType, OperationType} from "../utilities/grid";
import AweGridContainer from "./AweGridContainer";
import AweGridEmptyState, {isLoaded} from "./AweGridEmptyState";
import "./AweGrid.less";
import "./AweGridThemeClasses.less";
import {classNames} from "../utilities/components";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import {useGrid} from "../hooks/useGrid";
import {useGridRowDrag} from "../hooks/useGridRowDrag";
import AweGridSkeleton, {getGridIconLoading, gridLoadingIcon, isGridReloading, isSkeletonIcon} from "./AweGridSkeleton";
import PropTypes from "prop-types";
import {bodyCellPassThrough, displayRowHook, gridPassThrough} from "../utilities/testPassThrough";
import {TestIds, testHook} from "../utilities/testIds";
import {useTranslation} from "react-i18next";
import {gotoPageTemplate} from "./AweGridGotoPage";
import AweGridRowDetail from "./AweGridRowDetail";
import {getRowDetailInstanceId} from "../utilities/gridRowDetail";
import {syncRowDetailInstances} from "../redux/thunks/rowDetail";
import {useGridGrouping} from "../hooks/useGridGrouping";
import {getDisplayRowClassName, isDisplayRow} from "../utilities/gridGrouping";

/**
 * Template of the row expansion of a grid with row detail: the detail of each expanded row
 * @param {string} gridId Grid identifier
 * @param {object} rowDetail Row detail of the grid
 * @returns {function(object): JSX.Element} Row expansion template
 */
const getRowExpansionTemplate = (gridId, rowDetail) => function rowExpansionTemplate(rowData) {
  return <AweGridRowDetail gridId={gridId} rowDetail={rowDetail} rowData={rowData}>
    {rowDetail.component ? <AweGridRowDetailInstance id={getRowDetailInstanceId(rowDetail.component.id, rowData.id)} /> : null}
  </AweGridRowDetail>;
};

const PAGINATOR_TEMPLATE = {
  layout: "FirstPageLink PrevPageLink JumpToPageInput NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport",
  JumpToPageInput: gotoPageTemplate
};

/**
 * Instance of the row detail component of a row: the grid is shown once its instance is registered
 * @param {string} id Instance identifier
 * @returns {JSX.Element|null} Nested grid
 */
function AweGridRowDetailInstance({ id }) {
  const registered = useSelector(state => Boolean(state.components[id]));
  return registered ? <AweGrid id={id} /> : null;
}

AweGridRowDetailInstance.propTypes = {
  id: PropTypes.string.isRequired,
};

function AweGrid(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const {t} = useTranslation();
  const [rowsPerPageOptions, setRowsPerPageOptions] = useState([]);
  // Rows whose detail is shown, by row id. They stay open while the rows are paged or sorted on the client
  const [expandedRows, setExpandedRows] = useState({});

  const {
    address,
    attributes,
    model,
    modelLoaded,
    specificAttributes,
    settings,
    editRow,
    saveRow,
    cancelRow,
    filterRow,
    onSelect,
    onContextMenu,
    contextMenuTemplate,
    columnTemplate,
    headerColumnTemplate,
    footerColumnTemplate,
    cellTemplate,
    buttonsTemplate,
    preColumnTemplates,
    postColumnTemplates,
    visibleColumns: layoutColumns,
    getHeader
  } = useGrid(id);

  // The frozen columns go to the left and stay out of the horizontal scroll
  const { columns: visibleColumns, hasFrozen } = useMemo(
    () => arrangeFrozenColumns(layoutColumns, attributes.headerModel),
    [layoutColumns, attributes.headerModel]
  );

  // Row grouping (#833): while grouped the grid shows the group and subtotal rows built from its data rows, all of them
  // (no pagination), sorted here, and without column filters
  const grouping = useGridGrouping({ address, attributes, sort: specificAttributes.sort, values: model.values,
    firstColumn: visibleColumns[0]?.name, onSelect, onContextMenu, t });
  const enableFilters = attributes.enableFilters && !grouping.active;

  const onToggleRowDetail = useCallback((rowId) => {
    setExpandedRows(current => {
      const {[rowId]: open, ...others} = current;
      return open === undefined ? {...current, [rowId]: true} : others;
    });
  }, []);

  // A data reload (filter, reload, server paging or sorting) closes every detail
  useEffect(() => {
    if (attributes.loading) {
      setExpandedRows(current => Object.keys(current).length > 0 ? {} : current);
    }
  }, [attributes.loading]);

  // With components, each open row has its own instance of them: it is created when the row opens and removed when
  // it closes, when the grid reloads (which closes every row) and when the grid is gone
  const detailComponentId = attributes.rowDetail?.component?.id;
  useEffect(() => {
    if (detailComponentId) {
      const openRows = (model.values || []).filter(row => expandedRows[row.id] !== undefined);
      dispatch(syncRowDetailInstances(address, openRows));
    }
  }, [detailComponentId, expandedRows, model.values, address, dispatch]);
  const instancesAddress = useRef(null);
  instancesAddress.current = detailComponentId ? address : null;
  useEffect(() => () => {
    if (instancesAddress.current) {
      dispatch(syncRowDetailInstances(instancesAddress.current, []));
    }
  }, [dispatch]);

  const onRowDoubleClick = useCallback((event) => {
    const gridId = getGridIdentifier(attributes);
    const rowId = event.data?.[gridId];
    if (rowId != null) {
      editRow(rowId);
    }
  }, [attributes, editRow]);

  // Alt+ArrowUp, Alt+ArrowDown, Alt+Home and Alt+End move the selected row of an editable grid or a grid with move-rows
  const onKeyMoveRow = useCallback((position) => {
    dispatch(addActionsTop([{ type: MoveRowActionType[position], address }]));
  }, [dispatch, address]);

  // The grip of each row drags it to another row (grids with move-rows, never tree grids)
  const moveRows = !!attributes.moveRows && !attributes.treegrid;
  const { onGripDragStart, onGripDragEnd, getRowProps } = useGridRowDrag(address);

  const onPage = useCallback((event) => {
    const { loadAll } = attributes;
    const { first, rows, page } = event;
    filterRow({ type: "change-page", address, parameters: { page: page + 1, first, rows, max: loadAll ? 0 : rows } });
  }, [attributes, address, filterRow]);

  const onSort = useCallback((event) => {
    const { multiSortMeta } = event;
    filterRow({ type: "change-sort", address, parameters: { sort: multiSortMeta.map(s => ({ id: s.field, order: s.order, direction: s.order > 0 ? "asc" : "desc" })) } });
  }, [address, filterRow]);

  const onFilter = useCallback((event) => {
    const { filters } = event;
    const values = model.values || [];
    const editingRow = values.find(row => row.$row?.editing);
    const cancelRowAction = editingRow ? [{ type: "cancel-row", address }] : [];
    dispatch(addActionsTop([...cancelRowAction, { type: "change-filter", address, parameters: { filters } }]));
  }, [dispatch, model.values, address]);

  // Initialize rowsPerPageOptions when attributes change (mimic componentDidMount logic)
  useEffect(() => {
    const { pagerValues = [], max = 20 } = attributes;
    const pager = pagerValues.length > 0 ? pagerValues : [10, 20, 30];
    const set = new Set([...pager, max].filter(v => typeof v === 'number'));
    setRowsPerPageOptions(Array.from(set).sort((a, b) => a - b));
  }, [attributes.pagerValues, attributes.max]);

  // Scroll to the save button when a row starts being edited. Doing it on every render moves a grid wider than its
  // container whenever anything changes (a click on another row, a typed value), and the cells run away from under
  // the pointer of the user
  const editingRowId = (model.values || []).find(row => row.$row?.editing)?.[getGridIdentifier(attributes)];
  useEffect(() => {
    const element = document.querySelector(".p-row-editor-save");
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }, [editingRowId]);

  const headerTemplate = useCallback(() => {
    const { headerModel = [] } = attributes;
    if (headerModel.length > 0) {
      const header = getHeader();
      return <ColumnGroup>
        <Row>
          {preColumnTemplates("header", 2, { multiselect: attributes.multiselect, rowNumbers: attributes.rowNumbers, rowDetail: attributes.rowDetail, moveRows, frozen: hasFrozen })}
          {header.columns.map(col => col.startColumnName ? headerColumnTemplate(col) : columnTemplate(col, 2, enableFilters, hasFrozen))}
          {postColumnTemplates("header", 2, false, attributes.editable, attributes.multioperation)}
        </Row>
        <Row>
          {header.grouped.map(col => columnTemplate(col, 1, enableFilters, hasFrozen))}
        </Row>
      </ColumnGroup>;
    } else {
      return <ColumnGroup>
        <Row>
          {preColumnTemplates("header", 1, { multiselect: attributes.multiselect, rowNumbers: attributes.rowNumbers, rowDetail: attributes.rowDetail, moveRows, frozen: hasFrozen })}
          {visibleColumns.map(col => columnTemplate(col, 1, enableFilters, hasFrozen))}
          {postColumnTemplates("header", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
  }, [attributes, model, address, visibleColumns, hasFrozen, moveRows, enableFilters, getHeader, preColumnTemplates, headerColumnTemplate, columnTemplate, postColumnTemplates]);

  const footerTemplate = useCallback(() => {
    const { showTotals } = attributes;
    if (showTotals) {
      return <ColumnGroup>
        <Row>
          {preColumnTemplates("footer", 1, { multiselect: attributes.multiselect, rowNumbers: attributes.rowNumbers, rowDetail: attributes.rowDetail, moveRows, frozen: hasFrozen })}
          {visibleColumns.map(col => footerColumnTemplate(col, hasFrozen))}
          {postColumnTemplates("footer", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
    return null;
  }, [attributes, visibleColumns, hasFrozen, moveRows, footerColumnTemplate, postColumnTemplates, preColumnTemplates]);

  // PrimeReact does not expose the data of a row to the pass-through of the row, and the order of the rendered rows
  // depends on its own sort and filter. It evaluates "rowClassName" with the row data right before the row pass-through
  // in the same render, so the id of the row being rendered is kept here to give the row its "row-id".
  const renderingRowId = useRef(null);
  const renderingRowEditing = useRef(false);
  const renderingRowDeleted = useRef(false);
  // Group or subtotal row being rendered (grouped grids): it has its own classes and hook, and no row-id
  const renderingDisplayRow = useRef(null);
  const rowClassName = useCallback((data) => {
    if (isDisplayRow(data)) {
      renderingDisplayRow.current = data;
      return getDisplayRowClassName(data);
    }
    renderingDisplayRow.current = null;
    renderingRowId.current = data.id;
    renderingRowEditing.current = !!data.$row?.editing;
    renderingRowDeleted.current = data.$row?.operation === OperationType.DELETE;
    return [data.id, data.$row?.editing ? "editing" : null, data["_style_"]].filter(v => v).join(" ");
  }, []);

  const { style, headerModel = [], multiselect, disablePagination, max = settings.recordsPerPage, loadAll, visible, loading = false, iconLoading, rowDetail } = attributes;
  const { records = 0, values = [] } = model;
  const { filters, first = 0, rows = max, sort = [] } = specificAttributes;
  const classes = classNames("p-datatable-sm", "expand", style, { "hidden": !visible });
  const selectedValues = values.filter(item => item.selected);
  const loadingIcon = getGridIconLoading(iconLoading, settings);
  // The skeleton replaces the rows in the body instead of masking them. While it is shown the grid is kept lazy with
  // its known total, so a client-side paginated grid does not jump back to the first page when its rows disappear
  const reloading = isGridReloading(loading, loadingIcon, values.length);
  const showSkeleton = loading && !reloading && isSkeletonIcon(loadingIcon);
  let emptyMessage = loading || !isLoaded(attributes, modelLoaded) ? "" : <AweGridEmptyState attributes={attributes} />;
  if (showSkeleton) {
    emptyMessage = <AweGridSkeleton columns={visibleColumns} rows={rows} multiselect={!!multiselect} rowNumbers={!!attributes.rowNumbers}
                                    rowDetail={!!rowDetail} first={first} />;
  }
  // Only a grid with row detail shows the details under its rows
  const rowDetailProps = rowDetail ? {
    expandedRows,
    rowExpansionTemplate: getRowExpansionTemplate(address?.component, rowDetail)
  } : {};

  return <AweGridContainer id={id} onKeyCancelRow={cancelRow} onKeySaveRow={saveRow} onKeyMoveRow={attributes.editable || moveRows ? onKeyMoveRow : undefined}
                    onContextMenu={onContextMenu} loading={loading} reloading={reloading} rowHeight={attributes.rowHeight}>
    <DataTable
      selection={selectedValues}
      selectionMode={multiselect ? null : "single"}
      onSelectionChange={onSelect} onRowDoubleClick={onRowDoubleClick}
      onContextMenu={onContextMenu}
      contextMenuSelection={selectedValues}
      onContextMenuSelectionChange={onSelect}
      id={address?.component}
      value={showSkeleton ? [] : grouping.values} className={classes}
      headerColumnGroup={headerTemplate()}
      footerColumnGroup={footerTemplate()}
      rowClassName={rowClassName}
      dataKey="id"
      emptyMessage={emptyMessage}
      paginator lazy={!loadAll || showSkeleton}
      loading={loading && !showSkeleton}
      paginatorTemplate={disablePagination ? "" : PAGINATOR_TEMPLATE}
      currentPageReportTemplate={`${t("SCREEN_TEXT_GRID_VIEW")} {first} - {last} ${t("SCREEN_TEXT_GRID_OF")} {totalRecords}`}
      first={first} rows={rows} paginatorLeft={buttonsTemplate()} onPage={onPage}
      paginatorRight={<span>&nbsp;</span>} totalRecords={showSkeleton ? Math.max(records, values.length) : records} rowsPerPageOptions={rowsPerPageOptions}
      resizableColumns={headerModel.length === 0} columnResizeMode="fit"
      scrollable scrollHeight={"flex"}
      editMode="row"
      sortMode="multiple" removableSort
      multiSortMeta={sort.map(item => ({ field: item.id, order: item.direction === "asc" ? 1 : -1 }))}
      onSort={onSort}
      onFilter={onFilter}
      filters={filters} filterDisplay={"menu"}
      loadingIcon={gridLoadingIcon(reloading, loadingIcon)}
      {...rowDetailProps}
      {...grouping.tableProps}
      pt={gridPassThrough({
        getDisplayRowHook: () => renderingDisplayRow.current && displayRowHook(renderingDisplayRow.current),
        gridId: address?.component,
        getRowId: () => renderingRowId.current,
        isRowEditing: () => renderingRowEditing.current,
        isRowDeleted: () => renderingRowDeleted.current,
        getRowProps: moveRows ? getRowProps : undefined
      })}
    >
      {preColumnTemplates("cell", 1, { multiselect, first, rows, rowNumbers: attributes.rowNumbers, rowDetail, expandedRows, onToggleRowDetail, moveRows,
        onRowDragStart: onGripDragStart, onRowDragEnd: onGripDragEnd, frozen: hasFrozen, rowNumberOf: grouping.rowNumberOf })}
      {
        visibleColumns
          .map(col => {
            const { name, sortField, align, charlength = null, width = null } = col;
            return <Column
              key={name}
              columnKey={name}
              field={sortField || name}
              sortField={sortField || name}
              frozen={hasFrozen && !!col.frozen}
              body={rowData => isDisplayRow(rowData) ? grouping.renderCell(rowData, name) : cellTemplate(rowData, name, hasFrozen)}
              bodyClassName={`p-cell-editing ${name}${!hasFrozen && getColumnWrap(col).wrap ? " p-cell-wrapping" : ""}`}
              style={{ ...getWidthStyle(charlength, width) }}
              bodyStyle={{ textAlign: align, justifyContent: align }}
              pt={bodyCellPassThrough(name)}
            />;
          })
      }
      {postColumnTemplates("cell", 1, true, attributes.editable, attributes.multioperation)}
    </DataTable>
    {contextMenuTemplate()}
  </AweGridContainer>;
}

AweGrid.propTypes = {
  id: PropTypes.string,
};

export default AweGrid;
