import React, {Component} from "react";

export default class ColumnStaticSelect extends Component {

  render() {
    const {t, data, model} = this.props;
    const modelValue = model.values.find(cell => String(cell.value) === String(data.value));
    const visibleValue = modelValue ? t(modelValue.label) || modelValue.value : data.label || data.value;
    return <span className="p-cell-text white-space-nowrap p-text-truncate" title={visibleValue}>{visibleValue}</span>;
  }
}
