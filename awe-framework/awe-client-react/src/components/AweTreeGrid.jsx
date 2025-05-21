import React from "react";
import {TreeTable} from "primereact/treetable";
import {Column} from "primereact/column";
import {connectComponent} from "./AweComponent";
import {bindMethods, generateServerAction} from "../utilities";
import {getWidthStyle} from "../utilities/grid";
import {classNames} from "../utilities/components";
import {Columns} from "../utilities/structure";
import AweGridContainer from "./AweGridContainer";
import {AweGridCommons} from "./AweGridCommons";
import "./AweTreeGrid.less";
import {ColumnGroup} from "primereact/columngroup";
import {Row} from "primereact/row";
import {Ripple} from "primereact/ripple";
import _ from "lodash";
import {isEmpty} from "../utilities/general";

/**
 * AWE Grid component
 * @extends AweGridCommons
 * @category Components
 * @subcategory Grid
 */
class AweTreeGrid extends AweGridCommons {

  /**
   * Create a grid
   * @param {object} props Grid properties
   */
  constructor(props) {
    super(props);

    bindMethods(this, ["onTogglerClick", "onSelect",
      "formatTreeData", "getExpandedKeys", "getSelectionKeys", "findRowByIndex",
      "headerTemplate", "footerTemplate", "cellTemplate"]);
  }

  onTogglerClick(row) {
    const {address, addActionsTop, attributes, settings, model, treeId} = this.props;
    const {loadAll, targetAction} = attributes;
    const {values} = model;
    const rowValues = values.find(r => r[treeId] === row) || {};
    if (loadAll || rowValues.$row?.loaded) {
      addActionsTop([{type: "tree-branch", address, parameters: {row}}]);
    } else {
      // Load branch
      addActionsTop([generateServerAction({expandingBranch: row}, "tree-branch", targetAction, address, false, false, settings)]);
    }
  }

  onSelect(event) {
    const {address, addActionsTop} = this.props;
    const values = [event.value].flat().filter(i => !isEmpty(i));
    addActionsTop([{type: "select-row", address, parameters:{values}}]);
  }

  findRowByIndex(rowIndex) {
    const {nodes} = this.state;
    const row = String(rowIndex)
      .split("_")
      .map(value => parseInt(value, 10))
      .reduce((node, index) => node.children[index], {children:nodes})
    return row.key;
  }

  formatTreeData(data, parent, level) {
    const {loadAll, treeId = "id", treeParent = "parent"} = this.props;
    return data
      .map(row => ({...row, [treeParent]: row[treeParent] === null ? "" : row[treeParent] }))
      .filter(row => row[treeParent] === parent)
      .map(row => ({
        key: row[treeId],
        id: row[treeId],
        $row: {...(row.$row || {})},
        data: {...row},
        ...(!loadAll && {leaf: !!row.isLeaf} || {}),
        level,
        children: this.formatTreeData(data, row[treeId], level + 1)}));
  }

  getExpandedKeys(values) {
    const {treeId = "id"} = this.props;
    return values
      .filter(row => row.$row?.expanded)
      .reduce((keys, row) => ({...keys, [row[treeId]]: true}), {});
  }

  getSelectionKeys(values) {
    const {treeId = "id"} = this.props;
    return (values.find(row => row.selected) || {})[treeId] || null;
  }

  headerTemplate() {
    const {attributes} = this.props;
    const {headerModel, columnModel} = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (headerModel.length > 0) {
      const header = this.getHeader();

      return <ColumnGroup>
        <Row key="firstRow">
          {header.columns.map(col => col.startColumnName ? this.headerColumnTemplate(col) : this.columnTemplate(col, 2))}
          {this.postColumnTemplates("header", 2)}
        </Row>
        <Row key="secondRow">
          {header.grouped.map(col => this.columnTemplate(col, 1))}
        </Row>
      </ColumnGroup>;
    } else {
      return <ColumnGroup>
        <Row key="firstRow">
          {visibleColumns.map(col => this.columnTemplate(col, 1))}
          {this.postColumnTemplates("header", 1)}
        </Row>
      </ColumnGroup>;
    }
  }

  footerTemplate() {
    const {attributes} = this.props;
    const {columnModel, showTotals} = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (showTotals) {
      return <ColumnGroup>
        <Row>
          {visibleColumns.map(col => this.footerColumnTemplate(col))}
          {this.postColumnTemplates("footer", 1, false)}
        </Row>
      </ColumnGroup>;
    }
    return null;
  }

  cellTemplate(rowData, column) {
    const {t, settings, attributes, updateModelWithDependencies, updateAttributes, addActionsTop, address} = this.props;
    const {treeId = "id", expandColumn = treeId} = attributes;
    const iconClassName = classNames('p-treetable-toggler-icon pi pi-fw', { 'pi-chevron-right': !rowData?.$row?.expanded, 'pi-chevron-down': rowData?.$row?.expanded });
    const style = { marginLeft: rowData.level * 16 + 'px', visibility: (rowData.leaf === false || (rowData.children && rowData.children.length)) ? 'visible' : 'hidden' };
    if (column === expandColumn) {
      return <div className="p-treetable-expander-column">
        <button type="button" className="p-treetable-toggler p-link p-unselectable-text" onClick={() => this.onTogglerClick(rowData.data[treeId])} tabIndex={-1} style={style}>
          <i className={iconClassName}></i>
          <Ripple />
        </button>
        {
          Columns({
            ...attributes.columnModel.find(c => c.name === column),
            updateModelWithDependencies,
            updateAttributes,
            addActionsTop,
            address: {...address, column, row: rowData.data[treeId]},
            t, settings
          }, rowData.data[column], rowData?.$row?.editing)
        }
      </div>;
    } else {
      return super.cellTemplate({...rowData.data, id: rowData.data[treeId]}, column);
    }
  }

  /**
   * Update tree data
   * @param values Values to update
   */
  updateTreeData(values) {
    const nodes = this.formatTreeData(values, "", 0);
    const expandedKeys = this.getExpandedKeys(values);
    const selectionKeys = this.getSelectionKeys(values);
    this.setState({nodes, expandedKeys, selectionKeys});
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    const {model} = this.props;
    const {values} = model;
    this.updateTreeData(values);
  }

  /**
   * Check if component should update
   * @param nextProps Next properties
   * @param _nextState Next state
   * @param _nextContext Next context
   * @returns {boolean} Component should update
   */
  shouldComponentUpdate(nextProps, _nextState, _nextContext) {
    const {values} = nextProps.model;
    if (!_.isEqual(this.props.model.values, values)) {
      this.updateTreeData(values);
    }
    return !nextProps.disabled;
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {attributes, specificAttributes} = this.props;
    const {style, headerModel, columnModel, max, disablePagination, loadAll, visible} = attributes;
    const {first = 0, rows = max} = specificAttributes;
    const {nodes, expandedKeys, selectionKeys, rowsPerPageOptions} = this.state;
    const styles = classNames("p-treetable-sm", "expandible-vertical", style, {"hidden": !visible});

    return <AweGridContainer onKeyCancelRow={this.cancelRow} onKeySaveRow={this.saveRow} onContextMenu={this.onContextMenu}>
      <TreeTable
        id={this.props.address.component}
        value={nodes} expandedKeys={expandedKeys} selectionKeys={selectionKeys} onToggle={() => alert("toggle")}
        className={styles}
        selectionMode="single"
        onSelectionChange={this.onSelect}
        onContextMenu={this.onContextMenu}
        onContextMenuSelectionChange={this.onSelect}
        headerColumnGroup={this.headerTemplate()}
        footerColumnGroup={this.footerTemplate()}
        paginator lazy={!loadAll}
        paginatorTemplate={disablePagination ? "" : "FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"}
        first={first} rows={rows} paginatorLeft={this.buttonsTemplate()} onPage={this.onPage}
        paginatorRight={<span>&nbsp;</span>} totalRecords={this.state.length} rowsPerPageOptions={rowsPerPageOptions}
        emptyMessage={""}
        resizableColumns={headerModel.length === 0} columnResizeMode="fit"
        scrollable
      >
        {
          columnModel
            .filter(col => !col.hidden)
            .map(col => {
              const {name, sortField, align, charlength = null, width = null} = col;
              return <Column
                key={name}
                field={sortField || name}
                body={rowData => this.cellTemplate(rowData, name)}
                bodyClassName="p-cell-editing"
                bodyStyle={{...getWidthStyle(charlength, width), textAlign: align}}
              />;
            })
        }
        {this.postColumnTemplates("cell", 1, true)}
      </TreeTable>
      { this.contextMenuTemplate() }
    </AweGridContainer>;
  }
}

export default connectComponent(AweTreeGrid);
