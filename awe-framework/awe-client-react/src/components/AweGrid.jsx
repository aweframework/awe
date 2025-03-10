import React from "react";
import {DataTable} from "primereact/datatable";
import {Column} from "primereact/column";
import {ColumnGroup} from "primereact/columngroup";
import {Row} from "primereact/row";
import {connectComponent} from "./AweComponent";
import {bindMethods} from "../utilities";
import {getWidthStyle} from "../utilities/grid";
import AweGridContainer from "./AweGridContainer";
import {AweGridCommons} from "./AweGridCommons";
import "./AweGrid.less";
import {classNames} from "../utilities/components";

/**
 * AWE Grid component
 * @extends AweGridCommons
 * @category Components
 * @subcategory Grid
 */
class AweGrid extends AweGridCommons {

  /**
   * Create a grid
   * @param {object} props Grid properties
   */
  constructor(props) {
    super(props);

    bindMethods(this, ["onPage", "onSort", "onFilter", "onRowDoubleClick", "findRowByIndex",
      "headerTemplate", "footerTemplate", "rowClassName"]);
  }

  onRowDoubleClick(event) {
    return this.editRow(this.findRowByIndex(event.index));
  }

  findRowByIndex(rowIndex) {
    const {model} = this.props;
    return model.values[rowIndex].id;
  }

  onPage(event) {
    const {address, attributes} = this.props;
    const {loadAll} = attributes;
    const {first, rows, page} = event;
    this.filterRow({type: "change-page", address, parameters: {page: page + 1, first, rows, max: loadAll ? 0 : rows}});
  }

  onSort(event) {
    const {address} = this.props;
    const {multiSortMeta} = event;
    this.filterRow({type: "change-sort", address, parameters: {sort: multiSortMeta.map(s => ({
      id: s.field,
      order: s.order,
      direction: s.order > 0 ? "asc" : "desc"
    }))}});
  }

  onFilter(event) {
    const {address, addActionsTop, model} = this.props;
    const {filters} = event;
    const {values} = model;
    const editingRow = values.find(row => row.$row?.editing);
    const cancelRowAction = editingRow ? [{type:"cancel-row", address}] : [];
    addActionsTop([...cancelRowAction, {type:"change-filter", address, parameters: {filters}}]);
  }

  headerTemplate() {
    const {attributes} = this.props;
    const {headerModel, columnModel} = attributes;
    const visibleColumns = columnModel.filter(col => !col.hidden);
    if (headerModel.length > 0) {
      const header = this.getHeader();
      return <ColumnGroup>
        <Row>
          {this.preColumnTemplates("header", 2)}
          {header.columns.map(col => col.startColumnName ? this.headerColumnTemplate(col) : this.columnTemplate(col, 2))}
          {this.postColumnTemplates("header", 2)}
        </Row>
        <Row>
          {header.grouped.map(col => this.columnTemplate(col, 1))}
        </Row>
      </ColumnGroup>;
    } else {
      return <ColumnGroup>
        <Row>
          {this.preColumnTemplates("header", 1)}
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
          {this.preColumnTemplates("footer", 1)}
          {visibleColumns.map(col => this.footerColumnTemplate(col))}
          {this.postColumnTemplates("footer", 1, false)}
        </Row>
      </ColumnGroup>;
    }
    return null;
  }

  rowClassName(data) {
    return [data.id, data.$row?.editing ? "editing" : null, data["_style_"]].filter(v => v).join(" ");
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    const {attributes, settings} = this.props;
    const {pagerValues = []} = attributes;
    const max = attributes.max || settings.recordsPerPage;
    const pager = pagerValues.length > 0 ? pagerValues : [10, 20, 30];
    this.setState({
      rowsPerPageOptions: [..._.union([...pager, max]).sort(Number)]
    });
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    const element = document.querySelector(".p-row-editor-save");
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }

  /**
   * Check if component should update
   * @param nextProps Next properties
   * @param _nextState Next state
   * @param _nextContext Next context
   * @returns {boolean} Component should update
   */
  shouldComponentUpdate(nextProps, _nextState, _nextContext) {
    return !nextProps.disabled;
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {attributes, model, specificAttributes} = this.props;
    const {style, headerModel, columnModel, multiselect, disablePagination, max, loadAll, visible} = attributes;
    const {records = 0, values = []} = model;
    const {rowsPerPageOptions} = this.state;
    const {filters, first = 0, rows = max, sort = []} = specificAttributes;
    const classes = classNames("p-datatable-sm", "expand", style, {"hidden": !visible});
    const selectedValues = values.filter(item => item.selected);

    return <AweGridContainer onKeyCancelRow={this.cancelRow} onKeySaveRow={this.saveRow} onContextMenu={this.onContextMenu}>
      <DataTable
        selection={selectedValues}
        selectionMode={multiselect ? null : "single"}
        onSelectionChange={this.onSelect} onRowDoubleClick={this.onRowDoubleClick}
        onContextMenu={this.onContextMenu}
        contextMenuSelection={selectedValues}
        onContextMenuSelectionChange={this.onSelect}
        id={this.props.address.component}
        value={values} className={classes}
        headerColumnGroup={this.headerTemplate()}
        footerColumnGroup={this.footerTemplate()}
        rowClassName={this.rowClassName}
        dataKey="id"
        emptyMessage={""}
        paginator lazy={!loadAll}
        paginatorTemplate={disablePagination ? "" : "FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"}
        first={first} rows={rows} paginatorLeft={this.buttonsTemplate()} onPage={this.onPage}
        paginatorRight={<span>&nbsp;</span>} totalRecords={records} rowsPerPageOptions={rowsPerPageOptions}
        resizableColumns={headerModel.length === 0} columnResizeMode="fit"
        scrollable scrollHeight={"flex"}
        editMode="row"
        sortMode="multiple" removableSort
        multiSortMeta={sort.map(item => ({field: item.id, order: item.direction === "asc" ? 1 : -1}))}
        onSort={this.onSort}
        onFilter={this.onFilter}
        filters={filters} filterDisplay={"menu"}
      >
        {this.preColumnTemplates("cell", 1)}
        {
          columnModel.filter(col => !col.hidden)
            .map(col => {
              const {name, sortField, align, charlength = null, width = null} = col;
              return <Column
                key={name}
                columnKey={name}
                field={sortField || name}
                sortField={sortField || name}
                body={rowData => this.cellTemplate(rowData, name)}
                bodyClassName={`p-cell-editing ${name}`}
                style={{...getWidthStyle(charlength, width)}}
                bodyStyle={{textAlign: align, justifyContent: align}}
              />;
            })
        }
        {this.postColumnTemplates("cell", 1, true)}
      </DataTable>
      { this.contextMenuTemplate() }
    </AweGridContainer>;
  }
}

export default connectComponent(AweGrid);
