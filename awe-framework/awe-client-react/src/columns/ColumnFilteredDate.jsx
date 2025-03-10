import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Calendar} from "primereact/calendar";
import {bindMethods, formatMessage} from "../utilities";
import {
  fromDate,
  getAvailableDates,
  getDisabledDates,
  getMaxDate,
  getMinDate,
  toDate
} from "../utilities/dates";
import {classNames} from "../utilities/components";

class ColumnFilteredDate extends Component {

  constructor(props) {
    super(props);
    this.state = {disabledDates:[], maxDate: null, minDate: null};
    // Bind events
    bindMethods(this, ["onChange", "updateDisabledDates"]);
  }

  onChange(e) {
    const {address, updateModelWithDependencies, data} = this.props;
    if (data.value !== e.value) {
      updateModelWithDependencies(address, {values: fromDate(e.value)});
    }
  }

  updateDisabledDates() {
    const availableDates = getAvailableDates(this.props.model.values);
    this.setState({disabledDates: getDisabledDates(availableDates), maxDate: getMaxDate(availableDates), minDate: getMinDate(availableDates)});
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    this.updateDisabledDates();
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    if (!_.isEqual(prevProps.model.values, this.props.model.values)) {
      this.updateDisabledDates();
    }
  }

  render() {
    const {t, placeholder, required, readonly, data, align, settings} = this.props;
    const {disabledDates, minDate, maxDate} = this.state;
    const classes = classNames("column-editor", {"p-invalid": data?.error});
    return <Calendar
      value={toDate(data.value)}
      placeholder={t(placeholder)}
      required={required}
      readOnlyInput={readonly}
      className={classes}
      inputStyle={{textAlign: align || "center"}}
      showButtonBar
      disabledDates={disabledDates}
      maxDate={maxDate}
      minDate={minDate}
      dateFormat="dd/mm/yy"
      onChange={this.onChange}
      locale={settings.language}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />;
  }
}

export default withTranslation()(ColumnFilteredDate);
