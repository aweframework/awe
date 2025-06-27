import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Calendar} from "primereact/calendar";
import {fromTime, toTime} from "../utilities/dates";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnTime extends Component {

  constructor(props) {
    super(props);

    // Bind events
    this.onChange = this.onChange.bind(this);
  }

  onChange(e) {
    const {address, updateModelWithDependencies, data} = this.props;
    if (data.value !== e.value) {
      updateModelWithDependencies(address, {values: fromTime(e.value)});
    }
  }

  render() {
    const {t, placeholder, required, readonly, data, align, settings, style} = this.props;
    const classes = classNames("column-editor", {"p-invalid": data?.error}, style, data?.style);
    return <Calendar
      value={toTime(data.value)}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      className={classes}
      inputStyle={{textAlign: align || "center"}}
      timeOnly
      showSeconds
      onChange={this.onChange}
      locale={settings.language}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />;
  }
}

ColumnTime.propTypes = {
  updateModelWithDependencies: PropTypes.func.isRequired,
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  t: PropTypes.func.isRequired,
  style: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  settings: PropTypes.object,
  align: PropTypes.string,
};

export default withTranslation()(ColumnTime);
