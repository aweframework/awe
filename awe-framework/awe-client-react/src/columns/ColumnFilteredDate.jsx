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
import {getFirstDefinedValue} from "../utilities/general";

function ColumnFilteredDate(props) {
  const { placeholder, required, readonly, data, attrs, align, style, address, model } = props;
  const {style: cellStyle, value: cellValue, error = null} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true} = attrs;
  const {required: cellRequired} = validationRules;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const [disabledDates, setDisabledDates] = useState([]);
  const [maxDate, setMaxDate] = useState(null);
  const [minDate, setMinDate] = useState(null);

  const onChange = useCallback((e) => {
    if (cellValue !== e.value) {
      dispatch(updateModelWithDependencies(address, { values: fromDate(e.value) }));
    }
  }, [cellValue, address, dispatch]);

  useEffect(() => {
    const availableDates = getAvailableDates(model?.values);
    setDisabledDates(getDisabledDates(availableDates));
    setMaxDate(getMaxDate(availableDates));
    setMinDate(getMinDate(availableDates));
  }, [model?.values]);

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
      disabledDates={disabledDates}
      maxDate={maxDate}
      minDate={minDate}
      dateFormat="dd/mm/yy"
      onChange={onChange}
      locale={settings.language}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{ position: "bottom", className: "validation-tooltip" }}
    />
  );
}

ColumnFilteredDate.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object.isRequired,
  model: PropTypes.object,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnFilteredDate;
