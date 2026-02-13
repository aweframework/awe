import React, { useCallback } from "react";
import { InputNumber } from "primereact/inputnumber";
import { translateNumberFormat } from "../utilities/numbers";
import { formatMessage, translateLabel } from "../utilities";
import { classNames } from "../utilities/components";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { getFirstDefinedValue } from "../utilities/general";

function ColumnNumeric(props) {
  const { placeholder, required, readonly, data, attrs, numberFormat, align, style, address } = props;
  const { style: cellStyle, value: cellValue } = data;
  const { readonly: cellReadonly, validationRules = {}, visible = true, error = null } = attrs;
  const { required: cellRequired } = validationRules;
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    if (cellValue !== e.value) {
      dispatch(updateModelWithDependencies(address, { values: e.value }));
    }
  }, [cellValue, address, dispatch]);

  const { maxFractionDigits, minFractionDigits, min, max, suffix, locale, useGrouping } = translateNumberFormat(numberFormat);
  const classes = classNames("column-editor", style, { "p-invalid": error }, { "hidden": !visible });
  return <div className={classes}>
    <InputNumber
      value={cellValue}
      mode="decimal"
      placeholder={translateLabel(placeholder, t)}
      required={getFirstDefinedValue(cellRequired, required, false)}
      disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
      locale={locale}
      useGrouping={useGrouping}
      maxFractionDigits={maxFractionDigits}
      minFractionDigits={minFractionDigits}
      min={min}
      max={max}
      suffix={suffix}
      className={classNames(cellStyle)}
      invalid={error}
      inputStyle={{ textAlign: align || "right" }}
      onValueChange={onChange}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{ position: "bottom", className: "validation-tooltip" }}
    />
  </div>;
}

ColumnNumeric.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object.isRequired,
  numberFormat: PropTypes.object,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnNumeric;
