import React, {useCallback} from "react";
import {Calendar} from "primereact/calendar";
import {fromDate, toDate} from "../utilities/dates";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {getFirstDefinedValue} from "../utilities/general";

function ColumnDate(props) {
  const { placeholder, required, readonly, data, attrs, align, style, address } = props;
  const {style: cellStyle, value: cellValue} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    if (cellValue !== e.value) {
      dispatch(updateModelWithDependencies(address, { values: fromDate(e.value) }));
    }
  }, [data, address, dispatch]);

  const classes = classNames(style, cellStyle, "column-editor", { "p-invalid": error }, {"hidden": !visible});

  return (
    <Calendar
      value={toDate(cellValue)}
      placeholder={translateLabel(placeholder, t)}
      required={getFirstDefinedValue(cellRequired, required, false)}
      disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
      className={classes}
      inputStyle={{ textAlign: align || "center" }}
      showButtonBar
      dateFormat="dd/mm/yy"
      onChange={onChange}
      invalid={error}
      locale={settings.language}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{ position: "bottom", className: "validation-tooltip" }}
    />
  );
}

ColumnDate.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object.isRequired,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnDate;
