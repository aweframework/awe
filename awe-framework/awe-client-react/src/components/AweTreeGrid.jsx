import React, {useCallback, useEffect, useState} from "react";
import {TreeTable} from "primereact/treetable";
import {Column} from "primereact/column";
import {generateServerAction} from "../utilities";
import {getWidthStyle} from "../utilities/grid";
import {classNames} from "../utilities/components";
import AweGridContainer from "./AweGridContainer";
import "./AweTreeGrid.less";
import {ColumnGroup} from "primereact/columngroup";
import {Row} from "primereact/row";
import {Ripple} from "primereact/ripple";
import {isEmpty} from "../utilities/general";
import {useDispatch} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import {useGrid} from "../hooks/useGrid";
import {ProgressSpinner} from "primereact/progressspinner";
import PropTypes from "prop-types";

function AweTreeGrid(props) {
  const { id } = props;
  const dispatch = useDispatch();

  const [nodes, setNodes] = useState([]);
  const [expandedKeys, setExpandedKeys] = useState({});
  const [selectionKeys, setSelectionKeys] = useState(null);
  const [rowsPerPageOptions, setRowsPerPageOptions] = useState([]);

  const {
    address,
    attributes,
    model,
    specificAttributes,
    settings,
    saveRow,
    cancelRow,
    onContextMenu,
    contextMenuTemplate,
    columnTemplate,
    headerColumnTemplate,
    footerColumnTemplate,
    buttonsTemplate,
    postColumnTemplates,
    cellTemplate: baseCellTemplate,
    getHeader
  } = useGrid(id);

  const treeId = attributes.treeId || "id";
  const treeParent = attributes.treeParent || "parent";

  const onTogglerClick = useCallback((row) => {
    const { loadAll, targetAction } = attributes;
    const { values = [] } = model;
    const rowValues = values.find(r => r[treeId] === row) || {};
    if (loadAll || rowValues.$row?.loaded) {
      dispatch(addActionsTop([{ type: "tree-branch", address, parameters: { row } }]));
    } else {
      dispatch(addActionsTop([generateServerAction({ expandingBranch: row }, "tree-branch", targetAction, address, false, false, settings)]));
    }
  }, [attributes, model, treeId, dispatch, address, settings]);

  const onSelectTree = useCallback((event) => {
    const values = [event.value].flat().filter(i => !isEmpty(i));
    dispatch(addActionsTop([{ type: "select-row", address, parameters: { values } }]));
  }, [dispatch, address]);

  const formatTreeData = useCallback((data, parent, level) => {
    const { loadAll } = attributes;
    return data
      .map(row => ({ ...row, [treeParent]: row[treeParent] === null ? "" : row[treeParent] }))
      .filter(row => row[treeParent] === parent)
      .map(row => ({
        key: row[treeId],
        id: row[treeId],
        $row: { ...(row.$row || {}) },
        data: { ...row },
        ...(!loadAll && { leaf: !!row.isLeaf } || {}),
        level,
        children: formatTreeData(data, row[treeId], level + 1)
      }));
  }, [attributes, treeId, treeParent]);

  const getExpanded = useCallback((values) => {
    return values
      .filter(row => row.$row?.expanded)
      .reduce((keys, row) => ({ ...keys, [row[treeId]]: true }), {});
  }, [treeId]);

  const getSelection = useCallback((values) => {
    return (values.find(row => row.selected) || {})[treeId] || null;
  }, [treeId]);

  const updateTreeData = useCallback((values) => {
    const n = formatTreeData(values, "", 0);
    setNodes(n);
    setExpandedKeys(getExpanded(values));
    setSelectionKeys(getSelection(values));
  }, [formatTreeData, getExpanded, getSelection]);

  useEffect(() => {
    updateTreeData(model.values || []);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    updateTreeData(model.values || []);
  }, [model.values, updateTreeData]);

  useEffect(() => {
    const pagerValues = attributes.pagerValues || [];
    const max = attributes.max || 20;
    const pager = pagerValues.length > 0 ? pagerValues : [10, 20, 30];
    const set = new Set([...pager, max].filter(v => typeof v === 'number'));
    setRowsPerPageOptions(Array.from(set).sort((a, b) => a - b));
  }, [attributes.pagerValues, attributes.max]);

  const headerTemplate = useCallback(() => {
    const { headerModel = [], columnModel = [] } = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (headerModel.length > 0) {
      const header = getHeader();
      return <ColumnGroup>
        <Row key="firstRow">
          {header.columns.map(col => col.startColumnName ? headerColumnTemplate(col) : columnTemplate(col, 2, attributes.enableFilters))}
          {postColumnTemplates("header", 2, false, attributes.editable, attributes.multioperation)}
        </Row>
        <Row key="secondRow">
          {header.grouped.map(col => columnTemplate(col, 1, attributes.enableFilters))}
        </Row>
      </ColumnGroup>;
    } else {
      return <ColumnGroup>
        <Row key="firstRow">
          {visibleColumns.map(col => columnTemplate(col, 1, attributes.enableFilters))}
          {postColumnTemplates("header", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
  }, [attributes, getHeader, headerColumnTemplate, columnTemplate, postColumnTemplates]);

  const footerTemplate = useCallback(() => {
    const { columnModel = [], showTotals } = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (showTotals) {
      return <ColumnGroup>
        <Row>
          {visibleColumns.map(col => footerColumnTemplate(col))}
          {postColumnTemplates("footer", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
    return null;
  }, [attributes, footerColumnTemplate, postColumnTemplates]);

  const cellTemplate = useCallback((rowData, column) => {
    const { expandColumn = treeId } = attributes;
    const iconClassName = classNames('p-treetable-toggler-icon pi pi-fw', { 'pi-chevron-right': !rowData?.$row?.expanded, 'pi-chevron-down': rowData?.$row?.expanded });
    const style = { marginLeft: rowData.level * 16 + 'px', visibility: (rowData.leaf === false || (rowData.children && rowData.children.length)) ? 'visible' : 'hidden' };
    if (column === expandColumn) {
      return <div className="p-treetable-expander-column">
        <button type="button" className="p-treetable-toggler p-link p-unselectable-text" onClick={() => onTogglerClick(rowData.data[treeId])} tabIndex={-1} style={style}>
          <i className={iconClassName}></i>
          <Ripple />
        </button>
        {baseCellTemplate({ ...rowData.data, id: rowData.data[treeId] }, column)}
      </div>;
    } else {
      return baseCellTemplate({ ...rowData.data, id: rowData.data[treeId] }, column);
    }
  }, [attributes, onTogglerClick, baseCellTemplate, treeId]);

  const { style, headerModel = [], columnModel = [], max, disablePagination, loadAll, visible, loading = false } = attributes;
  const { first = 0, rows = max } = specificAttributes;
  const styles = classNames("p-treetable-sm", "expandible-vertical", style, { "hidden": !visible });

  return <AweGridContainer onKeyCancelRow={cancelRow} onKeySaveRow={saveRow} onContextMenu={onContextMenu}>
    <TreeTable
      id={address?.component}
      value={nodes} expandedKeys={expandedKeys} selectionKeys={selectionKeys} onToggle={() => null}
      className={styles}
      selectionMode="single"
      onSelectionChange={onSelectTree}
      onContextMenu={onContextMenu}
      onContextMenuSelectionChange={onSelectTree}
      headerColumnGroup={headerTemplate()}
      footerColumnGroup={footerTemplate()}
      paginator lazy={!loadAll}
      loading={loading}
      paginatorTemplate={disablePagination ? "" : "FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"}
      first={first} rows={rows} paginatorLeft={buttonsTemplate()} onPage={() => null}
      paginatorRight={<span>&nbsp;</span>} totalRecords={nodes.length} rowsPerPageOptions={rowsPerPageOptions}
      emptyMessage={""}
      resizableColumns={headerModel.length === 0} columnResizeMode="fit"
      scrollable
      loadingIcon={<ProgressSpinner />}
    >
      {
        columnModel
          .filter(col => !col.hidden)
          .map(col => {
            const { name, sortField, align, charlength = null, width = null } = col;
            return <Column
              key={name}
              field={sortField || name}
              body={rowData => cellTemplate(rowData, name)}
              bodyClassName="p-cell-editing"
              bodyStyle={{ ...getWidthStyle(charlength, width), textAlign: align }}
            />;
          })
      }
      {postColumnTemplates("cell", 1, true, attributes.editable, attributes.multioperation)}
    </TreeTable>
    {contextMenuTemplate()}
  </AweGridContainer>;
}

AweTreeGrid.propTypes = {
  id: PropTypes.string,
};

export default AweTreeGrid;
