import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {Calendar} from "primereact/calendar";
import {bindMethods, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {fromDate, getAvailableDates, getDisabledDates, getMaxDate, getMinDate, toDate} from "../utilities/dates";

class AweInputFilteredDate extends AweTextComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["onChange", "getComponent"]);
  }

  onChange(e) {
    if (e.target.value !== this.getValue()) {
      const value = fromDate(e.target.value) || "";
      const {address, updateModelWithDependencies} = this.props;
      updateModelWithDependencies(address, {values: [{value: value, label: value, selected: true}]});
    }
  }

  getComponent(style) {
    const {t, address, model, attributes, settings} = this.props;
    const {placeholder, required, readonly, size} = attributes;
    const availableDates = getAvailableDates(model.values);
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});

    return <Calendar
      id={address.component}
      value={toDate(this.getValue())}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      onChange={this.onChange}
      dateFormat="dd/mm/yy"
      disabledDates={getDisabledDates(availableDates)}
      maxDate={getMaxDate(availableDates)}
      minDate={getMinDate(availableDates)}
      required={required}
      disabled={readonly}
      locale={settings.language}
      showButtonBar
    />;
  }
}

export default connectComponent(AweInputFilteredDate);
