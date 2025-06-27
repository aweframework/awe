import React, {Component} from "react";
import {formatNumber} from "../utilities/numbers";

import {extractCellValue} from "../utilities/grid";
import PropTypes from "prop-types";

export default class ColumnStaticNumeric extends Component {

  render() {
    const {data, numberFormat} = this.props;
    return formatNumber(extractCellValue(data), numberFormat);
  }
}

ColumnStaticNumeric.propTypes = {
  data: PropTypes.object.isRequired,
};