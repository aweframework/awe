import React, {Children, cloneElement} from "react";
import TableRenderers from "react-pivottable/TableRenderers";
import {PivotData} from "react-pivottable/Utilities";
import "./TableBarchartRenderer.less";

const {Table} = TableRenderers;

/** Same scale as the AngularJS barchart: the maximum of the row uses 1/1.4 of the cell height */
const BAR_SCALE = 1.4;

const isDrawable = value => typeof value === "number" && Number.isFinite(value);

/**
 * Bar height (percentage of the cell) of a value, or undefined when no bar is drawn: a negative value, or a row
 * whose maximum is not positive (AngularJS gets an invalid or nonsensical CSS height there)
 * @param {number} value Cell value
 * @param {number} max Maximum of the values of the row
 * @returns {string|undefined} CSS height
 */
function barHeight(value, max) {
  const percentage = 100 * value / (BAR_SCALE * max);
  return max > 0 && Number.isFinite(percentage) && percentage >= 0 ? `${Math.min(percentage, 100)}%` : undefined;
}

const hasClass = (cell, name) => String(cell?.props?.className ?? "").split(/\s+/).includes(name);

const cellsOf = (row, cellClass) => Children.toArray(row?.props?.children).filter(cell => hasClass(cell, cellClass));

/**
 * Wrap the cells of one table row with their bar
 * @param {Object} row Table row element
 * @param {string} cellClass Class of the cells to wrap
 * @param {Array<number>} values Raw value of each cell of that class, in order
 * @returns {Object|null} Table row element with bars, or null when the cells do not match the values
 */
function drawBars(row, cellClass, values) {
  if (cellsOf(row, cellClass).length !== values.length) {
    return null;
  }
  const max = Math.max(...values.filter(isDrawable));
  let index = 0;
  return cloneElement(row, {}, Children.map(row.props.children, cell => {
    if (!hasClass(cell, cellClass)) {
      return cell;
    }
    const value = values[index++];
    if (!isDrawable(value)) {
      return cell;
    }
    const height = barHeight(value, max);
    return cloneElement(cell, {className: `${cell.props.className} pvtBarCell`},
      <div className="pvtBarWrapper">
        {height && <div className="pvtBar" style={{height}}/>}
        <div className="pvtBarText">{cell.props.children}</div>
      </div>);
  }));
}

/**
 * Draw the bars of the table body, or return null when its layout is not the expected one
 * @param {Object} body tbody element
 * @param {PivotData} pivotData Pivot data
 * @returns {Array|null} Rows of the body
 */
function drawBody(body, pivotData) {
  const rowKeys = pivotData.getRowKeys();
  const colKeys = pivotData.getColKeys();
  const value = (rowKey, colKey) => pivotData.getAggregator(rowKey, colKey).value();
  const rows = Children.toArray(body.props.children);
  const valueRows = rows.filter(row => cellsOf(row, "pvtVal").length > 0);
  if (valueRows.length !== rowKeys.length) {
    return null;
  }
  let valueRow = 0;
  const drawn = rows.map(row => {
    if (valueRows.includes(row)) {
      const rowKey = rowKeys[valueRow++];
      return drawBars(row, "pvtVal", colKeys.map(colKey => value(rowKey, colKey)));
    }
    return cellsOf(row, "pvtTotal").length > 0 ? drawBars(row, "pvtTotal", colKeys.map(colKey => value([], colKey))) : row;
  });
  return drawn.includes(null) ? null : drawn;
}

/**
 * "Table Barchart" pivot renderer: the react-pivottable Table with a bar behind every value, scaled per row,
 * and behind the totals of the columns (last row), as the AngularJS renderer does. It relies on the layout of the
 * react-pivottable 0.11 table: with any other layout it renders the plain table.
 * @param {Object} rendererProps react-pivottable renderer props
 * @returns {JSX.Element} Pivot table
 */
function TableBarchartRenderer(rendererProps) {
  const props = {...Table.defaultProps, ...rendererProps};
  const table = new Table(props).render();
  const [head, body] = Children.toArray(table?.props?.children);
  if (head?.type !== "thead" || body?.type !== "tbody") {
    return table;
  }
  const rows = drawBody(body, new PivotData(props));
  return rows ? cloneElement(table, {}, head, cloneElement(body, {}, rows)) : table;
}

TableBarchartRenderer.propTypes = Table.propTypes;

export default TableBarchartRenderer;
