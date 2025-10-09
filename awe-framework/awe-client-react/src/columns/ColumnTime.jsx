import React from "react";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {Calendar} from "primereact/calendar";
import {fromTime, toTime} from "../utilities/dates";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";

function ColumnTime(props) {
  const { placeholder, required, readonly, data, align, style, address } = props;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));

  const onChange = (e) => {
    if (data.value !== e.value) {
      updateModelWithDependencies(address, {values: fromTime(e.value)});
    }
  };

  const classes = classNames("column-editor", {"p-invalid": data?.error}, style, data?.style);
  return (
    <Calendar
      value={toTime(data.value)}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      className={classes}
      inputStyle={{textAlign: align || "center"}}
      timeOnly
      showSeconds
      onChange={onChange}
      locale={settings.language}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
  );
}

ColumnTime.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  style: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
};

export default ColumnTime;
