import React, {useCallback} from "react";
import {InputNumber} from "primereact/inputnumber";
import {translateNumberFormat} from "../utilities/numbers";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";

function ColumnNumeric(props) {
  const { placeholder, required, readonly, data, numberFormat, align, style, address } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    if (data.value !== e.value) {
      dispatch(updateModelWithDependencies(address, { values: e.value }));
    }
  }, [data, address, dispatch]);

  const { maxFractionDigits, minFractionDigits, min, max, suffix, locale } = translateNumberFormat(numberFormat);
  const classes = classNames(style, data?.style, { "p-invalid": data?.error });
  return <div className={"column-editor"}>
    <InputNumber
      value={data.value}
      mode="decimal"
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      locale={locale}
      maxFractionDigits={maxFractionDigits}
      minFractionDigits={minFractionDigits}
      min={min}
      max={max}
      suffix={suffix}
      className={classes}
      inputStyle={{ textAlign: align || "right" }}
      onValueChange={onChange}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{ position: "bottom", className: "validation-tooltip" }}
    />
  </div>;
}

ColumnNumeric.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  numberFormat: PropTypes.object,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnNumeric;
