import React from "react";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {Calendar} from "primereact/calendar";
import {fromTime, toTime} from "../utilities/dates";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import {getFirstDefinedValue} from "../utilities/general";

function ColumnTime(props) {
  const { placeholder, required, readonly, data, attrs, align, style, address } = props;
  const {style: cellStyle, value: cellValue} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;

  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));

  const onChange = (e) => {
    if (cellValue !== e.value) {
      updateModelWithDependencies(address, {values: fromTime(e.value)});
    }
  };

  const classes = classNames(style, cellStyle, "column-editor", {"p-invalid": error}, {"hidden": !visible});
  return (
    <Calendar
      value={toTime(cellValue)}
      placeholder={translateLabel(placeholder, t)}
      required={getFirstDefinedValue(cellRequired, required, false)}
      invalid={error}
      disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
      className={classes}
      inputStyle={{textAlign: align || "center"}}
      timeOnly
      showSeconds
      onChange={onChange}
      locale={settings.language}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
  );
}

ColumnTime.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object.isRequired,
  style: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
};

export default ColumnTime;
