import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Calendar} from "primereact/calendar";
import {fromTime, toTime} from "../utilities/dates";
import {formatMessage} from "../utilities";
import {classNames} from "../utilities/components";

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
    const {t, placeholder, required, readonly, data, align, settings} = this.props;
    const classes = classNames("column-editor", {"p-invalid": data?.error});
    return <Calendar
      value={toTime(data.value)}
      placeholder={t(placeholder)}
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

export default withTranslation()(ColumnTime);
