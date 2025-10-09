import React, {useCallback, useEffect, useState} from "react";
import {Calendar} from "primereact/calendar";
import {formatMessage, translateLabel} from "../utilities";
import {
  fromDate,
  getAvailableDates,
  getDisabledDates,
  getMaxDate,
  getMinDate,
  toDate
} from "../utilities/dates";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";

function ColumnFilteredDate(props) {
  const { placeholder, required, readonly, data, align, style, address, model } = props;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const [disabledDates, setDisabledDates] = useState([]);
  const [maxDate, setMaxDate] = useState(null);
  const [minDate, setMinDate] = useState(null);

  const onChange = useCallback((e) => {
    if (data.value !== e.value) {
      dispatch(updateModelWithDependencies(address, { values: fromDate(e.value) }));
    }
  }, [data, address, dispatch]);

  useEffect(() => {
    const availableDates = getAvailableDates(model?.values);
    setDisabledDates(getDisabledDates(availableDates));
    setMaxDate(getMaxDate(availableDates));
    setMinDate(getMinDate(availableDates));
  }, [model?.values]);

  const classes = classNames(style, data?.style, "column-editor", { "p-invalid": data?.error });

  return (
    <Calendar
      value={toDate(data.value)}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      readOnlyInput={readonly}
      className={classes}
      inputStyle={{ textAlign: align || "center" }}
      showButtonBar
      disabledDates={disabledDates}
      maxDate={maxDate}
      minDate={minDate}
      dateFormat="dd/mm/yy"
      onChange={onChange}
      locale={settings.language}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{ position: "bottom", className: "validation-tooltip" }}
    />
  );
}

ColumnFilteredDate.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  model: PropTypes.object,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnFilteredDate;
