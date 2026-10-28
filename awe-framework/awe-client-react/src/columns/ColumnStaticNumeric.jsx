import React from "react";
import {formatNumber} from "../utilities/numbers";
import {extractCellValue} from "../utilities/grid";

function ColumnStaticNumeric(props) {
  const {data, numberFormat} = props;
  const visibleValue = formatNumber(extractCellValue(data), numberFormat);
  // One line, cut with an ellipsis and whole in the hint, as the other columns
  return <span className="p-cell-text white-space-nowrap p-text-truncate" title={visibleValue}>{visibleValue}</span>;
}

export default ColumnStaticNumeric;
