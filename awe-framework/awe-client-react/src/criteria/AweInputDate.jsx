import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {Calendar} from "primereact/calendar";
import {bindMethods, translateLabel} from "../utilities";
import {fromDate, toDate} from "../utilities/dates";
import {classNames} from "../utilities/components";

class AweInputDate extends AweTextComponent {

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
    const {t, address, settings, attributes} = this.props;
    const {placeholder, required, readonly, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});

    return <Calendar
      id={address.component}
      value={this.getValue()}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      onChange={this.onChange}
      dateFormat="dd/mm/yy"
      required={required}
      disabled={readonly}
      locale={settings.language}
      showButtonBar
    />;
  }
}

export default connectComponent(AweInputDate);
