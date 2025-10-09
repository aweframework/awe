import {formatNumber} from "../utilities/numbers";
import {extractCellValue} from "../utilities/grid";

function ColumnStaticNumeric(props) {
  const {data, numberFormat} = props;
  return formatNumber(extractCellValue(data), numberFormat);
}

export default ColumnStaticNumeric;
