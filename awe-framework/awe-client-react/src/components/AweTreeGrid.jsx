import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {TreeTable} from "primereact/treetable";
import {Column} from "primereact/column";
import {generateServerAction, getIconCode} from "../utilities";
import {getColumnWrap, getWidthStyle, OperationType} from "../utilities/grid";
import {classNames} from "../utilities/components";
import AweGridContainer from "./AweGridContainer";
import AweGridEmptyState, {isLoaded} from "./AweGridEmptyState";
import "./AweTreeGrid.less";
import {ColumnGroup} from "primereact/columngroup";
import {Row} from "primereact/row";
import {Ripple} from "primereact/ripple";
import {isEmpty} from "../utilities/general";
import {useDispatch} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import {useGrid} from "../hooks/useGrid";
import AweGridSkeleton, {getGridIconLoading, gridLoadingIcon, isGridReloading, isSkeletonIcon} from "./AweGridSkeleton";
import PropTypes from "prop-types";
import {bodyCellPassThrough, gridPassThrough} from "../utilities/testPassThrough";
import {TestIds, testHook} from "../utilities/testIds";

// Values of the tree attributes arrive as booleans, numbers or strings: like the AngularJS parseBoolean, only true, "true"
// and 1 / "1" count as true ("false" and 0 do not)
const toBoolean = (value) => typeof value === "boolean" ? value : ["true", "1"].includes(String(value ?? "").trim().toLowerCase());

// Default Font Awesome icons, as in the AngularJS client
const DEFAULT_ICONS = { expand: "plus-square", collapse: "minus-square", leaf: "dot-circle-o" };

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
    modelLoaded,
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
    visibleColumns,
    getHeader
  } = useGrid(id);

  const treeId = attributes.treeId || "id";
  const treeParent = attributes.treeParent || "parent";
  const treeLeaf = attributes.treeLeaf || "isLeaf";
  const initialLevel = parseInt(attributes.initialLevel || "0", 10) || 0;

  const onTogglerClick = useCallback((row, expanded) => {
    const { loadAll, targetAction } = attributes;
    const { values = [] } = model;
    const rowValues = values.find(r => r[treeId] === row) || {};
    // An open node always collapses locally, even when it was opened by its row and its branch was never loaded
    if (loadAll || rowValues.$row?.loaded || expanded) {
      dispatch(addActionsTop([{ type: "tree-branch", address, parameters: { row, expanded: !!expanded } }]));
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
        leaf: treeLeaf in row ? toBoolean(row[treeLeaf]) : !!loadAll,
        level,
        children: formatTreeData(data, row[treeId], level + 1)
      }));
  }, [attributes, treeId, treeParent, treeLeaf]);

  // A node starts expanded (like AngularJS): by the state of the user, by the "expanded" field of its row or, when it
  // is loaded, while its level (roots are 0) is above the initial level
  const getExpanded = useCallback((nodes, loadAll, keys = {}) => {
    nodes.forEach(node => {
      const { $row = {}, data } = node;
      let expanded;
      if (typeof $row.expanded === "boolean") {
        expanded = $row.expanded;
      } else if ("expanded" in data) {
        expanded = toBoolean(data.expanded);
      } else {
        expanded = !!(loadAll || $row.loaded || node.children.length > 0) && node.level + 1 < initialLevel;
      }
      expanded && (keys[node.key] = true);
      getExpanded(node.children, loadAll, keys);
    });
    return keys;
  }, [initialLevel]);

  const getSelection = useCallback((values) => {
    return (values.find(row => row.selected) || {})[treeId] || null;
  }, [treeId]);

  const updateTreeData = useCallback((values) => {
    const n = formatTreeData(values, "", 0);
    setNodes(n);
    setExpandedKeys(getExpanded(n, attributes.loadAll));
    setSelectionKeys(getSelection(values));
  }, [formatTreeData, getExpanded, getSelection, attributes.loadAll]);

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
    const { headerModel = [] } = attributes;
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
  }, [attributes, visibleColumns, getHeader, headerColumnTemplate, columnTemplate, postColumnTemplates]);

  const footerTemplate = useCallback(() => {
    const { showTotals } = attributes;
    if (showTotals) {
      return <ColumnGroup>
        <Row>
          {visibleColumns.map(col => footerColumnTemplate(col))}
          {postColumnTemplates("footer", 1, false, attributes.editable, attributes.multioperation)}
        </Row>
      </ColumnGroup>;
    }
    return null;
  }, [attributes, visibleColumns, footerColumnTemplate, postColumnTemplates]);

  const cellTemplate = useCallback((rowData, column) => {
    const { expandColumn = treeId, iconExpand, iconCollapse, iconLeaf } = attributes;
    const expanded = !!expandedKeys[rowData.key];
    const leaf = rowData.leaf === true && !(rowData.children && rowData.children.length);
    const icon = leaf ? (iconLeaf || DEFAULT_ICONS.leaf) : expanded ? (iconCollapse || DEFAULT_ICONS.collapse) : (iconExpand || DEFAULT_ICONS.expand);
    // Each level indents the toggle by the size of a toggle (AWE 5 tree grid, #885)
    const style = { marginLeft: `calc(${rowData.level || 0} * var(--awe-space-6))` };
    if (column === expandColumn) {
      return <div className="p-treetable-expander-column">
        <button type="button" className="p-treetable-toggler p-link p-unselectable-text" onClick={() => !leaf && onTogglerClick(rowData.data[treeId], expanded)} tabIndex={-1} style={style}
          {...testHook(TestIds.treeIcon, { expanded, attributes: { "row-id": rowData.data[treeId] } })}>
          {getIconCode(`fa-${icon}`, "p-treetable-toggler-icon fa-fw")}
          <Ripple />
        </button>
        {baseCellTemplate({ ...rowData.data, id: rowData.data[treeId] }, column)}
      </div>;
    } else {
      return baseCellTemplate({ ...rowData.data, id: rowData.data[treeId] }, column);
    }
  }, [attributes, onTogglerClick, baseCellTemplate, treeId, expandedKeys]);

  // PrimeReact evaluates "rowClassName" with the node of a row right before the row pass-through, in the same render:
  // the id of the row being rendered is kept here to give the row its "row-id".
  const renderingRowId = useRef(null);
  const renderingRowDeleted = useRef(false);
  const rowClassName = useCallback((node) => {
    renderingRowId.current = node?.data?.[treeId];
    renderingRowDeleted.current = node?.data?.$row?.operation === OperationType.DELETE;
    return null;
  }, [treeId]);
  const passThrough = useMemo(() => gridPassThrough({
    gridId: address?.component,
    tree: true,
    getRowId: () => renderingRowId.current,
    isRowDeleted: () => renderingRowDeleted.current
  }), [address?.component]);

  const { style, headerModel = [], max, disablePagination, loadAll, visible, loading = false, iconLoading } = attributes;
  const { first = 0, rows = max } = specificAttributes;
  const styles = classNames("p-treetable-sm", "expandible-vertical", style, { "hidden": !visible });
  const loadingIcon = getGridIconLoading(iconLoading, settings);
  // The skeleton replaces the rows in the body instead of masking them (see AweGrid)
  const reloading = isGridReloading(loading, loadingIcon, model?.values?.length);
  const showSkeleton = loading && !reloading && isSkeletonIcon(loadingIcon);
  let emptyMessage = loading || !isLoaded(attributes, modelLoaded) ? "" : <AweGridEmptyState attributes={attributes} />;
  if (showSkeleton) {
    emptyMessage = <AweGridSkeleton columns={visibleColumns} rows={rows || settings.recordsPerPage} />;
  }

  return <AweGridContainer id={id} onKeyCancelRow={cancelRow} onKeySaveRow={saveRow} onContextMenu={onContextMenu} loading={loading} reloading={reloading}>
    <TreeTable
      id={address?.component}
      value={showSkeleton ? [] : nodes} expandedKeys={expandedKeys} selectionKeys={selectionKeys} onToggle={() => null}
      className={styles}
      selectionMode="single"
      onSelectionChange={onSelectTree}
      onContextMenu={onContextMenu}
      onContextMenuSelectionChange={onSelectTree}
      headerColumnGroup={headerTemplate()}
      footerColumnGroup={footerTemplate()}
      paginator lazy={!loadAll || showSkeleton}
      loading={loading && !showSkeleton}
      paginatorTemplate={disablePagination ? "" : "FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"}
      first={first} rows={rows} paginatorLeft={buttonsTemplate()} onPage={() => null}
      paginatorRight={<span>&nbsp;</span>} totalRecords={nodes.length} rowsPerPageOptions={rowsPerPageOptions}
      emptyMessage={emptyMessage}
      resizableColumns={headerModel.length === 0} columnResizeMode="fit"
      scrollable
      loadingIcon={gridLoadingIcon(reloading, loadingIcon)}
      rowClassName={rowClassName}
      pt={passThrough}
    >
      {
        visibleColumns
          .map(col => {
            const { name, sortField, align, charlength = null, width = null } = col;
            return <Column
              key={name}
              field={sortField || name}
              body={rowData => cellTemplate(rowData, name)}
              bodyClassName={getColumnWrap(col).wrap ? "p-cell-editing p-cell-wrapping" : "p-cell-editing"}
              bodyStyle={{ ...getWidthStyle(charlength, width), textAlign: align }}
              pt={bodyCellPassThrough(name)}
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
