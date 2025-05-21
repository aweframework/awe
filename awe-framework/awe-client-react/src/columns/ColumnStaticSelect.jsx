import React, {Component} from "react";
import {translateLabel} from "../utilities";

export default class ColumnStaticSelect extends Component {

  render() {
    const {t, data, model} = this.props;
    const modelValue = model.values.find(cell => String(cell.value) === String(data.value));
    const visibleValue = modelValue ? translateLabel(modelValue.label, t) || modelValue.value : data.label || data.value;
    return <span className="p-cell-text white-space-nowrap p-text-truncate" title={visibleValue}>{visibleValue}</span>;
  }
}
