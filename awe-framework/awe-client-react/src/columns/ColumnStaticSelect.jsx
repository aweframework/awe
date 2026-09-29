import React from "react";
import {translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import PropTypes from "prop-types";

function ColumnStaticSelect(props) {
  const {data, model} = props;

  const { t } = useTranslation();
  const modelValue = model.values.find(cell => String(cell.value) === String(data.value));
  const visibleValue = modelValue ? translateLabel(modelValue.label, t) || modelValue.value : data.label || data.value;
  return <span className="p-cell-text white-space-nowrap p-text-truncate" title={visibleValue}>{visibleValue}</span>;
}

ColumnStaticSelect.propTypes = {
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  model: PropTypes.object,
};

export default ColumnStaticSelect;