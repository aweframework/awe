import React, {useCallback} from "react";
import {Calendar} from "primereact/calendar";
import {fromDate, toDate} from "../utilities/dates";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";

function ColumnDate(props) {
  const { placeholder, required, readonly, data, align, style, address } = props;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    if (data.value !== e.value) {
      dispatch(updateModelWithDependencies(address, { values: fromDate(e.value) }));
    }
  }, [data, address, dispatch]);

  const classes = classNames(style, data.style, "column-editor", { "p-invalid": data?.error });

  return (
    <Calendar
      value={toDate(data.value)}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      className={classes}
      inputStyle={{ textAlign: align || "center" }}
      showButtonBar
      dateFormat="dd/mm/yy"
      onChange={onChange}
      locale={settings.language}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{ position: "bottom", className: "validation-tooltip" }}
    />
  );
}

ColumnDate.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnDate;
