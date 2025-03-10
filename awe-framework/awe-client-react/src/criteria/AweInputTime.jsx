import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {Calendar} from "primereact/calendar";
import {bindMethods} from "../utilities";
import {fromTime, toTime} from "../utilities/dates";
import {classNames} from "../utilities/components";

class AweInputTime extends AweTextComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["onChange", "getComponent"]);
  }

  onChange(e) {
    if (e.target.value !== this.getValue()) {
      const value = fromTime(e.target.value) || "";
      const {address, updateModelWithDependencies} = this.props;
      updateModelWithDependencies(address, {values: [{value: value, label: value, selected: true}]});
    }
  }

  getComponent(style) {
    const {t, address, settings, attributes} = this.props;
    const {placeholder, required, readonly, size} = attributes;
    const classes = classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style);

    return <Calendar
      id={address.component}
      value={toTime(this.getValue())}
      className={classes}
      placeholder={t(placeholder)}
      onChange={this.onChange}
      required={required}
      disabled={readonly}
      locale={settings.language}
      timeOnly
      showSeconds
    />;
  }
}

export default connectComponent(AweInputTime);
