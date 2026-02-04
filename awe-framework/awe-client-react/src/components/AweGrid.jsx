import React, {useCallback, useEffect, useState} from "react";
import {DataTable} from "primereact/datatable";
import {Column} from "primereact/column";
import {ColumnGroup} from "primereact/columngroup";
import {Row} from "primereact/row";
import {getWidthStyle} from "../utilities/grid";
import AweGridContainer from "./AweGridContainer";
import "./AweGrid.less";
import {classNames} from "../utilities/components";
import {useDispatch} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import {useGrid} from "../hooks/useGrid";
import {ProgressSpinner} from "primereact/progressspinner";
import PropTypes from "prop-types";

function AweGrid(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const [rowsPerPageOptions, setRowsPerPageOptions] = useState([]);

  const {
    address,
    attributes,
    model,
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
    getHeader
  } = useGrid(id);

  const onRowDoubleClick = useCallback((event) => {
    const index = event.index;
    const rowId = model.values[index]?.id;
    if (rowId != null) editRow(rowId);
  }, [model.values, editRow]);

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

  // Scroll to save button when present
  useEffect(() => {
    const element = document.querySelector(".p-row-editor-save");
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  });

  const headerTemplate = useCallback(() => {
    const { headerModel = [], columnModel = [] } = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (headerModel.length > 0) {
      const header = getHeader();
      return <ColumnGroup>
        <Row>
          {preColumnTemplates("header", 2, { multiselect: attributes.multiselect, rowNumbers: attributes.rowNumbers })}
          {header.columns.map(col => col.startColumnName ? headerColumnTemplate(col) : columnTemplate(col, 2, attributes.enableFilters))}
          {postColumnTemplates("header", 2, false, attributes.editable, attributes.multioperation)}
        </Row>
        <Row>
          {header.grouped.map(col => columnTemplate(col, 1, attributes.enableFilters))}
        </Row>
      </ColumnGroup>;
    } else {
      return <ColumnGroup>
        <Row>
          {preColumnTemplates("header", 1, { multiselect: attributes.multiselect, rowNumbers: attributes.rowNumbers })}
          {visibleColumns.map(col => columnTemplate(col, 1, attributes.enableFilters))}
          {postColumnTemplates("header", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
  }, [attributes, model, address, preColumnTemplates, headerColumnTemplate, columnTemplate, postColumnTemplates]);

  const footerTemplate = useCallback(() => {
    const { columnModel = [], showTotals } = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (showTotals) {
      return <ColumnGroup>
        <Row>
          {preColumnTemplates("footer", 1, { multiselect: attributes.multiselect, rowNumbers: attributes.rowNumbers })}
          {visibleColumns.map(col => footerColumnTemplate(col))}
          {postColumnTemplates("footer", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
    return null;
  }, [attributes, footerColumnTemplate, postColumnTemplates, preColumnTemplates]);

  const rowClassName = useCallback((data) => [data.id, data.$row?.editing ? "editing" : null, data["_style_"]].filter(v => v).join(" "), []);

  const { style, headerModel = [], columnModel = [], multiselect, disablePagination, max = settings.recordsPerPage, loadAll, visible, loading = false } = attributes;
  const { records = 0, values = [] } = model;
  const { filters, first = 0, rows = max, sort = [] } = specificAttributes;
  const classes = classNames("p-datatable-sm", "expand", style, { "hidden": !visible });
  const selectedValues = values.filter(item => item.selected);

  return <AweGridContainer onKeyCancelRow={cancelRow} onKeySaveRow={saveRow} onContextMenu={onContextMenu}>
    <DataTable
      selection={selectedValues}
      selectionMode={multiselect ? null : "single"}
      onSelectionChange={onSelect} onRowDoubleClick={onRowDoubleClick}
      onContextMenu={onContextMenu}
      contextMenuSelection={selectedValues}
      onContextMenuSelectionChange={onSelect}
      id={address?.component}
      value={values} className={classes}
      headerColumnGroup={headerTemplate()}
      footerColumnGroup={footerTemplate()}
      rowClassName={rowClassName}
      dataKey="id"
      emptyMessage={""}
      paginator lazy={!loadAll}
      loading={loading}
      paginatorTemplate={disablePagination ? "" : "FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"}
      first={first} rows={rows} paginatorLeft={buttonsTemplate()} onPage={onPage}
      paginatorRight={<span>&nbsp;</span>} totalRecords={records} rowsPerPageOptions={rowsPerPageOptions}
      resizableColumns={headerModel.length === 0} columnResizeMode="fit"
      scrollable scrollHeight={"flex"}
      editMode="row"
      sortMode="multiple" removableSort
      multiSortMeta={sort.map(item => ({ field: item.id, order: item.direction === "asc" ? 1 : -1 }))}
      onSort={onSort}
      onFilter={onFilter}
      filters={filters} filterDisplay={"menu"}
      loadingIcon={<ProgressSpinner />}
    >
      {preColumnTemplates("cell", 1, { multiselect, first, rows, rowNumbers: attributes.rowNumbers })}
      {
        columnModel.filter(col => !col.hidden)
          .map(col => {
            const { name, sortField, align, charlength = null, width = null } = col;
            return <Column
              key={name}
              columnKey={name}
              field={sortField || name}
              sortField={sortField || name}
              body={rowData => cellTemplate(rowData, name)}
              bodyClassName={`p-cell-editing ${name}`}
              style={{ ...getWidthStyle(charlength, width) }}
              bodyStyle={{ textAlign: align, justifyContent: align }}
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
