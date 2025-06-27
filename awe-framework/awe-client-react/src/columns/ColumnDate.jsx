import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Calendar} from "primereact/calendar";
import {fromDate, toDate} from "../utilities/dates";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnDate extends Component {

  constructor(props) {
    super(props);

    // Bind events
    this.onChange = this.onChange.bind(this);
  }

  onChange(e) {
    const {address, updateModelWithDependencies, data} = this.props;
    if (data.value !== e.value) {
      updateModelWithDependencies(address, {values: fromDate(e.value)});
    }
  }

  render() {
    const {t, placeholder, required, readonly, data, align, settings, style} = this.props;
    const classes = classNames(style, data.style, "column-editor", {"p-invalid": data?.error});
    return <Calendar
      value={toDate(data.value)}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      className={classes}
      inputStyle={{textAlign: align || "center"}}
      showButtonBar
      dateFormat="dd/mm/yy"
      onChange={this.onChange}
      locale={settings.language}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />;
  }
}

ColumnDate.propTypes = {
  updateModelWithDependencies: PropTypes.func.isRequired,
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  align: PropTypes.string,
  settings: PropTypes.object,
  t: PropTypes.func.isRequired,
  style: PropTypes.string
};

export default withTranslation()(ColumnDate);
